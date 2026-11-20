---
navigation:
  title: "光子共振ソーラー発電機"
  icon: mio_icif:generator/block_photon_resonance_solar_generator
  parent: generators.md
  position: 22
item_ids:
  - mio_icif:generator/block_photon_resonance_solar_generator
---

# 光子共振ソーラー発電機

<Row>
  <BlockImage id="mio_icif:generator/block_photon_resonance_solar_generator" scale="3" />
</Row>

## 機能

機械は光子共振パネルである：表面のナノ共振構造が広いスペクトルを吸収し、散乱光も使える。弱い光でも定格出力に達する（450 W/m²）。
遮られても空の光の 45% が届く。雨で約 85%、雷雨で約 61%。
温度の影響は小さい：25 °C を超えると 1 °C ごとに −0.2%、最大 15%。
夜、真上が空に開けていれば、機械は自身の熱を冷たい晴れた空へ放射し、その流れから発電する：定格の 25%（96 EU/t）。雨で半分、雷雨で 4 分の 1。
画面には本日の記録（パネルに当たる日射、快晴時の比較、出力）と夜空係数が表示される。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | HV (512 EU) |
| 出力 | 384 EU/t |
| 夜間出力 | 96 EU/t |
| 定格出力の日射 | 450 W/m² |
| 蓄電容量 | 40,000,000 EU |

この値は本バージョンのブロックエンティティから計測した。

## 使用手順

1. 発電機を屋外に置く。雨の多い場所や遮られた場所でも使える。
2. HVケーブルを発電機に接続する。
3. 充電スロットにバッテリーを入れる。

## レシピ

<RecipesFor id="mio_icif:generator/block_photon_resonance_solar_generator" fallbackText="-" />
