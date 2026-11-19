# 流体管道复原方案（青铜 / 钢 × 四尺寸）

> 2026-09-29 实施确认：采用 A（tint 染色），中文用“青铜管道 / 钢管道”。按用户最终选择新增两个方块，不替换旧输水管道。实现与验证记录见 [fluid_pipe_restoration_result.md](fluid_pipe_restoration_result.md)。

> 目标：把本项目的流体管道从「单一规格」复原为 IC2 1.12.2 的 **2 材质 × 4 尺寸 = 8 变体**，数值与行为对齐原版。
>
> 参考实现：`source/IC2Code 1.12.2/`（2.8.221-ex112），另有 `source/IC2Code_Not_for_us_Version/`（2.8.222）可作对照，两者管道定义完全一致。
>
> 范围：**仅流体管道（输水管道）**。物品管道、泵覆盖板机制不在本次范围（见「范围外事项」）。

## 1. 权威参数（来自 IC2 1.12.2 源码）

### 1.1 材质 PipeType

`source/IC2Code 1.12.2/ic2/core/block/transport/items/PipeType.java`

```java
bronze(2400, 174, 81, 17),
steel(4800, 128, 128, 128);
//      ↑transferRate ↑red ↑green ↑blue
```

| 材质 | `transferRate` | RGB（渲染/物品染色） |
|---|---|---|
| 青铜 bronze | 2400 | 174, 81, 17 |
| 钢 steel | 4800 | 128, 128, 128 |

- IC2 1.12.2 **没有 iron 管道**；第二档材质是 `steel`（钢），全仓检索 `iron_pipe` / `IronPipe` 为 0 命中。
- `transferRate` 对流体管道含义为「基础速率」，单位为 mB/s（Tooltip 显示用）。

### 1.2 尺寸 PipeSize

`source/IC2Code 1.12.2/ic2/core/block/transport/items/PipeSize.java`

```java
tiny(1, 0.25F, 0.16666667F),
small(4, 0.375F, 0.33333334F),
medium(16, 0.5F, 1.0F),
large(64, 0.625F, 2.0F);
//   ↑maxStackSize ↑thickness ↑multiplier
```

| 尺寸 | `maxStackSize` | `thickness` | `multiplier` | 粗细（像素） |
|---|---|---|---|---|
| tiny 微型 | 1 | 0.25 | **1/6 ≈ 0.1667** | 4px |
| small 小型 | 4 | 0.375 | **1/3 ≈ 0.3333** | 6px |
| medium 中型 | 16 | 0.5 | **1.0** | 8px |
| large 大型 | 64 | 0.625 | **2.0** | 10px |

> `maxStackSize` 只用于 `TileEntityItemPipe.getTransferRate()`，不是流体管道物品的堆叠限制。本次八种流体管道物品均堆叠 64。
>
> ⚠️ 最容易抄错的一处：`multiplier` 是 **1/6、1/3、1、2**，不是 1/4、1/2、1、2。
> 本项目 `mio_icif_pipe_fluid` 的注释把它记成了 `small multiplier=4`，属于错误注释。

### 1.3 流体管道换算公式

`source/IC2Code 1.12.2/ic2/core/block/transport/TileEntityFluidPipe.java`

```java
// 构造：内部储罐容量
this.tank = new PipeFluidTank(..., (int)(type.transferRate * size.multiplier));

// 每 tick：单次外送上限
int maxTransfer = Math.min(availableFluidAmount, (int)(this.type.transferRate * this.size.multiplier / 20.0F));
```

即：

```
内部容量 (mB)      = transferRate × size.multiplier
每 tick 外送上限   = transferRate × size.multiplier / 20
Tooltip「速率」    = transferRate × size.multiplier  (mB/s，与容量数值相同)
```

### 1.4 八变体数值总表（复原目标）

| 材质 \ 尺寸 | tiny 微型 | small 小型 | medium 中型 | large 大型 |
|---|---|---|---|---|
| 青铜 容量 / 速率 | 400 mB / 20 mB·t⁻¹ | 800 / 40 | 2400 / 120 | 4800 / 240 |
| 钢 容量 / 速率 | 800 mB / 40 mB·t⁻¹ | 1600 / 80 | 4800 / 240 | 9600 / 480 |

