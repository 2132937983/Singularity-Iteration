#!/usr/bin/env python3
"""0.1.8.5 guide pages: the Universal Suit Modification Station (rewritten) and a new page on the
nano suit units, in English, Japanese and Chinese.
Usage: guide_185.py <repo root>
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
G = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
SID = 'mio_icif:producer/block_quantum_modification_station'

POWER = {
    'en': ('| Item | Value |', '| Input voltage tier | HV (512 EU) |', '| Maximum input | 512 EU/t |', '| Energy storage | 40,000 EU |',
           '| Use while working | 64 EU/t |', '| Operation time | 100 tick (5 s) |', '| Energy per operation | 6,400 EU |'),
    'ja': ('| 項目 | 値 |', '| 入力電圧Tier | HV (512 EU) |', '| 最大入力 | 512 EU/t |', '| 蓄電容量 | 40,000 EU |',
           '| 稼働時消費 | 64 EU/t |', '| 1回の処理時間 | 100 tick (5 s) |', '| 1回あたりの電力 | 6,400 EU |'),
    'zh': ('| 项目 | 数值 |', '| 输入电压等级 | HV (512 EU) |', '| 最大输入 | 512 EU/t |', '| 储能 | 40,000 EU |',
           '| 工作耗电 | 64 EU/t |', '| 单次时间 | 100 tick (5 s) |', '| 单次耗电 | 6,400 EU |'),
}

STATION = {
    'en': ('Universal Suit Modification Station', '''## Function

The station installs upgrade units into quantum and nano suit pieces. Each installation uses EU.
A quantum piece counts units: helmet 4, chestplate 3, leggings 2, boots 2.
A nano piece has a nano lattice and counts lattice points (LP): helmet 4, chestplate 6, leggings 4, boots 4.
Each nano unit takes 1 to 3 LP. A Nano Lattice Densifier adds 2 LP to a nano piece, at most twice per piece.
Some units fit only one suit class. The tooltip of each unit shows its class and its LP.

## Power data

{power}

## Procedure

1. Connect an HV cable to the station.
2. Put a quantum or nano suit piece in the top slot.
3. Put a unit or a lattice densifier in the lower slot.
4. Wait until the station finishes the installation.
5. Click the x next to an installed unit to remove the unit.
6. Switch the unit on in the Singularity Management Panel.

## Notes

- For a nano piece, the screen shows the lattice bar. Each cell is one LP in the colour of the unit that uses it. The unit in the lower slot blinks where it would go.
- Sensor units show data only through a suit visor.
- The removal of a unit costs no EU and gives the unit back. A densifier stays in the piece.

## Recipe

<RecipesFor id="{sid}" fallbackText="-" />'''),
    'ja': ('汎用スーツ改造台', '''## 機能

量子スーツとナノスーツの部位に強化ユニットを取り付ける。取り付けのたびにEUを使う。
量子部位は個数で数える。ヘルメット4、チェストプレート3、レギンス2、ブーツ2。
ナノ部位はナノ格子を持ち、格子ポイント（LP）で数える。ヘルメット4、チェストプレート6、レギンス4、ブーツ4。
ナノユニットは1〜3 LPを使う。ナノ格子高密度化材は部位に2 LPを足す。1部位に2回まで。
一部のユニットは片方のスーツ専用。ユニットのツールチップに種別とLPが出る。

## 電力データ

{power}

## 使用手順

1. HVケーブルを改造台に接続する。
2. 上のスロットに量子またはナノスーツの部位を入れる。
3. 下のスロットにユニットか格子高密度化材を入れる。
4. 改造台が取り付けを終えるまで待つ。
5. 取り付け済みユニットの横の×を押し、ユニットを外す。
6. 奇点管理パネルでユニットをオンにする。

## 注意

- ナノ部位では格子バーを表示する。1マスが1 LPで、使っているユニットの色になる。下のスロットのユニットが入る位置は点滅する。
- センサーユニットはスーツのバイザー越しにだけデータを表示する。
- ユニットを外す時、EUは使わず、ユニットは戻る。高密度化材は部位に残る。

## レシピ

<RecipesFor id="{sid}" fallbackText="-" />'''),
    'zh': ('通用套装改装台', '''## 功能

改装台为量子套装与纳米套装的部件安装升级单元，每次安装消耗 EU。
量子部件按个数计：头盔 4、胸甲 3、护腿 2、靴子 2。
纳米部件有纳米晶格，按晶格点数（LP）计：头盔 4、胸甲 6、护腿 4、靴子 4。
每个纳米单元占用 1 至 3 LP。纳米晶格致密剂为部件增加 2 LP，每件最多 2 次。
部分单元只适用于一种套装。单元的提示中会显示类别与 LP。

## 电力数据

{power}

## 使用步骤

1. 用 HV 线缆连接改装台。
2. 在上方槽位放入量子或纳米套装部件。
3. 在下方槽位放入单元或晶格致密剂。
4. 等待改装台完成安装。
5. 点击已安装单元旁的 × 将其拆下。
6. 在奇点管理面板中开启单元。

## 注意

- 纳米部件会显示晶格条。每格为 1 LP，颜色为占用它的单元的颜色。下方槽位中的单元会在将要占用的位置闪烁。
- 传感器单元只能通过套装面罩显示数据。
- 拆下单元不耗 EU，单元会返还。致密剂留在部件中。

## 配方

<RecipesFor id="{sid}" fallbackText="-" />'''),
}

UNITS = [
    ('myofiber', 'LEGS', 2, '3 EU/t', 'V: Kinetic Dash'),
    ('thermoregulator', 'CHEST', 2, '2 EU/t', '-'),
    ('emp_emitter', 'CHEST', 3, '-', 'EMP'),
    ('optical_camo', 'CHEST', 3, '-', 'Camo'),
    ('toxin_filter', 'HEAD', 1, '1 EU/t', '-'),
    ('micro_capacitor', 'ANY', 1, '-', '-'),
]
SLOT = {
    'en': {'LEGS': 'Leggings', 'CHEST': 'Chestplate', 'HEAD': 'Helmet', 'ANY': 'Any piece'},
    'ja': {'LEGS': 'レギンス', 'CHEST': 'チェストプレート', 'HEAD': 'ヘルメット', 'ANY': '全部位'},
    'zh': {'LEGS': '护腿', 'CHEST': '胸甲', 'HEAD': '头盔', 'ANY': '任意部件'},
}

NANO = {
    'en': ('Nano suit units', '''The nano suit takes upgrade units since 0.1.8.5. Its units are light: a fully loaded nano suit draws about 10 EU/t.
Its strong effects are bursts. A burst uses the nano reserve and adds heat.

## Nano core

- **Reserve** (0 to 100): bursts use it. It refills by 2 points per second while you wear a nano piece. Each point costs 400 EU from the suit.
- **Heat** (0 to 100): each burst adds heat. The suit sheds 4 heat per second (8 with the thermoregulating membrane).
- **Overheat**: at 100 heat the suit overheats. Bursts lock until the heat falls below 40. The optical camouflage drops.

## Burst keys

Press **V** (primary) or **B** (secondary) to fire a burst. Change the keys in the controls menu.
The Singularity Management Panel assigns a burst unit to each key. Select a nano piece and click the burst rows. "auto" picks the worn burst units in this order: Kinetic Dash, Optical Camouflage, EMP.

## Units

{table}

## HF sync

Nano myofiber plus an active nanosaber in the main hand gives HF sync: attack speed +12 % and 15 % armour penetration. The blade trembles and sparks.

## HUD

The nano HUD needs no visor. Around the crosshair: the reserve arc (left, with the cost marks of V and B), the heat arc (right, with cooling chevrons) and the two burst chips with their cooldowns.

## Server settings

The common config section NanoSuit has a switch for each unit, PvP switches (all on by default) and every number above.

<ItemLink id="{sid}" />'''),
    'ja': ('ナノスーツのユニット', '''0.1.8.5 からナノスーツも強化ユニットを受け付ける。ナノのユニットは軽く、満載でも約 10 EU/t しか使わない。
強い効果はバーストで、ナノ予備量を使い、熱を加える。

## ナノコア

- **予備量**（0〜100）：バーストが使う。ナノ部位を着用している間、毎秒 2 回復する。1 ポイントにスーツの電力 400 EU を使う。
- **熱**（0〜100）：バーストのたびに熱がたまる。毎秒 4 放熱する（熱調整膜で 8）。
- **過熱**：熱が 100 になると過熱する。熱が 40 を下回るまでバーストできない。光学迷彩は解ける。

## バーストキー

**V**（メイン）か **B**（サブ）でバーストを使う。キーは操作設定で変えられる。
奇点管理パネルで各キーにバーストユニットを割り当てる。ナノ部位を選び、バーストの行をクリックする。「自動」は着用中のバーストユニットを次の順で選ぶ：キネティックダッシュ、光学迷彩、EMP。

## ユニット

{table}

## HF 同期

ナノ筋繊維と、メインハンドの起動したナノセイバーで HF 同期になる。攻撃速度 +12%、防具貫通 15%。刃が細かく震え、火花が散る。

## HUD

ナノ HUD にバイザーはいらない。照準の周りに予備量の弧（左、V と B のコスト目盛り）、熱の弧（右、冷却の矢印）、2 つのバーストチップとクールダウンを出す。

## サーバー設定

コモンコンフィグの NanoSuit 節に、ユニットごとのスイッチ、PvP スイッチ（初期値はすべてオン）、上のすべての数値がある。

<ItemLink id="{sid}" />'''),
    'zh': ('纳米套装单元', '''自 0.1.8.5 起，纳米套装也可以安装升级单元。纳米单元很轻：满载的纳米套装约耗 10 EU/t。
强力效果都是爆发：爆发消耗纳米储备并增加热量。

## 纳米核心

- **储备**（0 至 100）：爆发会消耗它。穿戴纳米部件时每秒回复 2 点，每点从套装扣除 400 EU。
- **热量**（0 至 100）：每次爆发都会增加热量。套装每秒散热 4 点（装有热调节膜时为 8）。
- **过热**：热量达到 100 时套装过热。热量降到 40 以下之前不能爆发，光学迷彩会解除。

## 爆发键

按 **V**（主）或 **B**（副）使用爆发。可在按键设置中更改。
在奇点管理面板中为每个键分配爆发单元：选中纳米部件，点击爆发行。“自动”按以下顺序选择已穿戴的爆发单元：动能冲刺、光学迷彩、EMP。

## 单元

{table}

## 高频同步

纳米肌纤维加上主手中开启的纳米剑即为高频同步：攻击速度 +12%，护甲穿透 15%。刀刃会高频颤动并迸出火花。

## HUD

纳米 HUD 不需要面罩。准星周围显示：储备弧（左侧，带 V 与 B 的消耗刻度）、热量弧（右侧，带散热箭头）以及两个带冷却的爆发标签。

## 服务器设置

通用配置的 NanoSuit 节包含每个单元的开关、PvP 开关（默认全部开启）以及上述所有数值。

<ItemLink id="{sid}" />'''),
}
HEAD = {
    'en': '| Unit | Fits | LP | Power | Burst |\n|---|---|---|---|---|',
    'ja': '| ユニット | 部位 | LP | 消費 | バースト |\n|---|---|---|---|---|',
    'zh': '| 单元 | 部位 | LP | 耗电 | 爆发 |\n|---|---|---|---|---|',
}
LANG_DIR = {'en': '', 'ja': '_ja_jp', 'zh': '_zh_cn'}


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def main():
    for lang, sub in LANG_DIR.items():
        base = os.path.join(G, sub) if sub else G
        title, body = STATION[lang]
        page = ('---\nnavigation:\n  title: "{t}"\n  icon: {sid}\n  parent: suit/index.md\n  position: 0\nitem_ids:\n  - {sid}\n---\n\n'
                '# {t}\n\n<Row>\n  <BlockImage id="{sid}" scale="3" />\n</Row>\n\n').format(t=title, sid=SID)
        page += body.format(power='\n'.join(POWER[lang]), sid=SID) + '\n'
        write(os.path.join(base, 'machines/producer_block_quantum_modification_station.md'), page)
        title, body = NANO[lang]
        rows = [HEAD[lang]]
        for uid, slot, lp, power, burst in UNITS:
            rows.append('| <ItemLink id="mio_icif:module/item_module_{u}" /> | {s} | {lp} | {p} | {b} |'.format(
                u=uid, s=SLOT[lang][slot], lp=lp, p=power, b=burst))
        rows.append('| <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | {s} | 1 | 6 EU/t | - |'.format(s=SLOT[lang]['CHEST']))
        rows.append('| <ItemLink id="mio_icif:module/item_module_blast_warning" /> | {s} | 1 | 2 EU/t | - |'.format(
            s=SLOT[lang]['CHEST'] + ' / ' + SLOT[lang]['HEAD']))
        page = ('---\nnavigation:\n  title: "{t}"\n  icon: mio_icif:armor/item_armor_nano_chestplate\n  parent: suit/index.md\n  position: 1\n'
                'item_ids:\n  - mio_icif:module/item_module_myofiber\n  - mio_icif:module/item_module_thermoregulator\n'
                '  - mio_icif:module/item_module_emp_emitter\n  - mio_icif:module/item_module_optical_camo\n'
                '  - mio_icif:module/item_module_toxin_filter\n  - mio_icif:module/item_module_micro_capacitor\n'
                '  - mio_icif:module/item_nano_lattice_densifier\n---\n\n# {t}\n\n').format(t=title)
        page += body.format(table='\n'.join(rows), sid=SID) + '\n'
        write(os.path.join(base, 'suit/nano_units.md'), page)
    print('wrote guide pages')


if __name__ == '__main__':
    main()
