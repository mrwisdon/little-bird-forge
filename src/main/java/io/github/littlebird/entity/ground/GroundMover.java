package io.github.littlebird.entity.ground;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Ground contact and collision-aware movement for ground vehicles. */
public final class GroundMover {
    /** Extra clearance above the steepest reachable corner ground before the ray starts. */
    private static final double PROBE_UP = 1.5D;
    /** ...and reaches this far below them before it is called a miss. */
    private static final double PROBE_DOWN = 4.0D;

    private GroundMover() {
    }

    /**
     * Moves the hull once, with step assist enabled for forward travel. Forge's Entity collision
     * solver handles ground contact, clearance and settling with the original movement request.
     * The step height is scoped to this move. The resolved rise feeds the next drive tick.
     */
    public static void moveDrive(
            Entity vehicle, Vec3 requested, double stepHeightBlocks, SlopeTracker slopeTracker) {
        double startY = vehicle.getY();
        float previousStepHeight = vehicle.maxUpStep();
        vehicle.setDeltaMovement(requested);
        vehicle.setMaxUpStep(driveStepHeight(requested, vehicle.getYRot(), stepHeightBlocks));
        try {
            vehicle.move(MoverType.SELF, requested);
        } finally {
            vehicle.setMaxUpStep(previousStepHeight);
        }
        boolean grounded = vehicle.onGround();
        slopeTracker.update(grounded ? vehicle.getY() - startY : 0.0D,
                grounded ? requested.horizontalDistance() : 0.0D);
    }

    static float driveStepHeight(Vec3 requested, float yawDegrees, double stepHeightBlocks) {
        double yaw = Math.toRadians(yawDegrees);
        double forwardDistance = requested.z * Math.cos(yaw) - requested.x * Math.sin(yaw);
        float stepHeight = (float) stepHeightBlocks;
        return forwardDistance > 0.0D && Float.isFinite(stepHeight) && stepHeight > 0.0F
                ? stepHeight : 0.0F;
    }

    /** Samples the terrain around the model's current running gear once per tick. */
    public static void updateSuspension(Entity vehicle, GroundSuspension suspension) {
        AABB box = vehicle.getBoundingBox();
        AABB support = new AABB(box.minX, box.minY - 0.08D, box.minZ,
                box.maxX, box.minY, box.maxZ);
        if (!vehicle.onGround()
                && !vehicle.level().getBlockCollisions(vehicle, support).iterator().hasNext()) {
            suspension.update(null);
            return;
        }

        double yaw = Math.toRadians(vehicle.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = vehicle.getX();
        double y = vehicle.getY();
        double z = vehicle.getZ();
        GroundSuspension.Pose pose = suspension.pose(1.0F);
        double pitch = Math.toRadians(pose.pitchDegrees());
        double along = Math.tan(pitch);
        double across = Math.tan(Math.toRadians(pose.rollDegrees())) / Math.cos(pitch);
        suspension.update((forward, lateral) -> {
            double px = x - forward * sin - lateral * cos;
            double pz = z + forward * cos - lateral * sin;
            double expected = y + pose.liftBlocks() + forward * along - lateral * across;
            if (!vehicle.level().hasChunkAt(BlockPos.containing(px, expected, pz))) {
                return Double.NaN;
            }
            return groundHeight(vehicle, px, pz, expected + 1.45D, expected - 1.35D) - y;
        });
    }

    /**
     * World-space displacement of a centreline mount as the hull tilts. The weapon can
     * retain its world bearing and elevation by counter-rotating about the displaced mount.
     */
    public static Vec3 suspensionMountOffset(GroundSuspension.Pose pose, float yaw,
                                              double pivotForwardBlocks, double pivotHeightBlocks) {
        double pitch = Math.toRadians(pose.pitchDegrees());
        double roll = Math.toRadians(pose.rollDegrees());
        double yawRadians = Math.toRadians(180.0D - yaw);
        // Match the model's Y yaw, X pitch, then negative Z roll, in matrix order.
        double lateral = pivotHeightBlocks * Math.sin(roll);
        double rolledHeight = pivotHeightBlocks * Math.cos(roll);
        double vertical = rolledHeight * Math.cos(pitch)
                + pivotForwardBlocks * Math.sin(pitch) + pose.liftBlocks() - pivotHeightBlocks;
        double depth = rolledHeight * Math.sin(pitch)
                + pivotForwardBlocks * (1.0D - Math.cos(pitch));
        double yawSin = Math.sin(yawRadians);
        double yawCos = Math.cos(yawRadians);
        return new Vec3(lateral * yawCos + depth * yawSin, vertical,
                depth * yawCos - lateral * yawSin);
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
        return groundHeight(vehicle, px, pz, top, bottom);
    }

    private static double groundHeight(Entity vehicle, double px, double pz, double top, double bottom) {
        BlockHitResult hit = vehicle.level().clip(new ClipContext(
                new Vec3(px, top, pz), new Vec3(px, bottom, pz),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
        return hit.getType() == HitResult.Type.MISS ? Double.NaN : hit.getLocation().y;
    }
}
