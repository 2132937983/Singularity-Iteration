#!/usr/bin/env python3
"""
0.1.9.0: offline preview of baked armor geometry and core attachments (no dependencies).

Renders orthographic views of one or more baked parts (armor_models/<name>.json) and frame core
attachments (armor_models/attachments/<id>.json) into a PNG, so the fit of an attachment on its
suit piece can be checked without starting the game. Textures are sampled nearest; faces get a
simple directional light; emissive segments are drawn unlit.

Usage: armor_preview.py <repo root> <out.png> <part> <layer> [<layer> ...] [--state=<0..1>] [--time=<s>]
  part    head | body (the arms are drawn with body space offsets when part is body)
  layer   base:<name>            a baked armor model
          attach:<id>            an attachment file
  state   deploy / thrust state used for the animated segments (default 1)
Views: front, side (right), back, top, three-quarter.
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import read_png, write_png  # noqa: E402

PIVOT = {'head': (0, 0, 0), 'body': (0, 0, 0), 'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0),
         'right_hand': (-5, 2, 0), 'left_hand': (5, 2, 0)}
VIEWS = [('front', 0, 0), ('side', 90, 0), ('back', 180, 0), ('top', 0, 89), ('3q', 35, 25), ('3q_back', 145, 20)]
CELL = 220
SCALE = 7.0


def rot_axis(v, axis, deg):
    """Right-handed rotation of v about the unit axis (same as Quaternionf.rotateAxis)."""
    a = math.radians(deg)
    x, y, z = axis
    n = math.sqrt(x * x + y * y + z * z) or 1
    x, y, z = x / n, y / n, z / n
    c, s = math.cos(a), math.sin(a)
    vx, vy, vz = v
    dot = x * vx + y * vy + z * vz
    cx, cy, cz = y * vz - z * vy, z * vx - x * vz, x * vy - y * vx
    return (vx * c + cx * s + x * dot * (1 - c), vy * c + cy * s + y * dot * (1 - c), vz * c + cz * s + z * dot * (1 - c))


def anim_angle(anim, state, time):
    t = anim.get('type')
    if t == 'deploy':
        return anim['closed'] + (anim['open'] - anim['closed']) * state
    if t == 'spin':
        return anim['speed'] * time
    if t == 'sweep':
        return anim['amplitude'] * math.sin(time * 2 * math.pi / anim['period'])
    return 0.0


def load_layers(root, layers, part_filter, state, time):
    """List of (verts[4] model space, uv[4], normal, texture, emissive)."""
    A = os.path.join(root, 'src/main/resources/assets/mio_icif')
    out = []
    for layer in layers:
        kind, name = layer.split(':', 1)
        if kind == 'base':
            m = json.load(open(os.path.join(A, 'armor_models', name + '.json'), encoding='utf-8'))
            tex = os.path.join(A, 'textures/models/armor', name + '.png')
            segs = [{'part': p, 'quads': q, 'emissive': False} for p, q in m['parts'].items()]
        else:
            m = json.load(open(os.path.join(A, 'armor_models/attachments', name + '.json'), encoding='utf-8'))
            ns, path = m['texture'].split(':')
            tex = os.path.join(A, path)
            segs = m['segments']
        for s in segs:
            part = s['part']
            if part_filter == 'head' and part != 'head':
                continue
            if part_filter == 'body' and part == 'head':
                continue
            px, py, pz = PIVOT[part]
            anim = s.get('anim')
            for q in s['quads']:
                v = q['v']
                pts, uvs = [], []
                for i in range(4):
                    p = (v[i * 5], v[i * 5 + 1], v[i * 5 + 2])
                    if anim:
                        pv = anim['pivot']
                        r = rot_axis((p[0] - pv[0], p[1] - pv[1], p[2] - pv[2]), anim['axis'], anim_angle(anim, state, time))
                        p = (r[0] + pv[0], r[1] + pv[1], r[2] + pv[2])
                    pts.append((p[0] + px, p[1] + py, p[2] + pz))
                    uvs.append((v[i * 5 + 3], v[i * 5 + 4]))
                n = q['n']
                if anim:
                    n = rot_axis(n, anim['axis'], anim_angle(anim, state, time))
                out.append((pts, uvs, n, tex, bool(s.get('emissive'))))
    return out


def render(quads, yaw, pitch, textures):
    W = H = CELL
    color = [[(26, 30, 36, 255)] * W for _ in range(H)]
    depth = [[-1e9] * W for _ in range(H)]
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def view(p):
        # model space: +x = left of the wearer, y down, front at -z. yaw 0 looks at the front
        # (screen right = +x), 90 at the left side, 180 at the back; pitch > 0 looks from above.
        # Returns screen x, screen y, depth (larger = nearer).
        x, y, z = p
        x1 = x * cy + z * sy
        d = -z * cy + x * sy
        y1 = y * cp + d * sp
        d1 = d * cp - y * sp
        return x1, y1, d1

    light = (0.35, -0.8, -0.5)
    ln = math.sqrt(sum(c * c for c in light))
    light = tuple(c / ln for c in light)
    for pts, uvs, n, tex, emissive in quads:
        vp = [view(p) for p in pts]
        nv = view(n)
        shade = 1.0 if emissive else min(1.0, 0.45 + 0.4 * abs(nv[2]) + 0.25 * max(0.0, -nv[1]) + 0.1 * max(0.0, nv[0]))
        tw, th, tpx = textures[tex]
        scr = [(W / 2 + x * SCALE, H * 0.55 + y * SCALE, z) for x, y, z in vp]
        for tri in ((0, 1, 2), (0, 2, 3)):
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = (scr[i] for i in tri)
            uv = [uvs[i] for i in tri]
            area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if abs(area) < 1e-9:
                continue
            minx, maxx = max(0, int(min(x0, x1, x2))), min(W - 1, int(max(x0, x1, x2)) + 1)
            miny, maxy = max(0, int(min(y0, y1, y2))), min(H - 1, int(max(y0, y1, y2)) + 1)
            for yy in range(miny, maxy + 1):
                for xx in range(minx, maxx + 1):
                    px_, py_ = xx + 0.5, yy + 0.5
                    w0 = ((x1 - px_) * (y2 - py_) - (x2 - px_) * (y1 - py_)) / area
                    w1 = ((x2 - px_) * (y0 - py_) - (x0 - px_) * (y2 - py_)) / area
                    w2 = 1 - w0 - w1
                    if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                        continue
                    z = w0 * z0 + w1 * z1 + w2 * z2
                    if z <= depth[yy][xx]:
                        continue
                    u = w0 * uv[0][0] + w1 * uv[1][0] + w2 * uv[2][0]
                    v = w0 * uv[0][1] + w1 * uv[1][1] + w2 * uv[2][1]
                    tx = min(tw - 1, max(0, int(u * tw)))
                    ty = min(th - 1, max(0, int(v * th)))
                    r, g, b, a = tpx[ty][tx]
                    if a < 20:
                        continue
                    depth[yy][xx] = z
                    color[yy][xx] = (r * shade, g * shade, b * shade, 255)
    return color


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    opts = dict(a[2:].split('=', 1) for a in sys.argv[1:] if a.startswith('--'))
    root, out, part = args[0], args[1], args[2]
    layers = args[3:]
    state = float(opts.get('state', 1))
    time = float(opts.get('time', 0.0))
    views = opts.get('views')
    view_list = [v for v in VIEWS if not views or v[0] in views.split(',')]
    quads = load_layers(root, layers, part, state, time)
    textures = {}
    for _, _, _, tex, _ in quads:
        if tex not in textures:
            textures[tex] = read_png(tex)
    cols = 3
    rows = (len(view_list) + cols - 1) // cols
    sheet = [[(10, 12, 16, 255)] * (CELL * cols) for _ in range(CELL * rows)]
    for i, (name, yaw, pitch) in enumerate(view_list):
        img = render(quads, yaw, pitch, textures)
        ox, oy = (i % cols) * CELL, (i // cols) * CELL
        for y in range(CELL):
            sheet[oy + y][ox:ox + CELL] = img[y]
    write_png(out, sheet)
    print('wrote', out, len(quads), 'quads')


if __name__ == '__main__':
    main()
