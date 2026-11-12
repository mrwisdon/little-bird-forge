package io.github.littlebird.entity;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.weapon.BurnZone;
import io.github.littlebird.entity.weapon.DamageType;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The burning ground an incendiary canister leaves behind, kept alive on the server.
 *
 * <p>A vanilla fire block on bare ground dies out within seconds, so the canister's
 * {@code incendiary_fire_ticks} used to describe how long *entities* burned while the ground went
 * dark - the fire read as a spark rather than as a weapon. This class holds one {@link BurnZone}
 * per impact: every two seconds it puts the fire back, and every second it charges whatever is
 * standing in it a pulse of damage, priced through the same armour rules as every other weapon.
 *
 * <p>The damage is ground fire, not a projectile: it is a slab around the impact point, so a
 * vehicle or a player standing in the burn is hurt while an aircraft passing overhead is not.
 * Rain puts the zone's fire out and stops its damage, and a fire-immune target is never charged.
 *
 * <p>Server side only, one zone per canister. Damage pulses are credited to no one - burning to
 * death is a fire kill, exactly as it is in vanilla; the bomb's kill feed still covers the blast.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IncendiaryBurnZones {

    /** Hard cap on the fire blocks one zone may hold, so a wide fire cannot lag the server. */
    private static final int MAX_FIRE_BLOCKS = 512;

    /** How far above and below the impact the burn can reach: a slab, not a whole column. */
    private static final double HURT_SLAB = 3.0D;

    private static final List<Zone> ZONES = new ArrayList<>();

    private IncendiaryBurnZones() {
    }

    private record Zone(ResourceKey<Level> dimension, double x, double y, double z, double radius,
                        BurnZone clock) {
    }

    /** Lights the ground now and keeps it alight for {@code lifetimeTicks}. */
    public static void light(ServerLevel level, double x, double y, double z, double radius,
                             int lifetimeTicks) {
        if (lifetimeTicks <= 0 || radius <= 0.0D) {
            return;
        }
        lightGround(level, BlockPos.containing(x, y, z), radius);
        ZONES.add(new Zone(level.dimension(), x, y, z, radius, new BurnZone(lifetimeTicks)));
    }

    /**
     * Lights every column of the disc whose surface can hold a fire: open air above a solid block,
     * and no rain. Water, ice, drops and covered columns are skipped - a fire in a lake reads as a
     * bug rather than as a weapon - and the block cap stops a very wide fire from costing more than
     * it is worth.
     */
    static void lightGround(ServerLevel server, BlockPos centre, double radius) {
        int reach = Mth.ceil(radius);
        int placed = 0;
        for (int dx = -reach; dx <= reach && placed < MAX_FIRE_BLOCKS; dx++) {
            for (int dz = -reach; dz <= reach && placed < MAX_FIRE_BLOCKS; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos surface = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                        centre.offset(dx, 0, dz));
                if (!server.isEmptyBlock(surface)) {
                    continue;
                }
                BlockPos below = surface.below();
                if (!server.getBlockState(below).isSolidRender(server, below)) {
                    continue;
                }
                if (server.isRainingAt(surface)) {
                    continue;
                }
                server.setBlockAndUpdate(surface, BaseFireBlock.getState(server, surface));
                placed++;
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ZONES.isEmpty()
                || !(event.level instanceof ServerLevel server)) {
            return;
        }
        BlockPos centre = null;
        Iterator<Zone> zones = ZONES.iterator();
        while (zones.hasNext()) {
            Zone zone = zones.next();
            if (server.getServer().getLevel(zone.dimension()) == null) {
                // The level is gone: the fire went with it.
                zones.remove();
                continue;
            }
            if (!zone.dimension().equals(server.dimension())) {
                continue;
            }
            centre = BlockPos.containing(zone.x(), zone.y(), zone.z());
            BurnZone.Step step = zone.clock().advance();
            if (step == BurnZone.Step.RELIGHT || step == BurnZone.Step.DAMAGE_AND_RELIGHT) {
                lightGround(server, centre, zone.radius());
            }
            if (step == BurnZone.Step.DAMAGE || step == BurnZone.Step.DAMAGE_AND_RELIGHT) {
                hurt(server, zone, centre);
            }
            if (zone.clock().expired()) {
                zones.remove();
            }
        }
    }

    /** One pulse of the burn: everything standing in the slab takes the configured per-second hit. */
    private static void hurt(ServerLevel server, Zone zone, BlockPos centre) {
        float damage = (float) LittleBirdConfig.incendiaryBurnDamagePerSecond();
        if (damage <= 0.0F || server.isRainingAt(centre)) {
            return;
        }
        double radius = zone.radius();
        AABB area = new AABB(zone.x() - radius, zone.y() - HURT_SLAB, zone.z() - radius,
                zone.x() + radius, zone.y() + HURT_SLAB, zone.z() + radius);
        for (Entity entity : server.getEntities((Entity) null, area)) {
            if (entity.fireImmune()) {
                continue;
            }
            float priced = LittleBirdBallistics.damageTo(DamageType.SHELL,
                    AirframeTargets.armorClass(entity),
                    LittleBirdBallistics.WeaponDamage.incendiary(damage));
            if (priced > 0.0F) {
                entity.hurt(server.damageSources().onFire(), priced);
            }
        }
    }

    /** Zones alive right now. Read-only, and server-side only. */
    public static int activeZones() {
        return ZONES.size();
    }
}
