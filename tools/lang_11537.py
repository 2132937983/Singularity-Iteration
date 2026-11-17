#!/usr/bin/env python3
"""0.1.11.37 language values (en_us / zh_cn / ja_jp / ru_ru), chunk loader audit C1: the no-energy grace (status, state, alarms,
config label) and a clearer "no energy" status.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11537.py <repo root> [--check]
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
    'gui.mio_icif.chunk_loader.status.grace': v('No energy: holding only its own chunk for %s more', '没电：只保持自身区块，还剩 %s', '電力なし：自分のチャンクだけを保持、残り %s', 'Нет энергии: держит только свой чанк, ещё %s'),
    'gui.mio_icif.chunk_loader.state.grace': v('no energy (own chunk kept)', '没电（保持自身区块）', '電力なし（自分のチャンクを保持）', 'нет энергии (свой чанк удержан)'),
    'gui.mio_icif.chunk_loader.status.no_energy': v('No energy: stopped (its chunk unloads unless a player is near)', '没电已停止（附近没有玩家时其区块会卸载）',
        '電力なしで停止（近くにプレイヤーがいなければチャンクはアンロードされる）', 'Нет энергии: остановлен (чанк выгрузится, если рядом нет игрока)'),
    'gui.mio_icif.grid_panel.alarm.loader_grace': v('Chunk loader out of energy', '区块加载器没电', 'チャンクローダーの電力切れ', 'У загрузчика чанков нет энергии'),
    'gui.mio_icif.grid_panel.alarm.loader_stopped': v('Chunk loader stopped', '区块加载器已停止', 'チャンクローダー停止', 'Загрузчик чанков остановлен'),
    'gui.mio_icif.grid_panel.alarm.loader_restored': v('Chunk loader running again', '区块加载器已恢复', 'チャンクローダー復帰', 'Загрузчик чанков снова работает'),
    'mio_icif.configuration.chunk_loaders.noEnergyGraceMinutes': v('No-energy grace (minutes)', '没电宽限（分钟）', '電力切れの猶予（分）', 'Отсрочка без энергии (минуты)'),
    'mio_icif.configuration.chunk_loaders.noEnergyGraceMinutes.tooltip': v('Minutes a loader out of energy keeps its own chunk and takes all its chunks back when energy returns. 0: it stops at once.', '加载器没电时保持自身区块的分钟数，期间来电即恢复全部区块。0：立即停下。', '電力切れの間、自分のチャンクを保持する分。その間に電力が戻ればすべてのチャンクを取り戻す。0：すぐ停止。', 'Сколько минут загрузчик без энергии держит свой чанк и возвращает все чанки, когда энергия появится. 0: сразу останавливается.'),
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
