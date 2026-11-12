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
 * The 370mm rocket: a long motor body, a pointed nose and four tail fins. Bigger than the 70mm
 * rocket so it still reads as an artillery round from the chase camera.
 */
public final class Phl191RocketModel extends EntityModel<net.minecraft.world.entity.Entity> {
    public static final ModelLayerLocation PHL191_ROCKET_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "phl191_rocket"), "main");

    private final ModelPart root;

    public Phl191RocketModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -3.0F, -14.0F, 6.0F, 6.0F, 24.0F)
                .texOffs(64, 0).addBox(-2.0F, -2.0F, -22.0F, 4.0F, 4.0F, 8.0F)
                .texOffs(64, 16).addBox(-2.5F, -2.5F, -18.0F, 5.0F, 5.0F, 4.0F)
                .texOffs(0, 32).addBox(-7.0F, -0.5F, 4.0F, 14.0F, 1.0F, 6.0F)
                .texOffs(0, 32).addBox(-0.5F, -7.0F, 4.0F, 1.0F, 14.0F, 6.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
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
