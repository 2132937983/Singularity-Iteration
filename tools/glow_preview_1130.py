#!/usr/bin/env python3
"""
0.1.10.30: offline preview of the worn quantum suit with its glow (no game needed).

Renders the four baked pieces in the standing pose (orthographic, front / back / three-quarter) in three looks:
  day     the armour texture with a simple directional light (the world-lit armour layer);
  night   the armour at night light (x 0.16) without glow: the dark shape of 0.1.10.29;
  glow    night plus the glow mask added in the glow colour, with the energy band at height --band (model pixels, -8 head top
          .. 24 feet; omit for none) and the charge colour rules of SuitGlowRules (--charge, 0..1).
With --palette=<primary hex>,<accent hex> the plating and accent layers are drawn tinted (the palette look) instead of the base.

Usage: glow_preview_1130.py <repo root> <out.png> [--mask=glow|accent] [--charge=1] [--band=<y>] [--palette=3A3F46,37E35A] [--colour=3CB4FF]
"""
import json
import math
import os
import sys

from PIL import Image

PIECES = ('quantum_helmet', 'quantum_chestplate', 'quantum_leggings', 'quantum_boots')
PIVOT = {'head': (0, 0, 0), 'body': (0, 0, 0), 'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0),
         'right_hand': (-5, 2, 0), 'left_hand': (5, 2, 0), 'right_leg': (-1.9, 12, 0), 'left_leg': (1.9, 12, 0)}
VIEWS = [('front', 0, 0), ('back', 180, 0), ('3q', 35, 12)]
CELL_W, CELL_H, SCALE = 200, 300, 7.0
AMBER, RED = (1.0, 0.66, 0.22), (1.0, 0.23, 0.19)


def hexrgb(s):
    v = int(s, 16)
    return ((v >> 16) & 255) / 255, ((v >> 8) & 255) / 255, (v & 255) / 255


def charge_colour(base, charge):
    # the colour rule of SuitGlowRules.colour: the base down to 50 %, towards amber to 25 %, towards red below
    def mix(a, b, t):
        return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))
    if charge >= 0.5:
        return base
    if charge >= 0.25:
        return mix(base, AMBER, (0.5 - charge) / 0.25)
    return mix(AMBER, RED, (0.25 - charge) / 0.25)


def load(root, mask_kind, palette):
    a = os.path.join(root, 'src/main/resources/assets/mio_icif')
    quads = []
    for name in PIECES:
        model = json.load(open(os.path.join(a, 'armor_models', name + '.json'), encoding='utf-8'))
        tex_dir = os.path.join(a, 'textures/models/armor')
        layers = [Image.open(os.path.join(tex_dir, name + '_plating.png' if palette else name + '.png')).convert('RGBA')]
        if palette:
            layers.append(Image.open(os.path.join(tex_dir, name + '_accent.png')).convert('RGBA'))
        mask = Image.open(os.path.join(tex_dir, name + '_' + mask_kind + '.png')).convert('RGBA')
        for part, qs in model['parts'].items():
            px, py, pz = PIVOT[part]
            for q in qs:
                v = q['v']
                pts = [(v[i * 5] + px, v[i * 5 + 1] + py, v[i * 5 + 2] + pz) for i in range(4)]
                uvs = [(v[i * 5 + 3], v[i * 5 + 4]) for i in range(4)]
                quads.append((pts, uvs, q['n'], layers, mask))
    return quads


