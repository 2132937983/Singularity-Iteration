---
navigation:
  title: "Reactor Chamber"
  icon: mio_icif:reactor/block_reactor_chamber
  parent: heavy.md
  position: 11
item_ids:
  - mio_icif:reactor/block_reactor_chamber
---

# Reactor Chamber

<Row>
  <BlockImage id="mio_icif:reactor/block_reactor_chamber" scale="3" />
</Row>

## 機能

原子炉チャンバーは隣接する原子炉発電機に部品スロットを1列追加する。
チャンバーは原子炉のインベントリ、EU出力、熱を共有する。チャンバーへのレッドストーン信号でも原子炉が起動する。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 原子炉発電機を置く。
2. 原子炉の面に最大6個のチャンバーを置く。
3. チャンバーを右クリックし、原子炉のGUIを開く。
4. チャンバーにケーブルを接続し、EUを取り出す。

## 注意

- チャンバーはちょうど1基の原子炉に接する必要がある。そうでない時、チャンバーは壊れてアイテムになる。

> **警告：** チャンバーは熱容量を増やさない。部品を変えるたびに原子炉の温度を確認する。


## レシピ

<RecipesFor id="mio_icif:reactor/block_reactor_chamber" fallbackText="-" />
