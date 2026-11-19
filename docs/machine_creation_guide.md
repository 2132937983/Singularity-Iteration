# 机器生产方块创建流程文档

## 概述

本文档介绍如何在 Industrial Craft in Future (mio_icif) 模组中创建一个新的机器生产方块。

## 一、Java 代码部分

### 1. 创建 Block 类

**文件位置**: `src/main/java/com/Industrial_Craft_In_Future/mio_icif/Blocks/Producer/`

**示例**: `mio_icif_block_[machine_name]_elc.java`

```java
package com.Industrial_Craft_In_Future.mio_icif.Blocks.Producer;

import com.Industrial_Craft_In_Future.mio_icif.Blocks.entity.producer.mio_icif_[machine_name]_elc;
import com.Industrial_Craft_In_Future.mio_icif.Blocks.entity.mio_icif_block_entities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class mio_icif_block_[machine_name]_elc extends mio_icif_entity_block {
    
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    
    public mio_icif_block_[machine_name]_elc(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false));
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mio_icif_[machine_name]_elc(pos, state);
    }
}
```

### 2. 创建 BlockEntity 类

**文件位置**: `src/main/java/com/Industrial_Craft_In_Future/mio_icif/Blocks/entity/producer/`

**示例**: `mio_icif_[machine_name]_elc.java`

```java
package com.Industrial_Craft_In_Future.mio_icif.Blocks.entity.producer;

import com.Industrial_Craft_In_Future.mio_icif.Blocks.entity.mio_icif_producer;
import com.Industrial_Craft_In_Future.mio_icif.energy.EnergyUnit.CableTier;

public class mio_icif_[machine_name]_elc extends mio_icif_producer {
    
    // 槽位定义
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int TOTAL_SLOTS = 2;
    
    // 配置
    public static final long DEFAULT_CAPACITY = 10000L;
    public static final long DEFAULT_MAX_RECEIVE = 128L;
    public static final long DEFAULT_MAX_EXTRACT = 128L;
    public static final int DEFAULT_WORK_TIME = 200;
    
    public mio_icif_[machine_name]_elc(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.[MACHINE_NAME]_ELC_ENTITY_TYPE.get());
    }
    
    public mio_icif_[machine_name]_elc(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            TOTAL_SLOTS,
            DEFAULT_ENERGY_PER_TICK,
            CableTier.LV);
    }
    
    @Override
    protected void doWork() {
        // 实现工作逻辑
    }
}
```

### 3. 注册方块

**文件**: `mio_icif_blocks.java`

```java
public static final DeferredBlock<mio_icif_block_[machine_name]_elc> [MACHINE_NAME]_ELC = 
    registerBlock("producer/block_[machine_name]_elc", () -> 
        new mio_icif_block_[machine_name]_elc(Block.Properties.of()
            .strength(3.0f, 3.0f)
            .requiresCorrectToolForDrops()));
```

### 4. 注册 BlockEntity 和 MenuType

**文件**: `mio_icif_block_entities.java`

```java
// 注册方块实体类型
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<mio_icif_[machine_name]_elc>> [MACHINE_NAME]_ELC_ENTITY_TYPE =
    BLOCK_ENTITIES.register("[machine_name]_elc", () ->
        BlockEntityType.Builder.of(mio_icif_[machine_name]_elc::new, mio_icif_blocks.[MACHINE_NAME]_ELC.get()).build(null));

// 注册 MenuType
public static final DeferredHolder<MenuType<?>, MenuType<[MachineName]Menu>> [MACHINE_NAME]_ELC_MENU_TYPE =
    MENU_TYPES.register("[machine_name]_elc_menu", () -> 
        new MenuType<>((containerId, playerInventory) -> 
            new [MachineName]Menu(containerId, playerInventory), 
            FeatureFlags.VANILLA_SET));
```

### 5. 创建 GUI 类

**文件位置**: `src/main/java/com/Industrial_Craft_In_Future/mio_icif/Screen/`

