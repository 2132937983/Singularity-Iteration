#!/usr/bin/env python3
"""
0.1.7.33: assets of the quantum suit upgrade system.

Writes (relative to src/main/resources):
  textures: upgrade unit items (16x16), station block atlas (128x64, 32 px cells), station GUI (256x256)
  models / blockstate / loot table / recipes / tags for the station and the units

Usage: gen_suit_assets.py <repo root>
"""
import json, os, sys
from PIL import Image, ImageDraw

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
RES = os.path.join(ROOT, 'src/main/resources')
A = os.path.join(RES, 'assets/mio_icif')
D = os.path.join(RES, 'data/mio_icif')

UNITS = {
    # id: (colour, glyph rows 10x7, '#' = glyph colour, '+' = light)
    'ore_scanner': (0x3FD4FF, [
        "....#.....",
        "...###....",
        "..##+##...",
        "..#####...",
        "...###....",
        "....#.....",
        "#.#.#.#.#."]),
    'grid_telemetry': (0xFFD23F, [
        ".....##...",
        "....##....",
        "...####...",
        "....##....",
        "...##.....",
        "..##......",
        ".#........"]),
    'entity_esp': (0x7CFFB2, [
        "..........",
        "..######..",
        ".#..##..#.",
        "#..#++#..#",
        ".#..##..#.",
        "..######..",
        ".........."]),
    'ballistic': (0xFF9F1C, [
        "....#.....",
        "..#####...",
        ".#..#..#..",
        "####+####.",
        ".#..#..#..",
        "..#####...",
        "....#....."]),
    'blast_warning': (0xFF5A36, [
        "....#.....",
        "...#+#....",
        "...#+#....",
        "..#.+.#...",
        "..#...#...",
        ".#..+..#..",
        "#########."]),
    'behavior_predictor': (0xC77DFF, [
        "......#...",
        ".......#..",
        "..######+.",
        ".#.....#..",
        "#.....#...",
        "#.........",
        "+........."]),
    'holomap': (0x4DE1C1, [
        "....#.....",
        "...#+#....",
        "..#...#.#.",
        ".#.....#+#",
        "#.#.#.#.#.",
        ".#.#.#.#..",
        "..#.#.#..."]),
    'threat_sensor': (0xFF3B30, [
        "..#####...",
        ".#.....#..",
        "#..###..#.",
        "#..#+#..#.",
        "#..###..#.",
        ".#.....#..",
        "..#####..."]),
    'deflector': (0x5AB8FF, [
        "...####...",
        "..#....#..",
        ".#..++..#.",
        ".#.+..+.#.",
        ".#..++..#.",
        "..#....#..",
        "...####..."]),
}


def rgb(c, a=255):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255, a)


def mix(c1, c2, t):
    return tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(4))


