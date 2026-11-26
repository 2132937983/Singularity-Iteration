# 分子重组仪 · 四角约束腔（MT1 结果）

GPT 在 `art/mt1` 分支交了三版造型：立式三向电极、四角约束腔、横向束线，每版都有青蓝和紫粉两种配色。用户选定**四角约束腔、青蓝**，物品栏显示待机方块模型，只调视角。0.1.11.212 接入。另两版和紫粉配色没有并入，原稿留在 `art/mt1` 分支（提交 `d1d7371a`），生成脚本在该分支的 `tools/art/mt1_quadrupole*.py`。

[模型与物品栏](../previews/MT1-quadrupole.png) · [四视图](../previews/MT1-quadrupole-views.png) · [背包实拍](../previews/MT1-quadrupole-client.png)

## 文件

| 内容 | 位置 |
| --- | --- |
| 方块模型 | `models/block/producer/block_molecular_transformer.json`（待机）、`_on.json`（工作），NeoForge 组合模型：实心、镂空、半透明三层 |
| 贴图 | `textures/block/producer/molecular_transformer/`：`porcelain`、`ceramic_corner`、`ceramic_corner_on`、`metal_closure`、`field`、`field_on`、`front_glass`、`smoked_glass` |
| 物品模型 | 直接继承待机方块，`display.gui` 旋转 `[19, 143, 0]`、缩放 0.65 |
| Blockbench 工程 | `tools/models/molecular_transformer.bbmodel`、`molecular_transformer_on.bbmodel`，贴图内嵌 |
| 检查 | `python3 tools/art/check_mt1.py` |

## 接入时的改动

- 贴图从 `mt1_quadrupole/` 移到 `molecular_transformer/`，模型和 Blockbench 工程里的路径一起改了。
- 两张玻璃贴图的不透明度乘以 0.6，从约 58% 降到约 35%，让腔内的工作特效能透出来。Blockbench 工程里内嵌的玻璃也换成了调淡后的版本。
- 方块的轮廓和碰撞改成三段：下层 `y 0..3` 整格、玻璃腔 `x/z 4..12, y 3..13`、上层 `y 13..16` 整格（四边中间宽 2 的缺口按实心算）。方块不再遮挡相邻方块的面，见 `mio_icif_entity_block.PARTIAL_MODELS`。
- 旧的 96×16 横条贴图 `block/producer/block_molecular_transformer.png` 已删除。

## 结构（北向，0–16 坐标）

| 部位 | 范围 |
| --- | --- |
| 上下四角瓷夹 | 每角 7×7，折臂宽 4；下层 `y 0..3`，上层 `y 13..16`，四边中点各留宽 2 的缺口 |
| 端盖 | `x/z 4..12`；下盖到 `y 3.5`，上盖从 `y 12.5` 起 |
| 观察腔 | 四面烟色玻璃，`x/z 4..12, y 3.5..12.5`，内部 8×9×8 |
| 发光面 | 下盖 `core_glow` 朝上、上盖 `upper_contacts_glow` 朝下，各 3 个满亮面；瓷夹嵌线由 `ceramic_corner_on` 贴图显示通电色 |
| 样品位 | 中心 `(8, 8, 8)`，展示范围 `(5.5, 5.5, 5.5)` 到 `(10.5, 10.5, 10.5)` |

待机和工作都是 21 个元素、94 个面。工作特效（腔内渲染）在后续版本做。
