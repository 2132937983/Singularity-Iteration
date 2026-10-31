---
navigation:
  title: "スターリング発電機"
  icon: mio_icif:generator/block_stirling_generator
  parent: generators.md
  position: 30
item_ids:
  - mio_icif:generator/block_stirling_generator
---

# スターリング発電機

<Row>
  <BlockImage id="mio_icif:generator/block_stirling_generator" scale="3" />
</Row>

## 機能

機械は熱（HU）をEUに変換する。
機械は背面と正面（銅板の面。設置時に自分を向いていた面）から熱を受け取る。両方に熱源があるときは背面から先に受け取る。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | MV (128 EU) |
| 蓄電容量 | 200,000 EU |
| 変換 | 2 HU → 1 EU |

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。

## 使用手順

1. 加熱機の正面を、スターリング発電機の背面か正面に接して置く。たとえば熱交換機とスターリング発電機は、銅板どうしを向かい合わせて置ける。
2. MVケーブルをスターリング発電機のほかの面に接続する（正面からもEUが出る。背面からは出ない）。
3. 加熱機に燃料またはEUを供給する。

## レシピ

<RecipesFor id="mio_icif:generator/block_stirling_generator" fallbackText="-" />
