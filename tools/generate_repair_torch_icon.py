#!/usr/bin/env python3
"""16x16 inventory icon for the field repair torch.

Pure-stdlib writer (zlib), same approach as tools/generate_item_icons.py, so the icon can be
regenerated without an imaging library. A copper nozzle, a steel body with a highlight down one
side, a dark grip and a spark at the tip: enough silhouette to read at inventory scale.
"""
import os
import struct
import zlib

SIZE = 16
CLEAR = (0, 0, 0, 0)
OUTLINE = (24, 26, 30, 255)
STEEL = (112, 120, 128, 255)
STEEL_LIGHT = (166, 174, 182, 255)
STEEL_DARK = (70, 76, 84, 255)
COPPER = (188, 112, 54, 255)
COPPER_DARK = (132, 74, 34, 255)
GRIP = (58, 54, 52, 255)
SPARK = (255, 214, 96, 255)
FLAME = (255, 138, 40, 255)


def blank():
    return [CLEAR] * (SIZE * SIZE)


def rect(px, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < SIZE and 0 <= y < SIZE:
                px[y * SIZE + x] = colour


def put(px, x, y, colour):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        px[y * SIZE + x] = colour


def torch():
    px = blank()
    # Spark and flame at the nozzle mouth.
    put(px, 8, 1, SPARK)
    put(px, 7, 2, FLAME)
    put(px, 8, 2, SPARK)
    put(px, 9, 2, FLAME)
    # Copper nozzle.
    rect(px, 6, 3, 9, 3, COPPER)
    rect(px, 6, 4, 9, 4, COPPER_DARK)
    # Steel barrel with a highlight on the left edge.
    rect(px, 6, 5, 8, 10, STEEL)
    rect(px, 6, 5, 6, 10, STEEL_LIGHT)
    rect(px, 8, 5, 8, 10, STEEL_DARK)
    # Grip.
    rect(px, 6, 11, 8, 14, GRIP)
    put(px, 6, 11, STEEL_DARK)
    # Outline the silhouette so it reads on any inventory background.
    for y in range(1, 15):
        for x in range(5, 11):
            if px[y * SIZE + x] == CLEAR:
                neighbours = ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))
                if any(px[ny * SIZE + nx] != CLEAR and nx >= 0 and ny >= 0
                       and nx < SIZE and ny < SIZE for nx, ny in neighbours):
                    put(px, x, y, OUTLINE)
    return px


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
    write_png(os.path.join(root, "repair_torch.png"), torch())
