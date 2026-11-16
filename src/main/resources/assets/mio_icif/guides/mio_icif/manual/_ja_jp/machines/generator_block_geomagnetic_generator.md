---
navigation:
  title: "地磁気発電機"
  icon: mio_icif:generator/block_geomagnetic_generator
  parent: generators.md
  position: 12
item_ids:
  - mio_icif:generator/block_geomagnetic_generator
---

# 地磁気発電機

<Row>
  <BlockImage id="mio_icif:generator/block_geomagnetic_generator" scale="3" />
</Row>

## 機能

構造が完成すると、機械は地磁気でEUを作る。
海面より低い場所や、下に空気か水があると出力が下がる。
ネザーと山岳のバイオームは出力を上げる。
機械は磁場の変化で発電するので、出力は磁場の擾乱に従う：30,720 EU/t は擾乱 1 のときの出力。静穏な日は昼に日の当たる空が磁場を揺らす（正午で約 0.55、夜は 0.08）。毎日 12% の確率で磁気嵐（Kp 6〜9、半日〜1 日半）が起き、擾乱は最大 1.5 になる。同じワールドの発電機は同じ嵐を受ける。平均で以前の一定出力の約 27%。
画面は磁力計の記録：直近 3 日の擾乱、3 時間ごとの Kp、出力と地点の係数。地磁気探知機は地点の係数と現在の Kp を表示する。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | ZPM (131,072 EU) |
| 出力 | 擾乱 1 で 30,720 EU/t（平均 約 8,300 EU/t） |
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
