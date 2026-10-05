---
navigation:
  title: "Pattern Storage"
  icon: mio_icif:producer/block_pattern_storage
  parent: machines.md
  position: 52
item_ids:
  - mio_icif:producer/block_pattern_storage
---

# Pattern Storage

<Row>
  <BlockImage id="mio_icif:producer/block_pattern_storage" scale="3" />
</Row>

## 機能

パターン保管機はUU物質の複製用に最大64個のアイテムパターンを保存する。
機械はパターン保存クリスタルでパターンを読み込み、書き出す。各操作でEUを使う。
隣のパターンスキャナーは新しいパターンをここに保存する。隣の複製機は選んだパターンを使う。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 100,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. パターンスキャナーと複製機の間にパターン保管機を置く。
2. LVケーブルを機械に接続する。
3. GUIの矢印ボタンでパターンを選ぶ。
4. パターンを読み込むか書き出すため、パターン保存クリスタルをスロットに入れる。

## 注意


> **警告：** LVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_pattern_storage" fallbackText="-" />
