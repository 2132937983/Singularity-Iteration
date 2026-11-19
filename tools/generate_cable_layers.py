"""Deterministic JSON assets for IC2 insulation layers and a tintable cable band."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1] / 'src/main/resources'
assets = root / 'assets/mio_icif'
data = root / 'data/mio_icif'

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

variants = [('gold', 2), ('iron', 2), ('iron', 3)]
for metal, layer in variants:
    old, new = f'block_{metal}_cable_1', f'block_{metal}_cable_{layer}'
    for suffix in ['', '_side']:
        original = read(assets / f'models/block/wiring/cable/{old}{suffix}.json')
        # Always derive from the base geometry, even when regenerating tint bands.
        original['elements'] = [e for e in original['elements'] if e.get('name') != 'color_band']
        for element in original['elements']:
            for field in ['from', 'to']:
                element[field] = [v if v in (0, 8, 16) else v + (layer - 1) * (1 if v > 8 else -1)
                                  for v in element[field]]
        if 'parent' in original and old in original['parent']:
            original['parent'] = original['parent'].replace(old, new)
        write(assets / f'models/block/wiring/cable/{new}{suffix}.json', original)
    states = read(assets / f'blockstates/wiring/cable/{old}.json')
    write(assets / f'blockstates/wiring/cable/{new}.json', json.loads(json.dumps(states).replace(old, new)))
    icon = f'item_{"golden" if metal == "gold" else "iron"}_cable_{layer}'
    write(assets / f'models/item/wiring/cable/{new}.json', {'parent': 'minecraft:item/generated',
          'textures': {'layer0': f'mio_icif:item/normal/{icon}'}})
    loot = read(data / f'loot_table/blocks/wiring/cable/{old}.json')
    write(data / f'loot_table/blocks/wiring/cable/{new}.json', json.loads(json.dumps(loot).replace(old, new)))
    for bulk in [False, True]:
        previous = f'block_{metal}_cable_{layer-1}' if not bulk else f'block_{metal}_cable'
        ingredients = [{'item': f'mio_icif:wiring/cable/{previous}'}]
        ingredients += [{'item': 'mio_icif:resource/item_rubber'}] * (layer if bulk else 1)
        write(data / f'recipe/insulate_{metal}_cable_{layer}{"_bulk" if bulk else ""}.json',
              {'type': 'minecraft:crafting_shapeless', 'ingredients': ingredients,
               'result': {'id': f'mio_icif:wiring/cable/{new}', 'count': 1}})

for filename, metal in [('shapeless_182_insulated_gold_cable', 'gold'), ('shapeless_183_insulated_hv_cable', 'iron')]:
    value = read(data / f'recipe/{filename}.json')
    value['ingredients'] = [{'item': 'mio_icif:resource/item_rubber'}, {'item': f'mio_icif:wiring/cable/block_{metal}_cable'}]
    write(data / f'recipe/{filename}.json', value)

for lang, names in [('zh_cn', ['双层绝缘金导线', '双层绝缘高压铁导线', '三层绝缘高压铁导线']),
                    ('en_us', ['2x Insulated Gold Cable', '2x Insulated HV Iron Cable', '3x Insulated HV Iron Cable'])]:
    path = assets / f'lang/{lang}.json'
    # Preserve unrelated formatting and entries in the existing translations.
    text = path.read_text(encoding='utf-8-sig')
    existing = json.loads(text)
    additions = {f'block.mio_icif.wiring.cable.block_{metal}_cable_{layer}': name
                 for (metal, layer), name in zip(variants, names)}
    missing = {key: value for key, value in additions.items() if key not in existing}
    if missing:
        at = text.rfind('}')
        text = text[:at].rstrip() + ',\n' + ',\n'.join('  ' + json.dumps(k) + ': ' + json.dumps(v, ensure_ascii=False)
                   for k, v in missing.items()) + '\n}' + text[at+1:]
        path.write_text(text, encoding='utf-8')

tag = root / 'data/c/tags/block/cable.json'
value = read(tag)
for metal, layer in variants:
    item = f'mio_icif:wiring/cable/block_{metal}_cable_{layer}'
    if item not in value['values']: value['values'].append(item)
write(tag, value)

for name in ['block_tin_cable_1', 'block_cable_1', 'block_glass_cable', 'block_gold_cable_1', 'block_gold_cable_2',
             'block_iron_cable_1', 'block_iron_cable_2', 'block_iron_cable_3']:
    path = assets / f'models/block/wiring/cable/{name}.json'
    model = read(path)
    model['textures']['color_band'] = 'minecraft:block/white_concrete'
    model['elements'] = [e for e in model['elements'] if e.get('name') != 'color_band']
    center = model['elements'][0]
    low, high = center['from'][0] - 0.01, center['to'][0] + 0.01
    model['elements'].append({'name': 'color_band', 'from': [low, 7, low], 'to': [high, 9, high],
        'faces': {side: {'texture': '#color_band', 'tintindex': 0} for side in ['north', 'south', 'east', 'west']}})
    write(path, model)

print('Cable layers, recipes, loot, translations and tint bands generated.')
