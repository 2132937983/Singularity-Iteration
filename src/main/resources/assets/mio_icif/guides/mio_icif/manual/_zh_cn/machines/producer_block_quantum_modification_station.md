---
navigation:
  title: "通用套装改装台"
  icon: mio_icif:producer/block_quantum_modification_station
  parent: suit/index.md
  position: 0
item_ids:
  - mio_icif:producer/block_quantum_modification_station
---

# 通用套装改装台

<Row>
  <BlockImage id="mio_icif:producer/block_quantum_modification_station" scale="3" />
</Row>

## 功能

改装台为量子套装与纳米套装的部件安装升级单元，每次安装消耗 EU。
量子部件按个数计：头盔 4、胸甲 3、护腿 2、靴子 2。
纳米部件有纳米晶格，按晶格点数（LP）计：头盔 4、胸甲 6、护腿 4、靴子 4。
每个纳米单元占用 1 至 3 LP。纳米晶格致密剂为部件增加 2 LP，每件最多 2 次。
部分单元只适用于一种套装。单元的提示中会显示类别与 LP。

## 电力数据

| 项目 | 数值 |
| 输入电压等级 | HV (512 EU) |
| 最大输入 | 512 EU/t |
| 储能 | 40,000 EU |
| 工作耗电 | 64 EU/t |
| 单次时间 | 100 tick (5 s) |
| 单次耗电 | 6,400 EU |

## 使用步骤

1. 用 HV 线缆连接改装台。
2. 在上方槽位放入量子或纳米套装部件。
3. 在下方槽位放入单元或晶格致密剂。
4. 等待改装台完成安装。
5. 点击已安装单元旁的 × 将其拆下。
6. 在奇点管理面板中开启单元。

## 注意

- 纳米部件会显示晶格条。每格为 1 LP，颜色为占用它的单元的颜色。下方槽位中的单元会在将要占用的位置闪烁。
- 传感器单元只能通过套装面罩显示数据。
- 拆下单元不耗 EU，单元会返还。致密剂留在部件中。

## 配方

<RecipesFor id="mio_icif:producer/block_quantum_modification_station" fallbackText="-" />
