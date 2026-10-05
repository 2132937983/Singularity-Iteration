---
navigation:
  title: "Stirling Kinetic Generator"
  icon: mio_icif:kugenerator/block_stirling_kinetic_generator
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:kugenerator/block_stirling_kinetic_generator
---

# Stirling Kinetic Generator

<Row>
  <BlockImage id="mio_icif:kugenerator/block_stirling_kinetic_generator" scale="3" />
</Row>

## 機能

機械は熱（HU）と水を運動エネルギー（KU）とお湯に変換する。
機械は正面以外のすべての面から熱を受け取る。
機械は正面だけからKUを送る。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 正面がKU消費機械に接するように機械を置く。
2. 機械の別の面に加熱機を置く。
3. 水バケツか水セルを水入力スロットに入れる。
4. 空の容器をお湯スロットに入れてお湯を集める。

## 注意

- 水タンクが空か、お湯タンクが満杯になると、機械は停止する。

## レシピ

<RecipesFor id="mio_icif:kugenerator/block_stirling_kinetic_generator" fallbackText="-" />
