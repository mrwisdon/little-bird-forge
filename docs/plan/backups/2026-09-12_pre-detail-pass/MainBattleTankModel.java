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
 * M1A5 main battle tank, built from the reference images recorded in
 * docs/plan/ground-units-reference-images.md (orthographic M1A1 side drawing plus the BF2042 M1A5
 * render). Unit space is unchanged: nose along -Z, model +X is the right hand side, 16 units per
 * block, turret on the ring at the roof.
 *
 * <p>Stage 1 structure, stage 2 secondary fittings, stage 3 the surface breaks the reference makes
 * visible: track treads and wheel hubs, engine grille slats, turret roof seams, cupola periscopes
 * and the bore evacuator. Texture coordinates point at the zones painted by
 * tools/generate_ground_unit_textures.py (minimal flat-shaded palette, not a full art pass).
 */
public final class MainBattleTankModel extends EntityModel<MainBattleTankEntity> {
    public static final ModelLayerLocation MAIN_BATTLE_TANK_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "main_battle_tank"), "main");
    private static final int TEXTURE_SIZE = 512;

    private static final int ROAD_WHEELS = 7;
    private static final float WHEEL_FRONT_Z = -42.0F;
    private static final float WHEEL_PITCH = 14.0F;
    private static final int SKIRT_PANELS = 6;
    private static final float SKIRT_PITCH = 17.0F;
    /** Track tread blocks a side. */
    private static final int TREADS = 9;
    /** Engine grille slats. */
    private static final int GRILLE_SLATS = 8;

    private final ModelPart root;
    private final ModelPart turret;

    public MainBattleTankModel(ModelPart root) {
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
        addTurret(body);

        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static void addHull(PartDefinition body) {
        body.addOrReplaceChild("hull_lower", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-26.0F, 8.0F, -50.0F, 52.0F, 18.0F, 100.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_upper", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.0F, 26.0F, -46.0F, 56.0F, 8.0F, 92.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glacis_lower", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-26.0F, 14.0F, -58.0F, 52.0F, 12.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glacis_upper", CubeListBuilder.create()
                .texOffs(128, 0).addBox(-24.0F, 22.0F, -62.0F, 48.0F, 10.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("engine_deck", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-26.0F, 34.0F, 16.0F, 52.0F, 6.0F, 34.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("rear_plate", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-26.0F, 10.0F, 50.0F, 52.0F, 24.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    private static void addRunningGear(PartDefinition body) {
        body.addOrReplaceChild("track_left", CubeListBuilder.create()
                .texOffs(320, 0).addBox(-31.0F, 0.0F, -54.0F, 5.0F, 14.0F, 108.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_right", CubeListBuilder.create()
                .texOffs(320, 0).addBox(26.0F, 0.0F, -54.0F, 5.0F, 14.0F, 108.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int tread = 0; tread < TREADS; tread++) {
            float z = -50.0F + tread * 12.0F;
            body.addOrReplaceChild("tread_left_" + tread, CubeListBuilder.create()
                    .texOffs(360, 32).addBox(-31.0F, 14.0F, z, 5.0F, 2.0F, 8.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("tread_right_" + tread, CubeListBuilder.create()
                    .texOffs(360, 32).addBox(26.0F, 14.0F, z, 5.0F, 2.0F, 8.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        for (int wheel = 0; wheel < ROAD_WHEELS; wheel++) {
            float z = WHEEL_FRONT_Z + wheel * WHEEL_PITCH - 5.0F;
            body.addOrReplaceChild("wheel_left_" + wheel, CubeListBuilder.create()
                    .texOffs(320, 64).addBox(-30.0F, 2.0F, z, 5.0F, 10.0F, 10.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("wheel_right_" + wheel, CubeListBuilder.create()
                    .texOffs(320, 64).addBox(25.0F, 2.0F, z, 5.0F, 10.0F, 10.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("hub_left_" + wheel, CubeListBuilder.create()
                    .texOffs(0, 192).addBox(-32.0F, 6.0F, z + 4.0F, 1.0F, 2.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("hub_right_" + wheel, CubeListBuilder.create()
                    .texOffs(0, 192).addBox(31.0F, 6.0F, z + 4.0F, 1.0F, 2.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        body.addOrReplaceChild("sprocket_left", CubeListBuilder.create()
                .texOffs(0, 192).addBox(-30.0F, 3.0F, 44.0F, 5.0F, 12.0F, 9.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sprocket_right", CubeListBuilder.create()
                .texOffs(0, 192).addBox(25.0F, 3.0F, 44.0F, 5.0F, 12.0F, 9.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_left", CubeListBuilder.create()
                .texOffs(0, 192).addBox(-30.0F, 3.0F, -53.0F, 5.0F, 12.0F, 9.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("idler_right", CubeListBuilder.create()
                .texOffs(0, 192).addBox(25.0F, 3.0F, -53.0F, 5.0F, 12.0F, 9.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    private static void addFittings(PartDefinition body) {
        for (int panel = 0; panel < SKIRT_PANELS; panel++) {
            float z = -50.0F + panel * SKIRT_PITCH;
            body.addOrReplaceChild("skirt_left_" + panel, CubeListBuilder.create()
                    .texOffs(0, 128).addBox(-32.0F, 10.0F, z, 3.0F, 16.0F, 15.0F)
                    .texOffs(0, 192).addBox(-33.0F, 12.0F, z + 3.0F, 1.0F, 12.0F, 1.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("skirt_right_" + panel, CubeListBuilder.create()
                    .texOffs(0, 128).addBox(29.0F, 10.0F, z, 3.0F, 16.0F, 15.0F)
                    .texOffs(0, 192).addBox(32.0F, 12.0F, z + 3.0F, 1.0F, 12.0F, 1.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        body.addOrReplaceChild("headlight_left", CubeListBuilder.create()
                .texOffs(448, 128).addBox(-27.0F, 24.0F, -60.0F, 6.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("headlight_right", CubeListBuilder.create()
                .texOffs(448, 128).addBox(21.0F, 24.0F, -60.0F, 6.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tow_hook_left", CubeListBuilder.create()
                .texOffs(64, 256).addBox(-10.0F, 18.0F, -64.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tow_hook_right", CubeListBuilder.create()
                .texOffs(64, 256).addBox(6.0F, 18.0F, -64.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Stage 3: the engine deck reads as slats, not one plate.
        for (int slat = 0; slat < GRILLE_SLATS; slat++) {
            body.addOrReplaceChild("grille_slat_" + slat, CubeListBuilder.create()
                    .texOffs(320, 32).addBox(-22.0F, 40.0F, 18.0F + slat * 4.0F, 44.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        body.addOrReplaceChild("exhaust", CubeListBuilder.create()
                .texOffs(64, 192).addBox(-32.0F, 14.0F, 52.0F, 6.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    private static void addTurret(PartDefinition body) {
        PartDefinition turret = body.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-16.0F, 0.0F, -20.0F, 32.0F, 16.0F, 40.0F)
                .texOffs(0, 64).addBox(-14.0F, 16.0F, -18.0F, 28.0F, 1.0F, 1.0F)
                .texOffs(0, 64).addBox(-14.0F, 16.0F, 14.0F, 28.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 34.0F, 0.0F));
        turret.addOrReplaceChild("bustle", CubeListBuilder.create()
                .texOffs(160, 64).addBox(-14.0F, 2.0F, 20.0F, 28.0F, 12.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("mantlet", CubeListBuilder.create()
                .texOffs(0, 256).addBox(-8.0F, 2.0F, -26.0F, 16.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(64, 256).addBox(-3.0F, 4.0F, -74.0F, 6.0F, 6.0F, 48.0F)
                .texOffs(144, 256).addBox(-4.0F, 3.0F, -66.0F, 8.0F, 8.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("thermal_sleeve", CubeListBuilder.create()
                .texOffs(320, 128).addBox(-4.0F, 3.0F, -60.0F, 8.0F, 8.0F, 20.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("muzzle", CubeListBuilder.create()
                .texOffs(128, 256).addBox(-4.0F, 3.0F, -78.0F, 8.0F, 8.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("cupola", CubeListBuilder.create()
                .texOffs(64, 64).addBox(-8.0F, 16.0F, -8.0F, 14.0F, 8.0F, 14.0F)
                .texOffs(320, 192).addBox(-6.0F, 24.0F, -10.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(320, 192).addBox(-2.0F, 24.0F, -10.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(320, 192).addBox(2.0F, 24.0F, -10.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("loader_hatch", CubeListBuilder.create()
                .texOffs(192, 64).addBox(6.0F, 16.0F, 0.0F, 10.0F, 3.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_left", CubeListBuilder.create()
                .texOffs(384, 192).addBox(-18.0F, 8.0F, -18.0F, 4.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("smoke_right", CubeListBuilder.create()
                .texOffs(384, 192).addBox(14.0F, 8.0F, -18.0F, 4.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("commander_gun", CubeListBuilder.create()
                .texOffs(160, 256).addBox(-2.0F, 24.0F, -8.0F, 4.0F, 6.0F, 6.0F)
                .texOffs(160, 256).addBox(-1.0F, 26.0F, -24.0F, 2.0F, 2.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("gunner_sight", CubeListBuilder.create()
                .texOffs(320, 192).addBox(-10.0F, 16.0F, -16.0F, 8.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("wind_sensor", CubeListBuilder.create()
                .texOffs(96, 192).addBox(-2.0F, 16.0F, 30.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antenna_left", CubeListBuilder.create()
                .texOffs(128, 192).addBox(-12.0F, 16.0F, 26.0F, 1.0F, 22.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antenna_right", CubeListBuilder.create()
                .texOffs(128, 192).addBox(10.0F, 16.0F, 28.0F, 1.0F, 18.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    @Override
    public void setupAnim(MainBattleTankEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
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
