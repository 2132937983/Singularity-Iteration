#!/usr/bin/env python3
"""
0.1.8.1: machine sound audit.

The script reads the source tree. It does not start the game.

For each block that has a block entity, the script finds:
  * the run state that the client can see:
      - a block-state property (active, lit, working, running);
      - "lamp": the block entity extends a prefab that reports a RUNNING lamp state
        (AbstractProcessingMachineBlockEntity or AbstractGeneratorBlockEntity, or a class that
        overrides machineStatus()); MachineStatusTracker syncs this state to the client;
      - "client": the block entity implements ClientRunState (synced flag);
  * the loop profile in MachineSoundManager (map(...) calls);
  * the silent entries in MachineSoundManager (silent("reason", ...) calls);
  * whether the block entity extends AbstractProcessingMachineBlockEntity (completion cue).

Status values:
  OK          the block has a run state and a loop profile
  SILENT      MachineSoundManager marks the block silent on purpose (reason in the report)
  NO_PROFILE  the block has a run state, but no loop profile and no silent entry
  NO_STATE    the block has a loop profile, but the client cannot see a run state
  PASSIVE     no run state and no profile (cables, tanks, storage and other passive blocks)

Exit code 1 when a NO_PROFILE or NO_STATE row exists, so a build step can use the script.

Usage: sound_audit_181.py <repo root> <report.md>
"""
import json
import os
import re
import sys

RUN_PROPS = ("active", "lit", "working", "running")
LAMP_PREFABS = ("AbstractProcessingMachineBlockEntity", "AbstractGeneratorBlockEntity")


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def java_files(root):
    for base, _, files in os.walk(os.path.join(root, "src", "main", "java")):
        for name in files:
            if name.endswith(".java"):
                yield os.path.join(base, name)


