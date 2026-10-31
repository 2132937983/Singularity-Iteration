#!/usr/bin/env python3
"""0.1.10.38 language values (en_us / zh_cn / ja_jp / ru_ru): the processing machine line of /mio_icif perf (PerfCommand).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1138.py <repo root> [--check]
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
    'command.mio_icif.perf.machines': v(
        'Processing machines per tick: full ticks %1$s, asleep %2$s, lamp switches %3$s, redstone reads %4$s',
        '加工机器每 tick：完整 tick %1$s，休眠 %2$s，状态灯切换 %3$s，红石读取 %4$s',
        '加工機械 1 tick あたり：通常 tick %1$s、休眠 %2$s、ランプ切替 %3$s、レッドストーン読み取り %4$s',
        'Машины обработки за тик: полных тиков %1$s, в ожидании %2$s, переключений ламп %3$s, чтений редстоуна %4$s'),
}


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
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in mine.items():
            data[key] = t
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(mine))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
