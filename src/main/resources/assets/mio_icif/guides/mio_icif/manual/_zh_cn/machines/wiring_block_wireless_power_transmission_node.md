---
navigation:
  title: "无线电力传输节点"
  icon: mio_icif:wiring/block_wireless_power_transmission_node
  parent: power.md
  position: 37
item_ids:
  - mio_icif:wiring/block_wireless_power_transmission_node
---

# 无线电力传输节点

<Row>
  <BlockImage id="mio_icif:wiring/block_wireless_power_transmission_node" scale="3" />
</Row>

## 功能

节点无需导线，把EU送到已加载区块中的目标方块，距离不限。
有目标的节点从相邻导线取电，并把EU送到目标。
无目标的节点向相邻机器和导线供电。电力包大小适配最弱的相邻方块。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LuV (32,768 EU) |
| 最大输入 | 32,768 EU/t |
| 储能 | 196,608 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 在需要EU的机器或导线旁放置接收节点。
2. 在发电机导线旁放置发送节点。
3. 潜行时用无线管理器右键点击接收节点，记录目标。
4. 用无线管理器右键点击发送节点，完成连接。

## 注意

- 目标也可以是机器或储电方块。
- 节点发送EU时发光。未加载区块中的目标得不到EU。

## 配方

<RecipesFor id="mio_icif:wiring/block_wireless_power_transmission_node" fallbackText="-" />
