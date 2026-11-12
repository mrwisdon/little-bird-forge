package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.weapon.ArtilleryStrikeRules;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.Plz05Ballistics;
import io.github.littlebird.entity.weapon.Phl191Effects;
import io.github.littlebird.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * One 155mm shell: a real flying round, not a hitscan.
 *
 * <p>Flight is {@link Plz05Ballistics} stepped on the server, which is exactly the code the
 * client's ground marker steps, so the marker and the impact cannot disagree. The trail is the
 * point of the round: a bright motor flame plus a long smoke column broadcast per player out to
 * 256 blocks, because the bulk particle packet stops at 32 and an artillery rocket is watched from
 * much further away than that.
 */
public class Plz05ShellEntity extends Entity implements TrackedRound {
    /** Blocks the trail is broadcast to. */
    private static final double PARTICLE_RANGE = 256.0D;
    /** How far behind the round the smoke column is laid, in blocks. */
    private static final double TRAIL_LENGTH = 2.2D;

    private Vec3 velocity = Vec3.ZERO;
    private Vec3 heading = new Vec3(0.0D, 0.0D, 1.0D);
    private UUID ownerUuid;
    private int ageTicks;
    /**
     * True for a round called in from off the map rather than fired out of the hull: it arrives
     * high above the target and falls, so it has no motor flame and leaves a smaller hole.
     */
    private boolean dropping;

    public Plz05ShellEntity(EntityType<? extends Plz05ShellEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
    }

    /** Points the round down the given direction and gives it the launch speed. */
    public void launch(Vec3 direction, Vec3 heading, Entity owner) {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        Vec3 unit = direction.normalize();
        this.heading = unit;
        Plz05Ballistics.V3 launch = Plz05Ballistics.launchVelocity(
                new Plz05Ballistics.V3(unit.x, unit.y, unit.z));
        this.velocity = new Vec3(launch.x(), launch.y(), launch.z());
        setYRot((float) Math.toDegrees(Math.atan2(-unit.x, unit.z)));
        setXRot((float) Math.toDegrees(-Math.asin(unit.y)));
        setDeltaMovement(velocity);
    }

