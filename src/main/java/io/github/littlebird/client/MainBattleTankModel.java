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
 * M1A2 SEP v1 main battle tank, generated from the Blender source model.
 *
 * <p>GENERATED FILE - do not edit by hand. Rebuild with
 * tools/generate_m1a2sepv1_model.py, which reads tools/data/m1a2sepv1_boxes.json
 * (exported from blender_m1a2sepv1/m1a2sepv1.blend by work/export_mc.py). The
 * Blender model is real M1A2 SEP v1 scale: hull 126.9 units, overall width 58.6
 * units, 16 units per block, nose along -Z, +X to the vehicle's right. The
 * turret ring and barrel reach are mirrored in MainBattleTankGeometry so the
 * server-side muzzle and the crew seats line up.
 */
public final class MainBattleTankModel extends EntityModel<MainBattleTankEntity> {
    public static final ModelLayerLocation MAIN_BATTLE_TANK_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "main_battle_tank"), "main");
    private static final int TEXTURE_SIZE = 512;
    private static final float DEG = (float) (Math.PI / 180.0D);
    /** Turret ring height, matching MainBattleTankGeometry.TURRET_PIVOT_HEIGHT_UNITS. */
    private static final float TURRET_Y = 25.28F;
    private static final float TURRET_Z = -7.2F;
    /** Gun trunnion, turret-local: matching MainBattleTankGeometry.GUN_TRUNNION_*. */
    private static final float GUN_TRUNNION_Y = 7.04F;
    private static final float GUN_TRUNNION_Z = -29.6F;
    /** Cupola machine gun pintle, turret-local: seat two's remote mount. */
    private static final float CUPOLA_X = 13.6F;
    private static final float CUPOLA_Y = 19.68F;
    private static final float CUPOLA_Z = 3.2F;

    private final ModelPart root;
    private final ModelPart turret;
    private final ModelPart gun;
    private final ModelPart cupola;

    public MainBattleTankModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.turret = body.getChild("turret");
        this.gun = this.turret.getChild("gun");
        this.cupola = this.turret.getChild("cupola");
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
        PartDefinition cupola = turret.addOrReplaceChild("cupola", CubeListBuilder.create(),
                PartPose.offset(CUPOLA_X, CUPOLA_Y, CUPOLA_Z));

        // ---- body (284 parts) ----
        body.addOrReplaceChild("track_l_top_0", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, 18.61F, -55.642F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_bot_0", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, -0.327F, -55.642F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_top_1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, 18.61F, -27.693F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_bot_1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, -0.327F, -27.693F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_top_2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, 18.61F, 0.257F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_bot_2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, -0.327F, 0.257F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_top_3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, 18.61F, 28.206F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_bot_3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, -0.327F, 28.206F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_front", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, -0.327F, -55.642F, 9.76F, 20.537F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_l_rear", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-29.12F, -0.327F, 54.556F, 9.76F, 20.537F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_top_0", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, 18.61F, -55.642F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_bot_0", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, -0.327F, -55.642F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_top_1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, 18.61F, -27.693F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_bot_1", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, -0.327F, -27.693F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_top_2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, 18.61F, 0.257F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_bot_2", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, -0.327F, 0.257F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_top_3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, 18.61F, 28.206F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_bot_3", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, -0.327F, 28.206F, 9.76F, 1.6F, 27.95F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_front", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, -0.327F, -55.642F, 9.76F, 20.537F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_r_rear", CubeListBuilder.create()
                .texOffs(0, 224).addBox(19.36F, -0.327F, 54.556F, 9.76F, 20.537F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_lower", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-19.52F, 7.2F, -60.8F, 39.04F, 8.0F, 120.32F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_lower_front", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-19.52F, 15.2F, -57.92F, 39.04F, 5.6F, 115.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_deck", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.28F, 20.8F, -36.8F, 58.56F, 4.48F, 100.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_nose", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.28F, 16.32F, -62.88F, 58.56F, 1.6F, 5.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_front_lower", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-19.52F, 7.2F, -62.88F, 39.04F, 9.12F, 2.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_glacis", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.28F, -2.0F, -27.676F, 58.56F, 4.0F, 29.176F),
                PartPose.offsetAndRotation(0.0F, 23.28F, -36.8F, -16.33F * DEG, 0.0F, 0.0F));
        body.addOrReplaceChild("ntenna__1", CubeListBuilder.create()
                .texOffs(0, 160).addBox(18.592F, 39.957F, 20.515F, 0.576F, 23.286F, 4.089F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ntenna_1", CubeListBuilder.create()
                .texOffs(0, 160).addBox(-19.168F, 39.957F, 20.515F, 0.576F, 23.286F, 4.089F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ntenna_base__1", CubeListBuilder.create()
                .texOffs(0, 160).addBox(17.68F, 38.4F, 19.6F, 2.4F, 1.92F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ntenna_base_1", CubeListBuilder.create()
                .texOffs(0, 160).addBox(-20.08F, 38.4F, 19.6F, 2.4F, 1.92F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_c", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.12F, 25.288F, 47.2F, 10.24F, 0.56F, 7.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_frame", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-26.24F, 25.288F, 36.8F, 20.8F, 0.56F, 17.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 53.34F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 52.082F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_10", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 40.768F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_11", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 39.511F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_12", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 38.254F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_13", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 36.997F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 50.825F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 49.568F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 48.311F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_5", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 47.054F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_6", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 45.797F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_7", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 44.54F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_8", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 43.282F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_l_louver_9", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.408F, 25.395F, 42.025F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_frame", CubeListBuilder.create()
                .texOffs(0, 0).addBox(5.44F, 25.288F, 36.8F, 20.8F, 0.56F, 17.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 53.34F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 52.082F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_10", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 40.768F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_11", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 39.511F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_12", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 38.254F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_13", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 36.997F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 50.825F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 49.568F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 48.311F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_5", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 47.054F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_6", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 45.797F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_7", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 44.54F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_8", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 43.282F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_grille_r_louver_9", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.272F, 25.395F, 42.025F, 19.136F, 0.666F, 0.864F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_hinge_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.8F, 25.488F, 35.04F, 1.6F, 0.8F, 3.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_hinge_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.4F, 25.488F, 35.04F, 1.6F, 0.8F, 3.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_latch_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(7.68F, 25.568F, 60.0F, 2.24F, 0.64F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_latch_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-9.92F, 25.568F, 60.0F, 2.24F, 0.64F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_rail__1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.448F, 25.248F, -31.2F, 0.704F, 0.704F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_rail_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.152F, 25.248F, -31.2F, 0.704F, 0.704F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_rail_post__1_19", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.551F, 25.36F, -31.008F, 0.499F, 0.56F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_rail_post__1_9", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.551F, 25.36F, -15.968F, 0.499F, 0.56F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_rail_post_1_19", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.049F, 25.36F, -31.008F, 0.499F, 0.56F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eck_rail_post_1_9", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.049F, 25.36F, -15.968F, 0.499F, 0.56F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("river_hatch", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.527F, 21.276F, -52.752F, 9.054F, 3.35F, 8.947F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("river_hatch_rim", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.321F, 20.747F, -53.369F, 10.642F, 3.578F, 10.424F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("river_hinge", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.72F, 23.912F, -43.16F, 5.44F, 1.271F, 2.031F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("river_periscope_0", CubeListBuilder.create()
                .texOffs(384, 320).addBox(2.8F, 20.411F, -54.797F, 1.44F, 1.677F, 2.22F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("river_periscope_1", CubeListBuilder.create()
                .texOffs(384, 320).addBox(-0.72F, 20.411F, -54.797F, 1.44F, 1.677F, 2.22F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("river_periscope_2", CubeListBuilder.create()
                .texOffs(384, 320).addBox(-4.24F, 20.411F, -54.797F, 1.44F, 1.677F, 2.22F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("river_periscope_gl_0", CubeListBuilder.create()
                .texOffs(384, 320).addBox(2.92F, 21.191F, -54.741F, 1.2F, 0.885F, 0.667F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("river_periscope_gl_1", CubeListBuilder.create()
                .texOffs(384, 320).addBox(-0.6F, 21.191F, -54.741F, 1.2F, 0.885F, 0.667F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("river_periscope_gl_2", CubeListBuilder.create()
                .texOffs(384, 320).addBox(-4.12F, 21.191F, -54.741F, 1.2F, 0.885F, 0.667F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ender_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.12F, 20.72F, -55.84F, 9.76F, 0.64F, 13.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ender_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.36F, 20.72F, -55.84F, 9.76F, 0.64F, 13.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ender_lip_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.12F, 18.88F, -56.08F, 9.76F, 2.08F, 1.12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ender_lip_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.36F, 18.88F, -56.08F, 9.76F, 2.08F, 1.12F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("inaldrive_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.8F, 9.92F, 45.76F, 3.84F, 7.68F, 7.68F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("inaldrive_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(16.96F, 9.92F, 45.76F, 3.84F, 7.68F, 7.68F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("uelcap_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.88F, 25.32F, 7.92F, 3.36F, 0.56F, 3.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("uelcap_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(25.52F, 25.32F, 7.92F, 3.36F, 0.56F, 3.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eadlight_box_l", CubeListBuilder.create()
                .texOffs(256, 384).addBox(-26.08F, 16.88F, -60.0F, 3.52F, 3.36F, 4.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eadlight_box_r", CubeListBuilder.create()
                .texOffs(256, 384).addBox(22.56F, 16.88F, -60.0F, 3.52F, 3.36F, 4.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eadlight_lens_l", CubeListBuilder.create()
                .texOffs(256, 384).addBox(-25.68F, 17.2F, -60.32F, 2.72F, 2.72F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eadlight_lens_r", CubeListBuilder.create()
                .texOffs(256, 384).addBox(22.96F, 17.2F, -60.32F, 2.72F, 2.72F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ull_rear_plate", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.8F, 20.8F, 61.52F, 57.6F, 4.48F, 1.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("dler_l", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.64F, 8.16F, -54.56F, 4.8F, 9.92F, 9.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("dler_r", CubeListBuilder.create()
                .texOffs(256, 256).addBox(21.84F, 8.16F, -54.56F, 4.8F, 9.92F, 9.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("dler_hub_l", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-26.96F, 11.36F, -51.36F, 5.44F, 3.52F, 3.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("dler_hub_r", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.52F, 11.36F, -51.36F, 5.44F, 3.52F, 3.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ift_eye_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.36F, 38.32F, -15.68F, 1.12F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ift_eye_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.24F, 38.32F, -15.68F, 1.12F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ift_eye_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-18.48F, 38.32F, 15.52F, 1.12F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ift_eye_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(17.36F, 38.32F, 15.52F, 1.12F, 1.44F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("udflap_front_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.04F, 15.84F, -57.72F, 9.6F, 5.44F, 0.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("udflap_front_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.44F, 15.84F, -57.72F, 9.6F, 5.44F, 0.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_exh_louver_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.76F, 18.165F, 63.146F, 22.72F, 0.79F, 0.907F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_exh_louver_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.76F, 19.045F, 63.146F, 22.72F, 0.79F, 0.907F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_exh_louver_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.76F, 19.925F, 63.146F, 22.72F, 0.79F, 0.907F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_exh_louver_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.76F, 20.805F, 63.146F, 22.72F, 0.79F, 0.907F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_exh_louver_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.76F, 21.685F, 63.146F, 22.72F, 0.79F, 0.907F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_exh_louver_5", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.76F, 22.565F, 63.146F, 22.72F, 0.79F, 0.907F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_exh_louver_6", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.76F, 23.445F, 63.146F, 22.72F, 0.79F, 0.907F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_exh_recess", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.8F, 17.92F, 63.0F, 24.8F, 6.72F, 0.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_panel_frame_s__1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.44F, 9.6F, 63.248F, 0.48F, 5.28F, 0.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_pintle", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.837F, 11.92F, 64.32F, 1.674F, 1.76F, 1.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ear_pintle_mount", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.08F, 10.88F, 62.88F, 4.16F, 3.84F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("earfender_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.12F, 20.72F, 42.08F, 9.76F, 0.64F, 20.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("earfender_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.36F, 20.72F, 42.08F, 9.76F, 0.64F, 20.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("earfender_lip_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.12F, 17.6F, 61.68F, 9.76F, 3.52F, 1.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("earfender_lip_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.36F, 17.6F, 61.68F, 9.76F, 3.52F, 1.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_l_0", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-26.0F, 14.56F, 24.16F, 3.52F, 2.88F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_l_1", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-26.0F, 14.56F, -1.44F, 3.52F, 2.88F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_l_2", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-26.0F, 14.56F, -27.04F, 3.52F, 2.88F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_r_0", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.48F, 14.56F, 24.16F, 3.52F, 2.88F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_r_1", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.48F, 14.56F, -1.44F, 3.52F, 2.88F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_r_2", CubeListBuilder.create()
                .texOffs(256, 160).addBox(22.48F, 14.56F, -27.04F, 3.52F, 2.88F, 2.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_arm_l_0", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-24.24F, 15.52F, 25.12F, 4.72F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_arm_l_1", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-24.24F, 15.52F, -0.48F, 4.72F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_arm_l_2", CubeListBuilder.create()
                .texOffs(256, 160).addBox(-24.24F, 15.52F, -26.08F, 4.72F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_arm_r_0", CubeListBuilder.create()
                .texOffs(256, 160).addBox(19.52F, 15.52F, 25.12F, 4.72F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_arm_r_1", CubeListBuilder.create()
                .texOffs(256, 160).addBox(19.52F, 15.52F, -0.48F, 4.72F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_arm_r_2", CubeListBuilder.create()
                .texOffs(256, 160).addBox(19.52F, 15.52F, -26.08F, 4.72F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_hub_l_0", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-26.24F, 15.208F, 24.808F, 4.0F, 1.584F, 1.584F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_hub_l_1", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-26.24F, 15.208F, -0.792F, 4.0F, 1.584F, 1.584F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_hub_l_2", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-26.24F, 15.208F, -26.392F, 4.0F, 1.584F, 1.584F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_hub_r_0", CubeListBuilder.create()
                .texOffs(0, 416).addBox(22.24F, 15.208F, 24.808F, 4.0F, 1.584F, 1.584F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_hub_r_1", CubeListBuilder.create()
                .texOffs(0, 416).addBox(22.24F, 15.208F, -0.792F, 4.0F, 1.584F, 1.584F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("eturnroller_hub_r_2", CubeListBuilder.create()
                .texOffs(0, 416).addBox(22.24F, 15.208F, -26.392F, 4.0F, 1.584F, 1.584F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_l_0", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 9.12F, 29.909F, 0.56F, 12.0F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_l_1", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 8.0F, 17.977F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_l_2", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 8.0F, 6.046F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_l_3", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 8.0F, -5.886F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_l_4", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 8.0F, -17.817F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_l_5", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 8.0F, -29.749F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_l_6", CubeListBuilder.create()
                .texOffs(448, 320).addBox(-29.28F, 8.8F, -41.68F, 0.56F, 12.32F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_r_0", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.72F, 9.12F, 29.909F, 0.56F, 12.0F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_r_1", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.72F, 8.0F, 17.977F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_r_2", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.72F, 8.0F, 6.046F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_r_3", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.72F, 8.0F, -5.886F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_r_4", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.72F, 8.0F, -17.817F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_r_5", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.72F, 8.0F, -29.749F, 0.56F, 13.12F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_r_6", CubeListBuilder.create()
                .texOffs(448, 320).addBox(28.72F, 8.8F, -41.68F, 0.56F, 12.32F, 11.451F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_rail_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.4F, 21.0F, -41.92F, 0.8F, 0.88F, 83.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("kirt_rail_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(28.6F, 21.0F, -41.92F, 0.8F, 0.88F, 83.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke__1_0_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(24.775F, 30.715F, -31.343F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke__1_0_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(24.775F, 32.635F, -31.343F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke__1_1_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(24.775F, 30.715F, -33.743F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke__1_1_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(24.775F, 32.635F, -33.743F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke__1_2_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(24.775F, 30.715F, -36.143F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke__1_2_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(24.775F, 32.635F, -36.143F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_1_0_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.065F, 30.715F, -31.343F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_1_0_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.065F, 32.635F, -31.343F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_1_1_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.065F, 30.715F, -33.743F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_1_1_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.065F, 32.635F, -33.743F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_1_2_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.065F, 30.715F, -36.143F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_1_2_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.065F, 32.635F, -36.143F, 2.291F, 2.171F, 3.326F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_base__1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(24.48F, 29.92F, -34.72F, 0.96F, 4.8F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_base_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-25.44F, 29.92F, -34.72F, 0.96F, 4.8F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap__1_0_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(25.623F, 31.517F, -31.725F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap__1_0_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(25.623F, 33.437F, -31.725F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap__1_1_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(25.623F, 31.517F, -34.125F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap__1_1_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(25.623F, 33.437F, -34.125F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap__1_2_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(25.623F, 31.517F, -36.525F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap__1_2_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(25.623F, 33.437F, -36.525F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap_1_0_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.097F, 31.517F, -31.725F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap_1_0_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.097F, 33.437F, -31.725F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap_1_1_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.097F, 31.517F, -34.125F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap_1_1_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.097F, 33.437F, -34.125F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap_1_2_0", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.097F, 31.517F, -36.525F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("moke_cap_1_2_1", CubeListBuilder.create()
                .texOffs(320, 384).addBox(-27.097F, 33.437F, -36.525F, 1.474F, 1.349F, 1.45F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_l", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.32F, 8.64F, 44.48F, 4.16F, 10.24F, 10.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_r", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.16F, 8.64F, 44.48F, 4.16F, 10.24F, 10.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_hub_l", CubeListBuilder.create()
                .texOffs(0, 416).addBox(-27.12F, 11.68F, 47.52F, 5.76F, 4.16F, 4.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_hub_r", CubeListBuilder.create()
                .texOffs(0, 416).addBox(21.36F, 11.68F, 47.52F, 5.76F, 4.16F, 4.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_0", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 13.32F, 43.64F, 3.84F, 0.88F, 1.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_1", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 15.521F, 44.131F, 3.84F, 1.386F, 1.588F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_10", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 7.794F, 48.449F, 3.84F, 1.448F, 1.03F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_11", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 8.618F, 45.865F, 3.84F, 1.593F, 1.471F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_12", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 10.613F, 44.131F, 3.84F, 1.386F, 1.588F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_2", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 17.309F, 45.865F, 3.84F, 1.593F, 1.471F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_3", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 18.277F, 48.449F, 3.84F, 1.448F, 1.03F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_4", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 17.916F, 50.831F, 3.84F, 1.561F, 1.282F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_5", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 16.494F, 52.765F, 3.84F, 1.534F, 1.575F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_6", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 14.441F, 53.969F, 3.84F, 1.165F, 1.516F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_7", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 11.914F, 53.969F, 3.84F, 1.165F, 1.516F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_8", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 9.492F, 52.765F, 3.84F, 1.534F, 1.575F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_l_9", CubeListBuilder.create()
                .texOffs(256, 256).addBox(-26.16F, 8.043F, 50.831F, 3.84F, 1.561F, 1.282F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_0", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 13.32F, 43.64F, 3.84F, 0.88F, 1.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_1", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 15.521F, 44.131F, 3.84F, 1.386F, 1.588F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_10", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 7.794F, 48.449F, 3.84F, 1.448F, 1.03F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_11", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 8.618F, 45.865F, 3.84F, 1.593F, 1.471F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_12", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 10.613F, 44.131F, 3.84F, 1.386F, 1.588F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_2", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 17.309F, 45.865F, 3.84F, 1.593F, 1.471F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_3", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 18.277F, 48.449F, 3.84F, 1.448F, 1.03F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_4", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 17.916F, 50.831F, 3.84F, 1.561F, 1.282F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_5", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 16.494F, 52.765F, 3.84F, 1.534F, 1.575F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_6", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 14.441F, 53.969F, 3.84F, 1.165F, 1.516F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_7", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 11.914F, 53.969F, 3.84F, 1.165F, 1.516F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_8", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 9.492F, 52.765F, 3.84F, 1.534F, 1.575F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("procket_tooth_r_9", CubeListBuilder.create()
                .texOffs(256, 256).addBox(22.32F, 8.043F, 50.831F, 3.84F, 1.561F, 1.282F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_l_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.76F, 6.72F, 37.28F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_l_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.76F, 6.72F, 24.48F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_l_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.76F, 6.72F, 11.68F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_l_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.76F, 6.72F, -1.12F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_l_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.76F, 6.72F, -13.92F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_l_5", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.76F, 6.72F, -26.72F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_l_6", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.76F, 6.72F, -39.52F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_r_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.24F, 6.72F, 37.28F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_r_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.24F, 6.72F, 24.48F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_r_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.24F, 6.72F, 11.68F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_r_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.24F, 6.72F, -1.12F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_r_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.24F, 6.72F, -13.92F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_r_5", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.24F, 6.72F, -26.72F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("wingarm_r_6", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.24F, 6.72F, -39.52F, 3.52F, 3.2F, 2.24F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("aillight__1", CubeListBuilder.create()
                .texOffs(256, 384).addBox(21.44F, 21.36F, 63.04F, 3.2F, 2.08F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("aillight_1", CubeListBuilder.create()
                .texOffs(256, 384).addBox(-24.64F, 21.36F, 63.04F, 3.2F, 2.08F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ool_clamp__1_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.08F, 25.6F, 6.72F, 1.76F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ool_clamp__1_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.08F, 25.6F, -6.88F, 1.76F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ool_clamp_1_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.84F, 25.6F, -23.68F, 1.76F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ool_clamp_1_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.84F, 25.6F, -37.28F, 1.76F, 0.96F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ool_handle__1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.6F, 25.48F, -8.8F, 0.72F, 0.72F, 19.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ool_handle_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.32F, 25.48F, -39.2F, 0.72F, 0.72F, 19.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ool_head__1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(23.68F, 25.56F, -11.36F, 2.56F, 0.56F, 4.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("ool_head_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-26.24F, 25.56F, -41.76F, 2.56F, 0.56F, 4.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable__1_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.984F, 21.824F, 19.19F, 1.152F, 1.472F, 18.42F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable__1_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.984F, 21.824F, 0.0F, 1.152F, 1.152F, 19.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable__1_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.984F, 21.824F, -19.2F, 1.152F, 1.152F, 19.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable__1_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.984F, 21.824F, -37.615F, 1.152F, 1.632F, 18.43F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_1_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.136F, 21.824F, 19.19F, 1.152F, 1.472F, 18.42F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_1_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.136F, 21.824F, 0.0F, 1.152F, 1.152F, 19.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_1_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.136F, 21.824F, -19.2F, 1.152F, 1.152F, 19.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_1_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.136F, 21.824F, -37.615F, 1.152F, 1.632F, 18.43F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_clamp__1__18", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.76F, 21.6F, 28.32F, 1.6F, 1.6F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_clamp__1_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.76F, 21.6F, -0.48F, 1.6F, 1.6F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_clamp__1_18", CubeListBuilder.create()
                .texOffs(0, 0).addBox(27.76F, 21.6F, -29.28F, 1.6F, 1.6F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_clamp_1__18", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.36F, 21.6F, 28.32F, 1.6F, 1.6F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_clamp_1_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.36F, 21.6F, -0.48F, 1.6F, 1.6F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owcable_clamp_1_18", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-29.36F, 21.6F, -29.28F, 1.6F, 1.6F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owhook_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-17.2F, 12.4F, -62.72F, 1.76F, 2.72F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owhook_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(15.44F, 12.4F, -62.72F, 1.76F, 2.72F, 3.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owpin_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-18.72F, 13.227F, -62.314F, 4.8F, 1.065F, 1.108F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("owpin_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(13.92F, 13.227F, -62.314F, 4.8F, 1.065F, 1.108F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("urret_rail__1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(25.248F, 35.168F, -8.8F, 0.704F, 0.704F, 17.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("urret_rail_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.952F, 35.168F, -8.8F, 0.704F, 0.704F, 17.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("urret_ring", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.44F, 24.48F, -28.48F, 42.88F, 1.44F, 42.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_0_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 1.36F, 33.04F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_0_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 1.36F, 33.04F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_1_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 1.36F, 20.24F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_1_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 1.36F, 20.24F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_2_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 1.36F, 7.44F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_2_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 1.36F, 7.44F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_3_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 1.36F, -5.36F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_3_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 1.36F, -5.36F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_4_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 1.36F, -18.16F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_4_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 1.36F, -18.16F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_5_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 1.36F, -30.96F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_5_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 1.36F, -30.96F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_6_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 1.36F, -43.76F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_l_6_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-27.52F, 1.36F, -43.76F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_0_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.96F, 1.36F, 33.04F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_0_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.48F, 1.36F, 33.04F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_1_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.96F, 1.36F, 20.24F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_1_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.48F, 1.36F, 20.24F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_2_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.96F, 1.36F, 7.44F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_2_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.48F, 1.36F, 7.44F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_3_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.96F, 1.36F, -5.36F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_3_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.48F, 1.36F, -5.36F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_4_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.96F, 1.36F, -18.16F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_4_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.48F, 1.36F, -18.16F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_5_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.96F, 1.36F, -30.96F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_5_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.48F, 1.36F, -30.96F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_6_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(20.96F, 1.36F, -43.76F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("heel_r_6_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.48F, 1.36F, -43.76F, 3.04F, 10.72F, 10.72F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("indsensor_mast", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11.9F, 38.4F, 14.806F, 0.761F, 5.76F, 0.788F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("geo_blackout_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.48F, 17.44F, -59.76F, 1.12F, 1.6F, 1.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("geo_blackout_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(21.36F, 17.44F, -59.76F, 1.12F, 1.6F, 1.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // ---- turret (80 parts) ----
        turret.addOrReplaceChild("turret_seg0_low", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-23.52F, -0.32F, -31.52F, 47.04F, 9.04F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg0_top", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.168F, 8.72F, -31.52F, 42.336F, 4.4F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg1_low", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.12F, -0.32F, -28.96F, 50.24F, 9.04F, 11.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg1_top", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.608F, 8.72F, -28.96F, 45.216F, 4.4F, 11.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg2_low", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.12F, -0.32F, -17.12F, 50.24F, 9.04F, 43.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg2_top", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.608F, 8.72F, -17.12F, 45.216F, 4.4F, 43.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg3_low", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-25.12F, -0.32F, 26.4F, 50.24F, 9.04F, 5.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_seg3_top", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-22.608F, 8.72F, 26.4F, 45.216F, 4.4F, 5.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("turret_cheek_l0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.2F, -25.28F, 7.2F, 2.4F, 13.44F, 8.157F),
                PartPose.offsetAndRotation(-17.92F, -0.32F, -31.52F, 0.0F, -65.433F * DEG, 0.0F));
        turret.addOrReplaceChild("turret_cheek_l1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.2F, -25.28F, 7.2F, 2.4F, 13.44F, 13.948F),
                PartPose.offsetAndRotation(-23.52F, -0.32F, -28.96F, 0.0F, -7.696F * DEG, 0.0F));
        turret.addOrReplaceChild("turret_cheek_r0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.2F, -25.28F, 7.2F, 2.4F, 13.44F, 8.157F),
                PartPose.offsetAndRotation(17.92F, -0.32F, -31.52F, 0.0F, 65.433F * DEG, 0.0F));
        turret.addOrReplaceChild("turret_cheek_r1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.2F, -25.28F, 7.2F, 2.4F, 13.44F, 13.948F),
                PartPose.offsetAndRotation(23.52F, -0.32F, -28.96F, 0.0F, 7.696F * DEG, 0.0F));
        turret.addOrReplaceChild("ux_sight", CubeListBuilder.create()
                .texOffs(384, 320).addBox(-3.36F, 13.12F, -20.96F, 3.52F, 2.4F, 4.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ustle_body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.48F, 3.2F, 32.0F, 40.96F, 8.32F, 18.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("itv_base", CubeListBuilder.create()
                .texOffs(384, 320).addBox(13.6F, 12.96F, -11.2F, 6.4F, 1.92F, 6.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("itv_head", CubeListBuilder.create()
                .texOffs(384, 320).addBox(13.6F, 14.4F, -11.68F, 6.4F, 4.8F, 7.36F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("itv_lens", CubeListBuilder.create()
                .texOffs(256, 384).addBox(15.28F, 15.44F, -12.0F, 3.04F, 3.04F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("itv_top", CubeListBuilder.create()
                .texOffs(384, 320).addBox(14.24F, 19.12F, -10.576F, 5.12F, 0.48F, 5.152F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("oax_port", CubeListBuilder.create()
                .texOffs(0, 320).addBox(4.08F, 4.262F, -34.4F, 1.76F, 1.716F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("upola_hatch", CubeListBuilder.create()
                .texOffs(0, 0).addBox(7.52F, 16.56F, -1.28F, 12.16F, 0.96F, 12.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("upola_peri_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(12.8F, 15.44F, -3.2F, 1.6F, 1.12F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("upola_peri_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.285F, 15.44F, 0.12F, 2.16F, 1.12F, 2.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("upola_peri_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.285F, 15.44F, 7.32F, 2.16F, 1.12F, 2.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("upola_peri_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(12.8F, 15.44F, 11.2F, 1.6F, 1.12F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("upola_peri_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.755F, 15.44F, 7.32F, 2.16F, 1.12F, 2.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("upola_peri_5", CubeListBuilder.create()
                .texOffs(0, 0).addBox(18.755F, 15.44F, 0.12F, 2.16F, 1.12F, 2.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("upola_ring", CubeListBuilder.create()
                .texOffs(0, 0).addBox(6.56F, 13.12F, -2.24F, 14.08F, 3.52F, 14.08F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ps_body", CubeListBuilder.create()
                .texOffs(384, 320).addBox(4.32F, 12.8F, -22.56F, 8.0F, 3.84F, 9.92F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ps_face", CubeListBuilder.create()
                .texOffs(384, 320).addBox(5.6F, 13.944F, -22.882F, 5.44F, 2.193F, 1.284F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ps_hood", CubeListBuilder.create()
                .texOffs(384, 320).addBox(4.16F, 16.56F, -21.92F, 8.32F, 0.8F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("un_canvas", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-2.8F, 4.24F, -36.48F, 5.6F, 5.6F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("un_canvas_tie", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-2.96F, 4.08F, -31.68F, 5.92F, 5.92F, 0.96F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("oader_hatch", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.277F, 14.88F, -4.32F, 11.754F, 0.96F, 11.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("oader_peri_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-17.04F, 13.36F, -4.4F, 2.08F, 1.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("oader_peri_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.24F, 13.36F, -2.16F, 2.08F, 1.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("oader_peri_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.88F, 13.36F, 2.0F, 2.08F, 1.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("oader_peri_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-17.04F, 13.36F, 5.2F, 2.08F, 1.44F, 2.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("oader_ring", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-21.071F, 12.8F, -5.12F, 13.342F, 2.4F, 13.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("240_barrel", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-17.242F, 17.966F, -12.32F, 0.564F, 0.548F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("240_body", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-17.68F, 17.44F, -7.04F, 1.44F, 1.6F, 6.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("240_grip", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-18.4F, 17.2F, -1.76F, 2.88F, 1.44F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("240_pintle", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-17.28F, 15.36F, -1.6F, 1.28F, 2.56F, 1.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("2_pedestal", CubeListBuilder.create()
                .texOffs(0, 0).addBox(12.742F, 16.16F, 2.32F, 1.716F, 3.52F, 1.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antlet", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-7.04F, 1.62F, -33.268F, 14.08F, 10.841F, 8.616F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antlet_collar", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-4.48F, 3.241F, -34.165F, 8.96F, 7.599F, 5.93F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antlet_top", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-5.6F, 10.026F, -32.638F, 11.2F, 3.948F, 5.755F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_back_top", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.16F, 10.848F, 55.808F, 40.32F, 0.704F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_back_top_001", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.16F, 10.848F, 55.808F, 40.32F, 0.704F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_back_v_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(15.879F, 1.6F, 55.872F, 0.499F, 9.6F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_back_v_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(7.815F, 1.6F, 55.872F, 0.499F, 9.6F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_back_v_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.249F, 1.6F, 55.872F, 0.499F, 9.6F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_back_v_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.313F, 1.6F, 55.872F, 0.499F, 9.6F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_back_v_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-16.377F, 1.6F, 55.872F, 0.499F, 9.6F, 0.576F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_bag_0", CubeListBuilder.create()
                .texOffs(0, 160).addBox(8.0F, 2.72F, 51.84F, 8.0F, 4.8F, 4.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_bag_1", CubeListBuilder.create()
                .texOffs(0, 160).addBox(-5.28F, 2.4F, 52.64F, 7.36F, 4.16F, 3.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_bag_2", CubeListBuilder.create()
                .texOffs(0, 160).addBox(-16.96F, 2.72F, 51.36F, 6.72F, 5.44F, 4.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_box1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-13.2F, 2.4F, 50.56F, 8.8F, 5.44F, 4.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_box2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(2.4F, 2.4F, 50.72F, 9.6F, 4.8F, 4.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_floor_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(15.879F, 1.312F, 50.4F, 0.499F, 0.576F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_floor_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(7.815F, 1.312F, 50.4F, 0.499F, 0.576F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_floor_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.249F, 1.312F, 50.4F, 0.499F, 0.576F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_floor_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.313F, 1.312F, 50.4F, 0.499F, 0.576F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_floor_4", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-16.377F, 1.312F, 50.4F, 0.499F, 0.576F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_post__1__270_168", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.808F, 1.6F, 50.048F, 0.704F, 9.6F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_post__1__270_227", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.808F, 1.6F, 50.048F, 0.704F, 9.6F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_post__1__306_168", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.808F, 1.6F, 55.808F, 0.704F, 9.6F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_post__1__306_227", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.808F, 1.6F, 55.808F, 0.704F, 9.6F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_post_1__270_168", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.512F, 1.6F, 50.048F, 0.704F, 9.6F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_post_1__270_227", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.512F, 1.6F, 50.048F, 0.704F, 9.6F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_post_1__306_168", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.512F, 1.6F, 55.808F, 0.704F, 9.6F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_post_1__306_227", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.512F, 1.6F, 55.808F, 0.704F, 9.6F, 0.704F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_side_bot__1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.808F, 1.248F, 50.4F, 0.704F, 0.704F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_side_bot_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.512F, 1.248F, 50.4F, 0.704F, 0.704F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_side_top__1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(19.808F, 10.848F, 50.4F, 0.704F, 0.704F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_side_top_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-20.512F, 10.848F, 50.4F, 0.704F, 0.704F, 5.76F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("ack_tarp", CubeListBuilder.create()
                .texOffs(0, 160).addBox(-17.6F, 6.4F, 51.04F, 35.2F, 4.48F, 4.48F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("urret_stow__1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.48F, 4.8F, 17.6F, 2.24F, 5.76F, 11.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("urret_stow_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-26.72F, 4.8F, 17.6F, 2.24F, 5.76F, 11.84F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("urret_stow_lid__1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.4F, 10.4F, 17.44F, 2.4F, 0.48F, 12.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("urret_stow_lid_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-26.8F, 10.4F, 17.44F, 2.4F, 0.48F, 12.16F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // ---- gun (10 parts) ----
        gun.addOrReplaceChild("un_clamp_275", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-1.872F, -1.872F, -7.64F, 3.744F, 3.744F, 0.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("un_clamp_325", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-1.872F, -1.872F, -15.64F, 3.744F, 3.744F, 0.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("un_clamp_375", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-1.872F, -1.872F, -23.64F, 3.744F, 3.744F, 0.88F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("un_evac", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-2.48F, -2.48F, -36.8F, 4.96F, 4.96F, 9.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("un_evac_ring_401", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-2.608F, -2.608F, -27.92F, 5.216F, 5.216F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("un_evac_ring_459", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-2.608F, -2.608F, -37.2F, 5.216F, 5.216F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("un_mrs", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-0.8F, 1.04F, -55.68F, 1.6F, 1.6F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("un_muzzle", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-1.52F, -1.52F, -56.16F, 3.04F, 3.04F, 1.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("un_sleeve", CubeListBuilder.create()
                .texOffs(256, 320).addBox(-1.68F, -1.68F, -27.52F, 3.36F, 3.36F, 27.52F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        gun.addOrReplaceChild("un_tube", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-1.36F, -1.36F, -56.08F, 2.72F, 2.72F, 19.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // ---- cupola (6 parts) ----
        cupola.addOrReplaceChild("2_ammo", CubeListBuilder.create()
                .texOffs(0, 0).addBox(1.6F, -2.32F, -0.48F, 2.88F, 3.04F, 4.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        cupola.addOrReplaceChild("2_barrel", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-0.368F, 0.592F, -18.4F, 0.736F, 0.736F, 5.44F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        cupola.addOrReplaceChild("2_body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.88F, -0.08F, -8.16F, 1.76F, 2.08F, 9.28F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        cupola.addOrReplaceChild("2_cradle", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.88F, -0.48F, -1.6F, 1.76F, 1.6F, 2.56F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        cupola.addOrReplaceChild("2_grip", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.92F, 0.08F, 1.28F, 3.84F, 1.76F, 1.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        cupola.addOrReplaceChild("2_jacket", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.672F, 0.305F, -13.6F, 1.344F, 1.31F, 5.44F),
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
        // Seat two's remote gun rides its own pintle. On the machine of the player who is actually
        // sitting there, drive it from his own view so the barrel tracks the mouse at frame rate
        // instead of stepping on the 20 Hz synced angle; everyone else uses the synced value.
        net.minecraft.client.Minecraft minecraftClient = net.minecraft.client.Minecraft.getInstance();
        boolean localGunner = minecraftClient.player != null
                && minecraftClient.player.getVehicle() == entity
                && entity.seatOf(minecraftClient.player)
                == io.github.littlebird.entity.seat.GroundSeat.TANK_GUNNER;
        float cupolaYaw = localGunner
                ? minecraftClient.player.getViewYRot(partialTick)
                : entity.renderCupolaYawDeg(partialTick);
        float cupolaPitch = localGunner
                ? minecraftClient.player.getViewXRot(partialTick)
                : entity.renderCupolaPitchDeg(partialTick);
        cupola.yRot = ApacheModelMath.turretYawRadians(cupolaYaw,
                entity.renderTurretYawDeg(partialTick));
        cupola.xRot = ApacheModelMath.turretPitchRadians(cupolaPitch, entity.getXRot());
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue,
                               float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
