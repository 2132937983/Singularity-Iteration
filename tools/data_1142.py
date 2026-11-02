#!/usr/bin/env python3
"""0.1.10.42: the resources and texts of the release.
- The Trade Boxes: blockstates, block and item models, loot tables, recipes (basic box, advanced box, upgrade kit), block
  tags; the Trade Terminal's recipe takes an Advanced Trade Box instead of a chest; the guide page in en / zh / ja.
- The guide (en / zh / ja): the futures limits, the shop upkeep period, the wealth ranking, the new settings (Futures Machine
  page); the Trade Box line of the Trade Terminal page.
- The datapack documents (zh): the new economy keys and commands (si_exchange.md), the futures limits (si_futures.md).
Each text block goes after its anchor line (the line that starts with the anchor text). Idempotent; keeps the line endings of
edited files. With --check it only reports what is missing or different.
Usage: data_1142.py <tree> [--check]"""
import json
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
RES = os.path.join(TREE, 'src/main/resources')
BASIC, ADVANCED = 'mio_icif:producer/block_trade_box', 'mio_icif:producer/block_trade_box_advanced'
KIT = 'mio_icif:normal/item_trade_box_upgrade'
missing = []


def write(rel, text):
    path = os.path.join(RES, rel)
    if CHECK:
        if not os.path.exists(path) or open(path, encoding='utf-8').read() != text:
            missing.append(rel)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w', encoding='utf-8', newline='\n').write(text)


def js(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False) + '\n'


# ---------------------------------------------------------------- the two boxes
face = lambda u0, u1, cull: {'uv': [u0, 0, u1, 16], 'texture': '#all_lbr', 'cullface': cull}
for name, particle in (('block_trade_box', 'minecraft:block/spruce_planks'), ('block_trade_box_advanced', 'mio_icif:block/producer/block_machine')):
    write(f'assets/mio_icif/blockstates/producer/{name}.json', js({'variants': {
        'facing=north': {'model': f'mio_icif:block/producer/{name}'},
        'facing=south': {'model': f'mio_icif:block/producer/{name}', 'y': 180},
        'facing=east': {'model': f'mio_icif:block/producer/{name}', 'y': 90},
        'facing=west': {'model': f'mio_icif:block/producer/{name}', 'y': 270}}}))
    write(f'assets/mio_icif/models/block/producer/{name}.json', js({
        'parent': 'block/cube',
        'textures': {'particle': particle, 'all_lbr': f'mio_icif:block/producer/{name}'},
        'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {
            'down': face(0, 2.66, 'down'), 'up': face(2.66, 5.33, 'up'), 'south': face(13.33, 16, 'south'),
            'north': face(8, 10.66, 'north'), 'west': face(10.66, 13.33, 'west'), 'east': face(5.33, 8, 'east')}}]}))
    write(f'assets/mio_icif/models/item/producer/{name}.json', js({'parent': f'mio_icif:block/producer/{name}'}))
    write(f'data/mio_icif/loot_table/blocks/producer/{name}.json', js({
        'type': 'minecraft:block',
        'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                   'entries': [{'type': 'minecraft:item', 'name': f'mio_icif:producer/{name}'}], 'rolls': 1.0}],
        'random_sequence': f'mio_icif:blocks/producer/{name}'}))
write('assets/mio_icif/models/item/normal/item_trade_box_upgrade.json', js({
    'parent': 'minecraft:item/generated', 'textures': {'layer0': 'mio_icif:item/normal/item_trade_box_upgrade'}}))

UU_CELL = {'type': 'mio_icif:fluid_cell', 'fluid': 'mio_icif:uumatter'}
write('data/mio_icif/recipe/shaped_360_trade_box.json', js({
    'type': 'minecraft:crafting_shaped',
    'pattern': ['IEI', 'RCR', 'III'],
    'key': {'I': {'tag': 'c:ingots/iron'}, 'E': {'item': 'minecraft:emerald'}, 'R': {'item': 'minecraft:redstone'},
            'C': {'item': 'minecraft:chest'}},
    'result': {'id': BASIC, 'count': 1}}))
