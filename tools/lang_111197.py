#!/usr/bin/env python3
"""0.1.11.197 language values (en_us / zh_cn / ja_jp / ru_ru): tool overhaul T4. The two plasma weapons get names that match
their ids: plasma_launcher is the plasma launcher (it was called the plasma ray gun in Chinese), electric_plasma_gun the
electric plasma gun (it was called a launcher).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_111197.py <repo root> [--check]
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

KEYS = {
    'item.mio_icif.item_tool_plasma_launcher': v(zh='等离子发射器'),
    'tooltip.mio_icif.plasma_launcher.active': v(zh='§a等离子发射器: 已激活'),
    'tooltip.mio_icif.plasma_launcher.inactive': v(zh='§7等离子发射器: 未激活'),
    'message.mio_icif.plasma_launcher.activated': v(zh='§a等离子发射器已激活！'),
    'message.mio_icif.plasma_launcher.deactivated': v(zh='§7等离子发射器已关闭'),
    'message.mio_icif.plasma_launcher.no_energy': v(zh='§c能量不足，无法激活等离子发射器！'),
    'message.mio_icif.plasma_launcher.not_active': v(zh='§c等离子发射器未开启！'),
    'hud.mio_icif.plasma_launcher.display': v(zh='等离子发射器: %s | 能量: %s / %s EU'),
    'item.mio_icif.item_tool_electric_plasma_gun': v('Electric Plasma Gun', '电力等离子枪', '電動プラズマガン', 'Электрическое плазменное ружьё'),
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
