---
navigation:
  title: "ディーゼル発電機"
  icon: mio_icif:generator/block_diesel_generator
  parent: generators.md
  position: 5
item_ids:
  - mio_icif:generator/block_diesel_generator
---

# ディーゼル発電機

<Row>
  <BlockImage id="mio_icif:generator/block_diesel_generator" scale="3" />
</Row>

## 機能

機械は軽油を燃やしてEUを作る。
機械は軽油バケツと軽油セルだけを受け入れる。
機械は電力網に追従するエンジンだ：取られた分だけ発電し、蓄電が 90% 未満なら少し多めに補う。蓄電が満杯（98%）で停止し、60% 未満で再始動するので、満杯の間は燃料を使わない。
1 EU あたりの燃料は負荷 80% 付近で最も少なく（全負荷より 5% 少ない）、低負荷ほど増える。冷えたエンジンの効率は 80% で、冷却水が約 80 °C になると全効率になる。
画面は機関室の監視盤：気筒ごとの排気温度、回転計、冷却水、燃料タンク、負荷と排気温度の記録。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | EV (2,048 EU) |
| 出力 | 120 EU/t |
| 蓄電容量 | 1,000,000 EU |
| 燃料：ディーゼル燃料 | 0.75 mB/t → 120 EU/t (160 EU/mB) |

この値は本バージョンのブロックエンティティから計測した。

発電倍率（設定 GeneratorOutput）が 1.0 以外のとき、EU の値が変わる。燃料消費は変わらない。

## 使用手順

1. HVケーブルを発電機に接続する。
2. 軽油バケツを入力スロットに入れる。
3. 出力スロットから空の容器を取り出す。
4. 充電するバッテリーをバッテリースロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_diesel_generator" fallbackText="-" />
