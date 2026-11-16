---
navigation:
  title: "进阶半流质发电机"
  icon: mio_icif:generator/block_advanced_semifluid_generator
  parent: generators.md
  position: 2
item_ids:
  - mio_icif:generator/block_advanced_semifluid_generator
---

# 进阶半流质发电机

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_semifluid_generator" scale="3" />
</Row>

## 功能

机器燃烧半流质燃料（沼气、生物质、原油、柴油）并产生 EU。
每种燃料每 mB 的发电量是半流质发电机的 1.25 倍。
机器接受这些燃料的桶、单元和其他容器。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | MV (128 EU) |
| 输出 | 36 EU/t |
| 储能 | 100,000 EU |
| 燃料：沼气 | 0.9 mB/t → 36 EU/t (40 EU/mB) |
| 燃料：生物质 | 3.6 mB/t → 36 EU/t (10 EU/mB) |
| 燃料：原油 | 1.8 mB/t → 36 EU/t (20 EU/mB) |
| 燃料：柴油 | 0.225 mB/t → 36 EU/t (160 EU/mB) |

这些数值由本版本的方块实体实测得出。

发电倍率（配置 GeneratorOutput）不为 1.0 时，EU 数值会改变。燃料消耗不变。

## 使用步骤

1. 把 MV 导线连接到发电机。
2. 把燃料桶或燃料单元放入输入槽位。
3. 从输出槽位取出空容器。
4. 把电池放入电池槽位来充电。

## 配方

<RecipesFor id="mio_icif:generator/block_advanced_semifluid_generator" fallbackText="-" />
