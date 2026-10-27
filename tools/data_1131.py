#!/usr/bin/env python3
"""0.1.10.31: the guide lines of the release, en / zh / ja. Quantum suit upgrades page: the thrusters and motion effects of the
normal mode, the flame look and the visor glass; Armor Showcase page: the physical display and its switch. Each block goes after
its anchor line (the line that starts with the anchor text). Also fixes a word of the 0.1.10.30 zh line (武装库装着 -> 由武装库穿戴).
Idempotent; keeps the line endings of edited files. With --check it only reports what is missing.
Usage: data_1131.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
GUIDE = os.path.join(TREE, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual/')
missing = []

SUIT = {
    '': ('Since 0.1.10.30 the quantum suit takes a palette',
         'Since 0.1.10.31 the quantum suit shows its thrust in normal mode too: in jetpack flight two jets burn under the back unit '
         '(and at the soles with the quantum boots); a boost jump of the quantum boots leaves a ring and a dust ring at the take-off '
         'point and the soles flare; a hard landing (3.5 blocks and more) raises a shock ring; the quantum leggings draw thin air '
         'streaks from the calves while you sprint. Other players see it too. The flames are animated and turn from white through '
         'blue to violet with the thrust, and throw sparks at full thrust. Client option motion_fx.\n'
         'Visor glass (client option visor_glass or the Look page, off by default): in first person with a nano or quantum helmet, '
         'a faint rim and colour fringes, a reflection that slides as you turn, raindrops in the rain and cracks when the helmet is '
         'almost empty. In Special Maneuver hover your own palm thrusters show in first person; a gauntlet flashes when its hand '
         'fires an energy weapon.\n'),
    '_zh_cn/': ('自 0.1.10.30 起量子套装也能像纳米套装一样设置配色',
                '自 0.1.10.31 起量子套装在普通模式下也会显示推力：喷气背包飞行时背部单元下方的两个喷口喷火（穿着量子靴时脚底也会）；'
                '量子靴的强化跳跃会在起跳点留下光环和尘土环，脚底喷出火焰；重落地（3.5 格以上）会激起冲击环；疾跑时量子护腿从小腿拖出细细的'
                '气流线。其他玩家也能看到。火焰带动画，随推力从白色经蓝色变为紫色，全推力时迸出火花。客户端选项 motion_fx。\n'
                '面罩玻璃（客户端选项 visor_glass 或外观页，默认关闭）：第一人称佩戴纳米或量子头盔时，有淡淡的边缘和彩色边纹、随转头滑动的'
                '反光、雨中的水滴，头盔几乎没电时出现裂纹。特殊机动悬停时第一人称可以看到自己掌心的推进器；用能量武器射击时，该手的护手会闪亮。\n'),
    '_ja_jp/': ('0.1.10.30 から量子スーツもナノスーツと同じく配色を設定できる',
                '0.1.10.31 から量子スーツは通常モードでも推力を見せる。ジェットパック飛行では背中のユニットの下の 2 つの噴射口が燃え'
                '（量子ブーツを履いていれば足裏も）、量子ブーツのブーストジャンプは踏み切り地点にリングと砂煙の輪を残して足裏が噴き、'
                '強い着地（3.5 ブロック以上）は衝撃リングを立て、量子レギンスは疾走中にふくらはぎから細い気流の筋を引く。'
                '他のプレイヤーからも見える。炎はアニメーションし、推力に応じて白から青、紫へ変わり、全開では火の粉を散らす。'
                'クライアント設定 motion_fx。\n'
                'バイザーガラス（クライアント設定 visor_glass または外観ページ、既定オフ）：ナノか量子のヘルメットを着けた一人称で、'
                'かすかな縁と色のにじみ、振り向くと流れる映り込み、雨粒、ヘルメットがほぼ空のときのヒビ。特殊機動のホバー中は自分の'
                '手のひらの噴射が一人称で見え、エネルギー武器を撃つとその手のガントレットが光る。\n'),
}
SHOWCASE = {
    '': ('5. Apply a redstone signal to the showcase to spin the turntable.',
         '6. Right-click a side of the base plate with an empty hand to switch between the hologram and the physical display (the '
         'top of the plate still turns the mannequin).\n'),
    '_zh_cn/': ('5. 向展示柜输入红石信号，使转台旋转。',
                '6. 空手右键底板的侧面，在全息展示和实体展示之间切换（右键底板顶面仍是转动人台）。\n'),
    '_ja_jp/': ('5. ショーケースにレッドストーン信号を入れ、ターンテーブルを回す。',
                '6. 素手で台座の側面を右クリックし、ホログラム展示と実体展示を切り替える（台座の上面はこれまでどおりマネキンを回す）。\n'),
}
PHYSICAL = {
    '': ('- The outfit is shown as a hologram:',
         '- Physical display: the real gear stands on the graphite mannequin in the light of the world, held by a mechanical '
         'cradle (a mast behind the figure, clamp arms to the shoulders and the hips) that unfolds when you switch; a nano or '
         'quantum suit keeps its breathing glow. Each projector keeps its own mode.\n'),
    '_zh_cn/': ('- 装备以全息影像显示：',
                '- 实体展示：实物装备穿在石墨人台上，受世界光照，由机械支架固定（人像背后的立柱、伸向肩部和腰部的夹臂），切换时支架会展开；'
                '纳米或量子套装保持呼吸般的发光。每台投影仪各自保存模式。\n'),
    '_ja_jp/': ('- 装備はホログラムで映る：',
                '- 実体展示：本物の装備がグラファイトのマネキンに着せられ、世界の光で照らされ、機械式の台座（人形の背後の支柱と、'
                '肩と腰へ伸びる固定アーム）で支えられる。切り替えると台座が展開する。ナノ・量子スーツは呼吸する発光を保つ。'
                'モードは投影機ごとに保存される。\n'),
}
FIXES = {'_zh_cn/suit/index.md': ('（穿上、武装库装着、从空电充电）', '（穿上、由武装库穿戴、从空电充电）')}

for pages, page in ((SUIT, 'suit/index.md'), (SHOWCASE, 'machines/producer_block_armor_showcase.md'),
                    (PHYSICAL, 'machines/producer_block_armor_showcase.md')):
    for prefix, (anchor, lines) in pages.items():
        path = os.path.join(GUIDE + prefix + page)
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        s = raw.decode('utf-8').replace('\r\n', '\n')
        if lines in s:
            continue
        if CHECK:
            missing.append(path)
            continue
        rows = s.split('\n')
        at = [i for i, r in enumerate(rows) if r.startswith(anchor)]
        assert len(at) == 1, (path, anchor, at)
        rows.insert(at[0] + 1, lines.rstrip('\n'))
        s = '\n'.join(rows)
        open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))
        print('guide', path)

for page, (old, new) in FIXES.items():
    path = GUIDE + page
    raw = open(path, 'rb').read()
    s = raw.decode('utf-8')
    if new in s and old not in s:
        continue
    if CHECK:
        missing.append(path + ' (fix)')
        continue
    assert s.count(old) == 1, (path, old)
    open(path, 'wb').write(s.replace(old, new).encode('utf-8'))
    print('fixed', path)

if CHECK:
    print('missing or different:', missing)
    sys.exit(1 if missing else 0)
print('data_1131 written')
