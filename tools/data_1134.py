#!/usr/bin/env python3
"""0.1.10.34: the texts of the release. The guide (en / zh / ja, Futures Machine page): bid prices (Treasury line); days by
rarity and the rush bonus, claims, the paid refresh, more bounties for more players and the refill during the day (after the
bounty lines); the new settings (operators). The datapack documents (zh): the bounty tiers' days and counts, the value
between ask and bid, the bounty floor of 0.1.10.33, the table fields rarity / days, the new economy keys (si_exchange.md);
bid_price (si_futures.md). The API guides (en / zh): the value of a need, the days and rarity of an API bounty, claims.
Replacements first, then each block goes after its anchor line (the line that starts with the anchor text). Idempotent;
keeps the line endings of edited files. With --check it only reports what is missing. Usage: data_1134.py <tree> [--check]"""
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

# (old text, new text) per file
REPLACE = {
    D_EX: [
        ('| 每天保持 `bountyNormalMin`～`bountyNormalMax`（3～5）件 | 3 天 |',
         '| 每天保持 `bountyNormalMin`～`bountyNormalMax`（3～5）件（0.1.10.34 起随活跃玩家增加，并在日间补充） | 3～7 天（0.1.10.34 起按稀有度抽取；'
         '关闭 `bountyRandomDays` 时为 3 天） |'),
        ('| 每个交易周（7 天）`bountyAdvancedPerWeekMin`～`Max`（2～3）件，分散在一周内 | 7 天 |',
         '| 每个交易周（7 天）`bountyAdvancedPerWeekMin`～`Max`（2～3）件，分散在一周内（0.1.10.34 起随活跃玩家增加） | 2～5 或 1～4 天（按稀有度；'
         '关闭时 7 天） |'),
        ('| 每天 `bountySpecialChancePerDay`（5%）的概率，同时最多 `bountySpecialMaxOpen`（1）件 | 7 天 |',
         '| 每天 `bountySpecialChancePerDay`（5%）的概率，同时最多 `bountySpecialMaxOpen`（1）件（0.1.10.34 起随活跃玩家增加） | 1～3 天（传说；'
         '关闭时 7 天） |'),
        ('- **价值**：每个要求物品按发布时市场的卖价（价格 × (1 + 价差/2)）计价；',
         '- **价值**：每个要求物品按发布时市场卖价与买价的中间值计价（0.1.10.34 起；之前按卖价 价格 × (1 + 价差/2)）；没有 `bid_price` 的商品即为价格本身；'),
        ('- **市场不出售**：普通或高级悬赏仍需要某商品时，市场不出售该商品（奖励高于卖价，否则“从市场买来交付”就会凭空产生货币）。',
         '- **市场的最低售价**（0.1.10.33 起）：普通或高级悬赏仍需要某商品时，市场以不低于悬赏每个物品报酬的价格出售该商品（之前完全不出售），'
         '“从市场买来交付”不会凭空产生货币。'),
    ],
}

