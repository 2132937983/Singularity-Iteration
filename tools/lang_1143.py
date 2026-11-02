#!/usr/bin/env python3
"""0.1.10.43 language values (en_us / zh_cn / ja_jp / ru_ru): the treasury bonds and the treasury fund (the Futures Machine's
treasury tab, messages, commands), the bonds and fund part of the wealth ranking and the new server settings.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys.
Usage: lang_1143.py <repo root> [--check]
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
LANGS = ('en_us', 'zh_cn', 'ja_jp', 'ru_ru')


def v(en=None, zh=None, ja=None, ru=None):
    return {k: t for k, t in zip(LANGS, (en, zh, ja, ru)) if t is not None}


G = 'gui.mio_icif.exchange.'
M = 'message.mio_icif.exchange.'
BC = 'command.mio_icif.bonds.'
FC = 'command.mio_icif.fund.'
W = 'command.mio_icif.wealth.'
CFG = 'mio_icif.configuration.FutureMarket.'

KEYS = {
    # ---------------------------------------------------------------- the treasury tab's switch and the ranking part
    G + 'treasury_switch.2': v('Bonds', '国债', '国債', 'Облигации'),
    G + 'treasury_switch.3': v('Fund', '基金', 'ファンド', 'Фонд'),
    G + 'ranking.part.5': v('Bonds and fund', '国债与基金', '国債・ファンド', 'Облигации и фонд'),
    G + 'ranking.part_help.5': v(
        'Principal of your treasury bonds and treasury fund units (with the units waiting for the next market day).',
        '你的国债本金与国库基金份额的本金（含等待下一个市场日的份额）。',
        '国債の元本と国庫運用ファンドの元本（次の市場日を待つ口数を含む）。',
        'Основная сумма ваших облигаций и паёв фонда (вместе с паями, ждущими следующего рыночного дня).'),

    # ---------------------------------------------------------------- treasury bonds (panel)
    G + 'bonds.title': v('Treasury bonds', '国债', '国債', 'Казначейские облигации'),
    G + 'bonds.rate': v('Weekly rate now', '当前周利率', '現在の週利', 'Ставка за неделю'),
    G + 'bonds.rate_help': v(
        'The rate is fixed when you buy. Stabiliser on: 0.5 %% + 1.5 %% × (net issue of the last 7 days ÷ target − 1), between '
        '0.2 %% and 2.0 %%: the more coins the server issues above its target, the more bonds pay. Stabiliser off: 0.5 %%. A '
        '28-day bond earns four weeks of it.',
        '购买时锁定利率。稳定器开启：0.5 %% + 1.5 %% × (最近 7 天净发行 ÷ 目标 − 1)，在 0.2 %% 到 2.0 %% 之间：服务器发行的货币越超过'
        '目标，国债利率越高。稳定器关闭：0.5 %%。28 天国债可得四周利息。',
        '利率は購入時に固定されます。スタビライザー有効時：0.5 %% + 1.5 %% × (直近 7 日の正味発行 ÷ 目標 − 1)、0.2 %%〜2.0 %% の範囲。'
        'サーバーの発行が目標を超えるほど利率が上がります。無効時：0.5 %%。28 日物は 4 週分の利息です。',
        'Ставка фиксируется при покупке. Стабилизатор включён: 0,5 %% + 1,5 %% × (чистый выпуск за 7 дней ÷ цель − 1), от 0,2 %% '
        'до 2,0 %%: чем больше сервер выпускает монет сверх цели, тем выше доход. Стабилизатор выключен: 0,5 %%. Облигация на 28 '
        'дней приносит доход четырёх недель.'),
    G + 'bonds.stabilizer': v(
        'Net issue 7 days %1$s / target %2$s', '7 天净发行 %1$s / 目标 %2$s', '7 日の正味発行 %1$s / 目標 %2$s',
        'Выпуск за 7 дн. %1$s / цель %2$s'),
    G + 'bonds.stabilizer_off': v(
        'Stabiliser off: fixed rate', '稳定器关闭：固定利率', 'スタビライザー無効：固定金利', 'Стабилизатор выключен: ставка постоянна'),
    G + 'bonds.term': v('%s days', '%s 天', '%s 日', '%s дн.'),
    G + 'bonds.units': v('Units', '份数', '口数', 'Штук'),
    G + 'bonds.buy': v('Buy', '购买', '購入', 'Купить'),
    G + 'bonds.buy_help': v(
        'Pay %1$s coins from your coin account or inventory for %2$s-day bonds at the rate shown.',
        '从货币账户或背包支付 %1$s 货币，按所示利率购买 %2$s 天国债。',
        '口座または手持ちの %1$s 硬貨で、表示の利率の %2$s 日物国債を購入します。',
        'Заплатить %1$s монет со счёта или из инвентаря за облигации на %2$s дн. по показанной ставке.'),
    G + 'bonds.cost': v('Price (1,000 coins a unit)', '价格（每份 1,000 货币）', '価格（1 口 1,000 硬貨）', 'Цена (1 000 монет за штуку)'),
    G + 'bonds.interest': v('Interest at maturity (day %s)', '到期利息（第 %s 天）', '満期の利息（%s 日目）', 'Доход к погашению (день %s)'),
    G + 'bonds.room': v('Units you may still buy', '还可购买的份数', 'あと買える口数', 'Можно купить ещё'),
    G + 'bonds.mine': v('Your bonds / cap', '你的国债 / 上限', '保有額 / 上限', 'Ваши облигации / лимит'),
    G + 'bonds.all': v('All bonds / server cap', '全部国债 / 服务器上限', '全国債 / サーバー上限', 'Все облигации / лимит'),
    G + 'bonds.all_help': v(
        "New bonds are sold only while all bonds stay below the server cap: a share of the stabiliser's money supply estimate "
        '(bondMaxSupplyPercent, 25 %% by default).',
        '只有全部国债低于服务器上限时才发行新国债：上限为稳定器货币量估算的一定比例（bondMaxSupplyPercent，默认 25 %%）。',
        '全国債がサーバー上限を下回る間だけ新規発行されます。上限はスタビライザーの推定流通量の一定割合（bondMaxSupplyPercent、既定 25 %%）。',
        'Новые облигации продаются, только пока все облигации ниже лимита сервера — доли оценки денежной массы стабилизатора '
        '(bondMaxSupplyPercent, по умолчанию 25 %%).'),
    G + 'bonds.exit_note': v(
        'Early exit: principal less 2 %%, no interest', '提前解约：本金扣 2 %%，无利息', '中途解約：元本 −2 %%、利息なし',
        'Досрочный выход: минус 2 %%, без дохода'),
    G + 'bonds.holdings': v('Your bonds', '持有的国债', '保有国債', 'Ваши облигации'),
    G + 'bonds.none': v('No bonds yet.', '尚无国债。', '国債はまだありません。', 'Облигаций пока нет.'),
    G + 'bonds.row': v('%1$s units · %2$s days · %3$s', '%1$s 份 · %2$s 天 · %3$s', '%1$s 口・%2$s 日・%3$s', '%1$s шт. · %2$s дн. · %3$s'),
    G + 'bonds.row2': v('Matures day %1$s · +%2$s', '第 %1$s 天到期 · +%2$s', '%1$s 日目に満期・+%2$s', 'Погашение: день %1$s · +%2$s'),
    G + 'bonds.exit': v('Exit', '解约', '解約', 'Выйти'),
    G + 'bonds.exit_help': v(
        'Exit now: %1$s coins to your coin account (penalty %2$s); the interest of %3$s is lost.',
        '现在解约：%1$s 货币进入货币账户（违约金 %2$s）；放弃利息 %3$s。',
        '今解約：%1$s 硬貨が口座へ（違約金 %2$s）。利息 %3$s は失われます。',
        'Выйти сейчас: %1$s монет на счёт (штраф %2$s); доход %3$s теряется.'),
    G + 'bonds.total': v('Principal + interest', '本金 + 利息', '元本 + 利息', 'Сумма + доход'),
    G + 'bonds.off': v(
        'Treasury bonds are not sold on this server (bondsEnabled). Bonds already bought still mature.',
        '本服务器不发行国债（bondsEnabled）。已购国债照常到期。',
        'このサーバーでは国債を販売していません（bondsEnabled）。購入済みの国債は通常どおり満期を迎えます。',
        'На этом сервере облигации не продаются (bondsEnabled). Купленные облигации погашаются как обычно.'),

    # ---------------------------------------------------------------- treasury fund (panel)
    G + 'fund.title': v('Treasury fund', '国库运用基金', '国庫運用ファンド', 'Казначейский фонд'),
    G + 'fund.units': v('Your units', '你的份额', '保有口数', 'Ваши паи'),
    G + 'fund.principal': v('Principal', '本金', '元本', 'Основная сумма'),
    G + 'fund.principal_help': v(
        'Coins your units stand for: 1,000 a unit, less the losses taken.', '份额对应的货币：每份 1,000，扣除已承担的亏损。',
        '口数が表す硬貨：1 口 1,000、負担した損失を引いた額。', 'Сколько монет стоят ваши паи: 1 000 за пай минус понесённые убытки.'),
    G + 'fund.pending': v('Waiting for the next day', '等待下一个市场日', '次の市場日待ち', 'Ждут следующего дня'),
    G + 'fund.booked': v('Booked for redemption', '已预约赎回', '解約予約', 'Заявлено к погашению'),
    G + 'fund.booked_value': v('%1$s (day %2$s)', '%1$s（第 %2$s 天）', '%1$s（%2$s 日目）', '%1$s (день %2$s)'),
    G + 'fund.result': v('Dividends / losses', '分红 / 亏损', '配当 / 損失', 'Дивиденды / убытки'),
    G + 'fund.units_box': v('Units', '份数', '口数', 'Паи'),
    G + 'fund.coins': v('= %s coins', '= %s 货币', '= %s 硬貨', '= %s монет'),
    G + 'fund.subscribe': v('Subscribe', '申购', '申込', 'Вложить'),
    G + 'fund.redeem': v('Redeem', '赎回', '解約', 'Погасить'),
    G + 'fund.cancel': v('Cancel waiting', '撤回申购', '申込取消', 'Отменить'),
    G + 'fund.subscribe_help': v(
        'Pay 1,000 coins a unit now; the units join the fund at the next market day. Units still waiting can be taken back.',
        '现在每份支付 1,000 货币；份额在下一个市场日加入基金。等待中的份额可以撤回。',
        '今 1 口 1,000 硬貨を支払い、次の市場日にファンドへ組み入れられます。待機中の口数は取り消せます。',
        'Заплатите сейчас 1 000 монет за пай; паи войдут в фонд в следующий рыночный день. Ожидающие паи можно забрать.'),
    G + 'fund.redeem_help': v(
        'Book units for redemption: paid 7 market days later (and at least 7 days after they joined), less 1 %%. A booking '
        'cannot be taken back; booked units share the results until they are paid.',
        '预约赎回份额：7 个市场日后支付（且加入至少满 7 天），扣 1 %% 手续费。预约不可撤回；已预约的份额在支付前继续分享损益。',
        '口数の解約を予約：7 市場日後（かつ組入れから 7 日以上）に手数料 1 %% を引いて支払われます。予約は取り消せません。'
        '支払いまでは損益の分配を受け続けます。',
        'Заявить паи к погашению: выплата через 7 рыночных дней (и не раньше 7 дней после входа) за вычетом 1 %%. Заявку нельзя '
        'отменить; до выплаты паи участвуют в результатах.'),
    G + 'fund.cancel_help': v(
        'Take back the units waiting for the next market day (coins to your coin account).',
        '撤回等待下一个市场日的份额（货币退回货币账户）。',
        '次の市場日を待っている口数を取り消します（硬貨は口座へ）。',
        'Забрать паи, ждущие следующего рыночного дня (монеты — на счёт).'),
    G + 'fund.rules': v(
        '1 unit = 1,000 coins, no daily price. Each market week the fund shares what the treasury realised: stock sold above its '
        'cost and import fees are profit; stock sold below its cost and stock paid as bounty rewards are loss. Investors\' share = '
        'invested ÷ (invested + stock cost), at most 50 %%. A profit pays half of the share as dividends (the rest stays '
        'destroyed); a loss takes the share from the principal.',
        '1 份 = 1,000 货币，没有每日净值。基金按市场周分享国库的已实现损益：库存以高于成本的价格卖出与进口手续费为收益；低于成本卖出'
        '与作为悬赏奖励发出的库存为亏损。投资者份额 = 投资本金 ÷ (投资本金 + 库存成本)，最多 50 %%。盈利时份额的一半作为分红发放（其余'
        '保持销毁）；亏损时份额从本金中扣除。',
        '1 口 = 1,000 硬貨、日々の基準価額はありません。市場週ごとに国庫の実現損益を分け合います：原価を上回る在庫の売却と輸入手数料が'
        '利益、原価割れの売却と懸賞報酬として払い出した在庫が損失です。投資家持分 = 投資元本 ÷ (投資元本 + 在庫原価)、最大 50 %%。'
        '利益の週は持分の半分を配当として支払い（残りは破棄のまま）、損失の週は持分を元本から差し引きます。',
        '1 пай = 1 000 монет, без ежедневной цены. Каждую рыночную неделю фонд делит реализованный результат казны: продажи запасов '
        'дороже себестоимости и импортные сборы — прибыль; продажи дешевле себестоимости и запасы, выданные как награды, — убыток. '
        'Доля инвесторов = вложено ÷ (вложено + себестоимость запасов), не более 50 %%. Прибыль: половина доли выплачивается '
        'дивидендами (остальное остаётся уничтоженным); убыток: доля списывается с основной суммы.'),
    G + 'fund.off': v(
        'The treasury fund takes no subscriptions on this server (treasuryFund). Units already in it still share the results and '
        'can be redeemed.',
        '本服务器的国库基金不接受申购（treasuryFund）。已有份额照常分享损益并可赎回。',
        'このサーバーでは国庫運用ファンドの申込を受け付けていません（treasuryFund）。既存の口数は引き続き損益を分け合い、解約できます。',
        'Фонд не принимает новые паи на этом сервере (treasuryFund). Имеющиеся паи участвуют в результатах и могут быть погашены.'),
    G + 'fund.results': v('Realised results', '已实现损益', '実現損益', 'Реализованный результат'),
    G + 'fund.invested': v('Invested (all)', '投资本金（全体）', '投資元本（全体）', 'Вложено (всего)'),
    G + 'fund.invested_help': v(
        "Principal of all investors' units: %1$s investors; %2$s units waiting for the next market day.",
        '所有投资者份额的本金：%1$s 名投资者；%2$s 份等待下一个市场日。',
        '全投資家の元本：投資家 %1$s 人、次の市場日を待つ口数 %2$s。',
        'Основная сумма паёв всех инвесторов: инвесторов %1$s; паёв ждут следующего дня: %2$s.'),
    G + 'fund.stock_cost': v('Treasury stock cost', '国库库存成本', '国庫在庫の原価', 'Себестоимость запасов'),
    G + 'fund.share': v("Investors' share now", '当前投资者份额', '現在の投資家持分', 'Доля инвесторов'),
    G + 'fund.share_help': v(
        'Invested ÷ (invested + stock cost), at most 50 %%; a week uses its average invested principal.',
        '投资本金 ÷ (投资本金 + 库存成本)，最多 50 %%；每周按该周的平均投资本金计算。',
        '投資元本 ÷ (投資元本 + 在庫原価)、最大 50 %%。週ごとにその週の平均投資元本で計算します。',
        'Вложено ÷ (вложено + себестоимость запасов), не более 50 %%; для недели берётся средняя вложенная сумма.'),
    G + 'fund.this_week': v('This market week so far', '本市场周至今', '今週（市場週）の途中経過', 'Эта рыночная неделя'),
    G + 'fund.gain': v('Profit', '收益', '利益', 'Прибыль'),
    G + 'fund.gain_help': v(
        'Stock sold above its cost: %1$s; import fees: %2$s.', '高于成本卖出的库存：%1$s；进口手续费：%2$s。',
        '原価を上回る在庫の売却：%1$s、輸入手数料：%2$s。', 'Продажа запасов дороже себестоимости: %1$s; импортные сборы: %2$s.'),
    G + 'fund.loss': v('Loss', '亏损', '損失', 'Убыток'),
    G + 'fund.loss_help': v(
        'Stock sold below its cost: %1$s; stock paid as bounty rewards: %2$s.', '低于成本卖出的库存：%1$s；作为悬赏奖励发出的库存：%2$s。',
        '原価割れの在庫の売却：%1$s、懸賞報酬として払い出した在庫：%2$s。',
        'Продажа запасов дешевле себестоимости: %1$s; запасы, выданные как награды: %2$s.'),
    G + 'fund.history': v('Past weeks (yours)', '往期（你的部分）', '過去の週（あなたの分）', 'Прошлые недели (ваше)'),
    G + 'fund.no_history': v('No week distributed yet.', '尚无已分配的周。', '分配済みの週はまだありません。', 'Распределённых недель пока нет.'),
    G + 'fund.week_row': v('Week %1$s: %2$s', '第 %1$s 周：%2$s', '第 %1$s 週：%2$s', 'Неделя %1$s: %2$s'),
    G + 'fund.week_help': v(
        'Week %1$s: profit %2$s, loss %3$s; invested %4$s, stock cost %5$s, share %6$s; dividends %7$s, losses %8$s, kept '
        'destroyed %9$s; yours %10$s.',
        '第 %1$s 周：收益 %2$s，亏损 %3$s；投资本金 %4$s，库存成本 %5$s，份额 %6$s；分红 %7$s，承担亏损 %8$s，保持销毁 %9$s；'
        '你的部分 %10$s。',
        '第 %1$s 週：利益 %2$s、損失 %3$s。投資元本 %4$s、在庫原価 %5$s、持分 %6$s。配当 %7$s、損失負担 %8$s、破棄のまま %9$s。'
        'あなたの分 %10$s。',
        'Неделя %1$s: прибыль %2$s, убыток %3$s; вложено %4$s, себестоимость %5$s, доля %6$s; дивиденды %7$s, убытки %8$s, '
        'осталось уничтоженным %9$s; ваша часть %10$s.'),

    # ---------------------------------------------------------------- messages
    M + 'bond_off': v(
        '§cTreasury bonds are not sold on this server.', '§c本服务器不发行国债。', '§cこのサーバーでは国債を販売していません。',
        '§cНа этом сервере облигации не продаются.'),
    M + 'bond_server_cap': v(
        '§cAll bonds have reached the server cap of %1$s coins; you may buy %2$s more units.',
        '§c全部国债已达服务器上限 %1$s 货币；你还可购买 %2$s 份。',
        '§c全国債がサーバー上限 %1$s 硬貨に達しています。あと %2$s 口まで購入できます。',
        '§cВсе облигации достигли лимита сервера %1$s монет; можно купить ещё %2$s шт.'),
    M + 'bond_player_cap': v(
        '§cYou may hold at most %1$s coins of bonds; you may buy %2$s more units.',
        '§c你最多持有 %1$s 货币的国债；还可购买 %2$s 份。',
        '§c国債の保有上限は %1$s 硬貨です。あと %2$s 口まで購入できます。',
        '§cМожно держать облигаций не более чем на %1$s монет; можно купить ещё %2$s шт.'),
    M + 'bond_holdings_full': v(
        '§cYou already hold %s bond holdings; wait for one to mature.', '§c你已持有 %s 笔国债；请等待到期。',
        '§c国債の保有が %s 件に達しています。満期をお待ちください。', '§cУ вас уже %s пакетов облигаций; дождитесь погашения.'),
    M + 'bond_bought': v(
        '§aBought %1$s bond units for %2$s coins at %3$s a week: +%4$s interest on day %5$s.',
        '§a以周利率 %3$s 购买国债 %1$s 份，支付 %2$s 货币：第 %5$s 天得到利息 +%4$s。',
        '§a国債 %1$s 口を %2$s 硬貨で購入しました（週利 %3$s）：%5$s 日目に利息 +%4$s。',
        '§aКуплено облигаций: %1$s шт. за %2$s монет под %3$s в неделю: +%4$s дохода в день %5$s.'),
    M + 'no_bond': v('§cNo such bond.', '§c没有这笔国债。', '§cその国債はありません。', '§cТакой облигации нет.'),
    M + 'bond_exited': v(
        '§eExited %1$s bond units: %2$s coins to your coin account (penalty %3$s).',
        '§e已解约国债 %1$s 份：%2$s 货币进入货币账户（违约金 %3$s）。',
        '§e国債 %1$s 口を解約しました：%2$s 硬貨を口座へ（違約金 %3$s）。',
        '§eВыход из облигаций: %1$s шт.; %2$s монет на счёт (штраф %3$s).'),
    M + 'bond_matured': v(
        '§aYour bonds matured: %1$s coins of principal and %2$s of interest to your coin account.',
        '§a你的国债已到期：本金 %1$s 与利息 %2$s 货币已进入货币账户。',
        '§a国債が満期になりました：元本 %1$s 硬貨と利息 %2$s 硬貨を口座へ。',
        '§aОблигации погашены: %1$s монет основной суммы и %2$s дохода на счёт.'),
    M + 'fund_off': v(
        '§cThe treasury fund takes no subscriptions on this server.', '§c本服务器的国库基金不接受申购。',
        '§cこのサーバーでは国庫運用ファンドの申込を受け付けていません。', '§cФонд не принимает новые паи на этом сервере.'),
    M + 'fund_full': v(
        '§cThe treasury fund has no room for more investors.', '§c国库基金的投资者已满。',
        '§c国庫運用ファンドの投資家数が上限に達しています。', '§cВ фонде нет места для новых инвесторов.'),
    M + 'fund_cap': v(
        '§cYou may hold at most %s fund units.', '§c你最多持有 %s 份基金。', '§cファンドの保有は最大 %s 口です。',
        '§cМожно держать не более %s паёв фонда.'),
    M + 'fund_lots_full': v(
        '§cYou already have %s lots in the fund; redeem some or wait.', '§c你在基金中已有 %s 批份额；请先赎回或等待。',
        '§cファンドの保有ロットが %s 件に達しています。解約するかお待ちください。', '§cУ вас уже %s партий паёв; погасите часть или подождите.'),
    M + 'fund_subscribed': v(
        '§aSubscribed %1$s fund units for %2$s coins; they join the fund on day %3$s.',
        '§a已申购基金 %1$s 份，支付 %2$s 货币；第 %3$s 天加入基金。',
        '§aファンドに %1$s 口（%2$s 硬貨）申し込みました。%3$s 日目に組み入れられます。',
        '§aВложено паёв: %1$s за %2$s монет; они войдут в фонд в день %3$s.'),
    M + 'fund_no_pending': v(
        '§cNo units are waiting for the next market day.', '§c没有等待下一个市场日的份额。', '§c次の市場日を待っている口数はありません。',
        '§cНет паёв, ждущих следующего рыночного дня.'),
    M + 'fund_cancelled': v(
        '§eTook back %1$s waiting units: %2$s coins to your coin account.', '§e已撤回 %1$s 份等待中的份额：%2$s 货币退回货币账户。',
        '§e待機中の %1$s 口を取り消しました：%2$s 硬貨を口座へ。', '§eОтменено паёв: %1$s; %2$s монет на счёт.'),
    M + 'fund_not_enough': v(
        '§cOnly %s of your units are free to redeem.', '§c你只有 %s 份可以赎回。', '§c解約できる口数は %s 口だけです。',
        '§cСвободно для погашения только %s паёв.'),
    M + 'fund_redeem_booked': v(
        '§aBooked %1$s units for redemption; paid on day %2$s, less 1 %%.', '§a已预约赎回 %1$s 份；第 %2$s 天支付，扣 1 %% 手续费。',
        '§a%1$s 口の解約を予約しました。%2$s 日目に手数料 1 %% を引いて支払われます。',
        '§aЗаявлено к погашению паёв: %1$s; выплата в день %2$s за вычетом 1 %%.'),
    M + 'fund_dividend': v(
        '§aTreasury fund dividend: %s coins to your coin account.', '§a国库基金分红：%s 货币进入货币账户。',
        '§a国庫運用ファンドの配当：%s 硬貨を口座へ。', '§aДивиденды фонда: %s монет на счёт.'),
    M + 'fund_loss': v(
        '§eThe treasury fund had a loss week: %s coins taken from your principal.', '§e国库基金本周亏损：从你的本金扣除 %s 货币。',
        '§e国庫運用ファンドは損失の週でした：元本から %s 硬貨を差し引きました。',
        '§eУ фонда убыточная неделя: с вашей основной суммы списано %s монет.'),
    M + 'fund_redeemed': v(
        '§aRedeemed %1$s fund units: %2$s coins to your coin account (fee %3$s).', '§a已赎回基金 %1$s 份：%2$s 货币进入货币账户（手续费 %3$s）。',
        '§aファンド %1$s 口を解約しました：%2$s 硬貨を口座へ（手数料 %3$s）。', '§aПогашено паёв: %1$s; %2$s монет на счёт (сбор %3$s).'),

    # ---------------------------------------------------------------- commands
    BC + 'rate': v(
        'Treasury bonds: weekly rate now %1$s (%2$s).', '国债：当前周利率 %1$s（%2$s）。', '国債：現在の週利 %1$s（%2$s）。',
        'Облигации: ставка за неделю %1$s (%2$s).'),
    BC + 'on': v('on sale', '发行中', '販売中', 'в продаже'),
    BC + 'off': v('not sold on this server', '本服务器不发行', 'このサーバーでは販売なし', 'не продаются на этом сервере'),
    BC + 'none': v('You hold no bonds.', '你没有国债。', '国債を保有していません。', 'У вас нет облигаций.'),
    BC + 'row': v(
        '%1$s units, %2$s days at %3$s: matures day %4$s, +%5$s', '%1$s 份，%2$s 天，%3$s：第 %4$s 天到期，+%5$s',
        '%1$s 口・%2$s 日物・%3$s：%4$s 日目に満期、+%5$s', '%1$s шт., %2$s дн. под %3$s: погашение в день %4$s, +%5$s'),
    BC + 'room': v(
        'Your principal %1$s; you may buy %2$s more units.', '你的本金 %1$s；还可购买 %2$s 份。', '保有元本 %1$s、あと %2$s 口購入できます。',
        'Ваша основная сумма %1$s; можно купить ещё %2$s шт.'),
    BC + 'all': v(
        'All bonds: %1$s coins, %2$s holders, %3$s holdings; server cap %4$s (money supply estimate %5$s).',
        '全部国债：%1$s 货币，%2$s 名持有人，%3$s 笔；服务器上限 %4$s（货币量估算 %5$s）。',
        '全国債：%1$s 硬貨、保有者 %2$s 人、%3$s 件。サーバー上限 %4$s（推定流通量 %5$s）。',
        'Все облигации: %1$s монет, держателей %2$s, пакетов %3$s; лимит сервера %4$s (оценка денежной массы %5$s).'),
    BC + 'no_cap': v('none', '不限', '上限なし', 'нет'),
    BC + 'all_due': v(
        'Maturing in the next 7 market days: principal %1$s, interest %2$s.', '未来 7 个市场日到期：本金 %1$s，利息 %2$s。',
        '今後 7 市場日に満期：元本 %1$s、利息 %2$s。', 'Погашается в ближайшие 7 рыночных дней: сумма %1$s, доход %2$s.'),
    BC + 'all_paid': v(
        'Interest of the last 7 days: %1$s (from the reserve: %2$s).', '最近 7 天支付的利息：%1$s（其中来自储备：%2$s）。',
        '直近 7 日の利払い：%1$s（うち準備金から：%2$s）。', 'Доход за последние 7 дней: %1$s (из резерва: %2$s).'),
    FC + 'status': v(
        "Treasury fund: invested %1$s, stock cost %2$s, investors' share %3$s (%4$s).",
        '国库基金：投资本金 %1$s，库存成本 %2$s，投资者份额 %3$s（%4$s）。',
        '国庫運用ファンド：投資元本 %1$s、在庫原価 %2$s、投資家持分 %3$s（%4$s）。',
        'Фонд: вложено %1$s, себестоимость запасов %2$s, доля инвесторов %3$s (%4$s).'),
    FC + 'on': v('open', '接受申购', '受付中', 'открыт'),
    FC + 'off': v('no subscriptions on this server', '本服务器不接受申购', 'このサーバーでは受付なし', 'не принимает паи на этом сервере'),
    FC + 'week': v(
        'This market week so far: profit %1$s, loss %2$s.', '本市场周至今：收益 %1$s，亏损 %2$s。', '今週の途中経過：利益 %1$s、損失 %2$s。',
        'Эта рыночная неделя: прибыль %1$s, убыток %2$s.'),
    FC + 'row': v(
        'Week %1$s: profit %2$s, loss %3$s, share %4$s, dividends %5$s, losses %6$s.',
        '第 %1$s 周：收益 %2$s，亏损 %3$s，份额 %4$s，分红 %5$s，承担亏损 %6$s。',
        '第 %1$s 週：利益 %2$s、損失 %3$s、持分 %4$s、配当 %5$s、損失負担 %6$s。',
        'Неделя %1$s: прибыль %2$s, убыток %3$s, доля %4$s, дивиденды %5$s, убытки %6$s.'),
    FC + 'none': v('You hold no fund units.', '你没有基金份额。', 'ファンドの口数を保有していません。', 'У вас нет паёв фонда.'),
    FC + 'mine': v(
        'Your units %1$s (principal %2$s), waiting %3$s, booked %4$s; dividends %5$s, losses %6$s.',
        '你的份额 %1$s（本金 %2$s），等待中 %3$s，已预约赎回 %4$s；分红 %5$s，亏损 %6$s。',
        '保有口数 %1$s（元本 %2$s）、待機 %3$s、解約予約 %4$s。配当 %5$s、損失 %6$s。',
        'Ваши паи %1$s (сумма %2$s), ждут %3$s, заявлено к погашению %4$s; дивиденды %5$s, убытки %6$s.'),
    FC + 'all': v(
        'Fund: %1$s investors, invested %2$s, waiting units %3$s; dividends %4$s and losses %5$s in total; kept destroyed (last 8 '
        'weeks) %6$s.',
        '基金：%1$s 名投资者，投资本金 %2$s，等待中份额 %3$s；累计分红 %4$s、亏损 %5$s；保持销毁（最近 8 周）%6$s。',
        'ファンド：投資家 %1$s 人、投資元本 %2$s、待機口数 %3$s。累計配当 %4$s、損失 %5$s。破棄のまま（直近 8 週）%6$s。',
        'Фонд: инвесторов %1$s, вложено %2$s, ожидающих паёв %3$s; всего дивидендов %4$s и убытков %5$s; осталось уничтоженным '
        '(8 недель) %6$s.'),
    W + 'parts_bonds': v(' bonds and fund %s', ' 国债与基金 %s', ' 国債・ファンド %s', ' облигации и фонд %s'),

    # ---------------------------------------------------------------- server settings
    CFG + 'bondsEnabled': v('Treasury Bonds', '国债', '国債', 'Казначейские облигации'),
    CFG + 'bondMaxPerPlayer': v(
        'Bond Principal Cap per Player (coins)', '每名玩家国债本金上限（货币）', 'プレイヤーごとの国債元本上限（硬貨）',
        'Лимит облигаций на игрока (монеты)'),
    CFG + 'bondMaxSupplyPercent': v(
        'Bond Cap of the Server (percent of the money supply)', '服务器国债上限（货币量的百分比）', 'サーバーの国債上限（流通量に対するパーセント）',
        'Лимит облигаций сервера (процент денежной массы)'),
    CFG + 'treasuryFund': v('Treasury Fund', '国库运用基金', '国庫運用ファンド', 'Казначейский фонд'),
}


def main():
    bad = 0
    for lang in LANGS:
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        mine = {k: t[lang] for k, t in KEYS.items() if lang in t}
        if CHECK:
            stray = [k for k, t in mine.items() if '%' in re.sub(r'%(?:\d+\$)?([A-Za-z%]|$)', '', t)]
            bad += len(stray)
            if stray:
                print(lang, 'stray %:', stray)
            wrong = [k for k, t in mine.items() if data.get(k) != t]
            bad += len(wrong)
            print(lang, 'missing or different:', len(wrong), wrong[:5])
            continue
        for key, t in mine.items():
            data[key] = t
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(mine))
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
