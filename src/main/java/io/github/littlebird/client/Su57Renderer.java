package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.JetGeometry;
import io.github.littlebird.entity.Su57Entity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the Su-57 through the same yaw/pitch/roll convention as the F-35E. */
public final class Su57Renderer extends EntityRenderer<Su57Entity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/su57.png");

    private final Su57Model model;

    public Su57Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new Su57Model(context.bakeLayer(Su57Model.SU57_LAYER));
        this.shadowRadius = 3.0F * JetGeometry.SCALE;
        this.shadowStrength = 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(Su57Entity entity) {
        return TEXTURE;
    }

    @Override
    public void render(Su57Entity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.scale(JetGeometry.SCALE, JetGeometry.SCALE, JetGeometry.SCALE);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float roll = entity.getRenderRollDeg();
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        pose.mulPose(Axis.ZP.rotationDegrees(LittleBirdModelMath.modelRollDegrees(roll)));

        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, pitch);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
