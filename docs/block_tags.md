# 方块标签体系文档

本文档说明 mio_icif 模组中各类机器方块**应该打上哪些标签**，以及当前各机器**已打上的标签汇总**。供内部开发和附属开发参考。

---

## 一、标签总表

| 标签ID | 命名空间 | 类型 | 用途 |
|--------|---------|------|------|
| `minecraft:mineable/pickaxe` | 原版 | 挖掘工具 | 镐子可挖掘 |
| `c:needs_stone_tool` | 通用 | 挖掘等级 | 需石镐以上 |
| `c:needs_iron_tool` | 通用 | 挖掘等级 | 需铁镐以上 |
| `c:needs_diamond_tool` | 通用 | 挖掘等级 | 需钻石镐以上 |
| `c:machine` | 通用 | 机器分类 | 扳手拆卸逻辑（保留能量/掉落外壳） |
| `c:cable` | 通用 | 电缆分类 | 扳手拆卸逻辑（保留能量） |
| `c:wrench_can_damaged` | 通用 | 扳手交互 | 扳手可拆卸（可能损坏） |
| `mio_icif:generator` | 模组私有 | 子分类 | 发电机 |
| `mio_icif:energy_container` | 模组私有 | 子分类 | 储能箱 |
| `mio_icif:chargepad` | 模组私有 | 子分类 | 充电座 |
| `mio_icif:producer` | 模组私有 | 子分类 | 加工机器 |
| `minecraft:villager_job_sites` | 原版 | 村民 | 村民工作站点 |

---

## 二、各类机器方块应打的标签

### 发电机（Generator）

| 标签 | 必须 | 说明 |
|------|------|------|
| `minecraft:mineable/pickaxe` | ✅ | 镐子可挖掘 |
| `c:needs_stone_tool` | ✅ | 需石镐以上（基础发电机） |
| `c:needs_iron_tool` | ⚠️ | 需铁镐以上（进阶太阳能板、量子发电机） |
| `c:machine` | ✅ | 扳手拆卸逻辑 |
| `mio_icif:generator` | ✅ | 标识为发电机 |

### 储能箱（Energy Container）

| 标签 | 必须 | 说明 |
|------|------|------|
| `minecraft:mineable/pickaxe` | ✅ | 镐子可挖掘 |
| `c:needs_stone_tool` | ✅ | 需石镐以上 |
| `c:machine` | ✅ | 扳手拆卸逻辑 |
| `mio_icif:energy_container` | ✅ | 标识为储能箱 |

### 充电座（Chargepad）

| 标签 | 必须 | 说明 |
|------|------|------|
| `minecraft:mineable/pickaxe` | ✅ | 镐子可挖掘 |
| `c:needs_stone_tool` | ✅ | 需石镐以上 |
| `c:machine` | ✅ | 扳手拆卸逻辑 |
| `mio_icif:chargepad` | ✅ | 标识为充电座 |
| 实现 `IChargepadBlock` 接口 | ✅ | 自动获得充电粒子特效 |

### 加工机器（Producer）

| 标签 | 必须 | 说明 |
|------|------|------|
| `minecraft:mineable/pickaxe` | ✅ | 镐子可挖掘 |
| `c:needs_stone_tool` | ✅ | 需石镐以上（大部分） |
| `c:needs_iron_tool` | ⚠️ | 需铁镐以上（能量转换器等） |
| `c:machine` | ✅ | 扳手拆卸逻辑 |
| `mio_icif:producer` | ✅ | 标识为加工机器 |

### 电缆（Cable）

| 标签 | 必须 | 说明 |
|------|------|------|
| `c:cable` | ✅ | 扳手拆卸逻辑 |
| 注：电缆无 `minecraft:mineable/pickaxe`，徒手可破坏 | | |

### 变压器（Transformer）

| 标签 | 必须 | 说明 |
|------|------|------|
| `minecraft:mineable/pickaxe` | ✅ | 镐子可挖掘 |
| `c:needs_stone_tool` | ✅ | 需石镐以上 |
| `c:machine` | ✅ | 扳手拆卸逻辑 |

---

## 三、各机器当前标签汇总

### 发电机

