# Advanced Solar Panels (ASP) 复原实施计划

## 概述

本文档详细规划如何将 IC2 附属 Advanced Solar Panels (高级太阳能板) 的源代码整合到我们的 mod 中。

---

## 第一阶段：基础架构搭建

### 1.1 创建基础包结构
```
com.singularity_iteration.mio_icif.addons.advanced_solar_panels/
├── AdvancedSolarPanels.java          # 主类
├── Configs.java                      # 配置文件
├── Recipes.java                      # 配方注册
├── blocks/
│   └── BlockAdvancedSolarPanels.java # 机器方块
├── items/
│   ├── ItemCraftingThings.java       # 合成材料
│   └── ItemArmourSolarHelmet.java    # 太阳能头盔
├── tiles/
│   ├── TileEntitySolarPanel.java     # 基础太阳能板实体
│   ├── TileEntityAdvancedSolar.java  # 高级太阳能板
│   ├── TileEntityHybridSolar.java    # 混合太阳能板
│   ├── TileEntityUltimateHybridSolar.java # 终极混合太阳能板
│   ├── TileEntityQuantumSolar.java   # 量子太阳能板
│   ├── TileEntityQuantumGenerator.java # 量子发电机
│   └── TileEntityMolecularAssembler.java # 分子重组仪
├── gui/
│   ├── BackgroundlessDynamicGUI.java # 无边框动态GUI
│   ├── TransparentDynamicGUI.java    # 透明动态GUI
│   └── ProgressBars.java             # 进度条样式
├── slots/
│   └── InvSlotMultiCharge.java       # 多槽位充电
├── network/
│   └── MolecularTransformerPacket.java # 分子重组仪网络包
└── renders/
    ├── PrettyMolecularTransformerModel.java
    └── PrettyMolecularTransformerTESR.java
```

### 1.2 创建方块注册类
**文件**: `AdvancedSolarPanels.java`
- 注册所有机器方块
- 注册 TileEntity
- 初始化配置

### 1.3 创建配置系统
**文件**: `Configs.java`
- 太阳能板发电配置
- 分子重组仪配置
- 量子发电机配置

---

## 第二阶段：方块与物品实现

### 2.1 机器方块
**文件**: `BlockAdvancedSolarPanels.java`

需要实现的机器类型：
1. **advanced_solar_panel** - 高级太阳能板
   - 白天发电: 8 EU/t
   - 夜晚发电: 1 EU/t
   - 存储: 32000 EU
   - 等级: 1

2. **hybrid_solar_panel** - 混合太阳能板
   - 白天发电: 64 EU/t
   - 夜晚发电: 8 EU/t
   - 存储: 100000 EU
   - 等级: 2

3. **ultimate_solar_panel** - 终极混合太阳能板
   - 白天发电: 512 EU/t
   - 夜晚发电: 64 EU/t
   - 存储: 1000000 EU
   - 等级: 3

4. **quantum_solar_panel** - 量子太阳能板
   - 白天发电: 4096 EU/t
   - 夜晚发电: 2048 EU/t
   - 存储: 10000000 EU
   - 等级: 4

5. **quantum_generator** - 量子发电机
   - 可配置输出
   - 无限能源
   - 创造模式物品

6. **molecular_transformer** - 分子重组仪
   - 用于转换材料
   - 需要 EU 能源

### 2.2 合成材料物品
**文件**: `ItemCraftingThings.java`

需要添加的材料：
- `SUNNARIUM_PART` - 阳光化合物碎片
- `SUNNARIUM` - 阳光化合物
- `ENRICHED_SUNNARIUM` - 富集阳光化合物
- `SUNNARIUM_ALLOY` - 阳光合金
- `ENRICHED_SUNNARIUM_ALLOY` - 富集阳光合金
- `IRIDIUM_INGOT` - 铱锭
- `URANIUM_INGOT` - 铀锭
- `IRIDIUM_IRON_PLATE` - 铱铁板
- `REINFORCED_IRIDIUM_IRON_PLATE` - 强化铱铁板
- `IRRADIANT_URANIUM` - 辐射铀
- `IRRADIANT_GLASS_PANE` - 辐射玻璃板
- `IRRADIANT_REINFORCED_PLATE` - 辐射强化板
- `MT_CORE` - 分子重组核心
- `QUANTUM_CORE` - 量子核心

### 2.3 太阳能头盔
**文件**: `ItemArmourSolarHelmet.java`

需要实现的头盔类型：
- **Advanced Solar Helmet** - 高级太阳能头盔
- **Hybrid Solar Helmet** - 混合太阳能头盔
- **Ultimate Solar Helmet** - 终极太阳能头盔

功能：
- 穿戴时给装备充电
- 提供护甲保护
- 太阳能发电功能

