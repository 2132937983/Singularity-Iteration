#!/usr/bin/env python3
"""0.1.8.2 crop-on-stick textures (16x16, modern vanilla style), crop sticks and fallback plants.

Every plant stage that a plant class asks for ("mio_icif:block/crop/<name>_<stage>") is drawn from
an archetype (grain, root, flower, bush, reed, mushroom, wart, metal, gourd, sapling, special) with
its own palette. The layout of a plant (stalk positions, lean, jitter) comes from a seed of the plant
name, so all stages of one plant line up and the growth reads as one plant.

Style rules: no black outlines; 3-4 tones per material; light from the upper left (the lighting
pass lightens left/top edges and darkens right edges); stage 1 is small and low, the last stage
fills about 13-14 px and shows the product clearly.

Usage: crops_182.py <repo root> [preview dir]
"""
import os
import random
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixart import Canvas, hexc, mix, shade, ramp, load, sheet  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
OUT = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/block/crop')

LEAF = hexc('4f9a2e')
STEM = hexc('3d7a24')
DRY = hexc('a8a040')


def pal(c, n=4):
    return ramp(c, n, -0.42, 0.32)


def light_pass(c):
    src = list(c.px)
    w, h = c.w, c.h
    def op(x, y):
        return 0 <= x < w and 0 <= y < h and src[y * w + x][3] > 0
    for y in range(h):
        for x in range(w):
            p = src[y * w + x]
            if p[3] == 0:
                continue
            l, r, t, b = op(x - 1, y), op(x + 1, y), op(x, y - 1), op(x, y + 1)
            if l and not r:
                c.put(x, y, shade(p, -0.16))
            elif r and not l:
                c.put(x, y, shade(p, 0.10))
            elif b and not t and not l and not r:
                c.put(x, y, shade(p, 0.08))


def leaf(c, x, y, d, size, p):
    """Leaf from the stem point (x, y) to side d (+1 / -1), rising. p = 4-tone ramp."""
    if size <= 0:
        return
    c.set(x + d, y, p[1])
    if size >= 2:
        c.set(x + 2 * d, y - 1, p[2])
    if size >= 3:
        c.set(x + d, y - 1, p[2])
        c.set(x + 3 * d, y - 2, p[3])
    if size >= 4:
        c.set(x + 2 * d, y, p[0])
        c.set(x + 3 * d, y - 1, p[2])
        c.set(x + 4 * d, y - 3, p[3])


def droop_leaf(c, x, y, d, size, p):
    """Long leaf that bends down (grass, maize-like)."""
    pts = [(1, 0), (2, -1), (3, -1), (4, 0), (5, 1)][:size]
    for i, (dx, dy) in enumerate(pts):
        c.set(x + d * dx, y + dy, p[1 if i < 2 else 2 if i < 4 else 0])


def stalk(c, x, base, h, col, lean=0, width=1, col2=None):
    pts = []
    for i in range(max(0, h)):
        y = base - i
        dx = int(round(lean * (i / max(1, h - 1)) ** 1.6))
        for k in range(width):
            c.set(x + dx + k, y, col if k == 0 or col2 is None else col2)
        pts.append((x + dx, y))
    return pts


def nugget(c, x, y, p, glint=(255, 255, 255, 255)):
    c.set(x, y, p[2]); c.set(x + 1, y, p[3]); c.set(x, y + 1, p[1]); c.set(x + 1, y + 1, p[0])
    c.set(x + 1, y, glint)


def layout(name, n, xs):
    rng = random.Random(zlib.crc32(name.encode()))
    out = []
    for i in range(n):
        out.append({'x': xs[i] + rng.choice([-1, 0, 0, 1]), 'h': rng.randint(-1, 1), 'lean': rng.choice([-1, 0, 0, 0, 1]),
                    'd': rng.choice([-1, 1]), 'r': rng.random()})
    return rng, out


# ================================================================== archetypes
def grain(c, name, s, m, head, ripe=None, flax=False):
    f = s / m
    rng, lay = layout(name, 4, [3, 6, 9, 12])
    green = pal(LEAF)
    ripe_col = ripe or head
    t = max(0.0, (f - 0.55) / 0.45) if not flax else 0.0
    stem = mix(STEM, ripe_col, t * 0.85)
    sp = pal(stem)
    hp = pal(head)
    H = round(2 + 11 * f)
    for i, st in enumerate(lay):
        h = max(2, H + st['h'] - (1 if i % 2 else 0))
        pts = stalk(c, st['x'], 15, h, sp[1], st['lean'] if h > 6 else 0)
        for k, (px, py) in enumerate(pts):
            if k in (2, 5, 8) and k < h - 2:
                leaf(c, px, py, st['d'] if k % 2 == 0 else -st['d'], 2 if f < 0.5 else 3, pal(mix(LEAF, ripe_col, t * 0.7)))
        top = pts[-1]
        if flax:
            if f >= 0.7:
                c.set(top[0], top[1] - 1, hp[2])
            if s == m:
                bx, by = top[0], top[1] - 2
                for dx, dy in ((0, 0), (-1, 1), (1, 1), (0, 2)):
                    c.set(bx + dx, by + dy, hp[2])
                c.set(bx, by + 1, hexc('f0e8a0'))
            continue
        if f >= 0.6:
            L = 2 + round(3 * (f - 0.6) / 0.4)
            for k in range(L):
                px, py = pts[-1 - k] if k < len(pts) else top
                c.set(px, py, hp[1 + (k % 2)])
                c.set(px + 1, py, hp[(k % 2)])
            c.set(top[0], top[1] - 1, hp[3])
            if s == m:
                c.set(top[0] + 1, top[1] - 2, hp[3])


