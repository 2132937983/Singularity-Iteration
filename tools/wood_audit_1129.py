#!/usr/bin/env python3
"""0.1.10.29: the vanilla recipes that make planks, read from the Minecraft resources jar: for every crafting recipe whose result
is a planks item and that has exactly one ingredient, the ingredient (item or tag), the planks and the count. The block cutter's
wood rule follows these recipes (6 planks for 4 crafted, IC2 1.12.2 block_cutter.ini); this lists what it gives for each vanilla
wood and the tags it accepts. Usage: wood_audit_1129.py <minecraft resources jar> [planks tag json]"""
import json
import sys
import zipfile

jar = sys.argv[1]
rows = []
with zipfile.ZipFile(jar) as z:
    names = set(z.namelist())
    planks_tag = json.loads(z.read('data/minecraft/tags/item/planks.json'))['values'] if 'data/minecraft/tags/item/planks.json' in names else []
    logs_tag = json.loads(z.read('data/minecraft/tags/item/logs.json'))['values'] if 'data/minecraft/tags/item/logs.json' in names else []
    for n in sorted(names):
        if not n.startswith('data/minecraft/recipe/') or not n.endswith('.json'):
            continue
        r = json.loads(z.read(n))
        res = r.get('result', {})
        out = res.get('id') or res.get('item')
        if not out or not out.endswith('_planks'):
            continue
        if r.get('type') == 'minecraft:crafting_shapeless':
            ings = r.get('ingredients', [])
        elif r.get('type') == 'minecraft:crafting_shaped':
            ings = [r['key'][c] for row in r['pattern'] for c in row if c != ' ']
        else:
            continue
        if len(ings) != 1:
            continue
        ing = ings[0]
        src = ('#' + ing['tag']) if 'tag' in ing else ing.get('item')
        count = res.get('count', 1)
        rows.append((n.rsplit('/', 1)[1], src, out, count, max(count, count * 3 // 2)))
print('planks tag:', planks_tag)
print('logs tag:', logs_tag)
print(f'{"recipe":34} {"ingredient":34} {"planks":28} {"crafted":>7} {"cutter":>6}')
for r in rows:
    print(f'{r[0]:34} {r[1]:34} {r[2]:28} {r[3]:>7} {r[4]:>6}')
print(f'{len(rows)} single-ingredient planks recipes')
