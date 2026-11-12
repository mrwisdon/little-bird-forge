package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.MainBattleTankEntity;
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
 * M1A5 main battle tank, generated from the Blender source model.
 *
 * <p>GENERATED FILE - do not edit by hand. Rebuild with
 * tools/generate_ground_unit_model.py, which reads tools/data/m1a5_boxes.json
 * (exported from blender_m1a5/m1a5.blend by export_mc.py). The Blender model is
 * real M1A2 scale: hull 126.9 units, overall width 58.6 units, 16 units per
 * block, nose along -Z, +X to the vehicle's right. Unit space is unchanged from
 * the rest of the pack; the turret ring and barrel reach are mirrored in
 * MainBattleTankGeometry so the server-side muzzle and the crew seats line up.
 */
public final class MainBattleTankModel extends EntityModel<MainBattleTankEntity> {
    public static final ModelLayerLocation MAIN_BATTLE_TANK_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "main_battle_tank"), "main");
    private static final int TEXTURE_SIZE = 512;
    private static final float DEG = (float) (Math.PI / 180.0D);
    /** Turret ring height, matching MainBattleTankGeometry.TURRET_PIVOT_HEIGHT_UNITS. */
    private static final float TURRET_Y = 23.04F;
    /** Gun trunnion, turret-local: matching MainBattleTankGeometry.GUN_TRUNNION_*. */
    private static final float GUN_TRUNNION_Y = 8.32F;
    private static final float GUN_TRUNNION_Z = -30.72F;

    private final ModelPart root;
    private final ModelPart turret;
    private final ModelPart gun;

    public MainBattleTankModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.turret = body.getChild("turret");
        this.gun = this.turret.getChild("gun");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition turret = body.addOrReplaceChild("turret", CubeListBuilder.create(),
                PartPose.offset(0.0F, TURRET_Y, 0.0F));
        PartDefinition gun = turret.addOrReplaceChild("gun", CubeListBuilder.create(),
                PartPose.offset(0.0F, GUN_TRUNNION_Y, GUN_TRUNNION_Z));

        // ---- body (316 parts) ----
        body.addOrReplaceChild("arm_l1", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-19.52F, 6.56F, -41.92F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_l2", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-19.52F, 6.56F, -28.32F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_l3", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-19.52F, 6.56F, -14.72F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_l4", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-19.52F, 6.56F, -1.12F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_l5", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-19.52F, 6.56F, 12.48F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_l6", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-19.52F, 6.56F, 26.08F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_l7", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-19.52F, 6.56F, 39.68F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_r1", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.6F, 6.56F, -41.92F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_r2", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.6F, 6.56F, -28.32F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_r3", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.6F, 6.56F, -14.72F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_r4", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.6F, 6.56F, -1.12F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_r5", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.6F, 6.56F, 12.48F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_r6", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.6F, 6.56F, 26.08F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("arm_r7", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.6F, 6.56F, 39.68F, 1.92F, 5.44F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deckframe", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-23.2F, 22.88F, 26.0F, 46.4F, 0.96F, 32.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("decklouverg1_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.68F, 23.44F, 30.24F, 18.4F, 0.8F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("decklouverg1_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.68F, 23.44F, 36.96F, 18.4F, 0.8F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("decklouverg1_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.68F, 23.44F, 43.68F, 18.4F, 0.8F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("decklouverg1_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.68F, 23.44F, 50.4F, 18.4F, 0.8F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("decklouverg2_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(3.28F, 23.44F, 30.24F, 18.4F, 0.8F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("decklouverg2_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(3.28F, 23.44F, 36.96F, 18.4F, 0.8F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("decklouverg2_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(3.28F, 23.44F, 43.68F, 18.4F, 0.8F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("decklouverg2_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(3.28F, 23.44F, 50.4F, 18.4F, 0.8F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("driverguard", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-14.08F, 23.52F, -44.64F, 10.56F, 1.6F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("driverhatch", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-13.76F, 22.96F, -43.36F, 9.92F, 1.12F, 11.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("exhaust", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.0F, 19.36F, 49.6F, 5.44F, 4.16F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fenderrail_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.68F, 22.24F, -57.6F, 1.6F, 0.96F, 108.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fenderrail_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(26.08F, 22.24F, -57.6F, 1.6F, 0.96F, 108.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("frontflap", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.76F, 5.6F, -62.32F, 11.52F, 5.44F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fuelcap_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-15.84F, 23.52F, 47.36F, 4.48F, 0.96F, 4.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fuelcap_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(11.36F, 23.52F, 47.36F, 4.48F, 0.96F, 4.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glacisera1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.6F, 21.28F, -52.16F, 9.6F, 2.24F, 6.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glacisera2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.4F, 21.28F, -52.16F, 9.6F, 2.24F, 6.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glacisera3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.8F, 21.28F, -52.16F, 9.6F, 2.24F, 6.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glacisera4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(12.0F, 21.28F, -52.16F, 9.6F, 2.24F, 6.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("grabbar_l1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.48F, 20.8F, -28.32F, 0.64F, 0.64F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("grabbar_l2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.48F, 20.8F, 2.08F, 0.64F, 0.64F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("grabbar_l3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.48F, 20.8F, 32.48F, 0.64F, 0.64F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("grabbar_r1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.84F, 20.8F, -28.32F, 0.64F, 0.64F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("grabbar_r2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.84F, 20.8F, 2.08F, 0.64F, 0.64F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("grabbar_r3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.84F, 20.8F, 32.48F, 0.64F, 0.64F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("headlight_l", CubeListBuilder.create()
                .texOffs(256, 384).addBox(-24.96F, 15.68F, -54.32F, 6.72F, 1.6F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("headlight_r", CubeListBuilder.create()
                .texOffs(256, 384).addBox(18.24F, 15.68F, -54.32F, 6.72F, 1.6F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hulllink_l1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.48F, 15.52F, -22.08F, 0.96F, 5.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hulllink_l2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.48F, 15.52F, -18.56F, 0.96F, 5.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hulllink_l3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.48F, 15.52F, -15.04F, 0.96F, 5.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hulllink_l4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.48F, 15.52F, -11.52F, 0.96F, 5.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hulllink_r1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.52F, 15.52F, -22.08F, 0.96F, 5.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hulllink_r2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.52F, 15.52F, -18.56F, 0.96F, 5.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hulllink_r3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.52F, 15.52F, -15.04F, 0.96F, 5.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hulllink_r4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.52F, 15.52F, -11.52F, 0.96F, 5.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idlerhub_l", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-25.44F, 6.086F, -56.96F, 3.52F, 3.348F, 3.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idlerhub_r", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.92F, 6.086F, -56.96F, 3.52F, 3.348F, 3.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_l", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.12F, 1.36F, -61.6F, 2.88F, 12.8F, 12.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_r", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.24F, 1.36F, -61.6F, 2.88F, 12.8F, 12.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lowerera1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.6F, 10.24F, -62.32F, 9.6F, 2.56F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lowerera2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.4F, 10.24F, -62.32F, 9.6F, 2.56F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lowerera3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.8F, 10.24F, -62.32F, 9.6F, 2.56F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lowerera4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(12.0F, 10.24F, -62.32F, 9.6F, 2.56F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mudflapf_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.64F, 5.52F, -64.16F, 5.44F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mudflapf_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(23.2F, 5.52F, -64.16F, 5.44F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mudflapr_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.64F, 6.56F, 62.32F, 5.44F, 8.0F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mudflapr_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(23.2F, 6.56F, 62.32F, 5.44F, 8.0F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("rearplate", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-18.4F, 9.6F, 62.32F, 36.8F, 14.4F, 1.12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_l1", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.44F, 2.53F, -44.08F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_l2", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.44F, 2.53F, -30.48F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_l3", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.44F, 2.53F, -16.88F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_l4", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.44F, 2.53F, -3.28F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_l5", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.44F, 2.53F, 10.32F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_l6", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.44F, 2.53F, 23.92F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_l7", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.44F, 2.53F, 37.52F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_r1", CubeListBuilder.create()
                .texOffs(256, 160).addBox(21.92F, 2.53F, -44.08F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_r2", CubeListBuilder.create()
                .texOffs(256, 160).addBox(21.92F, 2.53F, -30.48F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_r3", CubeListBuilder.create()
                .texOffs(256, 160).addBox(21.92F, 2.53F, -16.88F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_r4", CubeListBuilder.create()
                .texOffs(256, 160).addBox(21.92F, 2.53F, -3.28F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_r5", CubeListBuilder.create()
                .texOffs(256, 160).addBox(21.92F, 2.53F, 10.32F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_r6", CubeListBuilder.create()
                .texOffs(256, 160).addBox(21.92F, 2.53F, 23.92F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheelrim_r7", CubeListBuilder.create()
                .texOffs(256, 160).addBox(21.92F, 2.53F, 37.52F, 3.52F, 6.46F, 6.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_l1", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.12F, 0.48F, -46.08F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_l2", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.12F, 0.48F, -32.48F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_l3", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.12F, 0.48F, -18.88F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_l4", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.12F, 0.48F, -5.28F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_l5", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.12F, 0.48F, 8.32F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_l6", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.12F, 0.48F, 21.92F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_l7", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-25.12F, 0.48F, 35.52F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_r1", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.24F, 0.48F, -46.08F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_r2", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.24F, 0.48F, -32.48F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_r3", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.24F, 0.48F, -18.88F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_r4", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.24F, 0.48F, -5.28F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_r5", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.24F, 0.48F, 8.32F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_r6", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.24F, 0.48F, 21.92F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roadwheeltire_r7", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.24F, 0.48F, 35.52F, 2.88F, 10.56F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l1_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 15.36F, -48.96F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l1_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 15.36F, -43.813F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l1_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 15.36F, -38.667F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l1_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 18.88F, -48.96F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l1_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 18.88F, -43.813F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l1_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 18.88F, -38.667F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l2_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 12.8F, -32.08F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l2_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 12.8F, -26.933F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l2_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 12.8F, -21.787F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l2_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 17.6F, -32.08F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l2_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 17.6F, -26.933F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l2_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 17.6F, -21.787F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l3_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 11.2F, -15.2F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l3_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 11.2F, -10.053F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l3_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 11.2F, -4.907F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l3_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 16.8F, -15.2F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l3_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 16.8F, -10.053F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l3_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 16.8F, -4.907F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l4_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 11.2F, 1.68F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l4_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 11.2F, 6.827F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l4_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 11.2F, 11.973F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l4_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 16.8F, 1.68F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l4_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 16.8F, 6.827F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l4_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 16.8F, 11.973F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l5_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 12.48F, 18.56F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l5_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 12.48F, 23.707F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l5_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 12.48F, 28.853F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l5_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 17.44F, 18.56F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l5_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 17.44F, 23.707F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l5_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 17.44F, 28.853F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l6_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 15.68F, 35.44F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l6_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 15.68F, 40.587F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l6_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 15.68F, 45.733F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l6_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 19.04F, 35.44F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l6_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 19.04F, 40.587F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_l6_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 19.04F, 45.733F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r1_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 15.36F, -48.96F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r1_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 15.36F, -43.813F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r1_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 15.36F, -38.667F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r1_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 18.88F, -48.96F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r1_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 18.88F, -43.813F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r1_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 18.88F, -38.667F, 0.8F, 3.2F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r2_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 12.8F, -32.08F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r2_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 12.8F, -26.933F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r2_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 12.8F, -21.787F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r2_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 17.6F, -32.08F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r2_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 17.6F, -26.933F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r2_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 17.6F, -21.787F, 0.8F, 4.48F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r3_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 11.2F, -15.2F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r3_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 11.2F, -10.053F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r3_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 11.2F, -4.907F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r3_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 16.8F, -15.2F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r3_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 16.8F, -10.053F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r3_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 16.8F, -4.907F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r4_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 11.2F, 1.68F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r4_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 11.2F, 6.827F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r4_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 11.2F, 11.973F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r4_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 16.8F, 1.68F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r4_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 16.8F, 6.827F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r4_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 16.8F, 11.973F, 0.8F, 5.28F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r5_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 12.48F, 18.56F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r5_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 12.48F, 23.707F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r5_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 12.48F, 28.853F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r5_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 17.44F, 18.56F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r5_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 17.44F, 23.707F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r5_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 17.44F, 28.853F, 0.8F, 4.64F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r6_00", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 15.68F, 35.44F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r6_01", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 15.68F, 40.587F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r6_02", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 15.68F, 45.733F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r6_10", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 19.04F, 35.44F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r6_11", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 19.04F, 40.587F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirtera_r6_12", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.48F, 19.04F, 45.733F, 0.8F, 3.04F, 4.827F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l1", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-28.48F, 14.72F, -49.6F, 0.96F, 8.0F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l2", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-28.48F, 12.16F, -32.72F, 0.96F, 10.56F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l3", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-28.48F, 10.56F, -15.84F, 0.96F, 12.16F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l4", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-28.48F, 10.56F, 1.04F, 0.96F, 12.16F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l5", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-28.48F, 11.84F, 17.92F, 0.96F, 10.88F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l6", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-28.48F, 15.04F, 34.8F, 0.96F, 7.68F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r1", CubeListBuilder.create()
                .texOffs(448, 320).addBox(27.52F, 14.72F, -49.6F, 0.96F, 8.0F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r2", CubeListBuilder.create()
                .texOffs(448, 320).addBox(27.52F, 12.16F, -32.72F, 0.96F, 10.56F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r3", CubeListBuilder.create()
                .texOffs(448, 320).addBox(27.52F, 10.56F, -15.84F, 0.96F, 12.16F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r4", CubeListBuilder.create()
                .texOffs(448, 320).addBox(27.52F, 10.56F, 1.04F, 0.96F, 12.16F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r5", CubeListBuilder.create()
                .texOffs(448, 320).addBox(27.52F, 11.84F, 17.92F, 0.96F, 10.88F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r6", CubeListBuilder.create()
                .texOffs(448, 320).addBox(27.52F, 15.04F, 34.8F, 0.96F, 7.68F, 16.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-25.28F, 2.256F, 49.696F, 3.2F, 11.008F, 11.008F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.08F, 2.256F, 49.696F, 3.2F, 11.008F, 11.008F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("taillighta_l", CubeListBuilder.create()
                .texOffs(256, 384).addBox(-15.04F, 19.52F, 62.16F, 2.88F, 2.56F, 1.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("taillighta_r", CubeListBuilder.create()
                .texOffs(256, 384).addBox(12.16F, 19.52F, 62.16F, 2.88F, 2.56F, 1.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("taillightb_l", CubeListBuilder.create()
                .texOffs(256, 384).addBox(-15.04F, 16.48F, 62.16F, 2.88F, 2.24F, 1.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("taillightb_r", CubeListBuilder.create()
                .texOffs(256, 384).addBox(12.16F, 16.48F, 62.16F, 2.88F, 2.24F, 1.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("taillight_l", CubeListBuilder.create()
                .texOffs(256, 384).addBox(-14.88F, 18.88F, 61.68F, 2.56F, 3.2F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("taillight_r", CubeListBuilder.create()
                .texOffs(256, 384).addBox(12.32F, 18.88F, 61.68F, 2.56F, 3.2F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_l1", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-18.88F, 7.52F, -42.24F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_l2", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-18.88F, 7.52F, -28.64F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_l3", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-18.88F, 7.52F, -15.04F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_l4", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-18.88F, 7.52F, -1.44F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_l5", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-18.88F, 7.52F, 12.16F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_l6", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-18.88F, 7.52F, 25.76F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_l7", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-18.88F, 7.52F, 39.36F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_r1", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.28F, 7.52F, -42.24F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_r2", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.28F, 7.52F, -28.64F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_r3", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.28F, 7.52F, -15.04F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_r4", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.28F, 7.52F, -1.44F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_r5", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.28F, 7.52F, 12.16F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_r6", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.28F, 7.52F, 25.76F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("torsionbar_r7", CubeListBuilder.create()
                .texOffs(256, 256).addBox(17.28F, 7.52F, 39.36F, 1.6F, 1.6F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("towcable", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.4F, 14.0F, -59.04F, 20.8F, 0.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("towcableloop", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.4F, 10.88F, -60.24F, 0.8F, 3.2F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("towcableside_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.6F, 19.44F, -11.2F, 0.8F, 0.8F, 41.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("towcableside_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(26.8F, 19.44F, -11.2F, 0.8F, 0.8F, 41.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("towhook_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-13.12F, 8.64F, -63.28F, 2.24F, 2.56F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("towhook_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(10.88F, 8.64F, -63.28F, 2.24F, 2.56F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, -56.8F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l10", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, 1.647F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l11", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, 8.141F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l12", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, 14.635F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l13", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, 21.129F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l14", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, 27.623F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l15", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, 34.118F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l16", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, 40.612F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l17", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, 47.106F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l18", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, 53.6F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, -50.306F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, -43.812F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l4", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, -37.318F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l5", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, -30.823F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l6", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, -24.329F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l7", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, -17.835F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l8", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, -11.341F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_l9", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 0.0F, -4.847F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, -56.8F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r10", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, 1.647F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r11", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, 8.141F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r12", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, 14.635F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r13", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, 21.129F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r14", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, 27.623F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r15", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, 34.118F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r16", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, 40.612F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r17", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, 47.106F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r18", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, 53.6F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, -50.306F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, -43.812F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r4", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, -37.318F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r5", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, -30.823F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r6", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, -24.329F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r7", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, -17.835F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r8", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, -11.341F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoebot_r9", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 0.0F, -4.847F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, -56.8F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l10", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, 1.647F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l11", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, 8.141F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l12", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, 14.635F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l13", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, 21.129F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l14", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, 27.623F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l15", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, 34.118F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l16", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, 40.612F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l17", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, 47.106F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l18", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, 53.6F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, -50.306F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, -43.812F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l4", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, -37.318F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l5", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, -30.823F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l6", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, -24.329F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l7", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, -17.835F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l8", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, -11.341F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_l9", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.64F, 14.56F, -4.847F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, -56.8F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r10", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, 1.647F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r11", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, 8.141F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r12", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, 14.635F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r13", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, 21.129F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r14", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, 27.623F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r15", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, 34.118F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r16", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, 40.612F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r17", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, 47.106F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r18", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, 53.6F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, -50.306F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, -43.812F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r4", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, -37.318F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r5", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, -30.823F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r6", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, -24.329F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r7", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, -17.835F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r8", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, -11.341F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trackshoetop_r9", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.72F, 14.56F, -4.847F, 9.92F, 0.96F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("trimvane", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-16.8F, 9.12F, -64.64F, 33.6F, 6.72F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_l1", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-25.76F, 4.4F, -42.16F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_l2", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-25.76F, 4.4F, -28.56F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_l3", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-25.76F, 4.4F, -14.96F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_l4", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-25.76F, 4.4F, -1.36F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_l5", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-25.76F, 4.4F, 12.24F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_l6", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-25.76F, 4.4F, 25.84F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_l7", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-25.76F, 4.4F, 39.44F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_r1", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.6F, 4.4F, -42.16F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_r2", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.6F, 4.4F, -28.56F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_r3", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.6F, 4.4F, -14.96F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_r4", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.6F, 4.4F, -1.36F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_r5", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.6F, 4.4F, 12.24F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_r6", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.6F, 4.4F, 25.84F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheelhub_r7", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.6F, 4.4F, 39.44F, 4.16F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_top_0", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 13.6F, -62.96F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_bot_0", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 0.0F, -62.96F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_top_1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 13.6F, -31.48F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_bot_1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 0.0F, -31.48F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_top_2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 13.6F, 0.0F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_bot_2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 0.0F, 0.0F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_top_3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 13.6F, 31.48F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_bot_3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 0.0F, 31.48F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_front", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 0.0F, -62.96F, 9.6F, 15.52F, 6.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_rear", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-28.48F, 0.0F, 54.2F, 9.6F, 15.52F, 6.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_top_0", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 13.6F, -62.96F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_bot_0", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 0.0F, -62.96F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_top_1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 13.6F, -31.48F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_bot_1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 0.0F, -31.48F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_top_2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 13.6F, 0.0F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_bot_2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 0.0F, 0.0F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_top_3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 13.6F, 31.48F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_bot_3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 0.0F, 31.48F, 9.6F, 1.92F, 31.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_front", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 0.0F, -62.96F, 9.6F, 15.52F, 6.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_rear", CubeListBuilder.create()
                .texOffs(0, 224).addBox(18.88F, 0.0F, 54.2F, 9.6F, 15.52F, 6.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_lower", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 6.88F, -63.44F, 55.04F, 0.0F, 126.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_upper", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 6.88F, -55.44F, 55.04F, 16.16F, 118.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_nose", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 6.88F, -63.44F, 55.04F, 3.0F, 0.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_glacis", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, -2.5F, -19.532F, 55.04F, 5.0F, 21.032F),
                PartPose.offsetAndRotation(0.0F, 23.04F, -55.44F, -63.662F * DEG, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_noseplate", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, -3.0F, -1.5F, 55.04F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 6.88F, -63.44F, 180.0F * DEG, 0.0F, 0.0F));
        // ---- turret (98 parts) ----
        turret.addOrReplaceChild("antennabase1", CubeListBuilder.create()
                .texOffs(0, 160).addBox(-15.2F, 16.0F, 15.52F, 1.6F, 1.92F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antennabase2", CubeListBuilder.create()
                .texOffs(0, 160).addBox(13.6F, 16.0F, 15.52F, 1.6F, 1.92F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarl1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.4F, 1.6F, 22.32F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarl2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.4F, 1.6F, 27.173F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarl3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.4F, 1.6F, 32.027F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarl4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.4F, 1.6F, 36.88F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarr1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-19.44F, 1.6F, 38.8F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarr2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-13.093F, 1.6F, 38.8F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarr3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.747F, 1.6F, 38.8F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarr4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.4F, 1.6F, 38.8F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarr5", CubeListBuilder.create()
                .texOffs(0, 0).addBox(5.947F, 1.6F, 38.8F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarr6", CubeListBuilder.create()
                .texOffs(0, 0).addBox(12.293F, 1.6F, 38.8F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarr7", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.64F, 1.6F, 38.8F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarrt1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.6F, 1.6F, 22.32F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarrt2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.6F, 1.6F, 27.173F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarrt3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.6F, 1.6F, 32.027F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlebarrt4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.6F, 1.6F, 36.88F, 0.8F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlecan", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-17.6F, 2.56F, 22.64F, 4.8F, 5.76F, 3.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlecrate1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-18.96F, 1.92F, 25.2F, 16.8F, 6.4F, 12.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlecrate2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(2.32F, 1.92F, 22.8F, 15.2F, 5.76F, 9.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlefloor", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.0F, 1.2F, 20.8F, 40.0F, 0.8F, 18.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlepack", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.8F, 5.6F, 31.52F, 8.8F, 5.44F, 7.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlerailtop", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.0F, 9.92F, 38.72F, 40.0F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlerear", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.0F, 1.6F, 38.8F, 40.0F, 8.8F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustleroll", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11.6F, 6.72F, 33.2F, 13.6F, 5.12F, 5.12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlesidel", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.4F, 1.6F, 20.8F, 0.8F, 8.8F, 18.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("bustlesider", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.6F, 1.6F, 20.8F, 0.8F, 8.8F, 18.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("citv", CubeListBuilder.create()
                .texOffs(384, 320).addBox(2.24F, 16.16F, 7.04F, 5.76F, 4.48F, 7.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("citvlens", CubeListBuilder.create()
                .texOffs(384, 320).addBox(3.52F, 16.96F, 6.4F, 3.2F, 2.88F, 0.64F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("cupola", CubeListBuilder.create()
                .texOffs(0, 0).addBox(4.238F, 16.0F, -4.32F, 10.084F, 3.84F, 10.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("cupolalid", CubeListBuilder.create()
                .texOffs(0, 0).addBox(4.868F, 19.84F, -3.68F, 8.824F, 0.8F, 8.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("gunshield", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.68F, 3.36F, -28.0F, 15.36F, 9.92F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("gunnersight", CubeListBuilder.create()
                .texOffs(384, 320).addBox(-12.32F, 15.68F, -19.68F, 7.04F, 3.84F, 7.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("gunnersighthead", CubeListBuilder.create()
                .texOffs(384, 320).addBox(-11.2F, 16.48F, -20.32F, 4.8F, 2.88F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("loaderhatch", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-15.127F, 16.0F, -0.8F, 9.454F, 1.12F, 9.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("loadermgbarrel", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-15.936F, 18.624F, -8.8F, 0.512F, 0.512F, 11.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("loadermgbase", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-16.64F, 16.48F, 5.44F, 1.92F, 1.6F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("loadermgbody", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-16.48F, 17.92F, 1.12F, 1.6F, 1.92F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("m240barrel", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-9.844F, 19.264F, -20.4F, 0.487F, 0.512F, 13.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("m240mount", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-10.4F, 17.44F, -3.2F, 1.6F, 1.6F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("m240receiver", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-10.4F, 18.56F, -7.36F, 1.6F, 1.92F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("mantlet", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-8.48F, 3.84F, -34.08F, 16.96F, 8.96F, 6.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("rwsbarrel", CubeListBuilder.create()
                .texOffs(0, 320).addBox(13.425F, 20.448F, -25.6F, 0.67F, 0.704F, 12.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("rwsbase", CubeListBuilder.create()
                .texOffs(0, 320).addBox(11.68F, 16.16F, -8.8F, 4.16F, 4.16F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("rwsbody", CubeListBuilder.create()
                .texOffs(0, 320).addBox(12.16F, 19.2F, -13.12F, 3.2F, 3.2F, 7.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("rwsreceiver", CubeListBuilder.create()
                .texOffs(0, 320).addBox(12.48F, 21.6F, -15.2F, 2.56F, 2.24F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("rail_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.52F, 13.68F, -12.0F, 0.8F, 0.8F, 20.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("rail_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.72F, 13.68F, -12.0F, 0.8F, 0.8F, 20.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smokebase_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-18.72F, 3.68F, -28.64F, 4.8F, 4.16F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smokebase_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(13.92F, 3.68F, -28.64F, 4.8F, 4.16F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l00", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12.88F, 3.44F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l01", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-14.48F, 3.44F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l10", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12.88F, 5.2F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l11", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-14.48F, 5.2F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l20", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12.88F, 6.96F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l21", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-14.48F, 6.96F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r00", CubeListBuilder.create()
                .texOffs(0, 0).addBox(11.44F, 3.44F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r01", CubeListBuilder.create()
                .texOffs(0, 0).addBox(13.04F, 3.44F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r10", CubeListBuilder.create()
                .texOffs(0, 0).addBox(11.44F, 5.2F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r11", CubeListBuilder.create()
                .texOffs(0, 0).addBox(13.04F, 5.2F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r20", CubeListBuilder.create()
                .texOffs(0, 0).addBox(11.44F, 6.96F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r21", CubeListBuilder.create()
                .texOffs(0, 0).addBox(13.04F, 6.96F, -30.08F, 1.44F, 1.44F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sparelink_l1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.04F, 4.96F, -5.2F, 1.12F, 5.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sparelink_l2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.04F, 4.96F, -2.0F, 1.12F, 5.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sparelink_l3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.04F, 4.96F, 1.2F, 1.12F, 5.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sparelink_l4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.04F, 4.96F, 4.4F, 1.12F, 5.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sparelink_r1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.92F, 4.96F, -5.2F, 1.12F, 5.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sparelink_r2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.92F, 4.96F, -2.0F, 1.12F, 5.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sparelink_r3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.92F, 4.96F, 1.2F, 1.12F, 5.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sparelink_r4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.92F, 4.96F, 4.4F, 1.12F, 5.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("stowbag_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.72F, 10.24F, -18.08F, 2.88F, 3.84F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("stowbag_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.84F, 10.24F, -18.08F, 2.88F, 3.84F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("stowboxa_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.56F, 10.72F, 1.12F, 2.56F, 4.16F, 6.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("stowboxa_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.0F, 10.72F, 1.12F, 2.56F, 4.16F, 6.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("stowboxb_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.56F, 10.72F, 9.76F, 2.56F, 3.52F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("stowboxb_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.0F, 10.72F, 9.76F, 2.56F, 3.52F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("stowroll_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-23.114F, 10.4F, -12.0F, 3.348F, 3.52F, 12.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("stowroll_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.766F, 10.4F, -12.0F, 3.348F, 3.52F, 12.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turretera_l1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.88F, 6.56F, -10.4F, 2.24F, 5.44F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turretera_l2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.88F, 6.56F, -0.8F, 2.24F, 5.44F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turretera_l3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.88F, 6.56F, 8.8F, 2.24F, 5.44F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turretera_r1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.64F, 6.56F, -10.4F, 2.24F, 5.44F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turretera_r2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.64F, 6.56F, -0.8F, 2.24F, 5.44F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turretera_r3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.64F, 6.56F, 8.8F, 2.24F, 5.44F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("windsensor", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.88F, 16.0F, 17.44F, 0.96F, 6.72F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-13.6F, 0.0F, -32.8F, 27.2F, 10.56F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-18.8F, 0.0F, -28.8F, 37.6F, 13.12F, 7.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.44F, 0.0F, -21.6F, 42.88F, 16.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.76F, 0.0F, -9.6F, 43.52F, 16.0F, 19.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.76F, 0.0F, 9.6F, 43.52F, 16.0F, 11.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_cheek_R0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.72F, 7.56F),
                PartPose.offsetAndRotation(8.4F, 0.0F, -32.8F, 0.0F, 52.431F * DEG, 0.0F));
        turret.addOrReplaceChild("turret_cheek_R1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 10.56F, 9.881F),
                PartPose.offsetAndRotation(13.6F, 0.0F, -28.8F, 0.0F, 35.838F * DEG, 0.0F));
        turret.addOrReplaceChild("turret_cheek_R2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 13.12F, 13.287F),
                PartPose.offsetAndRotation(18.8F, 0.0F, -21.6F, 0.0F, 12.407F * DEG, 0.0F));
        turret.addOrReplaceChild("turret_cheek_L0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.72F, 7.56F),
                PartPose.offsetAndRotation(-8.4F, 0.0F, -32.8F, 0.0F, -52.431F * DEG, 0.0F));
        turret.addOrReplaceChild("turret_cheek_L1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 10.56F, 9.881F),
                PartPose.offsetAndRotation(-13.6F, 0.0F, -28.8F, 0.0F, -35.838F * DEG, 0.0F));
        turret.addOrReplaceChild("turret_cheek_L2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 13.12F, 13.287F),
                PartPose.offsetAndRotation(-18.8F, 0.0F, -21.6F, 0.0F, -12.407F * DEG, 0.0F));
        turret.addOrReplaceChild("turret_top_front", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-9.4F, -2.0F, -7.045F, 18.8F, 4.0F, 8.545F),
                PartPose.offsetAndRotation(0.0F, 10.56F, -28.8F, -43.831F * DEG, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_top_mid", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-14.6F, -2.0F, -9.142F, 29.2F, 4.0F, 10.642F),
                PartPose.offsetAndRotation(0.0F, 13.12F, -21.6F, -19.573F * DEG, 0.0F, 0.0F));
        // ---- gun (4 parts) ----
        gun.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-0.992F, -0.992F, -62.16F, 1.984F, 1.984F, 34.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("boreevacuator", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-2.048F, -2.048F, -28.88F, 4.096F, 4.096F, 8.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("muzzle", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-1.408F, -1.408F, -62.16F, 2.816F, 2.816F, 4.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("thermalsleeve", CubeListBuilder.create()
                .texOffs(256, 320).addBox(-1.68F, -1.68F, -20.88F, 3.36F, 3.36F, 32.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void setupAnim(MainBattleTankEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float partialTick = ageInTicks - entity.tickCount;
        turret.yRot = ApacheModelMath.turretYawRadians(
                entity.renderTurretYawDeg(partialTick), entity.getYRot());
        // The turret only traverses: elevation is the gun's alone, on its own trunnion.
        turret.xRot = 0.0F;
        gun.xRot = ApacheModelMath.turretPitchRadians(
                entity.renderTurretPitchDeg(partialTick), entity.getXRot());
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue,
                               float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
