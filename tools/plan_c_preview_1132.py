#!/usr/bin/env python3
"""
0.1.10.32: offline preview of plan C (no game needed). Three rows:
  1. the Armory iris hatch seen from above, shut / half open / open (the blades of ArmoryFxRules.blade with the generated
     armory_iris.png in planar mapping, the glowing shaft below);
  2. the hit ripple on the quantum suit at night (front view, a hit on the chest from the front), 2, 6 and 10 ticks after the
     hit: SuitFxRules.ripple at each pixel's 3D point, drawn as the hexagon mesh texture in the quantum light colour;
  3. a transfer gate seen from the front, opening / open with the launch flash / shutting (ArmoryFxRules.gate): dark disc, two
     counter-rotating rings, spiral arms.
Simplified renderer: no world lighting, no bloom.

Usage: plan_c_preview_1132.py <repo root> <out.png>
"""
import importlib.util
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('glow_preview_1130', os.path.join(HERE, 'glow_preview_1130.py'))
gp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gp)

CELL = 240
QUANTUM = (0x3C, 0xB4, 0xFF)


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


# ---------------------------------------------------------------- rules (as in the Java classes)
IRIS_R, TIP_OPEN, TWIST = 0.34, 0.92, 0.7
RIPPLE_TICKS, RIPPLE_SPEED, RIPPLE_WIDTH = 12, 0.15, 0.11


def blade(i, opening):
    a0, a1 = i * math.pi / 3, (i + 1) * math.pi / 3
    mid = (a0 + a1) / 2 + TWIST * opening
    tip = IRIS_R * TIP_OPEN * opening
    return [(math.cos(mid) * tip, math.sin(mid) * tip), (math.cos(a0) * IRIS_R, math.sin(a0) * IRIS_R), (math.cos(a1) * IRIS_R, math.sin(a1) * IRIS_R)]


def ripple(distance, age, strength=1.0):
    if age < 0 or age >= RIPPLE_TICKS:
        return 0.0
    radius = 0.04 + RIPPLE_SPEED * age
    d = (distance - radius) / RIPPLE_WIDTH
    ring = math.exp(-d * d)
    spot = math.exp(-(distance / 0.2) ** 2) * (1 - age / (RIPPLE_TICKS / 2)) if age < RIPPLE_TICKS / 2 else 0
    fade = 1 - (age / RIPPLE_TICKS) ** 3
    return min(1.5, strength * (ring + 0.7 * spot) * fade)


def inside(p, tri):
    (x, y), (a, b, c) = p, tri
    def s(p1, p2):
        return (x - p2[0]) * (p1[1] - p2[1]) - (p1[0] - p2[0]) * (y - p2[1])
    d1, d2, d3 = s(a, b), s(b, c), s(c, a)
    neg = d1 < 0 or d2 < 0 or d3 < 0
    pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (neg and pos)


# ---------------------------------------------------------------- row 1: iris
def iris(tex, opening):
    img = Image.new('RGB', (CELL, CELL), (24, 26, 30))
    px = img.load()
    scale = CELL / 1.0                      # one block over the cell
    tris = [blade(i, opening) for i in range(6)]
    for y in range(CELL):
        for x in range(CELL):
            u, v = (x + 0.5) / scale - 0.5, (y + 0.5) / scale - 0.5
            r = math.hypot(u, v)
            # the Armory top around the hatch (dark panel)
            col = (52, 56, 62) if max(abs(u), abs(v)) < 0.406 else (38, 40, 44)
            if r < IRIS_R * 0.98 and opening > 0.01:
                g = (1 - r / IRIS_R) * 0.6 + 0.4
                col = tuple(min(255, int(c + k * g * opening)) for c, k in zip(col, (90, 210, 255)))
            for t in tris:
                if inside((u, v), t):
                    tx, ty = min(31, int((0.5 + u / (2 * IRIS_R)) * 32)), min(31, int((0.5 + v / (2 * IRIS_R)) * 32))
                    tx, ty = max(0, tx), max(0, ty)
                    col = tex.getpixel((tx, ty))[:3]
                    break
            px[x, y] = col
    return img


