#!/usr/bin/env python3
"""0.1.11.94: the texts of the deuterium fuel rod, unstable hot water, the coolant-cell disruption mitigation and the fusion
meltdown, in en_us, zh_cn, ja_jp and ru_ru. A key that exists gets the new text (the deuterium cell's name, the mitigation
message, the JEI line on deuterium); a new key goes after the deuterium rod's name; REMOVED keys are dropped. Keeps each
file's line ends. Idempotent.

Usage: fusion_lang_11594.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
ANCHOR = 'item.mio_icif.normal.item_deuterium_cell'
REMOVED = ['gui.mio_icif.fusion.slot.ice']

# key, en, zh, ja, ru
TEXTS = [
    (ANCHOR, 'Fuel Rod (Deuterium)', '氘燃料棒', '燃料棒（重水素）', 'ТВЭЛ (дейтерий)'),
    ('item.mio_icif.reactor.component.burnup', 'Burn-up: %s / %s cycles', '消耗：%s / %s 个周期', '消費：%s / %s サイクル',
     'Выгорание: %s / %s циклов'),
    ('item.mio_icif.reactor.component.fusion_fuel', 'Fusion reactor fuel: 10,000 units', '聚变堆燃料：10,000 单位',
     '核融合炉の燃料：10,000 単位', 'Топливо термоядерного реактора: 10 000 единиц'),
    ('block.mio_icif.unstable_hotwater', 'Unstable Hot Water', '不稳定的热水', '不安定な熱水', 'Нестабильная горячая вода'),
    ('fluid_type.mio_icif.unstable_hotwater', 'Unstable Hot Water', '不稳定的热水', '不安定な熱水', 'Нестабильная горячая вода'),
    ('item.mio_icif.unstable_hotwater_bucket', 'Unstable Hot Water Bucket', '不稳定的热水桶', '不安定な熱水入りバケツ',
     'Ведро нестабильной горячей воды'),
    ('item.mio_icif.cell.item_cell_unstable_hotwater', 'Unstable Hot Water Cell', '不稳定的热水单元', '不安定な熱水セル',
     'Капсула с нестабильной горячей водой'),
    ('item.mio_icif.cell_unstable_hotwater', 'Unstable Hot Water Cell', '不稳定的热水单元', '不安定な熱水セル',
     'Капсула с нестабильной горячей водой'),
    ('gui.mio_icif.fusion.slot.coolant', 'Cool', '冷却', '冷却', 'Охл.'),
    ('message.mio_icif.fusion.disruption_mitigated', 'Plasma disruption, mitigated by coolant injection (one %s used).',
     '等离子体破裂，已由冷却剂注入缓解（用掉一个%s）。', 'プラズマ・ディスラプション。冷却材の注入で緩和した（%sを1個使用）。',
     'Срыв плазмы, смягчён впрыском хладагента (израсходован %s).'),
    ('message.mio_icif.fusion.meltdown', "The fusion reactor's blanket overheated: meltdown!", '聚变堆包层过热：熔毁！',
     '核融合炉のブランケットが過熱：メルトダウン！', 'Бланкет термоядерного реактора перегрелся: расплавление!'),
    ('jei.mio_icif.fusion_info.3',
     'Up to 39,200,000 EU/t with the best blanket (twice the Mekanism fusion reactor). Deuterium fuel rods: a thermal centrifuge '
     'turns hot water cells into unstable hot water cells; a canner cans one (left slot) into an empty fuel rod (middle slot). '
     'Tritium is bred from lithium cells.',
     '最优包层最高 39,200,000 EU/t（Mek 聚变堆的二倍）。氘燃料棒：热能离心机把热水单元变成不稳定的热水单元，再用装罐机装入空燃料棒'
     '（左槽放单元，中间槽放空燃料棒）；氚由锂单元增殖。',
     '最良のブランケットで最大39,200,000 EU/t。重水素燃料棒：熱遠心分離機で熱水セルを不安定な熱水セルにし、缶詰機で空の燃料棒に詰める'
     '（左スロットにセル、中央スロットに空の燃料棒）。三重水素はリチウムセルから増殖する。',
     'До 39 200 000 EU/t с лучшим бланкетом. Дейтериевые ТВЭЛы: термоцентрифуга превращает капсулы с горячей водой в капсулы с '
     'нестабильной горячей водой, консервный автомат заправляет их в пустые ТВЭЛы (капсула слева, пустой ТВЭЛ в середине). '
     'Тритий нарабатывается из литиевых капсул.'),
    ('jei.mio_icif.fusion_info.4',
     'Disruption mitigation takes 10k, 30k or 60k coolant cells (60k is best). A blanket at its maximum heat melts down: a '
     "nuclear blast 50 times the fission reactor's strongest (config fusionExplosionMultiplier).",
     '破裂缓解槽放 10k、30k 或 60k 冷却单元，60k 效果最好。包层热量到达上限会熔毁，引发核反应堆最强爆炸 50 倍的核爆'
     '（配置 fusionExplosionMultiplier）。',
     'ディスラプション緩和には10k・30k・60k冷却セルを使う（60kが最良）。ブランケットが最大熱に達するとメルトダウンし、'
     '核分裂炉の最大の爆発の50倍の核爆発が起きる（設定 fusionExplosionMultiplier）。',
     'Для подавления срывов — капсулы охлаждения 10k, 30k или 60k (60k лучше всего). Бланкет на максимуме тепла '
     'расплавляется: ядерный взрыв в 50 раз сильнее сильнейшего взрыва реактора деления (параметр fusionExplosionMultiplier).'),
]


def main():
    changed = 0
    for code, col in (('en_us', 1), ('zh_cn', 2), ('ja_jp', 3), ('ru_ru', 4)):
        path = os.path.join(LANG, code + '.json')
        raw = open(path, 'rb').read().decode('utf-8')
        eol = '\r\n' if '\r\n' in raw else '\n'
        lines = raw.replace('\r\n', '\n').split('\n')
        index = {}
        for i, line in enumerate(lines):
            stripped = line.strip()
            if stripped.startswith('"') and '":' in stripped:
                index[stripped[1:stripped.index('"', 1)]] = i
        for key in REMOVED:
            if key in index:
                del lines[index[key]]
                changed += 1
                index = {k: (v if v < index[key] else v - 1) for k, v in index.items() if k != key}
        anchor = index[ANCHOR]
        for entry in TEXTS:
            key, text = entry[0], entry[col]
            line = '  %s: %s,' % (json.dumps(key), json.dumps(text, ensure_ascii=False))
            if key in index:
                old = lines[index[key]]
                if old.rstrip(',') != line.rstrip(','):
                    lines[index[key]] = line if old.rstrip().endswith(',') else line.rstrip(',')
                    changed += 1
                anchor = index[key]
            else:
                lines.insert(anchor + 1, line)
                index = {k: (v + 1 if v > anchor else v) for k, v in index.items()}
                anchor += 1
                index[key] = anchor
                changed += 1
        text = '\n'.join(lines)
        json.loads(text)
        open(path, 'wb').write(text.replace('\n', eol).encode('utf-8'))
    print('lines changed:', changed)


if __name__ == '__main__':
    main()
