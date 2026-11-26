# 美术批次 MT1：分子重组仪（Blockbench 模型与物品栏显示）

先读 [`../README.md`](../README.md)。这一批只做一台机器：**分子重组仪**（`mio_icif:producer/block_molecular_transformer`）。现在它是一个普通立方体，用一张 96×16 的横条贴图，开机和待机长得一样；物品栏里显示的就是这个方块。这一批把它换成一台有造型的 Blockbench 机器，物品栏直接显示同一个待机方块模型，配色出两版给用户挑。

做法参照神秘工业的电动研究台（[批次说明](https://github.com/rianfalltwilight-lab/thaumic-industry/blob/main/docs/art/batches/ELECTRIC_RESEARCH_TABLE.md)，模型规矩见同仓库 [`SLIM.md`](https://github.com/rianfalltwilight-lab/thaumic-industry/blob/main/docs/art/batches/SLIM.md) 第 3、4 节）。那是私有仓库，打不开也没关系，要用的规矩都已写进本文第 3 节。

## 1. 这台机器是什么

消耗极大量电能，把一种物品重组成另一种：萤石粉变阳光化合物，铁锭变铱矿石，凋灵骷髅头变下界之星。界面里把过程分成四段，例如分子工艺是“解离 → 分选 → 合成 → 结晶”，核工艺是“解离 → 约束 → 嬗变 → 沉积”。它是奇点迭代后期的顶级机器，储能 1.2 亿 EU，不限输入电压。

造型要让人一眼看出：**一个被约束起来的反应腔，物质在里面被拆开、重新排列**。

## 2. 交付清单

| 文件 | 说明 |
| --- | --- |
| `src/main/resources/assets/mio_icif/models/block/producer/block_molecular_transformer.json` | 待机模型（覆盖现有文件） |
| `src/main/resources/assets/mio_icif/models/block/producer/block_molecular_transformer_on.json` | 工作模型（覆盖现有文件） |
| `src/main/resources/assets/mio_icif/textures/block/producer/molecular_transformer/*.png` | 模型用的贴图，文件夹是新的，里面的文件名你定（小写、下划线）。半透明玻璃的文件名里带 `glass` |
| `docs/art/mt1/variant_b/` | 紫粉版的方块部件贴图，目录与正式的 `block/producer/molecular_transformer/` 相同 |
| `tools/models/molecular_transformer.bbmodel`、`tools/models/molecular_transformer_on.bbmodel` | Blockbench 工程，贴图内嵌，与导出的 JSON 一致 |
| `tools/art/mt1_*.py` | 生成脚本（贴图、模型、预览） |
| `docs/art/previews/MT1.png`（可加 `MT1-*.png`） | 预览，见第 6 节 |

**不改：** 方块状态 `blockstates/producer/block_molecular_transformer.json`（`facing` 四向 + `lit`，`lit=true` 用 `_on` 模型）、Java 代码、语言文件、旧贴图 `block/producer/block_molecular_transformer.png`（接入时由 Claude 删除）。需要代码配合的地方写进报告的“给 Claude 的接入备注”。

## 3. 方块模型

### 3.1 造型方向（建议，可以改，改了在报告里说理由）

- 本轮参照 [Advanced Solar Panels 3.5.1（Minecraft 1.7.10）](https://mcarchive.net/mods/advancedsolarpanels?gvsn=1.7.10) 实际绘制的外形。中央竖立窄玻璃反应腔，腔内留出显示实际输入物的位置。
- 紧凑的上盘、底座周围分布三组上端头和下脚。各组之间及端头与脚之间留空，让侧面也能看到腔内。部件与材质按奇点迭代的美术风格绘制。
- **正面（北面，-Z）**：一块小控制面板或状态灯，让人分得清正面。
- 外轮廓可以不占满整格（例如四角留空），这时 Claude 会给它配碰撞和遮挡形状；在报告里写清外轮廓（底座、主体、盖板各占的范围）。

### 3.2 规矩

- 不超过 **40 个元素**、**160 个面**；每个实心方块长宽高都不小于 1 单位（厚度为 0 的平面除外）；
- 删掉看不见的面；贴着方块外边（坐标 0 或 16）的面必须写 `cullface`；
- 造型靠体块，细节画在贴图上：螺丝、刻线、细条不做成元素；
- 1:1 像素密度：贴图上 1 像素对应模型 1 单位（16 单位的面就是 16 像素），不混用别的密度；
- 正面朝北；
- **待机和工作两个模型的元素完全相同**（名字、位置一样），工作模型只换发光部位的贴图，并把那几个面设为满亮度：在面上写 `"neoforge_data": {"block_light": 15, "sky_light": 15}`；发光元素的名字以 `_glow` 结尾；
- `core_glow` 表示作用于样品的发光部件，空机时样品位置可见。客户端可以根据输入物另行绘制样品。
- 有玻璃时用 NeoForge 组合模型分材质：`"loader": "neoforge:composite"`，子模型分别写 `"render_type": "minecraft:solid"`、`"minecraft:cutout"`、`"minecraft:translucent"`；只有真正半透明的玻璃用 `translucent`，它的贴图文件名带 `glass`，也只有它允许半透明像素；
- `particle` 贴图用机壳那张；
- 不在模型里做烟、光柱、粒子这类特效。

### 3.3 待机和工作

- 待机：核心是暗的，玻璃透出深色腔体，状态灯暗；
- 工作：核心、腔体内壁的光、状态灯亮起，其余和待机一样。
- 现有方块代码已随加工状态切换 `lit`，动态样品显示由 Claude 接入。

## 4. 物品栏模型

物品直接继承待机方块。物品模型的 `display.gui` 可单独调节格内的俯角、朝向和缩放。A、B 使用同一视角，预览包含原尺寸格和按较高 GUI 分辨率重绘的格子。单独的物品 PNG 和手绘图标生成器已退役。

## 5. 配色

**机壳**取奇点迭代机器自己的灰（从 `block/producer/block_compressor_elc.png` 量的）：

| 用途 | 颜色 |
| --- | --- |
| 机壳亮面 | `#FBFBFB` `#F1F1F1` `#ECEAEB` |
| 机壳中间调、分缝 | `#DFDFDF` `#D0D0D0` |
| 底面、暗面 | `#AEAEAE` `#9F9E9E` `#838383` |
| 深色面板、腔体内壁 | `#8E8E92` `#606063` `#464648` `#2A2A2C` |
| 最深、描边 | `#1A1A1B` |

**重点色出两版**，结构完全相同，只换重点色（电极、腔体灯或状态标记）：

- **A 版：青蓝**，沿用现在这台机器的颜色：`#2ADED3`、`#02D1F3`、`#3DBBE3`，暗态用同色相的深色；放在正式路径下。
- **B 版：紫粉**，偏“物质重组”的感觉，和其他高端机器的青蓝区分开；色相在紫到品红之间，你定具体值；放在 `docs/art/mt1/variant_b/` 下。

用户看完两版再定，没选中的那版之后删掉。

## 6. 预览

`docs/art/previews/MT1.png`，一张图里放：

1. A 版：待机、工作两套，每套正面、侧面、顶面、斜视四个视角；
2. B 版：同上；
3. 和奇点迭代的电动压缩机（`block/producer/block_compressor_elc.png` 横条做成的立方体）并排，比例一致；
4. 两版待机方块在原版物品栏格子里以 1 倍与较高 GUI 缩放展示（格子 `#8B8B8B`，左上边框 `#373737`，右下边框 `#FFFFFF`）；
5. 每组下面写 A / B 和状态。

可以用仓库里的 `tools/art/item_model_preview.py` 渲染模型（不认组合模型时，自己把子模型合起来再渲染）。

## 7. 交付前

```
python3 tools/art/check_mt1.py
```

要输出 `OK`。它检查元素和面数、看不见的面、`cullface`、细于 1 单位的方块、旋转角度、两态元素一致、贴图都存在、像素规则、物品模型直接继承待机方块、B 版文件齐全、工程和预览存在。能构建的话再跑一次 Gradle `build`（不强制）。

## 8. 报告（简体中文）

- 交了哪些文件；
- 两个模型各多少元素、多少面；外轮廓的范围（给 Claude 配碰撞和遮挡形状）；
- 发光的是哪几个元素、哪几个面；
- 与本说明不一致的地方及原因，你不满意的地方；
- 给 Claude 的接入备注；
- 需要用户拍板的地方（至少：A、B 两版选哪个）。
