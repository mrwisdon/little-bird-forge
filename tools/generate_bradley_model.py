#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate BradleyModel.java from the Blender Bradley export.

Deterministic: the same tools/data/bradley_boxes.json always yields the same Java.
Also writes tools/data/bradley_uv.json (the texture zone map the painter must honour).

    python3 tools/generate_bradley_model.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC = os.path.join(ROOT, "tools", "data", "bradley_boxes.json")
OUT_JAVA = os.path.join(ROOT, "src", "main", "java", "io", "github", "littlebird",
                        "client", "BradleyModel.java")
OUT_UV = os.path.join(ROOT, "tools", "data", "bradley_uv.json")
TEXTURE_SIZE = 512

# The 25mm elevates on its own trunnion instead of taking the turret with it. The trunnion is the
# barrel axis (GUN_AXIS_HEIGHT_UNITS, matching BradleyGeometry) at the mantlet's mid plane, so the
# barrel and its shield swing together and the barrel stays inside the shield's bore at any
# elevation. Ring-local units, forward is -Z.
GUN_PIVOT_Y_UNITS = 10.4
GUN_PIVOT_Z_UNITS = -20.5
GUN_GROUP = "gun"
PAD = 4


def jf(x):
    s = ("%.3f" % float(x)).rstrip("0")
    if s.endswith("."):
        s += "0"
    if s in ("-0.0", "-0"):
        s = "0.0"
    return s + "F"


def jname(n):
    return n[4:].lower() if n.startswith("GEO-") else n.lower()


def footprint(box):
    w, h, d = box[3], box[4], box[5]
    return (2.0 * (w + d), h + d)


def pack(parts):
    """Lay each category out as a rectangle big enough for its widest UV footprint."""
    need = {}
    for p in parts:
        w, h = footprint(p["box"])
        cur = need.get(p["uv"])
        need[p["uv"]] = (max(cur[0], w) if cur else w, max(cur[1], h) if cur else h)
    zones = {}
    x = y = row_h = 0
    for cat in sorted(need):
        w = int(need[cat][0] + 0.999) + 1
        h = int(need[cat][1] + 0.999) + 1
        if x + w + PAD > TEXTURE_SIZE:
            x = 0
            y += row_h + PAD
            row_h = 0
        zones[cat] = [x, y]
        x += w + PAD
        row_h = max(row_h, h)
    if y + row_h > TEXTURE_SIZE:
        raise SystemExit("UV layout does not fit %d: %d" % (TEXTURE_SIZE, y + row_h))
    return zones, need


def main():
    spec = json.load(open(SPEC))
    parts = []
    for p in spec["parts"]:
        size = [p["max"][i] - p["min"][i] for i in range(3)]
        base_y, base_z = p["min"][1], p["min"][2]
        if p["group"] == GUN_GROUP:
            # Gun parts hang off the trunnion, so their boxes are trunnion-local.
            base_y -= GUN_PIVOT_Y_UNITS
            base_z -= GUN_PIVOT_Z_UNITS
        parts.append({"name": jname(p["name"]), "group": p["group"], "uv": p["category"],
                      "box": [p["min"][0], base_y, base_z, size[0], size[1], size[2]]})
    # Duplicate names would silently collide inside one ModelPart parent.
    seen = {}
    for b in parts:
        n = b["name"]
        if n in seen:
            seen[n] += 1
            b["name"] = "%s_%d" % (n, seen[n])
        else:
            seen[n] = 0
    zones, need = pack(parts)
    pivot = spec["pivot"]
    bad = []
    for b in parts:
        u, v = zones[b["uv"]]
        w, h = footprint(b["box"])
        if u + w > TEXTURE_SIZE or v + h > TEXTURE_SIZE:
            bad.append(b["name"])
    if bad:
        raise SystemExit("UV footprint outside the sheet: %s" % bad[:8])
    json.dump({"texture_size": TEXTURE_SIZE, "zones": zones,
               "need": {k: [round(v[0], 1), round(v[1], 1)] for k, v in need.items()}},
              open(OUT_UV, "w"), indent=1)

    def emit(b, parent):
        bx = b["box"]
        u, v = zones[b["uv"]]
        cb = (".texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)"
              % (u, v, jf(bx[0]), jf(bx[1]), jf(bx[2]), jf(bx[3]), jf(bx[4]), jf(bx[5])))
        return ('        %s.addOrReplaceChild("%s", CubeListBuilder.create()\n'
                '                %s,\n                PartPose.offset(0.0F, 0.0F, 0.0F));'
                % (parent, b["name"], cb))

    head = '''package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.BradleyEntity;
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
 * M2A2 Bradley, generated from the Blender source model.
 *
 * <p>GENERATED FILE - do not edit by hand. Rebuild with tools/generate_bradley_model.py, which
 * reads tools/data/bradley_boxes.json (exported from bradley/out/M2A2_bradley.blend by
 * export_mc_bradley.py). 16 units per block, 1 block = 1 m, so the hull is 104.8 units long and
 * 52.5 over the skirts, matching BradleyGeometry. Nose along -Z, +X to the vehicle's right; the
 * turret parts are relative to the ring, which sits at (%s, %s, %s).
 */
public final class BradleyModel extends EntityModel<BradleyEntity> {
    public static final ModelLayerLocation BRADLEY_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "bradley"), "main");
    private static final int TEXTURE_SIZE = %d;
    /** Turret ring, matching BradleyGeometry.TURRET_PIVOT_HEIGHT_UNITS. */
    private static final float TURRET_Y = %s;
    /** Ring offset aft of the hull centre, matching BradleyGeometry.TURRET_PIVOT_FORWARD_UNITS. */
    private static final float TURRET_Z = %s;
    /** Gun trunnion, ring-local: matching BradleyGeometry.GUN_TRUNNION_*. */
    private static final float GUN_TRUNNION_Y = %s;
    private static final float GUN_TRUNNION_Z = %s;

    private final ModelPart root;
    private final ModelPart turret;
    private final ModelPart gun;

    public BradleyModel(ModelPart root) {
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
                PartPose.offset(0.0F, TURRET_Y, TURRET_Z));
        PartDefinition gun = turret.addOrReplaceChild("gun", CubeListBuilder.create(),
                PartPose.offset(0.0F, GUN_TRUNNION_Y, GUN_TRUNNION_Z));
''' % (jf(pivot[0]), jf(pivot[1]), jf(pivot[2]), TEXTURE_SIZE,
       jf(pivot[1]), jf(pivot[2]), jf(GUN_PIVOT_Y_UNITS), jf(GUN_PIVOT_Z_UNITS))

    lines = [head]
    for group, var in (("body", "body"), ("turret", "turret"), (GUN_GROUP, "gun")):
        sel = [b for b in parts if b["group"] == group]
        lines.append("        // ---- %s (%d parts) ----" % (group, len(sel)))
        for b in sel:
            lines.append(emit(b, var))
    lines.append("        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);")
    lines.append("    }")
    lines.append('''
    @Override
    public void setupAnim(BradleyEntity entity, float limbSwing, float limbSwingAmount,
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
    print("wrote %s: %d body / %d turret / %d gun parts" % (
        OUT_JAVA, sum(1 for b in parts if b["group"] == "body"),
        sum(1 for b in parts if b["group"] == "turret"),
        sum(1 for b in parts if b["group"] == GUN_GROUP)))
    print("uv zones:", json.dumps(zones))


main()
