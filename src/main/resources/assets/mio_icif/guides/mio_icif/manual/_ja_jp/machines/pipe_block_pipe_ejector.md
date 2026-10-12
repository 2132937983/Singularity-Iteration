---
navigation:
  title: "イジェクターパイプ"
  icon: mio_icif:pipe/block_pipe_ejector
  parent: machines.md
  position: 12
item_ids:
  - mio_icif:pipe/block_pipe_ejector
---

# イジェクターパイプ

<Row>
  <BlockImage id="mio_icif:pipe/block_pipe_ejector" scale="3" />
</Row>

## 機能

パイプラインやコンテナのアイテム・液体をワールドへ噴出します。
ノズルは設置先のブロックから外側を向きます。

## 規則

* パイプや機械はノズル以外の全ての面からアイテム・液体を入れられます。
* 空のときは背面のコンテナから最大16個のアイテム、または1バケツの液体を取り出します。
* 8tickごとにアイテムをノズルから勢いよく射出します。
* 1秒ごとに、満タンの1バケツがあればノズルの前に液体の水源を置きます。置けない場合は待機し、液体を消去しません。
* レッドストーン信号で停止します。

**注意：** 噴出した液体はバケツと同様に広がります。ノズル前方を確認してください。

## レシピ

<RecipesFor id="mio_icif:pipe/block_pipe_ejector" fallbackText="-" />
