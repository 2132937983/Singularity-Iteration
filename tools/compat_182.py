#!/usr/bin/env python3
"""0.1.8.2 external farming compatibility data (Immersive Engineering, Thermal Series).

Generated files (all guarded by "neoforge:mod_loaded", so nothing loads without the other mod):

* data/mio_icif/tags/item/crop_seed_bags.json
    Own tag for the seed bag. The bag is NOT added to c:seeds: in the reference pack, Create
    Crafts & Additions, Kaleidoscope Cookery and Astral Sorcery consume #c:seeds in recipes
    (seed oil, millstone, altar), which would destroy analyzed bags.
* data/mio_icif/recipe/compat/immersiveengineering/cloche/<plant>_gain<band>.json
    Garden Cloche: the input is the custom ingredient mio_icif:crop_seed (plant + Gain band). The
    cloche tests the ingredient (Ingredient#test) and keeps the bag in its seed slot, so the bag
    and its traits are never lost. The output grows with the Gain band (x1, x1.5, x2, x3).
    Metal crops need their ore or storage block as soil (the IC2 root rule).
    Format checked against the IE 1.21.1 generated recipes (cloche/wheat.json, red_mushroom.json).
* data/mio_icif/recipe/compat/thermal/insolator_catalyst_seed_bag.json
    Thermal looks machine recipes up by item (SingleItemRecipeManager / ComparableItemStack,
    checked with javap on thermal-11.0.7), so per-plant bag inputs cannot work. The seed bag is an
    Insolator catalyst instead: use chance 0 (never used up), output x1.5 / x1.25 secondary.
* data/mio_icif/recipe/compat/thermal/insolator/<item>.json
    SI resource crops in the Insolator through their plain base items (no NBT): the six
    metal-rich seeds, coffee beans, terra wart and milk wart.

The script deletes the files it made before (the compat folders only) and writes them again.
Usage: compat_182.py <repo root>
"""
import json
import os
import shutil
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
DATA = os.path.join(ROOT, 'src/main/resources/data/mio_icif')

BANDS = [(0, 7, 1.0), (8, 15, 1.5), (16, 23, 2.0), (24, 31, 3.0)]
DIRT = [{'item': 'minecraft:dirt'}]


def ores(metal):
    return [{'tag': 'c:ores/' + metal}, {'tag': 'c:storage_blocks/' + metal}]


