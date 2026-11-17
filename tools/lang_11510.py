#!/usr/bin/env python3
"""0.1.11.10 language values (en_us / zh_cn / ja_jp / ru_ru): the generator (thermal) screen: chart recorder legend,
fuel queue, damper and states, readings, stored energy and tooltips.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11510.py <repo root> [--check]
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
    'gui.mio_icif.thermal.legend_temp': v('Furnace', '炉温', '炉温', 'Топка'),
    'gui.mio_icif.thermal.legend_out': v('Output', '输出', '出力', 'Выход'),
    'gui.mio_icif.thermal.per_item': v('%s EU a lump · %s t', '每块 %s EU · %s tick', '1個 %s EU · %s tick', '%s EU за штуку · %s т'),
    'gui.mio_icif.thermal.damper_open': v('Damper open', '风门开', 'ダンパー開', 'Заслонка открыта'),
    'gui.mio_icif.thermal.damper_closed': v('Damper shut', '风门关', 'ダンパー閉', 'Заслонка закрыта'),
    'gui.mio_icif.thermal.state.banked': v('banked', '封火', '埋火', 'топка притушена'),
    'gui.mio_icif.thermal.state.warming': v('warming %s', '升温 %s', '昇温 %s', 'разогрев %s'),
    'gui.mio_icif.thermal.state.full': v('full output', '满负荷', '全出力', 'полная мощность'),
    'gui.mio_icif.thermal.state.cooling': v('cooling', '冷却中', '冷却中', 'остывает'),
    'gui.mio_icif.thermal.state.cold': v('cold', '熄火', '消火', 'холодная'),
    'gui.mio_icif.thermal.temp_line': v('%s K · %s EU/t', '炉温 %s K · %s EU/t', '炉温 %s K · %s EU/t', '%s K · %s EU/t'),
    'gui.mio_icif.thermal.fuel_line': v('Fuel ≈ %s EU', '存煤 ≈ %s EU', '燃料 ≈ %s EU', 'Топливо ≈ %s EU'),
    'gui.mio_icif.thermal.time_line': v('%s:%s at full output', '满负荷可烧 %s:%s', '全出力で %s:%s', 'на полной %s:%s'),
    'gui.mio_icif.thermal.no_fuel': v('No fuel', '没有燃料', '燃料なし', 'Нет топлива'),
    'gui.mio_icif.thermal.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.thermal.tip.chart1': v('Chart recorder: one turn of the paper is a game day; a point every 15 seconds.',
        '圆盘记录纸：纸盘一圈是一个游戏日，每 15 秒记一个点。', '円形記録紙：1 周がゲーム内の 1 日、15 秒ごとに 1 点記録します。',
        'Круговой самописец: оборот диска — игровые сутки, точка каждые 15 секунд.'),
    'gui.mio_icif.thermal.tip.chart2': v('Red pen: furnace temperature (rim 1500 K). Blue pen: output (rim full output).',
        '红笔：炉温（外圈 1500 K）。蓝笔：输出功率（外圈为满功率）。', '赤ペン：炉温（外周 1500 K）。青ペン：出力（外周が全出力）。',
        'Красное перо: температура топки (край 1500 K). Синее перо: мощность (край — полная).'),
    'gui.mio_icif.thermal.tip.chart3': v('The pens stay at twelve o\'clock and the paper turns; older records lie further anticlockwise.',
        '笔尖固定在 12 点方向，纸盘转动，越往逆时针方向记录越早。', 'ペンは 12 時の位置に固定され、紙が回ります。反時計回りに行くほど古い記録です。',
        'Перья стоят на двенадцати, диск вращается; чем дальше против часовой, тем старше запись.'),
    'gui.mio_icif.thermal.tip.fuel1': v('Fuel queue: the first box is the lump burning, the others the fuel slot.',
        '燃料队列：第一格是正在烧的那块，其余是燃料槽里的。', '燃料キュー：最初の枠が燃焼中の分、残りは燃料スロットの分です。',
        'Очередь топлива: первая ячейка — горящая порция, остальные — слот топлива.'),
    'gui.mio_icif.thermal.tip.fuel2': v('A cold furnace burns its fuel slower and gives less: each lump still gives the same energy.',
        '炉子没热透时燃料烧得慢、出力低，每块燃料的总发电量不变。', '炉が冷えているうちは燃料の減りも出力も遅く、1 個あたりの総発電量は変わりません。',
        'Холодная топка горит медленнее и даёт меньше; энергия с каждой порции та же.'),
    'gui.mio_icif.thermal.tip.damper1': v('When the storage is full the damper shuts: the fuel waits instead of burning away, the fire is banked.',
        '储电满了风门自动关闭：燃料暂停、不再白烧，炉火封住保温。', '蓄電が満杯になるとダンパーが閉じ、燃料は無駄に燃えず、火は埋火で保温されます。',
        'Когда накопитель полон, заслонка закрывается: топливо ждёт, а не сгорает впустую, огонь притушен.'),
    'gui.mio_icif.thermal.tip.damper2': v('Below 85 %% stored the damper opens again and the furnace heats back to full output.',
        '储电降到 85%% 以下风门重新打开，炉子升温回到满负荷。', '蓄電が 85%% を下回るとダンパーが開き、炉は全出力まで再び昇温します。',
        'Ниже 85 %% заряда заслонка открывается и топка снова разогревается до полной мощности.'),
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
