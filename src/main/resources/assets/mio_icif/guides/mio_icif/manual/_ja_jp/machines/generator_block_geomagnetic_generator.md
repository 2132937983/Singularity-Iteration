---
navigation:
  title: "Geomagnetic Generator"
  icon: mio_icif:generator/block_geomagnetic_generator
  parent: generators.md
  position: 12
item_ids:
  - mio_icif:generator/block_geomagnetic_generator
---

# Geomagnetic Generator

<Row>
  <BlockImage id="mio_icif:generator/block_geomagnetic_generator" scale="3" />
</Row>

## 機能

構造が完成すると、機械は地磁気でEUを作る。
海面より低い場所や、下に空気か水があると出力が下がる。
ネザーと山岳のバイオームは出力を上げる。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | ZPM (131,072 EU) |
| 出力 | 30,720 EU/t |
| 蓄電容量 | 400,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 地磁気発電機を固い地面の上に置く。
2. 発電機の上に地磁気アンテナを2個縦に積む。
3. 発電機の下のブロックの四方に地磁気台座を4個置く。
4. ケーブルを発電機に接続する。
5. 充電するバッテリーをバッテリースロットに入れる。

## 注意

- 機械は下の最大20ブロックを調べる。空気か水のブロック1個ごとに出力が下がる。

## レシピ

<RecipesFor id="mio_icif:generator/block_geomagnetic_generator" fallbackText="-" />
