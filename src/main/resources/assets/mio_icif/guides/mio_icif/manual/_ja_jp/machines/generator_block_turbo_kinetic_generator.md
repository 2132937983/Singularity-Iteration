---
navigation:
  title: "ターボ運動エネルギー発電機"
  icon: mio_icif:generator/block_turbo_kinetic_generator
  parent: generators.md
  position: 33
item_ids:
  - mio_icif:generator/block_turbo_kinetic_generator
---

# ターボ運動エネルギー発電機

<Row>
  <BlockImage id="mio_icif:generator/block_turbo_kinetic_generator" scale="3" />
</Row>

## 機能

機械は正面のブロックからの運動エネルギー（KU）をEUに変換する。
風力または水力の運動エネルギー発生機のKUは全効率で変換する。
ほかの発生源のKUは低い効率で変換するが、基本の運動エネルギー発電機よりは高い。
画面はプラントの模擬盤：正面の KU 源、その回転で回る軸、発電機と EU 出力、入力負荷と KU あたりの EU。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | HV (512 EU)、512 EU/t を超えると EV (2,048 EU) |
| 生産 | 風力または水力の運動エネルギー発生機は 1 KU あたり 0.85 EU、ほかの発生源は 0.35 EU |
| KU入力 | 最大 2,000 KU/t (最大 1,700 EU/t) |
| 蓄電容量 | 200,000 EU |

この値は本バージョンのブロックエンティティから計測した。

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。

## 使用手順

1. 発電機の正面を風力運動エネルギー発生機の背面に接して置く。
2. EVケーブルを発電機に接続する（512 EU/t 以下なら HVケーブルでもよい）。

## 注意

- パケットの大きさは KU 入力で変わる：風力または水力の KU が 602 KU/t、ほかの発生源が 1,462 KU/t（512 EU/t）を超えると EV パケットになり、HVケーブルは焼け、HV機械は爆発する。

## レシピ

<RecipesFor id="mio_icif:generator/block_turbo_kinetic_generator" fallbackText="-" />
