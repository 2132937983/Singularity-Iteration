#!/usr/bin/env python3
"""0.1.10.36 data files (recipes, recipe advancements, loot tables, tags).
- 3.2 proton fuel rods: a full uranium rod becomes a proton rod in the Neutron Polymerizer; dual / quad rods as the uranium
  ones; depleted proton rods go back to uranium-238 and iron dust in the thermal centrifuge.
- 3.1 deepslate iridium ore (only made by nuclear blasts): loot (small iridium dust, fortune), 9 small dusts -> iridium shard
  (crafting, compressor), macerator ore -> 2 small dusts, pickaxe / diamond tool / c:ores tags, nuke_transmutable tag.
Idempotent: writes the same files every run.
Usage: data_1136.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
DATA = os.path.join(ROOT, 'src/main/resources/data')


def write(path, obj, eol='\n'):
    out = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, 'w', encoding='utf-8', newline=eol) as f:
        f.write(json.dumps(obj, indent=2, ensure_ascii=False) + '\n')


def unlock(folder, name, item):
    """The recipe-book advancement: unlocks mio_icif:<folder>/<name> once the player holds item."""
    has = 'has_' + item.split(':', 1)[1]
    write('mio_icif/advancement/recipes/%s/%s.json' % (folder, name), {
        'parent': 'minecraft:recipes/root',
        'criteria': {
            has: {'conditions': {'items': [{'items': item}]}, 'trigger': 'minecraft:inventory_changed'},
            'has_the_recipe': {'conditions': {'recipe': 'mio_icif:%s/%s' % (folder, name)}, 'trigger': 'minecraft:recipe_unlocked'},
        },
        'requirements': [['has_the_recipe'], [has]],
        'rewards': {'recipes': ['mio_icif:%s/%s' % (folder, name)]},
    })


def proton():
    R = 'mio_icif:reactor/item_reactor_'
    # only a full uranium rod: a part-used one would become a fresh proton rod
    write('mio_icif/recipe/neutron_polymerizer/uranium_rod_to_proton_rod.json', {
        'type': 'mio_icif:neutron_polymerizer',
        'ingredient': {'type': 'neoforge:components', 'items': R + 'uranium_simple',
                       'components': {'mio_icif:fuel_rod_durability': {'remaining_uses': 20000, 'max_uses': 20000, 'tick_counter': 0}},
                       'strict': False},
        'result': {'id': R + 'proton_simple', 'count': 1},
        'processingtime': 4000,
        'energyPerTick': 2048,
    })
    write('mio_icif/recipe/shaped_proton_dual.json', {
        'type': 'minecraft:crafting_shaped', 'pattern': ['UIU'],
        'key': {'U': {'item': R + 'proton_simple'}, 'I': {'tag': 'c:plates/iron'}},
        'result': {'id': R + 'proton_dual', 'count': 1},
    })
    write('mio_icif/recipe/shaped_proton_quad.json', {
        'type': 'minecraft:crafting_shaped', 'pattern': ['UIU', 'CIC', 'UIU'],
        'key': {'U': {'item': R + 'proton_simple'}, 'I': {'tag': 'c:plates/iron'}, 'C': {'tag': 'c:plates/copper'}},
        'result': {'id': R + 'proton_quad', 'count': 1},
    })
    for size, (u238, iron) in {'simple': (4, 1), 'dual': (8, 3), 'quad': (16, 7)}.items():
        name = 'proton_%s_depleted' % size
        write('mio_icif/recipe/centrifuge/%s.json' % name, {
            'type': 'mio_icif:centrifuge',
            'ingredient': {'item': R + name},
            'minheat': 4000,
            'primaryCount': u238, 'primaryResult': {'count': 1, 'id': 'mio_icif:resource/item_uran_238'},
            'secondaryCount': iron, 'secondaryResult': {'count': 1, 'id': 'mio_icif:resource/item_iron_dust'},
        })
        unlock('centrifuge', name, R + name)
    print('proton: 6 recipes, 3 advancements')


def tag(path, values):
    """A shared tag file: every entry optional (required: false), merged with other packs."""
    write(path, {'replace': False, 'values': [v if v.startswith('#') else {'id': v, 'required': False} for v in values]})


def add_to_tag(path, value):
    """Adds value to an existing tag file (kept as written; plain or {id, required} entries)."""
    out = os.path.join(DATA, path)
    if not os.path.exists(out):
        tag(path, [value])
        return
    raw = open(out, 'rb').read()
    data = json.loads(raw.decode('utf-8-sig'))
    ids = [v if isinstance(v, str) else v.get('id') for v in data.get('values', [])]
    if value not in ids:
        data.setdefault('values', []).append(value if value.startswith('#') else {'id': value, 'required': False})
        write(path, data, '\r\n' if b'\r\n' in raw else '\n')


def iridium():
    ORE = 'mio_icif:block_ore_iridium_in_deep'
    DUST = 'mio_icif:resource/item_iridium_dust_small'
    SHARD = 'mio_icif:resource/item_shard_iridium'
    # loot: silk touch keeps the ore; otherwise one small iridium dust, more with fortune
    write('mio_icif/loot_table/blocks/block_ore_iridium_in_deep.json', {
        'type': 'minecraft:block',
        'pools': [{'bonus_rolls': 0.0, 'rolls': 1.0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': ORE, 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {
                'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]},
            {'type': 'minecraft:item', 'name': DUST, 'functions': [
                {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
                {'function': 'minecraft:explosion_decay'}]}]}]}],
        'random_sequence': 'mio_icif:blocks/block_ore_iridium_in_deep',
    })
    # 9 small dusts -> 1 iridium shard (crafting and compressor); the macerator gives 2 small dusts per ore
    write('mio_icif/recipe/shaped_iridium_shard_from_small_dust.json', {
        'type': 'minecraft:crafting_shaped', 'pattern': ['XXX', 'XXX', 'XXX'],
        'key': {'X': {'tag': 'c:dusts/small_iridium'}}, 'result': {'id': SHARD, 'count': 1}})
    write('mio_icif/recipe/compressor/iridium_dust_small_to_shard.json', {
        'type': 'mio_icif:compressor', 'count': 9, 'energypertick': 5, 'ingredient': {'tag': 'c:dusts/small_iridium'},
        'processingtime': 100, 'result': {'count': 1, 'id': SHARD}})
    unlock('compressor', 'iridium_dust_small_to_shard', DUST)
    write('mio_icif/recipe/powder/deepslate_iridium_ore_to_small_dust.json', {
        'type': 'mio_icif:powder', 'count': 1, 'ingredient': {'item': ORE}, 'result': {'count': 2, 'id': DUST}})
    unlock('powder', 'deepslate_iridium_ore_to_small_dust', ORE)
    # tags
    add_to_tag('minecraft/tags/block/mineable/pickaxe.json', ORE)
    add_to_tag('minecraft/tags/block/needs_diamond_tool.json', ORE)
    add_to_tag('c/tags/block/ores.json', ORE)
    add_to_tag('c/tags/block/ores_in_ground/deepslate.json', ORE)
    tag('c/tags/block/ores/iridium.json', [ORE])
    add_to_tag('c/tags/item/ores.json', ORE)
    tag('c/tags/item/ores/iridium.json', [ORE])
    tag('c/tags/item/dusts/small_iridium.json', [DUST])
    tag('c/tags/item/tiny_dusts/iridium.json', [DUST])
    # the deepslate ores a nuclear blast turns into iridium ore (the code skips the iridium ore itself)
    write('mio_icif/tags/block/nuke_transmutable.json', {'replace': False, 'values': ['#c:ores_in_ground/deepslate']})
    print('iridium: loot table, 3 recipes, 2 advancements, tags')


if __name__ == '__main__':
    proton()
    iridium()
