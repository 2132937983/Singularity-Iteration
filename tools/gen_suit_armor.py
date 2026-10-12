#!/usr/bin/env python3
"""
0.1.8.0: converts suit Blockbench models (modded_entity, box UV) into the baked armor geometry
that the game loads (assets/mio_icif/armor_models/<name>.json) and writes the textures.

This tool replaces gen_nano_armor.py. Differences:
  * Group rotations apply. Each element is rotated around its own origin, then around the origin
    of each group that holds it, from the innermost group to the outermost group. (The quantum
    boots have a root group turned -180 deg; the quantum shoulder plates are tilted +-5 deg.)
  * An embedded texture (data:image/png;base64) is written as the texture file. An external PNG
    next to the model can be given instead.
  * Hand layer: an element in a "护手" (gauntlet) group whose lowest... highest point is at or
    below BB y = 16 covers the hand. It goes to the part "right_hand" or "left_hand". The renderer
    draws these parts with the arm transform, only when the hand cover option is on.

Geometry rules (match ModelPart.Cube so the result looks like a Blockbench Java export):
  * MC model space = (bb.x, 24 - bb.y, bb.z), y down, 1 unit = 1/16 block.
  * Box UV slots and the vertex -> UV corner mapping follow ModelPart.Cube / Polygon.
  * Part: helmet -> head; chest "胸甲主体" -> body, gauntlets -> arms or hands by side and height,
    other chest elements -> arms by side; leggings "裤裆" -> body, other leg elements -> legs by side;
    boots -> legs by side. Elements on the -x side go to the right limb (the right arm/leg of a
    ModelPart model is at -x).

Usage: gen_suit_armor.py <repo root> <name>=<bbmodel>[;turn][;png=<file>] ...
  name    output name, for example quantum_helmet (the piece type is the part after the last "_")
  turn    turn the piece 180 deg about the vertical axis before the split (old nano boots)
  png     use this external texture instead of the embedded one
"""
import base64
import json
import math
import os
import shutil
import sys

PIVOT = {  # MC model space
    'head': (0, 0, 0), 'body': (0, 0, 0),
    'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0),
    'right_hand': (-5, 2, 0), 'left_hand': (5, 2, 0),
    'right_leg': (-1.9, 12, 0), 'left_leg': (1.9, 12, 0),
}
HAND_TOP = 16.0   # BB y: a gauntlet element at or below this height covers the hand


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


def rotate_about(point, rotation, origin):
    if not any(rotation):
        return list(point)
    p = apply(rot_matrix(*rotation), [point[0] - origin[0], point[1] - origin[1], point[2] - origin[2]])
    return [p[0] + origin[0], p[1] + origin[1], p[2] + origin[2]]


def element_quads(e, res_w, res_h):
    """Six quads of one element in Blockbench space: list of (4 x (bbx, bby, bbz, u, v), face)."""
    x1, y1, z1 = e['from']
    x2, y2, z2 = e['to']
    w, h, d = x2 - x1, y2 - y1, z2 - z1
    u, v = e.get('uv_offset', [0, 0])
    if e.get('mirror_uv'):
        x1, x2 = x2, x1
    top, bot = y2, y1
    V = [
        (x1, top, z1), (x2, top, z1), (x2, bot, z1), (x1, bot, z1),
        (x1, top, z2), (x2, top, z2), (x2, bot, z2), (x1, bot, z2),
    ]
    polys = [
        ((5, 4, 0, 1), u + d, v, u + d + w, v + d, 'top'),
        ((2, 3, 7, 6), u + d + w, v + d, u + d + w + w, v, 'bottom'),
        ((0, 4, 7, 3), u, v + d, u + d, v + d + h, 'west'),
        ((1, 0, 3, 2), u + d, v + d, u + d + w, v + d + h, 'north'),
        ((5, 1, 2, 6), u + d + w, v + d, u + d + w + d, v + d + h, 'east'),
        ((4, 5, 6, 7), u + d + w + d, v + d, u + d + w + d + w, v + d + h, 'south'),
    ]
    out = []
    for idx, a1, b1, a2, b2, face in polys:
        uvs = [(a2, b1), (a1, b1), (a1, b2), (a2, b2)]   # Polygon remap order
        quad = [(V[i][0], V[i][1], V[i][2], uvs[k][0] / res_w, uvs[k][1] / res_h) for k, i in enumerate(idx)]
        out.append((quad, face))
    return out


