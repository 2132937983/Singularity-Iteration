---
navigation:
  title: "スターリング運動エネルギー発生器"
  icon: mio_icif:kugenerator/block_stirling_kinetic_generator
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:kugenerator/block_stirling_kinetic_generator
---

# スターリング運動エネルギー発生器

<Row>
  <BlockImage id="mio_icif:kugenerator/block_stirling_kinetic_generator" scale="3" />
</Row>

## 機能

機械は熱（HU）と水を運動エネルギー（KU）とお湯に変換する。
機械は正面以外のすべての面から熱を受け取る。
機械は正面だけからKUを送る。
水は冷却水で、冷却端は約 15 °C に保たれる。下の表の比率は設計点（加熱ヘッド 650 °C、定格熱流 500 HU/t）の値で、低負荷や冷えたエンジンでは 1 HU あたりの出力が減る（スターリング発電機を参照）。
KU はフライホイール（20,000 KU）に入る。速く回るほど毎 tick 多く出す（満杯で最大 2,000 KU/t）ので、軸はエンジンの出力と釣り合うまで加速し、負荷が減ると減速する。KU は失われない。フライホイールが満杯（過回転）になるとエンジンは熱を取らない。
画面はプラントの模擬盤：熱の入力、水冷エンジン、水と温水のタンク、回転数付きのフライホイール、正面からの KU 出力。

## 電力データ

| 項目 | 値 |
|---|---|
| 変換 | 4 HU + 水 1 mB → 12 KU + 熱水 1 mB |
| フライホイール | 20,000 KU、満杯で最大 2,000 KU/t |
| 熱バッファ | 1,000 HU |

このブロックはEUを使わない。

## 使用手順

1. 正面がKU消費機械に接するように機械を置く。
2. 機械の別の面に加熱機を置く。
3. 水バケツか水セルを水入力スロットに入れる。
4. 空の容器をお湯スロットに入れてお湯を集める。

## 注意

- 水タンクが空か、お湯タンクが満杯になると、機械は停止する。

## レシピ

<RecipesFor id="mio_icif:kugenerator/block_stirling_kinetic_generator" fallbackText="-" />
