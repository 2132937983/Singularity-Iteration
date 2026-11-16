#!/usr/bin/env python3
"""0.1.11.15 language values (en_us / zh_cn / ja_jp / ru_ru): the engine screen of the semifluid, advanced semifluid
and diesel generators (engine room monitor, readings, states, stored energy, tooltips).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11515.py <repo root> [--check]
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
    'gui.mio_icif.engine.egt': v('Exhaust', '排温', '排気', 'Выхлоп'),
    'gui.mio_icif.engine.tank_labels': v('W  F', '水 油', '水 油', 'В  Т'),
    'gui.mio_icif.engine.load': v('load %s', '负荷 %s', '負荷 %s', 'нагрузка %s'),
    'gui.mio_icif.engine.egt_value': v('exh. %s °C', '排温 %s °C', '排気 %s °C', 'выхл. %s °C'),
    'gui.mio_icif.engine.coolant': v('cool. %s °C', '水温 %s °C', '水温 %s °C', 'охл. %s °C'),
    'gui.mio_icif.engine.out_line': v('Output %s / %s EU/t · fuel %s mB/t', '输出 %s / %s EU/t · 油耗 %s mB/t', '出力 %s / %s EU/t · 燃料 %s mB/t', 'Выход %s / %s EU/t · топливо %s mB/t'),
    'gui.mio_icif.engine.fuel_line': v('Efficiency %s · %s EU/mB', '效率 %s · %s EU/mB', '効率 %s · %s EU/mB', 'КПД %s · %s EU/mB'),
    'gui.mio_icif.engine.no_fuel': v('no fuel', '无燃料', '燃料なし', 'нет топлива'),
    'gui.mio_icif.engine.state.stopped_full': v('Stopped · storage full (restarts below 60 %%)', '停机 · 储电已满（60%% 以下再启动）', '停止 · 蓄電満杯（60%% 未満で再始動）', 'Остановлен · накопитель полон (пуск ниже 60 %%)'),
    'gui.mio_icif.engine.state.stopped_fuel': v('Stopped · no fuel', '停机 · 没有燃料', '停止 · 燃料なし', 'Остановлен · нет топлива'),
    'gui.mio_icif.engine.state.warming': v('Warming up · a cold engine uses more fuel', '暖机中 · 冷机更费油', '暖機中 · 冷えたエンジンは燃料を多く使う', 'Прогрев · холодный двигатель расходует больше'),
    'gui.mio_icif.engine.state.low_load': v('Low load · more fuel per EU', '低负荷 · 每 EU 更费油', '低負荷 · 1 EU あたりの燃料が多い', 'Малая нагрузка · больше топлива на EU'),
    'gui.mio_icif.engine.state.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.engine.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.engine.tip.1': v('Exhaust temperature per cylinder, tachometer, coolant (W) and fuel tank (F).', '每个气缸的排气温度、转速表、水温（水）与油箱（油）。',
        '気筒ごとの排気温度、回転計、冷却水（水）と燃料タンク（油）。', 'Температура выхлопа по цилиндрам, тахометр, охлаждение (В) и бак (Т).'),
    'gui.mio_icif.engine.tip.2': v('The engine follows the grid: it makes what is taken, plus a top-up while the storage is below 90 %%.', '机组跟随电网：电网取多少就发多少，储电低于 90%% 时再多补一些。',
        'エンジンは電力網に追従する：取られた分だけ発電し、蓄電が 90%% 未満なら少し多めに補う。', 'Двигатель следует за сетью: даёт столько, сколько берут, и подзаряжает накопитель ниже 90 %%.'),
    'gui.mio_icif.engine.tip.3': v('Record of the last ~48 s: load (blue) and exhaust temperature (rust).', '最近约 48 秒的记录：负荷（蓝）与排温（锈红）。', '直近約 48 秒の記録：負荷（青）と排気温度（錆色）。', 'Запись за ~48 с: нагрузка (синий) и температура выхлопа (ржавый).'),
    'gui.mio_icif.engine.tip.4': v('Fuel per EU is lowest near 80 %% load and rises at part load; a cold engine has 80 %% of its efficiency.', '约 80%% 负荷时每 EU 最省油，负荷越低越费油；冷机只有 80%% 的效率。',
        '1 EU あたりの燃料は負荷 80%% 付近で最少、低負荷ほど増える。冷えたエンジンの効率は 80%%。', 'Расход на EU минимален около 80 %% нагрузки и растёт при малой; холодный двигатель — 80 %% КПД.'),
    'gui.mio_icif.engine.tip.5': v('Efficiency 100 %% is the fuel value at full load; near 80 %% load it reaches 105 %%.', '效率 100%% 即满负荷时的燃料值；约 80%% 负荷时可达 105%%。',
        '効率 100%% は全負荷時の燃料値。負荷 80%% 付近で 105%% に達する。', 'КПД 100 %% — ценность топлива при полной нагрузке; около 80 %% — до 105 %%.'),
    'gui.mio_icif.engine.tip.6': v('The engine stops at 98 %% stored and starts again below 60 %% (the two marks).', '储电到 98%% 停机，低于 60%% 再启动（两条刻线）。', '蓄電 98%% で停止し、60%% 未満で再始動する（2 本の目盛り）。', 'Двигатель останавливается на 98 %% и запускается ниже 60 %% (две отметки).'),
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
