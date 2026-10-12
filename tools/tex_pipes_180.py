#!/usr/bin/env python3
"""
0.1.8.0: heavy, modern pipe textures (16x16, procedural, no dependencies).

Item / water pipes (atlas layout of their models): the centre 8x8 (4..11) is the junction
box; the four 4-pixel edge bands are the sides of the connection arms. On the left and right
bands the arm length runs along x; on the top and bottom bands it runs along y.

Fluid pipes are tinted by their material: pipe_side and the end caps are greyscale. The side
texture looks right in any 10x10 crop (the models use auto UVs 3..13).

Files (textures/block/pipe/):
  block_pipe_item, block_pipe_item_input, block_pipe_item_output,
  block_pipe_water, block_pipe_water_input,
  pipe_side, pipe_large, pipe_medium, pipe_small, pipe_tiny,
  block_pipe_ejector (new Ejector Pipe)
Usage: tex_pipes_180.py <repo root>
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import write_png  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
OUT = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/block/pipe')


def noise(x, y, seed):
    return ((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) & 7


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, f):
    return clamp((c[0] * f, c[1] * f, c[2] * f, c[3] if len(c) > 3 else 255))


STEEL = (104, 110, 118, 255)
STEEL_DARK = (60, 64, 71, 255)
EDGE = (36, 39, 44, 255)
BOLT = (176, 182, 188, 255)


def junction_pipe(accent, stripe, chevron=0, seed=1):
    """chevron: 0 none, +1 arrows outward (output), -1 arrows inward (input)."""
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            centre = 4 <= x <= 11 and 4 <= y <= 11
            if centre:
                c = STEEL_DARK
                if x in (4, 11) or y in (4, 11):
                    c = EDGE
                elif x == 5 or y == 5:
                    c = shade(STEEL_DARK, 1.25)          # bevel light
                elif x == 10 or y == 10:
                    c = shade(STEEL_DARK, 0.8)
                if (x, y) in ((5, 5), (10, 5), (5, 10), (10, 10)):
                    c = BOLT
                if 7 <= x <= 8 and 7 <= y <= 8:
                    c = accent                            # status light
                px[y][x] = c
                continue
            # arm bands: along = coordinate along the arm length, across = across the arm
            horizontal_band = 4 <= y <= 11                 # left/right bands: length along x
            along = x if horizontal_band else y
            across = y if horizontal_band else x
            dist = along if along < 4 else 15 - along      # 0 at the block edge, 3 next to the junction
            c = shade(STEEL, 0.92 + 0.03 * noise(x, y, seed))
            if across in (4, 11):
                c = EDGE                                   # tube silhouette
            elif across == 5:
                c = shade(STEEL, 1.3)                      # highlight along the tube
            elif across == 10:
                c = shade(STEEL, 0.72)
            if across in (7, 8) and stripe:
                c = stripe                                 # content stripe along the tube
            if dist == 0:
                c = shade(STEEL, 0.62)                     # coupling flange at the block edge
            elif dist == 3:
                c = shade(STEEL, 0.78)                     # seam at the junction
            if chevron and 5 < across < 10 and dist in (1, 2):
                # arrow tip pointing out of (or into) the junction
                tip = 1 if (chevron > 0) == (dist == 1) else 2
                if dist == tip and across in (6, 9) or dist != tip and across in (7, 8):
                    c = accent
            if not (4 <= across <= 11):
                c = (0, 0, 0, 0)                           # outside the tube: transparent (cutout)
            px[y][x] = c
    return px


def pipe_side():
    px = [[(0, 0, 0, 255)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            v = 150 + 10 * math.sin(y * 0.9) + 4 * noise(x, y, 7)
            if y in (2, 13):
                v = 105                                    # flange seams
            elif y in (3, 12):
                v = 190
            elif y in (6, 7):
                v += 30                                    # highlight
            px[y][x] = (v, v, v, 255)
    return px


def cap(bore):
    px = [[(0, 0, 0, 255)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            v = 160 + 3 * noise(x, y, 3)
            if x == 0 or y == 0:
                v = 210                                    # bevel light
            elif x == 15 or y == 15:
                v = 90                                     # bevel dark
            if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
                v = 220                                    # bolts
            if d < bore + 1.2:
                v = 120                                    # rim ring
            if d < bore + 0.4:
                v = 200 if d > bore - 0.4 and x + y < 15 else 150
            if d < bore - 0.4:
                v = 28 + 30 * (d / max(bore, 0.1))         # dark bore with depth
            px[y][x] = (v, v, v, 255)
    return px


def ejector():
    """Ejector pipe: steel body, orange-black hazard nozzle ring, bright bore."""
    px = [[(0, 0, 0, 255)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            c = shade(STEEL, 0.9 + 0.03 * noise(x, y, 11))
            if x in (0, 15) or y in (0, 15):
                c = EDGE
            # hazard stripes on the outer ring
            d = max(abs(x - 7.5), abs(y - 7.5))
            if 4.5 <= d <= 6.5:
                c = (230, 140, 30, 255) if (x + y) % 4 < 2 else (30, 30, 34, 255)
            if d < 4.5:
                c = shade(STEEL_DARK, 1.0)
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 2.6:
                c = (255, 190, 90, 255) if r < 1.2 else (140, 80, 30, 255)
            px[y][x] = c
    return px


def main():
    os.makedirs(OUT, exist_ok=True)
    amber, green, orange, blue = (255, 196, 60, 255), (90, 220, 110, 255), (255, 130, 40, 255), (70, 170, 255, 255)
    files = {
        'block_pipe_item': junction_pipe(amber, (150, 120, 60, 255), 0, 1),
        'block_pipe_item_input': junction_pipe(green, (60, 140, 80, 255), -1, 2),
        'block_pipe_item_output': junction_pipe(orange, (150, 90, 40, 255), +1, 3),
        'block_pipe_water': junction_pipe(blue, (60, 120, 190, 255), 0, 4),
        'block_pipe_water_input': junction_pipe(green, (60, 120, 190, 255), -1, 5),
        'pipe_side': pipe_side(),
        'pipe_large': cap(5.8),
        'pipe_medium': cap(4.4),
        'pipe_small': cap(3.0),
        'pipe_tiny': cap(1.8),
        'block_pipe_ejector': ejector(),
    }
    for name, px in files.items():
        write_png(os.path.join(OUT, name + '.png'), [[clamp(c) for c in row] for row in px])
    print('wrote', len(files), 'pipe textures')


if __name__ == '__main__':
    main()