（速率单位 mB/t；Tooltip 显示的「mB/秒」= 表中速率 × 20 = 容量数值）

### 1.5 方块属性

`source/IC2Code 1.12.2/ic2/core/ref/TeBlock.java:286`

```java
fluid_pipe((Class)TileEntityFluidPipe.class, -1, false, Util.allFacings, false,
           HarvestTool.Wrench, DefaultDrop.Self, 1.0F, 5.0F,
           EnumRarity.COMMON, IC2Material.PIPE, true)
```

| 项目 | 值 |
|---|---|
| 硬度 / 爆炸抗性 | 1.0F / 5.0F |
| 采集工具 | 扳手（`HarvestTool.Wrench`） |
| 掉落 | 自身（`DefaultDrop.Self`） |
| 稀有度 / 材质 | COMMON / IC2Material.PIPE |
| 连接面 | `Util.allFacings`（六面均可连） |
| 外形 | 非完整方块：`isNormalCube=false`、`isSideSolid=false`、`doesSideBlockRendering=false`、`getLightOpacity=0` |

本项目现状为 `strength(0.5f)` + `noOcclusion()`，复原应改为 **1.0F / 5.0F**。

## 2. 行为特性（复原点）

### 2.1 通用（`TileEntityPipe` 基类）

- 连接状态为 6 bit 位掩码 `connectivity`，NBT 键 `"connectivity"`。
- 覆盖板为 6 bit 掩码 `covers`，NBT 键 `"covers"`；破坏时经 `getAuxDrops` 掉落已装覆盖板。

### 2.2 流体管道（`TileEntityFluidPipe`）

1. **自动连接**：邻居是「声明已连接」的流体管道，或邻居是流体容器（`LiquidUtil.isFluidTile`）→ 保持连接。
2. **放置**：`onPlaced` 自动把被点击的那一面接上（`facing.getOpposite()` 指向被点击方块），并让相邻管道反向接上。
3. **能力暴露**：`IFluidHandler` 只在 `facing != null && isConnected(facing)` 的面暴露；`fill/drain` 先询问覆盖板 `allowsInput/allowsOutput`。
4. **可接受流体**：全部流体（`fluid -> true`），输入/输出面均为全部朝向。
5. **每 tick 分配算法**（`updateEntityServer`，需严格复刻）：
   1. 遍历六面，收集可接收目标；相邻若也是管道且本管存量 ≥ 对方存量，则计入平均值分配池 `cPipes`；
   2. `available /= cPipes`，余数 `extraFluid` 在相邻管道间**随机 1 mB 逐个**分掉；
   3. 对**非管道目标**：`maxTransfer = min(available, transferRate × multiplier / 20)`，按目标数**平均分配**；不足 1 mB 时改为随机逐个 1 mB。
6. **拆除**：内部存量 > 1000 mB 且落点可放液体时，释放回世界（不随物品保存）。

### 2.3 物品管道（本次范围外，仅供后续参考）

`ItemPipe` / `TileEntityItemPipe` 在 IC2 1.12.2 中**未注册**（`BlocksItems` 只 `new ItemFluidPipe()`，`TeBlock` 无对应方块），属遗留代码；`ic2:pipe` 这个物品实际就是流体管道。其机制（单组内容、只推向 `getFacing()`、需扳手手动开面）留待将来做物品管道时参考。

## 3. 材质映射（本项目已有物品）

| IC2 | 本项目 | 标签 |
|---|---|---|
| `ic2:casing#bronze` | `mio_icif:resource/item_bronze_casing` | `c:casings/bronze` |
| `OreDict:plateBronze` | `mio_icif:resource/item_bronze_plate` | `c:plates/bronze` |
| `ic2:casing#steel` | `mio_icif:resource/item_adviron_casing` | `c:casings/adviron` |
| `OreDict:plateSteel` | `mio_icif:resource/item_adviron_plate` | `c:plates/adviron` |

