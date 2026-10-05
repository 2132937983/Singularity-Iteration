---
navigation:
  title: "Advanced Cyclone Macerator"
  icon: mio_icif:producer/block_powder_advanced_elc
  parent: machines.md
  position: 0
item_ids:
  - mio_icif:producer/block_powder_advanced_elc
---

# Advanced Cyclone Macerator

<Row>
  <BlockImage id="mio_icif:producer/block_powder_advanced_elc" scale="3" />
</Row>

## 機能

高度サイクロン粉砕機はMVで粉砕機のレシピを使う。
機械は粉砕機より短い時間で各処理を終える。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 750 EU |
| 稼働時消費 | 5 EU/t |
| 1回の処理時間 | 150 tick (7.5 s) |
| 1回あたりの電力 | 750 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. 粉砕する鉱石かアイテムを入力スロットに入れる。
3. オーバークロッカーアップグレードを入れて処理を速くする。
4. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_powder_advanced_elc" fallbackText="-" />
