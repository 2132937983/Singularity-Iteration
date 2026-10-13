#!/usr/bin/env python3
"""
0.1.9.0: item textures of the five frame cores (pure Python).

  item/module/item_core_<id>.png   16x16: an octagonal graphite cartridge with the core at its
                                   centre and the emblem of the core around it

  solar_array_1..3   gold sun core, blue cell petals in the corners, 1..3 tier pips
  graviton           violet singularity in a turned square ring
  propulsion         cyan reactor with twin nozzle chevrons

Usage: tex_core_190.py <repo root> [--preview <png>]
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import write_png  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
OUT = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item/module')

CLEAR = (0, 0, 0, 0)
OUTLINE = (16, 19, 24, 255)
WHITE = (255, 255, 255, 255)


def col(rgb, a=255):
    return ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a)


def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3)) + (max(a[3], b[3]),)


def inside(x, y, cut=3, lo=1, hi=14):
    if x < lo or x > hi or y < lo or y > hi:
        return False
    dx = min(x - lo, hi - x)
    dy = min(y - lo, hi - y)
    return dx + dy >= cut


def casing():
    px = [[CLEAR] * 16 for _ in range(16)]
    top, bottom = col(0x4A5464), col(0x232932)
    for y in range(16):
        for x in range(16):
            if not inside(x, y):
                continue
            if not inside(x, y, 3, 2, 13):
                px[y][x] = OUTLINE
                continue
            t = (x + y) / 30
            c = mix(top, bottom, t)
            # bevel: light upper-left rim, dark lower-right rim
            if not inside(x, y, 3, 3, 12):
                c = mix(c, WHITE, 0.22) if x + y < 15 else mix(c, OUTLINE, 0.45)
            px[y][x] = c
    # socket well
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 4.2 and px[y][x] != CLEAR:
                px[y][x] = mix(col(0x101318), col(0x1C222A), max(0, (d - 3.0) / 1.2))
    return px


def core_disc(px, rgb, radius=2.7):
    c = col(rgb)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < radius:
                shade = mix(mix(c, WHITE, 0.45), c, min(1, d / radius)) if (x + y) < 15 else mix(c, OUTLINE, 0.25 * d / radius)
                px[y][x] = shade
            elif d < radius + 0.9 and px[y][x] != CLEAR:
                px[y][x] = mix(px[y][x], c, 0.45)
    px[6][6] = WHITE


def solar(tier):
    px = casing()
    cell, line = col(0x1E4E9A), col(0x6FA8FF)
    # cell petals in the four bevel corners
    for (cx, cy) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        for y in range(cy - 1, cy + 1):
            for x in range(cx - 1, cx + 1):
                px[y][x] = cell
        px[cy - 1][cx - 1] = line
    core_disc(px, 0xFFC400 if tier < 3 else 0xFFAB00, 2.6 + 0.2 * tier)
    # sun rays through the well
    ray = col(0xFFE57F)
    for (x, y) in ((7, 3), (8, 3), (12, 7), (12, 8), (3, 7), (3, 8)):
        px[y][x] = ray
    # tier pips on the lower rim
    for i in range(tier):
        x = 8 - tier + i * 2
        px[13][x] = col(0xFFD740)
    return px


def graviton():
    px = casing()
    ring = col(0xB388FF)
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if 4.6 < d < 5.6 and inside(x, y, 3, 2, 13):
                px[y][x] = mix(ring, WHITE, 0.25) if y < 8 else ring
    core_disc(px, 0x7C4DFF, 2.4)
    px[7][7] = col(0xEDE7FF)
    return px


def propulsion():
    px = casing()
    core_disc(px, 0x4FC3F7, 2.6)
    flame = col(0x80D8FF)
    hot = col(0xE1F5FE)
    # twin nozzle chevrons below the reactor
    for (x, y, c) in ((5, 11, flame), (6, 12, hot), (4, 12, flame), (10, 11, flame), (9, 12, hot), (11, 12, flame)):
        px[y][x] = c
    # intake slits above
    for x in (5, 6, 9, 10):
        px[3][x] = mix(col(0x4FC3F7), OUTLINE, 0.35)
    return px


ICONS = {
    'solar_array_1': lambda: solar(1),
    'solar_array_2': lambda: solar(2),
    'solar_array_3': lambda: solar(3),
    'graviton': graviton,
    'propulsion': propulsion,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    sheet = []
    for cid, fn in ICONS.items():
        px = fn()
        write_png(os.path.join(OUT, 'item_core_' + cid + '.png'), px)
        sheet.append(px)
    if '--preview' in sys.argv:
        out = sys.argv[sys.argv.index('--preview') + 1]
        S = 12
        img = [[(30, 34, 40, 255)] * (len(sheet) * 16 * S + (len(sheet) + 1) * 8) for _ in range(16 * S + 16)]
        for i, px in enumerate(sheet):
            ox = 8 + i * (16 * S + 8)
            for y in range(16 * S):
                for x in range(16 * S):
                    p = px[y // S][x // S]
                    if p[3]:
                        img[8 + y][ox + x] = p
        write_png(out, img)
    print('wrote', len(ICONS), 'core icons')


if __name__ == '__main__':
    main()
