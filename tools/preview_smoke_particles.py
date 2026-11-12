"""Offline density check for the smoke puff field.

Renders what the client will draw: a few hundred billboard sprites scattered through the cloud's
cylinder, alpha blended in painter order over a sky and a ground plane, with a tank sized box for
scale. Use it to judge density before launching the game.

    python3 tools/preview_smoke_particles.py
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
FRAME_DIR = ROOT / "src/main/resources/assets/battlefield/textures/particle"

WIDTH, HEIGHT = 900, 620
FOV_DEGREES = 70.0
CAMERA_DISTANCE = 26.0
CAMERA_HEIGHT = 3.2
EYE_TARGET_Y = 3.4
CLOUD_RADIUS = 6.0
PUFF_SIZE = float(sys.argv[1]) if len(sys.argv) > 1 else 1.2
PUFF_COUNT = int(sys.argv[2]) if len(sys.argv) > 2 else 300
OUT_NAME = sys.argv[3] if len(sys.argv) > 3 else "smoke_particle_preview.png"
PEAK_ALPHA = float(sys.argv[4]) if len(sys.argv) > 4 else 1.0
SEED = 7
OUT = ROOT / "build" / OUT_NAME


def background() -> Image.Image:
    image = Image.new("RGB", (WIDTH, HEIGHT), (128, 160, 196))
    draw = ImageDraw.Draw(image)
    # Sky gradient.
    for y in range(HEIGHT // 2):
        t = y / (HEIGHT / 2)
        draw.line([(0, y), (WIDTH, y)],
                  fill=(int(120 + 60 * t), int(152 + 58 * t), int(190 + 46 * t)))
    horizon = HEIGHT // 2
    for y in range(horizon, HEIGHT):
        t = (y - horizon) / (HEIGHT - horizon)
        draw.line([(0, y), (WIDTH, y)],
                  fill=(int(96 - 40 * t), int(104 - 42 * t), int(84 - 34 * t)))
    return image


def project(point: tuple[float, float, float]) -> tuple[float, float, float]:
    """Camera at -z looking at the cloud: returns screen x, y and depth."""
    focal = (WIDTH * 0.5) / math.tan(math.radians(FOV_DEGREES) * 0.5)
    x, y, z = point
    depth = z + CAMERA_DISTANCE
    if depth <= 0.1:
        return 0.0, 0.0, -1.0
    return WIDTH * 0.5 + x * focal / depth, HEIGHT * 0.5 - (y - CAMERA_HEIGHT) * focal / depth, depth


def main() -> None:
    frames = [Image.open(FRAME_DIR / f"smoke_puff_{i}.png").convert("RGBA")
              for i in range(16)]
    rng = np.random.default_rng(SEED)
    base = background().convert("RGBA")

    sprites = []
    for _ in range(PUFF_COUNT):
        yaw = rng.random() * math.tau
        horizontal = CLOUD_RADIUS * 0.92 * math.sqrt(rng.random())
        position = (math.cos(yaw) * horizontal,
                    rng.random() * CLOUD_RADIUS * 1.15 - 0.10,
                    math.sin(yaw) * horizontal)
        size = PUFF_SIZE * (0.62 + rng.random() * 0.55)
        # Same fade curve the particle uses.
        life = rng.random()
        fade = PEAK_ALPHA * min(1.0, min(life / 0.18, (1.0 - life) / 0.32))
        sprites.append((position, size, int(rng.integers(0, 16)), max(0.0, fade)))

    sprites.sort(key=lambda item: project(item[0])[2], reverse=True)

    # Tank sized box for scale, drawn behind the smoke only partially: it stands at the centre.
    tank = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    tank_draw = ImageDraw.Draw(tank)
    half_w, height, depth = 1.6, 1.5, 3.5
    corners = []
    for sign_x in (-1, 1):
        for sign_z in (-1, 1):
            for y in (0.0, height):
                corners.append(project((sign_x * half_w, y, sign_z * depth)))
    xs = [c[0] for c in corners]
    ys = [c[1] for c in corners]
    tank_draw.rectangle([min(xs), min(ys), max(xs), max(ys)], fill=(58, 64, 52, 255))

    composed = Image.alpha_composite(base, tank)
    for position, size, frame_index, fade in sprites:
        sx, sy, depth = project(position)
        if depth <= 0.5:
            continue
        focal = (WIDTH * 0.5) / math.tan(math.radians(FOV_DEGREES) * 0.5)
        pixels = max(2, int(size * focal / depth))
        sprite = frames[frame_index].resize((pixels, pixels), Image.BILINEAR)
        if fade < 1.0:
            alpha = sprite.getchannel("A").point(lambda a, f=fade: int(a * f))
            sprite.putalpha(alpha)
        composed.alpha_composite(sprite, (int(sx - pixels / 2), int(sy - pixels / 2)))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    composed.convert("RGB").save(OUT)
    print(f"wrote {OUT.relative_to(ROOT)} with {len(sprites)} puffs")


if __name__ == "__main__":
    main()
