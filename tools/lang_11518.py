#!/usr/bin/env python3
"""0.1.11.18 language values (en_us / zh_cn / ja_jp / ru_ru): the water kinetic generator's rotor cross-section screen and
the water generator's screen: labels, reading lines, states, tooltips.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11518.py <repo root> [--check]
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
    'gui.mio_icif.water.no_rotor': v('no rotor', '没有转子', 'ローターなし', 'нет ротора'),
    'gui.mio_icif.water.flow': v('Flow', '流速', '流速', 'Поток'),
    'gui.mio_icif.water.disc': v('Water', '水量', '水量', 'Вода'),
    'gui.mio_icif.water.spring': v('spring tide', '大潮', '大潮', 'сизигийный'),
    'gui.mio_icif.water.neap': v('neap tide', '小潮', '小潮', 'квадратурный'),
    'gui.mio_icif.water.mid': v('mean tide', '中潮', '中潮', 'средний'),
    'gui.mio_icif.water.source.none': v('still water', '静水', '静水', 'стоячая вода'),
    'gui.mio_icif.water.source.river': v('River current', '河流', '河の流れ', 'Течение реки'),
    'gui.mio_icif.water.source.ocean': v('Tidal current', '潮流', '潮流', 'Приливное течение'),
    'gui.mio_icif.water.source.canal': v('Flowing water', '流水渠', '流水', 'Проточная вода'),
    'gui.mio_icif.water.line_source': v('%s · %s m/s · disc %s/%s water (%s)', '%s · %s m/s · 盘面 %s/%s 格是水（%s）', '%s · %s m/s · 円盤 %s/%s マスが水（%s）', '%s · %s м/с · диск %s/%s в воде (%s)'),
    'gui.mio_icif.water.line_output': v('%s KU/t · %s at the mean flow · diameter %s · wake %s', '%s KU/t · 平均水流时 %s · 直径 %s · 尾流 %s',
        '%s KU/t · 平均流で %s · 直径 %s · 後流 %s', '%s KU/t · при среднем потоке %s · диаметр %s · след %s'),
    'gui.mio_icif.water.state.no_rotor': v('Stopped · no rotor', '停转 · 没有转子', '停止 · ローターなし', 'Остановлен · нет ротора'),
    'gui.mio_icif.water.state.not_in_water': v('Stopped · no water in front', '停转 · 前方没有水', '停止 · 前に水がない', 'Остановлен · спереди нет воды'),
    'gui.mio_icif.water.state.still': v('Stopped · the water does not move (a river, the sea or flowing water is needed)', '停转 · 水不流动（需要河流、海洋或流动的水）',
        '停止 · 水が動かない（川・海・流水が必要）', 'Остановлен · вода не движется (нужна река, море или проточная вода)'),
    'gui.mio_icif.water.state.slack': v('Slack water · the tide is turning', '转潮 · 潮流正在转向', '憩潮 · 潮が変わるところ', 'Стоячая вода · прилив меняется'),
    'gui.mio_icif.water.state.blocked': v('Running · part of the disc is not water', '运行 · 盘面有一部分不是水', '運転中 · 円盤の一部が水でない', 'Работает · часть диска не в воде'),
    'gui.mio_icif.water.state.wake': v('Running · in the wake of a rotor in front', '运行 · 处在前方转子的尾流里', '運転中 · 前のローターの後流の中', 'Работает · в следе ротора спереди'),
    'gui.mio_icif.water.state.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.water.state.full': v('Stopped · storage full', '停机 · 储电已满', '停止 · 蓄電満杯', 'Остановлен · накопитель полон'),
    'gui.mio_icif.water.rotor': v('Rotor', '转子', 'ローター', 'Ротор'),
    'gui.mio_icif.water.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.water.tip1': v('The disc is the layer of blocks in front, seen from upstream: blue is moving water, dark blue still water, white air, grey solid blocks.',
        '盘面是前方那一层方块（从上游看过去）：蓝色是流动的水，深蓝是静水，白色是空气，灰色是实心方块。',
        '円盤は前の 1 層のブロック（上流から見た図）：青は動く水、濃い青は静水、白は空気、灰色は固体ブロック。',
        'Диск — слой блоков спереди, вид с верховья: синий — текущая вода, тёмно-синий — стоячая, белый — воздух, серый — твёрдые блоки.'),
    'gui.mio_icif.water.tip2': v('Power grows with the cube of the flow, over the cells of the disc that are water: sink the rotor fully.',
        '功率随流速的三次方增长，只算盘面里是水的格：把转子完全沉入水中。', '出力は流速の 3 乗に比例し、水のマスだけが数えられる。ローターを完全に沈めること。',
        'Мощность растёт как куб скорости, считаются только клетки с водой: погрузите ротор полностью.'),
    'gui.mio_icif.water.tip3': v('Rivers flow steadily (more in rain); the sea turns twice a day with the tide, spring tides at full and new moon.',
        '河流稳定（下雨更大）；海洋随潮汐一天转向两次，满月和新月是大潮。', '川は一定（雨で増える）。海は潮で 1 日 2 回向きが変わり、満月と新月は大潮。',
        'Реки текут ровно (в дождь сильнее); море дважды в день меняет течение, сизигийные приливы в полнолуние и новолуние.'),
    'gui.mio_icif.water.tip4': v('Flowing water anywhere counts along the rotor\'s axis; a rotor in line in front takes some of the flow.',
        '任何地方的流动水按沿转子轴向的分量计算；前方同一直线上的转子会挡掉一部分水流。', 'どこでも流水はローター軸方向の成分で数える。前に並んだローターは流れの一部を取る。',
        'Проточная вода где угодно считается вдоль оси ротора; ротор впереди на той же линии забирает часть потока.'),
    'gui.mio_icif.water.in_front': v('in front', '前方', '前', 'спереди'),
    'gui.mio_icif.water.gen_front': v('Water in front: %s · %s m/s → %s EU/t', '前方的水：%s · %s m/s → %s EU/t', '前の水：%s · %s m/s → %s EU/t', 'Вода спереди: %s · %s м/с → %s EU/t'),
    'gui.mio_icif.water.gen_tank': v('Tank %s / %s mB → 4 EU/t at 1 mB/t', '水罐 %s / %s mB → 4 EU/t，每 tick 1 mB', 'タンク %s / %s mB → 4 EU/t、1 mB/t', 'Бак %s / %s mB → 4 EU/t при 1 mB/t'),
    'gui.mio_icif.water.gen_tip1': v('Moving water in front gives up to 4 EU/t by its speed (a river, the tide, flowing water or a waterfall); still water gives nothing.',
        '前方流动的水按流速最多给 4 EU/t（河流、潮流、流动水或瀑布）；静水不发电。', '前の動く水は流速に応じて最大 4 EU/t（川・潮流・流水・滝）。静水は発電しない。',
        'Текущая вода спереди даёт до 4 EU/t по скорости (река, прилив, проточная вода, водопад); стоячая — ничего.'),
    'gui.mio_icif.water.gen_tip2': v('Water in the tank (buckets or pipes) gives 4 EU/t more, using 1 mB a tick.', '水罐里的水（水桶或管道）再给 4 EU/t，每 tick 用 1 mB。',
        'タンクの水（バケツか配管）でさらに 4 EU/t、1 tick に 1 mB 使う。', 'Вода в баке (вёдра или трубы) даёт ещё 4 EU/t, расходуя 1 mB за тик.'),
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
