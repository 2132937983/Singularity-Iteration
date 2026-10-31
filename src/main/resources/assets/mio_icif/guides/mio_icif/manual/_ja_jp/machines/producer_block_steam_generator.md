---
navigation:
  title: "蒸気発生器"
  icon: mio_icif:producer/block_steam_generator
  parent: machines.md
  position: 61
item_ids:
  - mio_icif:producer/block_steam_generator
---

# 蒸気発生器

<Row>
  <BlockImage id="mio_icif:producer/block_steam_generator" scale="3" />
</Row>

## 機能

蒸気発生機は熱（HU）と水を使い、蒸気か過熱蒸気を作る。
GUIで水の流量と圧力弁を設定する。機械は蒸気を隣のタンクへ送る。
普通の水はボイラーを石灰化させ、機械を止める。蒸留水は石灰化を起こさない。

## 電力データ

| 項目 | 値 |
|---|---|
| 水 → 蒸気 | 1 mB → 100 mB |
| 熱入力 | 最大 1,200 HU/t |
| 過熱蒸気 | 220 bar から (圧力弁は最大 300 bar) |

このブロックはEUを使わない。

## 使用手順

1. 蒸気発生機の隣に熱発生機を置く。
2. 蒸留水をパイプで機械に入れる。
3. GUIを開き、水の流量と圧力弁を設定する。
4. 蒸気を受け取るため、機械の隣にタンクか蒸気運動エネルギー発生機を置く。

## 注意


> **警告：** システム熱が上限を超えると、蒸気発生機は爆発して壊れる。


> **警告：** 隣のブロックが受け取らない蒸気は小さな爆発を起こす。


## レシピ

<RecipesFor id="mio_icif:producer/block_steam_generator" fallbackText="-" />
