---
navigation:
  title: "Sky Patrol Laser Defense Station"
  icon: mio_icif:producer/block_sky_patrol_laser_tower
  parent: machines.md
  position: 59
item_ids:
  - mio_icif:producer/block_sky_patrol_laser_tower
---

# Sky Patrol Laser Defense Station

<Row>
  <BlockImage id="mio_icif:producer/block_sky_patrol_laser_tower" scale="3" />
</Row>

## 機能

スカイパトロール防衛ステーションは視線内の最大10体の目標にレーザーを撃つ。命中ごとにEUを使う。
標準の索敵範囲は各方向32ブロックだ。GUIで最大64ブロックまで広げる。
ブラックリストモードはリスト外の敵対モブを攻撃する。ホワイトリストモードはリストのモブ、タグ、プレイヤーだけを攻撃する。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 500,000 EU |
| 稼働時消費 | 2,500 EU/t |
| 1回の処理時間 | 100 tick (5 s) |
| 1回あたりの電力 | 250,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 見通しのよい場所に塔を置く。
2. HVケーブルを機械に接続する。
3. GUIを開き、水平と垂直の範囲を設定する。
4. 目標モードを選び、リストに項目を追加する。

## 注意

- アップグレードベイはオーバークロッカー、変圧、蓄電、レッドストーン信号反転のアップグレードを受け付ける。
- レッドストーン信号は塔を停止する。

> **警告：** HVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_sky_patrol_laser_tower" fallbackText="-" />
