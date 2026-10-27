#!/usr/bin/env python3
"""0.1.10.32: the guide lines of the release, en / zh / ja. Quantum suit upgrades page: the hit ripple, the suit boot, the moving
parts, the Look page preview and the simpler effects far away; Armory page: the gantry (iris hatch, suit icons, maintenance
rings), the transfer gates and the console preview. Each block goes after its anchor line (the line that starts with the anchor
text). Idempotent; keeps the line endings of edited files. With --check it only reports what is missing.
Usage: data_1132.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
GUIDE = os.path.join(TREE, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual/')
missing = []

SUIT = {
    '': ('Visor glass (client option visor_glass',
         'Since 0.1.10.32: when the quantum armour absorbs a hit (with or without the deflector), a hexagonal ring of light runs over '
         'the suit from the hit. When the Armory locks the last piece of a suit-up, the suit boots: the light spreads from the chest '
         'core, a ring runs from the core and the visor lights from its centre outwards (the visor also does so when the helmet comes '
         'on and when your HUD boots). In flight the back vents slide open and glow; while sprinting the shoulder plates slide back a '
         'little. The Look page of the equipment console shows your suit in 3D beside the window (drag to turn, wheel to zoom); on '
         'the Palette page it wears the colours you are setting, and a scan line runs down it when they change. Far away the effects '
         'get simpler (client options hit_ripple, suit_gimmicks, fx_lod, fx_budget).\n'),
    '_zh_cn/': ('面罩玻璃（客户端选项',
                '自 0.1.10.32 起：量子装甲吸收攻击时（有无偏导护盾都会），六角形光环从受击点沿套装扩散。武装库扣上一次换装的最后一个部件时，'
                '套装启动：光从胸口核心扩散，一道光环从核心放出，面罩从中央向两侧亮起（头盔启用时和你的 HUD 启动时面罩也会这样亮起）。'
                '飞行时背部排气口滑开并发光；疾跑时肩甲稍稍向后滑动。装备控制台的外观页在窗口旁以 3D 显示你的套装（拖动旋转、滚轮缩放）；'
                '在配色页它穿着你正在设置的颜色，颜色改变时扫描线从上到下扫过。远处的特效会简化（客户端选项 hit_ripple、suit_gimmicks、'
                'fx_lod、fx_budget）。\n'),
    '_ja_jp/': ('バイザーガラス（クライアント設定',
                '0.1.10.32 から：量子装甲が攻撃を吸収すると（デフレクターの有無にかかわらず）、被弾点から六角形の光の輪がスーツを広がる。'
                '武装庫が装着の最後のパーツを固定するとスーツが起動する：胸のコアから光が広がり、コアから輪が走り、バイザーが中央から左右へ'
                '灯る（ヘルメットの起動時と HUD の起動時にもバイザーはこう灯る）。飛行中は背中の排気口が開いて光り、疾走中は肩装甲が少し'
                '後ろへずれる。装備コンソールの外観ページはウィンドウの横にスーツを 3D で表示する（ドラッグで回転、ホイールで拡大）。配色'
                'ページでは設定中の色で表示し、色を変えるとスキャンラインが上から下へ走る。遠くのエフェクトは簡略化される（クライアント設定 '
                'hit_ripple、suit_gimmicks、fx_lod、fx_budget）。\n'),
}
ARMORY = {
    '': ('- Each summon costs EU.',
         '- Since 0.1.10.32 the Armory is a gantry: an iris hatch on top twists open before a piece launches and as a returning piece '
         'comes in; small holograms of the stored suits circle above it (within 8 blocks); while it repairs and charges a ring with '
         'running arcs lies on the floor and a scan ring moves along its sides (amber or violet blinking: waiting for power or mana). '
         'A summon from far away or from another dimension opens a transfer gate in the sky for each piece: the piece leaves it with '
         'afterimages, and a piece that flies home to the sky goes into a gate. The console shows the suit under the mouse as a '
         'hologram beside its window, and a frame in the colour of the charge round each stored suit piece. Client option armory_fx.\n'),
    '_zh_cn/': ('- 每次召唤消耗EU。',
                '- 自 0.1.10.32 起武装库成为机库：顶部的光圈舱门在部件发射前和返回部件进入时旋开；所存套装的小全息图标在上方环绕（8 格内）；'
                '维修和充电时地面出现带流动光弧的光环，扫描环沿侧面上下移动（琥珀色或紫色闪烁：等待电力或魔力）。远距离或跨维度召唤会在'
                '空中为每个部件打开空间传送门：部件带着残影飞出，飞回空中的部件则进入传送门。控制台在窗口旁以全息显示鼠标所指的套装，'
                '并在每个所存部件周围显示电量颜色的边框。客户端选项 armory_fx。\n'),
    '_ja_jp/': ('- 召喚ごとにEUを使う。',
                '- 0.1.10.32 から武装庫はガントリーになる：上面のアイリスハッチがパーツの発射前と帰還パーツの到着時にねじれて開き、収納スーツの'
                '小さなホログラムが上を回り（8 ブロック以内）、整備・充電中は床に光の弧が走るリングが現れ、スキャンリングが側面を上下する'
                '（琥珀・紫の点滅は電力・マナ待ち）。遠隔や別次元からの召喚では、空中にパーツごとの空間転送ゲートが開き、パーツは残像を引いて'
                '出てくる。空へ帰るパーツはゲートに吸い込まれる。コンソールはマウスを置いたスーツをウィンドウの横にホログラムで表示し、収納'
                'パーツの周りに充電量の色の枠を出す。クライアント設定 armory_fx。\n'),
}

for pages, page in ((SUIT, 'suit/index.md'), (ARMORY, 'machines/producer_block_armory.md')):
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

if CHECK:
    print('missing or different:', missing)
    sys.exit(1 if missing else 0)
print('data_1132 written')
