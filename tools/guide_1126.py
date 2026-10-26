"""0.1.10.26 (X3): the Futures Machine guide page (en / zh / ja): the system bounties in three tiers (Bounties tab), the
treasury line, the operator command /mio_icif bounty and the si_exchange datapacks. Idempotent; --check reports pages
that miss the new text.
Usage: guide_1126.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
BASE = os.path.join(TREE, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
PAGE = 'machines/producer_block_future_elc.md'

EDITS = {
    '': [
        ('When a commodity runs short, the market posts commissions for it and stops selling it until they end.',
         'When a commodity runs short, the market posts a bounty for it (Bounties tab) and does not sell it while a normal or '
         'advanced bounty needs it.'),
        ('What stays in the box comes back when you close the panel.\n',
         'What stays in the box comes back when you close the panel.\n'
         '- **Bounties**: the market posts system bounties in three tiers. Normal: a commodity it lacks, every day, for 3 days, '
         'reward 1.15 times the value. Advanced: a set of goods, two or three a week, for 7 days, reward 1.3 times; in '
         'multiplayer one player may deliver at most half of it until its last day. Special: a rare item, now and then, with '
         'a rare reward. Rewards are coins, goods of the market\'s stock, UU-matter or items made within a weekly limit; rare '
         'items delivered to special bounties go to a vault and pay later ones. Press Deliver to hand over what you carry: '
         'you are paid at once in proportion, or by contribution when the bounty says so. In single player you can complete '
         'every bounty alone.\n'),
        ('see the coins created and destroyed (ledger) with /mio_icif market.\n',
         'see the coins created and destroyed (ledger) with /mio_icif market.\n'
         '- Operators can list, post and cancel bounties and see the datapack problems and the economy settings with '
         '/mio_icif bounty. Pack makers set bounty tables, reward pools, listings that are open from the start and economy '
         'defaults in the datapack folder data/(namespace)/si_exchange; a value changed in the server config wins.\n'),
    ],
    '_zh_cn/': [
        ('货品短缺时，市场发布委托，并在委托结束前停止出售该货品。',
         '货品短缺时，市场发布悬赏（“悬赏”页），在普通或高级悬赏需要该货品期间不出售它。'),
        ('关闭界面时箱中剩余的物品会退回。\n',
         '关闭界面时箱中剩余的物品会退回。\n'
         '- **悬赏**：市场分三个等级发布系统悬赏。普通：市场缺少的商品，每天发布，期限 3 天，奖励为价值的 1.15 倍。高级：一组物资，'
         '每周两三件，期限 7 天，奖励为 1.3 倍；多人游戏中，最后一天之前一名玩家最多交付一半。特别悬赏：稀有物品，偶尔出现，奖励稀有。'
         '奖励为货币、市场库存的物资、UU 物质，或在每周上限内制作的物品；交付给特别悬赏的稀有物品进入宝库，之后作为奖励发出。按“交付”'
         '交出你携带的物品：奖励按比例立即支付（悬赏注明时按贡献分配）。单人游戏中所有悬赏都可以由你一人完成。\n'),
        ('查看货币的发行与销毁（ledger）。\n',
         '查看货币的发行与销毁（ledger）。\n'
         '- 管理员可以用 /mio_icif bounty 列出、发布与取消悬赏，查看数据包问题与经济设置。整合包作者在数据包文件夹 '
         'data/(命名空间)/si_exchange 中设置悬赏表、奖励池、初始上市品与经济默认值；服务器配置中修改过的值优先。\n'),
    ],
    '_ja_jp/': [
        ('品目が不足すると市場は委託を出し、その委託が終わるまでその品目を売らない。',
         '品目が不足すると市場は懸賞を出し（「懸賞」タブ）、普通・上級の懸賞がその品目を求めている間は売らない。'),
        ('画面を閉じるとボックスの中身は戻る。\n',
         '画面を閉じるとボックスの中身は戻る。\n'
         '- **懸賞**: 市場は 3 段階のシステム懸賞を出す。普通：市場に足りない品目、毎日、期限 3 日、報酬は価値の 1.15 倍。上級：品物の'
         'セット、週に 2〜3 件、期限 7 日、報酬は 1.3 倍。マルチプレイでは最終日まで 1 人が納品できるのは半分まで。特別懸賞：希少品、'
         'ときどき出て、報酬も希少。報酬は硬貨、市場の在庫の品物、UU 物質、週の上限の内で作られるアイテムのいずれか。特別懸賞に納品された'
         '希少品は宝物庫に入り、後の懸賞の報酬になる。「納品」で持っている物を渡すと、報酬は比例してすぐ払われる（懸賞の指定があれば貢献で'
         '分配）。シングルプレイでは、どの懸賞も 1 人で完了できる。\n'),
        ('コインの発行と消滅（ledger）も見られる。\n',
         'コインの発行と消滅（ledger）も見られる。\n'
         '- 管理者は /mio_icif bounty で懸賞の一覧・掲示・取り消し、データパックの問題、経済の設定を確認できる。パック作者はデータパックの'
         'フォルダ data/(名前空間)/si_exchange で懸賞の表、報酬プール、最初からの上場品、経済の既定値を設定する。サーバー設定で変えた値が'
         '優先される。\n'),
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
            assert text.count(old) == 1, (sub, old[:50], text.count(old))
            text = text.replace(old, new)
        if not CHECK:
            open(path, 'wb').write(text.encode('utf-8').replace(b'\n', eol))
            print(f'{sub or "en"}: done')
    if CHECK:
        print('guide_1126 check:', 'OK' if not bad else f'{bad} missing')
        sys.exit(1 if bad else 0)


if __name__ == '__main__':
    main()
