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
下の表の比率は設計点（加熱ヘッド 650 °C、冷却端 40 °C）の値。エンジンの効率は加熱ヘッドと冷却端の間のカルノー効率に従い、設計比率の 40%〜110% になる。熱流が多いほど加熱ヘッドは熱くなり（定格で 650 °C、4 分の 1 で約 530 °C）、冷えたエンジンは数秒暖機する。冷却端はバイオームの空気（砂漠は雪原より不利）。機械に水が接していると冷却端は約 15 °C になる。
画面はプラントの模擬盤：熱の入力、加熱ヘッドと冷却端の温度付きのエンジン、フライホイール、発電機と EU 出力。
定格熱流：150 HU/t。

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
