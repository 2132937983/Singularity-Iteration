#!/usr/bin/env python3
"""
0.1.10.30: generates the glow masks of the quantum suit (textures/models/armor/<piece>_glow.png).

The glow layer of the suit (SuitGlowLayer) draws this mask additively and full bright over the worn armour, in the glow colour
(palette accent or the suit's own light colour) and with the charge, the breathing and the energy band. The mask holds:
  * the light pixels of the piece (the _accent mask: lamps, visor) at full brightness;
  * circuit lines generated on the outer faces of the armour along the box-UV face edges: a trim line inset one pixel from an
    edge, an L-shaped trace from an edge into the plate with a node at its end, or two parallel traces on large plates.
The base, plating and accent textures (the art of the Blockbench models) are not changed: the lines exist only as light, so
an empty suit shows none of them.

Rules: a face is a unique UV rectangle of a quad; faces that look up or down (|ny| > 0.7), faces smaller than 3 x 3 pixels
and faces under 90 % solid (dithered plates) get no line; a line stays inside its face and on opaque pixels of the base texture. The pattern of a face is chosen from a hash
of its size, its facing and its limb kind (not its position), so the mirrored faces of the left and right limbs carry the same
pattern. Deterministic: the same inputs give the same files.

Usage: glow_masks_1130.py <repo root> [--check]   (--check: report what differs from the files on disk, write nothing)
"""
import hashlib
import json
import os
import sys

from PIL import Image

PIECES = ('quantum_helmet', 'quantum_chestplate', 'quantum_leggings', 'quantum_boots')
TRIM, TRACE, NODE = 150, 185, 255
LAMP = 215


def kind_of(part):
    return part.replace('right_', '').replace('left_', '')


def faces(model):
    """Unique faces: (x0, y0, x1, y1) in texture pixels -> (normal, part kind)."""
    out = {}
    for part, quads in model['parts'].items():
        for q in quads:
            us = [q['v'][i * 5 + 3] for i in range(4)]
            vs = [q['v'][i * 5 + 4] for i in range(4)]
            out.setdefault((min(us), min(vs), max(us), max(vs)), (tuple(q['n']), kind_of(part)))
    return out


def seed(*values):
    return int.from_bytes(hashlib.sha256(repr(values).encode()).digest()[:8], 'big')


class Rng:
    def __init__(self, s):
        self.s = s & 0xFFFFFFFFFFFFFFFF or 1

    def next(self, n):
        self.s ^= (self.s << 13) & 0xFFFFFFFFFFFFFFFF
        self.s ^= self.s >> 7
        self.s ^= (self.s << 17) & 0xFFFFFFFFFFFFFFFF
        return self.s % max(1, n)


