"""Check shipped pipe resources, including every multipart state and recipe material."""
import itertools
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources"
ASSETS = ROOT / "assets/mio_icif"
DATA = ROOT / "data/mio_icif"
SIDES = ("down", "up", "north", "south", "west", "east")


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


for grade, material in (("bronze", "bronze"), ("steel", "adviron")):
    parts = read(ASSETS / f"blockstates/pipe/fluid_pipe_{grade}.json")["multipart"]
    assert len(parts) == 28
    drops = read(DATA / f"loot_table/blocks/pipe/fluid_pipe_{grade}.json")["pools"][0]["entries"]
    for size, pixels, count in (("tiny", 4, 6), ("small", 6, 3), ("medium", 8, 2), ("large", 10, 1)):
        path = f"pipe/fluid_pipe_{grade}_{size}"
        for faces in itertools.product(("true", "false"), repeat=6):
            state = dict(zip(SIDES, faces), size=size)
            active = [p for p in parts if all(state[k] == v for k, v in p["when"].items())]
            assert len(active) == 1 + faces.count("true")
            for part in active:
                model = read(ASSETS / ("models/" + part["apply"]["model"].split(":")[1] + ".json"))
                for texture in model["textures"].values():
                    assert (ASSETS / ("textures/" + texture.split(":")[1] + ".png")).is_file()
                assert all(f["tintindex"] == 0 for element in model["elements"] for f in element["faces"].values())
        center = read(ASSETS / f"models/block/pipe/fluid_{size}_center.json")["elements"][0]
        assert all(high - low == pixels for low, high in zip(center["from"], center["to"]))
        read(ASSETS / f"models/item/{path}.json")
        recipe = read(DATA / f"recipe/{path}.json")
        expected = {"item": f"mio_icif:resource/item_{material}_casing"} if size in ("tiny", "small") else {"tag": f"c:plates/{material}"}
        assert recipe["key"]["C"] == expected and recipe["result"] == {"id": "mio_icif:" + path, "count": count}
        assert sum(row.count("C") for row in recipe["pattern"]) == 6
        assert [entry["name"] for entry in drops if entry["conditions"][0]["properties"]["size"] == size] == ["mio_icif:" + path]
        unlock = read(DATA / f"advancement/recipes/{path}.json")
        assert unlock["rewards"]["recipes"] == ["mio_icif:" + path]
        for lang in ("zh_cn", "en_us"):
            assert f"item.mio_icif.{path.replace('/', '.')}" in read(ASSETS / f"lang/{lang}.json")

print("PASS: all 512 multipart states, 8 recipes/loot/unlocks/translations and model texture references.")
