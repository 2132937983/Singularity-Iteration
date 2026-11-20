---
navigation:
  title: "聚变能量输出端口"
  icon: mio_icif:fusion/block_fusion_power_port
  parent: heavy.md
  position: 41
item_ids:
  - mio_icif:fusion/block_fusion_power_port
---

# 聚变能量输出端口

<Row><BlockImage id="mio_icif:fusion/block_fusion_power_port" scale="3" /></Row>

## 功能

把[聚变堆](fusion_block_fusion_controller.md)的输出送入电网。放在聚变堆的端口位置；结构成型时为控制台服务。有多个能量输出端口时，输出在它们之间平分。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | 64 个包能送完分到的量的最低等级：IV (8,192 EU) 至 524,288 EU/t，再往上依次为 LuV、ZPMV、UV、UHV (2,097,152 EU)、UEV (8,388,608 EU)、UIV (33,554,432 EU) |
| 每 tick 包数 | 最多 64 |
| 储能 | 2,147,483,647 EU |

## 注意

- 只装一个端口时，39,200,000 EU/t 发 UHV 包，只有超导合金导线和该等级的机器接得住。装 2 或 4 个端口时发 UV 包，8 个时发 ZPMV 包。
- 聚变堆输出不封顶：一个端口分到 134,217,728 EU/t 以上时发 UEV 包，536,870,912 EU/t 以上时发 UIV 包。端口越多，每个端口的电压越低。
- 结构未成型时，端口仍会把已存的电量送出去。

## 配方

<RecipesFor id="mio_icif:fusion/block_fusion_power_port" fallbackText="-" />
