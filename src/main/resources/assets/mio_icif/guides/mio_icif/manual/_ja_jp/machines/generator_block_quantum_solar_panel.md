---
navigation:
  title: "量子ソーラーパネル"
  icon: mio_icif:generator/block_quantum_solar_panel
  parent: generators.md
  position: 24
item_ids:
  - mio_icif:generator/block_quantum_solar_panel
---

# 量子ソーラーパネル

<Row>
  <BlockImage id="mio_icif:generator/block_quantum_solar_panel" scale="3" />
</Row>

## 機能

機械は集光パネルである：レンズで直達日射をセルに集めてEUを作る。昼の出力が最も高いが、定格出力は正午前後だけ（850 W/m²）。
機械は直達日射だけを使う：真上が空に開けている必要があり、遮られると発電しない。雨で約 37%、雷雨ではほぼゼロ。砂漠など雨の降らない場所は天候の影響を受けない。
冷却がセルを 25 °C に保つので、温度は出力に影響しない。
夜は発電しない。夜間の電力には蓄電ブロックか光子共振ソーラー発電機を使う。
画面には本日の記録（パネルに当たる日射、快晴時の比較、出力）と、直達の割合、セル温度、冷却が表示される。
機械は4つのスロットでアイテムを充電する。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | IV (8,192 EU) |
| 出力 | 6,144 EU/t |
| 夜間出力 | 0 EU/t |
| 定格出力の日射 | 850 W/m² |
| 蓄電容量 | 10,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. パネルを直射日光の当たる屋外に置く（例：砂漠）。
2. パネルの上のブロックをすべて取り除く。
3. IVケーブルをパネルに接続する。
4. 最大4個のバッテリーを充電スロットに入れる。
5. 夜間の電力には、ネットワークに蓄電ブロックを接続する。

## レシピ

<RecipesFor id="mio_icif:generator/block_quantum_solar_panel" fallbackText="-" />
