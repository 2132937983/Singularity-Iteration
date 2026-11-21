---
navigation:
  title: "量子发电机"
  icon: mio_icif:generator/block_quantum_generator
  parent: generators.md
  position: 23
item_ids:
  - mio_icif:generator/block_quantum_generator
---

# 量子发电机

<Row>
  <BlockImage id="mio_icif:generator/block_quantum_generator" scale="3" />
</Row>

## 功能

机器不需要燃料或阳光即可产生 EU，不受昼夜、天气和温度影响。
机器是两条太阳能路线的终点：用量子太阳能发电机（聚光光热线的顶端）和终极光子振谐太阳能发电机（光子振谐线的顶端）合成。
机器向六个面上的方块输出 EU。

## 设定

本节是背景设定，不影响机器的数值。

量子发电机从真空的零点涨落中取能。
- 量子核心在机器内部的腔体中维持一个稳定的真空态。真空中的电磁场始终有零点涨落：虚光子对不断出现，又在极短时间内湮灭。
- 物质生成机的场约束结构在虚光子对湮灭之前把它们拉开，使它们成为真实的光子。
- 两块太阳能板不再接收外部的阳光，改为接收腔体内产生的光子：光子振谐板把光子的能量直接转为电荷分离，聚光板把剩余的光子聚到吸热体上再转为电能。
- 铱外壳屏蔽腔体，外界的扰动无法破坏真空态。所以机器的输出恒定，不受昼夜、天气和温度影响。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | IV (8,192 EU) |
| 输出 | 8,192 EU/t |
| 储能 | 100,000,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把发电机放在机器或导线旁边。
2. 打开界面查看输出和电压等级。

## 注意

- 界面只显示状态。界面没有控制按钮。

## 配方

<RecipesFor id="mio_icif:generator/block_quantum_generator" fallbackText="-" />
