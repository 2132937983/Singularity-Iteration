#!/usr/bin/env python3
"""0.1.10.30: the guide lines of the release, en / zh / ja. Quantum suit upgrades page: the suit glow and the palette of the quantum
suit; Armor Showcase page: the hologram. Each block goes after its anchor line (the line that starts with the anchor text).
Idempotent; keeps the line endings of edited files. With --check it only reports what is missing.
Usage: data_1130.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
GUIDE = os.path.join(TREE, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual/')
missing = []

SUIT = {
    '': ('Each installed unit adds a switch to the equipment console.',
         'The nano and quantum suits glow: their lights and circuit lines shine in the light colour, amber below half charge and red '
         'below a quarter, with a slow breath, an energy band that runs down the body and a heartbeat when the charge is low. An empty '
         'piece is dark. When a charged piece comes on (put on, the Armory suit-up, charged from empty) the light spreads from the '
         'chest core. Shader packs treat the light as emissive. Client options suit_glow and suit_glow_strength, or the Look page of '
         'the equipment console.\n'
         'Since 0.1.10.30 the quantum suit takes a palette like the nano suit (equipment console, Look page, Palette): the plate '
         'colour and the light colour of each piece. The light colour also colours the glowing parts of a frame core exterior.\n'),
    '_zh_cn/': ('每个已安装单元在装备控制台中增加一个开关。',
                '纳米套装和量子套装会发光：灯光和电路线以发光色闪耀，电量低于一半变为琥珀色，低于四分之一变为红色；有缓慢的呼吸、沿身体流下的'
                '能量带，电量低时还有心跳般的闪动。没电的部件是暗的。充有电的部件启用时（穿上、武装库装着、从空电充电）光从胸口核心扩散开。'
                '光影包会把这种光当作自发光处理。客户端选项 suit_glow 和 suit_glow_strength，或装备控制台的外观页。\n'
                '自 0.1.10.30 起量子套装也能像纳米套装一样设置配色（装备控制台→外观页→配色）：每个部件的装甲色和发光色。发光色也会'
                '为框架核心外装的发光部分上色。\n'),
    '_ja_jp/': ('取り付けた各ユニットは装備コンソールにスイッチを加える。',
                'ナノスーツと量子スーツは光る。ライトと回路ラインが発光色で光り、充電が半分を切ると琥珀色、4 分の 1 を切ると赤になる。ゆっくり'
                '呼吸するように明滅し、エネルギーの帯が体を流れ下り、残量が少ないと鼓動のように瞬く。空の部位は暗い。充電済みの部位が'
                '起動すると（着用、武装庫の装着、空からの充電）、光が胸のコアから広がる。シェーダーパックはこの光を発光として扱う。'
                'クライアント設定 suit_glow と suit_glow_strength、または装備コンソールの外観ページで切り替える。\n'
                '0.1.10.30 から量子スーツもナノスーツと同じく配色を設定できる（装備コンソール→外観ページ→配色）：部位ごとの装甲色と発光色。'
                '発光色はフレームコア外装の光る部分の色にもなる。\n'),
}
SHOWCASE = {
    '': ('- The console exchange works only with showcases near you or near the Armory.',
         '- The outfit is shown as a hologram: see-through cyan light with a hologram grid, a scan band rising through it and short '
         'glitches now and then. When the outfit changes, the new one builds up from the feet along a scan line.\n'),
    '_zh_cn/': ('- 控制台换装只适用于你或武装库附近的展示柜。',
                '- 装备以全息影像显示：半透明的青色光、全息网格、向上扫过的扫描带，偶尔有短暂的错位闪烁。装备更换时，新装备沿扫描线'
                '从脚下开始构建出来。\n'),
    '_ja_jp/': ('- コンソールの換装は、自分か武装庫の近くのショーケースだけが対象だ。',
                '- 装備はホログラムで映る：透けるシアンの光とホログラムの格子、下から上へ走るスキャンの帯、ときどき短い横ずれのグリッチ。'
                '装備が変わると、新しい装備がスキャンラインに沿って足元から組み上がる。\n'),
}

for pages, page in ((SUIT, 'suit/index.md'), (SHOWCASE, 'machines/producer_block_armor_showcase.md')):
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
print('data_1130 written')
