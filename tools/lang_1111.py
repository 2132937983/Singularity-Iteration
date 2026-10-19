#!/usr/bin/env python3
"""0.1.10.11 language keys (en_us / zh_cn / ja_jp): Creative Generator, chunk loader server rules
(screen, command, config), the Ultra-High Energy Blast sound, two coolant injector lines in Japanese.
Idempotent; keeps the line endings of each file.
Usage: lang_1111.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

CG = 'gui.mio_icif.creative_generator.'
CL = 'gui.mio_icif.chunk_loader.'
CMD = 'command.mio_icif.chunkloaders.'
CFG = 'mio_icif.configuration.chunk_loaders'

KEYS = {
    # ---------------------------------------------------------------- 3. Creative Generator
    'block.mio_icif.generator.block_creative_generator': ('Creative Generator', '创造模式发电机', 'クリエイティブ発電機'),
    'tooltip.mio_icif.creative_generator.line1': (
        'Debug source: any output, no voltage limit, no overload',
        '调试电源：任意输出，无电压限制，不会过载',
        'デバッグ用電源：任意の出力、電圧制限なし、過負荷なし'),
    'tooltip.mio_icif.creative_generator.line2': (
        'Creative mode only. Right-click to set the output.',
        '仅限创造模式。右键设定输出。',
        'クリエイティブモード専用。右クリックで出力を設定。'),
    'tooltip.mio_icif.creative_generator.line3': (
        'Cannot be mined in survival; drops nothing.',
        '生存模式下无法挖掘；不掉落任何物品。',
        'サバイバルでは採掘できず、何もドロップしない。'),
    'message.mio_icif.creative_generator.denied': (
        'Only a creative player or an operator can use the Creative Generator.',
        '只有创造模式玩家或管理员可以使用创造模式发电机。',
        'クリエイティブ発電機を使えるのは、クリエイティブモードのプレイヤーかオペレーターだけです。'),
    CG + 'field': ('Output (EU/t)', '输出 (EU/t)', '出力 (EU/t)'),
    CG + 'set': ('Set', '设定', '設定'),
    CG + 'on': ('ON', '开', 'ON'),
    CG + 'off': ('OFF', '关', 'OFF'),
    CG + 'output': ('Output: %s EU/t', '输出：%s EU/t', '出力: %s EU/t'),
    CG + 'sent': ('Sent last tick: %s EU', '上一 tick 送出：%s EU', '直前の tick の送電: %s EU'),
    CG + 'hint': ('0 - 1,000,000,000,000 EU/t (k, M, G, T)', '0 - 1,000,000,000,000 EU/t（可用 k、M、G、T）',
                  '0 - 1,000,000,000,000 EU/t（k・M・G・T も可）'),
    CG + 'invalid': ('Enter a number from 0 to 1,000,000,000,000', '请输入 0 至 1,000,000,000,000 之间的数',
                     '0〜1,000,000,000,000 の数を入力してください'),
    CG + 'no_limits': ('No voltage limit, no overload', '无电压限制，不会过载', '電圧制限なし・過負荷なし'),

    # ---------------------------------------------------------------- 5. chunk loader: screen
    CL + 'status.starting': ('Starting...', '启动中…', '起動中…'),
    CL + 'status.active': ('Active: %1$s chunks', '工作中：%1$s 个区块', '稼働中: %1$s チャンク'),
    CL + 'status.limited': ('Limit: %1$s of %2$s chunks loaded', '受限：已加载 %1$s / %2$s 个区块',
                            '上限: %2$s チャンク中 %1$s を読み込み'),
    CL + 'status.no_energy': ('No energy', '电量不足', 'エネルギー不足'),
    CL + 'status.empty': ('No chunk selected', '未选择区块', 'チャンク未選択'),
    CL + 'status.config_off': ('Chunk loaders are off on this server', '本服务器已关闭区块加载器',
                               'このサーバーではチャンクローダーが無効です'),
    CL + 'status.admin_off': ('Disabled by an administrator', '已被管理员停用', '管理者により停止中'),
    CL + 'status.owner_offline': ('Owner offline', '所有者离线', '所有者がオフライン'),
    CL + 'status.chunk_limit': ("Owner's chunk limit reached", '已达所有者的区块上限', '所有者のチャンク上限に到達'),
    CL + 'status.loader_limit': ("Owner's loader limit reached", '已达所有者的加载器上限', '所有者のローダー上限に到達'),
    CL + 'state.starting': ('starting', '启动中', '起動中'),
    CL + 'state.active': ('active', '工作中', '稼働中'),
    CL + 'state.limited': ('limited', '受限', '上限で一部'),
    CL + 'state.no_energy': ('no energy', '电量不足', 'エネルギー不足'),
    CL + 'state.empty': ('nothing selected', '未选择', '未選択'),
    CL + 'state.config_off': ('switched off', '已关闭', '無効'),
    CL + 'state.admin_off': ('admin lock', '管理员停用', '管理者停止'),
    CL + 'state.owner_offline': ('owner offline', '所有者离线', '所有者オフライン'),
    CL + 'state.chunk_limit': ('chunk limit', '区块上限', 'チャンク上限'),
    CL + 'state.loader_limit': ('loader limit', '加载器上限', 'ローダー上限'),
    CL + 'granted': ('Loaded %1$s of %2$s selected (max %3$s)', '已加载 %1$s / 已选 %2$s（上限 %3$s）',
                     '読み込み %1$s / 選択 %2$s（上限 %3$s）'),
    'message.mio_icif.chunk_loader.not_owner': (
        'Only the owner (%s) can change this chunk loader.',
        '只有所有者（%s）可以修改此区块加载器。',
        'このチャンクローダーを変更できるのは所有者（%s）だけです。'),

    # ---------------------------------------------------------------- 5. chunk loader: command
    CMD + 'header': ('SI chunk loaders: %1$s loaded, %2$s working, %3$s chunks held',
                     'SI 区块加载器：已载入 %1$s 个，工作中 %2$s 个，共保持 %3$s 个区块',
                     'SI チャンクローダー: 読み込み済み %1$s 台、稼働 %2$s 台、保持チャンク %3$s'),
    CMD + 'rules': ('Rules: switch %1$s, per loader %2$s, per player %3$s chunks / %4$s loaders, offline %5$s min, %6$s EU per chunk, random ticks %7$s',
                    '规则：开关 %1$s，每台 %2$s，每名玩家 %3$s 个区块 / %4$s 台，离线 %5$s 分钟，每区块 %6$s EU，随机刻 %7$s',
                    'ルール: スイッチ %1$s、1 台 %2$s、プレイヤーごと %3$s チャンク / %4$s 台、オフライン %5$s 分、チャンクあたり %6$s EU、ランダムティック %7$s'),
    CMD + 'more': ('... and %s more', '……另有 %s 台', '…ほか %s 台'),
    CMD + 'entry': ('%1$s - %2$s - %3$s/%4$s chunks - %5$s', '%1$s - %2$s - %3$s/%4$s 区块 - %5$s',
                    '%1$s - %2$s - %3$s/%4$s チャンク - %5$s'),
    CMD + 'no_owner': ('(no owner)', '（无所有者）', '（所有者なし）'),
    CMD + 'parked_header': ('Waiting to restart: %s', '等待恢复：%s 台', '再開待ち: %s 台'),
    CMD + 'parked': ('%1$s - %2$s', '%1$s - %2$s', '%1$s - %2$s'),
    CMD + 'reason.config': ('switched off', '开关关闭', 'スイッチ無効'),
    CMD + 'reason.admin': ('admin lock', '管理员停用', '管理者停止'),
    CMD + 'reason.offline': ('owner offline', '所有者离线', '所有者オフライン'),
    CMD + 'reason.limit': ('player limit', '玩家上限', 'プレイヤー上限'),
    CMD + 'teleport': ('Click to put a teleport command into the chat', '点击将传送命令填入聊天栏',
                       'クリックでテレポートコマンドをチャット欄に入力'),
    CMD + 'not_found': ('No SI chunk loader at %s', '%s 处没有 SI 区块加载器', '%s に SI チャンクローダーはありません'),
    CMD + 'disabled': ('Chunk loader at %s disabled', '已停用 %s 处的区块加载器', '%s のチャンクローダーを停止しました'),
    CMD + 'enabled': ('Chunk loader at %s enabled', '已启用 %s 处的区块加载器', '%s のチャンクローダーを再開しました'),
    CMD + 'disabled_all': ('%s chunk loaders disabled', '已停用 %s 台区块加载器', 'チャンクローダー %s 台を停止しました'),

    # ---------------------------------------------------------------- 5. chunk loader: config
    CFG: ('Chunk loaders', '区块加载器', 'チャンクローダー'),
    CFG + '.enabled': ('Chunk loaders work', '启用区块加载器', 'チャンクローダーを有効にする'),
    CFG + '.enabled.tooltip': ('Off: every SI chunk loader releases its chunks (the selections are kept).',
                               '关闭：所有 SI 区块加载器释放区块（保留选择）。',
                               'オフ: すべての SI チャンクローダーがチャンクを解放する（選択は残る）。'),
    CFG + '.maxChunksPerLoader': ('Chunks per loader', '每台加载器的区块数', 'ローダー 1 台のチャンク数'),
    CFG + '.maxChunksPerLoader.tooltip': ('1-25. Over the limit the loader keeps its own chunk first, then the nearest selected chunks.',
                                          '1-25。超出时优先保留自身区块，其次是最近的已选区块。',
                                          '1〜25。上限を超えると自分のチャンクを最優先し、次に近い選択チャンクから読み込む。'),
    CFG + '.maxChunksPerPlayer': ('Chunks per player', '每名玩家的区块数', 'プレイヤーごとのチャンク数'),
    CFG + '.maxChunksPerPlayer.tooltip': ('All loaders of one player together, in all dimensions. 0: no limit. First come, first served.',
                                          '同一玩家所有加载器合计（所有维度）。0：不限。先到先得。',
                                          '1 人のプレイヤーの全ローダー合計（全ディメンション）。0: 無制限。先着順。'),
    CFG + '.maxLoadersPerPlayer': ('Working loaders per player', '每名玩家同时工作的加载器数', 'プレイヤーごとの稼働ローダー数'),
    CFG + '.maxLoadersPerPlayer.tooltip': ('0: no limit.', '0：不限。', '0: 無制限。'),
    CFG + '.offlineOwnerMinutes': ('Minutes after the owner logs off', '所有者下线后的分钟数', '所有者ログアウト後の分数'),
    CFG + '.offlineOwnerMinutes.tooltip': ('How long a loader keeps working after its owner logged off. -1: always. 0: stop at once. Loaders placed before 0.1.10.11 have no owner.',
                                           '所有者下线后加载器继续工作的时间。-1：一直工作。0：立即停止。0.1.10.11 之前放置的加载器没有所有者。',
                                           '所有者のログアウト後にローダーが動き続ける時間。-1: 常に。0: すぐ止める。0.1.10.11 より前に置いたローダーには所有者がいない。'),
    CFG + '.euPerChunkPerTick': ('EU per chunk and tick', '每区块每 tick 的 EU', 'チャンク・tick あたりの EU'),
    CFG + '.euPerChunkPerTick.tooltip': ('Energy cost of one loaded chunk per tick.', '每个已加载区块每 tick 的能耗。',
                                         '読み込んだチャンク 1 つの 1 tick あたりの消費 EU。'),
    CFG + '.randomTicks': ('Random ticks in loaded chunks', '已加载区块的随机刻', '読み込みチャンクのランダムティック'),
    CFG + '.randomTicks.tooltip': ('On: random ticks and natural spawning as if a player stood there. Off: machines and entities keep running, no random ticks (crops, leaves, fire) and no natural spawning: much cheaper.',
                                   '开：像有玩家在场一样进行随机刻和自然生成。关：机器和实体照常运行，但没有随机刻（作物、树叶、火）和自然生成：开销小得多。',
                                   'オン: プレイヤーがいるのと同じくランダムティックと自然スポーンを行う。オフ: 機械とエンティティは動くが、ランダムティック（作物・葉・火）と自然スポーンはない。負荷がずっと軽い。'),
    CFG + '.ownerOnlyEdit': ('Only the owner edits', '仅所有者可修改', '所有者だけが変更可'),
    CFG + '.ownerOnlyEdit.tooltip': ('On: only the owner and operators can change the chunk selection.',
                                     '开：仅所有者和管理员可修改区块选择。',
                                     'オン: チャンクの選択を変えられるのは所有者とオペレーターだけ。'),

    # ---------------------------------------------------------------- 10. Ultra-High Energy Blast
    'mio_icif.subtitle.weapon.laser.ultra_blast': ('Ultra-high energy blast', '超高能爆破', '超高エネルギー爆破'),

    # ---------------------------------------------------------------- 2. coolant injectors (Japanese was missing)
    'gui.mio_icif.lapis_injector.lapis_blocks': ('Lapis Blocks: %s', '青金石块: %s', 'ラピスラズリブロック: %s'),
    'gui.mio_icif.redstone_injector.redstone_blocks': ('Redstone Blocks: %s', '红石块: %s', 'レッドストーンブロック: %s'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        for key, t in KEYS.items():
            data[key] = t[i]
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(KEYS))


if __name__ == '__main__':
    main()
