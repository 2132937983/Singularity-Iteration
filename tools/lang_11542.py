#!/usr/bin/env python3
"""0.1.11.42 language values (en_us / zh_cn / ja_jp / ru_ru), Wingman audit W3 (decision 2 C): a worn piece sends a Wingman
home (and stops a deployment), an energy weapon out of charge.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11542.py <repo root> [--check]
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
    'message.mio_icif.wingman.worn': v('%s is worn: the Wingman returns to the Armory for repairs', '%s 耐久过低：僚机返回武装库维修',
        '%s の耐久が低下：僚機は修理のため武装庫へ帰還', '%s изношен: ведомый возвращается в арсенал на ремонт'),
    'message.mio_icif.wingman.needs_repair': v('%s must be repaired before it can fly (below 10 %% durability)', '%s 需要先维修才能出击（耐久低于 10%%）',
        '%s は修理が必要です（耐久 10%% 未満）', '%s нужно починить перед вылетом (прочность ниже 10 %%)'),
    'message.mio_icif.wingman.no_charge': v('Wingman: %s is out of charge; it fights in melee', '僚机：%s 没电了，改为近战',
        '僚機：%s の電力切れ。近接戦闘に切り替えます', 'Ведомый: у %s нет заряда, он сражается в ближнем бою'),
}

REMOVE = []

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