```java
public class mio_icif_gui_[machine_name]_elc {
    public static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/gui/[machine_name]_elc.png");
    
    public static class [MachineName]Menu extends AbstractContainerMenu {
        // 实现菜单逻辑
    }
    
    public static class [MachineName]Screen extends AbstractContainerScreen<[MachineName]Menu> {
        // 实现屏幕渲染
    }
}
```

### 6. 注册 GUI

**文件**: `mio_icif_screens.java`

```java
event.register(
    mio_icif_block_entities.[MACHINE_NAME]_ELC_MENU_TYPE.get(),
    mio_icif_gui_[machine_name]_elc.[MachineName]Screen::new
);
```

### 7. 注册能力

**文件**: `mio_icif_capacities.java`

```java
// 注册 EU 能量能力
event.registerBlockEntity(EUApi.SIDED, [MACHINE_NAME]_ELC_ENTITY_TYPE.get(), (be, direction) -> {
    return be.getEnergyStorage(direction);
});

// 注册物品处理能力
event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, [MACHINE_NAME]_ELC_ENTITY_TYPE.get(), (be, direction) -> {
    return be.getItemHandler();
});
```

### 8. 添加到创造模式物品栏

**文件**: `mio_icif_creativeTabs.java`

```java
output.accept(mio_icif_blocks.[MACHINE_NAME]_ELC.get());
```

---

## 二、资源文件部分

### 1. BlockState 定义

**文件位置**: `src/main/resources/assets/mio_icif/blockstates/producer/`

**文件名**: `block_[machine_name]_elc.json`

```json
{
    "variants": {
        "lit=false,facing=north": {
            "model": "mio_icif:block/producer/block_[machine_name]_elc"
        },
        "lit=false,facing=south": {
            "model": "mio_icif:block/producer/block_[machine_name]_elc",
            "y": 180
        },
        "lit=false,facing=east": {
            "model": "mio_icif:block/producer/block_[machine_name]_elc",
            "y": 90
        },
        "lit=false,facing=west": {
            "model": "mio_icif:block/producer/block_[machine_name]_elc",
            "y": 270
        },
        "lit=true,facing=north": {
            "model": "mio_icif:block/producer/block_[machine_name]_elc_on"
        },
        "lit=true,facing=south": {
            "model": "mio_icif:block/producer/block_[machine_name]_elc_on",
            "y": 180
        },
        "lit=true,facing=east": {
            "model": "mio_icif:block/producer/block_[machine_name]_elc_on",
            "y": 90
        },
        "lit=true,facing=west": {
            "model": "mio_icif:block/producer/block_[machine_name]_elc_on",
            "y": 270
        }
    }
}
```

### 2. 模型定义

**文件位置**: `src/main/resources/assets/mio_icif/models/block/producer/`

#### 关闭状态模型: `block_[machine_name]_elc.json`

```json
{
    "parent": "block/cube",
    "textures": {
        "particle": "mio_icif:block/producer/block_[machine_name]_elc",
        "all": "mio_icif:block/producer/block_[machine_name]_elc"
    },
    "elements": [
        {
            "from": [ 0, 0, 0 ],
            "to": [ 16, 16, 16 ],
            "faces": {
                "down":  { "uv": [0, 0, 1.33, 16], "texture": "#particle", "cullface": "down" },
                "up":    { "uv": [1.33, 0, 2.66, 16], "texture": "#particle", "cullface": "up" },
                "south": { "uv": [6.66, 0, 8, 16], "texture": "#all", "cullface": "south" },
                "north": { "uv": [4, 0, 5.33, 16], "texture": "#all", "cullface": "north" },
                "west":  { "uv": [5.33, 0, 6.66, 16], "texture": "#all", "cullface": "west" },
                "east":  { "uv": [2.66, 0, 4, 16], "texture": "#all", "cullface": "east" }
            }
        }
    ]
}
```

#### 开启状态模型: `block_[machine_name]_elc_on.json`

