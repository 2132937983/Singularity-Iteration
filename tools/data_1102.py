#!/usr/bin/env python3
"""
0.1.10.2 data: recipe and item model of the holomap Mk.II (it projects the 3D holo table).
The nano Mk.II pattern (the Mk.I unit in the middle, neutron plates, a living circuit) with a map
as the theme material.

Usage: data_1102.py <repo root>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import data_1100 as d  # noqa: E402

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
REC = os.path.join(ROOT, 'src/main/resources/data/mio_icif/recipe/suit')
MODELS = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/models/item/module')

RECIPES = {
    'item_module_holomap_mk2': d.mk2('holomap', d.I('minecraft:map')),
}


def main():
    for name, r in RECIPES.items():
        d.write(os.path.join(REC, name + '.json'), r)
        d.write(os.path.join(MODELS, name + '.json'), {'parent': 'minecraft:item/generated',
                                                       'textures': {'layer0': 'mio_icif:item/module/' + name}})
    print('wrote', len(RECIPES), 'recipe(s) and item model(s)')


if __name__ == '__main__':
    main()
