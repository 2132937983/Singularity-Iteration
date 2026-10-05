#!/usr/bin/env python3
"""
STE linter for the SI manual (0.1.7.33). Mechanical checks modelled on ASD-STE100 and on the
Chinese adaptation asd-ste100-skill-zh (lemonhall): sentence length, passive voice, hollow verbs,
marketing words, empty phrases, vague words, semicolons, one-term-per-concept.

The linter never flags modality (may / 可能 / かもしれない): confidence is content, not style.

Usage:
  ste_lint.py tools/guide_data/*.json          # guide data files (function / usage / notes lists)
  ste_lint.py --md assets/.../manual            # generated markdown pages (all languages)
Exit code 1 when a hard rule fails.
"""
import json, os, re, sys

# ------------------------------------------------------------------ limits
LIMITS = {
    # (instruction, description)
    'en': (20, 25),      # words
    'zh': (25, 40),      # characters (CJK + alnum, punctuation excluded)
    'ja': (40, 60),      # characters
}

EN_PASSIVE = re.compile(r"\b(is|are|was|were|be|been|being|gets|got)\s+(\w+ly\s+)?(\w+ed|built|made|done|given|taken|shown|sent|set|kept|held|put|cut|found|lost|seen|known|thrown|broken|chosen|drawn|driven|eaten|fallen|forgotten|frozen|hidden|ridden|risen|shaken|spoken|stolen|stuck|sworn|torn|worn|woken|written)\b", re.I)
EN_BAN = ['seamless', 'powerful', 'robust', 'cutting-edge', 'state-of-the-art', 'revolutionary', 'amazing', 'awesome',
          'leverage', 'utilize', 'in order to', 'please note', 'it should be noted', 'basically', 'simply', 'just ',
          'etc.', 'and so on', 'various', 'several', 'some of', "don't", "can't", "won't", "it's", "isn't", "doesn't", ';']
EN_ALLOW_PASSIVE = ['is connected', 'are connected', 'is full', 'is empty', 'is installed', 'are installed', 'is required', 'is enabled', 'is disabled', 'is loaded']

ZH_PASSIVE = re.compile(r"被")
ZH_HOLLOW = re.compile(r"(进行|加以|予以|给予)[一-鿿]{1,4}")
ZH_BAN = ['无缝', '强大', '赋能', '闭环', '极致', '完美', '革命性', '需要注意的是', '值得一提的是', '在一定程度上', '众所周知',
          '若干', '一些', '相关', '等等', '；', ';']
JA_PASSIVE = re.compile(r"(さ|ら)れ(る|た|ます|ない|て)")
JA_BAN = ['シームレス', '強力な', '圧倒的', '革命的', '注意が必要です', 'いくつかの', 'など', '等', '；', ';', 'させていただ']
JA_PASSIVE_ALLOW = ['呼ばれ', '含まれ', '限られ', '置かれ']   # stative / lexical

# one term per concept: (preferred, [forbidden synonyms])
TERMS = {
    'en': [('cable', ['wire ', 'wires ']), ('storage block', ['energy storage unit', 'battery block']),
           ('voltage tier', ['voltage level', 'voltage class']), ('EU/t', ['eu per tick'])],
    'zh': [('导线', ['电线', '电缆']), ('电压等级', ['电压级别', '电压层级']), ('机器', ['机械']), ('槽位', ['格子', '插槽'])],
    'ja': [('ケーブル', ['ワイヤー', '電線']), ('電圧Tier', ['電圧レベル', '電圧等級', '電圧ティア']), ('スロット', ['枠'])],
}


def sentences(text, lang):
    text = re.sub(r'`[^`]*`', 'X', text)
    text = re.sub(r'<[^>]+>', ' ', text)
    text = re.sub(r'\[([^\]]*)\]\([^)]*\)', r'\1', text)
    if lang == 'en':
        parts = re.split(r'(?<=[.!?])\s+', text.strip())
    else:
        parts = re.split(r'(?<=[。！？])', text.strip())
    return [p.strip() for p in parts if p.strip()]


