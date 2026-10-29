#!/usr/bin/env python3
"""0.1.10.36 textures, derived from existing mod textures (no external art).
- proton fuel rods (single / dual / quad and their depleted rods): the uranium rods with the fuel window turned violet
  (only saturated pixels change hue; the grey casing stays).
- deepslate iridium ore and small iridium dust: the deepslate lead ore and small lead dust with the bluish speckles turned to
  the pale pink-white of the iridium item; their blockstate and models.
Idempotent: writes the same files from the same sources.
Usage: tex_1136.py <repo root>
"""
import colorsys
import os
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
TEX = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/textures')
MODELS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/models')


def recolor(src, dst, hue, sat_scale=1.0, val_scale=1.0):
    """Turns every saturated pixel of src to hue (0..1), scaling saturation and value; grey and transparent pixels stay."""
    im = Image.open(src).convert('RGBA')
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s < 0.2:
                continue
            r2, g2, b2 = colorsys.hsv_to_rgb(hue, min(1.0, s * sat_scale), min(1.0, v * val_scale))
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    im.save(dst)


def item_model(path):
    """models/item/<path>.json with item/generated and the texture of the same path."""
    out = os.path.join(MODELS, 'item', path + '.json')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, 'w', encoding='utf-8', newline='\n') as f:
        f.write('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "mio_icif:item/%s"\n  }\n}\n' % path)


def proton_rods():
    rods = {'simple': ('uranium_simple', 'uranium_simple_depleted'),
            'dual': ('uranium_dual', 'uranium_dual_depleted'),
            'quad': ('uranium_quad', 'uranium_quad_deplete')}
    base = os.path.join(TEX, 'item/reactor')
    for size, (fresh, spent) in rods.items():
        # fresh: bright violet; depleted: dull, dark violet
        recolor(os.path.join(base, 'item_reactor_%s.png' % fresh), os.path.join(base, 'item_reactor_proton_%s.png' % size), 0.79, 1.1, 1.25)
        recolor(os.path.join(base, 'item_reactor_%s.png' % spent), os.path.join(base, 'item_reactor_proton_%s_depleted.png' % size), 0.79, 0.7, 0.9)
        item_model('reactor/item_reactor_proton_%s' % size)
        item_model('reactor/item_reactor_proton_%s_depleted' % size)
    print('proton rods: 6 textures, 6 models')


def recolor_tinted(src, dst, hue, min_sat, val_scale, min_tint=10):
    """Turns the bluish pixels of src (blue - red > min_tint: ore speckles, lead dust) to hue; grey stone stays."""
    im = Image.open(src).convert('RGBA')
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0 or b - r <= min_tint:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hsv_to_rgb(hue, max(min_sat, s * 0.8), min(1.0, v * val_scale))
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    im.save(dst)


def iridium():
    """Deepslate iridium ore and small iridium dust, in the pale pink-white of the mod's iridium item."""
    blocks, items = os.path.join(TEX, 'block'), os.path.join(TEX, 'item/resource')
    recolor_tinted(os.path.join(blocks, 'block_ore_lead_in_deep.png'), os.path.join(blocks, 'block_ore_iridium_in_deep.png'), 0.93, 0.16, 1.18)
    recolor_tinted(os.path.join(items, 'item_lead_dust_small.png'), os.path.join(items, 'item_iridium_dust_small.png'), 0.93, 0.18, 1.12)
    assets = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
    with open(os.path.join(assets, 'blockstates/block_ore_iridium_in_deep.json'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('{\n    "variants": {\n        "": {\n            "model": "mio_icif:block/block_ore_iridium_in_deep"\n        }\n    }\n}\n')
    with open(os.path.join(MODELS, 'block/block_ore_iridium_in_deep.json'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('{\n    "parent": "minecraft:block/cube_all",\n    "textures": {\n        "all": "mio_icif:block/block_ore_iridium_in_deep"\n    }\n}\n')
    with open(os.path.join(MODELS, 'item/block_ore_iridium_in_deep.json'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('{\n    "parent": "mio_icif:block/block_ore_iridium_in_deep"\n}\n')
    item_model('resource/item_iridium_dust_small')
    print('iridium: 2 textures, blockstate, 3 models')


if __name__ == '__main__':
    proton_rods()
    iridium()
