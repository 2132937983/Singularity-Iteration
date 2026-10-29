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
    text = raw.decode('utf-8-sig')
    data = json.loads(text)
    ids = [v if isinstance(v, str) else v.get('id') for v in data.get('values', [])]
    if value in ids:
        return
    # insert as text after the last entry, in the file's own style (one-line or expanded entries), so nothing else changes
    eol = '\r\n' if '\r\n' in text else '\n'
    end = text.rindex(']')
    head = text[:end]          # everything before the closing bracket
    body = head.rstrip()       # up to the end of the last entry
    gap = head[len(body):]     # the line break and indent before the closing bracket
    last_line = body.splitlines()[-1]
    indent = last_line[:len(last_line) - len(last_line.lstrip())]
    if value.startswith('#'):
        entry = '"%s"' % value
    elif '{ "id"' in text or not any(isinstance(v, dict) for v in data.get('values', [])):
        entry = '{ "id": "%s", "required": false }' % value
    else:
        inner = indent + '  '
        entry = '{' + eol + inner + '"id": "%s",' % value + eol + inner + '"required": false' + eol + indent + '}'
    text = body + ',' + eol + indent + entry + gap + text[end:]
    json.loads(text)
    with open(out, 'wb') as f:
        f.write(text.encode('utf-8'))


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


def scanner():
    """4.1 the Scanner's pricing settings: magic mods it never prices, and the estimate policy (same as the code defaults)."""
    write('mio_icif/uu/scanner/excluded_namespaces.json', {
        '_comment': 'Namespaces the Scanner never prices (magic mods). "name*" matches every namespace starting with "name". '
                    'Lists of all packs are merged; "replace": true drops the lists of lower packs.',
        'namespaces': [
            'botania', 'extrabotany', 'mythicbotany', 'ars_nouveau', 'ars_*', 'irons_spellbooks', 'occultism', 'bloodmagic',
            'forbidden_arcanus', 'eidolon', 'malum', 'hexcasting', 'hexerei', 'theurgy', 'evilcraft', 'mahoutsukai', 'goety',
            'naturesaura', 'embers', 'astralsorcery', 'thaumcraft', 'mna', 'wizards_reborn', 'enigmaticlegacy', 'arcanus',
            'iceandfire_spells', 'aetherworks', 'reliquary', 'apotheotic_additions'],
    })
    write('mio_icif/uu/scanner/estimate_policy.json', {
        '_comment': 'Buckets of UU-matter for items no recipe prices. Blocks: block_tiers by the tool they need '
                    '[none, stone, iron, diamond, netherite] x hardness/3 (clamped), ores x ore_factor. Other items: item_base x '
                    'rarity [common, uncommon, rare, epic] x stack (by max stack size) x (1 + durability/500), x2 fire resistant.',
        'item_base': 0.1,
        'rarity': [1, 3, 10, 30],
        'stack': {'64': 1, '16': 2, '1': 5},
        'block_tiers': [0.0002, 0.0011, 0.006, 0.03, 0.3],
        'ore_factor': 3,
        'hardness_min': 0.5,
        'hardness_max': 3,
    })
    print('scanner: excluded namespaces, estimate policy')


def tag_has(path, value):
    out = os.path.join(DATA, path)
    if not os.path.exists(out):
        return False
    with open(out, encoding='utf-8-sig') as f:
        data = json.load(f)
    return value in [v if isinstance(v, str) else v.get('id') for v in data.get('values', [])]


