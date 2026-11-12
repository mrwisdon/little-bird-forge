package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.Phl191Entity;
import io.github.littlebird.entity.ground.GroundSuspension;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the PHL-191 chassis, its suspension and the independently elevated pods. */
public final class Phl191Renderer extends EntityRenderer<Phl191Entity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/phl191.png");

    private final Phl191Model model;

    public Phl191Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new Phl191Model(context.bakeLayer(Phl191Model.PHL191_LAYER));
        this.shadowRadius = 2.6F;
        this.shadowStrength = 0.7F;
    }

    @Override
    public ResourceLocation getTextureLocation(Phl191Entity entity) {
        return TEXTURE;
    }

    @Override
    public void render(Phl191Entity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        GroundSuspension.Pose chassis = entity.suspension().pose(partialTick);
        pose.translate(0.0D, chassis.liftBlocks(), 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(chassis.pitchDegrees()));
        pose.mulPose(Axis.ZP.rotationDegrees(-chassis.rollDegrees()));
        model.setLauncherPose(entity.renderElevationDeg(partialTick), chassis);
        model.setWheelOffsets(entity.suspension(), partialTick);
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
