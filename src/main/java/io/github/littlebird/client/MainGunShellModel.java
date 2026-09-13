package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 120mm main gun round: a long dart with a tapered cap, a driving band, a rear rim and four tail
 * fins. Nose along -Z like the vehicles, so the renderer can reuse the shared yaw/pitch maths.
 *
 * <p>UV zones in {@code textures/entity/main_gun_shell.png}: steel body, brass bands, dark tip and
 * fins. Nothing here is a copy of any other game's asset - it is procedural boxes over a palette
 * this repository generates.
 */
public final class MainGunShellModel extends EntityModel<net.minecraft.world.entity.Entity> {
    public static final ModelLayerLocation SHELL_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "main_gun_shell"), "main");

    private final ModelPart root;

    public MainGunShellModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                // Steel body and the tapered cap in front of it.
                .texOffs(0, 0).addBox(-2.5F, -2.5F, -6.0F, 5.0F, 5.0F, 14.0F)
                .texOffs(48, 0).addBox(-2.0F, -2.0F, -11.0F, 4.0F, 4.0F, 5.0F)
                .texOffs(48, 0).addBox(-1.0F, -1.0F, -15.0F, 2.0F, 2.0F, 4.0F)
                // Brass driving band and the rear rim.
                .texOffs(32, 0).addBox(-3.0F, -3.0F, 1.0F, 6.0F, 6.0F, 2.0F)
                .texOffs(32, 0).addBox(-3.0F, -3.0F, 8.0F, 6.0F, 6.0F, 2.0F)
                // Four tail fins.
                .texOffs(48, 0).addBox(-5.0F, -0.5F, 4.0F, 10.0F, 1.0F, 4.0F)
                .texOffs(48, 0).addBox(-0.5F, -5.0F, 4.0F, 1.0F, 10.0F, 4.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(net.minecraft.world.entity.Entity entity, float limbSwing,
                          float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