def root(c, name, s, m, top, kind):
    f = s / m
    rng, lay = layout(name, 3, [3, 8, 12])
    lp = pal(LEAF if kind != 'beet' else hexc('3f8a3a'))
    vein = hexc('b8304a')
    th = round(2 + 6 * f)
    for i, st in enumerate(lay):
        if s == 1 and i == 1:
            continue
        x = st['x']
        h = max(2, th + st['h'])
        base = 14
        # EN: a rosette of three leaves: centre, left and right (lines with light tips).
        tips = [(x, base - h), (x - 2, base - h + 2), (x + 2, base - h + 2)]
        if h >= 5:
            tips += [(x - 3, base - h + 4), (x + 3, base - h + 4)]
        for j, (tx, ty) in enumerate(tips):
            c.line(x, base, tx, ty, lp[1] if j % 2 == 0 else lp[2])
            c.set(tx, ty, lp[3])
            if kind == 'beet' and j == 0:
                c.line(x, base, tx, ty + 1, vein)
        c.set(x, base, lp[0])
        tp = pal(top)
        if f >= 0.99:
            if kind == 'carrot':
                c.set(x - 1, 15, tp[1]); c.set(x, 15, tp[2]); c.set(x, 14, tp[2]); c.set(x - 1, 14, tp[1])
            elif kind == 'beet':
                c.rect(x - 1, 14, x + 1, 15, tp[1]); c.set(x - 1, 14, tp[2]); c.set(x + 1, 15, tp[0])
            elif kind == 'potato':
                c.rect(x - 1, 14, x, 15, tp[1]); c.set(x - 1, 14, tp[2])
    if kind == 'potato' and s == m:
        for st in lay:
            c.set(st['x'], 15 - th - 1, hexc('ece2f4'))
            c.set(st['x'] + 1, 15 - th, hexc('b89ad8'))


def bloom(c, x, y, kind, petal, center):
    p = pal(petal)
    if kind == 'daisy':
        c.ellipse(x + 0.5, y + 0.5, 2.0, 1.6, p[1])
        c.set(x - 1, y - 1, p[3]); c.set(x, y - 1, p[2]); c.set(x - 1, y, p[2]); c.set(x + 1, y + 1, p[0])
        c.set(x, y, center)
    elif kind == 'rose':
        c.rect(x - 1, y - 1, x + 1, y + 1, p[1])
        c.set(x - 1, y - 1, p[2]); c.set(x + 1, y - 1, p[2]); c.set(x, y - 2, p[3]); c.set(x, y, center)
        c.set(x + 1, y + 1, p[0]); c.set(x - 2, y, p[2]); c.set(x + 2, y, p[0])
    elif kind == 'tulip':
        c.set(x - 1, y - 1, p[3]); c.set(x + 1, y - 1, p[2])
        c.rect(x - 1, y, x + 1, y + 1, p[1]); c.set(x - 1, y, p[2]); c.set(x + 1, y + 1, p[0]); c.set(x, y - 1, p[1])
    elif kind == 'spike':
        for k in range(4):
            c.set(x - (k % 2), y + k, p[2 if k % 2 == 0 else 1]); c.set(x + 1 - (k % 2), y + k, p[1 if k % 2 == 0 else 0])
        c.set(x, y - 1, p[3])
    elif kind == 'thorn':
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            c.set(x + dx, y + dy, p[1 if (dx + dy) % 2 else 2])
        c.set(x + 1, y, p[3]); c.set(x - 1, y + 2, p[1]); c.set(x - 1, y + 3, p[0])
    elif kind == 'star':
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1), (-1, -2), (1, -2), (-2, 1), (2, 1)):
            c.set(x + dx, y + dy, p[1 if dy > -1 else 2])
        c.set(x, y, center)
        c.set(x - 1, y - 1, p[3]); c.set(x + 1, y - 1, p[3])


def flower(c, name, s, m, kind, petal, center, stem=None, thorny=False, n=3):
    f = s / m
    rng, lay = layout(name, n, [4, 8, 11][:n])
    sp = pal(stem or STEM)
    lp = pal(LEAF if not thorny else hexc('3c5a2a'))
    H = round(3 + 8 * f)
    for i, st in enumerate(lay):
        h = max(2, H + st['h'] - (2 if i == 1 and n == 3 else 0))
        pts = stalk(c, st['x'], 15, h, sp[1], st['lean'] if h > 5 else 0)
        for k, (px, py) in enumerate(pts):
            if k in (1, 4) and k < h - 2:
                leaf(c, px, py, st['d'] if k == 1 else -st['d'], 3 if f > 0.5 else 2, lp)
            if thorny and k in (3, 6) and k < h - 2:
                c.set(px + (1 if k == 3 else -1), py - 1, shade(sp[2], 0.15))
        top = pts[-1]
        if s == m:
            bloom(c, top[0], top[1] - 2, kind, petal, center)
        elif s == m - 1 and f > 0.5:
            bp = pal(petal)
            c.set(top[0], top[1] - 1, bp[1]); c.set(top[0], top[1] - 2, bp[2])


