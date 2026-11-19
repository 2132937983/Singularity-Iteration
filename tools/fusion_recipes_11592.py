#!/usr/bin/env python3
"""0.1.11.92: the fusion reactor's recipes (data/mio_icif/recipe/fusion/). A reactor takes some 2,460 blocks, so the structural
blocks come 8 or 16 to a craft; the controller and the ports one each. Deuterium (0.1.11.94): hot water cell -> thermal centrifuge -> unstable hot
water cell -> canner with an empty fuel rod -> deuterium fuel rod.

Usage: fusion_recipes_11592.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
OUT = os.path.join(ROOT, 'src/main/resources/data/mio_icif/recipe/fusion')

M = 'mio_icif:'
TI, TID, PB, C, LI = M + 'resource/item_titanium_plate', M + 'resource/item_titanium_denseplate', M + 'resource/item_lead_plate', \
    M + 'resource/item_carbon_plate', M + 'resource/item_lithium_dust'
NB, SC, COIL, IR = M + 'resource/item_niobium_titanium_plate', M + 'wiring/cable/block_superconducting_cable', M + 'resource/item_coil', \
    M + 'resource/item_iridium_plate'
CIRCUIT, LAPO, CRYSTAL, HULL, CHAMBER = M + 'normal/item_advcircuit', M + 'normal/item_lapotron_crystal_lev0', M + 'normal/item_crystal_lev0', \
    M + 'producer/block_machine_hull_advanced', M + 'reactor/block_reactor_chamber'
VESSEL = M + 'fusion/block_fusion_vacuum_vessel'
CRYOSTAT = M + 'fusion/block_fusion_cryostat'
# an Empty Cell holding nothing (a filled one has the same item id)
EMPTY_CELL = {'type': 'mio_icif:fluid_cell', 'fluid': 'minecraft:empty'}


def block(name):
    return M + 'fusion/block_fusion_' + name


def shaped(result, count, pattern, keys):
    return {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
            'key': {k: (v if isinstance(v, dict) else {'item': v}) for k, v in keys.items()}, 'result': {'id': result, 'count': count}}


RECIPES = {
    # titanium and lead: the stainless double wall and its shielding
    'vacuum_vessel': shaped(block('vacuum_vessel'), 16, ['TPT', 'PTP', 'TPT'], {'T': TI, 'P': PB}),
    # carbon (first wall armour) and lithium (the breeder) on a vessel wall
    'blanket': shaped(block('blanket'), 8, ['CLC', 'LVL', 'CLC'], {'C': C, 'L': LI, 'V': VESSEL}),
    'divertor': shaped(block('divertor'), 8, ['DDD', 'CVC', 'DDD'], {'D': TID, 'C': C, 'V': VESSEL}),
    # niobium-titanium, the real superconductor, with superconducting cable and coils
    'toroidal_coil': shaped(block('toroidal_coil'), 8, ['NSN', 'SCS', 'NSN'], {'N': NB, 'S': SC, 'C': COIL}),
    'poloidal_coil': shaped(block('poloidal_coil'), 8, ['NCN', 'C C', 'NCN'], {'N': NB, 'C': COIL}),
    'central_solenoid': shaped(block('central_solenoid'), 8, ['NSN', 'S S', 'NSN'], {'N': NB, 'S': SC}),
    'cryostat': shaped(block('cryostat'), 8, ['TTT', 'TVT', 'TTT'], {'T': TI, 'V': VESSEL}),
    'controller': shaped(block('controller'), 1, ['ALA', 'RHR', 'AIA'], {'A': CIRCUIT, 'L': LAPO, 'R': CHAMBER, 'H': HULL, 'I': IR}),
    # 0.1.11.93: the ports and the heaters sit on cryostat casing (they pass through the cryostat), not on a machine hull
    'nbi_port': shaped(block('nbi_port'), 1, [' X ', 'VKV', ' A '], {'X': COIL, 'V': VESSEL, 'K': CRYOSTAT, 'A': CIRCUIT}),
    # the gyrotron's microwaves need its coils: one in each corner
    'ecrh_port': shaped(block('ecrh_port'), 1, ['CXC', 'VKV', 'CAC'], {'X': CRYSTAL, 'C': COIL, 'V': VESSEL, 'K': CRYOSTAT, 'A': CIRCUIT}),
    'fuel_port': shaped(block('fuel_port'), 1, [' X ', 'VKV', ' A '], {'X': EMPTY_CELL, 'V': VESSEL, 'K': CRYOSTAT, 'A': CIRCUIT}),
    # the exhaust is a vacuum pump that draws off the helium ash
    'exhaust_port': shaped(block('exhaust_port'), 1, [' X ', 'VKV', ' A '], {'X': M + 'producer/block_pump_elc', 'V': VESSEL, 'K': CRYOSTAT, 'A': CIRCUIT}),
    # the magnets' cold comes from a 60k coolant cell
    'cryo_port': shaped(block('cryo_port'), 1, [' X ', 'VKV', ' A '], {'X': M + 'reactor/item_reactor_coolant_six', 'V': VESSEL, 'K': CRYOSTAT, 'A': CIRCUIT}),
    'power_port': shaped(block('power_port'), 1, [' X ', 'VKV', ' A '], {'X': SC, 'V': VESSEL, 'K': CRYOSTAT, 'A': CIRCUIT}),
    # the pellet injector: a reactor coolant injector, the mod's own reactor-safety injector
    'mitigation_port': shaped(block('mitigation_port'), 1, [' X ', 'VKV', ' A '], {'X': M + 'producer/block_lapis_reactor_coolant_injector', 'V': VESSEL, 'K': CRYOSTAT, 'A': CIRCUIT}),
    # 0.1.11.94: deuterium comes from hot water: the thermal centrifuge enriches a hot water cell (static or dynamic) into an
    # unstable hot water cell, as slowly as the water cell before; a canner cans that (left slot) into an empty fuel rod
    # (middle slot): the deuterium fuel rod
    'unstable_hotwater_cell': {'type': 'mio_icif:centrifuge', 'ingredient': {'type': 'mio_icif:fluid_cell', 'fluid': M + 'hotwater'},
                               'minheat': 4000, 'processingtime': 1200,
                               'primaryResult': {'id': M + 'cell/item_cell_unstable_hotwater', 'count': 1}},
    'deuterium_fuel_rod': {'type': 'mio_icif:canning', 'can': {'type': 'mio_icif:fluid_cell', 'fluid': M + 'unstable_hotwater'},
                           'food': {'item': M + 'normal/item_fuel_rod'}, 'result': {'id': M + 'normal/item_deuterium_cell', 'count': 1}},
}
OBSOLETE = ['deuterium_cell']   # 0.1.11.94: the centrifuge no longer makes deuterium from water cells


def main():
    os.makedirs(OUT, exist_ok=True)
    for name in OBSOLETE:
        if os.path.exists(os.path.join(OUT, name + '.json')):
            os.remove(os.path.join(OUT, name + '.json'))
    for name, recipe in RECIPES.items():
        with open(os.path.join(OUT, name + '.json'), 'w', encoding='utf-8', newline='\n') as f:
            json.dump(recipe, f, indent=2, ensure_ascii=False)
            f.write('\n')
    print('recipes:', len(RECIPES))


if __name__ == '__main__':
    main()
