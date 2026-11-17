#!/usr/bin/env python3
"""0.1.11.29 language values (en_us / zh_cn / ja_jp / ru_ru): transformer names (the EV transformer works between EV and IV,
not "SC"; the Chinese names name both voltage tiers), and the removal of orphan cable keys (block.mio_icif.wire_*,
item.mio_icif.normal.item_*_cable, the old transformer_ltom..etos keys) that nothing registers or reads.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11529.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
LANGS = ('en_us', 'zh_cn', 'ja_jp', 'ru_ru')


def v(en=None, zh=None, ja=None, ru=None):
    return {k: t for k, t in zip(LANGS, (en, zh, ja, ru)) if t is not None}


KEYS = {
    'block.mio_icif.wiring.transformer_lv_mv': v(zh='低压–中压变压器'),
    'block.mio_icif.wiring.transformer_mv_hv': v(zh='中压–高压变压器'),
    'block.mio_icif.wiring.transformer_hv_ev': v(zh='高压–超高压变压器'),
    'block.mio_icif.wiring.transformer_ev_sc': v('Transformer EV→IV', '超高压–IV 变压器', '変圧器 EV→IV', 'Трансформатор EV→IV'),
    'block.mio_icif.wiring.transformer_iv_luv': v(zh='IV–LuV 变压器'),
    'block.mio_icif.wiring.transformer_luv_zpmv': v(zh='LuV–ZPMV 变压器'),
}

REMOVE = [
    'block.mio_icif.wire_lv', 'block.mio_icif.wire_mv', 'block.mio_icif.wire_hv', 'block.mio_icif.wire_ev', 'block.mio_icif.wire_iv',
    'block.mio_icif.wire_isolation_lv', 'block.mio_icif.wire_isolation_mv', 'block.mio_icif.wire_isolation_hv',
    'block.mio_icif.wire_isolation_ev',
    'item.mio_icif.normal.item_copper_cable', 'item.mio_icif.normal.item_copper_cable_1', 'item.mio_icif.normal.item_detector_cable',
    'item.mio_icif.normal.item_glass_cable', 'item.mio_icif.normal.item_golden_cable', 'item.mio_icif.normal.item_golden_cable_1',
    'item.mio_icif.normal.item_golden_cable_2', 'item.mio_icif.normal.item_iron_cable', 'item.mio_icif.normal.item_iron_cable_1',
    'item.mio_icif.normal.item_iron_cable_2', 'item.mio_icif.normal.item_iron_cable_3', 'item.mio_icif.normal.item_spliter_cable',
    'item.mio_icif.normal.item_tin_cable', 'item.mio_icif.normal.item_tin_cable_1',
    'block.mio_icif.transformer_ltom', 'block.mio_icif.transformer_mtoh', 'block.mio_icif.transformer_htoe', 'block.mio_icif.transformer_etos',
]


def main():
    bad = 0
    for lang in LANGS:
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        mine = {k: t[lang] for k, t in KEYS.items() if lang in t}
        if CHECK:
            stray = [k for k, t in mine.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t)]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
            wrong = [k for k, t in mine.items() if data.get(k) != t]
            left = [k for k in REMOVE if k in data]
            bad += len(wrong) + len(left)
            print(lang, 'missing or different:', len(wrong), wrong[:5], 'still present:', len(left), left)
            continue
        removed = sum(1 for k in REMOVE if data.pop(k, None) is not None)
        for key, t in mine.items():
            data[key] = t
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(mine), 'removed:', removed)
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