# plant id: (tier, [(output id, base count)], soil, render type, render block)
CROP = 'immersiveengineering:crop'
GEN = 'immersiveengineering:generic'
PLANTS = {
    'wheat': (1, [('minecraft:wheat', 2)], DIRT, CROP, 'minecraft:wheat'),
    'carrots': (2, [('minecraft:carrot', 2)], DIRT, CROP, 'minecraft:carrots'),
    'potato': (2, [('minecraft:potato', 2)], DIRT, CROP, 'minecraft:potatoes'),
    'beetroot': (1, [('minecraft:beetroot', 2)], DIRT, CROP, 'minecraft:beetroots'),
    'pumpkin': (1, [('minecraft:pumpkin', 1)], DIRT, GEN, 'minecraft:pumpkin'),
    'melon': (2, [('minecraft:melon_slice', 4)], DIRT, GEN, 'minecraft:melon'),
    'cocoa': (3, [('minecraft:cocoa_beans', 2)], DIRT, GEN, 'minecraft:cocoa'),
    'coffee': (7, [('mio_icif:resource/item_coffee_bean', 2)], DIRT, GEN, 'minecraft:sweet_berry_bush'),
    'hops': (5, [('mio_icif:normal/item_hops', 2)], DIRT, GEN, 'minecraft:fern'),
    'flax': (2, [('minecraft:string', 2)], DIRT, GEN, 'minecraft:short_grass'),
    'reed': (2, [('minecraft:sugar_cane', 2)], DIRT, GEN, 'minecraft:sugar_cane'),
    'stickreed': (4, [('mio_icif:resource/item_harz', 1), ('minecraft:sugar_cane', 1)], DIRT, GEN, 'minecraft:sugar_cane'),
    'blackthorn': (2, [('minecraft:black_dye', 2)], DIRT, GEN, 'minecraft:wither_rose'),
    'cyazint': (2, [('minecraft:cyan_dye', 2)], DIRT, GEN, 'minecraft:blue_orchid'),
    'dandelion': (1, [('minecraft:yellow_dye', 2)], DIRT, GEN, 'minecraft:dandelion'),
    'rose': (1, [('minecraft:red_dye', 2)], DIRT, GEN, 'minecraft:poppy'),
    'tulip': (2, [('minecraft:purple_dye', 2)], DIRT, GEN, 'minecraft:allium'),
    'brownMushroom': (1, [('minecraft:brown_mushroom', 2)], [{'item': 'minecraft:mycelium'}, {'item': 'minecraft:podzol'}], GEN, 'minecraft:brown_mushroom'),
    'redMushroom': (1, [('minecraft:red_mushroom', 2)], [{'item': 'minecraft:mycelium'}, {'item': 'minecraft:podzol'}], GEN, 'minecraft:red_mushroom'),
    'ferru': (6, [('mio_icif:resource/item_iron_dust_small', 2)], ores('iron'), GEN, 'minecraft:fern'),
    'cyprium': (6, [('mio_icif:resource/item_copper_dust_small', 2)], ores('copper'), GEN, 'minecraft:fern'),
    'stagnium': (6, [('mio_icif:resource/item_tin_dust_small', 2)], ores('tin'), GEN, 'minecraft:fern'),
    'plumbiscus': (6, [('mio_icif:resource/item_lead_dust_small', 2)], ores('lead'), GEN, 'minecraft:fern'),
    'aurelia': (6, [('mio_icif:resource/item_golden_dust_small', 2)], ores('gold'), GEN, 'minecraft:fern'),
    'shining': (6, [('mio_icif:resource/item_silver_dust_small', 2)], ores('silver'), GEN, 'minecraft:fern'),
    'titanium': (8, [('mio_icif:resource/item_titanium_dust_small', 2)], ores('titanium'), GEN, 'minecraft:fern'),
    'uranium': (9, [('mio_icif:resource/item_uran_238_small', 2)], ores('uranium'), GEN, 'minecraft:fern'),
    'redwheat': (6, [('minecraft:redstone', 2)], DIRT, CROP, 'minecraft:wheat'),
    'venomilia': (3, [('minecraft:purple_dye', 2)], DIRT, GEN, 'minecraft:allium'),
    'netherwart': (5, [('minecraft:nether_wart', 2)], [{'item': 'minecraft:soul_sand'}], CROP, 'minecraft:nether_wart'),
    'terrawart': (5, [('mio_icif:resource/item_terra_wart', 1)], [{'item': 'minecraft:snow_block'}], CROP, 'minecraft:nether_wart'),
    'eatingplant': (6, [('minecraft:cactus', 2)], [{'item': 'minecraft:sand'}], GEN, 'minecraft:cactus'),
    'blazereed': (6, [('minecraft:blaze_rod', 1)], [{'item': 'minecraft:soul_sand'}], GEN, 'minecraft:sugar_cane'),
    'bobs_yer_uncle_ranks_berries': (11, [('mio_icif:resource/item_bobs_yer_uncle_ranks_berry', 1)], DIRT, GEN, 'minecraft:sweet_berry_bush'),
    'corium': (6, [('minecraft:leather', 1)], DIRT, GEN, 'minecraft:vine'),
    'corpse_plant': (5, [('minecraft:rotten_flesh', 2)], DIRT, GEN, 'minecraft:dead_bush'),
    'creeper_weed': (7, [('minecraft:gunpowder', 1)], DIRT, GEN, 'minecraft:fern'),
    'diareed': (12, [('mio_icif:resource/item_diamond_dust_small', 1)], DIRT, GEN, 'minecraft:sugar_cane'),
    'egg_plant': (6, [('minecraft:egg', 1)], DIRT, GEN, 'minecraft:sweet_berry_bush'),
    'ender_blossom': (10, [('mio_icif:resource/item_ender_pearl_dust', 1)], [{'item': 'minecraft:end_stone'}], GEN, 'minecraft:chorus_flower'),
    'meat_rose': (7, [('minecraft:pink_dye', 2)], DIRT, GEN, 'minecraft:rose_bush'),
    'milk_wart': (6, [('mio_icif:resource/item_milk_wart', 1)], DIRT, GEN, 'minecraft:brown_mushroom'),
    'oil_berries': (9, [('mio_icif:resource/item_oil_berry', 1)], DIRT, GEN, 'minecraft:sweet_berry_bush'),
    'slime_plant': (6, [('minecraft:slime_ball', 1)], DIRT, GEN, 'minecraft:fern'),
    'spidernip': (4, [('minecraft:string', 2)], DIRT, GEN, 'minecraft:cobweb'),
    'tearstalks': (8, [('minecraft:ghast_tear', 1)], [{'item': 'minecraft:soul_sand'}], GEN, 'minecraft:sugar_cane'),
    'withereed': (8, [('mio_icif:resource/item_coal_dust', 2)], [{'item': 'minecraft:soul_sand'}], GEN, 'minecraft:sugar_cane'),
}

