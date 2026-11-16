#!/usr/bin/env python3
"""0.1.11.5 language values (en_us / zh_cn / ja_jp / ru_ru): the tactical holomap's title, read-outs, directions, legend
and controls (they were hard-coded English), the suit detail view key, and the passive text HUD (messages, relative
directions, status line, detail panel, telemetry card).
A value per language; a language left out keeps its current value. Idempotent; keeps the line endings of each file. With
--check it only reports missing or different keys and removed keys that are still present.
Usage: lang_1155.py <repo root> [--check]
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


H = 'hud.mio_icif.holo.'
KEYS = {
    H + 'title': v('HOLO  %sx%sx%s', '全息  %s×%s×%s', 'ホロ  %s×%s×%s', 'ГОЛО  %s×%s×%s'),
    H + 'roof': v('ROOF CUT +%s', '剖开 +%s', '天井カット +%s', 'СРЕЗ КРЫШИ +%s'),
    H + 'hostile': v('HOSTILE %s / %s', '敌对 %s / %s', '敵対 %s / %s', 'ВРАГИ %s / %s'),
    H + 'n': v('N', '北', '北', 'С'),
    H + 'e': v('E', '东', '東', 'В'),
    H + 's': v('S', '南', '南', 'Ю'),
    H + 'w': v('W', '西', '西', 'З'),
    H + 'legend': v('\u25b2 hostile  \u25cf player / ally  \u25a0 neutral  \u00b7 passive  hollow = behind cover',
                    '\u25b2 敌对  \u25cf 玩家 / 僚机  \u25a0 中立  \u00b7 被动  空心 = 被遮挡',
                    '\u25b2 敵対  \u25cf プレイヤー / 僚機  \u25a0 中立  \u00b7 友好  中空 = 遮蔽物の向こう',
                    '\u25b2 враг  \u25cf игрок / союзник  \u25a0 нейтрал  \u00b7 мирный  пустой = за укрытием'),
    H + 'controls': v('Wheel: zoom %s%%   Shift + wheel: tilt %s\u00b0', '滚轮 缩放 %s%%　Shift + 滚轮 俯角 %s\u00b0',
                      'ホイール：拡大 %s%%　Shift＋ホイール：俯角 %s\u00b0', 'Колесо: масштаб %s%%   Shift + колесо: наклон %s\u00b0'),
    # 0.1.11.5 passive text HUD: messages, directions, status line, detail panel, telemetry card
    'hud.mio_icif.msg.threat': v('Threat', '威胁', '脅威', 'Угроза'),
    'hud.mio_icif.msg.threat.detail': v('%1$s · %2$s hostile · %3$s m', '%1$s · %2$s 个敌对 · %3$s 米', '%1$s · 敵 %2$s · %3$s m', '%1$s · врагов %2$s · %3$s м'),
    'hud.mio_icif.msg.incoming': v('Incoming', '来袭', '飛来', 'Снаряд'),
    'hud.mio_icif.msg.incoming.detail': v('%1$s · %2$s shot · %3$s s', '%1$s · %2$s 发 · %3$s 秒', '%1$s · %2$s 発 · %3$s 秒', '%1$s · %2$s шт. · %3$s с'),
    'hud.mio_icif.msg.blast': v('Blast', '爆炸', '爆発', 'Взрыв'),
    'hud.mio_icif.msg.blast.detail': v('%1$s · %2$s m · %3$s s', '%1$s · %2$s 米 · %3$s 秒', '%1$s · %2$s m · %3$s 秒', '%1$s · %2$s м · %3$s с'),
    'hud.mio_icif.msg.blast.inside': v('IN RADIUS · %1$s · %3$s s', '在范围内 · %1$s · %3$s 秒', '範囲内 · %1$s · %3$s 秒', 'В РАДИУСЕ · %1$s · %3$s с'),
    'hud.mio_icif.msg.energy': v('Energy', '电量', '電力', 'Энергия'),
    'hud.mio_icif.msg.energy.detail': v('%1$s %2$s%%', '%1$s %2$s%%', '%1$s %2$s%%', '%1$s %2$s%%'),
    'hud.mio_icif.msg.sonar': v('Sonar', '声呐', 'ソナー', 'Сонар'),
    'hud.mio_icif.msg.sonar.detail': v('%s ore contacts', '%s 处矿物', '鉱石 %s か所', 'руда: %s'),
    'hud.mio_icif.dir.ahead': v('ahead', '正前', '前方', 'впереди'),
    'hud.mio_icif.dir.ahead_right': v('ahead right', '右前', '右前', 'справа впереди'),
    'hud.mio_icif.dir.right': v('right', '右侧', '右', 'справа'),
    'hud.mio_icif.dir.behind_right': v('behind right', '右后', '右後', 'справа сзади'),
    'hud.mio_icif.dir.behind': v('behind', '正后', '後方', 'сзади'),
    'hud.mio_icif.dir.behind_left': v('behind left', '左后', '左後', 'слева сзади'),
    'hud.mio_icif.dir.left': v('left', '左侧', '左', 'слева'),
    'hud.mio_icif.dir.ahead_left': v('ahead left', '左前', '左前', 'слева впереди'),
    'hud.mio_icif.status.ok': v('visor ok', '面罩正常', 'バイザー正常', 'визор в норме'),
    'hud.mio_icif.status.messages': v('visor · messages: %s', '面罩 · %s 条消息', 'バイザー · メッセージ %s', 'визор · сообщений: %s'),
    'hud.mio_icif.detail.title': v('SUIT · DETAIL', '套装 · 详细', 'スーツ · 詳細', 'КОСТЮМ · ДЕТАЛИ'),
    'hud.mio_icif.detail.hold': v('hold %s', '按住 %s', '%s 長押し', 'удерживать %s'),
    'hud.mio_icif.detail.energy': v('Energy', '能量', '電力', 'Энергия'),
    'hud.mio_icif.detail.flight': v('Flight', '机动', '機動', 'Полёт'),
    'hud.mio_icif.detail.idle': v('idle', '待机', '待機', 'ожидание'),
    'hud.mio_icif.detail.units': v('Units', '模块', 'ユニット', 'Модули'),
    'hud.mio_icif.detail.chunk': v('Chunk grid', '区块电网', 'チャンク電力網', 'Сеть чанка'),
    'hud.mio_icif.detail.recent': v('Recent', '最近消息', '最近', 'Недавние'),
    'hud.mio_icif.msg.ground': v('Ground', '坠地', '地面', 'Земля'),
    'hud.mio_icif.msg.ground.detail': v('%1$s m · %2$s s', '%1$s 米 · %2$s 秒', '%1$s m · %2$s 秒', '%1$s м · %2$s с'),
    'hud.mio_icif.msg.flight_energy': v('Thrust power', '推进电量', '推進電力', 'Энергия тяги'),
    'hud.mio_icif.flight.speed': v('Speed', '速度', '速度', 'Скорость'),
    'hud.mio_icif.flight.alt': v('Altitude', '高度', '高度', 'Высота'),
    'hud.mio_icif.flight.tag': v('MNVR', '机动', '機動', 'МАНЁВР'),
    'hud.mio_icif.flight.mode.hover': v('hover', '悬停', 'ホバー', 'зависание'),
    'hud.mio_icif.flight.mode.cruise': v('cruise', '巡航', '巡航', 'крейсер'),
    'hud.mio_icif.flight.mode.supersonic': v('supersonic', '超音速', '超音速', 'сверхзвук'),
    'hud.mio_icif.flight.thrust': v('thrust', '推力', '推力', 'тяга'),
    'hud.mio_icif.flight.lock': v('throttle locked', '油门锁定', 'スロットル固定', 'тяга зафиксирована'),
    'hud.mio_icif.telemetry.title': v('GRID', '电网', '電力網', 'СЕТЬ'),
    'key.mio_icif.hud_detail': v('Suit detail view (hold)', '套装详细视图（按住）', 'スーツ詳細ビュー（長押し）', 'Подробный вид костюма (удерживать)'),
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
