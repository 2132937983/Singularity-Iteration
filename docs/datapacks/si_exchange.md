# 交易所数据包 si_exchange（0.1.10.26 起）

整合包作者可以用数据包设定交易所的系统悬赏、奖励池、初始上市品与经济默认值：

```
data/<命名空间>/si_exchange/
  bounties/*.json      悬赏表（高级、特别悬赏，以及可选的普通悬赏）
  rewards/*.json       奖励池
  listings/*.json      开服即上市的玩家上市品
  economy.json         经济设置的默认值
  commodities/*.json   市场商品，格式与 si_futures 相同（在所有 si_futures 文件之后读取，可覆盖它们；可用 tag、accept_components）
```

模组自带的默认内容位于 `data/mio_icif/si_exchange/`：`bounties/advanced.json`（7 个高级悬赏）、`bounties/special.json`（4 个特别悬赏）、`rewards/normal.json`、`rewards/advanced.json`、`rewards/special.json`。修改后执行 `/reload` 生效。错误会写入日志，也可以用 `/mio_icif bounty errors` 查看。

## 悬赏的三个等级

| 等级 | 来源 | 发布 | 期限 | 奖励 |
|---|---|---|---|---|
| 普通 normal | 市场缺少的商品自动生成（无解锁进度、国库少于 `bountyTreasuryLow` 64 个），或数据包中 tier 为 normal 的表 | 每天保持 `bountyNormalMin`～`bountyNormalMax`（3～5）件 | 3 天 | 价值 × 1.15（`bountyNormalMultiplier`），奖励池 `mio_icif:normal` |
| 高级 advanced | 数据包表 | 每个交易周（7 天）`bountyAdvancedPerWeekMin`～`Max`（2～3）件，分散在一周内 | 7 天 | 价值 × 1.3，奖励池 `mio_icif:advanced`；多人游戏中最后一天之前一名玩家最多交付 `bountyAdvancedPlayerShare`（0.5）的价值 |
| 特别 special | 数据包表 | 每天 `bountySpecialChancePerDay`（5%）的概率，同时最多 `bountySpecialMaxOpen`（1）件 | 7 天 | 奖励池 `mio_icif:special`，默认没有货币 |

- **自动生成的普通委托**：价值 = `bountyNormalValue`（4,000）× 0.5～1.5，数量 = 价值 ÷ 市场卖价（四舍五入），必须在 `bountyNormalMinQuantity`～`bountyNormalMaxQuantity`（16～256）之内，否则不选该商品。
- **价值**：每个要求物品按发布时市场的卖价（价格 × (1 + 价差/2)）计价；不是商品的物品用表中的 `value`，没有时为 1 货币。价值决定按比例支付、按贡献分配与每名玩家份额。
- **市场不出售**：普通或高级悬赏仍需要某商品时，市场不出售该商品（奖励高于卖价，否则“从市场买来交付”就会凭空产生货币）。
- **交付**：在期货交易机的“悬赏”页按“交付”，交出物品栏主区域中该悬赏仍需要的普通物品。交付的商品进入国库；不是商品的物品进入**宝库**，之后可以作为奖励发出。单人游戏中所有悬赏都可以由玩家一人完成。
- **到期**：未支付的预留奖励（国库、宝库）退回原处；未支付的发行物品与货币不会产生。按贡献分配的悬赏在到期时按已交付的比例支付。
- 0.1.10.21～0.1.10.25 的“市场委托”不再发布；存档中已有的继续留在委托板上直到到期。配置 `systemCommissionsPerDay` 已删除。

## 悬赏表 bounties/*.json

一个文件可以是单个表，或 `{ "bounties": [ ... ] }`。

```json
{
  "bounties": [
    { "id": "mypack:circuits", "tier": "advanced", "weight": 10, "max_open": 1, "duration_days": 7, "cooldown_days": 7,
      "conditions": { "min_day": 7, "advancement": "minecraft:story/smelt_iron" },
      "requirements": [ { "item": "mio_icif:normal/item_circuit", "count": 32 },
                        { "item": "mio_icif:resource/item_rubber", "count": 64 } ],
      "reward": { "pool": "mypack:machine_parts", "rolls": 2, "multiplier": 1.3 },
      "split": "first", "player_share": 0.5 },
    { "id": "mypack:dragon_head", "tier": "special", "weight": 5, "cooldown_days": 14,
      "conditions": { "min_day": 30, "advancement": "minecraft:end/kill_dragon" },
      "requirements": [ { "item": "minecraft:dragon_head", "count": 1, "value": 20000 } ],
      "reward": { "pool": "mypack:ultimate_rewards" },
      "split": "contribution" }
  ]
}
```

