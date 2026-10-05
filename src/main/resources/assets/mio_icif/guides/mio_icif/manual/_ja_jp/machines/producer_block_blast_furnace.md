---
navigation:
  title: "高炉"
  icon: mio_icif:producer/block_blast_furnace
  parent: machines.md
  position: 13
item_ids:
  - mio_icif:producer/block_blast_furnace
---

# 高炉

<Row>
  <BlockImage id="mio_icif:producer/block_blast_furnace" scale="3" />
</Row>

## 機能

高炉は熱（HU）と圧縮空気を使い、鉄鉱石、鉄の粉、鉄インゴットから精錬鉄インゴットを作る。
副産物はスラグだ。高炉は正面の熱源からだけ熱を受け取る。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 高炉の正面に熱発生機を置く。
2. 圧縮機で空セルから圧縮空気セルを作る。
3. 圧縮空気セルを空気スロットに入れる。
4. 鉄を入力スロットに入れ、高炉が熱くなるまで待つ。
5. 出力スロットからインゴット、スラグ、空セルを取り出す。

## 注意

- 入力スロットに有効なアイテムがある間だけ、高炉は熱を受け取る。
- レッドストーン信号は機械を止める。レッドストーン信号反転アップグレードはこの動作を逆にする。

## レシピ

<RecipesFor id="mio_icif:producer/block_blast_furnace" fallbackText="-" />
