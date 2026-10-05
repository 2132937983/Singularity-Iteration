---
navigation:
  title: "洗矿机"
  icon: mio_icif:producer/block_washer_elc
  parent: machines.md
  position: 49
item_ids:
  - mio_icif:producer/block_washer_elc
---

# 洗矿机

<Row>
  <BlockImage id="mio_icif:producer/block_washer_elc" scale="3" />
</Row>

## 功能

洗矿机消耗EU和水，把粉碎矿石洗成纯净的粉碎矿石和副产物。
每次加工消耗内部储罐中的水。机器有3个输出槽位。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 8,000 EU |
| 工作耗电 | 16 EU/t |
| 单次工作时间 | 400 tick (20 s) |
| 单次耗电 | 6,400 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 把水桶或水单元放入水槽位，或连接输水管道。
3. 把粉碎矿石放入输入槽位。
4. 从3个输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_washer_elc" fallbackText="-" />
