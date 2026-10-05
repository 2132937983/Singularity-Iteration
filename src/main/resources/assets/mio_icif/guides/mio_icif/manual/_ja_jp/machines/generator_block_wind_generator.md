---
navigation:
  title: "Wind Generator"
  icon: mio_icif:generator/block_wind_generator
  parent: generators.md
  position: 39
item_ids:
  - mio_icif:generator/block_wind_generator
---

# Wind Generator

<Row>
  <BlockImage id="mio_icif:generator/block_wind_generator" scale="3" />
</Row>

## 機能

機械は風でEUを作る。
Y 64より高いほど出力が増える。雨と雷雨も出力を増やす。
機械の周囲9x9x7の範囲にあるブロックは出力を下げる。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 32 EU/t |
| 蓄電容量 | 64,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機をY 64より十分高い場所に置く。
2. 発電機の周囲のブロックを取り除く。
3. LVケーブルを発電機に接続する。
4. 充電するバッテリーをバッテリースロットに入れる。

## 注意

- Y 64より低い場所では、機械はEUを作らない。
- 風の強さは時間とともにランダムに変わる。

## レシピ

<RecipesFor id="mio_icif:generator/block_wind_generator" fallbackText="-" />
