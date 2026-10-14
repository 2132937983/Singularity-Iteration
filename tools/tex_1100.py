#!/usr/bin/env python3
"""
0.1.10.0: icons of the quantum action units (16x16, pure Python), drawn on the chip frame of the
Ballistic Computer icon like the other quantum units:

  item/module/item_module_phase_jump_drive.png      violet double chevron with a dotted trail (blink)
  item/module/item_module_phase_modulator.png       two crossing waves, magenta and white (phase)
  item/module/item_module_coherence_capacitor.png   cyan capacitor plates and leads
  item/module/item_module_entanglement_relay.png    two nodes joined by a double helix
  item/module/item_quantum_bay_expander.png         an empty socket with a plus (violet accent)
  item/module/item_module_<id>_mk2.png              Mk.II of the deflector, the behaviour predictor
                                                     and the two movement units: violet rim, gold LEDs

The chip colours follow the existing quantum icons: accent strip = 0.75 x unit colour, body
checker = (10,16,20) and (10,16,20) + 0.07 x unit colour, symbol = unit colour, highlight = a mix
with white.
Usage: tex_1100.py <repo root>
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tex_builder_180 import read_png, write_png  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
DIR = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item/module')

DARK = (10, 16, 20, 255)
RIM = (92, 100, 110, 255)
FRAME = (28, 32, 38, 255)
LED_ROW = 13


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255, 255)


def mix(a, b, k):
    return tuple(int(round(a[i] + (b[i] - a[i]) * k)) for i in range(3)) + (255,)


def chip(color):
    """The ballistic chip with the accent strip and the body checker in the unit colour."""
    w, h, px = read_png(os.path.join(DIR, 'item_module_ballistic.png'))
    assert (w, h) == (16, 16), (w, h)
    out = [row[:] for row in px]
    c = rgb(color)
    accent = tuple(int(round(v * 0.75)) for v in c[:3]) + (255,)
    tint = tuple(min(255, int(round(DARK[i] + 0.07 * c[i]))) for i in range(3)) + (255,)
    for y in range(3, 13):
        out[y][2] = accent
    for y in range(4, 12):
        for x in range(4, 14):
            out[y][x] = tint if (x + y) % 2 == 1 else DARK
    return out, c, mix(c, (255, 255, 255, 255), 0.7)


def put(px, pts, c):
    for (x, y) in pts:
        if 4 <= x <= 13 and 4 <= y <= 11:
            px[y][x] = c


def phase_jump_drive():
    px, g, hi = chip(0x8F7CFF)
    put(px, ((5, 8), (7, 8)), mix(g, DARK, 0.35))
    put(px, ((8, 6), (9, 7), (10, 8), (9, 9), (8, 10)), g)
    put(px, ((10, 6), (11, 7), (12, 8), (11, 9), (10, 10)), hi)
    return px


def phase_modulator():
    px, g, hi = chip(0xD06BFF)
    for x in range(5, 13):
        put(px, ((x, 8 + int(round(2.2 * math.sin(x * 0.95)))),), g)
        put(px, ((x, 8 + int(round(2.2 * math.sin(x * 0.95 + math.pi)))),), hi)
    return px


def coherence_capacitor():
    px, g, hi = chip(0x5CE1E6)
    put(px, [(7, y) for y in range(5, 12)] + [(10, y) for y in range(5, 12)], g)
    put(px, ((5, 8), (6, 8), (11, 8), (12, 8)), hi)
    put(px, ((8, 6), (9, 6)), mix(g, DARK, 0.5))
    return px


def entanglement_relay():
    px, g, hi = chip(0x6FF3D6)
    strand2 = mix(g, (185, 140, 255, 255), 0.7)
    for x in range(6, 12):
        k = int(round(1.8 * math.sin((x - 5.5) * 1.05)))
        put(px, ((x, 8 + k),), g)
        put(px, ((x, 8 - k),), strand2)
    # the two entangled nodes
    put(px, ((4, 8), (5, 7), (5, 8), (5, 9), (12, 7), (12, 8), (12, 9), (13, 8)), hi)
    return px


def bay_expander():
    px, g, hi = chip(0xB98CFF)
    # an empty socket outline and a plus in the middle
    for x in range(5, 13):
        if x % 2 == 1:
            put(px, ((x, 5), (x, 11)), mix(g, DARK, 0.3))
    for y in range(5, 12):
        if y % 2 == 1:
            put(px, ((5, y), (12, y)), mix(g, DARK, 0.3))
    put(px, ((8, 7), (9, 7), (8, 9), (9, 9), (7, 8), (10, 8)), g)
    put(px, ((8, 8), (9, 8)), hi)
    # gold pins all along the bottom edge (a bay, not a unit)
    for x in range(3, 13, 2):
        px[14][x] = (255, 205, 90, 255)
    return px


VIOLET = (185, 140, 255, 255)
VIOLET_FRAME = (70, 40, 120, 255)
GOLD = (255, 205, 90, 255)


def mk2(px):
    """Mk.II: the grey top rim turns violet, the frame dark violet, two gold LEDs light up."""
    out = [row[:] for row in px]
    for y in range(16):
        for x in range(16):
            if out[y][x] == RIM:
                out[y][x] = VIOLET
            elif out[y][x] == FRAME:
                out[y][x] = VIOLET_FRAME
    out[LED_ROW][10] = GOLD
    out[LED_ROW][11] = GOLD
    return out


NEW = {
    'item_module_phase_jump_drive': phase_jump_drive,
    'item_module_phase_modulator': phase_modulator,
    'item_module_coherence_capacitor': coherence_capacitor,
    'item_module_entanglement_relay': entanglement_relay,
    'item_quantum_bay_expander': bay_expander,
}
MK2 = ['deflector', 'behavior_predictor', 'phase_jump_drive', 'phase_modulator']


def main():
    for name, fn in NEW.items():
        write_png(os.path.join(DIR, name + '.png'), fn())
    for unit in MK2:
        w, h, px = read_png(os.path.join(DIR, 'item_module_' + unit + '.png'))
        write_png(os.path.join(DIR, 'item_module_' + unit + '_mk2.png'), mk2(px))
    print('wrote', len(NEW), 'icons and', len(MK2), 'Mk.II icons')


if __name__ == '__main__':
    main()
