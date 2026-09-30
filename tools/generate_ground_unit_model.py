#!/usr/bin/env python3
"""Generate MainBattleTankModel.java from the Blender M1A5 export.

Deterministic: the same tools/data/m1a5_boxes.json always yields the same Java.
The hull pentagon and turret loft are rebuilt as solid boxes plus rotated armour
plates; every other part becomes one axis-aligned Minecraft box.  Also writes
tools/data/m1a5_model_boxes.json so the result can be previewed in Blender.

    python3 tools/generate_ground_unit_model.py
"""
import json
import math
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC = os.path.join(ROOT, "tools", "data", "m1a5_boxes.json")
OUT_JAVA = os.path.join(ROOT, "src", "main", "java", "io", "github", "littlebird",
                        "client", "MainBattleTankModel.java")
OUT_BOXES = os.path.join(ROOT, "tools", "data", "m1a5_model_boxes.json")
TEXTURE_SIZE = 512

# The gun elevates on its own trunnion instead of taking the whole turret with it. The barrel axis
# runs at 31.36 units above the hull floor and the trunnion sits at the mantlet's mid plane, 30.72
# units ahead of the ring: that is the joint the barrel really pivots in, so the barrel stays
# centred in the shield at any elevation and never pulls out of it.
GUN_PIVOT_Y_UNITS = 31.36
GUN_PIVOT_Z_UNITS = -30.72
GUN_GROUP = "gun"

UV = {
    "olive": (0, 0), "olive_dark": (0, 160), "roller": (256, 160), "track": (0, 224),
    "steel": (256, 256), "gunmetal": (0, 320), "sleeve_tan": (256, 320),
    "glass": (384, 320), "light_yellow": (256, 384), "discharger": (320, 384),
    "marking": (384, 384), "hub": (0, 416), "skirt": (448, 320),
}


def jf(x):
    s = ("%.3f" % float(x)).rstrip("0")
    if s.endswith("."):
        s += "0"
    if s in ("-0.0", "-0"):
        s = "0.0"
    return s + "F"


def jname(n):
    return (n[5:] if n.startswith("M1A5_") else n).lower()


def plate(name, group, uv, p_tail, p_nose, half_x, thick):
    """Rotated armour plate hinged at p_tail (upper/rear) and reaching p_nose (lower/front)."""
    dz = p_nose[0] - p_tail[0]        # negative (toward the nose)
    dy = p_nose[1] - p_tail[1]        # negative
    length = math.hypot(dz, dy)
    ang = math.degrees(math.atan2(-dy, -dz))
    return {"name": name, "group": group, "uv": uv, "origin": [0.0, p_tail[1], p_tail[0]],
            "rot": ("x", -ang), "box": [-half_x, -thick / 2.0, -length - 1.5,
                                        2 * half_x, thick, length + 3.0]}