| 字段 | 默认 | 说明 |
|---|---|---|
| `id` | `<命名空间>:<文件路径>`（列表中加 `#序号`） | 表 ID。后读到的同 ID 替换先前的；`"remove": true` 删除。 |
| `tier` | advanced | `normal` / `advanced` / `special`。 |
| `weight` | 10 | 同等级中被选中的权重；0 = 不自动发布（仍可用命令或 API 发布）。普通等级中，自动生成的委托合计权重为 100。 |
| `max_open` | 1 | 该表同时开放的数量上限。 |
| `duration_days` | 该等级的设置 | 开放天数（1～60）。 |
| `cooldown_days` | 0 | 上次发布后至少隔多少天才能再次发布。 |
| `conditions.min_day` | 0 | 交易日达到此值后才发布。 |
| `conditions.advancement` | 无 | 有在线玩家完成此进度时才发布。 |
| `requirements` | 必填 | 1～8 项：`item`、`count`、可选 `value`（每个的货币价值；不写时商品按市场卖价，其他物品为 1）。物品不能重复。 |
| `reward.pool` | 该等级的奖励池 | 奖励池 ID。不存在时支付货币（特别悬赏不发布）。 |
| `reward.rolls` | 1 | 抽取次数（1～4），每次的目标价值 = 价值 × 倍率 ÷ 次数。 |
| `reward.multiplier` | 该等级的倍率 | 奖励价值 = 要求的价值 × 倍率。 |
| `split` | first | `first`：每次交付立即按比例支付。`contribution`：完成或到期时按每名玩家交付的价值分配（最大余数法）。 |
| `player_share` | 该等级的设置 | 多人游戏中最后一天之前一名玩家最多交付的价值比例（0～1；可越过最后一个物品）。 |
| `optional` | false | `true` 时引用了未安装模组物品的表被静默跳过（否则报告为错误）。 |

## 奖励池 rewards/*.json

奖励池 ID 为 `<命名空间>:<文件名>`。数据包中同路径的文件替换默认奖励池（例如 `data/mio_icif/si_exchange/rewards/normal.json`）。

```json
{
  "entries": [
    { "type": "coins", "weight": 3 },
    { "type": "coins", "amount": 5000, "weight": 1 },
    { "type": "item", "item": "mio_icif:normal/item_circuit", "source": "treasury", "weight": 2 },
    { "type": "item", "item": "mio_icif:upgrade/overclocker_upgrade", "count": 4, "source": "issue", "weight": 1 },
    { "type": "item", "item": "minecraft:elytra", "count": 1, "source": "vault", "weight": 1 },
    { "type": "uu", "mb": 1000, "weight": 1 }
  ]
}
```

| 类型 / 来源 | 说明 |
|---|---|
| `coins` | 支付时发行的新货币。`amount` 为固定数额；不写时为该次抽取的目标价值（可用 `multiplier` 调整）。 |
| `item` + `treasury` | 市场国库的库存，**发布时预留**。`count` 不写时按目标价值 ÷ 市场卖价（四舍五入）计算。库存不足时不会被抽中。 |
| `item` + `vault` | 宝库中的稀有物品（此前悬赏收到的），发布时预留，需要 `count`。 |
| `item` + `issue` | 支付时制作，需要 `count`；发布时计入该等级的每周上限（`bountyIssuePerWeekNormal` 0 / `Advanced` 16 / `Special` 1 个）。 |
| `uu` | UU 物质（`mb`），支付时存入交易所账户；发布时计入每周上限 `bountyUuPerWeek`（4,000 mB）。 |

- 无法支付的条目（库存不足、超过每周上限、价值不足一个物品）不会被抽中。全部无法支付时：普通与高级悬赏改为支付货币（价值 × 倍率），特别悬赏不发布。
- 奖励永远不会是该悬赏自己要求的物品。
- `weight` 为 0 的条目不会被抽中；`optional: true` 的条目在物品不存在时被跳过。

