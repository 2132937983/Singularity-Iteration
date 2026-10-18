#!/usr/bin/env python3
"""
0.1.10.4 guide pages (en / zh_cn / ja_jp): the heat stages of the nuclear reactor, the energy faces
of the reactor coolant injectors, the short vapour-cone burst of Special Maneuver Mode, the
compute core state in the Entanglement Matrix. Exact replacements; each must match once, and a page
that already has the new text is left alone (idempotent).
Usage: guide_1104.py <repo root>
"""
import os
import sys

ROOT = sys.argv[1]
G = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
LANGS = {'en': '', 'zh': '_zh_cn/', 'ja': '_ja_jp/'}

HEAT = {
    'en': """## Heat stages

From 4,000 HU of hull heat, the reactor checks these stages once per cycle (1 second), by hull heat
divided by the maximum hull heat. Chances and damage are multiplied by the containment factor: 1
without reactor plating, lower with each plate (a Reactor Plating multiplies it by 0.95). When the
server turns reactor explosions off (explosion multiplier or power limit 0), these effects are off too.

| Hull heat / maximum | Effect (centred on the reactor) | Area |
|---|---|---|
| ≥ 40 % | With the containment factor as chance, one random block: a flammable block (planks, logs, leaves, saplings, flowers, crops, grass) turns into fire. | 5×5×5 |
| ≥ 50 % | With the containment factor as chance, one random block: water boils away. Water sources and flowing water vanish, a waterlogged block only loses its water, kelp, seagrass and bubble columns dry up. | 5×5×5 |
| ≥ 70 % | Every cycle, each living creature in the area takes 0 to 3 radiation damage (times the containment factor). A full hazmat suit or a full quantum suit protects completely; the nano toxin filter reduces the damage. | 7×7×7 |
| ≥ 85 % | With 20 % times the containment factor as chance, one random block: air turns into fire; stone, metal and other pickaxe blocks turn into lava; other breakable blocks turn into fire. | 5×5×5 |
| 100 % | The reactor explodes. | - |

Blocks that the heat never changes: unbreakable blocks such as bedrock, blocks with a block entity
(chambers, ports, machines), and the blocks of the data tag `mio_icif:reactor_heat_immune`:
construction foam and its walls, the reactor, the reactor chambers, the Reactor Pressure Vessel and
its interface blocks (access hatch, fluid port, redstone port), the coolant injectors.

> **NOTE:** Around a reactor under water, the lava of the 85 % stage reacts with the water by the
> vanilla rules: a lava source becomes obsidian, flowing lava becomes cobblestone or stone. The
> protected blocks themselves never change, but ordinary blocks next to the reactor (stone, for
> example) can turn into lava first and then into obsidian.

""",
    'zh': """## 堆温阶段

堆温达到 4,000 HU 后，反应堆每个循环（1 秒）按“堆温 / 堆温上限”检查以下阶段。概率与伤害乘以防爆系数：
没有反应堆装甲板时为 1，每块装甲板都会降低它（反应堆装甲板 ×0.95）。服务器关闭反应堆爆炸时（爆炸倍率或威力上限为 0），这些效果也不发生。

| 堆温 / 上限 | 效果（以反应堆为中心） | 范围 |
|---|---|---|
| ≥ 40 % | 以防爆系数为概率选择 1 个随机方块：可燃方块（木板、原木、树叶、树苗、花、作物、草类）变为火。 | 5×5×5 |
| ≥ 50 % | 以防爆系数为概率选择 1 个随机方块：水蒸发。水源与流动水消失，含水方块只失去水，海带、海草与气泡柱干涸。 | 5×5×5 |
| ≥ 70 % | 每个循环，范围内每个生物受到 0–3 点辐射伤害（乘以防爆系数）。全套防化服或全套量子装甲完全防护；纳米毒素过滤器降低伤害。 | 7×7×7 |
| ≥ 85 % | 以 20 % × 防爆系数为概率选择 1 个随机方块：空气变为火；石头、金属等可用镐挖掘的方块变为岩浆；其他可破坏方块变为火。 | 5×5×5 |
| 100 % | 反应堆爆炸。 | - |

高温不会改变的方块：基岩等不可破坏方块、带方块实体的方块（反应仓、端口、机器等），以及数据标签
`mio_icif:reactor_heat_immune` 中的方块：建筑泡沫及其墙、反应堆、反应仓、反应堆压力容器及其接口方块（访问舱口、流体端口、红石端口）、冷却液注入器。

> **注意：** 浸在水中的反应堆，85 % 阶段生成的岩浆会按原版规则与相邻的水反应：岩浆源变为黑曜石，流动岩浆变为圆石或石头。
> 受保护的方块本身不会改变，但反应堆周围的普通方块（例如石头）可能先变为岩浆，再变为黑曜石。

""",
    'ja': """## 炉温の段階

炉の熱が 4,000 HU 以上になると、原子炉はサイクルごと（1 秒）に「炉の熱 / 最大熱容量」で次の段階を判定する。
確率とダメージには封じ込め係数を掛ける：原子炉プレートなしで 1、プレート 1 枚ごとに下がる（原子炉プレートは ×0.95）。
サーバーが原子炉の爆発を無効にしている（爆発倍率か威力上限が 0）とき、これらの効果も起きない。

| 炉温 / 最大 | 効果（原子炉が中心） | 範囲 |
|---|---|---|
| ≥ 40 % | 封じ込め係数の確率でランダムな 1 ブロック：燃えるブロック（板材、原木、葉、苗木、花、作物、草）が火になる。 | 5×5×5 |
| ≥ 50 % | 封じ込め係数の確率でランダムな 1 ブロック：水が蒸発する。水源と流れる水は消え、水浸しのブロックは水だけを失い、コンブ・海草・気泡柱は干上がる。 | 5×5×5 |
| ≥ 70 % | サイクルごとに、範囲内の各生物が 0〜3 の放射線ダメージ（封じ込め係数倍）を受ける。ハズマットスーツ一式か量子スーツ一式で完全に防げる。ナノの毒素フィルターはダメージを減らす。 | 7×7×7 |
| ≥ 85 % | 20 % × 封じ込め係数の確率でランダムな 1 ブロック：空気は火に、石・金属などツルハシで掘るブロックは溶岩に、その他の壊せるブロックは火になる。 | 5×5×5 |
| 100 % | 原子炉が爆発する。 | - |

熱で変わらないブロック：岩盤などの壊せないブロック、ブロックエンティティを持つブロック（チャンバー、ポート、機械など）、
データタグ `mio_icif:reactor_heat_immune` のブロック：建築フォームとその壁、原子炉、原子炉チャンバー、
原子炉圧力容器とそのインターフェースブロック（アクセスハッチ、流体ポート、レッドストーンポート）、冷却材注入器。

> **注意：** 水中の原子炉では、85 % 段階の溶岩がバニラの規則で隣の水と反応する：溶岩源は黒曜石に、流れる溶岩は丸石か石になる。
> 保護されたブロック自体は変わらないが、原子炉のまわりの普通のブロック（石など）は先に溶岩になり、そのあと黒曜石になることがある。

""",
}
NOTES_HEAD = {'en': '## Notes', 'zh': '## 注意', 'ja': '## 注意'}
HEAT_HEAD = {'en': '## Heat stages', 'zh': '## 堆温阶段', 'ja': '## 炉温の段階'}
OLD_HEAT_WARN = {
    'en': '> **WARNING:** At high hull heat, the reactor sets blocks on fire and hurts nearby mobs.',
    'zh': '> **警告：** 堆温较高时，反应堆会点燃方块并伤害附近的生物。',
    'ja': '> **警告：** 炉の温度が高いと、原子炉はブロックに火をつけ、近くのモブを傷つける。',
}
NEW_HEAT_WARN = {
    'en': '> **WARNING:** From 40 % hull heat the reactor sets blocks on fire, boils water, irradiates creatures and makes lava (see Heat stages).',
    'zh': '> **警告：** 堆温达到 40 % 起，反应堆会点燃方块、蒸发水、辐射生物并生成岩浆（见“堆温阶段”）。',
    'ja': '> **警告：** 炉温 40 % から、原子炉はブロックに火をつけ、水を蒸発させ、生物を被曝させ、溶岩を作る（「炉温の段階」を参照）。',
}

