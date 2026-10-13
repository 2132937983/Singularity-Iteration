#!/usr/bin/env python3
"""
0.1.8.5: textures of the nano suit units (pure Python).

  item/module/item_module_<id>.png       16x16 nano unit cells: a hexagonal graphite cell with
                                         a cyan lattice rim (quantum units are square chips), symbol
                                         in the unit colour
  item/module/item_nano_lattice_densifier.png  amber lattice crystal in a clamp
  effect/nano_hologrid.png               64x64 hologram grid for the energy-swirl render type
                                         (lines with alpha, transparent cells)
  mob_effect/emp_stun.png                18x18 effect icon (broken circuit with a spark)

Usage: tex_nano_185.py <repo root>
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import write_png  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
TEX = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures')

CLEAR = (0, 0, 0, 0)
DARK = (22, 28, 34, 255)
BODY = (40, 48, 58, 255)
BODY_L = (64, 76, 90, 255)
RIM = (70, 210, 240, 255)
RIM_D = (32, 110, 140, 255)
WHITE = (236, 252, 255, 255)


def blank(w=16, h=16):
    return [[CLEAR for _ in range(w)] for _ in range(h)]


def inside_hex(x, y):
    # flat-top hexagon in a 16x16 cell: |dy| <= 6.5 and |dx| + |dy| * 0.55 <= 7.6
    dx, dy = x - 7.5, y - 7.5
    return abs(dy) <= 6.6 and abs(dx) + abs(dy) * 0.55 <= 7.7


def cell():
    px = blank()
    for y in range(16):
        for x in range(16):
            if not inside_hex(x, y):
                continue
            edge = any(not inside_hex(x + ox, y + oy) for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                px[y][x] = RIM if (x + y) % 3 else RIM_D
            else:
                px[y][x] = BODY if y > 7 else BODY_L
    # lattice nodes on the rim corners
    for (x, y) in ((4, 1), (11, 1), (4, 14), (11, 14), (0, 7), (15, 7)):
        if 0 <= x < 16 and 0 <= y < 16 and px[y][x][3]:
            px[y][x] = WHITE
    # inner dark field for the symbol
    for y in range(4, 12):
        for x in range(4, 12):
            if px[y][x][3] and px[y][x] not in (RIM, RIM_D, WHITE):
                px[y][x] = DARK
    return px


def col(rgb, a=255):
    return (rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, a)


def mix(c, d, t):
    return tuple(int(c[i] + (d[i] - c[i]) * t) for i in range(3)) + (255,)


def myofiber():
    px = cell()
    c, hi = col(0x3DFFB4), mix(col(0x3DFFB4), WHITE, 0.5)
    # three braided fibres
    for x in range(4, 12):
        for k, off in enumerate((0, 2.1, 4.2)):
            y = 7.5 + 2.2 * math.sin((x - 4) * 0.9 + off)
            px[int(round(y))][x] = hi if k == 1 else c
    # dash chevron
    for (x, y) in ((9, 4), (10, 5), (9, 6)):
        px[y][x] = WHITE
    return px


def thermoregulator():
    px = cell()
    hot, cold = col(0xFF8A3D), col(0x6FE6FF)
    for y in range(4, 12):
        t = (y - 4) / 7
        for x in (5, 7, 9):
            px[y][x] = mix(hot, cold, t)
    for x in range(4, 11):
        px[11][x] = cold
    px[4][6] = px[4][8] = mix(hot, WHITE, 0.4)
    return px


def emp_emitter():
    px = cell()
    c = col(0x6FA8FF)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if abs(d - 3.3) < 0.55:
                px[y][x] = c
    for (x, y) in ((8, 4), (7, 5), (8, 6), (7, 7), (8, 8), (7, 9), (8, 10)):
        px[y][x] = WHITE
    return px


def optical_camo():
    px = cell()
    c = col(0xBFF6FF)
    # a half-faded figure: solid on the left, dotted (grid) on the right
    figure = ((7, 4), (8, 4), (7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (7, 8), (8, 8),
              (6, 9), (9, 9), (6, 10), (9, 10))
    for (x, y) in figure:
        if x <= 7:
            px[y][x] = c
        elif (x + y) % 2 == 0:
            px[y][x] = mix(c, DARK, 0.4)
    for y in range(4, 12, 2):
        px[y][11] = RIM_D
    return px


def toxin_filter():
    px = cell()
    c, d = col(0x9CFF57), mix(col(0x9CFF57), DARK, 0.5)
    for y in range(4, 12):
        for x in range(4, 12):
            if (x - 4) % 2 == 0 and 5 <= y <= 10:
                px[y][x] = d
    for x in range(4, 12):
        px[5][x] = c
        px[10][x] = c
    px[7][7] = px[8][8] = WHITE
    return px


def micro_capacitor():
    px = cell()
    c, hi = col(0xFFD45A), mix(col(0xFFD45A), WHITE, 0.5)
    for y in range(5, 11):
        px[y][6] = c
        px[y][9] = c
    for x in range(4, 6):
        px[7][x] = hi
        px[8][x] = hi
    for x in range(10, 12):
        px[7][x] = hi
        px[8][x] = hi
    for (x, y) in ((7, 6), (8, 7), (7, 8), (8, 9)):
        px[y][x] = mix(c, DARK, 0.3)
    return px


def densifier():
    px = blank()
    amber, amber_d, steel = col(0xFFC44A), col(0xB07A18), (118, 128, 140, 255)
    # crystal: elongated hexagonal prism
    for y in range(2, 14):
        half = 3 if 4 <= y <= 11 else (2 if y in (3, 12) else 1)
        for x in range(8 - half, 8 + half):
            shade = amber if x < 8 else amber_d
            if (x + y) % 4 == 0:
                shade = mix(shade, WHITE, 0.45)
            px[y][x] = shade
    # lattice lines in the crystal
    for y in range(4, 12, 3):
        for x in range(5, 11):
            if px[y][x][3]:
                px[y][x] = mix(px[y][x], WHITE, 0.6)
    # clamp
    for x in range(3, 13):
        px[13][x] = steel
        px[14][x] = (78, 86, 96, 255)
    for y in range(10, 14):
        px[y][3] = steel
        px[y][12] = steel
    return px


def hologrid():
    W = 64
    px = [[CLEAR for _ in range(W)] for _ in range(W)]
    for y in range(W):
        for x in range(W):
            line_v = x % 8 == 0
            line_h = y % 8 == 0
            node = line_v and line_h
            diag = (x + y) % 16 == 0 and (x % 8 == 4 or y % 8 == 4)
            if node:
                px[y][x] = (255, 255, 255, 255)
            elif line_v or line_h:
                px[y][x] = (140, 235, 255, 255)
            elif diag:
                px[y][x] = (90, 190, 230, 255)
    # a few brighter scan rows
    for y in (20, 21, 44):
        for x in range(W):
            if px[y][x][3] == 0 and x % 2 == 0:
                px[y][x] = (110, 220, 255, 255)
    return px


def emp_stun_icon():
    px = [[CLEAR for _ in range(18)] for _ in range(18)]
    c, d, spark = (111, 168, 255, 255), (40, 70, 130, 255), (240, 248, 255, 255)
    # circuit trace broken in the middle
    for x in range(2, 8):
        px[9][x] = c
    for x in range(10, 16):
        px[8][x] = c
    for y in range(4, 9):
        px[y][3] = d
    for y in range(9, 14):
        px[y][14] = d
    for (x, y) in ((3, 3), (14, 14), (2, 9), (15, 8)):
        px[y][x] = spark
    # spark bolt across the gap
    for (x, y) in ((9, 4), (8, 5), (9, 6), (8, 7), (9, 8), (8, 9), (9, 10), (8, 11), (9, 12)):
        px[y][x] = spark
    return px


def main():
    mod = os.path.join(TEX, 'item/module')
    for name, fn in (('myofiber', myofiber), ('thermoregulator', thermoregulator), ('emp_emitter', emp_emitter),
                     ('optical_camo', optical_camo), ('toxin_filter', toxin_filter), ('micro_capacitor', micro_capacitor)):
        write_png(os.path.join(mod, 'item_module_' + name + '.png'), fn())
    write_png(os.path.join(mod, 'item_nano_lattice_densifier.png'), densifier())
    os.makedirs(os.path.join(TEX, 'effect'), exist_ok=True)
    write_png(os.path.join(TEX, 'effect/nano_hologrid.png'), hologrid())
    os.makedirs(os.path.join(TEX, 'mob_effect'), exist_ok=True)
    write_png(os.path.join(TEX, 'mob_effect/emp_stun.png'), emp_stun_icon())
    print('wrote nano textures')


if __name__ == '__main__':
    main()