def unit_texture(color, glyph):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    body = (52, 58, 66, 255)
    edge = (28, 32, 38, 255)
    hi = (92, 100, 110, 255)
    # card body with clipped corners
    for y in range(1, 15):
        for x in range(1, 15):
            if (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
                continue
            px[x, y] = body
    for x in range(2, 14):
        px[x, 1] = edge
        px[x, 14] = edge
    for y in range(2, 14):
        px[1, y] = edge
        px[14, y] = edge
    for x in range(2, 14):
        px[x, 2] = hi
    # gold contact pins
    for x in range(3, 13, 2):
        px[x, 15] = (214, 168, 64, 255)
        px[x, 14] = (240, 200, 90, 255)
    # signature stripe
    c = rgb(color)
    for y in range(3, 13):
        px[2, y] = mix(c, (0, 0, 0, 255), 0.25)
    # screen
    screen = (10, 16, 20, 255)
    for y in range(4, 12):
        for x in range(4, 14):
            px[x, y] = screen
    glow = mix(c, (255, 255, 255, 255), 0.0)
    light = mix(c, (255, 255, 255, 255), 0.65)
    dim = mix(c, screen, 0.7)
    for gy, row in enumerate(glyph):
        for gx, ch in enumerate(row):
            x, y = 4 + gx, 4 + gy + 1 if gy < 7 else 11
            if x > 13 or y > 11:
                continue
            if ch == '#':
                px[x, y] = glow
            elif ch == '+':
                px[x, y] = light
            elif px[x, y] == screen and (gx + gy) % 2 == 0:
                px[x, y] = mix(screen, dim, 0.25)
    # status LED
    px[12, 13] = light
    return im


def station_atlas():
    im = Image.new('RGBA', (128, 64), (0, 0, 0, 255))
    d = ImageDraw.Draw(im)

    def panel(cx, cy, base=(206, 210, 214), dark=(150, 156, 162), light=(236, 238, 240)):
        x0, y0 = cx * 32, cy * 32
        d.rectangle([x0, y0, x0 + 31, y0 + 31], fill=base + (255,))
        d.line([x0, y0, x0 + 31, y0], fill=light + (255,))
        d.line([x0, y0, x0, y0 + 31], fill=light + (255,))
        d.line([x0, y0 + 31, x0 + 31, y0 + 31], fill=dark + (255,))
        d.line([x0 + 31, y0, x0 + 31, y0 + 31], fill=dark + (255,))
        d.rectangle([x0 + 2, y0 + 2, x0 + 29, y0 + 29], outline=(184, 189, 194, 255))
        return x0, y0

    # 0: pad top - dark plate, hex lattice, cyan ring
    x0, y0 = 0, 0
    d.rectangle([x0, y0, x0 + 31, y0 + 31], fill=(40, 46, 54, 255))
    d.rectangle([x0, y0, x0 + 31, y0 + 31], outline=(120, 128, 136, 255))
    d.rectangle([x0 + 1, y0 + 1, x0 + 30, y0 + 30], outline=(70, 78, 86, 255))
    import math
    for r in range(3, 14, 1):
        pass
    for q in range(-4, 5):
        for rr in range(-4, 5):
            hx = 16 + q * 4.5
            hy = 16 + (rr + q / 2) * 5.2
            if (hx - 16) ** 2 + (hy - 16) ** 2 > 12.5 ** 2:
                continue
            pts = [(x0 + hx + 2.2 * math.cos(a * math.pi / 3), y0 + hy + 2.2 * math.sin(a * math.pi / 3)) for a in range(6)]
            d.polygon(pts, outline=(60, 120, 132, 255))
    d.ellipse([x0 + 3, y0 + 3, x0 + 28, y0 + 28], outline=(77, 225, 193, 255))
    d.ellipse([x0 + 5, y0 + 5, x0 + 26, y0 + 26], outline=(40, 110, 104, 255))
    d.ellipse([x0 + 13, y0 + 13, x0 + 18, y0 + 18], fill=(170, 255, 236, 255))
    for a in range(0, 360, 45):
        ax = x0 + 16 + 14 * math.cos(math.radians(a))
        ay = y0 + 16 + 14 * math.sin(math.radians(a))
        d.point((ax, ay), fill=(170, 255, 236, 255))

    # 1: base side - bevel panel with cyan light strip
    x0, y0 = panel(1, 0)
    d.rectangle([x0 + 4, y0 + 22, x0 + 27, y0 + 23], fill=(77, 225, 193, 255))
    for i in range(6):
        d.line([x0 + 6 + i * 4, y0 + 26, x0 + 6 + i * 4, y0 + 28], fill=(150, 156, 162, 255))
    # 2: base front - panel with small display and slot
    x0, y0 = panel(2, 0)
    d.rectangle([x0 + 6, y0 + 19, x0 + 25, y0 + 27], fill=(16, 22, 26, 255), outline=(110, 116, 122, 255))
    for i, c in enumerate([(77, 225, 193), (124, 255, 178), (255, 210, 63)]):
        d.rectangle([x0 + 8 + i * 6, y0 + 22, x0 + 11 + i * 6, y0 + 24], fill=c + (255,))
    # 3: frame front - emitter panel with three lenses
    x0, y0 = panel(3, 0, base=(72, 80, 90), dark=(40, 46, 52), light=(120, 128, 138))
    for i in range(3):
        cx = x0 + 7 + i * 9
        d.ellipse([cx - 3, y0 + 9, cx + 3, y0 + 15], fill=(18, 40, 44, 255), outline=(150, 160, 170, 255))
        d.ellipse([cx - 1, y0 + 11, cx + 1, y0 + 13], fill=(170, 255, 236, 255))
    d.rectangle([x0 + 4, y0 + 20, x0 + 27, y0 + 21], fill=(77, 225, 193, 255))
    d.rectangle([x0 + 4, y0 + 24, x0 + 27, y0 + 27], fill=(30, 36, 42, 255))
    # 4: frame side/top
    x0, y0 = panel(0, 1, base=(196, 200, 205))
    d.line([x0 + 4, y0 + 16, x0 + 27, y0 + 16], fill=(150, 156, 162, 255))
    # 5: base bottom
    x0, y0 = 32, 32
    d.rectangle([x0, y0, x0 + 31, y0 + 31], fill=(110, 116, 122, 255))
    d.rectangle([x0 + 2, y0 + 2, x0 + 29, y0 + 29], outline=(90, 96, 102, 255))
    # 6: emitter glow
    x0, y0 = 64, 32
    for y in range(32):
        t = abs(y - 16) / 16
        c = mix((210, 255, 246, 255), (40, 160, 150, 255), t)
        d.line([x0, y0 + y, x0 + 31, y0 + y], fill=c)
    # 7: spare (dark)
    d.rectangle([96, 32, 127, 63], fill=(30, 34, 40, 255))
    return im


def cell_uv(c, x0, y0, x1, y1):
    """uv of pixel rect (in 0..32 of the cell) in 16-unit texture space (128x64 atlas)."""
    u0 = (c % 4) * 4
    v0 = (c // 4) * 8
    return [round(u0 + x0 / 8, 4), round(v0 + y0 / 4, 4), round(u0 + x1 / 8, 4), round(v0 + y1 / 4, 4)]


def station_model():
    def face(c, x0, y0, x1, y1, cull=None):
        f = {'uv': cell_uv(c, x0, y0, x1, y1), 'texture': '#t'}
        if cull:
            f['cullface'] = cull
        return f
    # base 16x7: side faces use the lower 7/16 of the cell (y 18..32 px)
    base = {'name': 'base', 'from': [0, 0, 0], 'to': [16, 7, 16], 'faces': {
        'north': face(2, 0, 18, 32, 32, 'north'), 'south': face(1, 0, 18, 32, 32, 'south'),
        'east': face(1, 0, 18, 32, 32, 'east'), 'west': face(1, 0, 18, 32, 32, 'west'),
        'up': face(0, 0, 0, 32, 32), 'down': face(5, 0, 0, 32, 32, 'down')}}
    frame = {'name': 'frame', 'from': [1, 7, 12], 'to': [15, 16, 15], 'faces': {
        'north': face(3, 2, 6, 30, 24), 'south': face(4, 2, 6, 30, 24),
        'east': face(4, 0, 6, 6, 24), 'west': face(4, 0, 6, 6, 24),
        'up': face(4, 2, 0, 30, 6, 'up'), 'down': face(4, 2, 0, 30, 6)}}
    emitter = {'name': 'emitter', 'from': [2, 14, 10.5], 'to': [14, 15, 12], 'faces': {
        'north': face(6, 0, 12, 32, 20), 'south': face(6, 0, 12, 32, 20), 'east': face(6, 0, 12, 4, 20),
        'west': face(6, 0, 12, 4, 20), 'up': face(4, 2, 0, 30, 4), 'down': face(6, 0, 8, 32, 24)}}
    posts = []
    for i, x in enumerate([1, 13]):
        posts.append({'name': 'post%d' % i, 'from': [x, 7, 1], 'to': [x + 2, 8.5, 3], 'faces': {
            k: face(4, 0, 0, 4, 4) for k in ['north', 'south', 'east', 'west', 'up', 'down']}})
    return {
        'credit': 'Singularity Iteration 0.1.7.33 (gen_suit_assets.py)',
        'parent': 'block/block',
        'textures': {'particle': 'mio_icif:block/producer/block_quantum_modification_station',
                     't': 'mio_icif:block/producer/block_quantum_modification_station'},
        'elements': [base, frame, emitter] + posts,
    }


def gui_texture():
    src = Image.open(os.path.join(A, 'textures/gui/gui_molecular_transformer.png')).convert('RGBA')
    out = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    W, H = 176, 206
    # top border rows (0..4) and the inventory part (78..166 -> 118..206)
    out.paste(src.crop((0, 0, 176, 6)), (0, 0))
    filler = src.crop((0, 6, 176, 26))       # plain panel band (no slots)
    for y in range(6, 118, 20):
        out.paste(filler.crop((0, 0, 176, min(20, 118 - y))), (0, y))
    out.paste(src.crop((0, 78, 176, 166)), (0, 118))
    d = ImageDraw.Draw(out)

    def slot(x, y, big=False):
        s = 26 if big else 18
        ox, oy = (x - 5, y - 5) if big else (x - 1, y - 1)
        d.rectangle([ox, oy, ox + s - 1, oy + s - 1], fill=(168, 172, 177, 255))
        d.line([ox, oy, ox + s - 1, oy], fill=(122, 127, 133, 255))
        d.line([ox, oy, ox, oy + s - 1], fill=(122, 127, 133, 255))
        d.line([ox + 1, oy + 1, ox + s - 2, oy + 1], fill=(156, 160, 165, 255))
        d.line([ox + 1, oy + 1, ox + 1, oy + s - 2], fill=(156, 160, 165, 255))
        d.line([ox, oy + s - 1, ox + s - 1, oy + s - 1], fill=(247, 248, 249, 255))
        d.line([ox + s - 1, oy, ox + s - 1, oy + s - 1], fill=(247, 248, 249, 255))
        if big:
            d.rectangle([ox + 4, oy + 4, ox + s - 5, oy + s - 5], outline=(122, 127, 133, 255))

    slot(30, 22, big=True)   # suit piece
    slot(30, 72)             # unit
    slot(8, 92)              # battery
    # energy column frame
    d.rectangle([8, 18, 17, 87], fill=(30, 34, 40, 255), outline=(122, 127, 133, 255))
    # progress channel
    d.rectangle([35, 41, 42, 70], fill=(30, 34, 40, 255), outline=(122, 127, 133, 255))
    # unit list screen
    lx, ly, lw, lh = 60, 18, 108, 92
    d.rectangle([lx - 1, ly - 1, lx + lw, ly + lh], fill=(247, 248, 249, 255))
    d.rectangle([lx - 1, ly - 1, lx + lw - 1, ly + lh - 1], fill=(122, 127, 133, 255))
    d.rectangle([lx, ly, lx + lw - 1, ly + lh - 1], fill=(14, 20, 24, 255))
    for x in range(lx + 4, lx + lw, 8):
        for y in range(ly + 14, ly + lh - 12, 8):
            d.point((x, y), fill=(32, 48, 52, 255))
    d.line([lx + 2, ly + 12, lx + lw - 3, ly + 12], fill=(40, 90, 80, 255))
    d.line([lx + 2, ly + lh - 13, lx + lw - 3, ly + lh - 13], fill=(40, 90, 80, 255))
    return out


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def main():
    # unit items
    for uid, (color, glyph) in UNITS.items():
        p = os.path.join(A, 'textures/item/module/item_module_%s.png' % uid)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        unit_texture(color, glyph).save(p)
        write_json(os.path.join(A, 'models/item/module/item_module_%s.json' % uid),
                   {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mio_icif:item/module/item_module_%s' % uid}})
    # station
    atlas = station_atlas()
    atlas.save(os.path.join(A, 'textures/block/producer/block_quantum_modification_station.png'))
    write_json(os.path.join(A, 'models/block/producer/block_quantum_modification_station.json'), station_model())
    write_json(os.path.join(A, 'models/item/producer/block_quantum_modification_station.json'),
               {'parent': 'mio_icif:block/producer/block_quantum_modification_station'})
    variants = {}
    for facing, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
        v = {'model': 'mio_icif:block/producer/block_quantum_modification_station'}
        if y:
            v['y'] = y
        variants['facing=%s' % facing] = v
    write_json(os.path.join(A, 'blockstates/producer/block_quantum_modification_station.json'), {'variants': variants})
    gui_texture().save(os.path.join(A, 'textures/gui/gui_quantum_modification_station.png'))

    write_json(os.path.join(D, 'loot_table/blocks/producer/block_quantum_modification_station.json'), {
        'type': 'minecraft:block',
        'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                   'entries': [{'type': 'minecraft:item', 'name': 'mio_icif:producer/block_quantum_modification_station'}], 'rolls': 1.0}],
        'random_sequence': 'mio_icif:blocks/producer/block_quantum_modification_station'})

    # recipes
    write_json(os.path.join(D, 'recipe/suit/block_quantum_modification_station.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['ECE', 'GHG', 'PLP'],
        'key': {'E': {'tag': 'c:circuits/elite'}, 'C': {'item': 'mio_icif:item_tool_ov_scanner'},
                'G': {'item': 'mio_icif:build/block_alloy_glass'}, 'H': {'item': 'mio_icif:producer/block_machine_hull_advanced'},
                'P': {'tag': 'c:plates/iridium'}, 'L': {'item': 'mio_icif:normal/item_lapotron_crystal_lev0'}},
        'result': {'id': 'mio_icif:producer/block_quantum_modification_station', 'count': 1}, 'show_notification': True})
    core = {'type': 'minecraft:crafting_shaped', 'category': 'equipment'}
    signature = {
        'ore_scanner': ('mio_icif:item_tool_ov_scanner', 'minecraft:amethyst_shard'),
        'grid_telemetry': ('mio_icif:item_tool_meter', 'minecraft:redstone'),
        'entity_esp': ('minecraft:spyglass', 'minecraft:ender_eye'),
        'ballistic': ('minecraft:crossbow', 'minecraft:spyglass'),
        'blast_warning': ('minecraft:tnt', 'minecraft:redstone_torch'),
        'behavior_predictor': ('minecraft:sculk_sensor', 'minecraft:ender_eye'),
        'holomap': ('minecraft:map', 'mio_icif:normal/item_diamond_lens'),
        'threat_sensor': ('minecraft:calibrated_sculk_sensor', 'minecraft:ender_eye'),
        'deflector': ('mio_icif:item_electric_force_field_generator', 'mio_icif:normal/item_lapotron_crystal_lev0'),
    }
    for uid, (x, top) in signature.items():
        r = dict(core)
        r['pattern'] = ['PTP', 'CXC', 'PBP']
        r['key'] = {'P': {'tag': 'c:plates/carbon'}, 'T': {'item': top}, 'C': {'tag': 'c:circuits/advanced'},
                    'X': {'item': x}, 'B': {'tag': 'c:plates/iridium'}}
        r['result'] = {'id': 'mio_icif:module/item_module_%s' % uid, 'count': 1}
        write_json(os.path.join(D, 'recipe/suit/item_module_%s.json' % uid), r)

    # tags
    write_json(os.path.join(D, 'tags/item/quantum_suit.json'), {'replace': False, 'values': [
        'mio_icif:armor/item_armor_quantum_helmet', 'mio_icif:armor/item_armor_quantum_chestplate',
        'mio_icif:armor/item_armor_quantum_leggings', 'mio_icif:armor/item_armor_quantum_boots',
        'mio_icif:armor/item_armor_advanced_quantum_chestplate', 'mio_icif:armor/item_armor_heavy_quantum_chestplate',
        'mio_icif:armor/item_armor_hybrid_solar_helmet', 'mio_icif:armor/item_armor_ultimate_solar_helmet']})

    def add_tag(path, entry):
        with open(path, encoding='utf-8') as f:
            tag = json.load(f)
        ids = [v['id'] if isinstance(v, dict) else v for v in tag['values']]
        if entry not in ids:
            tag['values'].append({'id': entry, 'required': False})
            write_json(path, tag)
    for t in ['data/minecraft/tags/block/mineable/pickaxe.json', 'data/c/tags/block/machine.json', 'data/mio_icif/tags/block/producer.json',
              'data/c/tags/block/needs_iron_tool.json']:
        add_tag(os.path.join(RES, t), 'mio_icif:producer/block_quantum_modification_station')
    print('ok')


if __name__ == '__main__':
    main()
