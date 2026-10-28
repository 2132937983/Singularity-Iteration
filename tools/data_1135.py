#!/usr/bin/env python3
"""0.1.10.35: the texts of the release. The guide (en / zh / ja, Futures Machine page): the processing bids (made_from cap,
raw ores sold only to the market), book orders in hours, commissions in hours and their notice, player shops, the treasury
reserve, the new settings. The datapack documents (zh): sell_only and made_from (si_futures.md); hours, reserve and shops,
the new economy keys (si_exchange.md). The API guides (en / zh): the ledger's created / destroyed with the reserve, the bids
of quoteSell. Each block goes after its anchor line (the line that starts with the anchor text). Idempotent; keeps the line
endings of edited files. With --check it only reports what is missing. Usage: data_1135.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
GUIDE = 'src/main/resources/assets/mio_icif/guides/mio_icif/manual/'
PAGE = 'machines/producer_block_future_elc.md'
missing = []

G_EN, G_ZH, G_JA = GUIDE + PAGE, GUIDE + '_zh_cn/' + PAGE, GUIDE + '_ja_jp/' + PAGE
D_EX, D_FU = 'docs/datapacks/si_exchange.md', 'docs/datapacks/si_futures.md'
A_EN, A_ZH = 'docs/api/API_GUIDE.en.md', 'docs/api/API_GUIDE.zh_cn.md'

# (anchor, block) per file
ADD = {
    G_EN: [
        ('- Since 0.1.10.33 an instant sale to the market',
         '- Since 0.1.10.35 an order that waits in the book lasts 72 hours of server time (before: 7 market days); the Orders tab shows '
         'the hours left. Orders placed before keep their days.\n'),
        ('- Since 0.1.10.34 the market buys a few goods back',
         '- Since 0.1.10.35 those bid prices lie halfway between selling the materials to the market and buying them from it (refined '
         'iron ingot 205, circuit board 760, advanced alloy ingot 665), and the market never pays more for them than their materials '
         'cost to buy from it that day, so buying the materials, making the product and selling it back earns nothing. Raw iron, raw '
         'copper and raw gold are only bought by the market (grinding a raw ore doubles it, so buying them paid); sell them, or trade them with '
         'other players in the order book.\n'),
        ('- Since 0.1.10.33 a commission with an item',
         '- **Hours** (since 0.1.10.35): a commission lasts 1 to 168 hours of server time (- and + step through 1, 3, 6, 12, 24, 48, '
         '72 and 168; 24 to start). The board shows the time left, red under 3 hours, and lists the commissions that end first at the '
         'top. When it ends, the unpaid reward returns to the requester\'s mailbox. New commissions are announced in the chat; '
         'Notice, at the top of the form, switches that off for you. Commissions posted before keep their days.\n'
         '- **Player shops** (since 0.1.10.35, Shops and My shop at the top of the Commissions tab): post standing offers that trade '
         'again and again: Swap (your goods for the customer\'s items), Sell (your goods for coins) or Buy (the customer\'s items for '
         'your coins). The goods or coins of the stock are set aside when you post or restock; customers take x1, xN or Max at once '
         'and pay from the inventory or from the container in the settlement slot. Items customers paid wait in the shop\'s storage '
         '(54 stacks together with the stock) until you Collect them; coins of sales go to your coin account. A new offer costs 50 '
         'coins and each offer pays 10 coins of upkeep every market week from your coins (an offer that cannot pay is paused until '
         'you resume it); the market fee of a coin trade is taken as usual. At most 12 offers. The list shows last the shops whose owner '
         'was away on 7 days the server ran; after 30 such days the shop closes and its stock, coins and proceeds go to the owner\'s '
         'mailbox (with a message at the next login). Filter lists only the shops that trade an item you click.\n'),
        ('- **Number fields**',
         '- **Treasury reserve** (since 0.1.10.35): the fees of order-book trades, commissions, shops and the bounty refresh go into a '
         'reserve instead of being destroyed, and the coins of bounties are paid from it before new coins are made. The reserve holds '
         'at most 4 market weeks of normal bounty coins (772,800 with one player); fees above that are destroyed. The Treasury tab '
         'shows the reserve and its cap; its tooltip shows this market week\'s fees in, payments and new bounty coins. The ledger '
         'counts coins into and out of circulation as before (a fee leaves circulation, a payment from the reserve comes back).\n'),
        ('- Since 0.1.10.34 the server config',
         '- Since 0.1.10.35 the server config (or economy.json) sets commissionDefaultHours (24) and commissionMaxHours (168), '
         'orderHours (72), reserveWeeks (4; 0 = no reserve, fees are destroyed as before), shopsEnabled (true), shopMaxOffers (12), '
         'shopStacks (54), shopOfferFee (50), shopUpkeepPerWeek (10), shopIdleDays (7) and shopCloseDays (30).\n'),
    ],
    G_ZH: [
        ('- 自 0.1.10.33 起，立即卖给市场',
         '- 自 0.1.10.35 起，在盘口中等待的挂单保留 72 小时服务器时间（之前为 7 个交易日）；“订单与邮件”页显示剩余时间。之前挂出的订单保持原来的天数。\n'),
        ('- 自 0.1.10.34 起，市场以远低于卖价',
         '- 自 0.1.10.35 起，这些收购价位于“把原料卖给市场”与“从市场买入原料”之间的中点（精炼铁锭 205、电路板 760、高级合金锭 665），'
         '并且市场为它们支付的价格永远不超过当天从市场买入其原料的价格，因此“买原料→加工→卖回市场”没有利润。粗铁、粗铜与粗金只由市场收购'
         '（粗矿粉碎后数量翻倍，买来加工可获利）；可以卖给市场，或在盘口与其他玩家交易。\n'),
        ('- 自 0.1.10.33 起，以物品或 UU 物质为报酬',
         '- **时长**（自 0.1.10.35 起）：委托保留 1～168 小时服务器时间（- 与 + 在 1、3、6、12、24、48、72、168 之间切换，初始 24）。'
         '委托板显示剩余时间，少于 3 小时显示为红色，并把最先到期的排在前面。到期后未支付的报酬退回委托人的邮箱。新委托会在聊天栏通知；'
         '表单顶部的“通知”可为你关闭。之前发布的委托保持原来的天数。\n'
         '- **玩家商店**（自 0.1.10.35 起，“委托”页顶部的“商店”与“我的商店”）：发布可反复成交的长期报价：交换（你的商品换顾客的物品）、'
         '出售（你的商品换货币）或收购（顾客的物品换你的货币）。发布或补货时预留库存所需的商品或货币；顾客可一次交易 x1、xN 或最大数量，'
         '用背包或结算槽中的容器支付。顾客支付的物品存放在商店仓储中（与库存合计 54 组），直到你“领取”；出售所得的货币进入你的货币账户。'
         '发布新报价需 50 货币，每个报价每个交易周从你的货币中支付 10 货币维护费（付不起的报价会暂停，直到你恢复）；货币交易照常收取市场'
         '手续费。最多 12 个报价。在服务器运行的 7 天里店主都未上线的商店排在列表最后；30 天后商店关闭，库存、货币与收益进入店主的邮箱（下次登录时'
         '提示）。“筛选”只列出交易你所点击物品的商店。\n'),
        ('- **数字输入框**',
         '- **国库储备**（自 0.1.10.35 起）：盘口交易、委托、商店与悬赏刷新的手续费存入储备，不再销毁；悬赏的货币先从储备支付，不足部分才新发行。'
         '储备最多保存 4 个交易周的普通悬赏货币（1 名玩家时为 772,800）；超出部分照旧销毁。“国库”页显示储备与上限，提示中显示本交易周的'
         '存入、支出与悬赏新发行。账本仍按进入与退出流通计算（手续费退出流通，储备的支出重新进入）。\n'),
        ('- 自 0.1.10.34 起，服务器配置',
         '- 自 0.1.10.35 起，服务器配置（或 economy.json）可以设置 commissionDefaultHours（24）与 commissionMaxHours（168）、orderHours（72）、'
         'reserveWeeks（4；0 = 不设储备，手续费照旧销毁）、shopsEnabled（true）、shopMaxOffers（12）、shopStacks（54）、shopOfferFee（50）、'
         'shopUpkeepPerWeek（10）、shopIdleDays（7）与 shopCloseDays（30）。\n'),
    ],
    G_JA: [
        ('- 0.1.10.33 から、市場への即時売却',
         '- 0.1.10.35 から、板で待つ指値注文はサーバー時間で 72 時間有効（以前は 7 市場日）。「注文と郵便」タブに残り時間が出る。以前に出した注文は'
         '元の日数のまま。\n'),
        ('- 0.1.10.34 から、市場は一部の品目を売値よ',
         '- 0.1.10.35 から、これらの買取価格は「材料を市場へ売る」と「材料を市場から買う」の中間（精錬鉄インゴット 205、回路基板 760、'
         '高度合金インゴット 665）で、しかも市場はその日に材料を市場から買う値段より高くは買い取らない。材料を買って加工し売り戻しても利益は出ない。'
         '鉄・銅・金の原石は市場が買い取るだけになった（原石は粉砕すると 2 倍になり、買って加工すると得をしていた）。市場へ売るか、板でほかのプレイヤーと取引できる。\n'),
        ('- 0.1.10.33 から、アイテム・UU 物質が報酬の',
         '- **時間**（0.1.10.35 から）：委託はサーバー時間で 1〜168 時間残る（- と + で 1・3・6・12・24・48・72・168 を切り替え、最初は 24）。'
         '掲示板に残り時間が出て、3 時間未満は赤くなり、早く終わるものから並ぶ。期限が来ると未払いの報酬は依頼者の郵便箱に戻る。新しい委託は'
         'チャットで知らされる。フォーム上部の「通知」で自分だけオフにできる。以前に出した委託は元の日数のまま。\n'
         '- **プレイヤーショップ**（0.1.10.35 から、「委託」タブ上部の「ショップ」と「自分の店」）：何度でも取引できる常設の取引を出せる。'
         '交換（自分の商品と客のアイテム）、販売（自分の商品と硬貨）、買取（客のアイテムと自分の硬貨）。出品や補充のときに在庫分の商品または'
         '硬貨を預ける。客は x1・xN・最大でまとめて取引でき、インベントリか精算スロットのコンテナから払う。客が払ったアイテムは店の保管枠'
         '（在庫と合わせて 54 スタック）にたまり、「受け取る」で回収する。販売の硬貨は硬貨口座に入る。出品は 50 硬貨、取引ごとに市場週ごと'
         '10 硬貨の維持費を硬貨から払う（払えない取引は再開するまで停止）。硬貨の取引には通常どおり市場手数料がかかる。取引は最大 12 件。'
         'サーバーが動いた日のうち 7 日店主が来なかった店は一覧の最後に並び、30 日で閉店して在庫・硬貨・売上は店主の郵便箱へ戻る（次のログインで通知）。'
         '「絞り込み」はクリックしたアイテムを扱う店だけを表示する。\n'),
        ('- **数値欄**',
         '- **国庫準備金**（0.1.10.35 から）：板の取引・委託・ショップ・懸賞の更新の手数料は消滅せず準備金に入り、懸賞の硬貨はまず準備金から'
         '払われ、足りない分だけ新しく発行される。準備金は普通の懸賞硬貨の 4 市場週分（1 人なら 772,800）まで。超えた手数料は以前どおり消滅する。'
         '「国庫」タブに準備金と上限が出て、ツールチップに今の市場週の入金・支払・懸賞の新規発行が出る。台帳は以前どおり流通への出入りで'
         '数える（手数料は流通から出て、準備金からの支払で戻る）。\n'),
        ('- 0.1.10.34 から、サーバー設定',
         '- 0.1.10.35 から、サーバー設定（または economy.json）で commissionDefaultHours（24）と commissionMaxHours（168）、orderHours（72）、'
         'reserveWeeks（4。0 = 準備金なし、手数料は以前どおり消滅）、shopsEnabled（true）、shopMaxOffers（12）、shopStacks（54）、'
         'shopOfferFee（50）、shopUpkeepPerWeek（10）、shopIdleDays（7）、shopCloseDays（30）を設定できる。\n'),
    ],
    D_EX: [
        ('- 0.1.10.34 新增的键',
         '- 0.1.10.35 新增的键：`commissionDefaultHours`（24）、`commissionMaxHours`（168，委托的时长上限，1～168 小时服务器时间）、`orderHours`'
         '（72，盘口挂单的有效时长）、`reserveWeeks`（4，国库储备上限 = 周数 × 7 × 每日普通发布数 × `bountyNormalValue` × 普通倍率；0 = 不设储备）、'
         '`shopsEnabled`（true）、`shopMaxOffers`（12）、`shopStacks`（54，库存与收益合计的组数）、`shopOfferFee`（50）、`shopUpkeepPerWeek`'
         '（10，每个报价每个交易周）、`shopIdleDays`（7）与 `shopCloseDays`（30，店主不在线的服务器运行日数：排到最后 / 关闭并退回邮箱）。\n'
         '- **国库储备**（0.1.10.35 起）：盘口交易、委托、商店（发布费、维护费、货币交易的手续费）与悬赏刷新的货币进入储备（上限内），'
         '超出部分销毁；悬赏的货币奖励先从储备支付，只有不足部分计为新发行。账本另记 `ReserveIn` / `ReserveOut`；稳定器、`/mio_icif market '
         'ledger` / `audit` 与 API 的 `ledger` 按进入 / 退出流通计算（发行 + 储备支出，销毁 + 存入储备），与 0.1.10.34 的含义相同。\n'),
    ],
    D_FU: [
        ('| `bid_price` |',
         '| `made_from` | 否（0.1.10.35） | 制作该物品所用的市场货品与数量（例如 `{ "minecraft:iron_ingot": 1 }`，0～4096，不能是物品自己）。'
         '市场的收购价不超过当天从市场买入这些原料的价格（Σ 数量 × 原料价格 × (1 + 价差/2)），即使各货品价格各自波动，“买原料→加工→卖回”'
         '也不会获利。某个原料不是市场货品时不设上限。默认：精炼铁锭（铁锭 1）、电路板（红石 2、铁锭 1、铜锭 2、橡胶 6）、高级合金锭'
         '（铁锭、青铜锭、锡锭各 1.5）。 |\n'
         '| `sell_only` | 否（0.1.10.35） | `true` 时市场只收购、不出售该物品（库存与进口都不卖；玩家之间的盘口照常）。默认的粗铁、粗铜、粗金'
         '设置了此项：粗矿粉碎后数量翻倍，从市场买粗矿加工后卖回会获利。 |\n'),
        ('- 0.1.10.34 起，设置了 `bid_price` 的商品',
         '- 0.1.10.35 起，默认的收购价改为精炼铁锭 205、电路板 760、高级合金锭 665（以基础价格计，在“卖出原料”与“买入原料”之间的中点），'
         '并由 `made_from` 限制为不超过当天原料的卖价。离线模拟 20,000 个交易日：0.1.10.34 的收购价下买原料加工卖回有利可图的天数为'
         ' 13,272 / 400 / 1,529，0.1.10.35 为 0。\n'),
    ],
    A_EN: [
        ('* Since 0.1.10.34 a need of value 0',
         '* Since 0.1.10.35 the fees of the exchange go into a treasury reserve and the coins of bounties are paid from it first. '
         '`ledger(server, days)` still reports coins into and out of circulation: `minted` = new coins + payments of the reserve, '
         '`burned` = destroyed coins + fees kept in the reserve (the same meaning as before the reserve). `quoteSell` uses the new '
         'bids: a commodity with `made_from` is bought for at most what its materials cost to buy that day; `sell_only` commodities '
         '(raw iron, copper, gold) have no supply in `quoteBuy`. The API types are unchanged.\n'),
    ],
    A_ZH: [
        ('* 自 0.1.10.34 起，价值为 0 的要求',
         '* 自 0.1.10.35 起，交易所的手续费进入国库储备，悬赏的货币先从储备支付。`ledger(server, days)` 仍按进入与退出流通报告：`minted` = '
         '新发行 + 储备支出，`burned` = 销毁 + 存入储备的手续费（与设立储备之前的含义相同）。`quoteSell` 使用新的收购价：带 `made_from` 的'
         '货品的收购价不超过当天其原料的买入价；`sell_only` 货品（粗铁、粗铜、粗金）在 `quoteBuy` 中没有库存。API 类型不变。\n'),
    ],
}

for rel in (G_EN, G_ZH, G_JA, D_EX, D_FU, A_EN, A_ZH):
    path = os.path.join(TREE, rel)
    raw = open(path, 'rb').read()
    eol = '\r\n' if b'\r\n' in raw else '\n'
    s = raw.decode('utf-8').replace('\r\n', '\n')
    before = s
    for anchor, block in ADD.get(rel, []):
        if block.rstrip('\n') in s:
            continue
        if CHECK:
            missing.append(f'{rel}: {block[:40]}')
            continue
        rows = s.split('\n')
        at = [i for i, r in enumerate(rows) if r.startswith(anchor)]
        assert len(at) == 1, (rel, anchor, at)
        rows.insert(at[0] + 1, block.rstrip('\n'))
        s = '\n'.join(rows)
    if rel in (G_EN, A_EN):
        assert s.isascii(), rel
    if s != before and not CHECK:
        open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))
        print('wrote', rel)

if CHECK:
    print('missing or different:', missing)
    sys.exit(1 if missing else 0)
print('data_1135 written')
