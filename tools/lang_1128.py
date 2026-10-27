#!/usr/bin/env python3
"""0.1.10.28 language values (en_us / zh_cn / ja_jp): the exchange X4 - the coin <-> UU board, the scrap sink, the stabiliser
(treasury tab, config entries), the ID card (item, tooltips, account screen, messages) and the Cards page and SCRAP mode of the
Trade Terminal. Idempotent; keeps the line endings of each file. With --check it only reports missing or different keys.
Usage: lang_1128.py <repo root> [--check]
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
T = 'gui.mio_icif.terminal.'
C = 'gui.mio_icif.card.'
I = 'item.mio_icif.normal.item_id_card'
F = 'mio_icif.configuration.'

KEYS = {
    # ---------------------------------------------------------------- the coin <-> UU board
    G + 'uu_board': ('UU-matter (lots of 100 mB)', 'UU 物质（每份 100 mB）', 'UU 物質（100 mB 単位）'),
    G + 'uu_lot': ('= %s mB of UU-matter (your account: %s mB)', '= %s mB UU 物质（你的账户：%s mB）', '= UU 物質 %s mB（あなたの口座：%s mB）'),
    G + 'uu_board_help': (
        'Players trade UU-matter for coins here: one unit is 100 mB of the UU account, the price is in coins per 100 mB. A sale '
        'holds UU-matter of your account, a purchase puts it there. The market itself never buys or sells UU-matter.',
        '玩家在这里用货币交易 UU 物质：一份为 UU 账户中的 100 mB，价格为每 100 mB 的货币。出售会冻结你账户中的 UU 物质，购买会存入你的账户。'
        '市场本身从不买卖 UU 物质。',
        'ここではプレイヤー同士が UU 物質と硬貨を交換します。1 単位は UU 口座の 100 mB、価格は 100 mB あたりの硬貨です。売りは口座の UU 物質を預かり、'
        '買いは口座へ入れます。市場自身は UU 物質を売買しません。'),
    # ---------------------------------------------------------------- the scrap sink
    G + 'scrap_on': ('Rest to scrap: on', '其余回收为废料：开', '残りをスクラップ：オン'),
    G + 'scrap_off': ('Rest to scrap: off', '其余回收为废料：关', '残りをスクラップ：オフ'),
    G + 'scrap_help': (
        'When the market pays almost nothing for what a sale leaves (the next bid below 5 %% of the price, or below 1 coin), the rest '
        'goes to the recycler instead: scrap at the IC2 recycler\'s rate (1 for 8 items; items with a recycler recipe give its '
        'product). The coins of the sale do not change.',
        '出售后剩余的物品若市场几乎不再收购（下一件的出价低于价格的 5%% 或低于 1 货币），其余改为回收：按 IC2 回收机的比率得到废料（8 件得 1 个；'
        '有回收机配方的物品得到其产物）。出售所得的货币不变。',
        '売却で残った品に市場がほとんど払わないとき（次の 1 個の買値が価格の 5%% 未満か 1 硬貨未満）、残りをリサイクルに回します。IC2 リサイクラーと'
        '同じ率でスクラップになります（8 個で 1 個。リサイクラーのレシピがある品はその産物）。売却で得る硬貨は変わりません。'),
    M + 'box_sold_scrap': ('Sold %s items for %s coins; %s items recycled into %s scrap', '以 %s 货币售出 %s 件……',
                           '%s 個を %s 硬貨で売却。%s 個をリサイクルし、スクラップ %s 個'),
    # ---------------------------------------------------------------- the stabiliser (treasury tab)
    G + 'stabilizer_on': ('Stabiliser: depths %s, net 7 days %s / target %s', '稳定器：深度 %s，7 天净发行 %s / 目标 %s',
                          '安定化：深度 %s、7 日の正味発行 %s / 目標 %s'),
    G + 'stabilizer_off': ('Stabiliser: off (net 7 days %s)', '稳定器：关闭（7 天净发行 %s）', '安定化：オフ（7 日の正味発行 %s）'),
    G + 'stabilizer_help': (
        'When the coins the market created in the last 7 days (minus those it destroyed) pass the target, the sell depths and the daily '
        'allowance shrink a step a day; below 80 %% of the target they grow back. Target: per player who sold in those days, or a share '
        'of the money supply (estimate %s) when that is more. Active sellers: %s.',
        '当市场在最近 7 天发行的货币（减去销毁的）超过目标时，卖出深度与每日额度每天缩小一步；低于目标的 80%% 时恢复。目标：按这几天出售过的玩家计算，'
        '或货币总量的一定比例（估计 %s），取较大者。活跃卖家：%s。',
        '市場が直近 7 日に発行した硬貨（消滅分を引く）が目標を超えると、売りの深度と 1 日の上限が 1 日 1 段ずつ縮みます。目標の 80%% を下回ると戻ります。'
        '目標は、その期間に売ったプレイヤー 1 人あたりの量か、通貨量（推定 %s）の割合の大きい方です。売り手：%s 人。'),
    G + 'scrap_week': ('Scrap sink, 7 days: %s items in, %s scrap out', '废料回收（7 天）：收入 %s 件，产出 %s 个废料',
                       'スクラップの受け皿（7 日）：%s 個を受け取り、スクラップ %s 個'),
    # ---------------------------------------------------------------- the ID card (item)
    I: ('ID Card', 'ID 卡', 'ID カード'),
    I + '.blank': ('Blank. Use it to make it your personal card, or write it for a player at your Trade Terminal.',
                   '空白卡。使用后成为你的个人卡，或在你的交易终端为某位玩家写入。',
                   '白紙。使うとあなたの本人カードになります。取引端末でプレイヤー用に書き込むこともできます。'),
    I + '.holder': ('Holder: %s', '持有人：%s', '持ち主：%s'),
    I + '.personal': ('Personal card: use it to open your exchange account', '个人卡：使用后打开你的交易所账户', '本人カード：使うと取引所の口座を開きます'),
    I + '.delegate': ('Delegate card of %s: %s at their Trade Terminals', '%s 的委任卡：在其交易终端拥有“%s”权限', '%s の委任カード：その取引端末で「%s」'),
    I + '.expiry': ('Valid through day %s', '有效至第 %s 天', '%s 日目まで有効'),
    I + '.serial': ('No. %s', '编号 %s', '番号 %s'),
    'message.mio_icif.card.personal': ('This is now your personal ID card.', '这已是你的个人 ID 卡。', 'これはあなたの本人 ID カードになりました。'),
    'message.mio_icif.card.not_yours': ('This card belongs to %s; it does nothing for you.', '此卡属于 %s，对你无效。', 'このカードは %s のものです。あなたには使えません。'),
    'message.mio_icif.card.hold_personal': ('Hold your personal ID card.', '请手持你的个人 ID 卡。', '本人 ID カードを手に持ってください。'),
    'message.mio_icif.card.status.ok': ('Delegate card of %s: %s, valid for %s more days.', '%s 的委任卡：%s，还有 %s 天有效。',
                                        '%s の委任カード：%s、あと %s 日有効です。'),
    'message.mio_icif.card.status.bad_signature': ('This card is not valid (its data was changed). Issuer %s, %s, %s days.',
                                                   '此卡无效（数据已被改动）。发行者 %s，%s，%s 天。', 'このカードは無効です（データが改変されています）。発行者 %s、%s、%s 日。'),
    'message.mio_icif.card.status.not_holder': ('This card belongs to another player (issuer %s, %s, %s days).', '此卡属于其他玩家（发行者 %s，%s，%s 天）。',
                                                'このカードは別のプレイヤーのものです（発行者 %s、%s、%s 日）。'),
    'message.mio_icif.card.status.expired': ('This delegate card of %s (%s) has expired (%s days).', '%s 的委任卡（%s）已过期（%s 天）。',
                                             '%s の委任カード（%s）は期限切れです（%s 日）。'),
    'message.mio_icif.card.status.revoked': ('%s revoked this delegate card (%s, %s days).', '%s 已吊销此委任卡（%s，%s 天）。',
                                             '%s がこの委任カードを無効にしました（%s、%s 日）。'),
    'message.mio_icif.card.status.wrong_issuer': ('This card of %s (%s, %s days) does not apply here.', '%s 的此卡（%s，%s 天）在此不适用。',
                                                  '%s のこのカード（%s、%s 日）はここでは使えません。'),
    M + 'card.self': ('You need no card for yourself: you own the terminal.', '你无需为自己写卡：终端属于你。', '自分用のカードは不要です。端末はあなたのものです。'),
    M + 'card.full': ('At most %s delegate cards can be open at once; revoke one first.', '最多同时 %s 张委任卡；请先吊销一张。',
                      '委任カードは同時に %s 枚までです。先に 1 枚無効にしてください。'),
    M + 'card.no_blank': ('Carry a blank ID card to write it.', '请携带一张空白 ID 卡以写入。', '書き込むには白紙の ID カードを持ってください。'),
    M + 'card.issued': ('Delegate card written for %s, valid for %s days.', '已为 %s 写入委任卡，有效 %s 天。', '%s 用の委任カードを書き込みました。%s 日有効です。'),
    M + 'card.revoked': ('%s cards revoked.', '已吊销 %s 张卡。', '%s 枚のカードを無効にしました。'),
    M + 'card.none_revoked': ('No card to revoke.', '没有可吊销的卡。', '無効にするカードはありません。'),
    # ---------------------------------------------------------------- the account screen of a personal card
    C + 'title': ('Exchange account', '交易所账户', '取引所の口座'),
    C + 'tab.0': ('Account', '账户', '口座'),
    C + 'tab.1': ('Cards', 'ID 卡', 'カード'),
    C + 'loading': ('Loading...', '加载中...', '読み込み中...'),
    C + 'mail': ('Mailbox: %s coins, %s mB UU-matter, %s items', '邮箱：%s 货币，%s mB UU 物质，%s 件物品', 'メールボックス：硬貨 %s、UU 物質 %s mB、品物 %s 個'),
    C + 'allowance': ('Allowance left today: %s', '今日剩余额度：%s', '今日の残りの上限：%s'),
    C + 'factor': ('Stabiliser: %s', '稳定器：%s', '安定化：%s'),
    C + 'orders': ('Resting orders and auction orders', '挂单与拍卖订单', '板の注文と一括清算の注文'),
    C + 'none': ('None.', '无。', 'ありません。'),
    C + 'order_buy': ('Buy %s: %s at %s', '买入 %s：%s 件，单价 %s', '%s を買い：%s 個、単価 %s'),
    C + 'order_sell': ('Sell %s: %s at %s', '卖出 %s：%s 件，单价 %s', '%s を売り：%s 個、単価 %s'),
    C + 'auction': ('Auction %s: %s (min %s%%)', '拍卖 %s：%s 件（最低 %s%%）', '一括清算 %s：%s 個（最低 %s%%）'),
    C + 'cancel': ('Cancel', '撤回', '取り消し'),
    C + 'remote_help': ('From afar refunds go to the mailbox; collect them at a Futures Machine.', '远程撤回的退款进入邮箱，请在期货机领取。',
                        '遠隔での払い戻しはメールボックスへ入ります。先物取引機で受け取ってください。'),
    C + 'issued': ('Delegate cards you wrote (%s)', '你写入的委任卡（%s）', 'あなたが書いた委任カード（%s）'),
    C + 'revoke_all': ('Revoke all', '全部吊销', 'すべて無効化'),
    C + 'revoke': ('Revoke', '吊销', '無効化'),
    C + 'no_cards': ('None. Write delegate cards at your Trade Terminal (Cards page).', '无。可在你的交易终端（ID 卡页）写入委任卡。',
                     'ありません。委任カードは取引端末の「カード」ページで書けます。'),
    C + 'card_line': ('%s: %s, through day %s (%s days left), No. %s', '%s：%s，至第 %s 天（剩余 %s 天），编号 %s',
                      '%s：%s、%s 日目まで（残り %s 日）、番号 %s'),
    C + 'cards_help': ('A revoked card stops working at once, wherever it is.', '吊销的卡无论在何处都会立即失效。', '無効にしたカードは、どこにあってもすぐに使えなくなります。'),
    # ---------------------------------------------------------------- the Trade Terminal
    T + 'tab.5': ('Cards', 'ID 卡', 'カード'),
    T + 'cards': ('Delegate ID cards (%s / %s)', '委任 ID 卡（%s / %s）', '委任 ID カード（%s / %s）'),
    T + 'cards_help': (
        'A delegate card gives its holder the written level at all your terminals until its last day. It works only for that player; '
        'you can revoke it at any time (here or on your personal card).',
        '委任卡在到期前让其持有人在你的所有终端拥有写入的权限。仅对该玩家有效；你可以随时吊销（在此处或通过你的个人卡）。',
        '委任カードは期限まで、持ち主にあなたのすべての端末で書いた権限を与えます。そのプレイヤーにしか使えず、いつでも無効にできます（ここか本人カードで）。'),
    T + 'cards_owner_only': ('Only the owner writes and revokes delegate cards.', '只有所有者可以写入和吊销委任卡。', '委任カードを書き、無効にできるのは所有者だけです。'),
    T + 'card_days': ('Days', '天数', '日数'),
    T + 'card_write': ('Write card', '写入卡', 'カードを書く'),
    T + 'card_write_help': ('Needs a blank ID card in your inventory; the written card goes to your inventory, hand it to the player.',
                            '需要背包中有一张空白 ID 卡；写好的卡进入你的背包，请交给该玩家。',
                            'インベントリに白紙の ID カードが必要です。書いたカードはインベントリに入るので、そのプレイヤーに渡してください。'),
    T + 'no_cards': ('No delegate card is open.', '没有有效的委任卡。', '有効な委任カードはありません。'),
    T + 'card_line': ('%s: %s, through day %s (%s days left), No. %s', '%s：%s，至第 %s 天（剩余 %s 天），编号 %s',
                      '%s：%s、%s 日目まで（残り %s 日）、番号 %s'),
    T + 'revoke': ('Revoke', '吊销', '無効化'),
    T + 'revoke_all': ('Revoke all', '全部吊销', 'すべて無効化'),
    T + 'cards_foot': ('A card in the wrong hands does nothing: it works only for the player it was written for.',
                       '落入他人之手的卡毫无用处：它只对写入时指定的玩家有效。', '他人の手に渡ったカードは何もできません。書き込んだ相手にしか使えないためです。'),
    T + 'scrap': ('Scrap rest', '其余回收', '残りスクラップ'),
    T + 'order_sell_scrap': ('Keep %s, sell rest now, scrap the unsold (min %s)', '保留 %s，其余立即出售，未售出的回收（最低 %s）',
                             '%s 個残し、残りを即時売却し、売れない分はスクラップ（最低 %s）'),
    T + 'log_scrap': ('Day %s: %s %s recycled into %s scrap', '第 %s 天：%s 个%s回收为 %s 个废料', '%s 日目：%s 個の%sをスクラップ %s 個に'),
}

CONFIG = {
    'uuBoard': ('Coin <-> UU-matter Board', '货币与 UU 物质交易板', '硬貨と UU 物質の板'),
    'scrapThreshold': ('Scrap Sink Threshold (share of the price)', '废料回收阈值（价格比例）', 'スクラップの受け皿のしきい値（価格の割合）'),
    'stabilizerEnabled': ('Stabiliser', '稳定器', '安定化'),
    'stabilizerTargetPerPlayerWeek': ('Stabiliser Target per Seller a Week (coins)', '稳定器目标：每名卖家每周（货币）', '安定化の目標：売り手 1 人あたり週（硬貨）'),
    'stabilizerTargetPercentPerWeek': ('Stabiliser Target: Percent of the Money Supply a Week', '稳定器目标：每周货币总量百分比', '安定化の目標：週あたり通貨量に対する割合'),
    'stabilizerStep': ('Stabiliser Step a Day', '稳定器每日步长', '安定化の 1 日の段階'),
    'stabilizerMinFactor': ('Stabiliser Lowest Factor', '稳定器最低系数', '安定化の最低係数'),
}
for key, values in CONFIG.items():
    KEYS[F + 'FutureMarket.' + key] = values
    KEYS[F + key] = values

# the scrap message of the sell box in zh: written out in full
KEYS[M + 'box_sold_scrap'] = ('Sold %s items for %s coins; %s items recycled into %s scrap', '售出 %s 件，获得 %s 货币；%s 件回收为 %s 个废料',
                              '%s 個を %s 硬貨で売却。%s 個をリサイクルし、スクラップ %s 個')


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
