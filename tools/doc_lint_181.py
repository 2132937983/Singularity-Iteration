#!/usr/bin/env python3
"""
0.1.8.1: trilingual comment lint (EN / ZH / JA). See docs/dev/COMMENT_STYLE.md.

The manifest docs/dev/trilingual_manifest.txt lists the files in scope, one per line:
  <path relative to src/main/java>            the type comment must be trilingual
  <path relative to src/main/java>  all       every Javadoc block must be trilingual

Checks (errors, exit code 1):
  * the type comment (or every Javadoc block in "all" mode) has EN:, ZH: and JA:, in this order;
  * the ZH part and the JA part contain CJK characters;
  * each "// EN:" block comment has "// ZH:" and "// JA:" in the next lines.
Checks (warnings only), on the English part, as an aid for ASD-STE100:
  * a sentence longer than 25 words;
  * a word from the list NON_STE (words that ASD-STE100 does not approve, or vague words).

Usage: doc_lint_181.py <repo root>
"""
import os
import re
import sys

NON_STE = ("utilize", "utilise", "leverage", "simply", "obviously", "basically", "etc.", "and/or",
           "in order to", "please note", "it should be noted")
CJK = re.compile(r'[぀-ヿ㐀-鿿＀-￯]')
TYPE_DECL = re.compile(r'^(?:@\w+(?:\([^)]*\))?\s+)*(?:public\s+|protected\s+)?(?:abstract\s+|final\s+|sealed\s+)*'
                       r'(?:class|interface|record|enum|@interface)\s+\w+', re.M)


def javadocs(text):
    return [(m.start(), m.group(0)) for m in re.finditer(r'/\*\*.*?\*/', text, re.S)]


def check_block(block):
    errors = []
    i_en, i_zh, i_ja = block.find('EN:'), block.find('ZH:'), block.find('JA:')
    if min(i_en, i_zh, i_ja) < 0:
        missing = [n for n, i in (('EN', i_en), ('ZH', i_zh), ('JA', i_ja)) if i < 0]
        return ['missing ' + '/'.join(missing)], ''
    if not (i_en < i_zh < i_ja):
        errors.append('order must be EN, ZH, JA')
    zh, ja = block[i_zh:i_ja], block[i_ja:]
    if not CJK.search(zh):
        errors.append('ZH part has no CJK text')
    if not CJK.search(ja):
        errors.append('JA part has no CJK text')
    return errors, block[i_en + 3:i_zh]


def english_warnings(en):
    text = re.sub(r'\{@\w+\s+([^}]*)\}', r'\1', en)
    text = re.sub(r'<[^>]+>', ' ', text)
    text = re.sub(r'\s*\*\s*', ' ', text)
    warnings = []
    for sentence in re.split(r'(?<=[.;:])\s+', text):
        words = [w for w in sentence.split() if re.search(r'[A-Za-z]', w)]
        if len(words) > 25:
            warnings.append('long sentence (%d words): %s...' % (len(words), ' '.join(words[:8])))
    low = text.lower()
    for word in NON_STE:
        if re.search(r'(?<![a-z])' + re.escape(word) + r'(?![a-z])', low):
            warnings.append('non-STE word: ' + word)
    return warnings


def main():
    root = sys.argv[1]
    src = os.path.join(root, 'src', 'main', 'java')
    manifest = os.path.join(root, 'docs', 'dev', 'trilingual_manifest.txt')
    errors = warnings = files = blocks_checked = 0
    for raw in open(manifest, encoding='utf-8'):
        line = raw.split('#', 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        rel, mode = parts[0], (parts[1] if len(parts) > 1 else 'type')
        path = os.path.join(src, rel)
        if not os.path.exists(path):
            print('ERROR', rel, ': file not found')
            errors += 1
            continue
        files += 1
        text = open(path, encoding='utf-8').read()
        docs = javadocs(text)
        if mode == 'all':
            targets = docs
        else:
            decl = TYPE_DECL.search(text) if not rel.endswith('package-info.java') else re.search(r'^package\s', text, re.M)
            before = [d for d in docs if decl and d[0] < decl.start()]
            targets = before[-1:] if before else []
            if not targets:
                print('ERROR', rel, ': no type comment')
                errors += 1
        for pos, block in targets:
            blocks_checked += 1
            line_no = text.count('\n', 0, pos) + 1
            errs, en = check_block(block)
            for e in errs:
                print('ERROR %s:%d: %s' % (rel, line_no, e))
                errors += 1
            for w in english_warnings(en):
                print('warn  %s:%d: %s' % (rel, line_no, w))
                warnings += 1
        lines = text.split('\n')
        for i, ln in enumerate(lines):
            if '// EN:' in ln:
                window = '\n'.join(lines[i + 1:i + 10])
                if '// ZH:' not in window or '// JA:' not in window:
                    print('ERROR %s:%d: "// EN:" without "// ZH:" and "// JA:"' % (rel, i + 1))
                    errors += 1
    print('files %d, javadoc blocks %d, errors %d, warnings %d' % (files, blocks_checked, errors, warnings))
    sys.exit(1 if errors else 0)


if __name__ == '__main__':
    main()
