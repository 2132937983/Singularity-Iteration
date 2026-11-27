# 奇点迭代 美术工作说明（给 GPT）

奇点迭代（Singularity Iteration，mod id `mio_icif`，NeoForge 1.21.1）这一轮的套装模块美术由你（GPT）来画，代码和接入由 Claude 负责。本文是你的入口：先读它，再读它点名的文件，然后按“批次”交付。

> **当前没有进行中的批次。** 最近完成的是 [SB1：三把联动拔刀](batches/SB1.md)（Codex，OBJ 模型与贴图）。下面第 0 到 3 节是为图标批次写的；第 4 节的分支、推送和报告规矩对所有批次适用。

## 0. 你负责什么

**你写的文件：**

- 贴图：当前批次清单里列出的 PNG（路径、文件名以清单为准）；
- 生成脚本：`tools/art/modules/`（一批一个前缀，例如 `tools/art/modules/mod1_*.py`）；
- 预览图：`docs/art/previews/`。

**你不改的文件：** 注册 ID 与文件路径、Java 代码、物品模型 JSON、语言文件、`tools/art/check_modules.py`。清单里有你认为不对的地方，写进报告。

**不要运行**旧的模块贴图脚本：`tools/tex_modules_180.py`、`tex_nano_185.py`、`tex_nano_186.py`、`tex_core_190.py`、`tex_1100.py`、`tex_1101.py`、`tex_1102.py`、`gen_suit_assets.py`。它们是历史版本的生成脚本，一运行就会把新图覆盖回旧图。现在的模块图标只由 `tools/art/modules/` 生成。

**净室：** 不复制、不描摹、不调色改用任何其他模组或游戏的贴图。奇点迭代自己的贴图只用来对照风格、尺寸和颜色。

## 1. 先读

1. [`style-suit-modules.md`](style-suit-modules.md)：模块美术规范。量子（陶瓷白）、纳米（碳纤维黑）、共用、Mk.II 各怎么画，调色板，画法。
2. 当前批次的说明 `docs/art/batches/<批次>.md`：每张图的文件名、模块名、功能、图标建议、功能色。
3. 对照用：
   - 量子套装物品图 `src/main/resources/assets/mio_icif/textures/item/armor/item_armor_quantum_*.png`（浅灰白）；
   - 纳米套装物品图 `.../item/armor/item_armor_nano_*.png`（深灰黑）；
   - 现在的模块图 `.../item/module/`（要被替换的旧图，看它们只为了知道每个模块原来画的是什么）。

## 2. 格式（代码依赖，不能改）

- 物品图标 **16×16**，8 位 RGBA、非隔行 PNG，透明背景。
- 物品模型是 `minecraft:item/generated`，单层 `layer0`，**没有着色**：颜色就是贴图里的颜色。
- 文件名不变，直接覆盖 `src/main/resources/assets/mio_icif/textures/item/module/` 下的同名文件。
- 不加 `.png.mcmeta`（这一批不做动画）。

## 3. 执行要求

- 1:1 像素密度；像素只用完全透明或完全不透明（alpha 只有 0 和 255）；
- 外描边用规范里的描边色，不用纯黑 `#000000`；光源在左上；
- 不画随机噪点、渐变、枕头阴影，不用抖动做过渡（碳纤维的规则斜纹不算噪点，见规范）；
- 每张图由脚本生成，脚本提交进仓库；可以用 PIL（脚本开头写明）；
- 先出预览，再验收；局部修改时其他已经定稿的图不动。

## 4. 一批的流程

1. 仓库是 `https://github.com/rianfalltwilight-lab/Singularity-Iteration`，从分支 `art/mod1-suit-modules-brief` 切新分支 `art/mod1`；画完提交并**推送这个分支**到同一个仓库，不开 PR，不改其他分支。Claude 从这个分支取图、检查、接入。
2. 按批次说明画，交预览图到 `docs/art/previews/<批次>.png`：
   - 所有图标放大 8 倍，放在原版物品栏格子背景上（格子 `#8B8B8B`，左上边框 `#373737`，右下边框 `#FFFFFF`），按批次清单的分组排，每张下面写文件名；
   - 另加一行原尺寸（1 倍）的同一批图，排在原版物品栏格子上，用来检查实际游戏里的可读性；
   - 另加一行“新旧对照”：每组挑 4 个，旧图和新图并排。
3. 交付前运行：
   ```
   python3 tools/art/check_modules.py
   ```
   要输出 `OK`。能构建的话再跑一次 Gradle `build`（不强制）。
4. 报告（简体中文）：交了哪些文件，与规范不一致的地方及原因，你不满意的地方，需要用户拍板的地方。

## 5. 当前批次

暂无。

已完成：

- [SB1：三把联动拔刀](batches/SB1.md)（纳米刃、量子刃、奇点刃，拔刀剑 OBJ 模型与贴图），检查脚本 `tools/art/check_sb1.py`，结果见 [`sb1/README.md`](sb1/README.md)，0.1.11.232 接入。
- [AN1：僚机与战甲的 GeckoLib 关键帧动作](batches/AN1.md)，契约文件 [`an1/spec.json`](an1/spec.json)，第一到三部分在 0.1.11.225～229 接入。
- [MT1：分子重组仪](batches/MT1.md)（Blockbench 机器模型），选用四角约束腔、青蓝配色，0.1.11.212 接入。结果见 [`mt1/README.md`](mt1/README.md)。
- MOD1 套装模块图标（[第一轮清单](batches/MOD1-suit-modules.md)，69 张；[第二轮修改清单](batches/MOD1-r2.md)），0.1.11.192 接入。
