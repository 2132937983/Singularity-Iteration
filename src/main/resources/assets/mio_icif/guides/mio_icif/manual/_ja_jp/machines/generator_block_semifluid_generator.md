---
navigation:
  title: "Semifluid Generator"
  icon: mio_icif:generator/block_semifluid_generator
  parent: generators.md
  position: 27
item_ids:
  - mio_icif:generator/block_semifluid_generator
---

# Semifluid Generator

<Row>
  <BlockImage id="mio_icif:generator/block_semifluid_generator" scale="3" />
</Row>

## 機能

機械は半流体燃料を燃やしてEUを作る。
機械はバイオガス、バイオマス、原油、軽油を受け入れる。
燃料ごとにEU出力が異なる。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 16 EU/t |
| 蓄電容量 | 32,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを発電機に接続する。
2. 燃料バケツまたは燃料セルを入力スロットに入れる。
3. 出力スロットから空の容器を取り出す。
4. 充電するバッテリーをバッテリースロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_semifluid_generator" fallbackText="-" />
