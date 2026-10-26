#!/usr/bin/env python3
"""0.1.10.22 language keys (en_us / zh_cn / ja_jp): grid G3 (dispatch mode), G4 (load priority), G5 (breaker and fuse
cables, their settings screen) and G7 (the monitoring panel of the energy management terminal).
Idempotent; keeps the line endings of each file. With --check it only reports missing or different keys.
Usage: lang_1122.py <repo root> [--check]
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

P = 'gui.mio_icif.protection.'
G = 'gui.mio_icif.grid_panel.'
CFG = 'mio_icif.configuration.'

KEYS = {
    'block.mio_icif.wiring.block_eu_breaker_cable': ('EU Breaker Cable', 'EU断路器导线', 'EU ブレーカーケーブル'),
    'block.mio_icif.wiring.block_eu_fuse_cable': ('EU Fuse Cable', 'EU保险丝导线', 'EU ヒューズケーブル'),

    # protection settings screen
    P + 'title': ('Protection Settings', '保护设置', '保護の設定'),
    P + 'apply': ('Apply', '应用', '適用'),
    P + 'reset': ('Reset', '复位', 'リセット'),
    P + 'voltage_off': ('Off', '关闭', 'なし'),
    P + 'reclose_on': ('Auto: on', '自动：开', '自動：オン'),
    P + 'reclose_off': ('Auto: off', '自动：关', '自動：オフ'),
    P + 'state.closed': ('Closed', '闭合', '投入'),
    P + 'state.tripped': ('Tripped', '跳闸', 'トリップ'),
    P + 'state.locked_out': ('Locked out', '闭锁', 'ロックアウト'),
    P + 'state.blown': ('Blown', '熔断', '溶断'),
    P + 'recloses': ('recloses in %s s', '%s 秒后重合', '%s 秒後に再投入'),
    P + 'cause.none': ('none', '无', 'なし'),
    P + 'cause.overload': ('overload', '过载', '過負荷'),
    P + 'cause.short_circuit': ('short circuit', '短路', '短絡'),
    P + 'cause.overvoltage': ('overvoltage', '过电压', '過電圧'),
    P + 'last': ('Last trip: %s', '上次跳闸：%s', '前回のトリップ：%s'),
    P + 'trips': ('%s in a row', '连续 %s 次', '%s 回連続'),
    P + 'flow': ('Rated flow (EU/t)', '额定流量（EU/t）', '定格流量（EU/t）'),
    P + 'voltage': ('Rated voltage', '额定电压', '定格電圧'),
    P + 'delay': ('Delay (ticks, %s s)', '延时（tick，%s 秒）', '遅延（tick、%s 秒）'),
    P + 'reclose': ('Automatic reclosing', '自动重合闸', '自動再閉路'),
    P + 'help': ('Opens after the delay at twice the rated flow, sooner for larger overloads, at once at ten times '
                 'or above the rated voltage. Closes again after 3 s, at most three times in a row. Redstone holds it open.',
                 '2 倍额定流量时经过延时断开，过载越大越快；10 倍流量或超过额定电压时立即断开。3 秒后自动重合，最多连续 3 次。红石信号使其保持断开。',
                 '定格の 2 倍の流量で遅延後に開き、過負荷が大きいほど早い。10 倍や定格電圧超えでは即座に開く。3 秒後に再投入（連続 3 回まで）。レッドストーン信号で開いたままにできる。'),
    P + 'help_fuse': ('Blows after the delay at twice the rated flow, at once at ten times or above the rated voltage. '
                      'Right-click a blown fuse with a new fuse cable to restore it.',
                      '2 倍额定流量时经过延时熔断，10 倍流量或超过额定电压时立即熔断。用新的保险丝导线右击熔断的保险丝即可恢复。',
                      '定格の 2 倍の流量で遅延後に切れ、10 倍や定格電圧超えでは即座に切れる。切れたヒューズを新しいヒューズケーブルで右クリックすると元に戻る。'),

    # monitoring panel
    G + 'open': ('Panel', '面板', 'パネル'),
    G + 'title': ('Grid Monitoring Panel', '电网监控面板', '系統監視パネル'),
    G + 'tab.0': ('Overview', '概览', '概要'),
    G + 'tab.1': ('Diagram', '系统图', '系統図'),
    G + 'tab.2': ('Devices', '设备', '機器'),
    G + 'tab.3': ('Protection', '保护', '保護'),
    G + 'compact': ('Compact', '紧凑', 'コンパクト'),
    G + 'dispatch.0': ('Dispatch: merit order', '调度：经济次序', '給電：メリットオーダー'),
    G + 'dispatch.1': ('Dispatch: economic', '调度：经济调度', '給電：経済給電'),
    G + 'dispatch.2': ('Dispatch: rotation', '调度：轮流', '給電：巡回'),
    G + 'generation': ('Generation', '发电', '発電'),
    G + 'of_capacity': ('of %s nameplate', '铭牌 %s', '定格 %s 中'),
    G + 'consumption': ('Consumption', '耗电', '消費'),
    G + 'of_demand': ('of %s demand', '需求 %s', '需要 %s 中'),
    G + 'storage': ('Storage', '储能', '蓄電'),
    G + 'balance': ('Balance', '收支', '収支'),
    G + 'storage_flow': ('storage in %s, out %s', '储能 入 %s 出 %s', '蓄電 入 %s・出 %s'),
    G + 'trend': ('Trend 112 s', '112 秒趋势', '112 秒の推移'),
    G + 'trend_help': ('Green: generation; orange: consumption; dotted: storage charge', '绿：发电；橙：耗电；点线：储能电量',
                       '緑：発電、橙：消費、点線：蓄電量'),
    G + 'merit': ('Supply by source class', '按电源类别的供电', '電源種別ごとの供給'),
    G + 'source.0': ('Must-run', '必须运行', '必ず使う電源'),
    G + 'source.1': ('Fuel', '燃料', '燃料'),
    G + 'source.2': ('Storage', '储能', '蓄電'),
    G + 'critical': ('Critical loads', '重要负荷', '重要な負荷'),
    G + 'critical_demand': ('Demand %s', '需求 %s', '需要 %s'),
    G + 'critical_short': ('Short by %s!', '缺少 %s！', '%s 不足！'),
    G + 'critical_ok': ('Fully supplied', '全部满足', 'すべて供給'),
    G + 'cost': ('Engine time', '引擎耗时', 'エンジン時間'),
    G + 'cost_value': ('%s µs per tick', '每 tick %s µs', '1 tick あたり %s µs'),
    G + 'cost_help': ('Time the energy engine spent on the windows of this network\'s sources (112 s)',
                      '能源引擎在本网络电源的窗口上花费的时间（112 秒）', 'エネルギーエンジンがこのネットワークの電源のウィンドウに使った時間（112 秒）'),
    G + 'subnets': ('Sub-networks: %s · %s cables', '子网：%s · 导线 %s', 'サブネット：%s・導線 %s'),
    G + 'no_network': ('No network: place the terminal next to a cable.', '没有网络：请把终端放在导线旁。', 'ネットワークなし：端末を導線の隣に置いてください。'),
    G + 'bus_tip': ('Bus #%s: %s cables, %s devices · generation %s, consumption %s, demand %s · load %s',
                    '母线 #%s：导线 %s，设备 %s · 发电 %s，耗电 %s，需求 %s · 负载率 %s',
                    '母線 #%s：導線 %s、機器 %s・発電 %s、消費 %s、需要 %s・負荷率 %s'),
    G + 'legend': ('One bus per sub-network (rows by cable rating) · T transformer · S storage · X open breaker · squares: breakers',
                   '每个子网一条母线（按导线额定分行）· T 变压器 · S 储能 · X 断开的断路器 · 方块：断路器',
                   'サブネットごとに母線 1 本（導線の定格で行分け）・T 変圧器・S 蓄電・X 開いたブレーカー・四角：ブレーカー'),
    G + 'filter.all': ('All', '全部', 'すべて'),
    G + 'filter.generators': ('Generators', '发电机', '発電機'),
    G + 'filter.consumers': ('Consumers', '用电器', '消費機器'),
    G + 'filter.storage': ('Storage', '储能', '蓄電'),
    G + 'filter.protection': ('Protection', '保护', '保護'),
    G + 'col.device': ('Device', '设备', '機器'),
    G + 'col.priority': ('Priority / class', '优先级 / 类别', '優先度 / 種別'),
    G + 'col.flow': ('Flow', '功率', '電力'),
    G + 'col.rated': ('Rated / charge', '额定 / 电量', '定格 / 蓄電量'),
    G + 'col.voltage': ('Voltage', '电压', '電圧'),
    G + 'priority.0': ('Critical', '重要', '重要'),
    G + 'priority.1': ('Normal', '普通', '通常'),
    G + 'priority.2': ('Deferrable', '可延后', '後回し'),
    G + 'class.0': ('Must-run', '必须运行', '必ず使う'),
    G + 'class.1': ('Fuel', '燃料', '燃料'),
    G + 'off': ('OFF', '关', 'OFF'),
    G + 'on': ('ON', '开', 'ON'),
    G + 'breakers': ('Breakers and fuses: %s', '断路器与保险丝：%s', 'ブレーカーとヒューズ：%s'),
    G + 'settings': ('%s of %s · voltage %s · delay %s s', '%s / %s · 电压 %s · 延时 %s 秒', '%s / %s・電圧 %s・遅延 %s 秒'),
    G + 'breaker_tip': ('Trips in a row: %s · last cause: %s · %s', '连续跳闸：%s · 上次原因：%s · %s', '連続トリップ：%s・前回の原因：%s・%s'),
    G + 'no_breakers': ('No breaker or fuse cable on this network.', '本网络没有断路器或保险丝导线。', 'このネットワークにブレーカーやヒューズのケーブルはありません。'),
    G + 'alarms': ('Alarms: %s', '警报：%s', '警報：%s'),
    G + 'alarm.trip': ('Breaker tripped', '断路器跳闸', 'ブレーカーがトリップ'),
    G + 'alarm.reclose': ('Breaker reclosed', '断路器重合', 'ブレーカーが再投入'),
    G + 'alarm.lockout': ('Breaker locked out', '断路器闭锁', 'ブレーカーがロックアウト'),
    G + 'alarm.reset': ('Protection reset', '保护已复位', '保護をリセット'),
    G + 'alarm.fuse': ('Fuse blown', '保险丝熔断', 'ヒューズが溶断'),
    G + 'alarm.melt': ('Cables melted', '导线熔毁', '導線が溶断'),
    G + 'alarm.insulation': ('Insulation burned', '绝缘烧毁', '絶縁が焼損'),
    G + 'alarm.explosion': ('Machine exploded', '机器爆炸', '機械が爆発'),
    G + 'alarm.shortfall': ('Critical load short of energy', '重要负荷电力不足', '重要な負荷の電力不足'),
    G + 'ago': ('%s ago', '%s 前', '%s 前'),
    G + 'count': ('%s blocks', '%s 个方块', '%s ブロック'),
    G + 'no_alarms': ('No alarm.', '没有警报。', '警報はありません。'),

    # config
    CFG + 'EnergyNet.dispatchMode': ('Dispatch Order of the Sources', '电源调度次序', '電源の給電順'),
    CFG + 'dispatchMode': ('Dispatch Order of the Sources', '电源调度次序', '電源の給電順'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 1, 'ja_jp': 2}
    bad = 0
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)
        if CHECK:
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
