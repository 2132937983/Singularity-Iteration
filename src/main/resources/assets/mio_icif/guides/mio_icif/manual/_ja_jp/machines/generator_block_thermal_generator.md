---
navigation:
  title: "Thermal Generator"
  icon: mio_icif:generator/block_thermal_generator
  parent: generators.md
  position: 32
item_ids:
  - mio_icif:generator/block_thermal_generator
---

# Thermal Generator

<Row>
  <BlockImage id="mio_icif:generator/block_thermal_generator" scale="3" />
</Row>

## 機能

機械はかまどの燃料を燃やしてEUを作る。
機械はバッテリースロットのバッテリーを充電する。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 10 EU/t |
| 蓄電容量 | 4,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを発電機に接続する。
2. 石炭を燃料スロットに入れる。
3. 充電するバッテリーをバッテリースロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_thermal_generator" fallbackText="-" />
