#!/usr/bin/env python3
"""0.1.10.38 language values (en_us / zh_cn / ja_jp / ru_ru): the processing machine line of /mio_icif perf (PerfCommand);
the Energy Saving description (levels above V); the Portable Futures Trader (name, tooltip, data plan message).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1138.py <repo root> [--check]
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
    'command.mio_icif.perf.machines': v(
        'Processing machines per tick: full ticks %1$s, asleep %2$s, lamp switches %3$s, redstone reads %4$s',
        '加工机器每 tick：完整 tick %1$s，休眠 %2$s，状态灯切换 %3$s，红石读取 %4$s',
        '加工機械 1 tick あたり：通常 tick %1$s、休眠 %2$s、ランプ切替 %3$s、レッドストーン読み取り %4$s',
        'Машины обработки за тик: полных тиков %1$s, в ожидании %2$s, переключений ламп %3$s, чтений редстоуна %4$s'),
    # D5: levels above V (commands, other mods) take another 5 % off the remaining cost
    'enchantment.mio_icif.energy_saving.desc': v(
        'Each level decreases the energy use of electric tools, weapons and armor by 5 %%. Above level V, each level takes '
        'another 5 %% off the remaining use.',
        '每级使电动工具、电动武器与电力护甲的耗电减少 5%%。V 级以上每级再减少剩余耗电的 5%%。',
        'レベルごとに電気ツール・電気武器・電力防具の消費電力を 5 %% 減らす。V を超えると、レベルごとに残りの消費電力をさらに 5 %% 減らす。',
        'Каждый уровень снижает расход энергии электрических инструментов, оружия и брони на 5%%. Выше уровня V каждый '
        'уровень снижает оставшийся расход ещё на 5%%.'),
    # D10: the Portable Futures Trader and its data plan
    'item.mio_icif.normal.item_portable_futures_trader': v(
        'Portable Futures Trader', '便携期货交易终端', '携帯先物取引端末', 'Портативный фьючерсный терминал'),
    'tooltip.mio_icif.portable_trader.use': v(
        'Right-click: the futures panel anywhere (%s EU per trade)', '右键：随时随地打开期货面板（每笔交易 %s EU）',
        '右クリック：どこでも先物パネルを開く（取引 1 回 %s EU）', 'ПКМ: панель фьючерсов где угодно (%s EU за сделку)'),
    'tooltip.mio_icif.portable_trader.plan': v(
        'Data plan: %1$s Industrial Coins per %2$s market days', '流量套餐：每 %2$s 个市场日 %1$s 工业货币',
        '通信プラン：市場日 %2$s 日ごとに工業コイン %1$s 枚', 'Тарифный план: %1$s промышленных монет за %2$s рыночных дней'),
    'tooltip.mio_icif.portable_trader.plan_free': v(
        'Data plan: free', '流量套餐：免费', '通信プラン：無料', 'Тарифный план: бесплатно'),
    'message.mio_icif.portable_trader.plan_unpaid': v(
        'Data plan unpaid: %1$s Industrial Coins needed for the next %2$s market days',
        '流量套餐未支付：下一期 %2$s 个市场日需要 %1$s 工业货币',
        '通信プラン未払い：次の市場日 %2$s 日分に工業コイン %1$s 枚が必要',
        'Тарифный план не оплачен: нужно %1$s промышленных монет на следующие %2$s рыночных дней'),
}


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
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in mine.items():
            data[key] = t
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(mine))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
