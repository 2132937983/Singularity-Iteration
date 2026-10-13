#!/usr/bin/env python3
"""
0.1.9.0: frame core attachments (the 3D exterior of a core on its suit piece).

The retired solar helmets and quantum chestplates were Blockbench models with named elements
(tools/models/armor/*.bbmodel). Their characteristic parts (solar panels and wings, the sun
fan, the graviton ring and gyro, the propulsion boosters) become attachments: segments of baked
quads that the CoreAttachmentLayer draws over the worn nano or quantum piece, with the textures
of the retired items (textures/models/armor/<legacy>.png, still shipped).

Output: assets/mio_icif/armor_models/attachments/<core>_<piece>.json

  {"format": "mio_icif:core_attachment/1", "source": "<legacy>.bbmodel",
   "texture": "mio_icif:textures/models/armor/<legacy>.png", "texture_size": [128, 128],
   "segments": [{"name": ..., "part": "head|body|left_arm|right_arm", "emissive": bool,
                 "glow": "solar|thrust|pulse|idle" (optional, additive glow pass),
                 "anim": {"type": "deploy|spin|sweep", "axis": [x, y, z], "pivot": [x, y, z],
                          "open": deg, "closed": deg | "speed": deg/s | "amplitude": deg, "period": s,
                          "drive": "solar|thrust|active"} (optional),
                 "quads": [{"v": [4 x (x y z u v)], "n": [x y z]}]}]}

Coordinates are part space, pixels, y down (as the baked armor). Angles: right-handed rotation
about the axis in part space (Quaternionf.rotateAxis).

Fitting: each group of segments moves together. A group snaps on its "contact" elements: their
outside face is placed on the surface of the base piece (the baked model of the worn nano or
quantum piece) inside the footprint of the contact elements, plus a gap (negative = sunk in).
  top    contact bottom on the highest point of the base under it
  back   contact front on the rearmost point of the base behind it
  front  contact back on the foremost point of the base in front of it
A fixed offset can be added after the snap.

Usage: gen_core_attachments.py <repo root>
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_suit_armor import element_quads, rotate_about, area  # noqa: E402

PIVOT = {'head': (0, 0, 0), 'body': (0, 0, 0), 'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0)}

# ---------------------------------------------------------------- attachment definitions
# element selector: (name, side) with side '+' (centre x > 0), '-' (x < 0) or None (all)


def solar_1(base):
    return {
        'source': 'advanced_solar_helmet',
        'groups': [
            {'snap': ('top', ['top_panel'], 0.15), 'segments': [
                {'name': 'mount', 'part': 'head', 'elements': [('mount', None)], 'extend_down': 2.5},
                {'name': 'panel', 'part': 'head', 'elements': [('top_panel', None), ('panel_frame_front', None),
                                                              ('panel_frame_back', None)], 'glow': 'solar',
                 'anim': {'type': 'deploy', 'axis': [1, 0, 0], 'pivot_bb': [0, 33.4, 0], 'open': 0, 'closed': -14,
                          'drive': 'solar'}},
                {'name': 'antenna', 'part': 'head', 'elements': [('antenna', None)], 'extend_down': 2.0},
            ]},
        ],
    }


def solar_2(base):
    return {
        'source': 'hybrid_solar_helmet',
        'groups': [
            {'snap': ('top', ['top_panel'], 0.15), 'segments': [
                {'name': 'panel', 'part': 'head', 'elements': [('top_panel', None)], 'glow': 'solar'},
                {'name': 'crest', 'part': 'head', 'elements': [('crest_light', None)], 'emissive': True, 'glow': 'solar'},
                {'name': 'hinges', 'part': 'head', 'elements': [('hinge', None)], 'extend_down': 1.5},
                {'name': 'wing_left', 'part': 'head', 'elements': [('wing', '+')], 'glow': 'solar',
                 'anim': {'type': 'deploy', 'axis': [0, 0, 1], 'pivot_bb': [4.9, 31.5, 0], 'open': 0, 'closed': -62,
                          'drive': 'solar'}},
                {'name': 'wing_right', 'part': 'head', 'elements': [('wing', '-')], 'glow': 'solar',
                 'anim': {'type': 'deploy', 'axis': [0, 0, 1], 'pivot_bb': [-4.9, 31.5, 0], 'open': 0, 'closed': 62,
                          'drive': 'solar'}},
            ]},
        ],
    }


def solar_3(base):
    return {
        'source': 'ultimate_solar_helmet',
        'groups': [
            {'snap': ('top', ['top_panel'], 0.15), 'segments': [
                {'name': 'panel', 'part': 'head', 'elements': [('top_panel', None), ('crown_front', None)], 'glow': 'solar'},
                {'name': 'hinges', 'part': 'head', 'elements': [('wing_hinge', None)], 'extend_down': 1.5},
                {'name': 'wing_left', 'part': 'head', 'elements': [('wing', '+')], 'glow': 'solar',
                 'anim': {'type': 'deploy', 'axis': [0, 0, 1], 'pivot_bb': [5.0, 32.1, 0], 'open': 0, 'closed': -40,
                          'drive': 'solar'}},
                {'name': 'wing_right', 'part': 'head', 'elements': [('wing', '-')], 'glow': 'solar',
                 'anim': {'type': 'deploy', 'axis': [0, 0, 1], 'pivot_bb': [-5.0, 32.1, 0], 'open': 0, 'closed': 40,
                          'drive': 'solar'}},
            ]},
            {'snap': ('front', ['sun_core'], 0.05), 'segments': [
                {'name': 'sun_core', 'part': 'head', 'elements': [('sun_core', None)], 'emissive': True, 'glow': 'solar'},
                {'name': 'brow_trim', 'part': 'head', 'elements': [('brow_trim', None)]},
            ]},
            {'snap': ('back', ['fan_hub'], -0.3), 'offset': (0, -1.0, 0), 'segments': [
                {'name': 'fan_hub', 'part': 'head', 'elements': [('fan_hub', None)], 'glow': 'solar'},
                {'name': 'fan', 'part': 'head', 'elements': [('fan_centre', None), ('fan_side', None)], 'glow': 'solar',
                 'anim': {'type': 'sweep', 'axis': [0, -0.38268, 0.92388], 'pivot_bb': [0, 29.3, 5.2], 'amplitude': 9,
                          'period': 7, 'drive': 'solar'}},
            ]},
        ],
    }


def graviton(base):
    return {
        'source': 'heavy_quantum_chestplate',
        'groups': [
            {'snap': ('front', ['ring_top', 'ring_bottom', 'ring_left', 'ring_right'], 0.1), 'offset': (0, -0.8, 0), 'segments': [
                {'name': 'ring', 'part': 'body', 'elements': [('ring_top', None), ('ring_bottom', None), ('ring_left', None),
                                                             ('ring_right', None)], 'glow': 'pulse',
                 'anim': {'type': 'spin', 'axis': [0, 0, 1], 'pivot_bb': [0, 20, -4.4], 'speed': 24, 'drive': 'active'}},
                {'name': 'gravity_core', 'part': 'body', 'elements': [('gravity_core', None)], 'emissive': True, 'glow': 'pulse',
                 'extend_back': 1.2},
            ]},
            {'snap': ('back', ['gyro_core'], 0.1), 'segments': [
                {'name': 'mount', 'part': 'body', 'elements': [('pack', None)], 'shrink': 1.6, 'extend_front': 2.0},
                {'name': 'gyro', 'part': 'body', 'elements': [('gyro_top', None), ('gyro_bottom', None), ('gyro_side', None),
                                                             ('gyro_spoke', None)], 'glow': 'pulse',
                 'anim': {'type': 'spin', 'axis': [0, 0, 1], 'pivot_bb': [0, 18.6, 5.5], 'speed': 90, 'drive': 'active'}},
                {'name': 'gyro_core', 'part': 'body', 'elements': [('gyro_core', None)], 'emissive': True, 'glow': 'pulse'},
            ]},
            {'snap': ('top', ['graviton_pad'], 0.05), 'side': '+', 'segments': [
                {'name': 'pad_left', 'part': 'left_arm', 'elements': [('graviton_pad', '+')], 'emissive': True, 'glow': 'pulse'},
            ]},
            {'snap': ('top', ['graviton_pad'], 0.05), 'side': '-', 'segments': [
                {'name': 'pad_right', 'part': 'right_arm', 'elements': [('graviton_pad', '-')], 'emissive': True, 'glow': 'pulse'},
            ]},
        ],
    }


def propulsion(base):
    return {
        'source': 'advanced_quantum_chestplate',
        'groups': [
            {'snap': ('back', ['booster'], 0.0), 'offset': (0, -0.5, 0), 'segments': [
                {'name': 'boosters', 'part': 'body', 'elements': [('booster', None)], 'extend_front': 1.0},
                {'name': 'intakes', 'part': 'body', 'elements': [('intake', None)]},
                {'name': 'nozzles', 'part': 'body', 'elements': [('nozzle', None)], 'emissive': True, 'glow': 'thrust'},
                {'name': 'spine_fin', 'part': 'body', 'elements': [('spine_fin', None)], 'extend_front': 1.5},
                {'name': 'stabilizer_left', 'part': 'body', 'elements': [('stabilizer', '+')],
                 'anim': {'type': 'deploy', 'axis': [0, 0, 1], 'pivot_bb': [4.2, 16.5, 5.7], 'open': -18, 'closed': 0,
                          'drive': 'thrust'}},
                {'name': 'stabilizer_right', 'part': 'body', 'elements': [('stabilizer', '-')],
                 'anim': {'type': 'deploy', 'axis': [0, 0, 1], 'pivot_bb': [-4.2, 16.5, 5.7], 'open': 18, 'closed': 0,
                          'drive': 'thrust'}},
            ]},
            {'snap': ('front', ['core_frame_top', 'core_frame_bottom', 'core_frame_side'], 0.05), 'offset': (0, -1.0, 0), 'segments': [
                {'name': 'core_frame', 'part': 'body', 'elements': [('core_frame_top', None), ('core_frame_bottom', None),
                                                                   ('core_frame_side', None)], 'extend_back': 0.8},
                {'name': 'core', 'part': 'body', 'elements': [('core', None)], 'emissive': True, 'glow': 'thrust',
                 'extend_back': 0.8},
            ]},
        ],
    }


ATTACHMENTS = {
    'solar_array_1_nano_helmet': (solar_1, 'nano_helmet'),
    'solar_array_1_quantum_helmet': (solar_1, 'quantum_helmet'),
    'solar_array_2_nano_helmet': (solar_2, 'nano_helmet'),
    'solar_array_2_quantum_helmet': (solar_2, 'quantum_helmet'),
    'solar_array_3_quantum_helmet': (solar_3, 'quantum_helmet'),
    'graviton_quantum_chestplate': (graviton, 'quantum_chestplate'),
    'propulsion_quantum_chestplate': (propulsion, 'quantum_chestplate'),
}


# ---------------------------------------------------------------- geometry

def load_bb(root, name):
    with open(os.path.join(root, 'tools/models/armor', name + '.bbmodel'), encoding='utf-8') as f:
        return json.load(f)


def pick(bb, selector):
    name, side = selector
    out = []
    for e in bb['elements']:
        if e['name'] != name:
            continue
        cx = (e['from'][0] + e['to'][0]) / 2
        if side == '+' and cx <= 0 or side == '-' and cx >= 0:
            continue
        out.append(e)
    if not out:
        raise SystemExit(f'element {selector} not found')
    return out


def adjust(e, seg):
    """Optional element changes: extend down (BB -y) / front (-z) / back (+z), shrink in x and y.
    Returns the new box (from, to); the texture keeps the UV layout of the original box (the faces
    stretch), so a longer mount does not read the neighbouring texture."""
    f, t = list(e['from']), list(e['to'])
    if seg.get('extend_down'):
        f[1] -= seg['extend_down']
    if seg.get('extend_front'):
        f[2] -= seg['extend_front']
    if seg.get('extend_back'):
        t[2] += seg['extend_back']
    if seg.get('shrink'):
        s = seg['shrink']
        f[0] += s / 2; t[0] -= s / 2; f[1] += s / 2; t[1] -= s / 2
    return f, t


def element_model_quads(e, res, box=None):
    """Quads of one element in body model space (before the part pivot): list of [(x, y, z, u, v)] * 4.
    box: optional new (from, to) of the element; the UVs stay those of the original box."""
    def remap(value, axis):
        if box is None:
            return value
        if abs(value - e['from'][axis]) < 1e-6:
            return box[0][axis]
        if abs(value - e['to'][axis]) < 1e-6:
            return box[1][axis]
        return value
    out = []
    for quad, _ in element_quads(e, res[0], res[1]):
        pts = []
        for (x, y, z, u, v) in quad:
            p = rotate_about((remap(x, 0), remap(y, 1), remap(z, 2)), e.get('rotation', [0, 0, 0]), e.get('origin', [0, 0, 0]))
            pts.append((p[0], 24 - p[1], p[2], u, v))
        out.append(pts)
    return out


def base_points(root, base, part):
    path = os.path.join(root, 'src/main/resources/assets/mio_icif/armor_models', base + '.json')
    with open(path, encoding='utf-8') as f:
        quads = json.load(f)['parts'].get(part, [])
    px, py, pz = PIVOT[part]
    pts = []
    for q in quads:
        v = q['v']
        P = [(v[i * 5] + px, v[i * 5 + 1] + py, v[i * 5 + 2] + pz) for i in range(4)]
        for a in range(7):
            for b in range(7):
                s, t = a / 6, b / 6
                p0 = [P[0][k] * (1 - s) + P[1][k] * s for k in range(3)]
                p1 = [P[3][k] * (1 - s) + P[2][k] * s for k in range(3)]
                pts.append(tuple(p0[k] * (1 - t) + p1[k] * t for k in range(3)))
    return pts


def snap_offset(kind, contact_pts, base_pts, gap):
    xs = [p[0] for p in contact_pts]; ys = [p[1] for p in contact_pts]; zs = [p[2] for p in contact_pts]
    x0, x1, y0, y1, z0, z1 = min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)
    m = 0.25
    if kind == 'top':
        under = [p[1] for p in base_pts if x0 - m <= p[0] <= x1 + m and z0 - m <= p[2] <= z1 + m]
        if not under:
            return (0, 0, 0)
        return (0, min(under) - gap - y1, 0)
    if kind == 'back':
        behind = [p[2] for p in base_pts if x0 - m <= p[0] <= x1 + m and y0 - m <= p[1] <= y1 + m]
        if not behind:
            return (0, 0, 0)
        return (0, 0, max(behind) + gap - z0)
    if kind == 'front':
        front = [p[2] for p in base_pts if x0 - m <= p[0] <= x1 + m and y0 - m <= p[1] <= y1 + m]
        if not front:
            return (0, 0, 0)
        return (0, 0, min(front) - gap - z1)
    raise SystemExit('unknown snap ' + kind)


def normal(v):
    a = v[0:3]; b = v[5:8]; c = v[10:13]
    ux, uy, uz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    vx, vy, vz = c[0] - a[0], c[1] - a[1], c[2] - a[2]
    nx, ny, nz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx
    ln = math.sqrt(nx * nx + ny * ny + nz * nz)
    if ln < 1e-9:
        return [0, -1, 0]
    return [round(nx / ln, 4), round(ny / ln, 4), round(nz / ln, 4)]


def build(root, out_id, fn, base):
    spec = fn(base)
    bb = load_bb(root, spec['source'])
    res = (bb['resolution']['width'], bb['resolution']['height'])
    segments = []
    for group in spec['groups']:
        part0 = group['segments'][0]['part']
        base_pts = base_points(root, base, 'body' if part0 == 'body' else part0)
        kind, contact_names, gap = group['snap']
        side = group.get('side')
        contact = []
        for name in contact_names:
            for e in pick(bb, (name, side)):
                for q in element_model_quads(e, res):
                    contact += [(x, y, z) for (x, y, z, _, _) in q]
        off = snap_offset(kind, contact, base_pts, gap)
        extra = group.get('offset', (0, 0, 0))
        off = (off[0] + extra[0], off[1] + extra[1], off[2] + extra[2])
        for seg in group['segments']:
            px, py, pz = PIVOT[seg['part']]
            quads = []
            for sel in seg['elements']:
                for e in pick(bb, sel):
                    for q in element_model_quads(e, res, adjust(e, seg)):
                        v = []
                        for (x, y, z, u, vv) in q:
                            v += [round(x + off[0] - px, 4), round(y + off[1] - py, 4), round(z + off[2] - pz, 4),
                                  round(u, 6), round(vv, 6)]
                        if area(v) > 1e-6:
                            quads.append({'v': v, 'n': normal(v)})
            out = {'name': seg['name'], 'part': seg['part'], 'emissive': bool(seg.get('emissive'))}
            if seg.get('glow'):
                out['glow'] = seg['glow']
            if seg.get('anim'):
                a = dict(seg['anim'])
                bx, by, bz = a.pop('pivot_bb')
                a['pivot'] = [round(bx + off[0] - px, 4), round(24 - by + off[1] - py, 4), round(bz + off[2] - pz, 4)]
                out['anim'] = a
            out['quads'] = quads
            segments.append(out)
    return {'format': 'mio_icif:core_attachment/1', 'source': spec['source'] + '.bbmodel', 'base': base,
            'texture': 'mio_icif:textures/models/armor/' + spec['source'] + '.png',
            'texture_size': list(res), 'segments': segments}


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(2)
    root = sys.argv[1]
    out_dir = os.path.join(root, 'src/main/resources/assets/mio_icif/armor_models/attachments')
    os.makedirs(out_dir, exist_ok=True)
    only = sys.argv[2:]
    for out_id, (fn, base) in ATTACHMENTS.items():
        if only and out_id not in only:
            continue
        model = build(root, out_id, fn, base)
        with open(os.path.join(out_dir, out_id + '.json'), 'w', encoding='utf-8') as f:
            json.dump(model, f, separators=(',', ':'))
        print(out_id, {s['name']: len(s['quads']) for s in model['segments']})


if __name__ == '__main__':
    main()
