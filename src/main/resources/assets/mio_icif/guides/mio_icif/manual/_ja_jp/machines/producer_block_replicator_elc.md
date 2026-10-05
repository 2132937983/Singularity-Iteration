---
navigation:
  title: "Replicator"
  icon: mio_icif:producer/block_replicator_elc
  parent: machines.md
  position: 57
item_ids:
  - mio_icif:producer/block_replicator_elc
---

# Replicator

<Row>
  <BlockImage id="mio_icif:producer/block_replicator_elc" scale="3" />
</Row>

## 機能

複製機はUU物質とEUを使い、保存したパターンからアイテムを複製する。
パターンはメモリスロットのパターン保存クリスタルか、隣のパターン保管機から来る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | EV (2,048 EU) |
| 最大入力 | 8,192 EU/t |
| 蓄電容量 | 2,000,000 EU |
| 稼働時消費 | 512 EU/t |
| 1回の処理時間 | 100 tick (5 s) |
| 1回あたりの電力 | 51,200 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 変圧器を通してEV電源を機械に接続する。
2. UU物質セルかパイプでUU物質を供給する。
3. パターン保存クリスタルをメモリスロットに入れるか、機械の隣にパターン保管機を置く。
4. GUIで単発かループをクリックして開始する。
5. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** EVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_replicator_elc" fallbackText="-" />
