#!/usr/bin/env python3
"""
0.1.9.0 data: frame cores and the retirement of the solar helmets and quantum chest variants.

  * item models of the five cores (module/item_core_<id>)
  * core recipes (the old helmet / chestplate recipes with a core cartridge in place of the suit piece)
      Solar Array I     advanced solar panel, carbon plate, advanced circuits, gold cables, LV-MV transformer
      Solar Array II    hybrid solar panel, iridium plate, advanced circuits, glass cables, HV-EV transformer
      Solar Array III   ultimate hybrid solar panel, iridium plate, ... ; or Solar Array II + ultimate hybrid panel
      Graviton Bulwark  alloy, lodestone, iridium, lapotron crystal, elite circuit (old heavy quantum chestplate)
      Adv. Propulsion   super iridium, super lapotron, life support, elite circuit, electric jetpack and a
                        Graviton Bulwark core (old advanced quantum chestplate, which consumed the heavy one)
  * the one-item conversion recipe of the legacy shells
  * the seven recipes of the retired items are deleted
  * tags: the legacy ids leave mio_icif:quantum_suit and mio_icif:enchantable/electric, and join
    c:hidden_from_recipe_viewers

Usage: data_190.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
RES = os.path.join(ROOT, 'src/main/resources')
REC = os.path.join(RES, 'data/mio_icif/recipe/suit')
MODELS = os.path.join(RES, 'assets/mio_icif/models/item/module')

T = lambda t: {'tag': t}       # noqa: E731
I = lambda i: {'item': i}      # noqa: E731

CORES = ['solar_array_1', 'solar_array_2', 'solar_array_3', 'graviton', 'propulsion']
LEGACY = [
    'mio_icif:normal/item_solar_helmet',
    'mio_icif:armor/item_armor_advanced_solar_helmet',
    'mio_icif:armor/item_armor_hybrid_solar_helmet',
    'mio_icif:armor/item_armor_ultimate_solar_helmet',
    'mio_icif:armor/item_armor_advanced_quantum_chestplate',
    'mio_icif:armor/item_armor_heavy_quantum_chestplate',
]
OLD_RECIPES = [
    'shaped_294_solar_helmet', 'shaped_advanced_solar_helmet', 'shaped_hybrid_solar_helmet',
    'shaped_ultimate_solar_helmet', 'shapeless_ultimate_solar_helmet_upgrade',
    'shaped_advanced_quantum_chestplate', 'shaped_heavy_quantum_chestplate',
]


def core(cid):
    return 'mio_icif:module/item_core_' + cid


def shaped(result, pattern, key):
    return {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern, 'key': key,
            'result': {'id': result, 'count': 1}}


RECIPES = {
    'item_core_solar_array_1': shaped(core('solar_array_1'), [' S ', 'CPC', 'GTG'], {
        'S': I('mio_icif:generator/block_advanced_solar_panel'), 'C': T('c:circuits/advanced'), 'P': T('c:plates/carbon'),
        'G': I('mio_icif:wiring/cable/block_gold_cable_1'), 'T': I('mio_icif:wiring/transformer_lv_mv')}),
    'item_core_solar_array_2': shaped(core('solar_array_2'), [' S ', 'CPC', 'GTG'], {
        'S': I('mio_icif:generator/block_hybrid_solar_panel'), 'C': T('c:circuits/advanced'), 'P': T('c:plates/iridium'),
        'G': I('mio_icif:wiring/cable/block_glass_cable'), 'T': I('mio_icif:wiring/transformer_hv_ev')}),
    'item_core_solar_array_3': shaped(core('solar_array_3'), [' S ', 'CPC', 'GTG'], {
        'S': I('mio_icif:generator/block_ultimate_hybrid_solar_panel'), 'C': T('c:circuits/advanced'), 'P': T('c:plates/iridium'),
        'G': I('mio_icif:wiring/cable/block_glass_cable'), 'T': I('mio_icif:wiring/transformer_hv_ev')}),
    'item_core_solar_array_3_upgrade': {
        'type': 'minecraft:crafting_shapeless', 'category': 'equipment',
        'ingredients': [I(core('solar_array_2')), I('mio_icif:generator/block_ultimate_hybrid_solar_panel')],
        'result': {'id': core('solar_array_3'), 'count': 1}},
    'item_core_graviton': shaped(core('graviton'), ['SLS', 'IBI', 'ICI'], {
        'S': T('c:ingots/alloy'), 'L': I('minecraft:lodestone'), 'I': I('mio_icif:resource/item_iridium'),
        'B': I('mio_icif:normal/item_lapotron_crystal_lev0'), 'C': T('c:circuits/elite')}),
    'item_core_propulsion': shaped(core('propulsion'), ['SBS', 'RCD', 'SAS'], {
        'S': T('c:dense_plates/super_iridium'), 'B': I('mio_icif:normal/item_super_lapotron_crystal'),
        'R': I('mio_icif:item_electric_first_aid_life_support'), 'C': T('c:circuits/elite'),
        'D': I('mio_icif:armor/item_armor_jetpack_electric'), 'A': I(core('graviton'))}),
    'legacy_armor_conversion': {'type': 'mio_icif:legacy_armor_conversion', 'category': 'equipment'},
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def edit_tag(path, drop=(), add=()):
    with open(path, encoding='utf-8') as f:
        tag = json.load(f)
    values = [v for v in tag['values'] if (v if isinstance(v, str) else v.get('id')) not in drop]
    for a in add:
        if a not in values:
            values.append(a)
    tag['values'] = values
    write(path, tag)


def main():
    for cid in CORES:
        write(os.path.join(MODELS, 'item_core_' + cid + '.json'), {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mio_icif:item/module/item_core_' + cid}})
    for name, recipe in RECIPES.items():
        write(os.path.join(REC, name + '.json'), recipe)
    removed = 0
    for name in OLD_RECIPES:
        p = os.path.join(RES, 'data/mio_icif/recipe', name + '.json')
        if os.path.exists(p):
            os.remove(p)
            removed += 1
    tags = os.path.join(RES, 'data/mio_icif/tags/item')
    edit_tag(os.path.join(tags, 'quantum_suit.json'), drop=LEGACY)
    edit_tag(os.path.join(tags, 'enchantable/electric.json'), drop=LEGACY)
    hidden = os.path.join(RES, 'data/c/tags/item/hidden_from_recipe_viewers.json')
    if os.path.exists(hidden):
        edit_tag(hidden, add=LEGACY)
    else:
        write(hidden, {'replace': False, 'values': LEGACY})
    print('wrote', len(CORES), 'core models,', len(RECIPES), 'recipes; removed', removed, 'old recipes; tags updated')


if __name__ == '__main__':
    main()
