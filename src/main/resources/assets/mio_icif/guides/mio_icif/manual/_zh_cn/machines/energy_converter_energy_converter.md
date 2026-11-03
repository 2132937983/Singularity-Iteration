---
navigation:
  title: "并网接口"
  icon: mio_icif:energy_converter/energy_converter
  parent: machines.md
  position: 24
item_ids:
  - mio_icif:energy_converter/energy_converter
---

# 并网接口

> **0.1.11.1。** 能源转换器现为并网接口（同一方块）。已放置的转换器保留朝向与模式（输出或输入），其 FE 缓存并入 EU 缓存。

<Row>
  <BlockImage id="mio_icif:energy_converter/energy_converter" scale="3" />
</Row>

## 功能

并网接口将 SI 电网连接到其他模组的 FE 电网。正面连接其他电网（FE），其余 5 个面连接 SI 电网（EU）。比例为 4 FE = 1 EU，无损耗。
SI 导线不接收 FE，也不与其他模组的导线连接。并网接口是两个电网之间的连接点。

- 输出：接口获取负载用剩的 SI 电能，从正面以 FE 输出。
- 输入：接口从正面获取 FE，作为发电机向 SI 电网供电。
- 双向：接口使缓存保持在半满附近。高于死区时从正面输出 FE，低于死区时从正面获取 FE。在 SI 一侧只接收发电机的剩余电能、只向负载供电，因此电能不会经由 SI 储能来回往返。
- 上限（32、128、512 或 2,048 EU/t）对每个方向生效：SI 输入与 FE 输入共用每 tick 一个上限，SI 输出与 FE 输出共用每 tick 一个上限。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | EV (2,048 EU) |
| 最大输入与输出 | 32、128、512 或 2,048 EU/t（上限） |
| 储能 | 200,000 EU |
| 死区（双向） | 缓存的 10、25 或 50 %，以半满为中心 |

## 使用步骤

1. 放置接口，使正面贴住其他模组的导线或方块。
2. 将 SI 导线连接到其他面。
3. 潜行并右键接口，选择模式：输出、输入、双向 10 %、25 % 或 50 %。
4. 右键接口，选择上限。

## 注意


> **警告：** 电压高于EV时，机器因过压爆炸。

- 正面不连接 SI 导线或 SI 机器。
- `/mio_icif perf` 显示并网接口每 tick 传输的 FE。


## 配方

<RecipesFor id="mio_icif:energy_converter/energy_converter" fallbackText="-" />
