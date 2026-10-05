---
navigation:
  title: "Heat Exchanger"
  icon: mio_icif:hugenerator/block_heat_source_fluid
  parent: generators.md
  position: 15
item_ids:
  - mio_icif:hugenerator/block_heat_source_fluid
---

# Heat Exchanger

<Row>
  <BlockImage id="mio_icif:hugenerator/block_heat_source_fluid" scale="3" />
</Row>

## 機能

機械は溶岩またはホットクーラントを熱（HU）に変換する。
溶岩はパホイホイ溶岩になる。ホットクーラントはクーラントになる。
熱伝導体1個ごとに熱出力が増える。機械は正面だけから熱を出す。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 正面が熱を使う機械に接するように機械を置く。
2. 熱伝導体を伝導体スロットに入れる。
3. 溶岩バケツかホットクーラントセルを高温流体入力スロットに入れる。
4. 空の容器を低温流体スロットに入れて出力流体を集める。

## 注意

- 出力タンクが満杯になると、機械は停止する。
- 熱伝導体がないと、機械は熱を作らない。

## レシピ

<RecipesFor id="mio_icif:hugenerator/block_heat_source_fluid" fallbackText="-" />
