---
navigation:
  title: "Advanced Item Distributor"
  icon: mio_icif:producer/block_item_distributor_elc
  parent: machines.md
  position: 3
item_ids:
  - mio_icif:producer/block_item_distributor_elc
---

# Advanced Item Distributor

<Row>
  <BlockImage id="mio_icif:producer/block_item_distributor_elc" scale="3" />
</Row>

## 機能

高度アイテム分配機は9スロットのバッファのアイテムを、優先順に隣のインベントリへ送る。
GUIで面の順番を設定する。このブロックはEUを使わない。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 分配機の各面の隣にインベントリを置く。
2. GUIを開き、優先順に面を追加する。
3. アイテムを分配機に入れる。

## 注意

- 前の面が全部を受け取れないときだけ、次の面がアイテムを受け取る。

## レシピ

<RecipesFor id="mio_icif:producer/block_item_distributor_elc" fallbackText="-" />
