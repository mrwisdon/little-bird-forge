#!/usr/bin/env python3
"""mc-verify: independent UV-cell -> texture-colour audit (Lead's highest-risk item).

Rule (Forge OBJ item model): flip_v=true means the sampled image row = (1-v)*H.
For every OBJ vt we sample the 5x5 median around (u*W, row) and name the cell from the
frozen contract sheet. Faces are area-weighted from their vt, so a flipped or mis-pinned
sheet shows up as the huge skin areas landing on a bright/gold cell.
"""
import json
import math
import os
import struct
import sys
import zlib

ROOT = "/root/projects/little-bird-forge"
CONTRACT = [["skin", "panel", "glass", "intake"],
            ["exhaust", "metal", "gear_white", "bay_white"],
            ["tire", "ordnance", "light_r", "light_g"],
            ["light_w", "gold", "dark", "spare"]]
RESULTS = []


def check(key, ok, detail):
    RESULTS.append((key, bool(ok), detail))
    print(("PASS  " if ok else "FAIL  ") + key + " :: " + detail, flush=True)


def note(key, detail):
    RESULTS.append((key, None, detail))
    print("INFO  " + key + " :: " + detail, flush=True)


def pred(name, c):
    r, g, b = c[:3]
    mx, mn = max(c[:3]), min(c[:3])
    if name == "light_r":
        return r > 110 and r > g + 50 and r > b + 50
    if name == "light_g":
        return g > 90 and g > r + 35 and g > b + 35
    if name in ("light_w", "gear_white", "bay_white"):
        return mx > 170 and mx - mn < 70
    if name == "gold":
        return r > 130 and g > 100 and b < 115
    if name == "ordnance":
        return g >= r and b < r
    if name == "metal":
        return 110 <= mx <= 215 and mx - mn < 70
    if name == "exhaust":
        return mx < 135
    return mx < 125


