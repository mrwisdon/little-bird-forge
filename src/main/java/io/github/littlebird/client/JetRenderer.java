package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.JetEntity;
import io.github.littlebird.entity.JetGeometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the F-35E from {@link JetModel}: yaw and pitch from the synced pose, roll from the
 * aircraft state, all through the shared helicopter rotation convention so a banked jet drops the
 * correct wing whichever way it turns.
 */
public final class JetRenderer extends EntityRenderer<JetEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/f35.png");

    private final JetModel model;

    public JetRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new JetModel(context.bakeLayer(JetModel.JET_LAYER));
        this.shadowRadius = 3.0F * JetGeometry.SCALE;
        this.shadowStrength = 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(JetEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(JetEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.scale(JetGeometry.SCALE, JetGeometry.SCALE, JetGeometry.SCALE);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float roll = entity.interpolatedRenderRollDeg(partialTick);
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        pose.mulPose(Axis.ZP.rotationDegrees(LittleBirdModelMath.modelRollDegrees(roll)));

        model.setVtol(entity.isRenderVtol());
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, pitch);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
