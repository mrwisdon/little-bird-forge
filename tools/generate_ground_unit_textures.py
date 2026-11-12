#!/usr/bin/env python3
"""Painted 512x512 sheets for the two ground vehicles.

The ten zone rectangles are unchanged (they are what the texOffs calls in MainBattleTankModel and
InfantryFightingVehicleModel sample), but each zone now carries panel lines, rivets, camouflage,
track wear, glass reflection and stencils. The tank stays NATO olive; the IFV gets a grey-blue
urban scheme so the two read apart at a glance. Deterministic, pure stdlib.
"""
import os
import struct
import zlib

SIZE = 512

# name -> (x, y, width, height). The IFV keeps the original shared layout so its sheet is
# unchanged; the tank gets its own layout sized for the real-scale Blender model (a hull box
# needs 2*(w+d) across, which no longer fits the old 320-wide olive zone).
IFV_ZONES = {
    "olive":        (0, 0, 320, 128),
    "olive_dark":   (0, 128, 320, 64),
    "steel":        (0, 192, 192, 64),
    "track":        (320, 0, 192, 128),
    "gunmetal":     (0, 256, 320, 128),
    "sleeve_tan":   (320, 128, 128, 64),
    "light_yellow": (448, 128, 64, 64),
    "glass":        (320, 192, 64, 64),
    "discharger":   (384, 192, 64, 64),
    "marking":      (448, 192, 64, 64),
}

TANK_ZONES = {
    "olive":        (0, 0, 512, 160),
    "olive_dark":   (0, 160, 128, 64),
    "roller":       (256, 160, 256, 96),
    "track":        (0, 224, 256, 96),
    "steel":        (256, 256, 128, 64),
    "gunmetal":     (0, 320, 256, 96),
    "sleeve_tan":   (256, 320, 128, 64),
    "glass":        (384, 320, 64, 64),
    "light_yellow": (256, 384, 64, 64),
    "discharger":   (320, 384, 64, 64),
    "marking":      (384, 384, 64, 64),
    "hub":          (0, 416, 128, 64),
    "skirt":        (448, 320, 64, 128),
}


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Rng:
    def __init__(self, seed):
        self.state = seed & 0xFFFFFFFF

    def next(self):
        self.state = (self.state * 1103515245 + 12345) & 0x7FFFFFFF
        return self.state

    def below(self, n):
        return self.next() % n


def rect(px, x0, y0, w, h, colour):
    for y in range(max(0, y0), min(SIZE, y0 + h)):
        row = px[y]
        for x in range(max(0, x0), min(SIZE, x0 + w)):
            row[x] = colour


def paint_hull(px, r, c, rng):
    x0, y0, w, h = r
    for yy in range(h):
        t = yy / max(1, h - 1)
        row = px[y0 + yy]
        for xx in range(w):
            row[x0 + xx] = mix(c["light"], c["base"], min(1.0, t * 1.5))
    for _ in range(60):
        cx, cy = x0 + rng.below(w), y0 + rng.below(h)
        colour = c["dark"] if rng.below(2) == 0 else c["camo"]
        radius = 14 + rng.below(30)
        for y in range(max(y0, cy - radius), min(y0 + h, cy + radius)):
            row = px[y]
            for x in range(max(x0, cx - radius), min(x0 + w, cx + radius)):
                if (x - cx) ** 2 + (y - cy) ** 2 <= radius * radius:
                    row[x] = mix(row[x], colour, 0.5)
    for yy in range(0, h, 24):
        for x in range(x0, x0 + w):
            px[y0 + yy][x] = mix(px[y0 + yy][x], c["line"], 0.6)
        for x in range(x0 + 8, x0 + w, 28):
            rect(px, x, y0 + yy - 1, 2, 2, c["rivet"])
    for xx in range(0, w, 48):
        for y in range(y0, y0 + h):
            px[y][x0 + xx] = mix(px[y][x0 + xx], c["line"], 0.45)
    for _ in range(50):
        sx, sy = x0 + rng.below(w), y0 + rng.below(max(1, h - 16))
        for i in range(5 + rng.below(16)):
            if sy + i < y0 + h:
                px[sy + i][sx] = mix(px[sy + i][sx], c["line"], 0.35)