def part_of(piece, chain, centre_x, max_y):
    side = 'right' if centre_x < 0 else 'left'
    if piece == 'helmet':
        return 'head'
    if piece == 'chestplate':
        if '胸甲主体' in chain:
            return 'body'
        if any('护手' in name for name in chain) and max_y <= HAND_TOP + 1e-6:
            return side + '_hand'
        return side + '_arm'
    if piece == 'leggings':
        return 'body' if '裤裆' in chain else side + '_leg'
    return side + '_leg'


def area(v):
    p = [v[i * 5:i * 5 + 3] for i in range(4)]

    def cross(a, b):
        return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
    s = [0, 0, 0]
    for i in range(4):
        c = cross(p[i], p[(i + 1) % 4])
        s = [s[j] + c[j] for j in range(3)]
    return math.sqrt(sum(x * x for x in s)) / 2


def convert(path, name, turn):
    with open(path, encoding='utf-8') as f:
        d = json.load(f)
    piece = name.rsplit('_', 1)[-1]
    res_w, res_h = d['resolution']['width'], d['resolution']['height']
    groups = {g['uuid']: g for g in d.get('groups', [])}
    owner = {}

    def walk(nodes, chain):
        for n in nodes:
            if isinstance(n, dict):
                walk(n.get('children', []), chain + [groups[n['uuid']]])
            else:
                owner[n] = chain

    walk(d['outliner'], [])
    parts = {}
    for e in d['elements']:
        if e.get('export') is False or e.get('visibility') is False:
            continue
        chain = owner.get(e['uuid'], [])
        names = [g.get('name', '') for g in chain]

        def place(point):
            p = rotate_about(point, e.get('rotation', [0, 0, 0]), e.get('origin', [0, 0, 0]))
            for g in reversed(chain):   # innermost group first
                p = rotate_about(p, g.get('rotation', [0, 0, 0]), g.get('origin', [0, 0, 0]))
            if turn:
                p = [-p[0], p[1], -p[2]]
            return p

        quads = element_quads(e, res_w, res_h)
        placed = [[(place((x, y, z)), uu, vv) for (x, y, z, uu, vv) in quad] for quad, _ in quads]
        xs = [p[0] for q in placed for (p, _, _) in q]
        ys = [p[1] for q in placed for (p, _, _) in q]
        part = part_of(piece, names, (min(xs) + max(xs)) / 2, max(ys))
        px, py, pz = PIVOT[part]
        for q in placed:
            verts = []
            for (bx, by, bz), uu, vv in q:
                verts += [round(bx - px, 4), round((24 - by) - py, 4), round(bz - pz, 4), round(uu, 6), round(vv, 6)]
            a = verts[0:3]; b = verts[5:8]; c = verts[10:13]
            ux, uy, uz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
            vx, vy, vz = c[0] - a[0], c[1] - a[1], c[2] - a[2]
            nx, ny, nz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx
            ln = math.sqrt(nx * nx + ny * ny + nz * nz)
            if ln < 1e-9:
                nx, ny, nz, ln = 0, -1, 0, 1
            parts.setdefault(part, []).append({'v': verts, 'n': [round(nx / ln, 4), round(ny / ln, 4), round(nz / ln, 4)]})
    for k in parts:
        parts[k] = [q for q in parts[k] if area(q['v']) > 1e-6]
    texture = None
    for t in d.get('textures', []):
        src = t.get('source', '')
        if src.startswith('data:image/png;base64,'):
            texture = base64.b64decode(src.split(',', 1)[1])
            break
    model = {'format': 'mio_icif:baked_armor/1', 'source': os.path.basename(path),
             'texture_size': [res_w, res_h], 'parts': parts}
    return model, texture


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    root = sys.argv[1]
    out_model = os.path.join(root, 'src/main/resources/assets/mio_icif/armor_models')
    out_tex = os.path.join(root, 'src/main/resources/assets/mio_icif/textures/models/armor')
    os.makedirs(out_model, exist_ok=True)
    os.makedirs(out_tex, exist_ok=True)
    for spec in sys.argv[2:]:
        name, rest = spec.split('=', 1)
        fields = rest.split(';')
        path, turn, png = fields[0], 'turn' in fields[1:], None
        for f in fields[1:]:
            if f.startswith('png='):
                png = f[4:]
        model, texture = convert(path, name, turn)
        with open(os.path.join(out_model, name + '.json'), 'w', encoding='utf-8') as f:
            json.dump(model, f, separators=(',', ':'))
        target = os.path.join(out_tex, name + '.png')
        if png:
            shutil.copyfile(png, target)
        elif texture:
            with open(target, 'wb') as f:
                f.write(texture)
        else:
            print('WARNING: no texture for', name)
        print(name, {k: len(v) for k, v in sorted(model['parts'].items())})


if __name__ == '__main__':
    main()
