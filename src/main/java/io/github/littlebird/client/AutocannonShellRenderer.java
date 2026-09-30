package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.AutocannonShellEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the 25mm round and the rigid streak it trails, the same way the 120mm round is drawn: the
 * body at {@link LightTexture#FULL_BRIGHT} so a burst stays visible in the dark, and a
 * constant-width bar of additive light from the shared {@link ShellTracerTrail} and
 * {@link ShellTracerDraw}.
 *
 * <p>The autocannon fires five rounds a second, so the streak is deliberately thinner than the
 * tank's and is scaled up by the same minimum-pixel rule: a burst that has to be read at a few
 * hundred blocks would otherwise be a row of invisible specks. Every 25mm round in flight shares one
 * static history, so this renderer also forgets each round as it disappears.
 */
public final class AutocannonShellRenderer extends EntityRenderer<AutocannonShellEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/autocannon_shell.png");
    /**
     * The rear of the brass case sits 5 model pixels behind the origin and 16 model pixels are one
     * block, so the streak leaves the case instead of starting inside it.
     */
    private static final double TAIL_OFFSET_BLOCKS = 5.0D / 16.0D;
    /** A 25mm tracer starts thinner than the 120mm one; the pixel rule thickens it with range. */
    private static final double TRACER_HALF_WIDTH = 0.022D;
    private static final int CORE_COLOUR = 0xFFE9A8;
    private static final int GLOW_COLOUR = 0xFF7A20;
    /** Same guard as the 120mm renderer: below this the copied velocity is noise, not a direction. */
    private static final double MOTION_EPSILON_SQR = 1.0E-6D;

    private final AutocannonShellModel model;

    public AutocannonShellRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new AutocannonShellModel(
                context.bakeLayer(AutocannonShellModel.SHELL_LAYER));
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(AutocannonShellEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(AutocannonShellEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        Vec3 position = entity.getPosition(partialTick);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // Feed the burst's streak before drawing it, and forget a round the moment it is gone: the
        // history is static and shared by every 25mm round in flight.
        ShellTracerTrail.track(entity.getId(), entity.tickCount + partialTick,
                position.x, position.y, position.z);
        if (entity.isRemoved()) {
            ShellTracerTrail.forget(entity.getId());
        }

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, pitch);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        // FULL_BRIGHT for the same reason as the tank round: a shell nobody can see in the dark is
        // not a tracer, and this round is small enough that the light map would black it out.
        model.renderToBuffer(pose, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();

        renderTracer(entity, position, partialTick, buffer, pose);

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    /**
     * Measures this frame's streak and hands it to the shared drawer. It is the same rule the 120mm
     * uses - pixels per radian through the real camera, distance to the round, minimum pixel size - so
     * both shells keep a legible bar at the range each of them can actually reach.
     */
    private void renderTracer(AutocannonShellEntity entity, Vec3 position, float partialTick,
                              MultiBufferSource buffer, PoseStack pose) {
        double baseLength = LittleBirdConfig.shellTracerLengthBlocks();
        if (!(baseLength > 0.0D)) {
            // The config can switch the streak off for both shells at once.
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        double pixelsPerRadian = ShellTracerTrail.pixelsPerRadian(
                ShellTracerDraw.effectiveVerticalFovDeg(minecraft),
                minecraft.getWindow().getHeight());
        double distance = this.entityRenderDispatcher.camera.getPosition().distanceTo(position);
        double length = ShellTracerTrail.lengthForDistance(baseLength, distance, pixelsPerRadian,
                LittleBirdConfig.shellTracerMinPixelLength());
        double halfWidth = ShellTracerTrail.halfWidthForDistance(TRACER_HALF_WIDTH, distance,
                pixelsPerRadian, LittleBirdConfig.shellTracerMinPixelWidth());
        Vec3 motion = entity.getDeltaMovement();
        Vec3 heading = motion.lengthSqr() > MOTION_EPSILON_SQR
                ? motion.normalize()
                : entity.getViewVector(partialTick);
        List<ShellTracerTrail.Segment> segments = ShellTracerTrail.segments(
                entity.getId(), length, halfWidth, heading.x, heading.y, heading.z);
        if (segments.isEmpty()) {
            return;
        }
        Vec3 shift = heading.scale(-TAIL_OFFSET_BLOCKS).subtract(position);
        ShellTracerDraw.draw(buffer, pose, segments, shift, CORE_COLOUR, GLOW_COLOUR);
    }
}

