#!/usr/bin/env python3
"""0.1.10.31 language values (en_us / zh_cn / ja_jp): the client options motion_fx and visor_glass, the Look page switch of the
visor glass, the display mode messages of the holo projector. Idempotent; keeps the line endings of each file. With --check it
only reports missing or different keys. Usage: lang_1131.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
F = 'mio_icif.configuration.'

KEYS = {
    F + 'motion_fx': ('Suit motion effects', '套装动作效果', 'スーツの動作エフェクト'),
    F + 'motion_fx.tooltip': (
        'Quantum suit in normal mode: back and sole jets in jetpack flight, the take-off ring of a boost jump, the shock ring of a '
        'hard landing, the air streaks of the sprint. Also the ion trail, re-entry glow and seam light of Armory pieces.',
        '普通模式下的量子套装：喷气背包飞行时背部与脚底的喷射、强化跳跃的起跳光环、重落地的冲击环、疾跑时的气流线。'
        '以及武装库飞行部件的离子尾迹、再入发光与接缝光线。',
        '通常モードの量子スーツ：ジェットパック飛行の背中と足裏の噴射、ブーストジャンプの踏み切りリング、強い着地の衝撃リング、'
        '疾走時の気流の筋。武装庫のパーツのイオン軌跡・突入発光・継ぎ目の光も含む。'),
    F + 'visor_glass': ('Visor glass', '面罩玻璃', 'バイザーガラス'),
    F + 'visor_glass.tooltip': (
        'Off by default. In first person with a nano or quantum helmet: a faint rim, colour fringes, a reflection that slides as you '
        'turn, raindrops in the rain, cracks when the helmet is almost empty.',
        '默认关闭。第一人称佩戴纳米或量子头盔时：淡淡的边缘、彩色边纹、随转头滑动的反光、雨中的水滴、头盔几乎没电时的裂纹。',
        '既定はオフ。ナノか量子のヘルメットを着けた一人称で：かすかな縁、色のにじみ、振り向くと流れる映り込み、雨粒、'
        'ヘルメットがほぼ空のときのヒビ。'),
    'gui.mio_icif.equipment_console.appearance.visor_glass': ('Visor glass (first person)', '面罩玻璃（第一人称）',
                                                              'バイザーガラス（一人称）'),
    'message.mio_icif.projector.mode_physical': ('Physical display: the real gear on the cradle', '实体展示：实物装备装在支架上',
                                                 '実体展示：本物の装備を台座のアームに固定'),
    'message.mio_icif.projector.mode_hologram': ('Hologram display', '全息展示', 'ホログラム展示'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    bad = 0
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        if CHECK:
            stray = [k for k, t in KEYS.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t[i])]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
            wrong = [k for k, t in KEYS.items() if data.get(k) != t[i]]
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in KEYS.items():
            data[key] = t[i]
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(KEYS))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
