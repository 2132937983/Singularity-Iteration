#!/usr/bin/env python3
"""
0.1.11.28: converts a Blockbench model (any format; cubes with per-face or box UV, element and group rotations of any
angle and axis, inflate) into a Wavefront OBJ + MTL for NeoForge's OBJ model loader ("loader": "neoforge:obj"), and
writes its embedded texture. Java block / item models only allow one rotation axis and angles of -45..45 in steps of
22.5; the OBJ keeps every angle, and it is still a baked model with the usual display transforms.

Geometry:
  * Every face Blockbench stores (box UV cubes store their computed face rectangles too) becomes one quad. A face
    without a texture is left out.
  * Face UV follows Blockbench's face convention (the Java block model one): seen from outside, u to the right, v down;
    up is seen from above with north at the top, down from below with south at the top; a face rotation turns it.
  * A point p (Blockbench pixels) is placed as  target + scale * R_y(turn) * (p - centre), then divided by 16: OBJ
    units are blocks, as in a block model. --centre defaults to the centre of the model's bounds.
  * Quads wind counter-clockwise seen from outside; each has its outward normal.
  * The MTL has one material whose map_Kd is "#texture": the model JSON names the texture.

Usage: bb_to_obj.py <in.bbmodel> <out.obj> [--png=<out.png>] [--scale=<s>] [--turn=<deg about y>]
                    [--target=x,y,z (pixels, default 8,8,8)] [--centre=x,y,z (Blockbench pixels)]
"""
import base64
import json
import math
import os
import sys


def rot_matrix(rx, ry, rz):
    rx, ry, rz = (math.radians(a) for a in (rx, ry, rz))
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    X = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mul(Z, mul(Y, X))


def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def rotate_about(p, rotation, origin):
    if not rotation or not any(rotation):
        return list(p)
    q = apply(rot_matrix(*rotation), [p[i] - origin[i] for i in range(3)])
    return [q[i] + origin[i] for i in range(3)]


def face_corners(face, f, t):
    """Corners for (u1,v1), (u2,v1), (u2,v2), (u1,v2), and the outward normal."""
    (x1, y1, z1), (x2, y2, z2) = f, t
    return {
        'north': ([(x2, y2, z1), (x1, y2, z1), (x1, y1, z1), (x2, y1, z1)], (0, 0, -1)),
        'south': ([(x1, y2, z2), (x2, y2, z2), (x2, y1, z2), (x1, y1, z2)], (0, 0, 1)),
        'east': ([(x2, y2, z2), (x2, y2, z1), (x2, y1, z1), (x2, y1, z2)], (1, 0, 0)),
        'west': ([(x1, y2, z1), (x1, y2, z2), (x1, y1, z2), (x1, y1, z1)], (-1, 0, 0)),
        'up': ([(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)], (0, 1, 0)),
        'down': ([(x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1)], (0, -1, 0)),
    }[face]