| 方块 | mineable/pickaxe | needs_stone_tool | needs_iron_tool | c:machine | mio_icif:generator |
|------|:-:|:-:|:-:|:-:|:-:|
| 热力发电机 | ✅ | ✅ | | ✅ | ✅ |
| 地热发电机 | ✅ | ✅ | | ✅ | ✅ |
| 太阳能发电机 | ✅ | ✅ | | ✅ | ✅ |
| 进阶太阳能板 | ✅ | | ✅ | ✅ | ✅ |
| 混合太阳能板 | ✅ | | ✅ | ✅ | ✅ |
| 终极混合太阳能板 | ✅ | | ✅ | ✅ | ✅ |
| 量子太阳能板 | ✅ | | ✅ | ✅ | ✅ |
| 量子发电机 | ✅ | | ✅ | ✅ | ✅ |
| 风力发电机 | ✅ | ✅ | | ✅ | ✅ |
| 水力发电机 | ✅ | ✅ | | ✅ | ✅ |
| 半流体发电机 | ✅ | ✅ | | ✅ | ✅ |
| 斯特林发电机 | ✅ | ✅ | | ✅ | ✅ |
| 动能发电机 | ✅ | ✅ | | ✅ | ✅ |
| RT发电机 | ✅ | ✅ | | ✅ | ✅ |
| 无限发电机 | ✅ | ✅ | | ✅ | ✅ |
| 核反应堆 | ✅ | ✅ | | ✅ | ✅ |
| 电力热发电机 | ✅ | ✅ | | ✅ | ✅ |
| 固体热发电机 | ✅ | ✅ | | ✅ | ✅ |
| 流体热发电机 | ✅ | ✅ | | ✅ | ✅ |
| RT热发电机 | ✅ | ✅ | | ✅ | ✅ |
| 热源流体 | ✅ | ✅ | | ✅ | ✅ |
| 手动动能发电机 | ✅ | ✅ | | ✅ | ✅ |
| 风力动能发电机 | ✅ | ✅ | | ✅ | ✅ |
| 水力动能发电机 | ✅ | ✅ | | ✅ | ✅ |
| 电力动能发电机 | ✅ | ✅ | | ✅ | ✅ |
| 斯特林动能发电机 | ✅ | ✅ | | ✅ | ✅ |

### 储能箱

| 方块 | mineable/pickaxe | needs_stone_tool | c:machine | mio_icif:energy_container |
|------|:-:|:-:|:-:|:-:|
| BatBox | ✅ | ✅ | ✅ | ✅ |
| CESU | ✅ | ✅ | ✅ | ✅ |
| MFE | ✅ | ✅ | ✅ | ✅ |
| MFSU | ✅ | ✅ | ✅ | ✅ |
| LESU | ✅ | ✅ | ✅ | ✅ |

### 充电座

| 方块 | mineable/pickaxe | needs_stone_tool | c:machine | mio_icif:chargepad | IChargepadBlock |
|------|:-:|:-:|:-:|:-:|:-:|
| BatBox 充电座 | ✅ | ✅ | ✅ | ✅ | ✅ |
| CESU 充电座 | ✅ | ✅ | ✅ | ✅ | ✅ |
| MFE 充电座 | ✅ | ✅ | ✅ | ✅ | ✅ |
| MFSU 充电座 | ✅ | ✅ | ✅ | ✅ | ✅ |
| LESU 充电座 | ✅ | ✅ | ✅ | ✅ | ✅ |

### 加工机器

