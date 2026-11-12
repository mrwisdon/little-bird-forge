"""Generates the smoke canister icon and the sixteen smoke puff frames.

Run from the project root:  python3 tools/generate_smoke_assets.py

The icon is a 16x16 canister in the same flat palette as the other item icons.

The puffs are sixteen 64x64 frames of a soft cloudy wisp. One texture per frame, because the
particle engine animates a sprite set by walking the list as the particle ages - that walk is what
makes the smoke boil instead of hanging still.

The density target for a frame is not "pretty blob" but "about a third of the pixels nearly
opaque, the rest feathered away". Measured from a reference smoke grenade's sprites: alpha mean
0.36, a little over a third of the pixels above 0.8. A sprite that is mostly transparent looks
like haze no matter how many of them are stacked, which is exactly how the first two versions of
this cloud failed.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ITEM_OUT = ROOT / "src/main/resources/assets/battlefield/textures/item/smoke_grenade.png"
PARTICLE_DIR = ROOT / "src/main/resources/assets/battlefield/textures/particle"

OUTLINE = (26, 32, 26, 255)
BODY = (86, 102, 79, 255)
BODY_LIGHT = (116, 134, 106, 255)
BODY_DARK = (58, 70, 54, 255)
CAP = (52, 58, 52, 255)
CAP_LIGHT = (86, 94, 86, 255)
BAND = (198, 178, 78, 255)
BAND_DARK = (150, 132, 52, 255)
SMOKE = (214, 214, 210, 255)
SMOKE_SOFT = (176, 176, 172, 255)

FRAME_COUNT = 16
FRAME_SIZE = 64


def canister_icon() -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle([5, 4, 10, 13], fill=BODY, outline=OUTLINE)
    draw.line([5, 4, 5, 13], fill=BODY_LIGHT)
    draw.line([10, 4, 10, 13], fill=BODY_DARK)
    draw.line([5, 13, 10, 13], fill=BODY_DARK)
    draw.rectangle([5, 8, 10, 9], fill=BAND)
    draw.line([5, 9, 10, 9], fill=BAND_DARK)
    draw.rectangle([6, 2, 9, 4], fill=CAP, outline=OUTLINE)
    draw.line([6, 2, 6, 3], fill=CAP_LIGHT)
    draw.rectangle([8, 1, 9, 1], fill=CAP_LIGHT, outline=OUTLINE)
    draw.point([(2, 2)], fill=SMOKE)
    draw.point([(3, 1)], fill=SMOKE)
    draw.point([(1, 3)], fill=SMOKE_SOFT)
    draw.point([(2, 4)], fill=SMOKE_SOFT)
    draw.point([(4, 2)], fill=SMOKE_SOFT)
    return image


def value_noise(rng: np.random.Generator, size: int, octaves: int) -> np.ndarray:
    """Summed value noise in 0..1: soft, lumpy, and free of straight edges."""
    total = np.zeros((size, size), dtype=np.float32)
    amplitude = 1.0
    weight = 0.0
    for octave in range(octaves):
        cells = max(2, 2 << octave)
        coarse = rng.random((cells, cells)).astype(np.float32)
        layer = np.asarray(
            Image.fromarray((coarse * 255).astype(np.uint8), "L").resize(
                (size, size), Image.BICUBIC),
            dtype=np.float32) / 255.0
        total += layer * amplitude
        weight += amplitude
        amplitude *= 0.55
    return total / weight


def puff_frame(size: int, rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray]:
    """One puff frame: a cluster of lobes with torn edges and holes inside.

    A single smooth disc is the trap here. Hundreds of discs stacked up read as a heap of grey
    balls however soft their falloff is, because each one keeps a recognisable round outline. The
    reference smoke sprites avoid that by being lumpy all the way through: several lobes of
    different sizes, a silhouette torn by noise, and holes punched through the body so that
    overlapping sprites interlock instead of piling up.
    """
    coordinates = np.mgrid[0:size, 0:size].astype(np.float32) + 0.5
    ys, xs = coordinates[0], coordinates[1]
    centre = size / 2.0

    # Metaball field: the union of a few lobes, so the outline is bumpy rather than round.
    field = np.zeros((size, size), dtype=np.float32)
    for _ in range(int(rng.integers(4, 8))):
        yaw = rng.random() * 2.0 * np.pi
        offset = rng.uniform(0.05, 0.30) * size
        lobe_x = centre + np.cos(yaw) * offset
        lobe_y = centre + np.sin(yaw) * offset
        radius = rng.uniform(0.26, 0.44) * size
        distance = np.sqrt((xs - lobe_x) ** 2 + (ys - lobe_y) ** 2) / radius
        field = np.maximum(field, np.clip(1.0 - distance, 0.0, 1.0) ** 1.1)

    low = value_noise(rng, size, 2)   # broad patches: the only detail that survives scaling down
    mid = value_noise(rng, size, 3)
    # Push the field around so the lobes melt into one another and their rims tear.
    field = np.clip(field + 0.34 * (mid - 0.5), 0.0, 1.0)
    # Near binary threshold: a plateau of opaque smoke with a narrow shoulder. A wide gradient turns
    # into mush once the GPU scales the sprite down to a handful of pixels.
    alpha = np.clip((field - 0.30) / 0.20, 0.0, 1.0)
    # Punch holes: without them the sprite is a solid shape and the overlap reads as a flat card.
    alpha *= np.clip(0.30 + 1.35 * mid, 0.0, 1.0)
    border = np.minimum(
        np.minimum(xs, size - xs), np.minimum(ys, size - ys)) / (size * 0.09)
    alpha = np.clip(alpha, 0.0, 1.0) * np.clip(border, 0.0, 1.0) ** 0.6
    # Mottling is what reads as volume: light and shadow across one puff, not a flat grey blob.
    mottle = 0.95 * (low - 0.5) + 0.35 * (mid - 0.5)
    brightness = np.clip(0.40 + 0.45 * np.clip(field, 0.0, 1.0) + mottle, 0.0, 1.0)
    return alpha.astype(np.float32), brightness.astype(np.float32)


def write_frames() -> None:
    PARTICLE_DIR.mkdir(parents=True, exist_ok=True)
    rng = np.random.default_rng(19818)
    alphas = []
    for index in range(FRAME_COUNT):
        alpha, brightness = puff_frame(FRAME_SIZE, rng)
        grey = np.clip(96.0 + brightness * 152.0, 96, 244).astype(np.uint8)
        tile = np.zeros((FRAME_SIZE, FRAME_SIZE, 4), dtype=np.uint8)
        tile[..., 0] = np.clip(grey.astype(np.int16) - 2, 0, 255).astype(np.uint8)
        tile[..., 1] = grey
        tile[..., 2] = np.clip(grey.astype(np.int16) + 6, 0, 255).astype(np.uint8)
        tile[..., 3] = (alpha * 255.0).astype(np.uint8)
        Image.fromarray(tile, "RGBA").save(PARTICLE_DIR / f"smoke_puff_{index}.png")
        alphas.append(alpha)
    stacked = np.stack(alphas)
    print(f"wrote {FRAME_COUNT} frames to {PARTICLE_DIR.relative_to(ROOT)}")
    print(f"alpha mean {stacked.mean():.3f}, "
          f"share above 0.8 {float((stacked > 0.8).mean()):.3f}")


def main() -> None:
    ITEM_OUT.parent.mkdir(parents=True, exist_ok=True)
    icon = canister_icon()
    icon.save(ITEM_OUT)
    print(f"wrote {ITEM_OUT.relative_to(ROOT)} {icon.size}")
    write_frames()


if __name__ == "__main__":
    main()