COND_IE = [{'type': 'neoforge:mod_loaded', 'modid': 'immersiveengineering'}]
COND_TH = [{'type': 'neoforge:mod_loaded', 'modid': 'thermal'}]

# Thermal Insolator inputs (plain items, returned with chance 1.0 like Thermal's own seed recipes).
THERMAL = {
    'iron_rich_seed': ('mio_icif:crop/iron_rich_seed', 'mio_icif:resource/item_iron_dust_small', 2.0),
    'copper_rich_seed': ('mio_icif:crop/copper_rich_seed', 'mio_icif:resource/item_copper_dust_small', 2.0),
    'tin_rich_seed': ('mio_icif:crop/tin_rich_seed', 'mio_icif:resource/item_tin_dust_small', 2.0),
    'lead_rich_seed': ('mio_icif:crop/lead_rich_seed', 'mio_icif:resource/item_lead_dust_small', 2.0),
    'titanium_rich_seed': ('mio_icif:crop/titanium_rich_seed', 'mio_icif:resource/item_titanium_dust_small', 1.5),
    'uranium_rich_seed': ('mio_icif:crop/uranium_rich_seed', 'mio_icif:resource/item_uran_238_small', 1.25),
    'coffee_bean': ('mio_icif:resource/item_coffee_bean', 'mio_icif:resource/item_coffee_bean', 2.0),
    'terra_wart': ('mio_icif:resource/item_terra_wart', 'mio_icif:resource/item_terra_wart', 2.0),
    'milk_wart': ('mio_icif:resource/item_milk_wart', 'mio_icif:resource/item_milk_wart', 2.0),
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def results(outputs, mult):
    out = []
    for item, count in outputs:
        exact = count * mult
        whole = int(exact)
        frac = round(exact - whole, 3)
        if whole > 0:
            out.append({'id': item, 'count': whole})
        if frac > 0:
            out.append({'chance': frac, 'output': {'id': item}})
    return out


def main():
    compat = os.path.join(DATA, 'recipe/compat')
    if os.path.isdir(compat):
        shutil.rmtree(compat)
    write(os.path.join(DATA, 'tags/item/crop_seed_bags.json'), {'values': ['mio_icif:normal/item_crop_seed']})
    n_ie = 0
    for plant, (tier, outputs, soil, render, block) in PLANTS.items():
        for i, (lo, hi, mult) in enumerate(BANDS):
            recipe = {
                'neoforge:conditions': COND_IE,
                'type': 'immersiveengineering:cloche',
                'input': {'type': 'mio_icif:crop_seed', 'plant': 'mio_icif:' + plant, 'min_gain': lo, 'max_gain': hi},
                'render': {'type': render, 'block': block},
                'results': results(outputs, mult),
                'soil': soil if len(soil) > 1 else soil[0],
                'time': 480 + 160 * tier,
            }
            write(os.path.join(compat, 'immersiveengineering/cloche', '%s_gain%d.json' % (plant.lower(), i)), recipe)
            n_ie += 1
    write(os.path.join(compat, 'thermal/insolator_catalyst_seed_bag.json'), {
        'neoforge:conditions': COND_TH,
        'type': 'thermal:insolator_catalyst',
        'ingredient': {'tag': 'mio_icif:crop_seed_bags'},
        'primary_mod': 1.5,
        'secondary_mod': 1.25,
        'energy_mod': 1.1,
        'min_chance': 0.0,
        'use_chance': 0.0,
    })
    for name, (inp, out, chance) in THERMAL.items():
        write(os.path.join(compat, 'thermal/insolator', name + '.json'), {
            'neoforge:conditions': COND_TH,
            'type': 'thermal:insolator',
            'ingredient': {'item': inp},
            'result': [{'item': out, 'chance': chance}] + ([] if inp == out else [{'item': inp, 'chance': 1.0}]),
            'energy': 40000,
        })
    print('IE cloche recipes:', n_ie, '| Thermal insolator recipes:', len(THERMAL), '| Thermal catalyst: 1 | tag: 1')


if __name__ == '__main__':
    main()
