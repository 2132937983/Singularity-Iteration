---
navigation:
  title: "Drop Generator"
  icon: mio_icif:generator/block_drop_generator
  parent: generators.md
  position: 6
item_ids:
  - mio_icif:generator/block_drop_generator
---

# Drop Generator

<Row>
  <BlockImage id="mio_icif:generator/block_drop_generator" scale="3" />
</Row>

## 機能

機械は近くのドロップアイテムを引き寄せ、破壊してEUを作る。
レアなアイテムは普通のアイテムより多くのEUを作る。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | ULV (8 EU) |
| 出力 | 20 EU/t |
| 蓄電容量 | 10,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機をアイテム回収ラインの出口に置く。
2. MVケーブルを発電機に接続する。
3. 不要なアイテムを発電機の近くに落とす。

## 注意


> **警告：** 機械は範囲内のすべてのドロップアイテムを破壊する。プレイヤーのアイテムも対象だ。


## レシピ

<RecipesFor id="mio_icif:generator/block_drop_generator" fallbackText="-" />
