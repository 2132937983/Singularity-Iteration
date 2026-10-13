#!/usr/bin/env python3
"""
0.1.8.6: textures of the nano suit phase 2 (pure Python).

  item/module/item_module_<id>.png       the 11 new nano unit cells (same hexagonal cell as 0.1.8.5)
  item/module/item_module_<id>_mk2.png   Mk.II of every nano unit: violet-gold rim and two rank pips
  effect/nano_hexmesh.png                64x64 hexagonal plating for the kinetic armor mesh

Usage: tex_nano_186.py <repo root>
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import read_png, write_png  # noqa: E402
import tex_nano_185 as base  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
TEX = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures')
MOD = os.path.join(TEX, 'item/module')

cell, col, mix = base.cell, base.col, base.mix
WHITE, DARK = base.WHITE, base.DARK


def gecko_setae():
    px = cell()
    c = col(0x9BE15D)
    # a foot with spread toes and fine setae
    for (x, y) in ((7, 9), (8, 9), (7, 10), (8, 10), (6, 8), (9, 8)):
        px[y][x] = c
    for (x0, y0, dx, dy) in ((6, 8, -1, -1), (9, 8, 1, -1), (7, 8, 0, -1), (8, 8, 0, -1)):
        x, y = x0, y0
        for _ in range(3):
            x += dx; y += dy
            if 4 <= x <= 11 and 4 <= y <= 11:
                px[y][x] = c
        if 4 <= x <= 11 and 4 <= y <= 11:
            px[y][x] = WHITE
    return px


def shock_gel():
    px = cell()
    c, hi = col(0x4FC3F7), mix(col(0x4FC3F7), WHITE, 0.5)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot((x - 7.5) / 3.6, (y - 8.5) / 2.6)
            if d < 1:
                px[y][x] = hi if (x - 7.5) + (y - 8.5) < -2 else c
    for x in range(5, 11):
        px[11][x] = mix(c, DARK, 0.4)
    px[6][6] = WHITE
    return px


def heat_sink_fins():
    px = cell()
    c = col(0xFF6E40)
    for x in (5, 7, 9):
        for y in range(4, 11):
            px[y][x] = mix(c, WHITE, 0.25) if y < 6 else c
    for x in range(4, 12):
        px[11][x] = mix(c, DARK, 0.3)
    return px


def thermal_camo():
    px = cell()
    c = col(0x9575CD)
    figure = ((7, 4), (8, 4), (7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (7, 8), (8, 8), (6, 9), (9, 9))
    for (x, y) in figure:
        px[y][x] = mix(c, DARK, 0.15 * ((x + y) % 3))
    for x in range(4, 12):            # cold band across the body
        px[7][x] = mix(col(0x6FE6FF), DARK, 0.2)
    return px


def jammer():
    px = cell()
    c = col(0xE040FB)
    for x in range(4, 12):
        y = 7.5 + 2.5 * math.sin(x * 1.9) * (1 if x % 2 else -0.6)
        px[max(4, min(11, int(round(y))))][x] = c
    for (x, y) in ((5, 5), (10, 10), (10, 5), (5, 10)):
        px[y][x] = WHITE
    return px


def recon_cloud():
    px = cell()
    c = col(0x64FFDA)
    pts = ((5, 8), (6, 6), (7, 9), (8, 5), (8, 7), (9, 10), (10, 6), (10, 8), (6, 10), (11, 7), (4, 7), (7, 4))
    for i, (x, y) in enumerate(pts):
        px[y][x] = WHITE if i % 4 == 0 else c
    return px


def kinetic_mesh():
    px = cell()
    c, d = col(0xFFB300), mix(col(0xFFB300), DARK, 0.45)
    for y in range(4, 12):
        for x in range(4, 12):
            q, r = (x - 4) % 4, (y - 4) % 4
            if (q == 0 and r in (1, 2)) or (r == 0 and q in (1, 2)) or (q == 3 and r == 3):
                px[y][x] = c
            elif (x + y) % 2 == 0:
                px[y][x] = d
    px[7][7] = px[8][8] = WHITE
    return px


def self_repair():
    px = cell()
    c = col(0x69F0AE)
    for x in range(5, 11):
        px[8][x] = c
    for y in range(5, 11):
        px[y][8] = c
    px[8][8] = WHITE
    for (x, y) in ((5, 5), (10, 11), (11, 5)):
        if 4 <= y <= 11:
            px[y][x] = mix(c, WHITE, 0.5)
    return px


def hydrophobic():
    px = cell()
    c, hi = col(0x40C4FF), WHITE
    drops = (((6, 6), 1.6), ((9, 9), 1.4), ((9, 5), 1.0))
    for (cx, cy), r in drops:
        for y in range(4, 12):
            for x in range(4, 12):
                if math.hypot(x - cx, (y - cy) * 1.2) <= r:
                    px[y][x] = c
        px[int(cy - r + 1)][int(cx)] = hi
    return px


def kinetic_generator():
    px = cell()
    c = col(0xFFEE58)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if abs(d - 3.0) < 0.55 and not (x > 7 and y < 7):
                px[y][x] = c
    for (x, y) in ((9, 4), (10, 5), (11, 4), (9, 6)):          # arrow head
        px[y][x] = WHITE
    for (x, y) in ((8, 6), (7, 7), (8, 8), (7, 9)):            # bolt
        px[y][x] = mix(c, WHITE, 0.4)
    return px


def solar_skin():
    px = cell()
    sun, panel = col(0xFFD740), mix(col(0x40C4FF), DARK, 0.35)
    for y in range(8, 12):
        for x in range(4, 12):
            px[y][x] = panel if (x + y) % 3 else mix(panel, WHITE, 0.3)
    for y in range(4, 8):
        for x in range(5, 11):
            if math.hypot(x - 7.5, y - 6) <= 1.6:
                px[y][x] = sun
    px[4][7] = WHITE
    return px


NEW = {
    'gecko_setae': gecko_setae, 'shock_gel': shock_gel, 'heat_sink_fins': heat_sink_fins, 'thermal_camo': thermal_camo,
    'jammer': jammer, 'recon_cloud': recon_cloud, 'kinetic_mesh': kinetic_mesh, 'self_repair': self_repair,
    'hydrophobic': hydrophobic, 'kinetic_generator': kinetic_generator, 'solar_skin': solar_skin,
}
MK2 = ['myofiber', 'thermoregulator', 'emp_emitter', 'optical_camo', 'toxin_filter', 'micro_capacitor'] + list(NEW)

GOLD = (255, 205, 90, 255)
VIOLET = (185, 140, 255, 255)
VIOLET_D = (110, 70, 190, 255)


def mk2(px):
    """Mk.II: the cyan rim turns violet with gold nodes; two gold rank pips at the bottom."""
    out = [row[:] for row in px]
    for y in range(16):
        for x in range(16):
            c = out[y][x]
            if c in (base.RIM, base.RIM_D):
                out[y][x] = VIOLET if c == base.RIM else VIOLET_D
            elif c == base.WHITE and (y in (1, 14) or x in (0, 15)):
                out[y][x] = GOLD
    for x in (6, 9):
        out[13][x] = GOLD
        out[12][x] = GOLD
    return out


def hexmesh():
    W = 64
    px = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(W)]
    s = 8.0                         # hexagon size
    h = math.sqrt(3) * s
    for y in range(W):
        for x in range(W):
            # axial hex grid; edge distance from the nearest centre
            best = 1e9
            for cy in range(-1, int(W / h) + 2):
                for cx in range(-1, int(W / (1.5 * s)) + 2):
                    px_c = cx * 1.5 * s
                    py_c = cy * h + (h / 2 if cx % 2 else 0)
                    dx, dy = abs(x - px_c), abs(y - py_c)
                    d = max(dy, dx * math.sqrt(3) / 2 + dy / 2)
                    best = min(best, d)
            edge = h / 2 - best
            if edge < 1.1:
                px[y][x] = (255, 210, 120, 255) if edge < 0.55 else (255, 170, 40, 255)
    return px


def main():
    for name, fn in NEW.items():
        write_png(os.path.join(MOD, 'item_module_' + name + '.png'), fn())
    for name in MK2:
        w, h, px = read_png(os.path.join(MOD, 'item_module_' + name + '.png'))
        write_png(os.path.join(MOD, 'item_module_' + name + '_mk2.png'), mk2(px))
    write_png(os.path.join(TEX, 'effect/nano_hexmesh.png'), hexmesh())
    print('wrote', len(NEW), 'unit icons,', len(MK2), 'Mk.II icons and the hex mesh')


if __name__ == '__main__':
    main()
