#!/usr/bin/env python3
"""0.1.10.43: the texts of the release.
- The guide (en / zh / ja, Futures Machine page): the treasury bonds, the treasury fund and the new settings.
- The datapack document (zh, si_exchange.md): the new economy keys, the ledger of frozen coins and the operator commands.
- The API guide (en / zh): what ledger(server, days) reports for the coins of bonds and the fund (the types are unchanged).
Each text block goes after its anchor line (the line that starts with the anchor text). Idempotent; keeps the line endings of
edited files. With --check it only reports what is missing.
Usage: data_1143.py <tree> [--check]"""
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
missing = []

GUIDE = 'src/main/resources/assets/mio_icif/guides/mio_icif/manual/'
FUT = 'machines/producer_block_future_elc.md'
D_EX = 'docs/datapacks/si_exchange.md'
API_EN, API_ZH = 'docs/api/API_GUIDE.en.md', 'docs/api/API_GUIDE.zh_cn.md'

ADD = {
    GUIDE + FUT: [
        ('- **Wealth ranking** (since 0.1.10.42',
         '- **Treasury bonds** (since 0.1.10.43, Bonds at the top of the Treasury tab): lend coins to the treasury for 7 or 28 market '
         'days, 1,000 coins a unit. The weekly rate is fixed when you buy: with the stabiliser on 0.5 % + 1.5 % x (net issue of the '
         'last 7 days / target - 1), between 0.2 % and 2.0 %, so bonds pay more the more coins the server issues above its target; '
         'with the stabiliser off a fixed 0.5 %. At maturity the principal and the interest go to your coin account (28 days at '
         '0.5 % a week pay 2 %). An early exit pays the principal less 2 % and no interest. A player holds at most 1,000,000 coins '
         'of bonds (bondMaxPerPlayer); all bonds together at most 25 % of the stabiliser\'s money supply estimate '
         '(bondMaxSupplyPercent): above it no new bonds are sold. The principal is out of circulation until it comes back (the '
         'ledger and the stabiliser count it so); the interest comes from the treasury reserve first, the rest is new coins. '
         'Commands: /mio_icif market bonds; operators: bonds all.'),
        ('- **Treasury bonds** (since 0.1.10.43',
         '- **Treasury fund** (since 0.1.10.43, Fund at the top of the Treasury tab): units of 1,000 coins, without a daily price, '
         'that share what the treasury realised in each market week. Profit: stock sold to players above what the market paid for '
         'it, and the import fees; loss: stock sold below its cost and stock paid out as bounty rewards (at its cost). The investors\' '
         'share is the invested principal / (invested principal + the cost of the treasury stock), at most 50 %; the principal counts '
         'by the days of the week it was in. A profit week pays half of the investors\' share as dividends to the coin accounts (the '
         'rest of the profit stays destroyed); a loss week takes the investors\' share from their principal. A subscription is paid '
         'at once and joins at the next market day (until then Cancel waiting gives it back); units stay at least 7 market days; '
         'Redeem books units to be paid 7 market days later less a fee of 1 % (to the reserve) and cannot be taken back. The '
         'Fund page shows this week\'s profit and loss so far and the last 8 weeks with your part. The wealth ranking counts bonds '
         'and fund units in the total assets (Bonds and fund). Commands: /mio_icif market fund; operators: fund all.'),
        ('- Since 0.1.10.42 the server config',
         '- Since 0.1.10.43 the server config (or economy.json) also sets bondsEnabled (true), bondMaxPerPlayer (1,000,000 coins; '
         '0 = no cap), bondMaxSupplyPercent (25; 0 = no cap) and treasuryFund (true).'),
    ],
    GUIDE + '_zh_cn/' + FUT: [
        ('- **财富排行**（自 0.1.10.42 起',
         '- **国债**（自 0.1.10.43 起，“国库”页顶部的“国债”）：把货币借给国库 7 或 28 个市场日，每份 1,000 货币。周利率在购买时锁定：'
         '稳定器开启时为 0.5 % + 1.5 % × (最近 7 天净发行 ÷ 目标 − 1)，在 0.2 % 到 2.0 % 之间，服务器发行的货币越超过目标，国债利率越高；'
         '稳定器关闭时固定为 0.5 %。到期时本金与利息进入你的货币账户（28 天、每周 0.5 % 共得 2 %）。提前解约返还本金的 98 %，没有利息。'
         '每名玩家最多持有 1,000,000 货币的国债（bondMaxPerPlayer）；全部国债合计不超过稳定器货币量估算的 25 %（bondMaxSupplyPercent），'
         '超过后停止发行新国债。本金在偿还前离开流通（账本与稳定器按此计算）；利息先由国库储备支付，其余为新发行。命令：'
         '/mio_icif market bonds；管理员：bonds all。'),
        ('- **国债**（自 0.1.10.43 起',
         '- **国库运用基金**（自 0.1.10.43 起，“国库”页顶部的“基金”）：每份 1,000 货币，没有每日净值，按市场周分享国库的已实现损益。'
         '收益：库存以高于市场买入成本的价格卖给玩家，以及进口手续费；亏损：低于成本卖出的库存，以及作为悬赏奖励发出的库存（按成本计）。'
         '投资者份额 = 投资本金 ÷ (投资本金 + 国库库存成本)，最多 50 %；本金按其在该周的天数计入。盈利周把投资者份额的一半作为分红发到'
         '货币账户（其余收益保持销毁）；亏损周从投资者本金中扣除投资者份额。申购当即付款，在下一个市场日加入（之前可用“撤回申购”退回）；'
         '份额至少持有 7 个市场日；“赎回”预约在 7 个市场日后支付，扣 1 % 手续费（进入储备），预约不可撤回。“基金”页显示本周至今的损益'
         '与最近 8 周及你的部分。财富排行的总资产计入国债与基金份额（“国债与基金”）。命令：/mio_icif market fund；管理员：fund all。'),
        ('- 自 0.1.10.42 起，服务器配置',
         '- 自 0.1.10.43 起，服务器配置（或 economy.json）还可设置 bondsEnabled（true）、bondMaxPerPlayer（1,000,000 货币；0 = 不限）、'
         'bondMaxSupplyPercent（25；0 = 不限）与 treasuryFund（true）。'),
    ],
    GUIDE + '_ja_jp/' + FUT: [
        ('- **資産ランキング**（0.1.10.42 から',
         '- **国債**（0.1.10.43 から、「国庫」タブ上部の「国債」）：硬貨を国庫に 7 または 28 市場日貸し出す。1 口 1,000 硬貨。週利は購入時に'
         '固定：スタビライザー有効時は 0.5 % + 1.5 % × (直近 7 日の正味発行 ÷ 目標 − 1)、0.2 %〜2.0 % の範囲で、サーバーの発行が目標を'
         '超えるほど利率が上がる。無効時は 0.5 % 固定。満期に元本と利息がコイン口座へ入る（28 日・週 0.5 % なら 2 %）。中途解約は元本の '
         '98 % を返し、利息はない。1 人の保有は最大 1,000,000 硬貨（bondMaxPerPlayer）、全国債の合計はスタビライザーの推定流通量の 25 % '
         'まで（bondMaxSupplyPercent）で、超えると新規発行が止まる。元本は償還まで流通から外れる（台帳とスタビライザーもそう数える）。'
         '利息は国庫準備金から先に払い、残りは新規発行。コマンド：/mio_icif market bonds、オペレーター：bonds all。'),
        ('- **国債**（0.1.10.43 から',
         '- **国庫運用ファンド**（0.1.10.43 から、「国庫」タブ上部の「ファンド」）：1 口 1,000 硬貨、日々の基準価額はなく、市場週ごとに'
         '国庫の実現損益を分け合う。利益：市場が買った原価を上回る在庫の売却と輸入手数料。損失：原価割れの売却と、懸賞報酬として払い出した'
         '在庫（原価で）。投資家持分 = 投資元本 ÷ (投資元本 + 国庫在庫の原価総額)、最大 50 %。元本はその週に在籍した日数で数える。利益の週は'
         '投資家持分の半分を配当としてコイン口座へ払い（残りの利益は破棄のまま）、損失の週は投資家持分を元本から差し引く。申込はその場で'
         '支払い、次の市場日に組み入れ（それまでは「申込取消」で戻せる）。口数は最低 7 市場日保有。「解約」は 7 市場日後の支払いを予約し、'
         '手数料 1 %（準備金へ）を引く。予約は取り消せない。「ファンド」ページに今週の途中経過と直近 8 週、あなたの分が出る。資産ランキング'
         'の総資産に国債とファンドの口数が入る（「国債・ファンド」）。コマンド：/mio_icif market fund、オペレーター：fund all。'),
        ('- 0.1.10.42 から、サーバー設定',
         '- 0.1.10.43 から、サーバー設定（または economy.json）で bondsEnabled（true）、bondMaxPerPlayer（1,000,000 硬貨、0 = 上限なし）、'
         'bondMaxSupplyPercent（25、0 = 上限なし）、treasuryFund（true）も設定できる。'),
    ],
    D_EX: [
        ('- 0.1.10.42 新增的键',
         '- 0.1.10.43 新增的键：`bondsEnabled`（true，国债）、`bondMaxPerPlayer`（1,000,000，每名玩家的国债本金上限；0 = 不限）、'
         '`bondMaxSupplyPercent`（25，全部国债本金不超过稳定器货币量估算的此百分比，超过后停止发行；0 = 不限）、`treasuryFund`'
         '（true，国库运用基金接受申购）。账本：国债本金与基金申购在偿还前计为冻结（`FrozenIn`，离开流通，稳定器按销毁计算），到期、'
         '提前解约、赎回与撤回申购时回到流通（`FrozenOut`）；国债利息（`BondInterest`）先由国库储备支付，其余为新发行；基金分红'
         '（`FundDividend`）为新发行；基金亏损（`FundLoss`）是不再返还的冻结本金。所有玩家可用 `/mio_icif market bonds` 与 '
         '`/mio_icif market fund` 查看。'),
        ('| `/mio_icif market wealth refresh` |',
         '| `/mio_icif market bonds all` | 全部国债：本金、持有人、服务器上限、未来 7 个市场日到期的本金与利息、最近 7 天的利息（0.1.10.43） |\n'
         '| `/mio_icif market fund all` | 国库基金：投资者、投资本金、等待中的份额、累计分红与亏损（0.1.10.43） |'),
    ],
    API_EN: [
        ('* Since 0.1.10.35 the fees of the exchange go into a treasury reserve',
         '* Since 0.1.10.43 coins put into treasury bonds and the treasury fund are frozen out of circulation until they are paid '
         'back: `ledger(server, days)` counts them in `burned` when they are frozen and in `minted` when they come back (the '
         'principal at maturity, early exits, redemptions and taken-back subscriptions, before their penalty or fee). Bond interest '
         'and fund dividends are in `minted` (bond interest is paid from the reserve first). The API types are unchanged.'),
    ],
    API_ZH: [
        ('* 自 0.1.10.35 起，交易所的手续费进入国库储备',
         '* 自 0.1.10.43 起，投入国债与国库基金的货币在偿还前冻结、离开流通：`ledger(server, days)` 在冻结时计入 `burned`，回到流通时'
         '（到期本金、提前解约、赎回与撤回申购，在扣除违约金或手续费之前）计入 `minted`。国债利息与基金分红计入 `minted`（国债利息先由'
         '储备支付）。API 类型不变。'),
    ],
}

for rel, blocks in ADD.items():
    path = os.path.join(TREE, rel)
    raw = open(path, 'rb').read()
    eol = '\r\n' if b'\r\n' in raw else '\n'
    s = raw.decode('utf-8').replace('\r\n', '\n')
    before = s
    for anchor, block in blocks:
        if block in s:
            continue
        if CHECK:
            missing.append(f'{rel}: {block[:40]}')
            continue
        rows = s.split('\n')
        at = [i for i, r in enumerate(rows) if r.startswith(anchor)]
        assert len(at) == 1, (rel, anchor, at)
        rows.insert(at[0] + 1, block)
        s = '\n'.join(rows)
    if rel == GUIDE + FUT:
        assert s.isascii(), rel
    for anchor, block in blocks:
        assert '<' not in block, (rel, block[:40])   # the guide lint of older checks flags any '<'
    if s != before and not CHECK:
        open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))
        print('wrote', rel)

if CHECK:
    print('missing or different:', missing)
    sys.exit(1 if missing else 0)
print('data_1143 written')
