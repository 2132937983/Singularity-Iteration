#!/usr/bin/env python3
"""
0.1.10.2: icon of the holomap Mk.II (16x16, pure Python): the Tactical 3D Holomap icon with the
quantum Mk.II overlay of tex_1100.py (violet rim, dark violet frame, gold LEDs).

Usage: tex_1102.py <repo root>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import read_png, write_png  # noqa: E402
import tex_1100  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
DIR = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item/module')
MK2 = ['holomap']


def main():
    for unit in MK2:
        w, h, px = read_png(os.path.join(DIR, 'item_module_' + unit + '.png'))
        assert (w, h) == (16, 16), (unit, w, h)
        out = tex_1100.mk2(px)
        assert out != px, 'no chip frame pixels to recolour in ' + unit
        write_png(os.path.join(DIR, 'item_module_' + unit + '_mk2.png'), out)
    print('wrote', len(MK2), 'Mk.II icon(s)')


if __name__ == '__main__':
    main()
