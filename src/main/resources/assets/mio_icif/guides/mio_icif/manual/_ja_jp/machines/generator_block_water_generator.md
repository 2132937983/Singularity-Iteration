---
navigation:
  title: "水力発電機"
  icon: mio_icif:generator/block_water_generator
  parent: generators.md
  position: 37
item_ids:
  - mio_icif:generator/block_water_generator
---

# 水力発電機

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
| 出力 | 最大 6 EU/t |
| 蓄電容量 | 64,000 EU |
| タンクの水 | 1 mB/t → 4 EU/t (4 EU/mB) |
| 正面の水 | +2 EU/t |

この値は本バージョンのブロックエンティティから計測した。

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。燃料消費は変わらない。

## 使用手順

1. 正面が水に接するように発電機を置く。
2. 水バケツをバケツスロットに入れる。
3. バケツスロットから空のバケツを取り出す。
4. LVケーブルを発電機に接続する。

## レシピ

<RecipesFor id="mio_icif:generator/block_water_generator" fallbackText="-" />
