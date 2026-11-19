# Minecraft 模组方块模型与纹理创建指南

## 概述

本文档介绍如何在 NeoForge 1.21 模组中为方块创建模型和纹理，以 Industrial Craft in Future (mio_icif) 模组为例。

---

## 一、文件结构

```
src/main/resources/assets/mio_icif/
├── blockstates/          # 方块状态定义
│   └── producer/
│       └── block_[name].json
├── models/
│   ├── block/           # 方块模型
│   │   └── producer/
│   │       ├── block_[name].json      # 关闭状态
│   │       └── block_[name]_on.json   # 开启状态（可选）
│   └── item/            # 物品模型
│       └── producer/
│           └── block_[name].json
├── textures/
│   └── block/
│       └── producer/
│           └── block_[name].png       # 纹理文件
└── lang/                # 本地化文件
    ├── zh_cn.json
    └── en_us.json
```

---

## 二、BlockState 文件

**用途**: 定义方块在不同状态下的模型映射

**文件**: `blockstates/producer/block_[name].json`

### 示例：带朝向和工作状态的机器

```json
{
    "variants": {
        "lit=false,facing=north": {
            "model": "mio_icif:block/producer/block_[name]"
        },
        "lit=false,facing=south": {
            "model": "mio_icif:block/producer/block_[name]",
            "y": 180
        },
        "lit=false,facing=east": {
            "model": "mio_icif:block/producer/block_[name]",
            "y": 90
        },
        "lit=false,facing=west": {
            "model": "mio_icif:block/producer/block_[name]",
            "y": 270
        },
        "lit=true,facing=north": {
            "model": "mio_icif:block/producer/block_[name]_on"
        },
        "lit=true,facing=south": {
            "model": "mio_icif:block/producer/block_[name]_on",
            "y": 180
        },
        "lit=true,facing=east": {
            "model": "mio_icif:block/producer/block_[name]_on",
            "y": 90
        },
        "lit=true,facing=west": {
            "model": "mio_icif:block/producer/block_[name]_on",
            "y": 270
        }
    }
}
```

### 关键属性

| 属性 | 说明 |
|------|------|
| `lit` | 是否发光/工作（布尔值） |
| `facing` | 朝向（north/south/east/west） |
| `y` | Y轴旋转角度（90/180/270） |
| `model` | 引用的模型路径 |

---

## 三、方块模型文件

### 3.1 基础方块模型

**文件**: `models/block/producer/block_[name].json`

```json
{
    "parent": "block/cube",
    "textures": {
        "particle": "mio_icif:block/producer/block_[name]",
        "all": "mio_icif:block/producer/block_[name]"
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

### 3.2 开启状态模型

**文件**: `models/block/producer/block_[name]_on.json`

```json
{
    "parent": "block/cube",
    "textures": {
        "particle": "mio_icif:block/producer/block_[name]",
        "all": "mio_icif:block/producer/block_[name]"
    },
    "elements": [
        {
            "from": [ 0, 0, 0 ],
            "to": [ 16, 16, 16 ],
            "faces": {
                "down":  { "uv": [8, 0, 9.33, 16], "texture": "#particle", "cullface": "down" },
                "up":    { "uv": [9.33, 0, 10.66, 16], "texture": "#particle", "cullface": "up" },
                "south": { "uv": [14.66, 0, 16, 16], "texture": "#all", "cullface": "south" },
                "north": { "uv": [12, 0, 13.33, 16], "texture": "#all", "cullface": "north" },
                "west":  { "uv": [13.33, 0, 14.66, 16], "texture": "#all", "cullface": "west" },
                "east":  { "uv": [10.66, 0, 12, 16], "texture": "#all", "cullface": "east" }
            }
        }
    ]
}
```

### 3.3 UV 映射说明

IC2 风格机器纹理通常是一个 16x16（或 32x32）的图像，包含6个面的贴图：

```
纹理布局（16x16 像素 = 1个单位）：

