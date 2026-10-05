---
navigation:
  title: "Pattern Scanner"
  icon: mio_icif:producer/block_scanner_elc
  parent: machines.md
  position: 51
item_ids:
  - mio_icif:producer/block_scanner_elc
---

# Pattern Scanner

<Row>
  <BlockImage id="mio_icif:producer/block_scanner_elc" scale="3" />
</Row>

## 機能

パターンスキャナーはEUを使ってアイテムをスキャンし、そのパターンとUU物質とEUの消費量を記録する。
スキャン完了でアイテムを消費する。機械はパターンをパターン保存クリスタルか隣のパターン保管機に保存する。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | HV (512 EU) |
| 最大入力 | 512 EU/t |
| 蓄電容量 | 512,000 EU |
| 稼働時消費 | 256 EU/t |
| 1回の処理時間 | 3,300 tick (165 s) |
| 1回あたりの電力 | 844,800 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. HVケーブルを機械に接続する。
2. スキャンするアイテムをスキャナースロットに入れる。
3. 空のパターン保存クリスタルを入れるか、隣にパターン保管機を置く。
4. スキャンが終わったら、GUIで結果を保存する。

## 注意

- 保存先にあるアイテムを、機械は再びスキャンしない。
- レッドストーン信号は機械を止める。

> **警告：** HVを超える過電圧で機械は爆発する。


## レシピ

<RecipesFor id="mio_icif:producer/block_scanner_elc" fallbackText="-" />
