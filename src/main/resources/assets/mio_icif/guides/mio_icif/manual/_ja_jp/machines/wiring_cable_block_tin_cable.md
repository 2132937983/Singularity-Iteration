---
navigation:
  title: "Low-Voltage Cable"
  icon: mio_icif:wiring/cable/block_tin_cable
  parent: power.md
  position: 25
item_ids:
  - mio_icif:wiring/cable/block_tin_cable
---

# Low-Voltage Cable

<Row>
  <BlockImage id="mio_icif:wiring/cable/block_tin_cable" scale="3" />
</Row>

## 機能

低圧ケーブルがLVの電圧TierでEUを運ぶ。
通電中の裸ケーブルは、触れた生物に感電ダメージを与える。
上限を超えるパケットがケーブルを焼き切る。

## 電力データ

| 項目 | 値 |
|---|---|
| 定格電圧Tier | LV (32 EU) |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機から機械までケーブルを1列に置く。
2. ゴムでケーブルを右クリックし、絶縁層を1枚足す。
3. レンチでケーブルの腕を右クリックし、その方向を切断する。
4. レンチでケーブルの中心を右クリックし、その方向を再接続する。

## 注意

- 鉄の足場にケーブルを通し、足場にCFフォームを吹き付けることができる。強化石の中でもケーブルが導電する。

## レシピ

<RecipesFor id="mio_icif:wiring/cable/block_tin_cable" fallbackText="-" />
