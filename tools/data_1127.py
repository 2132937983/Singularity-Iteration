#!/usr/bin/env python3
"""0.1.10.27 (X2): the resources of the Trade Terminal (blockstate, block and item model, loot table, recipe, block tags,
guide page in en / zh / ja) and the settlement line of the Futures Machine guide page. Idempotent; keeps the line endings
of edited files. With --check it only reports what is missing.
Usage: data_1127.py <tree> [--check]"""
import json
import os
import sys

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
RES = os.path.join(TREE, 'src/main/resources')
ID = 'mio_icif:producer/block_trade_terminal'
missing = []


def write(rel, text):
    path = os.path.join(RES, rel)
    if CHECK:
        if not os.path.exists(path) or open(path, encoding='utf-8').read() != text:
            missing.append(rel)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w', encoding='utf-8', newline='\n').write(text)


def js(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False) + '\n'


write('assets/mio_icif/blockstates/producer/block_trade_terminal.json', js({'variants': {
    'facing=north': {'model': 'mio_icif:block/producer/block_trade_terminal'},
    'facing=south': {'model': 'mio_icif:block/producer/block_trade_terminal', 'y': 180},
    'facing=east': {'model': 'mio_icif:block/producer/block_trade_terminal', 'y': 90},
    'facing=west': {'model': 'mio_icif:block/producer/block_trade_terminal', 'y': 270}}}))
face = lambda u0, u1, cull: {'uv': [u0, 0, u1, 16], 'texture': '#all_lbr', 'cullface': cull}
write('assets/mio_icif/models/block/producer/block_trade_terminal.json', js({
    'parent': 'block/cube',
    'textures': {'particle': 'mio_icif:block/producer/block_machine', 'all_lbr': 'mio_icif:block/producer/block_trade_terminal',
                 'face': 'mio_icif:block/producer/block_trade_terminal'},
    'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {
        'down': face(0, 2.66, 'down'), 'up': face(2.66, 5.33, 'up'), 'south': face(13.33, 16, 'south'),
        'north': face(8, 10.66, 'north'), 'west': face(10.66, 13.33, 'west'), 'east': face(5.33, 8, 'east')}}]}))
write('assets/mio_icif/models/item/producer/block_trade_terminal.json', js({'parent': 'mio_icif:block/producer/block_trade_terminal'}))
write('data/mio_icif/loot_table/blocks/producer/block_trade_terminal.json', js({
    'type': 'minecraft:block',
    'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
               'entries': [{'type': 'minecraft:item', 'name': ID}], 'rolls': 1.0}],
    'random_sequence': 'mio_icif:blocks/producer/block_trade_terminal'}))
write('data/mio_icif/recipe/shaped_359_trade_terminal.json', js({
    'type': 'minecraft:crafting_shaped',
    'pattern': ['ACA', 'BEB', 'AHA'],
    'key': {'A': {'tag': 'c:circuits/basic'}, 'B': {'item': 'minecraft:hopper'}, 'C': {'item': 'minecraft:emerald'},
            'E': {'item': 'mio_icif:producer/block_future_elc'}, 'H': {'item': 'minecraft:chest'}},
    'result': {'id': ID, 'count': 1}}))

# ---------------------------------------------------------------- block tags (after the futures machine)
for tag in ('data/c/tags/block/machine.json', 'data/c/tags/block/needs_stone_tool.json',
            'data/minecraft/tags/block/mineable/pickaxe.json', 'data/mio_icif/tags/block/producer.json'):
    path = os.path.join(RES, tag)
    raw = open(path, 'rb').read()
    eol = '\r\n' if b'\r\n' in raw else '\n'
    data = json.loads(raw.decode('utf-8-sig'))
    ids = [v['id'] if isinstance(v, dict) else v for v in data['values']]
    if ID in ids:
        continue
    if CHECK:
        missing.append(tag)
        continue
    at = ids.index('mio_icif:producer/block_future_elc') + 1
    data['values'].insert(at, {'id': ID, 'required': False})
    open(path, 'wb').write((json.dumps(data, indent=2, ensure_ascii=False) + '\n').replace('\n', eol).encode('utf-8'))