def png_rows(path):
    data = open(path, "rb").read()
    pos, idat, w = 8, b"", None
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        ch = data[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if tag == b"IHDR":
            w, h, bd, ct = struct.unpack(">IIBB", ch[:10])
        elif tag == b"IDAT":
            idat += ch
        elif tag == b"IEND":
            break
    raw = zlib.decompress(idat)
    nch = {0: 1, 2: 3, 4: 2, 6: 4}[ct]
    stride = w * nch
    rows, prev, p = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        if f == 1:
            for i in range(nch, stride):
                line[i] = (line[i] + line[i - nch]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                b = prev[i]
                c = prev[i - nch] if i >= nch else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        rows.append([tuple(line[x * nch:x * nch + nch]) for x in range(w)])
        prev = line
    return w, h, rows


def median(rows, w, h, cx, cy, rad=2):
    rs, gs, bs = [], [], []
    for y in range(max(0, cy - rad), min(h, cy + rad + 1)):
        for x in range(max(0, cx - rad), min(w, cx + rad + 1)):
            px = rows[y][x]
            rs.append(px[0]); gs.append(px[1] if len(px) > 1 else px[0])
            bs.append(px[2] if len(px) > 2 else px[0])
    rs.sort(); gs.sort(); bs.sort()
    return (rs[len(rs) // 2], gs[len(gs) // 2], bs[len(bs) // 2])


def main():
    obj = os.path.join(ROOT, "src/main/resources/assets/battlefield/models/item/b2_preview.obj")
    png = os.path.join(ROOT, "src/main/resources/assets/battlefield/textures/item/b2_preview.png")
    pj = os.path.join(ROOT, "src/main/resources/assets/battlefield/models/item/b2_preview.json")
    for p in (obj, png, pj):
        if not os.path.exists(p):
            check("uvcolor.file:" + os.path.basename(p), False, "missing " + p)
            return
    flip = bool(json.load(open(pj)).get("flip_v", False))
    W, H, rows = png_rows(png)
    check("uvcolor.png_size", W == H == 256, "%dx%d flip_v=%s" % (W, H, flip))
    # 1. cell colours + which row mapping fits the contract names
    cell = {}
    for r in range(4):
        for c in range(4):
            cell[(c, r)] = median(rows, W, H, c * 64 + 32, r * 64 + 32)
    bad_mirror = [(CONTRACT[3 - r][c], cell[(c, r)]) for r in range(4) for c in range(4)
                  if not pred(CONTRACT[3 - r][c], cell[(c, r)])]
    bad_ident = [(CONTRACT[r][c], cell[(c, r)]) for r in range(4) for c in range(4)
                 if not pred(CONTRACT[r][c], cell[(c, r)])]
    for tag, bad in (("mirror(image row = contract row 3-r)", bad_mirror),
                     ("identity(image row = contract row)", bad_ident)):
        note("uvcolor.mapping." + tag.split("(")[0],
             "conformance=%d/16 mismatches=%s" % (16 - len(bad), bad))
    ok_mirror = len(bad_mirror) == 0
    ok_ident = len(bad_ident) == 0
    check("uvcolor.mapping_unique", ok_mirror != ok_ident,
          "mirror_ok=%s identity_ok=%s (a unique mapping = the sheet's row order is proven "
          "from the 16 cell colours)" % (ok_mirror, ok_ident))
    mirrored = ok_mirror
    name_of = (lambda c, r: CONTRACT[3 - r][c]) if mirrored else (lambda c, r: CONTRACT[r][c])
    # 2. per-vt sample exactly as the item model will sample it
    vts, verts, faces = [], [], []
    for line in open(obj, "r", errors="replace").read().splitlines():
        t = line.split()
        if not t:
            continue
        if t[0] == "v":
            verts.append(tuple(float(x) for x in t[1:4]))
        elif t[0] == "vt":
            vts.append(tuple(float(x) for x in t[1:3]))
        elif t[0] == "f":
            idx = [int(p.split("/")[1]) - 1 for p in t[1:]]
            faces.append([(int(p.split("/")[0]) - 1, int(p.split("/")[1]) - 1) for p in t[1:]])
    note("uvcolor.obj", "v=%d vt=%d faces=%d" % (len(verts), len(vts), len(faces)))
    vt_tbl, disagree = [], 0
    for i, (u, v) in enumerate(vts):
        row = (1.0 - v) if flip else v
        cx, cy = int(round(u * W)), int(round(row * H))
        col_c = min(3, int(u * 4.0) % 4)
        row_c = min(3, int(row * 4.0) % 4)
        c = median(rows, W, H, max(0, min(W - 1, cx)), max(0, min(H - 1, cy)))
        nm = name_of(col_c, row_c)
        vt_tbl.append((i, u, v, col_c, row_c, nm, c, pred(nm, c)))
        print("      vt%-2d u=%.4f v=%.4f -> image cell(col=%d,row=%d) %-11s #%02X%02X%02X "
              "semantic=%s" % (i, u, v, col_c, row_c, nm, c[0], c[1], c[2],
                               "ok" if pred(nm, c) else "MISMATCH"), flush=True)
    bad_vt = [t for t in vt_tbl if not t[7]]
    check("uvcolor.vt_samples_match_material", not bad_vt,
          "vt=%d sampled, %d semantic mismatches %s" % (
              len(vt_tbl), len(bad_vt),
              [(t[0], t[5], "#%02X%02X%02X" % t[6][:3]) for t in bad_vt]))
    # 3. area-weighted: does the geometry land on the skin cell?
    area = {}
    face_bad = 0
    for f in faces:
        if len({vt for (_v, vt) in f}) != 1:
            disagree += 1
        p0, p1, p2 = (verts[f[0][0]], verts[f[1][0]], verts[f[2][0]])
        ax = [(p1[i] - p0[i]) for i in range(3)]
        bx = [(p2[i] - p0[i]) for i in range(3)]
        cr = (ax[1] * bx[2] - ax[2] * bx[1], ax[2] * bx[0] - ax[0] * bx[2],
              ax[0] * bx[1] - ax[1] * bx[0])
        tri = 0.5 * math.sqrt(sum(c * c for c in cr))
        vi = f[0][1]
        u, v = vts[vi]
        row = (1.0 - v) if flip else v
        key = (min(3, int(u * 4.0) % 4), min(3, int(row * 4.0) % 4))
        area[key] = area.get(key, 0.0) + tri
        if not pred(name_of(key[0], key[1]), cell[key]):
            face_bad += tri
    tot = sum(area.values())
    top = sorted(area.items(), key=lambda kv: -kv[1])
    note("uvcolor.area_by_cell", "; ".join(
        "%s %.1f%% #%02X%02X%02X" % (name_of(k[0], k[1]), 100 * v / tot,
                                     cell[k][0], cell[k][1], cell[k][2]) for k, v in top))
    check("uvcolor.faces_agree_on_cell", disagree == 0,
          "triangles whose 3 corners disagree on the cell=%d/%d" % (disagree, len(faces)))
    dark = sum(v for k, v in area.items() if max(cell[k][:3]) < 125)
    check("uvcolor.dominant_area_is_dark_skin", dark / tot > 0.5,
          "area on dark cells=%.1f%%  worst-case wrong-cell area=%.2f m^2 (%.2f%%)" % (
          100 * dark / tot, face_bad, 100 * face_bad / tot))
    check("uvcolor.all_cells_semantic", not bad_mirror if mirrored else not bad_ident,
          "selected mapping=%s ; 16/16 cells match their contract material" % (
              "mirror" if mirrored else "identity"))
    nf = sum(1 for _k, o, _d in RESULTS if o is False)
    print("=== UVCOLOR SUMMARY: %d PASS / %d FAIL ===" % (
        sum(1 for _k, o, _d in RESULTS if o is True), nf))
    return 1 if nf else 0


if __name__ == "__main__":
    sys.exit(main())
