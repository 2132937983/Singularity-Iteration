---
navigation:
  title: "運動エネルギー発電機"
  icon: mio_icif:generator/block_kinetic_generator
  parent: generators.md
  position: 18
item_ids:
  - mio_icif:generator/block_kinetic_generator
---

# 運動エネルギー発電機

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
| 生産 | 4 KU ごとに 1 EU、最大 250 EU/t |
| KU入力 | 最大 1,000 KU/t |
| 蓄電容量 | 100,000 EU |

この値は本バージョンのブロックエンティティから計測した。

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。

## 使用手順

1. 発電機の正面を風力運動エネルギー発生機の背面に接して置く。
2. MVケーブルを発電機に接続する。

## レシピ

<RecipesFor id="mio_icif:generator/block_kinetic_generator" fallbackText="-" />
