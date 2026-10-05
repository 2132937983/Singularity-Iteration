---
navigation:
  title: "Turbocharged Kinetic Generator"
  icon: mio_icif:generator/block_turbo_kinetic_generator
  parent: generators.md
  position: 33
item_ids:
  - mio_icif:generator/block_turbo_kinetic_generator
---

# Turbocharged Kinetic Generator

<Row>
  <BlockImage id="mio_icif:generator/block_turbo_kinetic_generator" scale="3" />
</Row>

## 機能

機械は正面のブロックからの運動エネルギー（KU）をEUに変換する。
風力または水力の運動エネルギー発生機のKUは全効率で変換する。
ほかの発生源のKUは低い効率で変換する。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | HV (512 EU) |
| 出力 | 2,048 EU/t |
| 蓄電容量 | 200,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機の正面を風力運動エネルギー発生機の背面に接して置く。
2. HVケーブルを発電機に接続する。

## レシピ

<RecipesFor id="mio_icif:generator/block_turbo_kinetic_generator" fallbackText="-" />
