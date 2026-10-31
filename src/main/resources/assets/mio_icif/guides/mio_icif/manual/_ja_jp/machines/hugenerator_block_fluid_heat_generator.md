---
navigation:
  title: "流体熱発生器"
  icon: mio_icif:hugenerator/block_fluid_heat_generator
  parent: generators.md
  position: 10
item_ids:
  - mio_icif:hugenerator/block_fluid_heat_generator
---

# 流体熱発生器

<Row>
  <BlockImage id="mio_icif:hugenerator/block_fluid_heat_generator" scale="3" />
</Row>

## 機能

機械は半流体燃料を燃やして熱（HU）を作る。
機械はバイオガス、バイオマス、原油、軽油を受け入れる。
機械は正面だけから熱を出す。

## 電力データ

| 項目 | 値 |
|---|---|
| 燃料：バイオガス | 0.5 mB/t → 32 HU/t (64 HU/mB) |
| 燃料：バイオマス | 1 mB/t → 16 HU/t (16 HU/mB) |
| 燃料：原油 | 0.5 mB/t → 32 HU/t (64 HU/mB) |
| 燃料：ディーゼル燃料 | 0.25 mB/t → 768 HU/t (3,072 HU/mB) |
| 燃料タンク | 10,000 mB |

このブロックはEUを使わない。

## 使用手順

1. 正面が熱を使う機械に接するように機械を置く。
2. 燃料バケツまたは燃料セルを入力スロットに入れる。
3. 出力スロットから空の容器を取り出す。

## レシピ

<RecipesFor id="mio_icif:hugenerator/block_fluid_heat_generator" fallbackText="-" />
