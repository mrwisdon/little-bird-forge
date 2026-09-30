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
 * M1A5 main battle tank. References: docs/plan/ground-units-reference-images.md (orthographic M1A1
 * side drawing plus the BF2042 M1A5 render).
 *
 * <p>Detail pass A with a low-slung profile: the hull and turret sit lower than the first pass (hull
 * top 28 units instead of 34) and the 120mm gun reaches further forward, so the length-to-height
 * silhouette reads closer to the reference. Rotated sub-parts give real sloped armour: the glacis is
 * a plate hinged at the hull roof line and the turret front cheeks are yawed plates.
 *
 * <p>Unit space is unchanged: 16 units per block, nose along -Z, +X to the vehicle's right. The
 * turret ring height and the barrel reach are mirrored in MainBattleTankGeometry so the server-side
 * muzzle and the crew seats stay aligned with this mesh.
 */
public final class MainBattleTankModel extends EntityModel<MainBattleTankEntity> {
    public static final ModelLayerLocation MAIN_BATTLE_TANK_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "main_battle_tank"), "main");
    private static final int TEXTURE_SIZE = 512;
    private static final float DEG = (float) (Math.PI / 180.0D);

    private static final int ROAD_WHEELS = 7;
    private static final float WHEEL_FRONT_Z = -42.0F;
    private static final float WHEEL_PITCH = 14.0F;
    private static final int SKIRT_PANELS = 6;
    private static final float SKIRT_PITCH = 17.0F;
    private static final int TRACK_LINKS = 18;
    private static final int ERA_SIDE_BLOCKS = 12;
    private static final int SMOKE_TUBES = 6;
    private static final int PERISCOPES = 6;
    /** Turret ring height, matching MainBattleTankGeometry.TURRET_PIVOT_HEIGHT_UNITS. */
    private static final float TURRET_Y = 28.0F;

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
        addGlacis(body);
        addRunningGear(body);
        addSkirtsAndArmour(body);
        addFittings(body);
        addTurret(body);
        addDetails(body);

        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /** Lower hull, upper hull, rear plate and engine deck, all dropped for the low profile. */
    private static void addHull(PartDefinition body) {
        body.addOrReplaceChild("hull_lower", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-26.0F, 8.0F, -50.0F, 52.0F, 14.0F, 100.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("hull_upper", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.0F, 22.0F, -46.0F, 56.0F, 6.0F, 92.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("rear_plate", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-26.0F, 10.0F, 50.0F, 52.0F, 18.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("engine_deck", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-26.0F, 28.0F, 16.0F, 52.0F, 5.0F, 34.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int slat = 0; slat < 8; slat++) {
            body.addOrReplaceChild("grille_slat_" + slat, CubeListBuilder.create()
                    .texOffs(320, 32).addBox(-22.0F, 33.0F, 18.0F + slat * 4.0F, 44.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
    }

    /** Hinged upper front plate, and the lower nose plate that closes the gap under it. */
    private static void addGlacis(PartDefinition body) {
        body.addOrReplaceChild("glacis", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-26.0F, -3.0F, -26.0F, 52.0F, 6.0F, 26.0F)
                .texOffs(160, 0).addBox(-25.0F, 3.0F, -24.0F, 50.0F, 3.0F, 22.0F),
                PartPose.offsetAndRotation(0.0F, 28.0F, -46.0F, -38.0F * DEG, 0.0F, 0.0F));
        body.addOrReplaceChild("nose_plate", CubeListBuilder.create()
                .texOffs(160, 32).addBox(-26.0F, 12.0F, -62.0F, 52.0F, 10.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    private static void addRunningGear(PartDefinition body) {
        body.addOrReplaceChild("track_left", CubeListBuilder.create()
                .texOffs(320, 0).addBox(-31.0F, 0.0F, -54.0F, 5.0F, 14.0F, 108.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_right", CubeListBuilder.create()
                .texOffs(320, 0).addBox(26.0F, 0.0F, -54.0F, 5.0F, 14.0F, 108.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int link = 0; link < TRACK_LINKS; link++) {
            float z = -52.0F + link * 6.0F;
            body.addOrReplaceChild("link_left_" + link, CubeListBuilder.create()
                    .texOffs(360, 32).addBox(-31.0F, 14.0F, z, 5.0F, 3.0F, 5.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("link_right_" + link, CubeListBuilder.create()
                    .texOffs(360, 32).addBox(26.0F, 14.0F, z, 5.0F, 3.0F, 5.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        for (int wheel = 0; wheel < ROAD_WHEELS; wheel++) {
            float z = WHEEL_FRONT_Z + wheel * WHEEL_PITCH - 5.0F;
            body.addOrReplaceChild("tyre_left_" + wheel, CubeListBuilder.create()
                    .texOffs(320, 64).addBox(-30.0F, 2.0F, z, 5.0F, 10.0F, 10.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("tyre_right_" + wheel, CubeListBuilder.create()
                    .texOffs(320, 64).addBox(25.0F, 2.0F, z, 5.0F, 10.0F, 10.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("hub_left_" + wheel, CubeListBuilder.create()
                    .texOffs(0, 192).addBox(-32.0F, 5.0F, z + 3.0F, 3.0F, 4.0F, 4.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("hub_right_" + wheel, CubeListBuilder.create()
                    .texOffs(0, 192).addBox(29.0F, 5.0F, z + 3.0F, 3.0F, 4.0F, 4.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        for (int side = 0; side < 2; side++) {
            float x = side == 0 ? -31.0F : 26.0F;
            String s = side == 0 ? "left" : "right";
            body.addOrReplaceChild("sprocket_" + s, CubeListBuilder.create()
                    .texOffs(0, 192).addBox(x, 3.0F, 44.0F, 5.0F, 12.0F, 9.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("idler_" + s, CubeListBuilder.create()
                    .texOffs(0, 192).addBox(x, 3.0F, -53.0F, 5.0F, 12.0F, 9.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            for (int tooth = 0; tooth < 4; tooth++) {
                float z = tooth < 2 ? 44.0F + tooth * 5.0F : -48.0F + (tooth - 2) * 5.0F;
                body.addOrReplaceChild("tooth_" + s + "_" + tooth, CubeListBuilder.create()
                        .texOffs(0, 192).addBox(x, 1.0F, z, 5.0F, 2.0F, 4.0F),
                        PartPose.offset(0.0F, 0.0F, 0.0F));
            }
        }
    }

    private static void addSkirtsAndArmour(PartDefinition body) {
        for (int panel = 0; panel < SKIRT_PANELS; panel++) {
            float z = -50.0F + panel * SKIRT_PITCH;
            body.addOrReplaceChild("skirt_left_" + panel, CubeListBuilder.create()
                    .texOffs(0, 128).addBox(-32.0F, 10.0F, z, 3.0F, 12.0F, 15.0F)
                    .texOffs(0, 192).addBox(-33.0F, 11.0F, z + 3.0F, 1.0F, 9.0F, 1.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("skirt_right_" + panel, CubeListBuilder.create()
                    .texOffs(0, 128).addBox(29.0F, 10.0F, z, 3.0F, 12.0F, 15.0F)
                    .texOffs(0, 192).addBox(32.0F, 11.0F, z + 3.0F, 1.0F, 9.0F, 1.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        for (int block = 0; block < ERA_SIDE_BLOCKS; block++) {
            float z = -42.0F + block * 6.0F;
            body.addOrReplaceChild("era_left_" + block, CubeListBuilder.create()
                    .texOffs(0, 128).addBox(-31.0F, 22.0F, z, 3.0F, 5.0F, 4.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("era_right_" + block, CubeListBuilder.create()
                    .texOffs(0, 128).addBox(28.0F, 22.0F, z, 3.0F, 5.0F, 4.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
    }

    private static void addFittings(PartDefinition body) {
        body.addOrReplaceChild("headlight_left", CubeListBuilder.create()
                .texOffs(448, 128).addBox(-27.0F, 14.0F, -60.0F, 6.0F, 6.0F, 4.0F)
                .texOffs(64, 192).addBox(-28.0F, 13.0F, -61.0F, 8.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("headlight_right", CubeListBuilder.create()
                .texOffs(448, 128).addBox(21.0F, 14.0F, -60.0F, 6.0F, 6.0F, 4.0F)
                .texOffs(64, 192).addBox(20.0F, 13.0F, -61.0F, 8.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("taillight_left", CubeListBuilder.create()
                .texOffs(384, 192).addBox(-24.0F, 18.0F, 52.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("taillight_right", CubeListBuilder.create()
                .texOffs(384, 192).addBox(20.0F, 18.0F, 52.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tow_hook_left", CubeListBuilder.create()
                .texOffs(64, 256).addBox(-10.0F, 14.0F, -64.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tow_hook_right", CubeListBuilder.create()
                .texOffs(64, 256).addBox(6.0F, 14.0F, -64.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tow_cable", CubeListBuilder.create()
                .texOffs(64, 192).addBox(-30.0F, 24.0F, -34.0F, 1.0F, 1.0F, 30.0F)
                .texOffs(64, 192).addBox(29.0F, 24.0F, -34.0F, 1.0F, 1.0F, 30.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pioneer_tool_a", CubeListBuilder.create()
                .texOffs(64, 192).addBox(-24.0F, 33.0F, 42.0F, 20.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pioneer_tool_b", CubeListBuilder.create()
                .texOffs(64, 192).addBox(6.0F, 33.0F, 42.0F, 16.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("exhaust", CubeListBuilder.create()
                .texOffs(64, 192).addBox(-32.0F, 14.0F, 52.0F, 6.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int handle = 0; handle < 3; handle++) {
            float z = -30.0F + handle * 24.0F;
            body.addOrReplaceChild("grab_left_" + handle, CubeListBuilder.create()
                    .texOffs(0, 192).addBox(-29.0F, 28.0F, z, 1.0F, 1.0F, 8.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("grab_right_" + handle, CubeListBuilder.create()
                    .texOffs(0, 192).addBox(28.0F, 28.0F, z, 1.0F, 1.0F, 8.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
    }

    /** Flatter turret on the lowered ring, with the lengthened 120mm gun. */
    private static void addTurret(PartDefinition body) {
        PartDefinition turret = body.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-16.0F, 0.0F, -20.0F, 32.0F, 14.0F, 40.0F),
                PartPose.offset(0.0F, TURRET_Y, 0.0F));
        turret.addOrReplaceChild("cheek_left", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-4.0F, -7.0F, -18.0F, 8.0F, 14.0F, 18.0F),
                PartPose.offsetAndRotation(-10.0F, 7.0F, -18.0F, 0.0F, 30.0F * DEG, 0.0F));
        turret.addOrReplaceChild("cheek_right", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-4.0F, -7.0F, -18.0F, 8.0F, 14.0F, 18.0F),
                PartPose.offsetAndRotation(10.0F, 7.0F, -18.0F, 0.0F, -30.0F * DEG, 0.0F));
        for (int panel = 0; panel < 4; panel++) {
            turret.addOrReplaceChild("blow_off_" + panel, CubeListBuilder.create()
                    .texOffs(0, 64).addBox(-14.0F + panel * 7.0F, 14.0F, -6.0F, 6.0F, 2.0F, 18.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        turret.addOrReplaceChild("bustle", CubeListBuilder.create()
                .texOffs(160, 64).addBox(-14.0F, 2.0F, 20.0F, 28.0F, 10.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int bar = 0; bar < 4; bar++) {
            turret.addOrReplaceChild("rack_bar_" + bar, CubeListBuilder.create()
                    .texOffs(64, 192).addBox(-15.0F, 2.0F + bar * 3.0F, 34.0F, 30.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        turret.addOrReplaceChild("stowage_a", CubeListBuilder.create()
                .texOffs(0, 128).addBox(-14.0F, 2.0F, 35.0F, 12.0F, 9.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("stowage_b", CubeListBuilder.create()
                .texOffs(0, 128).addBox(2.0F, 2.0F, 35.0F, 12.0F, 9.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("mantlet", CubeListBuilder.create()
                .texOffs(0, 256).addBox(-8.0F, 1.0F, -26.0F, 16.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Lengthened 120mm gun: barrel to -92, bore evacuator, thermal sleeve, muzzle to -96.
        turret.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(64, 256).addBox(-3.0F, 3.0F, -92.0F, 6.0F, 6.0F, 66.0F)
                .texOffs(144, 256).addBox(-4.0F, 2.0F, -86.0F, 8.0F, 8.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("thermal_sleeve", CubeListBuilder.create()
                .texOffs(320, 128).addBox(-4.0F, 2.0F, -78.0F, 8.0F, 8.0F, 22.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("muzzle", CubeListBuilder.create()
                .texOffs(128, 256).addBox(-4.0F, 2.0F, -96.0F, 8.0F, 8.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("cupola", CubeListBuilder.create()
                .texOffs(64, 64).addBox(-8.0F, 14.0F, -8.0F, 14.0F, 8.0F, 14.0F)
                .texOffs(0, 64).addBox(-6.0F, 22.0F, -6.0F, 10.0F, 2.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int scope = 0; scope < PERISCOPES; scope++) {
            float a = (float) (2.0D * Math.PI * scope / PERISCOPES);
            float px = (float) (Math.sin(a) * 7.0D);
            float pz = (float) (Math.cos(a) * 7.0D);
            turret.addOrReplaceChild("periscope_" + scope, CubeListBuilder.create()
                    .texOffs(320, 192).addBox(px - 1.0F, 19.0F, pz - 1.0F, 2.0F, 3.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        turret.addOrReplaceChild("fifty_mount", CubeListBuilder.create()
                .texOffs(160, 256).addBox(-2.0F, 22.0F, -8.0F, 4.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("fifty_receiver", CubeListBuilder.create()
                .texOffs(160, 256).addBox(-2.0F, 27.0F, -14.0F, 4.0F, 4.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("fifty_barrel", CubeListBuilder.create()
                .texOffs(160, 256).addBox(-1.0F, 28.0F, -28.0F, 2.0F, 2.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("loader_hatch", CubeListBuilder.create()
                .texOffs(192, 64).addBox(4.0F, 14.0F, -2.0F, 12.0F, 3.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("m240_mount", CubeListBuilder.create()
                .texOffs(160, 256).addBox(8.0F, 17.0F, -4.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("m240_barrel", CubeListBuilder.create()
                .texOffs(160, 256).addBox(9.0F, 18.0F, -16.0F, 1.0F, 1.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int tube = 0; tube < SMOKE_TUBES; tube++) {
            float tx = -17.0F + (tube % 2) * 4.0F;
            float ty = 6.0F + (tube / 2) * 4.0F;
            turret.addOrReplaceChild("smoke_left_" + tube, CubeListBuilder.create()
                    .texOffs(384, 192).addBox(tx, ty, -20.0F, 3.0F, 3.0F, 5.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            turret.addOrReplaceChild("smoke_right_" + tube, CubeListBuilder.create()
                    .texOffs(384, 192).addBox(-tx - 3.0F, ty, -20.0F, 3.0F, 3.0F, 5.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        turret.addOrReplaceChild("gunner_sight", CubeListBuilder.create()
                .texOffs(320, 192).addBox(-10.0F, 14.0F, -16.0F, 8.0F, 6.0F, 9.0F)
                .texOffs(320, 192).addBox(-8.0F, 15.0F, -17.0F, 4.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("wind_sensor", CubeListBuilder.create()
                .texOffs(96, 192).addBox(-2.0F, 14.0F, 30.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("gps_antenna", CubeListBuilder.create()
                .texOffs(96, 192).addBox(6.0F, 14.0F, 24.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antenna_left", CubeListBuilder.create()
                .texOffs(128, 192).addBox(-12.0F, 14.0F, 26.0F, 1.0F, 21.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("antenna_right", CubeListBuilder.create()
                .texOffs(128, 192).addBox(10.0F, 14.0F, 28.0F, 1.0F, 17.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Spare track links stowed on the turret flanks, the classic M1 look.
        for (int link = 0; link < 4; link++) {
            float z = -2.0F + link * 6.0F;
            turret.addOrReplaceChild("spare_link_left_" + link, CubeListBuilder.create()
                    .texOffs(360, 32).addBox(-18.0F, 2.0F, z, 2.0F, 8.0F, 4.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            turret.addOrReplaceChild("spare_link_right_" + link, CubeListBuilder.create()
                    .texOffs(360, 32).addBox(16.0F, 2.0F, z, 2.0F, 8.0F, 4.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
    }

    /** Mud flaps behind the tracks, a small silhouette detail the hull was missing. */
    private static void addDetails(PartDefinition body) {
        for (int sign = -1; sign <= 1; sign += 2) {
            String side = sign < 0 ? "left" : "right";
            body.addOrReplaceChild("mud_flap_" + side, CubeListBuilder.create()
                    .texOffs(0, 128).addBox(sign < 0 ? -31.0F : 26.0F, 0.0F, 54.0F,
                            5.0F, 10.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
    }

    @Override
    public void setupAnim(MainBattleTankEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float partialTick = ageInTicks - entity.tickCount;
        turret.yRot = ApacheModelMath.turretYawRadians(
                entity.renderTurretYawDeg(partialTick), entity.getYRot());
        turret.xRot = ApacheModelMath.turretPitchRadians(
                entity.renderTurretPitchDeg(partialTick), entity.getXRot());
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue,
                               float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
