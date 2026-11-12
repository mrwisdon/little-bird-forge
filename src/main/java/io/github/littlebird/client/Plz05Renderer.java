package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.Plz05Entity;
import io.github.littlebird.entity.ground.GroundSuspension;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the PLZ-05 chassis and the independently elevated gun. */
public final class Plz05Renderer extends EntityRenderer<Plz05Entity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/plz05.png");

    private final Plz05Model model;

    public Plz05Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new Plz05Model(context.bakeLayer(Plz05Model.PLZ05_LAYER));
        this.shadowRadius = 2.6F;
        this.shadowStrength = 0.7F;
    }

    @Override
    public ResourceLocation getTextureLocation(Plz05Entity entity) {
        return TEXTURE;
    }

    @Override
    public void render(Plz05Entity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        GroundSuspension.Pose chassis = entity.suspension().pose(partialTick);
        pose.translate(0.0D, chassis.liftBlocks(), 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(chassis.pitchDegrees()));
        pose.mulPose(Axis.ZP.rotationDegrees(-chassis.rollDegrees()));
        model.setGunPose(entity.renderElevationDeg(partialTick), chassis);
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
