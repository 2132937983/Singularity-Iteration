#!/usr/bin/env python3
"""
0.1.10.1 data: recipe and item model of the compute core, and the six quantum Mk.II items of
quantum phase 2.

Recipe design: the compute core keeps the quantum tier (iridium plates, elite circuits, a lapotron
crystal in the middle) with comparators for the logic and energy crystals for the power. The Mk.II
items follow the nano Mk.II pattern (the Mk.I unit in the middle, neutron plates, a living circuit)
with a theme material: a spyglass for the ore scanner (range), an observer for the grid telemetry,
a sculk sensor for the entity ESP, a crossbow for the ballistic computer (multishot arcs), a compass
for the smart guidance and a bell for the resonance field.

Usage: data_1101.py <repo root>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import data_1100 as d  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
REC = os.path.join(ROOT, 'src/main/resources/data/mio_icif/recipe/suit')
MODELS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/models/item/module')

I = d.I  # noqa: E741

RECIPES = {
    'item_module_compute_core': d.shaped(d.unit('compute_core'), ['PRP', 'CXC', 'PEP'],
                                         {'P': d.IRIDIUM, 'R': I('minecraft:comparator'), 'C': d.ELITE, 'X': d.LAPOTRON,
                                          'E': d.CRYSTAL}),
    'item_module_ore_scanner_mk2': d.mk2('ore_scanner', I('minecraft:spyglass')),
    'item_module_grid_telemetry_mk2': d.mk2('grid_telemetry', I('minecraft:observer')),
    'item_module_entity_esp_mk2': d.mk2('entity_esp', I('minecraft:sculk_sensor')),
    'item_module_ballistic_mk2': d.mk2('ballistic', I('minecraft:crossbow')),
    'item_module_smart_guidance_mk2': d.mk2('smart_guidance', I('minecraft:compass')),
    'item_module_resonance_field_mk2': d.mk2('resonance_field', I('minecraft:bell')),
}


def main():
    for name, r in RECIPES.items():
        d.write(os.path.join(REC, name + '.json'), r)
        d.write(os.path.join(MODELS, name + '.json'), {'parent': 'minecraft:item/generated',
                                                       'textures': {'layer0': 'mio_icif:item/module/' + name}})
    print('wrote', len(RECIPES), 'recipes and item models')


if __name__ == '__main__':
    main()
