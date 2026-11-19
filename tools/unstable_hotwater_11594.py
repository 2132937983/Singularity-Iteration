#!/usr/bin/env python3
"""0.1.11.94: the resources of unstable hot water (the deuterium-rich hot water the thermal centrifuge makes): its fluid
textures are hot water's, a little darker (DARKEN); its cell and bucket are hot water's with only the fluid pixels darkened
(the pixels where the hot water cell / bucket differs from the coolant one); the blockstate and the models follow hot water's.
Idempotent (reads only hot water's files).

Usage: unstable_hotwater_11594.py <repo root>
"""
import json
import os
import shutil
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
A = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
T = os.path.join(A, 'textures')
DARKEN = 0.78


def darker(c):
    return (round(c[0] * DARKEN), round(c[1] * DARKEN), round(c[2] * DARKEN), c[3])


def darken_all(src, dst):
    im = Image.open(src).convert('RGBA')
    px = im.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            px[x, y] = darker(px[x, y])
    im.save(dst)


def darken_fluid(src, other, dst):
    """Darkens only the pixels where src differs from other (the same container holding another fluid)."""
    im, ref = Image.open(src).convert('RGBA'), Image.open(other).convert('RGBA')
    px, rp = im.load(), ref.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            if px[x, y] != rp[x, y] and px[x, y][3] > 0:
                px[x, y] = darker(px[x, y])
    im.save(dst)


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    fluids = os.path.join(T, 'block/fluids')
    for kind in ('still', 'flow'):
        darken_all(os.path.join(fluids, 'hotwater_%s.png' % kind), os.path.join(fluids, 'unstable_hotwater_%s.png' % kind))
        meta = os.path.join(fluids, 'hotwater_%s.png.mcmeta' % kind)
        if os.path.exists(meta):
            shutil.copyfile(meta, os.path.join(fluids, 'unstable_hotwater_%s.png.mcmeta' % kind))
    darken_fluid(os.path.join(T, 'item/cell/item_cell_hotwater.png'), os.path.join(T, 'item/cell/item_cell_coolant.png'),
                 os.path.join(T, 'item/cell/item_cell_unstable_hotwater.png'))
    darken_fluid(os.path.join(T, 'item/bucket/hotwater_bucket.png'), os.path.join(T, 'item/bucket/coolant_bucket.png'),
                 os.path.join(T, 'item/bucket/unstable_hotwater_bucket.png'))
    write_json(os.path.join(A, 'blockstates/unstable_hotwater.json'),
               {'variants': {'level=%d' % i: {'model': 'mio_icif:block/unstable_hotwater'} for i in range(16)}})
    write_json(os.path.join(A, 'models/block/unstable_hotwater.json'),
               {'parent': 'block/cube_all', 'textures': {'all': 'mio_icif:block/fluids/unstable_hotwater_still'}})
    write_json(os.path.join(A, 'models/item/unstable_hotwater_bucket.json'),
               {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mio_icif:item/bucket/unstable_hotwater_bucket'}})
    write_json(os.path.join(A, 'models/item/cell/item_cell_unstable_hotwater.json'),
               {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mio_icif:item/cell/item_cell_unstable_hotwater'}})
    print('unstable hot water resources written')


if __name__ == '__main__':
    main()
