#!/usr/bin/env python3
"""0.1.11.7 language values (en_us / zh_cn / ja_jp / ru_ru): the pump screen (cross-section, readings, controls,
tooltips).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_1157.py <repo root> [--check]
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
    'gui.mio_icif.pump.mode.pump': v('Pump', '抽取', '吸引', 'Откачка'),
    'gui.mio_icif.pump.mode.discharge': v('Discharge', '排放', '排出', 'Сброс'),
    'gui.mio_icif.pump.state.run': v('RUN', '运行', '稼働', 'РАБОТА'),
    'gui.mio_icif.pump.state.idle': v('IDLE', '待机', '待機', 'ОЖИД.'),
    'gui.mio_icif.pump.state.hold': v('KEEP', '保留', '保持', 'ДЕРЖИТ'),
    'gui.mio_icif.pump.state.tank_full': v('TANK FULL', '罐满', '満タン', 'БАК ПОЛОН'),
    'gui.mio_icif.pump.state.no_bucket': v('NO BUCKET', '不可放', '不可', 'НЕЛЬЗЯ'),
    'gui.mio_icif.pump.state.no_place': v('BLOCKED', '受阻', '阻止', 'ЗАПРЕТ'),
    'gui.mio_icif.pump.state.basin_full': v('FULL', '已满', '満水', 'ПОЛНО'),
    'gui.mio_icif.pump.scanning': v('Surveying…', '扫描中…', '測量中…', 'Обследование…'),
    'gui.mio_icif.pump.no_body': v('No fluid in range', '范围内没有液体', '範囲内に液体なし', 'Нет жидкости рядом'),
    'gui.mio_icif.pump.no_basin': v('Tank is empty', '储罐无液体', 'タンクが空', 'Бак пуст'),
    'gui.mio_icif.pump.basin_free': v('Room for %s buckets', '可放 %s 桶', '残り %s バケツ', 'Места: %s ведер'),
    'gui.mio_icif.pump.pool': v('%s buckets in pool', '池内 %s 桶', '池に %s バケツ', 'В бассейне %s ведер'),
    'gui.mio_icif.pump.pool_capped': v('≥ %s buckets', '≥ %s 桶', '≥ %s バケツ', '≥ %s ведер'),
    'gui.mio_icif.pump.infinite': v('∞ Infinite source', '∞ 无限水源', '∞ 無限水源', '∞ Бесконечный'),
    'gui.mio_icif.pump.eta': v('Done in %s', '约 %s 完成', '約 %s で完了', 'Готово через %s'),
    'gui.mio_icif.pump.eta_more': v('At least %s', '至少 %s', '少なくとも %s', 'Не менее %s'),
    'gui.mio_icif.pump.eta_never': v('Never runs dry', '永远抽不完', '枯れない', 'Не иссякнет'),
    'gui.mio_icif.pump.eta_tank_full': v('Tank full, waiting', '罐满，等待输出', '満タン、出力待ち', 'Бак полон, ждёт'),
    'gui.mio_icif.pump.flow': v('Flow %s bucket/s', '流量 %s 桶/秒', '流量 %s バケツ/秒', 'Поток %s вед/с'),
    'gui.mio_icif.pump.lift': v('Lift %s m', '扬程 %s m', '揚程 %s m', 'Подъём %s м'),
    'gui.mio_icif.pump.gravity': v('Gravity feed %s m', '自流 %s m', '自然流下 %s m', 'Самотёк %s м'),
    'gui.mio_icif.pump.drop': v('Drop %s m', '落差 %s m', '落差 %s m', 'Перепад %s м'),
    'gui.mio_icif.pump.props': v('%s K · %s kg/m³', '%s K · %s kg/m³', '%s K · %s kg/m³', '%s K · %s кг/м³'),
    'gui.mio_icif.pump.viscosity': v('Viscosity ×%s (water 1)', '粘度 ×%s（水=1）', '粘度 ×%s（水=1）', 'Вязкость ×%s (вода 1)'),
    'gui.mio_icif.pump.time.hm': v('%sh %sm', '%s 小时 %s 分', '%s時間%s分', '%s ч %s мин'),
    'gui.mio_icif.pump.time.ms': v('%sm %ss', '%s 分 %s 秒', '%s分%s秒', '%s мин %s с'),
    'gui.mio_icif.pump.time.s': v('%ss', '%s 秒', '%s秒', '%s с'),
    'gui.mio_icif.pump.waterlogged.on': v('Wlog ✓', '含水 ✓', '水没 ✓', 'Затоп ✓'),
    'gui.mio_icif.pump.waterlogged.off': v('Wlog ✗', '含水 ✗', '水没 ✗', 'Затоп ✗'),
    'gui.mio_icif.pump.keep.value': v('Keep %s', '留 %s', '残 %s', 'Ост %s'),
    'gui.mio_icif.pump.target.value': v('Y%s', 'Y%s', 'Y%s', 'Y%s'),
    'gui.mio_icif.pump.tip.mode': v('Click to switch: pump fluid out of the pool, or pour the tank out in front', '点击切换：抽取池中液体 / 把储罐里的液体排放到正面', 'クリックで切替：池から吸引 / タンクの液体を正面に排出', 'Нажмите: откачивать жидкость или сбрасывать содержимое бака перед насосом'),
    'gui.mio_icif.pump.tip.waterlogged': v('Whether the water in waterlogged blocks (slabs, stairs…) is pumped too', '含水方块（含水的台阶、楼梯等）里的水是否也抽取', '水没ブロック（ハーフブロック・階段など）の水も吸うか', 'Откачивать ли воду из затопленных блоков (плиты, ступени…)'),
    'gui.mio_icif.pump.tip.keep': v('Keep level: stop when this many buckets are left (not for infinite or very large bodies)', '保留水位：池内剩这么多桶时停止（无限水源和超大水体不适用）', '保持水位：この量が残ったら停止（無限水源・巨大水域は対象外）', 'Сохранять уровень: остановиться, когда останется столько ведер (не для бесконечных и очень больших)'),
    'gui.mio_icif.pump.tip.keep_step': v('Click ±1, Shift ±10, Ctrl ±100', '单击 ±1，Shift ±10，Ctrl ±100', 'クリック ±1、Shift ±10、Ctrl ±100', 'Клик ±1, Shift ±10, Ctrl ±100'),
    'gui.mio_icif.pump.tip.target': v('Discharge target height, relative to the pump: the fluid fills up to this layer', '排放目标高度（相对泵）：液体只填到这一层', '排出目標高さ（ポンプ基準）：この層まで満たす', 'Высота сброса относительно насоса: жидкость заполняет до этого слоя'),
    'gui.mio_icif.pump.tip.tank': v('Tank: %s %s / %s mB', '储罐：%s %s / %s mB', 'タンク：%s %s / %s mB', 'Бак: %s %s / %s mB'),
    'gui.mio_icif.pump.tip.tank_empty': v('Tank: empty', '储罐：空', 'タンク：空', 'Бак: пуст'),
    'gui.mio_icif.pump.tip.energy': v('Energy: %s / %s EU', '能量：%s / %s EU', 'エネルギー：%s / %s EU', 'Энергия: %s / %s EU'),
    'gui.mio_icif.pump.view.scanning': v('Surveying the body…', '扫描水体中…', '水域を測量中…', 'Обследование водоёма…'),
    'gui.mio_icif.pump.view.no_body': v('No fluid to pump in front', '正面范围内没有可抽取的液体', '正面の範囲に吸える液体なし', 'Перед насосом нечего откачивать'),
    'gui.mio_icif.pump.view.no_basin': v('Nothing in the tank to discharge', '储罐里没有可排放的液体', 'タンクに排出する液体なし', 'В баке нечего сбрасывать'),
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
