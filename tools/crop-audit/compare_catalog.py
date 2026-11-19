"""Read-only crop property comparison against the local IC2 2.8.221 source.

Prints Markdown. Compares constructor tuples, not gameplay behavior or translations.
Run from any directory: python tools/crop-audit/compare_catalog.py
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
CURRENT = ROOT / "src/main/java/com/miophas/singularity_iteration/common"
REFERENCE = ROOT / "source/IC2Code 1.12.2/ic2/core/crop"


def read(path):
    return path.read_text(encoding="utf-8")


def properties(text, constructor):
    match = re.search(r"new " + constructor + r"\((\s*\d+(?:\s*,\s*\d+){5})\)", text)
    if not match:
        raise ValueError("Missing literal properties: " + constructor)
    return tuple(map(int, match[1].split(",")))


def compare():
    registration = read(CURRENT / "event/CropRegistryEvent.java")
    original = read(REFERENCE / "IC2Crops.java")
    declared = re.search(r"ZERO_ARG_PLANTS\s*=\s*\{(.*?)\};", registration, re.S)[1]
    classes = re.findall(r'"(Plant\w+)"', declared) + ["PlantEatingPlant"]
    mapping = {"Redwheat": "RedWheat", "EatingPlant": "Eating"}
    for names, base in [
        ("Aurelia Shining", "BaseMetalUncommon"),
        ("Blackthorn Cyazint Dandelion Rose Tulip", "ColorFlower"),
        ("BrownMushroom RedMushroom", "BaseMushroom"),
        ("Cyprium Ferru Plumbiscus Stagnium", "BaseMetalCommon"),
    ]:
        mapping.update(dict.fromkeys(names.split(), base))
    rows = []
    extensions = []
    for name in classes:
        current = read(CURRENT / "crop" / (name + ".java"))
        crop_id = re.search(r'getTypeId\(\)\s*\{\s*return "([^"]+)"', current)[1]
        reference = REFERENCE / "cropcard" / ("Crop" + mapping.get(name[5:], name[5:]) + ".java")
        if not reference.exists():
            extensions.append(crop_id)
            continue
        rows.append((crop_id, properties(current, "PlantStats"), properties(read(reference), "CropProperties")))

    configured = registration.split("private static List<PlantType> configuredPlants()", 1)[1]
    configured = configured.split("private static PlantGenericCrop generic", 1)[0]
    for match in re.finditer(r'generic\("([^"]+)"[^\n]*?s\(([^)]+)\)', configured):
        crop_id = match[1]
        reference = re.search(r'GenericCropCard.create\("' + re.escape(crop_id) + r'"\)(.*?)\.register\(\)', original, re.S)
        if not reference:
            raise ValueError("Missing generic IC2 crop: " + crop_id)
        rows.append((crop_id, tuple(map(int, match[2].split(","))), properties(reference[1], "CropProperties")))

    for crop_id in re.findall(r'sapling\("([^"]+)"', configured):
        rows.append((crop_id, properties(read(CURRENT / "crop/PlantBaseSapling.java"), "PlantStats"),
                     properties(read(REFERENCE / "cropcard/CropBaseSapling.java"), "CropProperties")))
    if len(rows) != len({row[0] for row in rows}):
        raise ValueError("Duplicate catalog entries")
    return sorted(rows), sorted(extensions)


if __name__ == "__main__":
    rows, extensions = compare()
    mismatches = sum(current != original for _, current, original in rows)
    print("# Crop property tuple audit\n")
    print(f"Compared: {len(rows)}; differing tuples: {mismatches}; identical tuples: {len(rows) - mismatches}.\n")
    print("SI-only crops: " + ", ".join(extensions) + ".\n")
    print("Tuple order is constructor argument order, NOT proof of equivalent field semantics or behavior.\n")
    print("| Crop ID | Current tuple | IC2 tuple | Numeric match |")
    print("| --- | --- | --- | --- |")
    for crop_id, current, original in rows:
        print(f"| {crop_id} | {current} | {original} | {'yes' if current == original else 'NO'} |")
