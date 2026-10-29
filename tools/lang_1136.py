#!/usr/bin/env python3
"""0.1.10.36 language values (en_us / zh_cn / ja_jp / ru_ru).
Phase 1: the doubled words of the Chinese weapon and laser tower names (激光镭射 -> 激光, 等离子电浆 -> 等离子), the renamed
materials (Coal Chunk -> Graphite Block, Nano Living Metal -> Nano Superfluid Metal, Neutron Plate -> Degenerate Zero
Matter) and the advanced battery / advanced charging battery name collision.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1136.py <repo root> [--check]
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
    # ---- 1.1 doubled words in Chinese names (the Mining Laser and the CF Sprayer keep theirs)
    'item.mio_icif.item_tool_electric_rifle': v(zh='电力激光步枪'),
    'item.mio_icif.item_tool_advanced_electric_rifle': v(zh='进阶电力激光步枪'),
    'item.mio_icif.item_tool_tactical_laser_rifle': v(zh='铱制刺刀战术激光步枪'),
    'item.mio_icif.item_tool_electric_plasma_gun': v(zh='电力等离子发射器'),
    'block.mio_icif.producer.block_laser_defense_tower': v(zh='激光防御塔'),
    'block.mio_icif.producer.block_sky_patrol_laser_tower': v(zh='巡天激光防御站'),
    'container.mio_icif.laser_defense_tower': v(zh='激光防御塔'),
    'container.mio_icif.sky_patrol_laser_tower': v(zh='巡天激光防御站'),
    'death.attack.laser_tower': v(zh='%1$s被激光防御塔汽化了'),
    'death.attack.laser_tower.player': v(zh='%1$s在与%2$s战斗时被激光防御塔汽化了'),
    # ---- 1.1 renamed materials (registry ids unchanged)
    'item.mio_icif.resource.item_coal_chunk': v('Graphite Block', '石墨块', '黒鉛ブロック', 'Графитовый блок'),
    'item.mio_icif.resource.item_nano_living_metal': v(
        'Nano Superfluid Metal', '纳米超流体金属', 'ナノ超流動金属', 'Нано-сверхтекучий металл'),
    'item.mio_icif.resource.item_neutron_plate': v(
        'Degenerate Zero Matter', '简并态零素', '縮退ゼロ物質', 'Вырожденная нуль-материя'),
    'module.mio_icif.tooltip.mk2_available': v(
        'Mk.II available (Living Circuit + Degenerate Zero Matter)', '可升级为 Mk.II（生体电路 + 简并态零素）',
        'Mk.II あり（生体回路＋縮退ゼロ物質）'),
    # ---- 1.1 advanced battery (advbat) vs advanced charging battery (advchargebat)
    'item.mio_icif.normal.item_advbat_lev0': v('Advanced Battery', '高级电池', '高度バッテリー', 'Продвинутая батарея'),
    'item.mio_icif.normal.item_advchargebat_lev0': v(
        'Advanced Charging Battery', '高级充电电池', '高度充電バッテリー', 'Продвинутая зарядная батарея'),
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
