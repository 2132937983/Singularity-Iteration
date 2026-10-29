#!/usr/bin/env python3
"""0.1.10.37 language values (en_us / zh_cn / ja_jp / ru_ru): the JEI category "Nuclear Fuel Breeding & Decay"
(core.api.reactor.ReactorTransformationRegistry) - its title, the process line of each entry and the notes SI writes for its own
components (ReactorInventory.transformations); the Steam Reactor plan in "Multiblock Structures" and the Steam Reactor
layouts in "Reactor Layouts" (their steam lines).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1137.py <repo root> [--check]
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
    'jei.mio_icif.reactor_transformation': v(
        'Nuclear Fuel Breeding & Decay', '核燃料增殖与衰变', '核燃料の増殖と崩壊', 'Размножение и распад ядерного топлива'),
    # the process line: amount > 0 / amount 0
    'jei.mio_icif.reactor_transformation.depletion': v(
        'Burns out: %s cycles', '燃尽：%s 个周期', '燃え尽き：%s サイクル', 'Выгорание: %s циклов'),
    'jei.mio_icif.reactor_transformation.depletion.short': v('Burns out', '燃尽', '燃え尽き', 'Выгорание'),
    'jei.mio_icif.reactor_transformation.breeding': v(
        'Bred at %s progress', '增殖进度 %s 时转变', '増殖の進行 %s で変化', 'Превращается при прогрессе %s'),
    'jei.mio_icif.reactor_transformation.breeding.short': v('Breeding', '增殖', '増殖', 'Размножение'),
    'jei.mio_icif.reactor_transformation.decay': v(
        'Decays: %s cycles', '衰变：%s 个周期', '崩壊：%s サイクル', 'Распад: %s циклов'),
    'jei.mio_icif.reactor_transformation.decay.short': v('Decay', '衰变', '崩壊', 'Распад'),
    # notes of SI's own components
    'jei.mio_icif.reactor_transformation.factors': v(
        'EU ×%s, heat ×%s', 'EU ×%s，热量 ×%s', 'EU ×%s、熱 ×%s', 'EU ×%s, тепло ×%s'),
    'jei.mio_icif.reactor_transformation.mox': v(
        'EU rises with hull heat', 'EU 随堆温升高', 'EU は炉温とともに増える', 'EU растёт с нагревом корпуса'),
    'jei.mio_icif.reactor_transformation.breeding_gain': v(
        'Per pulse: hull heat/3000 + %s', '每次脉冲：堆温/3000 + %s', 'パルスごと：炉温/3000 + %s', 'За импульс: нагрев/3000 + %s'),
    'jei.mio_icif.reactor_transformation.breeding_heat': v(
        '+%s HU to the hull per pulse', '每次脉冲向堆体放热 %s HU', 'パルスごとに炉へ %s HU', '+%s HU корпусу за импульс'),
    'jei.mio_icif.reactor_transformation.breeder_pulse': v(
        'Rod beside it: more heat, no EU', '相邻燃料棒：热量增加，EU 不变', '隣の燃料棒：熱は増え、EU は同じ', 'Соседний ТВЭЛ: больше тепла, EU тот же'),
    'jei.mio_icif.reactor_transformation.neutron_source': v(
        'Rods beside it: +1 pulse (EU, heat)', '相邻燃料棒多一次脉冲（EU、热量）', '隣の燃料棒はパルス +1（EU・熱）', 'Соседние ТВЭЛ: +1 импульс (EU, тепло)'),
    'jei.mio_icif.reactor_transformation.decay_heat': v(
        '+%s HU to the hull per cycle', '每周期向堆体放热 %s HU', 'サイクルごとに炉へ %s HU', '+%s HU корпусу за цикл'),
    'jei.mio_icif.reactor_transformation.ejected': v(
        'Thrown out at the next cycle', '下一周期被弹出反应堆', '次のサイクルで原子炉から出る', 'Выбрасывается в следующем цикле'),
    # ---- the Steam Reactor in "Multiblock Structures" and "Reactor Layouts" (ru_ru has none of the sibling keys: left out)
    'structure.mio_icif.steam_reactor': v(
        'Steam Reactor with chambers and a turbine', '蒸汽反应堆（含反应仓与涡轮发电机）', '蒸気原子炉（チャンバーとタービン付き）'),
    'structure.mio_icif.steam_reactor.chamber': v(
        'Optional: each adds one column; water in, steam out', '可选：每个增加一列；可注水、抽蒸汽',
        '任意: 1 個で 1 列増える。給水と蒸気の出口'),
    'structure.mio_icif.steam_reactor.turbine': v(
        'Must touch the reactor itself: the reactor pushes steam into it', '必须紧贴反应堆本体：反应堆向它推送蒸汽',
        '原子炉本体に接すること: 原子炉が蒸気を送り込む'),
    'structure.mio_icif.steam_reactor.note.columns': v(
        'Each Steam Reactor Chamber on a face of the reactor adds one column of component slots: 3 + chambers, at most 9. '
        'A Reactor Chamber does not connect to a Steam Reactor.',
        '反应堆每个面上的蒸汽反应仓增加一列元件槽位：3 + 反应仓数，最多 9 列。核反应仓不连接蒸汽反应堆。',
        '原子炉の面にある蒸気原子炉チャンバー 1 個ごとに部品スロットが 1 列増える: 3 + チャンバー数、最大 9 列。原子炉チャンバーは蒸気原子炉に接続しない。'),
    'structure.mio_icif.steam_reactor.note.vents': v(
        'Inside, put Steam Vents next to the fuel rods: above 100 HU of their own heat they make steam, at the full rate at 1,100 HU. '
        'The category "Reactor Layouts" shows Steam Reactor layouts.',
        '在反应堆内把蒸汽散热片放在燃料棒旁：散热片自身热量超过 100 HU 后产生蒸汽，1,100 HU 时达到最大速率。“反应堆布局”分类中有蒸汽反应堆布局。',
        '原子炉内では燃料棒の隣にスチームベントを置く: ベント自身の熱が 100 HU を超えると蒸気を作り、1,100 HU で最大になる。カテゴリ「原子炉レイアウト」に蒸気原子炉のレイアウトがある。'),
    'structure.mio_icif.steam_reactor.note.fluids': v(
        'Pump water into the reactor or a chamber. The reactor pushes steam into adjacent machines, not into chambers: '
        'with six chambers, take the steam out of a chamber with a pipe.',
        '把水泵入反应堆或反应仓。反应堆向相邻机器推送蒸汽，不推给反应仓：装满六个反应仓时，用管道从反应仓抽出蒸汽。',
        '水を原子炉かチャンバーに送り込む。原子炉は隣の機械に蒸気を送るが、チャンバーには送らない: チャンバーが 6 個のときはパイプでチャンバーから蒸気を抜く。'),
    'reactor_layout.mio_icif.steam_starter': v('Steam: dual rod, 960 mB/s', '蒸汽：双联棒，960 mB/s', '蒸気: デュアル棒、960 mB/s'),
    'reactor_layout.mio_icif.steam_starter.note': v(
        'One dual uranium rod and four Advanced Steam Vents (they settle at 350 HU). Feeds about 24 EU/t of Steam Turbine Generators.',
        '一根双联铀燃料棒与四个高级蒸汽散热片（稳定在 350 HU）。可供约 24 EU/t 的蒸汽涡轮发电机。',
        'デュアルウラン燃料棒 1 本と高度スチームベント 4 個（350 HU で安定）。蒸気タービン発電機で約 24 EU/t。'),
    'reactor_layout.mio_icif.steam_quad': v('Steam: quad rod, 3,840 mB/s', '蒸汽：四联棒，3,840 mB/s', '蒸気: クアッド棒、3,840 mB/s'),
    'reactor_layout.mio_icif.steam_quad.note': v(
        'One quad uranium rod and four Overclocked Steam Vents (they settle at 700 HU). Feeds about 96 EU/t: two Steam Turbine Generators.',
        '一根四联铀燃料棒与四个超频蒸汽散热片（稳定在 700 HU）。可供约 96 EU/t：两台蒸汽涡轮发电机。',
        'クアッドウラン燃料棒 1 本とオーバークロックスチームベント 4 個（700 HU で安定）。約 96 EU/t: 蒸気タービン発電機 2 台。'),
    'jei.mio_icif.reactor_layout.steam_output': v(
        'Steam: %s mB/s ≈ %s EU/t in turbines · Steam Chambers: %s', '蒸汽：%s mB/s ≈ 涡轮发电机 %s EU/t · 蒸汽反应仓：%s',
        '蒸気: %s mB/s ≈ タービンで %s EU/t · 蒸気チャンバー: %s'),
    'jei.mio_icif.reactor_layout.steam_fuel': v(
        'Fuel lasts %s · %s mB of steam per load', '燃料可用 %s · 每次装填 %s mB 蒸汽', '燃料の持続 %s · 1 回分で蒸気 %s mB'),
    'jei.mio_icif.reactor_layout.steam_model': v(
        'Steam Reactor: unlimited water and steam room; a turbine makes 1 EU from 2 mB of steam.',
        '蒸汽反应堆：水与蒸汽空间不限；涡轮发电机每 2 mB 蒸汽产生 1 EU。',
        '蒸気原子炉: 水と蒸気の空きは無制限。タービンは蒸気 2 mB で 1 EU。'),
}


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
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in mine.items():
            data[key] = t
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(mine))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
