#!/usr/bin/env python3
"""0.1.10.16 language files: the missing Japanese strings (about 1,850 keys), the missing English
names of the titanium / niobium items and blocks and a few other blocks (with zh_cn / ru_ru where
they were missing too), and one stale ru_ru key. The strings are in tools/lang_data/lang_1116.json.

After applying, every language is checked against en_us:
  - missing keys (in en_us, not in the language) and stale keys (in the language, not in en_us);
  - format placeholders: %s, %d, %1$s ... and %% must match en_us (positional and sequential
    arguments are compared by index, so "%2$s ... %1$s" matches "%s ... %s").
Exit status 1 when ja_jp misses a key, or when a key written by this tool has a placeholder
mismatch. Other mismatches are listed as warnings.
Idempotent; keeps the line endings of each file.
Usage: lang_1116.py <repo root>            apply, then check
       lang_1116.py <repo root> --check    check only
"""
import json
import os
import re
import sys
from collections import OrderedDict

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
CHECK_ONLY = '--check' in sys.argv[2:]
LANG = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/lang')
DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lang_data', 'lang_1116.json')
LANGS = ('en_us', 'zh_cn', 'ja_jp', 'ru_ru')

FORMAT = re.compile(r'%(?:(\d+)\$)?([A-Za-z%])')


def signature(text):
    """Arguments as sorted (index, conversion) pairs, plus the number of %% and of stray %."""
    args, literal, seq = [], 0, 0
    for m in FORMAT.finditer(text):
        if m.group(2) == '%':
            literal += 1
            continue
        if m.group(1):
            index = int(m.group(1))
        else:
            seq += 1
            index = seq
        args.append((index, m.group(2)))
    stray = text.count('%') - 2 * literal - len(args)
    return sorted(args), literal, stray


def load(lang):
    path = os.path.join(LANG, lang + '.json')
    raw = open(path, 'rb').read()
    eol = '\r\n' if b'\r\n' in raw else '\n'
    return path, eol, json.loads(raw.decode('utf-8-sig'), object_pairs_hook=OrderedDict)


def save(path, eol, data):
    text = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
    with open(path, 'wb') as f:
        f.write(text.replace('\n', eol).encode('utf-8'))


def apply(changes):
    for lang in LANGS:
        path, eol, data = load(lang)
        before = dict(data)
        for key, value in changes['set'].get(lang, {}).items():
            data[key] = value
        removed = 0
        for key in changes['remove'].get(lang, []):
            if data.pop(key, None) is not None:
                removed += 1
        if dict(data) != before or list(data) != list(before):
            save(path, eol, data)
        print(f'{lang}: {len(changes["set"].get(lang, {}))} keys set, {removed} stale keys removed')


def check(changes):
    files = {lang: load(lang)[2] for lang in LANGS}
    en = files['en_us']
    errors = 0
    for lang in LANGS[1:]:
        data = files[lang]
        written = set(changes['set'].get(lang, {}))
        missing = [k for k in en if k not in data]
        stale = [k for k in data if k not in en]
        bad = [k for k in data if k in en and signature(data[k]) != signature(en[k])]
        print(f'{lang}: {len(data)} keys, {len(missing)} missing, {len(stale)} stale, '
              f'{len(bad)} placeholder mismatches')
        for k in stale:
            print(f'    stale: {k}')
        for k in bad:
            own = k in written
            errors += own
            print(f'    {"ERROR" if own else "warning"} placeholders: {k}: en={en[k]!r} {lang}={data[k]!r}')
        if lang == 'ja_jp':
            errors += len(missing)
            for k in missing:
                print(f'    ERROR missing: {k}')
    return errors


def main():
    changes = json.load(open(DATA, encoding='utf-8'))
    if not CHECK_ONLY:
        apply(changes)
    errors = check(changes)
    print('lang check:', 'OK' if not errors else f'{errors} error(s)')
    sys.exit(1 if errors else 0)


if __name__ == '__main__':
    main()
