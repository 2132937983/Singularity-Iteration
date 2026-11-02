#!/usr/bin/env python3
"""0.1.10.42 language values (en_us / zh_cn / ja_jp / ru_ru): the fuel rod tooltips; the futures limits (rows, per-order cap, collateral cap, the
positions header), the shop upkeep period, the wealth ranking (Futures Machine, Trade Terminal, commands), the Trade Boxes
(blocks, kit, panel, messages) and the new server settings.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1142.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
LANGS = ('en_us', 'zh_cn', 'ja_jp', 'ru_ru')


def v(en=None, zh=None, ja=None, ru=None):
    return {k: t for k, t in zip(LANGS, (en, zh, ja, ru)) if t is not None}


G = 'gui.mio_icif.exchange.'
M = 'message.mio_icif.exchange.'
W = 'command.mio_icif.wealth.'
B = 'gui.mio_icif.trade_box.'
CFG = 'mio_icif.configuration.FutureMarket.'

KEYS = {
    # ---------------------------------------------------------------- the fuel rod tooltips (the first part of 0.1.10.42)
    'tooltip.mio_icif.fuel_rod.cycles': v(
        '%1$s reactor cycles; %2$s cells', '%1$s 个反应堆周期；%2$s 联', '原子炉サイクル %1$s；%2$s 連', '%1$s циклов реактора; ячеек: %2$s'),
    'tooltip.mio_icif.fuel_rod.cycles_double': v(
        '%1$s reactor cycles; %2$s cells ×2', '%1$s 个反应堆周期；%2$s 联 ×2', '原子炉サイクル %1$s；%2$s 連 ×2',
        '%1$s циклов реактора; ячеек: %2$s ×2'),
    # ---------------------------------------------------------------- futures limits
    G + 'quantity_max': v('Qty (max %1$s)', '数量（最多 %1$s）', '数量（最大 %1$s）', 'Кол-во (макс. %1$s)'),
    G + 'quantity_max_help': v(
        'The most one order can hold now on every contract of this commodity: max(%1$s, %2$s / contract price) items, at most '
        '%3$s, and within your collateral cap.',
        '现在本商品每份合约一笔订单最多可开的数量：max(%1$s, %2$s ÷ 合约价) 个，上限 %3$s 个，且不超过你的保证金上限。',
        '今この商品のどの契約でも 1 回の注文で建てられる最大数：max(%1$s, %2$s ÷ 契約価格) 個、最大 %3$s 個、かつ証拠金の上限内。',
        'Сколько сейчас вмещает один ордер по любому контракту этого товара: max(%1$s, %2$s / цена контракта) предметов, не более '
        '%3$s и в пределах лимита обеспечения.'),
    G + 'future_quantity_cap': v(
        'At %2$s per item one order holds at most %1$s items', '合约价 %2$s 时一笔订单最多 %1$s 个',
        '契約価格 %2$s では 1 回の注文は %1$s 個まで', 'При цене %2$s один ордер вмещает не более %1$s предметов'),
    G + 'future_full': v(
        'All %1$s position rows are in use (one per commodity, side and expiry)', '%1$s 行持仓已满（每个商品、方向与到期日一行）',
        '建玉 %1$s 行がすべて使用中（商品・方向・満期日ごとに 1 行）', 'Все %1$s строк позиций заняты (по одной на товар, сторону и срок)'),
    G + 'future_row_full': v(
        'This position row already holds %1$s items', '这一行持仓已有 %1$s 个', 'この建玉の行はすでに %1$s 個',
        'В этой строке позиций уже %1$s предметов'),
    G + 'future_collateral_cap': v(
        'Open collateral is capped at %1$s coins; room left: %2$s', '未平仓保证金上限为 %1$s 货币；剩余额度：%2$s',
        '未決済の証拠金の上限は %1$s 硬貨。残り：%2$s', 'Лимит обеспечения открытых позиций — %1$s монет; осталось: %2$s'),
    G + 'positions_line': v(
        'Positions %1$s/%2$s · collateral %3$s / %4$s', '持仓 %1$s/%2$s · 保证金 %3$s / %4$s', '建玉 %1$s/%2$s・証拠金 %3$s / %4$s',
        'Позиции %1$s/%2$s · обеспечение %3$s / %4$s'),
    G + 'positions_no_cap': v('no cap', '不限', '上限なし', 'без лимита'),
    G + 'positions_help': v(
        'At most %1$s position rows: orders of the same commodity, side and expiry day share a row (up to %5$s orders, each keeping '
        'its price). One order holds max(%2$s, 1,000,000 / contract price) items, at most %3$s. Collateral of all open futures: at '
        'most %4$s. A position settles on its last day at the market price.',
        '最多 %1$s 行持仓：同一商品、方向与到期日的订单共用一行（最多 %5$s 笔，各自保留价格）。一笔订单最多 max(%2$s, 1,000,000 ÷ '
        '合约价) 个，上限 %3$s 个。所有未平仓期货的保证金合计最多 %4$s。持仓在最后一天按市场价结算。',
        '建玉は最大 %1$s 行：同じ商品・方向・満期日の注文は 1 行にまとまる（最大 %5$s 件、それぞれの価格を保つ）。1 回の注文は '
        'max(%2$s, 1,000,000 ÷ 契約価格) 個、最大 %3$s 個。未決済の先物の証拠金の合計は最大 %4$s。建玉は最終日に市場価格で決済されます。',
        'Не более %1$s строк позиций: ордера на один товар, сторону и день исполнения объединяются в строку (до %5$s ордеров, у '
        'каждого своя цена). Один ордер — max(%2$s, 1 000 000 / цена контракта) предметов, не более %3$s. Обеспечение всех открытых '
        'фьючерсов — не более %4$s. Позиция закрывается в последний день по рыночной цене.'),
    G + 'positions_rows_full': v(
        'All %1$s rows are in use: a new order must join an existing row.', '%1$s 行已满：新订单只能加入已有的行。',
        '%1$s 行すべて使用中：新しい注文は既存の行に加える必要があります。', 'Все %1$s строк заняты: новый ордер может только войти в существующую строку.'),
    G + 'positions_cap_full': v(
        'Collateral %1$s of %2$s in use: no room for new orders.', '已用保证金 %1$s / %2$s：无法再开新订单。',
        '証拠金 %1$s / %2$s を使用中：新しい注文の余地がありません。', 'Обеспечение %1$s из %2$s занято: места для новых ордеров нет.'),
    G + 'position_lots': v('%1$s orders', '%1$s 笔', '%1$s 件', 'ордеров: %1$s'),
    G + 'position_help': v(
        '%1$s orders at an average of %2$s; collateral %3$s coins; worth %4$s now. Each order settles at its own price.',
        '%1$s 笔订单，平均价 %2$s；保证金 %3$s 货币；现值 %4$s。每笔订单按各自的价格结算。',
        '注文 %1$s 件、平均価格 %2$s。証拠金 %3$s 硬貨、現在の価値 %4$s。注文はそれぞれの価格で決済されます。',
        'Ордеров: %1$s, средняя цена %2$s; обеспечение %3$s монет; сейчас стоит %4$s. Каждый ордер закрывается по своей цене.'),
    M + 'future_quantity_cap': v(
        '§cAt %2$s per item one order holds at most %1$s items.', '§c合约价 %2$s 时一笔订单最多 %1$s 个。',
        '§c契約価格 %2$s では 1 回の注文は %1$s 個までです。', '§cПри цене %2$s один ордер вмещает не более %1$s предметов.'),
    M + 'too_many_positions': v(
        '§cYou already hold %s position rows.', '§c你已有 %s 行持仓。', '§c建玉がすでに %s 行あります。', '§cУ вас уже строк позиций: %s.'),
    M + 'future_lots_full': v(
        '§cThis position row already holds %1$s orders.', '§c这一行持仓已有 %1$s 笔订单。', '§cこの建玉の行にはすでに注文が %1$s 件あります。',
        '§cВ этой строке позиций уже ордеров: %1$s.'),
    M + 'future_row_full': v(
        '§cThis position row already holds %1$s items.', '§c这一行持仓已有 %1$s 个。', '§cこの建玉の行はすでに %1$s 個です。',
        '§cВ этой строке позиций уже %1$s предметов.'),
    M + 'future_collateral_cap': v(
        '§cYour open futures may hold at most %1$s coins of collateral; room left: %2$s.',
        '§c你的未平仓期货保证金最多 %1$s 货币；剩余额度：%2$s。', '§c未決済の先物の証拠金は最大 %1$s 硬貨です。残り：%2$s。',
        '§cОбеспечение ваших открытых фьючерсов — не более %1$s монет; осталось: %2$s.'),
    M + 'future_added': v(
        '§aAdded %1$s contracts at %2$s to your position (collateral %3$s coins).', '§a以 %2$s 加仓 %1$s 份（保证金 %3$s 货币）。',
        '§a%2$s で %1$s 枚を建玉に追加しました（証拠金 %3$s コイン）。', '§aК позиции добавлено контрактов: %1$s по цене %2$s (обеспечение: %3$s монет).'),

    # ---------------------------------------------------------------- shop upkeep
    G + 'my_shop_help': v(
        'A new offer costs %1$s coins; every offer pays %2$s coins of upkeep every %3$s hours of server time from your coins '
        '(at most %4$s per shop and period, shared by its offers; an offer that cannot pay is paused). Both go to the treasury '
        'reserve. Not online on 7 days the server ran (default): the shop is listed last; on 30: it closes and everything returns '
        'to your mailbox.',
        '发布新报价需 %1$s 货币；每个报价每 %3$s 小时服务器时间从你的货币中支付 %2$s 维护费（每家商店每期最多 %4$s，由其报价分摊；'
        '付不起的报价会暂停）。两者都进入国库储备。在服务器运行的日子里有 7 天（默认）未上线：商店排到最后；30 天：商店关闭，所有物品'
        '退回邮箱。',
        '新しい取引の出品は %1$s 硬貨。取引ごとにサーバー時間 %3$s 時間ごと %2$s 硬貨の維持費を硬貨から払います（1 店 1 期間で最大 '
        '%4$s、取引で分担。払えない取引は停止）。どちらも国庫準備金に入ります。サーバーが動いた日のうち 7 日（既定）ログインしないと店'
        'は最後尾に、30 日で閉店し、すべて郵便箱に戻ります。',
        'Создание нового предложения стоит %1$s монет; каждые %3$s ч серверного времени за каждое предложение из ваших монет '
        'списывается %2$s монет на обслуживание (не более %4$s за магазин и период, делится между предложениями; если оплатить не '
        'удаётся, предложение приостанавливается). Оба платежа поступают в резерв казначейства. Если вы не заходили в игру в течение '
        '7 дней работы сервера (по умолчанию), магазин перемещается в конец списка; через 30 дней он закрывается, а всё содержимое '
        'возвращается в ваш почтовый ящик.'),
    G + 'shop_form_help': v(
        'Offer fee %1$s coins; upkeep %2$s coins per offer every %3$s hours (at most %4$s per shop). The goods for sale leave your '
        'inventory now; a buy offer sets its coins aside.',
        '报价手续费 %1$s 货币；每个报价每 %3$s 小时维护费 %2$s 货币（每家商店最多 %4$s）。出售的商品现在就从背包取出；收购报价会预留'
        '所需货币。',
        '出品手数料 %1$s 硬貨、維持費は取引ごとに %3$s 時間ごと %2$s 硬貨（1 店で最大 %4$s）。売る商品は今インベントリから預け、買取の'
        '取引は硬貨を預けます。',
        'Комиссия за размещение предложения — %1$s монет; обслуживание — %2$s монет за каждое предложение каждые %3$s ч (не более %4$s '
        'за магазин). Товары для продажи будут изъяты из вашего инвентаря сразу; для предложения о покупке монеты резервируются.'),
    G + 'shop_resume_help': v(
        "This period's upkeep was not paid: resuming pays %1$s coins now.", '本期维护费未支付：恢复时立即支付 %1$s 货币。',
        '今期の維持費が未払いです：再開すると今 %1$s 硬貨を払います。',
        'Обслуживание за этот период не оплачено: для возобновления сейчас будет списано %1$s монет.'),
    G + 'upkeep_no_cap': v('no cap', '不限', '上限なし', 'без лимита'),

    # ---------------------------------------------------------------- wealth ranking
    G + 'treasury_switch.0': v('Treasury', '国库', '国庫', 'Казначейство'),
    G + 'treasury_switch.1': v('Ranking', '排行', 'ランキング', 'Рейтинг'),
    'gui.mio_icif.terminal.tab.6': v('Ranking', '排行', 'ランキング', 'Рейтинг'),
    G + 'ranking.title': v('Wealth ranking', '财富排行', '資産ランキング', 'Рейтинг богатства'),
    G + 'ranking.category.0': v('Total', '总资产', '総資産', 'Всего'),
    G + 'ranking.category.1': v('Trade', '交易', '取引', 'Торговля'),
    G + 'ranking.category.2': v('Futures', '期货', '先物', 'Фьючерсы'),
    G + 'ranking.category.3': v('Bounty', '悬赏', '懸賞', 'Награды'),
    G + 'ranking.help.0': v(
        'Total assets now: coins on the account and carried, UU-matter, the goods the exchange holds for you at the market\'s bid, '
        'coins held for orders, and open futures at today\'s price. Coins in chests and items outside the exchange do not count.',
        '当前总资产：账户与随身的货币、UU 物质、交易所为你保管的物品（按市场收购价）、为订单预留的货币，以及按今天价格计算的未平仓'
        '期货。箱子里的货币与交易所以外的物品不计。',
        '今の総資産：口座と手持ちの硬貨、UU 物質、取引所が預かるアイテム（市場の買値で）、注文のために預けた硬貨、今日の価格での'
        '未決済の先物。チェストの硬貨や取引所の外のアイテムは数えません。',
        'Все активы сейчас: монеты на счёте и при себе, UU-материя, товары, которые биржа хранит для вас (по цене покупки рынка), '
        'монеты в резерве ордеров и открытые фьючерсы по сегодняшней цене. Монеты в сундуках и предметы вне биржи не учитываются.'),
    G + 'ranking.help.1': v(
        "This market week's net trade: coins received from the market and other players minus coins paid. Trading back and forth "
        'only loses the fees.',
        '本交易周的净交易额：从市场与其他玩家收到的货币减去支付的货币。来回交易只会损失手续费。',
        '今の市場週の純取引額：市場とほかのプレイヤーから受け取った硬貨から、払った硬貨を引いた額。行ったり来たりの取引は手数料を失う'
        'だけです。',
        'Чистая торговля за эту рыночную неделю: монеты, полученные от рынка и других игроков, минус уплаченные. Торговля туда-обратно '
        'приносит только потерю комиссий.'),
    G + 'ranking.help.2': v(
        "This market week's realised futures profit.", '本交易周已实现的期货盈亏。', '今の市場週に確定した先物の損益。',
        'Реализованная прибыль по фьючерсам за эту рыночную неделю.'),
    G + 'ranking.help.3': v(
        "This market week's value delivered to bounties.", '本交易周交付给悬赏的价值。', '今の市場週に懸賞へ納品した価値。',
        'Стоимость, сданная по наградам за эту рыночную неделю.'),
    G + 'ranking.off': v(
        'The wealth ranking is turned off on this server (wealthRanking).', '本服务器已关闭财富排行（wealthRanking）。',
        'このサーバーでは資産ランキングがオフです（wealthRanking）。', 'Рейтинг богатства на этом сервере выключен (wealthRanking).'),
    G + 'ranking.empty': v('No players to rank yet.', '还没有可排名的玩家。', 'まだ順位を付けるプレイヤーがいません。', 'Пока некого ранжировать.'),
    G + 'ranking.anonymous': v('anonymous', '匿名', '匿名', 'аноним'),
    G + 'ranking.mine': v(
        'You: #%1$s of %2$s (%3$s)', '你：第 %1$s 名 / %2$s 人（%3$s）', 'あなた：%2$s 人中 %1$s 位（%3$s）', 'Вы: %1$s-е место из %2$s (%3$s)'),
    G + 'ranking.mine_place': v('You: #%1$s of %2$s', '你：第 %1$s 名 / %2$s 人', 'あなた：%2$s 人中 %1$s 位', 'Вы: %1$s-е место из %2$s'),
    G + 'ranking.unranked': v(
        'You are not ranked yet (%1$s players)', '你尚未上榜（%1$s 人）', 'まだ順位がありません（%1$s 人）', 'Вас пока нет в рейтинге (игроков: %1$s)'),
    G + 'ranking.note': v(
        'Top 10 · counted every 10 s', '前 10 名 · 最多每 10 秒统计一次', '上位 10 人・集計は最短 10 秒ごと', 'Топ-10 · пересчёт раз в 10 с'),
    G + 'ranking.assets': v('Your assets', '你的资产', 'あなたの資産', 'Ваши активы'),
    G + 'ranking.part.0': v('Coins', '货币', '硬貨', 'Монеты'),
    G + 'ranking.part.1': v('UU-matter', 'UU 物质', 'UU 物質', 'UU-материя'),
    G + 'ranking.part.2': v('Goods', '物品', 'アイテム', 'Товары'),
    G + 'ranking.part.3': v('Coins held', '预留货币', '預けた硬貨', 'В резерве'),
    G + 'ranking.part.4': v('Futures', '期货', '先物', 'Фьючерсы'),
    G + 'ranking.part_help.0': v(
        'Coins on your coin account and in your inventory (offline: as carried at the logout).',
        '货币账户与背包中的货币（离线时：按下线时随身的数量）。', '硬貨口座とインベントリの硬貨（オフライン時はログアウト時の手持ち）。',
        'Монеты на вашем счёте и в инвентаре (не в сети — сколько было при выходе).'),
    G + 'ranking.part_help.1': v(
        'UU-matter on your account and held for orders, at the coin price of the UU board.', '账户中与为订单预留的 UU 物质，按 UU 盘口的货币价格计。',
        '口座と注文のために預けた UU 物質、UU 板の硬貨価格で。', 'UU-материя на счёте и в резерве ордеров по цене UU-стакана в монетах.'),
    G + 'ranking.part_help.2': v(
        'Items in your mailbox, shop, auction, sell orders and commission rewards, at the market\'s bid.',
        '邮箱、商店、拍卖、卖单与委托奖励中的物品，按市场收购价计。', '郵便箱・ショップ・オークション・売り注文・委託の報酬のアイテム、市場の買値で。',
        'Предметы в почтовом ящике, магазине, на аукционе, в ордерах на продажу и наградах поручений по цене покупки рынка.'),
    G + 'ranking.part_help.3': v(
        'Coins held for your buy orders, commission rewards and shop purchases.', '为买单、委托奖励与商店收购预留的货币。',
        '買い注文・委託の報酬・ショップの買取のために預けた硬貨。', 'Монеты в резерве ордеров на покупку, наград поручений и закупок магазина.'),
    G + 'ranking.part_help.4': v(
        'What your open futures would pay at today\'s price.', '未平仓期货按今天价格结算可得的金额。', '未決済の先物を今日の価格で決済したときの額。',
        'Сколько принесли бы открытые фьючерсы по сегодняшней цене.'),
    G + 'ranking.part_total': v('Total', '合计', '合計', 'Итого'),
    G + 'ranking.hidden_on': v('Name hidden', '名字已隐藏', '名前を隠し中', 'Имя скрыто'),
    G + 'ranking.hidden_off': v('Hide name', '隐藏名字', '名前を隠す', 'Скрыть имя'),
    G + 'ranking.hide_help': v(
        'Hidden: the others see "anonymous" in your place; you still see yourself. Also: /mio_icif market wealth hide or show.',
        '隐藏后其他人在你的位置看到“匿名”，你自己仍能看到。也可用：/mio_icif market wealth hide 或 show。',
        '隠すと、ほかの人にはあなたの位置に「匿名」と表示されます（自分には見えます）。コマンド：/mio_icif market wealth hide または show。',
        'Скрыто: другие видят на вашем месте «аноним», вы видите себя. Также: /mio_icif market wealth hide или show.'),
    M + 'ranking_hidden': v(
        '§aYour name is hidden in the wealth ranking.', '§a你的名字已在财富排行中隐藏。', '§a資産ランキングで名前を隠しました。',
        '§aВаше имя скрыто в рейтинге богатства.'),
    M + 'ranking_shown': v(
        '§aYour name is shown in the wealth ranking.', '§a你的名字将显示在财富排行中。', '§a資産ランキングに名前を表示します。',
        '§aВаше имя показывается в рейтинге богатства.'),
    W + 'off': v(
        'The wealth ranking is turned off (wealthRanking).', '财富排行已关闭（wealthRanking）。', '資産ランキングはオフです（wealthRanking）。',
        'Рейтинг богатства выключен (wealthRanking).'),
    W + 'header': v(
        'Wealth ranking, %1$s (%2$s players):', '财富排行：%1$s（%2$s 人）：', '資産ランキング：%1$s（%2$s 人）：', 'Рейтинг богатства: %1$s (игроков: %2$s):'),
    W + 'empty': v('No players to rank yet.', '还没有可排名的玩家。', 'まだ順位を付けるプレイヤーがいません。', 'Пока некого ранжировать.'),
    W + 'row': v(' #%1$s %2$s: %3$s', ' 第 %1$s 名 %2$s：%3$s', ' %1$s 位 %2$s：%3$s', ' %1$s. %2$s: %3$s'),
    W + 'mine': v(
        'You: #%1$s of %2$s (%3$s)', '你：第 %1$s 名 / %2$s 人（%3$s）', 'あなた：%2$s 人中 %1$s 位（%3$s）', 'Вы: %1$s-е место из %2$s (%3$s)'),
    W + 'unranked': v('You are not ranked yet.', '你尚未上榜。', 'まだ順位がありません。', 'Вас пока нет в рейтинге.'),
    W + 'player': v(
        '%1$s: total assets %2$s, place %3$s of %4$s (%5$s)', '%1$s：总资产 %2$s，第 %3$s 名 / %4$s 人（%5$s）',
        '%1$s：総資産 %2$s、%4$s 人中 %3$s 位（%5$s）', '%1$s: всего активов %2$s, место %3$s из %4$s (%5$s)'),
    W + 'hidden': v('name hidden', '名字已隐藏', '名前は非表示', 'имя скрыто'),
    W + 'listed': v('name listed', '名字公开', '名前は表示', 'имя показано'),
    W + 'parts': v(
        ' coins %1$s, UU-matter %2$s, goods %3$s, coins held %4$s, futures %5$s',
        ' 货币 %1$s，UU 物质 %2$s，物品 %3$s，预留货币 %4$s，期货 %5$s',
        ' 硬貨 %1$s、UU 物質 %2$s、アイテム %3$s、預けた硬貨 %4$s、先物 %5$s',
        ' монеты %1$s, UU-материя %2$s, товары %3$s, монеты в резерве %4$s, фьючерсы %5$s'),
    W + 'week': v(
        ' this market week: net trade %1$s, futures profit %2$s, bounties %3$s', ' 本交易周：净交易额 %1$s，期货盈亏 %2$s，悬赏 %3$s',
        ' 今の市場週：純取引額 %1$s、先物の損益 %2$s、懸賞 %3$s', ' за эту рыночную неделю: чистая торговля %1$s, прибыль по фьючерсам %2$s, награды %3$s'),
    W + 'refreshed': v(
        'Wealth ranking counted again: %1$s players in %2$s ms.', '财富排行已重新统计：%1$s 人，用时 %2$s 毫秒。',
        '資産ランキングを再集計しました：%1$s 人、%2$s ms。', 'Рейтинг богатства пересчитан: игроков %1$s за %2$s мс.'),

    # ---------------------------------------------------------------- the Trade Boxes
    'block.mio_icif.producer.block_trade_box': v('Basic Trade Box', '基础交易箱', '基本取引箱', 'Простой торговый ящик'),
    'block.mio_icif.producer.block_trade_box_advanced': v('Advanced Trade Box', '高级交易箱', '上級取引箱', 'Улучшенный торговый ящик'),
    'container.mio_icif.trade_box': v('Basic Trade Box', '基础交易箱', '基本取引箱', 'Простой торговый ящик'),
    'container.mio_icif.trade_box_advanced': v('Advanced Trade Box', '高级交易箱', '上級取引箱', 'Улучшенный торговый ящик'),
    'item.mio_icif.normal.item_trade_box_upgrade': v(
        'Trade Box Upgrade Kit', '交易箱升级套件', '取引箱アップグレードキット', 'Набор улучшения торгового ящика'),
    'item.mio_icif.normal.item_trade_box_upgrade.desc': v(
        'Use on your Basic Trade Box: it becomes an Advanced Trade Box with its contents and settings.',
        '对你的基础交易箱使用：它连同内容与设置变为高级交易箱。', '自分の基本取引箱に使うと、中身と設定はそのままで上級取引箱になります。',
        'Используйте на своём простом торговом ящике: он станет улучшенным, сохранив содержимое и настройки.'),
    'message.mio_icif.trade_box.no_access': v(
        '§cThis Trade Box belongs to %1$s.', '§c这个交易箱属于 %1$s。', '§cこの取引箱は %1$s のものです。', '§cЭтот торговый ящик принадлежит игроку %1$s.'),
    'message.mio_icif.trade_box.upgraded': v(
        '§aThe box is now an Advanced Trade Box.', '§a箱子已升级为高级交易箱。', '§a箱は上級取引箱になりました。', '§aЯщик стал улучшенным торговым ящиком.'),
    B + 'no_owner': v('No owner', '无所有者', '所有者なし', 'Нет владельца'),
    B + 'owner': v('Owner: %1$s', '所有者：%1$s', '所有者：%1$s', 'Владелец: %1$s'),
    B + 'owner_help': v(
        "The box sells for its owner: the owner's sell depth, daily allowance, unlocks and coin account. Only the owner and "
        'operators open it. No energy needed.',
        '箱子为所有者出售：使用所有者的卖出深度、每日额度、解锁与货币账户。只有所有者和管理员可以打开。不需要能量。',
        '箱は所有者のために売ります：所有者の売却深度、1 日の上限、解放状況、硬貨口座を使います。開けるのは所有者とオペレーターだけ。'
        '電力は不要。',
        'Ящик продаёт для владельца: глубина продаж, дневной лимит, открытия и счёт владельца. Открыть его могут только владелец и '
        'операторы. Энергия не нужна.'),
    B + 'mode.0': v('Auction', '拍卖', 'オークション', 'Аукцион'),
    B + 'mode.1': v('Instant', '立即', '即時', 'Сразу'),
    B + 'mode.2': v('Scrap rest', '其余回收', 'スクラップ', 'В утиль'),
    B + 'mode_help.0': v(
        'Daily auction: every 10 seconds the commodities join your auction order at the minimum; what does not sell goes to your '
        'mailbox.',
        '每日拍卖：每 10 秒将商品按最低价加入你的拍卖订单；未售出的部分进入你的邮箱。',
        '日次オークション：10 秒ごとに商品を最低価格で自分のオークション注文に加えます。売れなかった分は郵便箱へ。',
        'Ежедневный аукцион: каждые 10 секунд товары добавляются в ваш аукционный ордер по минимальной цене; непроданное уходит в '
        'почтовый ящик.'),
    B + 'mode_help.1': v(
        'Instant: sold at once along your sell depth at the minimum, while you are online, in batches of 64 (unless the box is full).',
        '立即：在你在线时按最低价沿你的卖出深度立即出售，以 64 件为一批（箱子装满时除外）。',
        '即時：オンラインの間、最低価格で売却深度に沿ってすぐ売ります。64 個単位（箱が満杯のときを除く）。',
        'Сразу: продаётся немедленно по вашей глубине продаж не ниже минимума, пока вы в сети, партиями по 64 (если ящик не полон).'),
    B + 'mode_help.2': v(
        'Scrap rest: sold at once like Instant; what the market no longer pays for goes to the scrap sink, and the scrap comes out '
        'of the return slots.',
        '其余回收：与“立即”一样立即出售；市场不再收购的部分送入废料回收，废料从退回格取出。',
        'スクラップ：即時と同じくすぐ売り、市場が買わなくなった残りはスクラップ回収へ。スクラップは返却スロットから出ます。',
        'В утиль: продаётся сразу, как в режиме «Сразу»; то, за что рынок больше не платит, идёт в утилизацию, а лом выходит из '
        'слотов возврата.'),
    B + 'mode_basic': v('Mode: daily auction', '模式：每日拍卖', 'モード：日次オークション', 'Режим: ежедневный аукцион'),
    B + 'min_percent': v('Min. %% of bid', '最低 %% 收购价', '最低 買値の %%', 'Мин. %% цены'),
    B + 'min_coins': v('Min. coins', '最低货币', '最低 硬貨', 'Мин. монет'),
    B + 'min_percent_help': v(
        "The lowest price, in percent of the market's bid (0 to 100).", '最低价格，为市场收购价的百分比（0 到 100）。',
        '最低価格。市場の買値に対するパーセント（0〜100）。', 'Минимальная цена в процентах от цены покупки рынка (от 0 до 100).'),
    B + 'min_coins_help': v(
        'The lowest price in coins per item (instant sales).', '每件的最低价格（货币，立即出售）。', '1 個あたりの最低価格（硬貨、即時売却）。',
        'Минимальная цена в монетах за предмет (немедленные продажи).'),
    B + 'unit_help': v(
        'Switch the minimum between percent of the bid and coins per item.', '在收购价百分比与每件货币之间切换最低价。',
        '最低価格を買値のパーセントと 1 個あたりの硬貨で切り替えます。', 'Переключить минимум между процентом от цены и монетами за предмет.'),
    B + 'deliver_on': v(
        'Bounties: on (%1$s claimed)', '交付悬赏：开（已接取 %1$s）', '懸賞へ納品：オン（受注 %1$s）', 'Награды: вкл. (%1$s)'),
    B + 'deliver_off': v(
        'Bounties: off (%1$s claimed)', '交付悬赏：关（已接取 %1$s）', '懸賞へ納品：オフ（受注 %1$s）', 'Награды: выкл. (%1$s)'),
    B + 'deliver_help': v(
        'First delivers to the bounties you claimed (Claim on the Bounties tab), within your share; the rewards go to your coin '
        'account, UU account and mailbox.',
        '先向你已接取的悬赏（悬赏页的“接取”）交付，受你的份额限制；奖励进入你的货币账户、UU 账户与邮箱。',
        'まず受注した懸賞（懸賞タブの「受注」）へ、あなたの分担の範囲で納品します。報酬は硬貨口座、UU 口座、郵便箱に入ります。',
        'Сначала сдаёт по наградам, которые вы закрепили (кнопка «Закрепить» на вкладке наград), в пределах вашей доли; награды '
        'поступают на ваш счёт монет, счёт UU и в почтовый ящик. В скобках: сколько наград закреплено.'),
    B + 'offline_on': v('Offline: auction', '离线：拍卖', 'オフライン時：オークション', 'Не в сети: аукцион'),
    B + 'offline_off': v('Offline: wait', '离线：等待', 'オフライン時：待機', 'Не в сети: ждать'),
    B + 'offline_help': v(
        'Instant sales run only while you are online. On: the box uses the daily auction while you are away. Off: it waits.',
        '立即出售只在你在线时进行。开：你不在线时箱子改用每日拍卖。关：等待。',
        '即時売却はオンラインの間だけ行います。オン：いない間は日次オークションを使います。オフ：待機します。',
        'Немедленные продажи идут только пока вы в сети. Вкл.: в ваше отсутствие ящик использует ежедневный аукцион. Выкл.: ждёт.'),
    B + 'today_sold': v('Sold today', '今日售出', '今日の売却', 'Продано'),
    B + 'today_auction': v('To the auction', '加入拍卖', 'オークションへ', 'На аукцион'),
    B + 'today_auction_help': v(
        "Items put into today's auction order; what does not sell returns to your mailbox.", '今天加入拍卖订单的物品；未售出的部分退回你的邮箱。',
        '今日のオークション注文に加えたアイテム。売れなかった分は郵便箱に戻ります。',
        'Предметы, добавленные в сегодняшний аукционный ордер; непроданное вернётся в почтовый ящик.'),
    B + 'today_scrap': v('Scrapped', '回收', 'スクラップ', 'В утиль'),
    B + 'today_bounty': v('To bounties', '交付悬赏', '懸賞へ納品', 'Награды'),
    B + 'today_value': v('%1$s · %2$s c', '%1$s · %2$s 币', '%1$s・%2$s 硬貨', '%1$s · %2$s мон.'),
    B + 'today_value_help': v('%1$s items, %2$s coins', '%1$s 件，%2$s 货币', '%1$s 個、%2$s 硬貨', '%1$s шт., %2$s монет'),
    B + 'status': v('Last run: %1$s', '上次运行：%1$s', '前回の実行：%1$s', 'Статус: %1$s'),
    B + 'status_none': v('not yet', '尚未运行', 'まだ', 'ещё не было'),
    B + 'status_help': v(
        'The result of the last run; the box runs every 10 seconds while its chunk is loaded.', '上次运行的结果；区块加载时箱子每 10 秒运行一次。',
        '前回の実行の結果。チャンクが読み込まれている間、箱は 10 秒ごとに動きます。',
        'Результат последнего запуска; ящик работает каждые 10 секунд, пока его чанк загружен.'),
    B + 'basic_help': v(
        'Hoppers and pipes fill the 27 slots. Every 10 s the commodities join your daily auction order at the minimum; unsold '
        'items go to your mailbox.',
        '漏斗和管道放入 27 格。每 10 秒商品按最低价加入你的每日拍卖订单；未售出的部分进入你的邮箱。',
        'ホッパーやパイプが 27 スロットを満たします。10 秒ごとに商品を最低価格で日次オークションの注文に加え、売れなかった分は郵便箱へ。',
        'Воронки и трубы заполняют 27 слотов. Каждые 10 с товары идут в ваш аукционный ордер дня по минимальной цене; '
        'непроданное — в почту.'),
    B + 'upgrade_hint': v(
        'Use a Trade Box Upgrade Kit on the box for instant sales, a filter and bounty deliveries.',
        '对箱子使用交易箱升级套件，可获得立即出售、过滤与悬赏交付。',
        '取引箱アップグレードキットを使うと、即時売却・フィルター・懸賞への納品が使えます。',
        'Набор улучшения торгового ящика добавит немедленные продажи, фильтр и сдачу по наградам.'),
    B + 'filter': v('Filter', '过滤', 'フィルター', 'Фильтр'),
    B + 'filter_help': v(
        'Click a cell with an item on the cursor: only that commodity is taken. An empty filter takes every commodity; a click with an '
        'empty cursor clears the cell.',
        '手持物品点击一格：只接受该商品。过滤为空时接受所有商品；空手点击清除该格。',
        'カーソルにアイテムを持ってマスをクリック：その商品だけを受け取ります。空のフィルターはすべての商品を受け取り、空のカーソルで'
        'クリックするとそのマスを消します。',
        'Щёлкните по ячейке предметом на курсоре: принимается только этот товар. Пустой фильтр принимает все товары; щелчок пустым '
        'курсором очищает ячейку.'),
    B + 'filter_clear': v('Clear', '清除', '消去', 'Сброс'),
    B + 'returns': v('Returns', '退回', '返却', 'Возврат'),
    B + 'returns_help': v(
        'Items the filter refuses and the scrap; pipes and you take them out.', '过滤不接受的物品与废料；管道或你可以取出。',
        'フィルターが受け取らないアイテムとスクラップ。パイプや手で取り出せます。',
        'Предметы, отклонённые фильтром, и лом; их забирают трубы или вы.'),

    # ---------------------------------------------------------------- server settings
    CFG + 'shopUpkeepPerWeek': v(
        'Shop Upkeep per Offer and Period', '每个报价每期维护费', '取引ごとの 1 期間の維持費', 'Обслуживание магазина за предложение и период'),
    CFG + 'shopUpkeepHours': v('Shop Upkeep Period (hours)', '商店维护周期（小时）', 'ショップ維持費の期間（時間）', 'Период обслуживания магазина (часы)'),
    CFG + 'shopUpkeepMaxPerShop': v(
        'Most Upkeep per Shop and Period', '每家商店每期维护费上限', '1 店 1 期間の維持費の上限', 'Макс. обслуживание за магазин и период'),
    CFG + 'futuresMaxOpenCollateral': v(
        'Most Open Futures Collateral (multiplayer)', '未平仓期货保证金上限（多人）', '未決済の先物の証拠金上限（マルチ）',
        'Макс. обеспечение открытых фьючерсов (сетевая игра)'),
    CFG + 'wealthRanking': v('Wealth Ranking', '财富排行', '資産ランキング', 'Рейтинг богатства'),
}


def main():
    bad = 0
    for lang in LANGS:
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        mine = {k: t[lang] for k, t in KEYS.items() if lang in t}
        if CHECK:
            stray = [k for k, t in mine.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t)]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
            wrong = [k for k, t in mine.items() if data.get(k) != t]
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in mine.items():
            data[key] = t
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(mine))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
