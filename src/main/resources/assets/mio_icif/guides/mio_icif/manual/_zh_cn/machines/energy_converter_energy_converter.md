---
navigation:
  title: "能源转换器"
  icon: mio_icif:energy_converter/energy_converter
  parent: machines.md
  position: 24
item_ids:
  - mio_icif:energy_converter/energy_converter
---

# 能源转换器

<Row>
  <BlockImage id="mio_icif:energy_converter/energy_converter" scale="3" />
</Row>

## 功能

能源转换器在EU和FE之间转换能量。比例为1 EU = 4 FE。
EU转FE模式下，机器接收EU，并从正面输出FE。
FE转EU模式下，机器从其他5个面获取FE，并以HV向导线网络输出EU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 2,048 EU/t |
| 储能 | 100,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 放置转换器，使正面朝向目标。
2. 潜行并右键转换器，切换模式。
3. 将能量源连接到其他面。

## 注意


> **警告：** 电压高于HV时，机器因过压爆炸。


## 配方

<RecipesFor id="mio_icif:energy_converter/energy_converter" fallbackText="-" />
