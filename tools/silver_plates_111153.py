#!/usr/bin/env python3
"""0.1.11.153: the silver plate and dense silver plate textures, made from the mod's own lead plate and dense lead plate (the
silver casing already existed): every pixel takes the colour of the same brightness, a little lighter, on a ramp from the mod's
silver casing and silver ingot icons, so the plates match the casing (near-white, faintly cool grey where lead is lavender-blue).
Idempotent.

Usage: silver_plates_111153.py <repo root> [--preview <png>]
"""
import os
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith('--') else '.'
DIR = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures/item/resource')
TEXTURES = {'item_silver_plate': 'item_lead_plate', 'item_silver_denseplate': 'item_lead_denseplate'}
BRIGHTER, LIFT = 1.12, 8


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp():
    colours = set()
    for name in ('item_silver_casing', 'item_silver_ingot'):
        im = Image.open(os.path.join(DIR, name + '.png')).convert('RGBA')
        for y in range(16):
            for x in range(16):
                p = im.getpixel((x, y))
                if p[3] > 0:
                    colours.add(p[:3])
    return sorted(colours, key=lum)


def silver(target, colours):
    if target <= lum(colours[0]):
        return colours[0]
    for a, b in zip(colours, colours[1:]):
        la, lb = lum(a), lum(b)
        if la <= target <= lb:
            t = 0 if lb == la else (target - la) / (lb - la)
            return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))
    return colours[-1]


def main():
    colours = ramp()
    made = {}
    for target, source in TEXTURES.items():
        im = Image.open(os.path.join(DIR, source + '.png')).convert('RGBA')
        px = im.load()
        for y in range(im.size[1]):
            for x in range(im.size[0]):
                p = px[x, y]
                if p[3] == 0:
                    continue
                c = silver(min(255, lum(p) * BRIGHTER + LIFT), colours)
                px[x, y] = (c[0], c[1], c[2], p[3])
        im.save(os.path.join(DIR, target + '.png'))
        made[target] = im
        print(target, '<-', source)
    if '--preview' in sys.argv:
        names = ['item_lead_plate', 'item_silver_plate', 'item_lead_denseplate', 'item_silver_denseplate', 'item_silver_casing']
        out = Image.new('RGBA', (len(names) * 72, 72), (139, 139, 139, 255))
        for i, n in enumerate(names):
            im = Image.open(os.path.join(DIR, n + '.png')).convert('RGBA')
            out.alpha_composite(im.crop((0, 0, 16, 16)).resize((64, 64), Image.NEAREST), (i * 72 + 4, 4))
        out.save(sys.argv[sys.argv.index('--preview') + 1])


if __name__ == '__main__':
    main()
