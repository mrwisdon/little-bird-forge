#!/usr/bin/env python3
"""Pixel-art 16x16 inventory icons for the items whose textures were flat colour squares.

Pure-stdlib writer (zlib), same approach as generate_jet_texture.py, so no imaging library is
needed on the build machine. Each icon is a small silhouette with a dark outline, a mid tone and
one highlight so it reads at inventory scale.
"""
import os
import struct
import zlib

SIZE = 16
CLEAR = (0, 0, 0, 0)
OUT = (26, 30, 34, 255)
DARK = (58, 64, 70, 255)
MID = (96, 106, 114, 255)
LIGHT = (150, 160, 168, 255)
TRACK = (40, 44, 48, 255)
OLIVE = (110, 128, 74, 255)
OLIVE_D = (74, 88, 52, 255)
WHITE = (206, 212, 216, 255)


def blank():
    return [CLEAR] * (SIZE * SIZE)


def rect(px, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < SIZE and 0 <= y < SIZE:
                px[y * SIZE + x] = colour


def disc(px, cx, cy, radius, colour):
    r2 = radius * radius
    for y in range(SIZE):
        for x in range(SIZE):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r2:
                px[y * SIZE + x] = colour


def ring(px, cx, cy, outer, inner, colour):
    for y in range(SIZE):
        for x in range(SIZE):
            d2 = (x - cx) ** 2 + (y - cy) ** 2
            if inner * inner < d2 <= outer * outer:
                px[y * SIZE + x] = colour


def wheel_assembly():
    px = blank()
    disc(px, 8, 8, 7, OUT)
    disc(px, 8, 8, 5, TRACK)
    disc(px, 8, 8, 3, MID)
    disc(px, 8, 8, 1, LIGHT)
    for x, y in ((8, 3), (8, 13), (3, 8), (13, 8)):
        disc(px, x, y, 1, LIGHT)
    return px


def track_assembly():
    px = blank()
    rect(px, 1, 5, 14, 11, OUT)
    rect(px, 2, 6, 13, 10, TRACK)
    for cx in (4, 7, 10, 13):
        disc(px, cx, 8, 2, MID)
        disc(px, cx, 8, 1, LIGHT)
    return px


def turret_ring():
    px = blank()
    ring(px, 8, 8, 7, 5, OUT)
    ring(px, 8, 8, 5, 3, MID)
    ring(px, 8, 8, 3, 2, DARK)
    for x, y in ((8, 1), (8, 15), (1, 8), (15, 8), (3, 3), (13, 3), (3, 13), (13, 13)):
        disc(px, x, y, 1, LIGHT)
    return px


def cannon_barrel():
    px = blank()
    rect(px, 1, 6, 11, 9, OUT)
    rect(px, 2, 7, 10, 8, LIGHT)
    rect(px, 3, 6, 6, 9, MID)
    rect(px, 11, 5, 14, 10, OUT)
    rect(px, 12, 6, 13, 9, DARK)
    rect(px, 1, 7, 2, 9, WHITE)
    return px


def armor_plate():
    px = blank()
    rect(px, 2, 2, 13, 13, OUT)
    rect(px, 3, 3, 12, 12, MID)
    rect(px, 3, 3, 11, 4, LIGHT)
    rect(px, 3, 3, 4, 11, LIGHT)
    rect(px, 4, 11, 11, 12, DARK)
    for x, y in ((5, 5), (10, 5), (5, 10), (10, 10)):
        disc(px, x, y, 1, WHITE)
    return px


def tank_hull():
    px = blank()
    rect(px, 1, 7, 14, 12, OUT)
    rect(px, 2, 8, 13, 11, OLIVE)
    rect(px, 2, 8, 4, 10, OLIVE_D)
    rect(px, 10, 8, 13, 9, LIGHT)
    rect(px, 1, 12, 14, 13, TRACK)
    for cx in (3, 6, 9, 12):
        disc(px, cx, 12, 1, DARK)
    return px


def ifv_hull():
    px = blank()
    rect(px, 1, 5, 14, 11, OUT)
    rect(px, 2, 6, 13, 10, OLIVE)
    rect(px, 2, 6, 4, 9, OLIVE_D)
    rect(px, 10, 6, 13, 7, LIGHT)
    for cx in (4, 7, 10, 13):
        disc(px, cx, 12, 2, OUT)
        disc(px, cx, 12, 1, DARK)
    return px


def main_battle_tank():
    px = blank()
    rect(px, 1, 2, 4, 13, TRACK)
    rect(px, 11, 2, 14, 13, TRACK)
    for y in (4, 7, 10):
        rect(px, 1, y, 4, y + 1, DARK)
        rect(px, 11, y, 14, y + 1, DARK)
    rect(px, 4, 2, 11, 13, OUT)
    rect(px, 5, 3, 10, 12, OLIVE)
    rect(px, 5, 3, 10, 4, OLIVE_D)
    disc(px, 8, 8, 3, DARK)
    disc(px, 8, 8, 2, MID)
    disc(px, 8, 8, 1, LIGHT)
    rect(px, 7, 0, 8, 6, OUT)
    rect(px, 7, 1, 8, 5, MID)
    return px


def infantry_fighting_vehicle():
    px = blank()
    rect(px, 3, 2, 12, 13, OUT)
    rect(px, 4, 3, 11, 12, OLIVE)
    rect(px, 4, 3, 11, 4, OLIVE_D)
    for cy in (4, 7, 10):
        rect(px, 2, cy, 3, cy + 1, TRACK)
        rect(px, 12, cy, 13, cy + 1, TRACK)
    disc(px, 8, 6, 2, DARK)
    disc(px, 8, 6, 1, MID)
    rect(px, 8, 1, 8, 5, DARK)
    return px


ICONS = {
    "armor_plate": armor_plate,
    "cannon_barrel": cannon_barrel,
    "ifv_hull": ifv_hull,
    "infantry_fighting_vehicle": infantry_fighting_vehicle,
    "main_battle_tank": main_battle_tank,
    "tank_hull": tank_hull,
    "track_assembly": track_assembly,
    "turret_ring": turret_ring,
    "wheel_assembly": wheel_assembly,
}


def write_png(path, pixels, size=SIZE):
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            pixel = pixels[y * size + x]
            raw.extend(pixel if len(pixel) == 4 else (pixel[0], pixel[1], pixel[2], 255))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    data = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(data)
    print(path, len(data))


if __name__ == "__main__":
    root = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                        "src", "main", "resources", "assets", "battlefield", "textures", "item")
    for name, builder in ICONS.items():
        write_png(os.path.join(root, name + ".png"), builder())
