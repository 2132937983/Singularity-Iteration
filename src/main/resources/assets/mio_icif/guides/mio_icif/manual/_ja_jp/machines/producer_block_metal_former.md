---
navigation:
  title: "Metal Former"
  icon: mio_icif:producer/block_metal_former
  parent: machines.md
  position: 43
item_ids:
  - mio_icif:producer/block_metal_former
---

# Metal Former

<Row>
  <BlockImage id="mio_icif:producer/block_metal_former" scale="3" />
</Row>

## 機能

金属成形機はEUを使い、3つのモードで金属を成形する。
圧延モードは板とケースを作る。切断モードは板からケーブルを作る。押出モードはケーブル、シャフト、缶を作る。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 32 EU/t |
| 蓄電容量 | 2,000 EU |
| 稼働時消費 | 10 EU/t |
| 1回の処理時間 | 200 tick (10 s) |
| 1回あたりの電力 | 2,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを機械に接続する。
2. GUIのモードボタンをクリックし、圧延、切断、押出を選ぶ。
3. インゴットか板を入力スロットに入れる。
4. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** LVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_metal_former" fallbackText="-" />
