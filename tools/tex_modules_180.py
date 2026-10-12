#!/usr/bin/env python3
"""
0.1.8.0: icons of the new quantum suit units (16x16), drawn on the chip frame of the
Ballistic Computer icon:
  item_module_smart_guidance.png   red lock diamond with three closing triangles
  item_module_resonance_field.png  violet standing-wave rings
Usage: tex_modules_180.py <repo root>
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import read_png, write_png  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
DIR = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item/module')


def chip():
    w, h, px = read_png(os.path.join(DIR, 'item_module_ballistic.png'))
    assert (w, h) == (16, 16), (w, h)
    out = [row[:] for row in px]
    # clear the old symbol inside the chip body (keep the frame and the pins)
    body = out[8][8]
    for y in range(4, 12):
        for x in range(4, 12):
            if out[y][x][3] > 0:
                out[y][x] = (40, 44, 50, 255)
    return out


def smart_guidance():
    px = chip()
    red, hot = (255, 77, 109, 255), (255, 190, 200, 255)
    for (x, y) in ((8, 6), (7, 7), (9, 7), (6, 8), (10, 8), (7, 9), (9, 9), (8, 10)):
        px[y][x] = red
    px[8][8] = hot
    for (x, y) in ((8, 4), (5, 10), (11, 10)):           # closing triangles
        px[y][x] = hot
    return px


def resonance_field():
    px = chip()
    violet, bright = (155, 124, 255, 255), (215, 200, 255, 255)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if abs(d - 3.4) < 0.5:
                px[y][x] = violet
            elif abs(d - 1.6) < 0.5:
                px[y][x] = bright
    return px


def main():
    write_png(os.path.join(DIR, 'item_module_smart_guidance.png'), smart_guidance())
    write_png(os.path.join(DIR, 'item_module_resonance_field.png'), resonance_field())
    print('wrote module icons')


if __name__ == '__main__':
    main()
