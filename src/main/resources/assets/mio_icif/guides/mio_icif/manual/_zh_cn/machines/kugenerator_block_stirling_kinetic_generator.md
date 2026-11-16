---
navigation:
  title: "斯特林动能发生机"
  icon: mio_icif:kugenerator/block_stirling_kinetic_generator
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:kugenerator/block_stirling_kinetic_generator
---

# 斯特林动能发生机

<Row>
  <BlockImage id="mio_icif:kugenerator/block_stirling_kinetic_generator" scale="3" />
</Row>

## 功能

机器把热能（HU）和水转换为动能（KU）和热水。
机器从正面以外的所有面接收热能。
机器只从正面输出 KU。
水就是冷却水：冷却端保持在约 15 °C；下表的比例在设计工况下成立（加热头 650 °C，额定热流 500 HU/t），轻载或冷机时每 HU 出得少（见斯特林发电机）。
KU 进入一个飞轮（20,000 KU）。转得越快每 tick 给出越多（满时最多 2,000 KU/t），所以转轴会加速，直到给出的量等于发动机的产出；负荷下降时转速随之下降。KU 不会损失；飞轮满（超速）时发动机不再吸热。
界面是电厂模拟屏：热量输入、水冷发动机、水罐与热水罐、带转速的飞轮，以及正面输出的 KU。

## 电力数据

| 项目 | 数值 |
|---|---|
| 转换 | 4 HU + 1 mB 水 → 12 KU + 1 mB 热水 |
| 飞轮 | 20,000 KU，满时每 tick 最多给出 2,000 KU |
| 热量缓冲 | 1,000 HU |

该方块不使用EU。

## 使用步骤

1. 放置机器，使其正面对着动能机器。
2. 把加热机放在机器的其他面上。
3. 把水桶或水单元放入水输入槽位。
4. 把空容器放入热水槽位来收集热水。

## 注意

- 水储罐为空或热水储罐已满时，机器停止工作。

## 配方

<RecipesFor id="mio_icif:kugenerator/block_stirling_kinetic_generator" fallbackText="-" />
