#!/usr/bin/env python3
"""0.1.11.17 language values (en_us / zh_cn / ja_jp / ru_ru): the plant mimic screens of the Stirling generators, the
Stirling kinetic generator and the kinetic generators (shaft): labels, reading lines, states, tooltips.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11517.py <repo root> [--check]
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
    'gui.mio_icif.stirling.heat_in': v('heat', '热', '熱', 'тепло'),
    'gui.mio_icif.stirling.temps': v('Heater %s °C · cooler %s °C (%s) · Carnot %s · %s of design', '加热头 %s °C · 冷却端 %s °C（%s）· 卡诺 %s · 设计值的 %s',
        '加熱ヘッド %s °C · 冷却端 %s °C（%s）· カルノー %s · 設計比 %s', 'Нагреватель %s °C · холодильник %s °C (%s) · Карно %s · %s от расчёта'),
    'gui.mio_icif.stirling.water': v('water', '水冷', '水冷', 'вода'),
    'gui.mio_icif.stirling.air': v('air %s °C', '空气 %s °C', '空気 %s °C', 'воздух %s °C'),
    'gui.mio_icif.stirling.flow': v('Heat %s of %s HU/t rated → %s EU/t · %s EU/HU', '热流 %s / 额定 %s HU/t → %s EU/t · %s EU/HU',
        '熱流 %s / 定格 %s HU/t → %s EU/t · %s EU/HU', 'Тепло %s из %s HU/t → %s EU/t · %s EU/HU'),
    'gui.mio_icif.stirling.state.full': v('Stopped · storage full', '停机 · 储电已满', '停止 · 蓄電満杯', 'Остановлен · накопитель полон'),
    'gui.mio_icif.stirling.state.no_heat': v('Idle · no heat coming in', '待机 · 没有热量输入', '待機 · 熱の入力なし', 'Ожидание · нет тепла'),
    'gui.mio_icif.stirling.state.warming': v('Warming up · a cold heater converts less', '暖机中 · 加热头未热，转换率低', '暖機中 · 冷えたヘッドは変換が少ない', 'Прогрев · холодный нагреватель даёт меньше'),
    'gui.mio_icif.stirling.state.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.stirling.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.stirling.tip1': v('The engine works between the hot heater and the cold cooler: the larger the difference, the more it makes per HU.',
        '发动机工作在热的加热头和冷的冷却端之间：温差越大，每 HU 出得越多。', 'エンジンは熱い加熱ヘッドと冷たい冷却端の間で働く。温度差が大きいほど 1 HU あたりの出力が増える。',
        'Двигатель работает между горячим нагревателем и холодным холодильником: чем больше разница, тем больше на HU.'),
    'gui.mio_icif.stirling.tip2': v('The heater gets hotter with more heat flowing (650 °C at the rated flow); a cold engine warms up first.',
        '热流越大加热头越热（额定热流时 650 °C）；冷机要先暖机。', '熱流が多いほど加熱ヘッドは熱くなる（定格で 650 °C）。冷えたエンジンはまず暖機する。',
        'Чем больше поток тепла, тем горячее нагреватель (650 °C при номинале); холодный двигатель сначала прогревается.'),
    'gui.mio_icif.stirling.tip3': v('The cooler is the air around it (cold biomes help); water beside the machine cools it better.',
        '冷却端靠周围空气（寒冷群系更好）；机器旁边有水冷却更好。', '冷却端は周りの空気（寒いバイオームほど良い）。機械の隣に水があるとよく冷える。',
        'Холодильник — окружающий воздух (холодные биомы лучше); вода рядом охлаждает лучше.'),
    'gui.mio_icif.stirling.kinetic_flow': v('Heat %s HU/t · %s KU/HU · gives %s KU/t · water %s mB',
        '热流 %s HU/t · %s KU/HU · 输出 %s KU/t · 水 %s mB', '熱流 %s HU/t · %s KU/HU · 出力 %s KU/t · 水 %s mB',
        'Тепло %s HU/t · %s KU/HU · отдаёт %s KU/t · вода %s mB'),
    'gui.mio_icif.stirling.state.overspeed': v('Overspeed · flywheel full, no heat taken', '超速 · 飞轮已满，不再吸热', '過回転 · フライホイール満杯、熱を取らない', 'Разнос · маховик полон, тепло не берётся'),
    'gui.mio_icif.stirling.state.no_water': v('Stopped · no cooling water', '停机 · 没有冷却水', '停止 · 冷却水なし', 'Остановлен · нет охлаждающей воды'),
    'gui.mio_icif.stirling.state.hot_full': v('Stopped · hot water tank full', '停机 · 热水罐已满', '停止 · 温水タンク満杯', 'Остановлен · бак горячей воды полон'),
    'gui.mio_icif.stirling.state.coasting': v('Coasting · the flywheel still turns', '惯性运转 · 飞轮仍在转', '惰性回転 · フライホイールはまだ回っている', 'Выбег · маховик ещё крутится'),
    'gui.mio_icif.stirling.flywheel': v('Flywheel', '飞轮', 'フライホイール', 'Маховик'),
    'gui.mio_icif.stirling.tip_flywheel': v('The flywheel stores the KU; the faster it turns the more it gives each tick, so it settles where it gives what the engine makes.',
        '飞轮储存 KU；转得越快每 tick 给得越多，所以会稳定在"给出 = 发动机产出"的转速。', 'フライホイールが KU を蓄える。速く回るほど毎 tick 多く出すので、エンジンの出力と釣り合う回転で落ち着く。',
        'Маховик хранит KU; чем быстрее он крутится, тем больше отдаёт, и обороты устанавливаются там, где отдача равна выработке.'),
    'gui.mio_icif.stirling.tip_water': v('Its water is the cooling water: it leaves as hot water, which must be taken away.',
        '它的水就是冷却水：变成热水流出，热水必须导走。', '水は冷却水で、温水として出る。温水は抜き取ること。', 'Его вода — охлаждающая: она выходит горячей, её нужно отводить.'),
    'gui.mio_icif.shaft.source.none': v('none', '无', 'なし', 'нет'),
    'gui.mio_icif.shaft.source.natural': v('wind/water', '风/水', '風/水', 'ветер/вода'),
    'gui.mio_icif.shaft.source.other': v('KU source', '动能源', 'KU 源', 'источник KU'),
    'gui.mio_icif.shaft.in': v('In %s KU/t of %s max · load %s', '输入 %s KU/t / 最大 %s · 负荷 %s', '入力 %s KU/t / 最大 %s · 負荷 %s', 'Вход %s KU/t из %s · нагрузка %s'),
    'gui.mio_icif.shaft.ratio': v('%s EU per KU', '每 KU %s EU', 'KU あたり %s EU', '%s EU на KU'),
    'gui.mio_icif.shaft.ratio_natural': v('%s EU per KU (wind/water)', '每 KU %s EU（风/水）', 'KU あたり %s EU（風/水）', '%s EU на KU (ветер/вода)'),
    'gui.mio_icif.shaft.ratio_other': v('%s EU per KU (other sources)', '每 KU %s EU（其他来源）', 'KU あたり %s EU（その他）', '%s EU на KU (другие источники)'),
    'gui.mio_icif.shaft.state.full': v('Stopped · storage full', '停机 · 储电已满', '停止 · 蓄電満杯', 'Остановлен · накопитель полон'),
    'gui.mio_icif.shaft.state.no_source': v('No KU source on the front', '正面没有动能源', '正面に KU 源がない', 'Спереди нет источника KU'),
    'gui.mio_icif.shaft.state.idle': v('Idle · the source gives no KU', '待机 · 动能源没有输出', '待機 · KU 源の出力なし', 'Ожидание · источник не даёт KU'),
    'gui.mio_icif.shaft.state.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.shaft.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.shaft.tip1': v('Takes KU from the machine on its front and turns it into EU.', '从正面的机器取 KU 并转为 EU。', '正面の機械から KU を取り EU に変える。', 'Берёт KU у машины спереди и превращает в EU.'),
    'gui.mio_icif.shaft.tip_turbo': v('Wind and water turbines give the full rate; other KU sources a lower one.', '风力、水力动能给全额比例；其他动能源比例较低。',
        '風力・水力は全レート、その他の KU 源は低いレート。', 'Ветер и вода дают полный коэффициент, другие источники — меньший.'),
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
