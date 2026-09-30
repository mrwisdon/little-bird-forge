package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.JetEntity;
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
 * Su-57 Felon: the same box-built airframe language as {@link JetModel}, but a broader twin-engine
 * fuselage, two nozzles and the wide-set tails the Felon is known for. It samples the same zone
 * layout from its own sheet, so both jets share the texture generator.
 */
public final class Su57Model extends EntityModel<JetEntity> {
    public static final ModelLayerLocation SU57_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "su57"), "main");
    private static final int TEXTURE_SIZE = 512;

    private final ModelPart root;

    public Su57Model(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fuselage", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-15.0F, 8.0F, -48.0F, 30.0F, 16.0F, 88.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("nose", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.0F, 8.0F, -84.0F, 20.0F, 12.0F, 36.0F)
                .texOffs(0, 0).addBox(-6.0F, 9.0F, -94.0F, 12.0F, 9.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("cockpit", CubeListBuilder.create()
                .texOffs(256, 128).addBox(-9.0F, 20.0F, -44.0F, 18.0F, 8.0F, 28.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-8.0F, 22.0F, 0.0F, 16.0F, 6.0F, 38.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("intake_left", CubeListBuilder.create()
                .texOffs(384, 192).addBox(-17.0F, 12.0F, -18.0F, 8.0F, 10.0F, 28.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("intake_right", CubeListBuilder.create()
                .texOffs(384, 192).addBox(9.0F, 12.0F, -18.0F, 8.0F, 10.0F, 28.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("engine_left", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-13.0F, 9.0F, 40.0F, 12.0F, 13.0F, 18.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("engine_right", CubeListBuilder.create()
                .texOffs(256, 0).addBox(1.0F, 9.0F, 40.0F, 12.0F, 13.0F, 18.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("nozzle_left", CubeListBuilder.create()
                .texOffs(384, 128).addBox(-7.0F, 10.0F, 58.0F, 14.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("nozzle_right", CubeListBuilder.create()
                .texOffs(384, 128).addBox(-7.0F, 10.0F, 58.0F, 14.0F, 10.0F, 10.0F),
                PartPose.offset(14.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("cannon", CubeListBuilder.create()
                .texOffs(384, 192).addBox(-2.0F, 12.0F, -88.0F, 4.0F, 3.0F, 20.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wing_left_inner", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-36.0F, 12.0F, -18.0F, 22.0F, 5.0F, 54.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wing_left_outer", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-62.0F, 13.0F, 8.0F, 26.0F, 4.0F, 22.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wing_right_inner", CubeListBuilder.create()
                .texOffs(0, 0).addBox(14.0F, 12.0F, -18.0F, 22.0F, 5.0F, 54.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wing_right_outer", CubeListBuilder.create()
                .texOffs(0, 0).addBox(36.0F, 13.0F, 8.0F, 26.0F, 4.0F, 22.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("stabilizer_left", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-34.0F, 13.0F, 42.0F, 20.0F, 4.0F, 20.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("stabilizer_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(14.0F, 13.0F, 42.0F, 20.0F, 4.0F, 20.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fin_left_lower", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-18.0F, 18.0F, 32.0F, 6.0F, 20.0F, 22.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fin_left_upper", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-20.0F, 38.0F, 40.0F, 6.0F, 20.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fin_right_lower", CubeListBuilder.create()
                .texOffs(256, 0).addBox(12.0F, 18.0F, 32.0F, 6.0F, 20.0F, 22.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fin_right_upper", CubeListBuilder.create()
                .texOffs(256, 0).addBox(14.0F, 38.0F, 40.0F, 6.0F, 20.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pylon_left", CubeListBuilder.create()
                .texOffs(256, 192).addBox(-48.0F, 3.0F, -12.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pylon_right", CubeListBuilder.create()
                .texOffs(256, 192).addBox(40.0F, 3.0F, -12.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pod_left", CubeListBuilder.create()
                .texOffs(256, 192).addBox(-51.0F, 0.0F, -16.0F, 12.0F, 8.0F, 22.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pod_right", CubeListBuilder.create()
                .texOffs(256, 192).addBox(39.0F, 0.0F, -16.0F, 12.0F, 8.0F, 22.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        addDetails(body);
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /** Cosmetic detail: the IRST ball, leading edge extensions, bay doors and nozzle rings. */
    private static void addDetails(PartDefinition body) {
        body.addOrReplaceChild("nose_probe", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, 12.5F, -102.0F, 1.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // The Felon's IRST ball sits on the nose spine.
        body.addOrReplaceChild("irst_ball", CubeListBuilder.create()
                .texOffs(384, 192).addBox(-3.0F, 16.0F, -91.0F, 6.0F, 5.0F, 6.0F)
                .texOffs(384, 192).addBox(-2.0F, 17.0F, -93.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int sign = -1; sign <= 1; sign += 2) {
            String side = sign < 0 ? "left" : "right";
            // Leading edge root extensions: the signature wide LERX.
            body.addOrReplaceChild("lerx_" + side, CubeListBuilder.create()
                    .texOffs(0, 0).addBox(sign < 0 ? -24.0F : 15.0F, 11.0F, -40.0F,
                            9.0F, 3.0F, 24.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("bay_door_" + side, CubeListBuilder.create()
                    .texOffs(384, 192).addBox(sign < 0 ? -9.0F : 1.0F, 6.0F, -10.0F,
                            8.0F, 1.0F, 22.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("fin_light_" + side, CubeListBuilder.create()
                    .texOffs(384, 128).addBox(sign < 0 ? -21.0F : 20.0F, 46.0F, 46.0F,
                            1.0F, 2.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // Nozzle petal rings, one per engine.
            float base = sign < 0 ? 0.0F : 14.0F;
            body.addOrReplaceChild("nozzle_ring_top_" + side, CubeListBuilder.create()
                    .texOffs(384, 128).addBox(base - 7.0F, 20.0F, 58.0F, 14.0F, 2.0F, 10.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("nozzle_ring_bottom_" + side, CubeListBuilder.create()
                    .texOffs(384, 128).addBox(base - 7.0F, 8.0F, 58.0F, 14.0F, 2.0F, 10.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("nozzle_ring_outer_" + side, CubeListBuilder.create()
                    .texOffs(384, 128).addBox(base + (sign < 0 ? -9.0F : 7.0F), 10.0F, 58.0F,
                            2.0F, 10.0F, 10.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        body.addOrReplaceChild("dorsal_airbrake", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-6.0F, 24.0F, 8.0F, 12.0F, 2.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    @Override
    public void setupAnim(JetEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Fixed-wing airframe: no articulated parts.
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
