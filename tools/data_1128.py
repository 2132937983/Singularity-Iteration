#!/usr/bin/env python3
"""0.1.10.28 (X4): the resources of the ID card (item model, 16 x 16 texture, recipe) and the guide lines of the exchange X4
(Futures Machine page: the coin <-> UU board, the scrap sink, the stabiliser, the ID card; Trade Terminal page: the SCRAP mode
and the delegate cards), en / zh / ja. Idempotent; keeps the line endings of edited files. With --check it only reports what is
missing. Usage: data_1128.py <tree> [--check]"""
import json
import os
import struct
import sys
import zlib

TREE = sys.argv[1]
CHECK = '--check' in sys.argv[2:]
RES = os.path.join(TREE, 'src/main/resources')
missing = []


def write(rel, data):
    path = os.path.join(RES, rel)
    if CHECK:
        if not os.path.exists(path) or open(path, 'rb').read() != data:
            missing.append(rel)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(data)


def js(obj):
    return (json.dumps(obj, indent=2, ensure_ascii=False) + '\n').encode('utf-8')


def png(w, h, rows):
    raw = b''.join(b'\x00' + bytes(r) for r in rows)

    def chunk(kind, body):
        return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body) & 0xffffffff)

    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) \
        + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')


# ---------------------------------------------------------------- the ID card: 16 x 16 card with a chip, a photo and text lines
PAL = {'.': (0, 0, 0, 0), 'o': (24, 40, 64, 255), 'b': (64, 132, 196, 255), 'l': (120, 178, 228, 255), 'g': (232, 188, 64, 255),
       'G': (168, 124, 28, 255), 'p': (214, 222, 232, 255), 'P': (140, 152, 168, 255), 't': (36, 72, 112, 255)}
CARD = [
    '................',
    '................',
    '................',
    '.oooooooooooooo.',
    'obbbbbbbbbbbbbbo',
    'oblllllllbpppPbo',
    'obggGlllllpPPPbo',
    'obgGgllllbpPPPbo',
    'obggGllllbpppPbo',
    'oblllllllbbbbbbo',
    'obtttttltttttlbo',
    'oblllllllllllllo',
    'obtttltttttlllbo',
    '.oooooooooooooo.',
    '................',
    '................',
]
rows = []
for line in CARD:
    r = bytearray()
    for ch in line:
        r += bytes(PAL[ch])
    rows.append(r)
write('assets/mio_icif/textures/item/normal/item_id_card.png', png(16, 16, rows))
write('assets/mio_icif/models/item/normal/item_id_card.json', js({'parent': 'minecraft:item/generated',
                                                                  'textures': {'layer0': 'mio_icif:item/normal/item_id_card'}}))
write('data/mio_icif/recipe/shapeless_id_card.json', js({
    'type': 'minecraft:crafting_shapeless',
    'ingredients': [{'item': 'minecraft:paper'}, {'item': 'minecraft:paper'}, {'item': 'minecraft:gold_nugget'}, {'tag': 'c:circuits/basic'}],
    'result': {'id': 'mio_icif:normal/item_id_card', 'count': 2}}))

