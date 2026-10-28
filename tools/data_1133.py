#!/usr/bin/env python3
"""0.1.10.33: the guide lines of the release, en / zh / ja, on the Futures Machine page: the bounty floor replaces "does not sell
it" (Treasury line); instant sales under the band (Order book); commission lots; the stack rules and the missing needs of the
bounties; the panel at small windows; the market day and its repair (operators). Replacements first, then each block goes after
its anchor line (the line that starts with the anchor text). Idempotent; keeps the line endings of edited files. With --check it
only reports what is missing. Usage: data_1133.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
GUIDE = os.path.join(TREE, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual/')
PAGE = 'machines/producer_block_future_elc.md'
missing = []

# (old text, new text) per language
REPLACE = {
    '': [('and does not sell it while a normal or advanced bounty needs it.',
          'and, while a normal or advanced bounty needs it, sells it at no less than the bounty\'s reward per item (since 0.1.10.33; '
          'before, it did not sell it at all), so buying it to deliver earns nothing.')],
    '_zh_cn/': [('在普通或高级悬赏需要该货品期间不出售它。',
                 '在普通或高级悬赏需要该货品期间，以不低于悬赏每个物品报酬的价格出售（自 0.1.10.33 起；之前完全不出售），买来交付不会获利。')],
    '_ja_jp/': [('普通・上級の懸賞がその品目を求めている間は売らない。',
                 '普通・上級の懸賞がその品目を求めている間は、懸賞の 1 個あたりの報酬以上の価格で売る（0.1.10.33 から。以前は売らなかった）。'
                 '買って納品しても得にはならない。')],
}

# (anchor, block) per language
ADD = {
    '': [
        ('- **Order book**',
         '- Since 0.1.10.33 an instant sale to the market may name a limit under the price band (it is paid what the market\'s bid '
         'pays); orders that rest in the book stay within the band.\n'),
        ('- **Commissions**',
         '- Since 0.1.10.33 a commission with an item or UU-matter reward takes deliveries in whole lots, so every lot pays whole '
         'reward items: 64 items for 16 diamonds take lots of 4 items (1 diamond each), or all that is left. The tooltip of the row '
         'shows the lot.\n'),
        ('- **Bounties**',
         '- Since 0.1.10.33 the daily requests skip items that do not stack and ask at most two stacks of items that stack below '
         '64; datapack bounty tables ask at most eight inventory slots of an item. A delivery that hands over nothing, or not all, '
         'says what is still needed.\n'),
        ('- **Number fields**',
         '- **Small windows** (since 0.1.10.33): the panel fits every window from 426 x 240 (after the GUI scale). The account line '
         '(market day, coins, account, UU-matter) is the status bar at the bottom; long lists scroll with the mouse wheel and say how '
         'many entries are not shown; a text that does not fit uses short numbers (1.73M) and shows all of it as tooltip, and a '
         'number is never cut. On narrow windows the tabs use short names, the last trades are not shown and the bounty budgets are '
         'the tooltip of the board title.\n'),
        ('- Operators can check the exchange',
         '- Since 0.1.10.33 the market day counts the days the world lived: setting the world\'s time back moves it one day on '
         '(before, the market day went back with it and its bounties, orders and limits broke). /mio_icif market day shows the market '
         'day, the world day and the latest record; /mio_icif market day repair (it also runs at every load) moves a market that an '
         'older version set back up to its latest record, together with the records made since, which keep their remaining days.\n'),
    ],
    '_zh_cn/': [
        ('- **盘口**',
         '- 自 0.1.10.33 起，立即卖给市场时限价可以低于价格区间（按市场买价成交）；挂在盘口的订单仍须在价格区间内。\n'),
        ('- **委托**',
         '- 自 0.1.10.33 起，以物品或 UU 物质为报酬的委托按整批交付，每批报酬都是整数：16 颗钻石征集 64 个物品时每批 4 个（每批 1 颗钻石），'
         '或交付全部剩余。该行的提示会显示批量。\n'),
        ('- **悬赏**',
         '- 自 0.1.10.33 起，每日请求不再要求不可堆叠的物品，堆叠上限低于 64 的物品最多要求两组；数据包悬赏表中每种物品最多要求八个物品栏格。'
         '交付不了或没有全部交付时会说明还缺什么。\n'),
        ('- **数字',
         '- **小窗口**（自 0.1.10.33 起）：界面适配 426 x 240（按 GUI 缩放后）以上的所有窗口。账户行（市场日、货币、账户、UU 物质）移到底部状态栏；'
         '长列表可用鼠标滚轮滚动并显示还有多少项未显示；放不下的文字改用简写数字（1.73M），提示中显示全文，数字绝不会被截断。窄窗口中标签页使用简称，'
         '不显示最近成交，悬赏预算显示在悬赏板标题的提示中。\n'),
        ('- 管理员可以用 /mio_icif market audit',
         '- 自 0.1.10.33 起，市场日按世界经过的天数计算：把世界时间调回时市场日只前进一天（之前市场日随之倒退，悬赏、挂单与额度因此出错）。'
         '/mio_icif market day 显示市场日、世界日与最晚的记录；/mio_icif market day repair（每次载入时也会运行）把被旧版本调回的市场'
         '移到最晚的记录，并一起移动此后产生的记录，保留其剩余天数。\n'),
    ],
    '_ja_jp/': [
        ('- **板**',
         '- 0.1.10.33 から、市場への即時売却は値幅より低い指値でもよい（市場の買値で約定する）。板に残る注文は値幅の範囲内のまま。\n'),
        ('- **委託**',
         '- 0.1.10.33 から、アイテム・UU 物質が報酬の委託はまとまった単位で納品する。どの単位も報酬が整数になる：ダイヤ 16 個で 64 個の依頼は '
         '4 個単位（1 単位でダイヤ 1 個）、または残り全部。行のツールチップに単位が出る。\n'),
        ('- **懸賞**',
         '- 0.1.10.33 から、毎日の依頼はスタックできないアイテムを求めず、スタック上限が 64 未満のアイテムは最大 2 スタックまでにする。'
         'データパックの懸賞表は 1 品目につき最大 8 スロット分まで。何も（または全部は）納品できなかったときは、残りに何が必要かを表示する。\n'),
        ('- **数値欄**',
         '- **小さなウィンドウ**（0.1.10.33 から）：画面は 426 x 240（GUI スケール適用後）以上のすべてのウィンドウに収まる。口座の行（市場日、'
         '硬貨、口座、UU 物質）は下端のステータスバーになった。長いリストはマウスホイールでスクロールでき、表示しきれない件数を示す。収まらない'
         '文字は短い数字（1.73M）を使い、全文をツールチップに出す。数字が途中で切れることはない。幅の狭いウィンドウではタブが短い名前になり、'
         '直近の約定は表示せず、懸賞の予算は懸賞ボードの見出しのツールチップに出る。\n'),
        ('- 管理者は /mio_icif market audit',
         '- 0.1.10.33 から、市場日はワールドが経過した日数を数える。ワールドの時刻を巻き戻しても 1 日進むだけ（以前は市場日も戻り、懸賞・注文・'
         '上限が壊れた）。/mio_icif market day は市場日、ワールドの日、最新の記録を表示する。/mio_icif market day repair（読み込みのたびにも'
         '実行される）は、古い版で巻き戻された市場を最新の記録の日まで進め、その後に作られた記録も残り日数を保ったまま一緒に移す。\n'),
    ],
}

for prefix in ('', '_zh_cn/', '_ja_jp/'):
    path = os.path.join(GUIDE + prefix + PAGE)
    raw = open(path, 'rb').read()
    eol = '\r\n' if b'\r\n' in raw else '\n'
    s = raw.decode('utf-8').replace('\r\n', '\n')
    before = s
    for old, new in REPLACE[prefix]:
        if new in s:
            continue
        if CHECK:
            missing.append(f'{path}: {new[:40]}')
            continue
        assert s.count(old) == 1, (path, old)
        s = s.replace(old, new)
    for anchor, block in ADD[prefix]:
        if block in s:
            continue
        if CHECK:
            missing.append(f'{path}: {block[:40]}')
            continue
        rows = s.split('\n')
        at = [i for i, r in enumerate(rows) if r.startswith(anchor)]
        assert len(at) == 1, (path, anchor, at)
        rows.insert(at[0] + 1, block.rstrip('\n'))
        s = '\n'.join(rows)
    if s != before and not CHECK:
        open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))
        print('guide', path)

if CHECK:
    print('missing or different:', missing)
    sys.exit(1 if missing else 0)
print('data_1133 written')
