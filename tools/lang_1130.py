#!/usr/bin/env python3
"""0.1.10.30 language values (en_us / zh_cn / ja_jp): the suit glow (client options, Look page switch) and the palette hint (the
quantum suit takes a palette). Idempotent; keeps the line endings of each file. With --check it only reports missing or different
keys. Usage: lang_1130.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
F = 'mio_icif.configuration.'

KEYS = {
    F + 'suit_glow': ('Suit glow', '套装发光', 'スーツの発光'),
    F + 'suit_glow.tooltip': (
        'Nano and quantum suits glow in their light colour: amber and red as the charge falls, breathing, an energy band and a '
        'power-on sweep.',
        '纳米套装和量子套装以发光色发光：电量下降时变为琥珀色和红色，带有呼吸、能量带和启动时的扫过效果。',
        'ナノスーツと量子スーツが発光色で光る。充電が減ると琥珀色・赤に変わり、呼吸、エネルギーの帯、起動時のスイープがある。'),
    F + 'suit_glow_strength': ('Suit glow strength', '套装发光强度', 'スーツの発光の強さ'),
    F + 'suit_glow_strength.tooltip': ('Strength of the suit glow. 1.0 = default.', '套装发光的强度。1.0 = 默认。',
                                      'スーツの発光の強さ。1.0 = 既定。'),
    'gui.mio_icif.equipment_console.appearance.suit_glow': ('Suit glow (lights and circuit lines)', '套装发光（灯光与电路线）',
                                                            'スーツの発光（ライトと回路ライン）'),
    'gui.mio_icif.palette.hint': ('Set the plate colour and the light colour of each nano or quantum suit piece.',
                                  '设置每个纳米或量子套装部件的装甲色和发光色。', 'ナノスーツと量子スーツの部位ごとに装甲色と発光色を設定する。'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    bad = 0
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        if CHECK:
            stray = [k for k, t in KEYS.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t[i])]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
            wrong = [k for k, t in KEYS.items() if data.get(k) != t[i]]
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in KEYS.items():
            data[key] = t[i]
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(KEYS))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