# ---------------------------------------------------------------- guide lines
GUIDE = 'assets/mio_icif/guides/mio_icif/manual/'
FUTURES = {
    '': ('The container comes back when you close the panel. AE2 is optional.\n',
         '- **UU-matter board**: players trade UU-matter of their accounts for coins on the market "UU-matter (lots of 100 mB)": one unit is '
         '100 mB, the price is in coins per 100 mB, with the usual order book, fee and resting orders. The market itself never buys or sells '
         'UU-matter, so the board cannot turn energy into coins.\n'
         '- **Scrap sink**: with "Rest to scrap" on the sell or settlement page, what an instant sale leaves goes to the recycler when the '
         'market pays almost nothing for it (the next bid below 5 % of the price or below 1 coin): scrap at the IC2 recycler\'s rate, 1 for '
         '8 items. The coins of the sale stay the same.\n'
         '- **Stabiliser** (off unless the server turns it on): when the market creates more coins in 7 days than its target, the sell '
         'depths and the daily allowance shrink a little each day; small sales keep nearly their price. The Treasury tab shows its state.\n'
         '- **ID card**: use a blank card to make it your personal card; using it anywhere opens your account (mailbox, orders, auction '
         'orders, cancel; refunds go to the mailbox) and the delegate cards you wrote at your Trade Terminals.\n'),
    '_zh_cn/': ('关闭界面时容器会退回。AE2 为可选联动。\n',
                '- **UU 物质交易板**：玩家在“UU 物质（每份 100 mB）”市场用账户中的 UU 物质与货币交易：一份为 100 mB，价格为每 100 mB 的货币，'
                '使用普通的订单簿、手续费与挂单。市场本身从不买卖 UU 物质，因此交易板不会把能量变成货币。\n'
                '- **废料回收**：在出售页或大宗结算页开启“其余回收为废料”后，立即出售剩下的物品若市场几乎不再收购（下一件出价低于价格的 5% '
                '或低于 1 货币），会改为回收：按 IC2 回收机的比率，8 件得 1 个废料。出售所得的货币不变。\n'
                '- **稳定器**（服务器开启后才生效）：当市场在 7 天内发行的货币超过目标时，卖出深度与每日额度每天略微缩小；少量出售几乎保持原价。'
                '“国库”页显示其状态。\n'
                '- **ID 卡**：使用空白卡使其成为你的个人卡；在任何地方使用它都会打开你的账户（邮箱、挂单、拍卖订单、撤回，退款进入邮箱），'
                '以及你在交易终端写入的委任卡。\n'),
    '_ja_jp/': ('画面を閉じるとコンテナは戻る。AE2 は任意連携。\n',
                '- **UU 物質の板**: 「UU 物質（100 mB 単位）」の市場で、プレイヤー同士が口座の UU 物質と硬貨を交換する。1 単位は 100 mB、'
                '価格は 100 mB あたりの硬貨で、通常の板・手数料・指値が使える。市場自身は UU 物質を売買しないので、エネルギーが硬貨に化けることはない。\n'
                '- **スクラップの受け皿**: 売却ページか一括決済ページで「残りをスクラップ」をオンにすると、即時売却で残った品に市場が'
                'ほとんど払わないとき（次の買値が価格の 5% 未満か 1 硬貨未満）、残りをリサイクルに回す。IC2 リサイクラーと同じ率で、8 個で'
                'スクラップ 1 個。売却で得る硬貨は変わらない。\n'
                '- **安定化**（サーバーで有効にしたときだけ）: 市場が 7 日間に発行する硬貨が目標を超えると、売りの深度と 1 日の上限が毎日少しずつ'
                '縮む。少量の売却はほぼ元の価格のまま。「国庫」タブに状態が出る。\n'
                '- **ID カード**: 白紙のカードを使うと本人カードになる。どこで使っても口座（メールボックス、注文、一括清算の注文、取り消し。'
                '払い戻しはメールボックスへ）と、取引端末で書いた委任カードの一覧が開く。\n'),
}
TERMINAL = {
    '': ('- **Breaking** the terminal sends the buffers to the owner\'s mailbox; items with names, enchantments or damage drop.\n',
         '- **Scrap mode**: a sell order in the mode "Scrap rest" sells at once like an instant order and sends what the market no longer '
         'pays for to the scrap sink; the scrap goes to the output buffer (output mode) or the owner\'s mailbox.\n'
         '- **ID cards**: on the Cards page the owner writes a blank ID card for a player, with a level (view, trade or configure) and up to '
         '30 days. The card gives that player the level at all the owner\'s terminals; it works only for that player, and the owner revokes '
         'it here or with the personal card.\n'),
    '_zh_cn/': ('- **拆除**终端时，缓冲区中的物品进入所有者的邮箱；带名称、附魔或损耗的物品会掉落。\n',
                '- **回收模式**：“其余回收”模式的出售订单像立即出售一样成交，并把市场不再收购的部分交给废料回收；废料进入输出缓冲区'
                '（输出模式）或所有者的邮箱。\n'
                '- **ID 卡**：所有者在“ID 卡”页为某位玩家写入一张空白 ID 卡，设定权限（查看、交易或设置）与最多 30 天的期限。'
                '该卡让这位玩家在所有者的所有终端拥有此权限；它只对该玩家有效，所有者可在此处或用个人卡吊销。\n'),
    '_ja_jp/': ('- 端末を**壊す**と、バッファの中身は所有者のメールボックスへ送られる。名前・エンチャント・損耗のある品はその場に落ちる。\n',
                '- **スクラップモード**: 「残りスクラップ」の売り注文は即時売却と同じように売り、市場がもう払わない分をスクラップの受け皿へ'
                '回す。スクラップは出力バッファ（出力モード）か所有者のメールボックスへ入る。\n'
                '- **ID カード**: 所有者は「カード」ページで、白紙の ID カードをプレイヤー用に書き込む（権限は閲覧・取引・設定、期限は最大 30 日）。'
                'そのプレイヤーは所有者のすべての端末でその権限を持つ。カードはそのプレイヤーにしか使えず、所有者はここか本人カードで無効にできる。\n'),
}
for pages, page in ((FUTURES, 'machines/producer_block_future_elc.md'), (TERMINAL, 'machines/producer_block_trade_terminal.md')):
    for prefix, (anchor, lines) in pages.items():
        path = os.path.join(RES, GUIDE + prefix + page)
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        s = raw.decode('utf-8').replace('\r\n', '\n')
        if lines in s:
            continue
        if CHECK:
            missing.append(path)
            continue
        assert s.count(anchor) == 1, (path, anchor)
        s = s.replace(anchor, anchor + lines)
        open(path, 'wb').write(s.replace('\n', eol).encode('utf-8'))

if CHECK:
    print('missing or different:', missing)
    sys.exit(1 if missing else 0)
print('data_1128 written')
