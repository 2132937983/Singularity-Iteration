#!/usr/bin/env python3
"""0.1.11.5 language values (en_us / zh_cn / ja_jp / ru_ru): the tactical holomap's title, read-outs, directions, legend
and controls (they were hard-coded English), and the suit detail view key.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_1155.py <repo root> [--check]
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


H = 'hud.mio_icif.holo.'
KEYS = {
    H + 'title': v('HOLO  %sx%sx%s', '全息  %s×%s×%s', 'ホロ  %s×%s×%s', 'ГОЛО  %s×%s×%s'),
    H + 'roof': v('ROOF CUT +%s', '剖开 +%s', '天井カット +%s', 'СРЕЗ КРЫШИ +%s'),
    H + 'hostile': v('HOSTILE %s / %s', '敌对 %s / %s', '敵対 %s / %s', 'ВРАГИ %s / %s'),
    H + 'n': v('N', '北', '北', 'С'),
    H + 'e': v('E', '东', '東', 'В'),
    H + 's': v('S', '南', '南', 'Ю'),
    H + 'w': v('W', '西', '西', 'З'),
    H + 'legend': v('\u25b2 hostile  \u25cf player / ally  \u25a0 neutral  \u00b7 passive  hollow = behind cover',
                    '\u25b2 敌对  \u25cf 玩家 / 僚机  \u25a0 中立  \u00b7 被动  空心 = 被遮挡',
                    '\u25b2 敵対  \u25cf プレイヤー / 僚機  \u25a0 中立  \u00b7 友好  中空 = 遮蔽物の向こう',
                    '\u25b2 враг  \u25cf игрок / союзник  \u25a0 нейтрал  \u00b7 мирный  пустой = за укрытием'),
    H + 'controls': v('Wheel: zoom %s%%   Shift + wheel: tilt %s\u00b0', '滚轮 缩放 %s%%　Shift + 滚轮 俯角 %s\u00b0',
                      'ホイール：拡大 %s%%　Shift＋ホイール：俯角 %s\u00b0', 'Колесо: масштаб %s%%   Shift + колесо: наклон %s\u00b0'),
    'key.mio_icif.hud_detail': v('Suit detail view (hold)', '套装详细视图（按住）', 'スーツ詳細ビュー（長押し）', 'Подробный вид костюма (удерживать)'),
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
