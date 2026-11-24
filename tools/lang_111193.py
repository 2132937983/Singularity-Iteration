#!/usr/bin/env python3
"""0.1.11.193 language values (en_us / zh_cn / ja_jp / ru_ru): tool overhaul T0 messages (no permission, other
dimension, unloaded target, wireless target that holds no EU).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_111193.py <repo root> [--check]
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
    M + 'tool.no_permission': v('You may not change that block here', '你没有权限改动这个方块', 'ここではそのブロックを変更できません', 'Здесь нельзя изменять этот блок'),
    M + 'tool.other_dimension': v('The target is in another dimension', '目标在另一个维度', '対象は別のディメンションにあります', 'Цель находится в другом измерении'),
    M + 'tool.target_unloaded': v('The first block is not loaded; go closer to it', '第一个方块所在区块没有加载，请靠近它', '最初のブロックが読み込まれていません。近づいてください', 'Первый блок не загружен; подойдите ближе'),
    M + 'electric_wireless_manager.not_energy': v('That block stores no EU; pick a machine or a storage block', '这个方块不储存 EU，请选择机器或储电方块', 'そのブロックは EU を蓄えません。機械か蓄電ブロックを選んでください', 'Этот блок не хранит EU; выберите машину или накопитель'),
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
