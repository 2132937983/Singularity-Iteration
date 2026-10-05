---
navigation:
  title: "Oil Rig Input Module"
  icon: mio_icif:oilrig/block_oil_rig_input
  parent: heavy.md
  position: 7
item_ids:
  - mio_icif:oilrig/block_oil_rig_input
---

# Oil Rig Input Module

<Row>
  <BlockImage id="mio_icif:oilrig/block_oil_rig_input" scale="3" />
</Row>

## 機能

石油リグ入力モジュールはケーブルからEUを受け取り、リグのために蓄える。コアはすべての入力モジュールからEUを取る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 50,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 入力モジュールをリグのモジュール位置に置く。
2. HVケーブルをモジュールに接続する。
3. 入力モジュールを追加し、EUを多く蓄える。

## 注意

- リグには入力モジュールが1個以上必要だ。

## レシピ

<RecipesFor id="mio_icif:oilrig/block_oil_rig_input" fallbackText="-" />
