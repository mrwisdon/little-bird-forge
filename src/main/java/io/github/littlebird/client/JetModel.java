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
 * F-35E Panther built in the pack's Minecraft box style - hand-authored cubes on the 16-units-per
 * -block grid, nose along -Z, +X right, matching {@link io.github.littlebird.entity.JetGeometry}.
 *
 * <p>Shape notes come from the Battlefield 2042 F-35E (Factory) render: a long chined nose, a dark
 * canopy, DSI intakes on the shoulders, a big trapezoidal wing, tailerons and two outward canted
 * fins. Everything is boxes, so it shades and reads like the pack's other vehicles. UVs sample the
 * six zones painted by tools/generate_jet_texture.py.
 */
public final class JetModel extends EntityModel<JetEntity> {
    public static final ModelLayerLocation JET_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "f35"), "main");

    private static final int TEXTURE_SIZE = 512;
    private static final int BODY_U = 0, BODY_V = 0;
    private static final int DECK_U = 256, DECK_V = 0;
    private static final int GLASS_U = 256, GLASS_V = 128;
    private static final int ENG_U = 384, ENG_V = 128;
    private static final int ACC_U = 384, ACC_V = 192;
    private static final PartPose STILL = PartPose.offset(0.0F, 0.0F, 0.0F);

    private final ModelPart root;
    private final ModelPart nozzle;

    public JetModel(ModelPart root) {
        this.root = root;
        this.nozzle = root.getChild("body").getChild("nozzle");
    }

    /** Tilts the engine nozzle down while the F-35E is in its vector-thrust hover. */
    public void setVtol(boolean hover) {
        this.nozzle.xRot = hover ? (float) Math.toRadians(-82.0D) : 0.0F;
    }

    private static CubeListBuilder cube(int u, int v, float x, float y, float z,
                                        float w, float h, float d) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), STILL);

        // ---- fuselage: long chined nose tapering back to a wide tail boom ----
        body.addOrReplaceChild("nose_tip", cube(BODY_U, BODY_V, -3.0F, 10.0F, -86.0F, 6.0F, 8.0F, 12.0F), STILL);
        body.addOrReplaceChild("nose", cube(BODY_U, BODY_V, -6.0F, 9.0F, -76.0F, 12.0F, 11.0F, 18.0F), STILL);
        body.addOrReplaceChild("forebody", cube(BODY_U, BODY_V, -9.0F, 8.0F, -58.0F, 18.0F, 14.0F, 20.0F), STILL);
        body.addOrReplaceChild("mid", cube(BODY_U, BODY_V, -13.0F, 7.0F, -38.0F, 26.0F, 18.0F, 32.0F), STILL);
        body.addOrReplaceChild("midbody", cube(BODY_U, BODY_V, -14.0F, 6.0F, -6.0F, 28.0F, 19.0F, 28.0F), STILL);
        body.addOrReplaceChild("aftbody", cube(BODY_U, BODY_V, -14.0F, 6.0F, 22.0F, 28.0F, 19.0F, 30.0F), STILL);
        body.addOrReplaceChild("tail_boom", cube(BODY_U, BODY_V, -12.0F, 8.0F, 52.0F, 24.0F, 16.0F, 26.0F), STILL);
        body.addOrReplaceChild("nozzle_housing", cube(ENG_U, ENG_V, -10.0F, 10.0F, 78.0F, 20.0F, 12.0F, 8.0F), STILL);

        // ---- the signature chine edge down both sides of the forebody ----
        body.addOrReplaceChild("chine_right", cube(BODY_U, BODY_V, 9.0F, 15.0F, -50.0F, 10.0F, 2.0F, 36.0F), STILL);
        body.addOrReplaceChild("chine_left", cube(BODY_U, BODY_V, -19.0F, 15.0F, -50.0F, 10.0F, 2.0F, 36.0F), STILL);

        // ---- cockpit ----
        body.addOrReplaceChild("canopy_frame", cube(GLASS_U, GLASS_V, -8.0F, 25.0F, -60.0F, 16.0F, 2.0F, 32.0F), STILL);
        body.addOrReplaceChild("canopy", cube(GLASS_U, GLASS_V, -7.0F, 27.0F, -58.0F, 14.0F, 6.0F, 26.0F), STILL);
        body.addOrReplaceChild("canopy_nose", cube(GLASS_U, GLASS_V, -4.0F, 25.0F, -66.0F, 8.0F, 5.0F, 8.0F), STILL);
        body.addOrReplaceChild("spine", cube(DECK_U, DECK_V, -6.0F, 25.0F, -6.0F, 12.0F, 3.0F, 40.0F), STILL);

        // ---- DSI intakes on the shoulders ----
        body.addOrReplaceChild("intake_right", cube(BODY_U, BODY_V, 13.0F, 8.0F, -30.0F, 11.0F, 14.0F, 32.0F), STILL);
        body.addOrReplaceChild("intake_left", cube(BODY_U, BODY_V, -24.0F, 8.0F, -30.0F, 11.0F, 14.0F, 32.0F), STILL);
        body.addOrReplaceChild("dsi_right", cube(BODY_U, BODY_V, 12.0F, 20.0F, -26.0F, 6.0F, 5.0F, 20.0F), STILL);
        body.addOrReplaceChild("dsi_left", cube(BODY_U, BODY_V, -18.0F, 20.0F, -26.0F, 6.0F, 5.0F, 20.0F), STILL);

        addWing(body, true);
        addWing(body, false);
        addStab(body, true);
        addStab(body, false);

        // ---- outward canted fins (rotated parts, like the real aircraft) ----
        float cant = (float) Math.toRadians(15.0D);
        body.addOrReplaceChild("fin_right", cube(BODY_U, BODY_V, -2.0F, 0.0F, -14.0F, 4.0F, 24.0F, 26.0F),
                PartPose.offsetAndRotation(13.0F, 24.0F, 50.0F, 0.0F, 0.0F, -cant));
        body.addOrReplaceChild("fin_left", cube(BODY_U, BODY_V, -2.0F, 0.0F, -14.0F, 4.0F, 24.0F, 26.0F),
                PartPose.offsetAndRotation(-13.0F, 24.0F, 50.0F, 0.0F, 0.0F, cant));
        body.addOrReplaceChild("fin_light_right", cube(ACC_U, ACC_V, 1.0F, 18.0F, -4.0F, 1.0F, 2.0F, 8.0F),
                PartPose.offsetAndRotation(13.0F, 24.0F, 50.0F, 0.0F, 0.0F, -cant));
        body.addOrReplaceChild("fin_light_left", cube(ACC_U, ACC_V, -2.0F, 18.0F, -4.0F, 1.0F, 2.0F, 8.0F),
                PartPose.offsetAndRotation(-13.0F, 24.0F, 50.0F, 0.0F, 0.0F, cant));

        // ---- vector-thrust nozzle: its own pivot so setVtol can swing it ----
        PartDefinition nozzle = body.addOrReplaceChild("nozzle",
                cube(ENG_U, ENG_V, -7.0F, -6.0F, -6.0F, 14.0F, 12.0F, 12.0F),
                PartPose.offset(0.0F, 14.0F, 84.0F));
        nozzle.addOrReplaceChild("petal_up", cube(ENG_U, ENG_V, -6.0F, 6.0F, -6.0F, 12.0F, 2.0F, 12.0F), STILL);
        nozzle.addOrReplaceChild("petal_down", cube(ENG_U, ENG_V, -6.0F, -8.0F, -6.0F, 12.0F, 2.0F, 12.0F), STILL);

        // ---- details: pitot, EOTS, weapon-bay doors, dorsal antenna ----
        body.addOrReplaceChild("nose_probe", cube(ACC_U, ACC_V, -0.5F, 14.5F, -92.0F, 1.0F, 1.0F, 6.0F), STILL);
        body.addOrReplaceChild("eots", cube(ACC_U, ACC_V, -3.0F, 5.0F, -74.0F, 6.0F, 4.0F, 10.0F), STILL);
        body.addOrReplaceChild("bay_door_right", cube(ACC_U, ACC_V, 1.0F, 5.0F, -22.0F, 9.0F, 1.0F, 24.0F), STILL);
        body.addOrReplaceChild("bay_door_left", cube(ACC_U, ACC_V, -10.0F, 5.0F, -22.0F, 9.0F, 1.0F, 24.0F), STILL);
        body.addOrReplaceChild("dorsal_antenna", cube(DECK_U, DECK_V, -0.5F, 28.0F, 6.0F, 1.0F, 4.0F, 3.0F), STILL);

        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /** Mirrored slab: a positive-size cube whose min x flips to the other side of the centreline. */
    private static void slab(PartDefinition body, String name, boolean right,
                             float x, float y, float z, float w, float h, float d) {
        float minX = right ? x : -(x + w);
        body.addOrReplaceChild(name, cube(BODY_U, BODY_V, minX, y, z, w, h, d), STILL);
    }

    private static void addWing(PartDefinition body, boolean right) {
        String side = right ? "right" : "left";
        slab(body, "wing_" + side + "_1", right, 14.0F, 14.0F, -14.0F, 16.0F, 3.0F, 32.0F);
        slab(body, "wing_" + side + "_2", right, 30.0F, 14.0F, 2.0F, 14.0F, 3.0F, 20.0F);
        slab(body, "wing_" + side + "_3", right, 44.0F, 14.0F, 14.0F, 10.0F, 2.0F, 12.0F);
        slab(body, "wing_" + side + "_4", right, 54.0F, 14.0F, 20.0F, 6.0F, 2.0F, 8.0F);
    }

    private static void addStab(PartDefinition body, boolean right) {
        String side = right ? "right" : "left";
        slab(body, "stab_" + side + "_1", right, 12.0F, 14.0F, 48.0F, 18.0F, 3.0F, 22.0F);
        slab(body, "stab_" + side + "_2", right, 30.0F, 14.0F, 58.0F, 14.0F, 2.0F, 16.0F);
    }

    @Override
    public void setupAnim(JetEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // A fixed-wing airframe has no articulated parts: the whole body banks with the entity.
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
