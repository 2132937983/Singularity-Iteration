---
navigation:
  title: "Electric Sorter"
  icon: mio_icif:producer/block_sorter_elc
  parent: machines.md
  position: 22
item_ids:
  - mio_icif:producer/block_sorter_elc
---

# Electric Sorter

<Row>
  <BlockImage id="mio_icif:producer/block_sorter_elc" scale="3" />
</Row>

## 機能

電気ソーターはフィルターでアイテムを6面へ送る。各面に専用のフィルタースロットがある。
どのフィルターにも合わないアイテムは標準出力面へ行く。アイテム1個の移動ごとにEUを使う。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 15,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. 出力面の隣にインベントリを置く。
3. GUIで各面のフィルタースロットに見本のアイテムを入れる。
4. GUIで標準出力面を選ぶ。
5. アイテムをソーターに入れる。

## 注意

- フィルタースロットのスタック数が、そのフィルターの1回分の数を決める。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_sorter_elc" fallbackText="-" />
