#!/usr/bin/env python3
"""0.1.10.19 guide pages (en / zh_cn / ja_jp):
  * new page machines/multiblock_structures.md (under "Reactors, oil rigs and pipes"): every built-in
    blueprint as a 3D GameScene (<SiStructure>) with its material list (<SiStructureInfo>);
  * a "Structure" section before the recipe of 9 machine pages;
  * the reactor simulator page: the SAFE check runs a full fuel load, and the JEI "Reactor Layouts".
Idempotent (a page that already has the text is skipped). Keeps LF.
Usage: guide_1119.py <repo root>
"""
import os
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
MANUAL = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/guides/mio_icif/manual')
DIRS = {'en': 'machines', 'zh': '_zh_cn/machines', 'ja': '_ja_jp/machines'}

# blueprint -> (zoom, title en/zh/ja, page or None)
STRUCTURES = [
    ('nuclear_reactor', 4, ('Nuclear Reactor with chambers', '核反应堆与核反应仓', '原子炉とチャンバー'), 'generator_block_nuclear_reactor_generator'),
    ('fluid_reactor', 2.5, ('Fluid Reactor (5×5×5)', '流体反应堆（5×5×5）', '流体原子炉（5×5×5）'), 'reactor_block_reactor_vessel'),
    ('gesu', 4, ('GESU', 'GESU', 'GESU'), 'wiring_block_gesu_core'),
    ('large_fabricator', 4, ('Large Matter Fabricator', '大型物质生成机', '大型物質生成機'), 'producer_block_large_fabricator_core'),
    ('oil_rig', 3, ('Oil Rig', '石油钻机', '油井'), 'oilrig_block_oil_rig_core'),
    ('dimension_oil_rig', 2, ('Dimension Oil Rig', '维度石油钻机', '次元油井'), 'oilrig_block_dimension_oil_rig_core'),
    ('blast_furnace', 5, ('Blast Furnace and heat source', '高炉与热源', '高炉と熱源'), 'producer_block_blast_furnace'),
    ('fermenter', 5, ('Fermenter and heat source', '发酵机与热源', '発酵槽と熱源'), 'producer_block_fermenter_elc'),
]
EXTRA_PAGES = {'producer_block_blast_furnace_advanced': 'blast_furnace'}
RECIPE = {'en': '## Recipe', 'zh': '## 配方', 'ja': '## レシピ'}
HEADING = {'en': '## Structure', 'zh': '## 结构', 'ja': '## 構造'}
PLACEMENT = {'en': '## Heat source placement', 'zh': '## 热源摆放', 'ja': '## 熱源の配置'}
INTRO = {
    'en': 'Drag the view to turn it. The list gives the blocks and their rules. JEI shows the same plan in the category "Multiblock Structures".',
    'zh': '拖动视图可旋转。下方列表给出所需方块及其规则。JEI 的“多方块结构”分类中也有相同的建造图。',
    'ja': 'ドラッグで視点を回せます。下のリストは必要なブロックとそのルールです。JEI のカテゴリ「マルチブロック構造」にも同じ建造図があります。',
}


def scene(name, zoom):
    return (f'<GameScene zoom="{zoom}" interactive={{true}}>\n'
            f'  <SiStructure id="mio_icif:{name}" />\n'
            f'</GameScene>\n\n'
            f'<SiStructureInfo id="mio_icif:{name}" />\n')


