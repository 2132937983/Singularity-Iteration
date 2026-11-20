#!/usr/bin/env python3
"""0.1.11.98: the data and assets of the silver ore (stone and deepslate), the silver block and the raw silver block, after
lead's: blockstates, models, loot tables, recipes, ore generation (overworld and Twilight Forest), tags, the ore advancement,
the UU default and the names in en_us, zh_cn, ja_jp and ru_ru. Rarer than lead, about as rare as gold: one vein of 9 a chunk
in stone (y 0-32) and one in deepslate (y -64-0). The textures come from tools/silver_blocks_11598.py.

Writes new files as they should be; adds missing entries to the shared tag, advancement, UU and lang files (keeping their
line ends). Idempotent.

Usage: silver_data_11598.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
RES = os.path.join(ROOT, 'src/main/resources')
ASSETS = os.path.join(RES, 'assets/mio_icif')
DATA = os.path.join(RES, 'data')
NS = 'mio_icif'
ORE, DEEP, BLOCK, RAW = 'block_ore_silver', 'block_ore_silver_in_deep', 'block_silver', 'block_raw_silver'
INGOT, RAW_ITEM = 'mio_icif:resource/item_silver_ingot', 'mio_icif:resource/item_raw_silver_ore'
written = []


def put(path, obj):
    """A whole file this tool owns (LF, 2 spaces)."""
    full = os.path.join(RES, path)
    text = json.dumps(obj, indent=2, ensure_ascii=False) + '\n'
    os.makedirs(os.path.dirname(full), exist_ok=True)
    old = open(full, encoding='utf-8').read() if os.path.exists(full) else None
    if old != text:
        open(full, 'w', encoding='utf-8', newline='\n').write(text)
        written.append(path)


def edit_json(path, change):
    """A shared JSON file: {@code change(obj)} returns True when it changed it; keeps the file's line ends."""
    full = os.path.join(RES, path)
    raw = open(full, 'rb').read().decode('utf-8') if os.path.exists(full) else None
    obj = json.loads(raw) if raw else {'replace': False, 'values': []}
    if not change(obj) and raw is not None:
        return
    eol = '\r\n' if raw and '\r\n' in raw else '\n'
    text = json.dumps(obj, indent=2, ensure_ascii=False) + '\n'
    os.makedirs(os.path.dirname(full), exist_ok=True)
    open(full, 'wb').write(text.replace('\n', eol).encode('utf-8'))
    written.append(path)


def tag(path, *ids):
    """Adds ids (a block or item id, or #tag) to a tag file, each once."""
    def change(obj):
        values = obj.setdefault('values', [])
        have = {v['id'] if isinstance(v, dict) else v for v in values}
        added = False
        for i in ids:
            if i not in have:
                values.append({'id': i, 'required': False})
                added = True
        return added
    edit_json(path, change)


def block_assets(name):
    put('assets/mio_icif/blockstates/%s.json' % name, {'variants': {'': {'model': 'mio_icif:block/' + name}}})
    put('assets/mio_icif/models/block/%s.json' % name, {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'mio_icif:block/' + name}})
    put('assets/mio_icif/models/item/%s.json' % name, {'parent': 'mio_icif:block/' + name})


def ore_loot(name):
    put('data/mio_icif/loot_table/blocks/%s.json' % name, {
        'type': 'minecraft:block',
        'pools': [{'bonus_rolls': 0.0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {'predicates': {
                'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}],
             'name': 'mio_icif:' + name},
            {'type': 'minecraft:item', 'functions': [
                {'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops', 'function': 'minecraft:apply_bonus'},
                {'function': 'minecraft:explosion_decay'}], 'name': RAW_ITEM}]}], 'rolls': 1.0}],
        'random_sequence': 'mio_icif:blocks/' + name})


def self_loot(name):
    put('data/mio_icif/loot_table/blocks/%s.json' % name, {
        'type': 'minecraft:block',
        'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                   'entries': [{'type': 'minecraft:item', 'name': 'mio_icif:' + name}], 'rolls': 1.0}],
        'random_sequence': 'mio_icif:blocks/' + name})


