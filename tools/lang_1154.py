#!/usr/bin/env python3
"""0.1.11.4 language values (en_us / zh_cn / ja_jp / ru_ru): the Russian names of the double and triple insulated cables
name their voltage tier like the other languages (they named gold and iron, and the cable materials change in 0.1.11.4);
the five tooltips of the removed generator-mode liquid cooling go.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_1154.py <repo root> [--check]
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
    'block.mio_icif.wiring.cable.block_gold_cable_2': v(ru='Высоковольтный кабель с двойной изоляцией'),
    'block.mio_icif.wiring.cable.block_iron_cable_2': v(ru='Сверхвысоковольтный кабель с двойной изоляцией'),
    'block.mio_icif.wiring.cable.block_iron_cable_3': v(ru='Сверхвысоковольтный кабель с тройной изоляцией'),
}

# the gauges of the removed liquid cooling of the generator-mode reactor
REMOVE = ['tooltip.mio_icif.reactor.coolant', 'tooltip.mio_icif.reactor.hot', 'tooltip.mio_icif.reactor.coolant.rate',
          'tooltip.mio_icif.reactor.coolant.empty', 'tooltip.mio_icif.reactor.coolant.tip']


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
