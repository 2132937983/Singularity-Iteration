---
navigation:
  title: "Matter Generator"
  icon: mio_icif:producer/block_matter_elc
  parent: machines.md
  position: 42
item_ids:
  - mio_icif:producer/block_matter_elc
---

# Matter Generator

<Row>
  <BlockImage id="mio_icif:producer/block_matter_elc" scale="3" />
</Row>

## 機能

物質生成機は大量のEUをUU物質に変える。
増幅スロットのスクラップ、スクラップボックス、トリウムスクラップボックスはEU消費を減らす。

## 電力データ

| 項目 | 値 |
|---|---|
| 入力電圧Tier | EV (2,048 EU) |
| 最大入力 | 8,192 EU/t |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 変圧器を通してEV電源を機械に接続する。
2. スクラップを増幅スロットに入れる。
3. UU物質を集めるため、容器スロットに空セルを入れるか、パイプを接続する。
4. 出力スロットからUU物質セルを取り出す。

## 注意

- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

> **警告：** EVを超える過電圧で機械は爆発する。上位の電圧Tierには変圧アップグレードを入れる。


## レシピ

<RecipesFor id="mio_icif:producer/block_matter_elc" fallbackText="-" />
