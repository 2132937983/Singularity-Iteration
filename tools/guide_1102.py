#!/usr/bin/env python3
"""0.1.10.2 guide pages (English, Japanese, Chinese):
  * new page suit/wingman_command.md: the quantum command wheel, the orders, the data link, the
    quantum recall, the squad HUD and the 3D holo table
  * holomap unit page: a Mk.II section and the Mk.II recipe
  * Armory page: a pointer from "Wingman tactics" to the quantum command
  * Special Maneuver Mode page: the thruster effects
  * suit index: one line about the quantum command
Inserts each section once (checked by its content). GuideME parses MDX: no HTML comment, no bare '<' or '{' in text.
Usage: guide_1102.py <repo root>
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
G = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
LANGS = {'en': '', 'ja': '_ja_jp', 'zh': '_zh_cn'}


def link(name):
    return f'<ItemLink id="mio_icif:module/item_module_{name}" />'


ER, HM2 = link('entanglement_relay'), link('holomap_mk2')


def front(title):
    return f'''---
navigation:
  title: "{title}"
  icon: mio_icif:module/item_module_entanglement_relay
  parent: suit/index.md
  position: 5
---
'''


PAGE = {
    'en': front('Quantum command') + f'''
# Quantum command

Since 0.1.10.2 a quantum suit with an active {ER} commands its Wingmen directly: orders by point, focus fire, a data link and an emergency recall. Orders reach your Wingmen at any distance in your dimension.

## Command wheel

Hold **G** to open the wheel, move the mouse to a sector and let go. A click works too; the centre cancels.
G also toggles the mode of a toggleable electric tool: with such a tool in a hand, G stays the tool toggle. Both keys can be bound separately.

When the wheel opens, it takes the point and the creature in your crosshair (up to 256 blocks; a look at the sky gives a point in the air 48 blocks ahead). The chips above the wheel choose the whole squad or one Wingman; the mouse wheel steps through them.

| Sector | Order |
|---|---|
| MOVE | Fly to the point, then guard it: engage hostile creatures within the hold radius (10 blocks). No fighting on the way. |
| HOLD | Hold where each Wingman is now and engage what comes near. |
| SUPPRESS | Patrol a ring above the area and clear every hostile creature in it (14 blocks). |
| FOCUS | Attack the creature in your crosshair until it is down (up to 128 blocks from the Wingman), then form up. |
| VOLLEY | The Entangled Volley (as on the action key). |
| FORM UP | End the orders: back to formation and the tactical mode. |
| MODE | A fan: the four tactical modes, "follow squad" and "return home". A mode for one Wingman is its own; the squad mode no longer changes it until "follow squad". |
| DATA LINK | On or off (see below). |
| HOLO TABLE | Project the 3D holo table (holomap Mk.II). |
| Q-RECALL | Quantum recall (see below). |

A Wingman on an order no longer follows you: it guards its point instead of staying within 28 blocks of you. A Wingman in an unloaded chunk cannot move; the squad list shows it as UNLOADED, and only the quantum recall reaches it.

## Data link

With the data link on, the hostile creatures your Wingmen see (24 blocks around each) are marked on the holomap, the tactical map and the holo table. With the Fleet Datalink synergy you also get the world markers and your crosshair target becomes the Wingmen's first target.

## Quantum recall

Every Wingman you own folds back into its Armory at once: in any dimension, loaded or not (an unloaded Wingman's chunk loads for a moment). It costs 30 coherence and has a 30 s cooldown.

## Squad HUD

While the command link works, a squad panel on the left lists each Wingman with its mode (* = an individual mode), its order and distance, its health and its distance from you. Wingmen out of view get a chevron on the screen edge. In the world, hold points, suppression rings and focus targets are marked for you alone.

## 3D holo table

With a {HM2}, hold the tactical map key (**N**) or choose HOLO TABLE on the wheel. A hologram of the terrain around you rises in front of you: the columns grow out from the centre, interference bands and a scan line run over it. It shows you, your Wingmen with their numbers and orders, hostile creatures (the ones in the data link with a ring), other players and, with Ore Hologram, the scanned ores.

- Drag: turn the table. Mouse wheel: 24, 48 or 96 blocks.
- Left click on the terrain: MOVE there. Right click: SUPPRESS there. Left click on a creature: FOCUS.
- H: HOLD. F: FORM UP. 0 to 9: choose all or one Wingman.
''',
    'ja': front('量子指揮') + f'''
# 量子指揮

0.1.10.2 から、{ER} が有効な量子スーツは僚機を直接指揮できる：地点への命令、集中攻撃、データリンク、緊急回収。同じディメンションなら距離に関係なく命令が届く。

## コマンドホイール

**G** を押し続けるとホイールが開く。マウスをセクターへ動かして離すと命令する。クリックでもよい。中央はキャンセル。
G は切り替え式電動ツールのモード切り替えにも使われている。そのツールを手に持っているときは、G はツールの切り替えのまま。2 つのキーは別々に割り当て直せる。

ホイールを開いた瞬間に、照準の先の地点と生物を記録する（最大 256 ブロック。空を見ていると 48 ブロック先の空中の点）。ホイール上のチップで全機か 1 機を選ぶ。マウスホイールでも切り替えられる。

| セクター | 命令 |
|---|---|
| 移動 | 地点へ飛び、そこを守る：待機半径（10 ブロック）内の敵と戦う。移動中は戦わない。 |
| 待機 | 各機がいまの場所で待機し、近づく敵と戦う。 |
| 区域制圧 | 区域の上空を周回し、範囲（14 ブロック）内の敵をすべて排除する。 |
| 集中攻撃 | 照準の先の生物を倒すまで攻撃する（僚機から 128 ブロックまで）。倒したら編隊に戻る。 |
| 一斉射撃 | 一斉射撃（アクションキーと同じ）。 |
| 編隊復帰 | 命令を終了し、編隊と戦術モードに戻る。 |
| モード | 扇形メニュー：4 つの戦術モード、「全体に従う」、「武装庫へ帰還」。1 機に設定したモードはその機体のもので、「全体に従う」まで全体モードの変更を受けない。 |
| データリンク | オン／オフ（下記）。 |
| ホロテーブル | 3D ホロテーブルを投影する（ホロマップ Mk.II）。 |
| 量子回収 | 量子回収（下記）。 |

命令を受けた僚機は着用者についてこない。28 ブロックの範囲に留まる代わりに命令地点を守る。未ロードのチャンクにいる僚機は動けない。編隊リストに UNLOADED と表示され、量子回収だけが届く。

## データリンク

データリンクがオンなら、僚機が見た敵（各機の周囲 24 ブロック）がホロマップ、戦術マップ、ホロテーブルに表示される。艦隊データリンクのシナジーがあれば、ワールド内のマーカーも出て、照準の先の敵が僚機の最優先目標になる。

## 量子回収

自分の僚機すべてを、その場で武装庫へ戻す。どのディメンションでも、ロードされていなくても届く（未ロードの僚機のチャンクは一瞬だけロードされる）。コヒーレンス 30 を使い、クールダウンは 30 秒。

## 編隊 HUD

指揮リンクが働いている間、左側の編隊パネルに各僚機のモード（* は個別モード）、命令と距離、体力、自分からの距離が出る。画面外の僚機は画面端の矢印で示す。ワールドには待機点、制圧リング、集中攻撃の目標が自分にだけ表示される。

## 3D ホロテーブル

{HM2} があれば、戦術マップキー（**N**）を長押しするか、ホイールでホロテーブルを選ぶ。目の前に周囲の地形のホログラムが立ち上がる：柱が中心から外へ伸び、干渉縞と走査線が流れる。自分、番号と命令付きの僚機、敵（データリンクの対象は輪付き）、他のプレイヤー、地脈ホロがあれば探知した鉱石を表示する。

- ドラッグ：テーブルを回す。マウスホイール：24、48、96 ブロック。
- 地形を左クリック：そこへ移動。右クリック：そこを区域制圧。生物を左クリック：集中攻撃。
- H：待機。F：編隊復帰。0〜9：全機か 1 機を選ぶ。
''',
    'zh': front('量子指挥') + f'''
# 量子指挥

自 0.1.10.2 起，装有已启用 {ER} 的量子套装可以直接指挥僚机：按地点下达命令、集中攻击、数据链与紧急回收。同一维度内命令不受距离限制。

## 指挥轮盘

按住 **G** 打开轮盘，把鼠标移到扇区后松开即下达命令。也可以点击；中心为取消。
G 也是可切换电动工具的模式切换键：手持此类工具时，G 仍为工具切换。两个按键可分别重新绑定。

轮盘打开时会记录准星所指的地点与生物（最远 256 格；看向天空时为前方 48 格的空中点）。轮盘上方的标签选择全部僚机或其中一架；滚轮也可切换。

| 扇区 | 命令 |
|---|---|
| 移动 | 飞往该地点并守住：攻击待机半径（10 格）内的敌对生物。途中不交战。 |
| 待机 | 每架僚机在当前位置待命，攻击靠近的敌人。 |
| 区域压制 | 在区域上空巡航，清除范围（14 格）内的所有敌对生物。 |
| 集中攻击 | 攻击准星所指的生物直到其倒下（距僚机最远 128 格），然后归队。 |
| 齐射 | 纠缠齐射（与行动键相同）。 |
| 归队 | 结束命令：回到编队与战术模式。 |
| 模式 | 扇形菜单：四种战术模式、“跟随全队”与“返回军械库”。为单架僚机设定的模式属于该机，在“跟随全队”之前不受全队模式更改影响。 |
| 数据链 | 开启或关闭（见下文）。 |
| 全息台 | 投射 3D 全息台（全息地图 Mk.II）。 |
| 量子回收 | 量子回收（见下文）。 |

执行命令的僚机不再跟随你：它守卫命令地点，而不是停留在你周围 28 格内。位于未加载区块的僚机无法移动；编队列表中显示为 UNLOADED，只有量子回收能到达它。

## 数据链

开启数据链后，僚机看到的敌对生物（每架僚机周围 24 格）会标记在全息地图、战术地图与全息台上。若有舰队数据链协同，还会显示世界标记，且你准星所指的敌人成为僚机的首要目标。

## 量子回收

让你的所有僚机立即折返其军械库：任何维度、无论是否加载都能到达（未加载僚机所在的区块会短暂加载）。消耗 30 相干度，冷却 30 秒。

## 编队 HUD

指挥链路有效时，左侧的编队面板列出每架僚机的模式（* 为单独模式）、命令与距离、生命值以及与你的距离。视野外的僚机会在屏幕边缘显示箭头。世界中的待机点、压制圆环与集中攻击目标仅对你显示。

## 3D 全息台

装有 {HM2} 时，按住战术地图键（**N**）或在轮盘中选择全息台。你面前会升起周围地形的全息投影：地形柱从中心向外升起，干涉条纹与扫描线掠过其上。显示你自己、带编号与命令的僚机、敌对生物（数据链中的目标带圆环）、其他玩家，以及有地脉全息时扫描到的矿石。

- 拖动：旋转全息台。滚轮：24、48 或 96 格。
- 左键点击地形：移动到该处。右键：压制该区域。左键点击生物：集中攻击。
- H：待机。F：归队。0 到 9：选择全部或一架僚机。
''',
}

MK2 = {
    'en': '## Mk.II\n\nThe Mk.II holomap also projects the **3D holo table**: hold the tactical map key or choose HOLO TABLE on the command wheel (see Quantum command).\n',
    'ja': '## Mk.II\n\nMk.II は **3D ホロテーブル** も投影する：戦術マップキーを長押しするか、コマンドホイールでホロテーブルを選ぶ（「量子指揮」を参照）。\n',
    'zh': '## Mk.II\n\nMk.II 还能投射 **3D 全息台**：按住战术地图键，或在指挥轮盘中选择全息台（见“量子指挥”）。\n',
}
RECIPE_HEADINGS = {'en': '## Recipe', 'ja': '## レシピ', 'zh': '## 配方'}

ARMORY = {
    'en': 'With an Entanglement Relay on a quantum suit you can also give your Wingmen orders (move, hold, suppress, focus) from the command wheel (see Quantum command).\n',
    'ja': '量子スーツに量子もつれ中継器があれば、コマンドホイールから僚機に命令（移動、待機、区域制圧、集中攻撃）もできる（「量子指揮」を参照）。\n',
    'zh': '量子套装装有量子纠缠中继器时，还可以通过指挥轮盘向僚机下达命令（移动、待机、区域压制、集中攻击）（见“量子指挥”）。\n',
}

MANEUVER = {
    'en': '\n## Thruster effects\n\nSince 0.1.10.2 heat haze shimmers over the thruster flames, shock diamonds stand in the plumes at full burn, and a dust ring runs over the ground when you land. The Wingmen\'s jets shimmer too. Client option: thruster_fx.\n',
    'ja': '\n## スラスター演出\n\n0.1.10.2 から、スラスター炎の上に陽炎がゆらぎ、全開時には噴流にショックダイヤモンドが並び、着地すると地面に塵のリングが広がる。僚機の噴射もゆらぐ。クライアント設定：thruster_fx。\n',
    'zh': '\n## 推进器特效\n\n自 0.1.10.2 起，推进器火焰上方出现热浪扭曲，全推力时尾焰中出现激波钻石，着陆时地面扩散尘环。僚机的喷流同样有热浪。客户端选项：thruster_fx。\n',
}
MANEUVER_MARK = {'en': '## Thruster effects', 'ja': '## スラスター演出', 'zh': '## 推进器特效'}

INDEX_LINE = {
    'en': 'With an Entanglement Relay the suit commands its Wingmen: command wheel, orders, holo table (see Quantum command).',
    'ja': '量子もつれ中継器があれば、スーツで僚機を指揮できる：コマンドホイール、命令、ホロテーブル（「量子指揮」を参照）。',
    'zh': '装有量子纠缠中继器时，套装可以指挥僚机：指挥轮盘、命令、全息台（见“量子指挥”）。',
}
INDEX_PREV = {
    'en': 'Two quantum units can be entangled for a synergy; the suit computer has a compute bandwidth (see Entanglement and synergies).',
    'ja': '量子ユニット 2 つをもつれで結ぶとシナジーが働く。スーツ計算機には演算帯域がある（「もつれとシナジー」を参照）。',
    'zh': '两个量子单元可以纠缠以获得协同；套装计算机有演算带宽（见“纠缠与协同”）。',
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
        write(os.path.join(base, 'suit/wingman_command.md'), PAGE[lang])
        n += 1
        p = os.path.join(base, 'suit/units/holomap.md')
        text = open(p, encoding='utf-8').read()
        if '## Mk.II' not in text:
            heading = RECIPE_HEADINGS[lang]
            assert heading in text, (p, heading)
            text = text.replace(heading, MK2[lang] + '\n' + heading, 1)
            mk2 = 'mio_icif:module/item_module_holomap_mk2'
            text = text.rstrip('\n') + f'\n\n<RecipesFor id="{mk2}" fallbackText="-" />\n'
            text = add_item_id(text, mk2)
            write(p, text)
            n += 1
        p = os.path.join(base, 'machines/producer_block_armory.md')
        text = open(p, encoding='utf-8').read()
        if ARMORY[lang] not in text:
            heading = RECIPE_HEADINGS[lang]
            assert heading in text, (p, heading)
            text = text.replace(heading, ARMORY[lang] + '\n' + heading, 1)
            write(p, text)
            n += 1
        p = os.path.join(base, 'suit/maneuver_mode.md')
        text = open(p, encoding='utf-8').read()
        if MANEUVER_MARK[lang] not in text:
            write(p, text.rstrip('\n') + '\n' + MANEUVER[lang])
            n += 1
        p = os.path.join(base, 'suit/index.md')
        text = open(p, encoding='utf-8').read()
        if INDEX_LINE[lang] not in text:
            prev = INDEX_PREV[lang]
            assert prev in text, (p, prev)
            write(p, text.replace(prev, prev + '\n' + INDEX_LINE[lang], 1))
            n += 1
    print('pages written or updated:', n)


if __name__ == '__main__':
    main()
