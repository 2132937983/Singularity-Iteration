---
navigation:
  title: "Miner"
  icon: mio_icif:producer/block_miner_elc
  parent: machines.md
  position: 44
item_ids:
  - mio_icif:producer/block_miner_elc
---

# Miner

<Row>
  <BlockImage id="mio_icif:producer/block_miner_elc" scale="3" />
</Row>

## 機能

採掘機は採掘パイプを地下へ伸ばし、各層の鉱石を掘る。
機械はドリル、採掘パイプ、ODスキャナーかOVスキャナーが必要だ。
ODスキャナーの半径はパイプの周囲3ブロックだ。OVスキャナーは6ブロックだ。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | LV (32 EU) |
| 最大入力 | 128 EU/t |
| 蓄電容量 | 10,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを機械に接続する。
2. 鉄ドリル、ダイヤモンドドリル、イリジウムドリルのどれかをドリルスロットに入れる。
3. 採掘パイプをパイプスロットに入れる。
4. ODスキャナーかOVスキャナーをスキャナースロットに入れる。
5. 保管スロットから鉱石を取り出す。

## 注意

- 縦穴の流体を取り除くため、採掘機の隣にポンプを置く。
- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** LVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_miner_elc" fallbackText="-" />
