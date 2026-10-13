#!/usr/bin/env python3
"""0.1.9.0 guide: the "Frame cores" page (cores, socket rules, legacy conversion, exterior,
core burst) in English, Japanese and Chinese.
Usage: guide_190.py <repo root>
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
G = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
LANG_DIR = {'en': '', 'ja': '_ja_jp', 'zh': '_zh_cn'}
SID = 'mio_icif:producer/block_quantum_modification_station'
CORES = ['solar_array_1', 'solar_array_2', 'solar_array_3', 'graviton', 'propulsion']


def core(cid):
    return f'<ItemLink id="mio_icif:module/item_core_{cid}" />'


ROWS = {
    'en': [('Nano / quantum helmet', '8 EU/t day, 1 EU/t night (open sky)'),
           ('Nano / quantum helmet', '64 EU/t day, 8 EU/t night'),
           ('Quantum helmet', '512 EU/t day, 64 EU/t night'),
           ('Quantum chestplate', 'Gravity anchor (no fall damage, no knockback, no blast push), armour +1, emergency repair below 20 % health'),
           ('Quantum chestplate', '+90,000,000 EU, no flight ceiling, thrust +15 %, sharper maneuver vectoring, armour +1, knockback resistance +0.5, repair 2.0 health/s, arc discharge')],
    'ja': [('ナノ / 量子ヘルメット', '昼 8 EU/t、夜 1 EU/t（空の下）'),
           ('ナノ / 量子ヘルメット', '昼 64 EU/t、夜 8 EU/t'),
           ('量子ヘルメット', '昼 512 EU/t、夜 64 EU/t'),
           ('量子チェストプレート', '重力アンカー（落下ダメージなし・ノックバックなし・爆風で飛ばされない）、防御力 +1、体力 20% 未満で緊急修復'),
           ('量子チェストプレート', '+90,000,000 EU、飛行高度上限なし、推力 +15%、特殊機動のベクトル加速強化、防御力 +1、ノックバック耐性 +0.5、修復 2.0 体力/秒、アーク放電')],
    'zh': [('纳米 / 量子头盔', '白天 8 EU/t，夜间 1 EU/t（露天）'),
           ('纳米 / 量子头盔', '白天 64 EU/t，夜间 8 EU/t'),
           ('量子头盔', '白天 512 EU/t，夜间 64 EU/t'),
           ('量子胸甲', '重力锚（免疫摔落伤害、不受击退、不被爆炸冲飞），护甲 +1，生命低于 20% 时紧急修复'),
           ('量子胸甲', '+90,000,000 EU，无飞行高度上限，推力 +15%，特殊机动矢量加速增强，护甲 +1，击退抗性 +0.5，修复 2.0 生命/秒，电弧放电')],
}
HEAD = {'en': '| Core | Socket | Effect |', 'ja': '| コア | ソケット | 効果 |', 'zh': '| 核心 | 插槽 | 效果 |'}

TEXT = {
    'en': ('Frame cores', '''A frame core sits in the **core socket** of a suit helmet or chestplate. The socket is separate from the unit slots (quantum) and the lattice (nano), so a core never takes the place of a unit.

{table}

## Install, swap, remove

Put the piece and the core into the <ItemLink id="{sid}" /> (the core goes into the unit slot). Installing takes the usual install time and power.
A core in a piece that already has one swaps them: the old core comes back into the unit slot and waits there until you take it.
The core socket box of the station screen shows the core; click the small eject button next to it to take the core out (it goes to you).
A piece with a core shows a small diamond in the core colour in the corner of its icon.

## Exterior

Each core adds its exterior to the worn piece: a sun panel on a mast, a winged panel, the sun crown with a fan, the graviton ring with a gyro and shoulder pads, or twin boosters with a chest reactor.
The solar wings open in daylight under open sky and fold in the dark; the graviton ring and gyro turn while the suit has energy; the propulsion nozzles glow with the thrust.
Turn it off for one piece with its "Core exterior (3D)" switch in the Singularity Management Panel, or for everything in the **Look** tab (or the client option core_attachments).

## Core burst

With the Advanced Propulsion core, press **R** (core burst): a high-voltage arc discharge hits every creature around you and a shock wave throws them back. It costs 1,500,000 EU and has an 8 s cooldown (server config section FrameCores).

## Retired items

0.1.9.0 retired the solar helmets, the advanced quantum chestplate and the heavy quantum chestplate. Your old items are not lost: they turn into the standard piece with the matching core as soon as you carry them, in the modification station, on a Wingman, or when you craft one alone.

| Old item | Becomes |
|---|---|
| Solar Helmet | a Solar Skin unit |
| Advanced Solar Helmet | nano helmet + Solar Array I |
| Hybrid Solar Helmet | quantum helmet + Solar Array II |
| Ultimate Hybrid Solar Helmet | quantum helmet + Solar Array III |
| Heavy Quantum Chestplate | quantum chestplate + Graviton Bulwark |
| Advanced Quantum Chestplate | quantum chestplate + Advanced Propulsion |

The stored energy, units, switches, enchantments and name stay on the piece. The old 10 health/s repair of the advanced quantum chestplate is 2.0 health/s on the Advanced Propulsion core.'''),
    'ja': ('フレームコア', '''フレームコアはスーツのヘルメットまたはチェストプレートの**コアソケット**に入る。ソケットはユニットスロット（量子）や格子（ナノ）とは別なので、コアがユニットの枠を使うことはない。

{table}

## 取り付け・交換・取り外し

部位とコアを <ItemLink id="{sid}" /> に入れる（コアはユニットスロットへ）。取り付けには通常の取付時間と電力がかかる。
すでにコアのある部位に別のコアを入れると交換になる。古いコアはユニットスロットに戻り、取り出すまでそこで待つ。
改造台の画面のコアソケット枠にコアが表示される。横の小さな取り外しボタンを押すとコアを取り外す（手元に戻る）。
コア付きの部位はアイコンの角にコア色の小さなひし形が出る。

## 外装

コアは着用中の部位に外装を付ける：マスト上のソーラーパネル、翼付きパネル、ファン付きのサンクラウン、グラビトンリングとジャイロと肩パッド、またはツインブースターと胸のリアクター。
ソーラー翼は空の下の昼に開き、暗いと畳む。グラビトンリングとジャイロはスーツにエネルギーがある間回る。推進ノズルは推力に合わせて光る。
部位ごとに奇点管理パネルの「コア外装（3D）」スイッチで、またはすべてを **外観**（クライアント設定 core_attachments）で消せる。

## コアバースト

アドバンスド・プロパルジョン・コアがあれば **R**（コアバースト）を押す：高電圧アーク放電が周囲の生物すべてに当たり、衝撃波が吹き飛ばす。1,500,000 EU、クールダウン 8 秒（サーバー設定 FrameCores）。

## 退役アイテム

0.1.9.0 でソーラーヘルメット系、アドバンスド量子チェストプレート、ヘビー量子チェストプレートは退役した。古いアイテムは失われない：所持したとき、改造台、ウィングマン、または単体でクラフトしたときに、対応するコア付きの標準部位に変わる。

| 旧アイテム | 変換先 |
|---|---|
| ソーラーヘルメット | ソーラースキンユニット |
| アドバンスド・ソーラーヘルメット | ナノヘルメット + ソーラーアレイ I |
| ハイブリッド・ソーラーヘルメット | 量子ヘルメット + ソーラーアレイ II |
| アルティメット・ハイブリッド・ソーラーヘルメット | 量子ヘルメット + ソーラーアレイ III |
| ヘビー量子チェストプレート | 量子チェストプレート + グラビトン・バルワーク |
| アドバンスド量子チェストプレート | 量子チェストプレート + アドバンスド・プロパルジョン |

蓄電量、ユニット、スイッチ、エンチャント、名前は引き継がれる。アドバンスド量子チェストプレートの毎秒 10 回復は、アドバンスド・プロパルジョン・コアでは毎秒 2.0 回復になった。'''),
    'zh': ('框架核心', '''框架核心装在套装头盔或胸甲的**核心插槽**中。插槽独立于单元槽（量子）与晶格（纳米），核心不会占用单元的位置。

{table}

## 安装、更换、取出

将部件与核心放入 <ItemLink id="{sid}" />（核心放入单元槽）。安装需要通常的安装时间与电力。
向已有核心的部件放入另一个核心即为更换：旧核心回到单元槽，在你取出之前留在那里。
改装台界面的核心插槽框显示核心；点击旁边的小取出按钮即可取出核心（交给你）。
装有核心的部件在图标角落显示核心颜色的小菱形。

## 外装

核心会为穿戴的部件添加外装：桅杆上的太阳能板、带翼面板、带扇的太阳冠、重力环与陀螺仪及肩垫，或双推进器与胸前反应堆。
太阳能翼在露天白天展开，黑暗时收起；套装有电时重力环与陀螺仪旋转；推进喷口随推力发光。
可在奇点管理面板中用每个部件的"核心外装（3D）"开关关闭，或在**外观**（客户端选项 core_attachments）中全部关闭。

## 核心爆发

装有高级推进核心时按 **R**（核心爆发）：高压电弧放电命中周围所有生物，冲击波将其击退。消耗 1,500,000 EU，冷却 8 秒（服务器配置 FrameCores）。

## 退役物品

0.1.9.0 退役了太阳能头盔系列、高级量子胸甲与重型量子胸甲。旧物品不会丢失：携带时、放入改装台、由僚机穿戴或单独合成时，会变为装有对应核心的标准部件。

| 旧物品 | 转换为 |
|---|---|
| 太阳能头盔 | 太阳能皮肤单元 |
| 高级太阳能头盔 | 纳米头盔 + 太阳能阵列 I |
| 混合太阳能头盔 | 量子头盔 + 太阳能阵列 II |
| 终极混合太阳能头盔 | 量子头盔 + 太阳能阵列 III |
| 重型量子胸甲 | 量子胸甲 + 重力子壁垒 |
| 高级量子胸甲 | 量子胸甲 + 高级推进 |

储存的能量、单元、开关、附魔与名称都会保留。高级量子胸甲原先每秒 10 点的修复，在高级推进核心上为每秒 2.0 点。'''),
}


def main():
    for lang, sub in LANG_DIR.items():
        base = os.path.join(G, sub) if sub else G
        title, body = TEXT[lang]
        rows = [HEAD[lang], '|---|---|---|']
        for cid, (socket, effect) in zip(CORES, ROWS[lang]):
            rows.append(f'| {core(cid)} | {socket} | {effect} |')
        ids = '\n'.join('  - mio_icif:module/item_core_' + c for c in CORES)
        page = (f'---\nnavigation:\n  title: "{title}"\n  icon: mio_icif:module/item_core_propulsion\n  parent: suit/index.md\n  position: 2\n'
                f'item_ids:\n{ids}\n---\n\n# {title}\n\n')
        page += body.format(table='\n'.join(rows), sid=SID) + '\n'
        with open(os.path.join(base, 'suit/frame_cores.md'), 'w', encoding='utf-8', newline='\n') as f:
            f.write(page)
    print('wrote frame core pages')


if __name__ == '__main__':
    main()
