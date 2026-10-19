#!/usr/bin/env python3
"""0.1.10.12 language keys (en_us / zh_cn / ja_jp): the reactor accident audit (blackbox item,
/mio_icif reactor audit, the report screen, causes, incident kinds, component kinds).
Idempotent; keeps the line endings of each file.
Usage: lang_1112.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

CMD = 'command.mio_icif.reactor_audit.'
GUI = 'gui.mio_icif.reactor_audit.'
CAUSE = 'reactor_audit.cause.'
KIND = 'reactor_audit.kind.'
TYPE = 'reactor_audit.type.'

KEYS = {
    # ---------------------------------------------------------------- item
    'item.mio_icif.reactor.item_reactor_blackbox': ('Reactor Blackbox', '反应堆黑匣子', '原子炉ブラックボックス'),
    'tooltip.mio_icif.reactor_blackbox.empty': (
        'Blank recorder. A failed reactor leaves a recorded one at the site.',
        '空白记录器。发生事故的反应堆会在现场留下记录了数据的黑匣子。',
        '未記録のレコーダー。事故を起こした原子炉は、記録入りのものを現場に残す。'),
    'tooltip.mio_icif.reactor_blackbox.incident': ('Incident #%s', '事故 #%s', '事故 #%s'),
    'tooltip.mio_icif.reactor_blackbox.use': ('Right-click: open the audit report', '右键：打开事故调查报告', '右クリック: 調査レポートを開く'),
    'message.mio_icif.reactor_audit.missing': (
        'This incident is no longer in the log.', '事故记录中已没有这起事故。', 'この事故は記録から削除されています。'),

    # ---------------------------------------------------------------- command
    CMD + 'none': ('No reactor incidents recorded.', '没有反应堆事故记录。', '原子炉の事故記録はありません。'),
    CMD + 'header': ('Reactor incidents (%s), newest first. Click a line to open its report:',
                     '反应堆事故（%s 起），按时间从新到旧。点击一行打开报告：',
                     '原子炉の事故（%s 件、新しい順）。行をクリックするとレポートを開く:'),
    CMD + 'more': ('... and %s more', '……还有 %s 起', '…ほか %s 件'),
    CMD + 'entry': ('#%1$s  %2$s  %3$s  [%4$s]  %5$s', '#%1$s  %2$s  %3$s  [%4$s]  %5$s', '#%1$s  %2$s  %3$s  [%4$s]  %5$s'),
    CMD + 'open': ('Click to open the audit report', '点击打开事故调查报告', 'クリックで調査レポートを開く'),
    CMD + 'not_found': ('No reactor incident #%s that you may view.', '没有你可查看的反应堆事故 #%s。', '閲覧できる原子炉事故 #%s はありません。'),
    CMD + 'console': ('Reactor incident #%s, main cause: %s', '反应堆事故 #%s，主因：%s', '原子炉事故 #%s、主因: %s'),
    CMD + 'cleared': ('Removed %s reactor incidents from the log.', '已从记录中删除 %s 起反应堆事故。', '原子炉事故 %s 件を記録から削除しました。'),

    # ---------------------------------------------------------------- incident kinds
    TYPE + 'nuclear': ('Nuclear explosion', '核爆炸', '核爆発'),
    TYPE + 'local': ('Reactor explosion', '反应堆爆炸', '原子炉爆発'),
    TYPE + 'contained': ('Meltdown (explosions off)', '熔毁（爆炸已关闭）', 'メルトダウン（爆発無効）'),
    TYPE + 'removed': ('Removed while hot', '高温时被拆除', '高温状態で撤去'),

    # ---------------------------------------------------------------- causes
    CAUSE + 'removed': ('Removed while running or hot', '运行中或高温时被拆除', '稼働中・高温状態での撤去'),
    CAUSE + 'removed.desc': (
        'The reactor was taken away while it was running or hot (hull heat %s%%). There was no explosion; this record only keeps its last state.',
        '反应堆在运行中或高温时（外壳热量 %s%%）被拆除。没有发生爆炸，本记录只保存它最后的状态。',
        '原子炉は稼働中または高温（炉熱 %s%%）のまま撤去された。爆発は起きていない。この記録は最後の状態を残すだけ。'),
    CAUSE + 'chunk_boundary': ('Reactor chamber in an unloaded chunk', '反应仓位于未加载的区块', '未読み込みチャンク内の原子炉チャンバー'),
    CAUSE + 'chunk_boundary.desc': (
        '%1$s side(s) of the reactor were in chunks that were not loaded. The reactor lost chambers (%2$s → %3$s columns) and the cooling in them. Keep the reactor and its chambers in one chunk, or keep those chunks loaded.',
        '反应堆有 %1$s 个面位于未加载的区块中。反应堆因此失去了反应仓（%2$s → %3$s 列）及其中的冷却。请把反应堆和反应仓放在同一区块内，或保持这些区块加载。',
        '原子炉の %1$s 面が読み込まれていないチャンクにあった。原子炉はチャンバー（%2$s → %3$s 列）とそこの冷却を失った。原子炉とチャンバーを同じチャンクに置くか、そのチャンクを読み込んだままにすること。'),
    CAUSE + 'chamber_lost': ('Reactor chamber lost', '反应仓丢失', '原子炉チャンバーの喪失'),
    CAUSE + 'chamber_lost.desc': (
        'A reactor chamber was removed or destroyed: the reactor went from %1$s to %2$s columns and dropped %3$s components. The heat the lost parts used to carry away stayed in the hull.',
        '有反应仓被拆除或摧毁：反应堆从 %1$s 列变为 %2$s 列，掉落了 %3$s 个元件。原本由这些元件带走的热量留在了外壳中。',
        '原子炉チャンバーが撤去または破壊された。原子炉は %1$s 列から %2$s 列になり、部品 %3$s 個を落とした。失われた部品が運び出していた熱は炉に残った。'),
    CAUSE + 'grid_blast': ('External overvoltage (machine explosion)', '外部过电压（机器爆炸）', '外部からの許容電圧超過（機械の爆発）'),
    CAUSE + 'grid_blast.desc': (
        'A machine next to the reactor exploded from overvoltage %1$s s before the accident (at %2$s %3$s %4$s). Check the voltage tier of the cables, transformers and machines around the reactor.',
        '事故前 %1$s 秒，反应堆旁的一台机器因过电压爆炸（位置 %2$s %3$s %4$s）。请检查反应堆周围电缆、变压器和机器的电压等级。',
        '事故の %1$s 秒前、原子炉の隣の機械が過電圧で爆発した（座標 %2$s %3$s %4$s）。原子炉周辺のケーブル・変圧器・機械の電圧ティアを確認すること。'),
    CAUSE + 'coolant_hot_full': ('Hot coolant tank full: heat removal stopped', '高温冷却液储罐已满：排热停止', '内部高温冷却液タンク満杯による排熱停止'),
    CAUSE + 'coolant_hot_full.desc': (
        'Fluid reactor: the hot coolant tank was full (%1$s / %2$s mB) and nothing took hot coolant out, so no heat could leave the core. Pump the hot coolant out, e.g. to heat exchangers.',
        '流体反应堆：高温冷却液储罐已满（%1$s / %2$s mB），且没有抽出高温冷却液，热量无法离开堆芯。请把高温冷却液抽出（例如送往热交换器）。',
        '流体炉: 高温冷却液タンクが満杯（%1$s / %2$s mB）で、高温冷却液が抜き取られず、炉心から熱が出られなかった。高温冷却液を（熱交換器などへ）送り出すこと。'),
    CAUSE + 'coolant_empty': ('No coolant', '冷却液耗尽', '冷却液切れ'),
    CAUSE + 'coolant_empty.desc': (
        'Fluid reactor: the cold coolant tank was empty (%s mB), so the core could not be cooled. Keep the coolant supply running.',
        '流体反应堆：低温冷却液储罐为空（%s mB），堆芯无法冷却。请保持冷却液供应。',
        '流体炉: 低温冷却液タンクが空（%s mB）で、炉心を冷やせなかった。冷却液の供給を絶やさないこと。'),
    CAUSE + 'liquid_cooling_lost': ('Liquid cooling stopped', '液体冷却停止', '液体冷却の停止'),
    CAUSE + 'liquid_cooling_lost.desc': (
        'The liquid cooling took away up to %1$s HU per cycle and then stopped (%2$s). The heat it used to remove went into the hull.',
        '液体冷却每周期最多带走 %1$s HU，随后停止（%2$s）。原本由它带走的热量进入了外壳。',
        '液体冷却は 1 サイクルあたり最大 %1$s HU を除去していたが、停止した（%2$s）。除去されていた熱は炉に溜まった。'),
    'reactor_audit.detail.empty': ('coolant used up', '冷却液用尽', '冷却液を使い切った'),
    'reactor_audit.detail.hot_full': ('hot coolant not taken out', '高温冷却液未被抽出', '高温冷却液が抜き取られなかった'),
    CAUSE + 'sudden': ('Sudden heat surge', '热量骤升', '急激な熱上昇'),
    CAUSE + 'sudden.desc': (
        'The hull heat went from %1$s%% to %2$s%% within one cycle (one second). Usually a component was taken out or the layout changed while the reactor ran.',
        '外壳热量在一个周期（1 秒）内从 %1$s%% 升至 %2$s%%。通常是运行中取出了元件或改动了布局。',
        '炉熱が 1 サイクル（1 秒）で %1$s%% から %2$s%% になった。多くは稼働中に部品を取り出したか、配置を変えたことによる。'),
    CAUSE + 'component_melted': ('Thermal runaway from melted cooling parts', '冷却元件熔毁导致热失控', '冷却部品の融解による熱暴走'),
    CAUSE + 'component_melted.desc': (
        '%1$s cooling part(s) melted (%2$s), the first %3$s s before the accident. Their heat went into the hull and the remaining parts could not keep up.',
        '%1$s 个冷却元件熔毁（%2$s），第一个在事故前 %3$s 秒。它们的热量进入外壳，剩下的元件无法跟上。',
        '冷却部品 %1$s 個が融解した（%2$s）。最初の融解は事故の %3$s 秒前。その熱が炉に入り、残った部品では追いつかなかった。'),
    CAUSE + 'heat_runaway': ('Thermal runaway: not enough cooling', '热失控：冷却不足', '熱暴走：冷却不足'),
    CAUSE + 'heat_runaway.desc': (
        'For the last %3$s s the core made %1$s HU per cycle, but only %2$s HU per cycle left it. The hull heated up steadily until it failed. Add vents or exchangers, or use fewer fuel rods.',
        '最后 %3$s 秒内，堆芯每周期产生 %1$s HU，但每周期只有 %2$s HU 被带走。外壳热量持续上升直至失效。请增加散热片或热交换器，或减少燃料棒。',
        '最後の %3$s 秒間、炉心は 1 サイクルあたり %1$s HU を出したが、除去できたのは %2$s HU だけだった。炉熱は上がり続けて限界に達した。ベントや熱交換器を増やすか、燃料棒を減らすこと。'),
    CAUSE + 'fuel_added': ('Fuel added while running', '运行中添加了燃料', '稼働中の燃料追加'),
    CAUSE + 'fuel_added.desc': (
        'The number of fuel rods rose from %1$s to %2$s during the recorded time.',
        '记录期间燃料棒数量从 %1$s 增加到 %2$s。',
        '記録期間中に燃料棒が %1$s 本から %2$s 本に増えた。'),
    CAUSE + 'feed_rate': ('High feed rate', '高进料倍率', '高い供給倍率'),
    CAUSE + 'feed_rate.desc': (
        'The feed rate was ×%1$s, which added %2$s HU of hull heat per cycle.',
        '进料倍率为 ×%1$s，每周期额外增加 %2$s HU 外壳热量。',
        '供給倍率が ×%1$s で、1 サイクルあたり炉熱が %2$s HU 余計に増えた。'),
    CAUSE + 'automation': ('Automation moved items', '自动化搬运了物品', '自動化によるアイテムの出し入れ'),
    CAUSE + 'automation.desc': (
        'Automation put in %1$s and took out %2$s items in the last %3$s s. A wrong item or a vent taken out can upset the heat balance.',
        '最后 %3$s 秒内，自动化放入了 %1$s 个、取出了 %2$s 个物品。放错物品或取走散热片都可能破坏热平衡。',
        '最後の %3$s 秒間に、自動化がアイテムを %1$s 個入れ、%2$s 個取り出した。誤ったアイテムやベントの取り出しは熱収支を崩す。'),
    CAUSE + 'component_saturation': ('Cooling parts saturated', '冷却元件饱和', '冷却部品の飽和'),
    CAUSE + 'component_saturation.desc': (
        'The heat-storing parts were %s%% full and had no room left to take heat.',
        '储热元件已满 %s%%，无法再吸收热量。',
        '蓄熱部品が %s%% まで埋まり、熱を受け取る余地がなかった。'),
    CAUSE + 'unknown': ('Cause not determined', '原因不明', '原因不明'),
    CAUSE + 'unknown.desc': (
        'The blackbox did not record enough cycles (for example, the reactor failed right after the world was loaded).',
        '黑匣子没有记录到足够的周期（例如反应堆在世界加载后立即出事）。',
        'ブラックボックスに十分なサイクルが記録されていない（例: ワールドの読み込み直後に事故が起きた）。'),

    # ---------------------------------------------------------------- component kinds
    KIND + 'fuel': ('fuel rod', '燃料棒', '燃料棒'),
    KIND + 'cell': ('coolant cell', '冷却单元', '冷却セル'),
    KIND + 'condensator': ('condensator', '冷凝模块', 'コンデンセーター'),
    KIND + 'vent': ('heat vent', '散热片', 'ヒートベント'),
    KIND + 'component_vent': ('component heat vent', '元件散热片', 'コンポーネントヒートベント'),
    KIND + 'exchanger': ('heat exchanger', '热交换器', '熱交換器'),
    KIND + 'reflector': ('neutron reflector', '中子反射板', '中性子反射板'),
    KIND + 'plating': ('reactor plating', '反应堆隔板', '原子炉プレート'),
    KIND + 'heatpack': ('heat pack', '加热元件', 'ヒートパック'),
    KIND + 'lithium': ('lithium cell', '锂单元', 'リチウムセル'),
    KIND + 'isotope': ('isotope rod', '同位素棒', '同位体棒'),
    KIND + 'inert': ('other item', '其他物品', 'その他のアイテム'),

    # ---------------------------------------------------------------- report screen
    GUI + 'title': ('Reactor Accident Report #%s', '反应堆事故调查报告 #%s', '原子炉事故調査レポート #%s'),
    GUI + 'where': ('%1$s · day %2$s · %3$s · %4$s', '%1$s · 第 %2$s 天 · %3$s · %4$s', '%1$s ・ %2$s 日目 ・ %3$s ・ %4$s'),
    GUI + 'people': ('Owner: %1$s · Nearby: %2$s', '所有者：%1$s · 附近玩家：%2$s', '所有者: %1$s ・ 近くにいたプレイヤー: %2$s'),
    GUI + 'unknown': ('unknown', '未知', '不明'),
    GUI + 'nobody': ('nobody', '无', 'なし'),
    GUI + 'main': ('Main cause: %s', '主因：%s', '主因: %s'),
    GUI + 'factors': ('Contributing factors: %s', '次要因素：%s', '副次的要因: %s'),
    GUI + 'separator': (', ', '、', '、'),
    GUI + 'layout': ('Core at the last cycle', '最后一个周期的堆芯', '最終サイクルの炉心'),
    GUI + 'graph': ('Hull heat (bars: heat in / out)', '外壳热量（柱：产热 / 散热）', '炉熱（棒: 発熱 / 除熱）'),
    GUI + 'no_data': ('No recorded cycles', '没有记录到周期', '記録されたサイクルなし'),
    GUI + 'metrics1': ('Hull %1$s%% · heat in %2$s / out %3$s HU per cycle · %4$s EU/t',
                       '外壳 %1$s%% · 每周期产热 %2$s / 散热 %3$s HU · %4$s EU/t',
                       '炉熱 %1$s%% ・ 1 サイクルの発熱 %2$s / 除熱 %3$s HU ・ %4$s EU/t'),
    GUI + 'metrics_fluid': ('Feed ×%1$s · %2$s columns · coolant cold %3$s / hot %4$s of %5$s mB',
                            '进料 ×%1$s · %2$s 列 · 冷却液 低温 %3$s / 高温 %4$s（容量 %5$s mB）',
                            '供給 ×%1$s ・ %2$s 列 ・ 冷却液 低温 %3$s / 高温 %4$s（容量 %5$s mB）'),
    GUI + 'metrics_standard': ('Feed ×%1$s · %2$s columns · liquid cooling %3$s cold / %4$s hot mB, %5$s HU removed',
                               '进料 ×%1$s · %2$s 列 · 液体冷却 低温 %3$s / 高温 %4$s mB，带走 %5$s HU',
                               '供給 ×%1$s ・ %2$s 列 ・ 液体冷却 低温 %3$s / 高温 %4$s mB、除熱 %5$s HU'),
    GUI + 'metrics3': ('Rods %1$s · cooling parts %2$s (%3$s%% full) · automation +%4$s / -%5$s',
                       '燃料棒 %1$s · 冷却元件 %2$s（%3$s%% 满）· 自动化 +%4$s / -%5$s',
                       '燃料棒 %1$s ・ 冷却部品 %2$s（%3$s%% 使用）・ 自動化 +%4$s / -%5$s'),
    GUI + 'slot_outside': ('No chamber: this column was not built', '无反应仓：该列未建造', 'チャンバーなし: この列はない'),
    GUI + 'slot_empty': ('Empty', '空', '空き'),
    GUI + 'slot_melted': ('Melted', '已熔毁', '融解'),
    GUI + 'slot_expired': ('Worn out', '已耗尽', '寿命切れ'),
    GUI + 'slot_when': ('%s s before the accident', '事故前 %s 秒', '事故の %s 秒前'),
    GUI + 'slot_fuel': ('Fuel: %1$s / %2$s cycles left', '燃料：剩余 %1$s / %2$s 周期', '燃料: 残り %1$s / %2$s サイクル'),
    GUI + 'slot_heat': ('Heat: %1$s / %2$s (%3$s%%)', '热量：%1$s / %2$s（%3$s%%）', '熱: %1$s / %2$s（%3$s%%）'),
    GUI + 'slot_wear': ('Wear: %1$s / %2$s', '损耗：%1$s / %2$s', '消耗: %1$s / %2$s'),
    GUI + 'slot_depleted': ('Depleted', '已枯竭', '枯渇'),
    GUI + 'cycle_time': ('%s s before the accident', '事故前 %s 秒', '事故の %s 秒前'),
    GUI + 'cycle_hull': ('Hull: %1$s / %2$s HU (%3$s%%)', '外壳：%1$s / %2$s HU（%3$s%%）', '炉熱: %1$s / %2$s HU（%3$s%%）'),
    GUI + 'cycle_heat': ('Heat in %1$s / out %2$s HU', '产热 %1$s / 散热 %2$s HU', '発熱 %1$s / 除熱 %2$s HU'),
    GUI + 'cycle_melted': ('%s part(s) melted', '%s 个元件熔毁', '部品 %s 個が融解'),
    GUI + 'cycle_automation': ('Automation: +%1$s / -%2$s items', '自动化：+%1$s / -%2$s 个物品', '自動化: +%1$s / -%2$s 個'),
    GUI + 'cycle_columns': ('Columns: %1$s → %2$s', '列数：%1$s → %2$s', '列数: %1$s → %2$s'),
    GUI + 'close': ('Close', '关闭', '閉じる'),
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
