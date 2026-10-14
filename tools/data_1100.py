#!/usr/bin/env python3
"""
0.1.10.0 data: recipes and item models of the quantum action units, the quantum bay expander and
the four quantum Mk.II items.

Recipe design: the quantum units keep the quantum tier of the older units (iridium plates, elite
circuits, a lapotron crystal or a strong vanilla catalyst in the middle), with the theme of the
action: an eye of ender and an echo shard for the blink, phantom membrane for the phase shift,
energy crystals for the coherence capacitor, a recovery compass (it points through space) for the
entanglement relay. The bay expander packs a shulker shell (more room) between lapotrons.
The Mk.II items follow the nano Mk.II pattern: the Mk.I unit in the middle, neutron plates, a
living circuit and the theme material (lapotron for the force field, a clock for time dilation).

Usage: data_1100.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
DATA = os.path.join(ROOT, 'src/main/resources/data')
REC = os.path.join(DATA, 'mio_icif/recipe/suit')
MODELS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/models/item/module')

T = lambda t: {'tag': t}       # noqa: E731
I = lambda i: {'item': i}      # noqa: E731

CARBON = T('c:plates/carbon')
IRIDIUM = T('c:plates/iridium')
NEUTRON = T('c:plates/neutron')
ELITE = T('c:circuits/elite')
LAPOTRON = I('mio_icif:normal/item_lapotron_crystal_lev0')
CRYSTAL = I('mio_icif:normal/item_crystal_lev0')
LIVING = I('mio_icif:normal/item_living_circuit')


def shaped(result, pattern, key):
    return {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern, 'key': key,
            'result': {'id': result, 'count': 1}}


def unit(name):
    return 'mio_icif:module/item_module_' + name


def mk2(name, theme):
    return shaped(unit(name) + '_mk2', ['NAN', 'AUA', 'NLN'], {'N': NEUTRON, 'A': theme, 'U': I(unit(name)), 'L': LIVING})


RECIPES = {
    'item_module_phase_jump_drive': shaped(unit('phase_jump_drive'), ['PEP', 'CXC', 'PIP'],
                                           {'P': CARBON, 'E': I('minecraft:echo_shard'), 'C': ELITE, 'X': I('minecraft:ender_eye'), 'I': IRIDIUM}),
    'item_module_phase_modulator': shaped(unit('phase_modulator'), ['PAP', 'CXC', 'PIP'],
                                          {'P': CARBON, 'A': I('minecraft:phantom_membrane'), 'C': ELITE, 'X': LAPOTRON, 'I': IRIDIUM}),
    'item_module_coherence_capacitor': shaped(unit('coherence_capacitor'), ['PRP', 'EXE', 'PIP'],
                                              {'P': CARBON, 'R': I('minecraft:redstone_block'), 'E': CRYSTAL, 'X': LAPOTRON, 'I': IRIDIUM}),
    'item_module_entanglement_relay': shaped(unit('entanglement_relay'), ['PAP', 'CXC', 'PIP'],
                                             {'P': CARBON, 'A': I('minecraft:amethyst_shard'), 'C': ELITE, 'X': I('minecraft:recovery_compass'),
                                              'I': IRIDIUM}),
    'item_quantum_bay_expander': shaped('mio_icif:module/item_quantum_bay_expander', ['ICI', 'LXL', 'ICI'],
                                        {'I': IRIDIUM, 'C': ELITE, 'L': LAPOTRON, 'X': I('minecraft:shulker_shell')}),
    'item_module_deflector_mk2': mk2('deflector', LAPOTRON),
    'item_module_behavior_predictor_mk2': mk2('behavior_predictor', I('minecraft:clock')),
    'item_module_phase_jump_drive_mk2': mk2('phase_jump_drive', I('minecraft:ender_eye')),
    'item_module_phase_modulator_mk2': mk2('phase_modulator', I('minecraft:phantom_membrane')),
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def main():
    for name, r in RECIPES.items():
        write(os.path.join(REC, name + '.json'), r)
        write(os.path.join(MODELS, name + '.json'), {'parent': 'minecraft:item/generated',
                                                     'textures': {'layer0': 'mio_icif:item/module/' + name}})
    print('wrote', len(RECIPES), 'recipes and item models')


if __name__ == '__main__':
    main()
