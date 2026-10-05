---
navigation:
  title: "期货机"
  icon: mio_icif:producer/block_future_elc
  parent: machines.md
  position: 29
item_ids:
  - mio_icif:producer/block_future_elc
---

# 期货机

<Row>
  <BlockImage id="mio_icif:producer/block_future_elc" scale="3" />
</Row>

## 功能

期货机以每天变化的价格，用工业货币买卖商品。
每次交易消耗EU。机器从你的背包取出物品和货币。每天有交易上限。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输入电压等级 | LV (32 EU) |
| 最大输入 | 100 EU/t |
| 储能 | 10,000 EU |

这些数值由本版本的方块实体实测得出。

## 使用步骤

1. 将LV导线连接到机器。
2. 打开界面，选择类别和商品。
3. 设置交易数量。
4. 点击购买或出售。

## 注意

- 市场事件会在数天内改变价格走势。
- 部分商品在获得界面所示进度前保持锁定。

## 配方

<RecipesFor id="mio_icif:producer/block_future_elc" fallbackText="-" />
