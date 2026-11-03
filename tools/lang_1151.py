#!/usr/bin/env python3
"""0.1.11.1 language values (en_us / zh_cn / ja_jp / ru_ru): the grid-tie interface (the Energy Converter until
0.1.11.0, same id: name, tooltips, the mode and limit messages; the two old mode messages go), and the FE line of
/mio_icif perf (grid-tie interfaces instead of FE pushed into cables; lang_1139.py set the old line).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_1151.py <repo root> [--check]
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


C = 'mio_icif.energy_converter.'
KEYS = {
    'block.mio_icif.energy_converter.energy_converter': v(
        'Grid-Tie Interface', '并网接口', '系統連系インターフェース', 'Интерфейс сопряжения сетей'),
    'tooltip.' + C + 'desc': v(
        'Connects the SI grid to the FE cables of other mods (front face); 4 FE = 1 EU, lossless',
        '将 SI 电网连接到其他模组的 FE 导线（正面）；4 FE = 1 EU，无损耗',
        'SI 電力網を他 MOD の FE ケーブルにつなぐ（正面）。4 FE = 1 EU、損失なし',
        'Соединяет сеть SI с FE-кабелями других модов (лицевая сторона); 4 FE = 1 EU, без потерь'),
    'tooltip.' + C + 'sneak_to_switch': v(
        'Sneak + right-click: mode (export, import, bidirectional 10 / 25 / 50 %%)',
        '潜行 + 右键：切换模式（输出、输入、双向 10 / 25 / 50 %%）',
        'スニーク＋右クリック：モード切替（送出・受入・双方向 10 / 25 / 50 %%）',
        'Shift + ПКМ: режим (отдача, приём, двусторонний 10 / 25 / 50 %%)'),
    'tooltip.' + C + 'limit': v(
        'Right-click: limit (32, 128, 512, 2,048 EU/t)',
        '右键：切换上限（32、128、512、2,048 EU/t）',
        '右クリック：上限切替（32・128・512・2,048 EU/t）',
        'ПКМ: предел (32, 128, 512, 2 048 EU/t)'),
    'message.' + C + 'mode_export': v(
        'Grid-tie: export (SI grid to the front, as FE)',
        '并网：输出（SI 电网 → 正面，FE）',
        '系統連系：送出（SI 電力網 → 正面、FE）',
        'Сопряжение: отдача (сеть SI → лицевая сторона, FE)'),
    'message.' + C + 'mode_import': v(
        'Grid-tie: import (FE from the front to the SI grid)',
        '并网：输入（正面的 FE → SI 电网）',
        '系統連系：受入（正面の FE → SI 電力網）',
        'Сопряжение: приём (FE с лицевой стороны → сеть SI)'),
    'message.' + C + 'mode_bidirectional': v(
        'Grid-tie: bidirectional, dead band %s%% of the buffer around half charge',
        '并网：双向，死区为缓存的 %s%%（以半满为中心）',
        '系統連系：双方向、不感帯はバッファの %s%%（半分を中心）',
        'Сопряжение: двусторонний, мёртвая зона %s%% буфера вокруг половины заряда'),
    'message.' + C + 'limit': v(
        'Grid-tie limit: %s EU/t', '并网上限：%s EU/t', '系統連系の上限：%s EU/t', 'Предел сопряжения: %s EU/t'),
    'command.mio_icif.perf.fe': v(
        'FE per tick: grid-tie interfaces %1$s, exported %2$s, imported %3$s; neighbour probes %4$s (%5$s skipped)',
        'FE 每 tick：并网接口 %1$s，输出 %2$s，输入 %3$s；邻居探测 %4$s（跳过 %5$s）',
        'FE 1 tick あたり：系統連系インターフェース %1$s、送出 %2$s、受入 %3$s。隣接確認 %4$s（省略 %5$s）',
        'FE за тик: сетевые интерфейсы %1$s, отдано %2$s, принято %3$s; проверок соседей %4$s (пропущено %5$s)'),
}

# the two directions of the Energy Converter (the grid-tie interface has three modes)
REMOVE = ['message.' + C + 'mode_eu_to_fe', 'message.' + C + 'mode_fe_to_eu']


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
