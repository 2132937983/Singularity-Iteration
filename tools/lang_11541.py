#!/usr/bin/env python3
"""0.1.11.41 language values (en_us / zh_cn / ja_jp / ru_ru), Wingman audit W2 (decision 1 C): the upkeep reserve in the
deploy message, the flight time left (console, squad HUD, Jade) and the config entry reserveMinutes.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11541.py <repo root> [--check]
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
    'message.mio_icif.wingman.deployed_reserve': v('%s flies as a Wingman (%s). Cost: %s EU, upkeep reserve: %s EU (%s)',
        '%s 已作为僚机出动（%s），消耗 %s EU，维持费储备 %s EU（%s）', '%s を僚機として出撃（%s）、%s EU、維持費の予備 %s EU（%s）',
        '%s выпущен как ведомый (%s), затрачено %s EU, запас на содержание %s EU (%s)'),
    'gui.mio_icif.wingman.endurance': v('%s · %s left', '%s · 航时 %s', '%s · 残り %s', '%s · осталось %s'),
    'gui.mio_icif.wingman.endurance.armory': v('Armory', '武装库', '武装庫', 'Арсенал'),
    'jade.mio_icif.wingman.suit': v('Suit %s · owner: %s', '第 %s 套 · 主人：%s', 'スーツ %s・持ち主：%s', 'Костюм %s · владелец: %s'),
    'jade.mio_icif.wingman.owner_away': v('offline', '不在线', 'オフライン', 'не в сети'),
    'jade.mio_icif.wingman.endurance': v('Flight time left: %s (reserve %s EU)', '剩余航时：%s（储备 %s EU）', '残り飛行時間：%s（予備 %s EU）',
        'Осталось полёта: %s (запас %s EU)'),
    'config.jade.plugin_mio_icif.wingman': v('Wingman', '僚机', '僚機', 'Ведомый'),
    'mio_icif.configuration.wingmen.reserveMinutes': v('Upkeep reserve (minutes)', '维持费储备（分钟）', '維持費の予備（分）', 'Запас на содержание (минуты)'),
    'mio_icif.configuration.wingmen.reserveMinutes.tooltip': v(
        'Charged from the Armory at deployment (less when it has less). The suit pays first, then the reserve, then the loaded Armory; the rest goes back when the Wingman returns. 0 = no reserve.',
        '出击时从武装库预充（电量不足时少充）。先由套装支付，再用储备，最后由已加载的武装库支付；返航时剩余部分退回。0 = 不预充。',
        '出撃時に武装庫から充電（足りなければ少なめ）。スーツ、予備、読み込み中の武装庫の順に払い、帰還時に残りを戻す。0 = 予備なし。',
        'Заряжается из арсенала при выпуске (меньше, если энергии мало). Платит сначала костюм, затем запас, затем загруженный арсенал; остаток возвращается. 0 = без запаса.'),
    'mio_icif.configuration.wingmen.upkeepEuPerSecond.tooltip': v('The electric pieces of the suit pay first, then the upkeep reserve, then the Armory (when loaded). 0 = free.',
        '先由套装的电动部件支付，再用维持费储备，最后由武装库支付（须已加载）。0 = 免费。', 'スーツの電動パーツ、維持費の予備、武装庫（読み込み中のとき）の順に払う。0 = 無料。',
        'Сначала платят электрические части костюма, затем запас, затем арсенал (если загружен). 0 = бесплатно.'),
}

REMOVE = ['message.mio_icif.wingman.deployed']

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
