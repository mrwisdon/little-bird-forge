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
 * Small 70mm rocket: nose, motor body and tail fins. Typed to Entity so the same model serves
 * both the Little Bird's pods and the Apache's guided missiles.
 */
public final class LittleBirdRocketModel extends EntityModel<net.minecraft.world.entity.Entity> {
    public static final ModelLayerLocation ROCKET_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "little_bird_rocket"),
            "main");

    private final ModelPart root;

    public LittleBirdRocketModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-1.5F, -1.5F, -6.0F, 3.0F, 3.0F, 10.0F)
                .texOffs(480, 304).addBox(-1.0F, -1.0F, -10.0F, 2.0F, 2.0F, 4.0F)
                .texOffs(300, 400).addBox(-3.0F, -1.0F, 2.0F, 6.0F, 1.0F, 3.0F)
                .texOffs(300, 400).addBox(-1.0F, -3.0F, 2.0F, 1.0F, 6.0F, 3.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 512, 512);
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
