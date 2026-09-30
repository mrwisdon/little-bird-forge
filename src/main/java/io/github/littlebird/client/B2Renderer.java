package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.B2Entity;
import io.github.littlebird.entity.B2Geometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the B-2 through the same yaw/pitch/roll convention as the two fighters.
 *
 * <p>This is the box-model fallback: {@link B2ObjRenderer} is the one registered, because
 * the Blender mesh is the real airframe. Swap the registration in ClientModEvents to this
 * renderer if the OBJ assets ever fail to load.
 */
public final class B2Renderer extends EntityRenderer<B2Entity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/b2.png");

    private final B2Model model;

    public B2Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new B2Model(context.bakeLayer(B2Model.B2_LAYER));
        this.shadowRadius = 6.0F * B2Geometry.SCALE;
        this.shadowStrength = 0.5F;
    }

    /**
     * True when the local player is sitting inside this aeroplane with a first person sight up:
     * the rigid cockpit view or the electro-optical pod. Other players keep seeing the airframe.
     */
    private static boolean hidesOwnAirframe(B2Entity entity) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player != entity.getControllingPassenger()) {
            return false;
        }
        return entity.isCockpitView() || B2PodView.isActive();
    }

    @Override
    public ResourceLocation getTextureLocation(B2Entity entity) {
        return TEXTURE;
    }

    @Override
    public void render(B2Entity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.scale(B2Geometry.SCALE, B2Geometry.SCALE, B2Geometry.SCALE);
        if (hidesOwnAirframe(entity)) {
            // First person: the crew is inside the aeroplane, so the aeroplane is not drawn.
            return;
        }
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
