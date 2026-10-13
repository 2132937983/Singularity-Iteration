#!/usr/bin/env python3
"""
0.1.8.5: block model and texture of the Universal Suit Modification Station (the quantum
modification station, generalised for quantum and nano suits). The block id is unchanged.

Look: a graphite service plinth with a hex-lattice floor, a raised work pad with a lattice ring,
a rear spine with an emitter head that looks down at the pad, two front pylons with light caps
and two side manipulator clamps. The light bands are split cyan (nano lattice) and violet
(quantum field): one bench for both suit classes.

Usage: station_185.py <repo root>
Writes:
  assets/mio_icif/textures/block/producer/block_quantum_modification_station.png (128x128)
  assets/mio_icif/models/block/producer/block_quantum_modification_station.json
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import write_png  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
TEX = os.path.join(ASSETS, 'textures/block/producer/block_quantum_modification_station.png')
MODEL = os.path.join(ASSETS, 'models/block/producer/block_quantum_modification_station.json')

W = H = 128
rnd = random.Random(185)
px = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]

GRAPHITE = (46, 52, 60)
GRAPHITE_D = (30, 34, 40)
GRAPHITE_L = (74, 82, 92)
STEEL = (120, 130, 142)
CYAN = (90, 230, 255)
CYAN_D = (30, 120, 150)
VIOLET = (178, 120, 255)
VIOLET_D = (90, 50, 150)
AMBER = (255, 200, 90)


def put(x, y, c, a=255):
    if 0 <= x < W and 0 <= y < H:
        px[y][x] = (max(0, min(255, int(c[0]))), max(0, min(255, int(c[1]))), max(0, min(255, int(c[2]))), a)


def rect(x0, y0, w, h, c):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            put(x, y, c)


def noise_rect(x0, y0, w, h, c, amp=6):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            n = rnd.randint(-amp, amp)
            put(x, y, (c[0] + n, c[1] + n, c[2] + n))


def bevel(x0, y0, w, h, light=GRAPHITE_L, dark=GRAPHITE_D):
    for x in range(x0, x0 + w):
        put(x, y0, light)
        put(x, y0 + h - 1, dark)
    for y in range(y0, y0 + h):
        put(x0, y, light)
        put(x0 + w - 1, y, dark)


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


# ---------------------------------------------------------------- regions (pixels; 8 px = 1 uv unit)
REG = {}


def region(name, x, y, w, h):
    REG[name] = (x, y, w, h)
    return x, y, w, h


def uv(name):
    x, y, w, h = REG[name]
    return [x / 8, y / 8, (x + w) / 8, (y + h) / 8]


# plinth side 16x6 units at 4 px/unit -> 64x24
x, y, w, h = region('plinth_side', 0, 0, 64, 24)
noise_rect(x, y, w, h, GRAPHITE)
bevel(x, y, w, h)
rect(x + 1, y + 1, w - 2, 2, GRAPHITE_L)                     # top lip
rect(x, y + h - 3, w, 3, GRAPHITE_D)                         # kick plate
for i in range(w):                                          # light band: cyan -> violet
    t = i / (w - 1)
    c = mix(CYAN, VIOLET, t)
    put(x + i, y + 8, c)
    put(x + i, y + 9, mix(c, (255, 255, 255), 0.35))
    put(x + i, y + 10, mix(c, GRAPHITE_D, 0.5))
for k in range(6):                                          # vent slots
    vx = x + 6 + k * 9
    rect(vx, y + 14, 6, 1, GRAPHITE_D)
    rect(vx, y + 16, 6, 1, GRAPHITE_D)
for bx in (x + 2, x + w - 4):                               # corner bolts
    rect(bx, y + 4, 2, 2, STEEL)

# plinth top 16x16 -> 64x64: hex lattice floor
x, y, w, h = region('plinth_top', 0, 24, 64, 64)
noise_rect(x, y, w, h, GRAPHITE, 5)
bevel(x, y, w, h)
for yy in range(h):
    for xx in range(w):
        # hex grid lines
        q = (xx - 32) / 7.0
        r = (yy - 32) / 6.06
        col = int(math.floor(q))
        row = int(math.floor(r - (col % 2) * 0.5))
        fx = q - col
        fy = r - (col % 2) * 0.5 - row
        if fx < 0.12 or fy < 0.1:
            put(x + xx, y + yy, GRAPHITE_D)
for i in range(w):                                          # rim light
    t = i / (w - 1)
    put(x + i, y + 2, mix(CYAN_D, VIOLET_D, t))
    put(x + i, y + h - 3, mix(CYAN_D, VIOLET_D, t))
for bx, by in ((2, 4), (w - 4, 4), (2, h - 6), (w - 4, h - 6)):
    rect(x + bx, y + by, 2, 2, STEEL)

# pad top 10x10 -> 40x40: lattice ring emitter
x, y, w, h = region('pad_top', 64, 0, 40, 40)
for yy in range(h):
    for xx in range(w):
        d = math.hypot(xx - 19.5, yy - 19.5)
        c = GRAPHITE_D
        if d > 18.5:
            c = GRAPHITE_L
        elif abs(d - 15.5) < 1.2:
            ang = math.atan2(yy - 19.5, xx - 19.5)
            c = mix(CYAN, VIOLET, 0.5 + 0.5 * math.sin(ang))
        elif abs(d - 11) < 0.6:
            c = CYAN_D
        elif d < 6:
            c = mix((20, 60, 80), (12, 22, 30), d / 6)
        elif (int(xx) + int(yy)) % 6 == 0 and d < 14:
            c = (38, 46, 56)
        put(x + xx, y + yy, c)
for k in range(8):                                          # ring index ticks
    a = k * math.pi / 4
    for t in range(3):
        put(int(x + 19.5 + math.cos(a) * (12.5 + t)), int(y + 19.5 + math.sin(a) * (12.5 + t)), AMBER)

# pad side 10x1 -> 40x4
x, y, w, h = region('pad_side', 64, 40, 40, 4)
rect(x, y, w, h, GRAPHITE_L)
for i in range(w):
    put(x + i, y + 2, mix(CYAN, VIOLET, i / (w - 1)))

# spine front 8x10 -> 32x40, side 3x10 -> 12x40, top 8x3 -> 32x12
x, y, w, h = region('spine_front', 64, 44, 32, 40)
noise_rect(x, y, w, h, GRAPHITE)
bevel(x, y, w, h)
rect(x + 13, y + 4, 6, h - 8, GRAPHITE_D)                    # conduit channel
for yy in range(y + 5, y + h - 4):
    t = (yy - y) / h
    put(x + 15, yy, mix(VIOLET, CYAN, t))
    put(x + 16, yy, mix(mix(VIOLET, CYAN, t), (255, 255, 255), 0.3))
rect(x + 4, y + 26, 7, 9, (16, 30, 36))                      # status display
for k in range(3):
    rect(x + 5, y + 27 + k * 3, 3 + k, 1, CYAN if k != 1 else AMBER)
rect(x + 21, y + 26, 7, 9, (16, 30, 36))
for k in range(4):
    rect(x + 22 + k, y + 34 - (k * 2 + 1), 1, k * 2 + 1, VIOLET)
x, y, w, h = region('spine_side', 96, 44, 12, 40)
noise_rect(x, y, w, h, GRAPHITE)
bevel(x, y, w, h)
for k in range(6):
    rect(x + 2, y + 5 + k * 6, w - 4, 2, GRAPHITE_D)         # ribs
x, y, w, h = region('spine_top', 64, 84, 32, 12)
noise_rect(x, y, w, h, GRAPHITE_L, 4)
bevel(x, y, w, h)
rect(x + 4, y + 5, w - 8, 2, GRAPHITE_D)

# emitter head: front 10x2 -> 40x8 (lens strip), side 1.5x2 -> 6x8, top/bottom 10x1.5 -> 40x6
x, y, w, h = region('head_front', 0, 88, 40, 8)
rect(x, y, w, h, GRAPHITE_L)
bevel(x, y, w, h, STEEL, GRAPHITE_D)
rect(x + 3, y + 2, w - 6, 4, (14, 22, 28))
x, y, w, h = region('head_side', 40, 88, 6, 8)
rect(x, y, w, h, GRAPHITE)
bevel(x, y, w, h)
x, y, w, h = region('head_top', 0, 96, 40, 6)
noise_rect(x, y, w, h, GRAPHITE_L, 4)
bevel(x, y, w, h)
x, y, w, h = region('head_bottom', 0, 102, 40, 6)
rect(x, y, w, h, GRAPHITE_D)
for i in range(4, w - 4, 4):
    rect(x + i, y + 2, 2, 2, CYAN_D)
# lens strip (emissive element): 8x1 -> 32x4
x, y, w, h = region('lens', 0, 108, 32, 4)
for i in range(w):
    c = mix(CYAN, VIOLET, i / (w - 1))
    rect(x + i, y, 1, h, mix(c, (255, 255, 255), 0.25))
    put(x + i, y + 1, mix(c, (255, 255, 255), 0.6))
    put(x + i, y + 2, mix(c, (255, 255, 255), 0.6))

# pylon side 2x5 -> 8x20, top 2x2 -> 8x8, cap (emissive)
x, y, w, h = region('pylon_side', 104, 0, 8, 20)
noise_rect(x, y, w, h, GRAPHITE)
bevel(x, y, w, h)
rect(x + 3, y + 3, 2, 12, GRAPHITE_D)
x, y, w, h = region('pylon_top', 112, 0, 8, 8)
rect(x, y, w, h, GRAPHITE_L)
bevel(x, y, w, h)
x, y, w, h = region('cap_cyan', 112, 8, 8, 8)
for yy in range(h):
    for xx in range(w):
        d = math.hypot(xx - 3.5, yy - 3.5)
        put(x + xx, y + yy, mix((230, 255, 255), CYAN, min(1, d / 4)))
x, y, w, h = region('cap_violet', 120, 8, 8, 8)
for yy in range(h):
    for xx in range(w):
        d = math.hypot(xx - 3.5, yy - 3.5)
        put(x + xx, y + yy, mix((245, 230, 255), VIOLET, min(1, d / 4)))

# clamp side 6x3 -> 24x12, end 1.5x3 -> 6x12, top 1.5x6 -> 6x24
x, y, w, h = region('clamp_side', 104, 20, 24, 12)
noise_rect(x, y, w, h, GRAPHITE)
bevel(x, y, w, h)
for k in range(3):
    rect(x + 3 + k * 7, y + 4, 4, 4, GRAPHITE_D)
    put(x + 4 + k * 7, y + 5, CYAN)
x, y, w, h = region('clamp_end', 104, 32, 6, 12)
rect(x, y, w, h, GRAPHITE)
bevel(x, y, w, h)
x, y, w, h = region('clamp_top', 112, 32, 6, 24)
noise_rect(x, y, w, h, GRAPHITE_L, 4)
bevel(x, y, w, h)

# plain metal (bottoms)
x, y, w, h = region('metal', 96, 96, 32, 32)
noise_rect(x, y, w, h, GRAPHITE_D, 4)
bevel(x, y, w, h)

write_png(TEX, px)


# ---------------------------------------------------------------- model
def face(name, cull=None, light=False):
    f = {'uv': uv(name), 'texture': '#t'}
    if cull:
        f['cullface'] = cull
    return f


def element(name, frm, to, faces, emissive=False):
    e = {'name': name, 'from': frm, 'to': to, 'faces': faces}
    if emissive:
        e['shade'] = False
        e['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    return e


elements = [
    element('plinth', [0, 0, 0], [16, 6, 16], {
        'north': face('plinth_side', 'north'), 'south': face('plinth_side', 'south'),
        'east': face('plinth_side', 'east'), 'west': face('plinth_side', 'west'),
        'up': face('plinth_top'), 'down': face('metal', 'down')}),
    element('pad', [3, 6, 3], [13, 7, 13], {
        'north': face('pad_side'), 'south': face('pad_side'), 'east': face('pad_side'), 'west': face('pad_side'),
        'up': face('pad_top')}),
    element('spine', [4, 6, 12.5], [12, 16, 15.5], {
        'north': face('spine_front'), 'south': face('spine_front'),
        'east': face('spine_side'), 'west': face('spine_side'), 'up': face('spine_top', 'up')}),
    element('emitter_head', [3, 13, 11], [13, 15, 12.5], {
        'north': face('head_front'), 'east': face('head_side'), 'west': face('head_side'),
        'up': face('head_top'), 'down': face('head_bottom')}),
    element('lens', [4, 13.5, 10.95], [12, 14.5, 11], {'north': face('lens')}, emissive=True),
    element('pylon_left', [1, 6, 1], [3, 11, 3], {
        'north': face('pylon_side'), 'south': face('pylon_side'), 'east': face('pylon_side'), 'west': face('pylon_side'),
        'up': face('pylon_top')}),
    element('pylon_right', [13, 6, 1], [15, 11, 3], {
        'north': face('pylon_side'), 'south': face('pylon_side'), 'east': face('pylon_side'), 'west': face('pylon_side'),
        'up': face('pylon_top')}),
    element('cap_left', [1.25, 11, 1.25], [2.75, 11.75, 2.75], {
        'north': face('cap_cyan'), 'south': face('cap_cyan'), 'east': face('cap_cyan'), 'west': face('cap_cyan'),
        'up': face('cap_cyan')}, emissive=True),
    element('cap_right', [13.25, 11, 13.25 - 12], [14.75, 11.75, 2.75], {
        'north': face('cap_violet'), 'south': face('cap_violet'), 'east': face('cap_violet'), 'west': face('cap_violet'),
        'up': face('cap_violet')}, emissive=True),
    element('clamp_left', [0.5, 6, 5], [2, 9, 11], {
        'north': face('clamp_end'), 'south': face('clamp_end'), 'west': face('clamp_side'), 'east': face('clamp_side'),
        'up': face('clamp_top')}),
    element('clamp_right', [14, 6, 5], [15.5, 9, 11], {
        'north': face('clamp_end'), 'south': face('clamp_end'), 'west': face('clamp_side'), 'east': face('clamp_side'),
        'up': face('clamp_top')}),
]

model = {
    'credit': 'Singularity Iteration 0.1.8.5 (tools/station_185.py): Universal Suit Modification Station',
    'parent': 'block/block',
    'render_type': 'minecraft:cutout',
    'textures': {
        'particle': 'mio_icif:block/producer/block_quantum_modification_station',
        't': 'mio_icif:block/producer/block_quantum_modification_station',
    },
    'elements': elements,
    'display': {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    },
}
with open(MODEL, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(model, f, indent=2)
    f.write('\n')
print('wrote', TEX, 'and', MODEL)
