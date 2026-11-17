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

0.1.11.27: a GeckoLib armour model (Blockbench "geckolib_model": the armorHead / armorBody / armorRightArm / ... bones
of the GeckoLib armour template, per-face UV, inflate) gives all four pieces of a suit at once: suit:<prefix>=<bbmodel>.
  * GeckoLib Blockbench space has the front at -z and the wearer's right at +x (modded_entity: right at -x), so a point
    goes to MC model space as (-bb.x, 24 - bb.y, bb.z): a turn of 180 deg about z, not a mirror.
  * Per-face UV follows Blockbench's face convention (the Java block model one): seen from outside the face, u grows to
    the right and v down; up is seen from above with north at the top, down from below with south at the top.
  * Bones: armorHead -> helmet head; armorBody -> chestplate body; armorRightArm / armorLeftArm -> chestplate arms (or
    hands, an element wholly at or below BB y 16); armorRightLeg / armorLeftLeg -> leggings legs; armorRightBoot /
    armorLeftBoot -> boots legs. Elements with export off (the template's biped cubes) are left out.
  * The model's one texture is written as each piece's texture; texture_size is the model resolution.

Usage: gen_suit_armor.py <repo root> <name>=<bbmodel>[;turn][;png=<file>] ...
       gen_suit_armor.py <repo root> suit:<prefix>=<bbmodel>[;png=<file>] ...
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
    if piece == 'chestplate':
        # 0.1.8.3: keep gauntlet faces off the skin and sleeve planes (first-person flicker)
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        from armor_clearance import clear
        clear(parts)
    texture = None
    for t in d.get('textures', []):
        src = t.get('source', '')
        if src.startswith('data:image/png;base64,'):
            texture = base64.b64decode(src.split(',', 1)[1])
            break
    model = {'format': 'mio_icif:baked_armor/1', 'source': os.path.basename(path),
             'texture_size': [res_w, res_h], 'parts': parts}
    return model, texture


# ---- 0.1.11.27: GeckoLib armour models ----------------------------------------------------------------------------

GECKO_BONES = {
    'armorHead': ('helmet', 'head'), 'armorBody': ('chestplate', 'body'),
    'armorRightArm': ('chestplate', 'right_arm'), 'armorLeftArm': ('chestplate', 'left_arm'),
    'armorRightLeg': ('leggings', 'right_leg'), 'armorLeftLeg': ('leggings', 'left_leg'),
    'armorRightBoot': ('boots', 'right_leg'), 'armorLeftBoot': ('boots', 'left_leg'),
}


def face_corners(face, f, t):
    """Corners of one face in Blockbench space, in the order (u1,v1), (u2,v1), (u2,v2), (u1,v2), and its outward normal."""
    (x1, y1, z1), (x2, y2, z2) = f, t
    if face == 'north':
        return [(x2, y2, z1), (x1, y2, z1), (x1, y1, z1), (x2, y1, z1)], (0, 0, -1)
    if face == 'south':
        return [(x1, y2, z2), (x2, y2, z2), (x2, y1, z2), (x1, y1, z2)], (0, 0, 1)
    if face == 'east':
        return [(x2, y2, z2), (x2, y2, z1), (x2, y1, z1), (x2, y1, z2)], (1, 0, 0)
    if face == 'west':
        return [(x1, y2, z1), (x1, y2, z2), (x1, y1, z2), (x1, y1, z1)], (-1, 0, 0)
    if face == 'up':
        return [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)], (0, 1, 0)
    return [(x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1)], (0, -1, 0)


def gecko_quads(e, res_w, res_h):
    """Quads of one per-face UV element in Blockbench space: list of (4 x ((x, y, z), u, v), outward normal)."""
    inf = e.get('inflate', 0) or 0
    f = [c - inf for c in e['from']]
    t = [c + inf for c in e['to']]
    out = []
    for face, data in e.get('faces', {}).items():
        if data.get('texture') is None or 'uv' not in data:
            continue
        u1, v1, u2, v2 = data['uv']
        uvs = [(u1, v1), (u2, v1), (u2, v2), (u1, v2)]
        r = int(data.get('rotation', 0) or 0) // 90 % 4
        uvs = uvs[-r:] + uvs[:-r] if r else uvs   # a rotated face turns its UV corners clockwise
        corners, normal = face_corners(face, f, t)
        out.append(([(corners[k], uvs[k][0] / res_w, uvs[k][1] / res_h) for k in range(4)], normal))
    return out


