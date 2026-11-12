package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.BradleyEntity;
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
 * M2A2 Bradley, generated from the Blender source model.
 *
 * <p>GENERATED FILE - do not edit by hand. Rebuild with tools/generate_bradley_model.py, which
 * reads tools/data/bradley_boxes.json (exported from bradley/out/M2A2_bradley.blend by
 * export_mc_bradley.py). 16 units per block, 1 block = 1 m, so the hull is 104.8 units long and
 * 52.5 over the skirts, matching BradleyGeometry. Nose along -Z, +X to the vehicle's right; the
 * turret parts are relative to the ring, which sits at (0.0F, 24.2F, 9.9F).
 */
public final class BradleyModel extends EntityModel<BradleyEntity> {
    public static final ModelLayerLocation BRADLEY_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "bradley"), "main");
    private static final int TEXTURE_SIZE = 512;
    /** Turret ring, matching BradleyGeometry.TURRET_PIVOT_HEIGHT_UNITS. */
    private static final float TURRET_Y = 24.2F;
    /** Ring offset aft of the hull centre, matching BradleyGeometry.TURRET_PIVOT_FORWARD_UNITS. */
    private static final float TURRET_Z = 9.9F;
    /** Gun trunnion, ring-local: matching BradleyGeometry.GUN_TRUNNION_*. */
    private static final float GUN_TRUNNION_Y = 10.4F;
    private static final float GUN_TRUNNION_Z = -20.5F;

    private final ModelPart root;
    private final ModelPart turret;
    private final ModelPart gun;

    public BradleyModel(ModelPart root) {
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
                PartPose.offset(0.0F, TURRET_Y, TURRET_Z));
        PartDefinition gun = turret.addOrReplaceChild("gun", CubeListBuilder.create(),
                PartPose.offset(0.0F, GUN_TRUNNION_Y, GUN_TRUNNION_Z));

        // ---- body (357 parts) ----
        body.addOrReplaceChild("deck_box1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(15.52F, 26.24F, 16.4F, 6.72F, 4.16F, 18.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_box2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-22.88F, 26.24F, -29.6F, 5.44F, 3.52F, 9.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_hatch0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(4.16F, 26.88F, 32.8F, 11.52F, 0.56F, 12.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_hatch0_h1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(8.64F, 27.2F, 44.08F, 2.56F, 0.96F, 1.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_hatch0_hinge", CubeListBuilder.create()
                .texOffs(181, 0).addBox(7.52F, 27.36F, 41.44F, 4.8F, 0.8F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_hatch1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.68F, 26.88F, 32.8F, 11.52F, 0.56F, 12.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_hatch1_h1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-11.2F, 27.2F, 44.08F, 2.56F, 0.96F, 1.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_hatch1_hinge", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-12.32F, 27.36F, 41.44F, 4.8F, 0.8F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_towbar", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-3.68F, 26.88F, 36.48F, 7.36F, 1.44F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_tracklink0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(14.72F, 26.88F, 46.4F, 3.2F, 0.8F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_tracklink1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(14.72F, 26.88F, 42.88F, 3.2F, 0.8F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_tracklink2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(14.72F, 26.88F, 39.36F, 3.2F, 0.8F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("deck_tracklink3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(14.72F, 26.88F, 35.84F, 3.2F, 0.8F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_block_c0", CubeListBuilder.create()
                .texOffs(106, 0).addBox(3.406F, 39.68F, 0.786F, 2.321F, 1.76F, 1.795F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_block_c1", CubeListBuilder.create()
                .texOffs(106, 0).addBox(0.44F, 39.68F, 3.856F, 1.759F, 1.76F, 2.313F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_block_c2", CubeListBuilder.create()
                .texOffs(106, 0).addBox(0.864F, 39.68F, 8.464F, 2.067F, 1.76F, 2.329F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_block_c3", CubeListBuilder.create()
                .texOffs(106, 0).addBox(4.778F, 39.68F, 11.359F, 2.176F, 1.76F, 1.388F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_block_c4", CubeListBuilder.create()
                .texOffs(106, 0).addBox(9.126F, 39.68F, 9.344F, 2.222F, 1.76F, 2.238F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_block_c5", CubeListBuilder.create()
                .texOffs(106, 0).addBox(11.002F, 39.68F, 4.955F, 1.433F, 1.76F, 2.197F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_block_c6", CubeListBuilder.create()
                .texOffs(106, 0).addBox(8.028F, 39.68F, 1.125F, 2.334F, 1.76F, 2.042F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_d0", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-10.2F, 26.72F, -46.32F, 1.84F, 1.6F, 2.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_d0_gl", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-10.04F, 27.0F, -46.464F, 1.52F, 1.2F, 0.192F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_d1", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-13.72F, 26.72F, -46.32F, 1.84F, 1.6F, 2.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_d1_gl", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-13.56F, 27.0F, -46.464F, 1.52F, 1.2F, 0.192F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_d2", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-17.24F, 26.72F, -46.32F, 1.84F, 1.6F, 2.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_d2_gl", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-17.08F, 27.0F, -46.464F, 1.52F, 1.2F, 0.192F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_r0", CubeListBuilder.create()
                .texOffs(106, 0).addBox(5.12F, 27.76F, 49.12F, 2.56F, 1.44F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_r0_gl", CubeListBuilder.create()
                .texOffs(106, 0).addBox(5.36F, 27.92F, 50.704F, 2.08F, 1.12F, 0.192F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_r1", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-7.68F, 27.76F, 49.12F, 2.56F, 1.44F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_peri_r1_gl", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-7.44F, 27.92F, 50.704F, 2.08F, 1.12F, 0.192F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glass_sight_g", CubeListBuilder.create()
                .texOffs(106, 0).addBox(4.16F, 40.504F, -6.717F, 5.12F, 4.112F, 2.874F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-26.0F, 7.36F, -52.4F, 52.0F, 10.56F, 3.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_m02", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-26.24F, 6.88F, -39.2F, 52.48F, 20.0F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_m03", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-25.28F, 6.88F, -24.0F, 50.56F, 20.0F, 60.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_m04", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-25.28F, 6.88F, 48.0F, 50.56F, 19.2F, 3.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_bolts_l_a_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-25.472F, 25.072F, -41.008F, 0.192F, 0.416F, 86.016F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_bolts_l_b_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-25.472F, 20.592F, -41.008F, 0.192F, 0.416F, 86.016F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_bolts_l_c_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-16.832F, 10.992F, -17.808F, 0.192F, 0.416F, 62.816F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_bolts_r_a_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(25.28F, 25.072F, -41.008F, 0.192F, 0.416F, 86.016F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_bolts_r_b_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(25.28F, 20.592F, -41.008F, 0.192F, 0.416F, 86.016F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_bolts_r_c_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(16.64F, 10.992F, -17.808F, 0.192F, 0.416F, 62.816F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_bumpstrip", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-21.28F, 17.36F, -49.36F, 42.56F, 1.76F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-6.245F, 26.72F, -6.4F, 12.491F, 0.96F, 1.726F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m01", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-9.935F, 26.72F, -4.298F, 19.87F, 0.96F, 1.471F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m02", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-11.54F, 26.72F, -1.758F, 23.08F, 0.96F, 1.27F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m03", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-14.134F, 26.72F, -0.015F, 28.267F, 0.96F, 1.775F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m04", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.078F, 26.72F, 1.76F, 30.155F, 0.96F, 1.915F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m05", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.764F, 26.72F, 4.287F, 31.528F, 0.96F, 1.409F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m06", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-16.32F, 26.72F, 6.11F, 32.64F, 0.96F, 7.62F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m07", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.764F, 26.72F, 14.144F, 31.528F, 0.96F, 1.409F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m08", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.078F, 26.72F, 16.165F, 30.155F, 0.96F, 1.915F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m09", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-14.134F, 26.72F, 18.08F, 28.267F, 0.96F, 1.775F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m10", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-11.54F, 26.72F, 20.329F, 23.08F, 0.96F, 1.27F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m11", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-9.935F, 26.72F, 22.668F, 19.87F, 0.96F, 1.471F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck_ring_m12", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-6.245F, 26.72F, 24.514F, 12.491F, 0.96F, 1.726F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_fender_l", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-26.16F, 13.44F, -48.8F, 0.48F, 4.8F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_fender_r", CubeListBuilder.create()
                .texOffs(181, 0).addBox(25.68F, 13.44F, -48.8F, 0.48F, 4.8F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_l0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-27.42F, 23.776F, -35.264F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_l1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-27.42F, 23.776F, -22.464F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_l2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-27.42F, 23.776F, -9.664F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_l3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-27.42F, 23.776F, 11.136F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_l4", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-27.42F, 23.776F, 23.936F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_r0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.32F, 23.776F, -35.264F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_r1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.32F, 23.776F, -22.464F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_r2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.32F, 23.776F, -9.664F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_r3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.32F, 23.776F, 11.136F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_handle_r4", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.32F, 23.776F, 23.936F, 1.1F, 0.448F, 4.928F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_hatch_driver", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-17.36F, 26.888F, -44.88F, 9.12F, 0.88F, 9.12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_hatch_driver_ring", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-17.84F, 26.848F, -45.36F, 10.08F, 0.448F, 10.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_jbox_l0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-27.04F, 21.68F, -28.64F, 0.8F, 2.08F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_jbox_l1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-27.04F, 21.68F, 0.16F, 0.8F, 2.08F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_jbox_l2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-27.04F, 21.68F, 21.76F, 0.8F, 2.08F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_jbox_r0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.24F, 21.68F, -28.64F, 0.8F, 2.08F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_jbox_r1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.24F, 21.68F, 0.16F, 0.8F, 2.08F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_jbox_r2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.24F, 21.68F, 21.76F, 0.8F, 2.08F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_lift0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-23.28F, 26.72F, -36.48F, 1.12F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_lift1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(22.16F, 26.72F, -36.48F, 1.12F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_lift2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-24.56F, 26.72F, 3.52F, 1.12F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_lift3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(23.44F, 26.72F, 3.52F, 1.12F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_pintle", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-1.6F, 15.68F, 50.24F, 3.2F, 3.84F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_pintle_ring", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-0.72F, 16.4F, 51.138F, 1.44F, 2.4F, 2.364F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-12.8F, 8.32F, 51.44F, 25.6F, 17.6F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_frame0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.68F, 6.4F, 51.44F, 2.88F, 20.8F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_frame1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(12.8F, 6.4F, 51.44F, 2.88F, 20.8F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_frame2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.68F, 25.92F, 51.44F, 31.36F, 1.28F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_frame3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.68F, 6.4F, 51.44F, 31.36F, 1.92F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_hinge-55", CubeListBuilder.create()
                .texOffs(181, 0).addBox(7.2F, 9.68F, 51.44F, 3.2F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_hinge55", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-10.4F, 9.68F, 51.44F, 3.2F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_latch", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-1.92F, 24.64F, 51.824F, 3.84F, 2.56F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_rib0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(9.04F, 11.2F, 51.92F, 1.12F, 17.28F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_rib1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(4.24F, 11.2F, 51.92F, 1.12F, 17.28F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_rib2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-0.56F, 11.2F, 51.92F, 1.12F, 17.28F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_rib3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-5.36F, 11.2F, 51.92F, 1.12F, 17.28F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_rib4", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-10.16F, 11.2F, 51.92F, 1.12F, 17.28F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_ribh1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-12.48F, 27.92F, 51.92F, 24.96F, 1.12F, 0.448F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_ramp_ribh2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-12.48F, 10.64F, 51.92F, 24.96F, 1.12F, 0.448F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_rubrail2_l_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-25.96F, 19.68F, -44.8F, 0.72F, 0.96F, 86.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_rubrail2_r_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(25.24F, 19.68F, -44.8F, 0.72F, 0.96F, 86.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_rubrail_l_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-26.032F, 25.16F, -44.8F, 0.8F, 1.2F, 86.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_rubrail_r_m00", CubeListBuilder.create()
                .texOffs(181, 0).addBox(25.232F, 25.16F, -44.8F, 0.8F, 1.2F, 86.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_step_l", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-23.2F, 9.52F, 44.8F, 4.8F, 0.8F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_step_r", CubeListBuilder.create()
                .texOffs(181, 0).addBox(18.4F, 9.52F, 44.8F, 4.8F, 0.8F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_tiedown_l0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-26.672F, 27.071F, -37.742F, 0.384F, 1.201F, 1.884F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_tiedown_l1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-26.672F, 27.071F, -18.542F, 0.384F, 1.201F, 1.884F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_tiedown_l2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-26.672F, 27.071F, 3.858F, 0.384F, 1.201F, 1.884F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_tiedown_l3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-26.672F, 27.071F, 29.458F, 0.384F, 1.201F, 1.884F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_tiedown_r0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.288F, 27.071F, -37.742F, 0.384F, 1.201F, 1.884F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_tiedown_r1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.288F, 27.071F, -18.542F, 0.384F, 1.201F, 1.884F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_tiedown_r2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.288F, 27.071F, 3.858F, 0.384F, 1.201F, 1.884F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_tiedown_r3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(26.288F, 27.071F, 29.458F, 0.384F, 1.201F, 1.884F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_toweye-92", CubeListBuilder.create()
                .texOffs(181, 0).addBox(14.0F, 20.56F, -51.84F, 1.44F, 1.76F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_toweye92", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.44F, 20.56F, -51.84F, 1.44F, 1.76F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_toweye_ring-92", CubeListBuilder.create()
                .texOffs(181, 0).addBox(14.0F, 20.56F, -52.387F, 1.44F, 1.76F, 1.733F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_toweye_ring92", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.44F, 20.56F, -52.387F, 1.44F, 1.76F, 1.733F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_trimvane", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-18.08F, 14.594F, -50.301F, 36.16F, 8.366F, 7.792F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_trimvane_rib0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(14.6F, 15.276F, -50.456F, 1.52F, 7.765F, 7.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_trimvane_rib1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(9.48F, 15.276F, -50.456F, 1.52F, 7.765F, 7.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_trimvane_rib2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(4.36F, 15.276F, -50.456F, 1.52F, 7.765F, 7.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_trimvane_rib3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-0.76F, 15.276F, -50.456F, 1.52F, 7.765F, 7.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_trimvane_rib4", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-5.88F, 15.276F, -50.456F, 1.52F, 7.765F, 7.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_trimvane_rib5", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-11.0F, 15.276F, -50.456F, 1.52F, 7.765F, 7.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_trimvane_rib6", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-16.12F, 15.276F, -50.456F, 1.52F, 7.765F, 7.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_l_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-23.04F, 6.12F, 32.4F, 3.68F, 5.36F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_l_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-23.04F, 11.48F, 35.08F, 3.68F, 2.68F, 5.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_l_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-23.04F, 3.44F, 35.08F, 3.68F, 2.68F, 5.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_l_axle", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-26.0F, 7.84F, 36.8F, 9.6F, 1.92F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_l_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-23.6F, 7.28F, 36.24F, 4.8F, 3.04F, 3.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_l_tension", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-17.52F, 7.68F, 33.44F, 1.6F, 2.24F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_r_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.36F, 6.12F, 32.4F, 3.68F, 5.36F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_r_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.36F, 11.48F, 35.08F, 3.68F, 2.68F, 5.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_r_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.36F, 3.44F, 35.08F, 3.68F, 2.68F, 5.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_r_axle", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.4F, 7.84F, 36.8F, 9.6F, 1.92F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_r_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(18.8F, 7.28F, 36.24F, 4.8F, 3.04F, 3.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_r_tension", CubeListBuilder.create()
                .texOffs(409, 0).addBox(15.92F, 7.68F, 33.44F, 1.6F, 2.24F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_base_l", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-24.0F, 15.465F, -50.349F, 6.4F, 6.746F, 6.472F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_base_r", CubeListBuilder.create()
                .texOffs(106, 0).addBox(17.6F, 15.465F, -50.349F, 6.4F, 6.746F, 6.472F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_guard_l0", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-17.76F, 17.785F, -49.742F, 0.32F, 3.738F, 3.498F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_guard_l2", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-24.16F, 17.785F, -49.742F, 0.32F, 3.738F, 3.498F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_guard_l_bar", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-24.64F, 19.494F, -48.153F, 7.68F, 0.32F, 0.32F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_guard_r0", CubeListBuilder.create()
                .texOffs(106, 0).addBox(23.84F, 17.785F, -49.742F, 0.32F, 3.738F, 3.498F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_guard_r2", CubeListBuilder.create()
                .texOffs(106, 0).addBox(17.44F, 17.785F, -49.742F, 0.32F, 3.738F, 3.498F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_guard_r_bar", CubeListBuilder.create()
                .texOffs(106, 0).addBox(16.96F, 19.494F, -48.153F, 7.68F, 0.32F, 0.32F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_tail_l", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-25.6F, 23.68F, 49.12F, 2.56F, 3.2F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_tail_l_guard", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-26.08F, 23.2F, 50.8F, 3.52F, 4.16F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_tail_r", CubeListBuilder.create()
                .texOffs(106, 0).addBox(23.04F, 23.68F, 49.12F, 2.56F, 3.2F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lamp_tail_r_guard", CubeListBuilder.create()
                .texOffs(106, 0).addBox(22.56F, 23.2F, 50.8F, 3.52F, 4.16F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lens_tail_l", CubeListBuilder.create()
                .texOffs(106, 0).addBox(-25.312F, 24.303F, 50.56F, 1.984F, 1.954F, 0.64F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("lens_tail_r", CubeListBuilder.create()
                .texOffs(106, 0).addBox(23.328F, 24.303F, 50.56F, 1.984F, 1.954F, 0.64F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_l0", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-22.12F, 12.56F, -27.12F, 1.84F, 3.04F, 3.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_l0_brk", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-21.12F, 13.04F, -26.88F, 4.8F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_l0_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-22.44F, 13.36F, -26.302F, 2.48F, 1.44F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_l1", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-22.12F, 12.56F, -3.12F, 1.84F, 3.04F, 3.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_l1_brk", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-21.12F, 13.04F, -2.88F, 4.8F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_l1_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-22.44F, 13.36F, -2.302F, 2.48F, 1.44F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_l2", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-22.12F, 12.56F, 20.88F, 1.84F, 3.04F, 3.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_l2_brk", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-21.12F, 13.04F, 21.12F, 4.8F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_l2_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-22.44F, 13.36F, 21.698F, 2.48F, 1.44F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_r0", CubeListBuilder.create()
                .texOffs(409, 0).addBox(20.28F, 12.56F, -27.12F, 1.84F, 3.04F, 3.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_r0_brk", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.32F, 13.04F, -26.88F, 4.8F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_r0_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.96F, 13.36F, -26.302F, 2.48F, 1.44F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_r1", CubeListBuilder.create()
                .texOffs(409, 0).addBox(20.28F, 12.56F, -3.12F, 1.84F, 3.04F, 3.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_r1_brk", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.32F, 13.04F, -2.88F, 4.8F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_r1_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.96F, 13.36F, -2.302F, 2.48F, 1.44F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_r2", CubeListBuilder.create()
                .texOffs(409, 0).addBox(20.28F, 12.56F, 20.88F, 1.84F, 3.04F, 3.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_r2_brk", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.32F, 13.04F, 21.12F, 4.8F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roller_r2_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.96F, 13.36F, 21.698F, 2.48F, 1.44F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p1", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.24F, 9.28F, -39.68F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p1_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-27.215F, 14.512F, -36.288F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p1_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.592F, 17.48F, -39.2F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p2", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.24F, 9.28F, -27.68F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p2_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-27.215F, 14.512F, -24.288F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p2_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.592F, 17.48F, -27.2F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p3", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.24F, 9.28F, -15.68F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p3_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-27.215F, 14.512F, -12.288F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p3_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.592F, 17.48F, -15.2F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p4", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.24F, 9.28F, -3.68F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p4_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-27.215F, 14.512F, -0.288F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p4_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.592F, 17.48F, -3.2F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p5", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.24F, 9.28F, 8.32F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p5_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-27.215F, 14.512F, 11.712F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p5_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.592F, 17.48F, 8.8F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p6", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.24F, 9.28F, 20.32F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p6_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-27.215F, 14.512F, 23.712F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_l_p6_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(-26.592F, 17.48F, 20.8F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p1", CubeListBuilder.create()
                .texOffs(450, 0).addBox(25.6F, 9.28F, -39.68F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p1_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.432F, 14.512F, -36.288F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p1_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.24F, 17.48F, -39.2F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p2", CubeListBuilder.create()
                .texOffs(450, 0).addBox(25.6F, 9.28F, -27.68F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p2_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.432F, 14.512F, -24.288F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p2_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.24F, 17.48F, -27.2F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p3", CubeListBuilder.create()
                .texOffs(450, 0).addBox(25.6F, 9.28F, -15.68F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p3_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.432F, 14.512F, -12.288F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p3_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.24F, 17.48F, -15.2F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p4", CubeListBuilder.create()
                .texOffs(450, 0).addBox(25.6F, 9.28F, -3.68F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p4_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.432F, 14.512F, -0.288F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p4_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.24F, 17.48F, -3.2F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p5", CubeListBuilder.create()
                .texOffs(450, 0).addBox(25.6F, 9.28F, 8.32F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p5_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.432F, 14.512F, 11.712F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p5_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.24F, 17.48F, 8.8F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p6", CubeListBuilder.create()
                .texOffs(450, 0).addBox(25.6F, 9.28F, 20.32F, 0.64F, 9.12F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p6_handle", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.432F, 14.512F, 23.712F, 0.783F, 0.416F, 5.216F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("skirt_r_p6_rib", CubeListBuilder.create()
                .texOffs(450, 0).addBox(26.24F, 17.48F, 20.8F, 0.352F, 0.88F, 11.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-23.04F, 7.245F, -49.327F, 3.68F, 4.685F, 9.37F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-23.04F, 11.93F, -46.985F, 3.68F, 2.342F, 4.685F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-23.04F, 4.902F, -46.985F, 3.68F, 2.342F, 4.685F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_final_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-18.32F, 7.68F, -48.48F, 3.84F, 3.84F, 7.68F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_final_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-18.32F, 11.52F, -46.56F, 3.84F, 1.92F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_final_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-18.32F, 5.76F, -46.56F, 3.84F, 1.92F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_flange0_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-19.44F, 7.707F, -48.446F, 1.44F, 3.815F, 7.629F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_flange0_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-19.44F, 11.522F, -46.539F, 1.44F, 1.907F, 3.815F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_flange0_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-19.44F, 5.8F, -46.539F, 1.44F, 1.907F, 3.815F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_flange1_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-24.4F, 7.707F, -48.446F, 1.44F, 3.815F, 7.629F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_flange1_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-24.4F, 11.522F, -46.539F, 1.44F, 1.907F, 3.815F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_flange1_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-24.4F, 5.8F, -46.539F, 1.44F, 1.907F, 3.815F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_hub0", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-20.08F, 7.44F, -46.8F, 0.96F, 4.32F, 4.32F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_hub1", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-23.28F, 7.44F, -46.8F, 0.96F, 4.32F, 4.32F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_l_shaft", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-24.4F, 8.16F, -46.08F, 6.4F, 2.88F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.36F, 7.245F, -49.327F, 3.68F, 4.685F, 9.37F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.36F, 11.93F, -46.985F, 3.68F, 2.342F, 4.685F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.36F, 4.902F, -46.985F, 3.68F, 2.342F, 4.685F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_final_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(14.48F, 7.68F, -48.48F, 3.84F, 3.84F, 7.68F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_final_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(14.48F, 11.52F, -46.56F, 3.84F, 1.92F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_final_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(14.48F, 5.76F, -46.56F, 3.84F, 1.92F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_flange0_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(22.96F, 7.707F, -48.446F, 1.44F, 3.815F, 7.629F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_flange0_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(22.96F, 11.522F, -46.539F, 1.44F, 1.907F, 3.815F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_flange0_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(22.96F, 5.8F, -46.539F, 1.44F, 1.907F, 3.815F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_flange1_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(18.0F, 7.707F, -48.446F, 1.44F, 3.815F, 7.629F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_flange1_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(18.0F, 11.522F, -46.539F, 1.44F, 1.907F, 3.815F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_flange1_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(18.0F, 5.8F, -46.539F, 1.44F, 1.907F, 3.815F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_hub0", CubeListBuilder.create()
                .texOffs(409, 0).addBox(19.12F, 7.44F, -46.8F, 0.96F, 4.32F, 4.32F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_hub1", CubeListBuilder.create()
                .texOffs(409, 0).addBox(22.32F, 7.44F, -46.8F, 0.96F, 4.32F, 4.32F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_r_shaft", CubeListBuilder.create()
                .texOffs(409, 0).addBox(18.0F, 8.16F, -46.08F, 6.4F, 2.88F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_l0", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-21.769F, 4.271F, -37.92F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_l0_damper", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-19.36F, 9.44F, -38.462F, 1.44F, 6.72F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_l1", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-21.769F, 4.271F, -25.12F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_l1_damper", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-19.36F, 9.44F, -25.662F, 1.44F, 6.72F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_l2", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-21.769F, 4.271F, -12.32F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_l3", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-21.769F, 4.271F, 0.48F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_l4", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-21.769F, 4.271F, 13.28F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_l5", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-21.769F, 4.271F, 26.08F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_l5_damper", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-19.36F, 9.44F, 25.538F, 1.44F, 6.72F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_r0", CubeListBuilder.create()
                .texOffs(409, 0).addBox(15.271F, 4.271F, -37.92F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_r0_damper", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.92F, 9.44F, -38.462F, 1.44F, 6.72F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_r1", CubeListBuilder.create()
                .texOffs(409, 0).addBox(15.271F, 4.271F, -25.12F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_r1_damper", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.92F, 9.44F, -25.662F, 1.44F, 6.72F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_r2", CubeListBuilder.create()
                .texOffs(409, 0).addBox(15.271F, 4.271F, -12.32F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_r3", CubeListBuilder.create()
                .texOffs(409, 0).addBox(15.271F, 4.271F, 0.48F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_r4", CubeListBuilder.create()
                .texOffs(409, 0).addBox(15.271F, 4.271F, 13.28F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_r5", CubeListBuilder.create()
                .texOffs(409, 0).addBox(15.271F, 4.271F, 26.08F, 6.498F, 7.058F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("susp_r5_damper", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.92F, 9.44F, 25.538F, 1.44F, 6.72F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tool_pick", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-17.6F, 26.92F, -24.8F, 1.6F, 0.56F, 14.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tool_shovel", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-16.0F, 26.92F, -19.52F, 14.4F, 0.56F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_w00", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 3.26F, -49.992F, 8.528F, 11.922F, 6.703F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo01", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 0.119F, -42.782F, 8.528F, 3.333F, 6.146F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi01", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 14.493F, -42.716F, 8.528F, 1.016F, 5.732F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo02", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, -35.956F, 8.528F, 0.815F, 5.74F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi02", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 14.928F, -36.225F, 8.528F, 1.179F, 5.747F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo03", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, -29.401F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi03", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 15.363F, -29.718F, 8.528F, 1.069F, 6.539F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo04", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, -22.879F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi04", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 15.632F, -22.364F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo05", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, -16.358F, 8.528F, 0.8F, 6.521F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi05", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 15.632F, -15.843F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo06", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, -9.022F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi06", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 15.632F, -9.321F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo07", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, -2.501F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi07", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 15.632F, -2.8F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo08", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, 4.021F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi08", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 15.632F, 3.721F, 8.528F, 0.8F, 6.521F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo09", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, 10.542F, 8.528F, 0.8F, 6.521F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi09", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 15.632F, 11.057F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo10", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, 17.878F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi10", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 15.56F, 17.579F, 8.528F, 0.872F, 5.739F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo11", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, -0.032F, 24.399F, 8.528F, 1.033F, 5.725F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi11", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 14.959F, 24.056F, 8.528F, 1.323F, 5.735F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_lo12", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 0.45F, 30.757F, 8.528F, 2.543F, 6.415F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_hi12", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 14.281F, 30.55F, 8.528F, 1.248F, 6.567F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_w13", CubeListBuilder.create()
                .texOffs(59, 93).addBox(-25.464F, 2.223F, 37.25F, 8.528F, 12.78F, 6.702F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_w00", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 3.26F, -49.992F, 8.528F, 11.922F, 6.703F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo01", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 0.119F, -42.782F, 8.528F, 3.333F, 6.146F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi01", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 14.493F, -42.716F, 8.528F, 1.016F, 5.732F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo02", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, -35.956F, 8.528F, 0.815F, 5.74F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi02", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 14.928F, -36.225F, 8.528F, 1.179F, 5.747F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo03", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, -29.401F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi03", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 15.363F, -29.718F, 8.528F, 1.069F, 6.539F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo04", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, -22.879F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi04", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 15.632F, -22.364F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo05", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, -16.358F, 8.528F, 0.8F, 6.521F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi05", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 15.632F, -15.843F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo06", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, -9.022F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi06", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 15.632F, -9.321F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo07", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, -2.501F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi07", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 15.632F, -2.8F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo08", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, 4.021F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi08", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 15.632F, 3.721F, 8.528F, 0.8F, 6.521F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo09", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, 10.542F, 8.528F, 0.8F, 6.521F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi09", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 15.632F, 11.057F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo10", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, 17.878F, 8.528F, 0.8F, 5.706F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi10", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 15.56F, 17.579F, 8.528F, 0.872F, 5.739F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo11", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, -0.032F, 24.399F, 8.528F, 1.033F, 5.725F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi11", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 14.959F, 24.056F, 8.528F, 1.323F, 5.735F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_lo12", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 0.45F, 30.757F, 8.528F, 2.543F, 6.415F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_hi12", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 14.281F, 30.55F, 8.528F, 1.248F, 6.567F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_w13", CubeListBuilder.create()
                .texOffs(59, 93).addBox(16.936F, 2.223F, 37.25F, 8.528F, 12.78F, 6.702F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("vent_deck", CubeListBuilder.create()
                .texOffs(41, 0).addBox(1.92F, 26.8F, -47.04F, 19.84F, 0.88F, 9.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("vent_deck_frame", CubeListBuilder.create()
                .texOffs(41, 0).addBox(1.16F, 26.8F, -47.36F, 0.88F, 0.8F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("vent_deck_frame.001", CubeListBuilder.create()
                .texOffs(41, 0).addBox(21.64F, 26.8F, -47.36F, 0.88F, 0.8F, 10.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("vent_l", CubeListBuilder.create()
                .texOffs(41, 0).addBox(-27.36F, 19.36F, -8.32F, 1.12F, 6.72F, 15.68F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("vent_r", CubeListBuilder.create()
                .texOffs(41, 0).addBox(-26.24F, 19.36F, -8.32F, 1.12F, 6.72F, 15.68F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l1_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 3.24F, -40.4F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l1_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 8.12F, -37.96F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l1_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 0.8F, -37.96F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l1_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.6F, 4.32F, -36.88F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l2_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 3.24F, -27.6F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l2_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 8.12F, -25.16F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l2_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 0.8F, -25.16F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l2_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.6F, 4.32F, -24.08F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l3_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 3.24F, -14.8F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l3_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 8.12F, -12.36F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l3_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 0.8F, -12.36F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l3_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.6F, 4.32F, -11.28F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l4_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 3.24F, -2.0F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l4_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 8.12F, 0.44F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l4_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 0.8F, 0.44F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l4_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.6F, 4.32F, 1.52F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l5_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 3.24F, 10.8F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l5_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 8.12F, 13.24F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l5_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 0.8F, 13.24F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l5_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.6F, 4.32F, 14.32F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l6_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 3.24F, 23.6F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l6_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 8.12F, 26.04F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l6_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.12F, 0.8F, 26.04F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_l6_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(-25.6F, 4.32F, 27.12F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r1_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 3.24F, -40.4F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r1_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 8.12F, -37.96F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r1_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 0.8F, -37.96F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r1_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.8F, 4.32F, -36.88F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r2_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 3.24F, -27.6F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r2_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 8.12F, -25.16F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r2_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 0.8F, -25.16F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r2_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.8F, 4.32F, -24.08F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r3_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 3.24F, -14.8F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r3_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 8.12F, -12.36F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r3_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 0.8F, -12.36F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r3_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.8F, 4.32F, -11.28F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r4_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 3.24F, -2.0F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r4_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 8.12F, 0.44F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r4_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 0.8F, 0.44F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r4_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.8F, 4.32F, 1.52F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r5_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 3.24F, 10.8F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r5_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 8.12F, 13.24F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r5_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 0.8F, 13.24F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r5_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.8F, 4.32F, 14.32F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r6_m", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 3.24F, 23.6F, 7.84F, 4.88F, 9.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r6_t", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 8.12F, 26.04F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r6_b", CubeListBuilder.create()
                .texOffs(409, 0).addBox(17.28F, 0.8F, 26.04F, 7.84F, 2.44F, 4.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wheel_r6_hub", CubeListBuilder.create()
                .texOffs(409, 0).addBox(16.8F, 4.32F, 27.12F, 8.8F, 2.72F, 2.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // ---- turret (57 parts) ----
        turret.addOrReplaceChild("antenna0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-10.752F, 14.84F, 8.308F, 0.384F, 21.6F, 0.384F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antenna1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(10.368F, 14.84F, 8.308F, 0.384F, 21.6F, 0.384F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antenna_base0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-11.28F, 12.84F, 7.798F, 1.44F, 2.08F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antenna_base1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(9.84F, 12.84F, 7.798F, 1.44F, 2.08F, 1.404F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("canvas_roll", CubeListBuilder.create()
                .texOffs(0, 0).addBox(12.8F, 13.24F, 6.1F, 3.2F, 3.2F, 14.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("canvas_roll2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-15.12F, 13.16F, 7.7F, 2.72F, 2.72F, 11.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("cupola", CubeListBuilder.create()
                .texOffs(181, 0).addBox(1.078F, 14.44F, -8.502F, 10.643F, 3.52F, 10.643F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("cupola_hatch", CubeListBuilder.create()
                .texOffs(181, 0).addBox(1.04F, 17.888F, -8.54F, 10.72F, 0.88F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("cupola_hinge", CubeListBuilder.create()
                .texOffs(181, 0).addBox(5.68F, 16.92F, 1.46F, 1.44F, 1.44F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("cupola_ring", CubeListBuilder.create()
                .texOffs(181, 0).addBox(1.6F, 18.2F, -7.98F, 9.6F, 0.64F, 9.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("gun_breech", CubeListBuilder.create()
                .texOffs(137, 0).addBox(3.44F, 8.52F, -16.22F, 3.68F, 3.68F, 8.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("gun_cradle", CubeListBuilder.create()
                .texOffs(137, 0).addBox(2.56F, 7.64F, -21.58F, 5.44F, 4.8F, 13.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("mg_coax", CubeListBuilder.create()
                .texOffs(137, 0).addBox(1.408F, 8.888F, -27.18F, 1.024F, 1.024F, 7.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("mg_coax_ammo", CubeListBuilder.create()
                .texOffs(137, 0).addBox(4.16F, 6.68F, -12.62F, 3.2F, 4.16F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("mg_coax_flash", CubeListBuilder.create()
                .texOffs(137, 0).addBox(1.184F, 8.664F, -29.18F, 1.472F, 1.472F, 2.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("mg_coax_recv", CubeListBuilder.create()
                .texOffs(137, 0).addBox(0.32F, 7.64F, -19.34F, 3.2F, 3.52F, 9.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sight_guard", CubeListBuilder.create()
                .texOffs(181, 0).addBox(3.2F, 21.16F, -16.3F, 7.04F, 0.8F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sight_gunner", CubeListBuilder.create()
                .texOffs(181, 0).addBox(3.68F, 14.84F, -15.5F, 6.08F, 7.36F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l0", CubeListBuilder.create()
                .texOffs(41, 0).addBox(-15.04F, 10.698F, -21.1F, 1.44F, 1.404F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l1", CubeListBuilder.create()
                .texOffs(41, 0).addBox(-16.8F, 10.698F, -21.1F, 1.44F, 1.404F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l2", CubeListBuilder.create()
                .texOffs(41, 0).addBox(-15.04F, 12.458F, -21.1F, 1.44F, 1.404F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l3", CubeListBuilder.create()
                .texOffs(41, 0).addBox(-16.8F, 12.458F, -21.1F, 1.44F, 1.404F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_l_base", CubeListBuilder.create()
                .texOffs(41, 0).addBox(-17.28F, 10.2F, -19.02F, 4.16F, 4.16F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r0", CubeListBuilder.create()
                .texOffs(41, 0).addBox(15.36F, 10.698F, -21.1F, 1.44F, 1.404F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r1", CubeListBuilder.create()
                .texOffs(41, 0).addBox(13.6F, 10.698F, -21.1F, 1.44F, 1.404F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r2", CubeListBuilder.create()
                .texOffs(41, 0).addBox(15.36F, 12.458F, -21.1F, 1.44F, 1.404F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r3", CubeListBuilder.create()
                .texOffs(41, 0).addBox(13.6F, 12.458F, -21.1F, 1.44F, 1.404F, 3.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_r_base", CubeListBuilder.create()
                .texOffs(41, 0).addBox(13.12F, 10.2F, -19.02F, 4.16F, 4.16F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("tow_box", CubeListBuilder.create()
                .texOffs(0, 93).addBox(-21.84F, 9.4F, -16.62F, 7.36F, 7.04F, 19.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("tow_handle", CubeListBuilder.create()
                .texOffs(0, 93).addBox(-22.56F, 14.44F, -11.42F, 0.48F, 0.8F, 8.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("tow_lid", CubeListBuilder.create()
                .texOffs(0, 93).addBox(-22.08F, 16.44F, -16.3F, 7.84F, 0.48F, 18.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("tow_mount-55", CubeListBuilder.create()
                .texOffs(0, 93).addBox(-18.24F, 11.0F, -2.38F, 6.72F, 3.2F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("tow_mount20", CubeListBuilder.create()
                .texOffs(0, 93).addBox(-18.24F, 11.0F, -14.38F, 6.72F, 3.2F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("tow_tube1025", CubeListBuilder.create()
                .texOffs(0, 93).addBox(-17.6F, 11.72F, 2.42F, 2.4F, 2.4F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("tow_tube1245", CubeListBuilder.create()
                .texOffs(0, 93).addBox(-21.12F, 11.72F, 2.42F, 2.4F, 2.4F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.36F, 0.28F, -21.1F, 30.72F, 14.56F, 43.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_ammobox", CubeListBuilder.create()
                .texOffs(181, 0).addBox(14.72F, 7.0F, -11.66F, 3.52F, 4.8F, 9.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_ammobox2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(14.56F, 8.12F, 4.18F, 2.88F, 3.84F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_bustle", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-12.8F, 7.0F, 20.5F, 25.6F, 6.72F, 7.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_bustle_rib0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(8.4F, 6.84F, 20.34F, 0.8F, 7.04F, 7.68F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_bustle_rib1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-0.4F, 6.84F, 20.34F, 0.8F, 7.04F, 7.68F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_bustle_rib2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-9.2F, 6.84F, 20.34F, 0.8F, 7.04F, 7.68F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_guard", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-16.0F, -0.04F, -15.98F, 32.0F, 2.88F, 32.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_guard_l", CubeListBuilder.create()
                .texOffs(181, 0).addBox(15.52F, 7.96F, -16.3F, 0.96F, 3.52F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_handle0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-15.756F, 9.8F, -18.78F, 1.036F, 0.48F, 4.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_hatch_load", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-12.64F, 14.84F, 0.98F, 8.64F, 0.8F, 8.64F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_isu", CubeListBuilder.create()
                .texOffs(181, 0).addBox(5.6F, 14.52F, 2.58F, 4.8F, 4.8F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_isu_glass", CubeListBuilder.create()
                .texOffs(181, 0).addBox(6.24F, 15.8F, 2.34F, 3.52F, 3.2F, 0.32F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_lift0", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-10.4F, 14.68F, -9.34F, 0.96F, 1.28F, 2.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_lift1", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-10.4F, 14.68F, 10.66F, 0.96F, 1.28F, 2.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_lift2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(11.04F, 14.68F, -9.34F, 0.96F, 1.28F, 2.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_lift3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(11.04F, 14.68F, 10.66F, 0.96F, 1.28F, 2.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_ring", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-16.0F, -1.96F, -15.98F, 32.0F, 3.84F, 32.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_roofrack", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-10.4F, 13.16F, 11.86F, 9.92F, 2.08F, 8.32F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_stow_l", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-17.92F, 7.0F, 5.3F, 3.84F, 4.16F, 11.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_stowbox2", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-13.12F, 12.44F, -3.66F, 5.12F, 3.52F, 7.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_stowbox3", CubeListBuilder.create()
                .texOffs(181, 0).addBox(-1.44F, 12.76F, -11.5F, 4.48F, 2.88F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // ---- gun (12 parts) ----
        gun.addOrReplaceChild("canvas_mantlet", CubeListBuilder.create()
                .texOffs(0, 0).addBox(3.12F, -2.247F, -3.48F, 4.32F, 4.254F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_barrel", CubeListBuilder.create()
                .texOffs(137, 0).addBox(4.88F, -0.44F, -19.96F, 0.8F, 0.8F, 9.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_gas", CubeListBuilder.create()
                .texOffs(137, 0).addBox(5.936F, 0.067F, -18.2F, 0.448F, 0.426F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_mantlet", CubeListBuilder.create()
                .texOffs(137, 0).addBox(1.6F, -4.088F, -2.591F, 7.36F, 7.775F, 6.543F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_muzzle", CubeListBuilder.create()
                .texOffs(137, 0).addBox(4.64F, -0.68F, -19.8F, 1.28F, 1.28F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_muzzleslot0", CubeListBuilder.create()
                .texOffs(137, 0).addBox(4.464F, -0.136F, -19.64F, 0.192F, 0.192F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_muzzleslot1", CubeListBuilder.create()
                .texOffs(137, 0).addBox(5.184F, 0.584F, -19.64F, 0.192F, 0.192F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_muzzleslot2", CubeListBuilder.create()
                .texOffs(137, 0).addBox(5.904F, -0.136F, -19.64F, 0.192F, 0.192F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_muzzleslot3", CubeListBuilder.create()
                .texOffs(137, 0).addBox(5.184F, -0.856F, -19.64F, 0.192F, 0.192F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_ring", CubeListBuilder.create()
                .texOffs(137, 0).addBox(4.64F, -0.67F, -9.0F, 1.28F, 1.261F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_sleeve", CubeListBuilder.create()
                .texOffs(137, 0).addBox(4.512F, -0.808F, -8.6F, 1.536F, 1.536F, 7.04F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("gun_sleeve2", CubeListBuilder.create()
                .texOffs(137, 0).addBox(4.64F, -0.68F, -13.24F, 1.28F, 1.28F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void setupAnim(BradleyEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float partialTick = ageInTicks - entity.tickCount;
        turret.yRot = ApacheModelMath.turretYawRadians(
                entity.renderTurretYawDeg(partialTick), entity.getYRot());
        // The turret only traverses: elevation is the gun's alone, on its own trunnion.
        turret.xRot = 0.0F;
        gun.xRot = ApacheModelMath.turretPitchRadians(
                entity.renderTurretPitchDeg(partialTick), headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue,
                               float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
