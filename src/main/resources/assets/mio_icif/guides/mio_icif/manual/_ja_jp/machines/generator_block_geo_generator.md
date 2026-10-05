---
navigation:
  title: "Geothermal Generator"
  icon: mio_icif:generator/block_geo_generator
  parent: generators.md
  position: 14
item_ids:
  - mio_icif:generator/block_geo_generator
---

# Geothermal Generator

<Row>
  <BlockImage id="mio_icif:generator/block_geo_generator" scale="3" />
</Row>

## 機能

機械は溶岩をEUに変換する。
機械は溶岩バケツと溶岩セルの溶岩を内部タンクに移す。
隣接する溶岩ブロック1個ごとに発電量が増える。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 20 EU/t |
| 蓄電容量 | 2,400 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを発電機に接続する。
2. 溶岩バケツを入力スロットに入れる。
3. 出力スロットから空のバケツを取り出す。
4. 充電するバッテリーをバッテリースロットに入れる。

## 注意

- 出力スロットが満杯のとき、機械はバケツを空にしない。

## レシピ

<RecipesFor id="mio_icif:generator/block_geo_generator" fallbackText="-" />
