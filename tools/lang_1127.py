#!/usr/bin/env python3
"""0.1.10.27 language values (en_us / zh_cn / ja_jp): the exchange X2 - the Trade Terminal (block, panel pages Buffer /
Orders / Access / Log, order statuses, access levels, messages) and the settlement slot (the Settlement page of the futures
panel, its messages). Idempotent; keeps the line endings of each file. With --check it only reports missing or different keys.
Usage: lang_1127.py <repo root> [--check]
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
S = G + 'settle.'
M = 'message.mio_icif.exchange.'
T = 'gui.mio_icif.terminal.'
MT = M + 'terminal.'

KEYS = {
    'block.mio_icif.producer.block_trade_terminal': ('Trade Terminal', '交易终端', '取引端末'),
    'container.mio_icif.trade_terminal': ('Trade Terminal', '交易终端', '取引端末'),

    # ---------------------------------------------------------------- futures panel: Settlement page
    G + 'tab.7': ('Settlement', '大宗结算', '一括決済'),
    S + 'title': ('Settlement container', '结算容器', '決済コンテナ'),
    S + 'kind.0': ('Put a container in', '放入一个容器', 'コンテナを入れてください'),
    S + 'kind.1': ('Item container', '物品容器', 'アイテムコンテナ'),
    S + 'kind.2': ('AE2 storage cell', 'AE2 存储元件', 'AE2 ストレージセル'),
    S + 'kind.3': ('UU-matter container', 'UU 物质容器', 'UU 物質の容器'),
    S + 'help': (
        'A shulker box, a backpack, an AE2 storage cell or a container of UU-matter. Its items sell and deliver like the items '
        'you carry. It comes back to you when you close the panel.',
        '潜影盒、背包、AE2 存储元件或装有 UU 物质的容器。其中的物品与身上携带的物品一样出售和交付。关闭界面时它会回到你手中。',
        'シュルカーボックス、バックパック、AE2 ストレージセル、UU 物質の入った容器。中身は手持ちの品と同じように売却・納品できます。'
        'パネルを閉じると手元に戻ります。'),
    S + 'quote': ('Quote of the container', '容器报价', 'コンテナの見積'),
    S + 'empty': ('The slot is empty.', '槽位为空。', 'スロットは空です。'),
    S + 'no_commodity': ('Nothing in it sells as a commodity.', '其中没有可作为商品出售的物品。', '商品として売れる品は入っていません。'),
    S + 'other': ('Items that sell as nothing', '无法出售的物品', '売れない品'),
    S + 'sell_now_help': (
        'Sells the container\'s commodities now, each down the bid curve and within your daily allowance. What the market does '
        'not take stays in the container.',
        '立即出售容器中的商品，各自沿出价曲线并在你的每日额度之内。市场未收购的部分留在容器中。',
        'コンテナの商品をいますぐ売ります。品目ごとに買値曲線に沿い、1 日の上限の範囲内です。市場が引き取らなかった分はコンテナに残ります。'),
    S + 'uu': ('UU-matter in it: %s mB', '其中 UU 物质：%s mB', '中の UU 物質：%s mB'),
    S + 'uu_in': ('UU to account', 'UU 存入账户', 'UU を口座へ'),
    S + 'buy': ('Buy %s into it', '购买 %s 放入容器', '%s を買って格納'),
    S + 'buy_help': (
        'Buys the market selected on the Market tab now (price limit and quantity of the order ticket), paid with the coins you '
        'carry. The items go into the container; what does not fit goes to your inventory, then to the mailbox.',
        '立即购买在市场页选中的商品（使用订单票的价格上限和数量），用身上的货币支付。物品放入容器；放不下的进入背包，再进入邮箱。',
        '市場タブで選んだ品目をいま買います（注文票の上限価格と数量）。支払いは手持ちの硬貨です。品物はコンテナへ入り、'
        '入りきらない分はインベントリ、次にメールボックスへ送られます。'),
    S + 'buy_go': ('Buy', '购买', '購入'),
    S + 'limit_help': ('Highest price per item', '每件最高价格', '1 個あたりの上限価格'),
    S + 'targets': ('It can deliver to', '可交付给', '納品できる先'),
    S + 'no_targets': ('No bounty or commission needs its items.', '没有悬赏或委托需要其中的物品。', 'その品を必要とする懸賞・依頼はありません。'),
    S + 'target_bounty': ('Bounty: %s, %s items', '悬赏：%s，%s 件', '懸賞：%s、%s 個'),
    S + 'target_commission': ('Commission: %s, %s items', '委托：%s，%s 件', '依頼：%s、%s 個'),
    M + 'settle_nothing': ('Nothing in the container sells.', '容器中没有可出售的物品。', 'コンテナに売れる品がありません。'),
    M + 'settle_empty': ('Put a container into the settlement slot.', '请在结算槽中放入容器。', '決済スロットにコンテナを入れてください。'),
    M + 'settle_no_items': ('This container holds no items.', '该容器不能存放物品。', 'このコンテナには品物を入れられません。'),

    # ---------------------------------------------------------------- Trade Terminal panel
    T + 'tab.0': ('Buffer', '缓冲区', 'バッファ'),
    T + 'tab.1': ('Orders', '常设订单', '常設注文'),
    T + 'tab.2': ('Access', '权限', 'アクセス'),
    T + 'tab.3': ('Log', '记录', '履歴'),
    T + 'tab.4': ('Settlement', '大宗结算', '一括決済'),
    T + 'loading': ('Loading...', '加载中...', '読み込み中...'),
    T + 'no_owner': ('No owner: the terminal does nothing', '无所有者：终端不会工作', '所有者なし：端末は動きません'),
    T + 'owner': ('Owner: %s', '所有者：%s', '所有者：%s'),
    T + 'owner_help': (
        'Your access: %s. Energy %s / %s EU. Every trade is the owner\'s: the coins go to and come from the owner\'s mailbox. '
        'Green: the owner is online; yellow: offline (only the daily auction runs).',
        '你的权限：%s。能量 %s / %s EU。所有交易都属于所有者：货币进出所有者的邮箱。绿色：所有者在线；黄色：离线（只运行每日拍卖）。',
        'あなたの権限：%s。エネルギー %s / %s EU。取引はすべて所有者のもので、硬貨は所有者のメールボックスに出入りします。'
        '緑：所有者がオンライン、黄：オフライン（日次オークションだけが動きます）。'),
    T + 'sell_buffer': ('Sell buffer', '出售缓冲区', '売りバッファ'),
    T + 'sell_buffer_help': (
        'Pipes and hoppers put commodities in here (also items of a commodity\'s tag). Pipes cannot take anything out. The sell '
        'orders sell from it.',
        '管道和漏斗把商品放入这里（也包括商品标签中的物品）。管道无法从中取出物品。出售订单从这里出售。',
        'パイプやホッパーが商品を搬入します（商品のタグに含まれる品も可）。パイプは取り出せません。売り注文はここから売ります。'),
    T + 'output_buffer': ('Output buffer', '输出缓冲区', '出力バッファ'),
    T + 'settle': ('Settlement slot', '结算槽', '決済スロット'),
    T + 'settle_help': (
        'A container of yours: sell its commodities as yourself, put its UU-matter on your account, load it into the sell buffer, '
        'or unload the output buffer into it.',
        '你自己的容器：以你的身份出售其中的商品，把其中的 UU 物质存入你的账户，装入出售缓冲区，或把输出缓冲区卸入其中。',
        'あなたのコンテナ：中の商品をあなたとして売る、UU 物質をあなたの口座へ入れる、売りバッファへ積む、出力バッファを取り出して入れる。'),
    T + 'settle_quote': ('%s items: now %s, auction %s', '%s 件：立即 %s，拍卖 %s', '%s 個：即時 %s、オークション %s'),
    T + 'settle_quote_help': (
        'What the container\'s commodities would pay you now, and at the daily auction if the day changed now.',
        '容器中的商品现在可为你带来的货币，以及若此刻换日时每日拍卖的估计。',
        'コンテナの商品がいま払われる額と、いま日が変わった場合の日次オークションの見込み。'),
    T + 'settle_uu': ('UU-matter: %s mB', 'UU 物质：%s mB', 'UU 物質：%s mB'),
    T + 'sell_now': ('Sell now', '立即出售', '即時売却'),
    T + 'sell_auction': ('Auction', '拍卖', 'オークション'),
    T + 'min_percent': ('Min. %%', '最低 %%', '最低 %%'),
    T + 'uu_in': ('UU-matter to my account', 'UU 物质存入我的账户', 'UU 物質を自分の口座へ'),
    T + 'load': ('Load into sell buffer', '装入出售缓冲区', '売りバッファへ積む'),
    T + 'load_help': (
        'Moves the container\'s commodities into the sell buffer. They then belong to the terminal and are sold for its owner.',
        '把容器中的商品移入出售缓冲区。之后它们属于终端，并为所有者出售。',
        'コンテナの商品を売りバッファへ移します。以後それらは端末のもので、所有者のために売られます。'),
    T + 'unload': ('Unload output buffer', '卸出输出缓冲区', '出力バッファを取り出す'),
    T + 'unload_help': ('Moves the output buffer into the container (configure access).', '把输出缓冲区移入容器（需要设置权限）。',
                        '出力バッファをコンテナへ移します（設定権限が必要）。'),
    T + 'terminal': ('Terminal', '终端', '端末'),
    T + 'output_on': ('Output mode: on', '输出模式：开', '出力モード：オン'),
    T + 'output_off': ('Output mode: off', '输出模式：关', '出力モード：オフ'),
    T + 'output_help': (
        'On: purchases go to the output buffer (pipes take them out) and the buy orders count its stock. Off: purchases go to the '
        'owner\'s mailbox and the buy orders count the mailbox.',
        '开：购买的物品进入输出缓冲区（管道可取出），购买订单按其库存计算。关：购买的物品进入所有者的邮箱，购买订单按邮箱计算。',
        'オン：購入品は出力バッファへ入り（パイプで取り出せます）、買い注文はその在庫を数えます。オフ：購入品は所有者のメールボックスへ入り、'
        '買い注文はメールボックスを数えます。'),
    T + 'run_now': ('Run orders now', '立即运行订单', 'いま注文を実行'),
    T + 'buy_into': ('Buy into it:', '购买放入：', '購入して格納：'),
    T + 'buy_qty': ('Quantity', '数量', '数量'),
    T + 'buy_into_help': (
        'Buys the commodity now as yourself (price limit and quantity), paid with the coins you carry; the items go into the '
        'container, the rest to your inventory, then your mailbox.',
        '以你的身份立即购买该商品（价格上限与数量），用身上的货币支付；物品放入容器，其余进入背包，再进入邮箱。',
        'あなたとしてその品目をいま買います（上限価格と数量）。支払いは手持ちの硬貨で、品物はコンテナへ、残りはインベントリ、次にメールボックスへ入ります。'),
    T + 'owner_coins': ('Owner\'s mailbox: %s coins', '所有者邮箱：%s 货币', '所有者のメールボックス：%s 硬貨'),
    T + 'energy': ('Energy %s / %s EU', '能量 %s / %s EU', 'エネルギー %s / %s EU'),
    T + 'your_level': ('Your access: %s', '你的权限：%s', 'あなたの権限：%s'),
    T + 'level.0': ('None', '无', 'なし'),
    T + 'level.1': ('View', '查看', '閲覧'),
    T + 'level.2': ('Trade', '交易', '取引'),
    T + 'level.3': ('Configure', '设置', '設定'),
    T + 'level.4': ('Owner', '所有者', '所有者'),
    T + 'orders': ('Standing orders (%s / %s)', '常设订单（%s / %s）', '常設注文（%s / %s）'),
    T + 'orders_help': (
        'The orders run every 10 seconds as trades of the owner, under the same rules as manual trades. While the owner is '
        'offline only the auction sell orders run (not for commodities that need an unlock). Instant sales and purchases trade in '
        'batches of 64.',
        '订单每 10 秒以所有者的交易运行一次，规则与手动交易相同。所有者离线时只运行拍卖出售订单（需要解锁的商品除外）。立即出售和购买以 64 件为一批。',
        '注文は 10 秒ごとに所有者の取引として実行され、手動の取引と同じ規則に従います。所有者がオフラインの間はオークションの売り注文だけが動きます'
        '（解除が必要な品目は除く）。即時売却と購入は 64 個単位でまとめて行います。'),
    T + 'no_orders': ('No standing order yet.', '还没有常设订单。', '常設注文はまだありません。'),
    T + 'order_buy': ('Buy up to %s, max %s each', '买到 %s 件，每件最高 %s', '%s 個まで買う、1 個最大 %s'),
    T + 'order_sell_auction': ('Keep %s, rest to auction (min %s%%)', '保留 %s，其余拍卖（最低 %s%%）', '%s 個残し、残りをオークション（最低 %s%%）'),
    T + 'order_sell_now': ('Keep %s, sell rest now (min %s)', '保留 %s，其余立即出售（最低 %s）', '%s 個残し、残りを即時売却（最低 %s）'),
    T + 'status.0': ('Traded', '已成交', '取引済み'),
    T + 'status.1': ('Off', '已关闭', 'オフ'),
    T + 'status.2': ('Nothing to do', '无需操作', '対象なし'),
    T + 'status.3': ('Owner offline', '所有者离线', '所有者オフライン'),
    T + 'status.4': ('Locked', '未解锁', '未解除'),
    T + 'status.5': ('No energy', '能量不足', 'エネルギー不足'),
    T + 'status.6': ('No coins', '货币不足', '硬貨不足'),
    T + 'status.7': ('No market', '无市场', '市場なし'),
    T + 'status.8': ('Price limit', '价格限制', '価格制限'),
    T + 'status.9': ('Collecting', '凑批中', 'まとめ待ち'),
    T + 'status_help.0': ('The order traded in the last run.', '该订单在上次运行中成交。', '前回の実行で取引しました。'),
    T + 'status_help.1': ('The order is switched off.', '该订单已关闭。', '注文はオフです。'),
    T + 'status_help.2': ('Nothing above the kept amount, or the stock is at the target.', '没有超过保留数量的物品，或库存已达目标。',
                          '残す数を超える品がないか、在庫が目標に達しています。'),
    T + 'status_help.3': (
        'Waits for the owner: while the owner is offline only auction sell orders run, and not for commodities that need an unlock.',
        '等待所有者：所有者离线时只运行拍卖出售订单，需要解锁的商品除外。',
        '所有者を待っています。オフラインの間はオークションの売り注文だけが動き、解除が必要な品目は対象外です。'),
    T + 'status_help.4': ('The owner has not unlocked this commodity.', '所有者尚未解锁该商品。', '所有者がこの品目を解除していません。'),
    T + 'status_help.5': ('The terminal lacks the energy of a trade.', '终端缺少一次交易所需的能量。', '端末に取引 1 回分のエネルギーがありません。'),
    T + 'status_help.6': ('The owner\'s mailbox has not enough coins for one item at the price limit.', '所有者邮箱中的货币不足以按价格上限购买一件。',
                          '所有者のメールボックスに、上限価格で 1 個買う硬貨がありません。'),
    T + 'status_help.7': ('Not a datapack commodity of the market (any more).', '（已）不是市场的数据包商品。', '市場のデータパック商品ではありません（なくなりました）。'),
    T + 'status_help.8': ('The market does not trade at the price limit now.', '市场目前不按该价格上限交易。', '市場はいま上限価格では取引しません。'),
    T + 'status_help.9': (
        'Waits for a batch: an instant sale for 64 items above the kept amount (or a full buffer), a purchase until 64 are missing '
        '(or the whole target when it is smaller). Every trade rounds its coins, so one-item trades would lose value.',
        '等待凑够一批：立即出售需要超过保留数量 64 件（或缓冲区已满），购买需要缺少 64 件（目标更小时为整个目标）。每笔交易都会对货币取整，'
        '逐件交易会损失价值。',
        'まとまるのを待っています。即時売却は残す数を 64 個超えるまで（またはバッファが満杯になるまで）、購入は 64 個（目標がそれより小さいなら目標全部）'
        '足りなくなるまで待ちます。取引ごとに硬貨が端数処理されるので、1 個ずつの取引は損になります。'),
    T + 'edit': ('Edit', '编辑', '編集'),
    T + 'new_order': ('New order', '新订单', '新しい注文'),
    T + 'edit_order': ('Edit order %s', '编辑订单 %s', '注文 %s を編集'),
    T + 'buy': ('Buy', '购买', '買い'),
    T + 'sell': ('Sell', '出售', '売り'),
    T + 'instant': ('Instant', '立即', '即時'),
    T + 'auction': ('Auction', '拍卖', 'オークション'),
    T + 'mode_help': (
        'Auction: the items join the owner\'s daily auction order (also while the owner is offline). Instant: sold at once down '
        'the bid curve at the minimum price (only while the owner is online).',
        '拍卖：物品加入所有者的每日拍卖订单（所有者离线时也运行）。立即：沿出价曲线按最低价格立即出售（仅在所有者在线时）。',
        'オークション：品物を所有者の日次オークション注文に加えます（オフライン中も動作）。即時：最低価格を守って買値曲線に沿っていま売ります'
        '（所有者がオンラインのときのみ）。'),
    T + 'item_help': ('Scroll or use the arrows to pick the commodity.', '滚动或使用箭头选择商品。', 'スクロールか矢印で品目を選びます。'),
    T + 'target': ('Target', '目标', '目標'),
    T + 'keep': ('Keep', '保留', '残す数'),
    T + 'max_price': ('Max. price', '最高价格', '上限価格'),
    T + 'min_price': ('Min. price', '最低价格', '最低価格'),
    T + 'enabled': ('On', '开', 'オン'),
    T + 'disabled': ('Off', '关', 'オフ'),
    T + 'save': ('Save order', '保存订单', '注文を保存'),
    T + 'new': ('New', '新建', '新規'),
    T + 'access': ('Access list (%s / %s)', '权限列表（%s / %s）', 'アクセスリスト（%s / %s）'),
    T + 'access_help': ('Only the owner edits the access list and the team level.', '只有所有者可以编辑权限列表和队伍权限。',
                        'アクセスリストとチームの権限を編集できるのは所有者だけです。'),
    T + 'no_access': ('Only the owner (and operators) may use the terminal.', '只有所有者（和管理员）可以使用终端。',
                      '端末を使えるのは所有者（と管理者）だけです。'),
    T + 'add_player': ('Player', '玩家', 'プレイヤー'),
    T + 'add': ('Add', '添加', '追加'),
    T + 'team_level': ('Members of the owner\'s team', '所有者队伍的成员', '所有者のチームのメンバー'),
    T + 'team_help': ('The access of players in the owner\'s scoreboard team.', '所有者计分板队伍中玩家的权限。',
                      '所有者のスコアボードチームに属するプレイヤーの権限。'),
    T + 'levels_help': (
        'View: open and see. Trade: fill the sell buffer, use the settlement slot as yourself. Configure: orders, output mode, '
        'take items out of the buffers. Operators count as Configure.',
        '查看：打开和查看。交易：装入出售缓冲区，以自己的身份使用结算槽。设置：订单、输出模式、从缓冲区取出物品。管理员视为设置权限。',
        '閲覧：開いて見る。取引：売りバッファへ入れる、決済スロットを自分として使う。設定：注文、出力モード、バッファからの取り出し。管理者は設定と同等です。'),
    T + 'log': ('Trades of the orders', '订单的交易记录', '注文の取引履歴'),
    T + 'no_log': ('No trade yet.', '还没有交易。', '取引はまだありません。'),
    T + 'log_sold': ('Day %s: sold %s %s for %s', '第 %s 天：出售 %s 个%s，获得 %s', '%s 日目：%s 個の%sを %s で売却'),
    T + 'log_auction': ('Day %s: %s %s to the auction', '第 %s 天：%s 个%s交给拍卖', '%s 日目：%s 個の%sをオークションへ'),
    T + 'log_bought': ('Day %s: bought %s %s for %s', '第 %s 天：购买 %s 个%s，花费 %s', '%s 日目：%s 個の%sを %s で購入'),

    # ---------------------------------------------------------------- messages
    'message.mio_icif.terminal.no_access': ('This Trade Terminal belongs to %s.', '此交易终端属于 %s。', 'この取引端末は %s のものです。'),
    MT + 'denied': ('Your access to this terminal does not allow that.', '你在此终端的权限不允许此操作。', 'この端末でのあなたの権限ではできません。'),
    MT + 'bad_order': ('That order is not valid.', '该订单无效。', 'その注文は無効です。'),
    MT + 'orders_full': ('A terminal holds at most %s orders.', '一个终端最多 %s 个订单。', '端末に置ける注文は最大 %s 件です。'),
    MT + 'order_saved': ('Order saved.', '订单已保存。', '注文を保存しました。'),
    MT + 'order_removed': ('Order removed.', '订单已删除。', '注文を削除しました。'),
    MT + 'no_player': ('No player named %s is known to the server.', '服务器不认识名为 %s 的玩家。', '%s という名前のプレイヤーはサーバーに知られていません。'),
    MT + 'access_full': ('The access list holds at most %s players.', '权限列表最多 %s 名玩家。', 'アクセスリストは最大 %s 人です。'),
    MT + 'access_saved': ('Access of %s saved.', '已保存 %s 的权限。', '%s の権限を保存しました。'),
    MT + 'moved': ('%s items moved.', '已移动 %s 件物品。', '%s 個を移しました。'),
    MT + 'nothing_moved': ('Nothing to move, or no room.', '没有可移动的物品，或没有空间。', '移せる品がないか、空きがありません。'),
    MT + 'ran': ('The orders ran.', '订单已运行。', '注文を実行しました。'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    bad = 0
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        if CHECK:
            # a % that is not a format code: shown raw by Component.translatable, an error through String.format
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
