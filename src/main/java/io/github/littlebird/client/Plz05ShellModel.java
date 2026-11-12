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
 * The 155mm shell: a slim ogive projectile with a driving band and a tracer base.
 *
 * <p>Model units are 1/16 block, so the round is about 0.9 m long and 0.16 m across - a shell, not
 * a rocket: no motor and no fins, just the fuze, the body, the band and the tracer.
 */
public final class Plz05ShellModel extends EntityModel<net.minecraft.world.entity.Entity> {
    public static final ModelLayerLocation PLZ05_ROCKET_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "plz05_rocket"), "main");

    private final ModelPart root;

    public Plz05ShellModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.25F, -1.25F, -4.0F, 2.5F, 2.5F, 8.0F)
                .texOffs(60, 0).addBox(-1.0F, -1.0F, -7.0F, 2.0F, 2.0F, 3.0F)
                .texOffs(60, 0).addBox(-0.5F, -0.5F, -9.0F, 1.0F, 1.0F, 2.0F)
                .texOffs(60, 16).addBox(-1.45F, -1.45F, 1.2F, 2.9F, 2.9F, 1.2F)
                .texOffs(0, 24).addBox(-1.1F, -1.1F, 4.0F, 2.2F, 2.2F, 1.0F)
                .texOffs(88, 0).addBox(-0.5F, -0.5F, 5.0F, 1.0F, 1.0F, 1.0F),
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
