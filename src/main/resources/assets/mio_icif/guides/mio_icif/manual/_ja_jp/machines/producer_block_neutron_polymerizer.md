---
navigation:
  title: "Particle Aggregator"
  icon: mio_icif:producer/block_neutron_polymerizer
  parent: machines.md
  position: 50
item_ids:
  - mio_icif:producer/block_neutron_polymerizer
---

# Particle Aggregator

<Row>
  <BlockImage id="mio_icif:producer/block_neutron_polymerizer" scale="3" />
</Row>

## 機能

粒子凝集機は大量のEUを使い、アイテムを希少なアイテムに変える。
石炭はダイヤモンドに、チタンはイリジウムに、ガラスケーブルは超伝導ケーブルになる。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | EV (2,048 EU) |
| 最大入力 | 2,048 EU/t |
| 蓄電容量 | 2,304,000 EU |
| 稼働時消費 | 1,536 EU/t |
| 1回の処理時間 | 1,500 tick (75 s) |
| 1回あたりの電力 | 2,304,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 変圧器を通してEV電源を機械に接続する。
2. 入力アイテムを入力スロットに入れる。
3. オーバークロッカーアップグレードを入れて処理を速くする。
4. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** EVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_neutron_polymerizer" fallbackText="-" />
