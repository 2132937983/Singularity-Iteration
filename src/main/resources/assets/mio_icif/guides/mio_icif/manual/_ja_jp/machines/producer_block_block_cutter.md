---
navigation:
  title: "ブロックカッター"
  icon: mio_icif:producer/block_block_cutter
  parent: machines.md
  position: 14
item_ids:
  - mio_icif:producer/block_block_cutter
---

# ブロックカッター

<Row>
  <BlockImage id="mio_icif:producer/block_block_cutter" scale="3" />
</Row>

## 機能

ブロックカッターはEUと切断刃でブロックを切る。金属ブロックは板になる。原木は板材になる。
各レシピは最低限の刃の硬さを要求する。ダイヤモンド切断刃は鉄切断刃より硬い。
原木は同じ木の板材になる。すべての原木・木・幹とその樹皮を剥いだもの、竹ブロック、板材のクラフトレシピを持つ他 MOD の木材に対応する。原木 1 個で板材 6 枚（手作業では 4 枚）、竹ブロック 1 個で 3 枚（手作業では 2 枚）。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 43,200 EU |
| 稼働時消費 | 48 EU/t |
| 1回の処理時間 | 900 tick (45 s) |
| 1回あたりの電力 | 43,200 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. MVケーブルを機械に接続する。
2. 切断刃を刃スロットに入れる。
3. 切るブロックを入力スロットに入れる。
4. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_block_cutter" fallbackText="-" />
