---
navigation:
  title: "Dimension Oil Rig Core"
  icon: mio_icif:oilrig/block_dimension_oil_rig_core
  parent: heavy.md
  position: 0
item_ids:
  - mio_icif:oilrig/block_dimension_oil_rig_core
---

# Dimension Oil Rig Core

<Row>
  <BlockImage id="mio_icif:oilrig/block_dimension_oil_rig_core" scale="3" />
</Row>

## 機能

次元石油リグコアは大型の石油リグを制御する。ドリルはコアの周りを渦巻き状に動き、範囲の上限はない。
リグは石油リグと同じモジュールを使う。1ブロックあたりの原油が多く、EUも多く使う。

## 電力データ

| 項目 | 値 |
|---|---|
| 蓄電容量 | 200,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. コアを置き、周りに8個、上に2個のモジュールを置く。
2. 1層下、コアから2ブロックの位置に16個のモジュールを輪状に置く。
3. 上にチタン製ドリルフレームを4個積み、3個目の周りに4個置く。
4. コアの層で、コアから3ブロックの4方向にフレームを1個ずつ置く。
5. コアの1層上で、コアから2ブロックの4方向にフレームを1個ずつ置く。
6. HVケーブルを入力モジュールに接続する。

## 注意

- 構造には入力モジュールと貯油モジュールが1個以上ずつ必要だ。

> **警告：** ドリルは経路上のブロックを壊し、アイテムを落とさない。掘削範囲は際限なく広がる。


## レシピ

<RecipesFor id="mio_icif:oilrig/block_dimension_oil_rig_core" fallbackText="-" />
