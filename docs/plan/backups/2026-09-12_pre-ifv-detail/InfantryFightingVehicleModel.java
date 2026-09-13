package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.InfantryFightingVehicleEntity;
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
 * EBAA Wildcat infantry fighting vehicle, built from the reference images recorded in
 * docs/plan/ground-units-reference-images.md. Unit space is unchanged: nose along -Z, model +X is
 * the right hand side, 16 units per block.
 *
 * <p>Stage 1 structure (6x6 silhouette), stage 2 secondary fittings, stage 3 the surface breaks:
 * wheel hubs, bull bar bars, glazing frames, missile tube rims, stowage hinges, door seams and
 * hatch hinges. Texture coordinates point at the zones painted by
 * tools/generate_ground_unit_textures.py.
 */
public final class InfantryFightingVehicleModel extends EntityModel<InfantryFightingVehicleEntity> {
    public static final ModelLayerLocation INFANTRY_FIGHTING_VEHICLE_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "infantry_fighting_vehicle"),
            "main");
    private static final int TEXTURE_SIZE = 512;

    private static final int AXLES = 3;
    private static final float AXLE_FRONT_Z = -30.0F;
    private static final float AXLE_PITCH = 30.0F;
    private static final int MISSILE_TUBES = 4;
    private static final int BULL_BAR_BARS = 4;
    private static final int ROOF_HATCHES = 2;

    private final ModelPart root;
    private final ModelPart turret;

    public InfantryFightingVehicleModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.turret = body.getChild("turret");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        addHull(body);
        addRunningGear(body);
        addFittings(body);
        addRoofStation(body);

        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static void addHull(PartDefinition body) {
        body.addOrReplaceChild("hull_lower", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 10.0F, -46.0F, 48.0F, 16.0F, 92.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_upper", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 26.0F, -42.0F, 48.0F, 10.0F, 86.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("bow", CubeListBuilder.create()
                .texOffs(128, 0).addBox(-24.0F, 12.0F, -54.0F, 48.0F, 14.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("windscreen", CubeListBuilder.create()
                .texOffs(320, 192).addBox(-20.0F, 30.0F, -44.0F, 40.0F, 10.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("windscreen_frame", CubeListBuilder.create()
                .texOffs(96, 192).addBox(-21.0F, 40.0F, -45.0F, 42.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("rear_doors", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-22.0F, 12.0F, 44.0F, 44.0F, 20.0F, 4.0F)
                .texOffs(96, 192).addBox(-1.0F, 12.0F, 48.0F, 2.0F, 20.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    private static void addRunningGear(PartDefinition body) {
        for (int axle = 0; axle < AXLES; axle++) {
            float z = AXLE_FRONT_Z + axle * AXLE_PITCH - 7.0F;
            body.addOrReplaceChild("wheel_left_" + axle, CubeListBuilder.create()
                    .texOffs(320, 0).addBox(-30.0F, 0.0F, z, 5.0F, 14.0F, 14.0F)
                    .texOffs(0, 192).addBox(-31.0F, 5.0F, z + 6.0F, 1.0F, 4.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("wheel_right_" + axle, CubeListBuilder.create()
                    .texOffs(320, 0).addBox(25.0F, 0.0F, z, 5.0F, 14.0F, 14.0F)
                    .texOffs(0, 192).addBox(30.0F, 5.0F, z + 6.0F, 1.0F, 4.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        body.addOrReplaceChild("bull_bar", CubeListBuilder.create()
                .texOffs(64, 192).addBox(-22.0F, 0.0F, -60.0F, 44.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int bar = 0; bar < BULL_BAR_BARS; bar++) {
            float x = -18.0F + bar * 12.0F;
            body.addOrReplaceChild("bull_bar_bar_" + bar, CubeListBuilder.create()
                    .texOffs(64, 192).addBox(x, 0.0F, -60.5F, 2.0F, 18.0F, 1.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
    }

    private static void addFittings(PartDefinition body) {
        float[] stowageZ = {-30.0F, -10.0F, 10.0F};
        for (int box = 0; box < stowageZ.length; box++) {
            body.addOrReplaceChild("stowage_left_" + box, CubeListBuilder.create()
                    .texOffs(0, 128).addBox(-28.0F, 14.0F, stowageZ[box], 4.0F, 12.0F, 16.0F)
                    .texOffs(0, 192).addBox(-29.0F, 16.0F, stowageZ[box] + 2.0F, 1.0F, 8.0F, 1.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("stowage_right_" + box, CubeListBuilder.create()
                    .texOffs(0, 128).addBox(24.0F, 14.0F, stowageZ[box], 4.0F, 12.0F, 16.0F)
                    .texOffs(0, 192).addBox(28.0F, 16.0F, stowageZ[box] + 2.0F, 1.0F, 8.0F, 1.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        body.addOrReplaceChild("side_window_left", CubeListBuilder.create()
                .texOffs(320, 192).addBox(-25.0F, 30.0F, -42.0F, 1.0F, 10.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("side_window_right", CubeListBuilder.create()
                .texOffs(320, 192).addBox(24.0F, 30.0F, -42.0F, 1.0F, 10.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mirror_left", CubeListBuilder.create()
                .texOffs(0, 192).addBox(-30.0F, 36.0F, -44.0F, 4.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mirror_right", CubeListBuilder.create()
                .texOffs(0, 192).addBox(26.0F, 36.0F, -44.0F, 4.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("headlight_left", CubeListBuilder.create()
                .texOffs(448, 128).addBox(-22.0F, 16.0F, -58.0F, 6.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("headlight_right", CubeListBuilder.create()
                .texOffs(448, 128).addBox(16.0F, 16.0F, -58.0F, 6.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int hatch = 0; hatch < ROOF_HATCHES; hatch++) {
            float x = hatch == 0 ? -16.0F : 4.0F;
            body.addOrReplaceChild("roof_hatch_" + hatch, CubeListBuilder.create()
                    .texOffs(160, 64).addBox(x, 36.0F, 8.0F, 12.0F, 2.0F, 12.0F)
                    .texOffs(96, 192).addBox(x + 1.0F, 38.0F, 9.0F, 2.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        body.addOrReplaceChild("engine_grille", CubeListBuilder.create()
                .texOffs(320, 32).addBox(-20.0F, 36.0F, 30.0F, 40.0F, 1.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("handle_left", CubeListBuilder.create()
                .texOffs(96, 192).addBox(-10.0F, 18.0F, 48.0F, 4.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("handle_right", CubeListBuilder.create()
                .texOffs(96, 192).addBox(6.0F, 18.0F, 48.0F, 4.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("antenna", CubeListBuilder.create()
                .texOffs(128, 192).addBox(-14.0F, 36.0F, 28.0F, 1.0F, 16.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    private static void addRoofStation(PartDefinition body) {
        PartDefinition turret = body.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-14.0F, 0.0F, -14.0F, 28.0F, 10.0F, 30.0F),
                PartPose.offset(0.0F, 36.0F, 0.0F));
        PartDefinition missileBox = turret.addOrReplaceChild("missile_box", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-20.0F, 8.0F, -4.0F, 10.0F, 18.0F, 19.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int tube = 0; tube < MISSILE_TUBES; tube++) {
            float x = -19.0F + (tube % 2) * 5.0F;
            float y = 11.0F + (tube / 2) * 8.0F;
            missileBox.addOrReplaceChild("tube_" + tube, CubeListBuilder.create()
                    .texOffs(384, 192).addBox(x, y, -6.0F, 4.0F, 6.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        turret.addOrReplaceChild("rws_base", CubeListBuilder.create()
                .texOffs(128, 64).addBox(4.0F, 10.0F, -10.0F, 12.0F, 10.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("rws_gun", CubeListBuilder.create()
                .texOffs(192, 256).addBox(8.0F, 14.0F, -30.0F, 4.0F, 4.0F, 20.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("rws_ammo", CubeListBuilder.create()
                .texOffs(224, 256).addBox(4.0F, 14.0F, 6.0F, 8.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sight", CubeListBuilder.create()
                .texOffs(320, 192).addBox(-6.0F, 10.0F, -22.0F, 12.0F, 6.0F, 8.0F)
                .texOffs(320, 192).addBox(-2.0F, 12.0F, -23.0F, 4.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    @Override
    public void setupAnim(InfantryFightingVehicleEntity entity, float limbSwing,
                          float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch) {
        turret.yRot = ApacheModelMath.turretYawRadians(
                entity.getTurretYawDeg(), entity.getYRot());
        turret.xRot = ApacheModelMath.turretPitchRadians(
                entity.getTurretPitchDeg(), entity.getXRot());
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue,
                               float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
