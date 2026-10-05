---
navigation:
  title: "Nuclear Reactor Generator"
  icon: mio_icif:generator/block_nuclear_reactor_generator
  parent: generators.md
  position: 21
item_ids:
  - mio_icif:generator/block_nuclear_reactor_generator
---

# Nuclear Reactor Generator

<Row>
  <BlockImage id="mio_icif:generator/block_nuclear_reactor_generator" scale="3" />
</Row>

## 機能

機械は核燃料棒を燃やしてEUと熱を作る。
隣接する原子炉チャンバー1個ごとに部品スロットが1列増える。
原子炉圧力容器の構造を作ると、機械は流体モードに切り替わりクーラントを加熱する。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | IV (8,192 EU) |
| 出力 | 8,192 EU/t |
| 蓄電容量 | 1,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 原子炉チャンバーを原子炉の各面に接して置く。
2. 燃料棒と冷却部品をスロットに入れる。
3. ケーブルを原子炉に接続する。
4. 原子炉にレッドストーン信号を入れて起動する。

## 注意


> **警告：** 炉の温度が上限に達すると、原子炉は爆発する。


> **警告：** 炉の温度が高いと、原子炉はブロックに火をつけ、近くのモブを傷つける。


## レシピ

<RecipesFor id="mio_icif:generator/block_nuclear_reactor_generator" fallbackText="-" />
