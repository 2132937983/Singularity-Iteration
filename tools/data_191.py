#!/usr/bin/env python3
"""
0.1.9.1 data: item models of the Thermal Retention Unit and the Recovered Parts Case, and the
Thermal Retention Unit recipe (wool insulation around copper plates and a basic circuit).

Usage: data_191.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
RES = os.path.join(ROOT, 'src/main/resources')


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def model(path):
    write(os.path.join(RES, 'assets/mio_icif/models/item', path + '.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mio_icif:item/' + path}})


def main():
    model('upgrade/thermal_retention_upgrade')
    model('normal/item_recovered_parts')
    write(os.path.join(RES, 'data/mio_icif/recipe/shaped_thermal_retention_upgrade.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['WPW', 'PCP', 'WPW'],
        'key': {'W': {'tag': 'minecraft:wool'}, 'P': {'tag': 'c:plates/copper'}, 'C': {'tag': 'c:circuits/basic'}},
        'result': {'id': 'mio_icif:upgrade/thermal_retention_upgrade', 'count': 1}})
    print('wrote 2 item models and 1 recipe')


if __name__ == '__main__':
    main()