def build_boxes(spec):
    parts = spec["parts"]
    pivot_y = spec["pivot"][1]
    out = []

    for p in parts:
        n = p["name"]
        if p["kind"] in ("complex", "ring"):
            continue
        if p["group"] == "turret":
            base = [p["min"][0], p["min"][1] - pivot_y, p["min"][2]]
        elif p["group"] == GUN_GROUP:
            # Gun parts are emitted under the trunnion part, so their boxes are trunnion-local.
            base = [p["min"][0], p["min"][1] - GUN_PIVOT_Y_UNITS,
                    p["min"][2] - GUN_PIVOT_Z_UNITS]
        else:
            base = list(p["min"])
        size = [p["max"][i] - p["min"][i] for i in range(3)]
        out.append({"name": jname(n), "group": p["group"], "uv": p["category"],
                    "origin": [0.0, 0.0, 0.0], "rot": None,
                    "box": [base[0], base[1], base[2], size[0], size[1], size[2]]})

    # --- track rings: hollow band so the road wheels stay visible ---
    for p in parts:
        if p["kind"] != "ring":
            continue
        r = p["ring"]
        x0, x1 = p["min"][0], p["max"][0]
        zc, ro, ri = r["zc"] * 16.0, r["r_out"] * 16.0, r["r_in"] * 16.0
        zf, zr = r["y_front"] * 16.0, r["y_rear"] * 16.0
        side = jname(p["name"]).split("_")[-1]
        w = x1 - x0
        z0 = zf - ro
        band = (zr - zf) + 2 * ro
        chunks = max(1, int(math.ceil(band / 36.0)))
        clen = band / chunks
        for c in range(chunks):
            z = z0 + c * clen
            for tag, y in (("top", zc + ri), ("bot", zc - ro)):
                out.append({"name": "track_%s_%s_%d" % (side, tag, c), "group": "body",
                            "uv": "track", "origin": [0.0, 0.0, 0.0], "rot": None,
                            "box": [x0, y, z, w, ro - ri, clen]})
        out.append({"name": "track_%s_front" % side, "group": "body", "uv": "track",
                    "origin": [0.0, 0.0, 0.0], "rot": None,
                    "box": [x0, zc - ro, zf - ro, w, 2 * ro, ri + 1.0]})
        out.append({"name": "track_%s_rear" % side, "group": "body", "uv": "track",
                    "origin": [0.0, 0.0, 0.0], "rot": None,
                    "box": [x0, zc - ro, zr - 1.0, w, 2 * ro, ri + 1.0]})

    # --- hull: solid boxes + two rotated slope plates, from the exported profile ---
    hx = spec["profiles"]["hull_half"]
    pts = sorted((y * 16.0, z * 16.0) for y, z in spec["profiles"]["hull"])
    bot = min(z for _, z in pts)
    roof = max(z for _, z in pts)
    roof_pts = sorted(p for p in pts if abs(p[1] - roof) < 1e-6)
    C, D = roof_pts[0], roof_pts[-1]
    nose_pts = sorted(p for p in pts if p[0] < C[0])
    A, B = nose_pts[0], nose_pts[-1]
    out += [
        {"name": "hull_lower", "group": "body", "uv": "olive", "origin": [0.0, 0.0, 0.0], "rot": None,
         "box": [-hx, bot, B[0], 2 * hx, B[1] - bot, D[0] - B[0]]},
        {"name": "hull_upper", "group": "body", "uv": "olive", "origin": [0.0, 0.0, 0.0], "rot": None,
         "box": [-hx, B[1], C[0], 2 * hx, roof - B[1], D[0] - C[0]]},
        {"name": "hull_nose", "group": "body", "uv": "olive", "origin": [0.0, 0.0, 0.0], "rot": None,
         "box": [-hx, bot, A[0], 2 * hx, 3.0, B[0] - A[0]]},
        plate("hull_glacis", "body", "olive", C, B, hx, 5.0),
        plate("hull_noseplate", "body", "olive", B, A, hx, 6.0),
    ]

    # --- turret shell from the lofted sections, plus rotated top plates ---
    secs = [[y * 16.0, w * 16.0, z1 * 16.0 - pivot_y] for y, w, _z0, z1 in spec["profiles"]["turret"]]
    zs = [s[0] for s in secs]
    # solid core: one box per section interval, so the plan taper steps are small
    for i in range(len(secs) - 1):
        a, b = secs[i], secs[i + 1]
        w, h = max(a[1], b[1]), max(a[2], b[2])
        out.append({"name": "turret_seg%d" % i, "group": "turret", "uv": "olive",
                    "origin": [0.0, 0.0, 0.0], "rot": None,
                    "box": [-w / 2.0, 0.0, a[0], w, h, b[0] - a[0]]})
    # yawed cheek plates close the plan taper along each turret flank
    for side in (+1, -1):
        for i in range(3):
            a, b = secs[i], secs[i + 1]
            x0, z0 = side * a[1] / 2.0, a[0]
            x1, z1 = side * b[1] / 2.0, b[0]
            dx, dz = x1 - x0, z1 - z0
            length = math.hypot(dx, dz)
            ang = math.degrees(math.atan2(dx, dz))
            out.append({"name": "turret_cheek_%s%d" % ("R" if side > 0 else "L", i),
                        "group": "turret", "uv": "olive", "origin": [x0, 0.0, z0],
                        "rot": ("y", ang),
                        "box": [-1.5, 0.0, 0.0, 3.0, min(a[2], b[2]), length + 1.0]})
    out += [
        plate("turret_top_front", "turret", "olive", (zs[1], secs[1][2]), (zs[0], secs[0][2]),
              secs[0][1] / 2.0 + 1.0, 4.0),
        plate("turret_top_mid", "turret", "olive", (zs[2], secs[2][2]), (zs[1], secs[1][2]),
              secs[1][1] / 2.0 + 1.0, 4.0),
    ]
    return out