# ---------------------------------------------------------------- guide: the terminal page
GUIDE = 'assets/mio_icif/guides/mio_icif/manual/'
POWER = {
    '': ('## Power data\n\n| Item | Value |\n|---|---|\n| Input voltage tier | LV (32 EU) |\n| Maximum input | 100 EU/t |\n'
         '| Energy storage | 10,000 EU |\n\nThe values are those of the Futures Machine (the terminal uses its config entries).\n'),
    '_zh_cn/': ('## 能量数据\n\n| 项目 | 数值 |\n|---|---|\n| 输入电压等级 | LV（32 EU） |\n| 最大输入 | 100 EU/t |\n'
                '| 储能 | 10,000 EU |\n\n数值与期货机相同（终端使用期货机的配置项）。\n'),
    '_ja_jp/': ('## 電力データ\n\n| 項目 | 値 |\n|---|---|\n| 入力電圧ティア | LV（32 EU） |\n| 最大入力 | 100 EU/t |\n'
                '| 蓄電量 | 10,000 EU |\n\n値は先物取引機と同じ（端末は先物取引機の設定項目を使う）。\n'),
}
PAGES = {
    '': ('Trade Terminal', '''# Trade Terminal

<Row>
  <BlockImage id="mio_icif:producer/block_trade_terminal" scale="3" />
</Row>

## Function

The Trade Terminal trades on the exchange for its owner, the player who placed it, while its chunk is loaded.
Every trade it makes is the owner's: the same rules as a manual trade (sell depth, daily allowance, buy limit, ledger), coins from and to the owner's mailbox.
Each trade costs the EU of one trade, like the Futures Machine. A terminal also starts the new market day, so a base without a Futures Machine still has its daily auction.

- **Sell buffer** (27 slots): pipes and hoppers put commodities in, also items of a commodity's tag. Pipes cannot take anything out of it.
- **Output buffer** (9 slots): with the output mode on, purchases go here and pipes can take them out. With it off, purchases go to the owner's mailbox.
- **Standing orders** (up to 8, every 10 seconds): sell what is above a kept amount, either into the daily auction (with a minimum in percent of the bid) or at once (with a minimum price); or buy up to a target stock at a highest price, paid from the owner's mailbox coins. Instant sales and purchases wait for a batch of 64 items, because every trade rounds its coins. The Orders page shows why an order did not trade.
- **Owner offline**: only auction sell orders run, and not for commodities that need an unlock advancement. Instant sales and purchases wait for the owner.
- **Access**: the owner gives players View, Trade or Configure, and can give the members of the owner's scoreboard team a level. Trade fills the sell buffer and uses the settlement slot; Configure edits orders and takes items out. Operators count as Configure.
- **Settlement slot**: put in a shulker box, a backpack, an AE2 storage cell or a container of UU-matter. Sell its commodities as yourself, put its UU-matter on your account, load it into the sell buffer or unload the output buffer into it. The Settlement page delivers its items to bounties and commissions and buys into it. The Futures Machine has the same slot on its Settlement page. AE2 is optional; the container comes back when you close the panel.
- **Breaking** the terminal sends the buffers to the owner's mailbox; items with names, enchantments or damage drop.

'''),
    '_zh_cn/': ('交易终端', '''# 交易终端

<Row>
  <BlockImage id="mio_icif:producer/block_trade_terminal" scale="3" />
</Row>

## 功能

交易终端在其区块加载期间为所有者（放置它的玩家）在交易所交易。
它的每笔交易都属于所有者：规则与手动交易相同（出价曲线、每日额度、购买上限、账本），货币进出所有者的邮箱。
每笔交易与期货机一样消耗一次交易的 EU。终端也会开启新的市场日，因此没有期货机的基地也有每日拍卖。

- **出售缓冲区**（27 格）：管道和漏斗放入商品，也包括商品标签中的物品。管道无法从中取出物品。
- **输出缓冲区**（9 格）：输出模式开启时购买的物品进入这里，管道可以取出；关闭时购买的物品进入所有者的邮箱。
- **常设订单**（最多 8 个，每 10 秒运行一次）：出售超过保留数量的部分，可以交给每日拍卖（最低价为出价的百分比），也可以立即出售（设最低价格）；或按最高价格买到目标库存，用所有者邮箱中的货币支付。立即出售和购买以 64 件为一批进行，因为每笔交易都会对货币取整。“订单”页显示订单未成交的原因。
- **所有者离线时**：只运行拍卖出售订单，需要解锁进度的商品除外。立即出售和购买会等待所有者上线。
- **权限**：所有者可以给玩家“查看”“交易”“设置”权限，也可以给所有者计分板队伍的成员设定权限。“交易”可以装入出售缓冲区并使用结算槽；“设置”可以编辑订单和取出物品。管理员视为“设置”。
- **结算槽**：放入潜影盒、背包、AE2 存储元件或装有 UU 物质的容器。以自己的身份出售其中的商品，把 UU 物质存入自己的账户，装入出售缓冲区，或把输出缓冲区卸入其中。“大宗结算”页可以把其中的物品交付给悬赏和委托，并把购买的物品直接放入容器。期货机的“大宗结算”页也有同样的槽位。AE2 为可选联动；关闭界面时容器会退回。
- **拆除**终端时，缓冲区中的物品进入所有者的邮箱；带名称、附魔或损耗的物品会掉落。

'''),
    '_ja_jp/': ('取引端末', '''# 取引端末

<Row>
  <BlockImage id="mio_icif:producer/block_trade_terminal" scale="3" />
</Row>

## 機能

取引端末は、チャンクが読み込まれている間、所有者（設置したプレイヤー）のために取引所で取引する。
端末の取引はすべて所有者のもので、手動の取引と同じ規則（買値曲線・1 日の上限・購入上限・台帳）に従い、硬貨は所有者のメールボックスに出入りする。
取引ごとに先物取引機と同じく 1 取引分の EU を使う。端末も新しい取引日を始めるので、先物取引機のない拠点でも日次オークションが動く。

- **売りバッファ**（27 スロット）: パイプやホッパーが商品を搬入する。商品のタグに含まれる品も入る。パイプは取り出せない。
- **出力バッファ**（9 スロット）: 出力モードがオンなら購入品はここに入り、パイプで取り出せる。オフなら所有者のメールボックスへ入る。
- **常設注文**（最大 8 件、10 秒ごと）: 残す数を超えた分を売る。売り方は日次オークション（最低額は買値に対する %）か即時売却（最低価格）。または目標の在庫まで上限価格で買い、所有者のメールボックスの硬貨で払う。取引ごとに硬貨が端数処理されるので、即時売却と購入は 64 個単位でまとめて行う。注文が取引しなかった理由は「常設注文」ページに出る。
- **所有者がオフラインのとき**: オークションの売り注文だけが動く。解除の進捗が必要な品目は除く。即時売却と購入は所有者を待つ。
- **アクセス**: 所有者はプレイヤーに閲覧・取引・設定の権限を与え、所有者のスコアボードチームのメンバーにも権限を設定できる。取引は売りバッファへの搬入と決済スロットの利用、設定は注文の編集と取り出し。管理者は設定と同等。
- **決済スロット**: シュルカーボックス、バックパック、AE2 ストレージセル、UU 物質の入った容器を入れる。中の商品を自分として売る、UU 物質を自分の口座へ入れる、売りバッファへ積む、出力バッファを取り出して入れる。「一括決済」ページでは懸賞・依頼への納品と、購入品をコンテナへ直接入れることもできる。先物取引機の「一括決済」ページにも同じスロットがある。AE2 は任意連携。画面を閉じるとコンテナは戻る。
- 端末を**壊す**と、バッファの中身は所有者のメールボックスへ送られる。名前・エンチャント・損耗のある品はその場に落ちる。

'''),
}
for prefix, (title, body) in PAGES.items():
    text = ('---\nnavigation:\n  title: "%s"\n  icon: %s\n  parent: machines.md\n  position: 29\nitem_ids:\n  - %s\n---\n\n'
            % (title, ID, ID)) + body + POWER[prefix]
    write(GUIDE + prefix + 'machines/producer_block_trade_terminal.md', text)

