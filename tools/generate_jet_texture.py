#!/usr/bin/env python3
"""Detailed 512x512 sheets for the two fixed wing jets, plus their inventory icons.

The six zone rectangles are unchanged (they are baked into the texOffs calls of JetModel and
Su57Model), but every zone is now painted: a lit gradient, panel lines with rivets, camouflage
mottling, weathering streaks, low visibility markings, a reflective canopy, heat stained nozzles
and stencilled store metal. Deterministic, pure stdlib.
"""
import os
import struct
import zlib

SIZE = 512


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


def paint_airframe(px, x0, y0, w, h, c, rng):
    light, base, dark, line, rivet, mark = (c["light"], c["base"], c["dark"],
                                            c["line"], c["rivet"], c["mark"])
    for yy in range(h):
        t = yy / max(1, h - 1)
        row = px[y0 + yy]
        for xx in range(w):
            row[x0 + xx] = mix(light, base, min(1.0, t * 1.35))
    # Camouflage mottling in two contrasting tones.
    camo = c["camo"]
    for _ in range(70):
        cx, cy = x0 + rng.below(w), y0 + rng.below(h)
        colour = dark if rng.below(2) == 0 else camo
        radius = 12 + rng.below(26)
        for y in range(max(y0, cy - radius), min(y0 + h, cy + radius)):
            row = px[y]
            for x in range(max(x0, cx - radius), min(x0 + w, cx + radius)):
                if (x - cx) ** 2 + (y - cy) ** 2 <= radius * radius:
                    row[x] = mix(row[x], colour, 0.55)
    # Panel lines with rivets along them.
    for yy in range(0, h, 26):
        for x in range(x0, x0 + w):
            px[y0 + yy][x] = mix(px[y0 + yy][x], line, 0.6)
        for x in range(x0 + 10, x0 + w, 30):
            rect(px, x, y0 + yy - 1, 2, 2, rivet)
    for xx in range(0, w, 52):
        for y in range(y0, y0 + h):
            px[y][x0 + xx] = mix(px[y][x0 + xx], line, 0.45)
    # Weathering streaks.
    for _ in range(60):
        sx, sy = x0 + rng.below(w), y0 + rng.below(max(1, h - 20))
        for i in range(6 + rng.below(20)):
            if sy + i < y0 + h:
                px[sy + i][sx] = mix(px[sy + i][sx], line, 0.4)
    # A low visibility marking block.
    rect(px, x0 + 20, y0 + 40, 4, 70, mark)
    rect(px, x0 + 20, y0 + 40, 60, 4, mark)
    rect(px, x0 + 20, y0 + 106, 60, 4, mark)


