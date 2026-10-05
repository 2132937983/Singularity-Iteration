---
navigation:
  title: "Thermal Centrifuge"
  icon: mio_icif:producer/block_centrifuge_elc
  parent: machines.md
  position: 67
item_ids:
  - mio_icif:producer/block_centrifuge_elc
---

# Thermal Centrifuge

<Row>
  <BlockImage id="mio_icif:producer/block_centrifuge_elc" scale="3" />
</Row>

## 機能

熱遠心分離機はEUで加熱し、アイテムを最大3種類の成果物に分ける。
機械は最低限の熱に達してから処理を始める。電力がないと熱は下がる。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 24,000 EU |
| 稼働時消費 | 48 EU/t |
| 1回の処理時間 | 500 tick (25 s) |
| 1回あたりの電力 | 24,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. 粉砕鉱石を入力スロットに入れる。
3. 機械が作業温度に達するまで待つ。
4. 3つの出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_centrifuge_elc" fallbackText="-" />
