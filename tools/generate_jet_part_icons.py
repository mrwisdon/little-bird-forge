#!/usr/bin/env python3
"""16x16 icons for the four fixed wing assembly intermediates.

Standalone on purpose: the shared icon script only carries the ground units, and these four are a
one-off addition (two airframes, two weapon bays) drawn with the same pure-stdlib PNG writer.
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
OLIVE_D = (74, 88, 52, 255)
WHITE = (206, 212, 216, 255)


def blank():
    return [CLEAR] * (SIZE * SIZE)


def rect(px, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < SIZE and 0 <= y < SIZE:
                px[y * SIZE + x] = colour


def jet(body, wing):
    px = blank()
    for y in range(2, 15):
        rect(px, 7, y, 8, y, body)
    rect(px, 7, 1, 8, 2, LIGHT)
    for i in range(4):
        rect(px, 4 - i, 6 + i, 4 - i, 6 + i, wing)
        rect(px, 11 + i, 6 + i, 11 + i, 6 + i, wing)
    rect(px, 3, 9, 12, 9, wing)
    rect(px, 5, 12, 10, 12, wing)
    return px


def bay(shell):
    px = blank()
    rect(px, 2, 4, 13, 11, OUT)
    rect(px, 3, 5, 12, 10, shell)
    for x in (5, 8, 11):
        rect(px, x, 5, x, 10, WHITE)
    return px


ICONS = {
    "f35_airframe": lambda: jet(DARK, MID),
    "su57_airframe": lambda: jet(MID, DARK),
    "f35_weapons": lambda: bay(MID),
    "su57_weapons": lambda: bay(OLIVE_D),
}


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
                        "src", "main", "resources", "assets", "battlefield", "textures", "item")
    for name, builder in ICONS.items():
        write_png(os.path.join(root, name + ".png"), builder())