JAVA_HEAD = '''package io.github.littlebird.client;

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
 * M1A5 main battle tank, generated from the Blender source model.
 *
 * <p>GENERATED FILE - do not edit by hand. Rebuild with
 * tools/generate_ground_unit_model.py, which reads tools/data/m1a5_boxes.json
 * (exported from blender_m1a5/m1a5.blend by export_mc.py). The Blender model is
 * real M1A2 scale: hull 126.9 units, overall width 58.6 units, 16 units per
 * block, nose along -Z, +X to the vehicle's right. Unit space is unchanged from
 * the rest of the pack; the turret ring and barrel reach are mirrored in
 * MainBattleTankGeometry so the server-side muzzle and the crew seats line up.
 */
public final class MainBattleTankModel extends EntityModel<MainBattleTankEntity> {
    public static final ModelLayerLocation MAIN_BATTLE_TANK_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "main_battle_tank"), "main");
    private static final int TEXTURE_SIZE = %d;
    private static final float DEG = (float) (Math.PI / 180.0D);
    /** Turret ring height, matching MainBattleTankGeometry.TURRET_PIVOT_HEIGHT_UNITS. */
    private static final float TURRET_Y = %s;
    /** Gun trunnion, turret-local: matching MainBattleTankGeometry.GUN_TRUNNION_*. */
    private static final float GUN_TRUNNION_Y = %s;
    private static final float GUN_TRUNNION_Z = %s;

    private final ModelPart root;
    private final ModelPart turret;
    private final ModelPart gun;

    public MainBattleTankModel(ModelPart root) {
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
                PartPose.offset(0.0F, TURRET_Y, 0.0F));
        PartDefinition gun = turret.addOrReplaceChild("gun", CubeListBuilder.create(),
                PartPose.offset(0.0F, GUN_TRUNNION_Y, GUN_TRUNNION_Z));
'''


def emit(part, parent):
    box = part["box"]
    off = part["origin"]
    cb = ".texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)" % (
        UV[part["uv"]][0], UV[part["uv"]][1], jf(box[0]), jf(box[1]), jf(box[2]),
        jf(box[3]), jf(box[4]), jf(box[5]))
    if part["rot"] is None:
        pose = "PartPose.offset(%s, %s, %s)" % (jf(off[0]), jf(off[1]), jf(off[2]))
    else:
        axis, deg = part["rot"]
        rot = {"x": "%s * DEG" % jf(deg), "y": "0.0F", "z": "0.0F"}
        if axis != "x":
            rot["x"] = "0.0F"
            rot[axis] = "%s * DEG" % jf(deg)
        pose = "PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)" % (
            jf(off[0]), jf(off[1]), jf(off[2]), rot["x"], rot["y"], rot["z"])
    return ('        %s.addOrReplaceChild("%s", CubeListBuilder.create()\n'
            '                %s,\n                %s);' % (parent, part["name"], cb, pose))


def audit_uv(boxes):
    """Every box's Minecraft UV footprint must stay inside its texture zone.

    A box of size (w,h,d) at texOffs (u,v) samples 2*(w+d) across and h+d down."""
    import sys
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    from generate_ground_unit_textures import TANK_ZONES
    bad = []
    for b in boxes:
        u, v = UV[b["uv"]]
        zx, zy, zw, zh = TANK_ZONES[b["uv"]]
        w, h, d = b["box"][3], b["box"][4], b["box"][5]
        if u + 2 * (w + d) > zx + zw or v + (h + d) > zy + zh:
            bad.append(b["name"])
    return bad


