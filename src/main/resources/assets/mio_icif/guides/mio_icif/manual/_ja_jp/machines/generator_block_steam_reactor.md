---
navigation:
  title: "蒸気原子炉"
  icon: mio_icif:generator/block_steam_reactor
  parent: generators.md
  position: 30
item_ids:
  - mio_icif:generator/block_steam_reactor
---

# 蒸気原子炉

<Row>
  <BlockImage id="mio_icif:generator/block_steam_reactor" scale="3" />
</Row>

## 機能

機械は核燃料棒を燃やして熱を出す。EUは作らない。
機械内のスチームベントが熱を蒸気に変える。ベントが熱いほど蒸気が多い。
隣接する蒸気原子炉チャンバー1個ごとに部品スロットが1列増える。原子炉チャンバーは接続しない。

## 電力データ

このブロックはEUを使わない。

## 使用手順

1. 蒸気原子炉チャンバーを原子炉の各面に接して置く。
2. 燃料棒とスチームベントをスロットに入れる。
3. 水または蒸留水を原子炉かチャンバーに送り込む。
4. 蒸気タービン発電機か流体パイプを原子炉の隣に置く。
5. 原子炉にレッドストーン信号を入れて起動する。

## 炉温の段階・放射線の遮蔽・事故調査

炉温の段階、放射線の遮蔽、事故調査は[原子炉発電機](generator_block_nuclear_reactor_generator.md)と同じだ。

## 注意

- 原子炉は隣接する流体機械とパイプに蒸気を送る。蒸気タンクが満杯だと蒸気は止まる。

> **警告：** 水か蒸気の空きがないと、熱はスチームベントに残る。ベントと炉の温度が上がる。


> **警告：** 炉の温度が上限に達すると、原子炉は爆発する。


## レシピ

<RecipesFor id="mio_icif:generator/block_steam_reactor" fallbackText="-" />
