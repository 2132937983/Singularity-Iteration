---
navigation:
  title: "Wind Kinetic Generator"
  icon: mio_icif:kugenerator/block_wind_kinetic_generator
  parent: generators.md
  position: 40
item_ids:
  - mio_icif:kugenerator/block_wind_kinetic_generator
---

# Wind Kinetic Generator

<Row>
  <BlockImage id="mio_icif:kugenerator/block_wind_kinetic_generator" scale="3" />
</Row>

## 機能

機械はローターで風を運動エネルギー（KU）に変換する。
風は高さと悪天候で強くなる。ローター前方のブロックは風を弱める。
機械は背面だけからKUを送る。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. ローターをローター用スロットに入れる。
2. ローター前方の範囲にあるブロックをすべて取り除く。
3. 運動エネルギー発電機の正面を機械の背面に接して置く。
4. 風力計を機械に使って有効な風の強さを読む。

## 注意


> **警告：** 風がローターの上限を超えると、ローターの消耗が4倍になる。

- ローターは運転中に消耗する。壊れる前にローターを交換する。

## レシピ

<RecipesFor id="mio_icif:kugenerator/block_wind_kinetic_generator" fallbackText="-" />
