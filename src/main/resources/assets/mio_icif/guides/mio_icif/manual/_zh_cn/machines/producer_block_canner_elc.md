---
navigation:
  title: "装罐机"
  icon: mio_icif:producer/block_canner_elc
  parent: machines.md
  position: 15
item_ids:
  - mio_icif:producer/block_canner_elc
---

# 装罐机

<Row>
  <BlockImage id="mio_icif:producer/block_canner_elc" scale="3" />
</Row>

## 功能

装罐机消耗EU，有4种模式。装罐模式把物品装入容器，例如把食物装入锡罐。
其他模式把单元中的流体倒入储罐、用储罐流体装满单元，或混合流体与固体。
混合模式用蒸馏水和青金石粉制作冷却液。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 800 EU |
| 工作耗电 | 4 EU/t |
| 单次工作时间 | 200 tick (10 s) |
| 单次耗电 | 800 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将LV导线连接到机器。
2. 在界面中选择模式。
3. 把容器和材料放入输入槽位，或泵入流体。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_canner_elc" fallbackText="-" />
