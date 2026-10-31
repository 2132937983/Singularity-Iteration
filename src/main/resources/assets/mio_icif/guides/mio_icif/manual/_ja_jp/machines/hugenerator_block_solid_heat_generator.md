---
navigation:
  title: "固体熱発生器"
  icon: mio_icif:hugenerator/block_solid_heat_generator
  parent: generators.md
  position: 29
item_ids:
  - mio_icif:hugenerator/block_solid_heat_generator
---

# 固体熱発生器

<Row>
  <BlockImage id="mio_icif:hugenerator/block_solid_heat_generator" scale="3" />
</Row>

## 機能

機械はかまどの燃料を燃やして熱（HU）を作る。
燃料を1個燃やすごとに、灰スロットに灰が1個入る。
機械は正面だけから熱を出す。

## 電力データ

| 項目 | 値 |
|---|---|
| 熱出力 | 燃焼中 20 HU/t |
| 燃焼時間 | かまどの燃焼時間 ÷ 4 (石炭：400 t、8,000 HU) |

このブロックはEUを使わない。

## 使用手順

1. 正面が熱を使う機械に接するように機械を置く。
2. 石炭を燃料スロットに入れる。
3. 灰スロットから灰を取り出す。

## 注意

- 機械は溶岩を燃やさない。
- 灰スロットが満杯になると、機械は停止する。

## レシピ

<RecipesFor id="mio_icif:hugenerator/block_solid_heat_generator" fallbackText="-" />
