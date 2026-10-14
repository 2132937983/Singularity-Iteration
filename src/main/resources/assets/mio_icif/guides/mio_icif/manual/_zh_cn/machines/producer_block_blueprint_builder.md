---
navigation:
  title: "蓝图建造机"
  icon: mio_icif:producer/block_blueprint_builder
  parent: machines.md
  position: 10
item_ids:
  - mio_icif:producer/block_blueprint_builder
---

# 蓝图建造机

<Row>
  <BlockImage id="mio_icif:producer/block_blueprint_builder" scale="3" />
</Row>

## 功能

蓝图建造机有四架建造无人机，逐个放置蓝图中的方块。
无人机从[军械库资材接口](producer_block_material_interface.md)取用建材。

## 蓝图来源

* 槽位中已部署的机械动力蓝图。使用该蓝图的锚点、旋转与镜像。
* 游戏 `schematics` 文件夹中的文件（服务器为服务器文件夹）。可读取 `.nbt` 结构文件（含机械动力蓝图）与 `.litematic` 文件（Litematica）。

## 能量数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV（128 EU） |
| 储能 | 200,000 EU |
| 每个方块的能量 | 40 EU |
| 范围 | 建造机周围 64 格内（蓝图的全部方块） |
| 无人机 | 4 架 |

## 步骤

1. 在建造机 16 格内放置军械库资材接口。
2. 将建材放入接口或其相邻的存储。
3. 将已部署的机械动力蓝图放入槽位，或用 `<` `>` 按钮选择文件。
4. 对于文件，设置旋转（R）、镜像（M）与偏移（X、Y、Z）。偏移为建造机到蓝图最小角的距离。
5. 点击"读取"，全息投影显示工作范围。
6. 点击"开始"。

## 规则

* 无人机从最底层开始逐层向上放置。
* 无人机不会破坏方块。已有其他方块的位置会被跳过并计为"被阻挡"。
* 仅使用普通物品：不使用带内容物、自定义名称或附魔的物品。
* 门或高大植物的上半部分、床的头部不需要物品。
* 含水方块以无水状态放置，不放置流体。
* 每次放置都以建造机所有者的身份触发方块放置事件，保护类模组可以阻止。

**注意：** 点击"开始"之前请检查工作范围全息投影。无人机会填满范围内的所有空位。

## 配方

<RecipesFor id="mio_icif:producer/block_blueprint_builder" fallbackText="-" />