**命名决策**：中文名沿用 IC2 官方汉化「青铜管道 / 钢管道」。理由：本项目内「钢」已统一指 adviron（钢转子、钢脚手架、钢制储物箱），若写「铁管道」会与项目内 `iron`（铁质外壳/铁板）混淆。

语言键（8 个，对齐 IC2 `pipe.*_pipe_*`）：

| 键 | zh_cn | en_us |
|---|---|---|
| `block.mio_icif.pipe.block_pipe_water.bronze_tiny` | 微型青铜管道 | Tiny Bronze Pipe |
| `...bronze_small` | 小型青铜管道 | Small Bronze Pipe |
| `...bronze_medium` | 中型青铜管道 | Medium Bronze Pipe |
| `...bronze_large` | 大型青铜管道 | Large Bronze Pipe |
| `...steel_tiny` | 微型钢管道 | Tiny Steel Pipe |
| `...steel_small` | 小型钢管道 | Small Steel Pipe |
| `...steel_medium` | 中型钢管道 | Medium Steel Pipe |
| `...steel_large` | 大型钢管道 | Large Steel Pipe |

## 4. 技术方案

### 4.1 推荐方案 A：2 方块（材质）× `size` 属性 + 模型染色

- 新增枚举 `PipeSize`（TINY / SMALL / MEDIUM / LARGE：直径像素及容量倍率，不包含物品管道专属参数）
- 新增枚举 `PipeGrade`（BRONZE 2400 RGB(174,81,17)；STEEL 4800 RGB(128,128,128)）
- 方块：新增 `pipe/fluid_pipe_bronze`、`pipe/fluid_pipe_steel`，均有 `size` 属性（4 值）；旧 `pipe/block_pipe_water` 保持原样。
- 模型：每尺寸 7 个（center + 6 臂），共 **28 个**；`tintindex: 0` 配合方块染色。复用原版 1 张侧面与 4 张端面灰度纹理，不改旧贴图。
- 物品：8 个（2 材质 × 4 尺寸），8 个物品模型共用尺寸几何及灰度纹理，通过物品染色区分材质。
- BE：从方块状态读 `size`，由 grade/size 推导容量与速率

优点：方块/物品数量少，纹理与模型不翻倍，最贴近 IC2「同模型 + 材质染色」的做法。

### 4.2 方案 B：8 个独立方块

每材质每尺寸一个方块，各自 7 条 multipart。文件数最多（模型 56 个），但与现有代码风格最接近，无需引入 tint 与颜色处理器。

### 4.3 方案 C：BER 程序化渲染（IC2 原版做法）

几何与颜色全部在 `PipeFluidRenderer` 内绘制，模型只留空壳。资源最少，但改动渲染管线最深（现有 `PipeFluidRenderer.render()` 已被提前 `return` 停用）。

**已确认采用方案 A**。B / C 仅保留为历史备选。

## 5. 实现清单

### 5.1 Java

| 文件 | 改动 |
|---|---|
| `core/api/transport/PipeSize.java`（新增） | 四尺寸直径与精确分数倍率 |
| `core/api/transport/PipeGrade.java`（新增） | 青铜 / 钢容量、向机器输出速率、RGB |
| `common/block/pipe/FluidPipeBlock.java`（新增） | `SIZE` 属性、碰撞箱、按尺寸选取物品、拆除释放流体 |
| `common/blockentity/pipe/mio_icif_pipe_fluid.java` | 支持传入容量/速率；默认构造参数及旧存档字段保持不变，复用持久化转移保护 |
| `common/blockentity/pipe/FluidPipeBlockEntity.java`（新增） | 根据材质/尺寸初始化，显式连接及 IC2 均分规则 |
| `common/item/transport/FluidPipeItem.java`（新增） | 按尺寸放置，开启点击方向，容量/速率提示 |
| `common/registry/mio_icif_blocks.java` | 两个新方块及八个物品；`strength(1.0f, 5.0f)` |
| `common/registry/mio_icif_block_entities.java` | 新增 `fluid_pipe` 实体 ID；旧实体注册不变 |
| `common/registry/mio_icif_capacities.java` | 新方块的 `FluidHandler.BLOCK` 注册 |
| `common/item/tools/mio_icif_wrench*.java` | 两种扳手按实际尺寸切换端口，潜行拆除走按尺寸掉落表 |
| `common/item/mio_icif_creativeTabs.java` | 8 个变体加入创造栏，隐藏旧输水管道 |
| 客户端 | `RegisterColorHandlersEvent.Block` / `.Item` 注册材质染色 |

