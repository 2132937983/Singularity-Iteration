#!/usr/bin/env python3
"""0.1.10.24 language values (en_us / zh_cn / ja_jp / ru_ru): the built-in experimental resource pack is gone, so the
texture style cards of the Equipment Console's Look tab and the pack name are removed; the first page of the tab (hand
cover, frame core exterior) is now called "Suit". Idempotent; keeps the line endings of each file. With --check it only
reports keys that are missing, different or still present.
Usage: lang_1124.py <repo root> [--check]
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
LANGS = ('en_us', 'zh_cn', 'ja_jp', 'ru_ru')

A = 'gui.mio_icif.equipment_console.appearance.'

SET = {
    A + 'page.suit': ('Suit', '装甲', 'スーツ', 'Костюм'),
    'gui.mio_icif.equipment_console.tab.appearance.tip': (
        'Appearance settings: suit display and palette',
        '外观设置：装甲显示与配色',
        '外観設定: スーツの表示と配色',
        'Настройки внешнего вида: отображение костюма и цвета'),
}

REMOVE = [A + k for k in ('texture_style', 'hint', 'active', 'select', 'reload', 'missing', 'default', 'default.desc',
                          'experimental', 'experimental.desc', 'page.texture')] + ['pack.mio_icif.experimental']


def main():
    bad = 0
    for i, lang in enumerate(LANGS):
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        if CHECK:
            wrong = [k for k, t in SET.items() if data.get(k) != t[i]]
            left = [k for k in REMOVE if k in data]
            bad += len(wrong) + len(left)
            print(lang, 'missing or different:', len(wrong), wrong[:5], 'still present:', len(left), left[:5])
            continue
        removed = sum(1 for k in REMOVE if data.pop(k, None) is not None)
        for key, t in SET.items():
            data[key] = t[i]
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(SET), 'removed:', removed)
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
