#!/usr/bin/env python3
"""
0.1.7.29: hand transforms of the heavy guns, anchored on each gun's grip.

The mining laser's transforms (tuned together with LaserGunPose) are the reference: for every other
gun the grip element (lowest element behind z=6, the hand wraps its upper 70 %) is placed at the
same point the laser's grip ends up at, for third person (rotation 0) and first person (yaw 6 deg).
Scales follow the gun length.

Display transform order (ItemTransform.apply): translate, rotate (XYZ), scale about the block
centre, i.e. world = T + R * s * (p - 8) in model pixels.

Usage: gun_poses.py <assets/mio_icif/models/item/3d>
"""
import json, math, os, sys
import numpy as np

DIR = sys.argv[1] if len(sys.argv) > 1 else '.'
# (third-person scale, first-person scale)
SCALES = {
    'item_tool_electric_rifle': (0.55, 0.42),
    'item_tool_advanced_electric_rifle': (0.52, 0.40),
    'item_tool_tactical_laser_rifle': (0.50, 0.38),
    'item_tool_tachyon_disruptor': (0.52, 0.40),
    'item_tool_electric_plasma_gun': (0.66, 0.38),
    'item_tool_plasma_air_cannon': (0.66, 0.36),
    'item_tool_rocket_launcher': (0.62, 0.37),
}


def rot(r):
    x, y, z = (math.radians(a) for a in r)
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return rx @ ry @ rz


def grip(elements):
    g = min((e for e in elements if e['from'][2] >= 6), key=lambda e: e['from'][1])
    return np.array([8.0, g['from'][1] + 0.7 * (g['to'][1] - g['from'][1]), (g['from'][2] + g['to'][2]) / 2])


def load(name):
    return json.load(open(os.path.join(DIR, name + '.json')))


ref = load('item_tool_laser_miner')
g_ref = grip(ref['elements'])


def anchor(key):
    d = ref['display'][key]
    return np.array(d['translation']) + rot(d['rotation']) @ (d['scale'][0] * (g_ref - 8))


hand_tp, hand_fp = anchor('thirdperson_righthand'), anchor('firstperson_righthand')
for name, (s_tp, s_fp) in SCALES.items():
    model = load(name)
    g = grip(model['elements'])
    d = model['display']

    def place(hand, s, r):
        t = [round(float(v), 2) for v in hand - rot(r) @ (s * (g - 8))]
        return {'rotation': r, 'translation': t, 'scale': [s, s, s]}

    tp = place(hand_tp, s_tp, [0, 0, 0])
    fp = place(hand_fp, s_fp, [0, 6, 0])
    d['thirdperson_righthand'] = tp
    d['thirdperson_lefthand'] = dict(tp)
    d['firstperson_righthand'] = fp
    d['firstperson_lefthand'] = {'rotation': [0, -6, 0], 'translation': [-fp['translation'][0], *fp['translation'][1:]], 'scale': fp['scale']}
    json.dump(model, open(os.path.join(DIR, name + '.json'), 'w'), indent=2)
    print(name, tp['translation'], fp['translation'])
