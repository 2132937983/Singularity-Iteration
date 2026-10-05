---
navigation:
  title: "Solar Generator"
  icon: mio_icif:generator/block_solar_generator
  parent: generators.md
  position: 28
item_ids:
  - mio_icif:generator/block_solar_generator
---

# Solar Generator

<Row>
  <BlockImage id="mio_icif:generator/block_solar_generator" scale="3" />
</Row>

## 機能

機械は昼間に日光でEUを作る。
機械の真上から空が見える必要がある。
夜と雨の間、機械は停止する。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 1 EU/t |
| 蓄電容量 | 2 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 空のあるディメンション（例：オーバーワールド）に発電機を置く。
2. 発電機の上にある不透明なブロックをすべて取り除く。
3. LVケーブルを発電機に接続する。
4. 充電するバッテリーをバッテリースロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_solar_generator" fallbackText="-" />
