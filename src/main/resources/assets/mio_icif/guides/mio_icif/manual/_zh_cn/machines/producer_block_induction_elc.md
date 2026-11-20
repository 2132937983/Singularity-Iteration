---
navigation:
  title: "感应炉"
  icon: mio_icif:producer/block_induction_elc
  parent: machines.md
  position: 31
item_ids:
  - mio_icif:upgrade/thermal_retention_upgrade
  - mio_icif:producer/block_induction_elc
---

# 感应炉

<Row>
  <BlockImage id="mio_icif:producer/block_induction_elc" scale="3" />
</Row>

## 功能

感应炉消耗EU，按熔炉配方同时冶炼两个物品。

**原理：** 感应线圈加热坩埚里的熔池。加热功率最高 64 EU/t，从室温到 1,600 °C 约 30 秒。熔池按温度散热，满温时散热 8 EU/t；到温后加热器只补散热。不加热时熔池按时间常数约 4 分钟降温。

**冶炼速度：** 600 °C 开始冶炼，温度越高越快，1,600 °C 满速：两个格子每 12 tick 各炼一件，冶炼另耗 15 EU/t。超频加快冶炼，不加快升温。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 10,000 EU |
| 加热功率 | 最高 64 EU/t |
| 冶炼耗电 | 15 EU/t |
| 满温速度 | 每格每 12 tick 一件 |
| 开始冶炼 / 满速温度 | 600 °C / 1,600 °C |
| 满温散热 | 8 EU/t（装保温单元 2 EU/t） |

表中的工时和耗电是没有超频时的值。每个超频升级让工时变为 0.7 倍、耗电变为 1.6 倍。界面和 JEI 显示当前输入的实际数值。

## 使用步骤

1. 将MV导线连接到机器。
2. 把要冶炼的物品放入2个输入槽位。
3. 让机器持续工作，以保持热量。
4. 从2个输出槽位取出产物。

## 注意

- 红石信号使机器保持高温，并暂停加工。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 保温

界面里的「保温」按钮打开后，没有物品可冶炼时也会把熔池加热到 1,600 °C 并保持，下一件立即以最高速度冶炼。保温时加热器只补散热，满温约 8 EU/t。
在升级槽中放入 <ItemLink id="mio_icif:upgrade/thermal_retention_upgrade" /> 后，散热变为四分之一，保温约 2 EU/t。

## 配方

<RecipesFor id="mio_icif:producer/block_induction_elc" fallbackText="-" />
