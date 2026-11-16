---
navigation:
  title: "ソーラー発電機"
  icon: mio_icif:generator/block_solar_generator
  parent: generators.md
  position: 28
item_ids:
  - mio_icif:generator/block_solar_generator
---

# ソーラー発電機

<Row>
  <BlockImage id="mio_icif:generator/block_solar_generator" scale="3" />
</Row>

## 機能

機械は当たる日射でEUを作る。朝夕は日射が弱く、正午に最も強い。700 W/m² で定格出力に達し、それ以上でも定格のまま。
雨では約 70%、雷雨では約 37% の日射が残る。パネルが熱いほど出力は下がる（25 °C を超えると 1 °C ごとに −0.4%）ので、砂漠は日射が強いが暑い。
真上が空に開けている必要がある。遮られると散乱光のみ、暗い場所では発電しない。
画面には本日の記録（パネルに当たる日射、快晴時の比較、出力）が表示される。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | LV (32 EU) |
| 出力 | 1 EU/t |
| 蓄電容量 | 2 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 空のあるディメンション（例：オーバーワールド）に発電機を置く。
2. 発電機の上にある不透明なブロックをすべて取り除く。
3. LVケーブルを発電機に接続する。
4. 充電するバッテリーをバッテリースロットに入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_solar_generator" fallbackText="-" />
