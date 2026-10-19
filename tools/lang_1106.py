#!/usr/bin/env python3
"""0.1.10.6 language keys (en_us / zh_cn / ja_jp): overclock throttle tooltip, Weed-EX fluid name
(the fluid type key was missing in all three files), Japanese fluid names, Milk Wart tooltip, UU tank
of the Next-Gen Crop Analyzer. Idempotent; keeps the line endings of each file.
Usage: lang_1106.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

# Always set (new or corrected text).
KEYS = {
    # %1$s percent of the installed overclockers that work, %2$s working, %3$s installed
    'gui.mio_icif.overclock.title': (
        'Overclock: %1$s%% (%2$s of %3$s overclockers working)',
        '超频：%1$s%%（%3$s 个超频升级中 %2$s 个生效）',
        'オーバークロック：%1$s%%（%3$s 個中 %2$s 個が有効）'),
    # %1$s progress per tick, %2$s ticks per operation
    'gui.mio_icif.overclock.speed': (
        'Speed: %1$s progress/tick, %2$s ticks per operation',
        '速度：每 tick %1$s 进度，每次加工 %2$s tick',
        '速度：1 tick あたり進捗 %1$s、1 回の処理に %2$s tick'),
    # %1$s energy factor, %2$s EU per tick
    'gui.mio_icif.overclock.energy': (
        'Energy: x%1$s, %2$s EU/t',
        '能耗：x%1$s，%2$s EU/t',
        '消費電力：x%1$s、%2$s EU/t'),
    'gui.mio_icif.overclock.hint': (
        'Drag, click or scroll to set. Stored energy bonus stays.',
        '拖动、点击或滚轮调节。储能加成保持不变。',
        'ドラッグ・クリック・ホイールで調整。蓄電量のボーナスはそのまま。'),
    'fluid_type.mio_icif.weed_ex': ('Weed-EX', '除草剂', '除草剤'),
    'fluid.mio_icif.weed_ex': ('Weed-EX', '除草剂', '除草剤'),
    'block.mio_icif.weed_ex': ('Weed-EX', '除草剂', '除草剤'),
    'item.mio_icif.weed_ex_bucket': ('Weed-EX Bucket', '除草剂桶', '除草剤入りバケツ'),
    'item.mio_icif.normal.item_weedex': ('Weed-EX', '除草剂', '除草剤'),
    'tooltip.mio_icif.milk_wart': (
        'Eat to clear status effects, like milk. Counts as milk in recipes.',
        '食用后像牛奶一样清除状态效果。配方中可当作牛奶。',
        '食べると牛乳のように状態効果が消える。レシピでは牛乳として使える。'),
    # %1$s stored mB, %2$s capacity mB
    'gui.mio_icif.crop_scanner.tip.uu_tank': (
        'UU tank: %1$s / %2$s mB',
        'UU 储罐：%1$s / %2$s mB',
        'UU タンク：%1$s / %2$s mB'),
    'gui.mio_icif.crop_scanner.tip.uu_tank_hint': (
        'UU-Matter cells and buckets in the catalyst slot fill it. 1 mB = 1 catalyst.',
        '放入催化剂槽的 UU 物质单元和桶会倒进储罐。1 mB = 1 个催化剂。',
        '触媒スロットに入れた UU 物質セルやバケツがタンクに入る。1 mB = 触媒 1 個。'),
    'gui.mio_icif.crop_scanner.tip.slot_catalyst': (
        'Catalyst: UU-Matter, or UU-Matter cells and buckets (1 for each 3 raised points)',
        '催化剂：UU 物质，或 UU 物质单元与桶（每提升 3 点需 1 个）',
        '触媒：UU 物質、または UU 物質セルやバケツ（上げる 3 点ごとに 1 個）'),
}

# Japanese only: the fluid type names were not translated (the game showed English).
JA_FLUIDS = {
    'fluid_type.mio_icif.biogas': 'バイオガス',
    'fluid_type.mio_icif.hotwater': '熱水',
    'fluid_type.mio_icif.biomass': 'バイオマス',
    'fluid_type.mio_icif.constructionfoam': '建築用フォーム',
    'fluid_type.mio_icif.coolant': '冷却液',
    'fluid_type.mio_icif.distilledwater': '蒸留水',
    'fluid_type.mio_icif.hotcoolant': '高温冷却液',
    'fluid_type.mio_icif.pahoehoelava': 'パホイホイ溶岩',
    'fluid_type.mio_icif.steam': '蒸気',
    'fluid_type.mio_icif.superheatedsteam': '過熱蒸気',
    'fluid_type.mio_icif.uumatter': 'UU 物質',
    'fluid_type.mio_icif.air': '圧縮空気',
    'fluid_type.mio_icif.crudeoil': '原油',
    'fluid_type.mio_icif.dieseloil': 'ディーゼル燃料',
    'gui.mio_icif.matron.weedex_tooltip': '除草剤：%s / 2000 mB',
}

# Set only when the key is missing (keeps existing translations).
IF_MISSING = {
    'item.mio_icif.resource.item_milk_wart': ('Milk Wart', '牛奶疣', '牛乳イボ'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        count = 0
        for key, t in KEYS.items():
            data[key] = t[i]
            count += 1
        if lang == 'ja_jp':
            for key, text in JA_FLUIDS.items():
                data[key] = text
                count += 1
        for key, t in IF_MISSING.items():
            if key not in data:
                data[key] = t[i]
                count += 1
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', count)


if __name__ == '__main__':
    main()