def world_aabb(b):
    """Axis-aligned world bounds of a (possibly rotated) box, in model units."""
    x0, y0, z0, dx, dy, dz = b["box"]
    ox, oy, oz = b["origin"]
    pts = []
    for i in (0, 1):
        for j in (0, 1):
            for k in (0, 1):
                x, y, z = x0 + i * dx + ox, y0 + j * dy, z0 + k * dz
                if b["rot"] and b["rot"][0] == "x":
                    th = math.radians(b["rot"][1])
                    cs, sn = math.cos(th), math.sin(th)
                    y, z = y * cs - z * sn, y * sn + z * cs
                pts.append((x, y + oy, z + oz))
    lo = [min(p[i] for p in pts) for i in range(3)]
    hi = [max(p[i] for p in pts) for i in range(3)]
    if b["group"] == GUN_GROUP:
        # The preview lists every turret entry relative to the ring, so put the trunnion back.
        loft = GUN_PIVOT_Y_UNITS - b.get("pivot_y", GUN_PIVOT_Y_UNITS)
        lo[1] += loft
        hi[1] += loft
        lo[2] += GUN_PIVOT_Z_UNITS
        hi[2] += GUN_PIVOT_Z_UNITS
    return (lo, hi)


def write_models_data(boxes, pivot):
    """Emit the models/ STL box list so the exported STL matches the in-game model."""
    path = os.path.join(ROOT, "models", "src", "ground_units_tank_data.py")
    out = ['"""GENERATED from blender_m1a5 by tools/generate_ground_unit_model.py.',
           "Do not edit by hand. Minecraft model units (16 per block), Y up, nose -Z;",
           'turret entries are relative to the ring, lifted by TURRET_OFFSET_Y_UNITS."""',
           "", "TURRET_OFFSET_Y_UNITS = %.2f" % pivot, "", "TANK_BOXES = ["]
    for b in boxes:
        mn, mx = world_aabb(b)
        out.append('    ("%s", %s, %s, %s, %s, %s, %s, %s),' % (
            b["name"], "True" if b["group"] in ("turret", GUN_GROUP) else "False",
            round(mn[0], 3), round(mn[1], 3), round(mn[2], 3),
            round(mx[0] - mn[0], 3), round(mx[1] - mn[1], 3), round(mx[2] - mn[2], 3)))
    out.append("]")
    open(path, "w").write("\n".join(out) + "\n")
    return path


def main():
    spec = json.load(open(SPEC))
    boxes = build_boxes(spec)
    pivot = spec["pivot"][1]
    write_models_data(boxes, pivot)
    bad = audit_uv(boxes)
    if bad:
        raise SystemExit("UV footprint outside its texture zone: %s" % bad[:8])

    lines = [JAVA_HEAD % (TEXTURE_SIZE, jf(pivot), jf(GUN_PIVOT_Y_UNITS - pivot),
                          jf(GUN_PIVOT_Z_UNITS))]
    for group, var in (("body", "body"), ("turret", "turret"), (GUN_GROUP, "gun")):
        lines.append("        // ---- %s (%d parts) ----" % (group, sum(1 for b in boxes if b["group"] == group)))
        for b in boxes:
            if b["group"] == group:
                lines.append(emit(b, var))
    lines.append("        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);")
    lines.append("    }")
    lines.append('''
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
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue,
                               float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
''')
    open(OUT_JAVA, "w").write("\n".join(lines))
    json.dump(UV, open(os.path.join(ROOT, "tools", "data", "m1a5_uv.json"), "w"), indent=1)
    json.dump(boxes, open(OUT_BOXES, "w"), indent=1)
    print("MODEL parts=%d body=%d turret=%d gun=%d pivot=%.2f java=%s" % (
        len(boxes), sum(1 for b in boxes if b["group"] == "body"),
        sum(1 for b in boxes if b["group"] == "turret"),
        sum(1 for b in boxes if b["group"] == GUN_GROUP), pivot, OUT_JAVA))


if __name__ == "__main__":
    main()
