---
navigation:
  title: "蒸気タービン発電機"
  icon: mio_icif:generator/block_steam_turbine
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:generator/block_steam_turbine
---

# 蒸気タービン発電機

<Row>
  <BlockImage id="mio_icif:generator/block_steam_turbine" scale="3" />
</Row>

## 機能

機械は蒸気をEUに変える。
機械はバッテリースロットのバッテリーを充電し、蒸気セルをタンクに空ける。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | MV (128 EU) |
| 出力 | 64 EU/t |
| 蓄電容量 | 32,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 機械を蒸気原子炉の隣に置くか、蒸気を運ぶパイプを機械に接続する。
2. MVケーブルを機械に接続する。

## 注意

- 蓄電容量に空きがある間だけ、機械は蒸気を使う。

## レシピ

<RecipesFor id="mio_icif:generator/block_steam_turbine" fallbackText="-" />
