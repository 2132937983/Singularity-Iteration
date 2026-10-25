#!/usr/bin/env python3
"""0.1.10.19 language keys (en_us / zh_cn / ja_jp): the JEI categories "Reactor Layouts" and "Multiblock
Structures", the built-in layouts and blueprints, and the full-run verdict of the guide reactor simulator.
Idempotent; keeps the line endings of each file. With --check it only reports missing or different keys.
Usage: lang_1119.py <repo root> [--check]
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

J = 'jei.mio_icif.'
RL = 'jei.mio_icif.reactor_layout.'
L = 'reactor_layout.mio_icif.'
S = 'structure.mio_icif.'

KEYS = {
    # ---------------------------------------------------------------- categories
    J + 'category.reactor_layouts': ('Reactor Layouts', '反应堆布局', '原子炉レイアウト'),
    J + 'category.structures': ('Multiblock Structures', '多方块结构', 'マルチブロック構造'),

    # ---------------------------------------------------------------- structure view
    J + 'structure.layer_all': ('All %s layers', '全部 %s 层', '全 %s 層'),
    J + 'structure.layer': ('Layer %s/%s', '第 %s/%s 层', '第 %s/%s 層'),
    J + 'structure.layer_help': ('Left click: one layer up · Right click: one layer down',
                                 '左键：上一层 · 右键：下一层', '左クリック: 1 層上へ · 右クリック: 1 層下へ'),
    J + 'structure.view_help': ('Drag: turn · Mouse wheel: zoom', '拖动：旋转 · 滚轮：缩放', 'ドラッグ: 回転 · ホイール: 拡大縮小'),

    # ---------------------------------------------------------------- reactor layout lines
    RL + 'output': ('Output: %s EU/t · Reactor Chambers: %s', '输出：%s EU/t · 反应仓：%s', '出力: %s EU/t · 原子炉チャンバー: %s'),
    RL + 'heat': ('Heat: %s HU/s made, %s HU/s vented', '热量：产生 %s HU/s，散出 %s HU/s', '熱: 発生 %s HU/s、放熱 %s HU/s'),
    RL + 'stable': ('Stable for a full fuel load', '整个燃料周期内稳定', '燃料 1 回分を通して安定'),
    RL + 'hull_rising': ('Hull heats up by %s HU/s: meltdown after %s', '外壳每秒升温 %s HU：%s 后熔毁',
                         '炉殻が毎秒 %s HU 上昇: %s 後にメルトダウン'),
    RL + 'melts': ('Components melt after %s', '%s 后组件熔化', '%s 後に部品が溶融'),
    RL + 'meltdown': ('MELTDOWN after %s', '%s 后熔毁', '%s 後にメルトダウン'),
    RL + 'meltdown_after_melt': ('Components melt after %s, MELTDOWN after %s', '%s 后组件熔化，%s 后熔毁',
                                 '%s 後に部品が溶融、%s 後にメルトダウン'),
    RL + 'blast': ('If it explodes: power %s, radius %s blocks', '若爆炸：威力 %s，半径 %s 格', '爆発した場合: 威力 %s、半径 %s ブロック'),
    RL + 'blast_off': ('Reactor explosions are off in the config', '配置中已关闭反应堆爆炸', 'コンフィグで原子炉の爆発は無効'),
    RL + 'fuel': ('Fuel lasts %s · %s EU per load', '燃料可用 %s · 每次装填 %s EU', '燃料の持続 %s · 1 回分 %s EU'),
    RL + 'unknown': ('Not simulated: unknown component %s', '无法模拟：未知组件 %s', 'シミュレーション不可: 不明な部品 %s'),
    RL + 'model': ('Reactor block model: cold start, new components, a full fuel load; explosion with your config.',
                   '反应堆方块的模型：冷启动、全新组件、完整燃料周期；爆炸按你的配置计算。',
                   '原子炉ブロックと同じモデル: 冷えた炉・新品の部品・燃料 1 回分。爆発は自分のコンフィグで計算。'),

    # ---------------------------------------------------------------- built-in layouts
    L + 'starter': ('Starter: 5 EU/t', '入门：5 EU/t', '入門: 5 EU/t'),
    L + 'starter.note': ('One single uranium rod and four Heat Vents. No chambers.',
                         '一根单联铀棒加四个散热片。不需要反应仓。', '単一ウラン棒 1 本とヒートベント 4 個。チャンバー不要。'),
    L + 'dual_rod': ('Dual rod: 20 EU/t', '双联棒：20 EU/t', 'デュアル棒: 20 EU/t'),
    L + 'dual_rod.note': ('Each of the four Heat Vents gets 6 HU/s, its limit.',
                          '四个散热片各承受 6 HU/s，正好是其上限。', 'ヒートベント 4 個がそれぞれ上限の 6 HU/s を受ける。'),
    L + 'quad_compact': ('Compact quad rod: 60 EU/t', '紧凑四联棒：60 EU/t', 'コンパクト 4 連棒: 60 EU/t'),
    L + 'quad_compact.note': ('One quad rod with four Iridium Heat Vents (24 HU/s each). No chambers.',
                              '一根四联铀棒配四个铱金散热器（各 24 HU/s）。不需要反应仓。',
                              '4 連ウラン棒 1 本とイリジウムヒートベント 4 個（各 24 HU/s）。チャンバー不要。'),
    L + 'dual_rods_six': ('Six dual rods: 120 EU/t', '六根双联棒：120 EU/t', 'デュアル棒 6 本: 120 EU/t'),
    L + 'dual_rods_six.note': ('Heat Vents only. No vent touches two rods. 6 chambers.',
                               '只用散热片，没有散热片同时接触两根燃料棒。需要 6 个反应仓。',
                               'ヒートベントのみ。2 本の燃料棒に接するベントはない。チャンバー 6 個。'),
    L + 'high_output': ('High output: 300 EU/t', '高输出：300 EU/t', '高出力: 300 EU/t'),
    L + 'high_output.note': ('Guide tutorial 3: five quad rods, each with its own Iridium Heat Vents. 4 chambers.',
                             '手册教程 3：五根四联铀棒，各自配有铱金散热器。需要 4 个反应仓。',
                             'ガイドのチュートリアル 3: 4 連棒 5 本、それぞれ専用のイリジウムヒートベント。チャンバー 4 個。'),
    L + 'high_output_plated': ('High output, plated: 300 EU/t', '高输出（加装隔热板）：300 EU/t', '高出力・プレート付き: 300 EU/t'),
    L + 'high_output_plated.note': ('The same design with Containment Reactor Plating in the free slots: the explosion becomes very small.',
                                    '同一设计，在空槽放入密封反应堆隔热板：爆炸变得非常小。',
                                    '同じ設計の空きスロットに封じ込め原子炉プレートを入れる。爆発はとても小さくなる。'),
    L + 'max_output': ('Maximum output: 660 EU/t', '最大输出：660 EU/t', '最大出力: 660 EU/t'),
    L + 'max_output.note': ('Eleven quad rods; each Iridium Heat Vent touches one rod only. 6 chambers. The explosion is at the power limit.',
                            '十一根四联铀棒；每个铱金散热器只接触一根燃料棒。需要 6 个反应仓。爆炸达到威力上限。',
                            '4 連棒 11 本。各イリジウムヒートベントは 1 本の燃料棒にだけ接する。チャンバー 6 個。爆発は威力上限に達する。'),
    L + 'failure_vents': ('Failure: too few vents', '失败示例：散热片不足', '失敗例: ベント不足'),
    L + 'failure_vents.note': ('96 HU/s into three Heat Vents (18 HU/s). The vents melt, then the hull gets all the heat.',
                               '96 HU/s 的热量进入三个散热片（共 18 HU/s）。散热片熔化后，外壳承受全部热量。',
                               '96 HU/s がヒートベント 3 個（計 18 HU/s）に入る。ベントが溶け、炉殻が全熱を受ける。'),
    L + 'failure_reflector': ('Failure that looks safe', '看似安全的失败', '安全に見える失敗例'),
    L + 'failure_reflector.note': ('The reflector adds pulses: 160 HU/s, 108 vented. It looks safe for 100 s, but the vents store the rest and melt.',
                                   '反射板增加脉冲：产生 160 HU/s，只散出 108。前 100 秒看似安全，但散热片积累余热后熔化。',
                                   '反射板がパルスを増やす: 発生 160 HU/s、放熱 108。100 秒間は安全に見えるが、ベントが余熱をため込んで溶ける。'),

    # ---------------------------------------------------------------- structures: titles
    S + 'nuclear_reactor': ('Nuclear Reactor with chambers', '核反应堆与核反应仓', '原子炉とチャンバー'),
    S + 'fluid_reactor': ('Fluid Reactor (5×5×5)', '流体反应堆（5×5×5）', '流体原子炉（5×5×5）'),
    S + 'gesu': ('GESU', 'GESU', 'GESU'),
    S + 'large_fabricator': ('Large Matter Fabricator', '大型物质生成机', '大型物質生成機'),
    S + 'oil_rig': ('Oil Rig', '石油钻机', '油井'),
    S + 'dimension_oil_rig': ('Dimension Oil Rig', '维度石油钻机', '次元油井'),
    S + 'blast_furnace': ('Blast Furnace and heat source', '高炉与热源', '高炉と熱源'),
    S + 'fermenter': ('Fermenter and heat source', '发酵机与热源', '発酵槽と熱源'),

    # ---------------------------------------------------------------- structures: rules and notes
    S + 'nuclear_reactor.chamber': ('Optional: each chamber adds one column', '可选：每个反应仓增加一列', '任意: チャンバー 1 個で 1 列増える'),
    S + 'nuclear_reactor.note.columns': (
        'Each Reactor Chamber on a face of the reactor adds one column of component slots: 3 + chambers, at most 9.',
        '反应堆每个面上的反应仓各增加一列组件槽：列数 = 3 + 反应仓数，最多 9 列。',
        '原子炉の面に付けた原子炉チャンバー 1 個ごとに部品スロットが 1 列増える: 3 + チャンバー数、最大 9 列。'),
    S + 'nuclear_reactor.note.shared': (
        'A chamber that touches two reactors, or no reactor, drops as an item.',
        '同时接触两个反应堆或不接触反应堆的反应仓会掉落为物品。',
        '2 つの原子炉に接する、またはどの原子炉にも接しないチャンバーはアイテムとして落ちる。'),
    S + 'nuclear_reactor.note.chunk': (
        'Keep the reactor and its chambers in one chunk: a chamber in an unloaded chunk is lost to the reactor, with its cooling.',
        '请把反应堆和反应仓放在同一区块内：位于未加载区块的反应仓会脱离反应堆，其冷却也随之失去。',
        '原子炉とチャンバーは同じチャンクに置く。未読み込みチャンクのチャンバーは冷却ごと原子炉から外れる。'),
    S + 'fluid_reactor.shell': ('Reactor Pressure Vessel (any shell position can hold a port instead)', '核反应堆压力容器（任何外壳位置都可改放接口）',
                                '原子炉圧力容器（外殻のどの位置もポートにしてよい）'),
    S + 'fluid_reactor.port': ('A port: any shell position can hold one', '接口：可放在任何外壳位置', 'ポート: 外殻のどの位置にも置ける'),
    S + 'fluid_reactor.note.ports': (
        'The 98 outer blocks are Reactor Pressure Vessels or ports (fluid port, access hatch, redstone port). At least 1 port is necessary.',
        '外层 98 个方块为核反应堆压力容器或接口（流体接口、访问接口、红石接口）。至少需要 1 个接口。',
        '外側の 98 ブロックは原子炉圧力容器かポート（流体ポート、アクセスハッチ、レッドストーンポート）。ポートが 1 個以上必要。'),
    S + 'fluid_reactor.note.inner': (
        'The 6 faces of the reactor need Reactor Chambers. The other 20 inner positions can hold any block.',
        '反应堆的 6 个面必须是核反应仓。其余 20 个内部位置可以放任意方块。',
        '原子炉の 6 面には原子炉チャンバーが必要。残りの内側 20 か所はどのブロックでもよい。'),
    S + 'gesu.input': ('Input module (any face)', '输入模块（任意面）', '入力モジュール（どの面でも可）'),
    S + 'gesu.output': ('IV or LuV output module (any face)', 'IV 或 LuV 输出模块（任意面）', 'IV または LuV 出力モジュール（どの面でも可）'),
    S + 'gesu.note': (
        'A module on each of the 6 faces of the core, any type in any order. The corners are not part of the structure.',
        '核心的 6 个面各放一个模块，类型与顺序任意。角落不属于结构。',
        'コアの 6 面それぞれにモジュールを 1 個。種類も順番も自由。角は構造に含まれない。'),
    S + 'large_fabricator.input': ('Energy input module: at least 1', '能量输入模块：至少 1 个', 'エネルギー入力モジュール: 1 個以上'),
    S + 'large_fabricator.tank': ('Tank module: at least 1', '储罐模块：至少 1 个', 'タンクモジュール: 1 個以上'),
    S + 'large_fabricator.scrap': ('Scrap module: at most 1', '废料模块：最多 1 个', 'スクラップモジュール: 最大 1 個'),
    S + 'large_fabricator.note': (
        'All 6 faces of the core need a module; each module type can be on any face.',
        '核心的 6 个面都需要模块；每种模块都可放在任意面。',
        'コアの 6 面すべてにモジュールが必要。各モジュールはどの面にも置ける。'),
    S + 'oil_rig.input': ('Energy input module: at least 1', '能量输入模块：至少 1 个', 'エネルギー入力モジュール: 1 個以上'),
    S + 'oil_rig.output': ('Product output module: at least 1', '产物输出模块：至少 1 个', '産物出力モジュール: 1 個以上'),
    S + 'oil_rig.any_module': ('Any module type can be here', '此处可放任意类型的模块', 'ここはどの種類のモジュールでもよい'),
    S + 'oil_rig.note': (
        'Each module position takes any module type. The frame blocks are Titanium Drill Frames. The structure has no facing.',
        '每个模块位置可放任意类型的模块。框架方块为钛制钻机框架。结构没有朝向。',
        'モジュールの位置にはどの種類も置ける。フレームはチタン製ドリルフレーム。構造に向きはない。'),
    S + 'dimension_oil_rig.note': (
        'The Oil Rig shape plus a ring of 16 modules one block below the core and more frames. Each module position takes any module type.',
        '在石油钻机形状的基础上，核心下方一格增加 16 个模块组成的环，并增加框架。每个模块位置可放任意类型的模块。',
        '油井の形に、コアの 1 ブロック下の 16 モジュールの環とフレームを加えたもの。モジュールの位置にはどの種類も置ける。'),
    S + 'heat_source': ('Heat source (HU), front to the machine', '热源（HU），正面朝向机器', '熱源（HU）、正面を機械に向ける'),
    S + 'heated.note.front': (
        'The machine takes heat only through its front face, and a heat source gives heat only through its front face: put them front to front.',
        '机器只从正面接收热量，热源也只从正面输出热量：请让两者正面相对。',
        '機械は正面からしか熱を受けず、熱源も正面からしか熱を出さない。正面どうしを向かい合わせる。'),
    S + 'blast_furnace.note': (
        'For the Blast Furnace and the Advanced Titanium Alloy Blast Furnace. The Electric Nano Blast Furnace needs no heat source.',
        '适用于高炉与进阶钛合金高炉。电力纳米高炉不需要热源。',
        '高炉と高級チタン合金高炉が対象。電気ナノ高炉には熱源は不要。'),
    S + 'fermenter.note': (
        'The Fermenter uses the stored heat for each operation.',
        '发酵机每次加工都会消耗储存的热量。',
        '発酵槽は加工ごとに蓄えた熱を使う。'),

    # ---------------------------------------------------------------- guide simulator
    'guide.mio_icif.simulator.melts_later': ('Components melt after %s s', '%s 秒后组件熔化', '%s 秒後に部品が溶融'),
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
    if bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
