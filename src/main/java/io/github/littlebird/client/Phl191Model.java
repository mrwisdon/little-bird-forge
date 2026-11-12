package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.Phl191Entity;
import io.github.littlebird.entity.Phl191Geometry;
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
 * PHL-191 box model: the 8x8 hull, the cab, eight wheels and the elevating launcher.
 *
 * <p>Built from the same metre dimensions as {@link Phl191Geometry}: every box is declared by its
 * min/max in the Blender source frame (x nose, y left, z up), and the helpers below convert that
 * into Minecraft's model frame (nose along -Z, y up from the ground). The launcher is a child part
 * whose pivot is the trunnion, so setting its xRot elevates the pods and the framework together -
 * the same pivot the muzzle geometry uses.
 */
public final class Phl191Model extends EntityModel<Phl191Entity> {
    public static final ModelLayerLocation PHL191_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "phl191"), "main");

    private static final float UNITS = Phl191Geometry.UNITS_PER_BLOCK;
    private static final float PIVOT_X = Phl191Geometry.TRUNNION_FORWARD_UNITS / UNITS;
    private static final float PIVOT_Z = Phl191Geometry.TRUNNION_HEIGHT_UNITS / UNITS;

    /** Texture regions of the 1024 sheet (must match tools/generate_phl191_texture.py). */
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
    private final ModelPart launcher;
    private final ModelPart[][] wheels = new ModelPart[Phl191Geometry.AXLE_FORWARD_UNITS.length][2];
    private final ModelPart[][] hubs = new ModelPart[Phl191Geometry.AXLE_FORWARD_UNITS.length][2];

    public Phl191Model(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.launcher = body.getChild("launcher");
        for (int axle = 0; axle < wheels.length; axle++) {
            for (int side = 0; side < 2; side++) {
                String name = "wheel_" + axle + (side == 0 ? "l" : "r");
                wheels[axle][side] = body.getChild(name);
                hubs[axle][side] = body.getChild(name + "_hub");
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition launcher = body.addOrReplaceChild("launcher", CubeListBuilder.create(),
                PartPose.offset(0.0F, PIVOT_Z * UNITS, -PIVOT_X * UNITS));

        int seed = 1;

        // ---- chassis and deck ----
        seed = add(body, "frame", Zone.CAMO, seed, -5.20F, 3.00F, -1.30F, 1.30F, 0.55F, 1.10F);
        seed = add(body, "frame_side_l", Zone.DARK, seed, -5.10F, 2.90F, 1.16F, 1.30F, 0.62F, 1.04F);
        seed = add(body, "frame_side_r", Zone.DARK, seed, -5.10F, 2.90F, -1.30F, -1.16F, 0.62F, 1.04F);
        seed = add(body, "deck", Zone.CAMO, seed, -5.00F, 2.60F, -1.04F, 1.04F, 1.32F, 1.40F);
        seed = add(body, "fuel_tank", Zone.CAMO, seed, -1.00F, 0.60F, -1.36F, -0.94F, 0.86F, 1.34F);
        seed = add(body, "tool_box", Zone.CAMO, seed, 1.90F, 2.60F, 1.04F, 1.46F, 0.92F, 1.34F);
        seed = add(body, "step", Zone.DARK, seed, 0.00F, 0.40F, -1.62F, -1.54F, 0.45F, 1.40F);

        // ---- cab ----
        seed = add(body, "cab", Zone.CAMO, seed, 2.56F, 5.46F, -1.49F, 1.49F, 1.04F, 3.06F);
        seed = add(body, "cab_roof", Zone.CAMO, seed, 2.62F, 5.40F, -1.40F, 1.40F, 3.02F, 3.16F);
        seed = add(body, "cab_roof_rack", Zone.DARK, seed, 2.70F, 5.30F, -1.36F, 1.36F, 3.16F, 3.22F);
        seed = add(body, "windscreen", Zone.GLASS, seed, 5.40F, 5.48F, -1.24F, 1.24F, 2.08F, 2.92F);
        seed = add(body, "windscreen_frame", Zone.DARK, seed, 5.38F, 5.42F, -1.30F, 1.30F, 2.00F, 3.00F);
        seed = add(body, "grille", Zone.DARK, seed, 5.46F, 5.56F, -1.02F, 1.02F, 1.66F, 2.10F);
        seed = add(body, "bumper", Zone.CAMO, seed, 5.42F, 5.68F, -1.52F, 1.52F, 0.86F, 1.26F);
        seed = add(body, "headlamp_l", Zone.LIGHT, seed, 5.60F, 5.70F, 0.68F, 0.88F, 1.22F, 1.42F);
        seed = add(body, "headlamp_r", Zone.LIGHT, seed, 5.60F, 5.70F, -0.88F, -0.68F, 1.22F, 1.42F);
        seed = add(body, "door_window_l", Zone.GLASS, seed, 3.30F, 4.35F, 1.47F, 1.55F, 2.26F, 2.84F);
        seed = add(body, "door_window_r", Zone.GLASS, seed, 3.30F, 4.35F, -1.55F, -1.47F, 2.26F, 2.84F);
        seed = add(body, "mirror_l", Zone.DARK, seed, 4.92F, 5.02F, 1.56F, 1.74F, 2.38F, 2.82F);
        seed = add(body, "mirror_r", Zone.DARK, seed, 4.92F, 5.02F, -1.74F, -1.56F, 2.38F, 2.82F);
        seed = add(body, "equipment_box", Zone.CAMO, seed, 1.82F, 2.54F, -1.10F, 1.10F, 1.40F, 2.28F);
        seed = add(body, "exhaust", Zone.DARK, seed, 2.28F, 2.40F, -1.38F, -1.26F, 1.08F, 1.86F);
        seed = add(body, "railing_l", Zone.DARK, seed, -4.90F, -3.55F, 1.38F, 1.46F, 1.80F, 1.88F);
        seed = add(body, "railing_r", Zone.DARK, seed, -4.90F, -3.55F, -1.46F, -1.38F, 1.80F, 1.88F);

        // ---- eight wheels ----
        for (int axle = 0; axle < Phl191Geometry.AXLE_FORWARD_UNITS.length; axle++) {
            float x = Phl191Geometry.AXLE_FORWARD_UNITS[axle] / UNITS;
            float radius = Phl191Geometry.WHEEL_RADIUS_UNITS / UNITS;
            float half = Phl191Geometry.WHEEL_WIDTH_UNITS / UNITS * 0.5F;
            for (int side = -1; side <= 1; side += 2) {
                float inner = side * (Phl191Geometry.TRACK_HALF_WIDTH_UNITS / UNITS - half);
                float outer = side * (Phl191Geometry.TRACK_HALF_WIDTH_UNITS / UNITS + half);
                String tag = "wheel_" + axle + (side > 0 ? "l" : "r");
                seed = add(body, tag, Zone.DARK, seed, x - radius, x + radius,
                        Math.min(inner, outer), Math.max(inner, outer), 0.0F, radius * 2.0F);
                seed = add(body, tag + "_hub", Zone.CAMO, seed,
                        x - radius * 0.42F, x + radius * 0.42F,
                        side > 0 ? outer - 0.02F : outer - 0.08F,
                        side > 0 ? outer + 0.08F : outer + 0.02F,
                        radius * 0.58F, radius * 1.42F);
            }
        }

        // ---- launcher: frame, two pods, eight tubes ----
        seed = addLauncher(launcher, "frame_l", Zone.CAMO, seed,
                -5.02F, 2.06F, 1.26F, 1.42F, 1.62F, 1.94F);
        seed = addLauncher(launcher, "frame_r", Zone.CAMO, seed,
                -5.02F, 2.06F, -1.42F, -1.26F, 1.62F, 1.94F);
        seed = addLauncher(launcher, "frame_center", Zone.DARK, seed,
                -4.90F, 1.90F, -0.10F, 0.10F, 1.66F, 1.90F);
        for (int pod = 0; pod < 2; pod++) {
            float centre = (pod == 0 ? 1.0F : -1.0F)
                    * (Phl191Geometry.POD_GAP_UNITS / UNITS * 0.5F
                       + Phl191Geometry.POD_WIDTH_UNITS / UNITS * 0.5F);
            float halfWidth = Phl191Geometry.POD_WIDTH_UNITS / UNITS * 0.5F;
            float floor = Phl191Geometry.POD_FLOOR_UNITS / UNITS;
            float top = floor + Phl191Geometry.POD_HEIGHT_UNITS / UNITS;
            float front = Phl191Geometry.POD_FRONT_UNITS / UNITS;
            float rear = front - Phl191Geometry.POD_LENGTH_UNITS / UNITS;
            seed = addLauncher(launcher, "pod_" + pod, Zone.CAMO, seed, rear, front,
                    centre - halfWidth, centre + halfWidth, floor, top);
            // top rail and a side rib band so the box reads as a launcher, not a crate
            seed = addLauncher(launcher, "pod_rail_" + pod, Zone.CAMO, seed, rear, front,
                    centre - halfWidth * 0.45F, centre + halfWidth * 0.45F, top, top + 0.07F);
            seed = addLauncher(launcher, "pod_rear_" + pod, Zone.DARK, seed, rear - 0.08F, rear,
                    centre - halfWidth, centre + halfWidth, floor, top);
            for (int row = 0; row < 2; row++) {
                for (int column = 0; column < 2; column++) {
                    float ty = centre + (column == 0 ? -1.0F : 1.0F)
                            * Phl191Geometry.TUBE_PITCH_UNITS / UNITS * 0.5F;
                    float tz = floor + Phl191Geometry.POD_HEIGHT_UNITS / UNITS * 0.5F
                            + (row == 0 ? -1.0F : 1.0F)
                            * Phl191Geometry.TUBE_PITCH_UNITS / UNITS * 0.5F;
                    String tag = "tube_" + pod + "_" + row + column;
                    seed = addLauncher(launcher, tag + "_bore", Zone.DARK, seed,
                            front + 0.02F, front + 0.14F, ty - 0.21F, ty + 0.21F,
                            tz - 0.21F, tz + 0.21F);
                    seed = addLauncher(launcher, tag + "_ring", Zone.CAMO, seed,
                            front - 0.02F, front + 0.03F, ty - 0.25F, ty + 0.25F,
                            tz - 0.25F, tz + 0.25F);
                }
            }
        }
        return LayerDefinition.create(mesh, 1024, 1024);
    }

    /** Adds one box declared in the Blender frame (x nose, y left, z up). */
    private static int add(PartDefinition body, String name, Zone zone, int seed,
                           float x0, float x1, float y0, float y1, float z0, float z1) {
        return addBox(body, name, zone, seed, x0, x1, y0, y1, z0, z1, 0.0F, 0.0F);
    }

    /** Same, for a box under the launcher pivot: the pivot is subtracted off first. */
    private static int addLauncher(PartDefinition launcher, String name, Zone zone, int seed,
                                   float x0, float x1, float y0, float y1, float z0, float z1) {
        return addBox(launcher, name, zone, seed, x0, x1, y0, y1, z0, z1, PIVOT_X, PIVOT_Z);
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

    public void setLauncherPose(float elevationDegrees, GroundSuspension.Pose chassis) {
        // Undo the chassis tilt at the trunnion before applying the world elevation.
        launcher.zRot = (float) Math.toRadians(chassis.rollDegrees());
        launcher.xRot = (float) Math.toRadians(elevationDegrees - chassis.pitchDegrees());
    }

    public void setWheelOffsets(GroundSuspension suspension, float partialTick) {
        for (int axle = 0; axle < wheels.length; axle++) {
            for (int side = 0; side < 2; side++) {
                float offset = suspension.wheelOffsetUnits(axle, side == 0, partialTick);
                wheels[axle][side].y = offset;
                hubs[axle][side].y = offset;
            }
        }
    }

    @Override
    public void setupAnim(Phl191Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
