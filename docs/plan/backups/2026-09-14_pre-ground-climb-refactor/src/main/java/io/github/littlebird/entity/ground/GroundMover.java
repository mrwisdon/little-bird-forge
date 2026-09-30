package io.github.littlebird.entity.ground;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Step assist for the pack's Entity based ground vehicles.
 *
 * <p>Minecraft only applies step height inside {@code LivingEntity}; a plain Entity that calls
 * {@code move} drives into every one block lip and stops. These hulls are not LivingEntity, so the
 * climb is done here: when a drive move is blocked while on the ground, the same horizontal move is
 * retried from raised positions, smallest first, then the hull settles back onto the new surface.
 * A retry that is still blocked is reverted, so a wall taller than the step height stops the
 * vehicle exactly as before.
 */
public final class GroundMover {
    private GroundMover() {
    }

    /**
     * Call right after the drive move(). {@code requested} is the movement the drive asked for;
     * move() clips its own delta against the blocking lip, so the caller must pass the request
     * rather than re-reading getDeltaMovement().
     *
     * <p>The climb is spread over ticks: the hull rises at most the current horizontal speed (so
     * it can still keep up with a 45 degree staircase) and at least half a block per tick. A one
     * block lip therefore takes two ticks instead of snapping a whole block in one, which is what
     * made the body hop.
     */
    public static void stepUp(Entity vehicle, Vec3 requested, double stepHeightBlocks) {
        if (!vehicle.horizontalCollision || !vehicle.onGround()) {
            return;
        }
        double[] rises = GroundDrive.stepRises(stepHeightBlocks);
        if (rises.length == 0 || requested.horizontalDistanceSqr() <= 0.0D) {
            return;
        }
        // Only climb while driving forward. In reverse, or while a too steep grade is sliding the
        // hull back, the step assist must not fight the drive and haul it up anyway.
        double radians = Math.toRadians(vehicle.getYRot());
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        if (requested.dot(forward) <= 0.0D) {
            return;
        }
        Vec3 start = vehicle.position();
        Vec3 flat = new Vec3(requested.x, 0.0D, requested.z);
        // Smallest rise that lets the hull past the lip.
        double clear = -1.0D;
        for (double probe : rises) {
            vehicle.setPos(start.x, start.y + probe, start.z);
            vehicle.setDeltaMovement(flat);
            vehicle.move(MoverType.SELF, flat);
            boolean cleared = !vehicle.horizontalCollision;
            vehicle.setPos(start.x, start.y, start.z);
            if (cleared) {
                clear = probe;
                break;
            }
        }
        vehicle.setDeltaMovement(requested);
        if (clear < 0.0D) {
            return; // Too tall: leave the hull blocked.
        }
        double climbPerTick = Math.max(0.5D, requested.horizontalDistance());
        double rise = Math.min(clear, climbPerTick);
        // Rise with a real move, never a teleport. A teleport puts the hull inside the step and the
        // next drive tick's ground probe shoves it right back out, so the vehicle jammed on a small
        // lip, the drive speed fell to zero there, and steering then looked like a spin in place.
        // move() keeps the hull beside the block while it climbs.
        vehicle.setDeltaMovement(0.0D, rise, 0.0D);
        vehicle.move(MoverType.SELF, new Vec3(0.0D, rise, 0.0D));
        vehicle.setDeltaMovement(flat);
        vehicle.move(MoverType.SELF, flat);
        if (rise + 1.0E-9D < clear) {
            // Not high enough yet: keep the gained height and take the rest next tick.
            return;
        }
        if (!vehicle.horizontalCollision) {
            // Cleared the lip: settle onto the new surface.
            vehicle.move(MoverType.SELF, new Vec3(0.0D, -rise, 0.0D));
        }
    }
}
