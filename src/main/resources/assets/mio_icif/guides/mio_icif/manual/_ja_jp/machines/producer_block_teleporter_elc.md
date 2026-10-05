---
navigation:
  title: "Teleporter"
  icon: mio_icif:producer/block_teleporter_elc
  parent: machines.md
  position: 64
item_ids:
  - mio_icif:producer/block_teleporter_elc
---

# Teleporter

<Row>
  <BlockImage id="mio_icif:producer/block_teleporter_elc" scale="3" />
</Row>

## 機能

テレポーターは上に立つエンティティをリンク先のテレポーターへ送る。
移動ごとにEUを使う。距離が長いほど消費が増える。
テレポーターにGUIはない。機械はレッドストーン信号を受けている間だけ動く。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 8,192 EU/t |
| 蓄電容量 | 100,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. テレポーターを2台置き、それぞれにHVケーブルを接続する。
2. 周波数送信機で1台目のテレポーターを右クリックする。
3. 2台目のテレポーターを右クリックしてリンクを完了する。
4. テレポーターにレッドストーン信号を与える。
5. テレポーターの上に立ち、リンク先へ移動する。

## 注意

- リンクは双方向に働く。

> **警告：** HVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_teleporter_elc" fallbackText="-" />
