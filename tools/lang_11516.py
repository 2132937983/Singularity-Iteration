#!/usr/bin/env python3
"""0.1.11.16 language values (en_us / zh_cn / ja_jp / ru_ru): the plant mimic screens of the steam chain (boiler, steam
turbine, steam kinetic generator, condenser, repressurizer: labels, reading lines, states, tooltips).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_11516.py <repo root> [--check]
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
    'gui.mio_icif.steam.condenser': v('Condenser', '冷凝器', '復水器', 'Конденсатор'),
    'gui.mio_icif.steam.to_air': v('to air', '排大气', '大気へ', 'в атм.'),
    'gui.mio_icif.steam.no_steam': v('no steam', '无蒸汽', '蒸気なし', 'нет пара'),
    'gui.mio_icif.steam.inlet': v('In %s · %s bar %s °C · %s / %s mB', '进汽 %s · %s bar %s °C · %s / %s mB', '入口 %s · %s bar %s °C · %s / %s mB', 'Вход %s · %s бар %s °C · %s / %s mB'),
    'gui.mio_icif.steam.expansion': v('To %s bar · exhaust dryness %s · drop %s kJ/kg · %s EU/mB', '膨胀至 %s bar · 排汽干度 %s · 焓降 %s kJ/kg · %s EU/mB',
        '%s bar まで膨張 · 排気乾き度 %s · 熱落差 %s kJ/kg · %s EU/mB', 'До %s бар · сухость выхлопа %s · перепад %s кДж/кг · %s EU/mB'),
    'gui.mio_icif.steam.to_condenser': v('%s mB/t to the condenser', '%s mB/t 进冷凝器', '%s mB/t を復水器へ', '%s mB/t в конденсатор'),
    'gui.mio_icif.steam.vented': v('%s mB/t vented', '%s mB/t 排空', '%s mB/t 放出', '%s mB/t в атмосферу'),
    'gui.mio_icif.steam.output': v('Output %s EU/t', '输出 %s EU/t', '出力 %s EU/t', 'Выход %s EU/t'),
    'gui.mio_icif.steam.stored': v('Stored', '储电', '蓄電', 'Заряд'),
    'gui.mio_icif.steam.tip.turbine1': v('Steam expands through the blades from the inlet pressure to the back pressure.', '蒸汽在叶片间从进汽压力膨胀到背压。',
        '蒸気は入口圧から背圧まで翼の間で膨張する。', 'Пар расширяется в лопатках от давления на входе до противодавления.'),
    'gui.mio_icif.steam.tip.turbine2': v('A condenser beside it lowers the back pressure to 0.1 bar: more work per mB.', '旁边放一台冷凝机，背压降到 0.1 bar：每 mB 做功更多。',
        '隣に復水器を置くと背圧が 0.1 bar に下がり、1 mB あたりの仕事が増える。', 'Конденсатор рядом снижает противодавление до 0,1 бар: больше работы на mB.'),
    'gui.mio_icif.steam.tip.turbine3': v('Superheated steam stays drier and gives about twice the work.', '过热蒸汽更干，做功约为两倍。',
        '過熱蒸気は乾いたままで、仕事は約 2 倍。', 'Перегретый пар суше и даёт примерно вдвое больше работы.'),
    'gui.mio_icif.steam.kinetic_inlet': v('In %s · %s / %s mB · %s KU/mB', '进汽 %s · %s / %s mB · %s KU/mB', '入口 %s · %s / %s mB · %s KU/mB', 'Вход %s · %s / %s mB · %s KU/mB'),
    'gui.mio_icif.steam.kinetic_expansion': v('Back %s bar · dryness %s · blades %s · condensate %s / %s mB', '背压 %s bar · 干度 %s · 叶片 %s · 凝水 %s / %s mB',
        '背圧 %s bar · 乾き度 %s · 翼 %s · 復水 %s / %s mB', 'Против. %s бар · сухость %s · лопатки %s · конденсат %s / %s mB'),
    'gui.mio_icif.steam.state.no_turbine': v('Stopped · no turbine', '停机 · 没有涡轮', '停止 · タービンなし', 'Остановлен · нет турбины'),
    'gui.mio_icif.steam.state.waterlogged': v('Stopped · condensate floods the turbine', '停机 · 凝水淹没涡轮', '停止 · 復水がタービンを満たした', 'Остановлен · турбину залил конденсат'),
    'gui.mio_icif.steam.state.venting': v('Venting · the exhaust has nowhere to go', '泄漏 · 排汽无处可去', '放出中 · 排気の行き場がない', 'Сброс · выхлопу некуда уйти'),
    'gui.mio_icif.steam.state.throttled': v('Throttled · condensate builds up', '降速 · 凝水堆积', '絞り · 復水がたまっている', 'Дросселирование · копится конденсат'),
    'gui.mio_icif.steam.state.running': v('Running', '运行', '運転中', 'Работает'),
    'gui.mio_icif.steam.state.idle': v('Idle', '待机', '待機', 'Ожидание'),
    'gui.mio_icif.steam.steam': v('Steam', '蒸汽', '蒸気', 'Пар'),
    'gui.mio_icif.steam.tip.kinetic1': v('The steam drives the turbine in the slot; the shaft gives kinetic units.', '蒸汽推动槽中的涡轮，转轴输出动能。',
        '蒸気がスロットのタービンを回し、軸が運動エネルギーを出す。', 'Пар вращает турбину в слоте, вал отдаёт кинетическую энергию.'),
    'gui.mio_icif.steam.tip.kinetic2': v('Condensate must be taken away (pipe it out or a condenser beside it), or the turbine floods.', '凝水必须导走（管道抽出或旁边放冷凝机），否则涡轮被淹。',
        '復水は抜き取ること（配管か隣の復水器）。さもないとタービンが水没する。', 'Конденсат нужно отводить (трубой или конденсатором рядом), иначе турбину зальёт.'),
    'gui.mio_icif.steam.condenser_in': v('Steam %s / %s mB · condenses up to %s mB/t', '蒸汽 %s / %s mB · 最多冷凝 %s mB/t', '蒸気 %s / %s mB · 最大 %s mB/t 復水', 'Пар %s / %s mB · до %s mB/t'),
    'gui.mio_icif.steam.condenser_out': v('Distilled water %s / %s mB · vents %s · %s EU/t', '蒸馏水 %s / %s mB · 散热片 %s · %s EU/t', '蒸留水 %s / %s mB · 放熱板 %s · %s EU/t', 'Дист. вода %s / %s mB · радиаторы %s · %s EU/t'),
    'gui.mio_icif.steam.state.condensing': v('Condensing', '冷凝中', '復水中', 'Конденсация'),
    'gui.mio_icif.steam.tip.condenser1': v('Steam in, distilled water out; vents cool it faster and draw power.', '蒸汽进、蒸馏水出；散热片加快冷却并耗电。',
        '蒸気が入り蒸留水が出る。放熱板は冷却を速め、電力を使う。', 'Пар на входе, дистиллят на выходе; радиаторы ускоряют охлаждение и потребляют энергию.'),
    'gui.mio_icif.steam.tip.condenser2': v('Beside a steam turbine it takes the exhaust: the turbine then works down to 0.1 bar.', '放在汽轮机旁会接收排汽：汽轮机随之膨胀到 0.1 bar。',
        '蒸気タービンの隣に置くと排気を受け取り、タービンは 0.1 bar まで膨張する。', 'Рядом с паровой турбиной принимает выхлоп: турбина работает до 0,1 бар.'),
    'gui.mio_icif.steam.vessel': v('Vessel', '加压罐', '加圧容器', 'Сосуд'),
    'gui.mio_icif.steam.repress_in': v('In %s · %s / %s mB', '进料 %s · %s / %s mB', '入口 %s · %s / %s mB', 'Вход %s · %s / %s mB'),
    'gui.mio_icif.steam.repress_rule': v('10 mB superheated → 20 mB steam, the same energy', '10 mB 过热蒸汽 → 20 mB 蒸汽，能量相同', '過熱蒸気 10 mB → 蒸気 20 mB、エネルギーは同じ', '10 mB перегретого → 20 mB пара, та же энергия'),
    'gui.mio_icif.steam.repress_out': v('Steam out %s / %s mB', '出汽 %s / %s mB', '出口蒸気 %s / %s mB', 'Пар на выходе %s / %s mB'),
    'gui.mio_icif.steam.heat': v('Heat', '热量', '熱量', 'Тепло'),
    'gui.mio_icif.steam.tip.repress1': v('Turns superheated steam back into ordinary steam for machines that need it.', '把过热蒸汽转回普通蒸汽，供需要普通蒸汽的机器使用。',
        '過熱蒸気を普通の蒸気に戻し、それを使う機械へ送る。', 'Превращает перегретый пар обратно в обычный для машин, которым он нужен.'),
    'gui.mio_icif.steam.tip.repress2': v('Superheated steam is worth more straight in a turbine.', '过热蒸汽直接进汽轮机更划算。',
        '過熱蒸気はそのままタービンに入れたほうが得。', 'Перегретый пар выгоднее сразу пускать в турбину.'),
    'gui.mio_icif.steam.pressure': v('Valve', '压力阀', '圧力弁', 'Клапан'),
    'gui.mio_icif.steam.feed': v('Feed', '给水', '給水', 'Подача'),
    'gui.mio_icif.steam.no_output': v('no output', '无产出', '出力なし', 'нет выхода'),
    'gui.mio_icif.steam.boiler_line': v('Valve %s · %s °C · heat in %s HU/t', '压力阀 %s · %s °C · 输入热 %s HU/t', '圧力弁 %s · %s °C · 入熱 %s HU/t', 'Клапан %s · %s °C · тепло %s HU/t'),
    'gui.mio_icif.steam.boiler_flow': v('Feed %s mB/t → %s mB/t', '给水 %s mB/t → %s mB/t', '給水 %s mB/t → %s mB/t', 'Подача %s mB/t → %s mB/t'),
    'gui.mio_icif.steam.state.calcified': v('Stopped · scale %s (needs cleaning or distilled water)', '停机 · 水垢 %s（需清理或改用蒸馏水）', '停止 · スケール %s（清掃か蒸留水が必要）', 'Остановлен · накипь %s (чистка или дистиллят)'),
    'gui.mio_icif.steam.state.too_hot': v('Danger · explodes above 500 °C · scale %s', '危险 · 超过 500 °C 爆炸 · 水垢 %s', '危険 · 500 °C 超で爆発 · スケール %s', 'Опасно · взрыв выше 500 °C · накипь %s'),
    'gui.mio_icif.steam.state.heating': v('Heating up · scale %s', '升温中 · 水垢 %s', '昇温中 · スケール %s', 'Нагрев · накипь %s'),
    'gui.mio_icif.steam.state.cold': v('Cold · no heat · scale %s', '冷态 · 没有热量 · 水垢 %s', '冷態 · 熱なし · スケール %s', 'Холодный · нет тепла · накипь %s'),
    'gui.mio_icif.steam.state.supercritical': v('Above 374 °C · superheated steam · scale %s', '超过 374 °C · 产过热蒸汽 · 水垢 %s', '374 °C 超 · 過熱蒸気 · スケール %s', 'Выше 374 °C · перегретый пар · накипь %s'),
    'gui.mio_icif.steam.state.passing': v('Valve at 0 · water passes through · scale %s', '压力阀为 0 · 水直接流过 · 水垢 %s', '圧力弁 0 · 水がそのまま流れる · スケール %s', 'Клапан 0 · вода идёт насквозь · накипь %s'),
    'gui.mio_icif.steam.state.hot_idle': v('Hot · no steam this tick (not enough heat or water) · scale %s', '热态 · 本刻未产汽（热量或给水不足）· 水垢 %s', '高温 · この tick は蒸気なし（熱か給水が不足）· スケール %s', 'Горячий · пара нет (мало тепла или воды) · накипь %s'),
    'gui.mio_icif.steam.state.boiling': v('Boiling · steam · scale %s', '沸腾 · 产蒸汽 · 水垢 %s', '沸騰 · 蒸気 · スケール %s', 'Кипение · пар · накипь %s'),
    'gui.mio_icif.steam.water': v('Water', '水', '水', 'Вода'),
    'gui.mio_icif.steam.tip.boiler1': v('Heat from the sides boils the feed water in the drum.', '侧面输入的热量把锅筒里的给水烧开。',
        '側面からの熱がドラム内の給水を沸かす。', 'Тепло с боков кипятит воду в барабане.'),
    'gui.mio_icif.steam.tip.boiler2': v('Below 374 °C it makes steam, above it superheated steam; past 500 °C it explodes.', '374 °C 以下产蒸汽，以上产过热蒸汽；超过 500 °C 会爆炸。',
        '374 °C 未満で蒸気、以上で過熱蒸気。500 °C を超えると爆発する。', 'Ниже 374 °C даёт пар, выше — перегретый; выше 500 °C взрывается.'),
    'gui.mio_icif.steam.tip.boiler3': v('Tap water leaves scale; distilled water does not.', '普通水会结水垢，蒸馏水不会。',
        '普通の水はスケールを残し、蒸留水は残さない。', 'Обычная вода оставляет накипь, дистиллированная — нет.'),
    'gui.mio_icif.steam.tip.controls': v('Left: pressure valve. Right: feed water mB/t.', '左列：压力阀。右列：给水 mB/t。', '左：圧力弁。右：給水 mB/t。', 'Слева: клапан давления. Справа: подача воды mB/t.'),
}

REMOVE = ['gui.mio_icif.steam.superheated', 'gui.mio_icif.steam.steam_out']


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