def recipes():
    r = 'data/mio_icif/recipe/'
    put(r + 'shaped_silver_block.json', {'type': 'minecraft:crafting_shaped', 'pattern': ['MMM', 'MMM', 'MMM'],
        'key': {'M': {'tag': 'c:ingots/silver'}}, 'result': {'id': 'mio_icif:' + BLOCK, 'count': 1}})
    put(r + 'shaped_silver_block_to_ingot.json', {'type': 'minecraft:crafting_shaped', 'pattern': ['B'],
        'key': {'B': {'item': 'mio_icif:' + BLOCK}}, 'result': {'id': INGOT, 'count': 9}})
    put(r + 'compressor/silver_ingot_to_block.json', {'type': 'mio_icif:compressor', 'count': 9, 'energypertick': 20,
        'ingredient': {'tag': 'c:ingots/silver'}, 'processingtime': 400, 'result': {'count': 1, 'id': 'mio_icif:' + BLOCK}})
    put(r + 'block_raw_silver.json', {'type': 'minecraft:crafting_shaped', 'pattern': ['###', '###', '###'],
        'key': {'#': {'tag': 'c:raw_materials/silver'}}, 'result': {'id': 'mio_icif:' + RAW, 'count': 1}})
    put(r + 'block_raw_silver_uncraft.json', {'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'tag': 'c:storage_blocks/raw_silver'}], 'result': {'id': RAW_ITEM, 'count': 9}})
    for name, file in ((ORE, 'smelting_silver_ore'), (DEEP, 'smelting_silver_ore_deep')):
        put(r + file + '.json', {'type': 'minecraft:smelting', 'cookingtime': 200, 'experience': 1.0,
            'ingredient': {'item': 'mio_icif:' + name}, 'result': {'id': INGOT}})
    # the macerator's powder/silver_ore_to_silver_ore_crushed (c:ores/silver) takes both ores already


def worldgen():
    w = 'data/mio_icif/worldgen/'
    for feature, block, ground in (('silver_ore', ORE, 'minecraft:stone'), ('silver_ore_deep', DEEP, 'minecraft:deepslate')):
        put(w + 'configured_feature/%s.json' % feature, {'type': 'minecraft:ore', 'config': {
            'discard_chance_on_air_exposure': 0.0, 'size': 9,
            'targets': [{'state': {'Name': 'mio_icif:' + block}, 'target': {'block': ground, 'predicate_type': 'minecraft:block_match'}}]}})

    def placed(name, feature, count, low, high, biomes=None):
        biome = {'type': 'minecraft:biome'}
        if biomes:
            biome['biomes'] = biomes
        put(w + 'placed_feature/%s.json' % name, {'feature': 'mio_icif:' + feature, 'placement': [
            {'type': 'minecraft:count', 'count': count}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform',
                'max_inclusive': {'absolute': high}, 'min_inclusive': {'absolute': low}}}, biome]})

    tf = '#twilightforest:in_twilight_forest'
    placed('silver_ore_placed', 'silver_ore', 1, 0, 32)
    placed('silver_ore_deep_placed', 'silver_ore_deep', 1, -64, 0)
    placed('silver_ore_twilight_forest_placed', 'silver_ore', 1, -32, 32, tf)
    placed('silver_ore_deep_twilight_forest_placed', 'silver_ore_deep', 1, -64, -32, tf)
    m = 'data/mio_icif/neoforge/biome_modifier/'
    for file, biomes, feature in (('silver_ore_overworld', '#minecraft:is_overworld', 'silver_ore_placed'),
                                  ('silver_ore_deep_overworld', '#minecraft:is_overworld', 'silver_ore_deep_placed'),
                                  ('silver_ore_twilight_forest', tf, 'silver_ore_twilight_forest_placed'),
                                  ('silver_ore_deep_twilight_forest', tf, 'silver_ore_deep_twilight_forest_placed')):
        put(m + file + '.json', {'type': 'neoforge:add_features', 'biomes': biomes, 'features': 'mio_icif:' + feature,
                                 'step': 'underground_ores'})


def tags():
    b, i = 'data/c/tags/block/', 'data/c/tags/item/'
    ore, deep, block, raw = ('mio_icif:' + n for n in (ORE, DEEP, BLOCK, RAW))
    for root in (b, i):
        tag(root + 'ores/silver.json', ore, deep)
        tag(root + 'ores.json', ore, deep, '#c:ores/silver')
        tag(root + 'storage_blocks/silver.json', block)
        tag(root + 'storage_blocks/raw_silver.json', raw)
        tag(root + 'storage_blocks.json', block, raw, '#c:storage_blocks/silver', '#c:storage_blocks/raw_silver')
    tag(b + 'ores_in_ground/stone.json', ore)
    tag(b + 'ores_in_ground/deepslate.json', deep)
    tag('data/minecraft/tags/block/mineable/pickaxe.json', ore, deep, block, raw)
    tag('data/minecraft/tags/block/needs_iron_tool.json', ore, deep, block, raw)


