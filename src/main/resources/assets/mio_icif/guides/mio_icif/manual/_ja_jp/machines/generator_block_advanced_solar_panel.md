---
navigation:
  title: "Advanced Solar Panel"
  icon: mio_icif:generator/block_advanced_solar_panel
  parent: generators.md
  position: 3
item_ids:
  - mio_icif:generator/block_advanced_solar_panel
---

# Advanced Solar Panel

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_solar_panel" scale="3" />
</Row>

## 機能

機械は昼に日光でEUを作り、夜は低い出力で発電する。
機械の真上から空が見える必要がある。
雨の間、機械は夜の出力で発電する。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 32 EU/t |
| 蓄電容量 | 32,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 空のあるディメンションにパネルを置く。
2. パネルの上にある不透明なブロックをすべて取り除く。
3. LVケーブルをパネルに接続する。
4. 最大4個のバッテリーを充電スロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_advanced_solar_panel" fallbackText="-" />
