---
navigation:
  title: "Advanced Drop Generator"
  icon: mio_icif:generator/block_advanced_drop_generator
  parent: generators.md
  position: 0
item_ids:
  - mio_icif:generator/block_advanced_drop_generator
---

# Advanced Drop Generator

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_drop_generator" scale="3" />
</Row>

## 機能

機械はドロップアイテムを引き寄せ、破壊してEUを作る。
機械の範囲と出力はドロップ発電機より大きい。
レアなアイテムは普通のアイテムより多くのEUを作る。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | ULV (8 EU) |
| 出力 | 40 EU/t |
| 蓄電容量 | 100,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機をアイテム回収ラインの出口に置く。
2. HVケーブルを発電機に接続する。
3. 不要なアイテムを発電機の近くに落とす。

## 注意


> **警告：** 機械は範囲内のすべてのドロップアイテムを破壊する。プレイヤーのアイテムも対象だ。


## レシピ

<RecipesFor id="mio_icif:generator/block_advanced_drop_generator" fallbackText="-" />
