# SPDX-License-Identifier: GPL-3.0-only
import concurrent.futures
import gzip
import hashlib
import importlib.util
import json
import shutil
import struct
import time
import urllib.request
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
REF = Path(__file__).resolve().parent
RES = ROOT / "src/main/resources"
TARGET = RES / "assets/gtnotgood/textures/blocks/lighthunter"
NOTICE = RES / "META-INF/light-hunter-space-station-port"
TEMP = Path("C:/Users/Administrator/AppData/Local/Temp")
PACK = TEMP / "dimensional-focus-official-1.4.5.3.zip"
PACK_HASH = "1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b"
REPOS = {
    "gtladditions": ("Dragonators/GTLAdditions", "34fb152886164c56a07fc9d1b10e056f0c22b7ab", "GPL-3.0-only", "codex-gtladditions-tree-20261022.json"),
    "gtlcore": ("GNSW/GTLCore", "6443c848264fb15c90cf50e031f146522616a8b9", "LGPL-3.0-only", "gtl-heart-core-tree.json"),
    "gtceu": ("GregTechCEu/GregTech-Modern", "e3844ba943e2388c0856617ae4f8b473a0ae1b64", "LGPL-3.0-only", "gtl-heart-gtm-tree.json"),
}
SAVED_BLOBS = REF / "SOURCE_BLOBS.json"
TREES = json.loads(SAVED_BLOBS.read_text()) if SAVED_BLOBS.exists() else {}
for repo, data in REPOS.items():
    tree = TEMP / data[3]
    if tree.exists():
        TREES[repo] = {item["path"]: item["sha"] for item in json.loads(tree.read_text())["tree"]}

# Texture keys correspond to the original model faces, not newly drawn replacements.
FACES = {
    "uhv_machine_casing": ["gtceu:block/casings/voltage/uhv/" + part for part in ("bottom", "top", "side", "side", "side", "side")],
    "uiv_machine_casing": ["gtceu:block/casings/voltage/uiv/" + part for part in ("bottom", "top", "side", "side", "side", "side")],
    "uxv_machine_casing": ["gtceu:block/casings/voltage/uxv/" + part for part in ("bottom", "top", "side", "side", "side", "side")],
    "computer_heat_vent": ["gtceu:block/casings/hpca/computer_heat_vent_" + part for part in ("top_bot", "top_bot", "side", "side", "side", "side")],
    "super_computation_component": ["gtceu:block/casings/hpca/damaged_advanced_hpca_component_side"] * 6,
    "ultimate_stellar_containment_casing": ["gtlcore:block/stellar_containment_casing"] * 6,
    "dragon_strength_tritanium_casing": ["gtlcore:block/casings/extreme_strength_tritanium_casing"] * 6,
    "high_power_casing": ["gtceu:block/casings/hpca/high_power_casing"] * 6,
    "uhv_ultimate_battery": ["gtceu:block/casings/battery/uhv_ultimate/" + part for part in ("top", "top", "side", "side", "side", "side")],
    "advanced_computer_casing": ["gtceu:block/casings/hpca/advanced_computer_casing/" + part for part in ("bottom", "top", "front", "back", "side", "side")],
    "echo_casing": ["gtlcore:block/casings/echo_casing"] * 6,
    "hyper_core": ["gtlcore:block/hyper_core"] * 6,
    "fusion_casing_mk5": ["gtlcore:block/casings/fusion/fusion_casing_mk5"] * 6,
    "fusion_casing_mk4": ["gtlcore:block/casings/fusion/fusion_casing_mk4"] * 6,
    "uxv_hermetic_casing": ["gtceu:block/casings/voltage/uxv/bottom", "gtceu:block/casings/voltage/uxv/top"] + ["composite:uxv_hermetic_side"] * 4,
    "module_connector": ["kubejs:block/module_connector"] * 6,
    "neutronium_pipe_casing": ["kubejs:block/neutronium_pipe_casing"] * 6,
    "restraint_device": ["kubejs:block/restraint_device"] * 6,
    "force_field_glass": ["kubejs:block/force_field_glass"] * 6,
    "dimensional_bridge_casing": ["kubejs:block/dimensional_bridge_casing"] * 6,
    "hyper_core_glow": ["gtlcore:block/hyper_core_bloom"] * 6,
    "super_computation_component_active": ["gtlcore:block/damaged_advanced_hpca_component_side"] * 6,
    "super_computation_component_glow": ["gtlcore:block/gtlbloom_masks/damaged_advanced_hpca_component_side"] * 6,
    "fusion_casing_mk5_glow": ["gtceu:block/casings/fusion/fusion_casing_bloom"] * 6,
}
CONTROLLERS = {
    "station": "data_bank",
    "module": "fusion_reactor",
}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


