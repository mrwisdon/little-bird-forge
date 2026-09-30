package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.littlebird.entity.B2Entity;
import io.github.littlebird.entity.B2Geometry;
import io.github.littlebird.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the B-2 from the Blender-built OBJ mesh instead of the box {@link B2Model}: the
 * primary look for this aircraft.
 *
 * <p>The mesh ships as a Forge {@code forge:obj} model ({@code battlefield:b2_preview})
 * and is drawn through {@link net.minecraft.client.renderer.entity.ItemRenderer#renderStatic},
 * so the whole pipeline stays asset-only. The OBJ is in metres with the nose down +X and
 * the span on Z, while the model convention is nose -Z and span on +X, so the renderer
 * rotates it a quarter turn about Y first; yaw, pitch, roll and the shadow are copied
 * from {@link B2Renderer}.
 *
 * <p>Measured bounding box of b2_preview.obj: x -10.3977..10.45 (length 20.8477 m),
 * y -0.0038..5.1631, z +-26.18. Forge's OBJ loader treats the file's coordinates as blocks
 * (no /16), so {@link #SCALE} shrinks the 20.85 m mesh onto the
 * {@link B2Geometry#airframeLengthBlocks()} block hull and the two mesh offsets put its
 * bbox centre on the entity origin. The mesh already stands on y = 0, so no lift is
 * needed beyond the 3.8 mm its lowest vertex sits below the ground plane.
 */
public final class B2ObjRenderer extends EntityRenderer<B2Entity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "battlefield", "textures/entity/b2.png");

    /** Nose-to-tail length of the exported mesh, straight from its bounding box, in metres. */
    private static final float OBJ_LENGTH_BLOCKS = 20.8477F;
    /** Shrinks the mesh onto the 15 block box model's hull: 15.0 / 20.8477 = 0.7195. */
    private static final float SCALE = B2Geometry.airframeLengthBlocks() / OBJ_LENGTH_BLOCKS;
    /** The mesh is not centred on its origin along the nose axis. */
    private static final float MESH_LENGTH_CENTRE_M = 0.0261F;
    /** Lowest vertex of the mesh, in metres: lifted onto the entity origin. */
    private static final float MESH_MIN_Y_M = -0.0038F;

    private final ItemStack mesh = new ItemStack(ModItems.B2_PREVIEW.get());

    public B2ObjRenderer(EntityRendererProvider.Context context) {
        super(context);
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
        if (hidesOwnAirframe(entity)) {
            // First person: the crew is inside the aeroplane, so the aeroplane is not drawn.
            return;
        }
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float roll = entity.interpolatedRenderRollDeg(partialTick);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        pose.mulPose(Axis.ZP.rotationDegrees(LittleBirdModelMath.modelRollDegrees(roll)));
        // Blender nose +X -> model nose -Z.
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        // Mesh offsets are in metres and are applied before the scale, so they cancel the
        // mesh's own bbox offset: x -> model z, y stays y.
        pose.translate(-MESH_LENGTH_CENTRE_M, -MESH_MIN_Y_M, 0.0F);
        pose.scale(SCALE, SCALE, SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(mesh, ItemDisplayContext.NONE,
                packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(),
                entity.getId());
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
