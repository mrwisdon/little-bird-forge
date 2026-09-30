#!/usr/bin/env python3
"""mc-verify: independent acceptance checks for the B-2 -> battlefield (Minecraft) port.

READ-ONLY. Every line below is a file read or a decode; no claim is taken on trust.
Usage:  python3 tools/_b2verify_checks.py [--section json,obj,tex,java,reg] [--json]
"""
import hashlib
import json
import math
import os
import re
import struct
import sys
import zlib

ROOT = "/root/projects/little-bird-forge"
S = 11.4832535885                 # contract: MC units per metre
EXP = {"len_m": 20.85, "half_span_m": 26.18, "roof_m": 4.88, "belly_m": 1.49}
TOL_M = 0.05
RESULTS = []


def check(key, ok, detail):
    RESULTS.append((key, bool(ok), detail))
    print(("PASS  " if ok else "FAIL  ") + key + " :: " + detail, flush=True)


def note(key, detail):
    RESULTS.append((key, None, detail))
    print("INFO  " + key + " :: " + detail, flush=True)


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for blk in iter(lambda: fh.read(1 << 16), b""):
            h.update(blk)
    return h.hexdigest()


def _unfilter(raw, w, h, bpp, stride):
    out = bytearray()
    prev = bytearray(stride)
    p = 0
    for _ in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        if f == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                b = prev[i]
                c = prev[i - bpp] if i >= bpp else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out += line
        prev = line
    return bytes(out)


