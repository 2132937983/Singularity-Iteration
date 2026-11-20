#!/usr/bin/env python3
"""0.1.11.96: the cable skins back in the original order: medium voltage (block_cable_o / block_cable) copper, high voltage
(block_gold_cable*) gold, extra-high voltage (block_iron_cable*) iron.

0.1.11.4 made the cables of tin, iron, copper and gold (low to extra-high voltage); the user-supplied cable textures of
0.1.11.25 were drawn for that order, so the copper-named MV cable looked like iron, the gold-named HV cable like copper and the
iron-named EV cable like gold. The materials are back to tin, copper, gold, iron in 0.1.11.96, and this script gives each
cable the skin of its metal again, block textures and item icons alike:

  MV  <- the copper-coloured skins (0.1.11.25's HV files)
  HV  <- the gold-coloured skins (0.1.11.25's EV files, bare, 1 and 2 layers)
  EV  <- the iron-coloured skins (0.1.11.25's MV files, bare and 1 layer); the 2- and 3-layer EV cables had only gold cores,
         so their core is recoloured with the colours the 1-layer cable's core changes by (gold -> iron)

The sources are read from commit 981f771c (0.1.11.25, the last change of these files), so a second run writes the same files.

Usage: cable_skins_11596.py <repo root>
"""
import io
import os
import subprocess
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
SOURCE = '981f771c'
TEX = 'src/main/resources/assets/mio_icif/textures/'
B, I = 'block/wiring/cable/', 'item/normal/'

# target <- source (both as of SOURCE)
COPY = {
    B + 'block_cable_o': B + 'block_gold_cable', B + 'block_cable': B + 'block_gold_cable_1',
    B + 'block_gold_cable': B + 'block_iron_cable', B + 'block_gold_cable_1': B + 'block_iron_cable_1',
    B + 'block_gold_cable_2': B + 'block_iron_cable_2',
    B + 'block_iron_cable': B + 'block_cable_o', B + 'block_iron_cable_1': B + 'block_cable',
    I + 'item_copper_cable': I + 'item_golden_cable', I + 'item_copper_cable_1': I + 'item_golden_cable_1',
    I + 'item_golden_cable': I + 'item_iron_cable', I + 'item_golden_cable_1': I + 'item_iron_cable_1',
    I + 'item_golden_cable_2': I + 'item_iron_cable_2',
    I + 'item_iron_cable': I + 'item_copper_cable', I + 'item_iron_cable_1': I + 'item_copper_cable_1',
}
# target <- (source to recolour, the gold 1-layer skin, the iron 1-layer skin that gives the colour mapping)
RECOLOUR = {
    B + 'block_iron_cable_2': (B + 'block_iron_cable_2', B + 'block_iron_cable_1', B + 'block_cable'),
    B + 'block_iron_cable_3': (B + 'block_iron_cable_3', B + 'block_iron_cable_1', B + 'block_cable'),
    I + 'item_iron_cable_2': (I + 'item_iron_cable_2', I + 'item_iron_cable_1', I + 'item_copper_cable_1'),
    I + 'item_iron_cable_3': (I + 'item_iron_cable_3', I + 'item_iron_cable_1', I + 'item_copper_cable_1'),
}


def source(path):
    data = subprocess.run(['git', 'show', '%s:%s%s.png' % (SOURCE, TEX, path)], cwd=ROOT, capture_output=True, check=True).stdout
    return Image.open(io.BytesIO(data)).convert('RGBA')


def coloured(p):
    return p[3] > 0 and not (abs(p[0] - p[1]) < 12 and abs(p[1] - p[2]) < 12)


def mapping(gold, iron):
    a, b = source(gold), source(iron)
    out = {}
    for y in range(a.size[1]):
        for x in range(a.size[0]):
            if a.getpixel((x, y)) != b.getpixel((x, y)):
                out.setdefault(a.getpixel((x, y)), b.getpixel((x, y)))
    return out


def nearest(colour, table):
    """The mapped colour of the table entry closest in brightness (for a gold shade the 1-layer core does not have)."""
    lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    key = min(table, key=lambda k: abs(lum(k) - lum(colour)))
    return table[key]


def main():
    written = []
    for target, src in COPY.items():
        source(src).save(os.path.join(ROOT, TEX, target + '.png'))
        written.append(target)
    for target, (src, gold, iron) in RECOLOUR.items():
        table = {k: v for k, v in mapping(gold, iron).items() if coloured(k)}
        im = source(src)
        px = im.load()
        for y in range(im.size[1]):
            for x in range(im.size[0]):
                if coloured(px[x, y]):
                    px[x, y] = table.get(px[x, y]) or nearest(px[x, y], table)
        if any(coloured(px[x, y]) for y in range(im.size[1]) for x in range(im.size[0])):
            raise SystemExit('gold left in ' + target)
        im.save(os.path.join(ROOT, TEX, target + '.png'))
        written.append(target)
    print('cable skins written:', len(written))


if __name__ == '__main__':
    main()
