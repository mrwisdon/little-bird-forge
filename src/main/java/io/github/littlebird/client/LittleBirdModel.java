package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.entity.LittleBirdGeometry;
import io.github.littlebird.entity.weapon.LittleBirdPodTubes;
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
 * True-scale AH-6/MH-6 style light helicopter: about 10 blocks of rotor
 * diameter, a 7 block airframe and a 2.6 block cabin, built from stepped boxes
 * to suggest the tapered nose, the faceted canopy and the tube skids.
 *
 * Every vertical coordinate is measured from the ground, and the skid tubes sit
 * at exactly y = 0 because a Minecraft entity position is the bottom of its
 * bounding box. See LittleBirdModelGeometry for the invariant and its tests.
 */
public final class LittleBirdModel extends EntityModel<LittleBirdEntity> {
    public static final ModelLayerLocation LITTLE_BIRD_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "little_bird"), "main");

    private static final int TEXTURE_SIZE = 512;
    private static final float QUARTER_TURN = (float) (Math.PI / 2.0D);

    private final ModelPart root;
    private final ModelPart cabin;
    private final ModelPart glass;
    private final ModelPart mainRotor;
    private final ModelPart tailRotor;
    private final ModelPart rotorBlur;
    private final ModelPart[] cockpitHiddenParts;
    private final ModelPart leftCluster;
    private final ModelPart rightCluster;
    private final ModelPart leftHeatTips;
    private final ModelPart rightHeatTips;
    private final float[] heatRgba = new float[4];
    private final ModelPart[] podTubesLeft = new ModelPart[LittleBirdPodTubes.TUBES];
    private final ModelPart[] podTubesRight = new ModelPart[LittleBirdPodTubes.TUBES];
    private float lastClusterAge;
    private float clusterAngle;

    /**
     * Everything that would surround or fill the cockpit camera. The view then
     * shows only the nose, the cowl rail, the slim side pillars, the dashboard and
     * the guns, which is what a pilot actually looks past.
     */
    private static final String[] COCKPIT_HIDDEN_PARTS = {
            "cabin", "roof", "belly", "frame_top", "pillar_centre", "nose_upper",
            "wing_left", "wing_right", "pod_left", "pod_right",
            "tail_boom", "tail_fin", "stabilizer", "tail_rotor",
            "main_rotor", "skid_left", "skid_right",
            "exhaust_left", "exhaust_right", "antenna"
    };

    public LittleBirdModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.cabin = body.getChild("cabin");
        this.glass = body.getChild("glass");
        this.mainRotor = body.getChild("main_rotor");
        this.tailRotor = body.getChild("tail_rotor");
        this.rotorBlur = body.getChild("rotor_blur");
        this.rotorBlur.visible = false;
        this.glass.visible = false;
        ModelPart[] hidden = new ModelPart[COCKPIT_HIDDEN_PARTS.length];
        for (int i = 0; i < hidden.length; i++) {
            hidden[i] = body.getChild(COCKPIT_HIDDEN_PARTS[i]);
        }
        this.cockpitHiddenParts = hidden;
        this.leftCluster = body.getChild("gun_left").getChild("cluster");
        this.rightCluster = body.getChild("gun_right").getChild("cluster");
        this.leftHeatTips = body.getChild("gun_left").getChild("heat_tips");
        this.rightHeatTips = body.getChild("gun_right").getChild("heat_tips");
        this.leftHeatTips.visible = false;
        this.rightHeatTips.visible = false;
        for (int tube = 0; tube < LittleBirdPodTubes.TUBES; tube++) {
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
        addTail(body);
        addWings(body);
        addGuns(body);
        addSkids(body);
        addCockpit(body);
        addRotor(body);

        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static void addAirframe(PartDefinition body) {
        // Cabin, belly and engine deck. UV band A: olive drab panels.
        body.addOrReplaceChild("cabin", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-13.0F, 12.0F, -30.0F, 26.0F, 22.0F, 44.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("belly", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11.0F, 10.0F, -28.0F, 22.0F, 2.0F, 40.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Stepped nose: two shrinking boxes read as a tapered nose in a blocky world.
        body.addOrReplaceChild("nose_lower", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-9.0F, 14.0F, -44.0F, 18.0F, 12.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("nose_upper", CubeListBuilder.create()
                .texOffs(160, 0).addBox(-8.0F, 24.0F, -40.0F, 16.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("roof", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11.0F, 34.0F, -32.0F, 22.0F, 6.0F, 46.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Engine exhausts, pitot tube and a blade antenna. UV band C: dark metal.
        body.addOrReplaceChild("exhaust_left", CubeListBuilder.create()
                .texOffs(0, 208).addBox(3.0F, 36.0F, 8.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("exhaust_right", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-7.0F, 36.0F, 8.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pitot", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-0.5F, 26.0F, -52.0F, 1.0F, 1.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("antenna", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-0.5F, 40.0F, -6.0F, 1.0F, 12.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    /**
     * Windscreen and side glazing, drawn in a translucent second pass so the pilot
     * can see through the front glass, plus an opaque frame that keeps the canopy
     * readable from outside.
     */
    private static void addCanopy(PartDefinition body) {
        PartDefinition glass = body.addOrReplaceChild("glass",
                CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        glass.addOrReplaceChild("windscreen", CubeListBuilder.create()
                .texOffs(0, 112).addBox(-11.5F, 20.0F, -38.0F, 23.0F, 13.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        glass.addOrReplaceChild("window_left", CubeListBuilder.create()
                .texOffs(0, 112).addBox(13.0F, 22.0F, -24.0F, 1.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        glass.addOrReplaceChild("window_right", CubeListBuilder.create()
                .texOffs(0, 112).addBox(-14.0F, 22.0F, -24.0F, 1.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("frame_top", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-12.0F, 32.0F, -39.0F, 24.0F, 2.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("frame_bottom", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-12.0F, 18.5F, -39.0F, 24.0F, 2.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pillar_left", CubeListBuilder.create()
                .texOffs(0, 208).addBox(11.0F, 20.0F, -39.0F, 2.0F, 14.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pillar_right", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-13.0F, 20.0F, -39.0F, 2.0F, 14.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("pillar_centre", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-1.0F, 20.0F, -38.5F, 2.0F, 13.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    private static void addTail(PartDefinition body) {
        body.addOrReplaceChild("tail_boom", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 26.0F, 14.0F, 8.0F, 8.0F, 56.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // The fin samples the painted hazard-stripe block of band A.
        body.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                .texOffs(400, 0).addBox(-1.5F, 34.0F, 60.0F, 3.0F, 20.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("stabilizer", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12.0F, 26.0F, 58.0F, 24.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("tail_rotor", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-1.0F, -11.0F, -1.0F, 2.0F, 22.0F, 2.0F)
                .texOffs(0, 208).addBox(-11.0F, -1.0F, -1.0F, 22.0F, 2.0F, 2.0F),
                PartPose.offset(-5.0F, 34.0F, 64.0F));
    }

    /**
     * Each stub wing carries two pylons: the machine gun inboard, the 70mm pod
     * outboard. They are separate parts so the two guns can alternate and every
     * station can be shown or hidden on its own.
     */
    private static void addWings(PartDefinition body) {
        float wingLength = LittleBirdGeometry.WING_TIP_UNITS - 13.0F;
        body.addOrReplaceChild("wing_left", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, wingLength, 4.0F, 16.0F),
                PartPose.offset(13.0F, 20.0F, -8.0F));
        body.addOrReplaceChild("wing_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-wingLength, 0.0F, 0.0F, wingLength, 4.0F, 16.0F),
                PartPose.offset(-13.0F, 20.0F, -8.0F));
        addPylon(body, "pylon_gun_left", LittleBirdGeometry.GUN_X_UNITS, 18.0F, 2.0F);
        addPylon(body, "pylon_gun_right", -LittleBirdGeometry.GUN_X_UNITS, 18.0F, 2.0F);
        addPylon(body, "pylon_pod_left", LittleBirdGeometry.POD_X_UNITS, 14.0F, 6.0F);
        addPylon(body, "pylon_pod_right", -LittleBirdGeometry.POD_X_UNITS, 14.0F, 6.0F);
        addPod(body, "pod_left", LittleBirdGeometry.POD_X_UNITS);
        addPod(body, "pod_right", -LittleBirdGeometry.POD_X_UNITS);
    }

    private static void addPylon(PartDefinition body, String name, float x, float y, float height) {
        body.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 208).addBox(-1.5F, y, -3.0F, 3.0F, height, 6.0F),
                PartPose.offset(x, 0.0F, -6.0F));
    }

    /**
     * One 70mm pod: seven tubes, one centre and six around it, each with its own
     * rocket tip so spent tubes can be hidden as they fire.
     */
    private static void addPod(PartDefinition body, String name, float x) {
        PartDefinition pod = body.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 304).addBox(-4.5F, -4.5F, -13.5F, 9.0F, 9.0F, 27.0F)
                .texOffs(0, 208).addBox(-2.5F, 4.5F, -4.0F, 5.0F, 1.5F, 6.0F)
                .texOffs(0, 208).addBox(-3.5F, -3.5F, 13.5F, 7.0F, 7.0F, 1.5F),
                PartPose.offset(x, LittleBirdGeometry.POD_Y_UNITS, -6.0F));
        for (int tube = 0; tube < LittleBirdPodTubes.TUBES; tube++) {
            float tubeX = LittleBirdPodTubes.offsetX(tube);
            float tubeY = LittleBirdPodTubes.offsetY(tube);
            pod.addOrReplaceChild("tube_" + tube, CubeListBuilder.create()
                    .texOffs(128, 400).addBox(tubeX - 1.0F, tubeY - 1.0F, -15.0F,
                            2.0F, 2.0F, 3.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
            pod.addOrReplaceChild("tip_" + tube, CubeListBuilder.create()
                    .texOffs(480, 304).addBox(tubeX - 0.7F, tubeY - 0.7F, -17.5F,
                            1.4F, 1.4F, 2.5F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }
    }

    private static void addGuns(PartDefinition body) {
        addGun(body, "gun_left", LittleBirdGeometry.GUN_X_UNITS);
        addGun(body, "gun_right", -LittleBirdGeometry.GUN_X_UNITS);
    }

    /**
     * One under-wing Gatling, detailed after the reference project's drone guns:
     * mount, receiver, magazine, grip, a six-tube cluster with a centre rod and a
     * muzzle clamp, plus heat-only muzzle sleeves that glow with gun heat.
     */
    private static void addGun(PartDefinition body, String name, float x) {
        PartDefinition gun = body.addOrReplaceChild(name, CubeListBuilder.create(),
                PartPose.offset(x, LittleBirdGeometry.GUN_Y_UNITS, -6.0F));
        gun.addOrReplaceChild("mount", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-1.5F, 2.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.ZERO);
        gun.addOrReplaceChild("receiver", CubeListBuilder.create()
                .texOffs(0, 400).addBox(-2.3F, -1.6F, -3.5F, 4.6F, 3.2F, 7.0F),
                PartPose.ZERO);
        gun.addOrReplaceChild("magazine", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-1.5F, -4.0F, -2.5F, 3.0F, 2.4F, 2.2F),
                PartPose.ZERO);
        gun.addOrReplaceChild("grip", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-0.9F, -3.0F, 2.0F, 1.8F, 1.5F, 1.8F),
                PartPose.ZERO);

        CubeListBuilder cluster = CubeListBuilder.create();
        CubeListBuilder heatTips = CubeListBuilder.create();
        for (int barrel = 0; barrel < 6; barrel++) {
            double angle = Math.toRadians(60.0D * barrel);
            float ringX = (float) (Math.cos(angle) * 1.24D);
            float ringY = (float) (Math.sin(angle) * 1.24D);
            cluster.texOffs(0, 400).addBox(ringX - 0.45F, ringY - 0.45F, -12.0F,
                    0.9F, 0.9F, 12.0F);
            heatTips.texOffs(0, 400).addBox(ringX - 0.52F, ringY - 0.52F, -12.6F,
                    1.05F, 1.05F, 1.6F);
        }
        cluster.texOffs(128, 400).addBox(-0.55F, -0.55F, -12.2F, 1.1F, 1.1F, 12.4F);
        cluster.texOffs(0, 208).addBox(-2.9F, -2.9F, -1.2F, 5.8F, 5.8F, 1.6F);
        gun.addOrReplaceChild("cluster", cluster, PartPose.offset(0.0F, -0.4F, -3.5F));
        gun.addOrReplaceChild("heat_tips", heatTips, PartPose.offset(0.0F, -0.4F, -3.5F));
    }
    private static void addSkids(PartDefinition body) {
        addSkid(body, "skid_left", LittleBirdGeometry.SKID_HALF_SPACING_UNITS);
        addSkid(body, "skid_right", -LittleBirdGeometry.SKID_HALF_SPACING_UNITS);
    }

    private static void addSkid(PartDefinition body, String name, float x) {
        // The tube bottom is exactly at y = 0: the entity's feet, not the model centre.
        PartDefinition skid = body.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 400).addBox(-1.5F, LittleBirdGeometry.SKID_BOTTOM_UNITS, -26.0F,
                        3.0F, LittleBirdGeometry.SKID_TUBE_HEIGHT_UNITS, 46.0F),
                PartPose.offset(x, 0.0F, 0.0F));
        skid.addOrReplaceChild("front_strut", CubeListBuilder.create()
                .texOffs(0, 400).addBox(-1.0F, LittleBirdGeometry.SKID_TUBE_HEIGHT_UNITS, -18.0F,
                        2.0F, 9.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        skid.addOrReplaceChild("rear_strut", CubeListBuilder.create()
                .texOffs(0, 400).addBox(-1.0F, LittleBirdGeometry.SKID_TUBE_HEIGHT_UNITS, 8.0F,
                        2.0F, 9.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    /** Dashboard the pilot looks over; only visible from the cockpit camera. */
    private static void addCockpit(PartDefinition body) {
        // Raised so it lands just below the sight line of the cockpit camera.
        body.addOrReplaceChild("dashboard", CubeListBuilder.create()
                .texOffs(0, 400).addBox(-9.0F, 20.0F, -30.0F, 18.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("dash_glare", CubeListBuilder.create()
                .texOffs(300, 400).addBox(-7.0F, 23.0F, -30.5F, 14.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
    }

    private static void addRotor(PartDefinition body) {
        PartDefinition rotor = body.addOrReplaceChild("main_rotor", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-2.0F, -(LittleBirdGeometry.ROTOR_HUB_UNITS
                        - LittleBirdGeometry.ROOF_TOP_UNITS), -2.0F, 4.0F,
                        LittleBirdGeometry.ROTOR_HUB_UNITS
                                - LittleBirdGeometry.ROOF_TOP_UNITS, 4.0F)
                .texOffs(0, 208).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, LittleBirdGeometry.ROTOR_HUB_UNITS, -6.0F));
        for (int blade = 0; blade < 4; blade++) {
            rotor.addOrReplaceChild("blade_" + blade, CubeListBuilder.create()
                    .texOffs(300, 208).addBox(6.0F, -1.0F, -2.5F, LittleBirdGeometry.ROTOR_RADIUS_UNITS
                            - 6.0F, 2.0F, 5.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, QUARTER_TURN * blade, 0.0F));
        }

        // Translucent stand-in for blade motion; drawn only by the blur pass.
        PartDefinition blur = body.addOrReplaceChild("rotor_blur", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-LittleBirdGeometry.ROTOR_RADIUS_UNITS, -0.5F, -3.0F,
                        LittleBirdGeometry.ROTOR_RADIUS_UNITS * 2.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, LittleBirdGeometry.ROTOR_HUB_UNITS, -6.0F));
        for (int bar = 1; bar < 3; bar++) {
            blur.addOrReplaceChild("bar_" + bar, CubeListBuilder.create()
                    .texOffs(0, 208).addBox(-LittleBirdGeometry.ROTOR_RADIUS_UNITS, -0.5F, -3.0F,
                            LittleBirdGeometry.ROTOR_RADIUS_UNITS * 2.0F, 1.0F, 6.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F,
                            (float) (Math.PI / 3.0D) * bar, 0.0F));
        }
    }

    @Override
    public void setupAnim(LittleBirdEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float rpm = entity.getRenderRotorRpm();
        float spin = entity.getRenderRotorSpinFactor();
        mainRotor.yRot = LittleBirdModelMath.rotorAngleRadians(rpm, spin, ageInTicks);
        tailRotor.xRot = LittleBirdModelMath.tailRotorAngleRadians(rpm, ageInTicks);

        float frameDelta = Math.max(0.0F, Math.min(2.0F, ageInTicks - lastClusterAge));
        lastClusterAge = ageInTicks;
        clusterAngle = LittleBirdModelMath.wrap360(clusterAngle
                + LittleBirdModelMath.clusterSpinDegreesPerTick(entity.getGunState())
                        * frameDelta);
        float clusterRadians = (float) Math.toRadians(clusterAngle);
        leftCluster.zRot = clusterRadians;
        rightCluster.zRot = -clusterRadians * 0.9F;
        leftHeatTips.zRot = clusterRadians;
        rightHeatTips.zRot = -clusterRadians * 0.9F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Cockpit view hides the entire airframe: from the pilot's seat only the world ahead
     * matters and every part is close enough to the camera to be in the way. Hiding the
     * root covers the base pass, the emissive pass and every animation that re-shows a
     * part later in the frame. This only ever applies to the aircraft the local player is
     * flying: the cockpit flag is client-side and never synced.
     */
    public void setCockpitView(boolean cockpitView) {
        this.root.visible = !cockpitView;
        for (ModelPart part : cockpitHiddenParts) {
            part.visible = !cockpitView;
        }
    }

    /**
     * Emissive pass for the muzzle sleeves, faded in by gun heat. Hidden from the
     * base pass so a cold gun never glows.
     */
    public void renderGunHeat(PoseStack pose, VertexConsumer consumer, int packedLight,
                              int packedOverlay, int heat) {
        LittleBirdModelMath.heatGlowColor(heat, heatRgba);
        if (heatRgba[3] <= 0.0F) {
            return;
        }
        leftHeatTips.visible = true;
        rightHeatTips.visible = true;
        leftHeatTips.render(pose, consumer, packedLight, packedOverlay,
                heatRgba[0], heatRgba[1], heatRgba[2], heatRgba[3]);
        rightHeatTips.render(pose, consumer, packedLight, packedOverlay,
                heatRgba[0], heatRgba[1], heatRgba[2], heatRgba[3]);
        leftHeatTips.visible = false;
        rightHeatTips.visible = false;
    }

    /** Hides the rocket tips of tubes that have already fired. */
    public void setRocketState(int leftRounds, int rightRounds) {
        for (int tube = 0; tube < LittleBirdPodTubes.TUBES; tube++) {
            podTubesLeft[tube].visible = LittleBirdPodTubes.loaded(tube, leftRounds);
            podTubesRight[tube].visible = LittleBirdPodTubes.loaded(tube, rightRounds);
        }
    }

    /** Second pass: windscreen and side glazing, tinted and see-through. */
    public void renderGlass(PoseStack pose, VertexConsumer consumer, int packedLight,
                            int packedOverlay, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        glass.visible = true;
        glass.render(pose, consumer, packedLight, packedOverlay,
                1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
        glass.visible = false;
    }

    /** Third pass: the translucent rotor disc, faded in above the blur RPM. */
    public void renderRotorBlur(PoseStack pose, VertexConsumer consumer, int packedLight,
                                int packedOverlay, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        rotorBlur.visible = true;
        rotorBlur.render(pose, consumer, packedLight, packedOverlay,
                1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
        rotorBlur.visible = false;
    }
}
