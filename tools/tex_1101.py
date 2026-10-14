#!/usr/bin/env python3
"""
0.1.10.1: icons of quantum phase 2 (16x16, pure Python), on the chip frame of the Ballistic
Computer icon like the other quantum units:

  item/module/item_module_compute_core.png    a pale blue processor die with pins and a lit core
  item/module/item_module_<id>_mk2.png        Mk.II of the ore scanner, grid telemetry, entity ESP,
                                              ballistic computer, smart guidance and resonance
                                              field: violet rim, dark violet frame, gold LEDs
                                              (the same overlay as tex_1100.py)

Usage: tex_1101.py <repo root>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import read_png, write_png  # noqa: E402
import tex_1100  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
DIR = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item/module')
tex_1100.DIR = DIR


def compute_core():
    px, g, hi = tex_1100.chip(0x7FD8FF)
    pin = tex_1100.mix(g, tex_1100.DARK, 0.45)
    # pins above and below the die
    tex_1100.put(px, [(x, 5) for x in (7, 9, 11)] + [(x, 11) for x in (7, 9, 11)], pin)
    # the die outline
    tex_1100.put(px, [(x, 6) for x in range(6, 13)] + [(x, 10) for x in range(6, 13)]
                 + [(6, y) for y in range(7, 10)] + [(12, y) for y in range(7, 10)], g)
    # the lit core: a 2x3 bright block with a white centre
    tex_1100.put(px, ((8, 7), (10, 7), (8, 9), (10, 9)), tex_1100.mix(g, hi, 0.5))
    tex_1100.put(px, ((9, 7), (8, 8), (10, 8), (9, 9)), hi)
    tex_1100.put(px, ((9, 8),), (255, 255, 255, 255))
    return px


NEW = {'item_module_compute_core': compute_core}
MK2 = ['ore_scanner', 'grid_telemetry', 'entity_esp', 'ballistic', 'smart_guidance', 'resonance_field']


def main():
    for name, fn in NEW.items():
        write_png(os.path.join(DIR, name + '.png'), fn())
    for unit in MK2:
        w, h, px = read_png(os.path.join(DIR, 'item_module_' + unit + '.png'))
        assert (w, h) == (16, 16), (unit, w, h)
        write_png(os.path.join(DIR, 'item_module_' + unit + '_mk2.png'), tex_1100.mk2(px))
    print('wrote', len(NEW), 'icon and', len(MK2), 'Mk.II icons')


if __name__ == '__main__':
    main()
