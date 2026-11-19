#!/usr/bin/env python3
"""0.1.11.76 language values (en_us / zh_cn / ja_jp / ru_ru): the reactor config card (item, tooltip, clear message) and
its controls on the reactor screens (card slot, import / export / fill buttons, layout preview, status line).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11576.py <repo root> [--check]
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


C = 'tooltip.mio_icif.reactor_config_card.'
G = 'gui.mio_icif.reactor_card.'
S = G + 'status.'

KEYS = {
    'item.mio_icif.reactor.item_reactor_config_card': v('Reactor Config Card', '反应堆配置卡', '原子炉設定カード', 'Карта конфигурации реактора'),
    C + 'blank': v('Blank. Records a reactor layout in the card slot of a reactor screen.',
                   '空白。放进反应堆界面的配置卡槽，可以记录反应堆的布局。',
                   '空白。原子炉画面のカードスロットで原子炉の配置を記録します。',
                   'Пустая. Записывает схему реактора в слоте карты на экране реактора.'),
    C + 'how': v('↓ records the reactor, ↑ shows the layout on another reactor',
                 '↓ 记录反应堆布局，↑ 把布局投影到另一台反应堆',
                 '↓ で原子炉を記録、↑ で別の原子炉に配置を表示',
                 '↓ записать реактор, ↑ показать схему на другом реакторе'),
    C + 'parts': v('Layout: %s parts, %s columns', '布局：%s 个部件，需要 %s 列', '配置：部品 %s 個、%s 列', 'Схема: деталей %s, столбцов %s'),
    C + 'unknown': v('Unknown part (missing mod)', '未知部件（模组缺失）', '不明な部品（MOD がない）', 'Неизвестная деталь (нет мода)'),
    C + 'more': v(' … and %s more kinds', ' ……另有 %s 种', ' …ほか %s 種類', ' …и ещё видов: %s'),
    C + 'clear': v('Sneak + use: clear the card', '潜行 + 右键：清空配置卡', 'スニーク + 使用：カードを消去', 'Присесть + использовать: очистить карту'),
    'message.mio_icif.reactor_config_card.cleared': v('Reactor config card cleared', '反应堆配置卡已清空',
                                                      '原子炉設定カードを消去しました', 'Карта конфигурации реактора очищена'),
    G + 'label': v('Config Card', '配置卡', '設定カード', 'Карта'),
    G + 'import': v('Import layout', '导入布局', '配置を取り込む', 'Импорт схемы'),
    G + 'import.tip': v('Write the parts of this reactor onto the card (replaces the layout on it)',
                        '把这台反应堆当前的部件记录到配置卡上（覆盖卡上原有的布局）',
                        'この原子炉の部品配置をカードに書き込みます（カードの配置は上書きされます）',
                        'Записать детали этого реактора на карту (старая схема заменяется)'),
    G + 'export': v('Export layout', '导出布局', '配置を書き出す', 'Экспорт схемы'),
    G + 'export.tip': v("Show the card's layout on this reactor: faded parts in empty slots, a red tint on wrong parts",
                        '把配置卡上的布局投影到这台反应堆：空槽里显示半透明的部件，放错的部件标红',
                        'カードの配置をこの原子炉に表示します：空きスロットに半透明の部品、違う部品は赤く表示',
                        'Показать схему карты на этом реакторе: бледные детали в пустых слотах, неверные детали подсвечены красным'),
    G + 'export.shift': v('Shift + click: clear the layout shown on this reactor', 'Shift + 点击：清除这台反应堆上的布局投影',
                          'Shift + クリック：この原子炉の配置表示を消去', 'Shift + клик: убрать схему с этого реактора'),
    G + 'fill': v('Fill from inventory', '一键放置', '一括配置', 'Заполнить'),
    G + 'fill.tip': v('Take each part the layout asks for from your inventory, hotbar and offhand and place it in its empty slot (any durability)',
                      '从背包、快捷栏和副手中取出布局需要的部件，放进对应的空槽（只看物品种类，不看耐久）',
                      'インベントリ・ホットバー・オフハンドから配置どおりの部品を取り出し、空きスロットに置きます（耐久値は問いません）',
                      'Взять из инвентаря, хотбара и второй руки нужные по схеме детали и положить в пустые слоты (прочность не важна)'),
    G + 'slot': v('Card slot', '配置卡槽', 'カードスロット', 'Слот карты'),
    G + 'slot.tip': v('Put a reactor config card here', '放入反应堆配置卡', '原子炉設定カードを入れてください', 'Положите карту конфигурации реактора'),
    G + 'ghost': v('Layout: this part goes here', '布局预览：此处应放置该部件', '配置プレビュー：ここにこの部品を置きます', 'Схема: сюда нужна эта деталь'),
    G + 'mismatch': v('Layout asks for: %s', '布局要求：%s', '配置の指定：%s', 'По схеме нужно: %s'),
    S + 'imported': v('Card: %s parts recorded', '已记录 %s 个部件到配置卡', 'カードに部品 %s 個を記録しました', 'На карту записано деталей: %s'),
    S + 'exported': v('Layout shown: %s parts', '已投影布局：%s 个部件', '配置を表示：部品 %s 個', 'Схема показана: деталей %s'),
    S + 'exported_columns': v('Layout shown: %s parts · needs %s columns (add reactor chambers)',
                              '已投影布局：%s 个部件 · 需要 %s 列（请加装核反应仓）',
                              '配置を表示：部品 %s 個 · %s 列が必要（原子炉チャンバーを追加）',
                              'Схема показана: деталей %s · нужно столбцов: %s (добавьте камеры реактора)'),
    S + 'cleared': v('Layout cleared from this reactor', '已清除这台反应堆的布局投影', 'この原子炉の配置表示を消去しました', 'Схема убрана с этого реактора'),
    S + 'filled': v('Placed %s parts · layout complete', '已放置 %s 个部件 · 布局已齐', '%s 個配置しました · 配置完了', 'Размещено деталей: %s · схема заполнена'),
    S + 'filled_missing': v('Placed %s parts · %s still missing', '已放置 %s 个部件 · 还缺 %s 个', '%s 個配置しました · あと %s 個不足',
                            'Размещено: %s · не хватает: %s'),
    S + 'filled_blocked': v('Placed %s · %s missing · %s need more chambers', '已放置 %s 个 · 缺 %s 个 · %s 个需要更多核反应仓',
                            '%s 個配置 · %s 個不足 · %s 個はチャンバー不足', 'Размещено: %s · нет: %s · нужны камеры: %s'),
    S + 'no_card': v('Put a reactor config card in the card slot', '请先在配置卡槽放入反应堆配置卡',
                     'カードスロットに原子炉設定カードを入れてください', 'Положите карту конфигурации реактора в слот'),
    S + 'blank_card': v('The card is blank: import a layout first', '配置卡是空白的：请先导入布局',
                        'カードは空白です：先に配置を取り込んでください', 'Карта пустая: сначала импортируйте схему'),
    S + 'empty_reactor': v('The reactor is empty: nothing to record', '反应堆是空的：没有可记录的布局',
                           '原子炉は空です：記録する配置がありません', 'Реактор пуст: нечего записывать'),
    S + 'no_guide': v('This reactor shows no layout: export one first', '这台反应堆没有布局投影：请先导出',
                      'この原子炉には配置表示がありません：先に書き出してください', 'На этом реакторе нет схемы: сначала экспортируйте'),
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
