---
navigation:
  title: "Compressor"
  icon: mio_icif:producer/block_compressor_elc
  parent: machines.md
  position: 17
item_ids:
  - mio_icif:producer/block_compressor_elc
---

# Compressor

<Row>
  <BlockImage id="mio_icif:producer/block_compressor_elc" scale="3" />
</Row>

## 機能

圧縮機はEUを使い、アイテムを圧縮する。粉は板に、インゴットはブロックに、空セルは圧縮空気セルになる。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 32 EU/t |
| 蓄電容量 | 600 EU |
| 稼働時消費 | 2 EU/t |
| 1回の処理時間 | 300 tick (15 s) |
| 1回あたりの電力 | 600 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを機械に接続する。
2. 圧縮するアイテムを入力スロットに入れる。
3. オーバークロッカーアップグレードを入れて処理を速くする。
4. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** LVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_compressor_elc" fallbackText="-" />