def paint_dark(px, r, c, rng):
    x0, y0, w, h = r
    rect(px, x0, y0, w, h, c["dark"])
    for yy in range(0, h, 18):
        rect(px, x0, y0 + yy, w, 1, c["line"])
    for _ in range(30):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 8, 2, c["strap"])


def paint_steel(px, r, c, rng):
    x0, y0, w, h = r
    for yy in range(h):
        for xx in range(w):
            base = 60 + ((xx // 5) % 3) * 5
            px[y0 + yy][x0 + xx] = (base, base + 2, base + 3)
    for _ in range(60):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 6, 1, (110, 112, 116))
    for x in range(x0 + 12, x0 + w, 32):
        rect(px, x, y0 + 12, 2, 2, (140, 142, 146))


def paint_track(px, r, c, rng):
    x0, y0, w, h = r
    rect(px, x0, y0, w, h, c["track"])
    for yy in range(0, h, 10):
        rect(px, x0, y0 + yy, w, 2, (24, 25, 28))
        for x in range(x0 + 4, x0 + w, 18):
            rect(px, x, y0 + yy + 3, 8, 4, mix(c["track"], (120, 104, 84), 0.35))
    for _ in range(40):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 4, 2, (86, 74, 58))


def paint_gunmetal(px, r, c, rng):
    x0, y0, w, h = r
    for yy in range(h):
        for xx in range(w):
            base = 78 + ((yy // 6) % 2) * 8
            px[y0 + yy][x0 + xx] = (base, base + 2, base + 4)
    for yy in range(0, h, 22):
        rect(px, x0, y0 + yy, w, 2, (46, 48, 52))
    for _ in range(50):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 10, 1, (128, 130, 134))


def paint_roller(px, r, c, rng):
    """Road-wheel rubber: near-black with tread grooves and a few worn highlights."""
    x0, y0, w, h = r
    rect(px, x0, y0, w, h, (29, 30, 33))
    for yy in range(0, h, 12):
        rect(px, x0, y0 + yy, w, 3, (17, 17, 19))
    for _ in range(40):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 6, 2, (62, 60, 55))
    for x in range(x0 + 20, x0 + w, 40):
        rect(px, x, y0, 2, h, (12, 12, 14))


def paint_hub(px, r, c, rng):
    """Wheel hub: bare steel disc with a ring of bolts, so the 7 road wheels read apart."""
    x0, y0, w, h = r
    rect(px, x0, y0, w, h, (64, 66, 70))
    cx, cy = x0 + w // 2, y0 + h // 2
    for yy in range(h):
        for xx in range(w):
            d = ((xx - w / 2.0) ** 2 + (yy - h / 2.0) ** 2) ** 0.5
            if d > min(w, h) * 0.42:
                px[y0 + yy][x0 + xx] = (42, 44, 47)
    for k in range(8):
        a2 = 2 * 3.14159265 * k / 8
        bx = int(cx + 0.30 * w * __import__("math").cos(a2))
        by = int(cy + 0.30 * h * __import__("math").sin(a2))
        rect(px, bx - 1, by - 1, 3, 3, (132, 134, 138))
    rect(px, cx - 3, cy - 3, 6, 6, (30, 31, 34))


def paint_skirt(px, r, c, rng):
    """Side-skirt panel: TUSK-style ERA block grid with panel seams and bolts."""
    x0, y0, w, h = r
    for yy in range(h):
        t = yy / max(1, h - 1)
        for xx in range(w):
            px[y0 + yy][x0 + xx] = mix((98, 106, 66), (58, 64, 40), t)
    for by in range(0, h, 26):
        for bx in range(0, w, 26):
            rect(px, x0 + bx + 3, y0 + by + 3, 20, 20, (80, 88, 52))
            rect(px, x0 + bx + 3, y0 + by + 3, 20, 2, (44, 50, 28))
            rect(px, x0 + bx + 3, y0 + by + 21, 20, 2, (44, 50, 28))
            rect(px, x0 + bx + 4, y0 + by + 5, 2, 2, (132, 138, 96))
            rect(px, x0 + bx + 20, y0 + by + 5, 2, 2, (132, 138, 96))
    for yy in range(0, h, 13):
        rect(px, x0, y0 + yy, w, 1, (38, 44, 24))


def paint_tan(px, r, c):
    x0, y0, w, h = r
    rect(px, x0, y0, w, h, c["tan"])
    for yy in range(0, h, 12):
        rect(px, x0, y0 + yy, w, 2, mix(c["tan"], (40, 36, 26), 0.4))


