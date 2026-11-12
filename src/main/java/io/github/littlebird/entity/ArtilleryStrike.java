package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.weapon.ArtilleryStrikeRules;
import io.github.littlebird.entity.weapon.ArtilleryStrikeRules.Offset;
import io.github.littlebird.entity.weapon.ArtilleryStrikeRules.Phase;
import io.github.littlebird.entity.weapon.Phl191Effects;
import io.github.littlebird.entity.weapon.RocketStrikeGrid;
import io.github.littlebird.registry.ModEntities;
import io.github.littlebird.registry.ModSounds;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

/** A hull's server-owned fire mission and the state its driver sees on the command screen. */
public final class ArtilleryStrike {
    public static final double PHL191_RANGE = 140.0D;
    public static final double PLZ05_RANGE = 200.0D;

    // Keep the off-map rocket ticking even at the edge of the driver's simulation distance.
    // A ticket expires shortly after the longest permitted rocket fall.
    private static final TicketType<ChunkPos> ROCKET_STRIKE_TICKET = TicketType.create(
            "battlefield_rocket_strike", Comparator.comparingLong(ChunkPos::toLong), 100);
    private static final Phase[] PHASES = Phase.values();

    public enum Kind {
        PHL191, PLZ05
    }

    /** Registered by each entity class, in its own synchronized-data namespace. */
    public record Data(EntityDataAccessor<Integer> phase, EntityDataAccessor<Integer> elapsed,
                       EntityDataAccessor<Integer> rounds, EntityDataAccessor<Float> targetX,
                       EntityDataAccessor<Float> targetZ, EntityDataAccessor<Integer> cooldown) {
        public void define(SynchedEntityData data) {
            data.define(phase, Phase.IDLE.ordinal());
            data.define(elapsed, 0);
            data.define(rounds, 0);
            data.define(targetX, 0.0F);
            data.define(targetZ, 0.0F);
            data.define(cooldown, 0);
        }
    }

    private final Entity vehicle;
    private final Data keys;
    private final Kind kind;
    private int missionTick = -1;
    private int roundsSent;
    private int cooldown;
    private double targetX;
    private double targetZ;
    private UUID callerId;
    private RocketStrikeGrid grid;

    public ArtilleryStrike(Entity vehicle, Data keys, Kind kind) {
        this.vehicle = vehicle;
        this.keys = keys;
        this.kind = kind;
    }

    /** Validates the current driver and target on the server before reserving a mission. */
    public void request(ServerPlayer caller, double x, double z) {
        if (!(vehicle.level() instanceof ServerLevel server) || vehicle.isRemoved()
                || caller.getVehicle() != vehicle || vehicle.getControllingPassenger() != caller
                || !LittleBirdConfig.artilleryStrikeEnabled() || missionTick >= 0 || cooldown > 0) {
            return;
        }
        double dx = x - vehicle.getX();
        double dz = z - vehicle.getZ();
        WorldBorder border = server.getWorldBorder();
        if (!ArtilleryStrikeRules.inRange(dx, dz, rangeBlocks()) || !border.isWithinBounds(x, z)) {
            return;
        }

        if (isRocketGrid()) {
            RocketStrikeGrid candidate = RocketStrikeGrid.facing(dx, dz, vehicle.getYRot());
            if (!gridInsideBorder(candidate, border, x, z)) {
                return;
            }
            grid = candidate;
        }
        targetX = x;
        targetZ = z;
        callerId = caller.getUUID();
        missionTick = 0;
        roundsSent = 0;
        SynchedEntityData data = vehicle.getEntityData();
        data.set(keys.phase(), Phase.WARMUP.ordinal());
        data.set(keys.elapsed(), 0);
        data.set(keys.rounds(), 0);
        data.set(keys.targetX(), (float) x);
        data.set(keys.targetZ(), (float) z);
        Phl191Effects.sound(server, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                ModSounds.RADAR_PING.get(), 1.25F, 1.2F);
        Phl191Effects.sound(server, x, groundAt(server, x, z) + 1.0D, z,
                ModSounds.RADAR_PING.get(), 0.8F, 0.9F);
    }