[底面] [顶面] [东面] [北面] [西面] [南面] | [底面亮] [顶面亮] [东面亮] [北面亮] [西面亮] [南面亮]
 0-1.33 1.33-2.66 2.66-4 4-5.33 5.33-6.66 6.66-8   |   8-9.33 9.33-10.66 10.66-12 12-13.33 13.33-14.66 14.66-16

关闭状态使用左侧 UV（0-8）
开启状态使用右侧 UV（8-16）
```

| 面 | 关闭状态 UV | 开启状态 UV |
|----|------------|------------|
| down (底) | [0, 0, 1.33, 16] | [8, 0, 9.33, 16] |
| up (顶) | [1.33, 0, 2.66, 16] | [9.33, 0, 10.66, 16] |
| east (东) | [2.66, 0, 4, 16] | [10.66, 0, 12, 16] |
| north (北) | [4, 0, 5.33, 16] | [12, 0, 13.33, 16] |
| west (西) | [5.33, 0, 6.66, 16] | [13.33, 0, 14.66, 16] |
| south (南) | [6.66, 0, 8, 16] | [14.66, 0, 16, 16] |

### 3.4 模型属性说明

| 属性 | 说明 |
|------|------|
| `parent` | 父模型，通常使用 `block/cube` |
| `textures` | 纹理引用 |
| `particle` | 粒子效果纹理 |
| `elements` | 模型元素数组 |
| `from` / `to` | 元素的起始和结束坐标 [x, y, z] |
| `faces` | 六个面的定义 |
| `uv` | 纹理坐标 [u1, v1, u2, v2] |
| `cullface` | 剔除面（优化渲染） |
| `texture` | 引用 textures 中定义的键（#前缀） |

---

## 四、物品模型文件

**用途**: 定义方块在物品栏、手中、掉落时的显示

**文件**: `models/item/producer/block_[name].json`

```json
{
    "parent": "mio_icif:block/producer/block_[name]"
}
```

物品模型通常直接继承方块模型，使用相同的纹理和几何体。

---

## 五、纹理文件

**文件**: `textures/block/producer/block_[name].png`

### 5.1 纹理规格

| 属性 | 推荐值 |
|------|--------|
| 尺寸 | 16x16, 32x32, 64x64（2的幂次） |
| 格式 | PNG |
| 颜色模式 | RGB 或 RGBA（带透明） |
| 风格 | 与 IC2 原版保持一致 |

### 5.2 纹理布局示例

```
16x16 纹理（放大示意）：

+----+----+----+----+----+----+----+----+
| 底 | 顶 | 东 | 北 | 西 | 南 |底亮|顶亮|
+----+----+----+----+----+----+----+----+
| 东亮|北亮|西亮|南亮|    |    |    |    |
+----+----+----+----+----+----+----+----+