### 5.2 资源

- `blockstates/pipe/fluid_pipe_{bronze,steel}.json`：每文件 4 尺寸 × 7 段 = 28 条 multipart。
- `models/block/pipe/fluid_{size}_{part}.json`：28 个共用分段模型，含 `tintindex`。
- `models/item/pipe/fluid_pipe_{grade}_{size}.json`：8 个物品模型。
- `textures/block/pipe/ic2_pipe_*.png`：从本地 IC2 原版资源复制，旧纹理不变。
- `loot_table/blocks/pipe/`：新方块掉落表
- 方块标签：`c:wrench_can_damaged`。不加入 `c:machine`，避免机器通用掉落逻辑覆盖尺寸掉落表。

### 5.3 配方（对齐 `shaped_recipes.ini:726-736`）

| 图案 | 材料 | 产出 |
|---|---|---|
| `CCC \|   \| CCC` | 青铜外壳 ×6 | 微型青铜管道 ×6 |
| `C C \| C C \| C C` | 青铜外壳 ×6 | 小型青铜管道 ×3 |
| `PPP \|   \| PPP` | 青铜板 ×6 | 中型青铜管道 ×2 |
| `P P \| P P \| P P` | 青铜板 ×6 | 大型青铜管道 ×1 |

钢版本同构，材料换成 adviron 外壳 / 钢板。共 **8 个配方**（另需 8 个 advancement 解锁文件）。

> 旧 `shaped_pipe_water.json` 保留，供旧管道兼容使用；八个新配方独立注册。

## 6. 与现状的差异与回归风险

| 项目 | 现状 | 复原后 | 风险 |
|---|---|---|---|
| 材质 / 尺寸档位 | 无 | 2 × 4 | — |
| 外形 | 旧管固定 8px（4–12） | 新管 4/6/8/10px | 旧管不变 |
| 内部容量 | 旧管 1000 mB | 新管 400–9600 mB | 旧管不变 |
| 向机器总输出 | 旧管 200 mB/t | 新管 20–480 mB/t | 旧管不变 |
| 方块硬度 / 抗性 | 旧管 0.5 / 0.5 | 新管 1.0 / 5.0 | 仅新管变化 |
| 旧存档兼容 | 旧 ID 与 NBT | 原样读取 | 不执行迁移 |

**兼容策略**：新旧 ID 独立，不转换旧管道。用户实测后可自行替换；旧输水管道仅从创造栏隐藏，旧配方、方块实体、流体数据均保留。

## 7. 验收标准

1. 8 个变体均可合成、放置、掉落自身，tooltip 显示的容量/速率与 §1.4 表一致。
2. 放置时开启被点击一侧及相邻管道对应端；其他面用扳手逐面开关。
3. `内部流体 > 1000 mB` 破坏时尝试释放一个世界流体源；`<= 1000 mB` 不释放。流体需支持世界放置，且拆除位置仍为空气。
4. 分配到相邻容器的行为与 IC2 一致：管道间「补齐到平均值 + 余量随机 1 mB」，非管道目标「平均分配」。
5. 材质染色正确（青铜 RGB 174,81,17 / 钢 RGB 128,128,128）。
6. 旧输水管道仍按原来的 1000 mB / 200 mB/t 工作，保存重载不丢失流体。

## 8. 范围外事项（本次不做，记录备查）

- **物品管道**：IC2 1.12.2 未注册，本项目若要补齐需先实现单组在途 + 扳手手动开面。
- **泵覆盖板**：IC2 用 `cover#pump_lv` / `cover#pump_mv` 实现抽取，本项目现用独立的「抽水管道」方块替代。复原泵覆盖板后，`PIPE_WATER_EXTRACT` 的去留需单独决策。
- **覆盖板机制**：`TileEntityPipe` 的 `covers` 位掩码与 `ICoverHolder` 未在本次范围内。