def generate(root, name):
    a = os.path.join(root, 'src/main/resources/assets/mio_icif')
    model = json.load(open(os.path.join(a, 'armor_models', name + '.json'), encoding='utf-8'))
    base = Image.open(os.path.join(a, 'textures/models/armor', name + '.png')).convert('RGBA')
    accent = Image.open(os.path.join(a, 'textures/models/armor', name + '_accent.png')).convert('RGBA')
    W, H = base.size
    bp, ap = base.load(), accent.load()
    mask = [[0] * W for _ in range(H)]
    for y in range(H):
        for x in range(W):
            if ap[x, y][3] > 0:
                mask[y][x] = LAMP

    def put(x, y, v, box):
        x0, y0, x1, y1 = box
        if x0 <= x < x1 and y0 <= y < y1 and bp[x, y][3] > 0:
            mask[y][x] = max(mask[y][x], v)

    lines = 0
    for (u0, v0, u1, v1), (n, kind) in sorted(faces(model).items()):
        x0, y0 = int(round(u0 * W)), int(round(v0 * H))
        x1, y1 = int(round(u1 * W)), int(round(v1 * H))
        w, h = x1 - x0, y1 - y0
        if w < 3 or h < 3 or abs(n[1]) > 0.7:
            continue
        # dithered (partly transparent) plates would show a line as loose dots: lines only on solid faces
        solid = sum(1 for y in range(y0, y1) for x in range(x0, x1) if bp[x, y][3] >= 250)
        if solid < 0.9 * w * h:
            continue
        box = (x0, y0, x1, y1)
        facing = tuple(int(round(c)) for c in n)
        r = Rng(seed(name.split('_')[1], kind, w, h, (abs(facing[0]), facing[1], facing[2])))
        area = w * h
        roll = r.next(100)
        if area < 12:
            pattern = 'node' if roll < 25 else None
        elif area <= 40:
            pattern = 'trim' if roll < 50 else 'trace' if roll < 85 else None
        else:
            pattern = 'trace' if roll < 35 else 'double' if roll < 80 else 'trim'
        if pattern is None:
            continue
        lines += 1
        if pattern == 'node':
            put(x0 + 1 + r.next(max(1, w - 2)), y0 + 1 + r.next(max(1, h - 2)), NODE, box)
        elif pattern == 'trim':
            # along the longer side, one pixel in from one of its two edges, with a node at one end
            if w >= h:
                yy = y0 + 1 if r.next(2) == 0 else y1 - 2
                for x in range(x0 + 1, x1 - 1):
                    put(x, yy, TRIM, box)
                put(x0 + 1 if r.next(2) == 0 else x1 - 2, yy, NODE, box)
            else:
                xx = x0 + 1 if r.next(2) == 0 else x1 - 2
                for y in range(y0 + 1, y1 - 1):
                    put(xx, y, TRIM, box)
                put(xx, y0 + 1 if r.next(2) == 0 else y1 - 2, NODE, box)
        else:
            count = 2 if pattern == 'double' else 1
            vertical = h >= w
            for k in range(count):
                # enter from the top (or left) edge, run in, turn, run along, end in a node
                if vertical:
                    sx = x0 + 1 + min(w - 3, r.next(max(1, w - 2)) + 2 * k)
                    run = 2 + r.next(max(1, h - 3))
                    for y in range(y0 + 1, min(y1 - 1, y0 + 1 + run)):
                        put(sx, y, TRACE, box)
                    ty = min(y1 - 2, y0 + run)
                    step = 1 if sx < x0 + w // 2 else -1
                    length = 1 + r.next(max(1, w // 2))
                    for i in range(length + 1):
                        put(sx + step * i, ty, TRACE, box)
                    put(sx + step * length, ty, NODE, box)
                else:
                    sy = y0 + 1 + min(h - 3, r.next(max(1, h - 2)) + 2 * k)
                    run = 2 + r.next(max(1, w - 3))
                    for x in range(x0 + 1, min(x1 - 1, x0 + 1 + run)):
                        put(x, sy, TRACE, box)
                    tx = min(x1 - 2, x0 + run)
                    step = 1 if sy < y0 + h // 2 else -1
                    length = 1 + r.next(max(1, h // 2))
                    for i in range(length + 1):
                        put(tx, sy + step * i, TRACE, box)
                    put(tx, sy + step * length, NODE, box)
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    px = img.load()
    lit = 0
    for y in range(H):
        for x in range(W):
            v = mask[y][x]
            if v > 0:
                px[x, y] = (v, v, v, 255)
                lit += 1
    return img, lit, lines


def main():
    root = sys.argv[1]
    check = '--check' in sys.argv[2:]
    bad = 0
    for name in PIECES:
        img, lit, lines = generate(root, name)
        out = os.path.join(root, 'src/main/resources/assets/mio_icif/textures/models/armor', name + '_glow.png')
        if check:
            old = Image.open(out).convert('RGBA') if os.path.exists(out) else None
            same = old is not None and old.size == img.size and old.tobytes() == img.tobytes()
            print(f'{name}: {"same" if same else "DIFFERENT"} ({lit} lit pixels)')
            bad += 0 if same else 1
            continue
        img.save(out, optimize=True)
        print(f'{name}: {lit} lit pixels, {lines} faces with lines -> {out}')
    if check and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
