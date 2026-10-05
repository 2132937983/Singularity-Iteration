---
navigation:
  title: "Batch Crafter"
  icon: mio_icif:producer/block_batch_crafter
  parent: machines.md
  position: 12
item_ids:
  - mio_icif:producer/block_batch_crafter
---

# Batch Crafter

<Row>
  <BlockImage id="mio_icif:producer/block_batch_crafter" scale="3" />
</Row>

## 機能

バッチクラフターはEUを使い、作業台のレシピを自動でクラフトする。
GUIがレシピの型を持つ。機械は9つの材料スロットから材料を取る。
空のバケツのような容器の残りは、別の出力スロットへ行く。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 20,000 EU |
| 稼働時消費 | 2 EU/t |
| 1回の処理時間 | 40 tick (2 s) |
| 1回あたりの電力 | 80 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. GUIのクラフトグリッドにレシピの型を置く。
3. 材料を材料スロットに入れるか、パイプで入れる。
4. オーバークロッカーアップグレードを入れて処理を速くする。
5. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_batch_crafter" fallbackText="-" />