---

## 第三阶段：TileEntity 实现

### 3.1 基础太阳能板实体
**文件**: `TileEntitySolarPanel.java`

核心功能：
```java
public abstract class TileEntitySolarPanel extends TileEntityInventory 
    implements IEnergySource, IHasGui, IGuiValueProvider {
    
    // 配置类
    public static final class SolarConfig {
        public final int dayPower;      // 白天发电量
        public final int nightPower;    // 夜晚发电量
        final int storage;              // 最大存储
        final int tier;                 // 输出等级
    }
    
    // 发电状态
    public enum GenerationState { NONE, NIGHT, DAY }
    
    // 核心方法
    protected void updateEntityServer()  // 每 tick 更新
    public void checkTheSky()            // 检查天空状态
    public void tryGenerateEnergy(int amount)  // 尝试发电
    
    // IEnergySource 接口
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing side)
    public int getSourceTier()
    public double getOfferedEnergy()
    public void drawEnergy(double amount)
}
```

### 3.2 各等级太阳能板
**文件**: 
- `TileEntityAdvancedSolar.java`
- `TileEntityHybridSolar.java`
- `TileEntityUltimateHybridSolar.java`
- `TileEntityQuantumSolar.java`

每个类只需设置对应的 `SolarConfig`：
```java
public class TileEntityAdvancedSolar extends TileEntitySolarPanel {
    public static TileEntitySolarPanel.SolarConfig settings = 
        new SolarConfig(8, 1, 32000, 1);
    
    public TileEntityAdvancedSolar() {
        super(settings);
    }
}
```

### 3.3 量子发电机
**文件**: `TileEntityQuantumGenerator.java`

功能：
- 创造模式无限能源
- 可配置输出电量
- 可配置输出等级

### 3.4 分子重组仪
**文件**: `TileEntityMolecularAssembler.java`

功能：
- 消耗 EU 转换物品
- 支持自定义配方
- 有工作进度显示

核心组件：
```java
public class TileEntityMolecularAssembler extends TileEntityInventory {
    private InvSlotProcessable inputSlot;   // 输入槽
    private InvSlotOutput outputSlot;       // 输出槽
    private int progress;                   // 进度
    private int maxProgress;                // 最大进度
    
    // 配方管理
    public static class MTRecipe {
        ItemStack input;
        ItemStack output;
        int energy;  // 所需 EU
    }
}
```

---

## 第四阶段：GUI 实现

### 4.1 太阳能板 GUI
**文件**: `BackgroundlessDynamicGUI.java`

需要显示的信息：
- 能量存储进度条
- 当前发电状态（白天/夜晚/无）
- 发电量显示
- 最大输出显示

### 4.2 分子重组仪 GUI
**文件**: `TransparentDynamicGUI.java`

需要显示的信息：
- 输入槽
- 输出槽
- 工作进度条
- 能量存储

### 4.3 量子发电机 GUI
- 输出配置滑块
- 等级选择

---

## 第五阶段：配方系统

### 5.1 合成配方
**文件**: `Recipes.java`

需要添加的配方：
1. 高级太阳能板
2. 混合太阳能板
3. 终极混合太阳能板
4. 量子太阳能板
5. 分子重组仪
6. 量子发电机
7. 各种合成材料

### 5.2 分子重组仪配方
**文件**: `MolecularTransformerRecipeManager.java`

需要添加的转换配方：
- 铁锭 → 铱锭
- 钻石 → 强化铱铁板
- 青金石 → 辐射铀
- 玻璃 → 辐射玻璃板
- 等等...

---

## 第六阶段：资源文件

### 6.1 模型文件
```
assets/mio_icif/models/block/
├── advanced_solar_panel.json
├── hybrid_solar_panel.json
├── ultimate_solar_panel.json
├── quantum_solar_panel.json
├── quantum_generator.json
└── molecular_transformer.json
```

### 6.2 纹理文件
```
assets/mio_icif/textures/
├── blocks/
│   ├── advanced_solar_panel_top.png
│   ├── advanced_solar_panel_side.png
│   ├── advanced_solar_panel_bottom.png
│   ├── hybrid_solar_panel_top.png
│   ├── ... (其他太阳能板纹理)
│   ├── quantum_generator.png
│   ├── quantum_generator_active.png
│   └── molecular_transformer.png
├── gui/
│   ├── advanced_solar_panel.png
│   ├── molecular_transformer.png
│   └── quantum_generator.png
├── items/
│   ├── sunnarium.png
│   ├── sunnarium_alloy.png
│   ├── iridium_ingot.png
│   ├── ... (其他材料纹理)
│   └── advanced_solar_helmet.png
└── armour/
    ├── advanced_solar_helmet.png
    ├── hybrid_solar_helmet.png
    └── ultimate_solar_helmet.png
```

