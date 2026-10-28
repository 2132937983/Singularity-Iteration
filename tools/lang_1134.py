#!/usr/bin/env python3
"""0.1.10.34 language values (en_us / zh_cn / ja_jp): the bounty rarity, rush bonus, claims and refresh, the players and the
refill of the board (panel texts and messages), the four new settings, and the tier texts reworded for the random days and the
ask floor. Idempotent; keeps the line endings of each file. With --check it only reports missing or different keys.
Usage: lang_1134.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
G = 'gui.mio_icif.exchange.bounty.'
M = 'message.mio_icif.exchange.'
CF = 'mio_icif.configuration.'

KEYS = {
    # ---- rarity and rush
    G + 'rarity.0': ('Common', '常见', 'コモン'),
    G + 'rarity.1': ('Uncommon', '少见', 'アンコモン'),
    G + 'rarity.2': ('Rare', '稀有', 'レア'),
    G + 'rarity.3': ('Legendary', '传说', 'レジェンダリー'),
    G + 'rarity_help': (
        'The rarity sets how long a bounty stays open: common 3-7 days, uncommon 2-5, rare 1-4, legendary 1-3; a large request '
        'gets at least 1 day more for each doubling of its value above 4,000 coins.',
        '稀有度决定悬赏的持续天数：常见 3-7 天，少见 2-5 天，稀有 1-4 天，传说 1-3 天；价值超过 4,000 货币的大额悬赏，价值每翻一倍至少多 1 天。',
        'レア度が懸賞の期間を決めます：コモン 3〜7 日、アンコモン 2〜5 日、レア 1〜4 日、レジェンダリー 1〜3 日。価値が 4,000 硬貨を'
        '超える大きな懸賞は、価値が 2 倍になるごとに最低 1 日長くなります。'),
    G + 'rush': ('Rush +%1$s%%', '加急 +%1$s%%', '急ぎ +%1$s%%'),
    G + 'rush_help': (
        'Posted for fewer than 4 days: the rewards are %1$s%% higher (5 for each day below 4).',
        '发布期限不足 4 天：奖励提高 %1$s%%（每少 1 天加 5）。',
        '期間が 4 日未満の懸賞：報酬が %1$s%% 高くなります（4 日に 1 日足りないごとに 5）。'),
    # ---- claims
    G + 'claim': ('Claim', '接取', '受注'),
    G + 'claim_help': (
        'Claim this bounty for you and your team: nobody else can deliver to it. You hold one claim at a time; a claim with one day '
        'left gets one more day. Special bounties and bounties someone already delivered to cannot be claimed.',
        '为你和你的队伍接取此悬赏：其他人无法再交付。每人同时只能接取一个；接取时只剩 1 天的悬赏延长 1 天。特别悬赏和已有人交付过的悬赏不能接取。',
        'この懸賞を自分とチームのために受注します：ほかの人は納品できなくなります。受注は同時に 1 件まで。残り 1 日で受注すると'
        '1 日延びます。特別懸賞と、すでに誰かが納品した懸賞は受注できません。'),
    G + 'abandon': ('Abandon', '放弃', '放棄'),
    G + 'abandon_help': (
        'Give up the claim: everybody may deliver to the bounty again, and you cannot claim another one until the next market day.',
        '放弃接取：所有人都可以再次交付该悬赏，而你在下一个市场日之前不能再接取悬赏。',
        '受注を放棄します：誰でもまた納品できるようになり、あなたは次の市場日まで受注できません。'),
    G + 'claimed_mine': ('Claimed by you', '已由你接取', 'あなたが受注中'),
    G + 'claimed_by': ('Claimed: %1$s', '接取者：%1$s', '受注：%1$s'),
    G + 'claimed_help': (
        '%1$s claimed this bounty: only they and their team deliver to it. The claim ends when it is given up, when the bounty '
        'ends, or after two whole market days away.',
        '%1$s 已接取此悬赏：只有其本人和队伍可以交付。接取会在放弃、悬赏结束或其连续两个完整市场日未上线后解除。',
        '%1$s がこの懸賞を受注しています：本人とそのチームだけが納品できます。受注は、放棄したとき、懸賞が終わったとき、'
        'または市場日をまる 2 日留守にしたときに解除されます。'),
    G + 'claimed_team_help': (
        'Your teammate %1$s claimed this bounty: you can deliver to it too.',
        '你的队友 %1$s 已接取此悬赏：你也可以交付。',
        'チームの仲間 %1$s が受注しています：あなたも納品できます。'),
    # ---- refresh
    G + 'refresh': ('Refresh %1$s', '刷新 %1$s', '更新 %1$s'),
    G + 'refresh_none': ('Refresh', '刷新', '更新'),
    G + 'refresh_used': ('No refresh left', '今日已用完', '本日は終了'),
    G + 'refresh_wait': ('Refresh in %1$s', '%1$s 后可刷新', '%1$s 後に更新可'),
    G + 'refresh_help': (
        'Replaces the %1$s normal requests that nobody claimed or delivered to with new ones (never the same items). The coins '
        'are destroyed. Refreshes left today: %2$s. Cost now: %3$s coins (500, or 5 per 100 coins of their rewards when that is '
        'more; doubled for each refresh you made today; five minutes apart).',
        '把无人接取、无人交付的 %1$s 条普通悬赏换成新的（不会是相同的物品）。支付的货币会被销毁。今日剩余次数：%2$s。当前费用：%3$s '
        '货币（500，或其奖励每 100 货币收 5，取较高者；今日每刷新一次翻倍；两次之间间隔 5 分钟）。',
        '誰も受注・納品していない普通懸賞 %1$s 件を新しいものに入れ替えます（同じ品目にはなりません）。払った硬貨は消滅します。'
        '本日の残り回数：%2$s。現在の費用：%3$s 硬貨（500、または報酬 100 硬貨あたり 5 の多い方。本日更新するたびに 2 倍。間隔は 5 分）。'),
    # ---- the players and the refill (budget lines)
    G + 'players': ('Players: %1$s active, %2$s online', '玩家：活跃 %1$s，在线 %2$s', 'プレイヤー：アクティブ %1$s・オンライン %2$s'),
    G + 'posted': ('Posted today %1$s/%2$s, open %3$s/%4$s', '今日发布 %1$s/%2$s，进行中 %3$s/%4$s', '本日の掲示 %1$s/%2$s・掲示中 %3$s/%4$s'),
    G + 'next_refill': ('Next request in %1$s', '%1$s 后补充下一条', '次の補充まで %1$s'),
    G + 'no_refill': ('No request due', '暂不补充', '補充の予定なし'),
    # ---- reworded for 0.1.10.34 (random days, the ask floor since 0.1.10.33, claims, refresh, players)
    G + 'tier_help.0': (
        'Normal: a commodity the market lacks, or a datapack request. Reward 1.15 x its value (halfway between the market\'s ask '
        'and bid), in coins or in goods of the market\'s stock. While the bounty is open the market sells the commodity at no less '
        'than the reward per item.',
        '普通：市场缺少的商品，或数据包的悬赏。奖励为价值（市场卖价与买价的中间）的 1.15 倍，以货币或市场库存的物资支付。悬赏期间市场'
        '以不低于每个物品报酬的价格出售该商品。',
        '普通：市場に足りない品目か、データパックの依頼。報酬は価値（市場の売値と買値の中間）の 1.15 倍で、硬貨か市場の在庫の品物で'
        '払います。懸賞の間、市場はその品目を 1 個あたりの報酬以上の値段で売ります。'),
    G + 'tier_help.1': (
        'Advanced: a set of goods. Reward 1.3 x the value: goods of the market\'s stock, parts made for it, UU-matter or coins. In '
        'multiplayer one player may deliver at most a share until the last day.',
        '高级：一组物资。奖励为价值的 1.3 倍：市场库存的物资、专门制作的部件、UU 物质或货币。多人游戏中，最后一天之前一名玩家最多交付一定份额。',
        '上級：品物のセット。報酬は価値の 1.3 倍で、市場の在庫の品物、そのために作られる部品、UU 物質、硬貨のいずれか。マルチプレイでは'
        '最終日まで、1 人が納品できるのは一定の割合までです。'),
    G + 'tier_help.2': (
        'Special: a rare item. Rare rewards: equipment made within a weekly limit, or rare items that earlier bounties received '
        '(the vault). Special bounties cannot be claimed.',
        '特别悬赏：一件稀有物品。稀有奖励：在每周上限内制作的设备，或此前悬赏收到的稀有物品（宝库）。特别悬赏不能接取。',
        '特別懸賞：希少品。希少な報酬：週の上限の内で作られる設備か、以前の懸賞が受け取った希少品（宝物庫）。特別懸賞は受注できません。'),
    G + 'help': (
        'The market posts bounties in three tiers; more players (active in the last three market days) get more of them, and the '
        'board posts a new normal request during the day when it runs low. Deliver with the button: the reward is paid at once in '
        'proportion to what you delivered (or by contribution when the bounty says so). Claim a bounty to keep it for you and '
        'your team; refresh the unclaimed normal requests for coins. Commodities you deliver go to the market\'s stock; rare items '
        'go to the vault and pay later special bounties. Unpaid rewards of an expired bounty go back to where they came from.',
        '市场分三个等级发布悬赏；玩家越多（最近三个市场日内活跃的玩家），悬赏越多，普通悬赏不足时白天也会补充。用按钮交付：奖励按你交付的'
        '比例立即支付（悬赏注明时按贡献分配）。接取悬赏可留给你和你的队伍；可以花货币刷新无人接取的普通悬赏。交付的商品进入市场库存；'
        '稀有物品进入宝库，之后作为特别悬赏的奖励发出。悬赏到期时未支付的奖励退回原处。',
        '市場は 3 段階の懸賞を出します。プレイヤー（直近 3 市場日に活動した人）が多いほど懸賞も増え、普通懸賞が減ると日中にも補充'
        'されます。ボタンで納品すると、報酬は納品した分に比例してすぐに払われます（懸賞の指定があれば貢献で分配）。懸賞を受注すると'
        '自分とチームだけのものになり、受注されていない普通懸賞は硬貨を払って更新できます。納品した市場品目は市場の在庫に、希少品は'
        '宝物庫に入り、後の特別懸賞の報酬になります。期限切れの懸賞の未払いの報酬は元の場所に戻ります。'),
    # ---- messages
    M + 'bounty_claim_special': ('§cSpecial bounties cannot be claimed.', '§c特别悬赏不能接取。', '§c特別懸賞は受注できません。'),
    M + 'bounty_claim_mine': ('§cYou already claimed this bounty.', '§c你已经接取了此悬赏。', '§cこの懸賞はすでに受注しています。'),
    M + 'bounty_claimed': (
        '§c%1$s claimed this bounty: only they and their team deliver to it.',
        '§c%1$s 已接取此悬赏：只有其本人和队伍可以交付。',
        '§c%1$s がこの懸賞を受注しています：本人とそのチームだけが納品できます。'),
    M + 'bounty_claim_started': (
        '§cSomeone already delivered to this bounty: it can no longer be claimed.',
        '§c已有人交付过此悬赏：不能再接取。',
        '§cこの懸賞にはすでに納品があります：受注できません。'),
    M + 'bounty_claim_limit': (
        '§cYou hold as many claims as you may: finish or abandon one first.',
        '§c你接取的悬赏已达上限：请先完成或放弃一个。',
        '§c受注できる数の上限です：先に 1 件を完了するか放棄してください。'),
    M + 'bounty_claim_banned': (
        '§cYou gave up a claim this market day: you can claim again on the next one.',
        '§c你本市场日放弃过接取：下一个市场日才能再次接取。',
        '§cこの市場日に受注を放棄しました：次の市場日から受注できます。'),
    M + 'bounty_claim_done': (
        '§aClaimed: only you and your team deliver to it (%1$s days left).',
        '§a已接取：只有你和你的队伍可以交付（剩余 %1$s 天）。',
        '§a受注しました：あなたとチームだけが納品できます（残り %1$s 日）。'),
    M + 'bounty_not_mine': ('§cYou did not claim this bounty.', '§c你没有接取此悬赏。', '§cこの懸賞は受注していません。'),
    M + 'bounty_abandoned': (
        '§eClaim given up: you can claim again on the next market day.',
        '§e已放弃接取：下一个市场日才能再次接取。',
        '§e受注を放棄しました：次の市場日から受注できます。'),
    M + 'bounty_refresh_none': (
        '§cNo normal request to refresh (claimed ones and those with deliveries stay).',
        '§c没有可刷新的普通悬赏（已接取的和已有交付的会保留）。',
        '§c更新できる普通懸賞がありません（受注中と納品済みのものは残ります）。'),
    M + 'bounty_refresh_limit': (
        '§cNo refresh left this market day (%1$s a day).',
        '§c本市场日的刷新次数已用完（每天 %1$s 次）。',
        '§cこの市場日の更新回数を使い切りました（1 日 %1$s 回）。'),
    M + 'bounty_refresh_wait': (
        '§cYou can refresh again in %1$s seconds.',
        '§c%1$s 秒后才能再次刷新。',
        '§c次の更新まであと %1$s 秒です。'),
    M + 'bounty_refresh_nothing': (
        '§cNo new request can be posted now: nothing was refreshed or paid.',
        '§c现在无法发布新的悬赏：未刷新，也未扣费。',
        '§c今は新しい懸賞を出せません：更新も支払いもしていません。'),
    M + 'bounty_refreshed': (
        '§aRefreshed: %1$s requests replaced by %2$s new ones for %3$s coins.',
        '§a已刷新：%1$s 条悬赏换成 %2$s 条新悬赏，花费 %3$s 货币。',
        '§a更新しました：懸賞 %1$s 件を新しい %2$s 件に入れ替えました（%3$s 硬貨）。'),
}

CONFIG = {
    'bountyRandomDays': ('Random Bounty Days by Rarity', '悬赏天数按稀有度随机', '懸賞の日数をレア度で決める'),
    'bountyMinDays': ('Fewest Days of a Bounty', '悬赏最少天数', '懸賞の最短日数'),
    'bountyMaxDays': ('Most Days of a Bounty', '悬赏最多天数', '懸賞の最長日数'),
    'bountyAcceptMax': ('Bounty Claims per Player', '每名玩家可接取的悬赏数', '1 人が受注できる懸賞の数'),
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
