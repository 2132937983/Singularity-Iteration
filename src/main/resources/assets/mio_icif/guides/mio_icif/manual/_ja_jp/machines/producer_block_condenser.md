---
navigation:
  title: "Condenser"
  icon: mio_icif:producer/block_condenser
  parent: machines.md
  position: 18
item_ids:
  - mio_icif:producer/block_condenser
---

# Condenser

<Row>
  <BlockImage id="mio_icif:producer/block_condenser" scale="3" />
</Row>

## 機能

凝縮器は蒸気か過熱蒸気を蒸留水に変える。
4つのベントスロットのヒートベントが速度を上げる。ヒートベント1個ごとにEUを使う。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 10,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 凝縮器に蒸気をパイプで入れるか、蒸気運動エネルギー発生機の隣に凝縮器を置く。
2. 速度を上げるため、ベントスロットにヒートベントを入れる。
3. ヒートベントを入れたときは、HVケーブルを機械に接続する。
4. パイプで蒸留水を取り出すか、容器スロットに空のバケツかセルを入れる。

## 注意

- ヒートベントがないとき、凝縮器はEUを使わない。

> **警告：** HVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_condenser" fallbackText="-" />
