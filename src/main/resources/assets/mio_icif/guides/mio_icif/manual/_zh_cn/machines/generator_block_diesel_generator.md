---
navigation:
  title: "柴油发电机"
  icon: mio_icif:generator/block_diesel_generator
  parent: generators.md
  position: 5
item_ids:
  - mio_icif:generator/block_diesel_generator
---

# 柴油发电机

<Row>
  <BlockImage id="mio_icif:generator/block_diesel_generator" scale="3" />
</Row>

## 功能

机器燃烧柴油并产生 EU。
机器接受柴油桶和柴油单元，也可以从任意一面用管道或流体弹出升级输入柴油。
机器是一台跟随电网的发动机：电网取多少就发多少，储电低于 90% 时再多补一些。储电满（98%）时停机，低于 60% 再启动，所以储电满时不耗燃料。
约 80% 负荷时每 EU 最省油（比满负荷再省 5%），负荷越低越费油。冷机只有 80% 的效率，水温到约 80 °C 才达到全效率。
界面是机舱监控台：每个气缸的排气温度、转速表、水温、油箱，以及负荷与排温的记录。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | EV (2,048 EU) |
| 输出 | 120 EU/t |
| 储能 | 1,000,000 EU |
| 燃料：柴油 | 0.5 mB/t → 120 EU/t (240 EU/mB) |

这些数值由本版本的方块实体实测得出。

发电倍率（配置 GeneratorOutput）不为 1.0 时，EU 数值会改变。燃料消耗不变。

## 使用步骤

1. 把 HV 导线连接到发电机。
2. 把柴油桶放入输入槽位。
3. 从输出槽位取出空容器。
4. 把电池放入电池槽位来充电。

## 配方

<RecipesFor id="mio_icif:generator/block_diesel_generator" fallbackText="-" />
