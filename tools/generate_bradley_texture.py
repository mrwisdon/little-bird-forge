#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Paint the Bradley's entity texture from tools/data/bradley_uv.json.

Zero external dependencies (pure stdlib PNG writer), matching the pack's other procedural art.
Each UV zone gets its material's base colour plus a few panel lines, so a box face never reads
as a flat wash at Minecraft's block scale.
"""
import json
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UV = os.path.join(ROOT, "tools", "data", "bradley_uv.json")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "battlefield",
                   "textures", "entity", "bradley.png")

# zone -> (base rgb, line rgb, lines per axis)
PALETTE = {
    "hull":    ((158, 133, 84), (132, 110, 68), 6),
    "skirt":   ((148, 124, 78), (120, 100, 62), 4),
    "running": ((138, 122, 88), (104, 92, 66), 5),
    "track":   ((74, 72, 70), (52, 51, 50), 8),
    "gun":     ((62, 62, 66), (42, 42, 46), 4),
    "glass":   ((26, 38, 42), (14, 22, 26), 2),
    "canvas":  ((74, 70, 52), (56, 52, 38), 3),
    "dark":    ((40, 40, 42), (26, 26, 28), 4),
    "tow":     ((142, 122, 74), (112, 96, 58), 3),
}


def write_png(path, width, height, pixels):
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for pixel in row:
            raw += bytes(pixel)
    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "wb").write(png)


def main():
    spec = json.load(open(UV))
    size = spec["texture_size"]
    # DIAGNOSTIC: 未绘制区域用洋红，任何 UV 漏绘在游戏里一眼可见。交付前改回深灰。
    px = [[(255, 0, 255, 255) for _ in range(size)] for _ in range(size)]
    for cat, (x0, y0) in spec["zones"].items():
        base, line, lines = PALETTE.get(cat, ((150, 126, 80), (120, 100, 62), 4))
        w = int(spec["need"][cat][0]) + 1
        h = int(spec["need"][cat][1]) + 1
        for y in range(y0, min(size, y0 + h)):
            for x in range(x0, min(size, x0 + w)):
                noise = ((x * 7 + y * 13) % 9) - 4
                px[y][x] = (max(0, min(255, base[0] + noise)),
                            max(0, min(255, base[1] + noise)),
                            max(0, min(255, base[2] + noise)), 255)
        for i in range(1, lines + 1):
            yy = y0 + (h * i) // (lines + 1)
            if yy < size:
                for x in range(x0, min(size, x0 + w)):
                    px[yy][x] = (line[0], line[1], line[2], 255)
    write_png(OUT, size, size, px)
    print("wrote %s (%dx%d) zones=%d" % (OUT, size, size, len(spec["zones"])))


main()
