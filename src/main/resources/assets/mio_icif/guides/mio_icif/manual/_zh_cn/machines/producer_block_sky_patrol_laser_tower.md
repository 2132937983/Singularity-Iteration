---
navigation:
  title: "巡天镭射激光防御站"
  icon: mio_icif:producer/block_sky_patrol_laser_tower
  parent: machines.md
  position: 59
item_ids:
  - mio_icif:producer/block_sky_patrol_laser_tower
---

# 巡天镭射激光防御站

<Row>
  <BlockImage id="mio_icif:producer/block_sky_patrol_laser_tower" scale="3" />
</Row>

## 功能

巡天镭射激光防御站向视线内最多10个目标发射激光。每次命中消耗EU。
默认扫描范围为各方向32格。可在界面中扩大到64格。
黑名单模式攻击未列出的敌对生物。白名单模式只攻击列出的生物、标签和玩家。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 500,000 EU |
| 工作耗电 | 2,500 EU/t |
| 单次工作时间 | 100 tick (5 s) |
| 单次耗电 | 250,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 把防御塔放在视野开阔的位置。
2. 将HV导线连接到机器。
3. 打开界面，设置水平和垂直范围。
4. 选择目标模式，并向列表添加条目。

## 注意

- 升级舱接受超频、变压、储能和红石信号反转升级。
- 红石信号使防御塔关闭。

> **警告：** 电压高于HV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_sky_patrol_laser_tower" fallbackText="-" />
