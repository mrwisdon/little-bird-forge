package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.BradleyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the M2A2 Bradley: synced hull pose plus the turret's own angles. */
public final class BradleyRenderer extends EntityRenderer<BradleyEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/bradley.png");

    private final BradleyModel model;

    public BradleyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BradleyModel(context.bakeLayer(BradleyModel.BRADLEY_LAYER));
        this.shadowRadius = 1.7F;
        this.shadowStrength = 0.7F;
    }

    @Override
    public ResourceLocation getTextureLocation(BradleyEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BradleyEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        pose.mulPose(Axis.ZP.rotationDegrees(
                LittleBirdModelMath.modelRollDegrees(entity.getHullRollDeg())));
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, pitch);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
