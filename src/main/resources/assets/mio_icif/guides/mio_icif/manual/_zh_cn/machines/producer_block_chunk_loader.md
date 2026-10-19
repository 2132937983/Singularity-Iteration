---
navigation:
  title: "区块加载器"
  icon: mio_icif:producer/block_chunk_loader
  parent: machines.md
  position: 16
item_ids:
  - mio_icif:producer/block_chunk_loader
---

# 区块加载器

<Row>
  <BlockImage id="mio_icif:producer/block_chunk_loader" scale="3" />
</Row>

## 功能

区块加载器使选定的区块保持加载。机器在周围9×9区块范围内最多加载25个区块。
每个加载的区块消耗EU。EU耗尽时机器停止。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | MV (128 EU) |
| 最大输入 | 128 EU/t |
| 储能 | 2,500 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将MV导线连接到机器。
2. 打开界面，点击要加载的区块。
3. 为机器保持稳定的EU供应。

## 注意

- 区块加载器所在的区块也在选择网格中。

> **警告：** 电压高于MV时，机器因过压爆炸。安装变压升级可提高电压等级。

## 服务器规则

放置加载器的玩家即为所有者。服务器可在配置的 `ChunkLoaders` 节中限制加载器：

| 设置 | 默认值 | 效果 |
|---|---|---|
| `enabled` | true | false：所有加载器释放区块 |
| `maxChunksPerLoader` | 25 | 每个加载器的区块数 |
| `maxChunksPerPlayer` | 0（不限） | 同一玩家所有加载器合计的区块数 |
| `maxLoadersPerPlayer` | 0（不限） | 每名玩家同时工作的加载器数 |
| `offlineOwnerMinutes` | -1（一直工作） | 所有者下线后加载器继续工作的分钟数 |
| `euPerChunkPerTick` | 1.0 | 每个区块每 tick 的 EU |
| `randomTicks` | true | false：已加载区块不进行随机刻和自然生成，机器照常运行 |
| `ownerOnlyEdit` | false | 仅所有者和管理员可修改选择 |

- 超出区块上限时，优先保留自身区块，其次是最近的已选区块。
- 地图上方的红色横条说明加载器为何没有加载（全部）区块。将鼠标移到区块数上可查看详情。
- 因规则停止的加载器会在原因消除后自动恢复，例如所有者上线。
- 管理员：`/mio_icif chunkloaders` 列出所有加载器；`disable` 与 `enable` 停用或恢复单个加载器；`disable_all` 停止全部。


## 配方

<RecipesFor id="mio_icif:producer/block_chunk_loader" fallbackText="-" />