# (anchor, block) per file
ADD = {
    G_EN: [
        ('- **Treasury**',
         '- Since 0.1.10.34 the market buys a few goods back for much less than it sells them (their bid price): carbon plate, refined '
         'iron ingots, circuit boards and advanced alloy ingots, which are cheap to make from other goods of the market, so buying the '
         'materials and selling the product back earns little.\n'),
        ('- Since 0.1.10.33 the daily requests',
         '- **Days and rarity** (since 0.1.10.34): each bounty has a rarity that sets how long it stays open: common 3-7 days (the daily '
         'requests), uncommon 2-5, rare 1-4, legendary 1-3 (special bounties). A large one gets at least 2 days from 8,000 coins of value, '
         '3 from 16,000 and so on. A bounty open for fewer than 4 days pays a rush bonus of 5 % for each missing day (1 day: +15 %). The '
         'row shows the rarity and the bonus. Requests are valued halfway between the market\'s selling and buying price (before: the '
         'selling price).\n'
         '- **Claim** (since 0.1.10.34): Claim, under Deliver, reserves a normal or advanced bounty: only you and your team can deliver to '
         'it. You hold one claim at a time; a bounty somebody delivered to already cannot be claimed; a claim with one day left adds a day '
         '(once per bounty). Abandon gives it back, and you cannot claim again that market day. The claim of a player away for two whole '
         'market days is given back at the next change of day. Special bounties cannot be claimed.\n'
         '- **Refresh** (since 0.1.10.34, top right of the board): replaces the normal requests that nobody claimed or delivered to with as '
         'many new ones, never the same items. It costs 5 % of their rewards, at least 500 coins, and twice as much each further time: '
         'three times a market day per player, 5 minutes apart. When nothing new could be posted it is refused before you pay.\n'
         '- **More players, more bounties** (since 0.1.10.34): the board grows with the active players (online, or used the exchange in '
         'the last three market days): normal requests open 3-5 with one player, 5-7 with two, 8-10 with four, 14-16 with eight (up to '
         '16-18), more advanced bounties a week, more special ones at a time, and higher weekly limits of made items and UU-matter. During '
         'the day, when requests were completed, the board posts a new one after 6 minutes with one player online, 4 with two, 2.4 with '
         'four (at least 1 minute), up to 6 normal postings a market day and 4 more for each other active player. The budget tooltip '
         'shows the players, the postings and the next refill.\n'),
        ('- Operators can list, post and cancel bounties',
         '- Since 0.1.10.34 the server config (or economy.json) sets bountyRandomDays (off: the fixed days of the tiers, no rush bonus), '
         'bountyMinDays and bountyMaxDays (the range of the drawn days, 1-7) and bountyAcceptMax (claims a player may hold, 1).\n'),
    ],
    G_ZH: [
        ('- **国库**',
         '- 自 0.1.10.34 起，市场以远低于卖价的价格回收少数货品（收购价）：碳板、精炼铁锭、电路板与高级合金锭。它们能用市场的其他货品廉价制成，'
         '买入原料、加工后卖回市场几乎无利可图。\n'),
        ('- 自 0.1.10.33 起，每日请求',
         '- **期限与稀有度**（自 0.1.10.34 起）：每个悬赏都有稀有度，决定开放多久：常见 3～7 天（每日请求）、少见 2～5 天、稀有 1～4 天、'
         '传说 1～3 天（特别悬赏）。大的悬赏价值 8,000 货币起至少 2 天，16,000 起至少 3 天，依此类推。开放不足 4 天的悬赏有加急奖励：每少一天 '
         '+5%（1 天为 +15%）。该行显示稀有度与加急奖励。要求的价值按市场卖价与买价的中间值计算（之前按卖价）。\n'
         '- **接取**（自 0.1.10.34 起）：“交付”下方的“接取”可以预留一件普通或高级悬赏，之后只有你和你的队伍可以交付。每人同时只能接取一件；'
         '已有人交付过的悬赏不能接取；剩余 1 天时接取会延长 1 天（每件悬赏一次）。“放弃”会交还悬赏，当天不能再接取。连续两个完整交易日不在线的'
         '玩家的接取会在下次换日时交还。特别悬赏不能接取。\n'
         '- **刷新**（自 0.1.10.34 起，悬赏板右上角）：把无人接取、无人交付的普通请求换成同样数量的新请求，不会出现相同的物品。费用为其奖励的 5%，'
         '至少 500 货币，之后每次翻倍：每名玩家每个交易日 3 次，间隔 5 分钟。无法发布新请求时在付款前拒绝。\n'
         '- **玩家越多，悬赏越多**（自 0.1.10.34 起）：悬赏板随活跃玩家（在线，或在最近三个交易日用过交易所）增加：普通请求 1 人 3～5 件、'
         '2 人 5～7 件、4 人 8～10 件、8 人 14～16 件（最多 16～18 件），高级悬赏每周更多，特别悬赏同时更多，制作物品与 UU 物质的每周上限更高。'
         '日间有请求完成后，悬赏板在 1 人在线时 6 分钟、2 人 4 分钟、4 人 2.4 分钟（至少 1 分钟）后发布新的请求，每个交易日普通请求最多发布 6 件，'
         '每多一名活跃玩家多 4 件。预算的提示显示玩家数、发布数与下次补充。\n'),
        ('- 管理员可以用 /mio_icif bounty',
         '- 自 0.1.10.34 起，服务器配置（或 economy.json）可以设置 bountyRandomDays（关闭：各等级的固定天数，无加急奖励）、bountyMinDays 与 '
         'bountyMaxDays（抽取天数的范围，1～7）以及 bountyAcceptMax（每名玩家可同时接取的数量，1）。\n'),
    ],
    G_JA: [
        ('- **国庫**',
         '- 0.1.10.34 から、市場は一部の品目を売値よりかなり安く買い取る（買取価格）：カーボン板、精錬鉄インゴット、回路基板、高度合金インゴット。'
         'これらは市場の他の品目から安く作れるため、材料を買って加工し市場へ売り戻しても、ほとんど得にならない。\n'),
        ('- 0.1.10.33 から、毎日の依頼',
         '- **期限とレア度**（0.1.10.34 から）：懸賞にはレア度があり、開いている日数が決まる：コモン 3〜7 日（毎日の依頼）、アンコモン 2〜5 日、'
         'レア 1〜4 日、レジェンダリー 1〜3 日（特別懸賞）。大きな懸賞は価値 8,000 硬貨から最低 2 日、16,000 から最低 3 日、以下同様。4 日未満の'
         '懸賞には特急ボーナスがあり、足りない 1 日ごとに +5%（1 日なら +15%）。行にレア度とボーナスが出る。依頼の価値は市場の売値と買値の中間で'
         '計算する（以前は売値）。\n'
         '- **受注**（0.1.10.34 から）：「納品」の下の「受注」で普通・上級の懸賞を確保でき、以後はあなたとチームだけが納品できる。受注は 1 人 1 件'
         'まで。すでに誰かが納品した懸賞は受注できない。残り 1 日で受注すると 1 日延びる（懸賞ごとに 1 回）。「放棄」で返すと、その市場日はもう受注'
         'できない。市場日をまるごと 2 日不在だったプレイヤーの受注は、次の日替わりで返される。特別懸賞は受注できない。\n'
         '- **更新**（0.1.10.34 から、懸賞ボードの右上）：誰も受注・納品していない普通の依頼を、同じ数の新しい依頼に入れ替える。同じアイテムは出ない。'
         '費用はその報酬の 5%（最低 500 硬貨）で、回数ごとに倍になる。1 人 1 市場日に 3 回、5 分おき。新しい依頼を出せないときは支払い前に断る。\n'
         '- **人数が多いほど懸賞も多い**（0.1.10.34 から）：ボードはアクティブなプレイヤー（オンライン、または直近 3 市場日に取引所を使った人）に'
         '応じて増える。普通の依頼は 1 人 3〜5 件、2 人 5〜7 件、4 人 8〜10 件、8 人 14〜16 件（最大 16〜18 件）。上級の週あたりの件数、特別懸賞の'
         '同時件数、作られるアイテムと UU 物質の週の上限も増える。日中に依頼が片付くと、オンライン 1 人なら 6 分、2 人なら 4 分、4 人なら 2.4 分'
         '（最短 1 分）後に新しい依頼を出す。普通の掲示は 1 市場日 6 件まで、アクティブなプレイヤー 1 人ごとに 4 件増える。予算のツールチップに'
         '人数、掲示数、次の補充までの時間が出る。\n'),
        ('- 管理者は /mio_icif bounty',
         '- 0.1.10.34 から、サーバー設定（または economy.json）で bountyRandomDays（オフ：段階ごとの固定日数、特急ボーナスなし）、bountyMinDays と '
         'bountyMaxDays（抽選する日数の範囲、1〜7）、bountyAcceptMax（1 人が同時に受注できる数、1）を設定できる。\n'),
    ],
    D_EX: [
        ('- **到期**',
         '- **稀有度与期限**（0.1.10.34 起，`bountyRandomDays` 默认开启）：每件悬赏按稀有度抽取开放天数：常见 3～7、少见 2～5、稀有 1～4、传说 1～3 天，'
         '并限制在 `bountyMinDays`～`bountyMaxDays`（1～7）之内。价值 8,000 起至少 2 天，每翻一倍多 1 天（仍不超过该范围的上限）。不足 4 天的悬赏'
         '奖励倍率乘以 1 + 每少一天 5%（加急奖励）。自动生成的普通请求为常见；表的稀有度见下表 `rarity`。\n'
         '- **接取**（0.1.10.34 起）：玩家可以接取普通与高级悬赏（每人同时最多 `bountyAcceptMax` 件，默认 1），之后只有接取者与其队伍可以交付。'
         '已有交付的悬赏不能接取；剩余 1 天时接取延长 1 天（一次）；放弃后当天不能再接取；连续两个完整交易日不在线的玩家的接取在换日时解除。'
         '接取、放弃与刷新各消耗一次交易的 EU。\n'
         '- **刷新**（0.1.10.34 起）：玩家付费把无人接取、无人交付的普通请求换成同样数量的新请求（不再抽到被换掉的物品与表）。费用为其奖励价值的 5%、'
         '至少 500 货币，每名玩家每个交易日第 2、3 次翻倍，最多 3 次，间隔 6,000 tick。费用作为货币销毁计入账本。被换掉的悬赏的预留奖励退回。\n'
         '- **按活跃玩家**（0.1.10.34 起；活跃 = 在线，或在最近三个交易日登录或用过交易所，人数 n）：普通请求的范围为 '
         '`bountyNormalMin` + ⌈1.5 (n − 1)⌉（最多 16）起、宽度不变，另加已接取的普通请求；高级每周件数 × (1 + 0.5 (n − 1))（最多 21）；'
         '特别同时件数 `bountySpecialMaxOpen` + ⌊(n − 1) / 4⌋（最多 4）；每周发行上限（物品与 UU 物质）× √n。\n'
         '- **日间补充**（0.1.10.34 起）：普通请求少于当天的目标时，悬赏板在上次发布后经过 7,200 tick ÷ (1 + 0.5 (在线人数 − 1))（至少 1,200 tick）'
         '发布 1 件，每个交易日普通发布最多 6 + 4 (n − 1) 件（含换日时的发布）。无人在线时不补充。\n'),
        ('| `duration_days` |',
         '| `rarity` | 按等级与权重 | （0.1.10.34）`common` / `uncommon` / `rare` / `legendary`，决定抽取天数的范围（3～7 / 2～5 / 1～4 / 1～3）。'
         '不写时：普通权重 ≥ 4 为 common、< 4 为 uncommon；高级权重 ≥ 3 为 uncommon、< 3 为 rare；特别为 legendary。 |\n'
         '| `days` | 按稀有度 | （0.1.10.34）抽取天数的范围 `[最少, 最多]`（1～60），代替稀有度的范围。写了 `duration_days` 时天数固定，不抽取，也没有加急奖励。 |\n'),
        ('- 0.1.10.28 新增的键',
         '- 0.1.10.34 新增的键：`bountyRandomDays`（true，按稀有度抽取天数）、`bountyMinDays`（1）、`bountyMaxDays`（7）、`bountyAcceptMax`（1）。\n'),
    ],
    D_FU: [
        ('| `base_price` |',
         '| `bid_price` | 否（0.1.10.34） | 市场收购价的基准（硬币，0～`base_price`）：卖给市场的价格再乘 `bid_price ÷ base_price`，随价格一起变动；'
         '不写或等于 `base_price` 时与之前相同。用于能由其他商品廉价制成的物品，使“买入原料→加工→卖回市场”无利可图。悬赏按卖价与买价的中间值'
         '计价。默认商品中碳板（300 / 60）、精炼铁锭（400 / 220）、电路板（900 / 650）、高级合金锭（800 / 600）设置了此项。 |\n'),
        ('- 卖给市场的价格：',
         '- 0.1.10.34 起，设置了 `bid_price` 的商品在上式中再乘 `bid_price ÷ base_price`（`/mio_icif market arbitrage` 也按此计算）。\n'),
    ],
    A_EN: [
        ('  SPECIAL one is not posted.',
         '* Since 0.1.10.34 a need of value 0 is valued halfway between the market\'s ask and bid (before: the ask). A bounty of the API '
         'keeps the days of its `BountySpec` (or the tier\'s) without a rush bonus; its rarity follows the tier. Players may claim NORMAL '
         'and ADVANCED bounties; then only the claimant and their team deliver. The API types are unchanged.\n'),
    ],
    A_ZH: [
        ('  无法支付的奖励项被去掉',
         '* 自 0.1.10.34 起，价值为 0 的要求按市场卖价与买价的中间值计价（之前按卖价）。API 发布的悬赏使用 `BountySpec` 的天数（或该等级的天数），'
         '没有加急奖励，稀有度按等级决定。玩家可以接取普通与高级悬赏，之后只有接取者与其队伍可以交付。API 类型不变。\n'),
    ],
}

for rel in (G_EN, G_ZH, G_JA, D_EX, D_FU, A_EN, A_ZH):
    path = os.path.join(TREE, rel)
    raw = open(path, 'rb').read()
    eol = '\r\n' if b'\r\n' in raw else '\n'
    s = raw.decode('utf-8').replace('\r\n', '\n')
    before = s
    for old, new in REPLACE.get(rel, []):
        if new in s:
            continue
        if CHECK:
            missing.append(f'{rel}: {new[:40]}')
            continue
        assert s.count(old) == 1, (rel, old)
        s = s.replace(old, new)
    for anchor, block in ADD.get(rel, []):
        if block in s:
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
print('data_1134 written')
