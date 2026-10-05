---
navigation:
  title: "Electric Heat Generator"
  icon: mio_icif:hugenerator/block_heat_generator_elc
  parent: generators.md
  position: 7
item_ids:
  - mio_icif:hugenerator/block_heat_generator_elc
---

# Electric Heat Generator

<Row>
  <BlockImage id="mio_icif:hugenerator/block_heat_generator_elc" scale="3" />
</Row>

## 機能

機械はEUを熱（HU）に変換する。
コイルスロットのコイル1個ごとに熱出力が増える。
機械は正面だけから熱を出す。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | EV (2,048 EU) |
| 最大入力 | 2,048 EU/t |
| 蓄電容量 | 10,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 正面が熱を使う機械に接するように機械を置く。
2. コイルをコイルスロットに入れる。
3. ケーブルを機械に接続してEUを供給する。
4. 充電済みのバッテリーを予備電源としてバッテリースロットに入れる。

## 注意

- コイルがないと、機械は熱を作らない。

## レシピ

<RecipesFor id="mio_icif:hugenerator/block_heat_generator_elc" fallbackText="-" />
