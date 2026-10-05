---
navigation:
  title: "Induction Furnace"
  icon: mio_icif:producer/block_induction_elc
  parent: machines.md
  position: 31
item_ids:
  - mio_icif:producer/block_induction_elc
---

# Induction Furnace

<Row>
  <BlockImage id="mio_icif:producer/block_induction_elc" scale="3" />
</Row>

## 機能

誘導炉はEUを使い、かまどのレシピで2つのアイテムを同時に製錬する。
機械は運転中に熱くなる。熱が高いほど速い。電力がないと熱は下がる。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 10,000 EU |
| 稼働時消費 | 15 EU/t |
| 1回の処理時間 | 4,000 tick (200 s) |
| 1回あたりの電力 | 60,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. 製錬するアイテムを2つの入力スロットに入れる。
3. 熱を保つため、機械を動かし続ける。
4. 2つの出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を熱いまま保ち、処理を止める。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_induction_elc" fallbackText="-" />
