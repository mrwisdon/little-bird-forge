#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""B-2 Spirit textures for the battlefield mod: pure stdlib, deterministic.

    python3 tools/generate_b2_texture.py [--outdir DIR]

Reads tools/data/b2_uv.json (same zone table that is frozen into tools/generate_b2_model.py as
UV_ZONES) and writes, by default into src/main/resources/assets/battlefield/textures:

  entity/b2.png        512x512 sheet for the box model client/B2Model.java.
  item/b2_preview.png  256x256, 4x4 grid of 64 px flat colour cells for the OBJ preview
                       (models/item/b2_preview.obj pins every face UV on a cell centre).
  item/b2.png          16x16 deploy-item icon (models/item/b2.json -> layer0 battlefield:item/b2).

flip_v: models/item/b2_preview.json sets "flip_v": true, and Forge does

    ObjModel.java:428   getV((flipV ? 1 - texCoord.v : texCoord.v) * 16)

so an OBJ vt of v=0.125 samples image row (1-0.125)*H -- the BOTTOM row of the file. The contract
row0=[skin,panel,glass,intake] therefore lives in file row 3, and the file rows are the contract
rows in reverse order. Columns are not flipped. tools/data/b2_uv.json records the same note.
"""
import argparse
import hashlib
import json
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UV_JSON = os.path.join(ROOT, "tools", "data", "b2_uv.json")
DEFAULT_OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "battlefield", "textures")

SIZE = 512
PREVIEW_SIZE = 256
CELL = 64
SEED = 20420

# 16x16 inventory icon of the deploy item. '.' transparent, '#' charcoal airframe,
# '=' lit leading edge, 'o' canopy glass.
ICON_ART = [
    "................",
    ".......##.......",
    "......#oo#......",
    "......#oo#......",
    ".....=####=.....",
    "....==####==....",
    "...==######==...",
    "..==########==..",
    ".==##########==.",
    "==############==",
    "################",
    "###..######..###",
    "##...######...##",
    "................",
    "................",
    "................",
]
ICON_COLORS = {
    "#": (0x4A, 0x51, 0x5A),
    "=": (0x6E, 0x76, 0x82),
    "o": (0x1B, 0x2A, 0x24),
    ".": (0, 0, 0),
}


def load_spec(path=UV_JSON):
    """Return (spec, zones, colors); zones maps name -> (u0, v0, w, h)."""
    with open(path, encoding="utf-8") as fh:
        spec = json.load(fh)
    if spec["texture_size"] != SIZE:
        raise SystemExit("b2_uv.json texture_size %s != %d" % (spec["texture_size"], SIZE))
    if spec["preview"]["texture_size"] != PREVIEW_SIZE or spec["preview"]["cell"] != CELL:
        raise SystemExit("b2_uv.json preview grid is not %dx%d cells of %d"
                         % (PREVIEW_SIZE, PREVIEW_SIZE, CELL))
    zones = {}
    for name, uv in spec["zones"].items():
        w, h = spec["need"][name]
        zones[name] = (int(uv[0]), int(uv[1]), int(w), int(h))
    colors = {name: tuple(rgb) for name, rgb in spec["colors"].items()}
    for name, (u0, v0, w, h) in zones.items():
        if u0 + w > SIZE or v0 + h > SIZE:
            raise SystemExit("zone %s overflows the sheet" % name)
        if name not in colors:
            raise SystemExit("zone %s has no colour" % name)
    return spec, zones, colors


def mix(a, b, t):
    t = 0.0 if t < 0.0 else (1.0 if t > 1.0 else t)
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c)


class Rng:
    """Deterministic LCG, same as tools/generate_jet_texture.py."""

    def __init__(self, seed):
        self.state = seed & 0xFFFFFFFF

    def next(self):
        self.state = (self.state * 1103515245 + 12345) & 0x7FFFFFFF
        return self.state

    def below(self, n):
        return self.next() % n


def rect(px, x0, y0, w, h, colour, size=SIZE):
    for y in range(max(0, y0), min(size, y0 + h)):
        row = px[y]
        for x in range(max(0, x0), min(size, x0 + w)):
            row[x] = colour


# ---------------------------------------------------------------------------
# zone painters
# ---------------------------------------------------------------------------
def paint_skin(px, z, base, rng):
    """Charcoal RAM coating: lit gradient, panel lines with rivets, weathering."""
    x0, y0, w, h = z
    light, line, rivet = shade(base, 8), shade(base, -14), shade(base, 26)
    for yy in range(h):
        row = px[y0 + yy]
        col = mix(light, base, yy / max(1, h - 1))
        for xx in range(w):
            row[x0 + xx] = col
    for yy in range(0, h, 32):
        for x in range(x0, x0 + w):
            px[y0 + yy][x] = mix(px[y0 + yy][x], line, 0.65)
        for x in range(x0 + 12, x0 + w, 36):
            rect(px, x, y0 + yy - 1, 2, 2, rivet)
    for xx in range(0, w, 64):
        for y in range(y0, y0 + h):
            px[y][x0 + xx] = mix(px[y][x0 + xx], line, 0.5)
    for _ in range(70):
        sx, sy = x0 + rng.below(w), y0 + rng.below(max(1, h - 16))
        for i in range(4 + rng.below(18)):
            if sy + i < y0 + h:
                px[sy + i][sx] = mix(px[sy + i][sx], line, 0.35)
    # Low visibility stencil, clipped to the zone.
    rect(px, x0 + 16, y0 + 20, 3, 48, shade(base, 18))
    rect(px, x0 + 16, y0 + 20, 40, 3, shade(base, 18))


def paint_panel(px, z, base, rng):
    """Secondary plating: same charcoal family, 10 percent darker, denser seams."""
    x0, y0, w, h = z
    line, rivet = shade(base, -12), shade(base, 20)
    for yy in range(h):
        row = px[y0 + yy]
        col = mix(shade(base, 5), base, yy / max(1, h - 1))
        for xx in range(w):
            row[x0 + xx] = col
    for yy in range(0, h, 16):
        for x in range(x0, x0 + w):
            px[y0 + yy][x] = mix(px[y0 + yy][x], line, 0.6)
        for x in range(x0 + 8, x0 + w, 24):
            rect(px, x, y0 + yy - 1, 1, 2, rivet)
    for xx in range(0, w, 32):
        for y in range(y0, y0 + h):
            px[y][x0 + xx] = mix(px[y][x0 + xx], line, 0.45)


def paint_glass(px, z, base):
    """Canopy glass: dark, with a faint gold-green sheen across the pane."""
    x0, y0, w, h = z
    dark, sheen, line = shade(base, -6), (0x5B, 0x6B, 0x3E), shade(base, -12)
    for yy in range(h):
        for xx in range(w):
            t = (xx / max(1, w - 1)) * 0.45 + ((h - 1 - yy) / max(1, h - 1)) * 0.55
            px[y0 + yy][x0 + xx] = mix(dark, sheen, t)
    for yy in range(0, h, 12):
        rect(px, x0, y0 + yy, w, 1, line)


def paint_intake(px, z, base, rng):
    """Radar absorbent inlet: near black with faint ribbing."""
    x0, y0, w, h = z
    rib = shade(base, 12)
    rect(px, x0, y0, w, h, base)
    for xx in range(0, w, 6):
        for y in range(y0, y0 + h):
            px[y][x0 + xx] = mix(px[y][x0 + xx], rib, 0.5)
    for _ in range(30):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 3, 1, shade(base, 6))


def paint_exhaust(px, z, base, rng):
    """Gunmetal nozzle with temper colours: straw, blue, back to grey."""
    x0, y0, w, h = z
    stops = [(0.0, shade(base, 22)), (0.35, base), (0.55, (0x60, 0x54, 0x3C)),
             (0.75, (0x48, 0x4A, 0x5C)), (1.0, shade(base, -8))]
    for yy in range(h):
        for xx in range(w):
            t = xx / max(1, w - 1)
            for i in range(len(stops) - 1):
                a, b = stops[i], stops[i + 1]
                if a[0] <= t <= b[0]:
                    px[y0 + yy][x0 + xx] = mix(a[1], b[1], (t - a[0]) / (b[0] - a[0]))
                    break
            else:
                px[y0 + yy][x0 + xx] = stops[-1][1]
    for xx in range(0, w, 14):
        rect(px, x0 + xx, y0, 1, h, shade(base, -18))
    for yy in range(0, h, 10):
        rect(px, x0, y0 + yy, w, 1, shade(base, -10))
    rect(px, x0 + w - 8, y0, 4, h, shade(base, 30))
    for _ in range(24):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 5, 1, shade(base, -14))


def paint_metal(px, z, base, rng):
    """Bare steel: bright gradient, rivet rows."""
    x0, y0, w, h = z
    line, rivet = shade(base, -46), shade(base, 22)
    for yy in range(h):
        for xx in range(w):
            t = (yy / max(1, h - 1)) * 0.7 + (xx / max(1, w - 1)) * 0.3
            px[y0 + yy][x0 + xx] = mix(shade(base, 14), base, t)
    for yy in range(0, h, 14):
        for x in range(x0, x0 + w):
            px[y0 + yy][x] = mix(px[y0 + yy][x], line, 0.55)
        for x in range(x0 + 6, x0 + w, 18):
            rect(px, x, y0 + yy + 2, 2, 2, rivet)
    for _ in range(26):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 6, 1, line)


def paint_bay(px, z, base, rng, ribs):
    """Weapon bay / gear bay interior: white with structure ribs and a hydraulic line."""
    x0, y0, w, h = z
    line, stain = shade(base, -40), (0x8A, 0x7C, 0x5E)
    for yy in range(h):
        row = px[y0 + yy]
        col = mix(shade(base, 6), base, yy / max(1, h - 1))
        for xx in range(w):
            row[x0 + xx] = col
    for xx in range(0, w, max(8, w // ribs)):
        for y in range(y0, y0 + h):
            px[y][x0 + xx] = mix(px[y][x0 + xx], line, 0.5)
    for yy in range(0, h, 12):
        rect(px, x0, y0 + yy, w, 1, mix(base, line, 0.35))
    for _ in range(20):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 4, 1, stain)
    rect(px, x0, y0 + h // 2, w, 1, shade(base, -70))


def paint_tire(px, z, base):
    """Rubber: near black with tread bands."""
    x0, y0, w, h = z
    tread, wall = shade(base, 10), shade(base, -6)
    rect(px, x0, y0, w, h, base)
    for yy in range(0, h, 6):
        rect(px, x0, y0 + yy, w, 2, tread)
    for xx in range(0, w, 10):
        for y in range(y0, y0 + h):
            px[y][x0 + xx] = mix(px[y][x0 + xx], wall, 0.5)
    rect(px, x0, y0 + h - 3, w, 3, shade(base, -4))


def paint_light(px, z, base):
    """Lamp lens: flat coloured glass with a hot centre."""
    x0, y0, w, h = z
    rect(px, x0, y0, w, h, shade(base, -26))
    rect(px, x0 + 2, y0 + 2, max(1, w - 4), max(1, h - 4), base)
    hot = mix(base, (255, 255, 255), 0.35)
    rect(px, x0 + w // 4, y0 + h // 4, max(1, w // 2), max(1, h // 2), hot)


def paint_gold(px, z, base):
    """Gold trim / markings."""
    x0, y0, w, h = z
    rect(px, x0, y0, w, h, base)
    for yy in range(h):
        t = 1.0 - abs((yy / max(1, h - 1)) - 0.5) * 2.0
        for xx in range(w):
            px[y0 + yy][x0 + xx] = mix(base, shade(base, 34), t * 0.7)
    for yy in range(0, h, 8):
        rect(px, x0, y0 + yy, w, 1, shade(base, -30))


def paint_ordnance(px, z, base, rng):
    """Olive grey stores: body, stripe, nose cone."""
    x0, y0, w, h = z
    line, band = shade(base, -20), (0x8C, 0x7A, 0x3C)
    for yy in range(h):
        for xx in range(w):
            px[y0 + yy][x0 + xx] = mix(shade(base, 8), shade(base, -8), yy / max(1, h - 1))
    for xx in range(0, w, 18):
        rect(px, x0 + xx, y0, 1, h, line)
    rect(px, x0 + 4, y0 + h // 3, max(1, w - 8), 3, band)
    for _ in range(16):
        rect(px, x0 + rng.below(w), y0 + rng.below(h), 5, 1, line)


def build_sheet(zones, colors):
    rng = Rng(SEED)
    # Anything outside a zone keeps the skin colour, so UV bleed still reads as airframe.
    px = [[shade(colors["skin"], -6)] * SIZE for _ in range(SIZE)]
    z = zones
    paint_skin(px, z["skin"], colors["skin"], rng)
    paint_panel(px, z["panel"], colors["panel"], rng)
    paint_glass(px, z["glass"], colors["glass"])
    paint_intake(px, z["intake"], colors["intake"], rng)
    paint_exhaust(px, z["exhaust"], colors["exhaust"], rng)
    paint_metal(px, z["metal"], colors["metal"], rng)
    paint_bay(px, z["bay_white"], colors["bay_white"], rng, 10)
    paint_bay(px, z["gear_white"], colors["gear_white"], rng, 5)
    paint_ordnance(px, z["ordnance"], colors["ordnance"], rng)
    paint_tire(px, z["tire"], colors["tire"])
    paint_light(px, z["light_r"], colors["light_r"])
    paint_light(px, z["light_g"], colors["light_g"])
    paint_light(px, z["light_w"], colors["light_w"])
    paint_gold(px, z["gold"], colors["gold"])
    return px


def build_preview(rows, colors, size, cell):
    """4x4 grid. Rows are the contract rows bottom-up -- see the flip_v note above."""
    px = [[(0, 0, 0)] * size for _ in range(size)]
    for png_row in range(4):
        contract_row = 3 - png_row
        for col in range(4):
            name = rows[contract_row][col]
            base = colors[name]
            x0, y0 = col * cell, png_row * cell
            rect(px, x0, y0, cell, cell, base, size)
            # 1 px frame, and +-3 noise only outside the central 24x24 so the cell centre
            # stays an exact flat colour (the OBJ pins every UV there).
            frame = shade(base, -18)
            for i in range(cell):
                px[y0][x0 + i] = frame
                px[y0 + cell - 1][x0 + i] = frame
                px[y0 + i][x0] = frame
                px[y0 + i][x0 + cell - 1] = frame
            for yy in range(2, cell - 2):
                for xx in range(2, cell - 2):
                    if 20 <= xx <= 43 and 20 <= yy <= 43:
                        continue
                    n = ((xx * 7 + yy * 13 + col * 31 + png_row * 17) % 7) - 3
                    px[y0 + yy][x0 + xx] = shade(base, n)
    return px


def build_icon():
    """16x16 planform silhouette of the flying wing, alpha 0 outside the shape."""
    size = len(ICON_ART)
    px = [[(0, 0, 0, 0)] * size for _ in range(size)]
    for y, row in enumerate(ICON_ART):
        for x, ch in enumerate(row):
            r, g, b = ICON_COLORS[ch]
            px[y][x] = (0, 0, 0, 0) if ch == "." else (r, g, b, 255)
    return px


def write_png(path, px, size):
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            p = px[y][x]
            raw.extend(p if len(p) == 4 else (p[0], p[1], p[2], 255))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(png)
    return len(png)


def main():
    parser = argparse.ArgumentParser(description="Generate the B-2 textures from b2_uv.json")
    parser.add_argument("--outdir", default=DEFAULT_OUT,
                        help="texture root to write into (default: the mod's assets)")
    args = parser.parse_args()
    spec, zones, colors = load_spec()

    sheet = build_sheet(zones, colors)
    write_png(os.path.join(args.outdir, "entity", "b2.png"), sheet, SIZE)
    write_png(os.path.join(args.outdir, "item", "b2_preview.png"),
              build_preview(spec["preview"]["rows"], colors, PREVIEW_SIZE, CELL), PREVIEW_SIZE)
    write_png(os.path.join(args.outdir, "item", "b2.png"), build_icon(), len(ICON_ART))

    print("outdir %s" % args.outdir)
    for rel in ("entity/b2.png", "item/b2_preview.png", "item/b2.png"):
        path = os.path.join(args.outdir, rel)
        with open(path, "rb") as fh:
            data = fh.read()
        print("  %-22s %8d bytes  md5 %s" % (rel, len(data), hashlib.md5(data).hexdigest()))
    print("zones=%d" % len(zones))
    for name in sorted(zones):
        u0, v0, w, h = zones[name]
        print("  %-11s u0=%3d v0=%3d w=%3d h=%3d  #%02x%02x%02x"
              % (name, u0, v0, w, h, colors[name][0], colors[name][1], colors[name][2]))


if __name__ == "__main__":
    main()
