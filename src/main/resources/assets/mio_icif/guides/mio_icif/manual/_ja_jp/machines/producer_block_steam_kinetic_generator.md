---
navigation:
  title: "Steam Kinetic Generator"
  icon: mio_icif:producer/block_steam_kinetic_generator
  parent: machines.md
  position: 62
item_ids:
  - mio_icif:producer/block_steam_kinetic_generator
---

# Steam Kinetic Generator

<Row>
  <BlockImage id="mio_icif:producer/block_steam_kinetic_generator" scale="3" />
</Row>

## 機能

蒸気運動エネルギー発生機は蒸気か過熱蒸気をKUに変える。機械は正面からKUを出す。
機械はタービンスロットに蒸気タービンが必要だ。タービンは運転中に耐久値を失う。
機械は排気蒸気を隣の凝縮器へ送り、凝縮した蒸留水をタンクにためる。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 蒸気タービンをタービンスロットに入れる。
2. 機械の隣に凝縮器を置く。
3. KUを使うブロックを正面に置く。
4. 蒸気をパイプで機械に入れる。
5. パイプで蒸留水を抜く。

## 注意


> **警告：** 機械が排出できない蒸気は爆発を起こす。

- タンクの蒸留水は出力を下げる。水タンクが満杯になるとタービンは止まる。

## レシピ

<RecipesFor id="mio_icif:producer/block_steam_kinetic_generator" fallbackText="-" />
