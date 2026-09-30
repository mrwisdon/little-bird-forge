package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.littlebird.LittleBird;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws a falling bomb as a plain casing aligned with its own velocity.
 *
 * <p>Generic on purpose: the JDAM and the incendiary round share this renderer instead of dragging
 * two more baked models into the pack. The casing is drawn at full brightness like the shells, so
 * a stick released into an overcast night is still traceable.
 */
public final class BombRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            LittleBird.MOD_ID, "textures/entity/main_gun_shell.png");
    private static final float HALF_WIDTH = 0.23F;
    private static final float HALF_HEIGHT = 0.23F;
    private static final float NOSE = 1.55F;
    private static final float TAIL = -1.10F;

    public BombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.45F;
        this.shadowStrength = 0.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 motion = entity.getDeltaMovement();
        float yaw = (float) (Mth.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (-Mth.atan2(motion.y,
                Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * 180.0D / Math.PI);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        int light = LightTexture.FULL_BRIGHT;
        // nose cap, tail cap, then the four sides
        quad(consumer, matrix, normal, -HALF_WIDTH, -HALF_HEIGHT, NOSE, HALF_WIDTH, -HALF_HEIGHT,
                NOSE, HALF_WIDTH, HALF_HEIGHT, NOSE, -HALF_WIDTH, HALF_HEIGHT, NOSE, 0, 0, 1, light);
        quad(consumer, matrix, normal, HALF_WIDTH, -HALF_HEIGHT, TAIL, -HALF_WIDTH, -HALF_HEIGHT,
                TAIL, -HALF_WIDTH, HALF_HEIGHT, TAIL, HALF_WIDTH, HALF_HEIGHT, TAIL, 0, 0, -1, light);
        quad(consumer, matrix, normal, -HALF_WIDTH, -HALF_HEIGHT, TAIL, HALF_WIDTH, -HALF_HEIGHT,
                TAIL, HALF_WIDTH, -HALF_HEIGHT, NOSE, -HALF_WIDTH, -HALF_HEIGHT, NOSE, 0, -1, 0, light);
        quad(consumer, matrix, normal, HALF_WIDTH, HALF_HEIGHT, TAIL, -HALF_WIDTH, HALF_HEIGHT, TAIL,
                -HALF_WIDTH, HALF_HEIGHT, NOSE, HALF_WIDTH, HALF_HEIGHT, NOSE, 0, 1, 0, light);
        quad(consumer, matrix, normal, HALF_WIDTH, -HALF_HEIGHT, TAIL, HALF_WIDTH, HALF_HEIGHT, TAIL,
                HALF_WIDTH, HALF_HEIGHT, NOSE, HALF_WIDTH, -HALF_HEIGHT, NOSE, 1, 0, 0, light);
        quad(consumer, matrix, normal, -HALF_WIDTH, HALF_HEIGHT, TAIL, -HALF_WIDTH, -HALF_HEIGHT,
                TAIL, -HALF_WIDTH, -HALF_HEIGHT, NOSE, -HALF_WIDTH, HALF_HEIGHT, NOSE, -1, 0, 0,
                light);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz, int light) {
        vertex(consumer, matrix, normal, x0, y0, z0, 0.0F, 1.0F, nx, ny, nz, light);
        vertex(consumer, matrix, normal, x1, y1, z1, 1.0F, 1.0F, nx, ny, nz, light);
        vertex(consumer, matrix, normal, x2, y2, z2, 1.0F, 0.0F, nx, ny, nz, light);
        vertex(consumer, matrix, normal, x3, y3, z3, 0.0F, 0.0F, nx, ny, nz, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int light) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}
