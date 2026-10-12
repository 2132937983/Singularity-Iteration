#!/usr/bin/env python3
"""
0.1.8.0: models, blockstates, loot tables, recipes and tags of the Blueprint Builder system.
Idempotent: run it again after a change. Usage: assets_builder_180.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
RES = os.path.join(ROOT, 'src/main/resources')
A = os.path.join(RES, 'assets/mio_icif')
D = os.path.join(RES, 'data')


def save(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def load(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f, object_pairs_hook=OrderedDict)


def add_to_tag(path, *ids):
    tag = load(path) if os.path.exists(path) else OrderedDict([('replace', False), ('values', [])])
    for i in ids:
        if i not in tag['values']:
            tag['values'].append(i)
    save(path, tag)


def block_model(name):
    """Refined classic machine model of the Armory with the atlas of {name}."""
    model = load(os.path.join(A, 'models/block/producer/block_armory.json'))
    model['credit'] = 'Singularity Iteration 0.1.8.0 (assets_builder_180.py, refined machine frame)'
    model['textures']['r'] = 'mio_icif:block/refined/producer/' + name
    model['textures']['particle'] = 'mio_icif:block/producer/block_machine'
    save(os.path.join(A, 'models/block/producer', name + '.json'), model)
    save(os.path.join(A, 'models/item/producer', name + '.json'), {'parent': 'mio_icif:block/producer/' + name})


def loot(name):
    save(os.path.join(D, 'mio_icif/loot_table/blocks/producer', name + '.json'), OrderedDict([
        ('type', 'minecraft:block'),
        ('pools', [OrderedDict([
            ('bonus_rolls', 0.0),
            ('conditions', [{'condition': 'minecraft:survives_explosion'}]),
            ('entries', [{'type': 'minecraft:item', 'name': 'mio_icif:producer/' + name}]),
            ('rolls', 1.0)])]),
        ('random_sequence', 'mio_icif:blocks/producer/' + name)]))


def main():
    # Blueprint Builder: horizontal facing (machine block)
    block_model('block_blueprint_builder')
    variants = OrderedDict()
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        v = OrderedDict([('model', 'mio_icif:block/producer/block_blueprint_builder')])
        if y:
            v['y'] = y
        variants['facing=' + facing] = v
    save(os.path.join(A, 'blockstates/producer/block_blueprint_builder.json'), {'variants': variants})
    loot('block_blueprint_builder')
    save(os.path.join(D, 'mio_icif/recipe/block_blueprint_builder.json'), OrderedDict([
        ('type', 'minecraft:crafting_shaped'), ('category', 'misc'),
        ('key', OrderedDict([
            ('P', {'tag': 'c:plates/alloy'}),
            ('C', {'tag': 'c:circuits/advanced'}),
            ('R', {'item': 'minecraft:map'}),
            ('M', {'item': 'mio_icif:producer/block_machine_hull_advanced'}),
            ('F', {'item': 'minecraft:feather'})])),
        ('pattern', ['PRP', 'CMC', 'FPF']),
        ('result', {'count': 1, 'id': 'mio_icif:producer/block_blueprint_builder'}),
        ('show_notification', True)]))

    # Material Interface: no facing
    block_model('block_material_interface')
    save(os.path.join(A, 'blockstates/producer/block_material_interface.json'),
         {'variants': {'': {'model': 'mio_icif:block/producer/block_material_interface'}}})
    loot('block_material_interface')
    save(os.path.join(D, 'mio_icif/recipe/block_material_interface.json'), OrderedDict([
        ('type', 'minecraft:crafting_shaped'), ('category', 'misc'),
        ('key', OrderedDict([
            ('P', {'tag': 'c:plates/iron'}),
            ('C', {'tag': 'c:circuits/basic'}),
            ('H', {'item': 'minecraft:hopper'}),
            ('B', {'item': 'minecraft:chest'})])),
        ('pattern', ['PHP', 'CBC', 'PHP']),
        ('result', {'count': 1, 'id': 'mio_icif:producer/block_material_interface'}),
        ('show_notification', True)]))

    # tags: pickaxe for both; the builder is a machine (casing salvage, producer group)
    add_to_tag(os.path.join(D, 'minecraft/tags/block/mineable/pickaxe.json'),
               'mio_icif:producer/block_blueprint_builder', 'mio_icif:producer/block_material_interface')
    add_to_tag(os.path.join(D, 'c/tags/block/machine.json'), 'mio_icif:producer/block_blueprint_builder')
    add_to_tag(os.path.join(D, 'mio_icif/tags/block/producer.json'),
               'mio_icif:producer/block_blueprint_builder', 'mio_icif:producer/block_material_interface')
    # 0.1.8.0 quantum suit units: Smart Trajectory Guidance, Resonance Deconstruction Field
    units = {
        'smart_guidance': (['PTP', 'CXC', 'PBP'], OrderedDict([
            ('P', {'tag': 'c:plates/carbon'}), ('T', {'item': 'minecraft:target'}),
            ('C', {'tag': 'c:circuits/elite'}), ('X', {'item': 'mio_icif:module/item_module_ballistic'}),
            ('B', {'tag': 'c:plates/iridium'})])),
        'resonance_field': (['PNP', 'CAC', 'PBP'], OrderedDict([
            ('P', {'tag': 'c:plates/carbon'}), ('N', {'item': 'minecraft:note_block'}),
            ('C', {'tag': 'c:circuits/advanced'}), ('A', {'item': 'minecraft:amethyst_block'}),
            ('B', {'tag': 'c:plates/iridium'})])),
    }
    for uid, (pattern, key) in units.items():
        name = 'item_module_' + uid
        save(os.path.join(A, 'models/item/module', name + '.json'), OrderedDict([
            ('parent', 'minecraft:item/generated'), ('textures', {'layer0': 'mio_icif:item/module/' + name})]))
        save(os.path.join(D, 'mio_icif/recipe/suit', name + '.json'), OrderedDict([
            ('type', 'minecraft:crafting_shaped'), ('category', 'equipment'), ('pattern', pattern), ('key', key),
            ('result', OrderedDict([('id', 'mio_icif:module/' + name), ('count', 1)]))]))
    # 0.1.8.0 Ejector Pipe: model faces north (nozzle at z = 0); six facings by blockstate rotation
    face = lambda tex, uv=None, cull=None: OrderedDict([('texture', tex)] + ([('uv', uv)] if uv else []) + ([('cullface', cull)] if cull else []))
    save(os.path.join(A, 'models/block/pipe/block_pipe_ejector.json'), OrderedDict([
        ('parent', 'block/block'), ('render_type', 'cutout'),
        ('textures', OrderedDict([('particle', 'mio_icif:block/pipe/block_pipe_ejector'), ('front', 'mio_icif:block/pipe/block_pipe_ejector'),
                                  ('side', 'mio_icif:block/pipe/pipe_side'), ('cap', 'mio_icif:block/pipe/pipe_large')])),
        ('elements', [
            OrderedDict([('name', 'nozzle'), ('from', [4, 4, 0]), ('to', [12, 12, 4]), ('faces', OrderedDict([
                ('north', face('#front', [0, 0, 16, 16])), ('south', face('#front', [4, 4, 12, 12])),
                ('east', face('#front', [0, 0, 16, 4])), ('west', face('#front', [0, 0, 16, 4])),
                ('up', face('#front', [0, 0, 16, 4])), ('down', face('#front', [0, 0, 16, 4]))]))]),
            OrderedDict([('name', 'body'), ('from', [5, 5, 4]), ('to', [11, 11, 14]), ('faces', OrderedDict([
                ('east', face('#side', [0, 3, 10, 9])), ('west', face('#side', [0, 3, 10, 9])),
                ('up', face('#side', [3, 0, 9, 10])), ('down', face('#side', [3, 0, 9, 10]))]))]),
            OrderedDict([('name', 'flange'), ('from', [3, 3, 14]), ('to', [13, 13, 16]), ('faces', OrderedDict([
                ('north', face('#cap', [0, 0, 16, 16])), ('south', face('#cap', [0, 0, 16, 16], 'south')),
                ('east', face('#side', [0, 0, 2, 10])), ('west', face('#side', [0, 0, 2, 10])),
                ('up', face('#side', [0, 0, 10, 2])), ('down', face('#side', [0, 0, 10, 2]))]))]),
        ])]))
    rot = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    save(os.path.join(A, 'blockstates/pipe/block_pipe_ejector.json'), {'variants': OrderedDict(
        ('facing=' + f, OrderedDict([('model', 'mio_icif:block/pipe/block_pipe_ejector')] + list(r.items()))) for f, r in rot.items())})
    save(os.path.join(A, 'models/item/pipe/block_pipe_ejector.json'), {'parent': 'mio_icif:block/pipe/block_pipe_ejector'})
    save(os.path.join(D, 'mio_icif/loot_table/blocks/pipe/block_pipe_ejector.json'), OrderedDict([
        ('type', 'minecraft:block'),
        ('pools', [OrderedDict([('bonus_rolls', 0.0), ('conditions', [{'condition': 'minecraft:survives_explosion'}]),
                                ('entries', [{'type': 'minecraft:item', 'name': 'mio_icif:pipe/block_pipe_ejector'}]), ('rolls', 1.0)])]),
        ('random_sequence', 'mio_icif:blocks/pipe/block_pipe_ejector')]))
    save(os.path.join(D, 'mio_icif/recipe/block_pipe_ejector.json'), OrderedDict([
        ('type', 'minecraft:crafting_shaped'), ('category', 'misc'),
        ('key', OrderedDict([('P', {'tag': 'c:plates/iron'}), ('D', {'item': 'minecraft:dispenser'}), ('B', {'item': 'minecraft:bucket'})])),
        ('pattern', ['PDP', 'PBP']),
        ('result', {'count': 2, 'id': 'mio_icif:pipe/block_pipe_ejector'}), ('show_notification', True)]))
    add_to_tag(os.path.join(D, 'minecraft/tags/block/mineable/pickaxe.json'), 'mio_icif:pipe/block_pipe_ejector')
    print('wrote builder assets and data, unit models and recipes, ejector pipe')


if __name__ == '__main__':
    main()
