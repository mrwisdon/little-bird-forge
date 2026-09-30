package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.MainGunShellEntity;
import io.github.littlebird.entity.weapon.MainGunAmmoType;
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
 * Draws the 120mm round and the rigid streak it trails.
 *
 * <p>Two choices make the flight read as hard rather than as a soft fireball. The body is drawn at
 * {@link LightTexture#FULL_BRIGHT}, so it stays visible at midnight instead of turning black, while
 * the high explosive round keeps its slightly larger and warmer read over the armour piercing dart.
 * The streak is a constant-width bar of additive light built from {@link ShellTracerTrail} instead
 * of a particle cloud that follows the shell, which is what makes it straight and hard rather than
 * fuzzy. {@link ShellTracerDraw} owns the quads and the reasons behind their render type, so this
 * renderer and the 25mm one cannot drift apart.
 */
public final class MainGunShellRenderer extends EntityRenderer<MainGunShellEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/main_gun_shell.png");
    private static final float HE_SCALE = 1.15F;

    /**
     * The rear rim of the model sits 10 model pixels behind the origin and 16 model pixels are one
     * block, so the streak leaves the tail instead of starting inside the casing.
     */
    private static final double TAIL_OFFSET_BLOCKS = 10.0D / 16.0D;
    private static final int AP_CORE_COLOUR = 0xFFF2C0;
    private static final int AP_GLOW_COLOUR = 0xFFA040;
    private static final int HE_CORE_COLOUR = 0xFFD07A;
    private static final int HE_GLOW_COLOUR = 0xFF6A18;
    /**
     * The client only receives a copied velocity for some entities, so below this the value is noise
     * and the round's own facing is used for the fallback bar.
     */
    private static final double MOTION_EPSILON_SQR = 1.0E-6D;

    private final MainGunShellModel model;

    public MainGunShellRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new MainGunShellModel(context.bakeLayer(MainGunShellModel.SHELL_LAYER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(MainGunShellEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(MainGunShellEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        boolean highExplosive = entity.shellType() == MainGunAmmoType.HE;
        float scale = highExplosive ? HE_SCALE : 1.0F;
        Vec3 position = entity.getPosition(partialTick);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // Feed the streak before drawing it. The history is a static map, so this is also
        // the only place that can notice a round is gone and forget it.
        ShellTracerTrail.track(entity.getId(), entity.tickCount + partialTick,
                position.x, position.y, position.z);
        if (entity.isRemoved()) {
            ShellTracerTrail.forget(entity.getId());
        }

        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, pitch);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        float red = 1.0F;
        float green = highExplosive ? 0.92F : 1.0F;
        float blue = highExplosive ? 0.78F : 1.0F;
        // FULL_BRIGHT: a tracer round has to be readable in the dark, which is the whole
        // point of it; the world light map would otherwise leave the body a black speck.
        model.renderToBuffer(pose, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                red, green, blue, 1.0F);
        pose.popPose();

        renderTracer(entity, position, partialTick, buffer, pose);

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    /**
     * Measures the streak for this frame and hands it to the shared drawer.
     *
     * <p>The width and the length come from the camera distance and from how many pixels one radian
     * covers, so a far round still gets a bar of a few pixels instead of a hairline; that measurement
     * uses the field of view the camera is really rendering with, so the gun sight's zoom does not
     * shrink the streak. The shift then moves the whole streak onto the tail cone, in entity relative
     * space, which is the space the incoming pose already has its origin in.
     */
    private void renderTracer(MainGunShellEntity entity, Vec3 position, float partialTick,
                              MultiBufferSource buffer, PoseStack pose) {
        double baseLength = LittleBirdConfig.shellTracerLengthBlocks();
        if (!(baseLength > 0.0D)) {
            // The config can switch the streak off entirely.
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // Pixels per radian through this frame's camera, plus the distance to the round, turn the
        // configured minimum pixel size into the block size the streak has to be drawn at.
        double pixelsPerRadian = ShellTracerTrail.pixelsPerRadian(
                ShellTracerDraw.effectiveVerticalFovDeg(minecraft),
                minecraft.getWindow().getHeight());
        double distance = this.entityRenderDispatcher.camera.getPosition().distanceTo(position);
        double length = ShellTracerTrail.lengthForDistance(baseLength, distance, pixelsPerRadian,
                LittleBirdConfig.shellTracerMinPixelLength());
        double halfWidth = ShellTracerTrail.halfWidthForDistance(ShellTracerTrail.HALF_WIDTH,
                distance, pixelsPerRadian, LittleBirdConfig.shellTracerMinPixelWidth());
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
        boolean highExplosive = entity.shellType() == MainGunAmmoType.HE;
        ShellTracerDraw.draw(buffer, pose, segments, shift,
                highExplosive ? HE_CORE_COLOUR : AP_CORE_COLOUR,
                highExplosive ? HE_GLOW_COLOUR : AP_GLOW_COLOUR);
    }
}

