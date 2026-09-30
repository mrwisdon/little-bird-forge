package io.github.littlebird.client;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.B2Entity;
import io.github.littlebird.network.B2PodDesignatePacket;
import io.github.littlebird.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The bombing electro-optical pod: a gimballed sight bolted under the B-2's nose.
 *
 * <p>While it is up the mouse drives the gimbal instead of the airframe, the camera sits at the
 * pod station and chases the gimbal (the residual gap is what moves the reticle), and the zoom is
 * exponential between the configured limits. Everything numeric lives in {@link B2PodMath}.
 */
public final class B2PodView {
    /** Lock window: the reticle must be this close to the camera axis before designating. */
    public static final double LOCK_WINDOW_DEG = 6.0D;
    private static final double ZOOM_STEP = 0.08D;
    private static final double MAX_RANGE_BLOCKS = 512.0D;

    private static boolean active;
    private static B2PodMath.Gimbal gimbal = new B2PodMath.Gimbal(0.0D, 0.0D);
    private static B2PodMath.Gimbal camera = new B2PodMath.Gimbal(0.0D, 0.0D);
    private static double zoom = 0.30D;
    private static double[] designation;

    /** True while the airframe is still banking out of a pod run's turn. */
    private static boolean levelWarning;

    private B2PodView() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
        if (value) {
            // Raise the pod looking slightly down: the ground a bomb run cares about is below the
            // nose, so the first sight picture already shows it.
            gimbal = new B2PodMath.Gimbal(0.0D, 12.0D);
            camera = new B2PodMath.Gimbal(0.0D, 12.0D);
        }
    }

    public static double gimbalYaw() {
        return gimbal.yawDeg();
    }

    public static double gimbalPitch() {
        return gimbal.pitchDeg();
    }

    public static double zoomFov() {
        return B2PodMath.sightFov(zoom, LittleBirdConfig.podZoomTightFov(),
                LittleBirdConfig.podZoomWideFov());
    }

    public static double zoomFraction() {
        return zoom;
    }

    public static void setLevelWarning(boolean value) {
        levelWarning = value;
    }

    public static boolean levelWarning() {
        return levelWarning;
    }

    public static double[] designation() {
        return designation == null ? null : designation.clone();
    }

    /** The sight's vertical travel: {up, down} in positive degrees, floored so it can see ground. */
    public static double[] sightWindow() {
        return B2PodMath.pitchWindow(LittleBirdConfig.podSightUpDeg(),
                LittleBirdConfig.podSightDownDeg());
    }

    public static void addMouseDelta(double deltaYawDeg, double deltaPitchDeg) {
        double[] travel = B2PodMath.mouseToGimbal(deltaYawDeg, deltaPitchDeg);
        double[] window = sightWindow();
        // Minecraft pitch and gimbal pitch are both positive downwards, so the window is
        // [-up, +down] and the mouse needs no sign flip.
        gimbal = B2PodMath.drag(gimbal, travel[0], travel[1],
                LittleBirdConfig.podGimbalYawLimit(), -window[0], window[1]);
    }

    public static void addZoom(double scrollDelta) {
        zoom = B2PodMath.clamp(zoom - scrollDelta * ZOOM_STEP, 0.0D, 1.0D);
    }

    /** Chases the gimbal with the camera; call once per client tick. */
    public static void tick() {
        camera = B2PodMath.chase(camera, gimbal, B2PodMath.CAMERA_FOLLOW);
    }

    /** World camera yaw for a given airframe heading. */
    public static float cameraYawDeg(B2Entity b2) {
        return (float) B2PodMath.wrap(b2.getYRot() + camera.yawDeg());
    }

    /** World camera pitch for a given airframe pitch. */
    public static float cameraPitchDeg(B2Entity b2) {
        return (float) B2PodMath.clamp(b2.getXRot() + camera.pitchDeg(), -90.0D, 90.0D);
    }

    /** World position of the pod lens, in blocks. */
    public static double[] cameraPosition(B2Entity b2) {
        double ahead = B2CockpitMath.podStationAheadBlocks();
        double height = B2CockpitMath.podStationHeightBlocks();
        double yaw = Math.toRadians(b2.getYRot());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        return new double[]{b2.getX() + forwardX * ahead, b2.getY() + height,
                b2.getZ() + forwardZ * ahead};
    }

    /** Where the reticle meets the world: the first block the sight line hits. */
    public static double[] sightPoint() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof B2Entity b2)) {
            return null;
        }
        if (minecraft.level == null) {
            return null;
        }
        double[] origin = cameraPosition(b2);
        double[] dir = B2PodMath.lookVector(cameraYawDeg(b2), cameraPitchDeg(b2));
        Vec3 from = new Vec3(origin[0], origin[1], origin[2]);
        Vec3 to = from.add(dir[0] * MAX_RANGE_BLOCKS, dir[1] * MAX_RANGE_BLOCKS,
                dir[2] * MAX_RANGE_BLOCKS);
        BlockHitResult hit = minecraft.level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, b2));
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            return new double[]{pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D};
        }
        return null;
    }

    /** Takes (or refreshes) a designation at the reticle; returns true when one was taken. */
    public static boolean designate() {
        double[] point = sightPoint();
        if (point == null) {
            return false;
        }
        designation = point;
        ModNetwork.CHANNEL.sendToServer(
                new B2PodDesignatePacket(point[0], point[1], point[2], false));
        return true;
    }

    /** Clears the designation on the server and locally. */
    public static void clearDesignation() {
        designation = null;
        resetMissHistory();
        ModNetwork.CHANNEL.sendToServer(new B2PodDesignatePacket(0.0D, 0.0D, 0.0D, true));
    }

    /** The store the bay would release right now. */
    public static io.github.littlebird.entity.weapon.BombKind selectedKind(B2Entity b2) {
        return b2.getBombWeapon() == B2Entity.WEAPON_INCENDIARY
                ? io.github.littlebird.entity.weapon.BombKind.INCENDIARY
                : io.github.littlebird.entity.weapon.BombKind.JDAM;
    }

    /** Ground height at a column, falling back to the aeroplane's own level. */
    private static double groundAt(Minecraft minecraft, double x, double z, double fallback) {
        if (minecraft.level == null) {
            return fallback;
        }
        return minecraft.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types
                .MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
    }

    /** The predicted impact as a world point {x, groundY, z}, for drawing and ranging. */
    public static double[] impactPoint3() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof B2Entity b2)) {
            return null;
        }
        double[] impact = impactPoint();
        if (impact == null) {
            return null;
        }
        return new double[]{impact[0], groundAt(minecraft, impact[0], impact[1], b2.getY()),
                impact[1]};
    }

    /** Ground height under the aeroplane, which is what a release cue is measured against. */
    private static double groundLevel(Minecraft minecraft, B2Entity b2) {
        if (minecraft.level == null) {
            return b2.getY();
        }
        return minecraft.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types
                .MOTION_BLOCKING, (int) Math.floor(b2.getX()), (int) Math.floor(b2.getZ()));
    }

    /**
     * Where a bomb released right now would land: {x, z, ticks}, or null when it never reaches the
     * ground within its budget. This is what turns bombing into "press the button when it says
     * READY" instead of mental ballistics.
     */
    public static double[] impactPoint() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof B2Entity b2)) {
            return null;
        }
        double[] release = releasePoint(b2);
        double[] velocity = releaseVelocityFor(b2);
        // The stick does not leave the bay on this tick: the order has to reach the server and the
        // server releases on its next tick. Fly the prediction from where the aeroplane will be
        // then, or the marker describes a drop that never happens and the bomb lands long.
        double[] launch = io.github.littlebird.entity.weapon.B2BombAim.leadReleasePosition(
                release[0], release[1], release[2], velocity[0], velocity[1], velocity[2],
                releaseLeadTicks());
        double[] first = io.github.littlebird.entity.weapon.B2BombAim.predictImpact(launch,
                velocity, selectedKind(b2), designation, designation != null,
                groundLevel(minecraft, b2));
        if (first == null) {
            return null;
        }
        // Second pass: the ground under the impact is what the round actually meets, which matters
        // when the run crosses a ridge.
        double underImpact = groundAt(minecraft, first[0], first[1], b2.getY());
        double[] again = io.github.littlebird.entity.weapon.B2BombAim.predictImpact(launch,
                velocity, selectedKind(b2), designation, designation != null, underImpact);
        return again == null ? first : again;
    }

    /** Where a stick actually leaves the aeroplane: the bay under the belly, not the cockpit. */
    public static double[] releasePoint(B2Entity b2) {
        return new double[]{b2.getX(),
                b2.getY() + io.github.littlebird.entity.B2Geometry.bellyClearanceBlocks(),
                b2.getZ()};
    }

    /** Ticks the release order needs to reach the server, from the link's own latency. */
    public static int releaseLeadTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        int pingMillis = 0;
        if (minecraft.player != null && minecraft.getConnection() != null) {
            net.minecraft.client.multiplayer.PlayerInfo info =
                    minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
            if (info != null) {
                pingMillis = info.getLatency();
            }
        }
        return io.github.littlebird.entity.weapon.B2BombAim.releaseLeadTicks(pingMillis);
    }

    /**
     * The velocity the server will give the stick. Read straight off the airframe's own filtered
     * history, so the sight cannot disagree with the bomb it is predicting.
     */
    public static double[] releaseVelocityFor(B2Entity b2) {
        return b2.pilotVelocity();
    }

    /** Ground distance from the aeroplane to the predicted impact, in blocks. */
    public static double impactDistance() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof B2Entity b2)) {
            return -1.0D;
        }
        double[] impact = impactPoint();
        if (impact == null) {
            return -1.0D;
        }
        double[] point = impactPoint3();
        return point == null ? -1.0D : B2PodMath.range(b2.getX(), b2.getY(), b2.getZ(), point);
    }

    /** How far the predicted impact is from the designation, in blocks. */
    public static double impactMiss() {
        if (designation == null) {
            return -1.0D;
        }
        return io.github.littlebird.entity.weapon.B2BombAim.missDistance(impactPoint(),
                designation[0], designation[2]);
    }

    /** True when pressing release now would put the round on the designation. */
    public static boolean impactReady() {
        if (designation == null) {
            return false;
        }
        return io.github.littlebird.entity.weapon.B2BombAim.releaseReady(impactPoint(),
                designation[0], designation[2],
                io.github.littlebird.entity.weapon.B2BombAim.RELEASE_TOLERANCE_BLOCKS);
    }

    /** Screen pixels of the predicted impact marker, or null when there is nothing to mark. */
    public static int[] impactMarkerOnScreen(int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof B2Entity b2)) {
            return null;
        }
        double[] impact = impactPoint3();
        if (impact == null) {
            return null;
        }
        double[] origin = cameraPosition(b2);
        double[] offset = B2PodMath.angularOffset(origin[0], origin[1], origin[2],
                cameraYawDeg(b2), cameraPitchDeg(b2), impact);
        if (offset == null) {
            return null;
        }
        return B2PodMath.projectToScreen(offset[0], offset[1], zoomFov(), width, height);
    }

    private static double lastMiss = Double.NaN;
    private static boolean missClosing;

    /**
     * Samples the prediction error once a tick, so a release press can tell "still closing" from
     * "already flown past the target".
     */
    public static void sampleMiss() {
        if (designation == null) {
            lastMiss = Double.NaN;
            missClosing = false;
            return;
        }
        double miss = impactMiss();
        missClosing = io.github.littlebird.entity.weapon.B2BombAim.shouldArm(miss, lastMiss,
                io.github.littlebird.entity.weapon.B2BombAim.RELEASE_TOLERANCE_BLOCKS);
        lastMiss = miss;
    }

    /** True when the predicted impact walked closer to the designation since the last sample. */
    public static boolean missClosing() {
        return missClosing;
    }

    /** The prediction error one sample ago, NaN when there is no history yet. */
    public static double lastMiss() {
        return lastMiss;
    }

    /** Whether the dedicated auto-release key may arm the trigger right now. */
    public static boolean canArmNow() {
        return io.github.littlebird.entity.weapon.B2BombAim.canArm(impactMiss(), lastMiss,
                io.github.littlebird.entity.weapon.B2BombAim.RELEASE_TOLERANCE_BLOCKS);
    }

    /** Armed when the crew pressed release too early: the sight fires it at the right moment. */
    private static boolean autoArmed;

    public static void armAutoRelease() {
        autoArmed = true;
    }

    public static void clearAutoRelease() {
        autoArmed = false;
    }

    /** Forgets the closing history, so a fresh designation is not judged against the old one. */
    private static void resetMissHistory() {
        lastMiss = Double.NaN;
        missClosing = false;
    }

    public static boolean isAutoArmed() {
        return autoArmed;
    }

    /** Range readout for the overlay, or a negative number when there is no point. */
    public static double rangeToSight() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof B2Entity b2)) {
            return -1.0D;
        }
        double[] origin = cameraPosition(b2);
        return B2PodMath.range(origin[0], origin[1], origin[2], sightPoint());
    }
}
