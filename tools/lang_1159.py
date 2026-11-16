#!/usr/bin/env python3
"""0.1.11.9 language values (en_us / zh_cn / ja_jp / ru_ru): the wind machine screens (wind rose, power curve, factors,
storm protection, compass points, tooltips) and the wind meter's direction line.
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_1159.py <repo root> [--check]
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
    'gui.mio_icif.wind.point.n': v('N', '北', '北', 'С'),
    'gui.mio_icif.wind.point.ne': v('NE', '东北', '北東', 'СВ'),
    'gui.mio_icif.wind.point.e': v('E', '东', '東', 'В'),
    'gui.mio_icif.wind.point.se': v('SE', '东南', '南東', 'ЮВ'),
    'gui.mio_icif.wind.point.s': v('S', '南', '南', 'Ю'),
    'gui.mio_icif.wind.point.sw': v('SW', '西南', '南西', 'ЮЗ'),
    'gui.mio_icif.wind.point.w': v('W', '西', '西', 'З'),
    'gui.mio_icif.wind.point.nw': v('NW', '西北', '北西', 'СЗ'),
    'gui.mio_icif.wind.wind_line': v('Wind %s MCU (field %s)', '风 %s MCU（风场 %s）', '風 %s MCU（風場 %s）', 'Ветер %s MCU (поле %s)'),
    'gui.mio_icif.wind.factors': v('Facing %s · wake %s · obstruction %s', '朝向 %s · 尾流 %s · 遮挡 %s', '向き %s · 後流 %s · 遮蔽 %s', 'Ориент. %s · след %s · помехи %s'),
    'gui.mio_icif.wind.output_ku': v('%s KU/t', '%s KU/t', '%s KU/t', '%s KU/t'),
    'gui.mio_icif.wind.output_eu': v('%s EU/t', '%s EU/t', '%s EU/t', '%s EU/t'),
    'gui.mio_icif.wind.state.no_rotor': v('No rotor', '无转子', 'ローターなし', 'Нет ротора'),
    'gui.mio_icif.wind.state.blocked': v('Rotor space taken', '转子空间被占', 'ローター位置が塞がっている', 'Место ротора занято'),
    'gui.mio_icif.wind.state.weak': v('Too little wind', '风力不足', '風が弱い', 'Слабый ветер'),
    'gui.mio_icif.wind.state.feathered': v('Feathered (storm)', '顺桨保护', 'フェザリング（暴風）', 'Флюгер (шторм)'),
    'gui.mio_icif.wind.state.overload': v('Overload · rotor wear', '过载 · 转子损伤', '過負荷・ローター損傷', 'Перегрузка · износ'),
    'gui.mio_icif.wind.state.run': v('Running', '运行', '稼働中', 'Работа'),
    'gui.mio_icif.wind.state.full': v('Storage full', '储能已满', '蓄電満杯', 'Хранилище полно'),
    'gui.mio_icif.wind.state.too_low': v('Effective height too low', '有效高度不足', '有効高度不足', 'Мала высота'),
    'gui.mio_icif.wind.storm.on': v('Storm guard ON', '风暴保护 开', '暴風保護 ON', 'Защита ВКЛ'),
    'gui.mio_icif.wind.storm.off': v('Storm guard OFF', '风暴保护 关', '暴風保護 OFF', 'Защита ВЫКЛ'),
    'gui.mio_icif.wind.rotor_health': v('Rotor', '转子耐久', 'ローター耐久', 'Ротор'),
    'gui.mio_icif.wind.insert_rotor': v('Insert a rotor', '放入转子', 'ローターを入れる', 'Вставьте ротор'),
    'gui.mio_icif.wind.cut_in': v('Cut-in %s', '切入 %s', 'カットイン %s', 'Пуск %s'),
    'gui.mio_icif.wind.cut_out': v('Cut-out %s', '切出 %s', 'カットアウト %s', 'Отсечка %s'),
    'gui.mio_icif.wind.axis_ku': v('KU/t', 'KU/t', 'KU/t', 'KU/t'),
    'gui.mio_icif.wind.axis_eu': v('EU/t', 'EU/t', 'EU/t', 'EU/t'),
    'gui.mio_icif.wind.axis_height': v('Effective height Y%s–%s', '有效高度 Y%s～%s', '有効高度 Y%s～%s', 'Эфф. высота Y%s–%s'),
    'gui.mio_icif.wind.base_wind': v('Wind %s (weather %s)', '风力 %s（天气 %s）', '風力 %s（天気 %s）', 'Ветер %s (погода %s)'),
    'gui.mio_icif.wind.height_line': v('Effective height %s (%s obstacles, starts at Y%s)', '有效高度 %s（障碍 %s 格，起点 Y%s）', '有効高度 %s（障害 %s、起点 Y%s）', 'Эфф. высота %s (%s помех, от Y%s)'),
    'gui.mio_icif.wind.stored': v('Stored', '储能', '蓄電', 'Запас'),
    'gui.mio_icif.wind.tip.rose': v('Wind rose: the longer a sector, the more often the wind came from there; the darker, the stronger (about the last minute)', '风玫瑰：扇区越长，风越常从这个方向来；颜色越深风越大（约最近 1 分钟）', '風配図：扇形が長いほどその方角から吹いた頻度が高く、濃いほど強い（直近約1分）', 'Роза ветров: чем длиннее сектор, тем чаще ветер дул оттуда; чем темнее, тем сильнее (около минуты)'),
    'gui.mio_icif.wind.tip.facing': v('Black needle: the rotor; red: the wind now. Square to the wind (front or back) 100 %, side-on 70 %', '黑针是转子朝向，红针是当前风向。正对或背对来风 100%，完全侧风 70%', '黒針がローターの向き、赤針が今の風向。正対・背面 100%、真横 70%', 'Чёрная стрелка — ротор, красная — ветер. Прямо на ветер (или спиной) 100 %, боком 70 %'),
    'gui.mio_icif.wind.tip.curve': v('Power curve: nothing below cut-in; between cut-in and cut-out, output = wind × efficiency × 10; the red dot is now', '功率曲线：低于切入风速不转；切入到切出之间出力 = 风力 × 效率 × 10；红点是当前工作点', '出力曲線：カットイン未満は停止、カットインからカットアウトまで出力 = 風力 × 効率 × 10。赤点が現在', 'Кривая мощности: ниже пуска — ноль; от пуска до отсечки выход = ветер × КПД × 10; красная точка — сейчас'),
    'gui.mio_icif.wind.tip.storm': v("Storm guard: above the rotor's cut-out wind it feathers and stops: no output, no wear", '风暴保护：风力超过转子的切出风速时顺桨停转，不出力也不磨损', '暴風保護：カットアウトを超えるとフェザリングして停止、出力も摩耗もなし', 'Защита: выше отсечки ротор встаёт во флюгер — нет выхода и нет износа'),
    'gui.mio_icif.wind.tip.storm2': v('Off: it keeps turning, but the rotor wears four times as fast', '关闭时照常出力，但转子以 4 倍速度损耗', 'オフ：回り続けるがローターは4倍速く摩耗する', 'Выкл.: продолжает работать, но ротор изнашивается вчетверо быстрее'),
    'gui.mio_icif.wind.tip.factors': v('Effective wind = field wind × facing × wake × obstruction (plus ±8 % gusts)', '有效风力 = 风场风力 × 朝向 × 尾流 × 遮挡（另有 ±8% 阵风）', '有効風力 = 風場 × 向き × 後流 × 遮蔽（±8% の突風あり）', 'Эфф. ветер = поле × ориентация × след × помехи (плюс порывы ±8 %)'),
    'gui.mio_icif.wind.tip.wake': v('A turbine in front costs wind by its distance (Jensen wake model); only one right against the rotor (1 block) blocks it', '前方有其他风机时按距离计算尾流损失（Jensen 模型），只有紧贴（1 格内）才会完全阻塞', '前方の風車は距離に応じた後流損失（Jensen モデル）。密着（1マス以内）のときだけ完全に塞がる', 'Турбина впереди отнимает ветер по расстоянию (модель Йенсена); блокирует только вплотную (1 блок)'),
    'gui.mio_icif.wind.tip.omni': v('This small turbine turns with the wind: its facing does not matter', '风力发电机的小转子会跟着风转，不受风向影响', 'この小型風車は風に合わせて向きを変えるので向きは関係ない', 'Этот малый ветряк поворачивается за ветром: ориентация не важна'),
    'gui.mio_icif.wind.tip.height': v('Output = weather × wind × (effective height − start) / 750; effective height = Y − obstacles around it', '出力 = 天气 × 风力 × (有效高度 − 起点) / 750；有效高度 = Y − 周围障碍数', '出力 = 天気 × 風力 × (有効高度 − 起点) / 750。有効高度 = Y − 周囲の障害数', 'Выход = погода × ветер × (эфф. высота − старт) / 750; эфф. высота = Y − помехи вокруг'),
    'message.mio_icif.windmeter.direction': v('Wind from %s° (%s) · weather %s', '风向 %s°（%s 方吹来）· 天气 %s', '風向 %s°（%s から）・天気 %s', 'Ветер с %s° (%s) · погода %s'),
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
