#!/usr/bin/env python3
"""0.1.10.1 guide pages (English, Japanese, Chinese):
  * new page suit/entanglement.md: links, the Entanglement Matrix, the nine synergies, the chain
    combos, the compute bandwidth, the compute core and the guidance / resonance field Mk.II
  * ore scanner, grid telemetry, entity ESP and ballistic unit pages: a Mk.II section and the Mk.II recipe
  * quantum actions page: a short combo section before the expansion bay
  * suit index: the compute core row and one line about the links
Inserts each section once (checked by its content). GuideME parses MDX: no HTML comment, no bare '<' or '{' in text.
Usage: guide_1101.py <repo root>
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
G = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
LANGS = {'en': '', 'ja': '_ja_jp', 'zh': '_zh_cn'}

PAGE_ITEMS = ['compute_core', 'smart_guidance_mk2', 'resonance_field_mk2']
IDS = ['mio_icif:module/item_module_' + i for i in PAGE_ITEMS]


def link(name):
    return f'<ItemLink id="mio_icif:module/item_module_{name}" />'


def front(title):
    ids = '\n'.join('  - ' + i for i in IDS)
    return f'''---
navigation:
  title: "{title}"
  icon: mio_icif:module/item_module_compute_core
  parent: suit/index.md
  position: 4
item_ids:
{ids}
---
'''


def recipes(heading):
    lines = [f'## {heading}', '']
    for i in IDS:
        lines.append(f'<RecipesFor id="{i}" fallbackText="-" />')
        lines.append('')
    return '\n'.join(lines)


SG, BP, ESP, TS, HM, OS, BC, BW, DF, RF, PJ, PM, ER = (link(u) for u in (
    'smart_guidance', 'behavior_predictor', 'entity_esp', 'threat_sensor', 'holomap', 'ore_scanner', 'ballistic',
    'blast_warning', 'deflector', 'resonance_field', 'phase_jump_drive', 'phase_modulator', 'entanglement_relay'))
CC = link('compute_core')

PAGE = {
    'en': front('Entanglement and synergies') + f'''
# Entanglement and synergies

Since 0.1.10.1 two quantum units can be **entangled**. An entangled pair works together as a **synergy**: an effect that neither unit has alone.

## Links

- The quantum chestplate is the **link hub**: it stores the links. Without a quantum chestplate no synergy works.
- The hub holds **2 links**, 3 with an active {CC}.
- Both units of a pair must be installed on worn quantum pieces. A synergy works while both units are switched on and powered.
- Links stay stored on the chestplate when you change units; they work again when the units come back. A link to a unit of a removed mod stays stored but does nothing.

## Entanglement Matrix

Open the Singularity Management Panel, select the quantum chestplate and click the **Links** row under "Quantum core".

- Left: your quantum units as nodes on a ring. A faint dashed thread: the pair can be linked. A solid thread: linked, but a unit is off. A pulsing double helix: the synergy works.
- Right: every synergy and its state (LINKED, READY, OFFLINE, OVER CAP, MISSING). Click a row to link or unlink it.
- You can also **drag** one unit onto another. While you drag, the synergy of the pair is shown; release to link or unlink.
- The footer shows the links in use and the compute bandwidth.

On the HUD the working synergies show as small chips beside the crosshair.

## Synergies

| Synergy | Units | Effect |
|---|---|---|
| Predictive Guidance | {SG} + {BP} | Guided shots fly to where the target will be, along the path a creature walks. Turn +4 degrees per tick, 2 more locks. |
| Threat Sight | {ESP} + {TS} | Creatures that hunt you glow red through walls, with a ring that counts down to their attack. |
| Ore Hologram | {HM} + {OS} | The scanned ores appear as 3D voxels in the holomap and the tactical map. |
| Ballistic Sync | {BC} + {SG} | Locks take 30 % less time; the planned paths of the guided shots are drawn. |
| Blast Forecast | {BW} + {BP} | A coming blast shows its radius as a 3D sphere, and an arrow points the way out. |
| Resonant Deflection | {DF} + {RF} | 25 % of the damage the deflector stops charges a resonance. At 8 it bursts and hits the hostile creatures within 4 blocks. |
| Afterimage Decoy | {PJ} + {BP} | A blink leaves a decoy at the start point. For 2 s the creatures that hunted you walk to it and cannot take you back. |
| Phase Counter | {PM} + {DF} | Damage that passed through you during a phase shift (up to 20) returns as a shock wave when it ends. |
| Fleet Datalink | {ER} + {ESP} | The hostile creatures your Wingmen see appear on your HUD; the Wingmen attack the creature in your sights. |

Each synergy has an on/off switch in the server config (section QuantumSuit, synergies). Synergy bursts hit players only with PvP on.

## Chain combos

Combos need no link, only timing. A banner and a sound show each combo.

- **Quantum Tunnel:** a Phase Shift within 1 s after a Quantum Blink costs half the coherence.
- **Frozen Volley:** an Entangled Volley inside your own Time Dilation. Up to 8 slowed creatures become targets, the bolts strike at once without the charge time, and they deal 1.5 times the damage.
- **Static Barrage:** switch your Force Field off with the key, and every projectile it holds flies back at its shooter as your shot.

## Compute bandwidth

Every active unit on a quantum piece loads the suit computer, measured in QB. A unit installed on two pieces counts once; a Mk.II unit costs 1 QB more.

| QB | Units |
|---|---|
| 3 | biometric scanner (ESP), behavior predictor, holomap, ore scanner, phase modulator, entanglement relay |
| 2 | smart guidance, threat sensor, phase jump drive |
| 1 | ballistic computer, blast warning, grid telemetry, deflector, resonance field |
| 0 | all other units |

The budget is **20 QB**, 28 QB with an active compute core. Every suit that 0.1.9.2 allowed fits in 20 QB. Over the budget the suit **saturates**: the sensors refresh at half rate and the coherence regenerates at half speed. Nothing switches off. The QB meter near the crosshair turns red.

## Quantum Compute Core

The {CC} (chestplate or leggings, 6 EU/t) adds 8 QB to the budget and one more link to the hub.

## Mk.II: guidance and resonance field

- {link('smart_guidance_mk2')}: up to 10 locks, guided shots turn up to 20 degrees per tick (Mk.I: 6 locks, 14 degrees).
- {link('resonance_field_mk2')}: radius 3.75 and up to 9 blocks per tick (Mk.I: radius 2.5, 6 blocks).

The ore scanner, grid telemetry, biometric scanner and ballistic computer also have a Mk.II: see their pages.

''' + recipes('Recipes'),
    'ja': front('もつれとシナジー') + f'''
# もつれとシナジー

0.1.10.1 から、2 つの量子ユニットを **もつれ** で結べる。結んだ組は **シナジー** として働き、単体では持たない効果を生む。

## リンク

- 量子チェストが **リンクハブ** で、リンクを保存する。量子チェストを着けていなければシナジーは働かない。
- ハブのリンクは **2 本**。{CC} が有効なら 3 本。
- 組の両ユニットが、装着中の量子部位に取り付けられている必要がある。両方がオンで給電されている間だけシナジーが働く。
- ユニットを付け替えてもリンクはチェストに残り、ユニットが戻れば再び働く。外された MOD のユニットへのリンクは保存されたまま何もしない。

## もつれマトリクス

特異点管理パネルを開き、量子チェストを選んで「量子コア」の下の **リンク** 行をクリックする。

- 左：量子ユニットが円周上のノードとして並ぶ。薄い破線：リンクできる組。実線：リンク済みだが片方がオフ。脈打つ二重らせん：シナジーが働いている。
- 右：全シナジーと状態（LINKED、READY、OFFLINE、OVER CAP、MISSING）。行をクリックするとリンク／解除。
- ユニットを別のユニットへ **ドラッグ** してもよい。ドラッグ中はその組のシナジーが表示され、離すとリンク／解除する。
- 下部に使用中のリンク数と演算帯域が出る。

HUD では、働いているシナジーが照準の横に小さなチップで表示される。

## シナジー

| シナジー | ユニット | 効果 |
|---|---|---|
| 予測誘導 | {SG} + {BP} | 誘導弾は目標が来る位置へ飛ぶ。生物が歩く経路に沿って予測する。旋回 +4 度/tick、ロック +2。 |
| 脅威透視 | {ESP} + {TS} | 自分を狙う敵が壁越しに赤く光り、攻撃が届くまでの時間をリングで示す。 |
| 地脈ホロ | {HM} + {OS} | 震探で見つけた鉱石を、ホロマップと戦術マップに 3D ボクセルで表示する。 |
| 弾道同期 | {BC} + {SG} | ロック時間 -30%。誘導弾の予定軌道を描く。 |
| 爆心予報 | {BW} + {BP} | 爆発の半径を 3D の球で示し、退避方向を矢印で示す。 |
| 共鳴偏向 | {DF} + {RF} | 偏向器が止めたダメージの 25% を共鳴として蓄え、8 に達すると放出して半径 4 ブロックの敵に当てる。 |
| 残像欺瞞 | {PJ} + {BP} | 転移の出発点に残像デコイが残る。2 秒間、自分を狙っていた敵はデコイへ向かい、自分を狙い直せない。 |
| 位相反撃 | {PM} + {DF} | 位相シフト中にすり抜けたダメージ（最大 20）を、解除時に衝撃波として放つ。 |
| 艦隊データリンク | {ER} + {ESP} | 僚機が見た敵を HUD に表示し、自分が狙っている敵を僚機が攻撃する。 |

各シナジーにはサーバー設定（QuantumSuit の synergies）のスイッチがある。シナジーの放出がプレイヤーに当たるのは PvP が有効なときだけ。

## 連鎖コンボ

コンボにリンクは要らず、タイミングだけで発動する。発動するとバナーと効果音が出る。

- **量子トンネル：** 量子転移の後 1 秒以内の位相シフトは、コヒーレンスが半分で済む。
- **凍結斉射：** 自分の時間歪曲の中で一斉射撃する。減速中の敵が最大 8 体まで目標になり、溜め時間なしで同時に着弾し、ダメージは 1.5 倍。
- **静止弾幕：** 力場をキーで解除すると、止めていた投射物がすべて撃った者へ向かって飛び返る（自分の弾として扱う）。

## 演算帯域

量子部位で有効なユニットは、スーツ計算機に負荷（QB）をかける。2 つの部位に付けた同じユニットは 1 回だけ数える。Mk.II は 1 QB 多い。

| QB | ユニット |
|---|---|
| 3 | 生体スキャナー（ESP）、行動予測器、ホロマップ、震探鉱石スキャナー、位相変調器、量子もつれ中継器 |
| 2 | スマート弾道誘導、脅威センサー、位相跳躍ドライブ |
| 1 | 弾道計算機、爆発警告タイマー、電網テレメトリ、量子偏向器、力場共振解体 |
| 0 | その他のユニット |

予算は **20 QB**、演算コアが有効なら 28 QB。0.1.9.2 で組めた構成はすべて 20 QB に収まる。予算を超えるとスーツは **演算飽和** する：センサーの更新が半分になり、コヒーレンスの回復が半分の速さになる。機能が止まることはない。照準付近の QB メーターが赤くなる。

## 量子演算コア

{CC}（チェストかレギンス、6 EU/t）は帯域予算を 8 QB 増やし、ハブのリンクを 1 本増やす。

## Mk.II：誘導と共振解体

- {link('smart_guidance_mk2')}：ロック最大 10、誘導弾の旋回は最大 20 度/tick（Mk.I：6 ロック、14 度）。
- {link('resonance_field_mk2')}：半径 3.75、1 tick に最大 9 ブロック（Mk.I：半径 2.5、6 ブロック）。

震探鉱石スキャナー、電網テレメトリ、生体スキャナー、弾道計算機の Mk.II は各ページを参照。

''' + recipes('レシピ'),
    'zh': front('纠缠与协同') + f'''
# 纠缠与协同

自 0.1.10.1 起，可以把两个量子单元 **纠缠** 在一起。纠缠的一对单元作为 **协同** 运作，产生单个单元都没有的效果。

## 链接

- 量子胸甲是 **链接中枢**，链接保存在胸甲上。不穿量子胸甲时任何协同都不起作用。
- 中枢可容纳 **2 条链接**，{CC} 启用时为 3 条。
- 一对单元都必须安装在穿戴中的量子部件上。两个单元都开启并有供电时协同才起作用。
- 更换单元后链接仍保存在胸甲上，单元装回后重新起作用。指向已移除模组单元的链接会保留，但不起作用。

## 纠缠矩阵

打开奇点管理面板，选中量子胸甲，点击“量子核心”下方的 **链接** 行。

- 左侧：你的量子单元作为节点排列在圆周上。淡虚线：这一对可以链接。实线：已链接但有一个单元关闭。脉动的双螺旋：协同正在起作用。
- 右侧：所有协同及其状态（LINKED、READY、OFFLINE、OVER CAP、MISSING）。点击一行即可链接或解除。
- 也可以把一个单元 **拖动** 到另一个单元上。拖动时会显示这一对的协同，松开即链接或解除。
- 底部显示已用链接数与演算带宽。

HUD 上，正在起作用的协同以小标签显示在准星旁。

## 协同

| 协同 | 单元 | 效果 |
|---|---|---|
| 预测诱导 | {SG} + {BP} | 诱导弹飞向目标将要到达的位置，沿生物行走的路径预测。每 tick 转向 +4 度，锁定 +2。 |
| 威胁透视 | {ESP} + {TS} | 正在追猎你的生物透过墙壁发出红色轮廓，并以圆环倒数其攻击到达的时间。 |
| 地脉全息 | {HM} + {OS} | 扫描到的矿石以 3D 体素显示在全息地图与战术地图中。 |
| 弹道同步 | {BC} + {SG} | 锁定时间减少 30%，并绘制诱导弹的预定轨迹。 |
| 爆心预报 | {BW} + {BP} | 即将发生的爆炸以 3D 球体显示半径，并用箭头指示撤离方向。 |
| 共振偏转 | {DF} + {RF} | 偏转器挡下的伤害有 25% 蓄积为共振；达到 8 时爆发，伤害 4 格内的敌对生物。 |
| 残像诱饵 | {PJ} + {BP} | 跃迁会在出发点留下诱饵。2 秒内原本追猎你的生物会走向诱饵，且无法重新锁定你。 |
| 相位反击 | {PM} + {DF} | 相位偏移期间穿透你的伤害（最多 20）在结束时化为冲击波释放。 |
| 舰队数据链 | {ER} + {ESP} | 僚机看到的敌对生物显示在你的 HUD 上；僚机会攻击你瞄准的生物。 |

每种协同在服务器配置（QuantumSuit 的 synergies）中都有开关。只有开启 PvP 时协同爆发才会命中玩家。

## 连锁连击

连击不需要链接，只看时机。触发时会显示横幅并播放音效。

- **量子隧穿：** 量子跃迁后 1 秒内的相位偏移只消耗一半相干度。
- **冻结齐射：** 在你自己的时间扭曲中进行纠缠齐射。最多 8 个被减速的生物成为目标，弹体无需蓄力同时命中，伤害为 1.5 倍。
- **静止弹幕：** 用按键收起力场时，力场中停住的投射物全部飞回其射手（视为你的射击）。

## 演算带宽

量子部件上每个启用的单元都会占用套装计算机的负载（QB）。同一单元装在两个部件上只计算一次；Mk.II 单元多占 1 QB。

| QB | 单元 |
|---|---|
| 3 | 生物扫描仪（ESP）、行为预测器、战术全息地图、地震矿物扫描仪、相位调制器、量子纠缠中继器 |
| 2 | 智能弹道诱导、威胁传感器、相位跃迁驱动器 |
| 1 | 弹道计算机、爆炸预警计时器、电网遥测传感器、量子偏转器、力场共振解体 |
| 0 | 其他所有单元 |

预算为 **20 QB**，演算核心启用时为 28 QB。0.1.9.2 允许的所有配置都在 20 QB 之内。超出预算时套装 **演算饱和**：传感器刷新减半，相干度回复速度减半。不会关闭任何功能。准星附近的 QB 表会变红。

## 量子演算核心

{CC}（胸甲或护腿，6 EU/t）使带宽预算增加 8 QB，中枢链接增加 1 条。

## Mk.II：诱导与共振解体

- {link('smart_guidance_mk2')}：最多 10 个锁定，诱导弹每 tick 最多转向 20 度（Mk.I：6 个锁定，14 度）。
- {link('resonance_field_mk2')}：半径 3.75，每 tick 最多 9 个方块（Mk.I：半径 2.5，6 个方块）。

地震矿物扫描仪、电网遥测传感器、生物扫描仪与弹道计算机的 Mk.II 见各自页面。

''' + recipes('配方'),
}

MK2 = {
    'ore_scanner': {
        'en': '## Mk.II\n\nThe Mk.II scanner reaches 24 blocks. The suit piece that carries it gets an **Ore filter** row in the Singularity Management Panel: '
              'click it to step through all ores and each common ore tag (c:ores/...). The scan starts again with a pulse, and the echo read-out shows the filter.\n',
        'ja': '## Mk.II\n\nMk.II は半径 24 ブロックを探知する。取り付けた部位には特異点管理パネルに **鉱石フィルター** 行が出る：'
              'クリックすると「全鉱石」と各共通鉱石タグ（c:ores/...）を順に切り替える。切り替えると探知がパルス付きでやり直され、エコー表示にフィルターが出る。\n',
        'zh': '## Mk.II\n\nMk.II 扫描半径为 24 格。安装它的部件会在奇点管理面板中出现 **矿石筛选** 行：'
              '点击即可在“全部矿石”与各个通用矿石标签（c:ores/...）之间切换。切换后扫描会带脉冲重新开始，回波读数中显示筛选条件。\n',
    },
    'grid_telemetry': {
        'en': '## Mk.II\n\nThe Mk.II sensor reads the block in your line of sight up to 36 blocks away, not only the block in reach.\n',
        'ja': '## Mk.II\n\nMk.II は手の届く範囲だけでなく、視線の先 36 ブロックまでのブロックを読み取る。\n',
        'zh': '## Mk.II\n\nMk.II 不仅读取触及范围内的方块，还能读取视线方向 36 格内的方块。\n',
    },
    'entity_esp': {
        'en': '## Mk.II\n\nThe Mk.II scanner marks creatures within 48 blocks, and the target card also shows the exact health and the armor.\n',
        'ja': '## Mk.II\n\nMk.II は 48 ブロック以内の生物を表示し、目標カードに正確な体力と防具値も出す。\n',
        'zh': '## Mk.II\n\nMk.II 标记 48 格内的生物，目标卡片还会显示精确生命值与护甲值。\n',
    },
    'ballistic': {
        'en': '## Mk.II\n\nWith a multishot crossbow the Mk.II computer also draws the paths of the two side shots (10 degrees left and right).\n',
        'ja': '## Mk.II\n\nマルチショットのクロスボウでは、Mk.II が左右 10 度の 2 発の軌道も描く。\n',
        'zh': '## Mk.II\n\n使用多重射击弩时，Mk.II 还会绘制左右 10 度两发的弹道。\n',
    },
}
RECIPE_HEADINGS = {'en': '## Recipe', 'ja': '## レシピ', 'zh': '## 配方'}

COMBO_SECTION = {
    'en': '## Combos and synergies\n\nSince 0.1.10.1 the actions chain into combos (Quantum Tunnel, Frozen Volley, Static Barrage) and pairs of quantum units can be '
          'entangled for synergies. The suit computer also has a compute bandwidth. See Entanglement and synergies.\n',
    'ja': '## コンボとシナジー\n\n0.1.10.1 から、アクションは連鎖コンボ（量子トンネル、凍結斉射、静止弾幕）になり、量子ユニットの組をもつれで結んでシナジーを得られる。'
          'スーツ計算機には演算帯域もある。「もつれとシナジー」を参照。\n',
    'zh': '## 连击与协同\n\n自 0.1.10.1 起，行动可以组成连锁连击（量子隧穿、冻结齐射、静止弹幕），量子单元可以两两纠缠获得协同。'
          '套装计算机还有演算带宽。见“纠缠与协同”。\n',
}
COMBO_ANCHOR = {'en': '## Expansion bay', 'ja': '## 拡張ベイ', 'zh': '## 扩展槽'}

INDEX_LINE = {
    'en': 'Two quantum units can be entangled for a synergy; the suit computer has a compute bandwidth (see Entanglement and synergies).',
    'ja': '量子ユニット 2 つをもつれで結ぶとシナジーが働く。スーツ計算機には演算帯域がある（「もつれとシナジー」を参照）。',
    'zh': '两个量子单元可以纠缠以获得协同；套装计算机有演算带宽（见“纠缠与协同”）。',
}
INDEX_PREV = {
    'en': 'A Quantum Bay Expander adds one more slot to a piece (see Quantum actions).',
    'ja': '量子拡張ベイで部位ごとに 1 枠増やせる（「量子アクション」を参照）。',
    'zh': '量子扩展槽可以为每个部件增加一个单元槽（见“量子行动”）。',
}
INDEX_ROW = {
    'en': (CC, 'Chestplate / Leggings', '6 EU/t while on; bandwidth +8 QB, links +1'),
    'ja': (CC, 'チェストプレート / レギンス', 'オン時 6 EU/t、帯域 +8 QB、リンク +1'),
    'zh': (CC, '胸甲 / 护腿', '开启时 6 EU/t；带宽 +8 QB，链接 +1'),
}


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def add_item_id(text, item):
    head = text.split('---')[1]
    if item in head:
        return text
    return text.replace('item_ids:\n', 'item_ids:\n  - ' + item + '\n', 1)


def main():
    n = 0
    for lang, sub in LANGS.items():
        base = os.path.join(G, sub) if sub else G
        write(os.path.join(base, 'suit/entanglement.md'), PAGE[lang])
        n += 1
        for unit, sections in MK2.items():
            p = os.path.join(base, 'suit/units', unit + '.md')
            if not os.path.exists(p):
                continue
            text = open(p, encoding='utf-8').read()
            if '## Mk.II' in text:
                continue
            heading = RECIPE_HEADINGS[lang]
            assert heading in text, (p, heading)
            text = text.replace(heading, sections[lang] + '\n' + heading, 1)
            mk2 = f'mio_icif:module/item_module_{unit}_mk2'
            text = text.rstrip('\n') + f'\n\n<RecipesFor id="{mk2}" fallbackText="-" />\n'
            text = add_item_id(text, mk2)
            write(p, text)
            n += 1
        p = os.path.join(base, 'suit/quantum_actions.md')
        text = open(p, encoding='utf-8').read()
        if COMBO_SECTION[lang] not in text:
            anchor = COMBO_ANCHOR[lang]
            assert anchor in text, (p, anchor)
            text = text.replace(anchor, COMBO_SECTION[lang] + '\n' + anchor, 1)
            write(p, text)
            n += 1
        p = os.path.join(base, 'suit/index.md')
        text = open(p, encoding='utf-8').read()
        changed = False
        if INDEX_LINE[lang] not in text:
            prev = INDEX_PREV[lang]
            assert prev in text, (p, prev)
            text = text.replace(prev, prev + '\n' + INDEX_LINE[lang], 1)
            changed = True
        if 'item_module_compute_core' not in text:
            lines = text.split('\n')
            at = next(i for i, l in enumerate(lines) if 'item_module_coherence_capacitor"' in l and l.startswith('|'))
            lines.insert(at + 1, '| ' + ' | '.join(INDEX_ROW[lang]) + ' |')
            text = '\n'.join(lines)
            changed = True
        if changed:
            write(p, text)
            n += 1
    print('pages written or updated:', n)


if __name__ == '__main__':
    main()
