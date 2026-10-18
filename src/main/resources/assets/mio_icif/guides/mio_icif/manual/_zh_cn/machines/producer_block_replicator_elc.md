---
navigation:
  title: "复制机"
  icon: mio_icif:producer/block_replicator_elc
  parent: machines.md
  position: 57
item_ids:
  - mio_icif:producer/block_replicator_elc
---

# 复制机

<Row>
  <BlockImage id="mio_icif:producer/block_replicator_elc" scale="3" />
</Row>

## 功能

复制机消耗UU物质和EU，按存储的模式复制物品。
模式来自存储槽位中的模式存储水晶，或相邻的模式存储机。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | EV (2,048 EU) |
| 最大输入 | 8,192 EU/t |
| 储能 | 2,000,000 EU |
| 工作耗电 | 512 EU/t |
| 单次工作时间 | 100 tick (5 s) |
| 单次耗电 | 51,200 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 通过变压器将EV电源连接到机器。
2. 用UU物质单元或管道供应UU物质。
3. 把模式存储水晶放入存储槽位，或在机器旁放置模式存储机。
4. 在界面中点击单次或循环开始工作。
5. 从输出槽位取出产物。

## 可复制物品与数据包

- 复制机按图样（先扫描该物品）制造具有 UU 价格的物品。
- 价格来源：IC2 参照表、通向该物品的配方（合成或熔炼得到的物品继承材料的价格）、复制清单。
- 默认清单新增 95 种物品：新版矿石与粗矿（铜、深层矿石、远古残骸）、下界石英、紫水晶、新石材、生物掉落物（恶魂之泪、潜影壳、海晶碎片、幻翼膜、墨囊、鹦鹉螺壳、下界之星、生肉与鱼）、植物（藤蔓、下界疣、竹子、海带、浆果、紫颂果），以及本模组的粗矿与粘性树脂。龙蛋永远不可复制。
- 数据包可添加自己的清单：在其数据命名空间的 uu_replication 文件夹中放 JSON 文件（data/（命名空间）/uu_replication/（名称）.json）。放在 data/mio_icif/uu_replication/defaults.json 的文件会替换默认清单。
- 数组 "entries" 的每一项包含 "item"（物品 ID）或 "tag"（物品标签），以及 "uu"（每件所需 UU 物质 mB）。可选："eu"（每件 EU，标准为每 mB 5,120 EU）、"deny": true（不可复制）、"required": false（未知 ID 静默跳过）。
- 条目示例：`{ "item": "minecraft:netherite_scrap", "uu": 1500, "eu": 2000000 }`
- 文件按 ID 顺序加载，同一物品以后加载的条目为准。错误条目会在服务器日志中警告并跳过，其余照常加载。日志行 "[UU] Replication lists" 给出数量。

## 注意

- 红石信号使机器停止。红石信号反转升级可反转此行为。

> **警告：** 电压高于EV时，机器因过压爆炸。安装变压升级可提高电压等级。


## 配方

<RecipesFor id="mio_icif:producer/block_replicator_elc" fallbackText="-" />
