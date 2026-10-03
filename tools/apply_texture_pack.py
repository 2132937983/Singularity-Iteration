#!/usr/bin/env python3
"""
Applies an externally supplied texture / resource pack (zip or folder) to the mod resources.

Only art is overwritten automatically:
  * assets/<ns>/textures/**.png  whose pixels differ from ours (re-encodes of the same picture are skipped)
  * a .png whose pixels equal an OLDER revision of our own file (git history) is skipped as stale
  * .png.mcmeta animation files that are new or semantically different
  * new textures that do not exist yet
Models, blockstates, lang, sounds and data (recipes, tags, loot tables) are only REPORTED:
they decide behaviour and are reviewed by hand (--models also applies item/block models that
are new files, never overwrites).

Usage: apply_texture_pack.py <pack.zip|dir> <src/main/resources> <report.md> [--dry-run]
"""
import io, json, os, subprocess, sys, tempfile, zipfile, collections
from PIL import Image, ImageChops

PACK, RES, REPORT = sys.argv[1], sys.argv[2], sys.argv[3]
DRY = '--dry-run' in sys.argv

root = PACK
if PACK.endswith('.zip'):
    root = tempfile.mkdtemp()
    zipfile.ZipFile(PACK).extractall(root)
# the pack may be wrapped in one folder (e.g. resources/assets/...)
for cand in [root] + [os.path.join(root, d) for d in os.listdir(root) if os.path.isdir(os.path.join(root, d))]:
    if os.path.isdir(os.path.join(cand, 'assets')):
        root = cand
        break

repo = os.path.abspath(os.path.join(RES, '..', '..', '..'))
revs = subprocess.run(['git', '-C', repo, 'rev-list', '--all'], capture_output=True, text=True).stdout.split()
res_rel = os.path.relpath(os.path.abspath(RES), repo)


def history_pixels_match(rel, img):
    for r in revs:
        o = subprocess.run(['git', '-C', repo, 'show', f'{r}:{res_rel}/{rel}'], capture_output=True)
        if o.returncode: continue
        try:
            b = Image.open(io.BytesIO(o.stdout)).convert('RGBA')
        except Exception:
            continue
        if b.size == img.size and ImageChops.difference(b, img).getbbox() is None:
            return r[:7]
    return None


applied, skipped, report_only = [], collections.defaultdict(list), collections.defaultdict(list)
for d, _, fs in os.walk(root):
    for f in fs:
        src = os.path.join(d, f)
        rel = os.path.relpath(src, root).replace(os.sep, '/')
        dst = os.path.join(RES, rel)
        is_tex = rel.startswith('assets/') and '/textures/' in rel
        if is_tex and rel.endswith('.png'):
            try:
                a = Image.open(src).convert('RGBA')
            except Exception as e:
                skipped['unreadable png in the pack (not applied)'].append(f'{rel} ({str(e)[:60]})'); continue
            if os.path.exists(dst):
                try:
                    b = Image.open(dst).convert('RGBA')
                except Exception:
                    b = Image.new('RGBA', (1, 1))
                if a.size == b.size and ImageChops.difference(a, b).getbbox() is None:
                    skipped['identical pixels'].append(rel); continue
                old = history_pixels_match(rel, a)
                if old:
                    skipped[f'stale (equals our older revision)'].append(f'{rel} ({old})'); continue
                applied.append((rel, f'{b.size[0]}x{b.size[1]} -> {a.size[0]}x{a.size[1]}'))
            else:
                applied.append((rel, f'new {a.size[0]}x{a.size[1]}'))
            if not DRY:
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                open(dst, 'wb').write(open(src, 'rb').read())
        elif is_tex and rel.endswith('.mcmeta'):
            same = os.path.exists(dst) and json.load(open(src, encoding='utf-8-sig')) == json.load(open(dst, encoding='utf-8-sig'))
            if same:
                skipped['identical mcmeta'].append(rel); continue
            applied.append((rel, 'animation meta'))
            if not DRY:
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                open(dst, 'wb').write(open(src, 'rb').read())
        else:
            if not os.path.exists(dst):
                report_only['only in pack'].append(rel); continue
            try:
                same = open(src, 'rb').read() == open(dst, 'rb').read() or (
                    rel.endswith('.json') and json.load(open(src, encoding='utf-8-sig')) == json.load(open(dst, encoding='utf-8-sig')))
            except Exception:
                same = False
            if not same:
                kind = rel.split('/')[2] if rel.startswith(('assets/', 'data/')) and len(rel.split('/')) > 2 else rel
                report_only[f'differs, not applied: {rel.split("/")[0]}/{kind}'].append(rel)

with open(REPORT, 'w', encoding='utf-8') as out:
    out.write(f'# Texture pack application\n\npack: `{os.path.basename(PACK)}`{"  (dry run)" if DRY else ""}\n\n')
    out.write(f'## Applied ({len(applied)})\n\n')
    for rel, note in sorted(applied): out.write(f'- `{rel}` - {note}\n')
    for k, v in skipped.items():
        out.write(f'\n## Skipped: {k} ({len(v)})\n\n')
        for rel in sorted(v)[:400]: out.write(f'- `{rel}`\n')
    for k, v in sorted(report_only.items()):
        out.write(f'\n## {k} ({len(v)})\n\n')
        for rel in sorted(v)[:60]: out.write(f'- `{rel}`\n')
        if len(v) > 60: out.write(f'- ... {len(v) - 60} more\n')
print('applied', len(applied), '| skipped', {k: len(v) for k, v in skipped.items()}, '| report-only', {k: len(v) for k, v in report_only.items()})
