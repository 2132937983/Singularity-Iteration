---
navigation:
  title: "Crop Manager"
  icon: mio_icif:producer/block_matron_elc
  parent: machines.md
  position: 19
item_ids:
  - mio_icif:producer/block_matron_elc
---

# Crop Manager

<Row>
  <BlockImage id="mio_icif:producer/block_matron_elc" scale="3" />
</Row>

## 機能

作物管理機は周囲9×3×9の範囲のクロップスティックを世話する。
機械は各クロップスティックに肥料、水、除草剤を与える。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 32 EU/t |
| 蓄電容量 | 10,000 EU |
| 稼働時消費 | 1 EU/t |
| 1回の処理時間 | 10 tick (0.5 s) |
| 1回あたりの電力 | 10 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 機械を畑の中央に置く。
2. LVケーブルを機械に接続する。
3. 肥料を肥料スロットに入れる。
4. 作物管理用除草剤を除草剤スロットに入れる。
5. 水セルかパイプで水を供給する。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** LVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_matron_elc" fallbackText="-" />