write('data/mio_icif/recipe/shaped_361_trade_box_advanced.json', js({
    'type': 'minecraft:crafting_shaped',
    'pattern': ['AUA', ' B '],
    'key': {'A': {'tag': 'c:circuits/advanced'}, 'U': UU_CELL, 'B': {'item': BASIC}},
    'result': {'id': ADVANCED, 'count': 1}}))
write('data/mio_icif/recipe/shaped_362_trade_box_upgrade.json', js({
    'type': 'minecraft:crafting_shaped',
    'pattern': ['AUA', ' I '],
    'key': {'A': {'tag': 'c:circuits/advanced'}, 'U': UU_CELL, 'I': {'tag': 'c:ingots/iron'}},
    'result': {'id': KIT, 'count': 1}}))

# the Trade Terminal: an Advanced Trade Box instead of the chest (the other keys stay as they are)
TERMINAL = 'data/mio_icif/recipe/shaped_359_trade_terminal.json'
path = os.path.join(RES, TERMINAL)
recipe = json.loads(open(path, encoding='utf-8').read())
if recipe['key']['H'] != {'item': ADVANCED}:
    if CHECK:
        missing.append(TERMINAL)
    else:
        recipe['key']['H'] = {'item': ADVANCED}
        open(path, 'w', encoding='utf-8', newline='\n').write(js(recipe))

# block tags: the basic box for the axe, the advanced box for the pickaxe, both in the producer tag
for tag, ids in (('data/minecraft/tags/block/mineable/pickaxe.json', (ADVANCED,)), ('data/minecraft/tags/block/mineable/axe.json', (BASIC,)),
                 ('data/mio_icif/tags/block/producer.json', (BASIC, ADVANCED))):
    path = os.path.join(RES, tag)
    raw = open(path, 'rb').read()
    eol = '\r\n' if b'\r\n' in raw else '\n'
    data = json.loads(raw.decode('utf-8-sig'))
    have = [v['id'] if isinstance(v, dict) else v for v in data['values']]
    new = [i for i in ids if i not in have]
    if not new:
        continue
    if CHECK:
        missing.append(tag)
        continue
    for i in new:
        data['values'].append({'id': i, 'required': False})
    open(path, 'wb').write((json.dumps(data, indent=2, ensure_ascii=False) + '\n').replace('\n', eol).encode('utf-8'))

