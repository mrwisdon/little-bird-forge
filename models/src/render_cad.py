#!/usr/bin/env python3
"""Render a shaded axonometric preview of the CAD-flavoured tank mesh, and export it as STL.

Zero external dependencies. The shading is a plain Lambert model, the hidden-surface removal is a
painter's algorithm with back-face culling, which is enough to show what route B geometry (real
cylinders, chamfers) looks like before installing a CAD kernel.

Usage:  python3 models/src/render_cad.py [--out models/build]
"""
import argparse
import math
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cad_model  # noqa: E402


def write_png(path, width, height, pixels):
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            r, g, b = pixels[y][x]
            raw.extend((r, g, b, 255))
    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))
    with open(path, "wb") as fh:
        fh.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
                 + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


def write_stl(path, tris):
    with open(path, "wb") as fh:
        fh.write(b"CAD-flavoured tank mesh - models/src/render_cad.py".ljust(80, b"\0"))
        fh.write(struct.pack("<I", len(tris)))
        for (tri, n) in tris:
            fh.write(struct.pack("<12fH", n[0], n[1], n[2],
                                 tri[0][0], tri[0][1], tri[0][2],
                                 tri[1][0], tri[1][1], tri[1][2],
                                 tri[2][0], tri[2][1], tri[2][2], 0))


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def norm(v):
    n = math.sqrt(dot(v, v)) or 1.0
    return (v[0] / n, v[1] / n, v[2] / n)


def render(tris, width=1000, height=620, azimuth=35.0, elevation=22.0):
    az, el = math.radians(azimuth), math.radians(elevation)
    eye = (math.cos(el) * math.sin(az), math.sin(el), math.cos(el) * math.cos(az))
    right = norm(cross((0.0, 1.0, 0.0), eye))
    up = cross(eye, right)
    light = norm((0.45, 0.85, 0.30))
    base = (0.40, 0.45, 0.26)

    # frame the mesh
    pts = [p for (t, _) in tris for p in t]
    xs = [dot(p, right) for p in pts]
    ys = [dot(p, up) for p in pts]
    scale = min((width - 80) / (max(xs) - min(xs)), (height - 120) / (max(ys) - min(ys)))
    cx = (max(xs) + min(xs)) / 2.0
    cy = (max(ys) + min(ys)) / 2.0
    horizon = int(height * 0.68)

    sky = (158, 184, 217)
    ground = (140, 140, 135)
    pixels = [[sky if y < horizon else ground for _ in range(width)] for y in range(height)]

    def project(p):
        return (width / 2.0 + (dot(p, right) - cx) * scale,
                height / 2.0 + 40 - (dot(p, up) - cy) * scale)

    drawn = 0
    for (tri, n) in sorted(tris, key=lambda tn: sum(dot(p, eye) for p in tn[0]) / 3.0):
        if dot(n, eye) <= 0.0:
            continue
        shade = 0.28 + 0.72 * max(0.0, dot(n, light))
        colour = (int(255 * min(1.0, base[0] * shade + 0.03)),
                  int(255 * min(1.0, base[1] * shade + 0.03)),
                  int(255 * min(1.0, base[2] * shade + 0.03)))
        p = [project(v) for v in tri]
        minx = max(0, int(min(q[0] for q in p)))
        maxx = min(width - 1, int(max(q[0] for q in p)) + 1)
        miny = max(0, int(min(q[1] for q in p)))
        maxy = min(height - 1, int(max(q[1] for q in p)) + 1)
        (x0, y0), (x1, y1), (x2, y2) = p
        area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if abs(area) < 1e-9:
            continue
        for y in range(miny, maxy + 1):
            for x in range(minx, maxx + 1):
                w0 = ((x1 - x0) * (y - y0) - (x - x0) * (y1 - y0)) / area
                w1 = ((x - x0) * (y2 - y0) - (x2 - x0) * (y - y0)) / area
                if w0 >= 0.0 and w1 >= 0.0 and (w0 + w1) <= 1.0:
                    pixels[y][x] = colour
        drawn += 1
    return pixels, width, height, drawn


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=os.path.join(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))), "build"))
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    tris = cad_model.main_battle_tank()
    stl = os.path.join(args.out, "cad_main_battle_tank.stl")
    write_stl(stl, tris)
    pixels, w, h, drawn = render(tris)
    png = os.path.join(args.out, "cad_main_battle_tank_preview.png")
    write_png(png, w, h, pixels)
    print(f"triangles={len(tris)} drawn={drawn} -> {stl}")
    print("preview ->", png, f"{w}x{h}")


if __name__ == "__main__":
    main()
