---
navigation:
  title: "高度スターリング発電機"
  icon: mio_icif:generator/block_advanced_stirling_generator
  parent: generators.md
  position: 4
item_ids:
  - mio_icif:generator/block_advanced_stirling_generator
---

# 高度スターリング発電機

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_stirling_generator" scale="3" />
</Row>

## 機能

機械は熱（HU）をEUに変換する。変換比はスターリング発電機より高い。
機械は正面をこちらに向けた隣接の加熱機から熱を取る。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | MV (128 EU) |
| 蓄電容量 | 1,000,000 EU |
| 変換 | 100 HU → 85 EU |
| 熱入力 | 最大 2,000 HU/t |

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。

## 使用手順

1. 機械の隣に加熱機を1個以上置く。
2. 各加熱機の正面を機械に向ける。
3. MVケーブルを機械に接続する。

## レシピ

<RecipesFor id="mio_icif:generator/block_advanced_stirling_generator" fallbackText="-" />