def steam_reactor():
    """4.2 the steam reactor, its chamber and the steam vents: loot, the tags of the nuclear reactor / chamber, recipes."""
    REACTOR, CHAMBER = 'mio_icif:generator/block_steam_reactor', 'mio_icif:reactor/block_steam_reactor_chamber'
    for block, path in ((REACTOR, 'generator/block_steam_reactor'), (CHAMBER, 'reactor/block_steam_reactor_chamber')):
        write('mio_icif/loot_table/blocks/%s.json' % path, {
            'type': 'minecraft:block',
            'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                       'entries': [{'type': 'minecraft:item', 'name': block}], 'rolls': 1.0}],
            'random_sequence': 'mio_icif:blocks/%s' % path})
    # the same block tags as the nuclear reactor and its chamber
    for tag_file in ('minecraft/tags/block/mineable/pickaxe.json', 'c/tags/block/machine.json', 'c/tags/block/needs_stone_tool.json',
                     'mio_icif/tags/block/generator.json', 'mio_icif/tags/block/producer.json', 'mio_icif/tags/block/reactor_heat_immune.json'):
        if tag_has(tag_file, 'mio_icif:generator/block_nuclear_reactor_generator'):
            add_to_tag(tag_file, REACTOR)
        if tag_has(tag_file, 'mio_icif:reactor/block_reactor_chamber'):
            add_to_tag(tag_file, CHAMBER)
    # IC2C recipes on SI items: alloy plate, advanced circuit, blast glass (reinforced glass), reactor, chamber, heat vents
    write('mio_icif/recipe/shaped_steam_reactor.json', {
        'type': 'minecraft:crafting_shaped', 'pattern': ['AXA', 'YYY', 'ACA'],
        'key': {'A': {'tag': 'c:plates/alloy'}, 'X': {'tag': 'c:circuits/advanced'}, 'Y': {'item': CHAMBER},
                'C': {'item': 'mio_icif:generator/block_nuclear_reactor_generator'}},
        'result': {'id': REACTOR, 'count': 1}})
    write('mio_icif/recipe/shaped_steam_reactor_chamber.json', {
        'type': 'minecraft:crafting_shaped', 'pattern': ['XYX', 'YCY', 'XYX'],
        'key': {'X': {'tag': 'c:plates/alloy'}, 'Y': {'item': 'mio_icif:build/block_alloy_glass'},
                'C': {'item': 'mio_icif:reactor/block_reactor_chamber'}},
        'result': {'id': CHAMBER, 'count': 1}})
    R = 'mio_icif:reactor/item_reactor_'
    for steam, heat in (('steam_vent', 'vent'), ('steam_vent_core', 'vent_core'), ('steam_vent_overclocked', 'golden_vent'),
                        ('steam_vent_advanced', 'diamond_vent')):
        write('mio_icif/recipe/shaped_%s.json' % steam, {
            'type': 'minecraft:crafting_shaped', 'pattern': ['I#I', '#X#', 'I#I'],
            'key': {'I': {'tag': 'c:glass_blocks'}, '#': {'item': 'minecraft:iron_bars'}, 'X': {'item': R + heat}},
            'result': {'id': R + steam, 'count': 1}})
    print('steam reactor: 2 loot tables, tags, 6 recipes')


def steam_turbine():
    """4.2 the steam turbine: loot, the block tags of the semifluid generator, a recipe like the kinetic generator's with the
    steam turbine rotor."""
    TURBINE = 'mio_icif:generator/block_steam_turbine'
    write('mio_icif/loot_table/blocks/generator/block_steam_turbine.json', {
        'type': 'minecraft:block',
        'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                   'entries': [{'type': 'minecraft:item', 'name': TURBINE}], 'rolls': 1.0}],
        'random_sequence': 'mio_icif:blocks/generator/block_steam_turbine'})
    for tag_file in ('minecraft/tags/block/mineable/pickaxe.json', 'c/tags/block/machine.json', 'c/tags/block/needs_stone_tool.json',
                     'mio_icif/tags/block/generator.json'):
        if tag_has(tag_file, 'mio_icif:generator/block_semifluid_generator'):
            add_to_tag(tag_file, TURBINE)
    write('mio_icif/recipe/shaped_steam_turbine.json', {
        'type': 'minecraft:crafting_shaped', 'pattern': ['SSS', 'GET', 'SSS'],
        'key': {'S': {'tag': 'c:casings/steel'}, 'G': {'item': 'mio_icif:generator/block_thermal_generator'},
                'E': {'item': 'mio_icif:resource/item_motor'}, 'T': {'item': 'mio_icif:normal/item_steam_turbin'}},
        'result': {'id': TURBINE, 'count': 1}})
    print('steam turbine: loot table, tags, recipe')


if __name__ == '__main__':
    proton()
    iridium()
    scanner()
    steam_reactor()
    steam_turbine()
