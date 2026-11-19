"""Temporary helper: remove unused imports from Java sources.

Usage:
    python tools/_tmp_scan_warnings.py          # dry run
    python tools/_tmp_scan_warnings.py --apply  # rewrite files
"""
import os
import re
import sys

ROOT_DIRS = [
    r"e:/1.21NeoForge ModDev/MDK-1.21.1-ModDevGradle-main/src"
]

IMPORT_RE = re.compile(r'^\s*import\s+(static\s+)?([\w\.\*]+)\s*;\s*$')


def iter_java_files():
    for root_dir in ROOT_DIRS:
        if not os.path.isdir(root_dir):
            continue
        for base, _dirs, files in os.walk(root_dir):
            for name in files:
                if name.endswith(".java"):
                    yield os.path.join(base, name)


def collapse_header_blanks(lines, header_end):
    """Collapse runs of consecutive blank lines in the package/import header."""
    result = []
    for i, line in enumerate(lines):
        if i <= header_end and not line.strip() and result and not result[-1].strip():
            continue
        result.append(line)
    return result


def main():
    dry = "--apply" not in sys.argv
    files_with_unused = 0
    total_unused = 0
    changed = 0

    for path in iter_java_files():
        with open(path, encoding="utf-8") as handle:
            lines = handle.readlines()

        import_indices = [i for i, line in enumerate(lines) if IMPORT_RE.match(line)]
        if not import_indices:
            continue

        import_set = set(import_indices)
        body = "".join(line for i, line in enumerate(lines) if i not in import_set)

        to_remove = []
        for i in import_indices:
            m = IMPORT_RE.match(lines[i])
            fq = m.group(2)
            if fq.endswith(".*"):
                continue
            simple = fq.split(".")[-1]
            if not re.search(r"\b" + re.escape(simple) + r"\b", body):
                to_remove.append(i)

        if not to_remove:
            continue

        files_with_unused += 1
        total_unused += len(to_remove)
        rel = os.path.relpath(path, ROOT_DIRS[0]).replace(os.sep, "/")
        print(f"[{rel}]")
        for i in to_remove:
            print(f"    - {lines[i].strip()}")

        if not dry:
            remove_set = set(to_remove)
            kept = [line for i, line in enumerate(lines) if i not in remove_set]
            kept_import_idxs = [i for i, line in enumerate(kept) if IMPORT_RE.match(line)]
            header_end = kept_import_idxs[-1] if kept_import_idxs else 0
            while header_end + 1 < len(kept) and kept[header_end + 1].strip():
                header_end += 1
            kept = collapse_header_blanks(kept, header_end)
            with open(path, "w", encoding="utf-8", newline="") as handle:
                handle.writelines(kept)
            changed += 1

    print("----")
    print(f"files with unused imports: {files_with_unused}")
    print(f"total unused imports    : {total_unused}")
    print(f"files rewritten         : {changed}")


if __name__ == "__main__":
    main()
