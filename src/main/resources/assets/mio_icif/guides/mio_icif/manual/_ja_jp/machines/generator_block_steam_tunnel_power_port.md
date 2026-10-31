---
navigation:
  title: "スチームトンネル電力ポート"
  icon: mio_icif:generator/block_steam_tunnel_power_port
  parent: generators.md
  position: 33
item_ids:
  - mio_icif:generator/block_steam_tunnel_power_port
---

# スチームトンネル電力ポート

<Row>
  <BlockImage id="mio_icif:generator/block_steam_tunnel_power_port" scale="3" />
</Row>

## 機能

[スチームトンネル](generator_block_steam_tunnel.md)の EU を送り出す。トンネルの長い側面の中央か後端の中心に置く。

## 電力データ

| 項目 | 値 |
|---|---|
| 出力電圧ティア | HV（512 EU） |
| 出力 | 最大 2,048 EU/t（1 tick に 4 パケット） |
| 蓄電量 | 4,096 EU |

- トンネルが完成しているときだけ EU を送る。電力ポートが複数あれば出力を分け合う。

## レシピ

<RecipesFor id="mio_icif:generator/block_steam_tunnel_power_port" fallbackText="-" />
