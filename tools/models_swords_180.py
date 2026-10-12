#!/usr/bin/env python3
"""
0.1.8.0: 3D melee weapon models (Bronze Sword, Nano Saber off/on, Quantum Sword).

The models are built vertically (pommel at the bottom, tip at the top, centred on x = z = 8)
and every element is turned -45 degrees around z about the centre. Thus each model lies on the
diagonal of a vanilla sword sprite and the parent "minecraft:item/handheld" display transforms
(hand, GUI, ground, frame) fit without changes.

Texture: 32x32 per weapon. Regions (uv units = pixels / 2):
  blade [0,0,4,16]  guard [4,0,10,3]  grip [4,3,6,10]  pommel [6,3,8,5]
  glow  [8,3,10,16] edge  [10,3,12,16]
Glowing faces use NeoForge face data (block and sky light 15).
Usage: models_swords_180.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import write_png  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
A = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
UV = {'blade': [0, 0, 4, 16], 'guard': [4, 0, 10, 3], 'grip': [4, 3, 6, 10], 'pommel': [6, 3, 8, 5],
      'glow': [8, 3, 10, 16], 'edge': [10, 3, 12, 16]}
GLOW = {'block_light': 15, 'sky_light': 15}


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def texture(pal):
    px = [[(0, 0, 0, 0)] * 32 for _ in range(32)]

    def fill(region, fn):
        u0, v0, u1, v1 = [int(t * 2) for t in UV[region]]
        for y in range(v0, v1):
            for x in range(u0, u1):
                px[y][x] = clamp(fn(x - u0, y - v0, u1 - u0, v1 - v0))
    b0, b1 = pal['blade']
    fill('blade', lambda x, y, w, h: tuple(b0[i] + (b1[i] - b0[i]) * (y / h) + (18 if x in (0, w - 1) else 0) for i in range(3)) + (255,))
    fill('guard', lambda x, y, w, h: tuple(c * (1.15 if y == 0 else 0.85 if y == h - 1 else 1.0) for c in pal['guard'][:3]) + (255,))
    g = pal['grip']
    fill('grip', lambda x, y, w, h: tuple(c * (0.7 if (x + y) % 3 == 0 else 1.0) for c in g[:3]) + (255,))
    fill('pommel', lambda x, y, w, h: tuple(c * (1.2 if x + y < 2 else 1.0) for c in pal['pommel'][:3]) + (255,))
    gl, core = pal['glow'], pal.get('core', pal['glow'])
    fill('glow', lambda x, y, w, h: (core if x in (1, 2) else gl))
    fill('edge', lambda x, y, w, h: tuple(c for c in pal['edge'][:3]) + (255,))
    return px


def element(name, f, t, region, glow=False, faces='all'):
    face = OrderedDict([('uv', UV[region]), ('texture', '#0')])
    if glow:
        face['neoforge_data'] = GLOW
    sides = ['north', 'south', 'east', 'west', 'up', 'down']
    return OrderedDict([('name', name), ('from', f), ('to', t),
                        ('rotation', OrderedDict([('angle', -45), ('axis', 'z'), ('origin', [8, 8, 8])])),
                        ('faces', OrderedDict((s, dict(face)) for s in sides))])


def model(texture_id, elements, render_type='minecraft:cutout', extra=None):
    m = OrderedDict([('parent', 'minecraft:item/handheld'), ('render_type', render_type),
                     ('textures', OrderedDict([('0', texture_id), ('particle', texture_id)])), ('elements', elements)])
    if extra:
        m.update(extra)
    return m


def save(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def bronze():
    return [
        element('pommel', [7, -3, 7], [9, -1, 9], 'pommel'),
        element('grip', [7.25, -1, 7.25], [8.75, 4, 8.75], 'grip'),
        element('guard', [4.5, 4, 7], [11.5, 5.5, 9], 'guard'),
        element('blade', [6.5, 5.5, 7.5], [9.5, 16, 8.5], 'blade'),
        element('fuller', [7.75, 6, 7.4], [8.25, 15, 8.6], 'edge'),
        element('tip', [7, 16, 7.5], [9, 18, 8.5], 'blade'),
        element('point', [7.5, 18, 7.6], [8.5, 19, 8.4], 'blade'),
    ]


def nanosaber_hilt():
    return [
        element('cap', [7, -3, 7], [9, -2, 9], 'pommel'),
        element('grip', [7.25, -2, 7.25], [8.75, 4, 8.75], 'grip'),
        element('collar', [6.75, 4, 6.75], [9.25, 5.5, 9.25], 'guard'),
        element('emitter', [7.1, 5.5, 7.1], [8.9, 6.5, 8.9], 'edge'),
        element('light', [6.7, 4.6, 6.7], [9.3, 5.0, 9.3], 'glow', glow=True),
    ]


def nanosaber_blade():
    return [
        element('core', [7.65, 6.5, 7.65], [8.35, 19.2, 8.35], 'glow', glow=True),
        element('blade', [7.25, 6.5, 7.25], [8.75, 19.5, 8.75], 'blade', glow=True),
    ]


def quantum():
    return [
        element('pommel', [7, -3, 7], [9, -1.5, 9], 'pommel'),
        element('grip', [7.25, -1.5, 7.25], [8.75, 4, 8.75], 'grip'),
        element('guard', [5, 4, 6.5], [11, 6, 9.5], 'guard'),
        element('wing_l', [3.5, 4.5, 7], [5, 7, 9], 'guard'),
        element('wing_r', [11, 4.5, 7], [12.5, 7, 9], 'guard'),
        element('blade', [6.25, 6, 7.4], [9.75, 17, 8.6], 'blade'),
        element('core', [7.6, 6, 7.3], [8.4, 17.5, 8.7], 'glow', glow=True),
        element('tip', [6.75, 17, 7.4], [9.25, 18.5, 8.6], 'blade'),
        element('point', [7.4, 18.5, 7.5], [8.6, 20, 8.5], 'edge'),
    ]


def main():
    tex = os.path.join(A, 'textures/item/weapon')
    palettes = {
        'bronze_sword': {'blade': ((214, 150, 84), (160, 98, 48)), 'guard': (150, 96, 46, 255), 'grip': (98, 62, 36, 255),
                         'pommel': (176, 116, 58, 255), 'glow': (230, 170, 100, 255), 'edge': (120, 74, 38, 255)},
        'nanosaber': {'blade': ((120, 255, 160), (60, 230, 120)), 'guard': (88, 94, 102, 255), 'grip': (44, 48, 54, 255),
                      'pommel': (120, 126, 134, 255), 'glow': (90, 255, 140, 255), 'core': (230, 255, 236, 255), 'edge': (150, 156, 164, 255)},
        'quantum_sword': {'blade': ((226, 232, 238), (170, 180, 192)), 'guard': (52, 56, 64, 255), 'grip': (36, 40, 46, 255),
                          'pommel': (70, 76, 86, 255), 'glow': (80, 220, 255, 255), 'core': (210, 250, 255, 255), 'edge': (96, 104, 116, 255)},
    }
    for name, pal in palettes.items():
        px = texture(pal)
        if name == 'nanosaber':
            # translucent energy blade: the blade region of the saber texture is a glow shell
            for y in range(0, 32):
                for x in range(0, 8):
                    r, g, b, a = px[y][x]
                    px[y][x] = (r, g, b, 150)
        write_png(os.path.join(tex, name + '.png'), px)
    save(os.path.join(A, 'models/item/item_tool_bronze_sword.json'), model('mio_icif:item/weapon/bronze_sword', bronze()))
    save(os.path.join(A, 'models/item/item_tool_nanosaber.json'), model('mio_icif:item/weapon/nanosaber', nanosaber_hilt(), extra={
        'overrides': [{'predicate': {'mio_icif:nanosaber_active': 1}, 'model': 'mio_icif:item/item_tool_nanosaber_active'}]}))
    save(os.path.join(A, 'models/item/item_tool_nanosaber_active.json'),
         model('mio_icif:item/weapon/nanosaber', nanosaber_hilt() + nanosaber_blade(), render_type='minecraft:translucent'))
    save(os.path.join(A, 'models/item/item_tool_quantum_sword.json'), model('mio_icif:item/weapon/quantum_sword', quantum()))
    print('wrote 3 weapon textures and 4 weapon models')


if __name__ == '__main__':
    main()
