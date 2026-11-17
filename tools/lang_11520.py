#!/usr/bin/env python3
"""0.1.11.20 language values (en_us / zh_cn / ja_jp / ru_ru): the recorder screens of the RTG, the RT heat generator and
the geomagnetic generator (magnetogram), and the geomagnetic detector's Kp line.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11520.py <repo root> [--check]
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
    'gui.mio_icif.rtg.cold.air': v('air', '空气', '空気', 'воздух'),
    'gui.mio_icif.rtg.cold.hot': v('hot air', '热空气', '暑い空気', 'жаркий воздух'),
    'gui.mio_icif.rtg.cold.nether': v('Nether air', '下界空气', 'ネザーの空気', 'воздух Незера'),
    'gui.mio_icif.rtg.cold.water': v('water', '水', '水', 'вода'),
    'gui.mio_icif.rtg.cold.ice': v('ice', '冰', '氷', 'лёд'),
    'gui.mio_icif.rtg.line_table': v('%s pellets · %s EU/t in temperate air · cold side %s ×%s', '%s 颗靶丸 · 温带空气中 %s EU/t · 冷端 %s ×%s',
        '%s 個のペレット · 温帯の空気で %s EU/t · 冷却側 %s ×%s', '%s таблеток · %s EU/t в умеренном воздухе · холодная сторона %s ×%s'),
    'gui.mio_icif.rtg.line_temps': v('Hot junctions %s °C · cold side %s °C · ΔT %s K', '热端 %s °C · 冷端 %s °C · 温差 %s K', '高温接点 %s °C · 冷却側 %s °C · ΔT %s K', 'Горячие спаи %s °C · холодная сторона %s °C · ΔT %s K'),
    'gui.mio_icif.rtg.state.no_pellets': v('Idle · no pellets', '待机 · 没有靶丸', '待機 · ペレットなし', 'Ожидание · нет таблеток'),
    'gui.mio_icif.rtg.state.full': v('Waiting · storage full', '等待 · 储电已满', '待機 · 蓄電満杯', 'Ожидание · накопитель полон'),
    'gui.mio_icif.rtg.state.lava': v('Running · lava touching it costs 40 %', '运行 · 贴着岩浆，少 40%', '運転中 · 溶岩に接して 40% 減', 'Работает · касание лавы −40 %'),
    'gui.mio_icif.rtg.state.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.rtg.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.rtg.tip1': v('A thermoelectric generator: the pellets heat the hot junctions, the colder the other side the more it makes.',
        '温差发电机：靶丸加热热端，另一侧越凉发电越多。', '熱電発電機：ペレットが高温接点を温め、反対側が冷たいほど多く発電する。',
        'Термоэлектрический генератор: таблетки греют горячие спаи, чем холоднее другая сторона, тем больше энергии.'),
    'gui.mio_icif.rtg.tip2': v('The best face sets the cold side: ice or snow ×1.25, water ×1.15; hot biomes ×0.9, the Nether ×0.7; lava touching it ×0.6.',
        '六面中最好的一面决定冷端：冰或雪 ×1.25，水 ×1.15；炎热群系 ×0.9，下界 ×0.7；贴着岩浆再 ×0.6。',
        '一番良い面が冷却側になる：氷・雪 ×1.25、水 ×1.15。暑いバイオーム ×0.9、ネザー ×0.7。溶岩に接すると ×0.6。',
        'Лучшая грань задаёт холодную сторону: лёд или снег ×1,25, вода ×1,15; жаркие биомы ×0,9, Незер ×0,7; касание лавы ×0,6.'),
    'gui.mio_icif.rtg.pellets': v('%s pellets', '%s 颗靶丸', 'ペレット %s', 'таблеток: %s'),
    'gui.mio_icif.rtg.heat_line': v('%s pellets → %s HU/t', '%s 颗靶丸 → %s HU/t', 'ペレット %s 個 → %s HU/t', '%s таблеток → %s HU/t'),
    'gui.mio_icif.rtg.heat_front': v('The heat goes out of the front face', '热量从正面输出', '熱は正面から出る', 'Тепло выходит с передней грани'),
    'gui.mio_icif.rtg.heat': v('Heat', '热量', '熱量', 'Тепло'),
    'gui.mio_icif.rtg.heat_tip': v('The pellets give heat (2 to 64 HU/t) to the machine on the front face; they never run out.', '靶丸把热（2～64 HU/t）给正面的机器；靶丸不会耗尽。',
        'ペレットは正面の機械に熱（2〜64 HU/t）を渡す。ペレットは尽きない。', 'Таблетки отдают тепло (2–64 HU/t) машине спереди; они не истощаются.'),
    'gui.mio_icif.geomag.site': v('site ×%s', '场地 ×%s', '地点 ×%s', 'место ×%s'),
    'gui.mio_icif.geomag.storm': v('storm', '磁暴', '磁気嵐', 'буря'),
    'gui.mio_icif.geomag.quiet': v('quiet', '平静', '静穏', 'спокойно'),
    'gui.mio_icif.geomag.line_field': v('Disturbance ×%s · Kp %s · %s', '扰动 ×%s · Kp %s · %s', '擾乱 ×%s · Kp %s · %s', 'Возмущение ×%s · Kp %s · %s'),
    'gui.mio_icif.geomag.line_output': v('%s EU/t = 30,720 × site %s × disturbance %s', '%s EU/t = 30,720 × 场地 %s × 扰动 %s', '%s EU/t = 30,720 × 地点 %s × 擾乱 %s', '%s EU/t = 30 720 × место %s × возмущение %s'),
    'gui.mio_icif.geomag.state.structure': v('Stopped · the antennas and pedestals are incomplete', '停机 · 天线与基座不完整', '停止 · アンテナと台座が不完全', 'Остановлен · антенны и опоры неполные'),
    'gui.mio_icif.geomag.state.full': v('Waiting · storage full', '等待 · 储电已满', '待機 · 蓄電満杯', 'Ожидание · накопитель полон'),
    'gui.mio_icif.geomag.state.weak': v('Stopped · the field is too weak here (go deeper into solid ground)', '停机 · 这里磁场太弱（往实心地层里深处放）', '停止 · ここは磁場が弱すぎる（固い地中の深くへ）', 'Остановлен · поле здесь слишком слабое (глубже в твёрдый грунт)'),
    'gui.mio_icif.geomag.state.storm': v('Geomagnetic storm · output up to 1.5 times', '磁暴 · 出力最高 1.5 倍', '磁気嵐 · 出力最大 1.5 倍', 'Магнитная буря · до 1,5 раза'),
    'gui.mio_icif.geomag.state.quiet': v('Quiet · the daily variation (high by day, low at night)', '平静 · 日变化（白天高、夜里低）', '静穏 · 日変化（昼に高く夜に低い）', 'Спокойно · суточный ход (днём выше, ночью ниже)'),
    'gui.mio_icif.geomag.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.geomag.tip1': v('It runs on the field\'s change: the sunlit sky stirs it by day, and storms every few days stir it far more.',
        '它靠磁场的变化发电：白天日照的天空扰动它，隔几天一次的磁暴扰动得多得多。', '磁場の変化で発電する。昼は日の当たる空が揺らし、数日おきの磁気嵐はずっと大きく揺らす。',
        'Он работает на изменениях поля: днём его колышет освещённое небо, а бури раз в несколько дней — гораздо сильнее.'),
    'gui.mio_icif.geomag.tip2': v('The trace is the disturbance over the last three days; the bars are Kp every three hours, red in a storm.',
        '曲线是最近三天的扰动；柱是每三小时的 Kp，磁暴时为红色。', '曲線は直近 3 日の擾乱、柱は 3 時間ごとの Kp（嵐では赤）。',
        'Кривая — возмущение за последние три дня; столбики — Kp каждые три часа, красные во время бури.'),
    'gui.mio_icif.geomag.tip3': v('Every generator in a world sees the same storms; on average it makes about 27 % of the old constant output.',
        '同一个世界里所有发电机看到的是同一场磁暴；平均约为以前恒定出力的 27%。', '同じワールドの発電機は同じ嵐を受ける。平均で以前の一定出力の約 27%。',
        'Все генераторы в мире видят одни и те же бури; в среднем около 27 % прежней постоянной мощности.'),
    'message.mio_icif.geomagnetic_detector.kp': v('Kp %s · disturbance ×%s', 'Kp %s · 扰动 ×%s', 'Kp %s · 擾乱 ×%s', 'Kp %s · возмущение ×%s'),
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
