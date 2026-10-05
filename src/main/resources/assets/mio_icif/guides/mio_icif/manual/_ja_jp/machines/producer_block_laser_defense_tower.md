---
navigation:
  title: "Laser Defense Tower"
  icon: mio_icif:producer/block_laser_defense_tower
  parent: machines.md
  position: 38
item_ids:
  - mio_icif:producer/block_laser_defense_tower
---

# Laser Defense Tower

<Row>
  <BlockImage id="mio_icif:producer/block_laser_defense_tower" scale="3" />
</Row>

## 機能

レーザー防衛塔は視線内の最大5体の目標にレーザーを撃つ。命中ごとにEUを使う。
標準の索敵範囲は各方向16ブロックだ。GUIで最大32ブロックまで広げる。
ブラックリストモードはリスト外の敵対モブを攻撃する。ホワイトリストモードはリストのモブ、タグ、プレイヤーだけを攻撃する。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 250,000 EU |
| 稼働時消費 | 1,250 EU/t |
| 1回の処理時間 | 100 tick (5 s) |
| 1回あたりの電力 | 125,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 見通しのよい場所に塔を置く。
2. MVケーブルを機械に接続する。
3. GUIを開き、水平と垂直の範囲を設定する。
4. 目標モードを選び、リストに項目を追加する。

## 注意

- アップグレードベイはオーバークロッカー、変圧、蓄電、レッドストーン信号反転のアップグレードを受け付ける。
- レッドストーン信号は塔を停止する。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_laser_defense_tower" fallbackText="-" />
