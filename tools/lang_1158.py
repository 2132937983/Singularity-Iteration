#!/usr/bin/env python3
"""0.1.11.8 language values (en_us / zh_cn / ja_jp / ru_ru): the Particle Polymerizer screen (implosion record, log card,
chain target, accelerator warm-up, tooltips).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_1158.py <repo root> [--check]
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
    'gui.mio_icif.polymerizer.frame_time': v('%s ns', '%s ns', '%s ns', '%s нс'),
    'gui.mio_icif.polymerizer.streak': v('Streak camera r–t', '条纹相机 r–t', 'ストリークカメラ r–t', 'Щелевая камера r–t'),
    'gui.mio_icif.polymerizer.log': v('Implosion log', '内爆记录', '爆縮記録', 'Журнал сжатия'),
    'gui.mio_icif.polymerizer.state.idle': v('STANDBY', '待机', '待機', 'ОЖИД.'),
    'gui.mio_icif.polymerizer.state.warming': v('WARMING', '预热', '予熱', 'ПРОГРЕВ'),
    'gui.mio_icif.polymerizer.state.run': v('RUN', '运行', '稼働', 'РАБОТА'),
    'gui.mio_icif.polymerizer.no_recipe': v('Insert an item to polymerize', '放入可聚合的物品', '重合できるアイテムを入れる', 'Положите предмет'),
    'gui.mio_icif.polymerizer.stage': v('Step %s/%s · %s', '第 %s/%s 级 · %s', '第%s/%s段 · %s', 'Этап %s/%s · %s'),
    'gui.mio_icif.polymerizer.kind.densify': v('Densification', '致密化', '高密度化', 'Уплотнение'),
    'gui.mio_icif.polymerizer.kind.transmute': v('Transmutation', '嬗变', '核変換', 'Трансмутация'),
    'gui.mio_icif.polymerizer.kind.phase': v('Phase change', '相变', '相転移', 'Фазовый переход'),
    'gui.mio_icif.polymerizer.kind.structure': v('Restructuring', '结构重组', '構造再編', 'Перестройка'),
    'gui.mio_icif.polymerizer.kind.unknown': v('Unmarked', '未标注', '未指定', 'Не указано'),
    'gui.mio_icif.polymerizer.ratio': v('Radius ×%s · density ×%s', '半径 ×%s · 密度 ×%s', '半径 ×%s · 密度 ×%s', 'Радиус ×%s · плотн. ×%s'),
    'gui.mio_icif.polymerizer.radiation': v('Neutrons: %s blocks', '中子辐射 %s 格', '中性子線 %s ブロック', 'Нейтроны: %s бл.'),
    'gui.mio_icif.polymerizer.shielded': v('Neutrons shielded', '中子辐射已屏蔽', '中性子線を遮蔽', 'Нейтроны экранированы'),
    'gui.mio_icif.polymerizer.real': v('Real material', '真实材料', '実在の物質', 'Реальный материал'),
    'gui.mio_icif.polymerizer.fictional': v('Invented material (set density)', '虚构材料 · 密度为设定', '架空の物質（設定値）', 'Вымышленный материал'),
    'gui.mio_icif.polymerizer.eta': v('%s left', '剩余 %s', '残り %s', 'Осталось %s'),
    'gui.mio_icif.polymerizer.chain.off': v('Chain off', '链式 关', '連鎖 オフ', 'Цепь выкл.'),
    'gui.mio_icif.polymerizer.chain.to': v('Chain → %s', '链式 → %s', '連鎖 → %s', 'Цепь → %s'),
    'gui.mio_icif.polymerizer.accelerator': v('Accelerator', '加速器', '加速器', 'Ускоритель'),
    'gui.mio_icif.polymerizer.tip.energy': v('Energy: %s / %s EU', '能量：%s / %s EU', 'エネルギー：%s / %s EU', 'Энергия: %s / %s EU'),
    'gui.mio_icif.polymerizer.tip.draw': v('Draw: %s EU/t', '当前耗电：%s EU/t', '消費：%s EU/t', 'Расход: %s EU/t'),
    'gui.mio_icif.polymerizer.tip.chain': v('Chain polymerization: one input becomes the chosen product; the steps between stay inside the machine', '链式聚合：一个原料直接做到选定的产物，中间品不出机器', '連鎖重合：素材1個を選んだ製品まで一気に加工（中間品は機械の中）', 'Цепная полимеризация: один предмет сразу превращается в выбранный продукт'),
    'gui.mio_icif.polymerizer.tip.chain2': v('Click for the next target (only products this input can reach)', '点击切换目标（只列出这个原料能走到的产物）', 'クリックで目標を切替（この素材から作れる製品のみ）', 'Нажмите для следующей цели (только достижимые продукты)'),
    'gui.mio_icif.polymerizer.tip.warmup': v('Warm-up: full speed after about 10 s of work (the draw follows the speed); cools in about 20 s', '加速器预热：工作约 10 秒升到全速（耗电随速度），停机后约 20 秒冷却', '予熱：約10秒で全速（消費は速度に比例）、停止後約20秒で冷める', 'Прогрев: полная скорость примерно через 10 с (расход растёт со скоростью), остывает за 20 с'),
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