def length(s, lang):
    if lang == 'en':
        return len([w for w in re.split(r'\s+', s) if re.search(r'[A-Za-z0-9]', w)])
    return len(re.findall(r'[぀-ヿ一-鿿A-Za-z0-9]', s))


def check_sentence(s, lang, instruction):
    problems = []
    limit = LIMITS[lang][0 if instruction else 1]
    n = length(s, lang)
    if n > limit:
        problems.append('length %d > %d' % (n, limit))
    low = s.lower()
    if lang == 'en':
        m = EN_PASSIVE.search(s)
        if m and not any(a in low for a in EN_ALLOW_PASSIVE):
            problems.append('passive "%s"' % m.group(0))
        for b in EN_BAN:
            if b in low:
                problems.append('banned "%s"' % b.strip())
    elif lang == 'zh':
        if ZH_PASSIVE.search(s):
            problems.append('passive 被')
        m = ZH_HOLLOW.search(s)
        if m:
            problems.append('hollow verb "%s"' % m.group(0))
        for b in ZH_BAN:
            if b in s:
                problems.append('banned "%s"' % b)
    else:
        m = JA_PASSIVE.search(s)
        if m and not any(a in s for a in JA_PASSIVE_ALLOW):
            problems.append('passive "%s"' % m.group(0))
        for b in JA_BAN:
            if b in s:
                problems.append('banned "%s"' % b)
    scan = (low if lang == 'en' else s).replace('ワイヤーフレーム', '')
    for good, bad in TERMS[lang]:
        for w in bad:
            if w in scan:
                problems.append('term "%s" -> "%s"' % (w.strip(), good))
    return problems


def lint_data(path):
    data = json.load(open(path, encoding='utf-8'))
    fails = 0
    for key, entry in data.items():
        if key.startswith('_'):
            continue
        for field, instruction in [('function', False), ('usage', True), ('notes', False)]:
            block = entry.get(field)
            if not block:
                continue
            for lang in ('en', 'zh', 'ja'):
                for line in block.get(lang, []):
                    for s in sentences(line, lang):
                        for p in check_sentence(s, lang, instruction):
                            fails += 1
                            print('%s: %s %s.%s: %s  <<%s>>' % (os.path.basename(path), key, field, lang, p, s))
            langs = [len(block.get(l, [])) for l in ('en', 'zh', 'ja')]
            if len(set(langs)) > 1:
                fails += 1
                print('%s: %s %s: line count differs between languages %s' % (os.path.basename(path), key, field, langs))
    return fails


def lint_md(root):
    fails = 0
    for dirpath, _, files in os.walk(root):
        rel = os.path.relpath(dirpath, root)
        lang = 'zh' if rel.startswith('_zh_cn') else 'ja' if rel.startswith('_ja_jp') else 'en'
        for f in files:
            if not f.endswith('.md'):
                continue
            text = open(os.path.join(dirpath, f), encoding='utf-8').read()
            body = re.sub(r'^---.*?---', '', text, flags=re.S)
            for line in body.splitlines():
                t = line.strip()
                if not t or t.startswith(('#', '|', '<', '!', '```')):
                    continue
                instruction = bool(re.match(r'^\d+\.\s', t))
                t = re.sub(r'^(\d+\.|[-*])\s+', '', t)
                for s in sentences(t, lang):
                    for p in check_sentence(s, lang, instruction):
                        fails += 1
                        print('%s: %s  <<%s>>' % (os.path.join(rel, f), p, s))
    return fails


def main():
    args = sys.argv[1:]
    fails = 0
    if args and args[0] == '--md':
        for root in args[1:]:
            fails += lint_md(root)
    else:
        for p in args:
            fails += lint_data(p)
    print('STE lint: %d problem(s)' % fails)
    sys.exit(1 if fails else 0)


if __name__ == '__main__':
    main()
