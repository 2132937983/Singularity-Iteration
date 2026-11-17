#!/usr/bin/env python3
"""0.1.11.39 language values (en_us / zh_cn / ja_jp / ru_ru), Wingman audit W0: the drop-in refusal, the "out" mark of a
suit that flies as a Wingman and the new config section Wingmen.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11539.py <repo root> [--check]
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
    'message.mio_icif.wingman.no_room': v('No room for a Wingman to drop in above or beside you', '你的上方和身旁都没有空间投放僚机',
        '上にも横にも僚機を降下させる空間がありません', 'Нет места, чтобы сбросить ведомого над вами или рядом'),
    'gui.mio_icif.armory_console.deployed': v('Out', '出击中', '出撃中', 'В полёте'),
    'mio_icif.configuration.wingmen': v('Wingmen', '僚机', '僚機', 'Ведомые'),
    'mio_icif.configuration.wingmen.maxPerPlayer': v('Wingmen per player', '每名玩家的僚机上限', 'プレイヤーごとの僚機上限', 'Ведомых на игрока'),
    'mio_icif.configuration.wingmen.maxPerPlayer.tooltip': v('1-8, counting the Wingmen in unloaded chunks.', '1-8，包括未加载区块中的僚机。',
        '1〜8。未読み込みチャンクの僚機も数えます。', '1-8, включая ведомых в незагруженных чанках.'),
    'mio_icif.configuration.wingmen.upkeepEuPerSecond': v('Upkeep (EU per second)', '维持费（EU/秒）', '維持費（EU/秒）', 'Содержание (EU в секунду)'),
    'mio_icif.configuration.wingmen.upkeepEuPerSecond.tooltip': v('The electric pieces of the suit pay first, then the Armory (when loaded). 0 = free.',
        '先由套装的电动部件支付，再由武装库支付（须已加载）。0 = 免费。', 'スーツの電動パーツが先に払い、次にアーマリー（読み込み中のとき）。0 = 無料。',
        'Сначала платят электрические части костюма, затем арсенал (если загружен). 0 = бесплатно.'),
    'mio_icif.configuration.wingmen.lowPowerSeconds': v('Seconds without upkeep before return', '付不起维持费后返航前的秒数', '維持費が払えないまま帰還するまでの秒数', 'Секунд без оплаты до возврата'),
    'mio_icif.configuration.wingmen.lowPowerSeconds.tooltip': v('1-600.', '1-600。', '1〜600。', '1-600.'),
    'mio_icif.configuration.wingmen.scanRange': v('Target search range', '目标搜索范围', 'ターゲット探索範囲', 'Радиус поиска целей'),
    'mio_icif.configuration.wingmen.scanRange.tooltip': v('Blocks around the owner (offensive mode).', '主人周围的格数（进攻模式）。', '主人の周りのブロック数（攻撃モード）。', 'Блоков вокруг владельца (режим атаки).'),
    'mio_icif.configuration.wingmen.leash': v('Target leash', '目标牵引距离', 'ターゲット追跡距離', 'Предел удаления цели'),
    'mio_icif.configuration.wingmen.leash.tooltip': v('A target farther than this from the owner is dropped (no order). Never below the search range.',
        '目标离主人超过这个距离就放弃（无命令时）。不会小于搜索范围。', '主人からこれより遠いターゲットは追わない（命令なしのとき）。探索範囲より小さくならない。',
        'Цель дальше этого расстояния от владельца бросается (без приказа). Не меньше радиуса поиска.'),
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