def bush(c, name, s, m, leaf_col, fruit=None, fruit_unripe=None, fruit_shape='dot', extra=None, trunk=None):
    f = s / m
    rng, lay = layout(name, 6, [5, 8, 11, 6, 10, 8])
    lp = pal(leaf_col)
    if trunk:
        tp = pal(trunk)
        th = round(2 + 6 * f)
        stalk(c, 7, 15, th, tp[1], 0, 2, tp[0])
    r = 1.6 + 4.2 * f
    cy = 15 - r * 0.85 - (round(3 * f) if trunk else 0)
    blobs = [(8, cy, r, r * 0.85)]
    if f > 0.3:
        blobs += [(8 - r * 0.7, cy + r * 0.25, r * 0.7, r * 0.6), (8 + r * 0.75, cy + r * 0.2, r * 0.7, r * 0.6)]
    for bx, by, rx, ry in blobs:
        c.ellipse(bx, by, rx, ry, lp[1])
    # leaf texture: dark and light flecks
    for y in range(16):
        for x in range(16):
            p = c.get(x, y)
            if p[3] and p == lp[1]:
                v = rng.random()
                if v < 0.18:
                    c.put(x, y, lp[0])
                elif v < 0.36:
                    c.put(x, y, lp[2])
                elif v < 0.42:
                    c.put(x, y, lp[3])
    if not trunk:
        c.set(8, 15, pal(STEM)[1])
    if extra:
        extra(c, s, m, f, rng, blobs)
    if fruit and f >= 0.7:
        col = fruit if s == m else (fruit_unripe or mix(fruit, leaf_col, 0.6))
        fp = pal(col)
        spots = [(6, 9), (10, 8), (8, 11), (5, 12), (11, 12), (9, 6), (7, 7)]
        cnt = 3 if s < m else 6
        for (x, y) in spots[:cnt]:
            y = int(max(y, cy - r + 2))
            if fruit_shape == 'dot':
                c.set(x, y, fp[2]); c.set(x + 1, y, fp[1]); c.set(x, y + 1, fp[1]); c.set(x + 1, y + 1, fp[0])
                c.set(x, y, shade(fp[3], 0.2))
            elif fruit_shape == 'egg':
                c.set(x, y, fp[3]); c.set(x + 1, y, fp[2]); c.set(x, y + 1, fp[2]); c.set(x + 1, y + 1, fp[1]); c.set(x, y + 2, fp[1]); c.set(x + 1, y + 2, fp[0])
            elif fruit_shape == 'cherry':
                c.set(x, y, fp[2]); c.set(x + 1, y + 1, fp[1])
            elif fruit_shape == 'ball':
                c.ellipse(x + 1, y + 1, 1.6, 1.6, (fp[1][0], fp[1][1], fp[1][2], 215)); c.set(x, y, (255, 255, 255, 200))


