"""0.1.11.154: lang keys of the solar grid-tie and the panels' cells. Inserts each key after an anchor key; rerunning
leaves the files unchanged."""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'mio_icif', 'lang')

KEYS = {
    'gui.mio_icif.solar.stored_grid': {
        'zh_cn': '并网×%s', 'en_us': 'Grid×%s', 'ja_jp': '連系×%s', 'ru_ru': 'Сеть×%s'},
    'message.mio_icif.solar.no_room': {
        'zh_cn': '太阳能板放不下：它的面板要伸进的格子被占了（叶片下方只能走线缆）',
        'en_us': 'No room for the solar panel: a cell its panels reach into is taken (only a cable fits under a wing)',
        'ja_jp': 'ソーラーパネルを置けません：パネルがはみ出すマスが埋まっています（翼の下はケーブルのみ）',
        'ru_ru': 'Солнечной панели не хватает места: клетка, куда выходят её панели, занята (под крылом помещается только кабель)'},
    'message.mio_icif.solar.cell_taken': {
        'zh_cn': '这里被太阳能板的面板占着，放不了方块（叶片下方只能走线缆）',
        'en_us': 'A solar panel reaches into this cell: no block fits here (only a cable under a wing)',
        'ja_jp': 'ここはソーラーパネルがはみ出しています：ブロックは置けません（翼の下はケーブルのみ）',
        'ru_ru': 'Сюда выходит солнечная панель: блок не поставить (под крылом только кабель)'},
}
ANCHORS = {
    'gui.mio_icif.solar.stored_grid': 'gui.mio_icif.solar.stored',
    'message.mio_icif.solar.no_room': 'message.mio_icif.generator.gui_open_failed',
    'message.mio_icif.solar.cell_taken': 'message.mio_icif.solar.no_room',
}


def insert(lines, anchor, key, value):
    text = '  ' + json.dumps(key, ensure_ascii=False) + ': ' + json.dumps(value, ensure_ascii=False) + ','
    for i, line in enumerate(lines):
        if line.lstrip().startswith(json.dumps(key) + ':'):
            lines[i] = text
            return
    for i, line in enumerate(lines):
        if line.lstrip().startswith(json.dumps(anchor) + ':'):
            if not line.rstrip().endswith(','):
                lines[i] = line.rstrip() + ','
            lines.insert(i + 1, text)
            return
    raise SystemExit('no anchor ' + anchor)


for lang in ('zh_cn', 'en_us', 'ja_jp', 'ru_ru'):
    path = os.path.join(ROOT, lang + '.json')
    raw = open(path, 'rb').read().decode('utf-8')
    newline = '\r\n' if '\r\n' in raw else '\n'
    lines = raw.replace('\r\n', '\n').split('\n')
    for key in KEYS:
        insert(lines, ANCHORS[key], key, KEYS[key][lang])
    out = '\n'.join(lines)
    json.loads(out)
    open(path, 'wb').write(out.replace('\n', newline).encode('utf-8'))
    print(lang, 'ok')