def opts(argv):
    out = {}
    for a in argv:
        if a.startswith('--') and '=' in a:
            k, v = a[2:].split('=', 1)
            out[k] = v
    return out


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    if len(args) < 2:
        print(__doc__)
        sys.exit(2)
    o = opts(sys.argv[1:])
    with open(args[0], encoding='utf-8') as fh:
        d = json.load(fh)
    res_w, res_h = d['resolution']['width'], d['resolution']['height']
    groups = {g['uuid']: g for g in d.get('groups', [])}
    owner = {}

    def walk(nodes, chain):
        for n in nodes:
            if isinstance(n, dict):
                walk(n.get('children', []), chain + [groups[n['uuid']]])
            else:
                owner[n] = chain
    walk(d.get('outliner', []), [])

    quads = []   # (4 x (point, u, v), normal)
    for e in d['elements']:
        if e.get('export') is False or e.get('visibility') is False or e.get('type', 'cube') != 'cube':
            continue
        chain = owner.get(e['uuid'], [])
        inf = e.get('inflate', 0) or 0
        f = [c - inf for c in e['from']]
        t = [c + inf for c in e['to']]

        def place(p):
            q = rotate_about(p, e.get('rotation', [0, 0, 0]), e.get('origin', [0, 0, 0]))
            for g in reversed(chain):
                q = rotate_about(q, g.get('rotation', [0, 0, 0]), g.get('origin', [0, 0, 0]))
            return q

        def turn_normal(n):
            n = apply(rot_matrix(*e['rotation']), list(n)) if any(e.get('rotation', [0, 0, 0])) else list(n)
            for g in reversed(chain):
                if any(g.get('rotation', [0, 0, 0])):
                    n = apply(rot_matrix(*g['rotation']), n)
            return n

        for face, data in e.get('faces', {}).items():
            if data.get('texture') is None or 'uv' not in data:
                continue
            u1, v1, u2, v2 = data['uv']
            if abs(u1 - u2) < 1e-9 and abs(v1 - v2) < 1e-9:
                continue
            uvs = [(u1, v1), (u2, v1), (u2, v2), (u1, v2)]
            r = int(data.get('rotation', 0) or 0) // 90 % 4
            if r:
                uvs = uvs[-r:] + uvs[:-r]
            corners, normal = face_corners(face, f, t)
            pts = [place(c) for c in corners]
            if max(abs(pts[i][k] - pts[j][k]) for i in range(4) for j in range(4) for k in range(3)) < 1e-9:
                continue
            quads.append(([(pts[k], uvs[k][0] / res_w, uvs[k][1] / res_h) for k in range(4)], turn_normal(normal)))

    allp = [p for q, _ in quads for p, _, _ in q]
    lo = [min(p[k] for p in allp) for k in range(3)]
    hi = [max(p[k] for p in allp) for k in range(3)]
    centre = [float(x) for x in o['centre'].split(',')] if 'centre' in o else [(lo[k] + hi[k]) / 2 for k in range(3)]
    target = [float(x) for x in o.get('target', '8,8,8').split(',')]
    scale = float(o.get('scale', 1))
    turn = rot_matrix(0, float(o.get('turn', 0)), 0)

    def final(p):
        q = apply(turn, [p[k] - centre[k] for k in range(3)])
        return [(target[k] + scale * q[k]) / 16 for k in range(3)]

    out_obj = args[1]
    base = os.path.splitext(os.path.basename(out_obj))[0]
    lines = ['# ' + os.path.basename(args[0]) + ' (tools/bb_to_obj.py)', 'mtllib ' + base + '.mtl', 'usemtl texture']
    vi = vt = vn = 0
    flo, fhi = [9e9] * 3, [-9e9] * 3
    for quad, normal in quads:
        pts = [final(p) for p, _, _ in quad]
        n = apply(turn, normal)
        ln = math.sqrt(sum(c * c for c in n)) or 1
        n = [c / ln for c in n]
        a, b, c = pts[0], pts[1], pts[2]
        cr = [(b[1] - a[1]) * (c[2] - a[2]) - (b[2] - a[2]) * (c[1] - a[1]),
              (b[2] - a[2]) * (c[0] - a[0]) - (b[0] - a[0]) * (c[2] - a[2]),
              (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])]
        order = [0, 1, 2, 3] if sum(cr[k] * n[k] for k in range(3)) > 0 else [3, 2, 1, 0]   # counter-clockwise outside
        for k in order:
            p = pts[k]
            lines.append('v %.5f %.5f %.5f' % tuple(p))
            for j in range(3):
                flo[j] = min(flo[j], p[j]); fhi[j] = max(fhi[j], p[j])
        for k in order:
            lines.append('vt %.6f %.6f' % (quad[k][1], 1 - quad[k][2]))
        lines.append('vn %.5f %.5f %.5f' % tuple(n))
        vn += 1
        lines.append('f ' + ' '.join('%d/%d/%d' % (vi + i + 1, vt + i + 1, vn) for i in range(4)))
        vi += 4
        vt += 4
    with open(out_obj, 'w', encoding='utf-8') as fh:
        fh.write('\n'.join(lines) + '\n')
    with open(os.path.join(os.path.dirname(out_obj), base + '.mtl'), 'w', encoding='utf-8') as fh:
        fh.write('newmtl texture\nmap_Kd #texture\n')
    if 'png' in o:
        src = next((t.get('source', '') for t in d.get('textures', []) if t.get('source', '').startswith('data:image/png;base64,')), '')
        if src:
            with open(o['png'], 'wb') as fh:
                fh.write(base64.b64decode(src.split(',', 1)[1]))
    print('%s: %d quads, bounds (px) x %.2f..%.2f y %.2f..%.2f z %.2f..%.2f' % (
        out_obj, len(quads), flo[0] * 16, fhi[0] * 16, flo[1] * 16, fhi[1] * 16, flo[2] * 16, fhi[2] * 16))


if __name__ == '__main__':
    main()
