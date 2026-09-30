#!/usr/bin/env python3
"""Generate B2Model.java (B-2 Spirit) from the Blender box export.

    python3 tools/generate_b2_model.py

Reads tools/data/b2_mc.json (already in Minecraft model space: +X right / +Y up /
-Z nose, 16 units per block) and writes client/B2Model.java.

The exported boxes sit on the exporter's half-metre cell grid (8 units), so a single
wing slab can be 480 units wide. Minecraft unwraps a box into a
2*(width+depth) x (depth+height) pixel patch, which for such a slab runs far outside its
zone on the 512x512 sheet and bleeds into the neighbouring material. Every box that does
not fit its zone is therefore split on the same 8-unit grid until it does: the split is
exact (volume and outer bounds are preserved to the last unit), only the cube count
grows. The zone table below is frozen together with mc-tex (task-8, 512x512 b2.png);
the u_span / v_span limits are the zone's width and height.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC = os.path.join(ROOT, "tools", "data", "b2_mc.json")
OUT = os.path.join(ROOT, "src", "main", "java", "io", "github", "littlebird",
                   "client", "B2Model.java")
TEXTURE_SIZE = 512
# Frozen UV zones of textures/entity/b2.png: name -> (u0, v0, u_span_limit, v_span_limit).
UV_ZONES = {
    "skin": (0, 0, 256, 144),
    "panel": (256, 0, 128, 96),
    "glass": (384, 0, 64, 48),
    "intake": (448, 0, 64, 48),
    "exhaust": (0, 144, 128, 64),
    "metal": (128, 144, 128, 64),
    "bay_white": (256, 144, 96, 48),
    "gear_white": (352, 144, 64, 32),
    "ordnance": (416, 144, 96, 48),
    "tire": (0, 208, 48, 32),
    "light_r": (48, 208, 32, 16),
    "light_g": (80, 208, 32, 16),
    "light_w": (112, 208, 32, 16),
    "gold": (144, 208, 32, 16),
}
CELL_UNITS = 8.0


def jf(x):
    s = ("%.3f" % float(x)).rstrip("0")
    if s.endswith("."):
        s += "0"
    return ("0.0" if s in ("-0", "-0.0") else s) + "F"


def jname(n):
    s = re.sub(r"^B2_", "", n)
    s = re.sub(r"[^0-9a-zA-Z]+", "_", s).strip("_").lower()
    return s or "part"


def uv_span(mn, mx):
    """Texture patch a Minecraft box unwraps to: 2*(width+depth) x (depth+height)."""
    w = mx[0] - mn[0]
    h = mx[1] - mn[1]
    d = mx[2] - mn[2]
    return 2.0 * (w + d), d + h


def fits(mn, mx, zone):
    u, v = uv_span(mn, mx)
    return u <= zone[2] + 1e-6 and v <= zone[3] + 1e-6


def split_box(mn, mx, zone):
    """Split on the 8-unit cell grid until the patch fits; geometry neutral."""
    out = []

    def rec(a, b):
        if fits(a, b, zone):
            out.append((list(a), list(b)))
            return
        extents = [b[i] - a[i] for i in range(3)]
        axis = max(range(3), key=lambda i: (extents[i], -i))
        cut = round((a[axis] + extents[axis] / 2.0) / CELL_UNITS) * CELL_UNITS
        if cut <= a[axis] or cut >= b[axis]:
            cut = (a[axis] + b[axis]) / 2.0
        left = list(b)
        left[axis] = cut
        right = list(a)
        right[axis] = cut
        rec(a, left)
        rec(right, b)

    rec(list(mn), list(mx))
    return out


def main():
    spec = json.load(open(SPEC))
    parts = spec["parts"]
    units_per_block = float(spec.get("units_per_block", 16))
    zone_order = list(UV_ZONES.keys())

    cubes = []
    split_stats = [0, 0]  # [boxes that needed splitting, cubes added by splitting]
    used_zones = {}
    for p in parts:
        zone = UV_ZONES[p["uv"]]
        base = jname(p["name"])
        pieces = split_box(p["min"], p["max"], zone)
        if len(pieces) > 1:
            split_stats[0] += 1
            split_stats[1] += len(pieces) - 1
        for i, (mn, mx) in enumerate(pieces):
            name = base if len(pieces) == 1 else "%s_%d" % (base, i)
            cubes.append((name, p["uv"], mn, mx))
            used_zones.setdefault(p["uv"], []).append((mn, mx))

    # ---- mechanical self-check: the split must be geometry neutral ----
    vol_in = sum((p["max"][0] - p["min"][0]) * (p["max"][1] - p["min"][1])
                 * (p["max"][2] - p["min"][2]) for p in parts)
    vol_out = sum((mx[0] - mn[0]) * (mx[1] - mn[1]) * (mx[2] - mn[2])
                  for _, _, mn, mx in cubes)
    in_bb = ([min(p["min"][i] for p in parts) for i in range(3)],
             [max(p["max"][i] for p in parts) for i in range(3)])
    out_bb = ([min(c[2][i] for c in cubes) for i in range(3)],
              [max(c[3][i] for c in cubes) for i in range(3)])
    lines = []
    lines.append("        // ---- flying wing, %d cubes from %d exported boxes ----"
                 % (len(cubes), len(parts)))
    used = set()
    for name, uv, mn, mx in cubes:
        while name in used:
            name += "_x"
        used.add(name)
        u0, v0 = UV_ZONES[uv][0], UV_ZONES[uv][1]
        lines.append('        body.addOrReplaceChild("%s", CubeListBuilder.create()' % name)
        lines.append("                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s),"
                     % (u0, v0, jf(mn[0]), jf(mn[1]), jf(mn[2]),
                        jf(mx[0] - mn[0]), jf(mx[1] - mn[1]), jf(mx[2] - mn[2])))
        lines.append("                PartPose.offset(0.0F, 0.0F, 0.0F));")
    body_code = "\n".join(lines)

    java = '''package io.github.littlebird.client;

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
 * B-2 Spirit, generated from the Blender box export: a flying wing, so there is no tail,
 * no canard and no turret to articulate - the whole airframe banks with the entity, the
 * same way {@link Su57Model} does.
 *
 * <p>GENERATED FILE - do not edit by hand. Rebuild with tools/generate_b2_model.py, which
 * reads tools/data/b2_mc.json (exported from the B-2 Blender scene). The export is already
 * in model space (+X right, +Y up, -Z nose, 16 units per block), scaled to the 15-block
 * B2Geometry envelope: nose %s, tail %s, half span %s units.
 *
 * <p>The exporter works on a half-metre cell grid, so wide wing slabs are split by the
 * generator until every box's Minecraft UV patch fits its material zone of the 512x512
 * sheet. The split changes no geometry: the %d cubes keep the exact volume and outer
 * bounds of the %d exported boxes.
 */
public final class B2Model extends EntityModel<JetEntity> {
    public static final ModelLayerLocation B2_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "b2"), "main");
    private static final int TEXTURE_SIZE = %d;

    private final ModelPart root;

    public B2Model(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
%s
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void setupAnim(JetEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // A flying wing has no articulated surfaces: the whole body banks with the entity.
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
''' % (jf(out_bb[0][2]), jf(out_bb[1][2]), jf(out_bb[1][0]), len(cubes), len(parts),
       TEXTURE_SIZE, body_code)
    open(OUT, "w", encoding="utf-8").write(java)

    # ---- printed gate ----
    print("b2_mc.json: %d boxes -> %d cubes after UV split (%d boxes split, +%d cubes)"
          % (len(parts), len(cubes), split_stats[0], split_stats[1]))
    print("volume  in=%d out=%d %s" % (vol_in, vol_out, "OK" if vol_in == vol_out else "MISMATCH"))
    print("bbox in  %s..%s" % (in_bb[0], in_bb[1]))
    print("bbox out %s..%s %s" % (out_bb[0], out_bb[1], "OK" if in_bb == out_bb else "MISMATCH"))
    print("size blocks: length %.3f  span %.3f  height %.3f"
          % ((out_bb[1][2] - out_bb[0][2]) / units_per_block,
             (out_bb[1][0] - out_bb[0][0]) / units_per_block,
             (out_bb[1][1] - out_bb[0][1]) / units_per_block))
    bad = 0
    for uv in zone_order:
        boxes = used_zones.get(uv)
        if not boxes:
            print("  zone %-11s unused by the mesh" % uv)
            continue
        umax = max(uv_span(mn, mx)[0] for mn, mx in boxes)
        vmax = max(uv_span(mn, mx)[1] for mn, mx in boxes)
        ok = umax <= UV_ZONES[uv][2] and vmax <= UV_ZONES[uv][3]
        bad += 0 if ok else 1
        print("  zone %-11s %3d cubes  u_span<=%3.0f/%3d  v_span<=%3.0f/%3d  %s"
              % (uv, len(boxes), umax, UV_ZONES[uv][2], vmax, UV_ZONES[uv][3],
                 "OK" if ok else "OVER"))
    if vol_in != vol_out or in_bb != out_bb or bad:
        print("GENERATOR SELF-CHECK FAILED")
        return 1
    print("GENERATOR SELF-CHECK PASS -> %s" % os.path.relpath(OUT, ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
