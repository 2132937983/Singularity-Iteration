#!/usr/bin/env python3
"""0.1.11.19 language values (en_us / zh_cn / ja_jp / ru_ru): the recorder screens of the geothermal generator, the item
generators (incinerators) and the experience generators: labels, reading lines, states, tooltips.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11519.py <repo root> [--check]
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
    'gui.mio_icif.geo.depth': v('depth ×', '深度 ×', '深さ ×', 'глубина ×'),
    'gui.mio_icif.geo.nether': v('Nether ×1.5', '下界 ×1.5', 'ネザー ×1.5', 'Незер ×1.5'),
    'gui.mio_icif.geo.tank_part': v('+ tank %s EU/t', '+ 岩浆罐 %s EU/t', '+ タンク %s EU/t', '+ бак %s EU/t'),
    'gui.mio_icif.geo.line_world': v('Lava around: %s sources, %s flowing → %s EU/t (at most %s)', '周围岩浆：%s 格源、%s 格流动 → %s EU/t（最多 %s）',
        '周囲の溶岩：源 %s・流れ %s → %s EU/t（最大 %s）', 'Лава вокруг: %s источников, %s текущих → %s EU/t (до %s)'),
    'gui.mio_icif.geo.line_tank': v('Tank %s / %s mB → 20 EU/t · stored %s / %s EU', '岩浆罐 %s / %s mB → 20 EU/t · 储电 %s / %s EU',
        'タンク %s / %s mB → 20 EU/t · 蓄電 %s / %s EU', 'Бак %s / %s mB → 20 EU/t · заряд %s / %s EU'),
    'gui.mio_icif.geo.state.full': v('Waiting · storage full', '等待 · 储电已满', '待機 · 蓄電満杯', 'Ожидание · накопитель полон'),
    'gui.mio_icif.geo.state.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.geo.state.no_lava': v('Idle · no lava in the tank or around', '待机 · 罐里和周围都没有岩浆', '待機 · タンクにも周囲にも溶岩がない', 'Ожидание · нет лавы ни в баке, ни вокруг'),
    'gui.mio_icif.geo.tank': v('Lava', '岩浆', '溶岩', 'Лава'),
    'gui.mio_icif.geo.tip1': v('Lava within 2 blocks heats it: touching a face 1 EU/t, further 0.5; flowing lava counts half.',
        '2 格内的岩浆给它热：紧贴一面的 1 EU/t，更远的 0.5；流动的岩浆减半。', '2 ブロック以内の溶岩が熱を与える：面に接するもの 1 EU/t、それより遠いもの 0.5、流れる溶岩は半分。',
        'Лава в пределах 2 блоков греет его: касающаяся грани 1 EU/t, дальше 0,5; текущая — вдвое меньше.'),
    'gui.mio_icif.geo.tip2': v('Deeper is hotter: ×1 at Y 64 and above, ×2 at Y -64; ×1.5 in the Nether; at most 12 EU/t from the lava around.',
        '越深越热：Y 64 以上 ×1，Y -64 为 ×2；下界 ×1.5；周围岩浆最多 12 EU/t。', '深いほど熱い：Y 64 以上 ×1、Y -64 で ×2、ネザー ×1.5。周囲の溶岩は最大 12 EU/t。',
        'Чем глубже, тем горячее: ×1 на Y 64 и выше, ×2 на Y -64; ×1,5 в Незере; от лавы вокруг до 12 EU/t.'),
    'gui.mio_icif.geo.tip3': v('Lava in the tank (buckets, cells, pipes) gives 20 EU/t at 10 EU per mB.', '罐里的岩浆（桶、单元、管道）给 20 EU/t，每 mB 10 EU。',
        'タンクの溶岩（バケツ・セル・配管）で 20 EU/t、1 mB あたり 10 EU。', 'Лава в баке (вёдра, капсулы, трубы) даёт 20 EU/t, 10 EU на mB.'),
    'gui.mio_icif.incinerator.eff': v('efficiency %s', '效率 %s', '効率 %s', 'КПД %s'),
    'gui.mio_icif.incinerator.ash_short': v('ash %s / 64', '灰 %s / 64', '灰 %s / 64', 'зола %s / 64'),
    'gui.mio_icif.incinerator.line_chamber': v('Chamber %s °C · efficiency %s · design 850 °C, out below 300 °C', '炉膛 %s °C · 效率 %s · 设计 850 °C，300 °C 以下熄火',
        '炉内 %s °C · 効率 %s · 設計 850 °C、300 °C 未満で消火', 'Топка %s °C · КПД %s · расчёт 850 °C, ниже 300 °C гаснет'),
    'gui.mio_icif.incinerator.line_fuel': v('Fuel %s ticks · %s EU a tick at 850 °C · range %s', '燃料 %s tick · 850 °C 时每 tick %s EU · 范围 %s',
        '燃料 %s tick · 850 °C で 1 tick %s EU · 範囲 %s', 'Топливо %s тиков · %s EU за тик при 850 °C · радиус %s'),
    'gui.mio_icif.incinerator.state.ash_full': v('Stopped · the ash pit is full (take the ash out)', '停机 · 灰斗满了（把灰取出）', '停止 · 灰受けが満杯（灰を取り出す）', 'Остановлен · зольник полон (заберите золу)'),
    'gui.mio_icif.incinerator.state.full': v('Waiting · storage full', '等待 · 储电已满', '待機 · 蓄電満杯', 'Ожидание · накопитель полон'),
    'gui.mio_icif.incinerator.state.no_fuel': v('Idle · nothing to burn', '待机 · 没有东西可烧', '待機 · 燃やすものがない', 'Ожидание · нечего жечь'),
    'gui.mio_icif.incinerator.state.igniting': v('Igniting · the chamber is too cold to make power', '点火中 · 炉膛太冷，还不发电', '点火中 · 炉が冷たく発電しない', 'Розжиг · топка слишком холодна'),
    'gui.mio_icif.incinerator.state.warming': v('Warming up · below the design temperature', '升温中 · 未到设计温度', '昇温中 · 設計温度未満', 'Прогрев · ниже расчётной температуры'),
    'gui.mio_icif.incinerator.state.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.incinerator.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.incinerator.tip1': v('Items within range are drawn in and burnt; burning ones heat the chamber, others (stone, dirt) cool it by 25 °C each.',
        '范围内的物品被吸进来烧掉；可燃的把炉膛烧热，不可燃的（石头、泥土）每件让炉膛降 25 °C。', '範囲内のアイテムを吸い込んで燃やす。燃えるものは炉を温め、燃えないもの（石・土）は 1 個ごとに 25 °C 冷やす。',
        'Предметы в радиусе втягиваются и сгорают; горючие греют топку, негорючие (камень, земля) охлаждают на 25 °C каждый.'),
    'gui.mio_icif.incinerator.tip2': v('Power follows the chamber: nothing below 300 °C, all of it at 850 °C. Orange marks: burning items; grey: others.',
        '发电随炉温：300 °C 以下没有，850 °C 全额。橙色记号是可燃物，灰色是不可燃物。', '発電は炉温に従う：300 °C 未満はなし、850 °C で全量。橙の印は燃えるもの、灰色はそれ以外。',
        'Мощность следует за топкой: ниже 300 °C ничего, при 850 °C полностью. Оранжевые метки — горючее, серые — прочее.'),
    'gui.mio_icif.incinerator.tip3': v('Ash collects in the pit (64); take it out by hand, hopper or pipe, or the generator stops.',
        '灰会积在灰斗里（64）；用手、漏斗或管道取出，否则停机。', '灰は灰受け（64）にたまる。手・ホッパー・配管で取り出さないと停止する。',
        'Зола копится в зольнике (64); забирайте её вручную, воронкой или трубой, иначе генератор встанет.'),
    'gui.mio_icif.experience.stock': v('XP %s', '经验 %s', '経験値 %s', 'Опыт %s'),
    'gui.mio_icif.experience.range': v('range %s', '范围 %s', '範囲 %s', 'радиус %s'),
    'gui.mio_icif.experience.line_rate': v('%s EU/t · %s EU per experience point', '%s EU/t · 每点经验 %s EU', '%s EU/t · 経験値 1 あたり %s EU', '%s EU/t · %s EU за очко опыта'),
    'gui.mio_icif.experience.line_stock': v('Stocked %s XP (%s EU) · range %s', '存量 %s 点经验（%s EU）· 范围 %s', '蓄積 %s 経験値（%s EU）· 範囲 %s', 'Запас %s опыта (%s EU) · радиус %s'),
    'gui.mio_icif.experience.state.full': v('Waiting · storage full', '等待 · 储电已满', '待機 · 蓄電満杯', 'Ожидание · накопитель полон'),
    'gui.mio_icif.experience.state.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.experience.state.no_xp': v('Idle · no experience orbs nearby', '待机 · 附近没有经验球', '待機 · 近くに経験値オーブがない', 'Ожидание · рядом нет сфер опыта'),
    'gui.mio_icif.experience.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.experience.tip1': v('Experience orbs within range are drawn in; each point runs it for a tick. Green marks: orbs taken.',
        '范围内的经验球被吸进来；每点经验发电一 tick。绿色记号表示吸入了经验球。', '範囲内の経験値オーブを吸い込む。1 ポイントで 1 tick 発電する。緑の印はオーブを取り込んだ時。',
        'Сферы опыта в радиусе втягиваются; каждое очко — один тик работы. Зелёные метки — взятые сферы.'),
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