def convert_gecko(path):
    """All pieces of a GeckoLib armour model: {piece: model}, texture bytes."""
    with open(path, encoding='utf-8') as fh:
        d = json.load(fh)
    res_w, res_h = d['resolution']['width'], d['resolution']['height']
    groups = {g['uuid']: g for g in d.get('groups', [])}
    elements = {e['uuid']: e for e in d['elements']}
    owner = {}

    def walk(nodes, chain):
        for n in nodes:
            if isinstance(n, dict):
                walk(n.get('children', []), chain + [groups[n['uuid']]])
            else:
                owner[n] = chain

    walk(d['outliner'], [])
    pieces = {}
    for e in d['elements']:
        if e.get('export') is False or e.get('visibility') is False or e.get('type', 'cube') != 'cube':
            continue
        chain = owner.get(e['uuid'], [])
        bone = next((g['name'] for g in reversed(chain) if g['name'] in GECKO_BONES), None)
        if bone is None:
            print('WARNING: element outside the armour bones:', e.get('name'))
            continue
        piece, part = GECKO_BONES[bone]

        def place(point):
            q = rotate_about(point, e.get('rotation', [0, 0, 0]), e.get('origin', [0, 0, 0]))
            for g in reversed(chain):
                q = rotate_about(q, g.get('rotation', [0, 0, 0]), g.get('origin', [0, 0, 0]))
            return [-q[0], 24 - q[1], q[2]]   # MC model space (y down), a turn about z

        quads = gecko_quads(e, res_w, res_h)
        placed = [([(place(c), uu, vv) for c, uu, vv in quad], normal) for quad, normal in quads]
        if part.endswith('_arm'):
            max_bb_y = max(24 - p[1] for quad, _ in placed for p, _, _ in quad) if placed else 99
            if max_bb_y <= HAND_TOP + 1e-6:
                part = part.replace('_arm', '_hand')
        px, py, pz = PIVOT[part]
        rot = e.get('rotation', [0, 0, 0])
        for quad, normal in placed:
            # the outward normal, turned like the element, then into MC space
            n = apply(rot_matrix(*rot), list(normal)) if any(rot) else list(normal)
            for g in reversed(chain):
                if any(g.get('rotation', [0, 0, 0])):
                    n = apply(rot_matrix(*g['rotation']), n)
            n = [-n[0], -n[1], n[2]]
            verts = []
            for (bx, by, bz), uu, vv in quad:
                verts += [round(bx - px, 4), round(by - py, 4), round(bz - pz, 4), round(uu, 6), round(vv, 6)]
            a, b, c = verts[0:3], verts[5:8], verts[10:13]
            ux, uy, uz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
            vx, vy, vz = c[0] - a[0], c[1] - a[1], c[2] - a[2]
            cx, cy, cz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx
            # ModelPart winding: the cross product of the first two edges points along the outward normal
            if cx * n[0] + cy * n[1] + cz * n[2] < 0:
                verts = verts[15:20] + verts[10:15] + verts[5:10] + verts[0:5]
            ln = math.sqrt(sum(k * k for k in n)) or 1
            pieces.setdefault(piece, {}).setdefault(part, []).append(
                {'v': verts, 'n': [round(n[0] / ln, 4), round(n[1] / ln, 4), round(n[2] / ln, 4)]})
    models = {}
    for piece, parts in pieces.items():
        for k in parts:
            parts[k] = [q for q in parts[k] if area(q['v']) > 1e-6]
        if piece == 'chestplate':
            sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
            from armor_clearance import clear
            clear(parts)
        # no separate vents or shoulder plates: SuitGimmicks leaves the piece still
        models[piece] = {'format': 'mio_icif:baked_armor/1', 'source': os.path.basename(path),
                         'texture_size': [res_w, res_h], 'gimmicks': False, 'parts': parts}
    texture = None
    for t in d.get('textures', []):
        src = t.get('source', '')
        if src.startswith('data:image/png;base64,'):
            texture = base64.b64decode(src.split(',', 1)[1])
            break
    return models, texture


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
        if name.startswith('suit:'):
            prefix = name[5:]
            fields = rest.split(';')
            png = next((f[4:] for f in fields[1:] if f.startswith('png=')), None)
            models, texture = convert_gecko(fields[0])
            for piece, model in models.items():
                out = prefix + '_' + piece
                with open(os.path.join(out_model, out + '.json'), 'w', encoding='utf-8') as f:
                    json.dump(model, f, separators=(',', ':'))
                target = os.path.join(out_tex, out + '.png')
                if png:
                    shutil.copyfile(png, target)
                elif texture:
                    with open(target, 'wb') as f:
                        f.write(texture)
                print(out, {k: len(v) for k, v in sorted(model['parts'].items())})
            continue
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
