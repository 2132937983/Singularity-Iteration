#!/usr/bin/env python3
"""0.1.10.35 language values (en_us / zh_cn / ja_jp): commissions in hours and their chat notice, the treasury reserve, the
player shops (list, open shop, own shop, offer form, messages), the hours of book orders, the missing ask, the ledger help
reworded for the reserve, and the eleven new settings. Idempotent; keeps the line endings of each file. With --check it only
reports missing or different keys.
Usage: lang_1135.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
G = 'gui.mio_icif.exchange.'
M = 'message.mio_icif.exchange.'
CF = 'mio_icif.configuration.'

KEYS = {
    # ---- commissions in hours (item 3)
    G + 'hours': ('Hours', '时长（小时）', '時間'),
    G + 'hours_help': (
        'How long the commission stays on the board, in hours while the server runs; - and + step through 1, 3, 6, 12, 24, 48, '
        '72 and 168 hours. When it ends, the unpaid reward returns to your mailbox.',
        '委托在委托板上保留的时间，按服务器运行的小时计算；- 和 + 在 1、3、6、12、24、48、72、168 小时之间切换。到期后未支付的报酬退回你的邮箱。',
        '委託が掲示板に残る時間（サーバーが動いている時間で数えます）。- と + で 1・3・6・12・24・48・72・168 時間を切り替えます。'
        '期限が来ると、未払いの報酬は郵便箱に戻ります。'),
    G + 'hours_left': ('%1$sh %2$sm left', '剩余 %1$s 小时 %2$s 分', '残り %1$s 時間 %2$s 分'),
    G + 'minutes_left': ('%1$sm left', '剩余 %1$s 分', '残り %1$s 分'),
    G + 'hours_short': ('%1$sh', '%1$s 小时', '%1$s 時間'),
    G + 'minutes_short': ('%1$sm', '%1$s 分', '%1$s 分'),
    G + 'notify_on': ('Notice: on', '通知：开', '通知：オン'),
    G + 'notify_off': ('Notice: off', '通知：关', '通知：オフ'),
    G + 'notify_help': (
        'New commissions are announced in the chat to everybody online. Switch it off to stop hearing of them.',
        '新委托会在聊天栏通知所有在线玩家。关闭后你将不再收到这些通知。',
        '新しい委託はオンラインの全員にチャットで知らされます。オフにすると、あなたには知らされなくなります。'),
    # ---- the switch of the commissions tab
    G + 'shop_switch.0': ('Requests', '委托', '委託'),
    G + 'shop_switch.1': ('Shops', '商店', 'ショップ'),
    G + 'shop_switch.2': ('My shop', '我的商店', '自分の店'),
    # ---- the shop list
    G + 'shops_off': ('Player shops are switched off on this server.', '本服务器已关闭玩家商店。', 'このサーバーではプレイヤーショップが無効です。'),
    G + 'shop_filter': ('Filter', '筛选', '絞り込み'),
    G + 'shop_filter_help': (
        'Click, then click an item of your inventory: only shops that sell, buy or swap it are listed.',
        '点击后再点击背包中的物品：只列出出售、收购或交换该物品的商店。',
        'クリックしてから、インベントリのアイテムをクリック：そのアイテムを販売・買取・交換する店だけを表示します。'),
    G + 'shop_filter_clear': ('All shops', '全部商店', 'すべての店'),
    G + 'shop_filter_pick': ('Click the item to filter the shops by', '点击用于筛选商店的物品', '絞り込みに使うアイテムをクリック'),
    G + 'shops_count': ('%1$s shops', '%1$s 家商店', '%1$s 店'),
    G + 'shop_offers': ('%1$s/%2$s offers open', '%1$s/%2$s 个报价可用', '取引可 %1$s/%2$s 件'),
    G + 'shop_idle': ('away', '店主离开', '不在'),
    G + 'shop_idle_help': (
        'The owner has not played for a while: the shop is listed last. After a longer absence it closes and its goods return to '
        "the owner's mailbox.",
        '店主已有一段时间没有上线：该商店排在最后。离开更久后商店会关闭，商品退回店主的邮箱。',
        '店主がしばらく遊んでいません：この店は一覧の最後に並びます。さらに長く不在だと閉店し、商品は店主の郵便箱に戻ります。'),
    G + 'shop_last_trade': ('Last trade: day %1$s', '最近交易：第 %1$s 天', '最終取引：%1$s 日目'),
    G + 'shop_no_trade': ('No trades yet', '尚无交易', 'まだ取引なし'),
    G + 'shop_open': ('Open', '打开', '開く'),
    G + 'shop_mine': ('Mine', '我的', '自分の店'),
    G + 'shops_none': (
        'No player shops yet. Open one under "My shop".', '还没有玩家商店。可在“我的商店”中开店。',
        'まだプレイヤーショップはありません。「自分の店」から出店できます。'),
    G + 'shops_none_item': ('No shop trades this item.', '没有商店交易此物品。', 'このアイテムを扱う店はありません。'),
    # ---- an open shop
    G + 'shop_back': ('Back', '返回', '戻る'),
    G + 'shop_n_help': ('Trades of the xN button', 'xN 按钮的交易次数', 'xN ボタンの取引回数'),
    G + 'shop_src_inv': ('Inventory', '背包', 'インベントリ'),
    G + 'shop_src_settle': ('Container', '容器', 'コンテナ'),
    G + 'shop_src_help': (
        'Where the items you pay with come from: your inventory, or the container in the settlement slot (settlement page). '
        'Coins always come from your inventory and coin account.',
        '支付用物品的来源：背包，或结算槽中的容器（结算页）。货币始终从背包和货币账户支付。',
        '支払うアイテムの出どころ：インベントリ、または精算スロットのコンテナ（精算ページ）。硬貨は常にインベントリと硬貨口座から払います。'),
    G + 'take_help': (
        'You can take %1$s now (your items or coins, the stock, and the room in the shop).',
        '你现在可以交易 %1$s 次（取决于你的物品或货币、库存和商店仓储空间）。',
        '今 %1$s 回取引できます（手持ちのアイテムや硬貨、在庫、店の保管枠による）。'),
    G + 'take_max': ('Max', '最大', '最大'),
    G + 'shop_sold_out': ('Sold out', '已售罄', '売り切れ'),
    G + 'shop_no_offers': ('This shop has no offers.', '该商店没有报价。', 'この店には取引がありません。'),
    G + 'offer.0': ('%1$s x%2$s for %3$s x%4$s', '%1$s x%2$s（用 %3$s x%4$s 交换）', '%1$s x%2$s（%3$s x%4$s と交換）'),
    G + 'offer.1': ('%1$s x%2$s for %3$s coins', '%1$s x%2$s，售价 %3$s 货币', '%1$s x%2$s（%3$s 硬貨）'),
    G + 'offer.2': ('Buys %1$s x%2$s for %3$s coins', '收购 %1$s x%2$s，支付 %3$s 货币', '%1$s x%2$s を %3$s 硬貨で買取'),
    G + 'offer_state': ('%1$s left · %2$s traded', '剩余 %1$s · 已成交 %2$s', '残り %1$s · 取引済み %2$s'),
    G + 'offer_paused': ('paused', '已暂停', '停止中'),
    G + 'offer_unpaid': ('upkeep unpaid', '维护费未付', '維持費未払い'),
    # ---- the own shop
    G + 'my_shop_summary': ('Offers %1$s/%2$s · storage %3$s/%4$s stacks', '报价 %1$s/%2$s · 仓储 %3$s/%4$s 组',
                            '取引 %1$s/%2$s · 保管 %3$s/%4$s スタック'),
    G + 'my_shop_help': (
        'A new offer costs %1$s coins; every offer pays %2$s coins of upkeep each market week from your coins (an offer that '
        'cannot pay is paused). Both go to the treasury reserve. Not online on 7 days the server ran (default): the shop is listed '
        'last; on 30: it closes and everything returns to your mailbox.',
        '发布新报价需 %1$s 货币；每个报价每个市场周从你的货币中支付 %2$s 维护费（付不起的报价会暂停）。两者都进入国库储备。'
        '在服务器运行的日子里有 7 天（默认）未上线：商店排到最后；30 天：商店关闭，所有物品退回邮箱。',
        '新しい取引の出品は %1$s 硬貨。取引ごとに市場週ごと %2$s 硬貨の維持費を硬貨から払います（払えない取引は停止）。'
        'どちらも国庫準備金に入ります。サーバーが動いた日のうち 7 日（既定）ログインしないと店は最後尾に、30 日で閉店し、すべて郵便箱に戻ります。'),
    G + 'shop_collect': ('Collect', '领取', '受け取る'),
    G + 'shop_collect_help': (
        'The proceeds (the items customers paid) into your inventory; what does not fit goes to the mailbox.',
        '将收益（顾客支付的物品）放入背包；放不下的进入邮箱。',
        '売上（客が払ったアイテム）をインベントリへ。入りきらない分は郵便箱へ。'),
    G + 'proceeds': ('Proceeds:', '收益：', '売上：'),
    G + 'proceeds_none': ('none', '无', 'なし'),
    G + 'restock_help': (
        'Restock %1$s trades (the "Trades in stock" box of the form): goods from your inventory; a buy offer takes coins.',
        '补货 %1$s 次交易（表单中的“库存交易次数”）：商品取自背包；收购报价扣除货币。',
        '%1$s 回分を補充します（フォームの「在庫（取引回数）」）。商品はインベントリから、買取の取引は硬貨を使います。'),
    G + 'shop_pause': ('Pause', '暂停', '停止'),
    G + 'shop_resume': ('Resume', '恢复', '再開'),
    G + 'shop_resume_help': (
        "This week's upkeep was not paid: resuming pays %1$s coins now.",
        '本周维护费未支付：恢复时立即支付 %1$s 货币。',
        '今週の維持費が未払いです：再開すると今 %1$s 硬貨を払います。'),
    G + 'shop_withdraw_help': ('Take the offer down: its stock goes to your mailbox.', '下架报价：库存退回你的邮箱。', '取引を取り下げます：在庫は郵便箱に戻ります。'),
    G + 'my_shop_empty': ('No offers yet: fill in the form at the right.', '还没有报价：请填写右侧表单。', 'まだ取引がありません：右のフォームから出品できます。'),
    # ---- the offer form
    G + 'shop_form': ('New offer', '新报价', '新しい取引'),
    G + 'shop_form_help': (
        'Offer fee %1$s coins; upkeep %2$s coins per offer and market week. The goods for sale leave your inventory now; a buy '
        'offer sets its coins aside.',
        '报价手续费 %1$s 货币；每个报价每个市场周维护费 %2$s 货币。出售的商品现在就从背包取出；收购报价会预留所需货币。',
        '出品手数料 %1$s 硬貨、維持費は取引ごとに市場週 %2$s 硬貨。売る商品は今インベントリから預け、買取の取引は硬貨を預けます。'),
    G + 'shop_kind.0': ('Swap', '交换', '交換'),
    G + 'shop_kind.1': ('Sell', '出售', '販売'),
    G + 'shop_kind.2': ('Buy', '收购', '買取'),
    G + 'shop_kind_help.0': ('Swap: customers give the cost items and get your goods.', '交换：顾客交出代价物品，换取你的商品。',
                             '交換：客が代価アイテムを渡し、あなたの商品を受け取ります。'),
    G + 'shop_kind_help.1': (
        'Sell: customers pay coins for your goods (the market fee comes off your proceeds).',
        '出售：顾客用货币购买你的商品（市场手续费从你的收入中扣除）。',
        '販売：客が硬貨で商品を買います（市場手数料はあなたの売上から引かれます）。'),
    G + 'shop_kind_help.2': (
        'Buy: customers bring the cost items and get your coins (the market fee comes off their pay).',
        '收购：顾客交出代价物品，获得你的货币（市场手续费从其所得中扣除）。',
        '買取：客が代価アイテムを持ち込み、あなたの硬貨を受け取ります（市場手数料は客の受取額から引かれます）。'),
    G + 'shop_goods': ('Goods', '商品', '商品'),
    G + 'shop_goods_count': ('Goods per trade', '每次交易的商品数', '1 回の商品数'),
    G + 'shop_cost': ('Cost item', '代价物品', '代価アイテム'),
    G + 'shop_cost_count': ('Cost items per trade', '每次交易的代价数量', '1 回の代価数'),
    G + 'shop_price_sell': ('Coins per trade', '每次交易售价', '1 回の価格（硬貨）'),
    G + 'shop_price_buy': ('Coins you pay per trade', '每次交易支付的货币', '1 回に払う硬貨'),
    G + 'shop_units': ('Trades in stock', '库存交易次数', '在庫（取引回数）'),
    G + 'shop_units_help': (
        'How many trades the offer holds: the goods (or coins) for all of them are set aside now. The + of an offer restocks this '
        'many.',
        '报价可成交的次数：所需的全部商品（或货币）现在就预留。报价的 + 按钮按此数量补货。',
        '取引できる回数：その分の商品（または硬貨）を今預けます。取引の + はこの回数だけ補充します。'),
    G + 'shop_need_items': ('Takes %1$s goods + %2$s coins fee', '需要 %1$s 个商品 + %2$s 货币手续费', '商品 %1$s 個 + 手数料 %2$s 硬貨'),
    G + 'shop_need_coins': ('Takes %1$s coins (fee included)', '需要 %1$s 货币（含手续费）', '%1$s 硬貨（手数料込み）'),
    G + 'shop_post': ('Post offer (%1$s coins)', '发布报价（%1$s 货币）', '出品（%1$s 硬貨）'),
    G + 'shop_post_pick': (
        'Pick the goods and the cost item first (for a swap they must differ).', '请先选择商品和代价物品（交换时两者必须不同）。',
        '先に商品と代価アイテムを選んでください（交換では別のアイテムにします）。'),
    # ---- treasury reserve (item 4)
    G + 'reserve_line': ('Reserve %1$s / %2$s', '储备 %1$s / %2$s', '準備金 %1$s / %2$s'),
    G + 'reserve_help': (
        'Treasury reserve: the fees go in, bounty coins are paid from it first; fees above the cap are destroyed. This market '
        'week: %1$s in, %2$s paid out, %3$s newly created for bounties.',
        '国库储备：手续费存入，悬赏货币优先从储备支付；超过上限的手续费被销毁。本市场周：存入 %1$s，支出 %2$s，为悬赏新产生 %3$s。',
        '国庫準備金：手数料が入り、懸賞の硬貨はまずここから払われます。上限を超えた手数料は消滅します。今の市場週：入金 %1$s、'
        '支払 %2$s、懸賞のための新規発行 %3$s。'),
    G + 'ask_none_help': (
        'The market does not sell this now: it has no stock, or it only buys the item (raw ores).',
        '市场目前不出售此物品：没有库存，或该物品只收购不出售（粗矿）。',
        '市場は今これを売っていません：在庫がないか、買い取り専用のアイテム（原石）です。'),
    G + 'ledger_help': (
        'Left bar: into circulation (market buys, rewards, futures wins); right bar: out of circulation (market sales, fees to the '
        'reserve)',
        '左柱：进入流通（市场收购、报酬、期货盈利）；右柱：退出流通（市场出售、进入储备的手续费）',
        '左の棒: 流通に入った硬貨（市場の買い取り・報酬・先物の利益）、右の棒: 流通から出た硬貨（市場の販売・準備金に入る手数料）'),
    # ---- messages
    M + 'shops_off': ('§cPlayer shops are switched off on this server.', '§c本服务器已关闭玩家商店。', '§cこのサーバーではプレイヤーショップが無効です。'),
    M + 'bad_offer': (
        '§cThat offer is not valid: pick the items and set the counts and the price.', '§c报价无效：请选择物品并设置数量和价格。',
        '§c取引の内容が不正です：アイテムを選び、数と価格を設定してください。'),
    M + 'no_offer': ('§cThat offer is gone.', '§c该报价已不存在。', '§cその取引はもうありません。'),
    M + 'shop_offers_full': ('§cYour shop has the most offers already (%1$s).', '§c你的商店报价已达上限（%1$s）。', '§c店の取引数が上限（%1$s）です。'),
    M + 'shop_storage_full': (
        "§cNot enough room in the shop's storage (stock and proceeds).", '§c商店仓储空间不足（库存与收益）。',
        '§c店の保管枠（在庫と売上）が足りません。'),
    M + 'shop_posted': ('§aOffer posted with %1$s trades in stock.', '§a报价已发布，库存 %1$s 次交易。', '§a在庫 %1$s 回分で出品しました。'),
    M + 'shop_restocked': ('§a%1$s trades added to the stock.', '§a已补货 %1$s 次交易。', '§a%1$s 回分を補充しました。'),
    M + 'shop_withdrawn': ('§eOffer taken down; its stock went to your mailbox.', '§e报价已下架，库存已退回你的邮箱。', '§e取引を取り下げました。在庫は郵便箱に戻りました。'),
    M + 'shop_paused': ('§eOffer paused.', '§e报价已暂停。', '§e取引を停止しました。'),
    M + 'shop_resumed': ('§aOffer resumed.', '§a报价已恢复。', '§a取引を再開しました。'),
    M + 'shop_no_proceeds': ('§cNo proceeds to collect.', '§c没有可领取的收益。', '§c受け取る売上がありません。'),
    M + 'shop_collected': ('§a%1$s items collected.', '§a已领取 %1$s 个物品。', '§a%1$s 個を受け取りました。'),
    M + 'shop_own': ('§cThat is your own shop.', '§c这是你自己的商店。', '§cそれは自分の店です。'),
    M + 'shop_sold_out': ('§cSold out.', '§c已售罄。', '§c売り切れです。'),
    M + 'shop_offer_paused': ('§cThat offer is paused.', '§c该报价已暂停。', '§cその取引は停止中です。'),
    M + 'shop_traded': ('§aTraded %1$s times.', '§a成交 %1$s 次。', '§a%1$s 回取引しました。'),
    M + 'shop_closed_notice': (
        '§eYour shop closed while you were away; its stock, coins and proceeds are in your exchange mailbox.',
        '§e你离开期间商店已关闭；其库存、货币和收益已放入你的交易所邮箱。',
        '§e不在の間に店が閉じました。在庫・硬貨・売上は取引所の郵便箱にあります。'),
    M + 'commission_notice_on': ('§aNew commissions will be announced to you in the chat.', '§a新委托将在聊天栏通知你。', '§a新しい委託をチャットで知らせます。'),
    M + 'commission_notice_off': ('§eNew commissions will no longer be announced to you.', '§e将不再通知你新委托。', '§e新しい委託を知らせないようにしました。'),
    M + 'commission_new': (
        '§e[Commission] %4$s asks for %2$s × %1$s for %3$s (%5$s hours).',
        '§e[委托] %4$s 求购 %2$s 个 %1$s，报酬 %3$s（%5$s 小时）。',
        '§e[委託] %4$s が %1$s を %2$s 個求めています。報酬 %3$s（%5$s 時間）。'),
    M + 'commission_reward_coins': ('%1$s coins', '%1$s 货币', '%1$s 硬貨'),
    M + 'commission_reward_uu': ('%1$s UU-matter', '%1$s UU 物质', 'UU 物質 %1$s'),
    M + 'commission_reward_items': ('%2$s × %1$s', '%2$s 个 %1$s', '%1$s %2$s 個'),
}

CONFIG = {
    'commissionDefaultHours': ('Default Hours of a Commission', '委托默认时长（小时）', '委託の既定の時間'),
    'commissionMaxHours': ('Most Hours of a Commission', '委托最长时长（小时）', '委託の最長時間'),
    'orderHours': ('Hours of a Resting Order', '挂单有效时长（小时）', '指値注文の有効時間'),
    'reserveWeeks': ('Treasury Reserve (Weeks of Bounty Coins)', '国库储备上限（悬赏货币周数）', '国庫準備金の上限（懸賞硬貨の週数）'),
    'shopsEnabled': ('Player Shops', '玩家商店', 'プレイヤーショップ'),
    'shopMaxOffers': ('Offers per Shop', '每家商店的报价数', '1 店あたりの取引数'),
    'shopStacks': ('Shop Storage (Stacks)', '商店仓储（组）', '店の保管枠（スタック）'),
    'shopOfferFee': ('Fee of a Shop Offer', '商店报价手续费', '出品手数料'),
    'shopUpkeepPerWeek': ('Shop Upkeep per Offer and Week', '每个报价每周维护费', '取引ごとの週の維持費'),
    'shopIdleDays': ('Days Away Until a Shop Is Listed Last', '店主离开多少天后商店排到最后', '店が最後尾になるまでの不在日数'),
    'shopCloseDays': ('Days Away Until a Shop Closes', '店主离开多少天后商店关闭', '店が閉じるまでの不在日数'),
}
for name, t in CONFIG.items():
    KEYS[CF + 'FutureMarket.' + name] = t
    KEYS[CF + name] = t


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    bad = 0
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        if CHECK:
            stray = [k for k, t in KEYS.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t[i])]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
            wrong = [k for k, t in KEYS.items() if data.get(k) != t[i]]
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in KEYS.items():
            data[key] = t[i]
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(KEYS))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