| 方块 | mineable/pickaxe | needs_stone_tool | needs_iron_tool | c:machine | mio_icif:producer |
|------|:-:|:-:|:-:|:-:|:-:|
| 磁化机 | ✅ | ✅ | | ✅ | ✅ |
| 特斯拉线圈 | ✅ | ✅ | | ✅ | ✅ |
| 金属成型机 | ✅ | ✅ | | ✅ | ✅ |
| 收割机 | ✅ | ✅ | | ✅ | ✅ |
| 传送器 | ✅ | ✅ | | ✅ | ✅ |
| 电炉 | ✅ | ✅ | | ✅ | ✅ |
| 打粉机 | ✅ | ✅ | | ✅ | ✅ |
| 进阶打粉机 | ✅ | ✅ | | ✅ | ✅ |
| 洗矿机 | ✅ | ✅ | | ✅ | ✅ |
| 发酵机 | ✅ | ✅ | | ✅ | ✅ |
| 高炉 | ✅ | ✅ | | ✅ | ✅ |
| 蒸汽发电机 | ✅ | ✅ | | ✅ | ✅ |
| 蒸汽再加压机 | ✅ | ✅ | | ✅ | ✅ |
| 太阳能蒸馏器 | ✅ | ✅ | | ✅ | ✅ |
| 冷凝机 | ✅ | ✅ | | ✅ | ✅ |
| 切割机 | ✅ | ✅ | | ✅ | ✅ |
| 车床 | ✅ | ✅ | | ✅ | ✅ |
| 蒸汽动能发电机 | ✅ | ✅ | | ✅ | ✅ |
| 离心机 | ✅ | ✅ | | ✅ | ✅ |
| 提取机 | ✅ | ✅ | | ✅ | ✅ |
| 压缩机 | ✅ | ✅ | | ✅ | ✅ |
| 罐装机 | ✅ | ✅ | | ✅ | ✅ |
| 采矿机 | ✅ | ✅ | | ✅ | ✅ |
| 进阶采矿机 | ✅ | ✅ | | ✅ | ✅ |
| 泵 | ✅ | ✅ | | ✅ | ✅ |
| 电解机 | ✅ | ✅ | | ✅ | ✅ |
| 回收机 | ✅ | ✅ | | ✅ | ✅ |
| 红石冷却剂注入器 | ✅ | ✅ | | ✅ | ✅ |
| 青金石冷却剂注入器 | ✅ | ✅ | | ✅ | ✅ |
| 地形改造机 | ✅ | ✅ | | ✅ | ✅ |
| 期货机 | ✅ | ✅ | | ✅ | ✅ |
| 感应炉 | ✅ | ✅ | | ✅ | ✅ |
| 物质生成机 | ✅ | ✅ | | ✅ | ✅ |
| 粒子聚合发生器 | ✅ | ✅ | | ✅ | ✅ |
| 金属成型机 | ✅ | ✅ | | ✅ | ✅ |
| 扫描机 | ✅ | ✅ | | ✅ | ✅ |
| 分子转换器 | ✅ | ✅ | | ✅ | ✅ |
| 模式存储器 | ✅ | ✅ | | ✅ | ✅ |
| 复制机 | ✅ | ✅ | | ✅ | ✅ |
| 物品缓冲器 | ✅ | ✅ | | ✅ | ✅ |
| 分类机 | ✅ | ✅ | | ✅ | ✅ |
| 物品分配器 | ✅ | ✅ | | ✅ | ✅ |
| 流体分配器 | ✅ | ✅ | | ✅ | ✅ |
| 加权流体分配器 | ✅ | ✅ | | ✅ | ✅ |
| 流体调节器 | ✅ | ✅ | | ✅ | ✅ |
| 进阶压缩机 | ✅ | ✅ | | ✅ | ✅ |
| 进阶金属成型机 | ✅ | ✅ | | ✅ | ✅ |
| 能量转换器 | ✅ | | ✅ | ✅ | ✅ |
| 反应堆腔室 | ✅ | ✅ | | ✅ | ✅ |

### 变压器

| 方块 | mineable/pickaxe | needs_stone_tool | c:machine |
|------|:-:|:-:|:-:|
| LV→MV 变压器 | ✅ | ✅ | ✅ |
| MV→HV 变压器 | ✅ | ✅ | ✅ |
| HV→EV 变压器 | ✅ | ✅ | ✅ |
| EV→SC 变压器 | ✅ | ✅ | ✅ |

### 电缆

| 方块 | c:cable |
|------|:-:|
| 锡电缆 (LV) | ✅ |
| 铜电缆 (MV) | ✅ |
| 金电缆 (HV) | ✅ |
| 铁电缆 (EV) | ✅ |
| 玻璃电缆 (IV) | ✅ |
| 绝缘锡电缆 (LV) | ✅ |
| 绝缘铜电缆 (MV) | ✅ |
| 绝缘金电缆 (HV) | ✅ |
| 绝缘铁电缆 (EV) | ✅ |

### 其他

