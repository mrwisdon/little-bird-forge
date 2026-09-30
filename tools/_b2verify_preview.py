#!/usr/bin/env python3
"""mc-verify: box-model preview (side + top) straight from tools/data/b2_mc.json.

Standard library only. Writes docs/plan/b2-mc-verify-preview.png.
"""
import json
import os
import struct
import sys
import zlib

ROOT = "/root/projects/little-bird-forge"
UPB = 16.0
PALETTE = {"body": (150, 156, 165), "cockpit": (120, 190, 235), "engine": (185, 140, 110),
           "gear": (225, 228, 232), "detail": (205, 175, 80), "bay": (250, 250, 245),
           "wpn": (140, 140, 150), "ordnance": (110, 120, 100), "light": (240, 90, 90)}


def write_png(path, width, height, pixels):
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for (r, g, b) in pixels[y]:
            raw.extend((r, g, b, 255))
    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    with open(path, "wb") as fh:
        fh.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
                 + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


def colour(group, shade):
    base = PALETTE.get(group, (170, 170, 170))
    return tuple(max(0, min(255, int(c * shade))) for c in base)


def draw(px, boxes, ha, va, k, frame, ox=30, oy=12):
    for (_n, group, mn, mx) in sorted(boxes, key=lambda b: -b[2][va]):
        d = 0.55 + 0.45 * ((mn[va] - frame[va][0]) / max(1e-6, frame[va][1] - frame[va][0]))
        x0 = int(ox + (frame[ha][1] - mx[ha]) * k)
        x1 = int(ox + (frame[ha][1] - mn[ha]) * k)
        y0 = int(oy + (frame[va][1] - mx[va]) * k)
        y1 = int(oy + (frame[va][1] - mn[va]) * k)
        col = colour(group, d)
        for y in range(max(0, y0), min(len(px), y1)):
            for x in range(max(0, x0), min(len(px[0]), x1)):
                px[y][x] = col


def main():
    out = os.path.join(ROOT, "docs/plan/b2-mc-verify-preview.png")
    if "--out" in sys.argv:
        out = sys.argv[sys.argv.index("--out") + 1]
    spec = json.load(open(os.path.join(ROOT, "tools/data/b2_mc.json")))
    parts = spec["parts"]
    boxes = [(p["name"], p.get("group", "?"), p["min"], p["max"]) for p in parts]
    mn = [min(p["min"][i] for p in parts) for i in range(3)]
    mx = [max(p["max"][i] for p in parts) for i in range(3)]
    W = 880
    sk = 800.0 / (mx[2] - mn[2])
    side = [[(20, 22, 28) for _ in range(W)] for _ in range(int((mx[1] - mn[1]) * sk) + 26)]
    draw(side, boxes, 2, 1, sk, {2: (mn[2], mx[2]), 1: (mn[1], mx[1])})
    tk = 800.0 / (mx[0] - mn[0])
    top = [[(20, 22, 28) for _ in range(W)] for _ in range(int((mx[0] - mn[0]) * tk) + 26)]
    draw(top, boxes, 2, 0, tk, {2: (mn[2], mx[2]), 0: (mn[0], mx[0])})
    H = len(side) + len(top) + 10
    canvas = [[(12, 13, 16) for _ in range(W)] for _ in range(H)]
    for y in range(len(side)):
        canvas[y] = side[y]
    for y in range(len(top)):
        canvas[len(side) + 10 + y] = top[y]
    for i in range(16):
        gx = 30 + int(i * UPB * sk)
        if gx < W:
            for y in range(12, len(side) - 12, 3):
                canvas[y][gx] = (64, 68, 78)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    write_png(out, W, H, canvas)
    print("PREVIEW %s %dx%d parts=%d side_scale=%.3f top_scale=%.3f" % (
        out, W, H, len(parts), sk, tk))
    print("side: horizontal nose(-Z)->tail(+Z) left->right, vertical +Y up, grid = 1 block")
    print("top : horizontal nose left->right, vertical +X up (right wing at top)")
    print("bbox units min=%s max=%s" % (mn, mx))


if __name__ == "__main__":
    main()
