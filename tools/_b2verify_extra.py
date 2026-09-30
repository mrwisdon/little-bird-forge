#!/usr/bin/env python3
"""mc-verify: box-texture zone means, renderer wiring, insert-only evidence."""
import hashlib
import json
import os
import re
import sys
from collections import Counter

sys.path.insert(0, "/root/projects/little-bird-forge/tools")
from _b2verify_uvcolor import png_rows  # noqa: E402

ROOT = "/root/projects/little-bird-forge"
R = []


def check(key, ok, detail):
    R.append((key, bool(ok), detail))
    print(("PASS  " if ok else "FAIL  ") + key + " :: " + detail, flush=True)


def note(key, detail):
    R.append((key, None, detail))
    print("INFO  " + key + " :: " + detail, flush=True)


def sha(p):
    return hashlib.sha256(open(p, "rb").read()).hexdigest()


def main():
    mp = os.path.join(ROOT, "src/main/java/io/github/littlebird/client/B2Model.java")
    src = open(mp, "r", errors="replace").read()
    offs = re.findall(r"\.texOffs\((\d+),\s*(\d+)\)", src)
    cnt = Counter(offs)
    note("extra.B2Model.texOffs", "distinct=%d cubes_total=%d %s" % (
        len(cnt), sum(cnt.values()),
        {("(%s,%s)" % k): v for k, v in sorted(cnt.items())}))
    png = os.path.join(ROOT, "src/main/resources/assets/battlefield/textures/entity/b2.png")
    W, H, rows = png_rows(png)
    check("extra.b2png.size", W == H == 512 and (W & (W - 1)) == 0,
          "%dx%d bytes=%d sha256=%s" % (W, H, os.path.getsize(png), sha(png)[:16]))
    allpx = [(rows[y][x][0], rows[y][x][1], rows[y][x][2])
             for y in range(0, H, 4) for x in range(0, W, 4)]
    note("extra.b2png.mean_full", "mean=#%02X%02X%02X sampled=%d px" % (
        sum(p[0] for p in allpx) // len(allpx), sum(p[1] for p in allpx) // len(allpx),
        sum(p[2] for p in allpx) // len(allpx), len(allpx)))
    for (u, v) in sorted(set(offs), key=lambda t: (int(t[0]), int(t[1]))):
        u0, v0 = int(u), int(v)
        px = [(rows[y][x][0], rows[y][x][1], rows[y][x][2])
              for y in range(v0, min(H, v0 + 64)) for x in range(u0, min(W, u0 + 64))]
        mean = tuple(sum(p[i] for p in px) // len(px) for i in range(3))
        note("extra.b2png.zone(%d,%d)" % (u0, v0),
             "cubes=%d mean=#%02X%02X%02X zone_max=%d zone_min=%d" % (
                 cnt[(u, v)], mean[0], mean[1], mean[2], max(max(p) for p in px),
                 min(min(p) for p in px)))
        check("extra.b2png.zone_semantics(%d,%d)" % (u0, v0),
              max(mean) < 130 or (max(mean) - min(mean)) < 25,
              "zone mean=#%02X%02X%02X -> %s" % (
                  mean[0], mean[1], mean[2],
                  "dark skin/panel family" if max(mean) < 130
                  else "bright but desaturated = metal/white detail zone"))
    ce = os.path.join(ROOT, "src/main/java/io/github/littlebird/client/ClientModEvents.java")
    ces = open(ce, "r", errors="replace").read()
    used = re.findall(r"registerEntityRenderer\(ModEntities\.B2\.get\(\)\s*,\s*(\w+)", ces)
    check("extra.renderer_wired", len(used) == 1,
          "renderer=%s B2_LAYER refs=%d sha256=%s" % (
              used, len(re.findall(r"B2Model\.B2_LAYER", ces)), sha(ce)[:16]))
    for cls in ("B2Renderer", "B2ObjRenderer"):
        p = os.path.join(ROOT, "src/main/java/io/github/littlebird/client/%s.java" % cls)
        t = open(p, "r", errors="replace").read() if os.path.exists(p) else ""
        note("extra." + cls, "exists=%s lines=%d uses_B2_PREVIEW=%s entity_b2_png=%s sha256=%s" % (
            bool(t), len(t.splitlines()), "B2_PREVIEW" in t,
            "textures/entity/b2.png" in t, sha(p)[:16] if t else "-"))
    ob = os.path.join(ROOT, "src/main/java/io/github/littlebird/client/B2ObjRenderer.java")
    obs = open(ob, "r", errors="replace").read()
    for k in ("OBJ_LENGTH_BLOCKS", "SCALE", "MESH_BOTTOM_Y", "MESH_CENTRE_X",
              "MESH_CENTRE_Z", "OBJ_HALF_SPAN_BLOCKS"):
        m = re.search(k + r"\s*=\s*([^;]+);", obs)
        if m:
            note("extra.B2ObjRenderer." + k, m.group(1).strip()[:110])
    ent = os.path.join(ROOT, "src/main/java/io/github/littlebird/entity/B2Entity.java")
    note("extra.B2Entity", "lines=%d extends=%s sha256=%s" % (
        len(open(ent, "r", errors="replace").read().splitlines()),
        (re.search(r"class B2Entity\s+extends\s+(\w+)",
                   open(ent, "r", errors="replace").read()) or [None, "?"])[1],
        sha(ent)[:16]))
    it = os.path.join(ROOT, "src/main/java/io/github/littlebird/registry/ModItems.java")
    its = open(it, "r", errors="replace").read()
    m = re.search(r'B2_PREVIEW\s*=\s*ITEMS\.register\("b2_preview",\s*\(\)\s*->\s*([^;]+);',
                  its, re.S)
    note("extra.ModItems.b2_preview", (m.group(1).strip()[:160] if m else "not found"))
    m2 = re.search(r'B2\s*=\s*ITEMS\.register\("b2",\s*\(\)\s*->\s*([^;]+);', its, re.S)
    note("extra.ModItems.b2", (m2.group(1).strip()[:160] if m2 else "not found"))
    nf = sum(1 for _k, o, _d in R if o is False)
    print("=== EXTRA SUMMARY: %d PASS / %d FAIL ===" % (
        sum(1 for _k, o, _d in R if o is True), nf))
    return 1 if nf else 0


if __name__ == "__main__":
    sys.exit(main())
