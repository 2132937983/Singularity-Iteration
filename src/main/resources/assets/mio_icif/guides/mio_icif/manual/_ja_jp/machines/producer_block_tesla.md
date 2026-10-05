---
navigation:
  title: "Tesla Coil"
  icon: mio_icif:producer/block_tesla
  parent: machines.md
  position: 66
item_ids:
  - mio_icif:producer/block_tesla
---

# Tesla Coil

<Row>
  <BlockImage id="mio_icif:producer/block_tesla" scale="3" />
</Row>

## 機能

テスラコイルはEUを使い、周囲9ブロック以内の全生物にダメージを与える。
レッドストーン信号を受け、EUが十分な間、コイルは一定間隔で攻撃する。
目標への最初の攻撃は大きなダメージを与える。各攻撃は防具も傷める。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 10,000 EU |
| 稼働時消費 | 500 EU/t |
| 1回の処理時間 | 100 tick (5 s) |
| 1回あたりの電力 | 50,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 守る場所にテスラコイルを置く。
2. MVケーブルを機械に接続する。
3. コイルに十分なEUがたまるまで待つ。
4. レッドストーン信号を与えてコイルを起動する。

## 注意


> **警告：** コイルは範囲内のプレイヤーと飼いならした動物も攻撃する。


> **警告：** MVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_tesla" fallbackText="-" />
