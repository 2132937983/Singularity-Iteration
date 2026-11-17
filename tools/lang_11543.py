#!/usr/bin/env python3
"""0.1.11.43 language values (en_us / zh_cn / ja_jp / ru_ru), Wingman audit W4: the guard of the owner's place of death,
and the Fulton mode removed (its keys and the "lifting" state go).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11543.py <repo root> [--check]
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
    'message.mio_icif.wingman.death_guard': v('Your Wingmen guard the place where you fell (up to %s min). Come back within 24 blocks and they form up.',
        '你的僚机正在驻守你倒下的地方（最多 %s 分钟）。回到 24 格内，它们就会归队。',
        '僚機があなたの倒れた場所を守っています（最大 %s 分）。24 ブロック以内に戻ると編隊に戻ります。',
        'Ваши ведомые охраняют место, где вы пали (до %s мин). Вернитесь ближе чем на 24 блока, и они встанут в строй.'),
    'message.mio_icif.wingman.death_guard_over': v('The guard of your place of death is over: the Wingman returns to the Armory',
        '死亡地点驻守结束：僚机返回武装库', '死亡地点の警護が終了：僚機は武装庫へ帰還', 'Охрана места гибели окончена: ведомый возвращается в арсенал'),
}

REMOVE = ['gui.mio_icif.wingman.mode.fulton', 'gui.mio_icif.wingman.mode.fulton.tip', 'gui.mio_icif.wingman.mode_short.fulton',
          'gui.mio_icif.wingman.state.2', 'message.mio_icif.wingman.fulton_done', 'message.mio_icif.wingman.fulton_no_armory']

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
