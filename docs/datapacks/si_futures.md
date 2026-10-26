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
| `category` | 否 | `mineral` / `agriculture` / `wood` / `food` / `industrial`（0.1.10.25）/ `other`；不写时使用文件顶层的 `category`。 |
| `period_days` | 否，默认 1 | 每隔多少游戏日调价一次（1～3650）。 |
| `unlock.advancement` | 否 | 玩家完成该进度后才能交易；未解锁的商品在界面中显示为灰色，服务器拒绝交易。 |
| `remove` | 否 | `true` 时移除此前文件中同一物品的条目（例如去掉默认商品）。 |
| `depth.player` / `depth.server` | 否（0.1.10.25） | 该商品的卖出深度（个），不写时用配置 `sellDepthPlayer`（256）/ `sellDepthServer`（1024）。数量少、价格高的商品可以设得更小。 |
| `export_per_day` | 否（0.1.10.25） | 每天转出的卖压比例（0.05～1），不写时用配置 `sellExportPerDay`（0.5）。 |
| `import_per_day` | 否（0.1.10.25） | 国库为空时每天的进口数量，不写时用配置 `importPerDay`（64）。经加工可以倍增的原料（例如粗矿：粉碎后得到 2 个锭）应设为 0，否则市场进口会成为货币来源。 |
| `import_premium` | 否（0.1.10.25） | 进口的加价（0～2），不写时用配置 `importPremium`（0.1）。 |

文件也可以只写单个条目对象（不带 `commodities`）。

## 覆盖规则

所有文件按资源位置顺序读取；同一物品后读到的条目覆盖先前的条目，因此数据包可以直接覆盖默认商品的价格与参数。修改后执行 `/reload` 即可生效，并自动同步到所有在线玩家。

旧配置项 `FutureMarket.commodities` 已删除，其余机器参数（能量、卖出深度、额度等）仍在配置文件中。

## 与交易所的关系（0.1.10.21 起）

- 数据包商品是交易所的“市场品目”：市场自己按价格买卖（买卖价差；0.1.10.25 起卖给市场按卖出深度曲线计价，数量不限），并只出售国库库存与少量加价进口。其他物品只能通过委托自动上市，成为只在玩家之间交易的“玩家上市品”。
- `base_price` 也用于校准货币与 UU 物质的换算：所有商品的“基础价格 ÷ UU 价值（桶）”取中位数，作为每桶 UU 物质的货币价值。玩家上市品的基本价值与价格区间（基本价值的 0.5～3 倍）由此计算。没有 UU 价值的商品不参与校准。
- 期货合约价格按商品自身的价格模型（`base_price`、`volatility`、`period_days`、当前事件）模拟得出。
- 带 `unlock` 的商品不会出现在市场的每日委托中。

## 卖出深度与套利检查（0.1.10.25 起）

- 卖给市场的价格：`价格 × (1 − 价差/2) × exp(−玩家今天已卖数量 / depth.player) × exp(−全体未转出的卖压 / depth.server)`。一名玩家一天内一个商品最多获得 `价格 × 0.97 / (1/depth.player + 1/depth.server)` 货币；所有玩家（任意数量的账号）首日合计最多 `价格 × 0.97 × depth.server`，此后每天约为其四分之一（`export_per_day` 为 0.5 时）。
- 每日集中清算：所有人当天交给集中清算的物品在交易日更替时一起出售（同时、按各自数量的比例），先后顺序无关；单独一人时与立即出售的所得相同。
- 添加或调整商品后，请在游戏中执行 `/mio_icif market arbitrage`：它按服务器的全部配方查找“从市场买入、加工、卖回市场即可获利”的途径，以及会使物品增殖的配方循环。能量（EU）不计价，输出中给出每货币利润所需的 EU，便于区分大量耗能的生产与价格漏洞。`/mio_icif market simulate <物品> <数量>` 可以预览一次卖出的所得。
