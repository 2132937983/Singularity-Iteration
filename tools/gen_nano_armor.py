#!/usr/bin/env python3
"""
0.1.7.34: converts the nano suit Blockbench models (modded_entity, box UV) into the baked armor
geometry the game loads (assets/mio_icif/armor_models/nano_*.json) and copies the textures.

Geometry rules (match ModelPart.Cube so the result looks like a Blockbench Java export):
  * MC model space = (bb.x, 24 - bb.y, bb.z), y down, 1 unit = 1/16 block.
  * Each element is rotated in Blockbench space (Euler order ZYX around its origin) and then
    stored relative to the pivot of the humanoid part that carries it.
  * Box UV slots and the vertex -> UV corner mapping follow ModelPart.Cube / Polygon.
  * Part: helmet -> head; chest body group -> body, other chest elements -> arms by side;
    leggings crotch -> body, other leg elements -> legs by side; boots -> legs by side.
    Elements on the -x side go to the right limb (the right arm/leg of a ModelPart model is at -x).

Usage: gen_nano_armor.py <bbmodel dir> <repo root>
"""
import json, math, os, shutil, sys

SRC = sys.argv[1] if len(sys.argv) > 1 else '.'
ROOT = sys.argv[2] if len(sys.argv) > 2 else '.'
OUT_MODEL = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/armor_models')
OUT_TEX = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/models/armor')

PIVOT = {  # MC model space
    'head': (0, 0, 0), 'body': (0, 0, 0),
    'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0),
    'right_leg': (-1.9, 12, 0), 'left_leg': (1.9, 12, 0),
}

# Pieces authored facing +z (south) instead of the Blockbench entity front (-z): turned 180 deg
# about the vertical axis before they are split into parts (0.1.7.35: the boots rendered back to front).
TURN_AROUND = {'nano_boots'}

PIECES = [
    # file stem, output name
    ('纳米头盔', 'nano_helmet'),
    ('纳米胸甲', 'nano_chestplate'),
    ('纳米护腿', 'nano_leggings'),
    ('纳米靴子', 'nano_boots'),
]


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


def part_of(piece, group_name, centre_x):
    side = 'right' if centre_x < 0 else 'left'
    if piece == 'nano_helmet':
        return 'head'
    if piece == 'nano_chestplate':
        return 'body' if group_name == '胸甲主体' else side + '_arm'
    if piece == 'nano_leggings':
        return 'body' if group_name == '裤裆' else side + '_leg'
    return side + '_leg'


def element_quads(e, res_w, res_h):
    """Six quads of one element in Blockbench space: list of (4 x (bbx, bby, bbz, u, v), face)."""
    x1, y1, z1 = e['from']
    x2, y2, z2 = e['to']
    w, h, d = x2 - x1, y2 - y1, z2 - z1
    u, v = e.get('uv_offset', [0, 0])
    if e.get('mirror_uv'):
        x1, x2 = x2, x1
    # ModelPart.Cube vertex set in MC model space (minY = top). Convert MC (x, yMC, z) corners to
    # Blockbench: yMC min -> bb top (y2), yMC max -> bb bottom (y1).
    top, bot = y2, y1
    V = [
        (x1, top, z1), (x2, top, z1), (x2, bot, z1), (x1, bot, z1),
        (x1, top, z2), (x2, top, z2), (x2, bot, z2), (x1, bot, z2),
    ]
    # (vertex indices, u1, v1, u2, v2) as in Cube; MC "DOWN" (minY) is the visual top.
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


def convert(path, piece):
    d = json.load(open(path, encoding='utf-8'))
    res_w, res_h = d['resolution']['width'], d['resolution']['height']
    groups = {g['uuid']: g for g in d['groups']}
    elements = {e['uuid']: e for e in d['elements']}
    owner = {}

    def walk(nodes, name):
        for n in nodes:
            if isinstance(n, dict):
                walk(n.get('children', []), groups[n['uuid']]['name'])
            else:
                owner[n] = name

    walk(d['outliner'], None)
    parts = {}
    for uid, e in elements.items():
        if e.get('export') is False or e.get('visibility') is False:
            continue
        turn = piece in TURN_AROUND
        cx = (e['from'][0] + e['to'][0]) / 2
        part = part_of(piece, owner.get(uid), -cx if turn else cx)
        rot = e.get('rotation', [0, 0, 0])
        origin = e.get('origin', [0, 0, 0])
        m = rot_matrix(*rot)
        px, py, pz = PIVOT[part]
        for quad, face in element_quads(e, res_w, res_h):
            verts = []
            for (x, y, z, uu, vv) in quad:
                p = apply(m, [x - origin[0], y - origin[1], z - origin[2]])
                bx, by, bz = p[0] + origin[0], p[1] + origin[1], p[2] + origin[2]
                if turn:
                    bx, bz = -bx, -bz
                mx, my, mz = bx - px, (24 - by) - py, bz - pz
                verts += [round(mx, 4), round(my, 4), round(mz, 4), round(uu, 6), round(vv, 6)]
            # normal from the quad (MC space); renderer uses no culling, the normal is for light only
            a = verts[0:3]; b = verts[5:8]; c = verts[10:13]
            ux, uy, uz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
            vx, vy, vz = c[0] - a[0], c[1] - a[1], c[2] - a[2]
            nx, ny, nz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx
            ln = math.sqrt(nx * nx + ny * ny + nz * nz)
            if ln < 1e-9:
                # zero-area face of a flat element: keep it (it is a side of a plane), use any normal
                nx, ny, nz, ln = 0, -1, 0, 1
            parts.setdefault(part, []).append({'v': verts, 'n': [round(nx / ln, 4), round(ny / ln, 4), round(nz / ln, 4)]})
    # drop degenerate (zero-area) quads: they render nothing
    for k in parts:
        parts[k] = [q for q in parts[k] if area(q['v']) > 1e-6]
    return {'format': 'mio_icif:baked_armor/1', 'source': os.path.basename(path), 'texture_size': [res_w, res_h], 'parts': parts}


def area(v):
    p = [v[i * 5:i * 5 + 3] for i in range(4)]
    def cross(a, b):
        return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
    s = [0, 0, 0]
    for i in range(4):
        c = cross(p[i], p[(i + 1) % 4])
        s = [s[j] + c[j] for j in range(3)]
    return math.sqrt(sum(x * x for x in s)) / 2


def main():
    os.makedirs(OUT_MODEL, exist_ok=True)
    os.makedirs(OUT_TEX, exist_ok=True)
    for stem, name in PIECES:
        model = convert(os.path.join(SRC, stem + '.bbmodel'), name)
        with open(os.path.join(OUT_MODEL, name + '.json'), 'w', encoding='utf-8') as f:
            json.dump(model, f, separators=(',', ':'))
        shutil.copyfile(os.path.join(SRC, stem + '纹理.png'), os.path.join(OUT_TEX, name + '.png'))
        print(name, {k: len(v) for k, v in model['parts'].items()})


if __name__ == '__main__':
    main()
