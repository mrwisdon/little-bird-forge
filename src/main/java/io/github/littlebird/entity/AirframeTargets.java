package io.github.littlebird.entity;

import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import net.minecraft.world.entity.Entity;

/**
 * One place that decides which class of airframe a hit landed on.
 *
 * Every weapon needs the same answer - the Little Bird's gun, its rockets, the Apache's chain
 * gun and its TOW all price a hull differently from a mob - and while each weapon kept its own
 * copy of the check, an Apache shot by a Little Bird was billed as an unarmoured target.
 */
public final class AirframeTargets {
    private AirframeTargets() {
    }

    /** The airframe class of a hit target, or {@code NONE} for anything that is not an aircraft. */
    public static LittleBirdBallistics.Airframe classify(Entity entity) {
        if (entity instanceof JetEntity) {
            return LittleBirdBallistics.Airframe.FIGHTER_JET;
        }
        if (entity instanceof ApacheEntity) {
            return LittleBirdBallistics.Airframe.APACHE;
        }
        if (entity instanceof LittleBirdEntity) {
            return LittleBirdBallistics.Airframe.LITTLE_BIRD;
        }
        return LittleBirdBallistics.Airframe.NONE;
    }

    /**
     * What the hit target is made of. The single answer every weapon prices against.
     *
     * <p>Tracked hulls are heavy armour - the M1A5 and the Bradley - and nothing short of a shell
     * touches them. The wheeled IFV is light armour: rifle fire is wasted on it, cannon is not.
     * Aircraft are their own class precisely so that bullets keep working against them.
     */
    public static io.github.littlebird.entity.weapon.ArmorClass armorClass(Entity entity) {
        if (isAirframe(entity)) {
            return io.github.littlebird.entity.weapon.ArmorClass.AIRFRAME;
        }
        // BF2042 puts the wheeled EBAA Wildcat in the same armour class as the tracked hulls, so
        // all three of the pack's ground vehicles are heavy armour. LIGHT_ARMOR stays in the enum
        // for the light vehicles the expansion plan adds (LATV4, LCAA, M5C Bolte).
        if (entity instanceof MainBattleTankEntity
                || entity instanceof InfantryFightingVehicleEntity
                || entity instanceof BradleyEntity) {
            return io.github.littlebird.entity.weapon.ArmorClass.HEAVY_ARMOR;
        }
        return io.github.littlebird.entity.weapon.ArmorClass.UNARMORED;
    }

    /** True for a hull the air-to-ground missile may lock: armour and IFVs, not aircraft. */
    public static boolean isGroundVehicle(Entity entity) {
        return entity instanceof MainBattleTankEntity
                || entity instanceof InfantryFightingVehicleEntity
                || entity instanceof BradleyEntity;
    }

    /** True for anything the dogfight prices fly against. */
    public static boolean isAirframe(Entity entity) {
        return classify(entity) != LittleBirdBallistics.Airframe.NONE;
    }

    /**
     * The player to credit for a projectile: its owner when that is a player, otherwise whoever is
     * flying the owner. A B-2 launches its own bombs, so without this the crew would never be told
     * anything about their own hits.
     */
    public static net.minecraft.server.level.ServerPlayer creditPlayer(Entity owner) {
        if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
            return player;
        }
        if (owner != null
                && owner.getControllingPassenger()
                        instanceof net.minecraft.server.level.ServerPlayer pilot) {
            return pilot;
        }
        return null;
    }

    /** True for entities a kill feed should name: mobs and armour, not items or experience. */
    public static boolean isKillFeedTarget(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        return entity instanceof net.minecraft.world.entity.LivingEntity
                || isAirframe(entity)
                || armorClass(entity) == io.github.littlebird.entity.weapon.ArmorClass.HEAVY_ARMOR;
    }

    /** Short display name for the feed. */
    public static String feedName(Entity entity) {
        String name = entity.getDisplayName().getString();
        if (name.length() > 48) {
            name = name.substring(0, 48);
        }
        return name;
    }

    /**
     * Names of everything the blast killed: the direct hit plus anything in {@code around} that is
     * no longer alive. Called after the explosion, so the dead can be read off directly.
     */
    public static java.util.List<String> kills(Entity owner, Entity directHit,
                                               java.util.List<Entity> around) {
        java.util.List<String> killed = new java.util.ArrayList<>();
        if (directHit != null && !directHit.isAlive() && directHit != owner) {
            killed.add(feedName(directHit));
        }
        if (around != null) {
            for (Entity entity : around) {
                if (entity == null || entity == owner || entity == directHit) {
                    continue;
                }
                if (!entity.isAlive() && isKillFeedTarget(entity)) {
                    killed.add(feedName(entity));
                }
            }
        }
        return killed;
    }
}
