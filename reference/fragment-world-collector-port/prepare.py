"""Extract pinned recipe tables and unchanged local GTLCore textures."""
import hashlib
import json
from pathlib import Path
import re
import shutil
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
REFERENCE = Path(__file__).resolve().parent
PIN = "6443c848264fb15c90cf50e031f146522616a8b9"
SOURCE = REFERENCE / "upstream/SkyTearsAndGregHeart.java"
JAR = Path(sys.argv[1])
text = SOURCE.read_text(encoding="utf-8")


def array(name):
    body = re.search(r"\b" + name + r"\s*=\s*\{(.*?)\};", text, re.S)[1]
    return re.sub(r"//[^\n]*", "", body).strip()


def numbers(name):
    return json.loads("[" + array(name).replace("{", "[").replace("}", "]") + "]")


data = {
    "ores": [re.findall(r"[A-Za-z][A-Za-z0-9]*", row)
             for row in re.findall(r"\{([^}]+)\}", array("skyOres"))],
    "oreCounts": numbers("skyOres_n"),
    "fluids": re.findall(r"[A-Za-z][A-Za-z0-9]*", array("skyFluid")),
    "fluidAmounts": numbers("skyFluid_n"),
    "dimensionCounts": numbers("skyDNumber"),
    "dimensionMaps": numbers("skyDMap"),
}
assert len(data["ores"]) == len(data["oreCounts"]) == 50
assert len(data["dimensionCounts"]) == len(data["dimensionMaps"]) == 16
assert len(data["fluids"]) == len(data["fluidAmounts"]) == 26
for counts, mapping in zip(data["dimensionCounts"], data["dimensionMaps"]):
    assert sum(counts) == len(mapping)

resources = ROOT / "src/main/resources"
destination = resources / "assets/gtnotgood/recipes/fragment_world_collection.json"
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
records = []
with zipfile.ZipFile(JAR) as archive:
    prefix = "assets/gtceu/textures/block/machines/fragment_world_collection_machine/"
    for name in archive.namelist():
        if not name.startswith(prefix) or name.endswith("/"):
            continue
        target = resources / "assets/gtnotgood/textures/blocks/basicmachines/fragment_collector" / Path(name).name
        target.parent.mkdir(parents=True, exist_ok=True)
        content = archive.read(name)
        target.write_bytes(content)
        records.append({"source": name, "destination": target.relative_to(resources).as_posix(),
                        "sha256": hashlib.sha256(content).hexdigest(), "modifications": "none"})

provenance = resources / "META-INF/fragment-world-collector-port"
provenance.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ROOT / "reference/light-hunter-space-station-port/upstream/gtlcore/LICENSE", provenance / "LICENSE-GTLCore")
manifest = {
    "source": "https://github.com/GNSW/GTLCore", "sourceCommit": PIN, "license": "LGPL-3.0-only",
    "localJar": JAR.name, "localJarSha256": hashlib.sha256(JAR.read_bytes()).hexdigest(),
    "recipeSource": "src/main/java/org/gtlcore/gtlcore/data/recipe/processing/SkyTearsAndGregHeart.java",
    "recipeSourceSha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
    "recipeDataSha256": hashlib.sha256(destination.read_bytes()).hexdigest(),
    "recipeDataDestination": destination.relative_to(resources).as_posix(),
    "recipeDataModifications": "Original six tables extracted to JSON; native material/dimension mappings are applied in Java",
    "adaptedRecipeDestination": "src/main/java/com/xyp/gtnotgood/common/recipe/machine/FragmentWorldCollectorRecipes.java",
    "adaptations": ["Minecraft 1.7.10 / GTNH native APIs", "Non-consumable dimension display blocks",
                    "Reactor to Twilight Forest; Glacio to Triton (user-approved)",
                    "Native material/rock mappings and optional output availability",
                    "Fluid circuits renumbered within GTNH 0-24; drill disambiguates collection mode",
                    "Infinity drill replaces full-output grinding casing; long-checked int-max tank",
                    "GTL-exclusive crystals and downstream chains excluded; see PORT.md"],
    "assets": records,
}
(provenance / "MANIFEST.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
print(f"Prepared {len(records)} unchanged textures/metadata and six verified recipe tables")
