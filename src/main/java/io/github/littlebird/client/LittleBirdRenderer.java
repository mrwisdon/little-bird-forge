package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.entity.LittleBirdGeometry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the helicopter: entity yaw and pitch come from the synced pose, roll
 * from the aircraft state, the rotors spin with rotor RPM and a translucent disc
 * fades in at speed so the blades read as motion rather than as a slow cross.
 */
public final class LittleBirdRenderer extends EntityRenderer<LittleBirdEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/little_bird.png");

    /** Windscreen tint; low enough that the front glass never blocks the view. */
    private static final float GLASS_ALPHA = 0.28F;

    private final LittleBirdModel model;

    public LittleBirdRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new LittleBirdModel(context.bakeLayer(LittleBirdModel.LITTLE_BIRD_LAYER));
        this.shadowRadius = 2.2F * LittleBirdGeometry.SCALE;
        this.shadowStrength = 0.55F;
    }

    @Override
    public ResourceLocation getTextureLocation(LittleBirdEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(
            LittleBirdEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight) {
        pose.pushPose();
        pose.scale(LittleBirdGeometry.SCALE, LittleBirdGeometry.SCALE, LittleBirdGeometry.SCALE);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float roll = entity.getRenderRollDeg();
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        pose.mulPose(Axis.ZP.rotationDegrees(LittleBirdModelMath.modelRollDegrees(roll)));

        model.setCockpitView(entity.isCockpitView());
        model.setRocketState(entity.getRocketsLeft(), entity.getRocketsRight());
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, pitch);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        // The windscreen and the rotor disc are skipped from inside the cockpit:
        // there they would sit right against the camera and block the view.
        int gunHeat = entity.getGunHeat();
        if (gunHeat > 0 && !entity.isCockpitView()) {
            VertexConsumer heatConsumer = buffer.getBuffer(
                    RenderType.entityTranslucentEmissive(TEXTURE));
            model.renderGunHeat(pose, heatConsumer, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, gunHeat);
        }

        if (!entity.isCockpitView()) {
            VertexConsumer glassConsumer = buffer.getBuffer(
                    RenderType.entityTranslucentCull(TEXTURE));
            model.renderGlass(pose, glassConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                    GLASS_ALPHA);

            float blurAlpha = LittleBirdModelMath.rotorBlurAlpha(
                    entity.getRenderRotorRpm(), entity.getRenderRotorSpinFactor());
            if (blurAlpha > 0.0F) {
                VertexConsumer blurConsumer = buffer.getBuffer(
                        RenderType.entityTranslucentCull(TEXTURE));
                model.renderRotorBlur(pose, blurConsumer, packedLight,
                        OverlayTexture.NO_OVERLAY, blurAlpha);
            }
        }
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
