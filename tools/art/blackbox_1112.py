#!/usr/bin/env python3
"""0.1.10.12 icon of the reactor blackbox (16x16): an orange flight-recorder case with reflective
stripes, a riveted lid, a small trefoil plate and a scorched corner.

Usage: blackbox_1112.py <repo root> [--preview <dir>]
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixart import Canvas, hexc, ramp, sheet  # noqa: E402

N = 16
OUT = 'src/main/resources/assets/mio_icif/textures/item/reactor/item_reactor_blackbox.png'


def draw():
    c = Canvas(N, N)
    body = ramp(hexc('#e2701e'), 5, -0.5, 0.35)          # dark .. light orange
    steel = ramp(hexc('#8a9096'), 4, -0.55, 0.3)
    rim = hexc('#2a1a12')
    # case (x 2..13, y 4..14) with a lid band on top (y 2..4)
    c.rect(2, 4, 13, 14, body[2])
    for y in range(4, 15):
        c.set(2, y, body[3])
        c.set(13, y, body[0])
    for x in range(2, 14):
        c.set(x, 14, body[0])
        c.set(x, 4, body[4])
    # lid
    c.rect(3, 2, 12, 3, steel[1])
    for x in range(3, 13):
        c.set(x, 2, steel[3])
    c.set(3, 3, steel[2])
    c.set(12, 3, steel[0])
    # handle
    c.rect(6, 1, 9, 1, steel[0])
    c.set(6, 1, steel[2])
    # reflective stripes (white, diagonal hatching along the lower band)
    white = hexc('#f2efe6')
    for x in range(3, 13):
        if (x // 2) % 2 == 0:
            c.set(x, 12, white)
            c.set(x, 13, hexc('#cfcac0'))
    # trefoil plate (yellow with a black mark)
    c.rect(5, 6, 10, 10, hexc('#f2c230'))
    for x in range(5, 11):
        c.set(x, 10, hexc('#b88a1c'))
    mark = hexc('#1d1a16')
    c.set(7, 8, mark)
    c.set(8, 8, mark)
    c.set(6, 7, mark)
    c.set(9, 7, mark)
    c.set(7, 9, mark)
    c.set(8, 9, mark)
    c.set(6, 6, hexc('#3a2f20'))
    c.set(9, 6, hexc('#3a2f20'))
    # rivets
    for x, y in ((3, 5), (12, 5), (3, 11), (12, 11)):
        c.set(x, y, steel[3])
    # scorched corner (upper right of the case)
    soot = hexc('#3b2a20', 230)
    for x, y in ((12, 5), (13, 5), (12, 6), (13, 6), (13, 7), (11, 5), (13, 8)):
        c.set(x, y, soot)
    c.set(12, 7, hexc('#5a3c26'))
    c.outline(rim)
    return c


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else '.'
    icon = draw()
    path = os.path.join(root, OUT)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    icon.save(path)
    print('wrote', OUT, icon.opaque_count(), 'opaque pixels')
    if '--preview' in sys.argv:
        out = sys.argv[sys.argv.index('--preview') + 1]
        os.makedirs(out, exist_ok=True)
        sheet([icon], 1, 16).save(os.path.join(out, 'blackbox_1112_preview.png'))


if __name__ == '__main__':
    main()
