#!/usr/bin/env python3
"""0.1.11.14 language values (en_us / zh_cn / ja_jp / ru_ru): the solar panel screen (pyranometer record, readings,
shade and states, stored energy, tooltips).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11514.py <repo root> [--check]
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
    'gui.mio_icif.solar.rated': v('rated', '满功率', '定格', 'номинал'),
    'gui.mio_icif.solar.sun_line': v('%s · %s W/m² (clear sky %s)', '%s · 辐照 %s W/m²（晴空 %s）', '%s · 日射 %s W/m²（快晴 %s）', '%s · %s Вт/м² (ясно %s)'),
    'gui.mio_icif.solar.env_line': v('Clouds ×%s · cell %s °C ×%s', '云量 ×%s · 板温 %s °C ×%s', '雲 ×%s · セル %s °C ×%s', 'Облака ×%s · элемент %s °C ×%s'),
    'gui.mio_icif.solar.shade.open': v('open sky', '露天', '空が開けている', 'открытое небо'),
    'gui.mio_icif.solar.shade.diffuse': v('covered: diffuse light only', '被遮挡：只有散射光', '遮られている：散乱光のみ', 'под навесом: только рассеянный свет'),
    'gui.mio_icif.solar.shade.dark': v('no sky light', '没有天光', '空の光がない', 'нет света неба'),
    'gui.mio_icif.solar.shade.no_sky': v('no sky in this dimension', '这个维度没有天空', 'この次元に空はない', 'в этом измерении нет неба'),
    'gui.mio_icif.solar.out_line': v('%s / %s EU/t · today %s EU', '输出 %s / %s EU/t · 今日 %s EU', '出力 %s / %s EU/t · 今日 %s EU', '%s / %s EU/t · сегодня %s EU'),
    'gui.mio_icif.solar.state.core': v('night core', '夜间核心', '夜間コア', 'ночное ядро'),
    'gui.mio_icif.solar.state.night': v('night', '夜间', '夜間', 'ночь'),
    'gui.mio_icif.solar.state.none': v('no light', '无光照', '光がない', 'нет света'),
    'gui.mio_icif.solar.state.full': v('storage full', '储电已满', '蓄電満杯', 'накопитель полон'),
    'gui.mio_icif.solar.state.clipped': v('full output (clipped)', '满功率（限幅）', '定格出力（クリップ）', 'полная мощность (ограничено)'),
    'gui.mio_icif.solar.state.run': v('generating', '发电中', '発電中', 'выработка'),
    'gui.mio_icif.solar.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.solar.tip.1': v("Pyranometer record of today: the sunlight on the panel (amber) and the output (blue, rated output at the top).",
        '日射计今日记录：照到板上的阳光（黄）与输出（蓝，顶端为满功率）。', '本日の日射計記録：パネルに当たる日射（黄）と出力（青、上端が定格）。',
        'Запись пиранометра за сегодня: солнце на панели (жёлтый) и выработка (синий, вверху номинал).'),
    'gui.mio_icif.solar.tip.2': v('Dashed: a clear day. Gaps under it are clouds or shade.', '虚线：晴天应有的辐照。比它低的部分是云或遮挡。', '破線：快晴の日の日射。それより低い部分は雲か影。', 'Пунктир: ясный день. Провалы под ним — облака или тень.'),
    'gui.mio_icif.solar.tip.3': v('Dotted: 700 W/m². Above it the panel is at its rated output (the inverter clips).', '点线：700 W/m²。高于它时板子按满功率输出（逆变器限幅）。', '点線：700 W/m²。それを超えるとパネルは定格出力になる（インバーターのクリップ）。', 'Точки: 700 Вт/м². Выше панель даёт номинал (инвертор ограничивает).'),
    'gui.mio_icif.solar.tip.4': v('The sun is weak in the morning and evening (more air to pass through); rain keeps about 70 %%, a thunderstorm about 37 %%.',
        '早晚太阳光要穿过更厚的大气，所以弱；下雨约剩 70%%，雷暴约剩 37%%。', '朝夕は通る大気が厚いので日射が弱い。雨では約 70%%、雷雨では約 37%% が残る。',
        'Утром и вечером солнце слабее (больше воздуха на пути); дождь оставляет около 70 %%, гроза около 37 %%.'),
    'gui.mio_icif.solar.tip.5': v('A hot panel makes less: −0.4 %% per °C above 25 °C. Deserts are sunny but hot.', '板子越热发电越少：25 °C 以上每升 1 °C 少 0.4%%。沙漠阳光足但温度高。', 'パネルが熱いほど出力は下がる：25 °C を超えると 1 °C ごとに −0.4%%。砂漠は日射が強いが暑い。', 'Горячая панель даёт меньше: −0,4 %% на °C выше 25 °C. В пустыне солнечно, но жарко.'),
    'gui.mio_icif.solar.tip.6': v('A panel needs open sky straight up; under cover it gets only the diffuse light. A night core runs at night under open sky.',
        '正上方需要露天；被遮住时只有散射光。夜间核心在夜里、露天时工作。', '真上が空に開けている必要がある。遮られると散乱光のみ。夜間コアは夜、空が開けているときに動く。',
        'Панели нужно открытое небо прямо над ней; под навесом только рассеянный свет. Ночное ядро работает ночью под открытым небом.'),
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
