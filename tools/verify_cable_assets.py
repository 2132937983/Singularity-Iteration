"""Check the cable assets touched by the insulation/color restoration, offline."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1] / 'src/main/resources'
assets = root / 'assets/mio_icif'
data = root / 'data/mio_icif'
checked = set()

def read(path):
    checked.add(path)
    return json.loads(path.read_text(encoding='utf-8-sig'))

def verify_model(model_id):
    namespace, name = model_id.split(':', 1) if ':' in model_id else ('minecraft', model_id)
    if namespace != 'mio_icif':
        return
    path = assets / f'models/{name}.json'
    if path in checked:
        return
    model = read(path)
    if 'parent' in model:
        verify_model(model['parent'])
    for texture in model.get('textures', {}).values():
        if texture.startswith('mio_icif:'):
            assert (assets / f'textures/{texture.split(":", 1)[1]}.png').is_file(), texture
    for element in model.get('elements', []):
        assert all(a < b for a, b in zip(element['from'], element['to'])), path

for metal, layer in [('gold', 2), ('iron', 2), ('iron', 3)]:
    name = f'block_{metal}_cable_{layer}'
    state = read(assets / f'blockstates/wiring/cable/{name}.json')
    for part in state['multipart']:
        models = part['apply']
        for model in models if isinstance(models, list) else [models]:
            verify_model(model['model'])
    verify_model(f'mio_icif:item/wiring/cable/{name}')
    read(data / f'loot_table/blocks/wiring/cable/{name}.json')
    for bulk in [False, True]:
        recipe = read(data / f'recipe/insulate_{metal}_cable_{layer}{"_bulk" if bulk else ""}.json')
        assert len(recipe['ingredients']) == 1 + (layer if bulk else 1)
        assert recipe['result']['id'] == f'mio_icif:wiring/cable/{name}'
    for locale in ['en_us', 'zh_cn']:
        translations = read(assets / f'lang/{locale}.json')
        assert f'block.mio_icif.wiring.cable.{name}' in translations
    assert f'mio_icif:wiring/cable/{name}' in read(root / 'data/c/tags/block/cable.json')['values']

for name in ['block_tin_cable_1', 'block_cable_1', 'block_glass_cable', 'block_gold_cable_1',
             'block_gold_cable_2', 'block_iron_cable_1', 'block_iron_cable_2', 'block_iron_cable_3']:
    verify_model(f'mio_icif:block/wiring/cable/{name}')
    model = read(assets / f'models/block/wiring/cable/{name}.json')
    bands = [element for element in model['elements'] if element.get('name') == 'color_band']
    assert len(bands) == 1 and all(face.get('tintindex') == 0 for face in bands[0]['faces'].values()), name

print(f'Cable assets verified: {len(checked)} JSON files, model/texture references, recipes, translations and tags.')
