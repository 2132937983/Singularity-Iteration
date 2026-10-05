---
navigation:
  title: "EU Detector Cable"
  icon: mio_icif:wiring/block_eu_detector_cable
  parent: power.md
  position: 9
item_ids:
  - mio_icif:wiring/block_eu_detector_cable
---

# EU Detector Cable

<Row>
  <BlockImage id="mio_icif:wiring/block_eu_detector_cable" scale="3" />
</Row>

## 機能

EU検出ケーブルはIVケーブルで、通過するEUの流れを測る。
EUが流れる間、ケーブルが最大強度のレッドストーン信号を出す。
コンパレーターが読む信号強度は流量に応じて上がる。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 線路のケーブル1ブロックをEU検出ケーブルに置き換える。
2. 検出ケーブルの隣にレッドストーンランプを置く。
3. 検出ケーブルの隣にコンパレーターを置き、流量を読む。

## 注意

- 信号の更新には短い遅れがある。

## レシピ

<RecipesFor id="mio_icif:wiring/block_eu_detector_cable" fallbackText="-" />
