#!/usr/bin/env python3
"""
0.1.8.5 data: nano unit recipes, the lattice densifier, the new (nano-tier) recipe of the
Universal Suit Modification Station, the nano_suit item tag and the emp_sensitive entity tag.

Recipe design: every Mk.I nano unit has one Nano Living Metal at its centre (the lattice that
grows into the suit), carbon composite plates (the nano suit material), an advanced circuit and
the materials of its family: elastic and agile (slime, rabbit foot) for the myofiber, heat
conductor and vents for the membrane, a lightning rod and an energy crystal for the EMP,
refracting crystals and an invisibility catalyst for the camouflage. The toxin filter (a shared
1 LP unit) is cheap: activated charcoal and honeycomb. The station drops from the quantum tier
(elite circuits, lapotron, iridium) to the nano tier (advanced circuits, energy crystal, carbon
plates): nano players need it first, and the quantum units keep their own iridium costs.

Usage: data_185.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
DATA = os.path.join(ROOT, 'src/main/resources/data')
REC = os.path.join(DATA, 'mio_icif/recipe/suit')

T = lambda t: {'tag': t}       # noqa: E731
I = lambda i: {'item': i}      # noqa: E731

CARBON = T('c:plates/carbon')
ADV = T('c:circuits/advanced')
NANO = T('mio_icif:nano_living_metals')
CRYSTAL = I('mio_icif:normal/item_crystal_lev0')


def shaped(result, pattern, key, count=1, category='equipment'):
    return {'type': 'minecraft:crafting_shaped', 'category': category, 'pattern': pattern, 'key': key,
            'result': {'id': result, 'count': count}}


RECIPES = {
    'item_module_myofiber': shaped('mio_icif:module/item_module_myofiber', ['PFP', 'SMS', 'PCP'],
                                   {'P': CARBON, 'F': I('minecraft:rabbit_foot'), 'S': I('minecraft:slime_ball'), 'M': NANO, 'C': ADV}),
    'item_module_thermoregulator': shaped('mio_icif:module/item_module_thermoregulator', ['PHP', 'VMV', 'PCP'],
                                          {'P': CARBON, 'H': I('mio_icif:resource/item_heatconductor'), 'V': I('mio_icif:reactor/item_reactor_vent'),
                                           'M': NANO, 'C': ADV}),
    'item_module_emp_emitter': shaped('mio_icif:module/item_module_emp_emitter', ['PRP', 'CMC', 'PEP'],
                                      {'P': CARBON, 'R': I('minecraft:lightning_rod'), 'C': ADV, 'M': NANO, 'E': CRYSTAL}),
    'item_module_optical_camo': shaped('mio_icif:module/item_module_optical_camo', ['QGQ', 'CMC', 'PIP'],
                                       {'Q': I('minecraft:prismarine_crystals'), 'G': I('minecraft:tinted_glass'), 'C': ADV, 'M': NANO,
                                        'P': CARBON, 'I': I('minecraft:fermented_spider_eye')}),
    'item_module_toxin_filter': shaped('mio_icif:module/item_module_toxin_filter', ['PKP', 'HCH', 'PKP'],
                                       {'P': CARBON, 'K': I('minecraft:charcoal'), 'H': I('minecraft:honeycomb'), 'C': ADV}),
    'item_module_micro_capacitor': shaped('mio_icif:module/item_module_micro_capacitor', ['PRP', 'ECE', 'PMP'],
                                          {'P': CARBON, 'R': I('minecraft:redstone_block'), 'E': CRYSTAL, 'C': ADV, 'M': NANO}),
    'item_nano_lattice_densifier': shaped('mio_icif:module/item_nano_lattice_densifier', ['MIM', 'MEM'],
                                          {'M': NANO, 'I': T('c:plates/iridium'), 'E': CRYSTAL}),
    'block_quantum_modification_station': shaped('mio_icif:producer/block_quantum_modification_station', ['ACA', 'GHG', 'PEP'],
                                                 {'A': ADV, 'C': I('mio_icif:item_tool_ov_scanner'), 'G': I('mio_icif:build/block_alloy_glass'),
                                                  'H': I('mio_icif:producer/block_machine_hull_advanced'), 'P': CARBON, 'E': CRYSTAL},
                                                 category='misc'),
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def main():
    for name, r in RECIPES.items():
        if name == 'block_quantum_modification_station':
            r['show_notification'] = True
        write(os.path.join(REC, name + '.json'), r)
    write(os.path.join(DATA, 'mio_icif/tags/item/nano_suit.json'), {'values': [
        'mio_icif:armor/item_armor_nano_helmet', 'mio_icif:armor/item_armor_nano_chestplate',
        'mio_icif:armor/item_armor_nano_leggings', 'mio_icif:armor/item_armor_nano_boots']})
    write(os.path.join(DATA, 'mio_icif/tags/entity_type/emp_sensitive.json'), {'values': [
        'minecraft:creeper', 'minecraft:iron_golem', 'minecraft:snow_golem', 'minecraft:vex', 'minecraft:blaze',
        {'id': 'minecraft:breeze', 'required': False}, 'minecraft:shulker', 'minecraft:guardian']})
    print('wrote', len(RECIPES), 'recipes and 2 tags')


if __name__ == '__main__':
    main()
