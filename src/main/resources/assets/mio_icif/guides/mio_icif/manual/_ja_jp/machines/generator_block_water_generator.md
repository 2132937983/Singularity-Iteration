---
navigation:
  title: "Water Generator"
  icon: mio_icif:generator/block_water_generator
  parent: generators.md
  position: 37
item_ids:
  - mio_icif:generator/block_water_generator
---

# Water Generator

<Row>
  <BlockImage id="mio_icif:generator/block_water_generator" scale="3" />
</Row>

## 機能

機械は水バケツの水をEUに変換する。
機械の正面に水があると、バケツなしでもEUを作る。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 32 EU/t |
| 蓄電容量 | 64,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 正面が水に接するように発電機を置く。
2. 水バケツをバケツスロットに入れる。
3. バケツスロットから空のバケツを取り出す。
4. LVケーブルを発電機に接続する。

## レシピ

<RecipesFor id="mio_icif:generator/block_water_generator" fallbackText="-" />
