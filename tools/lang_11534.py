#!/usr/bin/env python3
"""0.1.11.34 language values (en_us / zh_cn / ja_jp / ru_ru), cable audit W5: the protective cable screen (rated flow
not set, automatic rated voltage, help texts).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11534.py <repo root> [--check]
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
    'gui.mio_icif.protection.not_set': v('Not set', '未设定', '未設定', 'Не задано'),
    'gui.mio_icif.protection.voltage_auto': v('Line (%s EU)', '跟随线路（%s EU）', '線路に合わせる（%s EU）', 'По линии (%s EU)'),
    'gui.mio_icif.protection.voltage_auto_none': v('Line (no cable)', '跟随线路（无导线）', '線路に合わせる（ケーブルなし）', 'По линии (нет кабеля)'),
    'gui.mio_icif.protection.help': v(
        'Opens after the delay at twice the rated flow, sooner for larger overloads, at once at ten times; with no rated flow it does not open for flow. Above the rated voltage (automatic: the packet limit of the line) it opens before the packet passes. Closes again after 3 s, at most three times in a row. Redstone holds it open.',
        '2 倍额定流量时经过延时断开，过载越大越快，10 倍时立即断开；额定流量未设定时不因流量断开。包超过额定电压（自动：线路的单包上限）时在其通过前断开。3 秒后自动重合，最多连续 3 次。红石信号使其保持断开。',
        '定格流量の 2 倍で遅延の後に開き、過負荷が大きいほど早く、10 倍で即座に開く。定格流量が未設定なら流量では開かない。定格電圧（自動：線路の最大パケット）を超えるパケットは通る前に開く。3 秒後に再投入、連続 3 回まで。レッドストーンで開いたままになる。',
        'Размыкается после задержки при двойном номинальном потоке, быстрее при большей перегрузке, сразу при десятикратном; без номинального потока по потоку не размыкается. Пакет выше номинального напряжения (авто: предел пакета линии) размыкает его до прохода. Замыкается через 3 с, не более трёх раз подряд. Редстоун держит разомкнутым.'),
    'gui.mio_icif.protection.help_fuse': v(
        'Blows after the delay at twice the rated flow, at once at ten times; with no rated flow it does not blow for flow. Above the rated voltage (automatic: the packet limit of the line) it blows before the packet passes. Right-click a blown fuse with a new fuse cable to restore it.',
        '2 倍额定流量时经过延时熔断，10 倍时立即熔断；额定流量未设定时不因流量熔断。包超过额定电压（自动：线路的单包上限）时在其通过前熔断。用新的保险丝导线右击熔断的保险丝即可恢复。',
        '定格流量の 2 倍で遅延の後に切れ、10 倍で即座に切れる。定格流量が未設定なら流量では切れない。定格電圧（自動：線路の最大パケット）を超えるパケットは通る前に切れる。切れたヒューズは新しいヒューズケーブルで右クリックして戻す。',
        'Перегорает после задержки при двойном номинальном потоке, сразу при десятикратном; без номинального потока по потоку не перегорает. Пакет выше номинального напряжения (авто: предел пакета линии) пережигает его до прохода. Перегоревший предохранитель восстанавливается правым кликом новым кабелем-предохранителем.'),
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
