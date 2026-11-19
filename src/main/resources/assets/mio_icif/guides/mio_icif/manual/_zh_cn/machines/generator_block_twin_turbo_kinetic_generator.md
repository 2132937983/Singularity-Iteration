---
navigation:
  title: "双涡轮增压动能发电机"
  icon: mio_icif:generator/block_twin_turbo_kinetic_generator
  parent: generators.md
  position: 34
item_ids:
  - mio_icif:generator/block_twin_turbo_kinetic_generator
---

# 双涡轮增压动能发电机

<Row>
  <BlockImage id="mio_icif:generator/block_twin_turbo_kinetic_generator" scale="3" />
</Row>

## 功能

机器把正面方块提供的动能（KU）转换为 EU。
来自风力或水力动能发生机的 KU 按全效率转换。
来自其他来源的 KU 按较低效率转换，但仍高于基础动能发电机。
界面是电厂模拟屏：正面的动能源、按其转速转动的传动轴、发电机与 EU 输出，并显示输入负荷和每 KU 的 EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | EV (2,048 EU)；产出超过 2,048 EU/t 时为 IV (8,192 EU) |
| 产出 | 风力或水力动能发生机每 KU 1.2 EU，其他来源每 KU 0.45 EU |
| 动能输入 | 最多 4,000 KU/t (最多 4,800 EU/t) |
| 储能 | 400,000 EU |

这些数值由本版本的方块实体实测得出。

发电倍率（配置 GeneratorOutput）不为 1.0 时，EU 数值会改变。

## 使用步骤

1. 放置发电机，使其正面对着风力动能发生机背面。
2. 把 IV 导线连接到发电机（产出不超过 2,048 EU/t 时 EV 导线也可以）。

## 注意

- 机器的 KU 转 EU 比率高于涡轮增压动能发电机。
- 电力包的大小随动能输入变化：风力或水力动能超过 1,706 KU/t（产出超过 2,048 EU/t）时变为 IV 包，EV 导线会熔毁，EV 机器会爆炸。其他来源最多 4,000 × 0.45 = 1,800 EU/t，始终是 EV 包。

## 配方

<RecipesFor id="mio_icif:generator/block_twin_turbo_kinetic_generator" fallbackText="-" />
