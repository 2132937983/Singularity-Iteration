#!/usr/bin/env python3
"""0.1.11.21 language values (en_us / zh_cn / ja_jp / ru_ru): the reactor's control room (annunciators, SCRAM, setpoint,
reset, record, heat balance) and the tripped status.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11521.py <repo root> [--check]
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
    'gui.mio_icif.reactor.status.tripped': v('Tripped (SCRAM) · press Reset or turn the redstone signal off and on', '已跳堆（SCRAM）· 按复位，或把红石信号关掉再打开',
        'トリップ（SCRAM）· リセットを押すか、レッドストーン信号を切って入れ直す', 'Аварийная остановка (SCRAM) · нажмите «Сброс» или выключите и включите сигнал'),
    'gui.mio_icif.reactor.control.lamp.redstone': v('REDSTONE', '红石', 'レッドストーン', 'СИГНАЛ'),
    'gui.mio_icif.reactor.control.lamp.heat': v('HEAT HI', '热高', '高温', 'ПЕРЕГРЕВ'),
    'gui.mio_icif.reactor.control.lamp.trip': v('TRIP', '跳堆', 'トリップ', 'СБРОС'),
    'gui.mio_icif.reactor.control.lamp.rising': v('RISING', '升温', '昇温', 'РОСТ'),
    'gui.mio_icif.reactor.control.lamp.columns': v('9 COLUMNS', '9 列', '9 列', '9 СТОЛБЦОВ'),
    'gui.mio_icif.reactor.control.lamp.output': v('OUTPUT', '发电', '出力', 'ВЫХОД'),
    'gui.mio_icif.reactor.control.reset': v('Reset', '复位', 'リセット', 'Сброс'),
    'gui.mio_icif.reactor.control.setpoint': v('Trip at %s', '%s 跳堆', '%s でトリップ', 'Сброс при %s'),
    'gui.mio_icif.reactor.control.setpoint_off': v('Auto trip off', '自动跳堆关闭', '自動トリップ切', 'Автосброс выкл.'),
    'gui.mio_icif.reactor.control.feed': v('Feed %sx', '进料 %sx', '送り %sx', 'Подача %sx'),
    'gui.mio_icif.reactor.control.record': v('hull · 60 cycles', '堆壳 · 60 周期', '炉殻 · 60 サイクル', 'корпус · 60 циклов'),
    'gui.mio_icif.reactor.control.line_hull': v('Hull %s / %s HU (%s) · %s EU/t · feed %sx', '堆壳 %s / %s HU（%s）· %s EU/t · 进料 %sx',
        '炉殻 %s / %s HU（%s）· %s EU/t · 送り %sx', 'Корпус %s / %s HU (%s) · %s EU/t · подача %sx'),
    'gui.mio_icif.reactor.control.line_balance': v('Last cycle: fuel %s HU, vented %s, hull %s', '上个周期：燃料产热 %s HU，排走 %s，堆壳 %s',
        '前サイクル：燃料 %s HU、排熱 %s、炉殻 %s', 'Прошлый цикл: топливо %s HU, отвод %s, корпус %s'),
    'gui.mio_icif.reactor.control.eta': v('%s s to the setpoint', '%s 秒到设定', '設定まで %s 秒', '%s с до уставки'),
    'gui.mio_icif.reactor.control.eta_none': v('not rising', '没有升温', '昇温していない', 'не растёт'),
    'gui.mio_icif.reactor.control.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.reactor.control.no_output': v('No output · no fuel or no signal', '没有输出 · 没有燃料或红石信号', '出力なし · 燃料か信号がない', 'Нет выхода · нет топлива или сигнала'),
    'gui.mio_icif.reactor.control.hottest': v('hottest row %s col %s at %s', '最热：第 %s 行第 %s 列 %s', '最も熱い：%s 行 %s 列 %s', 'горячее всего: ряд %s, столбец %s — %s'),
    'gui.mio_icif.reactor.control.tip_scram1': v('SCRAM stops the fuel at once (from the next cycle); the reactor stays tripped until reset.',
        'SCRAM 立即让燃料停止（从下一个周期起）；复位前反应堆保持跳堆。', 'SCRAM で燃料はすぐ止まる（次のサイクルから）。リセットするまでトリップのまま。',
        'SCRAM сразу останавливает топливо (со следующего цикла); реактор остаётся остановленным до сброса.'),
    'gui.mio_icif.reactor.control.tip_scram2': v('Trip at: the reactor trips by itself when the hull heat reaches this share (- and + by 5 %, 100 % is off).',
        '跳堆设定：堆壳热到这个比例时自动跳堆（- 和 + 每次 5%，100% 为关闭）。', 'トリップ設定：炉殻の熱がこの割合に達すると自動でトリップ（- と + で 5% ずつ、100% は切）。',
        'Уставка: реактор сам останавливается, когда нагрев корпуса доходит до этой доли (- и + по 5 %, 100 % — выкл.).'),
    'gui.mio_icif.reactor.control.tip_scram3': v('Reset works only below the setpoint; a redstone signal turned off and on resets it too.',
        '只有堆壳热低于设定时才能复位；把红石信号关掉再打开也能复位。', 'リセットは設定未満のときだけ。レッドストーン信号を切って入れ直してもリセットできる。',
        'Сброс возможен только ниже уставки; выключение и включение сигнала тоже сбрасывает.'),
    'gui.mio_icif.reactor.control.tip_record1': v('Orange: the hull heat of the last 60 cycles (a cycle is a second); green: the output; red dashes: the setpoint.',
        '橙线：最近 60 个周期的堆壳热（每周期 1 秒）；绿线：输出；红虚线：跳堆设定。', '橙：直近 60 サイクルの炉殻の熱（1 サイクル 1 秒）、緑：出力、赤の破線：トリップ設定。',
        'Оранжевая — нагрев корпуса за 60 циклов (цикл — секунда); зелёная — выход; красный пунктир — уставка.'),
    'gui.mio_icif.reactor.control.tip_record2': v('The outlined slots are the three hottest parts (green, amber above 50 %, red above 80 %).',
        '描边的格子是最热的三个元件（绿色，50% 以上琥珀色，80% 以上红色）。', '枠で囲まれたマスは最も熱い 3 つの部品（緑、50% 超で琥珀、80% 超で赤）。',
        'Обведены три самые горячие детали (зелёный, выше 50 % — жёлтый, выше 80 % — красный).'),
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
