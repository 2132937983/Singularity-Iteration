#!/usr/bin/env python3
"""0.1.11.6 language values (en_us / zh_cn / ja_jp / ru_ru): the observation window of the Molecular Transformer screen
(process classes, paths, phases, sensor panels, text column, tooltips).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_1156.py <repo root> [--check]
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
    'gui.mio_icif.mt.kind.nuclear': v('Transmutation', '核嬗变', '核変換', 'Трансмутация'),
    'gui.mio_icif.mt.kind.lattice': v('Lattice rebuild', '晶格重构', '格子再構成', 'Перестройка решётки'),
    'gui.mio_icif.mt.kind.molecular': v('Molecular rebuild', '分子重组', '分子再構成', 'Молекулярная сборка'),
    'gui.mio_icif.mt.kind.exotic': v('Anomalous', '异常', '異常', 'Аномалия'),
    'gui.mio_icif.mt.kind.unknown': v('Unmarked', '未标注', '未指定', 'Не указано'),
    'gui.mio_icif.mt.state.run': v('RUN', '运行', '稼働', 'РАБОТА'),
    'gui.mio_icif.mt.state.low': v('LOW POWER', '低功率', '低出力', 'МАЛО ЭНЕРГИИ'),
    'gui.mio_icif.mt.state.idle': v('STANDBY', '待机', '待機', 'ОЖИДАНИЕ'),
    'gui.mio_icif.mt.path.strip': v('Photonuclear stripping (γ,p) ×%s', '光核剥离 (γ,p) ×%s', '光核剥離 (γ,p) ×%s', 'Фотоядерное отщепление (γ,p) ×%s'),
    'gui.mio_icif.mt.path.fuse': v('Heavy-ion fusion + neutron evaporation', '重离子熔合 + 中子蒸发', '重イオン融合 + 中性子蒸発', 'Слияние тяжёлых ионов + испарение нейтронов'),
    'gui.mio_icif.mt.path.isotope': v('Isotope exchange', '同位素交换', '同位体交換', 'Изотопный обмен'),
    'gui.mio_icif.mt.path.lattice': v('High-pressure recrystallisation', '高温高压重结晶', '高温高圧再結晶', 'Перекристаллизация под давлением'),
    'gui.mio_icif.mt.path.molecular': v('Dissociation and resynthesis', '分子解离与重组', '分子の解離と再合成', 'Диссоциация и пересинтез'),
    'gui.mio_icif.mt.path.exotic': v('No known physical process', '无已知物理过程', '既知の物理過程なし', 'Нет известного физического процесса'),
    'gui.mio_icif.mt.path.unknown': v('The recipe has no process data', '配方未标注工艺', '工程の指定なし', 'Процесс в рецепте не указан'),
    'gui.mio_icif.mt.idle.insert': v('Insert material to start', '放入原料后开始', '素材を入れると開始', 'Положите материал для запуска'),
    'gui.mio_icif.mt.idle.wait': v('Waiting for power or output space', '等待供电或输出空位', '電力か出力の空きを待機中', 'Ждёт энергию или место на выходе'),
    'gui.mio_icif.mt.idle.trap': v('Trap evacuated · B 7.00 T', '阱已抽空 · B 7.00 T', 'トラップ排気済み · B 7.00 T', 'Ловушка откачана · B 7.00 T'),
    'gui.mio_icif.mt.line.z': v('Now Z %s', '当前 Z %s', '現在 Z %s', 'Сейчас Z %s'),
    'gui.mio_icif.mt.line.lattice_note': v('Same atoms, new arrangement', '原子不变，只改排列', '原子は同じ、配列だけ変える', 'Атомы те же, меняется порядок'),
    'gui.mio_icif.mt.line.molecular_note': v('Nuclei unchanged', '原子核不变', '原子核は不変', 'Ядра не меняются'),
    'gui.mio_icif.mt.line.exotic_note': v('No real-world counterpart', '没有对应的现实过程', '現実に対応する過程なし', 'Нет реального аналога'),
    'gui.mio_icif.mt.line.rate': v('%s items/s', '%s 件/秒', '%s 個/秒', '%s шт/с'),
    'gui.mio_icif.mt.line.eu': v('%s / %s EU', '%s / %s EU', '%s / %s EU', '%s / %s EU'),
    'gui.mio_icif.mt.line.power': v('%s EU/t', '%s EU/t', '%s EU/t', '%s EU/t'),
    'gui.mio_icif.mt.line.pulse': v('Pulse %s', '脉冲 %s', 'パルス %s', 'Импульс %s'),
    'gui.mio_icif.mt.line.press': v('Press %s', '压机 %s', 'プレス %s', 'Пресс %s'),
    'gui.mio_icif.mt.line.per_item_s': v('%s s each', '单件 %s 秒', '1個 %s 秒', '%s с/шт'),
    'gui.mio_icif.mt.line.per_item_min': v('%s min each', '单件 %s 分', '1個 %s 分', '%s мин/шт'),
    'gui.mio_icif.mt.phase.continuous': v('Continuous run', '连续运转', '連続運転', 'Непрерывный режим'),
    'gui.mio_icif.mt.phase.nuclear.0': v('Ionise', '解离', '電離', 'Ионизация'),
    'gui.mio_icif.mt.phase.nuclear.1': v('Trap', '约束', '捕捉', 'Захват'),
    'gui.mio_icif.mt.phase.nuclear.2': v('Transmute', '嬗变', '変換', 'Трансмутация'),
    'gui.mio_icif.mt.phase.nuclear.3': v('Deposit', '沉积', '堆積', 'Осаждение'),
    'gui.mio_icif.mt.phase.lattice.0': v('Heat', '加热', '加熱', 'Нагрев'),
    'gui.mio_icif.mt.phase.lattice.1': v('Press', '加压', '加圧', 'Сжатие'),
    'gui.mio_icif.mt.phase.lattice.2': v('Nucleate', '成核', '核生成', 'Зарождение'),
    'gui.mio_icif.mt.phase.lattice.3': v('Grow', '生长', '成長', 'Рост'),
    'gui.mio_icif.mt.phase.molecular.0': v('Split', '解离', '解離', 'Распад'),
    'gui.mio_icif.mt.phase.molecular.1': v('Sort', '分选', '選別', 'Отбор'),
    'gui.mio_icif.mt.phase.molecular.2': v('Synthesise', '合成', '合成', 'Синтез'),
    'gui.mio_icif.mt.phase.molecular.3': v('Crystallise', '结晶', '結晶', 'Кристаллизация'),
    'gui.mio_icif.mt.phase.exotic.0': v('Ionise', '解离', '電離', 'Ионизация'),
    'gui.mio_icif.mt.phase.exotic.1': v('Trap', '约束', '捕捉', 'Захват'),
    'gui.mio_icif.mt.phase.exotic.2': v('???', '???', '???', '???'),
    'gui.mio_icif.mt.phase.exotic.3': v('Deposit', '沉积', '堆積', 'Осаждение'),
    'gui.mio_icif.mt.r.z': v('Z', 'Z', 'Z', 'Z'),
    'gui.mio_icif.mt.r.spectrum': v('MS', '质谱', '質量', 'МС'),
    'gui.mio_icif.mt.r.beam': v('Beam', '束流', 'ビーム', 'Пучок'),
    'gui.mio_icif.mt.r.field': v('B', 'B', 'B', 'B'),
    'gui.mio_icif.mt.r.vacuum': v('Vac', '真空', '真空', 'Вак.'),
    'gui.mio_icif.mt.r.trap_temp': v('Trap', '阱温', 'トラップ', 'Лов.'),
    'gui.mio_icif.mt.r.dose': v('Dose', '剂量', '線量', 'Доза'),
    'gui.mio_icif.mt.r.nuclide': v('Nuclide', '核素', '核種', 'Нуклид'),
    'gui.mio_icif.mt.r.order': v('Order', '有序度', '秩序', 'Порядок'),
    'gui.mio_icif.mt.r.pressure': v('P', '压力', '圧力', 'Давл.'),
    'gui.mio_icif.mt.r.temperature': v('T', '温度', '温度', 'Темп.'),
    'gui.mio_icif.mt.r.nuclei': v('Nuclei', '成核', '核数', 'Зародыши'),
    'gui.mio_icif.mt.r.lattice': v('Lattice', '晶格', '格子', 'Решётка'),
    'gui.mio_icif.mt.r.press': v('Press', '压机', 'プレス', 'Пресс'),
    'gui.mio_icif.mt.r.progress': v('Prog', '进度', '進捗', 'Ход'),
    'gui.mio_icif.mt.r.grain': v('Grain', '晶粒', '粒径', 'Зерно'),
    'gui.mio_icif.mt.r.process': v('Proc', '过程', '過程', 'Проц.'),
    'gui.mio_icif.mt.r.conservation': v('Cons', '守恒', '保存', 'Сохр.'),
    'gui.mio_icif.mt.r.mz': v('m/z', 'm/z', 'm/z', 'm/z'),
    'gui.mio_icif.mt.r.nucleus': v('Nuclei', '原子核', '原子核', 'Ядра'),
    'gui.mio_icif.mt.r.purity': v('Purity', '纯度', '純度', 'Чист.'),
    'gui.mio_icif.mt.r.fragments': v('Frag', '碎片', '断片', 'Фрагм.'),
    'gui.mio_icif.mt.v.hexagonal': v('hex', '六方', '六方', 'гекс.'),
    'gui.mio_icif.mt.v.cubic': v('cubic', '立方', '立方', 'куб.'),
    'gui.mio_icif.mt.v.unknown': v('unknown', '未知', '不明', 'неизв.'),
    'gui.mio_icif.mt.v.broken': v('broken', '不成立', '不成立', 'нарушено'),
    'gui.mio_icif.mt.v.unchanged': v('same', '不变', '不変', 'те же'),
    'gui.mio_icif.mt.tip.energy': v('Energy: %s / %s EU', '能量：%s / %s EU', 'エネルギー: %s / %s EU', 'Энергия: %s / %s EU'),
    'gui.mio_icif.mt.tip.draw': v('Last tick: %s EU/t', '上一刻消耗：%s EU/t', '直前の消費: %s EU/t', 'Расход за тик: %s EU/t'),
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