def hops(c, name, s, m):
    f = s / m
    lp = pal(hexc('5aa03a'))
    pole = pal(hexc('9a7a50'))
    h = round(3 + 11 * f)
    stalk(c, 7, 15, min(15, h + 1), pole[1])
    rng, lay = layout(name, 2, [6, 9])
    for k in range(h):
        y = 15 - k
        x = 8 + (1 if (k // 2) % 2 else 0)
        c.set(x, y, lp[1])
        if k % 3 == 1:
            leaf(c, x, y, 1 if (k // 3) % 2 else -1, 3, lp)
    if f >= 0.7:
        cp = pal(hexc('b8d870') if s == m else hexc('8ac050'))
        for (x, y) in ((5, 7), (10, 9), (5, 11), (10, 5))[: (4 if s == m else 2)]:
            c.set(x, y, cp[3]); c.set(x + 1, y, cp[2]); c.set(x, y + 1, cp[2]); c.set(x + 1, y + 1, cp[1]); c.set(x, y + 2, cp[1])


def cocoa(c, name, s, m):
    def pods(c, s, m, f, rng, blobs):
        if f < 0.5:
            return
        pc = hexc('8a9a38') if s < m - 1 else hexc('c07a2c') if s == m - 1 else hexc('8a4a22')
        pp = pal(pc)
        for (x, y) in ((5, 10), (10, 11))[: (1 if s < m else 2)]:
            c.rect(x, y, x + 1, y + 2, pp[1]); c.set(x, y, pp[2]); c.set(x + 1, y + 2, pp[0]); c.set(x, y + 1, pp[3])
    bush(c, name, s, m, hexc('3f7a2e'), trunk=hexc('7a5a3a'), extra=pods)


def reed(c, name, s, m, cane, tip=None, leafc=None, wide=True, special=None):
    f = s / m
    rng, lay = layout(name, 3, [3, 7, 11])
    cp = pal(cane)
    lp = pal(leafc or LEAF)
    H = round(3 + 11 * f)
    for i, st in enumerate(lay):
        if s == 1 and i == 2:
            continue
        h = max(2, min(15, H + st['h']))
        x = st['x']
        for k in range(h):
            y = 15 - k
            joint = k % 4 == 3
            c.set(x, y, cp[0] if joint else cp[2])
            if wide:
                c.set(x + 1, y, cp[0] if joint else cp[1])
            if joint and k < h - 2 and (k // 4 + i) % 2 == 0:
                # EN: narrow blade that rises steeply (does not join the next cane).
                d = st['d'] if (k // 4) % 2 == 0 else -st['d']
                bx = x + (1 if d > 0 else 0)
                c.set(bx + d, y - 1, lp[1]); c.set(bx + d, y - 2, lp[2]); c.set(bx + 2 * d, y - 3, lp[3])
        if special:
            special(c, x, 15 - h + 1, s, m, f)


def mushroom(c, name, s, m, cap, spots=None, flat=False):
    capp = pal(cap)
    stem = pal(hexc('e6dcc0'))
    sizes = {1: [(6, 1, 1), (10, 1, 1)], 2: [(5, 3, 2), (10, 2, 2)], 3: [(5, 4, 3), (11, 3, 2)]}[max(1, min(3, round(3 * s / m)))]
    for (x, sh, rw) in sizes:
        top = 15 - sh - 1
        stalk(c, x, 15, sh, stem[2], 0, 1)
        if flat:
            c.rect(x - rw, top - 1, x + rw, top, capp[1])
            for k in range(-rw, rw + 1):
                c.set(x + k, top - 1, capp[2])
            c.set(x - rw, top, capp[0]); c.set(x + rw, top, capp[0])
        else:
            c.ellipse(x + 0.5, top, rw + 0.5, max(1.0, rw * 0.8), capp[1])
            c.set(x - rw + 1, top - max(1, rw - 1), capp[3])
            if spots and rw >= 2:
                c.set(x - 1, top - 1, spots); c.set(x + 1, top, spots)
                if rw >= 3:
                    c.set(x + 2, top - 1, spots)


def wart(c, name, s, m, col):
    f = s / m
    p = pal(col)
    rng, lay = layout(name, 4, [3, 6, 9, 12])
    H = round(2 + 8 * f)
    for i, st in enumerate(lay):
        h = max(1, H + st['h'] - (i % 2) * 2)
        x = st['x']
        for k in range(h):
            y = 15 - k
            c.set(x, y, p[1]); c.set(x + 1, y, p[0])
            if k % 3 == 2 and k < h - 1:
                side = -1 if (k // 3 + i) % 2 == 0 else 2
                c.set(x + side, y, p[2]); c.set(x + side, y - 1, p[1])
        # EN: round bulb on top. ZH: 顶部圆球。JA: 先端の丸いこぶ。
        if f > 0.4:
            c.set(x - 1, 15 - h, p[2]); c.set(x, 15 - h, p[3]); c.set(x + 1, 15 - h, p[2]); c.set(x + 2, 15 - h, p[1])
            c.set(x, 14 - h, p[2]); c.set(x + 1, 14 - h, p[1])


def ore_chunk(c, x, y, p, glint):
    """3x3 metallic cluster with a glint (the product of a metal crop)."""
    for dx, dy, k in ((1, 0, 3), (0, 1, 2), (1, 1, 2), (2, 1, 1), (1, 2, 1), (0, 2, 0), (2, 2, 0), (2, 0, 2), (0, 0, 2)):
        c.set(x + dx, y + dy, p[k])
    c.set(x + 1, y, glint)


def metal(c, name, s, m, col, glow=False):
    f = s / m
    rng, lay = layout(name, 3, [4, 8, 11])
    t = max(0.0, (f - 0.3) / 0.7)
    lp = pal(mix(LEAF, col, 0.18 + 0.32 * t))
    tipc = shade(col, 0.15)
    sp = pal(mix(STEM, col, 0.25 * t))
    mp = ramp(col, 4, -0.5, 0.35)
    glint = hexc('eaffd8') if glow else (255, 255, 255, 255)
    H = round(3 + 9 * f)
    for i, st in enumerate(lay):
        h = max(2, H + st['h'] - (1 if i == 1 else 0))
        pts = stalk(c, st['x'], 15, h, sp[1], st['lean'] if h > 6 else 0)
        for k, (px, py) in enumerate(pts):
            if k in (2, 5, 8) and k < h - 1:
                d = st['d'] if k != 5 else -st['d']
                leaf(c, px, py, d, 3 if f > 0.4 else 2, lp)
                if f >= 0.5:
                    c.set(px + 3 * d, py - 2, tipc)
        top = pts[-1]
        if s == m - 1 and m > 2:
            c.set(top[0], top[1] - 1, mp[2]); c.set(top[0] + 1, top[1] - 1, mp[1])
        if s == m:
            ore_chunk(c, top[0] - 1, top[1] - 3, mp, glint)
    if s == m:
        ore_chunk(c, 6, 11, mp, glint)
        if glow:
            for (x, y) in ((2, 7), (13, 5), (10, 10), (5, 4)):
                c.set(x, y, (160, 255, 120, 190))


def gourd(c, name, s, m, fruit, stripes=None):
    f = s / m
    lp = pal(LEAF)
    vine = pal(hexc('4a8a2a'))
    length = round(2 + 10 * f)
    for k in range(length):
        x = 2 + k
        y = 15 - (1 if k % 4 in (1, 2) else 0)
        c.set(x, y, vine[1])
        if k % 3 == 1:
            c.rect(x - 1, y - 3, x + 1, y - 1, lp[1]); c.set(x - 1, y - 3, lp[3]); c.set(x + 1, y - 1, lp[0]); c.set(x, y - 2, lp[2])
    if s == m - 1:
        c.set(9, 11, hexc('f0d040')); c.set(10, 11, hexc('e0b020'))
    if s == m:
        fp = pal(fruit)
        c.ellipse(9, 12.5, 3.6, 2.8, fp[1])
        for x in range(6, 13):
            for y in range(10, 16):
                if c.get(x, y) == fp[1]:
                    if stripes and x % 2 == 0:
                        c.put(x, y, pal(stripes)[1])
                    elif not stripes and x in (7, 9, 11):
                        c.put(x, y, fp[0])
        c.set(7, 10, fp[3]); c.set(8, 10, fp[2])
        c.set(9, 9, vine[0]); c.set(9, 8, vine[1])


SAPLINGS = {
    'oak_sapling': (hexc('6b5232'), hexc('4c8f2f'), 'round'),
    'spruce_sapling': (hexc('4a3420'), hexc('2f5a34'), 'cone'),
    'birch_sapling': (hexc('e8e4d8'), hexc('6aa84a'), 'oval'),
    'jungle_sapling': (hexc('6a5228'), hexc('3f9a2a'), 'jungle'),
    'acacia_sapling': (hexc('7a6a5a'), hexc('6a9a2a'), 'flat'),
    'dark_oak_sapling': (hexc('3e2c18'), hexc('2e6a22'), 'dense'),
}


def sapling(c, name, s, m):
    trunk, leafc, shape = SAPLINGS[name]
    tp = pal(trunk)
    lp = pal(leafc)
    rng, _ = layout(name, 1, [8])
    th = [0, 2, 4, 6, 7, 8][min(5, s)]
    width = 2 if shape == 'dense' and s >= 4 else 1
    x = 7 if width == 2 else 8
    lean = 1 if shape == 'flat' and s >= 4 else 0
    pts = stalk(c, x, 15, th, tp[1], lean, width, tp[0])
    if shape == 'oval':
        for (px, py) in pts[1::2]:
            c.set(px, py, hexc('3a3a3a'))
    top = pts[-1] if pts else (8, 14)
    r = [0, 1.2, 2.0, 2.8, 3.6, 4.4][min(5, s)]
    cx, cy = top[0] + 0.5 + (0.5 if width == 2 else 0), top[1] - r * 0.6
    if s == 1:
        c.set(top[0] - 1, top[1], lp[2]); c.set(top[0] + 1, top[1] - 1, lp[3]); return
    if shape == 'cone':
        for k in range(int(r * 2.2)):
            half = int(round((k + 1) * 0.45))
            y = int(cy - r + k)
            for dx in range(-half, half + 1):
                c.set(int(cx) + dx, y, lp[1 if (dx + k) % 3 else 2])
    elif shape == 'flat':
        c.ellipse(cx + 1, cy, r * 1.25, max(1.0, r * 0.5), lp[1])
    elif shape == 'oval':
        c.ellipse(cx, cy, r * 0.8, r * 1.05, lp[1])
    else:
        c.ellipse(cx, cy, r, r * 0.9, lp[1])
    for y in range(16):
        for xx in range(16):
            p = c.get(xx, y)
            if p == lp[1]:
                v = rng.random()
                c.put(xx, y, lp[0] if v < 0.2 else lp[2] if v < 0.38 else lp[3] if v < 0.44 else lp[1])
    if shape == 'jungle' and s >= 4:
        for vx in (int(cx - r + 1), int(cx + r - 1)):
            for k in range(3):
                c.set(vx, int(cy + r * 0.6) + k, lp[0])


def eating(c, name, s, m):
    body = pal(hexc('5a9a3a'))
    inner = pal(hexc('d8506a'))
    teeth = hexc('f4f0d8')
    stalk(c, 8, 15, 2 + s, body[0])
    leaf(c, 8, 14, -1, 3, body); leaf(c, 8, 14, 1, 3, body)
    if s <= 2:
        c.ellipse(8.5, 13 - s, 1.5 + s * 0.4, 1.2, body[1]); return
    y = 15 - (2 + s)
    if s in (3, 4, 6):
        span = 3 if s == 3 else 4
        for k in range(span):
            c.set(8 - k, y - k, body[1]); c.set(9 + k, y - k, body[1])
            c.set(8 - k, y - k + 1, inner[1]); c.set(9 + k, y - k + 1, inner[1])
            if k % 2 == 0:
                c.set(8 - k - 1, y - k - 1, teeth); c.set(9 + k + 1, y - k - 1, teeth)
        c.rect(8, y, 9, y + 1, inner[2] if s == 6 else inner[0])
    else:
        # EN: closed trap (digesting): a bulb with a toothed seam. ZH: 闭合的捕虫夹（消化中）：带齿缝的球。JA: 閉じた罠（消化中）：歯の並んだ合わせ目のある球。
        c.ellipse(8.5, y - 1, 3.0, 2.2, body[1])
        for dx in range(-2, 4):
            c.set(8 + dx, y - 1, inner[0] if dx % 2 else teeth)
        c.set(7, y - 3, body[3]); c.set(8, y - 3, body[2])


def weed(c, name, s, m):
    f = s / m
    rng, lay = layout(name, 7, [2, 4, 6, 8, 10, 12, 13])
    gp = pal(mix(hexc('6aa83a'), hexc('8a8a3a'), f * 0.6))
    n = 2 + s
    for st in lay[:n]:
        h = max(2, round(2 + 9 * f) + st['h'] + int(st['r'] * 3) - 1)
        stalk(c, st['x'], 15, h, gp[1 if st['r'] > 0.5 else 2], st['lean'])
        if s >= 4 and st['r'] > 0.5:
            c.set(st['x'] + st['lean'], 15 - h, hexc('b09a5a'))
        droop_leaf(c, st['x'], 15 - h // 2, st['d'], 3, gp)


def generic_fallback(c, name, s, m):
    flower(c, name, s, m, 'daisy', hexc('9ab0a0'), hexc('d8e0c0'), stem=hexc('5a7a5a'))


# ------------------------------------------------------------------ specials for reeds and bushes
def resin(c, x, ytop, s, m, f):
    if s == m:
        for k in (2, 6):
            c.set(x + 2, ytop + k, hexc('e8a030')); c.set(x + 2, ytop + k + 1, hexc('b86a18'))


def flame(c, x, ytop, s, m, f):
    if f >= 0.7:
        c.set(x, ytop - 1, hexc('ffe060')); c.set(x + 1, ytop - 1, hexc('ff9a20'))
        if s == m:
            c.set(x, ytop - 2, hexc('ffb030')); c.set(x + 1, ytop - 3, hexc('ff6a10')); c.set(x - 1, ytop, hexc('ff8a20'))


def crystal(c, x, ytop, s, m, f):
    if s == m:
        nugget(c, x, ytop - 2, pal(hexc('5ae0e8')))
        c.set(x, ytop - 3, hexc('c8ffff'))
    elif f >= 0.7:
        c.set(x, ytop - 1, hexc('5ac8d0'))


def tears(c, x, ytop, s, m, f):
    if s == m:
        for k in (1, 5):
            c.set(x + 2, ytop + k, hexc('cfefff')); c.set(x + 2, ytop + k + 1, hexc('8ac8f0'))


def ash(c, x, ytop, s, m, f):
    if f >= 0.7:
        c.set(x, ytop - 1, hexc('6a2020'))
    if s == m:
        c.set(x + 2, ytop + 3, hexc('9a9a9a')); c.set(x - 1, ytop + 6, hexc('b0b0b0'))


def webs(c, s, m, f, rng, blobs):
    if f >= 0.6:
        w = (240, 240, 240, 190)
        c.line(3, 6, 7, 10, w); c.line(12, 5, 9, 9, w)
        if s == m:
            for (x, y) in ((6, 6), (10, 7), (8, 9)):
                c.set(x, y, hexc('a058c8'))


def creeper_face(c, s, m, f, rng, blobs):
    if s == m:
        bx, by = 7, int(blobs[0][1]) - 1
        dark = hexc('1a2a14')
        for dx, dy in ((0, 0), (3, 0), (1, 2), (2, 2), (1, 3), (2, 3), (0, 4), (3, 4)):
            c.set(bx + dx, by + dy, dark)


def bones(c, s, m, f, rng, blobs):
    if f >= 0.7:
        b = hexc('ece6d6')
        c.set(5, 10, b); c.set(6, 11, b); c.set(7, 12, b); c.set(4, 10, b); c.set(7, 13, b)
        if s == m:
            c.set(10, 9, hexc('8a3a2a')); c.set(11, 9, hexc('6a2a1a')); c.set(11, 11, b); c.set(12, 12, b)


def leather(c, s, m, f, rng, blobs):
    if f >= 0.7:
        lp = pal(hexc('a0683a'))
        for (x, y) in ((5, 9), (10, 10), (8, 12))[: (2 if s < m else 3)]:
            c.rect(x, y, x + 1, y + 1, lp[1]); c.set(x, y, lp[2]); c.set(x + 1, y + 1, lp[0])


def sparkle(c, s, m, f, rng, blobs):
    if s == m:
        for (x, y) in ((4, 7), (12, 6)):
            c.set(x, y, (255, 255, 255, 230))


# ================================================================== catalogue
def fixed_max(name, m):
    return m


PLANTS = {
    # name: (max stage, draw function)
    'wheat': (7, lambda c, n, s, m: grain(c, n, s, m, hexc('d8b440'), hexc('c8a838'))),
    'redwheat': (7, lambda c, n, s, m: grain(c, n, s, m, hexc('d03a2a'), hexc('a83a2a'))),
    'flax': (4, lambda c, n, s, m: grain(c, n, s, m, hexc('5a82e0'), flax=True)),
    'carrots': (3, lambda c, n, s, m: root(c, n, s, m, hexc('f08a20'), 'carrot')),
    'potato': (4, lambda c, n, s, m: root(c, n, s, m, hexc('c8a050'), 'potato')),
    'beetroots': (3, lambda c, n, s, m: root(c, n, s, m, hexc('9a1a30'), 'beet')),
    'dandelion': (4, lambda c, n, s, m: flower(c, n, s, m, 'daisy', hexc('f6d02a'), hexc('d89010'))),
    'rose': (4, lambda c, n, s, m: flower(c, n, s, m, 'rose', hexc('d4202a'), hexc('6a0a10'), thorny=True)),
    'tulip': (4, lambda c, n, s, m: flower(c, n, s, m, 'tulip', hexc('a85ad8'), hexc('5a2a8a'))),
    'cyazint': (4, lambda c, n, s, m: flower(c, n, s, m, 'spike', hexc('3aa8e0'), hexc('2a5ab0'))),
    'blackthorn': (4, lambda c, n, s, m: flower(c, n, s, m, 'thorn', hexc('3a2a4a'), hexc('1a1020'), stem=hexc('4a3020'), thorny=True)),
    'venomilia': (6, lambda c, n, s, m: flower(c, n, s, m, 'star', hexc('9a3ad0'), hexc('c8ff40'), stem=hexc('4a6a2a'), thorny=True)),
    'coffee': (5, lambda c, n, s, m: bush(c, n, s, m, hexc('2f7a2a'), hexc('c42a20'), hexc('7ab040'), 'cherry')),
    'hops': (7, hops),
    'cocoa': (4, cocoa),
    'reed': (3, lambda c, n, s, m: reed(c, n, s, m, hexc('8ad05a'))),
    'stickreed': (4, lambda c, n, s, m: reed(c, n, s, m, hexc('9ab858'), special=resin)),
    'brown_mushroom': (3, lambda c, n, s, m: mushroom(c, n, s, m, hexc('a07850'), flat=True)),
    'red_mushroom': (3, lambda c, n, s, m: mushroom(c, n, s, m, hexc('d02a2a'), spots=hexc('f4f0e8'))),
    'netherwart': (3, lambda c, n, s, m: wart(c, n, s, m, hexc('a82a30'))),
    'terra_wart': (3, lambda c, n, s, m: wart(c, n, s, m, hexc('3ab0a0'))),
    'ferru': (4, lambda c, n, s, m: metal(c, n, s, m, hexc('b8b8b8'))),
    'cyprium': (4, lambda c, n, s, m: metal(c, n, s, m, hexc('d87a3a'))),
    'stagnium': (4, lambda c, n, s, m: metal(c, n, s, m, hexc('d8dce0'))),
    'plumbiscus': (4, lambda c, n, s, m: metal(c, n, s, m, hexc('6a6a8a'))),
    'aurelia': (5, lambda c, n, s, m: metal(c, n, s, m, hexc('f0c830'))),
    'shining': (5, lambda c, n, s, m: metal(c, n, s, m, hexc('e8eef4'))),
    'titanium': (4, lambda c, n, s, m: metal(c, n, s, m, hexc('a8b8d0'))),
    'uranium': (4, lambda c, n, s, m: metal(c, n, s, m, hexc('5ae040'), glow=True)),
    'pumpkin': (4, lambda c, n, s, m: gourd(c, n, s, m, hexc('e08a1a'))),
    'melon': (4, lambda c, n, s, m: gourd(c, n, s, m, hexc('5aa83a'), stripes=hexc('2f6a20'))),
    'eatingplant': (6, eating),
    'weed': (5, weed),
    # generic configured crops (0.1.8.2: 13 of them had no texture)
    'blazereed': (4, lambda c, n, s, m: reed(c, n, s, m, hexc('e0a030'), leafc=hexc('c86a20'), special=flame)),
    'bobs_yer_uncle_ranks_berries': (4, lambda c, n, s, m: bush(c, n, s, m, hexc('3a8a4a'), hexc('2ad870'), hexc('6ab06a'), 'dot', extra=sparkle)),
    'corium': (4, lambda c, n, s, m: bush(c, n, s, m, hexc('5a7a3a'), extra=leather)),
    'corpse_plant': (4, lambda c, n, s, m: bush(c, n, s, m, hexc('7a8a6a'), extra=bones)),
    'creeper_weed': (4, lambda c, n, s, m: bush(c, n, s, m, hexc('4ab03a'), extra=creeper_face)),
    'diareed': (4, lambda c, n, s, m: reed(c, n, s, m, hexc('5a8a7a'), leafc=hexc('4a7a6a'), special=crystal)),
    'egg_plant': (3, lambda c, n, s, m: bush(c, n, s, m, hexc('4a8a3a'), hexc('f4eedc'), hexc('d8e0b0'), 'egg')),
    'ender_blossom': (4, lambda c, n, s, m: flower(c, n, s, m, 'star', hexc('2ab0a0'), hexc('3a1050'), stem=hexc('2a4a4a'))),
    'meat_rose': (4, lambda c, n, s, m: flower(c, n, s, m, 'rose', hexc('e87a90'), hexc('b0303a'), thorny=True)),
    'milk_wart': (3, lambda c, n, s, m: wart(c, n, s, m, hexc('e8e4dc'))),
    'oil_berries': (3, lambda c, n, s, m: bush(c, n, s, m, hexc('2a4a2a'), hexc('2a1a30'), hexc('4a5a3a'), 'dot')),
    'slime_plant': (4, lambda c, n, s, m: bush(c, n, s, m, hexc('5aa04a'), hexc('7ad860'), hexc('7ad860'), 'ball')),
    'spidernip': (4, lambda c, n, s, m: bush(c, n, s, m, hexc('8aa86a'), extra=webs)),
    'tearstalks': (4, lambda c, n, s, m: reed(c, n, s, m, hexc('c8dce8'), leafc=hexc('9ab8c8'), special=tears)),
    'withereed': (4, lambda c, n, s, m: reed(c, n, s, m, hexc('4a4a4a'), leafc=hexc('3a3a32'), special=ash)),
}
for _sap in SAPLINGS:
    PLANTS[_sap] = (5, sapling)
PLANTS['fallback'] = (4, generic_fallback)


def draw(name, s, m, fn):
    c = Canvas(16, 16)
    fn(c, name, s, m)
    light_pass(c)
    return c


# ------------------------------------------------------------------ crop sticks
def sticks(upgraded=False, cross=False):
    c = Canvas(16, 16)
    wood = pal(hexc('b08a52') if not upgraded else hexc('8a7a68'))
    band = pal(hexc('9aa4ae'))
    twine = pal(hexc('d8c890'))
    for x in (3, 12):
        for y in range(2, 16):
            c.set(x, y, wood[2]); c.set(x + 1, y, wood[1])
        c.set(x, 2, wood[3]); c.set(x + 1, 2, wood[2])
    for x in range(3, 14):
        c.set(x, 5, wood[2]); c.set(x, 6, wood[0])
    if cross:
        for k in range(9):
            c.set(4 + k, 7 + k if 7 + k < 16 else 15, wood[2])
            c.set(11 - k, 7 + k if 7 + k < 16 else 15, wood[1])
        c.set(7, 10, wood[3]); c.set(8, 10, wood[3])
    for x in (3, 12):
        if upgraded:
            c.set(x, 4, band[2]); c.set(x + 1, 4, band[1]); c.set(x, 7, band[2]); c.set(x + 1, 7, band[1])
            c.set(x, 12, band[2]); c.set(x + 1, 12, band[1])
        else:
            c.set(x, 4, twine[2]); c.set(x + 1, 4, twine[1]); c.set(x, 7, twine[1]); c.set(x + 1, 7, twine[0])
    if upgraded:
        c.set(8, 5, band[3]); c.set(8, 6, band[1])
    return c


def main():
    os.makedirs(OUT, exist_ok=True)
    made = {}
    for name, (m, fn) in PLANTS.items():
        frames = []
        for s in range(1, m + 1):
            img = draw(name, s, m, fn)
            img.save(os.path.join(OUT, '%s_%d.png' % (name, s)))
            frames.append(img)
        made[name] = frames
    extra = {'stick': sticks(), 'stick_upgraded': sticks(True), 'stick_cross': sticks(False, True), 'stick_upgraded_cross': sticks(True, True)}
    for k, img in extra.items():
        img.save(os.path.join(OUT, k + '.png'))
    total = sum(len(v) for v in made.values()) + len(extra)
    print('crop textures written:', total)
    if PREVIEW:
        os.makedirs(PREVIEW, exist_ok=True)
        rows = []
        names = list(made.keys())
        for name in names:
            row = made[name] + [None] * (7 - len(made[name]))
            rows.extend(row)
        for start in range(0, len(names), 14):
            part = []
            for name in names[start:start + 14]:
                part.extend(made[name] + [None] * (7 - len(made[name])))
            sheet(part, 7, 6).save(os.path.join(PREVIEW, 'crops_new_%02d.png' % (start // 14)))
        sheet(list(extra.values()), 4, 8).save(os.path.join(PREVIEW, 'crop_sticks.png'))
        with open(os.path.join(PREVIEW, 'crops_index.txt'), 'w', encoding='utf-8') as f:
            for start in range(0, len(names), 14):
                f.write('crops_new_%02d.png rows: %s\n' % (start // 14, ', '.join(names[start:start + 14])))


if __name__ == '__main__':
    main()
