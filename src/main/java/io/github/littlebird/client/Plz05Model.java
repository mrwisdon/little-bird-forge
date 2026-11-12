package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.Plz05Entity;
import io.github.littlebird.entity.Plz05Geometry;
import io.github.littlebird.entity.ground.GroundSuspension;
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
 * PLZ-05 model: the six wheel tracked hull, the turret and the elevating 155mm gun.
 *
 * <p>Built from the same metre dimensions as {@link Plz05Geometry}: every box is declared by its
 * min/max in the Blender source frame (x nose, y left, z up), and the helpers convert that into
 * Minecraft's model frame (nose along -Z, y up from the ground). The turret is a child part of the
 * hull and the gun is a child of the turret whose pivot is the trunnion, so setting its xRot
 * elevates the barrel - the same pivot the muzzle geometry uses.
 */
public final class Plz05Model extends EntityModel<Plz05Entity> {
    public static final ModelLayerLocation PLZ05_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "plz05"), "main");

    private static final float UNITS = Plz05Geometry.UNITS_PER_BLOCK;
    /** Turret ring centre: x = -1.0 m, deck height. */
    private static final float RING_X = -1.0F;
    private static final float RING_Z = Plz05Geometry.DECK_UNITS / UNITS;
    /** Trunnion in model space: 0.86 m ahead of the hull centre, 2.42 m up. */
    private static final float TRUNNION_X = 0.86F;
    private static final float TRUNNION_Z = Plz05Geometry.TRUNNION_HEIGHT_UNITS / UNITS;

    /** Texture regions of the 1024 sheet (must match tools/generate_plz05_texture.py). */
    private enum Zone {
        CAMO(0, 0, 1024, 720),
        DARK(0, 720, 512, 144),
        GLASS(512, 720, 320, 120),
        LIGHT(832, 720, 120, 64);

        final int x;
        final int y;
        final int w;
        final int h;

        Zone(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private final ModelPart root;
    private final ModelPart gun;

    public Plz05Model(ModelPart root) {
        this.root = root;
        this.gun = root.getChild("body").getChild("turret").getChild("gun");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition turret = body.addOrReplaceChild("turret", CubeListBuilder.create(),
                PartPose.offset(0.0F, RING_Z * UNITS, -RING_X * UNITS));
        PartDefinition gun = turret.addOrReplaceChild("gun", CubeListBuilder.create(),
                PartPose.offset(0.0F, (TRUNNION_Z - RING_Z) * UNITS,
                        -(TRUNNION_X - RING_X) * UNITS));

        int seed = 1;

        // ---- running gear ----
        for (int side = -1; side <= 1; side += 2) {
            String tag = side > 0 ? "l" : "r";
            float inner = side * 1.02F;
            float outer = side * 1.60F;
            seed = add(body, "track_" + tag, Zone.DARK, seed, -3.32F, 3.32F,
                    Math.min(inner, outer), Math.max(inner, outer), 0.10F, 0.76F);
            seed = add(body, "skirt_" + tag, Zone.CAMO, seed, -3.34F, 3.34F,
                    side * 1.62F, side * 1.74F, 0.66F, 1.48F);
            for (int axle = 0; axle < Plz05Geometry.AXLE_FORWARD_UNITS.length; axle++) {
                float x = Plz05Geometry.AXLE_FORWARD_UNITS[axle] / UNITS;
                float radius = Plz05Geometry.WHEEL_RADIUS_UNITS / UNITS;
                seed = add(body, "wheel_" + axle + tag, Zone.DARK, seed,
                        x - radius, x + radius, Math.min(inner, outer) + 0.02F,
                        Math.max(inner, outer) - 0.02F, 0.10F, 0.10F + radius * 2.0F);
                seed = add(body, "wheel_hub_" + axle + tag, Zone.CAMO, seed,
                        x - radius * 0.40F, x + radius * 0.40F,
                        side > 0 ? 1.58F : -1.62F, side > 0 ? 1.66F : -1.58F,
                        0.34F, 0.62F);
            }
            seed = add(body, "sprocket_" + tag, Zone.DARK, seed, 2.86F, 3.46F,
                    Math.min(inner, outer), Math.max(inner, outer), 0.30F, 0.94F);
            seed = add(body, "idler_" + tag, Zone.DARK, seed, -3.46F, -2.86F,
                    Math.min(inner, outer), Math.max(inner, outer), 0.30F, 0.94F);
        }

        // ---- hull ----
        seed = add(body, "hull_lower", Zone.CAMO, seed, -3.62F, 3.62F, -1.02F, 1.02F,
                0.44F, 1.14F);
        seed = add(body, "hull_upper", Zone.CAMO, seed, -3.65F, 3.65F, -1.69F, 1.69F,
                1.04F, 1.95F);
        seed = add(body, "glacis", Zone.CAMO, seed, 3.30F, 3.72F, -1.60F, 1.60F, 1.10F,
                1.92F);
        seed = add(body, "front_plate", Zone.CAMO, seed, 3.58F, 3.76F, -1.60F, 1.60F, 0.46F,
                1.16F);
        seed = add(body, "deck", Zone.CAMO, seed, -3.60F, 2.30F, -1.66F, 1.66F, 1.90F,
                1.97F);
        seed = add(body, "engine_grille", Zone.DARK, seed, -3.55F, -2.30F, -1.40F, 1.40F,
                1.96F, 2.00F);
        seed = add(body, "driver_hatch", Zone.CAMO, seed, 1.86F, 2.72F, -1.10F, -0.42F,
                1.96F, 2.03F);
        seed = add(body, "drv_periscope", Zone.GLASS, seed, 2.74F, 2.92F, -1.02F, -0.50F,
                1.98F, 2.08F);
        seed = add(body, "bumper_l", Zone.CAMO, seed, 3.66F, 3.84F, 0.82F, 1.62F, 0.72F,
                1.20F);
        seed = add(body, "bumper_r", Zone.CAMO, seed, 3.66F, 3.84F, -1.62F, -0.82F, 0.72F,
                1.20F);
        seed = add(body, "headlamp_l", Zone.LIGHT, seed, 3.74F, 3.86F, 1.06F, 1.30F, 1.02F,
                1.22F);
        seed = add(body, "headlamp_r", Zone.LIGHT, seed, 3.74F, 3.86F, -1.30F, -1.06F, 1.02F,
                1.22F);
        seed = add(body, "towhook_l", Zone.DARK, seed, 3.70F, 3.86F, 0.66F, 0.88F, 0.44F,
                0.62F);
        seed = add(body, "towhook_r", Zone.DARK, seed, 3.70F, 3.86F, -0.88F, -0.66F, 0.44F,
                0.62F);
        seed = add(body, "toolbox_l", Zone.CAMO, seed, -0.20F, 1.30F, 1.70F, 1.80F, 1.24F,
                1.56F);
        seed = add(body, "toolbox_r", Zone.CAMO, seed, -0.20F, 1.30F, -1.80F, -1.70F, 1.24F,
                1.56F);
        seed = add(body, "exhaust", Zone.DARK, seed, -2.20F, -1.60F, 1.62F, 1.74F, 1.60F,
                1.78F);

        // ---- turret: boxes are declared in the same world frame as the hull and the
        // helper subtracts the ring pivot, so the turret sits on the deck wherever it is drawn.
        seed = addTurret(turret, "turret_body", Zone.CAMO, seed, -3.55F, 1.10F, -1.51F, 1.51F,
                2.00F, 3.00F);
        seed = addTurret(turret, "turret_nose", Zone.CAMO, seed, 1.10F, 1.62F, -1.30F, 1.30F,
                2.10F, 2.90F);
        seed = addTurret(turret, "cheek_l", Zone.CAMO, seed, 0.55F, 1.34F, 1.18F, 1.52F,
                2.10F, 2.85F);
        seed = addTurret(turret, "cheek_r", Zone.CAMO, seed, 0.55F, 1.34F, -1.52F, -1.18F,
                2.10F, 2.85F);
        seed = addTurret(turret, "bustle", Zone.CAMO, seed, -4.00F, -3.45F, -1.22F, 1.22F,
                2.06F, 2.72F);
        seed = addTurret(turret, "roof", Zone.CAMO, seed, -3.45F, 1.10F, -1.46F, 1.46F,
                3.00F, 3.06F);
        seed = addTurret(turret, "cupola", Zone.CAMO, seed, -0.20F, 0.50F, 0.20F, 0.90F,
                3.05F, 3.23F);
        seed = addTurret(turret, "cupola_ring", Zone.DARK, seed, -0.25F, 0.55F, 0.15F, 0.95F,
                3.00F, 3.06F);
        seed = addTurret(turret, "loader_hatch", Zone.CAMO, seed, -1.30F, -0.70F, -1.05F,
                -0.45F, 3.06F, 3.12F);
        seed = addTurret(turret, "sight_house", Zone.CAMO, seed, 0.55F, 1.15F, -0.95F, -0.35F,
                3.00F, 3.24F);
        seed = addTurret(turret, "sight_glass", Zone.GLASS, seed, 1.05F, 1.17F, -0.88F, -0.42F,
                3.06F, 3.18F);
        seed = addTurret(turret, "smoke_l", Zone.DARK, seed, 1.15F, 1.50F, 0.70F, 1.44F,
                2.62F, 2.82F);
        seed = addTurret(turret, "smoke_r", Zone.DARK, seed, 1.15F, 1.50F, -1.44F, -0.70F,
                2.62F, 2.82F);
        seed = addTurret(turret, "rail_l", Zone.DARK, seed, -3.30F, 0.60F, 1.42F, 1.48F,
                3.06F, 3.10F);
        seed = addTurret(turret, "rail_r", Zone.DARK, seed, -3.30F, 0.60F, -1.48F, -1.42F,
                3.06F, 3.10F);
        seed = addTurret(turret, "antenna_l", Zone.DARK, seed, -3.20F, -3.14F, 1.10F, 1.16F,
                3.06F, 5.05F);
        seed = addTurret(turret, "antenna_r", Zone.DARK, seed, -3.20F, -3.14F, -1.16F, -1.10F,
                3.06F, 5.05F);
        seed = addTurret(turret, "stow_box_l", Zone.CAMO, seed, -3.10F, -1.80F, 1.52F, 1.66F,
                2.25F, 2.70F);
        seed = addTurret(turret, "stow_box_r", Zone.CAMO, seed, -3.10F, -1.80F, -1.66F, -1.52F,
                2.25F, 2.70F);
        seed = addTurret(turret, "basket_floor", Zone.DARK, seed, -4.25F, -3.45F, -1.15F, 1.15F,
                2.02F, 2.06F);

        // ---- gun: world frame too; the trunnion pivot is subtracted by the helper ----
        seed = addGun(gun, "barrel", Zone.CAMO, seed, 1.00F, 7.55F, -0.14F, 0.14F, 2.28F, 2.56F);
        seed = addGun(gun, "barrel_step", Zone.CAMO, seed, 3.60F, 3.74F, -0.16F, 0.16F,
                2.26F, 2.58F);
        seed = addGun(gun, "fume_extractor", Zone.CAMO, seed, 4.80F, 5.70F, -0.21F, 0.21F,
                2.21F, 2.63F);
        seed = addGun(gun, "muzzle_brake", Zone.DARK, seed, 7.42F, 7.90F, -0.21F, 0.21F,
                2.21F, 2.63F);
        seed = addGun(gun, "muzzle_face", Zone.DARK, seed, 7.90F, 7.95F, -0.10F, 0.10F,
                2.32F, 2.52F);
        seed = addGun(gun, "mantlet", Zone.CAMO, seed, 0.55F, 1.10F, -0.56F, 0.56F, 1.96F,
                2.88F);
        return LayerDefinition.create(mesh, 1024, 1024);
    }

    private static int add(PartDefinition body, String name, Zone zone, int seed,
                           float x0, float x1, float y0, float y1, float z0, float z1) {
        return addBox(body, name, zone, seed, x0, x1, y0, y1, z0, z1, 0.0F, 0.0F);
    }

    /** Turret boxes: world x is shifted by the ring centre (-1.0 m). */
    private static int addTurret(PartDefinition turret, String name, Zone zone, int seed,
                                 float x0, float x1, float y0, float y1, float z0, float z1) {
        return addBox(turret, name, zone, seed, x0, x1, y0, y1, z0, z1, RING_X, RING_Z);
    }

    /** Gun boxes: world x is shifted by the trunnion (0.86 m) and z by its height. */
    private static int addGun(PartDefinition gun, String name, Zone zone, int seed,
                              float x0, float x1, float y0, float y1, float z0, float z1) {
        return addBox(gun, name, zone, seed, x0, x1, y0, y1, z0, z1, TRUNNION_X, TRUNNION_Z);
    }

    private static int addBox(PartDefinition parent, String name, Zone zone, int seed,
                              float x0, float x1, float y0, float y1, float z0, float z1,
                              float pivotX, float pivotZ) {
        float w = (y1 - y0) * UNITS;
        float h = (z1 - z0) * UNITS;
        float d = (x1 - x0) * UNITS;
        int[] uv = uvFor(zone, seed, Math.round(2.0F * (w + d)), Math.round(d + h));
        float ox = -y1 * UNITS;
        float oy = (z0 - pivotZ) * UNITS;
        float oz = -(x1 - pivotX) * UNITS;
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(uv[0], uv[1])
                .addBox(ox, oy, oz, w, h, d), PartPose.offset(0.0F, 0.0F, 0.0F));
        return seed * 31 + name.hashCode() % 97 + 7;
    }

    /** A texture offset inside the requested zone that fits the box's UV footprint. */
    private static int[] uvFor(Zone zone, int seed, int uvWidth, int uvHeight) {
        int width = Math.min(uvWidth, zone.w - 8);
        int height = Math.min(uvHeight, zone.h - 8);
        int u = zone.x + 4 + Math.floorMod(seed * 37, Math.max(1, zone.w - width - 4));
        int v = zone.y + 4 + Math.floorMod(seed * 53, Math.max(1, zone.h - height - 4));
        return new int[] {u, v};
    }

    public void setGunPose(float elevationDegrees, GroundSuspension.Pose chassis) {
        // Undo the chassis tilt at the trunnion before applying the world elevation.
        gun.zRot = (float) Math.toRadians(chassis.rollDegrees());
        gun.xRot = (float) Math.toRadians(elevationDegrees - chassis.pitchDegrees());
    }

    @Override
    public void setupAnim(Plz05Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
