#!/usr/bin/env python3
"""0.1.7.36 language keys (en_us / zh_cn / ja_jp): the transformer mode message. Before 0.1.7.36
the engine sent hard-coded Chinese text. ASD-STE100 style."""
import json, os, sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')

ADD = {
    'en_us': {
        'message.mio_icif.transformer.mode_switched': 'Transformer: %s. Sneak + empty-hand right click to switch.',
    },
    'zh_cn': {
        'message.mio_icif.transformer.mode_switched': '变压器：%s；蹲下空手右键切换',
    },
    'ja_jp': {
        'message.mio_icif.transformer.mode_switched': '変圧器：%s（スニーク＋素手右クリックで切り替え）',
        'gui.mio_icif.transformer.mode.step_up': '昇圧モード',
        'gui.mio_icif.transformer.mode.step_down': '降圧モード',
        'gui.mio_icif.transformer.mode.redstone_control': 'レッドストーン制御',
    },
}


def main():
    for lang, add in ADD.items():
        path = os.path.join(LANG, lang + '.json')
        with open(path, encoding='utf-8') as f:
            data = json.load(f, object_pairs_hook=OrderedDict)
        for k, v in add.items():
            if lang == 'ja_jp' and k.startswith('gui.') and k in data:
                continue                     # keep an existing translation
            data[k] = v
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')
    print('ok')


if __name__ == '__main__':
    main()