INJ = {
    'en': [('2. Connect an MV cable to the machine.',
            '2. Connect an MV (or lower) cable to a free face: the back, a side, the top or the bottom. The front and every face that touches a reactor part take no power.'),
           ('| Use while working | 1,000 EU/t |', '| Use per repair | 1,000 EU |'),
           ('- When the stock is empty, the condensators in the reactor wear out.',
            '- When the stock is empty, the condensators in the reactor wear out.\n'
            '- Since 0.1.10.4 the injector takes no power on its front nor on a face that touches the reactor, a reactor chamber, the pressure vessel or its interface blocks. Before, the reactor fed its own output (up to 8,192 EU per packet) straight into the injector, and the injector exploded from overvoltage as soon as the reactor started.\n'
            '- The injector gives no redstone signal (only a comparator level) and moves no heat.')],
    'zh': [('2. 将MV导线连接到机器。',
            '2. 用MV（或更低）导线连接注入器的空闲面：背面、侧面、顶面或底面。正面以及接触反应堆部件的面不接电。'),
           ('| 工作耗电 | 1,000 EU/t |', '| 每次修复耗电 | 1,000 EU |'),
           ('- 库存用完时，反应堆中的冷凝模块会耗尽。',
            '- 库存用完时，反应堆中的冷凝模块会耗尽。\n'
            '- 自 0.1.10.4 起，注入器的正面以及接触反应堆、反应仓、压力容器或其接口方块的面不再接受电力。此前反应堆的输出（每个电包最高 8,192 EU）会直接灌入注入器，反应堆一启动注入器就因过压爆炸。\n'
            '- 注入器不输出红石信号（只提供比较器读数），也不传递热量。')],
    'ja': [('2. MVケーブルを機械に接続する。',
            '2. MV（以下）のケーブルを空いている面（背面、側面、上面、下面）に接続する。正面と原子炉部品に接する面は受電しない。'),
           ('| 稼働時消費 | 1,000 EU/t |', '| 修理1回の消費 | 1,000 EU |'),
           ('- 在庫がなくなると、原子炉のコンデンセーターは消耗しきる。',
            '- 在庫がなくなると、原子炉のコンデンセーターは消耗しきる。\n'
            '- 0.1.10.4 から、注入器は正面と、原子炉・原子炉チャンバー・圧力容器・そのインターフェースブロックに接する面では受電しない。以前は原子炉の出力（1 パケット最大 8,192 EU）がそのまま注入器に流れ込み、原子炉を起動した瞬間に注入器が過電圧で爆発した。\n'
            '- 注入器はレッドストーン信号を出さず（コンパレーター出力のみ）、熱も移さない。')],
}

