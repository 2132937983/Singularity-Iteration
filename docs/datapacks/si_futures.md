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

## 与交易所的关系（0.1.10.21 起）

- 数据包商品是交易所的“市场品目”：市场自己按价格买卖（买卖价差、每日收购额度），并只出售国库库存与少量加价进口。其他物品只能通过委托自动上市，成为只在玩家之间交易的“玩家上市品”。
- `base_price` 也用于校准货币与 UU 物质的换算：所有商品的“基础价格 ÷ UU 价值（桶）”取中位数，作为每桶 UU 物质的货币价值。玩家上市品的基本价值与价格区间（基本价值的 0.5～3 倍）由此计算。没有 UU 价值的商品不参与校准。
- 期货合约价格按商品自身的价格模型（`base_price`、`volatility`、`period_days`、当前事件）模拟得出。
- 带 `unlock` 的商品不会出现在市场的每日委托中。
