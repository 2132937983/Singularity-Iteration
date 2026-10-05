---
navigation:
  title: "Magnetizer"
  icon: mio_icif:producer/block_magnetizer
  parent: machines.md
  position: 41
item_ids:
  - mio_icif:producer/block_magnetizer
---

# Magnetizer

<Row>
  <BlockImage id="mio_icif:producer/block_magnetizer" scale="3" />
</Row>

## 機能

磁化機はEUを使い、つながった鉄格子と鉄フェンスを磁化する。
鉄、金、ネザライト、電気のブーツを履いたプレイヤーは、磁化した格子の中で上昇する。
機械は自身の高さから上下20ブロック以内の格子を磁化する。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 32 EU/t |
| 蓄電容量 | 100 EU |
| 稼働時消費 | 5 EU/t |
| 1回の処理時間 | 100 tick (5 s) |
| 1回あたりの電力 | 500 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 鉄格子で縦の柱を作る。
2. 柱の一番下のブロックの隣に磁化機を置く。
3. LVケーブルを機械に接続する。
4. 鉄のブーツか電気ブーツを履く。
5. 格子の中に入って上昇する。

## 注意

- レッドストーン信号は機械を止める。

> **警告：** LVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_magnetizer" fallbackText="-" />
