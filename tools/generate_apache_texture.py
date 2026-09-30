#!/usr/bin/env python3
"""AH-64 entity texture: a 512x512 painted panel sheet, rebuilt in WSL.

Port of the original PowerShell generator (which needs pwsh interop here) with a real detail pass:
top-lit gradients, camouflage mottling, riveted panel lines, weathering streaks, low visibility
markings, canopy reflection, brushed metal, a ribbed pod with red tube caps, treaded tyres and an
MFD instrument strip. Bands must stay in step with the texOffs calls in ApacheModel:

  y   0-111  olive airframe panels (cabin, wings, nose, boom, engines)
  y 112-207  smoke glazing with frame ribs
  y 208-303  dark metal (mast, hub, radar, pylons, rails, exhausts); blade stripe x 440-458
  y 304-399  rocket pod black with dark red tube caps at x 480-511
  y 400-511  turret, barrels, wheels and the instrument strip at x 300-380
"""
import os
import struct
import zlib

SIZE = 512


def blank(fill=(0, 0, 0)):
    return [[fill for _ in range(SIZE)] for _ in range(SIZE)]


def rect(px, x0, y0, w, h, colour):
    for y in range(max(0, y0), min(SIZE, y0 + h)):
        row = px[y]
        for x in range(max(0, x0), min(SIZE, x0 + w)):
            row[x] = colour


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Rng:
    """Tiny deterministic LCG so every rebuild writes the same bytes."""

    def __init__(self, seed=2042):
        self.state = seed & 0xFFFFFFFF

    def next(self):
        self.state = (self.state * 1103515245 + 12345) & 0x7FFFFFFF
        return self.state

    def below(self, n):
        return self.next() % n


def band_a(px, rng):
    """Olive airframe: gradient, camo blotches, riveted panels, streaks and markings."""
    top = (66, 72, 60)
    bottom = (44, 50, 40)
    for y in range(112):
        base = mix(top, bottom, y / 111.0)
        row = px[y]
        for x in range(512):
            row[x] = base
    # Camouflage mottling: overlapping blobs in two contrasting olives.
    for _ in range(150):
        cx, cy = rng.below(512), rng.below(112)
        colour = (38, 44, 34) if rng.below(2) == 0 else (74, 82, 66)
        radius = 14 + rng.below(26)
        for y in range(max(0, cy - radius), min(112, cy + radius)):
            row = px[y]
            dy = y - cy
            for x in range(max(0, cx - radius), min(512, cx + radius)):
                if (x - cx) ** 2 + dy * dy <= radius * radius:
                    row[x] = mix(row[x], colour, 0.65)
    # Panel lines: a few horizontals and fewer verticals, with rivets along them.
    rivet = (96, 102, 86)
    for y in range(0, 112, 22):
        for x in range(512):
            px[y][x] = mix(px[y][x], (30, 34, 28), 0.55)
        for x in range(8, 512, 32):
            rect(px, x, y - 1, 2, 2, rivet)
    for x in range(0, 512, 56):
        for y in range(112):
            px[y][x] = mix(px[y][x], (30, 34, 28), 0.45)
        for y in range(12, 112, 24):
            rect(px, x - 1, y, 2, 2, rivet)
    # Weathering: dark streaks running back from random panel joints.
    for _ in range(70):
        x, y = rng.below(512), rng.below(96)
        length = 6 + rng.below(24)
        for i in range(length):
            row = px[y + i]
            if 0 <= y + i < 112:
                row[x] = mix(row[x], (30, 34, 28), 0.45)
    # Subdued warning stripe block and a low visibility marking block.
    for x in range(400, 512, 16):
        rect(px, x, 0, 8, 40, (96, 88, 36))
        rect(px, x + 8, 0, 8, 40, (30, 32, 28))
    # Low visibility serial block, parked in the region the nose and fin unwrap.
    rect(px, 176, 50, 68, 4, (118, 124, 106))
    rect(px, 176, 76, 68, 4, (118, 124, 106))
    for i in range(7):
        rect(px, 182 + i * 9, 56, 5, 18, (130, 136, 116))


def band_b(px):
    """Smoke glazing: diagonal reflection plus frame ribs."""
    for y in range(112, 208):
        for x in range(512):
            t = (x / 511.0) * 0.35 + ((207 - y) / 95.0) * 0.65
            px[y][x] = mix((16, 22, 22), (54, 74, 92), t)
    frame = (68, 78, 82)
    for y in range(112, 208, 24):
        rect(px, 0, y, 512, 2, frame)
    for x in range(0, 512, 48):
        rect(px, x, 112, 2, 96, frame)


def band_c(px, rng):
    """Dark metal: brushed noise, a pale blade tip stripe and a hub highlight."""
    for y in range(208, 304):
        for x in range(512):
            base = 40 + (x // 7) % 3
            px[y][x] = (base, base + 2, base + 3)
    for y in range(208, 304, 16):
        rect(px, 0, y, 440, 1, (28, 30, 32))
    for _ in range(200):
        rect(px, rng.below(440), 208 + rng.below(96), 8, 1, (58, 62, 64))
    rect(px, 440, 208, 18, 96, (198, 198, 192))
    rect(px, 440, 208, 3, 96, (232, 232, 226))


def band_d(px):
    """Rocket pod: ribbed black with red tube caps and tube rims."""
    rect(px, 0, 304, 512, 96, (22, 24, 22))
    for x in range(0, 480, 24):
        rect(px, x, 304, 4, 96, (32, 34, 32))
    # Tube rim hints on the pod face unwrap.
    import math
    for row, count in enumerate((4, 5, 5, 5)):
        for col in range(count):
            cx = 10 + col * 14 + (row % 2) * 2
            cy = 312 + row * 18
            for y in range(cy - 6, cy + 7):
                for x in range(cx - 6, cx + 7):
                    d = (x - cx) ** 2 + (y - cy) ** 2
                    if 25 <= d <= 36 and 0 <= x < 480 and 0 <= y < 400:
                        px[y][x] = (12, 12, 12)
    rect(px, 480, 304, 32, 96, (98, 34, 30))
    rect(px, 480, 304, 32, 6, (140, 54, 46))


def band_e(px):
    """Turret and gunmetal, treaded tyres, and the MFD instrument strip."""
    for y in range(400, 512):
        for x in range(0, 280):
            px[y][x] = mix((44, 48, 50), (26, 28, 30), (y - 400) / 111.0)
    for y in range(400, 504, 12):
        rect(px, 0, y, 280, 1, (20, 22, 24))
    rect(px, 0, 400, 24, 112, (16, 18, 20))
    rect(px, 400, 400, 112, 112, (24, 24, 26))
    for y in range(404, 508, 10):
        rect(px, 400, y, 112, 3, (14, 14, 16))
    rect(px, 448, 436, 22, 22, (70, 70, 74))
    rect(px, 452, 440, 14, 14, (32, 32, 34))
    rect(px, 300, 400, 80, 112, (10, 12, 14))
    for y in range(408, 504, 12):
        rect(px, 304, y, 72, 6, (40, 150, 190))
        rect(px, 304, y, 72, 2, (150, 220, 240))
    rect(px, 288, 400, 40, 40, (74, 78, 74))


def build_pixels():
    rng = Rng()
    px = blank()
    band_a(px, rng)
    band_b(px)
    band_c(px, rng)
    band_d(px)
    band_e(px)
    return px


def write_png(path, pixels, size=SIZE):
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            r, g, b = pixels[y][x]
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
    write_png(os.path.join(root, "apache.png"), build_pixels())
