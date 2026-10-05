---
navigation:
  title: "Terraformer"
  icon: mio_icif:producer/block_terra_elc
  parent: machines.md
  position: 65
item_ids:
  - mio_icif:producer/block_terra_elc
---

# Terraformer

<Row>
  <BlockImage id="mio_icif:producer/block_terra_elc" scale="3" />
</Row>

## 機能

テラフォーマーはEUを使い、テラフォーマーテンプレートで機械の周りの地形を変える。
テンプレートには耕作、砂漠、灌漑、冷却、平坦化、キノコがある。
機械にGUIはない。標準の半径は128ブロックだ。サーバー設定が半径を決める。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 100,000 EU |
| 稼働時消費 | 100 EU/t |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 変えたい範囲の中央にテラフォーマーを置く。
2. HVケーブルを機械に接続する。
3. テンプレートを持って機械を右クリックし、テンプレートを入れる。
4. スニークして機械を右クリックし、テンプレートを取り出す。

## 注意


> **警告：** 地形の変化は元に戻らない。建物のない場所でテンプレートを試す。

- レッドストーン信号は機械を止める。

> **警告：** HVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_terra_elc" fallbackText="-" />
