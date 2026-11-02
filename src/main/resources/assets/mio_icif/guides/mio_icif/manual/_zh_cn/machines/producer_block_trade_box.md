---
navigation:
  title: "交易箱"
  icon: mio_icif:producer/block_trade_box
  parent: machines.md
  position: 29
item_ids:
  - mio_icif:producer/block_trade_box
  - mio_icif:producer/block_trade_box_advanced
  - mio_icif:normal/item_trade_box_upgrade
---

# 交易箱

<Row>
  <BlockImage id="mio_icif:producer/block_trade_box" scale="3" />
  <BlockImage id="mio_icif:producer/block_trade_box_advanced" scale="3" />
</Row>

## 功能

交易箱在其区块加载期间为所有者（放置它的玩家）出售物品，不需要能量。
每笔出售都属于所有者：使用所有者的卖出深度、每日额度、解锁与记录，货币进入所有者的货币账户。只有所有者和管理员可以打开。

### 基础交易箱

- **27 格**：漏斗和管道放入商品，也包括商品标签中的物品。管道无法取出物品。
- **每日拍卖**：每 10 秒将其中的商品按箱子的最低价（出价的百分比，初始 50%）加入所有者的每日拍卖订单。拍卖未售出的部分与其他拍卖订单一样进入所有者的邮箱。
- 用箱子、绿宝石、铁锭和红石合成，不需要电路板。

### 高级交易箱

- **54 格**、管道可以取出的 **9 个退回格**和 9 格**过滤**：手持物品点击过滤格，只接受该商品；过滤为空时接受所有商品，空手点击清除一格。过滤不接受的物品移到退回格。
- **模式**：拍卖（每日拍卖）、立即（按所有者的卖出深度立即出售）或其余回收（立即出售，市场不再收购的部分送入废料回收，废料从退回格取出）。最低价为出价的百分比，立即出售时也可以设为每件的货币数（c / % 按钮）。
- **所有者离线时**：立即出售与交易终端相同，只在所有者在线时进行，并以 64 件为一批（箱子装满时除外）。开启“离线：拍卖”后，所有者不在线时箱子改用每日拍卖。
- **悬赏**：开启“交付悬赏”后，箱子先向所有者已接取的悬赏（悬赏页的“接取”）交付，受所有者份额限制。奖励进入所有者的货币账户、UU 账户与邮箱。
- **升级套件**：所有者对已放置的基础交易箱使用后，它连同内容与设置变为高级交易箱。

### 两种箱子

- 界面显示本市场日的结果（立即售出的数量与货币、加入拍卖的数量、回收的数量、交付给悬赏的数量）与上次运行的状态。
- 拆除箱子时内容进入所有者的邮箱；带名称、附魔或耐久损耗的物品会掉落。
- 自 0.1.10.42 起，交易终端的配方用高级交易箱代替箱子。

## 配方

<RecipesFor id="mio_icif:producer/block_trade_box" fallbackText="-" />

<RecipesFor id="mio_icif:producer/block_trade_box_advanced" fallbackText="-" />

<RecipesFor id="mio_icif:normal/item_trade_box_upgrade" fallbackText="-" />