实际像素（16x16）：
- 每个面 16x16 像素
- 水平排列 6 个面（关闭状态）+ 6 个面（开启状态）
- 总宽度：16 * 12 = 192 像素（或按比例缩放）
```

### 5.3 纹理命名规范

| 文件 | 用途 |
|------|------|
| `block_[name].png` | 基础纹理（包含关闭和开启状态） |
| `block_[name]_top.png` | 顶面单独纹理（可选） |
| `block_[name]_front.png` | 正面单独纹理（可选） |
| `block_[name]_side.png` | 侧面单独纹理（可选） |

---

## 六、本地化文件

### 6.1 中文本地化

**文件**: `lang/zh_cn.json`

```json
{
    "block.mio_icif.block_[name]": "[中文名称]",
    "gui.mio_icif.[name].title": "[GUI标题中文]"
}
```

### 6.2 英文本地化

**文件**: `lang/en_us.json`

```json
{
    "block.mio_icif.block_[name]": "[English Name]",
    "gui.mio_icif.[name].title": "[GUI Title English]"
}
```

---

## 七、完整示例：高级采矿机

### 文件清单

```
blockstates/producer/block_advanced_miner_elc.json
models/block/producer/block_advanced_miner_elc.json
models/block/producer/block_advanced_miner_elc_on.json
models/item/producer/block_advanced_miner_elc.json
textures/block/producer/block_adv_miner_elc.png
```

### BlockState

```json
{
    "variants": {
        "lit=false,facing=north": {
            "model": "mio_icif:block/producer/block_advanced_miner_elc"
        },
        "lit=false,facing=south": {
            "model": "mio_icif:block/producer/block_advanced_miner_elc",
            "y": 180
        },
        "lit=false,facing=east": {
            "model": "mio_icif:block/producer/block_advanced_miner_elc",
            "y": 90
        },
        "lit=false,facing=west": {
            "model": "mio_icif:block/producer/block_advanced_miner_elc",
            "y": 270
        },
        "lit=true,facing=north": {
            "model": "mio_icif:block/producer/block_advanced_miner_elc_on"
        },
        "lit=true,facing=south": {
            "model": "mio_icif:block/producer/block_advanced_miner_elc_on",
            "y": 180
        },
        "lit=true,facing=east": {
            "model": "mio_icif:block/producer/block_advanced_miner_elc_on",
            "y": 90
        },
        "lit=true,facing=west": {
            "model": "mio_icif:block/producer/block_advanced_miner_elc_on",
            "y": 270
        }
    }
}
```

### 方块模型（关闭）

```json
{
    "parent": "block/cube",
    "textures": {
        "particle": "mio_icif:block/producer/block_adv_miner_elc",
        "all": "mio_icif:block/producer/block_adv_miner_elc"
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

### 方块模型（开启）

```json
{
    "parent": "block/cube",
    "textures": {
        "particle": "mio_icif:block/producer/block_adv_miner_elc",
        "all": "mio_icif:block/producer/block_adv_miner_elc"
    },
    "elements": [
        {
            "from": [ 0, 0, 0 ],
            "to": [ 16, 16, 16 ],
            "faces": {
                "down":  { "uv": [8, 0, 9.33, 16], "texture": "#particle", "cullface": "down" },
                "up":    { "uv": [9.33, 0, 10.66, 16], "texture": "#particle", "cullface": "up" },
                "south": { "uv": [14.66, 0, 16, 16], "texture": "#all", "cullface": "south" },
                "north": { "uv": [12, 0, 13.33, 16], "texture": "#all", "cullface": "north" },
                "west":  { "uv": [13.33, 0, 14.66, 16], "texture": "#all", "cullface": "west" },
                "east":  { "uv": [10.66, 0, 12, 16], "texture": "#all", "cullface": "east" }
            }
        }
    ]
}
```

### 物品模型

```json
{
    "parent": "mio_icif:block/producer/block_advanced_miner_elc"
}
```

### 本地化

```json
// zh_cn.json
{
    "block.mio_icif.block_advanced_miner_elc": "高级采矿机",
    "gui.mio_icif.advanced_miner_elc.title": "高级采矿机"
}

// en_us.json
{
    "block.mio_icif.block_advanced_miner_elc": "Advanced Miner",
    "gui.mio_icif.advanced_miner_elc.title": "Advanced Miner"
}
```

---

## 八、调试技巧

### 8.1 模型不显示

1. 检查文件路径是否正确
2. 检查 JSON 语法（缺少逗号、括号不匹配）
3. 检查纹理路径是否正确
4. 使用 F3+T 重新加载资源

### 8.2 纹理显示错误

1. 检查 UV 坐标是否正确
2. 检查纹理尺寸是否为 2 的幂次
3. 检查纹理路径是否正确

### 8.3 方块状态不工作

1. 检查 BlockState 属性名是否与代码中一致
2. 检查属性值是否正确（布尔值、枚举值）
3. 检查模型路径是否正确

---

## 九、参考资源

- [Minecraft Wiki: Model](https://minecraft.wiki/w/Model)
- [NeoForge Documentation](https://docs.neoforged.net/)
- IC2 原版纹理作为风格参考
