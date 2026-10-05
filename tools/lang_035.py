#!/usr/bin/env python3
"""0.1.7.35 language keys (en_us / zh_cn / ja_jp): Energy Saving enchantment, battery auto-charge
mode, and Japanese HUD lines of the swords and the jetpack. ASD-STE100 style."""
import json, os, sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

ADD = {
    'en_us': {
        'enchantment.mio_icif.energy_saving': 'Energy Saving',
        'enchantment.mio_icif.energy_saving.desc': 'Each level decreases the energy use of electric tools, weapons and armor by 5 %.',
        'message.mio_icif.bat.auto_on': '§aInventory auto-charge: ON§r (%s / %s EU)',
        'message.mio_icif.bat.auto_off': '§7Inventory auto-charge: OFF§r (%s / %s EU)',
        'tooltip.mio_icif.bat.auto_on': 'Auto-charge ON: feeds the electric items in your inventory',
        'tooltip.mio_icif.bat.auto_off': 'Auto-charge OFF. Right click to switch it on',
        'tooltip.mio_icif.pack.auto_hint': 'Sneak + right click: inventory auto-charge on / off',
    },
    'zh_cn': {
        'enchantment.mio_icif.energy_saving': '省电',
        'enchantment.mio_icif.energy_saving.desc': '每级使电动工具、电动武器与电力护甲的耗电减少 5%。',
        'message.mio_icif.bat.auto_on': '§a背包自动充电：开§r（%s / %s EU）',
        'message.mio_icif.bat.auto_off': '§7背包自动充电：关§r（%s / %s EU）',
        'tooltip.mio_icif.bat.auto_on': '自动充电已开启：为背包中的电力物品供电',
        'tooltip.mio_icif.bat.auto_off': '自动充电已关闭。右键开启',
        'tooltip.mio_icif.pack.auto_hint': '潜行 + 右键：开关背包自动充电',
    },
    'ja_jp': {
        'enchantment.mio_icif.energy_saving': '省電力',
        'enchantment.mio_icif.energy_saving.desc': 'レベルごとに電気ツール・電気武器・電力防具の消費電力を 5 % 減らす。',
        'message.mio_icif.bat.auto_on': '§aインベントリ自動充電：ON§r（%s / %s EU）',
        'message.mio_icif.bat.auto_off': '§7インベントリ自動充電：OFF§r（%s / %s EU）',
        'tooltip.mio_icif.bat.auto_on': '自動充電ON：インベントリ内の電気アイテムに給電する',
        'tooltip.mio_icif.bat.auto_off': '自動充電OFF。右クリックでONにする',
        'tooltip.mio_icif.pack.auto_hint': 'スニーク + 右クリック：インベントリ自動充電のON/OFF',
        'hud.mio_icif.nanosaber.display': 'ナノセイバー：%s | エネルギー：%s / %s EU',
        'hud.mio_icif.nanosaber.mode_active': '§a起動中',
        'hud.mio_icif.nanosaber.mode_inactive': '§7停止',
        'hud.mio_icif.quantum_sword.display': '量子剣：%s | エネルギー：%s / %s EU',
        'hud.mio_icif.quantum_sword.mode_hyper': '§aハイパー',
        'hud.mio_icif.quantum_sword.mode_normal': '§7通常',
        'hud.mio_icif.jetpack.display': 'ジェットパック：%s | エネルギー：%s / %s EU',
        'hud.mio_icif.jetpack.mode_jetpack': 'ジェット',
        'hud.mio_icif.jetpack.mode_hover': 'ホバー',
    },
}


def main():
    for lang, add in ADD.items():
        path = os.path.join(LANG, lang + '.json')
        with open(path, encoding='utf-8') as f:
            data = json.load(f, object_pairs_hook=OrderedDict)
        for k, v in add.items():
            data[k] = v
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')
    print('ok')


if __name__ == '__main__':
    main()
