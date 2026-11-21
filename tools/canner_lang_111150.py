#!/usr/bin/env python3
"""0.1.11.150: the canner's own mode-button tooltip (it showed the metal former's "rolling, cutting, extruding"), in en_us,
zh_cn, ja_jp and ru_ru. The arguments are the four mode names (gui.mio_icif.canner.mode_*) and the current one. A new key goes
after ANCHOR; keeps each file's line ends. Idempotent.

Usage: canner_lang_111150.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
ANCHOR = 'gui.mio_icif.canner.mode_tooltip'

# key, en, zh, ja, ru
TEXTS = [
    ('gui.mio_icif.canner.tip.switch_mode', 'Cycles %1$s, %2$s, %3$s and %4$s; now: %5$s.',
     '在%1$s、%2$s、%3$s、%4$s四种模式之间循环切换；当前：%5$s。',
     '%1$s・%2$s・%3$s・%4$sの4つのモードを順に切り替える。現在：%5$s。',
     'Переключает по кругу: %1$s, %2$s, %3$s, %4$s; сейчас: %5$s.'),
]


def main():
    changed = 0
    for code, col in (('en_us', 1), ('zh_cn', 2), ('ja_jp', 3), ('ru_ru', 4)):
        path = os.path.join(LANG, code + '.json')
        raw = open(path, 'rb').read().decode('utf-8')
        eol = '\r\n' if '\r\n' in raw else '\n'
        lines = raw.replace('\r\n', '\n').split('\n')
        index = {}
        for i, line in enumerate(lines):
            s = line.strip()
            if s.startswith('"') and '":' in s:
                index[s[1:s.index('"', 1)]] = i
        anchor = index[ANCHOR]
        for entry in TEXTS:
            key, text = entry[0], entry[col]
            line = '  %s: %s,' % (json.dumps(key), json.dumps(text, ensure_ascii=False))
            if key in index:
                if lines[index[key]].rstrip(',') != line.rstrip(','):
                    lines[index[key]] = line if lines[index[key]].rstrip().endswith(',') else line.rstrip(',')
                    changed += 1
                anchor = index[key]
            else:
                lines.insert(anchor + 1, line)
                index = {k: (v + 1 if v > anchor else v) for k, v in index.items()}
                anchor += 1
                index[key] = anchor
                changed += 1
        text = '\n'.join(lines)
        json.loads(text)
        open(path, 'wb').write(text.replace('\n', eol).encode('utf-8'))
    print('lines changed:', changed)


if __name__ == '__main__':
    main()
