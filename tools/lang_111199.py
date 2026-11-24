#!/usr/bin/env python3
"""0.1.11.199 language values (en_us / zh_cn / ja_jp / ru_ru): tool overhaul T5b, the ore scanner shows where the nearest
block of each kind is.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_111199.py <repo root> [--check]
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


M = 'message.mio_icif.'

G = 'gui.mio_icif.od_scanner.'

KEYS = {
    G + 'nearest': v('nearest %s, %s m', '最近：%s %s 格', '最寄り：%s %s m', 'ближайшая: %s, %s м'),
    G + 'here': v('right here', '就在脚下', 'すぐここ', 'прямо здесь'),
    G + 'up': v('above', '上方', '上', 'выше'),
    G + 'down': v('below', '下方', '下', 'ниже'),
    G + 'dir.n': v('north', '北', '北', 'север'),
    G + 'dir.ne': v('north-east', '东北', '北東', 'северо-восток'),
    G + 'dir.e': v('east', '东', '東', 'восток'),
    G + 'dir.se': v('south-east', '东南', '南東', 'юго-восток'),
    G + 'dir.s': v('south', '南', '南', 'юг'),
    G + 'dir.sw': v('south-west', '西南', '南西', 'юго-запад'),
    G + 'dir.w': v('west', '西', '西', 'запад'),
    G + 'dir.nw': v('north-west', '西北', '北西', 'северо-запад'),
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
