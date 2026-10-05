---
navigation:
  title: "電気ナノ高炉"
  icon: mio_icif:producer/block_blast_furnace_elc
  parent: machines.md
  position: 21
item_ids:
  - mio_icif:producer/block_blast_furnace_elc
---

# 電気ナノ高炉

<Row>
  <BlockImage id="mio_icif:producer/block_blast_furnace_elc" scale="3" />
</Row>

## 機能

電気ナノ高炉はEUを使い、高炉のレシピを処理する。機械は熱も圧縮空気も使わない。
機械は主産物だけを出す。機械はスラグを出さない。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | IV (8,192 EU) |
| 最大入力 | 8,192 EU/t |
| 蓄電容量 | 320,000 EU |
| 稼働時消費 | 8,000 EU/t |
| 1回の処理時間 | 40 tick (2 s) |
| 1回あたりの電力 | 320,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 変圧器を通してIV電源を機械に接続する。
2. 鉄を入力スロットに入れる。
3. オーバークロッカーアップグレードを入れて処理を速くする。
4. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** IVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_blast_furnace_elc" fallbackText="-" />