def write(path, text):
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def overview(lang):
    i = ('en', 'zh', 'ja').index(lang)
    title = ('Multiblock Structures', '多方块结构', 'マルチブロック構造')[i]
    lead = (
        'These are the build plans of the multiblock structures and of the machines that need a neighbour block in a fixed '
        'position. The structure checks itself in the world; the plans show a correct build.',
        '这里是多方块结构，以及需要相邻方块固定摆放的机器的建造图。结构在世界中会自行检查；建造图展示正确的搭建方式。',
        'マルチブロック構造と、隣のブロックを決まった位置に置く必要がある機械の建造図です。構造はワールドで自分を検査します。'
        '建造図は正しい組み方を示します。',
    )[i]
    use = (
        'Drag a view to turn it. In JEI, the category "Multiblock Structures" shows the same plans: drag to turn, '
        'scroll to zoom, and click the layer label to see one layer at a time. Look up the uses of a block in JEI to find '
        'the structures that contain it.',
        '拖动视图可旋转。JEI 的“多方块结构”分类显示相同的建造图：拖动旋转、滚轮缩放，点击层标签可逐层查看。'
        '在 JEI 中查看某个方块的用途，即可找到包含它的结构。',
        'ドラッグで視点を回せます。JEI のカテゴリ「マルチブロック構造」にも同じ建造図があります。ドラッグで回転、ホイールで拡大縮小、'
        '層ラベルのクリックで 1 層ずつ表示できます。JEI でブロックの用途を調べると、そのブロックを含む構造が見つかります。',
    )[i]
    addon = ('For addon authors', '附属模组作者', 'アドオン作者向け')[i]
    addon_text = (
        'Register a plan with `StructureBlueprintRegistry.register(...)` (package `core.api.multiblock`). JEI then shows it, '
        'and a page of this manual can show it with `<SiStructure id="mymod:plan" />` inside a `GameScene` and `<SiStructureInfo id="mymod:plan" />`. '
        'See the API guide, section 20.',
        '用 `StructureBlueprintRegistry.register(...)`（包 `core.api.multiblock`）注册建造图后，JEI 会显示它；本手册的页面可在 `GameScene` 中用 '
        '`<SiStructure id="mymod:plan" />`，并用 `<SiStructureInfo id="mymod:plan" />` 显示它。详见 API 指南第 20 节。',
        '`StructureBlueprintRegistry.register(...)`（パッケージ `core.api.multiblock`）で建造図を登録すると JEI に表示されます。このガイドのページでは '
        '`GameScene` の中の `<SiStructure id="mymod:plan" />` と `<SiStructureInfo id="mymod:plan" />` で表示できます。API ガイドの第 20 節を参照してください。',
    )[i]
    see = ('Page', '页面', 'ページ')[i]
    out = ['---', 'navigation:', f'  title: "{title}"', '  icon: mio_icif:producer/block_large_fabricator_core',
           '  parent: heavy.md', '  position: 1', '---', '', f'# {title}', '', lead, '', use, '']
    for name, zoom, titles, page in STRUCTURES:
        out += [f'## {titles[i]}', '', scene(name, zoom).rstrip('\n'), '']
        if page:
            out += [f'{see}: [{titles[i]}]({page}.md)', '']
    out += [f'## {addon}', '', addon_text, '']
    return '\n'.join(out)


def add_sections(lang):
    edited = 0
    pages = {page: name for name, _, _, page in STRUCTURES if page}
    pages.update(EXTRA_PAGES)
    zooms = {name: zoom for name, zoom, _, _ in STRUCTURES}
    for page, name in pages.items():
        path = os.path.join(MANUAL, DIRS[lang], page + '.md')
        text = open(path, encoding='utf-8').read()
        if '<SiStructure ' in text:
            continue
        at = text.rfind('\n' + RECIPE[lang] + '\n')
        assert at >= 0, (path, 'recipe heading')
        heading = PLACEMENT[lang] if name in ('blast_furnace', 'fermenter') else HEADING[lang]
        block = f'\n{heading}\n\n{INTRO[lang]}\n\n{scene(name, zooms[name])}'
        text = text[:at] + block + text[at:]
        write(path, text)
        edited += 1
    return edited


SIM_ANCHOR = {'en': '* No meltdown occurs.\n', 'zh': '* 没有发生堆芯熔毁。\n', 'ja': '* メルトダウンが起きない。\n'}
SIM_TEXT = {
    'en': ('\nThe SAFE check runs the design for a full uranium fuel load (20,000 cycles, 5 h 33 min), not only for the '
           '100 seconds of the values. A design whose vents heat up slowly can look safe for 100 seconds and still melt later; '
           'the result then tells when the components melt. JEI shows tested designs in the category "Reactor Layouts".\n'),
    'zh': ('\n“安全”判定会让设计运行完整的一次铀燃料周期（20,000 个周期，5 小时 33 分），而不只是数值所用的 100 秒。'
           '散热片缓慢升温的设计可能在 100 秒内看似安全，之后才熔化；此时结果会显示组件熔化的时间。'
           'JEI 的“反应堆布局”分类中有经过验证的设计。\n'),
    'ja': ('\n「安全」の判定は、数値の 100 秒だけでなく、ウラン燃料 1 回分（20,000 サイクル、5 時間 33 分）を通して設計を動かします。'
           '放熱板がゆっくり熱をためる設計は、100 秒間は安全に見えても後で溶けることがあります。その場合、結果に部品が溶ける時刻が出ます。'
           'JEI のカテゴリ「原子炉レイアウト」には検証済みの設計があります。\n'),
}


def simulator(lang):
    path = os.path.join(MANUAL, DIRS[lang], 'reactor_simulator.md')
    text = open(path, encoding='utf-8').read()
    if '20,000' in text:
        return 0
    assert text.count(SIM_ANCHOR[lang]) == 1, (path, 'anchor')
    write(path, text.replace(SIM_ANCHOR[lang], SIM_ANCHOR[lang] + SIM_TEXT[lang]))
    return 1


def main():
    for lang in ('en', 'zh', 'ja'):
        path = os.path.join(MANUAL, DIRS[lang], 'multiblock_structures.md')
        new = overview(lang)
        if not os.path.exists(path) or open(path, encoding='utf-8').read() != new:
            write(path, new)
        print(lang, 'overview written; sections added:', add_sections(lang), '; simulator note:', simulator(lang))


if __name__ == '__main__':
    main()
