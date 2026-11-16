---
navigation:
  title: "地热发电机"
  icon: mio_icif:generator/block_geo_generator
  parent: generators.md
  position: 14
item_ids:
  - mio_icif:generator/block_geo_generator
---

# 地热发电机

<Row>
  <BlockImage id="mio_icif:generator/block_geo_generator" scale="3" />
</Row>

## 功能

机器把岩浆转换为 EU。
机器把岩浆桶和岩浆单元中的岩浆移入内部储罐。
机器周围 2 格内的岩浆增加发电量：紧贴一面的岩浆 1 EU/t，2 格内其他位置 0.5 EU/t，流动的岩浆减半。机器越深越多：Y 64 以上 ×1，到 Y −64 为 ×2；下界 ×1.5。周围岩浆合计最多 12 EU/t。
界面是记录仪：带深度系数的钻孔柱、周围岩浆的合计，以及岩浆罐。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | LV (32 EU) |
| 输出 | 20 EU/t |
| 储能 | 2,400 EU |
| 燃料：熔岩 | 2 mB/t → 20 EU/t (10 EU/mB) |
| 周围岩浆（2 格内） | 紧贴 1 EU/t，更远 0.5 EU/t，× 深度，最多 12 EU/t |

这些数值由本版本的方块实体实测得出。

发电倍率（配置 GeneratorOutput）不为 1.0 时，EU 数值会改变。燃料消耗不变。

## 使用步骤

1. 把 LV 导线连接到发电机。
2. 把岩浆桶放入输入槽位。
3. 从输出槽位取出空桶。
4. 把电池放入电池槽位来充电。

## 注意

- 输出槽位已满时，机器不会倒空桶。

## 配方

<RecipesFor id="mio_icif:generator/block_geo_generator" fallbackText="-" />
