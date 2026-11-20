#!/usr/bin/env python3
"""0.1.11.98: the silver ore (stone and deepslate), silver block and raw silver block textures, made from the mod's own lead
ones (block_ore_lead, block_ore_lead_in_deep, block_lead, block_raw_lead).

The lead pixels (blue-tinted; in the ores, the stone and deepslate around them are grey and stay as they are) take the colour
of the same brightness, a little lighter, on a ramp from the mod's raw silver and silver ingot icons: silver is a near-white,
faintly cool grey where lead is lavender-blue. Every pixel of the two storage blocks is metal. Idempotent.

Usage: silver_blocks_11598.py <repo root>
"""
import os
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
BLOCK = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/block')
ITEM = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item/resource')
# target <- (lead source, whether every pixel is metal)
TEXTURES = {
    'block_ore_silver': ('block_ore_lead', False),
    'block_ore_silver_in_deep': ('block_ore_lead_in_deep', False),
    'block_silver': ('block_lead', True),
    'block_raw_silver': ('block_raw_lead', True),
}
BRIGHTER = 1.12   # silver is lighter than lead at the same shade
LIFT = 8
# the ores' specks: their shades spread over this brightness range, so they stand out of the grey stone as lead's blue does
SPECK_LOW, SPECK_HIGH = 158, 255


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp():
    """The silver colours of the icons, darkest first."""
    colours = set()
    for name in ('item_raw_silver_ore', 'item_silver_ingot'):
        im = Image.open(os.path.join(ITEM, name + '.png')).convert('RGBA')
        for y in range(im.size[1]):
            for x in range(im.size[0]):
                p = im.getpixel((x, y))
                if p[3] > 0:
                    colours.add(p[:3])
    return sorted(colours, key=lum)


def silver(target, colours):
    """The ramp colour at brightness {@code target}, between its two neighbours."""
    if target <= lum(colours[0]):
        return colours[0]
    for a, b in zip(colours, colours[1:]):
        la, lb = lum(a), lum(b)
        if la <= target <= lb:
            t = 0 if lb == la else (target - la) / (lb - la)
            return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))
    return colours[-1]


def is_lead(p):
    """A lead pixel of an ore: tinted blue (the stone and deepslate are grey)."""
    r, g, b = p[:3]
    return p[3] > 0 and b - (r + g) / 2 >= 10


def main():
    colours = ramp()
    for target, (source, all_metal) in TEXTURES.items():
        im = Image.open(os.path.join(BLOCK, source + '.png')).convert('RGBA')
        px = im.load()
        metal = [px[x, y] for y in range(im.size[1]) for x in range(im.size[0])
                 if px[x, y][3] > 0 and (all_metal or is_lead(px[x, y]))]
        low, high = (min(map(lum, metal)), max(map(lum, metal))) if metal else (0, 1)
        changed = 0
        for y in range(im.size[1]):
            for x in range(im.size[0]):
                p = px[x, y]
                if p[3] == 0 or not (all_metal or is_lead(p)):
                    continue
                if all_metal:
                    shade = min(255, lum(p) * BRIGHTER + LIFT)
                else:
                    shade = SPECK_LOW + (lum(p) - low) / max(1e-6, high - low) * (SPECK_HIGH - SPECK_LOW)
                c = silver(shade, colours)
                px[x, y] = (c[0], c[1], c[2], p[3])
                changed += 1
        if changed == 0:
            raise SystemExit('no lead pixels in ' + source)
        im.save(os.path.join(BLOCK, target + '.png'))
        print(target, '<-', source, changed, 'pixels')


if __name__ == '__main__':
    main()
