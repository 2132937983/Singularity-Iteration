---
navigation:
  title: "Futures Machine"
  icon: mio_icif:producer/block_future_elc
  parent: machines.md
  position: 29
item_ids:
  - mio_icif:producer/block_future_elc
---

# Futures Machine

<Row>
  <BlockImage id="mio_icif:producer/block_future_elc" scale="3" />
</Row>

## 機能

先物取引機は毎日変わる価格で、工業コインと商品を売買する。
取引ごとにEUを使う。機械はインベントリからアイテムとコインを取る。1日の上限がある。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 100 EU/t |
| 蓄電容量 | 10,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを機械に接続する。
2. GUIを開き、カテゴリと商品を選ぶ。
3. 取引数量を設定する。
4. 購入か売却をクリックする。

## 注意

- 市場イベントは数日間、価格の動きを変える。
- 一部の商品は、GUIが示す進捗を得るまで使えない。

## レシピ

<RecipesFor id="mio_icif:producer/block_future_elc" fallbackText="-" />