# ---------------------------------------------------------------- guide: the Trade Box page
GUIDE = 'assets/mio_icif/guides/mio_icif/manual/'
HEAD = '''---
navigation:
  title: "{title}"
  icon: {icon}
  parent: machines.md
  position: 29
item_ids:
  - {basic}
  - {advanced}
  - {kit}
---

'''
RECIPES = f'''
<RecipesFor id="{BASIC}" fallbackText="-" />

<RecipesFor id="{ADVANCED}" fallbackText="-" />

<RecipesFor id="{KIT}" fallbackText="-" />
'''
PAGES = {
    '': ('Trade Boxes', '''# Trade Boxes

<Row>
  <BlockImage id="mio_icif:producer/block_trade_box" scale="3" />
  <BlockImage id="mio_icif:producer/block_trade_box_advanced" scale="3" />
</Row>

## Function

A Trade Box sells for its owner, the player who placed it, while its chunk is loaded. It needs no energy.
Every sale is the owner's: the owner's sell depth, daily allowance, unlocks and records, the coins on the owner's coin account. Only the owner and operators open it.

### Basic Trade Box

- **27 slots** that hoppers and pipes fill with commodities, also items of a commodity's tag. Nothing comes out by pipe.
- **Daily auction**: every 10 seconds its commodities join the owner's daily auction order with the box's minimum in percent of the bid (50 % to start). What the auction does not sell goes to the owner's mailbox, as with every auction order.
- Made from a chest, an emerald, iron ingots and redstone: no circuit.

### Advanced Trade Box

- **54 slots**, **9 return slots** that pipes empty and a **filter** of 9 items: click a filter cell with an item on the cursor to take only that commodity; an empty filter takes every commodity, a click with an empty cursor clears a cell. Items the filter does not take move to the return slots.
- **Modes**: Auction (the daily auction), Instant (sold at once along the owner's sell depth) or Scrap rest (sold at once; what the market no longer pays for goes to the scrap sink, and the scrap comes out of the return slots). The minimum is in percent of the bid or, for instant sales, in coins per item (the c / % button).
- **Owner offline**: instant sales follow the Trade Terminal: they run only while the owner is online and wait for a batch of 64 items unless the box is full. With "Offline: auction" on, the box uses the daily auction while the owner is away.
- **Bounties**: with "Bounties" on, the box first delivers to the bounties the owner claimed (Claim on the Bounties tab), within the owner's share. The rewards go to the owner's coin account, UU account and mailbox.
- **Upgrade kit**: used by the owner on a placed Basic Trade Box, it makes an Advanced Trade Box with the same contents and settings.

### Both boxes

- The panel shows the results of the market day (sold at once and their coins, put into the auction, scrapped, delivered to bounties) and the status of the last run.
- Breaking a box sends its contents to the owner's mailbox; items with names, enchantments or damage drop.
- Since 0.1.10.42 the recipe of the Trade Terminal takes an Advanced Trade Box instead of a chest.

## Recipe
'''),
    '_zh_cn/': ('交易箱', '''# 交易箱

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
'''),
    '_ja_jp/': ('取引箱', '''# 取引箱

<Row>
  <BlockImage id="mio_icif:producer/block_trade_box" scale="3" />
  <BlockImage id="mio_icif:producer/block_trade_box_advanced" scale="3" />
</Row>

## 機能

取引箱は、チャンクが読み込まれている間、所有者（設置したプレイヤー）のために売る。電力は要らない。
売却はすべて所有者のもの：所有者の売却深度、1 日の上限、解放状況、記録を使い、硬貨は所有者の硬貨口座に入る。開けるのは所有者とオペレーターだけ。

### 基本取引箱

- **27 スロット**：ホッパーやパイプが商品（商品のタグのアイテムも）を入れる。パイプでは取り出せない。
- **日次オークション**：10 秒ごとに、中の商品を箱の最低価格（買値に対する %、初期値 50 %）で所有者の一括清算の注文に加える。オークションで売れなかった分は、ほかのオークション注文と同じく所有者の郵便箱に入る。
- チェスト、エメラルド、鉄インゴット、レッドストーンで作る。回路は要らない。

### 上級取引箱

- **54 スロット**、パイプで取り出せる **9 個の返却スロット**、9 マスの**フィルター**：カーソルにアイテムを持ってフィルターのマスをクリックすると、その商品だけを受け取る。フィルターが空ならすべての商品を受け取り、空のカーソルでクリックするとそのマスを消す。フィルターが受け取らないアイテムは返却スロットへ移る。
- **モード**：オークション（日次の一括清算）、即時（所有者の売却深度に沿ってすぐ売る）、スクラップ（すぐ売り、市場が買わなくなった残りはスクラップ回収へ。スクラップは返却スロットから出る）。最低価格は買値に対する %、即時売却では 1 個あたりの硬貨でも指定できる（c / % ボタン）。
- **所有者がオフラインのとき**：即時売却は取引端末と同じく、所有者がオンラインの間だけ、64 個単位で行う（箱が満杯のときを除く）。「オフライン時：オークション」にすると、所有者がいない間は日次オークションを使う。
- **懸賞**：「懸賞へ納品」をオンにすると、箱はまず所有者が受注した懸賞（懸賞タブの「受注」）へ、所有者の分担の範囲で納品する。報酬は所有者の硬貨口座、UU 口座、郵便箱に入る。
- **アップグレードキット**：所有者が設置済みの基本取引箱に使うと、中身と設定はそのままで上級取引箱になる。

### 両方の箱

- 画面には市場日の結果（即時売却の個数と硬貨、オークションに出した個数、スクラップ、懸賞への納品）と前回の実行の状態が出る。
- 箱を壊すと中身は所有者の郵便箱へ送られる。名前・エンチャント・耐久減少のあるアイテムはその場に落ちる。
- 0.1.10.42 から、取引端末のレシピはチェストの代わりに上級取引箱を使う。

## レシピ
'''),
}
for lang, (title, body) in PAGES.items():
    text = HEAD.format(title=title, icon=BASIC, basic=BASIC, advanced=ADVANCED, kit=KIT) + body + RECIPES
    write(GUIDE + lang + 'machines/producer_block_trade_box.md', text)

