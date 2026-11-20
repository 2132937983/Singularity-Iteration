---
navigation:
  title: "高度金属成型機"
  icon: mio_icif:producer/block_metal_former_advanced
  parent: machines.md
  position: 5
item_ids:
  - mio_icif:producer/block_metal_former_advanced
---

# 高度金属成型機

<Row>
  <BlockImage id="mio_icif:producer/block_metal_former_advanced" scale="3" />
</Row>

## 機能

高度金属成形機はMVで金属成形機のレシピを使う。
圧延モードは板とケースを作る。切断モードは板からケーブルを作る。押出モードはケーブル、シャフト、缶を作る。
ブロックの見た目が現在のモードを示す。

処理時間はレシピの処理時間の5分の1（切り上げ）、電力は 50 EU/t 固定なので、1回あたりの電力は金属成形機と同じで、速さは 5 倍。例：鉄板を巻いて鉄ケーシングにするのは 60 tick。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | MV (128 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 1,000 EU |
| 稼働時消費 | 50 EU/t |
| 1回の処理時間 | レシピの処理時間の 1/5（既定 40 tick） |
| 1回あたりの電力 | 金属成形機と同じ（既定 2,000 EU） |

表の処理時間と電力はオーバークロックなしの値。オーバークロックアップグレード 1 個ごとに処理時間は 0.7 倍、電力は 1.6 倍になる。画面と JEI は現在の入力の実際の値を表示する。

## 使用手順

1. MVケーブルを機械に接続する。
2. GUIのモードボタンをクリックし、圧延、切断、押出を選ぶ。
3. インゴットか板を入力スロットに入れる。
4. 出力スロットから成果物を取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** MVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_metal_former_advanced" fallbackText="-" />
