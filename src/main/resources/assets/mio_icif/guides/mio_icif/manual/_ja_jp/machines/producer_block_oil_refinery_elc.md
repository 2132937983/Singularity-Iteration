---
navigation:
  title: "Oil Refinery"
  icon: mio_icif:producer/block_oil_refinery_elc
  parent: machines.md
  position: 48
item_ids:
  - mio_icif:producer/block_oil_refinery_elc
---

# Oil Refinery

<Row>
  <BlockImage id="mio_icif:producer/block_oil_refinery_elc" scale="3" />
</Row>

## 機能

精製機はEUを使い、流体を精製する。原油は軽油になる。
機械は水か蒸気も蒸留水に変える。
オーバークロッカーアップグレード1個ごとに、1サイクルの流体量とEU消費が増える。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 2,000 EU |
| 稼働時消費 | 8 EU/t |
| 1回の処理時間 | 10 tick (0.5 s) |
| 1回あたりの電力 | 80 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. 入力流体をパイプで機械に入れるか、満タンのセルを入力スロットに入れる。
3. 出力セルスロットに空セルを入れるか、パイプを接続して成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_oil_refinery_elc" fallbackText="-" />
