#!/usr/bin/env python3
"""0.1.8.3 language keys (en_us / zh_cn / ja_jp): names and tooltips of the client options in
mio_icif-client.toml as the NeoForge config screen shows them (mio_icif.configuration.<option>
and <option>.tooltip). Item names do not change in this version.

The script is idempotent and keeps the line endings of each file.
Usage: lang_183.py <repo root>
"""
import json
import os
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
C = 'mio_icif.configuration.'

# option: (en name, en tooltip, zh name, zh tooltip, ja name, ja tooltip)
OPTIONS = {
    'hud_enabled': ('FCS HUD', 'Master switch of the FCS HUD. The HUD key also toggles it.',
                    'FCS 抬头显示', 'FCS 抬头显示的总开关。HUD 按键也可切换。',
                    'FCS HUD', 'FCS HUD の主スイッチ。HUD キーでも切り替わる。'),
    'palette': ('HUD colour', 'Main HUD colour: green, amber or cyan. Warnings stay yellow and red.',
                'HUD 颜色', 'HUD 主色：绿色、琥珀色或青色。警告仍为黄色和红色。',
                'HUD の色', 'HUD の主色：緑、琥珀、シアン。警告は黄と赤のまま。'),
    'esp_outlines': ('ESP outlines', 'Draw a see-through outline around each tagged creature.',
                     'ESP 轮廓', '为每个已标记的生物绘制透视轮廓。',
                     'ESP 輪郭', 'タグ付けした生物ごとに透視の輪郭を描く。'),
    'holomap_scale': ('Holomap size', 'Size of the tactical holomap.', '全息地图大小', '战术全息地图的大小。',
                      'ホロマップの大きさ', '戦術ホロマップの大きさ。'),
    'show_compass': ('Compass tape', 'Show the heading tape at the top of the HUD.', '罗盘带', '在 HUD 顶部显示航向带。',
                     'コンパステープ', 'HUD 上部に方位テープを表示する。'),
    'camera_roll': ('Camera roll', 'The camera banks in turns in Special Maneuver Mode.', '镜头侧倾', '特殊机动模式下转弯时镜头侧倾。',
                    'カメラロール', '特殊機動モードの旋回でカメラが傾く。'),
    'speed_fov': ('Speed FOV', 'The field of view widens with speed in Special Maneuver Mode.', '速度视野', '特殊机动模式下视野随速度变宽。',
                  '速度 FOV', '特殊機動モードで速度に応じて視野が広がる。'),
    'vapor_effects': ('Flight vapour effects', 'Vapour cone, wake trails, sonic boom rings and condensation bursts in Special Maneuver Mode.',
                      '飞行气雾特效', '特殊机动模式下的音爆云锥、尾迹、音爆环和凝结云团。',
                      '飛行ベイパー効果', '特殊機動モードのベイパーコーン、航跡、ソニックブームのリングと凝結雲。'),
    'vapor_intensity': ('Vapour intensity', 'Opacity of the flight vapour effects. 1.0 is the default.',
                        '气雾强度', '飞行气雾特效的不透明度。默认 1.0。',
                        'ベイパーの濃さ', '飛行ベイパー効果の不透明度。既定は 1.0。'),
    'first_person_hand_armor': ('First-person hand armor', 'Blockbench chestplates show their arm armor and hand cover on your first-person arm.',
                                '第一人称手部护甲', 'Blockbench 胸甲在第一人称手臂上显示臂甲和护手。',
                                '一人称の手の装甲', 'Blockbench の胸当てが一人称の腕に腕甲と手甲を表示する。'),
    'third_person_hand_armor': ('Third-person hand armor', 'Blockbench chestplates cover the hands of the worn model (third person, other players).',
                                '第三人称手部护甲', 'Blockbench 胸甲覆盖穿戴模型的手部（第三人称、其他玩家）。',
                                '三人称の手の装甲', 'Blockbench の胸当てが装着モデルの手を覆う（三人称、他プレイヤー）。'),
    'muzzle_flash': ('Muzzle flash', 'A short glow at the muzzle when an energy weapon fires.',
                     '枪口闪光', '能量武器射击时在枪口产生短暂光芒。',
                     'マズルフラッシュ', 'エネルギー武器の発射時に銃口が短く光る。'),
    'reduced_flashes': ('Reduced flashes', 'Photosensitivity: dims every weapon flash and beam flare to about a third.',
                        '减弱闪光', '光敏保护：将所有武器闪光与光束光斑减弱到约三分之一。',
                        '閃光を弱める', '光過敏対策：武器の閃光とビームのフレアを約 3 分の 1 に弱める。'),
    'recoil_camera': ('Recoil camera kick', 'Camera kick when you fire a gun. 0 = off, 1 = default, 2 = strong. Visual only: the aim does not move.',
                      '后坐力镜头震动', '开枪时镜头上跳。0 = 关闭，1 = 默认，2 = 强。仅为视觉效果，准星不移动。',
                      '反動カメラ', '射撃時のカメラの跳ね上がり。0 = オフ、1 = 既定、2 = 強。見た目だけで照準は動かない。'),
    'beam_tracers': ('Beam tracers', 'Energy bolts start at the muzzle of the gun, so your own shots show in first person.',
                     '光束曳光', '能量弹从枪口开始绘制，第一人称下也能看到自己的射击。',
                     'ビームのトレーサー', 'エネルギー弾を銃口から描き、一人称でも自分の射撃が見える。'),
    'jetpack_volume': ('Thruster volume', 'Volume of the jetpack and flight-suit thrusters. 1.0 is the default.',
                       '推进器音量', '喷气背包与飞行装甲推进器的音量。默认 1.0。',
                       'スラスター音量', 'ジェットパックと飛行装甲のスラスター音量。既定は 1.0。'),
    'jetpack_cues': ('Ignition and shutdown sounds', 'Play the ignition and shutdown sounds of the thrusters.',
                     '点火与熄火音', '播放推进器的点火音与熄火音。',
                     '点火音と停止音', 'スラスターの点火音と停止音を鳴らす。'),
}


def main():
    col = {'en_us': 0, 'zh_cn': 2, 'ja_jp': 4}
    for lang, i in col.items():
        path = os.path.join(LANG, lang + '.json')
        raw = open(path, 'rb').read()
        eol = '\r\n' if b'\r\n' in raw else '\n'
        data = json.loads(raw.decode('utf-8'), object_pairs_hook=OrderedDict)
        for key, t in OPTIONS.items():
            data[C + key] = t[i]
            data[C + key + '.tooltip'] = t[i + 1]
        text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        with open(path, 'wb') as f:
            f.write(text.replace('\n', eol).encode('utf-8'))
        print(lang, 'keys set:', 2 * len(OPTIONS))


if __name__ == '__main__':
    main()
