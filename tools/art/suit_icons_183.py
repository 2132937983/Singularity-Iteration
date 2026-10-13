#!/usr/bin/env python3
"""0.1.8.3 inventory icons of the Blockbench suit pieces, drawn from the baked models.

The inventory no longer renders the 3D armor model (0.1.7.35 - 0.1.8.2); it shows a flat 32x32
icon again. To keep the icons true to the 3D pieces, this tool renders each baked model
(assets/mio_icif/armor_models/<name>.json + textures/models/armor/<name>.png) with a small
software rasterizer:

* parts are placed with the same pivots as the in-game item renderer (BakedArmorItemRenderer),
* the piece is turned 3/4 toward the viewer (yaw, pitch) and drawn orthographically with a
  z-buffer, nearest texel sampling and cutout alpha, at 8x the icon size,
* faces are shaded by their normal (light from the upper left front),
* the image is reduced to 32x32 by coverage (a pixel is opaque when at least 45 % of its samples
  hit the piece; its colour is the mean of those samples), lightly sharpened toward the source
  palette, and outlined with a dark rim.

Usage: suit_icons_183.py <repo root> <model name>=<icon path under textures/item>[;yaw=..][;pitch=..][;spread=..] ... [--preview <dir>]
  spread  moves the right and left limb parts apart by this many model pixels each
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixart import Canvas, load, sheet  # noqa: E402

PIVOT = {
    'head': (0, 0, 0), 'body': (0, 0, 0),
    'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0),
    'right_hand': (-5, 2, 0), 'left_hand': (5, 2, 0),
    'right_leg': (-1.9, 12, 0), 'left_leg': (1.9, 12, 0),
}
N = 32
SS = 8                      # supersampling factor
LIGHT = (-0.45, -0.75, -0.5)   # toward the light, model space (y down, front = -z)


def norm(v):
    ln = math.sqrt(sum(c * c for c in v)) or 1.0
    return tuple(c / ln for c in v)


def rot(yaw, pitch):
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def f(p):
        x, y, z = p
        x, z = x * cy - z * sy, x * sy + z * cy           # yaw about the vertical axis
        y, z = y * cp - z * sp, y * sp + z * cp           # pitch about the screen x axis
        return x, y, z
    return f


def edge(ax, ay, bx, by, px, py):
    return (bx - ax) * (py - ay) - (by - ay) * (px - ax)


def render(model, tex, yaw, pitch, spread=0.0):
    R = rot(yaw, pitch)
    light = norm(R(norm(LIGHT)))
    tris = []
    for part, quads in model['parts'].items():
        px, py, pz = PIVOT.get(part, (0, 0, 0))
        # exploded view: limbs apart, so the legs of leggings and boots read as two
        px += -spread if part.startswith('right_') else spread if part.startswith('left_') else 0
        for q in quads:
            v = q['v']
            pts = []
            for i in range(4):
                p = R((v[i * 5] + px, v[i * 5 + 1] + py, v[i * 5 + 2] + pz))
                pts.append((p, v[i * 5 + 3], v[i * 5 + 4]))
            n = R(tuple(q['n']))
            if n[2] > 0:                       # back face seen through a gap: light it as the front
                n = (-n[0], -n[1], -n[2])
            shade = 0.56 + 0.5 * max(sum(n[i] * light[i] for i in range(3)), 0.0)
            tris.append((pts[0], pts[1], pts[2], shade))
            tris.append((pts[0], pts[2], pts[3], shade))
    xs = [p[0][0] for t in tris for p in t[:3]]
    ys = [p[0][1] for t in tris for p in t[:3]]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    span = max(maxx - minx, maxy - miny)
    W = N * SS
    scale = (N - 2.4) * SS / span
    ox = W / 2 - (minx + maxx) / 2 * scale
    oy = W / 2 - (miny + maxy) / 2 * scale
    zbuf = [1e9] * (W * W)
    col = [None] * (W * W)
    tw, th = tex.w, tex.h
    for a, b, c, shade in tris:
        (pa, ua, va), (pb, ub, vb), (pc, uc, vc) = a, b, c
        ax, ay = pa[0] * scale + ox, pa[1] * scale + oy
        bx, by = pb[0] * scale + ox, pb[1] * scale + oy
        cx, cy = pc[0] * scale + ox, pc[1] * scale + oy
        area = edge(ax, ay, bx, by, cx, cy)
        if abs(area) < 1e-9:
            continue
        x0, x1 = max(0, int(min(ax, bx, cx))), min(W - 1, int(max(ax, bx, cx)) + 1)
        y0, y1 = max(0, int(min(ay, by, cy))), min(W - 1, int(max(ay, by, cy)) + 1)
        for y in range(y0, y1 + 1):
            sy = y + 0.5
            for x in range(x0, x1 + 1):
                sx = x + 0.5
                w0 = edge(bx, by, cx, cy, sx, sy) / area
                w1 = edge(cx, cy, ax, ay, sx, sy) / area
                w2 = 1 - w0 - w1
                if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                    continue
                z = w0 * pa[2] + w1 * pb[2] + w2 * pc[2]
                i = y * W + x
                if z >= zbuf[i] - 1e-4:
                    continue
                u = w0 * ua + w1 * ub + w2 * uc
                v = w0 * va + w1 * vb + w2 * vc
                tx = min(tw - 1, max(0, int(u * tw)))
                ty = min(th - 1, max(0, int(v * th)))
                t = tex.px[ty * tw + tx]
                if t[3] < 26:
                    continue
                zbuf[i] = z
                col[i] = (min(255, t[0] * shade), min(255, t[1] * shade), min(255, t[2] * shade))
    return col, W


def reduce(col, W):
    out = Canvas(N, N)
    for y in range(N):
        for x in range(N):
            hits = []
            for sy in range(SS):
                row = (y * SS + sy) * W + x * SS
                for sx in range(SS):
                    c = col[row + sx]
                    if c is not None:
                        hits.append(c)
            if len(hits) >= SS * SS * 0.45:
                r = sum(h[0] for h in hits) / len(hits)
                g = sum(h[1] for h in hits) / len(hits)
                b = sum(h[2] for h in hits) / len(hits)
                # push toward the most common sample (keeps the pixel-art palette crisp)
                best = max(set(hits), key=hits.count)
                k = 0.45
                out.put(x, y, (int(r + (best[0] - r) * k), int(g + (best[1] - g) * k), int(b + (best[2] - b) * k), 255))
    return out


def finish(c, lift):
    """Lift the darks (the nano suit is near black), then a rim light on the upper-left edges."""
    src = list(c.px)
    for y in range(N):
        for x in range(N):
            p = src[y * N + x]
            if not p[3]:
                continue
            r, g, b = (255 * (ch / 255) ** lift for ch in p[:3])
            up = src[(y - 1) * N + x] if y > 0 else (0, 0, 0, 0)
            left = src[y * N + x - 1] if x > 0 else (0, 0, 0, 0)
            if not up[3] or not left[3]:
                r, g, b = r + (255 - r) * 0.28, g + (255 - g) * 0.28, b + (255 - b) * 0.28
            c.put(x, y, (int(r), int(g), int(b), 255))


def outline(c):
    src = list(c.px)
    for y in range(N):
        for x in range(N):
            if src[y * N + x][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < N and 0 <= ny < N and src[ny * N + nx][3]:
                    n = src[ny * N + nx]
                    c.put(x, y, (int(n[0] * 0.22), int(n[1] * 0.22), int(n[2] * 0.25) + 8, 255))
                    break


def icon(root, name, yaw, pitch, spread=0.0):
    res = os.path.join(root, 'src/main/resources/assets/mio_icif')
    with open(os.path.join(res, 'armor_models', name + '.json'), encoding='utf-8') as f:
        model = json.load(f)
    tex = load(os.path.join(res, 'textures/models/armor', name + '.png'))
    col, W = render(model, tex, yaw, pitch, spread)
    img = reduce(col, W)
    # dark sets (mean luminance below 70) get their darks lifted so the icon reads on a slot
    lum = [0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2] for p in img.px if p[3]]
    finish(img, 0.72 if lum and sum(lum) / len(lum) < 70 else 0.95)
    outline(img)
    return img


def main():
    args = sys.argv[1:]
    preview = None
    if '--preview' in args:
        i = args.index('--preview')
        preview = args[i + 1]
        args = args[:i] + args[i + 2:]
    root = args[0]
    made = []
    for spec in args[1:]:
        name, rest = spec.split('=', 1)
        fields = rest.split(';')
        target = fields[0]
        yaw, pitch, spread = -16.0, 8.0, 0.0
        for f in fields[1:]:
            k, v = f.split('=')
            if k == 'yaw':
                yaw = float(v)
            elif k == 'pitch':
                pitch = float(v)
            elif k == 'spread':
                spread = float(v)
        img = icon(root, name, yaw, pitch, spread)
        path = os.path.join(root, 'src/main/resources/assets/mio_icif/textures/item', target + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        made.append(img)
        print(name, '->', target)
    if preview:
        os.makedirs(preview, exist_ok=True)
        sheet(made, 4, 6, bg=((139, 139, 139, 255), (139, 139, 139, 255))).save(os.path.join(preview, 'suit_icons_183.png'))


if __name__ == '__main__':
    main()
