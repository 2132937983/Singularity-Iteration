#!/usr/bin/env python3
"""0.1.10.29: the guide lines of the release, en / zh / ja. Futures Machine page: the coin account, the minimum-price sale, the
number fields; Trade Terminal page: percent minimums of sell orders, the settlement minimum and coins onto the account; Block Cutter
page: the planks of each wood. Each block goes after its anchor line (the line that starts with the anchor text). Idempotent; keeps
the line endings of edited files. With --check it only reports what is missing. Usage: data_1129.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
GUIDE = os.path.join(TREE, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual/')
missing = []

FUTURES = {
    '': ('- **ID card**:',
         '- **Coin account**: the coins the exchange keeps for you (the coins of the mailbox). Deposit the coins of the inventory or of the '
         'settlement container, and withdraw them, in the Orders and Mail tab (amount 0 = all). Purchases, buy orders, commissions and '
         'futures collateral take from the inventory and the account together. In bank mode sales and payouts go to the account and '
         'payments take from it first, so large trades do not fill the inventory.\n'
         '- **Minimum price**: the sell and settlement pages take a minimum, in percent of the bid without pressure or in coins per item. '
         'An instant sale sells of each commodity the most items whose own price is still at least the minimum, in one batch, and pays '
         'the sum at once; the page shows what each commodity sells for before you click. In the order panel a sale can name its limit '
         'in percent (the c / % button), and the panel shows what the market part of the sale pays now.\n'
         '- **Number fields**: a click selects the number, so typing replaces it; the mouse wheel and the arrow keys change it (Shift: 64, '
         'Ctrl: 10).\n'),
    '_zh_cn/': ('- **ID 卡**',
                '- **货币账户**：交易所为你保管的货币（邮箱中的货币）。在“订单与邮箱”页存入物品栏或大宗结算容器中的货币、取出货币'
                '（数量 0 = 全部）。购买、买单、委托与期货保证金从物品栏和账户合计支付。银行模式下，销售与支付所得进入账户，付款优先从账户扣除，'
                '大额交易不会塞满物品栏。\n'
                '- **最低价**：出售页与大宗结算页可设定最低价，单位为无抛压买价的百分比或每件货币。立即出售时，每种商品卖出自身价格仍不低于'
                '最低价的最多件数，一次成交并一次支付总额；点击前页面就会显示每种商品能卖多少。在下单面板中，出售的限价可用百分比设定'
                '（c / % 按钮），面板会显示此次出售中市场部分现在的支付额。\n'
                '- **数字输入框**：点击会选中数字，输入即可替换；鼠标滚轮与方向键可增减（Shift：64，Ctrl：10）。\n'),
    '_ja_jp/': ('- **ID カード**',
                '- **硬貨口座**: 取引所があなたのために預かる硬貨（メールボックスの硬貨）。「注文とメール」タブで、インベントリや一括決済'
                'コンテナの硬貨を入金し、出金する（数量 0 = 全部）。購入・買い注文・依頼・先物の証拠金は、インベントリと口座の合計から払う。'
                'バンクモードでは売上や支払いが口座へ入り、支払いは口座から先に引かれるので、大口の取引でもインベントリが埋まらない。\n'
                '- **最低価格**: 売却ページと一括決済ページで、最低価格を売り圧のない買値に対するパーセントか 1 個あたりの硬貨で指定できる。'
                '即時売却は品目ごとに、1 個ずつの価格がまだ最低価格以上である最大の個数を一括で売り、合計額を一度に払う。クリックする前に、'
                '各品目がいくらで売れるかがページに出る。注文パネルでは売りの指値をパーセントで指定でき（c / % ボタン）、その売りのうち市場が'
                'いま払う額が表示される。\n'
                '- **数値欄**: クリックすると数値が選択され、入力で置き換わる。マウスホイールと矢印キーで増減する（Shift：64、Ctrl：10）。\n'),
}
TERMINAL = {
    '': ('- **ID cards**:',
         '- **Minimum in percent**: an instant or scrap sell order can name its lowest price in percent of the bid without pressure (the '
         'c / % button); the percent becomes coins at each run. The tooltip of an order shows its limit now and what the surplus of the '
         'sell buffer would sell for.\n'
         '- **Settlement**: the minimum (percent or coins) applies to Sell now as well; "Coins to account" puts the coins of the container '
         'onto your coin account.\n'),
    '_zh_cn/': ('- **ID 卡**',
                '- **百分比最低价**：立即出售或回收模式的出售订单可用无抛压买价的百分比设定最低价（c / % 按钮），每次运行时换算为货币。'
                '订单的提示会显示当前的限价，以及出售缓冲区的余量现在能卖多少。\n'
                '- **大宗结算**：最低价（百分比或货币）同样适用于“立即出售”；“货币存入账户”把容器中的货币存入你的货币账户。\n'),
    '_ja_jp/': ('- **ID カード**',
                '- **パーセントの最低価格**: 即時売却とスクラップの売り注文は、最低価格を売り圧のない買値に対するパーセントで指定できる'
                '（c / % ボタン）。パーセントは実行のたびに硬貨に換算される。注文のツールチップに、いまの指値と売りバッファの余剰がいくらで'
                '売れるかが出る。\n'
                '- **一括決済**: 最低価格（パーセントか硬貨）は「今すぐ売る」にも効く。「硬貨を口座へ」でコンテナの硬貨をあなたの硬貨口座へ入れる。\n'),
}
CUTTER = {
    '': ('Each recipe needs a minimum blade hardness.',
         'Logs give the planks of their own wood: every log, wood, stem and stripped variant, bamboo blocks, and the woods of other mods '
         'that have a crafting recipe to planks. A log gives 6 planks (4 by hand), a bamboo block 3 (2 by hand).\n'),
    '_zh_cn/': ('每个配方要求最低刀片硬度。',
                '原木切成同种木材的木板：所有原木、木头、菌柄及其去皮变种、竹块，以及其他模组中有木板合成配方的木材。一个原木得 6 块木板'
                '（手工 4 块），一个竹块得 3 块（手工 2 块）。\n'),
    '_ja_jp/': ('各レシピは最低限の刃の硬さを要求する。',
                '原木は同じ木の板材になる。すべての原木・木・幹とその樹皮を剥いだもの、竹ブロック、板材のクラフトレシピを持つ他 MOD の木材に'
                '対応する。原木 1 個で板材 6 枚（手作業では 4 枚）、竹ブロック 1 個で 3 枚（手作業では 2 枚）。\n'),
}

for pages, page in ((FUTURES, 'machines/producer_block_future_elc.md'), (TERMINAL, 'machines/producer_block_trade_terminal.md'),
                    (CUTTER, 'machines/producer_block_block_cutter.md')):
    for prefix, (anchor, lines) in pages.items():
        path = os.path.join(GUIDE + prefix + page)
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        s = raw.decode('utf-8').replace('\r\n', '\n')
        if lines in s:
            continue
        if CHECK:
            missing.append(path)
            continue
        rows = s.split('\n')
        at = [i for i, r in enumerate(rows) if r.startswith(anchor)]
        assert len(at) == 1, (path, anchor, at)
        rows.insert(at[0] + 1, lines.rstrip('\n'))
        s = '\n'.join(rows)
        open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))
        print('guide', path)

if CHECK:
    print('missing or different:', missing)
    sys.exit(1 if missing else 0)
print('data_1129 written')