MANEUVER = {
    'en': ('At throttle 60 % (48 m/s) the suit breaks the shock barrier. You hear a boom and see a vapor cone and a shock ring.',
           'At throttle 60 % (48 m/s) the suit breaks the shock barrier: a boom sounds, and a vapor cone and a shock ring flash up for about 0.6 s. The cone does not stay in front of the view while you cruise.'),
    'zh': ('油门达到60%（48 m/s）时套装突破音障。你会听到爆鸣并看到蒸汽锥与激波环。',
           '油门达到60%（48 m/s）时套装突破音障：爆鸣响起，蒸汽锥与激波环一闪而过（约 0.6 秒）。巡航时蒸汽锥不会持续挡在视野前。'),
    'ja': ('スロットル60 %（48 m/s）でスーツは音速の壁を突破する。衝撃音が鳴り、ベイパーコーンと衝撃波リングが出る。',
           'スロットル60 %（48 m/s）でスーツは音速の壁を突破する。衝撃音が鳴り、ベイパーコーンと衝撃波リングが一瞬（約 0.6 秒）現れて消える。巡航中にコーンが視界を覆い続けることはない。'),
}

CORE = {
    'en': ('adds 8 QB to the budget and one more link to the hub.',
           'adds 8 QB to the budget and one more link to the hub.\n'
           'Only a working core counts: switched on in the panel and powered by the suit. The footer of the matrix shows its state '
           '(Core ON, switched off, or no suit EU); the links row of the Equipment Console shows CPU, CPU- or CPU!.'),
    'zh': ('使带宽预算增加 8 QB，中枢链接增加 1 条。',
           '使带宽预算增加 8 QB，中枢链接增加 1 条。\n'
           '只有工作中的核心生效：面板开关打开，且套装有电。矩阵底部显示它的状态（运行、已关闭、缺电）；装备控制台的链接行显示 CPU、CPU- 或 CPU!。'),
    'ja': ('は帯域予算を 8 QB 増やし、ハブのリンクを 1 本増やす。',
           'は帯域予算を 8 QB 増やし、ハブのリンクを 1 本増やす。\n'
           '効くのは稼働中のコアだけ（パネルのスイッチがオンで、スーツに電力がある）。マトリクス下部に状態（稼働・オフ・電力不足）、装備コンソールのリンク行に CPU・CPU-・CPU! を表示する。'),
}


def edit(path, pairs):
    with open(path, encoding='utf-8') as f:
        s = f.read()
    changed = False
    for old, new in pairs:
        if new in s:
            continue
        if s.count(old) != 1:
            raise SystemExit('%s: expected one "%s"' % (path, old[:40]))
        s = s.replace(old, new)
        changed = True
    if changed:
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(s)
    return changed


def main():
    n = 0
    for lang, prefix in LANGS.items():
        reactor = os.path.join(G, prefix + 'machines/generator_block_nuclear_reactor_generator.md')
        with open(reactor, encoding='utf-8') as f:
            s = f.read()
        if HEAT_HEAD[lang] not in s:
            n += edit(reactor, [(NOTES_HEAD[lang] + '\n', HEAT[lang] + NOTES_HEAD[lang] + '\n'), (OLD_HEAT_WARN[lang], NEW_HEAT_WARN[lang])])
        for kind in ('redstone', 'lapis'):
            page = os.path.join(G, prefix + 'machines/producer_block_%s_reactor_coolant_injector.md' % kind)
            n += edit(page, INJ[lang])
        n += edit(os.path.join(G, prefix + 'suit/maneuver_mode.md'), [MANEUVER[lang]])
        n += edit(os.path.join(G, prefix + 'suit/entanglement.md'), [CORE[lang]])
    print('guide pages changed:', n)


if __name__ == '__main__':
    main()
