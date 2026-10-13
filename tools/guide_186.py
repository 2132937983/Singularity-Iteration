#!/usr/bin/env python3
"""0.1.8.6 guide: the "Nano suit units" page with all 17 units, Mk.II, HF sync levels, the
mesh/camouflage rule and the nano to quantum rebuild (English, Japanese, Chinese).
Usage: guide_186.py <repo root>
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
G = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
SID = 'mio_icif:producer/block_quantum_modification_station'

# id, slots key, LP, power, burst, class (n = nano, s = shared)
UNITS = [
    ('myofiber', 'L', 2, '3 EU/t', 'Dash', 'n'), ('thermoregulator', 'C', 2, '2 EU/t', '-', 'n'),
    ('emp_emitter', 'C', 3, '-', 'EMP', 'n'), ('optical_camo', 'C', 3, '-', 'Camo', 'n'),
    ('kinetic_mesh', 'C', 2, '-', 'Mesh', 'n'), ('recon_cloud', 'H', 2, '-', 'Recon', 'n'),
    ('jammer', 'H', 2, '4 EU/t', '-', 'n'), ('thermal_camo', 'C', 2, '2 EU/t', '-', 'n'),
    ('gecko_setae', 'F', 2, '1 EU/t', '-', 'n'), ('shock_gel', 'F', 1, '-', '-', 'n'),
    ('heat_sink_fins', 'CL', 1, '-', '-', 'n'), ('micro_capacitor', 'A', 1, '-', '-', 'n'),
    ('solar_skin', 'HC', 1, '+2 EU/t', '-', 'n'), ('toxin_filter', 'H', 1, '1 EU/t', '-', 's'),
    ('self_repair', 'A', 1, '2000 EU / heal', '-', 's'), ('hydrophobic', 'CL', 1, '-', '-', 's'),
    ('kinetic_generator', 'L', 1, '+1 EU / block', '-', 's'),
]
SLOTS = {
    'en': {'H': 'Helmet', 'C': 'Chestplate', 'L': 'Leggings', 'F': 'Boots', 'A': 'Any', 'CL': 'Chest / Legs', 'HC': 'Helmet / Chest'},
    'ja': {'H': 'ヘルメット', 'C': 'チェストプレート', 'L': 'レギンス', 'F': 'ブーツ', 'A': '全部位', 'CL': '胴 / 脚', 'HC': '頭 / 胴'},
    'zh': {'H': '头盔', 'C': '胸甲', 'L': '护腿', 'F': '靴子', 'A': '任意', 'CL': '胸 / 腿', 'HC': '头 / 胸'},
}
CLASS = {'en': {'n': 'Nano', 's': 'Shared'}, 'ja': {'n': 'ナノ', 's': '共用'}, 'zh': {'n': '纳米', 's': '共用'}}
HEAD = {
    'en': '| Unit | Fits | LP | Power | Burst | Class |',
    'ja': '| ユニット | 部位 | LP | 消費 | バースト | 種別 |',
    'zh': '| 单元 | 部位 | LP | 耗电 | 爆发 | 类别 |',
}

TEXT = {
    'en': ('Nano suit units', '''The nano suit takes upgrade units. Its units are light: a fully loaded nano suit draws about 10 EU/t.
Its strong effects are bursts. A burst uses the nano reserve and adds heat.

## Nano core

- **Reserve** (0 to 100): bursts use it. It refills by 2 points per second while you wear a nano piece with units. Each point costs 400 EU from the suit.
- **Heat** (0 to 100, more with heat-sink fins): each burst adds heat. The suit sheds 4 heat per second.
- **Overheat**: at the cap the suit overheats. Bursts lock until the heat falls below 40 % of the cap. Without fins you also move 20 % slower.

## Burst keys

Press **V** (primary) or **B** (secondary). The Singularity Management Panel assigns a burst unit to each key. "auto" picks: Kinetic Dash, Optical Camouflage, Kinetic Armor Mesh, EMP, Recon Cloud.
The Kinetic Armor Mesh and the Optical Camouflage cannot be on the two keys at the same time, and they never run together.

## Units

{table}

## Mk.II

Every unit made for the nano lattice has a Mk.II item: the Mk.I unit, a Living Circuit, four neutron plates and three family materials.
Mk.II is stronger (the item tooltip shows how) and draws 20 % less power. It takes the same lattice points.
To upgrade, put the piece in the station and one Mk.II item in the unit slot. The station replaces the Mk.I unit and gives it back.

## HF sync

An active nanosaber in the main hand syncs with the Nano Myofiber and the Kinetic Armor Mesh weave. Each Mk.I source gives one level, each Mk.II source two, at most three.
Level 1: attack speed +12 %, armour penetration 15 %. Level 2: +15 %, 22 %. Level 3: +18 %, 30 %. The blade trembles harder at each level.

## Nano to quantum

When you craft a quantum piece from a nano piece, shared units move onto the quantum piece. Nano-only units and the lattice densifiers come back as items.

## Server settings

The common config section NanoSuit has a switch for each unit, PvP switches (all on by default) and the numbers.

<ItemLink id="{sid}" />'''),
    'ja': ('ナノスーツのユニット', '''ナノスーツは強化ユニットを受け付ける。ナノのユニットは軽く、満載でも約 10 EU/t しか使わない。
強い効果はバーストで、ナノ予備量を使い、熱を加える。

## ナノコア

- **予備量**（0〜100）：バーストが使う。ユニット付きのナノ部位の着用中、毎秒 2 回復する。1 ポイントにスーツの電力 400 EU を使う。
- **熱**（0〜100、放熱フィンで増える）：バーストのたびにたまる。毎秒 4 放熱する。
- **過熱**：上限に達すると過熱する。熱が上限の 40% を下回るまでバーストできない。フィンがないと移動も 20% 遅くなる。

## バーストキー

**V**（メイン）と **B**（サブ）。奇点管理パネルで各キーにユニットを割り当てる。「自動」の順番：キネティックダッシュ、光学迷彩、運動装甲メッシュ、EMP、ナノ偵察雲。
運動装甲メッシュと光学迷彩は 2 つのキーに同時に割り当てられず、同時に動くこともない。

## ユニット

{table}

## Mk.II

ナノ格子用のユニットにはすべて Mk.II がある：Mk.I ユニット、生体回路、中性子板 4 枚、系統の素材 3 つで作る。
Mk.II は効果が強く（ツールチップに内容が出る）、消費電力が 20% 少ない。LP は同じ。
強化するには、改造台に部位を入れ、ユニットスロットに Mk.II を 1 個入れる。改造台が Mk.I と入れ替え、Mk.I を返す。

## HF 同期

メインハンドの起動したナノセイバーは、ナノ筋繊維と運動装甲メッシュの織り目に同期する。Mk.I の源は 1 段、Mk.II は 2 段、最大 3 段。
1 段：攻撃速度 +12%、防具貫通 15%。2 段：+15%、22%。3 段：+18%、30%。段が上がるほど刃が強く震える。

## ナノから量子へ

ナノ部位から量子部位をクラフトすると、共用ユニットは量子部位へ移る。ナノ専用ユニットと格子高密度化材はアイテムとして戻る。

## サーバー設定

コモンコンフィグの NanoSuit 節に、ユニットごとのスイッチ、PvP スイッチ（初期値はすべてオン）、数値がある。

<ItemLink id="{sid}" />'''),
    'zh': ('纳米套装单元', '''纳米套装可以安装升级单元。纳米单元很轻：满载的纳米套装约耗 10 EU/t。
强力效果都是爆发：爆发消耗纳米储备并增加热量。

## 纳米核心

- **储备**（0 至 100）：爆发会消耗它。穿戴装有单元的纳米部件时每秒回复 2 点，每点从套装扣除 400 EU。
- **热量**（0 至 100，散热鳍片可提高上限）：每次爆发都会增加热量。每秒散热 4 点。
- **过热**：达到上限时过热。热量降到上限的 40% 以下之前不能爆发；没有鳍片时移动速度也降低 20%。

## 爆发键

按 **V**（主）或 **B**（副）。在奇点管理面板中为每个键分配单元。“自动”顺序：动能冲刺、光学迷彩、动能装甲网、EMP、纳米侦察云。
动能装甲网与光学迷彩不能同时分配到两个键上，也不会同时运行。

## 单元

{table}

## Mk.II

每个为纳米晶格设计的单元都有 Mk.II：用 Mk.I 单元、生体电路、4 块中子板与 3 个同系材料合成。
Mk.II 效果更强（提示中有说明），耗电减少 20%，占用的 LP 相同。
升级时把部件放入改装台，在单元槽中放入 1 个 Mk.II。改装台替换 Mk.I 并将其返还。

## 高频同步

主手中开启的纳米剑会与纳米肌纤维和动能装甲网的织物同步。每个 Mk.I 来源 1 级，Mk.II 来源 2 级，最多 3 级。
1 级：攻击速度 +12%，护甲穿透 15%。2 级：+15%，22%。3 级：+18%，30%。等级越高刀刃颤动越强。

## 纳米改造为量子

用纳米部件合成量子部件时，共用单元会转移到量子部件上；纳米专用单元与晶格致密剂作为物品返还。

## 服务器设置

通用配置的 NanoSuit 节包含每个单元的开关、PvP 开关（默认全部开启）以及数值。

<ItemLink id="{sid}" />'''),
}
LANG_DIR = {'en': '', 'ja': '_ja_jp', 'zh': '_zh_cn'}


def main():
    for lang, sub in LANG_DIR.items():
        base = os.path.join(G, sub) if sub else G
        title, body = TEXT[lang]
        rows = [HEAD[lang], '|---|---|---|---|---|---|']
        for uid, slots, lp, power, burst, cls in UNITS:
            rows.append(f'| <ItemLink id="mio_icif:module/item_module_{uid}" /> | {SLOTS[lang][slots]} | {lp} | {power} | {burst} | {CLASS[lang][cls]} |')
        rows.append(f'| <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | {SLOTS[lang]["C"]} | 1 | 6 EU/t | - | {CLASS[lang]["s"]} |')
        rows.append(f'| <ItemLink id="mio_icif:module/item_module_blast_warning" /> | {SLOTS[lang]["HC"]} | 1 | 2 EU/t | - | {CLASS[lang]["s"]} |')
        ids = '\n'.join('  - mio_icif:module/item_module_' + u[0] for u in UNITS)
        page = (f'---\nnavigation:\n  title: "{title}"\n  icon: mio_icif:armor/item_armor_nano_chestplate\n  parent: suit/index.md\n  position: 1\n'
                f'item_ids:\n{ids}\n  - mio_icif:module/item_nano_lattice_densifier\n---\n\n# {title}\n\n')
        page += body.format(table='\n'.join(rows), sid=SID) + '\n'
        with open(os.path.join(base, 'suit/nano_units.md'), 'w', encoding='utf-8', newline='\n') as f:
            f.write(page)
    print('wrote nano unit pages')


if __name__ == '__main__':
    main()