def paint_deck(px, x0, y0, w, h, c, rng):
    dark, line, rivet = c["dark"], c["line"], c["rivet"]
    for yy in range(h):
        for xx in range(w):
            check = 0 if ((xx // 8) + (yy // 8)) % 2 == 0 else 6
            px[y0 + yy][x0 + xx] = tuple(min(255, v + check) for v in dark)
    for yy in range(0, h, 32):
        rect(px, x0, y0 + yy, w, 1, line)
    for _ in range(40):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 6, 1, line)


def paint_glass(px, x0, y0, w, h, c):
    for yy in range(h):
        for xx in range(w):
            t = (xx / max(1, w - 1)) * 0.4 + ((h - 1 - yy) / max(1, h - 1)) * 0.6
            px[y0 + yy][x0 + xx] = mix(c["glass_dark"], c["glass_light"], t)
    for yy in range(0, h, 16):
        rect(px, x0, y0 + yy, w, 2, c["line"])


def paint_engine(px, x0, y0, w, h, c):
    for yy in range(h):
        for xx in range(w):
            t = xx / max(1, w - 1)
            if t < 0.6:
                colour = mix(c["steel_light"], c["steel_dark"], t / 0.6)
            else:
                colour = mix(c["steel_dark"], c["hot"], (t - 0.6) / 0.4)
            px[y0 + yy][x0 + xx] = colour
    for xx in range(0, w, 16):
        rect(px, x0 + xx, y0, 1, h, c["line"])
    rect(px, x0 + w - 10, y0, 4, h, c["ring"])
    rect(px, x0, y0, w, 4, c["ring"])


def paint_store(px, x0, y0, w, h, c):
    for yy in range(h):
        for xx in range(w):
            px[y0 + yy][x0 + xx] = mix(c["store_light"], c["store_dark"], yy / max(1, h - 1))
    for xx in range(0, w, 24):
        rect(px, x0 + xx, y0, 3, h, c["line"])
    for xx in range(x0 + 4, x0 + w, 16):
        rect(px, xx, y0 + 6, 8, 10, c["warn"])
        rect(px, xx, y0 + 6, 8, 2, c["line"])
    rect(px, x0 + 40, y0 + 40, 48, 4, c["mark"])
    for i in range(6):
        rect(px, x0 + 44 + i * 7, y0 + 46, 4, 10, c["mark"])


def paint_accent(px, x0, y0, w, h, c):
    rect(px, x0, y0, w, h, c["dark"])
    for yy in range(0, h, 12):
        rect(px, x0, y0 + yy, w, 1, c["line"])
    for xx in range(0, w, 20):
        rect(px, x0 + xx, y0, 1, h, mix(c["dark"], c["line"], 0.5))


F35_FRONT = {
    "body": (0, 0, 256, 256),
    "deck": (256, 0, 256, 128),
    "glass": (256, 128, 128, 64),
    "engine": (384, 128, 128, 64),
    "store": (256, 192, 128, 64),
    "accent": (384, 192, 128, 64),
}

F35_PALETTE = {
    "light": (126, 134, 146), "base": (88, 96, 108), "dark": (58, 64, 74),
    "line": (46, 52, 62), "rivet": (150, 156, 166), "mark": (176, 182, 192),
    "camo": (74, 82, 94), "glass_dark": (26, 34, 44), "glass_light": (86, 118, 150),
    "steel_light": (128, 132, 138), "steel_dark": (62, 64, 70), "hot": (108, 74, 60),
    "ring": (168, 172, 178), "store_light": (140, 144, 150), "store_dark": (96, 100, 106),
    "warn": (150, 132, 60),
}

SU57_PALETTE = {
    "light": (132, 138, 120), "base": (96, 104, 92), "dark": (64, 72, 62),
    "line": (52, 58, 48), "rivet": (156, 160, 142), "mark": (180, 184, 168),
    "camo": (80, 88, 76), "glass_dark": (26, 36, 40), "glass_light": (92, 118, 122),
    "steel_light": (132, 132, 128), "steel_dark": (60, 60, 62), "hot": (112, 78, 58),
    "ring": (170, 170, 164), "store_light": (136, 138, 126), "store_dark": (92, 96, 86),
    "warn": (150, 132, 60),
}


def build(palette, seed):
    rng = Rng(seed)
    px = [[palette["base"]] * SIZE for _ in range(SIZE)]
    paint_airframe(px, *F35_FRONT["body"], palette, rng)
    paint_deck(px, *F35_FRONT["deck"], palette, rng)
    paint_glass(px, *F35_FRONT["glass"], palette)
    paint_engine(px, *F35_FRONT["engine"], palette)
    paint_store(px, *F35_FRONT["store"], palette)
    paint_accent(px, *F35_FRONT["accent"], palette)
    # write_png takes a flat row-major list, the painters work on a 2D grid.
    return [px[y][x] for y in range(SIZE) for x in range(SIZE)]


def item_icon(body=(120, 128, 140)):
    glass = (70, 110, 150, 255)
    flame = (230, 140, 60, 255)
    clear = (0, 0, 0, 0)
    body4 = (body[0], body[1], body[2], 255)
    pixels = [clear] * (16 * 16)

    def put(x, y, colour):
        if 0 <= x < 16 and 0 <= y < 16:
            pixels[y * 16 + x] = colour

    for y in range(0, 15):
        put(7, y, body4)
        put(8, y, body4)
    for y in range(0, 3):
        put(6, y, body4)
        put(9, y, body4)
    for y, inset in {6: 3, 7: 2, 8: 1, 9: 2, 10: 3}.items():
        for x in range(inset, 16 - inset):
            put(x, y, body4)
    for x in range(2, 14):
        put(x, 12, body4)
    for y in range(3, 6):
        for x in range(6, 10):
            put(x, y, glass)
    put(7, 15, flame)
    put(8, 15, flame)
    return pixels


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
                        "src", "main", "resources", "assets", "battlefield", "textures")
    write_png(os.path.join(root, "entity", "f35.png"), build(F35_PALETTE, 3501))
    write_png(os.path.join(root, "entity", "su57.png"), build(SU57_PALETTE, 5701))
    write_png(os.path.join(root, "item", "f35.png"), item_icon(), size=16)
    write_png(os.path.join(root, "item", "su57.png"),
              item_icon(body=(110, 118, 104)), size=16)
