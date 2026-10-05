---
navigation:
  title: "Wireless Power Transmission Node"
  icon: mio_icif:wiring/block_wireless_power_transmission_node
  parent: power.md
  position: 37
item_ids:
  - mio_icif:wiring/block_wireless_power_transmission_node
---

# Wireless Power Transmission Node

<Row>
  <BlockImage id="mio_icif:wiring/block_wireless_power_transmission_node" scale="3" />
</Row>

## 機能

ノードがケーブルなしで、読み込み済みチャンク内の目標ブロックへ距離に関係なくEUを送る。
目標を持つノードは隣のケーブルからEUを取り、目標へ送る。
目標のないノードは隣の機械とケーブルに給電する。パケットは最も弱い隣接ブロックに合わせる。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LuV (32,768 EU) |
| 最大入力 | 32,768 EU/t |
| 蓄電容量 | 196,608 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. EUが必要な機械またはケーブルの隣に受信ノードを置く。
2. 発電機のケーブルの隣に送信ノードを置く。
3. スニークしながら無線電力マネージャーで受信ノードを右クリックし、目標を記録する。
4. 無線電力マネージャーで送信ノードを右クリックし、ノードを接続する。

## 注意

- 目標は機械や蓄電ブロックでもよい。
- ノードはEUを送る間に光る。読み込まれていないチャンクの目標にはEUが届かない。

## レシピ

<RecipesFor id="mio_icif:wiring/block_wireless_power_transmission_node" fallbackText="-" />
