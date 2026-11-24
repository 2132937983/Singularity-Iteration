#!/usr/bin/env python3
"""0.1.11.200 language values (en_us / zh_cn / ja_jp / ru_ru): tool overhaul T5c, the EU meter becomes the multimeter with
energy, wind and geomagnetic probes; the old wind meter and geomagnetic detector say they convert into it.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_111200.py <repo root> [--check]
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

M = 'item.mio_icif.item_tool_meter'

KEYS = {
    M: v('Multimeter', '多功能仪表', 'マルチメーター', 'Мультиметр'),
    M + '.probe.energy': v('Energy', '电能', 'エネルギー', 'Энергия'),
    M + '.probe.wind': v('Wind', '风力', '風力', 'Ветер'),
    M + '.probe.geomagnetic': v('Geomagnetic', '地磁', '地磁気', 'Геомагнетизм'),
    M + '.probe_switched': v('Probe: %s', '探头：%s', 'プローブ：%s', 'Зонд: %s'),
    M + '.tooltip_probe': v('Probe: %s (G to switch)', '探头：%s（G 键切换）', 'プローブ：%s（G で切替）', 'Зонд: %s (G — сменить)'),
    M + '.help': v('Energy probe: right-click a grid device for its data, sneak + right-click to switch the reading. G switches the probe.',
                   '电能探头：右键电网设备查看数据，潜行右键切换读数。G 键切换探头。',
                   'エネルギープローブ：電力網の機器を右クリックで情報表示、スニーク右クリックで表示切替。G でプローブ切替。',
                   'Энергозонд: ПКМ по устройству сети — данные, присесть + ПКМ — сменить показание. G — сменить зонд.'),
    'tooltip.mio_icif.windmeter.usage': v('Replaced by the multimeter (wind probe): craft it into one.', '已由多功能仪表（风力探头）取代，可直接合成为多功能仪表。',
                                          'マルチメーター（風力プローブ）に置き換え。クラフトで変換できます。', 'Заменён мультиметром (зонд ветра): переделайте в него.'),
    'tooltip.mio_icif.geomagnetic_detector.desc': v('Replaced by the multimeter (geomagnetic probe): craft it into one.', '已由多功能仪表（地磁探头）取代，可直接合成为多功能仪表。',
                                                    'マルチメーター（地磁気プローブ）に置き換え。クラフトで変換できます。', 'Заменён мультиметром (геомагнитный зонд): переделайте в него.'),
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