    private static boolean gridInsideBorder(RocketStrikeGrid grid, WorldBorder border,
                                            double x, double z) {
        int last = RocketStrikeGrid.SIZE - 1;
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                Offset corner = grid.pointOffset(row * last, column * last);
                if (!border.isWithinBounds(x + corner.dx(), z + corner.dz())) {
                    return false;
                }
            }
        }
        return true;
    }

    public void tick(ServerLevel server) {
        SynchedEntityData data = vehicle.getEntityData();
        if (cooldown > 0) {
            data.set(keys.cooldown(), --cooldown);
        }
        if (missionTick < 0) {
            return;
        }

        int warmup = LittleBirdConfig.artilleryStrikeWarmupSeconds();
        int duration = durationSeconds();
        int total = roundsTotal();
        Phase phase = ArtilleryStrikeRules.phaseAt(missionTick, warmup, duration);
        if (phase == Phase.WARMUP && missionTick % 5 == 0) {
            markTarget(server);
        }
        int due = ArtilleryStrikeRules.shellsDue(missionTick, warmup, duration, total);
        while (roundsSent < due) {
            sendRound(server, roundsSent, total);
            roundsSent++;
        }
        data.set(keys.rounds(), roundsSent);

        if (phase == Phase.DONE) {
            clearMission();
            cooldown = configuredCooldown();
            data.set(keys.phase(), Phase.IDLE.ordinal());
            data.set(keys.elapsed(), 0);
            data.set(keys.cooldown(), cooldown);
        } else {
            data.set(keys.phase(), phase.ordinal());
            data.set(keys.elapsed(), missionTick++);
        }
    }

    private void markTarget(ServerLevel server) {
        double ground = groundAt(server, targetX, targetZ);
        Phl191Effects.particles(server, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, targetX,
                ground + 1.0D, targetZ, 2, 0.4D, 55.0D, 0.4D, 0.01D);
        Phl191Effects.particles(server, ParticleTypes.FLAME, targetX,
                ground + 0.2D, targetZ, 2, 0.3D, 0.1D, 0.3D, 0.01D);
    }

    private void sendRound(ServerLevel server, int index, int total) {
        Offset offset = isRocketGrid() ? grid.roundOffset(index)
                : ArtilleryStrikeRules.scatterOffset(index, total,
                        LittleBirdConfig.artilleryStrikeSpreadRadius());
        double x = targetX + offset.dx();
        double z = targetZ + offset.dz();
        Entity owner = server.getPlayerByUUID(callerId);
        double dropHeight = LittleBirdConfig.artilleryStrikeDropHeight();
        if (isRocketGrid()) {
            ChunkPos chunk = new ChunkPos(BlockPos.containing(x, 0.0D, z));
            server.getChunkSource().addRegionTicket(ROCKET_STRIKE_TICKET, chunk, 2, chunk);
            Phl191RocketEntity rocket = new Phl191RocketEntity(ModEntities.PHL191_ROCKET.get(), server);
            rocket.dropTo(x, z, owner, dropHeight);
            server.addFreshEntity(rocket);
        } else {
            Plz05ShellEntity shell = new Plz05ShellEntity(ModEntities.PLZ05_SHELL.get(), server);
            shell.dropTo(x, z, owner, dropHeight);
            server.addFreshEntity(shell);
        }
    }

    private static double groundAt(ServerLevel server, double x, double z) {
        return server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(x), Mth.floor(z));
    }

    public boolean isRocketGrid() {
        return kind == Kind.PHL191;
    }

    public double rangeBlocks() {
        return isRocketGrid() ? PHL191_RANGE : PLZ05_RANGE;
    }

    public int durationSeconds() {
        return isRocketGrid() ? RocketStrikeGrid.DURATION_SECONDS
                : LittleBirdConfig.artilleryStrikeDurationSeconds();
    }

    public int roundsTotal() {
        return isRocketGrid() ? RocketStrikeGrid.TOTAL_ROUNDS
                : ArtilleryStrikeRules.clampShells(LittleBirdConfig.artilleryStrikeShells());
    }

    public Phase phase() {
        return PHASES[vehicle.getEntityData().get(keys.phase())];
    }

    public boolean active() {
        return phase() != Phase.IDLE;
    }

    public boolean ready() {
        return LittleBirdConfig.artilleryStrikeEnabled() && !active() && cooldownTicks() <= 0;
    }

    public int cooldownTicks() {
        return vehicle.getEntityData().get(keys.cooldown());
    }

    public int elapsedTicks() {
        return vehicle.getEntityData().get(keys.elapsed());
    }

    public int roundsFired() {
        return vehicle.getEntityData().get(keys.rounds());
    }

    public double targetX() {
        return vehicle.getEntityData().get(keys.targetX());
    }

    public double targetZ() {
        return vehicle.getEntityData().get(keys.targetZ());
    }

    public void read(CompoundTag tag) {
        cooldown = Math.max(0, tag.getInt("StrikeCooldown"));
        clearMission();
        SynchedEntityData data = vehicle.getEntityData();
        data.set(keys.phase(), Phase.IDLE.ordinal());
        data.set(keys.elapsed(), 0);
        data.set(keys.rounds(), 0);
        data.set(keys.cooldown(), cooldown);
    }

    public void write(CompoundTag tag) {
        // Pending rockets are canceled across a save, with a cooldown to prevent a repeat call.
        int savedCooldown = isRocketGrid() && missionTick >= 0 ? configuredCooldown() : cooldown;
        tag.putInt("StrikeCooldown", savedCooldown);
    }

    private static int configuredCooldown() {
        return ArtilleryStrikeRules.cooldownTicks(LittleBirdConfig.artilleryStrikeCooldownSeconds());
    }

    private void clearMission() {
        missionTick = -1;
        roundsSent = 0;
        callerId = null;
        grid = null;
    }
}
