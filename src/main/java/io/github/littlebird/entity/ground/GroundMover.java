package io.github.littlebird.entity.ground;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ground contact and step assist for the pack's Entity based ground vehicles.
 *
 * <p>Minecraft only applies step height inside {@code LivingEntity}; a plain Entity that calls
 * {@code move} drives into every one block lip and stops. These hulls are not LivingEntity, so the
 * climb is done here. Nothing in this class teleports the hull: every position change is a real,
 * collision-clipped {@code move}, so a tick can never start from a penetrated or airborne state.
 */
public final class GroundMover {
    /** Extra clearance above the steepest reachable corner ground before the ray starts. */
    private static final double PROBE_UP = 1.5D;
    /** ...and reaches this far below them before it is called a miss. */
    private static final double PROBE_DOWN = 4.0D;

    private GroundMover() {
    }

    /**
     * Samples the ground under the four corners of a box centred on the hull and fits a contact
     * plane to it. Callers pass probes inside the hull's own footprint, so the sample is the
     * surface the hull is standing on, not ground it has not reached. A corner with no ground
     * under it inside the probe reach is a miss and the whole sample is invalid, which the
     * {@link HullPose} smoother reads as "level out".
     */
    public static GroundContact.Sample sampleSurface(
            Entity vehicle, double halfLengthBlocks, double halfWidthBlocks) {
        if (vehicle == null || vehicle.level() == null
                || !(halfLengthBlocks > 0.0D) || !(halfWidthBlocks > 0.0D)) {
            return GroundContact.Sample.LEVEL;
        }
        double yaw = Math.toRadians(vehicle.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = vehicle.getX();
        double z = vehicle.getZ();
        // Start each ray above the highest ground a corner could stand on at the steepest grade
        // we accept. A ray started lower would begin inside the slope on a climb and read the
        // slope's own body instead of its surface.
        double reach = halfLengthBlocks * Math.tan(Math.toRadians(GroundContact.MAX_GRADE_DEGREES));
        double top = vehicle.getY() + reach + PROBE_UP;
        double bottom = vehicle.getY() - PROBE_DOWN;
        double frontLeft = groundHeight(vehicle, x, z, halfLengthBlocks, -halfWidthBlocks,
                sin, cos, top, bottom);
        double frontRight = groundHeight(vehicle, x, z, halfLengthBlocks, halfWidthBlocks,
                sin, cos, top, bottom);
        double backLeft = groundHeight(vehicle, x, z, -halfLengthBlocks, -halfWidthBlocks,
                sin, cos, top, bottom);
        double backRight = groundHeight(vehicle, x, z, -halfLengthBlocks, halfWidthBlocks,
                sin, cos, top, bottom);
        return GroundContact.fit(frontLeft, frontRight, backLeft, backRight,
                halfLengthBlocks, halfWidthBlocks);
    }

    /**
     * One downward block ray under a hull-local point. Yaw 0 faces +Z, so a local (forward,
     * lateral) offset is forward along (-sin, cos) plus lateral along (-cos, -sin), the same
     * frame the seat offsets use. A miss comes back as NaN.
     */
    private static double groundHeight(Entity vehicle, double x, double z, double forward,
            double lateral, double sin, double cos, double top, double bottom) {
        double px = x + forward * -sin + lateral * -cos;
        double pz = z + forward * cos + lateral * -sin;
        BlockHitResult hit = vehicle.level().clip(new ClipContext(
                new Vec3(px, top, pz), new Vec3(px, bottom, pz),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
        return hit.getType() == HitResult.Type.MISS ? Double.NaN : hit.getLocation().y;
    }

    /**
     * Call right after the drive move(). {@code requested} is the movement the drive asked for;
     * move() clips its own delta against the blocking lip, so the caller must pass the request
     * rather than re-reading getDeltaMovement().
     *
     * <p>The smallest rung of the step ladder that clears the lip is lifted over, stepped across
     * and settled back down, all with real collision-clipped moves. If even the top rung is
     * blocked the hull is left exactly where the drive put it, so a wall taller than the step
     * height stops the vehicle as before.
     */
    public static void stepUp(Entity vehicle, Vec3 requested, double stepHeightBlocks) {
        if (!vehicle.horizontalCollision || !vehicle.onGround()) {
            return;
        }
        if (!(stepHeightBlocks > 0.0D) || requested.horizontalDistanceSqr() <= 0.0D) {
            return;
        }
        // Only climb while driving forward. In reverse, or while a too steep grade is sliding the
        // hull back, the step assist must not fight the drive and haul it up anyway.
        double radians = Math.toRadians(vehicle.getYRot());
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        if (requested.dot(forward) <= 0.0D) {
            return;
        }
        Vec3 flat = new Vec3(requested.x, 0.0D, requested.z);
        double clear = clearance(vehicle, flat, stepHeightBlocks);
        if (clear < 0.0D) {
            return; // Too tall: leave the hull blocked.
        }
        // Real moves only. Lift clear of the lip, step across, then settle straight back down so
        // the hull is grounded again. Collision clips each move, so the hull never ends up inside
        // the step the way a teleport would, and it is never left hovering mid-climb.
        vehicle.setDeltaMovement(0.0D, clear, 0.0D);
        vehicle.move(MoverType.SELF, new Vec3(0.0D, clear, 0.0D));
        vehicle.setDeltaMovement(flat);
        vehicle.move(MoverType.SELF, flat);
        vehicle.setDeltaMovement(flat);
        vehicle.move(MoverType.SELF, new Vec3(0.0D, -clear, 0.0D));
    }

    /** How far ahead the clearance probe reaches: just enough to feel the lip the hull is on. */
    private static final double PROBE_FORWARD = 0.1D;

    /**
     * The smallest rung of the step ladder whose lifted bounding box clears the lip directly
     * ahead, or -1 when even the top rung is blocked. Purely reads the world: the hull is never
     * touched, so probing cannot leave collision or ground state behind.
     *
     * <p>The probe only reaches {@link #PROBE_FORWARD} ahead, not the whole requested step. Using
     * the whole step made this speed dependent: the faster the hull, the further the lifted box
     * reached and the more likely it already overlapped the next tread up, so every rung looked
     * blocked and the hull simply stopped. The real step across is a collision-clipped move, so
     * the clearance test only has to feel the immediate lip.
     */
    private static double clearance(Entity vehicle, Vec3 flat, double stepHeightBlocks) {
        AABB box = vehicle.getBoundingBox();
        double length = flat.length();
        Vec3 probe = length <= 1.0E-9D ? Vec3.ZERO
                : flat.scale(Math.min(1.0D, PROBE_FORWARD / length));
        for (double rise : GroundDrive.stepRises(stepHeightBlocks)) {
            AABB target = box.move(0.0D, rise, 0.0D).move(probe.x, 0.0D, probe.z);
            if (!vehicle.level().getBlockCollisions(vehicle, target).iterator().hasNext()) {
                return rise;
            }
        }
        return -1.0D;
    }
}
