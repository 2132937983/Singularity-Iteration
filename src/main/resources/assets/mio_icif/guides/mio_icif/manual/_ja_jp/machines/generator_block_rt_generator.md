---
navigation:
  title: "放射性同位体熱電発電機"
  icon: mio_icif:generator/block_rt_generator
  parent: generators.md
  position: 26
item_ids:
  - mio_icif:generator/block_rt_generator
---

# 放射性同位体熱電発電機

<Row>
  <BlockImage id="mio_icif:generator/block_rt_generator" scale="3" />
</Row>

## 機能

機械は放射性同位体燃料ペレットでEUを作る。
ペレットを 1 個足すごとに EU 出力が 2 倍になる（温帯の空気で 1、2、4、8、16、32 EU/t）。ペレットは消耗しない。
熱電発電機なので、冷却側が冷たいほど多く発電する。6 面のうち一番良い面が冷却側になる：氷・氷塊・青氷・雪ブロック・粉雪 ×1.25、水 ×1.15。それ以外は空気で、暑いバイオーム（35 °C 以上）×0.9、ネザー ×0.7。どこかの面に溶岩が接するとさらに ×0.6。
画面は記録計：高温接点と冷却側の温度計、直近 12 秒の出力とペレット表、冷却側の係数。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 32 EU/t |
| 蓄電容量 | 20,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. LVケーブルを発電機に接続する。
2. 最大6個の燃料ペレットをペレットスロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_rt_generator" fallbackText="-" />
