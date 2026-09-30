#!/usr/bin/env python3
"""16x16 block texture for the vehicle factory: iron frame, red band, gear hub."""
import os
import struct
import zlib

SIZE = 16
OUT = (26, 30, 34, 255)
DARK = (58, 64, 70, 255)
MID = (96, 106, 114, 255)
LIGHT = (150, 160, 168, 255)
RED = (168, 62, 48, 255)
RED_D = (112, 40, 32, 255)


def blank():
    return [DARK] * (SIZE * SIZE)


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


def factory():
    px = blank()
    rect(px, 1, 1, 14, 14, MID)
    rect(px, 2, 2, 13, 13, LIGHT)
    rect(px, 2, 6, 13, 9, RED)
    rect(px, 2, 6, 13, 6, RED_D)
    disc(px, 8, 8, 3, DARK)
    disc(px, 8, 8, 2, MID)
    # gear teeth
    for x, y in ((8, 4), (8, 12), (4, 8), (12, 8)):
        px[y * SIZE + x] = DARK
    # corner bolts
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[y * SIZE + x] = OUT
    return px


def write_png(path, pixels, size=SIZE):
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            raw.extend(pixels[y * size + x])

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
                        "src", "main", "resources", "assets", "battlefield", "textures", "block")
    write_png(os.path.join(root, "vehicle_factory.png"), factory())
