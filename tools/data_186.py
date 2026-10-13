#!/usr/bin/env python3
"""
0.1.8.6 data: item models and recipes of the 11 new nano units and of the 17 Mk.II units.

Mk.I: carbon plates, an advanced circuit and the family materials; the 2 LP units also have a Nano
Living Metal at their centre, the light 1 LP units do not.
Mk.II: the Mk.I unit at the centre, a Living Circuit, four neutron plates (densified nano metal) and
three family materials. Mk.II is end-game by design (living circuit).

Usage: data_186.py <repo root>
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
CARBON = T('c:plates/carbon')
ADV = T('c:circuits/advanced')
NANO = T('mio_icif:nano_living_metals')


def shaped(result, pattern, key):
    return {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern, 'key': key,
            'result': {'id': result, 'count': 1}}


def unit(uid):
    return 'mio_icif:module/item_module_' + uid


MK1 = {
    'gecko_setae': (['PEP', 'SMS', 'PCP'], {'P': CARBON, 'E': I('minecraft:spider_eye'), 'S': I('minecraft:string'), 'M': NANO, 'C': ADV}),
    'shock_gel': (['PBP', 'GCG'], {'P': CARBON, 'B': I('minecraft:slime_block'), 'G': I('minecraft:slime_ball'), 'C': ADV}),
    'heat_sink_fins': (['VHV', 'PCP'], {'V': I('mio_icif:reactor/item_reactor_vent'), 'H': I('mio_icif:resource/item_heatconductor'),
                                        'P': CARBON, 'C': ADV}),
    'thermal_camo': (['PIP', 'FMF', 'PCP'], {'P': CARBON, 'I': I('minecraft:blue_ice'), 'F': I('minecraft:phantom_membrane'), 'M': NANO, 'C': ADV}),
    'jammer': (['PEP', 'AMA', 'PRP'], {'P': CARBON, 'E': I('minecraft:amethyst_shard'), 'A': ADV, 'M': NANO, 'R': I('minecraft:comparator')}),
    'recon_cloud': (['PEP', 'SMS', 'PCP'], {'P': CARBON, 'E': I('minecraft:ender_eye'), 'S': I('minecraft:glow_ink_sac'), 'M': NANO, 'C': ADV}),
    'kinetic_mesh': (['PAP', 'AMA', 'PCP'], {'P': CARBON, 'A': T('c:plates/alloy'), 'M': NANO, 'C': ADV}),
    'self_repair': (['PGP', 'GCG', 'PGP'], {'P': CARBON, 'G': I('minecraft:glistering_melon_slice'), 'C': ADV}),
    'hydrophobic': (['PKP', 'KCK', 'PKP'], {'P': CARBON, 'K': I('minecraft:prismarine_shard'), 'C': ADV}),
    'kinetic_generator': (['PIP', 'RCR', 'PIP'], {'P': CARBON, 'I': I('minecraft:piston'), 'R': I('minecraft:copper_ingot'), 'C': ADV}),
    'solar_skin': (['DDD', 'PCP'], {'D': I('minecraft:daylight_detector'), 'P': CARBON, 'C': ADV}),
}

FAMILY = {
    'myofiber': I('minecraft:rabbit_foot'), 'thermoregulator': I('mio_icif:reactor/item_reactor_vent'),
    'emp_emitter': I('minecraft:lightning_rod'), 'optical_camo': I('minecraft:prismarine_crystals'),
    'toxin_filter': I('minecraft:honeycomb'), 'micro_capacitor': I('mio_icif:normal/item_crystal_lev0'),
    'gecko_setae': I('minecraft:spider_eye'), 'shock_gel': I('minecraft:slime_block'),
    'heat_sink_fins': I('mio_icif:resource/item_heatconductor'), 'thermal_camo': I('minecraft:phantom_membrane'),
    'jammer': I('minecraft:amethyst_shard'), 'recon_cloud': I('minecraft:ender_eye'), 'kinetic_mesh': T('c:plates/iridium'),
    'self_repair': I('minecraft:glistering_melon_slice'), 'hydrophobic': I('minecraft:nautilus_shell'),
    'kinetic_generator': I('minecraft:piston'), 'solar_skin': I('minecraft:daylight_detector'),
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def model(name):
    write(os.path.join(MODELS, name + '.json'), {'parent': 'minecraft:item/generated',
                                                  'textures': {'layer0': 'mio_icif:item/module/' + name}})


def main():
    for uid, (pattern, key) in MK1.items():
        write(os.path.join(REC, 'item_module_' + uid + '.json'), shaped(unit(uid), pattern, key))
        model('item_module_' + uid)
    for uid, fam in FAMILY.items():
        write(os.path.join(REC, 'item_module_' + uid + '_mk2.json'), shaped(unit(uid) + '_mk2', ['NAN', 'AUA', 'NLN'], {
            'N': T('c:plates/neutron'), 'A': fam, 'U': I(unit(uid)), 'L': I('mio_icif:normal/item_living_circuit')}))
        model('item_module_' + uid + '_mk2')
    print('wrote', len(MK1), 'Mk.I and', len(FAMILY), 'Mk.II recipes and models')


if __name__ == '__main__':
    main()
