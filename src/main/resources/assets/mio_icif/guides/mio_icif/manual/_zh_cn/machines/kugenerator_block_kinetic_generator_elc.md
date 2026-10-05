---
navigation:
  title: "电力动能发生机"
  icon: mio_icif:kugenerator/block_kinetic_generator_elc
  parent: generators.md
  position: 8
item_ids:
  - mio_icif:kugenerator/block_kinetic_generator_elc
---

# 电力动能发生机

<Row>
  <BlockImage id="mio_icif:kugenerator/block_kinetic_generator_elc" scale="3" />
</Row>

## 功能

机器把 EU 转换为动能（KU）。
马达槽位中的每个电动马达都使 KU 输出增加。
机器只从正面输出 KU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 1,000 EU/t |
| 储能 | 40,000 EU |
| 工作耗电 | 100 EU/t |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置机器，使其正面对着动能机器。
2. 把电动马达放入马达槽位。
3. 把导线连接到机器来供应 EU。
4. 把已充电的电池放入电池槽位作为备用电源。

## 注意

- 没有电动马达时，机器不产生 KU。
- 涡轮增压发电机转换本机器的 KU 时效率较低。

## 配方

<RecipesFor id="mio_icif:kugenerator/block_kinetic_generator_elc" fallbackText="-" />