def main():
    root, out = sys.argv[1], sys.argv[2]
    sources = {p: read(p) for p in java_files(root)}
    by_class = {os.path.splitext(os.path.basename(p))[0]: t for p, t in sources.items()}

    # 1. Block registrations: (Class, FIELD) -> registry path
    reg = re.compile(r'(\w+)\s*=\s*(?:\w+\.)?(?:BLOCKS\.register|registerBlock|registerBlockNoItem)\(\s*"([^"]+)"')
    field_path = {}
    for p, text in sources.items():
        cls = os.path.splitext(os.path.basename(p))[0]
        for m in reg.finditer(text):
            field_path[(cls, m.group(1))] = m.group(2)

    # 2. Block entity types: the blocks in each Builder.of(...) call, and the block entity class
    be_blocks = {}
    for p, text in sources.items():
        cls = os.path.splitext(os.path.basename(p))[0]
        for m in re.finditer(r'BlockEntityType\.Builder\.(?:<[^>]*>)?of\((.*?)\)\s*\.build\(', text, re.S):
            body = m.group(1)
            ctor = re.search(r'new\s+([\w.]+)\s*\(', body) or re.search(r'([\w.]+)::new', body)
            be_class = ctor.group(1).split(".")[-1] if ctor else "?"
            refs = [(r.group(1), r.group(2)) for r in re.finditer(r'(\w+)\.(\w+)\.get\(\)', body)]
            refs += [(cls, r.group(1)) for r in re.finditer(r'(?<![\w.])([A-Z][A-Z0-9_]*)\.get\(\)', body)]
            for ref in refs:
                path = field_path.get(ref)
                if path:
                    be_blocks[path] = be_class

    # 3. Loop profiles and silent entries in MachineSoundManager
    msm = by_class["MachineSoundManager"]
    profile, silent = {}, {}
    for m in re.finditer(r'map\(\s*((?:new\s+)?\w+\([^)]*\)|\w+)\s*,\s*((?:"[^"]+"\s*,?\s*)+)\)', msm):
        name = re.sub(r'^(?:new\s+)?\w+\(mio_icif_sounds\.(\w+)(?:::get)?\s*,\s*([\d.]+)F?.*', r'\1 @\2', m.group(1).strip())
        for path in re.findall(r'"([^"]+)"', m.group(2)):
            profile[path] = name
    for m in re.finditer(r'silent\(\s*"([^"]+)"\s*,\s*((?:"[^"]+"\s*,?\s*)+)\)', msm):
        for path in re.findall(r'"([^"]+)"', m.group(2)):
            silent[path] = m.group(1)

    # 4. Run-state property from the block-state files
    bs_dir = os.path.join(root, "src", "main", "resources", "assets", "mio_icif", "blockstates")

    def props(path):
        f = os.path.join(bs_dir, path + ".json")
        if not os.path.exists(f):
            return set()
        data = json.loads(read(f))
        names = set()
        for key in data.get("variants", {}):
            for part in key.split(","):
                if "=" in part:
                    names.add(part.split("=")[0])
        for part in data.get("multipart", []):
            when = part.get("when", {})
            for cond in when.get("OR", [when]):
                names.update(k for k in cond if k != "OR")
        return names

    # 5. Class chain helpers
    def chain(be_class):
        seen = []
        cls = be_class
        while cls and cls not in seen:
            seen.append(cls)
            text = by_class.get(cls)
            if text is None:
                break
            m = re.search(r'class\s+' + cls + r'\b[^{]*?extends\s+(\w+)', text)
            cls = m.group(1) if m else None
        return seen

    def has_lamp(be_class):
        for cls in chain(be_class):
            if cls in LAMP_PREFABS:
                return True
            text = by_class.get(cls, "")
            if cls != "AbstractEnergyBlockEntity" and re.search(r'MachineStatus\s+machineStatus\s*\(', text):
                return True
        return False

    def client_state(be_class):
        return any(re.search(r'implements[^{]*\bClientRunState\b', by_class.get(c, "")) for c in chain(be_class))

    rows = []
    for path in sorted(be_blocks):
        be = be_blocks[path]
        names = props(path)
        run = next((r for r in RUN_PROPS if r in names), None)
        if run is None and client_state(be):
            run = "client"
        if run is None and has_lamp(be):
            run = "lamp"
        prof = profile.get(path)
        if path in silent:
            status, prof = "SILENT", "silent: " + silent[path]
        elif run and prof:
            status = "OK"
        elif run:
            status = "NO_PROFILE"
        elif prof:
            status = "NO_STATE"
        else:
            status = "PASSIVE"
        cue = "yes" if "AbstractProcessingMachineBlockEntity" in chain(be) else "no"
        rows.append((status, path, be, run or "-", prof or "-", cue))

    order = {"NO_STATE": 0, "NO_PROFILE": 1, "OK": 2, "SILENT": 3, "PASSIVE": 4}
    rows.sort(key=lambda r: (order[r[0]], r[1]))
    counts = {}
    for r in rows:
        counts[r[0]] = counts.get(r[0], 0) + 1

    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write("# Machine sound audit\n\n")
        f.write("Generated by `tools/sound_audit_181.py` from the source tree (no game run).\n\n")
        f.write("Run state: block-state property, `lamp` (synced status lamp), `client` (ClientRunState) or `-`.\n\n")
        f.write("| Status | Count |\n|---|---|\n")
        for k in sorted(counts, key=lambda k: order[k]):
            f.write(f"| {k} | {counts[k]} |\n")
        f.write("\n| Status | Block | Block entity | Run state | Loop profile | Completion cue |\n")
        f.write("|---|---|---|---|---|---|\n")
        for r in rows:
            f.write("| " + " | ".join(r) + " |\n")
    for k in sorted(counts, key=lambda k: order[k]):
        print(f"{k}: {counts[k]}")
    sys.exit(1 if counts.get("NO_PROFILE") or counts.get("NO_STATE") else 0)


if __name__ == "__main__":
    main()
