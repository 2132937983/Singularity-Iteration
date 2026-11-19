---
navigation:
  title: "核融合電力出力ポート"
  icon: mio_icif:fusion/block_fusion_power_port
  parent: heavy.md
  position: 41
item_ids:
  - mio_icif:fusion/block_fusion_power_port
---

# 核融合電力出力ポート

<Row><BlockImage id="mio_icif:fusion/block_fusion_power_port" scale="3" /></Row>

## 機能

[核融合炉](fusion_block_fusion_controller.md)の出力を電力網に送る。核融合炉のポート位置に置くと、構造が完成している間コントローラーのために働く。電力出力ポートが複数あると、出力はそれらに均等に分けられる。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧Tier | 分担分を64パケットで送れる最低のTier：524,288 EU/tまでIV (8,192 EU)、その上はLuV、ZPMV、UV、UHV (2,097,152 EU) |
| 1 tickのパケット数 | 最大64 |
| 蓄電容量 | 39,200,000 EU |

## 注意

- ポート1基で最大の39,200,000 EU/tを送るとUHVパケットになり、超伝導ケーブルとそのTierの機械しか受けられない。2基か4基ならUV、8基ならZPMVになる。
- 構造が未完成でも、ポートはたまっている電力を送り出す。

## レシピ

<RecipesFor id="mio_icif:fusion/block_fusion_power_port" fallbackText="-" />
