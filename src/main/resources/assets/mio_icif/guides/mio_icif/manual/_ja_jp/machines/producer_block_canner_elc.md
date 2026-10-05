---
navigation:
  title: "Canner"
  icon: mio_icif:producer/block_canner_elc
  parent: machines.md
  position: 15
item_ids:
  - mio_icif:producer/block_canner_elc
---

# Canner

<Row>
  <BlockImage id="mio_icif:producer/block_canner_elc" scale="3" />
</Row>

## 機能

缶詰機はEUを使い、4つのモードで動く。缶詰モードはアイテムを容器に入れる。例として食料を錫缶に入れる。
その他のモードは、セルをタンクに空ける、タンクからセルを満たす、流体と固体を混ぜる。
混合モードは蒸留水とラピスの粉から冷却材を作る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 32 EU/t |
| 蓄電容量 | 800 EU |
| 稼働時消費 | 4 EU/t |
| 1回の処理時間 | 200 tick (10 s) |
| 1回あたりの電力 | 800 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを機械に接続する。
2. GUIでモードを選ぶ。
3. 容器と素材を入力スロットに入れるか、流体をパイプで入れる。
4. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** LVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_canner_elc" fallbackText="-" />
