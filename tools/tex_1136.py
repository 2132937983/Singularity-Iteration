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


def colorize(src, dst, hue, min_sat):
    """Every visible pixel of src takes hue, with at least min_sat saturation (grey parts too): a clearly tinted copy."""
    im = Image.open(src).convert('RGBA')
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hsv_to_rgb(hue, max(min_sat, s), v)
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


def clone_json(src, dst, renames):
    """Copies a blockstate / model JSON, renaming the mod texture and model paths."""
    with open(src, encoding='utf-8') as f:
        text = f.read()
    for old, new in renames:
        text = text.replace(old, new)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    with open(dst, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def steam_reactor():
    """Steam reactor, steam reactor chamber (the nuclear reactor and chamber turned steel blue) and the four steam vents
    (the heat vents turned cyan); blockstates, block / item models."""
    assets = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
    renames = [('block_nuclear_reactor_generator', 'block_steam_reactor'), ('block_reactor_chamber', 'block_steam_reactor_chamber')]
    pairs = [('blockstates/generator/block_nuclear_reactor_generator.json', 'blockstates/generator/block_steam_reactor.json'),
             ('blockstates/reactor/block_reactor_chamber.json', 'blockstates/reactor/block_steam_reactor_chamber.json'),
             ('models/block/generator/block_nuclear_reactor_generator.json', 'models/block/generator/block_steam_reactor.json'),
             ('models/block/generator/block_nuclear_reactor_generator_on.json', 'models/block/generator/block_steam_reactor_on.json'),
             ('models/block/reactor/block_reactor_chamber.json', 'models/block/reactor/block_steam_reactor_chamber.json'),
             ('models/item/generator/block_nuclear_reactor_generator.json', 'models/item/generator/block_steam_reactor.json'),
             ('models/item/reactor/block_reactor_chamber.json', 'models/item/reactor/block_steam_reactor_chamber.json')]
    for src, dst in pairs:
        clone_json(os.path.join(assets, src), os.path.join(assets, dst), renames)
    # every mod texture the cloned models use, recoloured
    textures = set()
    for _, dst in pairs:
        with open(os.path.join(assets, dst), encoding='utf-8') as f:
            for ref in __import__('re').findall(r'"mio_icif:(block/[a-z_/]+)"', f.read()):
                if 'steam_reactor' in ref:
                    textures.add(ref)
    for ref in sorted(textures):
        old = ref.replace('block_steam_reactor_chamber', 'block_reactor_chamber').replace('block_steam_reactor', 'block_nuclear_reactor_generator')
        src = os.path.join(TEX, old + '.png')
        if os.path.exists(src):
            recolor(src, os.path.join(TEX, ref + '.png'), 0.56, 0.9, 1.1)
            meta = src + '.mcmeta'
            if os.path.exists(meta):
                with open(meta, 'rb') as f_in, open(os.path.join(TEX, ref + '.png.mcmeta'), 'wb') as f_out:
                    f_out.write(f_in.read())
    vents = {'steam_vent': 'vent', 'steam_vent_core': 'vent_core', 'steam_vent_overclocked': 'golden_vent', 'steam_vent_advanced': 'diamond_vent'}
    base = os.path.join(TEX, 'item/reactor')
    for name, source in vents.items():
        colorize(os.path.join(base, 'item_reactor_%s.png' % source), os.path.join(base, 'item_reactor_%s.png' % name), 0.55, 0.35)
        item_model('reactor/item_reactor_%s' % name)
    print('steam reactor: %d block textures, 7 json files; 4 steam vents' % len(textures))


def steam_turbine():
    """Steam turbine: the kinetic generator's texture, its grey casing faintly steel blue and the dark rotor socket of its
    front (off and on) steam cyan; the semifluid generator's blockstate and models (the same strip layout)."""
    assets = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
    renames = [('block_semifluid_generator', 'block_steam_turbine')]
    for kind, rel in (('blockstates', 'generator/%s.json'), ('models/block', 'generator/%s.json'),
                      ('models/block', 'generator/%s_on.json'), ('models/item', 'generator/%s.json')):
        clone_json(os.path.join(assets, kind, rel % 'block_semifluid_generator'), os.path.join(assets, kind, rel % 'block_steam_turbine'), renames)
    im = Image.open(os.path.join(TEX, 'block/generator/block_kinetic_generator.png')).convert('RGBA')
    px = im.load()
    face = im.width // 12
    sockets = {3: 1.0, 9: 1.4}   # the north face, off and on (brighter while it runs)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if a == 0 or s >= 0.2:   # the red EU bolt stays
                continue
            lift = sockets.get(x // face)
            if lift is not None and v <= 0.45:
                r2, g2, b2 = colorsys.hsv_to_rgb(0.52, 0.7, min(1.0, (v + 0.22) * lift))
            else:
                r2, g2, b2 = colorsys.hsv_to_rgb(0.58, 0.12, v)
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    im.save(os.path.join(TEX, 'block/generator/block_steam_turbine.png'))
    print('steam turbine: 1 block texture, 4 json files')


if __name__ == '__main__':
    proton_rods()
    iridium()
    steam_reactor()
    steam_turbine()
