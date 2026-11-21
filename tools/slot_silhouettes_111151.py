#!/usr/bin/env python3
"""0.1.11.151: slot silhouettes, as vanilla's empty armour slots show what goes there: for each item below, a 16 x 16 icon in
textures/gui/sprites/slot/<name>.png (the GUI sprite atlas, drawn with GuiGraphics.blitSprite) made from the item's own texture
(the mod's; no vanilla texture is used): its outline, one pixel wide in vanilla's #555555, and the item's own dark line work inside
it, on a transparent ground (the slot's grey shows through).

The outline is every pixel of the item (any alpha) next to a transparent one (or the image edge); the line work is every pixel
inside that is much darker than the item's average (its drawn edges and seams), and for the items in CONTRAST also every pixel much
darker than a neighbour (a grille's slots). All the model's layers count (a cell's glass and its fluid). Idempotent.

Usage: slot_silhouettes_111151.py <repo root> [--preview <png>]
"""
import json
import os
import sys

from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith('--') else '.'
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
OUT = os.path.join(ASSETS, 'textures/gui/sprites/slot')
INK = (0x55, 0x55, 0x55, 255)
DARK = 0.62          # a pixel this much darker than the item's average luminance is line work

CONTRAST = {'vent': 70, 'battery': 40}   # silhouette -> how much darker than a neighbour a pixel of line work is (the
# vent's grille, the line between the battery's cap and its body)

# silhouette name -> item model (models/item/<model>.json; its layers are the texture)
ITEMS = {
    'battery': 'normal/item_chargebat_lev0',
    'cell': 'cell/item_cell_dynamic_empty',   # what an empty cell shows (item_cell_empty picks it by an override)
    'vent': 'reactor/item_reactor_vent',
    'coil': 'resource/item_coil',
    'rotor': 'rotors/item_iron_rotor',
    'turbine': 'normal/item_steam_turbine_blade',
    'heat_conductor': 'resource/item_heatconductor',
    'drill': 'item_tool_iron_driller',
    'scanner': 'item_tool_od_scanner',
    'memory': 'resource/item_crystal_memory',
    'rtg_pellet': 'resource/item_rtg_pellet',
    'fuel_rod': 'normal/item_fuel_rod',
    'tin_can': 'normal/item_tin_empty_can',
    'cut_blade': 'resource/item_iron_cut_blade',
    'coolant_cell': 'reactor/item_reactor_coolant_six',
    'deuterium_rod': 'normal/item_deuterium_cell',
    'tritium_rod': 'normal/item_tritium_cell',
    'neon_pellet': 'normal/item_frozen_neon_pellet',
}


def textures_of(item):
    """The texture files of the item model's layers (layer0 up), following its parents to the first that names layers."""
    model = 'item/' + item
    for _ in range(6):
        path = os.path.join(ASSETS, 'models', model + '.json')
        if not os.path.exists(path):
            break
        data = json.load(open(path, encoding='utf-8'))
        layers = [data.get('textures', {}).get('layer%d' % i) for i in range(5)]
        layers = [layer for layer in layers if layer]
        if layers:
            files = []
            for layer in layers:
                ns, rel = layer.split(':', 1) if ':' in layer else ('minecraft', layer)
                if ns != 'mio_icif':
                    raise SystemExit('%s uses %s (not the mod\'s own texture)' % (item, layer))
                files.append(os.path.join(ASSETS, 'textures', rel + '.png'))
            return files
        parent = data.get('parent', '')
        if not parent.startswith('mio_icif:'):
            break
        model = parent.split(':', 1)[1]
    raise SystemExit('no layer textures for ' + item)


def load(src):
    im = Image.open(src).convert('RGBA')
    if im.size != (16, 16):
        im = im.crop((0, 0, im.size[0], im.size[0])).resize((16, 16), Image.NEAREST)   # an animated strip: its first frame
    return im


def silhouette(srcs, contrast=None):
    im = load(srcs[0])
    for src in srcs[1:]:
        im.alpha_composite(load(src))
    px = im.load()
    solid = [[px[x, y][3] > 0 for x in range(16)] for y in range(16)]
    lum = lambda p: 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]
    inside = [lum(px[x, y]) for y in range(16) for x in range(16) if solid[y][x]]
    mean = sum(inside) / max(1, len(inside))
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    o = out.load()
    for y in range(16):
        for x in range(16):
            if not solid[y][x]:
                continue
            sides = ((1, 0), (-1, 0), (0, 1), (0, -1))
            edge = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or not solid[y + dy][x + dx] for dx, dy in sides)
            l = lum(px[x, y])
            line = l < mean * DARK or contrast is not None and any(
                0 <= x + dx < 16 and 0 <= y + dy < 16 and solid[y + dy][x + dx] and lum(px[x + dx, y + dy]) - l > contrast
                for dx, dy in sides)
            if edge or line:
                o[x, y] = INK
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    made = {}
    for name, item in ITEMS.items():
        im = silhouette(textures_of(item), CONTRAST.get(name))
        im.save(os.path.join(OUT, name + '.png'))
        made[name] = im
    print('silhouettes written:', len(made))
    if '--preview' in sys.argv:
        out = Image.new('RGBA', (len(made) * 80, 80), (0x8B, 0x8B, 0x8B, 255))
        for i, (name, im) in enumerate(made.items()):
            out.alpha_composite(im.resize((64, 64), Image.NEAREST), (i * 80 + 8, 8))
        out.save(sys.argv[sys.argv.index('--preview') + 1])


if __name__ == '__main__':
    main()
