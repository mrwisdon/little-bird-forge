package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.MainGunShellEntity;
import io.github.littlebird.entity.weapon.MainGunAmmoType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the 120mm round itself, so the player can see the shell fly rather than only its fireball.
 * The high explosive round is drawn a touch larger and warmer than the armour-piercing dart, which
 * is the second read on which shell is loaded.
 */
public final class MainGunShellRenderer extends EntityRenderer<MainGunShellEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/main_gun_shell.png");
    private static final float HE_SCALE = 1.15F;

    private final MainGunShellModel model;

    public MainGunShellRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new MainGunShellModel(context.bakeLayer(MainGunShellModel.SHELL_LAYER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(MainGunShellEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(MainGunShellEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        boolean highExplosive = entity.shellType() == MainGunAmmoType.HE;
        float scale = highExplosive ? HE_SCALE : 1.0F;
        pose.scale(scale, scale, scale);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        pose.mulPose(Axis.YP.rotationDegrees(LittleBirdModelMath.modelYawDegrees(yaw)));
        pose.mulPose(Axis.XP.rotationDegrees(LittleBirdModelMath.modelPitchDegrees(pitch)));
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, pitch);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        float red = 1.0F;
        float green = highExplosive ? 0.92F : 1.0F;
        float blue = highExplosive ? 0.78F : 1.0F;
        model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                red, green, blue, 1.0F);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
