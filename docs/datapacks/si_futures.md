# 期货交易所数据包（0.1.7.32 起）

期货交易所的商品不再写在配置文件里，而是由数据包定义：

```
data/<命名空间>/si_futures/<任意名称>.json
```

模组自带的默认商品位于 `data/mio_icif/si_futures/`（mineral / agriculture / wood / food 四个文件）。

## 文件格式

```json
{
  "category": "mineral",
  "commodities": [
    { "item": "minecraft:diamond", "base_price": 800, "volatility": 0.22 },
    { "item": "minecraft:netherite_ingot", "base_price": 4000, "volatility": 0.30,
      "period_days": 3,
      "unlock": { "advancement": "minecraft:nether/obtain_ancient_debris" } },
    { "item": "minecraft:coal", "remove": true }
  ]
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `item` | 是 | 物品 ID。未安装的模组物品会被静默忽略。 |
| `base_price` | 是（`remove` 时除外） | 基础价格（硬币），必须大于 0。 |
| `volatility` | 否，默认 0.05 | 每次调价的波动率，0～1。 |
| `category` | 否 | `mineral` / `agriculture` / `wood` / `food` / `other`；不写时使用文件顶层的 `category`。 |
| `period_days` | 否，默认 1 | 每隔多少游戏日调价一次（1～3650）。 |
| `unlock.advancement` | 否 | 玩家完成该进度后才能交易；未解锁的商品在界面中显示为灰色，服务器拒绝交易。 |
| `remove` | 否 | `true` 时移除此前文件中同一物品的条目（例如去掉默认商品）。 |

文件也可以只写单个条目对象（不带 `commodities`）。

## 覆盖规则

所有文件按资源位置顺序读取；同一物品后读到的条目覆盖先前的条目，因此数据包可以直接覆盖默认商品的价格与参数。修改后执行 `/reload` 即可生效，并自动同步到所有在线玩家。

旧配置项 `FutureMarket.commodities` 已删除，其余机器参数（每日交易上限、能量等）仍在配置文件中。
