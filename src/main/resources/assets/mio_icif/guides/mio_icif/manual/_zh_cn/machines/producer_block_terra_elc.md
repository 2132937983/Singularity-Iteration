---
navigation:
  title: "地形转换机"
  icon: mio_icif:producer/block_terra_elc
  parent: machines.md
  position: 65
item_ids:
  - mio_icif:producer/block_terra_elc
---

# 地形转换机

<Row>
  <BlockImage id="mio_icif:producer/block_terra_elc" scale="3" />
</Row>

## 功能

地形转换机消耗EU，按地形转换模板改变机器周围的地形。
模板包括耕作、沙漠、灌溉、冰冻、平整和蘑菇。
机器没有界面。默认半径为128格。服务器配置决定半径。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 100,000 EU |
| 工作耗电 | 100 EU/t |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把地形转换机放在要改变的区域中央。
2. 将HV导线连接到机器。
3. 手持模板右键机器，插入模板。
4. 潜行并右键机器，取出模板。

## 注意


> **警告：** 地形变化是永久的。先在没有建筑的区域测试模板。

- 红石信号使机器停止。

> **警告：** 电压高于HV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:producer/block_terra_elc" fallbackText="-" />
