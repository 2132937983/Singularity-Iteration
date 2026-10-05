---
navigation:
  title: "Reactor Coolant Injector (RSH)"
  icon: mio_icif:producer/block_redstone_reactor_coolant_injector
  parent: machines.md
  position: 55
item_ids:
  - mio_icif:producer/block_redstone_reactor_coolant_injector
---

# Reactor Coolant Injector (RSH)

<Row>
  <BlockImage id="mio_icif:producer/block_redstone_reactor_coolant_injector" scale="3" />
</Row>

## 機能

原子炉冷却材注入器（RSH）は原子炉のレッドストーンコンデンセーターを修理する。修理ごとにレッドストーンブロック1個とEUを使う。
正面は原子炉か原子炉チャンバーに接する必要がある。機械にはレッドストーンブロックを入れる保管スロットがある。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 10,000 EU |
| 稼働時消費 | 1,000 EU/t |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 正面を原子炉か原子炉チャンバーに向けて注入器を置く。
2. MVケーブルを機械に接続する。
3. 保管スロットをレッドストーンブロックで満たす。
4. レッドストーンブロックの残りを定期的に確認する。

## 注意

- 在庫がなくなると、原子炉のコンデンセーターは消耗しきる。

> **警告：** MVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_redstone_reactor_coolant_injector" fallbackText="-" />
