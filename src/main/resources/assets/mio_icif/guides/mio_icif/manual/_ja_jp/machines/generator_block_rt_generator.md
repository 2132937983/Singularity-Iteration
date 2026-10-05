---
navigation:
  title: "Radioisotope Thermoelectric Generator"
  icon: mio_icif:generator/block_rt_generator
  parent: generators.md
  position: 26
item_ids:
  - mio_icif:generator/block_rt_generator
---

# Radioisotope Thermoelectric Generator

<Row>
  <BlockImage id="mio_icif:generator/block_rt_generator" scale="3" />
</Row>

## 機能

機械は放射性同位体燃料ペレットでEUを作る。
ペレットを1個足すごとにEU出力が2倍になる。ペレットは消耗しない。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 32 EU/t |
| 蓄電容量 | 20,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを発電機に接続する。
2. 最大6個の燃料ペレットをペレットスロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_rt_generator" fallbackText="-" />
