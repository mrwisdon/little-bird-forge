#!/usr/bin/env python3
"""PHL-191 entity, rocket and item textures.

1024x1024 digital camo sheet for the launcher (zones must match Phl191Model.Zone), a 128x64
rocket sheet and a 32x32 item icon. Deterministic; standard library only (raw PNG writer).
"""
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENTITY = os.path.join(ROOT, "src", "main", "resources", "assets", "battlefield", "textures", "entity")
ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "battlefield", "textures", "item")

CAMO = [(0xAD, 0x96, 0x70), (0x8A, 0x70, 0x48), (0x56, 0x57, 0x37), (0x35, 0x2D, 0x1E)]


def write_png(path, width, height, pixels):
    raw = b"".join(b"\x00" + bytes(pixels[y * width * 3:(y + 1) * width * 3]) for y in range(height))

    def chunk(tag, data):
        out = struct.pack(">I", len(data)) + tag + data
        return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) \
        + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(png)


def hash01(x, y, seed=0):
    h = (x * 374761393 + y * 668265263 + seed * 1442695040888963407) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def camo_pixel(x, y):
    # two scales of digital blocks, the coarse one deciding the patch colour
    coarse = hash01(x // 26, y // 26, 7)
    fine = hash01(x // 9, y // 9, 13)
    if coarse < 0.28:
        index = 0
    elif coarse < 0.56:
        index = 1 if fine > 0.25 else 0
    elif coarse < 0.82:
        index = 2 if fine > 0.3 else 1
    else:
        index = 3 if fine > 0.35 else 2
    base = CAMO[index]
    jitter = int((fine - 0.5) * 12)
    return tuple(max(0, min(255, c + jitter)) for c in base)


def launcher_texture():
    size = 1024
    px = [0] * (size * size * 3)
    for y in range(size):
        for x in range(size):
            r, g, b = camo_pixel(x, y)
            px[(y * size + x) * 3:(y * size + x) * 3 + 3] = [r, g, b]
    # zones: must match Phl191Model.Zone
    def fill(x0, y0, w, h, colour, noise=6):
        for yy in range(y0, min(size, y0 + h)):
            for xx in range(x0, min(size, x0 + w)):
                n = int((hash01(xx, yy, 3) - 0.5) * noise)
                i = (yy * size + xx) * 3
                px[i] = max(0, min(255, colour[0] + n))
                px[i + 1] = max(0, min(255, colour[1] + n))
                px[i + 2] = max(0, min(255, colour[2] + n))

    fill(0, 720, 512, 144, (0x1A, 0x1C, 0x1B), 8)          # DARK
    fill(512, 720, 320, 120, (0x16, 0x29, 0x2E), 4)         # GLASS
    for i in range(120):                                    # glass highlight
        xx = 512 + 20 + i
        yy = 720 + 18 + i // 4
        if 0 <= xx < 832 and 720 <= yy < 840:
            i0 = (yy * size + xx) * 3
            px[i0], px[i0 + 1], px[i0 + 2] = 0x4A, 0x74, 0x7E
    fill(832, 720, 120, 64, (0xF2, 0xEF, 0xD8), 3)          # LIGHT
    write_png(os.path.join(ENTITY, "phl191.png"), size, size, px)


def rocket_texture():
    w, h = 128, 64
    px = [0] * (w * h * 3)
    def fill(x0, y0, ww, hh, colour, noise=5):
        for yy in range(y0, min(h, y0 + hh)):
            for xx in range(x0, min(w, x0 + ww)):
                n = int((hash01(xx, yy, 11) - 0.5) * noise)
                i = (yy * w + xx) * 3
                px[i] = max(0, min(255, colour[0] + n))
                px[i + 1] = max(0, min(255, colour[1] + n))
                px[i + 2] = max(0, min(255, colour[2] + n))
    fill(0, 0, 60, 30, (0x2A, 0x2E, 0x2A))          # body
    fill(64, 0, 24, 12, (0x6E, 0x5E, 0x42))         # nose
    fill(64, 16, 18, 9, (0x8A, 0x70, 0x48))         # band
    fill(0, 32, 40, 7, (0x20, 0x22, 0x20))          # fins
    write_png(os.path.join(ENTITY, "phl191_rocket.png"), w, h, px)


def item_icon():
    size = 32
    px = [0] * (size * size * 3)
    for y in range(size):
        for x in range(size):
            r, g, b = camo_pixel(x * 4, y * 4)
            px[(y * size + x) * 3:(y * size + x) * 3 + 3] = [r, g, b]
    # black pods: two rows of four tube mouths on the launcher body
    for row in range(2):
        for column in range(4):
            cx = 6 + column * 5
            cy = 7 + row * 5
            for dy in range(3):
                for dx in range(3):
                    i = ((cy + dy) * size + cx + dx) * 3
                    px[i], px[i + 1], px[i + 2] = 0x14, 0x15, 0x13
    # darker chassis strip at the bottom
    for y in range(24, 30):
        for x in range(3, 29):
            i = (y * size + x) * 3
            px[i], px[i + 1], px[i + 2] = 0x1C, 0x1E, 0x1B
    write_png(os.path.join(ITEM, "phl191.png"), size, size, px)


launcher_texture()
rocket_texture()
item_icon()
print("textures written")
