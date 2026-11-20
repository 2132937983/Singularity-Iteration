#!/usr/bin/env python3
"""0.1.11.97: the texts of the fusion reactor's fuel injection port and disruption mitigation port screens (slots of their own
for automation), in en_us, zh_cn, ja_jp and ru_ru. A key that exists gets the new text; a new key goes after ANCHOR. Keeps
each file's line ends. Idempotent.

Usage: fusion_lang_11597.py <repo root>
"""
import json
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
ANCHOR = 'gui.mio_icif.fusion.slot.empty'

# key, en, zh, ja, ru
TEXTS = [
    ('gui.mio_icif.fusion_port.deuterium', 'Deuterium', '氘燃料棒', '重水素燃料棒', 'Дейтерий'),
    ('gui.mio_icif.fusion_port.tritium', 'Tritium', '氚燃料棒', '三重水素燃料棒', 'Тритий'),
    ('gui.mio_icif.fusion_port.empty', 'Empty rods', '空燃料棒', '空の燃料棒', 'Пустые'),
    ('gui.mio_icif.fusion_port.coolant', 'Coolant cells (10k / 30k / 60k)', '冷却单元（10k / 30k / 60k）',
     '冷却セル（10k / 30k / 60k）', 'Капсулы охлаждения (10k / 30k / 60k)'),
    ('gui.mio_icif.fusion_port.linked', 'Linked to the Fusion Reactor Console', '已连接核聚变堆控制台',
     '核融合炉コントローラーに接続済み', 'Подключён к контроллеру реактора'),
    ('gui.mio_icif.fusion_port.unlinked', 'Not linked: the reactor is not formed', '未连接：聚变堆结构未成型',
     '未接続：核融合炉が未完成', 'Не подключён: реактор не собран'),
    ('gui.mio_icif.fusion_port.fuel_stock', 'Console fuel: D %s  T %s', '控制台燃料：氘 %s  氚 %s',
     'コントローラーの燃料：D %s  T %s', 'Топливо контроллера: D %s  T %s'),
    ('gui.mio_icif.fusion_port.coolant_stock', 'Console mitigation slot: %s cells', '控制台缓解槽：%s 个冷却单元',
     'コントローラーの緩和スロット：冷却セル %s 個', 'Слот подавления контроллера: %s капс.'),
    ('jei.mio_icif.fusion_info.5',
     'The fuel injection port and the disruption mitigation port have slots and a screen of their own: pipes and hoppers '
     'put fuel rods or coolant cells only into their own slots (never into the blanket), the console takes one whenever its '
     'slot is empty, and the empty fuel rods come back out of the fuel injection port.',
     '加料端口和破裂缓解端口有自己的槽位和界面：管道和漏斗只能把燃料棒或冷却单元送进对应的槽（不会进入包层），控制台的槽空了就从端口取一个，'
     '空燃料棒从加料端口取出。',
     '燃料注入ポートとディスラプション緩和ポートには専用のスロットと画面がある：パイプやホッパーは燃料棒や冷却セルを対応するスロットにだけ入れ'
     '（ブランケットには入らない）、コントローラーはスロットが空になるとポートから1つ取り、空の燃料棒は燃料注入ポートから取り出せる。',
     'У порта подачи топлива и порта подавления срывов свои слоты и интерфейс: трубы и воронки кладут ТВЭЛы и капсулы '
     'охлаждения только в их слоты (не в бланкет), контроллер берёт по одной, когда его слот пуст, а пустые ТВЭЛы выходят '
     'из порта подачи топлива.'),
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
