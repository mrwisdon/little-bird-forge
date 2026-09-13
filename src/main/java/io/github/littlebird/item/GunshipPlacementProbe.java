package io.github.littlebird.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Runs the downward probe that {@link GunshipPlacement} turns into a spawn position: the airframe
 * lands standing on whatever is under the spot the player aimed at, instead of buried in it.
 */
public final class GunshipPlacementProbe {
    private GunshipPlacementProbe() {
    }

    /**
     * On the ground, distance blocks in front of the player. The offset is horizontal: using the
     * full look vector meant that looking straight down spawned the aircraft on top of the
     * player, and that looking down slightly shortened how far in front it appeared.
     */
    public static Vec3 spawnPosition(ServerLevel level, Player player, double distance) {
        Vec3 aim = player.position().add(
                Vec3.directionFromRotation(0.0F, player.getYRot()).scale(distance));
        BlockHitResult hit = level.clip(new ClipContext(
                new Vec3(aim.x, GunshipPlacement.probeFromY(player.getY()), aim.z),
                new Vec3(aim.x, GunshipPlacement.probeToY(player.getY()), aim.z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return new Vec3(aim.x,
                GunshipPlacement.baseY(hit.getType() != HitResult.Type.MISS,
                        hit.getLocation().y, player.getY()),
                aim.z);
    }
}
