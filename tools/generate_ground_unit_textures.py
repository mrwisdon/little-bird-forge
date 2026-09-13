#!/usr/bin/env python3
"""Minimal flat-shade palette for the ground vehicle models.

The two ground models sample this 512x512 sheet by zone; it is deliberately a flat palette, not an
art pass (the reviewer chose "minimal UV + colour" so the structure stays readable in game).
Zones must stay in step with the texOffs calls in MainBattleTankModel / InfantryFightingVehicleModel.
"""
import os
import struct
import zlib

SIZE = 512

# name -> (x, y, width, height, r, g, b)
ZONES = {
    "olive":        (0, 0, 320, 128, 75, 83, 48),
    "olive_dark":   (0, 128, 320, 64, 58, 64, 36),
    "steel":        (0, 192, 192, 64, 59, 62, 67),
    "track":        (320, 0, 192, 128, 35, 37, 42),
    "gunmetal":     (0, 256, 320, 128, 91, 97, 105),
    "sleeve_tan":   (320, 128, 128, 64, 138, 127, 92),
    "light_yellow": (448, 128, 64, 64, 217, 199, 107),
    "glass":        (320, 192, 64, 64, 43, 55, 66),
    "discharger":   (384, 192, 64, 64, 90, 59, 51),
    "marking":      (448, 192, 64, 64, 201, 201, 201),
}


def build_pixels(base=(60, 66, 40)):
    pixels = [base] * (SIZE * SIZE)
    for (x, y, w, h, r, g, b) in ZONES.values():
        colour = (r, g, b)
        for yy in range(y, min(y + h, SIZE)):
            row = yy * SIZE
            for xx in range(x, min(x + w, SIZE)):
                pixels[row + xx] = colour
    return pixels


def write_png(path, pixels, size=SIZE):
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            r, g, b = pixels[y * size + x]
            raw.extend((r, g, b, 255))
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
                        "src", "main", "resources", "assets", "battlefield", "textures", "entity")
    pixels = build_pixels()
    write_png(os.path.join(root, "main_battle_tank.png"), pixels)
    write_png(os.path.join(root, "infantry_fighting_vehicle.png"), pixels)
