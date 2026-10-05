---
navigation:
  title: "Electric Kinetic Generator"
  icon: mio_icif:kugenerator/block_kinetic_generator_elc
  parent: generators.md
  position: 8
item_ids:
  - mio_icif:kugenerator/block_kinetic_generator_elc
---

# Electric Kinetic Generator

<Row>
  <BlockImage id="mio_icif:kugenerator/block_kinetic_generator_elc" scale="3" />
</Row>

## 機能

機械はEUを運動エネルギー（KU）に変換する。
モータースロットの電気モーター1個ごとにKU出力が増える。
機械は正面だけからKUを送る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 1,000 EU/t |
| 蓄電容量 | 40,000 EU |
| 稼働時消費 | 100 EU/t |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 正面がKU消費機械に接するように機械を置く。
2. 電気モーターをモータースロットに入れる。
3. ケーブルを機械に接続してEUを供給する。
4. 充電済みのバッテリーを予備電源としてバッテリースロットに入れる。

## 注意

- 電気モーターがないと、機械はKUを作らない。
- ターボ発電機はこの機械のKUを低い効率で変換する。

## レシピ

<RecipesFor id="mio_icif:kugenerator/block_kinetic_generator_elc" fallbackText="-" />
