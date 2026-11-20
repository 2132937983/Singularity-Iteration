---
navigation:
  title: "进阶高压压缩机"
  icon: mio_icif:producer/block_compressor_advanced_elc
  parent: machines.md
  position: 2
item_ids:
  - mio_icif:producer/block_compressor_advanced_elc
---

# 进阶高压压缩机

<Row>
  <BlockImage id="mio_icif:producer/block_compressor_advanced_elc" scale="3" />
</Row>

## 功能

进阶高压压缩机在MV下使用压缩机配方。

**原理：** 机器带一个液压蓄能器。油泵从电网给蓄能器充压，满压 32 MPa 约存 20,000 EU。冲压时蓄能器放压，所以冲压的功率可以远高于电网的输入。

**冲压：** 每次做和压缩机同样的功，但只用配方工时的四分之一，功率是配方的 4 倍。例：致密板 100 tick × 80 EU/t，工业钻石 150 tick × 120 EU/t。没写数值的配方 75 tick × 8 EU/t。

**充压门槛：** 蓄能器存够整次冲压的能量（油压达到这次冲压所需）才开始，开始后做完。不够时状态显示「蓄能器充压中」。供电够强时它是压缩机的 4 倍速；供电弱时，按油泵补压的速度运行。

界面画出液压机和蓄能器油瓶，油位是当前油压，红线是下一次冲压所需的油压。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能（蓄能器） | 20,000 EU（满压 32 MPa） |
| 冲压功率 | 配方的 4 倍（默认 8 EU/t） |
| 单次工作时间 | 配方的 1/4（默认 75 tick） |
| 单次耗电 | 与压缩机相同（默认 600 EU） |

表中的工时和耗电是没有超频时的值。每个超频升级让工时变为 0.7 倍、耗电变为 1.6 倍。界面和 JEI 显示当前输入的实际数值。

## 使用步骤

1. 将MV导线连接到机器。
2. 把要压缩的物品放入输入槽位。
3. 安装超频升级以加快加工。
4. 从输出槽位取出产物。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_compressor_advanced_elc" fallbackText="-" />