def advancement_and_uu():
    def ore_advancement(obj):
        """The silver ores after the lead ones in the 'mined a mod ore' advancement."""
        items = obj['criteria']['ore']['conditions']['items'][0]['items']
        if 'mio_icif:' + ORE in items and 'mio_icif:' + DEEP in items:
            return False
        at = items.index('mio_icif:block_ore_lead_in_deep') + 1
        for n in ('mio_icif:' + ORE, 'mio_icif:' + DEEP):
            if n not in items:
                items.insert(at, n)
            at = items.index(n) + 1
        return True
    edit_json('data/mio_icif/advancement/singularity_ore.json', ore_advancement)
    edit_uu()


def edit_uu():
    """The UU defaults keep one entry per line: a line is added before raw silver's, as the file is laid out."""
    path = os.path.join(DATA, 'mio_icif/uu_replication/defaults.json')
    raw = open(path, 'rb').read().decode('utf-8')
    if '"mio_icif:%s"' % DEEP in raw:
        return
    eol = '\r\n' if '\r\n' in raw else '\n'
    lines = raw.replace('\r\n', '\n').split('\n')
    at = next(k for k, line in enumerate(lines) if '"%s"' % RAW_ITEM in line)
    indent = lines[at][:len(lines[at]) - len(lines[at].lstrip())]
    lines.insert(at, indent + '{ "item": "mio_icif:%s", "uu": 47.6 },' % DEEP)
    text = '\n'.join(lines)
    json.loads(text)
    open(path, 'wb').write(text.replace('\n', eol).encode('utf-8'))
    written.append('data/mio_icif/uu_replication/defaults.json')


# key, en, zh, ja, ru
LANG = [
    ('block.mio_icif.' + ORE, 'Silver Ore', '银矿石', '銀鉱石', 'Серебряная руда'),
    ('block.mio_icif.' + DEEP, 'Deepslate Silver Ore', '深层银矿石', '深層岩銀鉱石', 'Глубинносланцевая серебряная руда'),
    ('block.mio_icif.' + BLOCK, 'Silver Block', '银块', '銀ブロック', 'Серебряный блок'),
    ('block.mio_icif.' + RAW, 'Raw Silver Block', '粗银块', '銀の原石ブロック', 'Блок необработанного серебра'),
]


def lang():
    for code, col in (('en_us', 1), ('zh_cn', 2), ('ja_jp', 3), ('ru_ru', 4)):
        path = os.path.join(ASSETS, 'lang', code + '.json')
        raw = open(path, 'rb').read().decode('utf-8')
        eol = '\r\n' if '\r\n' in raw else '\n'
        lines = raw.replace('\r\n', '\n').split('\n')
        index = {}
        for k, line in enumerate(lines):
            s = line.strip()
            if s.startswith('"') and '":' in s:
                index[s[1:s.index('"', 1)]] = k
        anchor = index['block.mio_icif.block_raw_lead']
        changed = False
        for entry in LANG:
            key, text = entry[0], entry[col]
            line = '  %s: %s,' % (json.dumps(key), json.dumps(text, ensure_ascii=False))
            if key in index:
                if lines[index[key]].rstrip(',') != line.rstrip(','):
                    lines[index[key]] = line if lines[index[key]].rstrip().endswith(',') else line.rstrip(',')
                    changed = True
                anchor = index[key]
            else:
                lines.insert(anchor + 1, line)
                index = {k: (v + 1 if v > anchor else v) for k, v in index.items()}
                anchor += 1
                index[key] = anchor
                changed = True
        if changed:
            text = '\n'.join(lines)
            json.loads(text)
            open(path, 'wb').write(text.replace('\n', eol).encode('utf-8'))
            written.append('lang/' + code)


def main():
    for name in (ORE, DEEP, BLOCK, RAW):
        block_assets(name)
    ore_loot(ORE)
    ore_loot(DEEP)
    self_loot(BLOCK)
    self_loot(RAW)
    recipes()
    worldgen()
    tags()
    advancement_and_uu()
    lang()
    print('files written:', len(written))
    for w in written:
        print(' ', w)


if __name__ == '__main__':
    main()
