---
navigation:
  title: "Chunk Loader"
  icon: mio_icif:producer/block_chunk_loader
  parent: machines.md
  position: 16
item_ids:
  - mio_icif:producer/block_chunk_loader
---

# Chunk Loader

<Row>
  <BlockImage id="mio_icif:producer/block_chunk_loader" scale="3" />
</Row>

## 機能

チャンクローダーは選んだチャンクを読み込んだまま保つ。機械は周囲9×9チャンクの範囲で最大25チャンクを読み込む。
読み込むチャンクごとにEUを使う。EUがなくなると機械は止まる。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 2,500 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. GUIを開き、読み込むチャンクをクリックする。
3. 機械へのEU供給を安定させる。

## 注意

- チャンクローダーのあるチャンクも選択グリッドに含まれる。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_chunk_loader" fallbackText="-" />
