#!/usr/bin/env python3
"""0.1.10.26 language values (en_us / zh_cn / ja_jp): the exchange X3 - the Bounties tab of the futures panel (tiers,
needs, rewards and their sources, the player's share, weekly budgets, vault), the bounty messages, the operator command
/mio_icif bounty and the new config entries of the bounty tiers. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1126.py <repo root> [--check]
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
B = G + 'bounty.'
M = 'message.mio_icif.exchange.'
C = 'command.mio_icif.bounty.'
F = 'mio_icif.configuration.'

KEYS = {
    G + 'tab.6': ('Bounties', '悬赏', '懸賞'),
    B + 'board': ('System bounties', '系统悬赏', 'システム懸賞'),
    B + 'none': (
        'No bounty is open. The market posts normal bounties every day, advanced ones a few times a week and special ones rarely.',
        '目前没有悬赏。市场每天发布普通悬赏，每周发布数件高级悬赏，特别悬赏则很少出现。',
        '開いている懸賞はありません。市場は普通の懸賞を毎日、上級を週に数件、特別懸賞をまれに出します。'),
    B + 'tier.0': ('Normal', '普通', '普通'),
    B + 'tier.1': ('Advanced', '高级', '上級'),
    B + 'tier.2': ('Special', '特别悬赏', '特別懸賞'),
    B + 'tier_help.0': (
        'Normal: a commodity the market lacks, 3 days. Reward 1.15 x the value at the market\'s ask, in coins or in goods of the '
        'market\'s stock. The market does not sell this commodity while the bounty is open.',
        '普通：市场缺少的商品，3 天。奖励为按市场卖价计算的价值的 1.15 倍，以货币或市场库存的物资支付。悬赏期间市场不出售该商品。',
        '普通：市場に足りない品目、3 日。報酬は市場の売値で見た価値の 1.15 倍で、硬貨か市場の在庫の品物で払います。懸賞の間、市場はその品目を売りません。'),
    B + 'tier_help.1': (
        'Advanced: a set of goods, 7 days. Reward 1.3 x the value: goods of the market\'s stock, parts made for it, UU-matter or '
        'coins. In multiplayer one player may deliver at most a share until the last day.',
        '高级：一组物资，7 天。奖励为价值的 1.3 倍：市场库存的物资、专门制作的部件、UU 物质或货币。多人游戏中，最后一天之前一名玩家最多交付一定份额。',
        '上級：品物のセット、7 日。報酬は価値の 1.3 倍で、市場の在庫の品物、そのために作られる部品、UU 物質、硬貨のいずれか。マルチプレイでは最終日まで、'
        '1 人が納品できるのは一定の割合までです。'),
    B + 'tier_help.2': (
        'Special: a rare item, 7 days. Rare rewards: equipment made within a weekly limit, or rare items that earlier bounties '
        'received (the vault).',
        '特别悬赏：一件稀有物品，7 天。稀有奖励：在每周上限内制作的设备，或此前悬赏收到的稀有物品（宝库）。',
        '特別懸賞：希少品、7 日。希少な報酬：週の上限の内で作られる設備か、以前の懸賞が受け取った希少品（宝物庫）。'),
    B + 'shared': ('shared by contribution', '按贡献分配', '貢献で分配'),
    B + 'shared_help': (
        'Paid when the bounty is complete or expires: every reward is split by the value each player delivered.',
        '在完成或到期时支付：每项奖励按每名玩家交付的价值分配。',
        '完了か期限切れのときに払います。各報酬は、各プレイヤーが納品した価値で分けます。'),
    B + 'contributors': ('%s players', '%s 名玩家', '%s 人'),
    B + 'reward': ('Reward', '奖励', '報酬'),
    B + 'source.0': ('new coins', '新发行货币', '新規発行の硬貨'),
    B + 'source.1': ('market stock', '市场库存', '市場の在庫'),
    B + 'source.2': ('vault', '宝库', '宝物庫'),
    B + 'source.3': ('made for the bounty', '为悬赏制作', '懸賞のために生成'),
    B + 'paid': ('paid %1$s of %2$s', '已付 %1$s / %2$s', '支払い済み %1$s / %2$s'),
    B + 'coin_amount': ('%s coins', '%s 货币', '%s コイン'),
    B + 'uu_amount': ('%s mB UU', '%s mB UU', '%s mB UU'),
    B + 'deliver_help': (
        'Delivers everything this bounty still needs that you carry (plain items of the main inventory, not the armour or '
        'off-hand slots).',
        '交付你携带的、该悬赏仍需要的全部物品（主物品栏中的普通物品，不含盔甲与副手栏）。',
        'この懸賞がまだ必要とする物のうち、持っている物をすべて納品します（メインインベントリの、手を加えていない物。防具と左手は除く）。'),
    B + 'mine': ('Yours %s', '你的 %s', 'あなた %s'),
    B + 'mine_room': ('Yours %1$s, room %2$s (%3$s%%)', '你的 %1$s，还可 %2$s（%3$s%%）', 'あなた %1$s、残り %2$s（%3$s%%）'),
    B + 'value_help': (
        'Values in coins at the market\'s ask when the bounty was posted. In multiplayer one player may deliver at most this '
        'share of an advanced bounty until its last day.',
        '数值为发布悬赏时按市场卖价计算的货币价值。多人游戏中，高级悬赏在最后一天之前一名玩家最多交付此份额。',
        '値は懸賞の掲示時の市場の売値で見た硬貨の価値です。マルチプレイでは、上級の懸賞は最終日まで 1 人がこの割合までしか納品できません。'),
    B + 'budget': ('This market week', '本交易周', '今週の市場'),
    B + 'budget_advanced': ('Advanced rewards made: %1$s / %2$s', '高级奖励已制作：%1$s / %2$s', '上級の報酬の生成：%1$s / %2$s'),
    B + 'budget_special': ('Special rewards made: %1$s / %2$s', '特别奖励已制作：%1$s / %2$s', '特別の報酬の生成：%1$s / %2$s'),
    B + 'budget_uu': ('UU-matter paid: %1$s / %2$s mB', 'UU 物质已发放：%1$s / %2$s mB', 'UU 物質の支払い：%1$s / %2$s mB'),
    B + 'vault': ('Vault: %1$s kinds, %2$s items', '宝库：%1$s 种，%2$s 个', '宝物庫：%1$s 種、%2$s 個'),
    B + 'budget_help': (
        'Bounties make items and UU-matter only within these weekly limits (counted when a bounty is posted). Other rewards '
        'come from the market\'s stock or the vault, or are coins.',
        '悬赏只在这些每周上限内制作物品与发放 UU 物质（在发布悬赏时计入）。其他奖励来自市场库存或宝库，或为货币。',
        '懸賞がアイテムを作り UU 物質を払うのは、この週の上限の内側だけです（懸賞の掲示時に数えます）。その他の報酬は市場の在庫か宝物庫の物、'
        'または硬貨です。'),
    B + 'help': (
        'The market posts bounties in three tiers. Deliver with the button: the reward is paid at once in proportion to what '
        'you delivered (or by contribution when the bounty says so). Commodities you deliver go to the market\'s stock; rare '
        'items go to the vault and pay later special bounties. Unpaid rewards of an expired bounty go back to where they came '
        'from. In single player every bounty can be delivered by you alone.',
        '市场分三个等级发布悬赏。用按钮交付：奖励按你交付的比例立即支付（悬赏注明时按贡献分配）。交付的商品进入市场库存；稀有物品进入宝库，'
        '之后作为特别悬赏的奖励发出。悬赏到期时未支付的奖励退回原处。单人游戏中所有悬赏都可以由你一人完成。',
        '市場は 3 段階の懸賞を出します。ボタンで納品すると、報酬は納品した分に比例してすぐに払われます（懸賞の指定があれば貢献で分配）。'
        '納品した市場品目は市場の在庫に、希少品は宝物庫に入り、後の特別懸賞の報酬になります。期限切れの懸賞の未払いの報酬は元の場所に戻ります。'
        'シングルプレイでは、どの懸賞も 1 人で完了できます。'),
    M + 'no_bounty': ('§cThis bounty is closed.', '§c该悬赏已结束。', '§cこの懸賞は終了しています。'),
    M + 'bounty_share': (
        '§cYou delivered your share (%s%%) of this bounty; others can deliver the rest, or you on its last day.',
        '§c你已交付了该悬赏的份额（%s%%）；其余由其他玩家交付，或在最后一天由你交付。',
        '§cこの懸賞のあなたの割合（%s%%）は納品済みです。残りはほかのプレイヤーか、最終日にあなたが納品できます。'),
    M + 'bounty_delivered': ('§aDelivered %1$s items; %2$s coins paid now.', '§a已交付 %1$s 个；现在支付 %2$s 货币。', '§a%1$s 個を納品しました。今 %2$s コインを払いました。'),
    M + 'bounty_completed': (
        '§6Bounty complete: %1$s items delivered, %2$s coins paid now (other rewards go to your inventory or mailbox).',
        '§6悬赏完成：交付 %1$s 个，现在支付 %2$s 货币（其他奖励进入物品栏或邮箱）。',
        '§6懸賞完了：%1$s 個を納品、今 %2$s コインを払いました（その他の報酬はインベントリかメールボックスへ）。'),
    M + 'bounty_completed_mail': (
        '§6[Exchange] A bounty you delivered to is complete; your rewards are in your mailbox.',
        '§6[交易所] 你参与交付的悬赏已完成；奖励已放入邮箱。',
        '§6[取引所] 納品した懸賞が完了しました。報酬はメールボックスにあります。'),
    M + 'bounty_special_posted': (
        '§6[Exchange] Special bounty: the market wants %1$s (%2$s days). See the Bounties tab of a futures machine.',
        '§6[交易所] 特别悬赏：市场需要 %1$s（%2$s 天）。请查看期货交易机的“悬赏”页。',
        '§6[取引所] 特別懸賞：市場が %1$s を求めています（%2$s 日）。先物取引機の「懸賞」タブを見てください。'),
    C + 'budget': (
        'Market day %1$s. Left this week: advanced %2$s of %3$s items, special %4$s of %5$s items, UU-matter %6$s of %7$s mB; vault %8$s kinds',
        '交易日 %1$s。本周剩余：高级 %2$s / %3$s 个，特别 %4$s / %5$s 个，UU 物质 %6$s / %7$s mB；宝库 %8$s 种',
        '取引日 %1$s。今週の残り：上級 %2$s / %3$s 個、特別 %4$s / %5$s 個、UU 物質 %6$s / %7$s mB。宝物庫 %8$s 種'),
    C + 'none': ('No open bounty.', '没有未结束的悬赏。', '開いている懸賞はありません。'),
    C + 'line': (
        '#%1$s %2$s [%3$s]: %4$s | %5$s | %6$s days | %7$s | %8$s contributors',
        '#%1$s %2$s [%3$s]：%4$s | %5$s | %6$s 天 | %7$s | %8$s 名贡献者',
        '#%1$s %2$s [%3$s]：%4$s | %5$s | %6$s 日 | %7$s | 貢献者 %8$s 人'),
    C + 'tables': ('%1$s bounty tables, %2$s reward pools', '%1$s 个悬赏表，%2$s 个奖励池', '懸賞の表 %1$s、報酬プール %2$s'),
    C + 'table': (
        '%1$s: %2$s, weight %3$s, %4$s requirements, pool %5$s, %6$s',
        '%1$s：%2$s，权重 %3$s，%4$s 项要求，奖励池 %5$s，%6$s',
        '%1$s：%2$s、重み %3$s、要求 %4$s 件、プール %5$s、%6$s'),
    C + 'eligible': ('can be posted now', '现在可发布', '今掲示できる'),
    C + 'waiting': ('waiting (day, cooldown or open count)', '等待中（日数、冷却或同时数量）', '待機中（日数、クールダウン、同時の件数）'),
    C + 'no_table': ('§cNo bounty table %s', '§c没有悬赏表 %s', '§c懸賞の表 %s はありません'),
    C + 'not_posted': (
        '§cThe bounty %s was not posted (no reward can be paid, a listener cancelled it, or the board is full).',
        '§c悬赏 %s 未发布（没有可支付的奖励、监听器取消了发布，或悬赏板已满）。',
        '§c懸賞 %s は掲示されませんでした（払える報酬がない、リスナーが取り消した、または掲示板が満杯）。'),
    C + 'posted': ('Posted %1$s as bounty #%2$s', '已发布 %1$s，悬赏编号 #%2$s', '%1$s を懸賞 #%2$s として掲示しました'),
    C + 'no_bounty': ('§cNo open bounty #%s', '§c没有未结束的悬赏 #%s', '§c開いている懸賞 #%s はありません'),
    C + 'cancelled': ('Bounty #%s removed; its unpaid reserved rewards went back', '已移除悬赏 #%s；未支付的预留奖励已退回', '懸賞 #%s を取り消しました。未払いの確保済み報酬は戻りました'),
    C + 'errors': ('%s problems in the si_exchange datapacks:', 'si_exchange 数据包中有 %s 个问题：', 'si_exchange データパックの問題 %s 件：'),
    C + 'settings': ('%s economy settings (value, from config / datapack / default):', '%s 项经济设置（值，来自配置 / 数据包 / 默认）：',
                     '経済の設定 %s 項目（値、設定ファイル / データパック / 既定のどれか）：'),
}

CONFIG = {
    'bountyNormalMin': ('Normal Bounties Open: Fewest', '普通悬赏同时数量：最少', '普通の懸賞の同時件数：最少'),
    'bountyNormalMax': ('Normal Bounties Open: Most', '普通悬赏同时数量：最多', '普通の懸賞の同時件数：最多'),
    'bountyNormalValue': ('Value of a Normal Request (coins)', '普通委托的价值（货币）', '普通の依頼の価値（硬貨）'),
    'bountyNormalDays': ('Days of a Normal Bounty', '普通悬赏天数', '普通の懸賞の日数'),
    'bountyNormalMultiplier': ('Normal Bounty Reward Multiplier', '普通悬赏奖励倍率', '普通の懸賞の報酬倍率'),
    'bountyNormalMinQuantity': ('Fewest Items of a Normal Request', '普通委托的最少数量', '普通の依頼の最少個数'),
    'bountyNormalMaxQuantity': ('Most Items of a Normal Request', '普通委托的最多数量', '普通の依頼の最多個数'),
    'bountyAdvancedPerWeekMin': ('Advanced Bounties per Week: Fewest', '每周高级悬赏：最少', '週あたりの上級の懸賞：最少'),
    'bountyAdvancedPerWeekMax': ('Advanced Bounties per Week: Most', '每周高级悬赏：最多', '週あたりの上級の懸賞：最多'),
    'bountyAdvancedDays': ('Days of an Advanced Bounty', '高级悬赏天数', '上級の懸賞の日数'),
    'bountyAdvancedMultiplier': ('Advanced Bounty Reward Multiplier', '高级悬赏奖励倍率', '上級の懸賞の報酬倍率'),
    'bountyAdvancedPlayerShare': ('Advanced Bounty Share per Player', '高级悬赏每名玩家份额', '上級の懸賞の 1 人あたりの割合'),
    'bountySpecialChancePerDay': ('Special Bounty Chance per Day', '每日特别悬赏概率', '1 日あたりの特別懸賞の確率'),
    'bountySpecialMaxOpen': ('Special Bounties Open at Once', '同时开放的特别悬赏', '同時に開く特別懸賞'),
    'bountySpecialDays': ('Days of a Special Bounty', '特别悬赏天数', '特別懸賞の日数'),
    'bountyIssuePerWeekNormal': ('Items Made for Normal Bounties per Week', '每周为普通悬赏制作的物品', '週あたりに普通の懸賞のため作るアイテム'),
    'bountyIssuePerWeekAdvanced': ('Items Made for Advanced Bounties per Week', '每周为高级悬赏制作的物品', '週あたりに上級の懸賞のため作るアイテム'),
    'bountyIssuePerWeekSpecial': ('Items Made for Special Bounties per Week', '每周为特别悬赏制作的物品', '週あたりに特別懸賞のため作るアイテム'),
    'bountyUuPerWeek': ('UU-Matter for Bounties per Week (mB)', '每周悬赏发放的 UU 物质（mB）', '週あたりに懸賞が払う UU 物質（mB）'),
    'bountyTreasuryLow': ('Stock Below Which the Market Asks for a Commodity', '市场发出委托的库存下限', '市場が依頼を出す在庫の下限'),
}
for key, values in CONFIG.items():
    KEYS[F + 'FutureMarket.' + key] = values
    KEYS[F + key] = values


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