```json
{
    "parent": "mio_icif:block/producer/block_[machine_name]_elc",
    "textures": {
        "particle": "mio_icif:block/producer/block_[machine_name]_elc_on",
        "all": "mio_icif:block/producer/block_[machine_name]_elc_on"
    }
}
```

### 3. 纹理文件

**文件位置**: `src/main/resources/assets/mio_icif/textures/block/producer/`

**文件名**:
- `block_[machine_name]_elc.png` - 关闭状态纹理
- `block_[machine_name]_elc_on.png` - 开启状态纹理

**纹理规格**:
- 尺寸: 16x16 像素（或 32x32、64x64 等 2 的幂次）
- 格式: PNG
- 推荐: 使用 16x16 保持原版风格，或使用 32x32 获得更高细节

### 4. GUI 纹理

**文件位置**: `src/main/resources/assets/mio_icif/textures/gui/`

**文件名**: `[machine_name]_elc.png`

**尺寸**: 根据 GUI 大小，通常为 256x256 像素

---

## 三、本地化

### 1. 中文本地化

**文件**: `src/main/resources/assets/mio_icif/lang/zh_cn.json`

```json
{
    "block.mio_icif.block_[machine_name]_elc": "[机器中文名]",
    "gui.mio_icif.[machine_name]_elc.title": "[机器中文名]"
}
```

### 2. 英文本地化

**文件**: `src/main/resources/assets/mio_icif/lang/en_us.json`

```json
{
    "block.mio_icif.block_[machine_name]_elc": "[Machine English Name]",
    "gui.mio_icif.[machine_name]_elc.title": "[Machine English Name]"
}
```

---

## 四、完整文件清单

### Java 文件
1. `Blocks/Producer/mio_icif_block_[machine_name]_elc.java` - Block 类
2. `Blocks/entity/producer/mio_icif_[machine_name]_elc.java` - BlockEntity 类
3. `Screen/mio_icif_gui_[machine_name]_elc.java` - GUI 类

### 注册文件（修改）
4. `Blocks/mio_icif_blocks.java` - 注册方块
5. `Blocks/entity/mio_icif_block_entities.java` - 注册 BlockEntity 和 MenuType
6. `Screen/mio_icif_screens.java` - 注册 GUI
7. `Blocks/mio_icif_capacities.java` - 注册能力
8. `Items/mio_icif_creativeTabs.java` - 添加到创造模式物品栏

### 资源文件
9. `blockstates/producer/block_[machine_name]_elc.json` - BlockState 定义
10. `models/block/producer/block_[machine_name]_elc.json` - 关闭状态模型
11. `models/block/producer/block_[machine_name]_elc_on.json` - 开启状态模型
12. `textures/block/producer/block_[machine_name]_elc.png` - 关闭状态纹理
13. `textures/block/producer/block_[machine_name]_elc_on.png` - 开启状态纹理
14. `textures/gui/[machine_name]_elc.png` - GUI 纹理

### 本地化文件（修改）
15. `lang/zh_cn.json` - 中文本地化
16. `lang/en_us.json` - 英文本地化

---

## 五、注意事项

1. **命名规范**: 使用小写字母和下划线，保持与现有代码一致
2. **电压等级**: 根据机器类型选择合适的 CableTier（LV/MV/HV/EV）
3. **槽位设计**: 合理规划输入、输出、电池、升级等槽位位置
4. **纹理风格**: 保持与 IC2 原版机器一致的工业风格
5. **性能优化**: 避免在 `doWork` 中执行耗时操作
6. **NBT 保存**: 确保重要状态在 `saveAdditional` 和 `loadAdditional` 中正确保存

---

## 六、示例参考

- **压缩机**: `mio_icif_block_compressor_elc` / `mio_icif_compressor_elc`
- **采矿机**: `mio_icif_block_miner_elc` / `mio_icif_miner_elc`
- **高级采矿机**: `mio_icif_block_advanced_miner_elc` / `mio_icif_advanced_miner_elc`
