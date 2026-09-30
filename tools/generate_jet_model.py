#!/usr/bin/env python3
"""Generate JetModel.java (F-35E) from the Blender export.

    python3 tools/generate_jet_model.py
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC = os.path.join(ROOT, "tools", "data", "f35_mc.json")
OUT = os.path.join(ROOT, "src", "main", "java", "io", "github", "littlebird", "client", "JetModel.java")
TEXTURE_SIZE = 512
UV = {"body": (0, 0), "deck": (256, 0), "glass": (256, 128),
      "engine": (384, 128), "store": (256, 192), "accent": (384, 192)}


def jf(x):
    s = ("%.3f" % float(x)).rstrip("0")
    if s.endswith("."):
        s += "0"
    return ("0.0" if s in ("-0", "-0.0") else s) + "F"


def jname(n):
    s = re.sub(r"[^0-9a-zA-Z]+", "_", n[5:] if n.startswith("F35_") else n).strip("_").lower()
    return s or "part"


def main():
    spec = json.load(open(SPEC))
    parts = spec["parts"]
    body = [p for p in parts if p["group"] == "body"]
    nozzle_parts = [p for p in parts if p["group"] == "nozzle"]
    nz = next(p for p in parts if p["name"] == "F35_Nozzle")
    pivot = [(nz["min"][i] + nz["max"][i]) / 2.0 for i in range(3)]

    def emit(part, parent, name=None, base=None):
        mn = part["min"] if base is None else [part["min"][i] - base[i] for i in range(3)]
        mx = part["max"] if base is None else [part["max"][i] - base[i] for i in range(3)]
        return ('        %s.addOrReplaceChild("%s", CubeListBuilder.create()\n'
                '                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s),\n'
                '                PartPose.offset(0.0F, 0.0F, 0.0F));' % (
                    parent, name or jname(part["name"]), UV[part["uv"]][0], UV[part["uv"]][1],
                    jf(mn[0]), jf(mn[1]), jf(mn[2]),
                    jf(mx[0] - mn[0]), jf(mx[1] - mn[1]), jf(mx[2] - mn[2])))

    lines = []
    lines.append("        // ---- fuselage sections (11 boxes) ----")
    secs = spec["profiles"]["sections"]
    for i in range(len(secs) - 1):
        a, b = secs[i], secs[i + 1]
        w = max(a[1], b[1])
        y0, y1 = min(a[2], b[2]), max(a[3], b[3])
        z0, z1 = a[0], b[0]
        lines.append('        body.addOrReplaceChild("fuselage_%d", CubeListBuilder.create()' % i)
        lines.append('                .texOffs(0, 0).addBox(%s, %s, %s, %s, %s, %s),' % (
            jf(-w / 2.0), jf(y0), jf(z0), jf(w), jf(y1 - y0), jf(z1 - z0)))
        lines.append('                PartPose.offset(0.0F, 0.0F, 0.0F));')
    lines.append("        // ---- body (%d parts) ----" % len(body))
    used = set()
    for p in body:
        nm = jname(p["name"])
        while nm in used:
            nm += "_x"
        used.add(nm)
        lines.append(emit(p, "body", nm))
    lines.append("        // ---- engine nozzle + petals (%d) ----" % len(nozzle_parts))
    lines.append('        PartDefinition nozzle = body.addOrReplaceChild("nozzle", CubeListBuilder.create()')
    lines.append('                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s),' % (
        UV[nz["uv"]][0], UV[nz["uv"]][1],
        jf(nz["min"][0] - pivot[0]), jf(nz["min"][1] - pivot[1]), jf(nz["min"][2] - pivot[2]),
        jf(nz["max"][0] - nz["min"][0]), jf(nz["max"][1] - nz["min"][1]),
        jf(nz["max"][2] - nz["min"][2])))
    lines.append('                PartPose.offset(%s, %s, %s));' % (
        jf(pivot[0]), jf(pivot[1]), jf(pivot[2])))
    for p in nozzle_parts:
        if p is nz:
            continue
        lines.append(emit(p, "nozzle", jname(p["name"]), base=pivot))

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
 * F-35E Panther, generated from the Blender source model.
 *
 * <p>GENERATED FILE - do not edit by hand. Rebuild with tools/generate_jet_model.py, which reads
 * tools/data/f35_mc.json (exported from blender_f35/f35.blend by export_mc.py). The Blender model
 * is real F-35A scale (15.54 m long, 10.67 m span), uniformly scaled to the mod's 176-unit airframe
 * envelope so it matches JetGeometry. Nose along -Z, +X right, 16 units per block.
 */
public final class JetModel extends EntityModel<JetEntity> {
    public static final ModelLayerLocation JET_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "f35"), "main");
    private static final int TEXTURE_SIZE = %d;

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
''' % (TEXTURE_SIZE, body_code)
    open(OUT, "w").write(java)
    print("JETMODEL body=%d nozzle=%d fuselage=%d pivot=%s" % (
        len(body), len(nozzle_parts), len(secs) - 1, [round(v, 1) for v in pivot]))


if __name__ == "__main__":
    main()
