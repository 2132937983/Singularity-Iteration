---
navigation:
  title: "Extractor"
  icon: mio_icif:producer/block_extractor_elc
  parent: machines.md
  position: 25
item_ids:
  - mio_icif:producer/block_extractor_elc
---

# Extractor

<Row>
  <BlockImage id="mio_icif:producer/block_extractor_elc" scale="3" />
</Row>

## 機能

抽出機はEUを使い、アイテムから素材を取り出す。粘着性樹脂はゴムになる。
機械は入力、出力、バッテリーのスロットを各1つ、アップグレードスロットを4つ持つ。

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
2. アイテムを入力スロットに入れる。
3. オーバークロッカーアップグレードを入れて処理を速くする。
4. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** LVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_extractor_elc" fallbackText="-" />
