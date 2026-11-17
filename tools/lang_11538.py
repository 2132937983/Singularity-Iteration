#!/usr/bin/env python3
"""0.1.11.38 language values (en_us / zh_cn / ja_jp / ru_ru), chunk loader audit C2: the map cell states, its controls and the
select-all / clear buttons.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11538.py <repo root> [--check]
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
    'gui.mio_icif.chunk_loader.cell.held': v('Loading [%s, %s]', '加载中 [%s, %s]', '読み込み中 [%s, %s]', 'Загружается [%s, %s]'),
    'gui.mio_icif.chunk_loader.cell.waiting': v('Selected, not loading [%s, %s]', '已选中，未加载 [%s, %s]', '選択済み・未読み込み [%s, %s]', 'Выбран, не загружается [%s, %s]'),
    'gui.mio_icif.chunk_loader.cell.ring': v('Runs beside a loading chunk, free [%s, %s]', '随相邻的加载区块运转，不收费 [%s, %s]', '隣の読み込みチャンクに伴って動作・無料 [%s, %s]', 'Работает рядом с загруженным чанком, бесплатно [%s, %s]'),
    'gui.mio_icif.chunk_loader.cell.unselected': v('Not selected [%s, %s]', '未选中 [%s, %s]', '未選択 [%s, %s]', 'Не выбран [%s, %s]'),
    'gui.mio_icif.chunk_loader.cell.own': v('The loader\'s own chunk (always selected)', '加载器所在区块（总是选中）', 'ローダー自身のチャンク（常に選択）', 'Чанк самого загрузчика (всегда выбран)'),
    'gui.mio_icif.chunk_loader.cell.help': v('Click: select or remove · drag: select an area · right click or drag: remove', '单击：选中或取消 · 拖动：框选 · 右键单击或拖动：取消',
        'クリック：選択・解除 · ドラッグ：範囲選択 · 右クリック・右ドラッグ：解除', 'Щелчок: выбрать или убрать · перетаскивание: выбрать область · правый щелчок или перетаскивание: убрать'),
    'gui.mio_icif.chunk_loader.select_all': v('Select all (nearest first, up to the limit)', '全选（由近到远，至上限）', 'すべて選択（近い順、上限まで）', 'Выбрать все (от ближних, до предела)'),
    'gui.mio_icif.chunk_loader.clear': v('Clear (keeps the own chunk)', '清空（保留自身区块）', 'クリア（自分のチャンクは残す）', 'Очистить (свой чанк остаётся)'),
}

REMOVE = ['gui.mio_icif.chunk_loader.cell.self', 'gui.mio_icif.chunk_loader.cell.selected']


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
