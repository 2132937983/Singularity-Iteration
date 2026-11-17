#!/usr/bin/env python3
"""0.1.11.30 language values (en_us / zh_cn / ja_jp / ru_ru), cable audit W1: the cable item tooltip (largest packet, line
loss, shock), the meter HUD's own-cable and cut-line lines, "packets up to" instead of "max EU/t", the Jade tier text,
/mio_icif grid and the JEI information page of the cables.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11530.py <repo root> [--check]
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


KEYS = {
    'tooltip.mio_icif.cable.packet': v('Largest packet %s EU (%s) · no flow limit', '单包上限 %s EU（%s）· 通量不限',
        '最大パケット %s EU（%s）· 流量の上限なし', 'Наибольший пакет %s EU (%s) · поток не ограничен'),
    'tooltip.mio_icif.cable.any_packet': v('Packets of any size · no flow limit', '任意大小的包 · 通量不限',
        'どの大きさのパケットも可 · 流量の上限なし', 'Пакеты любого размера · поток не ограничен'),
    'tooltip.mio_icif.cable.lossless': v('No line loss', '无线路损耗', '線路損失なし', 'Без потерь в линии'),
    'tooltip.mio_icif.cable.loss': v('Loses %s EU per block per packet', '每格每包损耗 %s EU', '1 ブロック・1 パケットごとに %s EU の損失',
        'Потери %s EU на блок на пакет'),
    'tooltip.mio_icif.cable.no_shock': v('Does not shock', '不会电击', '感電しない', 'Не бьёт током'),
    'tooltip.mio_icif.cable.insulated_full': v('Fully insulated (layers: %s): does not shock', '已完全绝缘（%s 层）：不会电击',
        '完全に絶縁（%s 層）：感電しない', 'Полная изоляция (слоёв: %s): не бьёт током'),
    'tooltip.mio_icif.cable.shock': v('Shocks within 1.5 blocks on packets above %s EU · insulation layers for no shock: %s',
        '包超过 %s EU 时电击 1.5 格内的生物 · 完全绝缘需要 %s 层',
        '%s EU を超えるパケットで 1.5 ブロック以内に感電 · 完全絶縁に必要な層：%s',
        'Бьёт током в радиусе 1,5 блока при пакетах выше %s EU · слоёв для полной изоляции: %s'),
    'hud.mio_icif.voltage.here': v('This cable: %s EU/t · largest packet %s EU', '本根导线：%s EU/t · 最大包 %s EU',
        'このケーブル：%s EU/t · 最大パケット %s EU', 'Этот кабель: %s EU/t · наибольший пакет %s EU'),
    'hud.mio_icif.voltage.idle': v('%s cables in chunks that do not tick: the line is cut there', '%s 根导线在不 tick 的区块里：线路在那里断开',
        'tick されないチャンクに %s 本のケーブル：線路はそこで切れる', '%s кабелей в неактивных чанках: линия там разорвана'),
    'hud.mio_icif.voltage.rated': v('Rated %s · packets up to %s EU', '额定 %s · 单包上限 %s EU', '定格 %s · 最大パケット %s EU',
        'Номинал %s · пакеты до %s EU'),
    'hud.mio_icif.voltage.cable': v('Cable %s · packets up to %s EU', '电缆 %s · 单包上限 %s EU', '電線 %s · 最大パケット %s EU',
        'Кабель %s · пакеты до %s EU'),
    'jade.mio_icif.tier_packet': v('(packets up to %s EU)', '（单包上限 %s EU）', '（最大パケット %s EU）', '(пакеты до %s EU)'),
    'command.mio_icif.grid.header': v('Energy grid: %s rebuilds, %s isolated failures, %s dimensions recovering, %s chunks in quarantine · last reason: %s',
        '电网：重建 %s 次，隔离故障 %s 次，%s 个维度正在恢复，%s 个区块被隔离 · 最近原因：%s',
        '送電網：再構築 %s 回、隔離された障害 %s 回、復旧中の次元 %s、隔離中のチャンク %s · 最後の理由：%s',
        'Энергосеть: перестроений %s, изолированных сбоев %s, восстанавливается измерений %s, чанков в карантине %s · последняя причина: %s'),
    'command.mio_icif.grid.level': v('%s: %s cables, %s machines, %s FE ports · %s chunks with cables, %s not ticking (cut there) · %s passes stopped by the time cap · %s EU dropped at FE ports',
        '%s：导线 %s，机器 %s，FE 接口 %s · 有导线的区块 %s 个，其中 %s 个不 tick（线路在那里断开）· 时间上限截停 %s 次 · FE 接口丢弃 %s EU',
        '%s：ケーブル %s、機械 %s、FE ポート %s · ケーブルのあるチャンク %s、うち tick されない %s（そこで切れる）· 時間上限で止めたパス %s · FE ポートで失った %s EU',
        '%s: кабелей %s, машин %s, FE-портов %s · чанков с кабелями %s, неактивных %s (там разрыв) · проходов остановлено лимитом времени %s · потеряно на FE-портах %s EU'),
    'command.mio_icif.grid.empty': v('No dimension has an energy grid.', '没有任何维度有电网。', '送電網のある次元はありません。',
        'Ни в одном измерении нет энергосети.'),
    'jei.mio_icif.cable_info.rating': v('The rating of a cable is its largest packet, not a flow limit: a cable carries any number of packets up to it each tick. A larger packet melts the cable; a machine explodes on a packet above its input tier. Use transformers between tiers.',
        '导线的额定值是单包上限，不是通量上限：每 tick 可以送任意多个不超过上限的包。更大的包会熔毁导线；超过机器输入等级的包会让机器爆炸。不同等级之间用变压器。',
        'ケーブルの定格は最大パケットで、流量の上限ではない：上限以下のパケットを 1 tick にいくつでも運ぶ。それより大きいパケットはケーブルを溶かし、入力 Tier を超えるパケットは機械を爆発させる。Tier の間には変圧器を使う。',
        'Номинал кабеля — наибольший пакет, а не предел потока: за тик кабель несёт сколько угодно пакетов до него. Больший пакет плавит кабель; машина взрывается от пакета выше своего уровня входа. Между уровнями ставьте трансформаторы.'),
    'jei.mio_icif.cable_info.loss': v('No line loss by default. With the server option lineLossModel = proportional a packet loses the tooltip\'s amount per block.',
        '默认没有线路损耗。服务器选项 lineLossModel = proportional 打开后，每个包每格损失提示中的量。',
        '既定では線路損失なし。サーバー設定 lineLossModel = proportional では、パケットは 1 ブロックごとに説明の量を失う。',
        'По умолчанию потерь в линии нет. С параметром сервера lineLossModel = proportional пакет теряет указанное в подсказке на каждый блок.'),
    'jei.mio_icif.cable_info.shock': v('A bare or partly insulated cable shocks living things within 1.5 blocks while a packet is above what its insulation absorbs. Rubber adds a layer, the Cable Cutter takes one off; neither works through foam.',
        '裸线或绝缘不足的导线在包超过绝缘吸收值时，电击 1.5 格内的生物。橡胶加一层绝缘，剪线钳去掉一层；隔着泡沫都不行。',
        '裸または絶縁の足りないケーブルは、パケットが絶縁の吸収量を超える間、1.5 ブロック以内の生物を感電させる。ゴムで 1 層増え、ケーブルカッターで 1 層外れる。フォーム越しにはできない。',
        'Голый или недоизолированный кабель бьёт током существ в радиусе 1,5 блока, пока пакет выше поглощения изоляции. Резина добавляет слой, кусачки снимают; сквозь пену нельзя.'),
    'jei.mio_icif.cable_info.colour': v('Dyed insulated cables connect only to the same colour; an undyed cable connects to every colour.',
        '染色的绝缘导线只连接同色导线；未染色的导线和任何颜色都连接。',
        '染めた絶縁ケーブルは同じ色とだけつながり、染めていないケーブルはどの色ともつながる。',
        'Окрашенные изолированные кабели соединяются только с тем же цветом; неокрашенный — с любым.'),
    'jei.mio_icif.cable_info.chunks': v('A line is cut where it crosses a chunk that does not tick; keep the whole route chunk-loaded. The EU Meter on a cable shows the EU through that cable.',
        '线路经过不 tick 的区块时会在那里断开，整条线路要保持区块加载。用 EU 测量仪对准导线，可以看到通过这根导线的 EU。',
        'tick されないチャンクを通る線路はそこで切れる。経路全体をチャンク読み込みすること。EU メーターをケーブルに向けると、そのケーブルを通る EU が分かる。',
        'Линия разрывается там, где проходит неактивный чанк; держите весь маршрут загруженным. EU-метр на кабеле показывает EU через этот кабель.'),
}

REMOVE = []


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
            left = [k for k in REMOVE if k in data]
            bad += len(wrong) + len(left)
            print(lang, 'missing or different:', len(wrong), wrong[:5], 'still present:', len(left), left)
            continue
        removed = sum(1 for k in REMOVE if data.pop(k, None) is not None)
        for key, t in mine.items():
            data[key] = t
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', len(mine), 'removed:', removed)
    if CHECK and bad:
        sys.exit(1)


if __name__ == '__main__':
    main()
