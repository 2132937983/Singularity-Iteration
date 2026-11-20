---
navigation:
  title: "进阶金属成型机"
  icon: mio_icif:producer/block_metal_former_advanced
  parent: machines.md
  position: 5
item_ids:
  - mio_icif:producer/block_metal_former_advanced
---

# 进阶金属成型机

<Row>
  <BlockImage id="mio_icif:producer/block_metal_former_advanced" scale="3" />
</Row>

## 功能

进阶金属成型机在MV下使用金属成型机配方。
辊压模式制作板和外壳。切割模式用板制作导线。挤压模式制作导线、轴和罐。
方块外观显示当前模式。

工时是配方工时的五分之一（向上取整），功率固定 50 EU/t，所以每次耗电和金属成型机相同、速度是它的 5 倍。例：铁板卷成铁外壳 60 tick。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 1,000 EU |
| 工作耗电 | 50 EU/t |
| 单次工作时间 | 配方工时的 1/5（默认 40 tick） |
| 单次耗电 | 与金属成型机相同（默认 2,000 EU） |

表中的工时和耗电是没有超频时的值。每个超频升级让工时变为 0.7 倍、耗电变为 1.6 倍。界面和 JEI 显示当前输入的实际数值。

## 使用步骤

1. 将MV导线连接到机器。
2. 点击界面中的模式按钮，选择辊压、切割或挤压。
3. 把锭或板放入输入槽位。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_metal_former_advanced" fallbackText="-" />