# ---------------------------------------------------------------- guide: the settlement line of the futures page
SETTLE = {
    '': ('every bounty alone.\n',
         '- **Settlement**: put a shulker box, a backpack, an AE2 storage cell or a container of UU-matter into the slot of the '
         'Settlement page. Its commodities sell now or at the daily auction like the sell box, its plain items deliver to '
         'bounties and commissions, its UU-matter goes onto your account, and purchases of the selected market can go straight '
         'into it. The container comes back when you close the panel. AE2 is optional.\n'),
    '_zh_cn/': ('所有悬赏都可以由你一人完成。\n',
                '- **大宗结算**：在“大宗结算”页的槽位中放入潜影盒、背包、AE2 存储元件或装有 UU 物质的容器。其中的商品与出售箱一样可以'
                '立即出售或交给每日拍卖，普通物品可以交付给悬赏和委托，UU 物质可以存入账户，所选商品的购买也可以直接放入容器。关闭界面时'
                '容器会退回。AE2 为可选联动。\n'),
    '_ja_jp/': ('どの懸賞も 1 人で完了できる。\n',
                '- **一括決済**: 「一括決済」ページのスロットにシュルカーボックス、バックパック、AE2 ストレージセル、UU 物質の入った容器を'
                '入れる。中の商品は売却ボックスと同じく即時売却か日次オークションに出せ、通常の品は懸賞・依頼に納品でき、UU 物質は口座へ'
                '入れられ、選んだ品目の購入品をそのまま入れることもできる。画面を閉じるとコンテナは戻る。AE2 は任意連携。\n'),
}
for prefix, (anchor, line) in SETTLE.items():
    path = os.path.join(RES, GUIDE + prefix + 'machines/producer_block_future_elc.md')
    raw = open(path, 'rb').read()
    eol = '\r\n' if b'\r\n' in raw else '\n'
    s = raw.decode('utf-8').replace('\r\n', '\n')
    if line in s:
        continue
    if CHECK:
        missing.append(path)
        continue
    assert s.count(anchor) == 1, (path, anchor)
    s = s.replace(anchor, anchor + line)
    open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))

if CHECK:
    print('missing or different:', missing)
    sys.exit(1 if missing else 0)
print('data_1127 written')