| 方块 | mineable/pickaxe | needs_stone_tool | needs_iron_tool | needs_diamond_tool | c:machine |
|------|:-:|:-:|:-:|:-:|:-:|
| 基础机器外壳 | ✅ | ✅ | | | |
| 高级机器外壳 | ✅ | ✅ | | | |
| 木箱 | ✅ | | | | |
| 青铜箱 | ✅ | ✅ | | | ✅ |
| 铁箱 | ✅ | ✅ | | | ✅ |
| 铬铁箱 | ✅ | | ✅ | | ✅ |
| 铱箱 | ✅ | | | ✅ | |
| 无线电力传输节点 | ✅ | | ✅ | | ✅ |
| 工业工作台 | ✅ | ✅ | | | |
| 建筑墙壁 | ✅ | | ✅ | | |

---

## 四、标签的代码逻辑

### `c:machine` — 扳手拆卸行为

定义在 `mio_icif_entity_block.getDrops()`：

- **扳手拆卸**：保留方块完整物品，保存能量数据到 `BlockEntityData`
- **非扳手破坏**：掉落基础机器外壳 `block_machine_hull_basic`

```java
if (state.is(mio_icif_tags.MACHINE) || state.is(mio_icif_tags.CABLE)) {
    if (tool.is(Tags.Items.TOOLS_WRENCH)) {
        // 扳手拆卸：保留完整方块 + 能量数据
    }
    if (state.is(mio_icif_tags.MACHINE)) {
        // 非扳手：掉落机器外壳
    }
}
```

### `c:cable` — 电缆扳手拆卸行为

- **扳手拆卸**：保留方块完整物品，保存能量数据
- **非扳手破坏**：无特殊掉落（电缆不掉落外壳）

### `c:wrench_can_damaged` — 扳手可损坏

引用 `#c:machine` 和 `#c:cable`，表示扳手可以交互但可能损坏的方块。

### `mio_icif:chargepad` + `IChargepadBlock` — 充电粒子

客户端粒子系统通过 `IChargepadBlock` 接口判断，不依赖标签。标签用于逻辑分类查询。

---

## 五、Java 代码中的标签常量

定义在 `mio_icif_tags.java`：

```java
// 通用标签
mio_icif_tags.MACHINE          // c:machine
mio_icif_tags.CABLE            // c:cable

// 模组私有子分类标签
mio_icif_tags.GENERATOR        // mio_icif:generator
mio_icif_tags.ENERGY_CONTAINER // mio_icif:energy_container
mio_icif_tags.PRODUCER         // mio_icif:producer
mio_icif_tags.CHARGEPAD        // mio_icif:chargepad
```

使用示例：

```java
if (state.is(mio_icif_tags.MACHINE)) { ... }
if (state.is(mio_icif_tags.ENERGY_CONTAINER)) { ... }
```

---

## 六、附属开发者指南

### 新增机器方块时需要打的标签

在附属模组的 `src/main/resources/data/` 中创建标签JSON文件：

**1. 添加到 `c:machine`**（扳手拆卸逻辑生效）：

```json
// data/c/tags/block/machine.json
{
  "replace": false,
  "values": [
    "your_mod:your_machine"
  ]
}
```

**2. 添加到 `minecraft:mineable/pickaxe`**（镐子可挖掘）：

```json
// data/minecraft/tags/block/mineable/pickaxe.json
{
  "replace": false,
  "values": [
    "your_mod:your_machine"
  ]
}
```

**3. 添加挖掘等级**（如需石镐）：

```json
// data/c/tags/block/needs_stone_tool.json
{
  "replace": false,
  "values": [
    "your_mod:your_machine"
  ]
}
```

**4. 添加到子分类标签**（如发电机）：

```json
// data/mio_icif/tags/block/generator.json
{
  "replace": false,
  "values": [
    "your_mod:your_generator"
  ]
}
```

### 新增充电座方块

除上述标签外，还需实现 `IChargepadBlock` 接口：

```java
public class MyChargepad extends Block implements IChargepadBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    @Override
    public boolean isCharging(BlockState state) {
        return state.getValue(LIT);
    }
}
```

> **重要**：所有标签JSON必须使用 `"replace": false`，确保追加而非覆盖。