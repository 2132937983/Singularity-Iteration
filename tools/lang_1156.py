#!/usr/bin/env python3
"""0.1.11.6 language values (en_us / zh_cn / ja_jp / ru_ru): the observation window of the Molecular Transformer screen
(film labels, the scanner's log, phases, tooltips).
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
    'gui.mio_icif.mt.state.run': v('RUN', '运行', '稼働', 'РАБОТА'),
    'gui.mio_icif.mt.state.low': v('LOW POWER', '低功率', '低出力', 'МАЛО ЭНЕРГИИ'),
    'gui.mio_icif.mt.state.idle': v('STANDBY', '待机', '待機', 'ОЖИДАНИЕ'),
    'gui.mio_icif.mt.path.strip': v('Photonuclear stripping (γ,p) ×%s', '光核剥离 (γ,p) ×%s', '光核剥離 (γ,p) ×%s', 'Фотоядерное отщепление (γ,p) ×%s'),
    'gui.mio_icif.mt.path.fuse': v('Heavy-ion fusion + neutron evaporation', '重离子熔合 + 中子蒸发', '重イオン融合 + 中性子蒸発', 'Слияние тяжёлых ионов + испарение нейтронов'),
    'gui.mio_icif.mt.path.isotope': v('Isotope exchange', '同位素交换', '同位体交換', 'Изотопный обмен'),
    'gui.mio_icif.mt.path.unknown': v('The recipe has no process data', '配方未标注工艺', '工程の指定なし', 'Процесс в рецепте не указан'),
    'gui.mio_icif.mt.idle.insert': v('Insert material to start', '放入原料后开始', '素材を入れると開始', 'Положите материал для запуска'),
    'gui.mio_icif.mt.idle.wait': v('Waiting for power or output space', '等待供电或输出空位', '電力か出力の空きを待機中', 'Ждёт энергию или место на выходе'),
    'gui.mio_icif.mt.line.lattice_note': v('Same atoms, new arrangement', '原子不变，只改排列', '原子は同じ、配列だけ変える', 'Атомы те же, меняется порядок'),
    'gui.mio_icif.mt.line.molecular_note': v('Nuclei unchanged', '原子核不变', '原子核は不変', 'Ядра не меняются'),
    'gui.mio_icif.mt.line.exotic_note': v('No real-world counterpart', '没有对应的现实过程', '現実に対応する過程なし', 'Нет реального аналога'),
    'gui.mio_icif.mt.line.rate': v('%s items/s', '%s 件/秒', '%s 個/秒', '%s шт/с'),
    'gui.mio_icif.mt.line.power': v('%s EU/t', '%s EU/t', '%s EU/t', '%s EU/t'),
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
    'gui.mio_icif.mt.tip.energy': v('Energy: %s / %s EU', '能量：%s / %s EU', 'エネルギー: %s / %s EU', 'Энергия: %s / %s EU'),
    'gui.mio_icif.mt.tip.draw': v('Last tick: %s EU/t', '上一刻消耗：%s EU/t', '直前の消費: %s EU/t', 'Расход за тик: %s EU/t'),
    'gui.mio_icif.mt.film.chamber': v('bubble chamber', '气泡室', '泡箱', 'пузырьковая камера'),
    'gui.mio_icif.mt.film.diffraction': v('diffraction film', '衍射底片', '回折フィルム', 'дифракционная плёнка'),
    'gui.mio_icif.mt.film.plate': v('spectrograph plate', '质谱底板', '質量分析乾板', 'пластинка масс-спектрографа'),
    'gui.mio_icif.mt.film.edge': v('SI-MT  ROLL 07  FR %s', 'SI-MT  卷 07  画格 %s', 'SI-MT  ロール 07  コマ %s', 'SI-MT  РУЛОН 07  КАДР %s'),
    'gui.mio_icif.mt.film.old_phase': v('old phase', '旧相', '旧相', 'старая фаза'),
    'gui.mio_icif.mt.film.new_phase': v('new phase', '新相', '新相', 'новая фаза'),
    'gui.mio_icif.mt.idle.shutter': v('Shutter closed', '快门关闭', 'シャッター閉', 'Затвор закрыт'),
    'gui.mio_icif.mt.log.frame': v('Frame %s', '画格 %s', 'コマ %s', 'Кадр %s'),
    'gui.mio_icif.mt.log.z': v('Z %s / %s', 'Z %s / %s', 'Z %s / %s', 'Z %s / %s'),
    'gui.mio_icif.mt.log.new_rings': v('New-phase rings %s', '新相衍射环 %s', '新相の回折環 %s', 'Кольца новой фазы %s'),
    'gui.mio_icif.mt.log.unconserved': v('Tracks break conservation', '径迹不守恒', '飛跡が保存則に反する', 'Треки нарушают сохранение'),
    'gui.mio_icif.mt.log.peak': v('m/z %s · %s', 'm/z %s · %s', 'm/z %s · %s', 'm/z %s · %s'),
    'gui.mio_icif.mt.log.phase': v('Phase: %s %s/4', '阶段 %s %s/4', '段階 %s %s/4', 'Этап: %s %s/4'),
}

REMOVE = ['gui.mio_icif.mt.log.title', 'gui.mio_icif.mt.kind.nuclear', 'gui.mio_icif.mt.kind.lattice', 'gui.mio_icif.mt.kind.molecular', 'gui.mio_icif.mt.kind.exotic', 'gui.mio_icif.mt.kind.unknown', 'gui.mio_icif.mt.path.lattice', 'gui.mio_icif.mt.path.molecular', 'gui.mio_icif.mt.path.exotic', 'gui.mio_icif.mt.idle.trap', 'gui.mio_icif.mt.line.z', 'gui.mio_icif.mt.line.eu', 'gui.mio_icif.mt.line.pulse', 'gui.mio_icif.mt.line.press', 'gui.mio_icif.mt.r.z', 'gui.mio_icif.mt.r.spectrum', 'gui.mio_icif.mt.r.beam', 'gui.mio_icif.mt.r.field', 'gui.mio_icif.mt.r.vacuum', 'gui.mio_icif.mt.r.trap_temp', 'gui.mio_icif.mt.r.dose', 'gui.mio_icif.mt.r.nuclide', 'gui.mio_icif.mt.r.order', 'gui.mio_icif.mt.r.pressure', 'gui.mio_icif.mt.r.temperature', 'gui.mio_icif.mt.r.nuclei', 'gui.mio_icif.mt.r.lattice', 'gui.mio_icif.mt.r.press', 'gui.mio_icif.mt.r.progress', 'gui.mio_icif.mt.r.grain', 'gui.mio_icif.mt.r.process', 'gui.mio_icif.mt.r.conservation', 'gui.mio_icif.mt.r.mz', 'gui.mio_icif.mt.r.nucleus', 'gui.mio_icif.mt.r.purity', 'gui.mio_icif.mt.r.fragments', 'gui.mio_icif.mt.v.hexagonal', 'gui.mio_icif.mt.v.cubic', 'gui.mio_icif.mt.v.unknown', 'gui.mio_icif.mt.v.broken', 'gui.mio_icif.mt.v.unchanged']


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
