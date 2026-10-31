---
navigation:
  title: "蒸汽隧道电力端口"
  icon: mio_icif:generator/block_steam_tunnel_power_port
  parent: generators.md
  position: 33
item_ids:
  - mio_icif:generator/block_steam_tunnel_power_port
---

# 蒸汽隧道电力端口

<Row>
  <BlockImage id="mio_icif:generator/block_steam_tunnel_power_port" scale="3" />
</Row>

## 功能

电力端口输出[蒸汽隧道涡轮机](generator_block_steam_tunnel.md)的 EU。放在隧道某个长侧面的中间或后端中心。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | HV（512 EU） |
| 输出 | 最多 2,048 EU/t（每 tick 4 个数据包） |
| 储能 | 4,096 EU |

- 只有隧道成型时才输出 EU。多个电力端口共享隧道的输出。

## 配方

<RecipesFor id="mio_icif:generator/block_steam_tunnel_power_port" fallbackText="-" />