def paint_light(px, r, c):
    x0, y0, w, h = r
    cx, cy = x0 + w // 2, y0 + h // 2
    for yy in range(h):
        for xx in range(w):
            d = ((xx - w // 2) ** 2 + (yy - h // 2) ** 2) ** 0.5
            t = min(1.0, d / (w / 2.0))
            px[y0 + yy][x0 + xx] = mix(c["lens"], c["line"], t)


def paint_glass(px, r, c):
    x0, y0, w, h = r
    for yy in range(h):
        for xx in range(w):
            t = (xx / max(1, w - 1)) * 0.4 + ((h - 1 - yy) / max(1, h - 1)) * 0.6
            px[y0 + yy][x0 + xx] = mix(c["glass_dark"], c["glass_light"], t)
    for yy in range(0, h, 16):
        rect(px, x0, y0 + yy, w, 2, (30, 34, 38))


def paint_discharger(px, r, c):
    x0, y0, w, h = r
    rect(px, x0, y0, w, h, c["discharger"])
    for row in range(2):
        for col in range(2):
            cx, cy = x0 + 16 + col * 32, y0 + 16 + row * 32
            for yy in range(-9, 10):
                for xx in range(-9, 10):
                    d = xx * xx + yy * yy
                    if d <= 81:
                        px[cy + yy][cx + xx] = (46, 30, 26) if d > 36 else (120, 60, 48)


def paint_marking(px, r, c):
    x0, y0, w, h = r
    rect(px, x0, y0, w, h, c["mark"])
    for i in range(5):
        rect(px, x0 + 8 + i * 10, y0 + 18, 5, 28, c["line"])
    rect(px, x0 + 6, y0 + 12, 52, 3, c["line"])
    rect(px, x0 + 6, y0 + 49, 52, 3, c["line"])


TANK = {
    # M1A2 SEP v1 desert CARC (sRGB): a warm sand base with slightly darker panel shading,
    # so the box edges still read without the old olive camo pattern.
    "base": (150, 138, 108), "dark": (110, 100, 76), "camo": (134, 122, 94),
    "light": (176, 164, 134), "line": (96, 88, 66), "rivet": (190, 180, 152),
    "strap": (52, 48, 40), "track": (38, 40, 44), "tan": (160, 146, 112),
    "lens": (240, 232, 190), "glass_dark": (30, 38, 46), "glass_light": (96, 120, 140),
    "discharger": (90, 59, 51), "mark": (201, 201, 201),
}

IFV = {
    "base": (92, 98, 102), "dark": (54, 58, 62), "camo": (66, 72, 76),
    "light": (124, 130, 134), "line": (44, 48, 52), "rivet": (150, 154, 156),
    "strap": (32, 34, 36), "track": (33, 35, 38), "tan": (146, 140, 118),
    "lens": (236, 228, 186), "glass_dark": (30, 38, 46), "glass_light": (100, 126, 148),
    "discharger": (92, 62, 54), "mark": (206, 206, 202),
}

PAINTERS = {
    "olive": paint_hull,
    "olive_dark": paint_dark,
    "steel": paint_steel,
    "track": paint_track,
    "gunmetal": paint_gunmetal,
    "sleeve_tan": lambda px, r, c, rng: paint_tan(px, r, c),
    "light_yellow": lambda px, r, c, rng: paint_light(px, r, c),
    "glass": lambda px, r, c, rng: paint_glass(px, r, c),
    "discharger": lambda px, r, c, rng: paint_discharger(px, r, c),
    "marking": lambda px, r, c, rng: paint_marking(px, r, c),
    "roller": paint_roller,
    "hub": paint_hub,
    "skirt": paint_skirt,
}


def build(palette, seed, zones):
    rng = Rng(seed)
    grid = [[palette["base"]] * SIZE for _ in range(SIZE)]
    for name, r in zones.items():
        PAINTERS[name](grid, r, palette, rng)
    return [grid[y][x] for y in range(SIZE) for x in range(SIZE)]


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
    write_png(os.path.join(root, "main_battle_tank.png"), build(TANK, 1105, TANK_ZONES))
    write_png(os.path.join(root, "infantry_fighting_vehicle.png"), build(IFV, 2205, IFV_ZONES))
