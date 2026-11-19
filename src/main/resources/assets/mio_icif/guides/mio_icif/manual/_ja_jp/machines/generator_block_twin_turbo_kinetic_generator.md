---
navigation:
  title: "ツインターボ運動エネルギー発電機"
  icon: mio_icif:generator/block_twin_turbo_kinetic_generator
  parent: generators.md
  position: 34
item_ids:
  - mio_icif:generator/block_twin_turbo_kinetic_generator
---

# ツインターボ運動エネルギー発電機

<Row>
  <BlockImage id="mio_icif:generator/block_twin_turbo_kinetic_generator" scale="3" />
</Row>

## 機能

機械は正面のブロックからの運動エネルギー（KU）をEUに変換する。
風力または水力の運動エネルギー発生機のKUは全効率で変換する。
ほかの発生源のKUは低い効率で変換するが、基本の運動エネルギー発電機よりは高い。
画面はプラントの模擬盤：正面の KU 源、その回転で回る軸、発電機と EU 出力、入力負荷と KU あたりの EU。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | EV (2,048 EU)、2,048 EU/t を超えると IV (8,192 EU) |
| 生産 | 風力または水力の運動エネルギー発生機は 1 KU あたり 1.2 EU、ほかの発生源は 0.45 EU |
| KU入力 | 最大 4,000 KU/t (最大 4,800 EU/t) |
| 蓄電容量 | 400,000 EU |

この値は本バージョンのブロックエンティティから計測した。

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。

## 使用手順

1. 発電機の正面を風力運動エネルギー発生機の背面に接して置く。
2. IVケーブルを発電機に接続する（2,048 EU/t 以下なら EVケーブルでもよい）。

## 注意

- 機械のKUからEUへの変換比はターボ運動エネルギー発電機より高い。
- パケットの大きさは KU 入力で変わる：風力または水力の KU が 1,706 KU/t（2,048 EU/t）を超えると IV パケットになり、EVケーブルは焼け、EV機械は爆発する。ほかの発生源は最大 4,000 × 0.45 = 1,800 EU/t で、常に EV のまま。

## レシピ

<RecipesFor id="mio_icif:generator/block_twin_turbo_kinetic_generator" fallbackText="-" />
