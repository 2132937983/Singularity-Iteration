---
navigation:
  title: "Ore Washer"
  icon: mio_icif:producer/block_washer_elc
  parent: machines.md
  position: 49
item_ids:
  - mio_icif:producer/block_washer_elc
---

# Ore Washer

<Row>
  <BlockImage id="mio_icif:producer/block_washer_elc" scale="3" />
</Row>

## 機能

鉱石洗浄機はEUと水を使い、粉砕鉱石を精製粉砕鉱石と副産物にする。
各処理は内部タンクの水を使う。機械は出力スロットを3つ持つ。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 8,000 EU |
| 稼働時消費 | 16 EU/t |
| 1回の処理時間 | 400 tick (20 s) |
| 1回あたりの電力 | 6,400 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. 水バケツか水セルを水スロットに入れるか、水のパイプを接続する。
3. 粉砕鉱石を入力スロットに入れる。
4. 3つの出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_washer_elc" fallbackText="-" />
