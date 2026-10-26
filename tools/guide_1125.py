"""0.1.10.25 (X1): the Futures Machine guide page (en / zh / ja): the sell page, the sell curve instead of the daily
limit, the operator commands. Idempotent; --check reports pages that miss the new text.
Usage: guide_1125.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
BASE = os.path.join(TREE, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
PAGE = 'machines/producer_block_future_elc.md'

EDITS = {
    '': [
        ('- **Mailbox**: what the exchange owes you (sales, refunds, items that did not fit). Collect it in the Orders and Mail tab.\n',
         '- **Mailbox**: what the exchange owes you (sales, refunds, items that did not fit). Collect it in the Orders and Mail tab.\n'
         '- **Sell page**: put items in the sell box (shift-click, or Fill takes every sellable stack outside the hotbar). The page shows '
         'what each commodity pays now and at the daily auction. Sell now, or give the items to the auction: it sells them together '
         'with everyone\'s at the change of the market day, so the order of the sellers does not matter. What stays in the box comes '
         'back when you close the panel.\n'),
        ('- A daily limit per player applies to trades with the market.\n',
         '- Selling to the market has no item limit. Each item you sold of a commodity today, and each item everyone sold that the '
         'market has not passed on yet, lowers the bid; half of that sell pressure leaves each day. Small sales get almost the full '
         'price, bulk sales a lower average price.\n'
         '- In multiplayer the market pays each player at most 50,000 coins a day for sales (unused coins are kept up to 3 days). '
         'Buying from the market has no daily limit; its stock limits it.\n'),
        ('- Operators can check the exchange with /mio_icif market audit.\n',
         '- Operators can check the exchange with /mio_icif market audit, and price a sale (simulate), look for recipe loops '
         '(arbitrage), compare prices with UU-matter values (calibrate) and see the coins created and destroyed (ledger) with '
         '/mio_icif market.\n'),
    ],
    '_zh_cn/': [
        ('- **邮箱**：交易所欠你的东西（卖出所得、退款、背包放不下的物品）。在“挂单与邮箱”页领取。\n',
         '- **邮箱**：交易所欠你的东西（卖出所得、退款、背包放不下的物品）。在“挂单与邮箱”页领取。\n'
         '- **出售页**：把物品放入出售箱（Shift 点击，或用“放入”放入快捷栏以外所有可出售的物品）。页面显示每种商品立即出售与参加每日集中清算'
         '的所得。可以立即出售，或交给集中清算：在交易日更替时与所有人的一起出售，先后顺序无关。关闭界面时箱中剩余的物品会退回。\n'),
        ('- 与市场的交易有每名玩家的每日上限。\n',
         '- 卖给市场没有数量上限。你今天每卖出一个该商品，以及所有人卖出且市场尚未转出的每一个，都会降低买价；这份卖压每天转出一半。'
         '少量出售几乎得到全价，大量出售的平均价格较低。\n'
         '- 多人游戏中，市场每天最多付给每名玩家 50,000 货币的卖出所得（未用完的额度最多保留 3 天）。从市场购买没有每日上限，由库存限制。\n'),
        ('- 管理员可以用 /mio_icif market audit 查看交易所。\n',
         '- 管理员可以用 /mio_icif market audit 查看交易所，并用 /mio_icif market 估算卖出（simulate）、查找配方套利（arbitrage）、'
         '比较价格与 UU 物质价值（calibrate）、查看货币的发行与销毁（ledger）。\n'),
    ],
    '_ja_jp/': [
        ('- **郵便箱**: 取引所からの受け取り分（売却代金・返金・入りきらなかったアイテム）。「注文と郵便箱」タブで受け取る。\n',
         '- **郵便箱**: 取引所からの受け取り分（売却代金・返金・入りきらなかったアイテム）。「注文と郵便箱」タブで受け取る。\n'
         '- **売却ページ**: 売却ボックスに物を入れる（Shift クリック、または「入れる」でホットバー以外の売れる物をすべて入れる）。'
         '品目ごとに、今すぐ売った場合と毎日の一括清算に出した場合の受取額が表示される。今すぐ売るか、一括清算に出す。一括清算は取引日の'
         '変わり目に全員分をまとめて売るので、順番は関係ない。画面を閉じるとボックスの中身は戻る。\n'),
        ('- 市場との取引には、プレイヤーごとの 1 日の上限がある。\n',
         '- 市場への売却に数量の上限はない。その品目を今日自分が売った分と、全員が売って市場がまだ流していない分だけ買値が下がる。'
         'この売り圧力は毎日半分ずつ流れる。少量ならほぼ満額、大量なら平均単価が下がる。\n'
         '- マルチプレイでは、市場が売却代金として払うのは 1 人 1 日 50,000 コインまで（使わなかった分は 3 日まで繰り越し）。'
         '市場からの購入に 1 日の上限はなく、在庫が上限になる。\n'),
        ('- 管理者は /mio_icif market audit で取引所を確認できる。\n',
         '- 管理者は /mio_icif market audit で取引所を確認できる。/mio_icif market では、売却の試算（simulate）、レシピの裁定ループの'
         '検出（arbitrage）、価格と UU 物質の価値の比較（calibrate）、コインの発行と消滅（ledger）も見られる。\n'),
    ],
}


def main():
    bad = 0
    for sub, edits in EDITS.items():
        path = os.path.join(BASE, sub + PAGE)
        raw = open(path, 'rb').read()
        eol = b'\r\n' if b'\r\n' in raw else b'\n'
        text = raw.decode('utf-8').replace('\r\n', '\n')
        for old, new in edits:
            if new in text:
                continue
            if CHECK:
                bad += 1
                print(f'{sub or "en"}: missing: {new[:60]}')
                continue
            assert text.count(old) == 1, (sub, old[:50])
            text = text.replace(old, new)
        if not CHECK:
            open(path, 'wb').write(text.encode('utf-8').replace(b'\n', eol))
            print(f'{sub or "en"}: done')
    if CHECK:
        print('guide_1125 check:', 'OK' if not bad else f'{bad} missing')
        sys.exit(1 if bad else 0)


if __name__ == '__main__':
    main()