def render(quads, yaw, pitch, look, colour, band, palette):
    W, H = CELL_W, CELL_H
    img = [[(14, 16, 22)] * W for _ in range(H)]
    depth = [[-1e9] * W for _ in range(H)]
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def view(p):
        x, y, z = p
        x1 = x * cy + z * sy
        d = -z * cy + x * sy
        return x1, y * cp + d * sp, d * cp - y * sp

    ambient = 0.16 if look != 'day' else 1.0
    for pts, uvs, n, layers, mask in quads:
        vp = [view(p) for p in pts]
        nv = view(n)
        shade = min(1.0, 0.45 + 0.4 * abs(nv[2]) + 0.25 * max(0.0, -nv[1]) + 0.1 * max(0.0, nv[0]))
        scr = [(W / 2 + x * SCALE, 40 + (y + 8) * SCALE, z) for x, y, z in vp]
        for tri in ((0, 1, 2), (0, 2, 3)):
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = (scr[i] for i in tri)
            uv = [uvs[i] for i in tri]
            ys = [pts[i][1] for i in tri]
            area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if abs(area) < 1e-9:
                continue
            for yy in range(max(0, int(min(y0, y1, y2))), min(H - 1, int(max(y0, y1, y2)) + 1) + 1):
                for xx in range(max(0, int(min(x0, x1, x2))), min(W - 1, int(max(x0, x1, x2)) + 1) + 1):
                    px_, py_ = xx + 0.5, yy + 0.5
                    w0 = ((x1 - px_) * (y2 - py_) - (x2 - px_) * (y1 - py_)) / area
                    w1 = ((x2 - px_) * (y0 - py_) - (x0 - px_) * (y2 - py_)) / area
                    w2 = 1 - w0 - w1
                    if min(w0, w1, w2) < -1e-6:
                        continue
                    z = w0 * z0 + w1 * z1 + w2 * z2
                    if z <= depth[yy][xx]:
                        continue
                    u = w0 * uv[0][0] + w1 * uv[1][0] + w2 * uv[2][0]
                    v = w0 * uv[0][1] + w1 * uv[1][1] + w2 * uv[2][1]
                    base = layers[0]
                    tx, ty = min(base.width - 1, int(u * base.width)), min(base.height - 1, int(v * base.height))
                    r, g, b, al = base.getpixel((tx, ty))
                    if al < 20:
                        continue
                    if palette:
                        r, g, b = r * palette[0][0], g * palette[0][1], b * palette[0][2]
                        ar, ag, ab, aa = layers[1].getpixel((tx, ty))
                        if aa > 0:
                            r, g, b = ar * palette[1][0], ag * palette[1][1], ab * palette[1][2]
                    depth[yy][xx] = z
                    k = shade * ambient
                    col = [r * k, g * k, b * k]
                    if look == 'glow':
                        mr, mg, mb, ma = mask.getpixel((min(mask.width - 1, int(u * mask.width)), min(mask.height - 1, int(v * mask.height))))
                        if ma > 0:
                            my = w0 * ys[0] + w1 * ys[1] + w2 * ys[2]
                            bright = 0.9 + (0.6 * math.exp(-((my - band) / 2.5) ** 2) if band is not None else 0)
                            for i in range(3):
                                col[i] += mr * colour[i] * bright
                    img[yy][xx] = tuple(min(255, int(c)) for c in col)
    return img


def main():
    args = [x for x in sys.argv[1:] if not x.startswith('--')]
    opts = dict(x[2:].split('=', 1) for x in sys.argv[1:] if x.startswith('--'))
    root, out = args[0], args[1]
    palette = [hexrgb(c) for c in opts['palette'].split(',')] if 'palette' in opts else None
    base_colour = hexrgb(opts.get('colour', palette and opts['palette'].split(',')[1] or '3CB4FF'))
    colour = charge_colour(base_colour, float(opts.get('charge', 1)))
    band = float(opts['band']) if 'band' in opts else None
    quads = load(root, opts.get('mask', 'glow'), palette)
    looks = ['day', 'night', 'glow']
    sheet = Image.new('RGB', (CELL_W * len(VIEWS), CELL_H * len(looks)), (8, 9, 12))
    for li, look in enumerate(looks):
        for vi, (_, yaw, pitch) in enumerate(VIEWS):
            cell = render(quads, yaw, pitch, look, colour, band, palette)
            im = Image.new('RGB', (CELL_W, CELL_H))
            im.putdata([p for row in cell for p in row])
            sheet.paste(im, (vi * CELL_W, li * CELL_H))
    sheet.save(out)
    print('wrote', out)


if __name__ == '__main__':
    main()
