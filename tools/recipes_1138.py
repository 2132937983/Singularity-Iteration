#!/usr/bin/env python3
"""0.1.10.38 (A7 / D14): crafting recipe inputs that are electric items or machine blocks become mio_icif:electric ingredients.

A plain {"item": X} ingredient shows X's default stack. 36 electric items are fully charged by default, while a crafted copy has
0 charge, so autocrafting patterns recorded from the recipes (Refined Storage, AE2, JEI transfer) never matched what the
sub-patterns made. mio_icif:electric matches X whatever its charge or stored data (as before) and shows the empty stack.

Input: tools/guide_data/autocraft_ingredients.json, written by the GameTest Round1138GameTests.dumpAutocraftIngredients (items:
electric / electric_full / machine; tags that contain one of them). Only the crafting recipe types below are changed; an
ingredient that already has a "type" is left alone, so the script is idempotent. Text edits keep each file's formatting.
Usage: recipes_1138.py <repo root> [--check]
"""
import glob
import json
import os
import re
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
CRAFTING = {'minecraft:crafting_shaped', 'minecraft:crafting_shapeless', 'mio_icif:charge_carrying', 'mio_icif:charge_carrying_shapeless'}

data = json.load(open(os.path.join(ROOT, 'tools/guide_data/autocraft_ingredients.json'), encoding='utf-8'))
ITEMS, TAGS = set(data['items']), set(data['tags'])


def ingredients(recipe):
    """The ingredient objects of a crafting recipe (key values of a shaped recipe, the list of a shapeless one)."""
    groups = list(recipe.get('key', {}).values()) + list(recipe.get('ingredients', []))
    for g in groups:
        for obj in (g if isinstance(g, list) else [g]):
            if isinstance(obj, dict):
                yield obj


def convertible(obj):
    if 'type' in obj:
        return None
    if set(obj) == {'item'} and obj['item'] in ITEMS:
        return 'item', obj['item']
    if set(obj) == {'tag'} and obj['tag'] in TAGS:
        return 'tag', obj['tag']
    return None


changed, converted, wanted = [], 0, 0
for path in sorted(glob.glob(os.path.join(ROOT, 'src/main/resources/data/mio_icif/recipe/**/*.json'), recursive=True)):
    raw = open(path, 'rb').read().decode('utf-8')
    try:
        recipe = json.loads(raw)
    except ValueError:
        continue
    if recipe.get('type') not in CRAFTING:
        continue
    targets = {c for c in (convertible(o) for o in ingredients(recipe)) if c}
    if not targets:
        continue
    wanted += 1
    text = raw
    for field, value in sorted(targets):
        pattern = re.compile(r'\{(\s*)"' + field + r'"(\s*):(\s*)"' + re.escape(value) + r'"(\s*)\}')
        text, n = pattern.subn(lambda m: '{' + m.group(1) + '"type":' + m.group(3) + '"mio_icif:electric",' + (m.group(1) or ' ')
                               + '"' + field + '"' + m.group(2) + ':' + m.group(3) + '"' + value + '"' + m.group(4) + '}', text)
        converted += n
    after = json.loads(text)
    assert not any(convertible(o) for o in ingredients(after)), path   # every electric input converted, nothing else broken
    if text != raw:
        changed.append(os.path.relpath(path, ROOT))
        if not CHECK:
            open(path, 'wb').write(text.encode('utf-8'))

print('recipes with electric or machine inputs left to convert:' if CHECK else 'recipes converted:', len(changed),
      '| ingredient objects:', converted)
if CHECK and changed:
    for p in changed:
        print('  ', p)
    sys.exit(1)
