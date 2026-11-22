#!/usr/bin/env python3
"""0.1.11.153: the data of the silver plate and the dense silver plate, after lead's, and the first recipes that make the silver
casing (it had only its tag): item models, the plate and dense plate tags, recipes (forge hammer: ingot -> plate, plate -> 2
casings; metal former rolling the same; compressor 9 plates -> dense plate; block cutter silver block -> 9 plates; macerator plate
-> 8 small dust, dense plate -> 9 dust) and the names in en_us, zh_cn, ja_jp and ru_ru. The textures come from
tools/silver_plates_111153.py. Writes new files as they should be, adds missing entries to the shared tags and lang files (their
line ends kept). Idempotent.

Usage: silver_plates_data_111153.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
RES = os.path.join(ROOT, 'src/main/resources')
PLATE, DENSE, CASING = 'mio_icif:resource/item_silver_plate', 'mio_icif:resource/item_silver_denseplate', 'mio_icif:resource/item_silver_casing'
HAMMER = 'mio_icif:item_tool_hammer'
written = []


def put(path, obj):
    full = os.path.join(RES, path)
    text = json.dumps(obj, indent=2, ensure_ascii=False) + '\n'
    os.makedirs(os.path.dirname(full), exist_ok=True)
    old = open(full, encoding='utf-8').read() if os.path.exists(full) else None
    if old != text:
        open(full, 'w', encoding='utf-8', newline='\n').write(text)
        written.append(path)


def tag(path, *ids):
    full = os.path.join(RES, path)
    raw = open(full, 'rb').read().decode('utf-8') if os.path.exists(full) else None
    obj = json.loads(raw) if raw else {'replace': False, 'values': []}
    values = obj.setdefault('values', [])
    have = {v['id'] if isinstance(v, dict) else v for v in values}
    missing = [i for i in ids if i not in have]
    if not missing and raw is not None:
        return
    for i in missing:
        values.append({'id': i, 'required': False})
    eol = '\r\n' if raw and '\r\n' in raw else '\n'
    os.makedirs(os.path.dirname(full), exist_ok=True)
    open(full, 'wb').write((json.dumps(obj, indent=2, ensure_ascii=False) + '\n').replace('\n', eol).encode('utf-8'))
    written.append(path)


def main():
    for item in (PLATE, DENSE):
        name = item.split(':', 1)[1]
        put('assets/mio_icif/models/item/%s.json' % name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mio_icif:item/' + name}})
    tag('data/c/tags/item/plates/silver.json', PLATE)
    tag('data/c/tags/item/plates.json', '#c:plates/silver')
    tag('data/c/tags/item/dense_plates/silver.json', DENSE)
    tag('data/c/tags/item/dense_plates.json', '#c:dense_plates/silver')
    r = 'data/mio_icif/recipe/'
    put(r + 'resource/item_silver_plate.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [{'tag': 'c:ingots/silver'}, {'item': HAMMER}], 'result': {'count': 1, 'id': PLATE}})
    put(r + 'resource/item_silver_casing.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [{'tag': 'c:plates/silver'}, {'item': HAMMER}], 'result': {'count': 2, 'id': CASING}})
    put(r + 'metal_former/rolling/silver_ingot_to_plate.json', {'type': 'mio_icif:metal_former_rolling',
        'ingredient': {'tag': 'c:ingots/silver'}, 'processingtime': 200, 'result': {'count': 1, 'id': PLATE}})
    put(r + 'metal_former/rolling/silver_plate_to_casing.json', {'type': 'mio_icif:metal_former_rolling',
        'ingredient': {'tag': 'c:plates/silver'}, 'processingtime': 300, 'result': {'count': 2, 'id': CASING}})
    put(r + 'compressor/silver_plate_to_dense.json', {'type': 'mio_icif:compressor', 'count': 9, 'energypertick': 20,
        'ingredient': {'tag': 'c:plates/silver'}, 'processingtime': 400, 'result': {'count': 1, 'id': DENSE}})
    put(r + 'block_cutter/silver_block_to_plate.json', {'type': 'mio_icif:block_cutter', 'ingredient': {'item': 'mio_icif:block_silver'},
        'result': {'count': 9, 'id': PLATE}, 'processingtime': 150, 'energypertick': 16})
    put(r + 'powder/silver_plate_to_silver_dust_small.json', {'type': 'mio_icif:powder', 'count': 1,
        'ingredient': {'tag': 'c:plates/silver'}, 'result': {'count': 8, 'id': 'mio_icif:resource/item_silver_dust_small'}})
    put(r + 'powder/silver_denseplate_to_dust.json', {'type': 'mio_icif:powder', 'count': 1,
        'ingredient': {'tag': 'c:dense_plates/silver'}, 'result': {'count': 9, 'id': 'mio_icif:resource/item_silver_dust'}})
    lang()
    print('files written:', len(written))
    for w in written:
        print(' ', w)


# key, en, zh, ja, ru
LANG = [
    ('item.mio_icif.resource.item_silver_plate', 'Silver Plate', '银板', '銀板', 'Серебряная пластина'),
    ('item.mio_icif.resource.item_silver_denseplate', 'Dense Silver Plate', '致密银板', '高密度銀板', 'Плотная серебряная пластина'),
]


def lang():
    for code, col in (('en_us', 1), ('zh_cn', 2), ('ja_jp', 3), ('ru_ru', 4)):
        path = os.path.join(RES, 'assets/mio_icif/lang', code + '.json')
        raw = open(path, 'rb').read().decode('utf-8')
        eol = '\r\n' if '\r\n' in raw else '\n'
        lines = raw.replace('\r\n', '\n').split('\n')
        index = {}
        for k, line in enumerate(lines):
            s = line.strip()
            if s.startswith('"') and '":' in s:
                index[s[1:s.index('"', 1)]] = k
        anchor = index['item.mio_icif.resource.item_silver_casing']
        changed = False
        for entry in LANG:
            key, text = entry[0], entry[col]
            line = '  %s: %s,' % (json.dumps(key), json.dumps(text, ensure_ascii=False))
            if key in index:
                if lines[index[key]].rstrip(',') != line.rstrip(','):
                    lines[index[key]] = line if lines[index[key]].rstrip().endswith(',') else line.rstrip(',')
                    changed = True
                anchor = index[key]
            else:
                lines.insert(anchor + 1, line)
                index = {k: (v + 1 if v > anchor else v) for k, v in index.items()}
                anchor += 1
                index[key] = anchor
                changed = True
        if changed:
            text = '\n'.join(lines)
            json.loads(text)
            open(path, 'wb').write(text.replace('\n', eol).encode('utf-8'))
            written.append('lang/' + code)


if __name__ == '__main__':
    main()
