#!/usr/bin/env python3
"""
0.1.10.31: generates the textures of plan B (no hand-drawn art).

  textures/effect/thruster_flame.png  64 x 64: four flame frames side by side, each 16 wide (across the flame) and 64 long (v = 0
                                      at the nozzle, 1 at the tip). Grey light for an additive render type (RGB = brightness,
                                      the vertex colour gives the hue): a hot core that narrows to the tip, a soft sheath, three
                                      shock bands and turbulence that flows outward; the four frames loop.
  textures/gui/visor/rim.png          256 x 256: the dark rim of the visor glass (alpha only at the edge).
  textures/gui/visor/fringe.png       256 x 256: colour fringes in the corners (cyan / violet).
  textures/gui/visor/streak.png       32 x 256: a soft reflection streak.
  textures/gui/visor/drop.png         32 x 32: a raindrop (dark rim, bright highlight).
  textures/gui/visor/crack.png        256 x 256: cracks from the lower left corner (white lines, seeded).

Deterministic: the same files every run. With --check it compares with the files on disk and writes nothing.
Usage: thrust_textures_1131.py <repo root> [--check]
"""
import math
import os
import random
import sys

from PIL import Image


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def flame():
    w, h, frames = 16, 64, 4
    img = Image.new('RGBA', (w * frames, h), (0, 0, 0, 255))
    px = img.load()
    for f in range(frames):
        phase = f / frames * 2 * math.pi
        for y in range(h):
            v = (y + 0.5) / h
            # width of the core and of the sheath shrink towards the tip
            core_w = 0.20 * (1 - 0.75 * v)
            sheath_w = 0.42 * (1 - 0.45 * v)
            along = (1 - v) ** 1.6
            shock = 1 + 0.35 * sum(math.exp(-((v - c) / 0.035) ** 2) for c in (0.22, 0.41, 0.58))
            for x in range(w):
                u = (x + 0.5) / w - 0.5
                turb = 0.78 + 0.22 * math.sin(v * 21 - phase * 2 + math.sin(u * 9 + phase) * 1.3) \
                    * (0.6 + 0.4 * math.sin(v * 9 + u * 13 - phase))
                core = math.exp(-(u / core_w) ** 2) if core_w > 0 else 0
                sheath = math.exp(-(u / sheath_w) ** 2) * 0.45
                i = (core + sheath) * along * shock * turb
                i *= smooth(v / 0.04)                       # soft start at the nozzle lip
                g = max(0, min(255, int(round(255 * i))))
                px[f * w + x, y] = (g, g, g, 255)
    return img


def rim():
    n = 256
    img = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    px = img.load()
    for y in range(n):
        for x in range(n):
            u, v = (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1
            # rounded rectangle distance (superellipse), 1 at the edge
            d = (abs(u) ** 6 + abs(v) ** 6) ** (1 / 6)
            a = smooth((d - 0.72) / 0.28)
            px[x, y] = (8, 16, 24, int(round(255 * a)))
    return img


def fringe():
    n = 256
    img = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    px = img.load()
    for y in range(n):
        for x in range(n):
            u, v = (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1
            corner = smooth((abs(u) * abs(v) - 0.45) / 0.5)
            cyan = (u * v) < 0
            rgb = (60, 220, 255) if cyan else (190, 110, 255)
            px[x, y] = rgb + (int(round(255 * corner)),)
    return img


def streak():
    w, h = 32, 256
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        v = (y + 0.5) / h
        along = math.sin(math.pi * v) ** 1.5
        for x in range(w):
            u = (x + 0.5) / w - 0.5
            a = math.exp(-(u / 0.22) ** 2) * along + 0.5 * math.exp(-((u - 0.28) / 0.05) ** 2) * along
            px[x, y] = (235, 245, 255, max(0, min(255, int(round(255 * a)))))
    return img


def drop():
    n = 32
    img = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    px = img.load()
    for y in range(n):
        for x in range(n):
            u, v = (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1
            r = math.sqrt(u * u + v * v)
            if r > 1:
                continue
            edge = smooth((r - 0.6) / 0.4)
            body = 0.18 + 0.5 * edge
            hi = math.exp(-(((u + 0.35) / 0.18) ** 2 + ((v + 0.38) / 0.14) ** 2))
            shade = int(round(40 + 60 * (1 - edge) + 155 * hi))
            a = min(1.0, body + hi) * smooth((1 - r) / 0.12)
            px[x, y] = (shade, min(255, shade + 10), min(255, shade + 25), int(round(255 * a)))
    return img


def crack():
    n = 256
    img = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(1131)

    def plot(x, y, a):
        xi, yi = int(x), int(y)
        if 0 <= xi < n and 0 <= yi < n:
            old = px[xi, yi][3]
            px[xi, yi] = (240, 250, 255, max(old, a))

    def branch(x, y, angle, length, width_a, depth):
        for _ in range(length):
            angle += rnd.uniform(-0.18, 0.18)
            x += math.cos(angle) * 0.8
            y += math.sin(angle) * 0.8
            plot(x, y, width_a)
            plot(x + 1, y, width_a // 2)
            if depth < 3 and rnd.random() < 0.018:
                branch(x, y, angle + rnd.choice((-1, 1)) * rnd.uniform(0.5, 1.1), int(length * 0.45), int(width_a * 0.75), depth + 1)
            if x < 0 or y < 0 or x >= n or y >= n:
                return

    for k in range(5):
        branch(4, n - 4, -math.pi / 2 + k * 0.32 + rnd.uniform(-0.1, 0.1), 180 + rnd.randint(0, 100), 230, 0)
    return img


def main():
    root = sys.argv[1]
    check = '--check' in sys.argv[2:]
    a = os.path.join(root, 'src/main/resources/assets/mio_icif/textures')
    files = {
        'effect/thruster_flame.png': flame(),
        'gui/visor/rim.png': rim(),
        'gui/visor/fringe.png': fringe(),
        'gui/visor/streak.png': streak(),
        'gui/visor/drop.png': drop(),
        'gui/visor/crack.png': crack(),
    }
    bad = 0
    for rel, img in files.items():
        path = os.path.join(a, rel)
        if check:
            old = Image.open(path).convert('RGBA') if os.path.exists(path) else None
            same = old is not None and old.size == img.size and old.tobytes() == img.tobytes()
            print(rel, 'same' if same else 'DIFFERENT')
            bad += 0 if same else 1
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path, optimize=True)
        print('wrote', rel)
    if check and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
