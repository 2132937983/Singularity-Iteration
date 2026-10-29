#!/usr/bin/env python3
"""0.1.10.37 language values (en_us / zh_cn / ja_jp / ru_ru): the JEI category "Nuclear Fuel Breeding & Decay"
(core.api.reactor.ReactorTransformationRegistry) - its title, the process line of each entry and the notes SI writes for its own
components (ReactorInventory.transformations).
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