# ---------------------------------------------------------------- row 2: hit ripple
def ripple_cell(quads, mesh, age):
    W, H = CELL, CELL
    img = [[(10, 12, 16)] * W for _ in range(H)]
    depth = [[-1e9] * W for _ in range(H)]
    world = [[None] * W for _ in range(H)]
    scale = (H - 12) / 32.0
    hit = (0.0, 4.5, -6.0)                     # chest front, model pixels (y down)
    for pts, uvs, n, layers, mask in quads:
        scr = [(W / 2 + x * scale, 6 + (y + 8) * scale, -z) for x, y, z in pts]
        shade = 0.16 * min(1.0, 0.45 + 0.4 * abs(n[2]) + 0.25 * max(0.0, -n[1]))
        for tri in ((0, 1, 2), (0, 2, 3)):
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = (scr[i] for i in tri)
            p3 = [pts[i] for i in tri]
            uv = [uvs[i] for i in tri]
            area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if abs(area) < 1e-9:
                continue
            for yy in range(max(0, int(min(y0, y1, y2))), min(H - 1, int(max(y0, y1, y2)) + 1) + 1):
                for xx in range(max(0, int(min(x0, x1, x2))), min(W - 1, int(max(x0, x1, x2)) + 1) + 1):
                    pxx, pyy = xx + 0.5, yy + 0.5
                    w0 = ((x1 - pxx) * (y2 - pyy) - (x2 - pxx) * (y1 - pyy)) / area
                    w1 = ((x2 - pxx) * (y0 - pyy) - (x0 - pxx) * (y2 - pyy)) / area
                    w2 = 1 - w0 - w1
                    if min(w0, w1, w2) < -1e-6:
                        continue
                    z = w0 * z0 + w1 * z1 + w2 * z2
                    if z <= depth[yy][xx]:
                        continue
                    u = w0 * uv[0][0] + w1 * uv[1][0] + w2 * uv[2][0]
                    v = w0 * uv[0][1] + w1 * uv[1][1] + w2 * uv[2][1]
                    base = layers[0]
                    r, g, b, al = base.getpixel((min(base.width - 1, int(u * base.width)), min(base.height - 1, int(v * base.height))))
                    if al < 20:
                        continue
                    depth[yy][xx] = z
                    world[yy][xx] = (tuple(w0 * p3[0][k] + w1 * p3[1][k] + w2 * p3[2][k] for k in range(3)), u, v)
                    img[yy][xx] = (int(r * shade), int(g * shade), int(b * shade))
    out = Image.new('RGB', (W, H))
    flat = []
    for yy in range(H):
        for xx in range(W):
            col = img[yy][xx]
            wv = world[yy][xx]
            if wv is not None:
                (mx, my, mz), u, v = wv
                d = math.dist((mx, my, mz), hit) / 16.0
                light = ripple(d, age)
                if light > 0.01:
                    m = mesh.getpixel((int(u * 4 * mesh.width) % mesh.width, int(v * 4 * mesh.height) % mesh.height))
                    k = (m[0] + m[1] + m[2]) / (3 * 255) * light
                    col = tuple(min(255, int(c + q * k)) for c, q in zip(col, QUANTUM))
            flat.append(col)
    out.putdata(flat)
    return out


# ---------------------------------------------------------------- row 3: gate
def gate(t):
    img = Image.new('RGB', (CELL, CELL), (70, 110, 150))       # sky
    px = img.load()
    t2 = t
    if t2 < -8:
        opening = 0.0
    elif t2 < 0:
        opening = smooth((t2 + 8) / 8)
    elif t2 < 6:
        opening = 1.0
    else:
        opening = 1 - smooth((t2 - 6) / 8)
    R = 0.85 * opening
    scale = CELL / 2.4
    flash = 1 - t / 3 if 0 <= t <= 3 else 0
    for y in range(CELL):
        for x in range(CELL):
            u, v = (x + 0.5 - CELL / 2) / scale, (y + 0.5 - CELL / 2) / scale
            r = math.hypot(u, v)
            a = math.atan2(v, u)
            col = list(px[x, y])
            if R > 0.01 and r < R:
                k = 0.6 * (1 - r / R * 0.67) * opening
                col = [c * (1 - k) + q * k for c, q in zip(col, (8, 0, 20))]
            add = 0.0
            for ring, rr, w, spin in ((0, R, 0.07, 1), (1, R * 0.72, 0.045, -1.6)):
                if R > 0.01 and rr <= r <= rr + w:
                    dash = 0.45 + 0.55 * max(0.0, math.cos(a * 4 + spin * t * 0.09)) ** 2
                    add += opening * dash * (0.9 if ring == 0 else 0.6) * (1 - (r - rr) / w)
            for arm in range(3):
                for k in range(40):
                    uu = k / 40
                    ang = arm * math.pi * 2 / 3 + uu * 2.4 - t * 0.15
                    rad = R * (1 - uu)
                    if abs(u - math.cos(ang) * rad) < 0.02 and abs(v - math.sin(ang) * rad) < 0.02:
                        add = max(add, 0.55 * opening * (1 - uu * 0.6))
            if flash > 0:
                add += flash * math.exp(-(r / (0.15 + 0.5 * flash)) ** 2) * 1.2
            col = [min(255, int(c + q * add)) for c, q in zip(col, (190, 140, 255))]
            px[x, y] = tuple(col)
    return img


def main():
    root, out = sys.argv[1], sys.argv[2]
    a = os.path.join(root, 'src/main/resources/assets/mio_icif')
    tex = Image.open(os.path.join(a, 'textures/block/armory_iris.png')).convert('RGB')
    mesh = Image.open(os.path.join(a, 'textures/effect/nano_hexmesh.png')).convert('RGB')
    quads = gp.load(root, 'glow', None)
    sheet = Image.new('RGB', (CELL * 3, CELL * 3), (8, 9, 12))
    for i, o in enumerate((0.0, 0.5, 1.0)):
        sheet.paste(iris(tex, o), (i * CELL, 0))
    for i, age in enumerate((2, 6, 10)):
        sheet.paste(ripple_cell(quads, mesh, age), (i * CELL, CELL))
    for i, t in enumerate((-4, 1, 9)):
        sheet.paste(gate(t), (i * CELL, 2 * CELL))
    sheet.save(out)
    print('wrote', out)


if __name__ == '__main__':
    main()
