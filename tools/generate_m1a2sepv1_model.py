#!/usr/bin/env python3
"""Generate MainBattleTankModel.java from the M1A2 SEP v1 Blender export.

    python3 tools/generate_m1a2sepv1_model.py

Reads tools/data/m1a2sepv1_boxes.json (written by
blender_m1a2sepv1/work/export_mc.py) and writes
src/main/java/io/github/littlebird/client/MainBattleTankModel.java plus
models/src/ground_units_tank_data.py for the STL preview.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC = os.path.join(ROOT, "tools", "data", "m1a2sepv1_boxes.json")
OUT_JAVA = os.path.join(ROOT, "src", "main", "java", "io", "github", "littlebird",
                        "client", "MainBattleTankModel.java")
OUT_BOXES = os.path.join(ROOT, "tools", "data", "m1a2sepv1_model_boxes.json")
DATA_PY = os.path.join(ROOT, "models", "src", "ground_units_tank_data.py")
TEXTURE_SIZE = 512

UV = {
    "olive": (0, 0), "olive_dark": (0, 160), "roller": (256, 160), "track": (0, 224),
    "steel": (256, 256), "gunmetal": (0, 320), "sleeve_tan": (256, 320),
    "glass": (384, 320), "light_yellow": (256, 384), "discharger": (320, 384),
    "marking": (384, 384), "hub": (0, 416), "skirt": (448, 320),
}

SPEC_JSON = json.load(open(SPEC))
PIVOT = SPEC_JSON["pivot"]
TRUNNION = SPEC_JSON["trunnion"]
TURRET_Y = PIVOT[1]
TURRET_Z = PIVOT[2]
GUN_Y = TRUNNION[1] - TURRET_Y
GUN_Z = TRUNNION[2] - TURRET_Z


def jf(x):
    s = ("%.3f" % float(x)).rstrip("0")
    if s.endswith("."):
        s += "0"
    if s in ("-0.0", "-0"):
        s = "0.0"
    return s + "F"


def jname(n):
    n = n[5:] if n.startswith("GEO-") else n
    n = n.lower().replace("-", "_").replace(".", "_")
    return "".join(ch if (ch.isalnum() or ch == "_") else "_" for ch in n)


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
 * M1A2 SEP v1 main battle tank, generated from the Blender source model.
 *
 * <p>GENERATED FILE - do not edit by hand. Rebuild with
 * tools/generate_m1a2sepv1_model.py, which reads tools/data/m1a2sepv1_boxes.json
 * (exported from blender_m1a2sepv1/m1a2sepv1.blend by work/export_mc.py). The
 * Blender model is real M1A2 SEP v1 scale: hull 126.9 units, overall width 58.6
 * units, 16 units per block, nose along -Z, +X to the vehicle's right. The
 * turret ring and barrel reach are mirrored in MainBattleTankGeometry so the
 * server-side muzzle and the crew seats line up.
 */
public final class MainBattleTankModel extends EntityModel<MainBattleTankEntity> {
    public static final ModelLayerLocation MAIN_BATTLE_TANK_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "main_battle_tank"), "main");
    private static final int TEXTURE_SIZE = %d;
    private static final float DEG = (float) (Math.PI / 180.0D);
    /** Turret ring height, matching MainBattleTankGeometry.TURRET_PIVOT_HEIGHT_UNITS. */
    private static final float TURRET_Y = %s;
    private static final float TURRET_Z = %s;
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
                PartPose.offset(0.0F, TURRET_Y, TURRET_Z));
        PartDefinition gun = turret.addOrReplaceChild("gun", CubeListBuilder.create(),
                PartPose.offset(0.0F, GUN_TRUNNION_Y, GUN_TRUNNION_Z));
'''


def emit(part):
    box = part["box"]
    off = part["origin"]
    cb = ".texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)" % (
        UV[part["uv"]][0], UV[part["uv"]][1], jf(box[0]), jf(box[1]), jf(box[2]),
        jf(box[3]), jf(box[4]), jf(box[5]))
    if part.get("rot") is None:
        pose = "PartPose.offset(%s, %s, %s)" % (jf(off[0]), jf(off[1]), jf(off[2]))
    else:
        axis, deg = part["rot"]
        rot = {"x": "0.0F", "y": "0.0F", "z": "0.0F"}
        rot[axis] = "%s * DEG" % jf(deg)
        pose = "PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)" % (
            jf(off[0]), jf(off[1]), jf(off[2]), rot["x"], rot["y"], rot["z"])
    return ('        %s.addOrReplaceChild("%s", CubeListBuilder.create()\n'
            '                %s,\n                %s);' % (
                {"body": "body", "turret": "turret", "gun": "gun"}[part["group"]],
                jname(part["name"]), cb, pose))


def audit_uv(boxes):
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


def write_models_data(boxes):
    out = ['"""GENERATED from blender_m1a2sepv1 by tools/generate_m1a2sepv1_model.py.',
           "Do not edit by hand. Minecraft model units (16 per block), Y up, nose -Z;",
           'turret entries are relative to the ring, lifted by TURRET_OFFSET_Y_UNITS."""',
           "", "TURRET_OFFSET_Y_UNITS = %.2f" % TURRET_Y, "", "TANK_BOXES = ["]
    for b in boxes:
        x, y, z, w, h, d = b["box"]
        lift = TURRET_Y if b["group"] in ("turret", "gun") else 0.0
        out.append('    ("%s", %s, %s, %s, %s, %s, %s, %s),' % (
            jname(b["name"]), "True" if b["group"] in ("turret", "gun") else "False",
            round(x, 3), round(y + lift, 3), round(z, 3),
            round(w, 3), round(h, 3), round(d, 3)))
    out.append("]")
    open(DATA_PY, "w").write("\n".join(out) + "\n")


def main():
    boxes = SPEC_JSON["boxes"]
    bad = audit_uv(boxes)
    if bad:
        raise SystemExit("UV footprint outside its texture zone: %s" % bad[:8])

    lines = [JAVA_HEAD % (TEXTURE_SIZE, jf(TURRET_Y), jf(TURRET_Z), jf(GUN_Y), jf(GUN_Z))]
    for group in ("body", "turret", "gun"):
        lines.append("        // ---- %s (%d parts) ----" % (
            group, sum(1 for b in boxes if b["group"] == group)))
        for b in boxes:
            if b["group"] == group:
                lines.append(emit(b))
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
    json.dump(UV, open(os.path.join(ROOT, "tools", "data", "m1a2sepv1_uv.json"), "w"), indent=1)
    json.dump(boxes, open(OUT_BOXES, "w"), indent=1)
    write_models_data(boxes)
    print("MODEL parts=%d body=%d turret=%d gun=%d pivot=%.2f java=%s" % (
        len(boxes), sum(1 for b in boxes if b["group"] == "body"),
        sum(1 for b in boxes if b["group"] == "turret"),
        sum(1 for b in boxes if b["group"] == "gun"), TURRET_Y, OUT_JAVA))


if __name__ == "__main__":
    main()
