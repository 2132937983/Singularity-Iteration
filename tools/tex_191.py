#!/usr/bin/env python3
"""
0.1.9.1: item textures (pure Python).

  item/upgrade/thermal_retention_upgrade.png  the IC2 upgrade board (from the overclocker) with a
                                              flame held in an amber insulation bracket
  item/normal/item_recovered_parts.png        a steel parts case with a cyan latch and a gear

Usage: tex_191.py <repo root> [--preview <png>]
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import read_png, write_png  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
TEX = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item')
CLEAR = (0, 0, 0, 0)


def col(rgb, a=255):
    return ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a)


def thermal():
    w, h, base = read_png(os.path.join(TEX, 'upgrade/overclocker_upgrade.png'))
    px = [list(row) for row in base]
    plate = px[4][4] if px[4][4][3] else (168, 168, 168, 255)
    shade = px[11][11] if px[11][11][3] else (140, 140, 140, 255)
    # clear the overclocker arrow: plate grey with the bevel of the original
    for y in range(4, 12):
        for x in range(4, 12):
            p = px[y][x]
            if p[2] > p[0] + 30:                          # the blue arrow
                px[y][x] = plate if (x + y) < 16 else shade
    amber, amber_d = col(0xE0A030), col(0x9A6418)
    # insulation bracket: a U around the flame
    for y in range(6, 12):
        px[y][5] = amber_d if y > 9 else amber
        px[y][10] = amber_d if y > 9 else amber
    for x in range(5, 11):
        px[11][x] = amber_d
    # flame
    outer, mid, core = col(0xE2461C), col(0xF59A23), col(0xFFF2B0)
    flame = {(7, 4): outer, (8, 5): outer, (6, 6): outer, (7, 5): mid, (8, 6): mid, (9, 7): outer, (6, 7): outer,
             (7, 6): mid, (7, 7): core, (8, 7): mid, (6, 8): outer, (7, 8): core, (8, 8): core, (9, 8): outer,
             (6, 9): outer, (7, 9): mid, (8, 9): mid, (9, 9): outer, (7, 10): outer, (8, 10): outer}
    for (x, y), c in flame.items():
        px[y][x] = c
    return px


def case():
    px = [[CLEAR] * 16 for _ in range(16)]
    dark, body, light, edge = col(0x2A2F36), col(0x5D6670), col(0x8E99A4), col(0x16191E)
    for y in range(3, 15):
        for x in range(1, 15):
            border = x in (1, 14) or y in (3, 14)
            px[y][x] = edge if border else (light if y == 4 else body if y < 9 else col(0x4E5660))
    # lid seam and ribs
    for x in range(2, 14):
        px[8][x] = dark
    for y in range(9, 14):
        px[y][4] = dark
        px[y][11] = dark
    # handle
    for x in range(5, 11):
        px[1][x] = edge
    px[2][5] = edge
    px[2][10] = edge
    # cyan latch
    cy = col(0x40D8F0)
    for x in range(7, 9):
        px[7][x] = cy
        px[8][x] = cy
        px[9][x] = col(0x1C8FB3)
    # gear emblem on the lid
    g = col(0xC9D2DB)
    for (x, y) in ((6, 5), (9, 5), (7, 4), (8, 4), (7, 6), (8, 6), (5, 5), (10, 5)):
        px[y][x] = g
    return px


ICONS = {'upgrade/thermal_retention_upgrade': thermal, 'normal/item_recovered_parts': case}


def main():
    sheet = []
    for name, fn in ICONS.items():
        px = fn()
        write_png(os.path.join(TEX, name + '.png'), px)
        sheet.append(px)
    if '--preview' in sys.argv:
        out = sys.argv[sys.argv.index('--preview') + 1]
        S = 12
        img = [[(30, 34, 40, 255)] * (len(sheet) * (16 * S + 8) + 8) for _ in range(16 * S + 16)]
        for i, px in enumerate(sheet):
            ox = 8 + i * (16 * S + 8)
            for y in range(16 * S):
                for x in range(16 * S):
                    p = px[y // S][x // S]
                    if p[3]:
                        img[8 + y][ox + x] = p
        write_png(out, img)
    print('wrote', len(ICONS), 'icons')


if __name__ == '__main__':
    main()
