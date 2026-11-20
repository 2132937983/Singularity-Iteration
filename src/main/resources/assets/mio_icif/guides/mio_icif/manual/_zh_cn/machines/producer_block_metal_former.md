---
navigation:
  title: "金属成型机"
  icon: mio_icif:producer/block_metal_former
  parent: machines.md
  position: 43
item_ids:
  - mio_icif:producer/block_metal_former
---

# 金属成型机

<Row>
  <BlockImage id="mio_icif:producer/block_metal_former" scale="3" />
</Row>

## 功能

金属成型机消耗EU，以3种模式加工金属。
辊压模式制作板和外壳。切割模式用板制作导线。挤压模式制作导线、轴和罐。

每条配方按自己写的工时运行（例如铁板卷成铁外壳 300 tick），功率是配方写的值，没写时 10 EU/t。界面按当前模式画出辊压、切割或挤压的示意图，模式按钮在界面里。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 32 EU/t |
| 储能 | 2,000 EU |
| 工作耗电 | 按配方（默认 10 EU/t） |
| 单次工作时间 | 按配方（默认 200 tick） |
| 单次耗电 | 按配方（默认 2,000 EU） |

表中的工时和耗电是没有超频时的值。每个超频升级让工时变为 0.7 倍、耗电变为 1.6 倍。界面和 JEI 显示当前输入的实际数值。

## 使用步骤

1. 将LV导线连接到机器。
2. 点击界面中的模式按钮，选择辊压、切割或挤压。
3. 把锭或板放入输入槽位。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于LV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_metal_former" fallbackText="-" />
