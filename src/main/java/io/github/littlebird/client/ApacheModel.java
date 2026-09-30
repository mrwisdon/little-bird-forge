package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.ApacheGeometry;
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
 * AH-64 model: tandem cockpit, chin turret, stub wings with a rocket pod and a missile rail per
 * side, wheeled gear, four blade rotor and the mast mounted Longbow radar.
 *
 * Built in the same unit space as the Little Bird (nose along -Z, one unscaled block is 16
 * units, the renderer applies SCALE) so the two aircraft can share conventions and maths.
 * Model +X is the aircraft's right, and left hand parts are named accordingly.
 */
public final class ApacheModel extends EntityModel<ApacheEntity> {
    public static final ModelLayerLocation APACHE_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "apache"), "main");
    private static final int TEXTURE_SIZE = 512;
    /** Nineteen tube rocket pods, one per outer pylon. */
    public static final int ROCKET_TUBES = 19;
    /** Row sizes of a 19 tube pod face: 4 then three rows of 5. */
    private static final int[] TUBE_ROWS = {4, 5, 5, 5};
    private static final float QUARTER_TURN = (float) (Math.PI / 2.0D);

    private final ModelPart root;
    private final ModelPart mainRotor;
    private final ModelPart tailRotor;
    private final ModelPart rotorBlur;
    private final ModelPart glass;
    private final ModelPart radar;
    private final ModelPart turret;
    private final ModelPart[] podTubesLeft = new ModelPart[ROCKET_TUBES];
    private final ModelPart[] podTubesRight = new ModelPart[ROCKET_TUBES];
    private final ModelPart missileLeft;
    private final ModelPart missileRight;
    private final ModelPart barrelGlow;
    private final float[] heatRgba = new float[4];

    public ApacheModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.mainRotor = body.getChild("main_rotor");
        this.tailRotor = body.getChild("tail_rotor");
        this.rotorBlur = body.getChild("rotor_blur");
        this.rotorBlur.visible = false;
        this.glass = body.getChild("glass");
        this.glass.visible = false;
        this.radar = body.getChild("radar");
        this.turret = body.getChild("turret");
        this.missileLeft = body.getChild("missile_left");
        this.missileRight = body.getChild("missile_right");
        this.barrelGlow = body.getChild("turret").getChild("barrel_glow");
        this.barrelGlow.visible = false;
        for (int tube = 0; tube < ROCKET_TUBES; tube++) {
            this.podTubesLeft[tube] = body.getChild("pod_left").getChild("tip_" + tube);
            this.podTubesRight[tube] = body.getChild("pod_right").getChild("tip_" + tube);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        addAirframe(body);
        addCanopy(body);
        addEngines(body);
        addWings(body);
        addTurret(body);
        addGear(body);
        addTail(body);
        addRotorAndRadar(body);
        addDetails(body);

        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static void addAirframe(PartDefinition body) {
        // Band A: dark olive airframe panels.
        body.addOrReplaceChild("belly", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.0F, ApacheGeometry.BELLY_BOTTOM_UNITS, -30.0F,
                        20.0F, 2.0F, 34.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("cabin", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11.0F, 10.0F, -32.0F, 22.0F, 20.0F, 36.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roof", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.0F, 30.0F, -32.0F, 20.0F, 6.0F, 38.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Stepped nose: a long pointed snout with the sensor turret underneath.
        body.addOrReplaceChild("nose_lower", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-7.0F, 10.0F, ApacheGeometry.NOSE_TIP_UNITS + 8.0F,
                        14.0F, 10.0F, 30.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("nose_upper", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-8.0F, 20.0F, ApacheGeometry.NOSE_TIP_UNITS + 12.0F,
                        16.0F, 8.0F, 26.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("sensor_turret", CubeListBuilder.create()
                .texOffs(288, 400).addBox(-4.0F, 14.0F, ApacheGeometry.NOSE_TIP_UNITS,
                        8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("antenna", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-0.5F, 36.0F, -4.0F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    /** Smoke tinted glazing plus the frames that keep the tandem cockpit readable. */
    private static void addCanopy(PartDefinition body) {
        PartDefinition glass = body.addOrReplaceChild("glass", CubeListBuilder.create()
                .texOffs(0, 112).addBox(-11.5F, 20.0F, -45.0F, 23.0F, 12.0F, 42.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("canopy_frame", CubeListBuilder.create()
                .texOffs(0, 112).addBox(-11.0F, 31.0F, -45.0F, 22.0F, 2.0F, 42.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Blast shield between the gunner and the pilot, the giveaway of a tandem cockpit.
        body.addOrReplaceChild("blast_shield", CubeListBuilder.create()
                .texOffs(0, 112).addBox(-9.0F, 20.0F, -18.0F, 18.0F, 11.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("dashboard_front", CubeListBuilder.create()
                .texOffs(300, 400).addBox(-8.0F, 20.0F, -44.0F, 16.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("dashboard_rear", CubeListBuilder.create()
                .texOffs(300, 400).addBox(-8.0F, 20.0F, -24.0F, 16.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    private static void addEngines(PartDefinition body) {
        for (int sign = -1; sign <= 1; sign += 2) {
            String side = sign < 0 ? "left" : "right";
            float x = 11.0F * sign;
            body.addOrReplaceChild("engine_" + side, CubeListBuilder.create()
                    .texOffs(0, 0).addBox(sign < 0 ? -20.0F : 12.0F, 18.0F, -8.0F,
                            8.0F, 14.0F, 32.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("exhaust_" + side, CubeListBuilder.create()
                    .texOffs(0, 208).addBox(sign < 0 ? -19.0F : 13.0F, 24.0F, 24.0F,
                            5.0F, 5.0F, 6.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
    }

    /** Stub wings: an outer 19 tube pod and an inner missile rail per side. */
    private static void addWings(PartDefinition body) {
        addWing(body, "left", -1);
        addWing(body, "right", 1);
    }

    private static void addWing(PartDefinition body, String side, int sign) {
        float outer = sign < 0 ? -30.0F : 10.0F;
        body.addOrReplaceChild("wing_" + side, CubeListBuilder.create()
                .texOffs(0, 0).addBox(outer, 20.0F, -10.0F, 20.0F, 4.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        float outerPylon = sign < 0 ? -28.0F : 24.0F;
        body.addOrReplaceChild("pylon_outer_" + side, CubeListBuilder.create()
                .texOffs(0, 208).addBox(outerPylon, 12.0F, -6.0F, 4.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        float podX = sign < 0 ? -36.0F : 23.0F;
        PartDefinition pod = body.addOrReplaceChild("pod_" + side, CubeListBuilder.create()
                .texOffs(0, 304).addBox(podX, 6.0F, -20.0F, 13.0F, 13.0F, 28.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        int tube = 0;
        for (int row = 0; row < TUBE_ROWS.length; row++) {
            int count = TUBE_ROWS[row];
            for (int column = 0; column < count; column++) {
                float spacing = 2.6F;
                float rowWidth = (count - 1) * spacing;
                float centre = podX + 6.5F + (column * spacing) - rowWidth * 0.5F;
                float y = 8.0F + row * 2.8F;
                pod.addOrReplaceChild("tip_" + tube, CubeListBuilder.create()
                        .texOffs(0, 336).addBox(centre, y, -21.5F, 2.0F, 2.0F, 2.0F),
                        PartPose.offset(0.0F, 0.0F, 0.0F));
                tube++;
            }
        }
        float innerPylon = sign < 0 ? -18.0F : 14.0F;
        body.addOrReplaceChild("pylon_inner_" + side, CubeListBuilder.create()
                .texOffs(0, 208).addBox(innerPylon, 14.0F, -6.0F, 4.0F, 6.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        float railX = sign < 0 ? -19.0F : 13.0F;
        body.addOrReplaceChild("rail_" + side, CubeListBuilder.create()
                .texOffs(0, 208).addBox(railX, 18.0F, -22.0F, 6.0F, 2.0F, 26.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        float missileX = sign < 0 ? -18.0F : 14.0F;
        body.addOrReplaceChild("missile_" + side, CubeListBuilder.create()
                .texOffs(0, 400).addBox(missileX, 12.0F, -18.0F, 4.0F, 4.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    /** Chin turret: the part that turns on its own, so the gunner can shoot off axis. */
    private static void addTurret(PartDefinition body) {
        PartDefinition turret = body.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(0, 400).addBox(-5.0F, 2.0F, -6.0F, 10.0F, 7.0F, 10.0F),
                PartPose.offset(0.0F, ApacheGeometry.TURRET_PIVOT_HEIGHT_UNITS,
                        -ApacheGeometry.TURRET_PIVOT_FORWARD_UNITS));
        turret.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(0, 400).addBox(-1.5F, 3.5F, -24.0F, 3.0F, 3.0F, 20.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Emissive muzzle sleeve: drawn only by the heat pass, so a cold barrel never glows.
        turret.addOrReplaceChild("barrel_glow", CubeListBuilder.create()
                .texOffs(0, 400).addBox(-2.0F, 3.0F, -26.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    /** Tricycle gear: the lowest geometry sits at y = 0, where the entity position is. */
    private static void addGear(PartDefinition body) {
        body.addOrReplaceChild("wheel_nose", CubeListBuilder.create()
                .texOffs(400, 400).addBox(-3.0F, 0.0F, -56.0F, 6.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int sign = -1; sign <= 1; sign += 2) {
            String side = sign < 0 ? "left" : "right";
            float x = sign < 0 ? -16.0F : 10.0F;
            body.addOrReplaceChild("wheel_" + side, CubeListBuilder.create()
                    .texOffs(400, 400).addBox(x, 0.0F, 2.0F, 6.0F, 8.0F, 10.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
    }

    private static void addTail(PartDefinition body) {
        body.addOrReplaceChild("tail_boom", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, 16.0F, ApacheGeometry.TAIL_BOOM_FRONT_UNITS,
                        10.0F, 10.0F, ApacheGeometry.tailBoomLengthUnits()),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-3.0F, 26.0F, ApacheGeometry.tailFinFrontUnits(),
                        6.0F, 22.0F, ApacheGeometry.TAIL_FIN_LENGTH_UNITS),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("stabilator", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-16.0F, 14.0F, ApacheGeometry.TAIL_TIP_UNITS - 10.0F,
                        32.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition tailRotor = body.addOrReplaceChild("tail_rotor", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(-6.0F, 40.0F, ApacheGeometry.TAIL_TIP_UNITS - 6.0F));
        for (int blade = 0; blade < 4; blade++) {
            tailRotor.addOrReplaceChild("blade_" + blade, CubeListBuilder.create()
                    .texOffs(300, 208).addBox(-1.0F, 1.0F, -1.5F, 2.0F, 12.0F, 3.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                            QUARTER_TURN * blade, 0.0F, 0.0F));
        }
    }

    private static void addRotorAndRadar(PartDefinition body) {
        PartDefinition rotor = body.addOrReplaceChild("main_rotor", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F)
                .texOffs(0, 208).addBox(-5.0F, -3.0F, -5.0F, 10.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 46.0F, -2.0F));
        for (int blade = 0; blade < 4; blade++) {
            rotor.addOrReplaceChild("blade_" + blade, CubeListBuilder.create()
                    .texOffs(300, 208).addBox(6.0F, -1.0F, -2.5F, 74.0F, 2.0F, 5.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F,
                            QUARTER_TURN * blade, 0.0F));
            // Blade grip, so the blades read as bolted to the hub instead of floating.
            rotor.addOrReplaceChild("grip_" + blade, CubeListBuilder.create()
                    .texOffs(0, 208).addBox(4.0F, -2.0F, -3.0F, 3.0F, 4.0F, 6.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F,
                            QUARTER_TURN * blade, 0.0F));
        }
        body.addOrReplaceChild("mast", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-2.5F, 36.0F, -2.0F, 5.0F, 10.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("radar_mast", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-2.0F, 46.0F, -1.5F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition radar = body.addOrReplaceChild("radar", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-11.0F, 52.0F, -11.0F, 22.0F, 4.0F, 22.0F)
                .texOffs(0, 208).addBox(-7.0F, 56.0F, -7.0F, 14.0F, 2.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition blur = body.addOrReplaceChild("rotor_blur", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-80.0F, -0.5F, -3.0F, 160.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 46.0F, -2.0F));
    }

    /**
     * Cosmetic detail the box layout lacks: probes, antennas, gear struts and end plates. Every
     * part stays inside the established envelope and above y = 0, so the geometry constants and
     * the "model rests on its feet" invariant are untouched.
     */
    private static void addDetails(PartDefinition body) {
        // Air data probe ahead of the sensor turret.
        body.addOrReplaceChild("nose_probe", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-0.5F, 20.0F,
                        ApacheGeometry.NOSE_TIP_UNITS - 6.0F, 1.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        for (int sign = -1; sign <= 1; sign += 2) {
            String side = sign < 0 ? "left" : "right";
            // APR-39 warning antennas on the nose cheeks and the fin.
            body.addOrReplaceChild("rwr_nose_" + side, CubeListBuilder.create()
                    .texOffs(160, 0).addBox(sign < 0 ? -9.0F : 8.0F, 12.0F, -56.0F,
                            1.0F, 5.0F, 6.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("rwr_tail_" + side, CubeListBuilder.create()
                    .texOffs(0, 208).addBox(sign < 0 ? -4.0F : 3.0F, 30.0F, 82.0F,
                            1.0F, 4.0F, 6.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // Main gear struts from the belly down to the wheels.
            body.addOrReplaceChild("strut_main_" + side, CubeListBuilder.create()
                    .texOffs(400, 400).addBox(sign < 0 ? -13.0F : 11.0F, 6.0F, 4.0F,
                            2.0F, 6.0F, 4.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // Stabilator end plates.
            body.addOrReplaceChild("stabilator_plate_" + side, CubeListBuilder.create()
                    .texOffs(160, 0).addBox(sign < 0 ? -16.0F : 14.0F, 14.0F,
                            ApacheGeometry.TAIL_TIP_UNITS - 10.0F, 2.0F, 7.0F, 8.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // Wing tip fairing and its formation light.
            body.addOrReplaceChild("wing_tip_" + side, CubeListBuilder.create()
                    .texOffs(0, 0).addBox(sign < 0 ? -32.0F : 30.0F, 19.0F, -8.0F,
                            2.0F, 6.0F, 12.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("formation_light_" + side, CubeListBuilder.create()
                    .texOffs(440, 208).addBox(sign < 0 ? -33.0F : 32.0F, 21.0F, -4.0F,
                            1.0F, 2.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // Chaff dispenser on the boom.
            body.addOrReplaceChild("dispenser_" + side, CubeListBuilder.create()
                    .texOffs(0, 208).addBox(sign < 0 ? -6.0F : 5.0F, 18.0F, 22.0F,
                            1.0F, 4.0F, 10.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // Dark exhaust sleeve behind the IR suppressed outlet.
            body.addOrReplaceChild("exhaust_sleeve_" + side, CubeListBuilder.create()
                    .texOffs(0, 400).addBox(sign < 0 ? -18.5F : 13.5F, 24.5F, 29.0F,
                            4.0F, 4.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            // Engine intake lip.
            body.addOrReplaceChild("intake_lip_" + side, CubeListBuilder.create()
                    .texOffs(0, 0).addBox(sign < 0 ? -21.0F : 11.0F, 17.0F, -10.0F,
                            10.0F, 16.0F, 3.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        // Nose gear strut and drag brace.
        body.addOrReplaceChild("strut_nose", CubeListBuilder.create()
                .texOffs(400, 400).addBox(-1.5F, 6.0F, -54.0F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("brace_nose", CubeListBuilder.create()
                .texOffs(400, 400).addBox(-1.0F, 9.0F, -51.0F, 2.0F, 2.0F, 9.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Tail rotor gearbox fairing.
        body.addOrReplaceChild("gearbox_fairing", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-7.0F, 37.0F, 84.0F, 4.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    @Override
    public void setupAnim(ApacheEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float rpm = entity.getRenderRotorRpm();
        float spin = entity.getRenderRotorSpinFactor();
        mainRotor.yRot = LittleBirdModelMath.rotorAngleRadians(rpm, spin, ageInTicks);
        tailRotor.xRot = LittleBirdModelMath.tailRotorAngleRadians(rpm, ageInTicks)
                * ApacheModelMath.TAIL_ROTOR_MULTIPLIER;
        radar.yRot = ApacheModelMath.radarAngleRadians(ageInTicks);
        // The barrel's synced attitude is absolute (it follows the gunner's aim), so the swing
        // this mesh shows is measured against the airframe it is bolted to.
        turret.yRot = ApacheModelMath.turretYawRadians(
                entity.getTurretAimYawDeg(), entity.getYRot());
        turret.xRot = ApacheModelMath.turretPitchRadians(
                entity.getTurretAimPitchDeg(), entity.getXRot());
    }

    /** Shows only the tubes the crew still has loaded; pods empty in firing order. */
    public void setRocketState(int leftReady, int rightReady) {
        for (int tube = 0; tube < ROCKET_TUBES; tube++) {
            podTubesLeft[tube].visible = ApacheModelMath.tubeLoaded(tube, leftReady);
            podTubesRight[tube].visible = ApacheModelMath.tubeLoaded(tube, rightReady);
        }
    }

    /** Shows the loaded missiles on the inner rails. */
    public void setMissileState(int ready) {
        missileLeft.visible = ready >= 1;
        missileRight.visible = ready >= 2;
    }

    /** Emissive pass for the chain gun muzzle, faded in by barrel heat. */
    public void renderGunHeat(PoseStack pose, VertexConsumer consumer, int packedLight,
                              int packedOverlay, float heatFraction) {
        LittleBirdModelMath.heatGlowColor(heatFraction, heatRgba);
        if (heatRgba[3] <= 0.0F) {
            return;
        }
        barrelGlow.visible = true;
        barrelGlow.render(pose, consumer, packedLight, packedOverlay,
                heatRgba[0], heatRgba[1], heatRgba[2], heatRgba[3]);
        barrelGlow.visible = false;
    }

    /** Second pass: canopy glazing, tinted and see-through. */
    public void renderGlass(PoseStack pose, VertexConsumer consumer, int packedLight,
                            int packedOverlay, float alpha) {
        glass.visible = true;
        glass.render(pose, consumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, alpha);
        glass.visible = false;
    }

    /** Third pass: the translucent disc that reads as blade motion. */
    public void renderRotorBlur(PoseStack pose, VertexConsumer consumer, int packedLight,
                                int packedOverlay, float alpha) {
        rotorBlur.visible = true;
        rotorBlur.yRot = mainRotor.yRot;
        rotorBlur.render(pose, consumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, alpha);
        rotorBlur.visible = false;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /** Seat views hide the whole airframe: everything is close enough to block the camera. */
    public void setCockpitView(boolean cockpitView) {
        this.root.visible = !cockpitView;
    }
}
