package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.AutocannonShellEntity;
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
 * One 25mm M242 round as a real body: brass case, steel shell, pointed cap. Small next to the 70mm
 * rocket, but big enough at Minecraft's block scale that a burst reads as projectiles rather than a
 * streak of light.
 */
public final class AutocannonShellModel extends EntityModel<AutocannonShellEntity> {
    public static final ModelLayerLocation SHELL_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "autocannon_shell"), "main");

    private final ModelPart root;

    public AutocannonShellModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("shell", CubeListBuilder.create()
                        // Steel body down the middle, brass case at the base, dark cap up front.
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 8.0F)
                        .texOffs(24, 0).addBox(-1.5F, -1.5F, 3.0F, 3.0F, 3.0F, 2.0F)
                        .texOffs(36, 0).addBox(-0.5F, -0.5F, -8.0F, 1.0F, 1.0F, 3.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AutocannonShellEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
