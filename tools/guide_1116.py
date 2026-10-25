#!/usr/bin/env python3
"""0.1.10.16 guide page titles. gen_guide.py took the title of a machine page from the language
file and fell back to the English name (or to the raw id) when the name was missing. With the
names added in 0.1.10.16 (tools/lang_1116.py), a page for one item (or for the item of its icon)
gets that item's name as its navigation title and H1 when:
  - ja_jp: the title is still ASCII (the English fallback) and the Japanese name is not ASCII;
  - any language: the title is the raw id (for example "block_superconducting_cable").
Only the navigation title line and the H1 line that carry the old title are changed.
Idempotent. Run tools/lang_1116.py first.
Usage: guide_1116.py <repo root>
"""
import json
import os
import re
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
RES = os.path.join(ROOT, 'src/main/resources/assets/mio_icif')
MANUAL = os.path.join(RES, 'guides/mio_icif/manual')
DIRS = {'en_us': MANUAL, 'zh_cn': os.path.join(MANUAL, '_zh_cn'), 'ja_jp': os.path.join(MANUAL, '_ja_jp')}

TITLE = re.compile(r'^  title: "([^"]*)"$', re.M)
ITEM_ID = re.compile(r'^  - mio_icif:([a-z0-9_/]+)$', re.M)
ICON = re.compile(r'^  icon: mio_icif:([a-z0-9_/]+)$', re.M)


def pages(lang):
    for dp, dns, fns in os.walk(DIRS[lang]):
        if lang == 'en_us':
            dns[:] = [d for d in dns if not d.startswith('_')]
        for f in sorted(fns):
            if f.endswith('.md'):
                yield os.path.join(dp, f)


def main():
    names = {lang: json.load(open(os.path.join(RES, 'lang', lang + '.json'), encoding='utf-8-sig')) for lang in DIRS}
    for lang in DIRS:
        changed = 0
        for path in pages(lang):
            raw = open(path, 'rb').read()
            eol = '\r\n' if b'\r\n' in raw else '\n'
            text = raw.decode('utf-8').replace('\r\n', '\n')
            if not text.startswith('---\n'):
                continue
            head = text.split('\n---\n', 1)[0]
            title = TITLE.search(head)
            ids = ITEM_ID.findall(head)
            icon = ICON.search(head)
            if icon and icon.group(1) in ids:
                ids = [icon.group(1)]
            if not title or len(ids) != 1:
                continue
            old = title.group(1)
            new = None
            for pre in ('block.mio_icif.', 'item.mio_icif.'):
                new = new or names[lang].get(pre + ids[0].replace('/', '.'))
            if not new or new == old or any(c in new for c in '"<>{}'):
                continue
            raw_id = old == ids[0].split('/')[-1]
            fallback = lang == 'ja_jp' and old.isascii() and not new.isascii()
            if not (raw_id or fallback):
                continue
            text = text.replace('  title: "%s"\n' % old, '  title: "%s"\n' % new, 1)
            text = text.replace('\n# %s\n' % old, '\n# %s\n' % new, 1)
            with open(path, 'wb') as f:
                f.write(text.replace('\n', eol).encode('utf-8'))
            changed += 1
        print(f'{lang}: {changed} page titles set')


if __name__ == '__main__':
    main()
