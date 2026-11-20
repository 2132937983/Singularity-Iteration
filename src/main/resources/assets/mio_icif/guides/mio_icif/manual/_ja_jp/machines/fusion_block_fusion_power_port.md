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
| 出力電圧Tier | 分担分を64パケットで送れる最低のTier：524,288 EU/tまでIV (8,192 EU)、その上はLuV、ZPMV、UV、UHV (2,097,152 EU)、UEV (8,388,608 EU)、UIV (33,554,432 EU) |
| 1 tickのパケット数 | 最大64 |
| 蓄電容量 | 2,147,483,647 EU |

## 注意

- ポート1基で39,200,000 EU/tを送るとUHVパケットになり、超伝導ケーブルとそのTierの機械しか受けられない。2基か4基ならUV、8基ならZPMVになる。
- 核融合炉の出力に上限はない：ポート1基の分担が134,217,728 EU/tを超えるとUEV、536,870,912 EU/tを超えるとUIVパケットになる。ポートが多いほど1基あたりの電圧は下がる。
- 構造が未完成でも、ポートはたまっている電力を送り出す。

## レシピ

<RecipesFor id="mio_icif:fusion/block_fusion_power_port" fallbackText="-" />
