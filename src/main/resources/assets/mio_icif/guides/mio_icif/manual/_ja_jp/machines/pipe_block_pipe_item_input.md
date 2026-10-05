---
navigation:
  title: "Item Extraction Pipe"
  icon: mio_icif:pipe/block_pipe_item_input
  parent: heavy.md
  position: 2
item_ids:
  - mio_icif:pipe/block_pipe_item_input
---

# Item Extraction Pipe

<Row>
  <BlockImage id="mio_icif:pipe/block_pipe_item_input" scale="3" />
</Row>

## 機能

アイテム抽出パイプは隣接するコンテナからアイテムを取り出し、接続したアイテム輸送パイプに送る。
EUがない時、パイプは1回に1個を運ぶ。MVケーブルのEUで1回に運ぶ数が増える。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. アイテム抽出パイプを元のチェストの隣に置く。
2. 抽出パイプから目標のコンテナまでアイテム輸送パイプをつなぐ。
3. MVケーブルを接続し、1回に運ぶ数を増やす。
4. レンチでパイプの枝を右クリックし、その方向を閉じる。

## 注意

- 抽出パイプはコンテナにアイテムを入れない。輸送パイプが受け取れる時だけ動作する。

## レシピ

<RecipesFor id="mio_icif:pipe/block_pipe_item_input" fallbackText="-" />
