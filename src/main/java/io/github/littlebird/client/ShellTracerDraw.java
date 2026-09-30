package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBirdConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The streak half of a shell renderer: it turns the pure {@link ShellTracerTrail} segments into
 * additive light, and it measures the field of view they are drawn through. The 120mm round and the
 * 25mm autocannon round share it, so the two shells cannot drift apart visually.
 *
 * <p>{@code RenderType.lightning()} draws every ribbon. It is the vanilla type that takes
 * {@code DefaultVertexFormat.POSITION_COLOR} with no texture at all, and its shader never samples
 * the light map, so a bar is exactly as bright at noon as at midnight; its additive transparency
 * lets the narrow core and the wider glow stack into a hot centre. It keeps the default back-face
 * cull, so every ribbon is submitted with both windings. Checked against
 * {@code forge-1.20.1-47.4.20_mapped_official_1.20.1.jar}, where LIGHTNING is created with
 * POSITION_COLOR, RENDERTYPE_LIGHTNING_SHADER, LIGHTNING_TRANSPARENCY and COLOR_DEPTH_WRITE. It is
 * the same buffer vanilla's own lightning bolts fetch from an entity renderer, so the entity pass
 * flushes it for us and this class must not flush the shared buffer itself.
 *
 * <p>Each piece is drawn in two perpendicular planes, which keeps the bar readable from any viewing
 * angle without having to face it at the camera every frame. Vertices are entity relative: the
 * caller passes the shift that moves the streak onto the shell's tail, and the entity render
 * dispatcher's pose already has its origin at the entity's interpolated position.
 */
public final class ShellTracerDraw {
    /** The glow is the same bar, wider and dimmer, so the core keeps a hot edge. */
    private static final double GLOW_WIDTH_SCALE = 2.4D;
    private static final float GLOW_ALPHA_SCALE = 0.3F;
    /** Additive brightness at the shell end of a segment, out of 255. */
    private static final int CORE_ALPHA = 240;
    /** A segment still fades along its own length, so the taper does not step at every segment. */
    private static final float SEGMENT_TAIL_FADE = 0.45F;
    private static final double DEGENERATE_SQR = 1.0E-12D;

    private ShellTracerDraw() {
    }

    /**
     * Draws a whole streak: one glow ribbon and one core ribbon per segment, each in the two
     * perpendicular planes. Colours are packed {@code 0xRRGGBB}; the brightness of a ribbon comes
     * from its segment, so the far end of the streak is dimmer than the end at the shell.
     */
    public static void draw(MultiBufferSource buffer, PoseStack pose,
                            List<ShellTracerTrail.Segment> segments, Vec3 shift,
                            int coreColour, int glowColour) {
        VertexConsumer vertices = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();
        for (ShellTracerTrail.Segment segment : segments) {
            Vec3 far = new Vec3(segment.ax() + shift.x, segment.ay() + shift.y,
                    segment.az() + shift.z);
            Vec3 near = new Vec3(segment.bx() + shift.x, segment.by() + shift.y,
                    segment.bz() + shift.z);
            Vec3 direction = near.subtract(far);
            if (direction.lengthSqr() < DEGENERATE_SQR) {
                continue;
            }
            direction = direction.normalize();
            Vec3 side = perpendicularTo(direction);
            Vec3 up = side.cross(direction).normalize();
            float nearAlpha = segment.brightness();
            float farAlpha = nearAlpha * SEGMENT_TAIL_FADE;
            double glowWidth = segment.halfWidth() * GLOW_WIDTH_SCALE;
            float glowNear = nearAlpha * GLOW_ALPHA_SCALE;
            float glowFar = farAlpha * GLOW_ALPHA_SCALE;
            drawRibbon(vertices, matrix, far, near, side, glowWidth, glowColour, glowFar, glowNear);
            drawRibbon(vertices, matrix, far, near, up, glowWidth, glowColour, glowFar, glowNear);
            drawRibbon(vertices, matrix, far, near, side, segment.halfWidth(), coreColour,
                    farAlpha * CORE_ALPHA, nearAlpha * CORE_ALPHA);
            drawRibbon(vertices, matrix, far, near, up, segment.halfWidth(), coreColour,
                    farAlpha * CORE_ALPHA, nearAlpha * CORE_ALPHA);
        }
    }

    /**
     * The vertical field of view the streak is really projected through. A ground vehicle's gun sight
     * is a magnifier, so while the local player is looking through that optic the compensation has to
     * be measured through the zoomed field of view: measured through the unzoomed one instead, the
     * streak would shrink by exactly the factor the sight magnifies by, which is backwards.
     */
    public static double effectiveVerticalFovDeg(Minecraft minecraft) {
        double fov = minecraft.options.fov().get();
        if (minecraft.player != null && minecraft.player.getVehicle() instanceof GroundView view
                && view.isInteriorView()) {
            return fov * LittleBirdConfig.groundSightFovMultiplier();
        }
        return fov;
    }

    /** Any perpendicular will do, so it picks a reference the streak is not parallel to. */
    private static Vec3 perpendicularTo(Vec3 direction) {
        Vec3 reference = Math.abs(direction.y) < 0.95D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
        return direction.cross(reference).normalize();
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix, Vec3 far, Vec3 near,
                                   Vec3 normal, double halfWidth, int colour,
                                   float farAlpha, float nearAlpha) {
        Vec3 offset = normal.scale(halfWidth);
        int red = (colour >> 16) & 0xFF;
        int green = (colour >> 8) & 0xFF;
        int blue = colour & 0xFF;
        int farA = packAlpha(farAlpha);
        int nearA = packAlpha(nearAlpha);
        vertex(vertices, matrix, far.subtract(offset), red, green, blue, farA);
        vertex(vertices, matrix, far.add(offset), red, green, blue, farA);
        vertex(vertices, matrix, near.add(offset), red, green, blue, nearA);
        vertex(vertices, matrix, near.subtract(offset), red, green, blue, nearA);
        // lightning() culls back faces, so the same quad goes out with the opposite winding:
        // whichever side the camera is on, one of the two survives.
        vertex(vertices, matrix, near.subtract(offset), red, green, blue, nearA);
        vertex(vertices, matrix, near.add(offset), red, green, blue, nearA);
        vertex(vertices, matrix, far.add(offset), red, green, blue, farA);
        vertex(vertices, matrix, far.subtract(offset), red, green, blue, farA);
    }

    private static int packAlpha(float alpha) {
        return Math.max(0, Math.min(255, Math.round(alpha)));
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}