def fetch(request):
    repo, path = request
    target = REF / "upstream" / repo / path
    target.parent.mkdir(parents=True, exist_ok=True)
    if not target.exists():
        for port in (ROOT / "reference").glob("*/upstream/" + repo):
            source = port / path
            if source.exists() and blob(source.read_bytes()) == TREES[repo][path]:
                shutil.copyfile(source, target)
                break
    for attempt in range(4):
        if target.exists() and blob(target.read_bytes()) == TREES[repo][path]:
            return request, target
        try:
            target.write_bytes(urllib.request.urlopen(
                "https://raw.githubusercontent.com/" + REPOS[repo][0] + "/" + REPOS[repo][1] + "/" + path,
                timeout=30).read())
        except OSError:
            if attempt == 3:
                raise
            time.sleep(attempt + 1)
    assert blob(target.read_bytes()) == TREES[repo][path], path
    return request, target


def texture_path(texture):
    repo, name = texture.split(":")
    return repo, "src/main/resources/assets/" + repo + "/textures/" + name + ".png"


def main():
    assert sha(PACK.read_bytes()) == PACK_HASH
    TARGET.mkdir(parents=True, exist_ok=True)
    NOTICE.mkdir(parents=True, exist_ok=True)
    textures = set(texture for faces in FACES.values() for texture in faces if not texture.startswith(("kubejs:", "composite:")))
    textures.add("gtceu:block/casings/hermetic_casing/hermetic_casing_overlay")
    requests = {(repo, "LICENSE") for repo in REPOS}
    requests.add(("gtladditions", "src/main/java/com/gtladd/gtladditions/common/machine/multiblock/MultiBlockMachine.kt"))
    for name in ("light_hunter_space_station", "light_hunter_space_station_module"):
        requests.add(("gtladditions", "scripts/structure_sources/multiblock/" + name + ".json"))
    for repo in REPOS:
        requests.update((repo, str(path.relative_to(REF / "upstream" / repo)).replace("\\", "/"))
                        for path in (REF / "upstream" / repo).rglob("*") if path.is_file())
    for texture in textures:
        repo, path = texture_path(texture)
        for suffix in ("", ".mcmeta", "_ctm"):
            candidate = path.replace(".png", "_ctm.png") if suffix == "_ctm" else path + suffix
            if candidate in TREES[repo]:
                requests.add((repo, candidate))
    for source in CONTROLLERS.values():
        prefix = "src/main/resources/assets/gtceu/textures/block/multiblock/" + source + "/overlay_front"
        requests.update(("gtceu", path) for path in TREES["gtceu"]
                        if path.startswith(prefix) and path.removeprefix(prefix) in
                        (".png", ".png.mcmeta", "_active.png", "_active.png.mcmeta", "_emissive.png", "_emissive.png.mcmeta",
                         "_active_emissive.png", "_active_emissive.png.mcmeta"))
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
        originals = dict(executor.map(fetch, sorted(requests)))
    SAVED_BLOBS.write_text(json.dumps({repo: {path: TREES[repo][path] for key, path in sorted(requests) if key == repo}
                                     for repo in REPOS}, indent=2) + "\n")
    records, derived, texture_specs = [], [], {}

    def record(source, destination, data, license_name, revision, source_path, modifications="Copied byte-for-byte.", source_data=None):
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        records.append({"source": source, "revision": revision, "sourcePath": source_path,
                        "destination": destination.relative_to(RES).as_posix(), "license": license_name,
                        "modifications": modifications, "sourceSha256": sha(data if source_data is None else source_data),
                        "destinationSha256": sha(destination.read_bytes())})

    for texture in sorted(textures):
        repo, source = texture_path(texture)
        repo_name, revision, license_name, _ = REPOS[repo]
        name = source.split("/textures/block/")[1].removesuffix(".png").replace("/", "_")
        texture_specs[texture] = {"name": name, "connected": (repo, source.replace(".png", "_ctm.png")) in originals}
        for suffix in ("", ".mcmeta", "_ctm"):
            path = source.replace(".png", "_ctm.png") if suffix == "_ctm" else source + suffix
            if (repo, path) not in originals:
                continue
            directory = TARGET / "source_ctm" if suffix == "_ctm" else TARGET
            destination = directory / (name + ("_ctm.png" if suffix == "_ctm" else ".png" + suffix))
            record("https://github.com/" + repo_name, destination, originals[(repo, path)].read_bytes(),
                   license_name, revision, path)
    for name, source in CONTROLLERS.items():
        prefix = "src/main/resources/assets/gtceu/textures/block/multiblock/" + source + "/overlay_front"
        for (repo, path), original in originals.items():
            if repo != "gtceu" or not path.startswith(prefix):
                continue
            suffix = path.removeprefix(prefix).replace("_emissive", "_glow")
            record("https://github.com/" + REPOS[repo][0], TARGET / (name + "_front" + suffix),
                   original.read_bytes(), REPOS[repo][2], REPOS[repo][1], path)
    pack_url = "https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465"
    with zipfile.ZipFile(PACK) as pack:
        for texture in sorted(set(t for faces in FACES.values() for t in faces if t.startswith("kubejs:"))):
            name = texture.split("/")[-1]
            path = ".minecraft/kubejs/assets/kubejs/textures/block/" + name + ".png"
            texture_specs[texture] = {"name": name, "connected": False}
            for suffix in ("", ".mcmeta"):
                if path + suffix not in pack.namelist():
                    continue
                data = pack.read(path + suffix)
                target = REF / "upstream/gtl-pack" / (path + suffix)
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
                record(pack_url, TARGET / (name + ".png" + suffix), data, "CC-BY-NC-SA-4.0", "1.4.5.3", path + suffix)
        for path in ("LICENSE", ".minecraft/kubejs/startup_scripts/block.js",
                     ".minecraft/kubejs/assets/kubejs/lang/en_us.json", ".minecraft/kubejs/assets/kubejs/lang/zh_cn.json"):
            target = REF / "upstream/gtl-pack" / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(pack.read(path))
        record(pack_url, NOTICE / "STRUCTURE_LICENSE-GTL-pack-CC-BY-NC-SA-4.0.txt", pack.read("LICENSE"),
               "CC-BY-NC-SA-4.0", "1.4.5.3", "LICENSE")
    side = Image.open(TARGET / "casings_voltage_uxv_side.png").convert("RGBA")
    overlay = Image.open(TARGET / "casings_hermetic_casing_hermetic_casing_overlay.png").convert("RGBA")
    assert side.size == overlay.size
    Image.alpha_composite(side, overlay).save(TARGET / "uxv_hermetic_side.png")
    texture_specs["composite:uxv_hermetic_side"] = {"name": "uxv_hermetic_side", "connected": False}
    derived.append({"destination": "assets/gtnotgood/textures/blocks/lighthunter/uxv_hermetic_side.png",
                    "sources": ["casings_voltage_uxv_side.png", "casings_hermetic_casing_hermetic_casing_overlay.png"],
                    "license": "LGPL-3.0-only", "sha256": sha((TARGET / "uxv_hermetic_side.png").read_bytes()),
                    "modifications": "Original hermetic model's two coincident cube side layers alpha-composited at native resolution."})
    spec = importlib.util.spec_from_file_location("original_ctm", ROOT / "reference/field-extruder-port/generate-connected-textures.py")
    ctm = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(ctm)
    for texture, value in texture_specs.items():
        if not value["connected"]:
            continue
        name = value["name"]
        base = Image.open(TARGET / (name + ".png")).convert("RGBA")
        atlas = Image.open(TARGET / "source_ctm" / (name + "_ctm.png")).convert("RGBA")
        assert base.width == 16 and atlas.width == 32, name
        frames = max(base.height // 16, atlas.height // 32)
        for mask in range(256):
            if ctm.normalize(mask) != mask:
                continue
            result = Image.new("RGBA", (16, 16 * frames))
            for frame in range(frames):
                by, ay = frame % (base.height // 16) * 16, frame % (atlas.height // 32) * 32
                result.paste(ctm.compose(base.crop((0, by, 16, by + 16)), atlas.crop((0, ay, 32, ay + 32)), mask), (0, 16 * frame))
            destination = TARGET / "ctm" / name / (str(mask) + ".png")
            destination.parent.mkdir(parents=True, exist_ok=True)
            result.save(destination)
            derived.append({"destination": destination.relative_to(RES).as_posix(), "mask": mask,
                            "license": REPOS[texture.split(":")[0]][2], "sha256": sha(destination.read_bytes()),
                            "modifications": "Lossless original LDLib quarter-tile composition."})
            if frames > 1:
                meta = {"animation": {"frametime": 2}}
                metadata = TARGET / (name + ".png.mcmeta")
                if metadata.exists():
                    meta = {"animation": json.loads(metadata.read_text()).get("animation", meta["animation"])}
                destination.with_suffix(".png.mcmeta").write_text(json.dumps(meta) + "\n")
    shapes = []
    for name, width, height, length, controller in (
        ("light_hunter_space_station", 73, 73, 189, (36, 36, 188)),
        ("light_hunter_space_station_module", 5, 21, 13, (2, 10, 12)),
    ):
        path = "scripts/structure_sources/multiblock/" + name + ".json"
        source = originals[("gtladditions", path)].read_bytes()
        aisles = json.loads(source)["aisles"]
        assert len(aisles) == length and all(len(a) == height for a in aisles)
        assert all(len(row) == width for a in aisles for row in a)
        assert aisles[controller[2]][controller[1]][controller[0]] == "~"
        encoded = bytearray()

        def varint(value):
            value &= 0xffffffff
            while value > 127:
                encoded.append((value & 127) | 128)
                value >>= 7
            encoded.append(value)

        for value in (-1, 1, 2, 0, 4, length):
            varint(value)
        for aisle in aisles:
            varint(height)
            for row in aisle:
                data = row.encode("ascii")
                encoded.extend(struct.pack(">H", len(data)))
                encoded.extend(data)
        destination = RES / "assets/gtnotgood/structures" / (name + ".mbs")
        record("https://github.com/" + REPOS["gtladditions"][0], destination, gzip.compress(encoded, mtime=0),
               "GPL-3.0-only", REPOS["gtladditions"][1], path,
               "Lossless GZIP MBS encoding of unchanged original aisles and spaces.", source)
        shapes.append({"name": name, "width": width, "height": height, "length": length,
                       "controllerSource": controller, "structureLibOffset": [controller[0], height - 1 - controller[1], length - 1 - controller[2]],
                       "nonSpace": sum(c != " " for a in aisles for row in a for c in row),
                       "coordinatesSha256": sha("\n".join(row for a in aisles for row in a).encode("ascii"))})
    for repo, (name, revision, license_name, _) in REPOS.items():
        record("https://github.com/" + name, NOTICE / ("STRUCTURE_LICENSE-" + repo + ".txt"),
               originals[(repo, "LICENSE")].read_bytes(), license_name, revision, "LICENSE")
    manifest = {"formatVersion": 1, "officialPackSha256": PACK_HASH, "structures": shapes,
                "connectedTextureGenerator": {"source": "Low-Drag-MC/LDLib-MultiLoader",
                                              "revision": "1d4e4390ae58d16038845c651e122185c29f334b",
                                              "license": "GPL-3.0-only",
                                              "adapter": "reference/field-extruder-port/generate-connected-textures.py"},
                "originalAssets": records, "derivedAssets": derived,
                "officialPackReferences": [{"sourcePath": p.relative_to(REF / "upstream/gtl-pack").as_posix(),
                                            "referencePath": p.relative_to(REF).as_posix(), "sha256": sha(p.read_bytes())}
                                           for p in sorted((REF / "upstream/gtl-pack").rglob("*")) if p.is_file()],
                "sourceReferences": [{"repository": REPOS[repo][0], "revision": REPOS[repo][1],
                                      "sourcePath": path, "referencePath": p.relative_to(REF).as_posix(),
                                      "sha256": sha(p.read_bytes()), "gitBlobSha1": blob(p.read_bytes())}
                                     for (repo, path), p in sorted(originals.items())]}
    data = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    (REF / "STRUCTURE_ASSET_MANIFEST.json").write_text(data, encoding="utf-8")
    (NOTICE / "STRUCTURE_ASSET_MANIFEST.json").write_text(data, encoding="utf-8")
    (REF / "texture-faces.json").write_text(json.dumps({name: [texture_specs[t] for t in faces] for name, faces in FACES.items()}, indent=2) + "\n")
    print("Imported", len(records), "original assets and", len(derived), "derived sprites;", shapes, flush=True)


if __name__ == "__main__":
    main()
