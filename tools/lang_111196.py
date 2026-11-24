#!/usr/bin/env python3
"""0.1.11.196 language values (en_us / zh_cn / ja_jp / ru_ru): tool overhaul T3 (drill area modes, chainsaw felling mode,
the iridium drill HUD line).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_111196.py <repo root> [--check]
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
    'tooltip.mio_icif.drill.area': v('%s×%s', '%s×%s', '%s×%s', '%s×%s'),
    'tooltip.mio_icif.drill.area_line': v('Area: %s (G to switch)', '范围：%s（G 键切换）', '範囲：%s（G で切替）', 'Область: %s (G — сменить)'),
    M + 'drill.area': v('Drill area: %s', '钻头范围：%s', 'ドリル範囲：%s', 'Область бура: %s'),
    M + 'chainsaw.mode': v('Chainsaw: %s', '链锯：%s', 'チェーンソー：%s', 'Бензопила: %s'),
    'tooltip.mio_icif.chainsaw.single': v('One block', '单块', '1 ブロック', 'Один блок'),
    'tooltip.mio_icif.chainsaw.felling': v('Fell the tree', '整棵伐木', '木を丸ごと伐採', 'Валить дерево целиком'),
    'hud.mio_icif.driller.display': v('Iridium drill: %s · %s / %s EU', '铱钻头：%s · %s / %s EU', 'イリジウムドリル：%s · %s / %s EU', 'Иридиевый бур: %s · %s / %s EU'),
    'hud.mio_icif.driller.switch_mode': v('Mode: %s (sneak + G to switch)', '模式：%s（潜行 + G 切换）', 'モード：%s（スニーク + G で切替）', 'Режим: %s (присесть + G — сменить)'),
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
