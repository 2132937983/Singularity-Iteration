---
navigation:
  title: "Kinetic Generator"
  icon: mio_icif:generator/block_kinetic_generator
  parent: generators.md
  position: 18
item_ids:
  - mio_icif:generator/block_kinetic_generator
---

# Kinetic Generator

<Row>
  <BlockImage id="mio_icif:generator/block_kinetic_generator" scale="3" />
</Row>

## 機能

機械は運動エネルギー（KU）をEUに変換する。
機械は正面に隣接するブロックからKUを受け取る。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | MV (128 EU) |
| 出力 | 4,096 EU/t |
| 蓄電容量 | 100,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機の正面を風力運動エネルギー発生機の背面に接して置く。
2. MVケーブルを発電機に接続する。

## レシピ

<RecipesFor id="mio_icif:generator/block_kinetic_generator" fallbackText="-" />
