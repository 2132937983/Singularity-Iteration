---
navigation:
  title: "Oil Rig Core"
  icon: mio_icif:oilrig/block_oil_rig_core
  parent: heavy.md
  position: 6
item_ids:
  - mio_icif:oilrig/block_oil_rig_core
---

# Oil Rig Core

<Row>
  <BlockImage id="mio_icif:oilrig/block_oil_rig_core" scale="3" />
</Row>

## 機能

石油リグコアは石油リグのマルチブロックを制御する。リグは岩盤まで掘り下げ、原油を作る。
ドリルはリグの下のブロックを取り除く。取り除いたブロックごとに、貯油モジュールへ原油が入ることがある。
ドリルがコアの周りの限られた範囲を掘り終えると、リグは止まる。

## 電力データ

| 項目 | 値 |
|---|---|
| 蓄電容量 | 50,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 石油リグコアを置き、同じ層の周りに8個のモジュールを置く。
2. コアの上にモジュールを2個積む。
3. その2個の上にチタン製ドリルフレームを3個積む。
4. 積んだモジュールの4側面にフレームを2個ずつ置く。
5. 入力モジュールと貯油モジュールを1個以上ずつ入れる。
6. HVケーブルを入力モジュールに接続する。

## 注意

- 石油リグパネルを右クリックし、リグの状態を見る。コアにGUIはない。

> **警告：** ドリルは掘削列のブロックを壊し、アイテムを落とさない。拠点の上にリグを作らない。


## レシピ

<RecipesFor id="mio_icif:oilrig/block_oil_rig_core" fallbackText="-" />
