---
navigation:
  title: "Pump"
  icon: mio_icif:producer/block_pump_elc
  parent: machines.md
  position: 53
item_ids:
  - mio_icif:producer/block_pump_elc
---

# Pump

<Row>
  <BlockImage id="mio_icif:producer/block_pump_elc" scale="3" />
</Row>

## 機能

ポンプは機械の前の流体源ブロックを取り除き、流体をタンクにためる。
汲み上げ範囲は高さ1層、長さ8ブロック、左右各8ブロックだ。
採掘機の隣では、ポンプは採掘機が見つけた流体も取り除く。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 32 EU/t |
| 蓄電容量 | 20 EU |
| 稼働時消費 | 1 EU/t |
| 1回の処理時間 | 20 tick (1 s) |
| 1回あたりの電力 | 20 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 正面を流体に向けてポンプを置く。
2. LVケーブルを機械に接続する。
3. 容器スロットに空のバケツか空セルを入れるか、パイプを接続する。
4. 出力スロットから満タンの容器を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** LVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_pump_elc" fallbackText="-" />
