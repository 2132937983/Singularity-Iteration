#!/usr/bin/env python3
"""0.1.11.36 language values (en_us / zh_cn / ja_jp / ru_ru), chunk loader audit C0: the screen's tooltips (energy, cost, the
chunk cells) and the Jade line of a chunk loader.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11536.py <repo root> [--check]
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
    'gui.mio_icif.chunk_loader.energy': v('EU: %s / %s', 'EU：%s / %s', 'EU：%s / %s', 'EU: %s / %s'),
    'gui.mio_icif.chunk_loader.cost': v('Uses %s EU/t now', '当前耗电 %s EU/t', '現在の消費 %s EU/t', 'Сейчас потребляет %s EU/t'),
    'gui.mio_icif.chunk_loader.cell.self': v('Own chunk [%s, %s]', '自身区块 [%s, %s]', '自分のチャンク [%s, %s]', 'Свой чанк [%s, %s]'),
    'gui.mio_icif.chunk_loader.cell.selected': v('Selected [%s, %s] · click to remove', '已选中 [%s, %s] · 点击取消', '選択中 [%s, %s] · クリックで解除', 'Выбран [%s, %s] · нажмите, чтобы убрать'),
    'gui.mio_icif.chunk_loader.cell.unselected': v('Not selected [%s, %s] · click to select', '未选中 [%s, %s] · 点击选中', '未選択 [%s, %s] · クリックで選択', 'Не выбран [%s, %s] · нажмите, чтобы выбрать'),
    'jade.mio_icif.chunk_loader': v('Chunks: %s held / %s selected · %s · %s EU/t', '区块：加载 %s / 已选 %s · %s · %s EU/t',
        'チャンク：保持 %s / 選択 %s · %s · %s EU/t', 'Чанки: удержано %s / выбрано %s · %s · %s EU/t'),
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
