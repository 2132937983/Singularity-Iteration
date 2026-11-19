---
navigation:
  title: "动能发电机"
  icon: mio_icif:generator/block_kinetic_generator
  parent: generators.md
  position: 18
item_ids:
  - mio_icif:generator/block_kinetic_generator
---

# 动能发电机

<Row>
  <BlockImage id="mio_icif:generator/block_kinetic_generator" scale="3" />
</Row>

## 功能

机器把动能（KU）转换为 EU。
机器从正面相邻的方块接收 KU。
界面是电厂模拟屏：正面的动能源、按其转速转动的传动轴、发电机与 EU 输出，并显示输入负荷和每 KU 的 EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | MV (128 EU)；产出超过 128 EU/t 时为 HV (512 EU) |
| 产出 | 每 4 KU 产 1 EU，最多 250 EU/t |
| 动能输入 | 最多 1,000 KU/t |
| 储能 | 100,000 EU |

这些数值由本版本的方块实体实测得出。

发电倍率（配置 GeneratorOutput）不为 1.0 时，EU 数值会改变。

## 使用步骤

1. 放置发电机，使其正面对着风力动能发生机背面。
2. 把 HV 导线连接到发电机（产出不超过 128 EU/t 时 MV 导线也可以）。

## 注意

- 电力包的大小随动能输入变化：输入超过 512 KU/t（产出超过 128 EU/t）时变为 HV 包，MV 导线会熔毁，MV 机器会爆炸。

## 配方

<RecipesFor id="mio_icif:generator/block_kinetic_generator" fallbackText="-" />
