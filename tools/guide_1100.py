#!/usr/bin/env python3
"""0.1.10.0 guide pages (English, Japanese, Chinese):
  * new page suit/quantum_actions.md: coherence, the five actions, the expansion bay, the capacitor
  * deflector and behavior predictor unit pages: a Mk.II section and the Mk.II recipe
  * suit index: one line about the expansion bay after the slot counts
Inserts each section once (checked by its content). GuideME parses MDX: no HTML comment, no bare '<' or '{' in text.
Usage: guide_1100.py <repo root>
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
G = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
LANGS = {'en': '', 'ja': '_ja_jp', 'zh': '_zh_cn'}

ITEMS = ['phase_jump_drive', 'phase_jump_drive_mk2', 'phase_modulator', 'phase_modulator_mk2', 'coherence_capacitor',
         'entanglement_relay', 'deflector_mk2', 'behavior_predictor_mk2']
IDS = ['mio_icif:module/item_module_' + i for i in ITEMS] + ['mio_icif:module/item_quantum_bay_expander']


def link(name):
    return f'<ItemLink id="mio_icif:module/item_module_{name}" />'


BAY = '<ItemLink id="mio_icif:module/item_quantum_bay_expander" />'
CAP = link('coherence_capacitor')


def front(title):
    ids = '\n'.join('  - ' + i for i in IDS)
    return f'''---
navigation:
  title: "{title}"
  icon: mio_icif:module/item_module_phase_jump_drive
  parent: suit/index.md
  position: 3
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


PAGE = {
    'en': front('Quantum actions') + f'''
# Quantum actions

Since 0.1.10.0 the quantum suit has active abilities. They use the action keys **V** and **B**, the same keys as the nano bursts. Choose what each key does in the Singularity Management Panel: select a quantum piece and use the two key rows under "Quantum core". By default the keys take the first two actions that you wear.

## Coherence

The actions are paid with **coherence**, shown as the ring around the crosshair (0 to 100). It comes back by itself 1.5 s after the last action, 8 points per second, paid with EU from the suit.
When it reaches 0 the core **decoheres**: for 4 s no action works, a running force field falls, the visor glitches and the sensors refresh at half speed.

## Actions

| Action | Unit | Coherence | Cooldown |
|---|---|---|---|
| Quantum Blink | {link('phase_jump_drive')} | 25 | 2.5 s per charge |
| Phase Shift | {link('phase_modulator')} | 35 | 8 s |
| Force Field | {link('deflector_mk2')} | 15, then 10 per second | 6 s after it falls |
| Time Dilation | {link('behavior_predictor_mk2')} | 60 | 25 s |
| Entangled Volley | {link('entanglement_relay')} | 20 | 10 s |

- **Quantum Blink:** you jump to the point you look at, up to 12 blocks (Mk.II: 20 blocks and two charges). It never goes through a wall: your eyes, waist and feet need a clear line to the landing, and your body must fit there. No fall damage right after the jump.
- **Phase Shift:** for 1 s (Mk.II: 1.5 s) creature attacks, projectiles and blasts pass through you. You cannot attack or mine meanwhile. Lava, fire, drowning and the void still hurt. The Mk.II ends with a shock wave.
- **Force Field:** press once to raise a dome of 4 blocks, press again to drop it. Hostile projectiles stop in the air and fall when the dome falls; fireballs burst on it. Hostile creatures are pushed out.
- **Time Dilation:** for 4 s hostile creatures within 10 blocks move 70 % slower (bosses half as much) and their projectiles fly at a quarter of their speed, while you move 20 % faster. The game has one clock for everybody, so this is a local slow. A server option (single player only, off by default) slows the whole game instead.
- **Entangled Volley:** every Wingman in your dimension fires at your Smart Guidance locks, or at the creature you look at. The bolts are timed so that they all strike at the same moment.

A Mk.I deflector and a Mk.I behavior predictor keep their old effect; the Mk.II items add the action. Actions affect other players only with PvP on (server config section QuantumSuit has one switch per action).

## Expansion bay

Put a {BAY} into the unit slot of the Universal Suit Modification Station together with a quantum piece. The piece gets one more unit slot: helmet 5, chestplate 4, leggings 3, boots 3. The expansion is permanent; pieces without it keep their old slot count.

## Coherence capacitor

The {CAP} (leggings or boots) adds 50 to the coherence maximum and makes it regenerate 25 % faster.

''' + recipes('Recipes'),
    'ja': front('量子アクション') + f'''
# 量子アクション

0.1.10.0 から、量子スーツに能動アクションがある。アクションキー **V** と **B**（ナノバーストと同じキー）で使う。各キーの内容は奇点管理パネルで選ぶ：量子部位を選び、「量子コア」の下の 2 つのキー行を使う。初期設定では、身に着けている最初の 2 つのアクションが割り当てられる。

## コヒーレンス

アクションは **コヒーレンス** で払う。照準まわりのリングに表示される（0〜100）。最後のアクションから 1.5 秒後に毎秒 8 ずつ自動で回復し、スーツの EU を使う。
0 になるとコアが **デコヒーレンス** する：4 秒間どのアクションも使えず、展開中の力場は消え、バイザーが乱れ、センサーの更新が半分になる。

## アクション

| アクション | ユニット | コヒーレンス | クールダウン |
|---|---|---|---|
| 量子転移 | {link('phase_jump_drive')} | 25 | 1 チャージ 2.5 秒 |
| 位相シフト | {link('phase_modulator')} | 35 | 8 秒 |
| 力場展開 | {link('deflector_mk2')} | 15、以後毎秒 10 | 消えてから 6 秒 |
| 時間歪曲 | {link('behavior_predictor_mk2')} | 60 | 25 秒 |
| 一斉射撃 | {link('entanglement_relay')} | 20 | 10 秒 |

- **量子転移：** 視線の先へ最大 12 ブロック瞬間移動する（Mk.II：20 ブロック、2 チャージ）。壁は抜けられない：目・腰・足の高さから着地点まで遮るものがなく、着地点に体が収まる必要がある。転移直後は落下ダメージを受けない。
- **位相シフト：** 1 秒間（Mk.II：1.5 秒）、生物の攻撃・投射物・爆発がすり抜ける。その間は攻撃も採掘もできない。溶岩・火・溺水・奈落のダメージは受ける。Mk.II は解除時に衝撃波を放つ。
- **力場展開：** 押すと半径 4 ブロックのドームが張られ、もう一度押すと消える。敵の投射物は空中で止まり、ドームが消えると落ちる。火の玉はドームで破裂する。敵対生物は外へ押し出される。
- **時間歪曲：** 4 秒間、10 ブロック以内の敵対生物の移動が 70% 遅くなり（ボスは半分）、その投射物は 4 分の 1 の速さになる。自分は 20% 速く動ける。ゲームの時計は全員で 1 つなので、これは範囲内だけの減速である。サーバー設定（シングルプレイ専用、初期値オフ）で、ゲーム全体を遅くすることもできる。
- **一斉射撃：** 同じディメンションの全ウィングマンが、スマート誘導のロック目標（なければ視線の先の生物）へ撃つ。全弾が同時に着弾するよう発射時刻が調整される。

Mk.I の量子偏向器と行動予測器は従来の効果のまま。Mk.II でアクションが加わる。他プレイヤーへの効果は PvP が有効なときだけ（サーバー設定 QuantumSuit にアクションごとのスイッチがある）。

## 拡張ベイ

{BAY} を量子部位と一緒に汎用スーツ改造台のユニットスロットに入れる。部位のユニット枠が 1 つ増える：ヘルメット 5、チェスト 4、レギンス 3、ブーツ 3。取り外しはできない。拡張していない部位は従来の枠数のまま。

## コヒーレンスキャパシタ

{CAP}（レギンスかブーツ）はコヒーレンスの上限を 50 増やし、回復を 25% 速くする。

''' + recipes('レシピ'),
    'zh': front('量子行动') + f'''
# 量子行动

自 0.1.10.0 起，量子套装拥有主动行动。使用行动键 **V** 与 **B**（与纳米爆发相同的按键）。在奇点管理面板中选择各键的行动：选中一件量子部件，使用“量子核心”下方的两行按键设置。默认情况下按键使用你穿戴的前两个行动。

## 相干度

行动消耗 **相干度**，显示为准星周围的圆环（0 至 100）。最后一次行动 1.5 秒后自动回复，每秒 8 点，消耗套装的 EU。
降到 0 时核心 **退相干**：4 秒内无法使用任何行动，正在运行的力场消失，面罩出现故障效果，传感器刷新减半。

## 行动

| 行动 | 单元 | 相干度 | 冷却 |
|---|---|---|---|
| 量子跃迁 | {link('phase_jump_drive')} | 25 | 每次充能 2.5 秒 |
| 相位偏移 | {link('phase_modulator')} | 35 | 8 秒 |
| 力场展开 | {link('deflector_mk2')} | 15，之后每秒 10 | 消失后 6 秒 |
| 时间扭曲 | {link('behavior_predictor_mk2')} | 60 | 25 秒 |
| 纠缠齐射 | {link('entanglement_relay')} | 20 | 10 秒 |

- **量子跃迁：** 瞬移到视线所指的位置，最远 12 格（Mk.II：20 格，两次充能）。不会穿墙：眼睛、腰部与脚部到落点之间必须畅通，并且落点要容得下身体。跃迁后短时间内不受摔落伤害。
- **相位偏移：** 1 秒内（Mk.II：1.5 秒）生物攻击、投射物与爆炸从你身上穿过。期间不能攻击或挖掘。岩浆、火焰、溺水与虚空仍会造成伤害。Mk.II 结束时释放冲击波。
- **力场展开：** 按一次展开半径 4 格的力场罩，再按一次收起。敌方投射物停在空中，力场消失时落下；火球会在力场上爆开。敌对生物被推出去。
- **时间扭曲：** 4 秒内 10 格范围的敌对生物移动减慢 70%（首领减半），其投射物速度降为四分之一，你的移动加快 20%。游戏对所有人只有一个时钟，因此这是局部减速。服务器选项（仅限单人，默认关闭）可以让整个游戏变慢。
- **纠缠齐射：** 本维度内的所有僚机向你的智能制导锁定目标（或你注视的生物）开火。发射时刻经过调整，所有弹体同时命中。

Mk.I 量子偏转器与行为预测器保持原有效果；Mk.II 物品增加行动。只有开启 PvP 时行动才会影响其他玩家（服务器配置 QuantumSuit 中每个行动都有开关）。

## 扩展槽

把 {BAY} 与量子部件一起放入通用套装改装台的单元槽。该部件增加一个单元槽：头盔 5、胸甲 4、护腿 3、靴子 3。扩展是永久的；未扩展的部件保持原来的单元数。

## 相干电容器

{CAP}（护腿或靴子）使相干度上限增加 50，回复加快 25%。

''' + recipes('配方'),
}

MK2 = {
    'deflector': {
        'en': '## Mk.II\n\nThe Mk.II deflector pays 3000 EU per damage point instead of 4000 and adds the **Force Field** action (see Quantum actions).\n',
        'ja': '## Mk.II\n\nMk.II はダメージ 1 点あたり 4000 EU ではなく 3000 EU を払い、アクション **力場展開** が加わる（「量子アクション」を参照）。\n',
        'zh': '## Mk.II\n\nMk.II 每点伤害只需 3000 EU（原为 4000 EU），并增加行动 **力场展开**（见“量子行动”）。\n',
    },
    'behavior_predictor': {
        'en': '## Mk.II\n\nThe Mk.II predictor adds the **Time Dilation** action (see Quantum actions); the paths and attack marks stay the same.\n',
        'ja': '## Mk.II\n\nMk.II ではアクション **時間歪曲** が加わる（「量子アクション」を参照）。進路と攻撃予告の表示は同じ。\n',
        'zh': '## Mk.II\n\nMk.II 增加行动 **时间扭曲**（见“量子行动”）；路径与攻击预告的显示不变。\n',
    },
}
RECIPE_HEADINGS = {'en': '## Recipe', 'ja': '## レシピ', 'zh': '## 配方'}

INDEX_LINE = {
    'en': 'A Quantum Bay Expander adds one more slot to a piece (see Quantum actions).',
    'ja': '量子拡張ベイで部位ごとに 1 枠増やせる（「量子アクション」を参照）。',
    'zh': '量子扩展槽可以为每个部件增加一个单元槽（见“量子行动”）。',
}
INDEX_ANCHOR = {'en': 'boots 2.', 'ja': 'ブーツ2。', 'zh': '靴子2。'}
# new rows of the unit table (after the deflector row)
INDEX_ROWS = {
    'en': [(link('phase_jump_drive'), 'Leggings / Boots', 'Action: Quantum Blink (coherence)'),
           (link('phase_modulator'), 'Chestplate', 'Action: Phase Shift (coherence)'),
           (link('entanglement_relay'), 'Chestplate / Leggings', 'Action: Entangled Volley (coherence)'),
           (link('coherence_capacitor'), 'Leggings / Boots', 'Passive: coherence maximum +50')],
    'ja': [(link('phase_jump_drive'), 'レギンス / ブーツ', 'アクション：量子転移（コヒーレンス）'),
           (link('phase_modulator'), 'チェストプレート', 'アクション：位相シフト（コヒーレンス）'),
           (link('entanglement_relay'), 'チェストプレート / レギンス', 'アクション：一斉射撃（コヒーレンス）'),
           (link('coherence_capacitor'), 'レギンス / ブーツ', '常時：コヒーレンス上限 +50')],
    'zh': [(link('phase_jump_drive'), '护腿 / 靴子', '行动：量子跃迁（相干度）'),
           (link('phase_modulator'), '胸甲', '行动：相位偏移（相干度）'),
           (link('entanglement_relay'), '胸甲 / 护腿', '行动：纠缠齐射（相干度）'),
           (link('coherence_capacitor'), '护腿 / 靴子', '被动：相干度上限 +50')],
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
        write(os.path.join(base, 'suit/quantum_actions.md'), PAGE[lang])
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
        p = os.path.join(base, 'suit/index.md')
        text = open(p, encoding='utf-8').read()
        if INDEX_LINE[lang] not in text:
            anchor = INDEX_ANCHOR[lang]
            assert anchor in text, (p, anchor)
            text = text.replace(anchor, anchor + '\n' + INDEX_LINE[lang], 1)
            n += 1
        if 'item_module_phase_jump_drive' not in text:
            lines = text.split('\n')
            at = next(i for i, l in enumerate(lines) if 'item_module_deflector"' in l and l.startswith('|'))
            lines[at + 1:at + 1] = ['| ' + ' | '.join(r) + ' |' for r in INDEX_ROWS[lang]]
            text = '\n'.join(lines)
        write(p, text)
    print('pages written or updated:', n)


if __name__ == '__main__':
    main()