    /**
     * Arrives a round from off the map onto a ground point: the fire mission does not fire these
     * out of the barrel, it drops them from high above the marked coordinate.
     *
     * <p>The spawn height is measured from the terrain heightmap, so the round always appears the
     * same distance above the ground it is aimed at, whatever the hill under it.
     */
    public void dropTo(double targetX, double targetZ, Entity owner, double dropHeight) {
        this.dropping = true;
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.ageTicks = 0;
        this.heading = new Vec3(0.0D, -1.0D, 0.0D);
        this.velocity = new Vec3(0.0D, -ArtilleryStrikeRules.DEFAULT_FALL_SPEED, 0.0D);
        double groundY = level().getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.util.Mth.floor(targetX), net.minecraft.util.Mth.floor(targetZ));
        setPos(targetX, groundY + ArtilleryStrikeRules.clampDropHeight(dropHeight), targetZ);
        setYRot(0.0F);
        setXRot(90.0F);
        setDeltaMovement(velocity);
    }

    /** True when this round was called in rather than fired. */
    public boolean isDropping() {
        return dropping;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ageTicks++;
        Plz05Ballistics.V3 stepped = Plz05Ballistics.advance(
                new Plz05Ballistics.V3(velocity.x, velocity.y, velocity.z),
                new Plz05Ballistics.V3(heading.x, heading.y, heading.z), ageTicks);
        velocity = new Vec3(stepped.x(), stepped.y(), stepped.z());
        setDeltaMovement(velocity);
        setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
        setXRot((float) Math.toDegrees(-Math.atan2(velocity.y,
                Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z))));

        Vec3 start = position();
        Vec3 end = start.add(velocity);
        EntityHitResult directHit = ProjectileUtil.getEntityHitResult(level(), this, start, end,
                getBoundingBox().expandTowards(velocity).inflate(1.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && candidate != this && candidate != owner()
                        && !(candidate instanceof Plz05ShellEntity));
        BlockHitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blockIsNearer = blockHit.getType() != HitResult.Type.MISS
                && (directHit == null || start.distanceToSqr(blockHit.getLocation())
                        <= start.distanceToSqr(directHit.getLocation()));
        if (blockIsNearer) {
            setPos(blockHit.getLocation());
            explode(null);
            return;
        }
        if (directHit != null) {
            setPos(directHit.getLocation());
            explode(directHit.getEntity());
            return;
        }
        move(MoverType.SELF, velocity);
        if (horizontalCollision || verticalCollision || onGround()
                || Plz05Ballistics.expired(ageTicks)) {
            explode(null);
            return;
        }
        spawnTrail();
        if (ageTicks % 8 == 0 && level() instanceof ServerLevel soundServer) {
            Phl191Effects.sound(soundServer, getX(), getY(), getZ(), ModSounds.PHL_FLY.get(),
                    0.95F + random.nextFloat() * 0.1F, 1.0F);
        }
    }

    /**
     * The motor flame and the smoke column, laid along the stretch flown since the last tick so the
     * trail is continuous instead of a dotted line. Sent to every player in range by hand: the bulk
     * packet's 32 block radius would cut the trail off a few hundred blocks short of the impact.
     */
    private void spawnTrail() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 tail = velocity.normalize().scale(-0.9D);
        Vec3 nozzle = position().add(tail);
        Vec3 back = velocity.normalize().scale(-TRAIL_LENGTH * 0.5D);
        Vec3 middle = nozzle.add(back);
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(getX(), getY(), getZ()) > PARTICLE_RANGE * PARTICLE_RANGE) {
                continue;
            }
            if (!dropping) {
                // A called-in round has no motor, only the wind trail of a falling shell.
                server.sendParticles(player, ParticleTypes.FLAME, true,
                        nozzle.x, nozzle.y, nozzle.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
            }
            server.sendParticles(player, ParticleTypes.SMOKE, true,
                    nozzle.x, nozzle.y, nozzle.z, 3, 0.06D, 0.06D, 0.06D, 0.01D);
            server.sendParticles(player, ParticleTypes.CLOUD, true,
                    nozzle.x, nozzle.y, nozzle.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
            server.sendParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    middle.x, middle.y, middle.z, 2, 0.12D, 0.12D, 0.12D, 0.01D);
            server.sendParticles(player, ParticleTypes.SMOKE, true,
                    middle.x, middle.y, middle.z, 6, 0.22D, 0.22D, 0.22D, 0.004D);
        }
    }

    /** Warhead: direct hit damage, then the blast. */
    private void explode(Entity directHit) {
        if (level() instanceof ServerLevel server) {
            Entity owner = owner();
            if (directHit != null && owner instanceof net.minecraft.world.entity.player.Player) {
                float damage = io.github.littlebird.entity.weapon.VehicleDamageTable.damage(
                        io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon.PLZ05_SHELL,
                        directHit);
                directHit.hurt(server.damageSources().playerAttack(
                        (net.minecraft.world.entity.player.Player) owner), damage);
            }
            // A called-in round carries the same 155mm warhead as one fired out of the barrel:
            // the same blast, the same particles, the same crater. The barrage has to read as
            // thirty real shells, not thirty small ones.
            int bursts = 5;
            int ringDots = 40;
            Phl191Effects.particles(server, ParticleTypes.EXPLOSION, getX(), getY(), getZ(),
                    40, 0.6D, 0.6D, 0.6D, 0.0D);
            Phl191Effects.particles(server, ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6D, getZ(),
                    30, 1.0D, 0.8D, 1.0D, 0.02D);
            Phl191Effects.particles(server, ParticleTypes.FLAME, getX(), getY() + 0.5D, getZ(), 20,
                    0.9D, 0.7D, 0.9D, 0.03D);
            for (int i = 0; i < bursts; i++) {
                Phl191Effects.particles(server, ParticleTypes.EXPLOSION_EMITTER,
                        getX() + (i - 1) * 0.8D, getY() + 0.4D, getZ() + (i - 1) * 0.8D, 1,
                        0.4D, 0.4D, 0.4D, 0.0D);
            }
            for (int i = 0; i < ringDots; i++) {
                double angle = i * Math.PI * 2.0D / ringDots;
                double ring = 5.5D;
                Phl191Effects.particles(server, ParticleTypes.CLOUD,
                        getX() + Math.cos(angle) * ring, getY() + 0.3D,
                        getZ() + Math.sin(angle) * ring, 1,
                        Math.cos(angle) * 0.35D, 0.25D, Math.sin(angle) * 0.35D, 0.05D);
            }
            Phl191Effects.particles(server, ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(),
                    getY() + 1.0D, getZ(), 24, 1.4D, 0.6D, 1.4D, 0.02D);
            Phl191Effects.sound(server, getX(), getY(), getZ(), ModSounds.PHL_IMPACT.get(), 1.0F,
                    1.0F);
            Phl191Effects.sound(server, getX(), getY(), getZ(), ModSounds.EXPLOSION.get(), 0.45F,
                    0.7F);
            io.github.littlebird.entity.weapon.WarheadSplash.apply(server, this, directHit,
                    new io.github.littlebird.entity.weapon.WarheadSplash.PlayerCredit(
                            owner() instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null),
                    io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon.PLZ05_SHELL,
                    position(), Plz05Ballistics.SPLASH_RADIUS, Plz05Ballistics.SPLASH_FRACTION);
            carveCrater(server);
        }
        discard();
    }

    /** The player who fired this round, if they are still online. */
    public Entity owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getPlayerByUUID(ownerUuid);
    }

    /**
     * Tears a two block bowl where the round lands. The vanilla explosion above only settles
     * entity damage (interaction NONE), so the hole is carved here: the crater radius stays a
     * separate knob from the kill radius, and tough material is left standing.
     */
    private void carveCrater(ServerLevel server) {
        if (!LittleBirdConfig.plz05BreaksBlocks()) {
            return;
        }
        double radius = dropping ? LittleBirdConfig.artilleryStrikeCraterRadius()
                : LittleBirdConfig.plz05CraterRadius();
        if (radius <= 0.0D) {
            return;
        }
        int reach = net.minecraft.util.Mth.ceil(radius);
        net.minecraft.core.BlockPos centre = blockPosition();
        int broken = 0;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (broken >= io.github.littlebird.entity.weapon.CraterShape.MAX_BLOCKS) {
                        return;
                    }
                    if (!io.github.littlebird.entity.weapon.CraterShape.isCracked(
                            dx, dy, dz, radius)) {
                        continue;
                    }
                    net.minecraft.core.BlockPos pos = centre.offset(dx, dy, dz);
                    if (!server.isLoaded(pos)) {
                        continue;
                    }
                    net.minecraft.world.level.block.state.BlockState state =
                            server.getBlockState(pos);
                    if (state.isAir()
                            || !io.github.littlebird.entity.weapon.CraterShape.isBreakable(
                                    state.getBlock().getExplosionResistance(),
                                    io.github.littlebird.entity.weapon.CraterShape.MAX_RESISTANCE)) {
                        continue;
                    }
                    server.setBlock(pos,
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    broken++;
                }
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        velocity = new Vec3(tag.getDouble("VX"), tag.getDouble("VY"), tag.getDouble("VZ"));
        heading = new Vec3(tag.getDouble("HX"), tag.getDouble("HY"), tag.getDouble("HZ"));
        ageTicks = tag.getInt("Age");
        dropping = tag.getBoolean("Dropping");
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("VX", velocity.x);
        tag.putDouble("VY", velocity.y);
        tag.putDouble("VZ", velocity.z);
        tag.putDouble("HX", heading.x);
        tag.putDouble("HY", heading.y);
        tag.putDouble("HZ", heading.z);
        tag.putInt("Age", ageTicks);
        tag.putBoolean("Dropping", dropping);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