# ---------------------------------------------------------------- lines after anchors
FUT = 'machines/producer_block_future_elc.md'
TERM = 'machines/producer_block_trade_terminal.md'
G = 'src/main/resources/' + GUIDE
D_EX, D_FU = 'docs/datapacks/si_exchange.md', 'docs/datapacks/si_futures.md'

ADD = {
    G + FUT: [
        ('- **Futures**: cash-settled',
         '- **Futures limits** (since 0.1.10.42): orders of the same commodity, side and expiry day are one position row; you hold at '
         'most 16 rows (before: 8 contracts). Inside a row every order keeps its own price, so a row settles exactly like the orders '
         'it holds; the row shows their average price and how many there are. One order holds max(1,024, 1,000,000 / contract price) '
         'items, at most 65,536 (1,024 from a contract price of 976 coins up). In multiplayer the collateral of all your open futures stays within '
         'futuresMaxOpenCollateral (5,000,000 by default; 0 = no cap; single player: no cap). The positions are always on screen (beside '
         'the contracts from 448 px wide, else under them) under the header "Positions N/16 - collateral X / 5M"; a contract that '
         'cannot open says why in its tooltip.'),
        ('- **Player shops**',
         '- **Shop upkeep** (since 0.1.10.42): the upkeep falls due every 24 hours of server time (shopUpkeepHours) instead of every '
         'market week (about 2 h 20 min), and a shop pays at most 60 coins per period (shopUpkeepMaxPerShop; 0 = no cap), shared evenly '
         'by its offers: 12 offers pay 5 coins each. A shop of 12 offers paid about 1,234 coins in 24 hours before, 60 now. Resuming an '
         'unpaid offer pays its share. A shop of an older version starts its first period at the update.'),
        ('- **Treasury reserve**',
         '- **Wealth ranking** (since 0.1.10.42, Ranking at the top of the Treasury tab; also a tab of the Trade Terminal): the players '
         'of the server by total assets (coins on the account and carried, UU-matter, the goods of the mailbox, shop, auction, sell '
         'orders and commission rewards at the market\'s bid, the coins held for buy orders, commissions and shop purchases, and what '
         'the open futures pay at today\'s price; coins in chests and items outside the exchange do not count), or by this market '
         'week\'s net trade, futures profit or bounty deliveries. It lists the top 10 and your place; Hide name keeps your name out '
         'of it (others see "anonymous"). It is counted again at most every 10 seconds. Commands: /mio_icif market wealth '
         '[total|trade|futures|bounty], wealth hide or show; operators: wealth player followed by a player name (the assets by part) and wealth refresh. '
         'The server setting wealthRanking (true) turns it off.'),
        ('- Since 0.1.10.35 the server config',
         '- Since 0.1.10.42 the server config (or economy.json) also sets shopUpkeepHours (24), shopUpkeepMaxPerShop (60), '
         'futuresMaxOpenCollateral (5,000,000; multiplayer only) and wealthRanking (true). shopUpkeepPerWeek (10) keeps its name and '
         'is now the upkeep of an offer per period.'),
    ],
    G + '_zh_cn/' + FUT: [
        ('- **期货**：',
         '- **期货上限**（自 0.1.10.42 起）：同一商品、方向与到期日的订单合为一行持仓，每人最多 16 行（之前：8 份合约）。行内每笔订单'
         '保留各自的价格，因此一行的结算与其中各笔订单之和完全相同；行中显示它们的平均价与笔数。一笔订单最多 max(1,024, 1,000,000 ÷ '
         '合约价) 个，上限 65,536 个（合约价 976 货币及以上时为 1,024 个）。多人游戏中你所有未平仓期货的保证金合计不超过 '
         'futuresMaxOpenCollateral（默认 5,000,000；0 = 不限；单人游戏不限）。持仓始终显示（宽 448 像素起在合约旁边，否则在其下方），'
         '标题为“持仓 N/16 · 保证金 X / 5M”；无法开仓的合约在提示中说明原因。'),
        ('- **玩家商店**',
         '- **商店维护费**（自 0.1.10.42 起）：维护费改为每 24 小时服务器时间收取一次（shopUpkeepHours），不再每个交易周（约 2 小时 '
         '20 分钟）收取；每家商店每期最多支付 60 货币（shopUpkeepMaxPerShop；0 = 不限），由其报价平均分摊：12 个报价每个 5 货币。'
         '以前 12 个报价的商店 24 小时约支付 1,234 货币，现在为 60 货币。恢复未付维护费的报价时支付其分摊额。旧版本的商店从更新时'
         '开始第一个周期。'),
        ('- **国库储备**',
         '- **财富排行**（自 0.1.10.42 起，“国库”页顶部的“排行”；交易终端也有此页）：按总资产（账户与随身的货币、UU 物质，邮箱、'
         '商店、拍卖、卖单与委托奖励中的物品按市场收购价计，买单、委托与商店收购预留的货币，以及未平仓期货按今天价格的结算额；箱子里'
         '的货币与交易所以外的物品不计）或本交易周的净交易额、期货盈亏、悬赏交付额为服务器的玩家排名。显示前 10 名与你的名次；'
         '“隐藏名字”使你的名字不出现在排行中（其他人看到“匿名”）。最多每 10 秒重新统计一次。命令：/mio_icif market wealth '
         '[total|trade|futures|bounty]、wealth hide 或 show；管理员：wealth player 加玩家名（各部分资产）与 wealth refresh。服务器设置 '
         'wealthRanking（true）可关闭排行。'),
        ('- 自 0.1.10.35 起，服务器配置',
         '- 自 0.1.10.42 起，服务器配置（或 economy.json）还可设置 shopUpkeepHours（24）、shopUpkeepMaxPerShop（60）、'
         'futuresMaxOpenCollateral（5,000,000；仅多人游戏）与 wealthRanking（true）。shopUpkeepPerWeek（10）保留原名，现在是每个报价'
         '每期的维护费。'),
    ],
    G + '_ja_jp/' + FUT: [
        ('- **先物**: ',
         '- **先物の上限**（0.1.10.42 から）：同じ商品・方向・満期日の注文は 1 行の建玉にまとまり、建玉は最大 16 行（以前は 8 件）。'
         '行の中の注文はそれぞれの価格を保つので、行の決済は中の注文の合計とまったく同じ。行には平均価格と注文の数が出る。1 回の注文は '
         'max(1,024, 1,000,000 ÷ 契約価格) 個まで、最大 65,536 個（契約価格 976 硬貨以上では 1,024 個）。マルチプレイでは未決済の'
         '先物の証拠金の合計が futuresMaxOpenCollateral（既定 5,000,000、0 = 上限なし、シングルプレイは上限なし）を超えない。建玉は常に'
         '表示され（幅 448 px からは契約の横、それ未満は下）、見出しは「建玉 N/16・証拠金 X / 5M」。建てられない契約はツールチップに理由'
         'が出る。'),
        ('- **プレイヤーショップ**',
         '- **ショップの維持費**（0.1.10.42 から）：維持費は市場週（約 2 時間 20 分）ごとではなく、サーバー時間 24 時間ごと'
         '（shopUpkeepHours）にかかる。1 店が 1 期間に払うのは最大 60 硬貨（shopUpkeepMaxPerShop、0 = 上限なし）で、取引で均等に分ける：'
         '取引 12 件なら 1 件 5 硬貨。取引 12 件の店は以前 24 時間で約 1,234 硬貨を払っていたが、今は 60 硬貨。未払いで止まった取引を'
         '再開するときはその分担分を払う。以前のバージョンのショップは更新時から最初の期間が始まる。'),
        ('- **国庫準備金**',
         '- **資産ランキング**（0.1.10.42 から、「国庫」タブ上部の「ランキング」。取引端末にもタブがある）：サーバーのプレイヤーを総資産'
         '（口座と手持ちの硬貨、UU 物質、郵便箱・ショップ・オークション・売り注文・委託の報酬のアイテムを市場の買値で、買い注文・委託・'
         'ショップの買取のために預けた硬貨、未決済の先物を今日の価格で決済した額。チェストの硬貨や取引所の外のアイテムは数えない）か、'
         '今の市場週の純取引額・先物の損益・懸賞への納品額で並べる。上位 10 人と自分の順位が出る。「名前を隠す」で自分の名前を出さない'
         '（ほかの人には「匿名」と表示）。集計は最短 10 秒ごと。コマンド：/mio_icif market wealth [total|trade|futures|bounty]、wealth '
         'hide または show。オペレーター：wealth player とプレイヤー名（資産の内訳）と wealth refresh。サーバー設定 wealthRanking（true）で'
         'オフにできる。'),
        ('- 0.1.10.35 から、サーバー設定',
         '- 0.1.10.42 から、サーバー設定（または economy.json）で shopUpkeepHours（24）、shopUpkeepMaxPerShop（60）、'
         'futuresMaxOpenCollateral（5,000,000、マルチプレイのみ）、wealthRanking（true）も設定できる。shopUpkeepPerWeek（10）は名前を'
         'そのままに、1 取引あたり 1 期間の維持費になった。'),
    ],
    G + TERM: [
        ('- **Settlement**: the minimum',
         '- **Recipe** (since 0.1.10.42): the terminal takes an Advanced Trade Box instead of a chest. A Trade Box sells without '
         'energy and without standing orders; see Trade Boxes.'),
    ],
    G + '_zh_cn/' + TERM: [
        ('- **大宗结算**：最低价',
         '- **配方**（自 0.1.10.42 起）：终端用高级交易箱代替箱子。交易箱不需要能量也不需要常设订单即可出售，见“交易箱”。'),
    ],
    G + '_ja_jp/' + TERM: [
        ('- **一括決済**: 最低価',
         '- **レシピ**（0.1.10.42 から）：端末はチェストの代わりに上級取引箱を使う。取引箱は電力も常設注文もなしで売る。「取引箱」を参照。'),
    ],
    D_EX: [
        ('- **国库储备**（0.1.10.35 起）',
         '- 0.1.10.42 新增的键：`shopUpkeepHours`（24，商店维护费的收取间隔，1～168 小时服务器时间；之前每个交易周）、'
         '`shopUpkeepMaxPerShop`（60，每家商店每期最多支付的维护费，由其报价平均分摊；0 = 不限）、`futuresMaxOpenCollateral`'
         '（5,000,000，多人游戏中每名玩家未平仓期货保证金的上限；0 = 不限；单人游戏不限）、`wealthRanking`（true，财富排行）。'
         '`shopUpkeepPerWeek` 保留原名，现在是每个报价每期的维护费。所有玩家可用 `/mio_icif market wealth [total|trade|futures|bounty]` '
         '查看排行，`wealth hide` / `wealth show` 隐藏或显示自己的名字。'),
        ('| `/mio_icif bounty settings` |',
         '| `/mio_icif market wealth player <玩家>` | 玩家的各部分资产、总资产名次与本周结果（0.1.10.42） |\n'
         '| `/mio_icif market wealth refresh` | 立即重新统计财富排行的资产（0.1.10.42；否则最多每 10 秒一次） |'),
        ('除出售箱与每日集中清算外',
         '0.1.10.42 起，交易箱（基础 / 高级）也按这两个字段接收商品（管道与漏斗放入），按本商品出售；高级交易箱的过滤按商品比较。'),
    ],
    D_FU: [
        ('- 期货合约价格按商品自身的价格模型',
         '- 0.1.10.42 起，一笔期货订单最多 max(1,024, 1,000,000 ÷ 合约价) 个（上限 65,536），同一商品、方向与到期日的订单合为一行，'
         '每人最多 16 行；多人游戏中未平仓保证金合计受 `futuresMaxOpenCollateral` 限制（见 si_exchange.md）。价格低的商品因此可以开'
         '更大的合约。'),
    ],
}

for rel, blocks in ADD.items():
    path = os.path.join(TREE, rel)
    raw = open(path, 'rb').read()
    eol = '\r\n' if b'\r\n' in raw else '\n'
    s = raw.decode('utf-8').replace('\r\n', '\n')
    before = s
    for anchor, block in blocks:
        if block in s:
            continue
        if CHECK:
            missing.append(f'{rel}: {block[:40]}')
            continue
        rows = s.split('\n')
        at = [i for i, r in enumerate(rows) if r.startswith(anchor)]
        assert len(at) == 1, (rel, anchor, at)
        rows.insert(at[0] + 1, block)
        s = '\n'.join(rows)
    if rel.endswith(G + FUT) or rel.endswith(G + TERM):
        assert s.isascii(), rel
    if s != before and not CHECK:
        open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))
        print('wrote', rel)

if CHECK:
    print('missing or different:', missing)
    sys.exit(1 if missing else 0)
print('data_1142 written')
