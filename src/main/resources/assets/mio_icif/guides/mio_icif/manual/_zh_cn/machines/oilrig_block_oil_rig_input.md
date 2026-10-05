---
navigation:
  title: "石油钻机输入模块"
  icon: mio_icif:oilrig/block_oil_rig_input
  parent: heavy.md
  position: 7
item_ids:
  - mio_icif:oilrig/block_oil_rig_input
---

# 石油钻机输入模块

<Row>
  <BlockImage id="mio_icif:oilrig/block_oil_rig_input" scale="3" />
</Row>

## 功能

石油钻机输入模块从导线接收EU，并为钻机储存。核心从所有输入模块取用EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 50,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把输入模块放在钻机的模块位置。
2. 把HV导线连接到模块。
3. 增加输入模块，储存更多EU。

## 注意

- 钻机至少需要一个输入模块。

## 配方

<RecipesFor id="mio_icif:oilrig/block_oil_rig_input" fallbackText="-" />
