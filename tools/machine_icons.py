#!/usr/bin/env python3
"""
32x32 inventory icons of the refined machines, rendered straight from the refined textures
(front panel + side + top), so a redesigned front shows up in the inventory automatically.

Camera: from the front-left and above, front face (north) large on the left, the west side
narrow on the right, the top above - the IC2 icon layout. The front carries the dark status
slit and the category LED like the block. Output feeds dsp_icons.py (contour + top accent).

Usage: machine_icons.py <assets/mio_icif> <fallback_icon_dir> <out_dir>
  (icons whose block is not a refined panel machine are copied from the fallback dir)
"""
import os, sys, json, glob, shutil
from PIL import Image

A, FALLBACK, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
src = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'dsp_icons.py')).read()
ns = {}
exec(src[src.index('COL = {'):src.index('# 9x9 pictograms')], ns)
exec(src[src.index('RULES = ['):src.index('def mix(')], ns)
COL, classify = ns['COL'], ns['classify']

# screen = O + x'*FX + y*FY + z*FZ   (x' = 1 - x: the front is seen from outside, east on its left)
O = (2.0, 25.5)
FX = (18.0, 4.0)      # along the front, to the right and slightly down
FZ = (10.0, -4.5)      # away from the viewer: right and up
FY = (0.0, -20.0)     # up
CELLS = {'north': 0, 'west': 3, 'up': 4}
SHADE = {'north': 0.92, 'west': 0.70, 'up': 1.0}


def solve(p, o, a, b):
    """p = o + s*a + t*b  ->  (s, t)."""
    det = a[0] * b[1] - a[1] * b[0]
    dx, dy = p[0] - o[0], p[1] - o[1]
    return (dx * b[1] - dy * b[0]) / det, (a[0] * dy - a[1] * dx) / det


def render(strip, cat, lit=False):
    W = 32
    sp = strip.load()
    out = Image.new('RGBA', (W, W), (0, 0, 0, 0))
    op = out.load()
    led = COL[cat]
    for py in range(W):
        for px in range(W):
            acc = [0, 0, 0]; n = 0; centre = None
            for sy in (0.25, 0.5, 0.75):
                for sx in (0.25, 0.5, 0.75):
                    p = (px + sx, py + sy)
                    hit = None
                    # front (z=0): params (x', y)
                    s, t = solve(p, O, FX, FY)
                    if 0 <= s < 1 and 0 <= t < 1: hit = ('north', s, 1 - t)
                    # west side (x'=1): params (z, y)
                    if hit is None:
                        s, t = solve(p, (O[0] + FX[0], O[1] + FX[1]), FZ, FY)
                        if 0 <= s < 1 and 0 <= t < 1: hit = ('west', s, 1 - t)
                    # top (y=1): params (x', z)
                    if hit is None:
                        s, t = solve(p, (O[0] + FY[0], O[1] + FY[1]), FX, FZ)
                        if 0 <= s < 1 and 0 <= t < 1: hit = ('up', 1 - s, t)
                    if hit is None: continue
                    face, u, v = hit
                    cell = CELLS[face]
                    if face == 'north':
                        tu, tv = u * 32, v * 32
                        if 5 <= tu < 27 and 29.6 <= tv < 31.4: c = (46, 52, 60)                       # status slit (off)
                        elif 29 <= tu < 31 and 1 <= tv < 3: c = led                                    # category LED
                        else: c = sp[(cell % 4) * 32 + min(31, int(tu)), (cell // 4) * 32 + min(31, int(tv))][:3]
                    elif face == 'up':
                        c = sp[(cell % 4) * 32 + min(31, int(u * 32)), (cell // 4) * 32 + min(31, int(v * 32))][:3]
                    else:
                        c = sp[(cell % 4) * 32 + min(31, int(u * 32)), (cell // 4) * 32 + min(31, int(v * 32))][:3]
                    k = SHADE[face]
                    col = (int(c[0] * k), int(c[1] * k), int(c[2] * k))
                    if sx == 0.5 and sy == 0.5: centre = col
                    acc[0] += col[0]; acc[1] += col[1]; acc[2] += col[2]; n += 1
            if n >= 5:
                op[px, py] = (centre if centre else (acc[0] // n, acc[1] // n, acc[2] // n)) + (255,)
    return out


count = rendered = 0
for folder in sorted(os.listdir(FALLBACK)):
    fd = os.path.join(FALLBACK, folder)
    if not os.path.isdir(fd): continue
    os.makedirs(os.path.join(OUT, folder), exist_ok=True)
    for f in sorted(os.listdir(fd)):
        if not f.endswith('.png'): continue
        name = f[:-4]; count += 1
        model = os.path.join(A, 'models', 'block', folder, name + '.json')
        tex = os.path.join(A, 'textures', 'block', 'refined', folder, name + '.png')
        ok = False
        if os.path.exists(model) and os.path.exists(tex):
            d = json.load(open(model, encoding='utf-8-sig'))
            ok = any(e.get('name') == 'panels_ns' for e in d.get('elements', []))
        if ok:
            _, cat = classify(name, folder)
            render(Image.open(tex).convert('RGBA'), cat).save(os.path.join(OUT, folder, f))
            rendered += 1
        else:
            shutil.copy(os.path.join(fd, f), os.path.join(OUT, folder, f))
print(rendered, 'of', count, 'icons rendered from the refined models')
