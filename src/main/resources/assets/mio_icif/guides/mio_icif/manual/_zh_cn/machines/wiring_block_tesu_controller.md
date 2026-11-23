---
navigation:
  title: "TESU 控制器"
  icon: mio_icif:wiring/block_tesu_controller
  parent: power.md
  position: 17
item_ids:
  - mio_icif:wiring/block_tesu_controller
---

# TESU 控制器

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_controller" scale="3" />
</Row>

## 功能

TESU 控制器是 TESU（太级储电罐）的核心，这是一座 7×7×13 的长方体储电设备。控制器存电，底圈上的输入端口和输出端口负责进出电。
控制器朝向结构外侧，整个结构随它的朝向转动。控制器自己的面不收发电，电线要接在端口上。

## 电力数据

| 项目 | 数值 |
|---|---|
| 储电 | 227,000,000,000 EU |

## 使用步骤

1. 按下面的结构搭建，控制器放在底圈正面正中。
2. 在端口位上放 TESU 输入端口、TESU 输出端口和 TESU 无线输出端口，不用的端口位用 TESU 外壳封住。
3. 打开界面：显示储电、端口数量和输出合计；结构没完成时显示第一个不对的坐标和那里应该放什么。

## 注意

- 控制器每秒检查一次结构。
- 界面有一个给物品充电的槽和一个从电池取电的槽。

## 结构

| 层 | 方块 |
|---|---|
| 1 | 底层：7×7 的 TESU 外壳 |
| 2 | 正面正中是控制器，四面各 2 个端口位共 8 个，其余是外壳 |
| 3–11 | 罐身：外壳罐壁，四面中间各有三列宽的防爆玻璃观察窗；罐内中间 3-5-5-5-3 的 21 格是 TESU 储能单元（共 189 块），四个内角是外壳 |
| 12 | 罐盖：7×7 外壳 |
| 13 | 顶层：7×7 外壳 |

拖动视图可以转动。下面的列表给出各方块和规则。JEI 的"多方块结构"分类里有同样的结构图。

<GameScene zoom="2" interactive={true}>
  <SiStructure id="mio_icif:tesu" />
</GameScene>

<SiStructureInfo id="mio_icif:tesu" />

## 配方

<RecipesFor id="mio_icif:wiring/block_tesu_controller" fallbackText="-" />