## 初始上市品 listings/*.json

```json
{ "listings": [ { "item": "mio_icif:normal/item_super_lapotron_crystal", "currency": "uu", "price": 2000 },
                { "item": "minecraft:shulker_shell", "currency": "coin", "price": 900 } ] }
```

开服或 `/reload` 后，这些物品直接成为玩家上市品（只在玩家之间交易），无需委托证据，也不会因长期无成交而下市。`currency` 为 `coin` 或 `uu`（UU 物质 mB），`price` 为首个参考价。已是市场商品的物品被忽略。从数据包中删除后，该上市品变为普通上市品。

## 经济设置 economy.json

`data/<命名空间>/si_exchange/economy.json` 使用服务器配置 `FutureMarket` 分区的键名，例如：

```json
{ "sellDepthPlayer": 512, "sellDepthServer": 2048, "sellAllowancePerDay": 0, "bountyNormalValue": 6000,
  "bountySpecialChancePerDay": 0.1, "bountyIssuePerWeekSpecial": 2, "bountyAdvancedPool": "mypack:machine_parts" }
```

- 优先顺序：**服务器配置中修改过的值**（与默认值不同）＞ 数据包 ＞ 模组默认值。整合包作者用数据包发布默认值，服务器管理员仍可在配置文件中覆盖。
- 0.1.10.28 新增的键：`uuBoard`（货币与 UU 物质交易板）、`scrapThreshold`（废料回收的下限，0 = 不回收）、`stabilizerEnabled`、`stabilizerTargetPerPlayerWeek`、`stabilizerTargetPercentPerWeek`、`stabilizerStep`、`stabilizerMinFactor`（自动稳定器，见 `docs/releases/0.1.10.28/UPDATE.md` 第 4 节）。整合包作者可以在数据包中默认开启稳定器。
- 可用的键：`buyItemsPerDay`、`spread`、`sellDepthPlayer`、`sellDepthServer`、`sellExportPerDay`、`sellAllowancePerDay`、`sellAllowanceDays`、`sellAllowanceInSingleplayer`、`exchangeFee`、`importPerDay`、`importPremium`、`listingRequesters`、`listingFulfilled`、`listingMinPlayMinutes`、`uuDepth`、所有 `bounty*` 键，以及只能在数据包中设置的 `bountyNormalPool`、`bountyAdvancedPool`、`bountySpecialPool`。
- 未知的键、类型错误与超出范围的值会被报告并忽略。`/mio_icif bounty settings` 显示每个值及其来源（config / datapack / default）。

## 管理命令（权限等级 2）

| 命令 | 内容 |
|---|---|
| `/mio_icif bounty list` | 未结束的悬赏（要求、奖励及来源、剩余天数、分配方式）与本周的发行余量 |
| `/mio_icif bounty tables` | 已加载的悬赏表及现在能否发布 |
| `/mio_icif bounty post <表 ID>` | 立即发布该表（不检查权重、冷却与条件；奖励必须可支付），用于测试数据包 |
| `/mio_icif bounty cancel <编号>` | 取消悬赏，未支付的预留奖励退回 |
| `/mio_icif bounty errors` | 最近一次加载时 si_exchange 数据包的问题 |
| `/mio_icif bounty settings` | 经济设置的值与来源 |

## 商品的 tag 与 accept_components 在 0.1.10.27 的作用范围

除出售箱与每日集中清算外，交易终端的出售缓冲区（管道搬入）与大宗结算槽（容器中的商品）也按这两个字段收购，按本商品出售。挂单与悬赏交付仍只接受物品本身（普通物品）。

## Java API 与事件

见 API 指南第 22 节（`core.api.exchange`）：`ExchangeApi`（报价、国库、宝库、账本、邮箱货币的存取、发布与取消悬赏）以及事件 `MarketDayEvent`、`ExchangeTradeEvent`、`BountyPostingEvent`（可修改、可取消）、`BountyCompletedEvent`。KubeJS 或 FTB Quests 可以通过这些事件联动，奇点迭代不依赖它们。
