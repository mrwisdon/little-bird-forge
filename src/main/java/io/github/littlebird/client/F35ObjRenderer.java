package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.littlebird.entity.JetEntity;
import io.github.littlebird.entity.JetGeometry;
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
 * Draws the F-35E from the Blender-built OBJ mesh instead of the box {@link JetModel}.
 *
 * <p>The mesh ships as a Forge {@code forge:obj} model ({@code battlefield:f35_preview}) and is
 * drawn here through {@link net.minecraft.client.renderer.entity.ItemRenderer#renderStatic}, so the
 * whole import pipeline stays asset-only: no custom geometry loader, no baking code. The OBJ points
 * down +X (Blender nose) while the box model points down -Z, so the renderer rotates it into the
 * shared convention first; yaw, pitch, roll and the shadow are copied from {@link JetRenderer}.
 *
 * <p>Forge's OBJ loader uses the file's coordinates as blocks (no /16), so the exported mesh is
 * {@link #OBJ_LENGTH_BLOCKS} blocks long and is scaled down to {@link JetGeometry}'s 11-block hull.
 */
public final class F35ObjRenderer extends EntityRenderer<JetEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "battlefield", "textures/entity/f35.png");

    /** Nose-to-tail length of the exported mesh, straight from its bounding box, in blocks. */
    private static final float OBJ_LENGTH_BLOCKS = 15.55F;
    /** Shrinks the mesh onto the box model's hull. One constant to retune if the jet looks off. */
    private static final float SCALE = JetGeometry.airframeLengthBlocks() / OBJ_LENGTH_BLOCKS;
    /** The mesh is not centred on its origin: shift its hull centre onto the entity. */
    private static final float MESH_CENTRE_X = 0.0F;
    /** Lifts the scaled mesh so its lowest point (0.56 m) sits on the entity origin. */
    private static final float MESH_BOTTOM_Y = -0.40F;

    private final ItemStack mesh = new ItemStack(ModItems.F35_PREVIEW.get());

    public F35ObjRenderer(EntityRendererProvider.Context context) {
        super(context);
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
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float roll = entity.interpolatedRenderRollDeg(partialTick);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        pose.mulPose(Axis.ZP.rotationDegrees(LittleBirdModelMath.modelRollDegrees(roll)));
        // Blender nose +X -> model nose -Z.
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(-MESH_CENTRE_X, MESH_BOTTOM_Y, 0.0F);
        Minecraft.getInstance().getItemRenderer().renderStatic(mesh, ItemDisplayContext.NONE,
                packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(),
                entity.getId());
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
