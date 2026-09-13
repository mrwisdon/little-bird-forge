package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.ApacheGeometry;
import io.github.littlebird.entity.weapon.ApachePods;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the AH-64: entity yaw and pitch from the synced pose, roll from the aircraft state, the
 * rotor, tail rotor and radar animated from rotor RPM and age, and the chain gun turret aimed
 * from its own synced angles. Canopy glazing and the rotor blur are separate translucent passes.
 */
public final class ApacheRenderer extends EntityRenderer<ApacheEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/apache.png");
    /** Windscreen tint; low enough that the front glass never blocks the view. */
    private static final float GLASS_ALPHA = 0.28F;

    private final ApacheModel model;

    public ApacheRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ApacheModel(context.bakeLayer(ApacheModel.APACHE_LAYER));
        this.shadowRadius = 3.0F * ApacheGeometry.SCALE;
        this.shadowStrength = 0.6F;
    }

    @Override
    public ResourceLocation getTextureLocation(ApacheEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(
            ApacheEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight) {
        pose.pushPose();
        pose.scale(ApacheGeometry.SCALE, ApacheGeometry.SCALE, ApacheGeometry.SCALE);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float roll = entity.getRenderRollDeg();
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        pose.mulPose(Axis.ZP.rotationDegrees(LittleBirdModelMath.modelRollDegrees(roll)));

        model.setCockpitView(entity.isCockpitView());
        int readyRockets = entity.getReadyRockets();
        model.setRocketState(ApachePods.podReady(readyRockets, 0),
                ApachePods.podReady(readyRockets, 1));
        model.setMissileState(entity.getReadyMissiles());
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, pitch);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        // A hot chain gun barrel glows at the muzzle, which is the only warning the front
        // seat gets before the barrel locks out.
        float heat = entity.getChainGunHeatFraction();
        if (heat > 0.0F && !entity.isCockpitView()) {
            VertexConsumer heatConsumer = buffer.getBuffer(
                    RenderType.entityTranslucentEmissive(TEXTURE));
            model.renderGunHeat(pose, heatConsumer, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, heat);
        }

        // The glazing and the rotor disc are skipped from inside a seat: there they would sit
        // right against the camera and block the view.
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