def png_info(path):
    data = open(path, "rb").read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    pos, idat, plte = 8, b"", b""
    w = h = bd = ct = il = None
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if tag == b"IHDR":
            w, h, bd, ct, _c, _f, il = struct.unpack(">IIBBBBB", chunk)
        elif tag == b"IDAT":
            idat += chunk
        elif tag == b"PLTE":
            plte = chunk
        elif tag == b"IEND":
            break
    if il != 0:
        raise ValueError("interlaced PNG unsupported")
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    depth = max(1, bd // 8)
    bpp = max(1, nch * depth)
    stride = w * nch * depth
    raw = _unfilter(zlib.decompress(idat), w, h, bpp, stride)
    rows = []
    for y in range(h):
        row = []
        base = y * stride
        for x in range(w):
            o = base + x * nch * depth
            px = raw[o:o + nch * depth]
            if bd == 16:
                px = bytes(px[0::2])
            if ct == 3:
                i = px[0] * 3
                r, g, b, a = plte[i], plte[i + 1], plte[i + 2], 255
            elif ct == 0:
                r = g = b = px[0]; a = 255
            elif ct == 4:
                r = g = b = px[0]; a = px[1]
            elif ct == 2:
                r, g, b = px[0], px[1], px[2]; a = 255
            else:
                r, g, b, a = px[0], px[1], px[2], px[3]
            row.append((r, g, b, a))
        rows.append(row)
    return {"path": path, "w": w, "h": h, "bit_depth": bd, "colour_type": ct,
            "bytes": len(data), "sha256": sha256(path), "rows": rows}


def is_pow2(n):
    return n > 0 and (n & (n - 1)) == 0


def load_json():
    path = os.path.join(ROOT, "tools/data/b2_mc.json")
    if not os.path.exists(path):
        return None
    spec = json.load(open(path))
    parts = spec["parts"]
    mn = [min(p["min"][i] for p in parts) for i in range(3)]
    mx = [max(p["max"][i] for p in parts) for i in range(3)]
    scale = float(spec.get("scale_units_per_metre", spec.get("scale", S))) or S
    return {"spec": spec, "parts": parts, "mn": mn, "mx": mx,
            "n": len(parts), "s": scale, "path": path}


def sec_json(j):
    if j is None:
        check("json.exists", False, "missing tools/data/b2_mc.json")
        return
    spec, parts, mn, mx = j["spec"], j["parts"], j["mn"], j["mx"]
    check("json.parts", len(parts) > 0,
          "parts=%d keys=%s bytes=%d sha256=%s" % (
              len(parts), sorted(spec.keys()), os.path.getsize(j["path"]),
              sha256(j["path"])[:16]))
    bad = [p for p in parts if "min" not in p or "max" not in p]
    check("json.part_schema", not bad,
          "parts missing min/max=%d sample_keys=%s" % (len(bad), sorted(parts[0].keys())))
    byg = {}
    for p in parts:
        byg[p.get("group", "?")] = byg.get(p.get("group", "?"), 0) + 1
    note("json.groups", json.dumps(byg, sort_keys=True))
    note("json.materials", "uv/material values=%s" % (sorted({p.get("uv") for p in parts}),))
    b = spec.get("bounds", {})
    if "min" in b:
        same = (all(abs(b["min"][i] - mn[i]) < 1e-6 for i in range(3))
                and all(abs(b["max"][i] - mx[i]) < 1e-6 for i in range(3)))
        note("json.envelope_vs_parts_union",
             "envelope=%s parts-union=%s identical=%s" % (
                 [round(v, 2) for v in b["min"] + b["max"]],
                 [round(v, 2) for v in mn + mx], same))
    s = j["s"]
    note("json.scale", "scale=%.10f units/m units_per_block=%s" % (
        s, spec.get("units_per_block")))
    length_m = (mx[2] - mn[2]) / s
    half_span_m = max(abs(mn[0]), abs(mx[0])) / s
    roof_m = mx[1] / s
    bottom_m = mn[1] / s
    check("json.length_m", abs(length_m - EXP["len_m"]) <= TOL_M + 1e-9,
          "%.3f m (expect %.2f+-%.2f) z-span=%.2f units" % (
              length_m, EXP["len_m"], TOL_M, mx[2] - mn[2]))
    check("json.half_span_m", abs(half_span_m - EXP["half_span_m"]) <= TOL_M,
          "%.3f m (expect %.2f+-%.2f) |x|max=%.2f units" % (
              half_span_m, EXP["half_span_m"], TOL_M, half_span_m * s))
    check("json.roof_m", abs(roof_m - EXP["roof_m"]) <= 0.02,
          "roof=%.3f m (expect %.2f) max_y=%.2f units" % (roof_m, EXP["roof_m"], mx[1]))
    check("json.bottom_m", abs(bottom_m - 0.0) <= 0.02,
          "lowest y=%.3f m (expect 0.00) min_y=%.2f units" % (bottom_m, mn[1]))
    check("json.nose_tail_units",
          abs(mn[2] + 120.0) <= 1.0 and abs(mx[2] - 120.0) <= 1.0,
          "nose z=%.2f (contract -120.0) tail z=%.2f (contract +120.0)" % (mn[2], mx[2]))
    note("json.belly_units", "contract BELLY_BOTTOM_UNITS=17.1 (non-gear min y); "
         "parts-union min y=%.2f" % mn[1])


def _objlines(txt):
    verts, uvs, nrm, nff, tris, mtllib, usemtl, nf = [], [], 0, 0, 0, None, [], 0
    for line in txt.splitlines():
        t = line.split()
        if not t:
            continue
        if t[0] == "v" and len(t) >= 4:
            try:
                verts.append(tuple(float(x) for x in t[1:4]))
            except ValueError:
                nf += 1
        elif t[0] == "vt" and len(t) >= 3:
            try:
                uvs.append(tuple(float(x) for x in t[1:3]))
            except ValueError:
                nf += 1
        elif t[0] == "vn":
            nrm += 1
        elif t[0] == "f":
            nff += 1
            tris += max(0, len(t) - 3)
        elif t[0] == "mtllib":
            mtllib = t[1]
        elif t[0] == "usemtl":
            usemtl.append(t[1])
    return verts, uvs, nrm, nff, tris, mtllib, usemtl, nf


def sec_obj(j):
    obj = os.path.join(ROOT, "src/main/resources/assets/battlefield/models/item/b2_preview.obj")
    mtl = obj[:-4] + ".mtl"
    if not os.path.exists(obj):
        check("obj.exists", False, "missing " + obj)
        return
    txt = open(obj, "r", errors="replace").read()
    verts, uvs, nrm, nff, tris, mtllib, usemtl, nf = _objlines(txt)
    nan = sum(1 for v in verts for c in v if not math.isfinite(c))
    check("obj.counts", len(verts) > 0,
          "v=%d vt=%d vn=%d f=%d tris=%d bytes=%d sha256=%s" % (
              len(verts), len(uvs), nrm, nff, tris, os.path.getsize(obj),
              sha256(obj)[:16]))
    check("obj.no_nan", nf == 0 and nan == 0,
          "unparsable numeric lines=%d non-finite verts=%d" % (nf, nan))
    mn = [min(v[i] for v in verts) for i in range(3)]
    mx = [max(v[i] for v in verts) for i in range(3)]
    ext = [mx[i] - mn[i] for i in range(3)]
    note("obj.bbox", "min=%s max=%s ext=%s (OBJ space, metres)" % (
        [round(v, 3) for v in mn], [round(v, 3) for v in mx], [round(v, 3) for v in ext]))
    check("obj.bbox_matches_contract",
          abs(ext[0] - 20.85) <= 0.05 and abs(ext[1] - 5.16) <= 0.3
          and abs(max(abs(mn[2]), abs(mx[2])) - 26.18) <= 0.05,
          "nose-axis=%.3f m (20.85+-0.05) up=%.3f m (0..~5.16) span/2=%.3f m (26.18+-0.05)"
          % (ext[0], ext[1], max(abs(mn[2]), abs(mx[2]))))
    check("obj.ground_and_centred",
          abs(mn[1]) <= 0.02 and mn[0] < 0 < mx[0] and mn[2] < 0 < mx[2],
          "y_min=%.3f (0=ground) x=[%.2f,%.2f] z=[%.2f,%.2f]" % (
              mn[1], mn[0], mx[0], mn[2], mx[2]))
    if j:
        s = j["s"]
        exp = {"span": (j["mx"][0] - j["mn"][0]) / s, "up": (j["mx"][1] - j["mn"][1]) / s,
               "len": (j["mx"][2] - j["mn"][2]) / s}
        oext = sorted(ext, reverse=True)
        order = {"span": oext[0], "len": oext[1], "up": oext[2]}
        errs = {k: (order[k] / exp[k] - 1.0) if exp[k] else None for k in exp}
        check("obj.matches_json_metres",
              all(abs(v) <= 0.02 for v in errs.values()),
              "obj ranked(span,len,up)=[%.3f,%.3f,%.3f] json=[%.3f,%.3f,%.3f] err=%s" % (
                  oext[0], oext[1], oext[2], exp["span"], exp["len"], exp["up"],
                  {k: round(v, 4) for k, v in errs.items()}))
    check("obj.mtllib", mtllib == "b2_preview.mtl",
          "mtllib=%r expect 'b2_preview.mtl' exists=%s" % (mtllib, os.path.exists(mtl)))
    check("obj.usemtl_present", mtllib is not None,
          "usemtl distinct=%s" % (sorted(set(usemtl)),))
    pj = os.path.join(ROOT, "src/main/resources/assets/battlefield/models/item/b2_preview.json")
    flip_v = False
    if os.path.exists(pj):
        try:
            pjdat = json.load(open(pj))
            flip_v = bool(pjdat.get("flip_v", False))
            note("obj.preview_json", "loader=%s model=%s flip_v=%s" % (
                pjdat.get("loader"), pjdat.get("model"), flip_v))
        except Exception as exc:
            check("obj.preview_json", False, "unreadable: %r" % (exc,))
    off, used = [], set()
    for (u, v) in uvs:
        vv = (1.0 - v) if flip_v else v
        cu = (math.floor(u * 4.0) + 0.5) / 4.0
        cv = (math.floor(vv * 4.0) + 0.5) / 4.0
        off.append(max(abs(u - cu), abs(vv - cv)))
        used.add((int(math.floor(u * 4.0)) % 4, int(math.floor(vv * 4.0)) % 4))
    worst = max(off) if off else 9.9
    check("obj.uv_on_grid_centres", worst <= 0.005,
          "flip_v=%s vt=%d worst|uv-centre|=%.5f (<=0.005) cells=%d/16 %s" % (
              flip_v, len(uvs), worst, len(used), sorted(used)))
    check("obj.uv_in_range",
          bool(uvs) and all(-1e-6 <= u <= 1 + 1e-6 and -1e-6 <= v <= 1 + 1e-6
                            for (u, v) in uvs),
          "vt all inside [0,1]: %s" % (bool(uvs),))
    check("obj.tri_count_plausible", 1000 <= tris <= 200000,
          "tris=%d (Lead claim 31652)" % tris)
    if os.path.exists(mtl):
        m = open(mtl, "r", errors="replace").read()
        kd = re.findall(r"^\s*map_Kd\s+(\S+)", m, re.M)
        newmtl = re.findall(r"^\s*newmtl\s+(\S+)", m, re.M)
        tex = os.path.join(ROOT,
                           "src/main/resources/assets/battlefield/textures/item/b2_preview.png")
        check("mtl.map_Kd", kd == ["battlefield:item/b2_preview"] and os.path.exists(tex),
              "newmtl=%s map_Kd=%s target_texture_exists=%s sha256=%s" % (
                  newmtl, kd, os.path.exists(tex), sha256(mtl)[:16]))
    else:
        check("mtl.exists", False, "missing " + mtl)


def sec_tex():
    ent = os.path.join(ROOT, "src/main/resources/assets/battlefield/textures/entity/b2.png")
    itm = os.path.join(ROOT,
                       "src/main/resources/assets/battlefield/textures/item/b2_preview.png")
    for tag, path, want in (("entity_b2", ent, 512), ("item_b2_preview", itm, 256)):
        if not os.path.exists(path):
            check("tex.%s" % tag, False, "missing " + path)
            continue
        info = png_info(path)
        check("tex.%s.size" % tag,
              info["w"] == info["h"] == want and is_pow2(info["w"]) and is_pow2(info["h"]),
              "%dx%d bit_depth=%d colour_type=%d (expect %dx%d pow2) bytes=%d sha256=%s" % (
                  info["w"], info["h"], info["bit_depth"], info["colour_type"], want, want,
                  info["bytes"], info["sha256"][:16]))
        if tag == "item_b2_preview" and info["w"] == 256:
            cw = info["w"] // 4
            flat, centres, mismatch = True, [], 0
            for r in range(4):
                for c in range(4):
                    ref = info["rows"][r * cw + cw // 2][c * cw + cw // 2]
                    centres.append(ref)
                    for y in range(r * cw, r * cw + cw):
                        for x in range(c * cw, c * cw + cw):
                            if info["rows"][y][x][:3] != ref[:3]:
                                flat = False
                                mismatch += 1
            hexes = ["%02X%02X%02X" % c[:3] for c in centres]
            check("tex.item.cells_flat", flat,
                  "16 cells x 64px uniform=%s pixels_off_centre_colour=%d" % (flat, mismatch))
            check("tex.item.cells_distinct", len(set(hexes)) >= 14,
                  "distinct=%d/16 row-major=%s" % (len(set(hexes)), hexes))
            note("tex.item.centres_xy",
                 str([(c * cw + cw // 2, r * cw + cw // 2) for r in range(4) for c in range(4)]))


JNAME = lambda s: re.sub(r"[^0-9a-zA-Z]+", "_",
                         s[3:] if s.startswith("B2_") else s).strip("_").lower()


def sec_java(j):
    mp = os.path.join(ROOT, "src/main/java/io/github/littlebird/client/B2Model.java")
    if not os.path.exists(mp):
        check("java.B2Model.exists", False, "missing " + mp)
        return
    src = open(mp, "r", errors="replace").read()
    stmts = re.findall(r'addOrReplaceChild\("([^"]+)"\s*,(.*?)\)\s*;', src, re.S)
    nchild = len(stmts)
    flat = []
    for name, body in stmts:
        mo = re.search(r"PartPose\.offset\(([^)]*)\)", body)
        if not mo:
            continue
        o = [float(x.strip().rstrip("Ff")) for x in mo.group(1).split(",")]
        for m in re.finditer(r"addBox\(([^)]*)\)", body):
            a = [float(x.strip().rstrip("Ff")) for x in m.group(1).split(",")]
            flat.append((name, o, a))
    if j is None:
        check("java.B2Model.box_count", False, "cannot compare: b2_mc.json missing")
        return
    cubes = []
    for (name, o, a) in flat:
        mn = [o[i] + a[i] for i in range(3)]
        mx = [o[i] + a[i] + a[i + 3] for i in range(3)]
        cubes.append((name, mn, mx, a[3] * a[4] * a[5]))
    ncube = len(cubes)
    if not cubes:
        check("java.B2Model.cube_count", False, "no addBox() parsed out of B2Model.java")
        return
    check("java.B2Model.cube_count", ncube > 0,
          "addBox()=%d children=%d b2_mc.json parts=%d B2_LAYER=%s id-b2=%s sha256=%s" % (
              ncube, nchild, j["n"], "B2_LAYER" in src,
              bool(re.search(r'"b2"', src)), sha256(mp)[:16]))
    check("java.B2Model.box_count_equals_parts", ncube == j["n"],
          "addBox()=%d vs b2_mc.json parts=%d -> delta=%+d cubes" % (
              ncube, j["n"], ncube - j["n"]))
    cmn = [min(c[1][i] for c in cubes) for i in range(3)]
    cmx = [max(c[2][i] for c in cubes) for i in range(3)]
    check("java.B2Model.union_bbox_equals_json",
          all(abs(cmn[i] - j["mn"][i]) <= 0.01 for i in range(3))
          and all(abs(cmx[i] - j["mx"][i]) <= 0.01 for i in range(3)),
          "java union min=[%.2f,%.2f,%.2f] max=[%.2f,%.2f,%.2f] json min=%s max=%s" % (
              cmn[0], cmn[1], cmn[2], cmx[0], cmx[1], cmx[2],
              [round(v, 2) for v in j["mn"]], [round(v, 2) for v in j["mx"]]))
    jvol = 0.0
    for p in j["parts"]:
        v = 1.0
        for i in range(3):
            v *= (p["max"][i] - p["min"][i])
        jvol += v
    cvol = sum(c[3] for c in cubes)
    check("java.B2Model.volume_preserved", abs(cvol - jvol) <= 1e-6 * max(1.0, jvol),
          "java cube volume=%.1f units^3 json=%.1f delta=%.3f" % (cvol, jvol, cvol - jvol))
    parts = j["parts"]
    picks = [parts[0], parts[len(parts) // 2], parts[-1]]
    ok, lines = True, []
    for p in picks:
        eps = 1e-3
        sub = [c for c in cubes
               if all(c[1][i] >= p["min"][i] - eps for i in range(3))
               and all(c[2][i] <= p["max"][i] + eps for i in range(3))]
        if not sub:
            ok = False
            lines.append("%s: NO java cube inside" % p["name"])
            continue
        smn = [min(c[1][i] for c in sub) for i in range(3)]
        smx = [max(c[2][i] for c in sub) for i in range(3)]
        pv = 1.0
        for i in range(3):
            pv *= (p["max"][i] - p["min"][i])
        sv = sum(c[3] for c in sub)
        d = max(max(abs(smn[i] - p["min"][i]) for i in range(3)),
                max(abs(smx[i] - p["max"][i]) for i in range(3)))
        ok &= d <= 0.01 and abs(sv - pv) <= 1e-6 * max(1.0, pv)
        lines.append("%s: %d cubes union[%.1f,%.1f,%.1f]-[%.1f,%.1f,%.1f] vol %.1f vs json"
                     "[%.1f,%.1f,%.1f]-[%.1f,%.1f,%.1f] vol %.1f bboxdiff=%.4f voldiff=%.3f" % (
                         p["name"], len(sub), smn[0], smn[1], smn[2], smx[0], smx[1], smx[2],
                         sv, p["min"][0], p["min"][1], p["min"][2],
                         p["max"][0], p["max"][1], p["max"][2], pv, d, sv - pv))
    check("java.B2Model.spotcheck_3_parts", ok, " | ".join(lines))
    gp = os.path.join(ROOT, "src/main/java/io/github/littlebird/entity/B2Geometry.java")
    if os.path.exists(gp):
        m = re.search(r"WING_TIP_UNITS\s*=\s*(-?[0-9.]+)",
                      open(gp, "r", errors="replace").read())
        wt = float(m.group(1)) if m else None
        if wt is not None:
            check("java.B2Model.span_vs_declared_geometry", abs(cmx[0] - wt) <= 1.0,
                  "box model max|x|=%.2f units vs B2Geometry.WING_TIP_UNITS=%.2f "
                  "(delta=%.2f units = %.3f m per side)" % (
                      cmx[0], wt, cmx[0] - wt, (cmx[0] - wt) / j["s"]))





def sec_geom():
    gp = os.path.join(ROOT, "src/main/java/io/github/littlebird/entity/B2Geometry.java")
    if not os.path.exists(gp):
        check("geom.B2Geometry.exists", False, "missing " + gp)
        return
    gsrc = open(gp, "r", errors="replace").read()
    want = {"NOSE_TIP_UNITS": -120.0, "TAIL_TIP_UNITS": 120.0, "WING_TIP_UNITS": 300.87,
            "ROOF_TOP_UNITS": 56.0, "BELLY_BOTTOM_UNITS": 17.1, "SKID_BOTTOM_UNITS": 0.0}
    got = {}
    for k in want:
        m = re.search(k + r"\s*=\s*(-?[0-9.]+)", gsrc)
        got[k] = float(m.group(1)) if m else None
    bad = {k: [got[k], want[k]] for k in want
           if got[k] is None or abs(got[k] - want[k]) > 1e-6}
    check("geom.constants_match_contract", not bad,
          "got=%s expect=%s mismatches=%s sha256=%s" % (
              got, want, bad or "none", sha256(gp)[:16]))
    m = re.search(r"UNITS_PER_BLOCK\s*=\s*([0-9.]+)", gsrc)
    m2 = re.search(r"(?:LENGTH_BLOCKS|AIRFRAME_LENGTH_BLOCKS)\s*=\s*([0-9.]+)", gsrc)
    note("geom.units_per_block", "UNITS_PER_BLOCK=%s LENGTH_BLOCKS=%s" % (
        m.group(1) if m else None, m2.group(1) if m2 else None))


def sec_reg2():
    rel = {"ModCreativeTabs": "src/main/java/io/github/littlebird/registry/ModCreativeTabs.java",
           "JetType": "src/main/java/io/github/littlebird/entity/flight/JetType.java"}
    for tag, p in rel.items():
        fp = os.path.join(ROOT, p)
        if not os.path.exists(fp):
            check("reg.%s" % tag, False, "missing " + p)
            continue
        rsrc = open(fp, "r", errors="replace").read()
        n = len(re.findall(r"(?i)\bb2\b", rsrc))
        check("reg.%s.b2_present" % tag, n >= 1,
              "b2 refs=%d lines=%d sha256=%s" % (n, len(rsrc.splitlines()), sha256(fp)[:16]))
    imp = os.path.join(ROOT, "src/main/resources/assets/battlefield/models/item/b2.json")
    if os.path.exists(imp):
        note("reg.item_model_b2", json.dumps(json.load(open(imp)))[:200])
    else:
        check("reg.item_model_b2", False, "missing " + imp)
    for lang in ("en_us", "zh_cn"):
        lp = os.path.join(ROOT,
                          "src/main/resources/assets/battlefield/lang/%s.json" % lang)
        if os.path.exists(lp):
            ltxt = open(lp, "r", errors="replace").read()
            hits = re.findall(r'"[^"]*b2[^"]*"\s*:\s*"[^"]*"', ltxt)
            note("reg.lang_%s" % lang, "b2 keys=%d %s" % (len(hits), hits[:3]))
        else:
            note("reg.lang_%s" % lang, "missing " + lp)


def sec_reg():
    rel = {"ModEntities": "src/main/java/io/github/littlebird/registry/ModEntities.java",
           "ModItems": "src/main/java/io/github/littlebird/registry/ModItems.java",
           "ClientModEvents": "src/main/java/io/github/littlebird/client/ClientModEvents.java"}
    txt = {k: (open(os.path.join(ROOT, v), "r", errors="replace").read()
               if os.path.exists(os.path.join(ROOT, v)) else "") for k, v in rel.items()}
    ent, it, ce = txt["ModEntities"], txt["ModItems"], txt["ClientModEvents"]
    n = len(re.findall(r'ENTITY_TYPES\.register\("b2"', ent))
    check("reg.ModEntities.b2_once", n == 1,
          'register("b2")=%d B2Entity-import=%s B2Geometry-import=%s sha256=%s' % (
              n, "entity.B2Entity" in ent, "entity.B2Geometry" in ent,
              sha256(os.path.join(ROOT, rel["ModEntities"]))[:16]))
    n2 = len(re.findall(r'ITEMS\.register\("b2_preview"', it))
    check("reg.ModItems.b2_preview_once", n2 == 1,
          'register("b2_preview")=%d sha256=%s' % (
              n2, sha256(os.path.join(ROOT, rel["ModItems"]))[:16]))
    nlayer = len(re.findall(r"B2Model\.B2_LAYER", ce))
    used = re.findall(r"registerEntityRenderer\(ModEntities\.B2\.get\(\)\s*,\s*(\w+)", ce)
    check("reg.ClientModEvents.b2_once", nlayer == 1 and len(used) == 1,
          "B2_LAYER refs=%d renderer=%s B2Renderer=%s B2ObjRenderer=%s sha256=%s" % (
              nlayer, used, "B2Renderer" in ce, "B2ObjRenderer" in ce,
              sha256(os.path.join(ROOT, rel["ClientModEvents"]))[:16]))
    dup = []
    for tag, pat in (("entity", r'ENTITY_TYPES\.register\("([^"]+)"'),
                     ("item", r'ITEMS\.register\("([^"]+)"'),
                     ("block", r'BLOCKS\.register\("([^"]+)"')):
        seen = {}
        for dp, _d, fns in os.walk(os.path.join(ROOT, "src/main/java")):
            for fn in fns:
                if fn.endswith(".java"):
                    fp = os.path.join(dp, fn)
                    for m in re.finditer(pat, open(fp, "r", errors="replace").read()):
                        seen.setdefault(m.group(1), []).append(os.path.relpath(fp, ROOT))
        for k, v in seen.items():
            if len(v) > 1:
                dup.append("%s:%s%s" % (tag, k, v))
    check("reg.no_duplicate_ids", not dup, "duplicate register ids=%s" % (dup or "none"))
    need = ["src/main/resources/assets/battlefield/models/item/b2_preview.json",
            "src/main/resources/assets/battlefield/models/item/b2_preview.obj",
            "src/main/resources/assets/battlefield/models/item/b2_preview.mtl",
            "src/main/resources/assets/battlefield/textures/item/b2_preview.png",
            "src/main/resources/assets/battlefield/textures/entity/b2.png",
            "src/main/java/io/github/littlebird/entity/B2Entity.java",
            "src/main/java/io/github/littlebird/entity/B2Geometry.java",
            "src/main/java/io/github/littlebird/client/B2Model.java",
            "src/main/java/io/github/littlebird/client/B2Renderer.java",
            "src/main/java/io/github/littlebird/client/B2ObjRenderer.java",
            "tools/generate_b2_model.py",
            "src/main/resources/assets/battlefield/models/item/b2.json"]
    missing = [n for n in need if not os.path.exists(os.path.join(ROOT, n))]
    check("reg.required_files", not missing, "missing=%s" % (missing or "none"))
    pj = os.path.join(ROOT, "src/main/resources/assets/battlefield/models/item/b2_preview.json")
    if os.path.exists(pj):
        d = json.load(open(pj))
        check("reg.preview_model_json",
              d.get("loader") == "forge:obj"
              and str(d.get("model", "")).endswith("b2_preview.obj"),
              "loader=%s model=%s flip_v=%s" % (
                  d.get("loader"), d.get("model"), d.get("flip_v")))


def sec_blend():
    dump = "/tmp/b2verify/blend_facts.json"
    if not os.path.exists(dump):
        note("blend.facts", "no dump at " + dump)
        return None
    f = json.load(open(dump))
    note("blend.facts", json.dumps(f))
    return f


def main():
    argv = sys.argv[1:]
    want = set("json,obj,tex,java,reg".split(","))
    if "--section" in argv:
        want = set(argv[argv.index("--section") + 1].split(","))
    j = load_json()
    if "json" in want:
        print("--- 1. tools/data/b2_mc.json ---"); sec_json(j)
    if "obj" in want:
        print("--- 2. b2_preview.obj / .mtl ---"); sec_obj(j)
    if "tex" in want:
        print("--- 3. textures ---"); sec_tex()
    if "java" in want:
        print("--- 4. client/B2Model.java vs b2_mc.json ---"); sec_java(j)
    if "java" in want:
        print("--- 4c. B2Geometry constants ---"); sec_geom()
    if "reg" in want:
        print("--- 4b. registry wiring + required files ---"); sec_reg()
    print("--- 4d. extra registries + item model + lang ---"); sec_reg2()
    print("--- blend cross-check ---"); sec_blend()
    npass = sum(1 for _k, o, _d in RESULTS if o is True)
    nfail = sum(1 for _k, o, _d in RESULTS if o is False)
    print("=== SUMMARY: %d PASS / %d FAIL / %d INFO ===" % (
        npass, nfail, sum(1 for _k, o, _d in RESULTS if o is None)))
    if "--json" in argv:
        os.makedirs("/tmp/b2verify", exist_ok=True)
        json.dump([{"key": k, "ok": o, "detail": d} for (k, o, d) in RESULTS],
                  open("/tmp/b2verify/checks.json", "w"), indent=1)
    return 1 if nfail else 0


if __name__ == "__main__":
    sys.exit(main())