### 6.3 语言文件
```json
{
  "block.mio_icif.advanced_solar_panel": "高级太阳能板",
  "block.mio_icif.hybrid_solar_panel": "混合太阳能板",
  "block.mio_icif.ultimate_solar_panel": "终极混合太阳能板",
  "block.mio_icif.quantum_solar_panel": "量子太阳能板",
  "block.mio_icif.quantum_generator": "量子发电机",
  "block.mio_icif.molecular_transformer": "分子重组仪",
  
  "item.mio_icif.sunnarium": "阳光化合物",
  "item.mio_icif.sunnarium_alloy": "阳光合金",
  "item.mio_icif.iridium_ingot": "铱锭",
  "item.mio_icif.iridium_iron_plate": "铱铁板",
  "item.mio_icif.reinforced_iridium_iron_plate": "强化铱铁板",
  "item.mio_icif.irradiant_uranium": "辐射铀",
  "item.mio_icif.irradiant_glass_pane": "辐射玻璃板",
  "item.mio_icif.mt_core": "分子重组核心",
  "item.mio_icif.quantum_core": "量子核心",
  
  "item.mio_icif.advanced_solar_helmet": "高级太阳能头盔",
  "item.mio_icif.hybrid_solar_helmet": "混合太阳能头盔",
  "item.mio_icif.ultimate_solar_helmet": "终极太阳能头盔",
  
  "gui.mio_icif.solar_panel.generating": "发电中",
  "gui.mio_icif.solar_panel.max_output": "最大输出",
  "gui.mio_icif.solar_panel.sunlight": "阳光充足",
  "gui.mio_icif.solar_panel.moonlight": "月光充足"
}
```

---

## 第七阶段：网络同步

### 7.1 分子重组仪进度同步
**文件**: `MolecularTransformerPacket.java`

需要同步的数据：
- 工作进度
- 输入物品
- 输出物品

---

## 第八阶段：渲染效果

### 8.1 分子重组仪渲染
**文件**: 
- `PrettyMolecularTransformerModel.java`
- `PrettyMolecularTransformerTESR.java`

效果：
- 工作时的粒子效果
- 能量流动画
- 旋转核心

---

## 实施优先级

### 高优先级（核心功能）
1. [ ] 基础方块注册
2. [ ] TileEntitySolarPanel 基础类
3. [ ] 四种太阳能板实现
4. [ ] 基础 GUI
5. [ ] 基础配方

### 中优先级（功能完善）
6. [ ] 分子重组仪
7. [ ] 分子重组仪配方
8. [ ] 合成材料物品
9. [ ] 太阳能头盔
10. [ ] 完整配方系统

### 低优先级（视觉效果）
11. [ ] 量子发电机
12. [ ] 高级 GUI 效果
13. [ ] 分子重组仪渲染
14. [ ] 粒子效果

---

## 注意事项

### 版本兼容性
- ASP 源代码基于 Minecraft 1.12.2 和 IC2 2.8.74
- 需要适配到 Minecraft 1.21.1 和 IC2 2.9.x
- NeoForge API 有较大变化，需要调整

### 关键适配点
1. **方块注册**: 从 `TeBlockRegistry` 改为 NeoForge 的 `DeferredRegister`
2. **TileEntity 注册**: 使用 NeoForge 的 `RegisterEvent`
3. **能量系统**: IC2 的 EU 系统 API 可能有变化
4. **GUI 系统**: 从 `DynamicContainer` 改为 NeoForge 的 `MenuType`
5. **网络包**: 从 `IC2Packet` 改为 NeoForge 的 `SimpleChannel`
6. **配方系统**: 从自定义配方改为 NeoForge 的 `RecipeType`

### 依赖关系
- 需要 IC2 作为前置 mod
- 需要 NeoForge 21.1.x
- 可选: JEI 用于配方显示

---

## 参考文件

### ASP 源代码位置
```
source/ASP/
├── com/chocohead/advsolar/
│   ├── AdvancedSolarPanels.java
│   ├── ASP_Items.java
│   ├── Configs.java
│   ├── Recipes.java
│   ├── blocks/
│   ├── gui/
│   ├── items/
│   ├── renders/
│   ├── slots/
│   └── tiles/
└── assets/advanced_solar_panels/
```

### 我们的实现位置
```
src/main/java/com/singularity_iteration/mio_icif/addons/advanced_solar_panels/
src/main/resources/assets/mio_icif/
```

---

## 下一步行动

1. 创建基础包结构和主类
2. 实现第一个太阳能板（高级太阳能板）作为原型
3. 测试与 IC2 的兼容性
4. 逐步添加其他机器和功能