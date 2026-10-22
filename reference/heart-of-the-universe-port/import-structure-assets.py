# SPDX-License-Identifier: GPL-3.0-only
import concurrent.futures
import gzip
import hashlib
import importlib.util
import json
import struct
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
REFERENCE = Path(__file__).resolve().parent
RESOURCES = ROOT / "src/main/resources"
TARGET = RESOURCES / "assets/gtnotgood/textures/blocks/heart"
NOTICES = RESOURCES / "META-INF/heart-of-the-universe-port"
ADD_REV = "34fb152886164c56a07fc9d1b10e056f0c22b7ab"
CORE_REV = "6443c848264fb15c90cf50e031f146522616a8b9"
GTM_REV = "e3844ba943e2388c0856617ae4f8b473a0ae1b64"
PACK_HASH = "1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b"
CORE = "GNSW/GTLCore"
ADD = "Dragonators/GTLAdditions"
GTM = "GregTechCEu/GregTech-Modern"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def fetch(request):
    repo, rev, path = request
    destination = REFERENCE / "upstream" / {ADD: "gtladditions", CORE: "gtlcore", GTM: "gtceu"}[repo] / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    if not destination.exists():
        for attempt in range(3):
            try:
                destination.write_bytes(urllib.request.urlopen(f"https://raw.githubusercontent.com/{repo}/{rev}/{path}", timeout=45).read())
                break
            except OSError:
                if attempt == 2:
                    raise
                time.sleep(1 + attempt)
    return request, destination


def main():
    archive = Path(sys.argv[1])
    assert digest(archive.read_bytes()) == PACK_HASH
    TARGET.mkdir(parents=True, exist_ok=True)
    NOTICES.mkdir(parents=True, exist_ok=True)
    core_prefix = "src/main/resources/assets/gtlcore/textures/block/"
    variants = {"graviton_field_constraint_casing": "casings/graviton_field_constraint_casing",
                "rhenium_reinforced_energy_glass": "casings/rhenium_reinforced_energy_glass",
                "temporal_anchor_field_casing": "casings/sps_casing",
                "dimension_connection_casing": "dimension_connection_casing", "create_casing": "create_casing"}
    connected = set(list(variants)[:3])
    controller_assets = [(CORE, CORE_REV, "src/main/resources/assets/gtceu/textures/block/multiblock/cosmos_simulation/overlay_front", "controller_front"),
                         (CORE, CORE_REV, "src/main/resources/assets/gtceu/textures/block/multiblock/cosmos_simulation/overlay_front_active", "controller_front_active"),
                         (CORE, CORE_REV, "src/main/resources/assets/gtceu/textures/block/multiblock/cosmos_simulation/overlay_front_active_emissive", "controller_front_active_glow"),
                         (ADD, ADD_REV, "src/main/resources/assets/gtceu/textures/block/multiblock/top/cosmos_simulation/overlay_top", "controller_top"),
                         (ADD, ADD_REV, "src/main/resources/assets/gtceu/textures/block/multiblock/top/cosmos_simulation/overlay_top_active", "controller_top_active"),
                         (GTM, GTM_REV, "src/main/resources/assets/gtceu/textures/block/casings/hpca/high_power_casing", "high_power_casing")]
    requests = {(CORE, CORE_REV, "LICENSE"), (ADD, ADD_REV, "LICENSE"),
                (GTM, GTM_REV, "LICENSE"),
                (ADD, ADD_REV, "src/main/resources/assets/gtladditions/structures/multiblock/annihilate_generator.bin"),
                (ADD, ADD_REV, "scripts/structure_sources/multiblock/annihilate_generator.json"),
                (ADD, ADD_REV, "src/main/java/com/gtladd/gtladditions/common/machine/multiblock/MultiBlockMachine.kt"),
                (ADD, ADD_REV, "src/generated/resources/assets/gtladditions/models/block/temporal_anchor_field_casing.json")}
    for repo, rev, source, _ in controller_assets:
        for suffix in (".png", ".png.mcmeta"):
            requests.add((repo, rev, source + suffix))
    for name, source in variants.items():
        requests.add((CORE, CORE_REV, core_prefix + source + ".png"))
        if name in connected:
            requests.add((CORE, CORE_REV, core_prefix + source + ".png.mcmeta"))
            requests.add((CORE, CORE_REV, core_prefix + source + "_ctm.png"))
        requests.add((CORE, CORE_REV, "src/generated/resources/assets/gtlcore/models/block/" + source.rsplit("/", 1)[-1] + ".json"))
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
        originals = dict(executor.map(fetch, sorted(requests)))
    records = []

    def record(source, destination, data, license_name, revision, source_path, modifications="Copied byte-for-byte."):
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        records.append({"source": source, "revision": revision, "sourcePath": source_path,
                        "destination": destination.relative_to(RESOURCES).as_posix(), "license": license_name,
                        "modifications": modifications, "sourceSha256": digest(data),
                        "destinationSha256": digest(destination.read_bytes())})

    for name, source in variants.items():
        for suffix in (".png", ".png.mcmeta", "_ctm.png"):
            key = (CORE, CORE_REV, core_prefix + source + suffix)
            if key not in originals:
                continue
            directory = TARGET / "source_ctm" if suffix == "_ctm.png" else NOTICES / "source-metadata" if suffix.endswith("mcmeta") else TARGET
            record("https://github.com/" + CORE, directory / (name + suffix), originals[key].read_bytes(),
                   "LGPL-3.0", CORE_REV, key[2])
    for repo, rev, source, name in controller_assets:
        for suffix in (".png", ".png.mcmeta"):
            record("https://github.com/" + repo, TARGET / (name + suffix),
                   originals[(repo, rev, source + suffix)].read_bytes(),
                   "GPL-3.0" if repo == ADD else "LGPL-3.0", rev, source + suffix)
    for repo, rev, license_name in ((CORE, CORE_REV, "LGPL-3.0"), (ADD, ADD_REV, "GPL-3.0"), (GTM, GTM_REV, "LGPL-3.0")):
        record("https://github.com/" + repo, NOTICES / ("LICENSE-" + repo.split("/")[1] + ".txt"),
               originals[(repo, rev, "LICENSE")].read_bytes(), license_name, rev, "LICENSE")
    structure_path = "scripts/structure_sources/multiblock/annihilate_generator.json"
    aisles = json.loads(originals[(ADD, ADD_REV, structure_path)].read_text(encoding="utf-8"))["aisles"]
    assert len(aisles) == 109 and all(len(aisle) == 112 for aisle in aisles)
    assert all(len(row) == 121 for aisle in aisles for row in aisle)
    encoded = bytearray()

    def varint(value):
        value &= 0xffffffff
        while value > 127:
            encoded.append((value & 127) | 128)
            value >>= 7
        encoded.append(value)

    for value in (-1, 1, 2, 0, 4, len(aisles)):
        varint(value)
    for aisle in aisles:
        varint(len(aisle))
        for row in aisle:
            data = row.encode("ascii")
            encoded.extend(struct.pack(">H", len(data)))
            encoded.extend(data)
    destination = RESOURCES / "assets/gtnotgood/structures/heart_of_the_universe.mbs"
    record("https://github.com/" + ADD, destination, gzip.compress(encoded, mtime=0), "GPL-3.0", ADD_REV,
           structure_path, "Factory aisles encoded as GZIP MBS varints and UTF rows; all coordinates/symbols unchanged.")
    records[-1]["sourceSha256"] = digest(originals[(ADD, ADD_REV, structure_path)].read_bytes())
    records[-1]["uncompressedAislesSha256"] = digest("\n".join(row for aisle in aisles for row in aisle).encode("ascii"))
    pack_url = "https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465"
    with zipfile.ZipFile(archive) as pack:
        for name in ("annihilate_core", "dimensional_stability_casing", "dyson_deployment_magnet", "dyson_deployment_magnet_side"):
            source = ".minecraft/kubejs/assets/kubejs/textures/block/" + name + ".png"
            for suffix in ("", ".mcmeta"):
                if source + suffix not in pack.namelist():
                    continue
                record(pack_url, TARGET / (name + ".png" + suffix), pack.read(source + suffix),
                       "CC-BY-NC-SA-4.0", "1.4.5.3", source + suffix)
                raw = REFERENCE / "upstream/gtl-pack" / (source + suffix)
                raw.parent.mkdir(parents=True, exist_ok=True)
                raw.write_bytes(pack.read(source + suffix))
        record(pack_url, NOTICES / "LICENSE-GTL-pack-CC-BY-NC-SA-4.0.txt", pack.read("LICENSE"),
               "CC-BY-NC-SA-4.0", "1.4.5.3", "LICENSE")
        source = ".minecraft/kubejs/startup_scripts/block.js"
        raw = REFERENCE / "upstream/gtl-pack" / source
        raw.parent.mkdir(parents=True, exist_ok=True)
        raw.write_bytes(pack.read(source))
    spec = importlib.util.spec_from_file_location("ctm", ROOT / "reference/field-extruder-port/generate-connected-textures.py")
    ctm = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(ctm)
    derived = []
    for name in sorted(connected):
        base = Image.open(TARGET / (name + ".png")).convert("RGBA")
        atlas = Image.open(TARGET / "source_ctm" / (name + "_ctm.png")).convert("RGBA")
        assert base.width == 16 and atlas.width == 32
        frames = max(base.height // 16, atlas.height // 32)
        for mask in range(256):
            if ctm.normalize(mask) != mask:
                continue
            result = Image.new("RGBA", (16, 16 * frames))
            for frame in range(frames):
                by = frame % (base.height // 16) * 16
                ay = frame % (atlas.height // 32) * 32
                result.paste(ctm.compose(base.crop((0, by, 16, by + 16)), atlas.crop((0, ay, 32, ay + 32)), mask), (0, frame * 16))
            destination = TARGET / "ctm" / name / (str(mask) + ".png")
            destination.parent.mkdir(parents=True, exist_ok=True)
            result.save(destination)
            derived.append({"destination": destination.relative_to(RESOURCES).as_posix(), "mask": mask,
                            "license": "LGPL-3.0", "modifications": "Lossless original LDLib quarter-tile composition.",
                            "sha256": digest(destination.read_bytes())})
            if frames > 1:
                destination.with_suffix(".png.mcmeta").write_text(json.dumps({"animation": {"frametime": 2}}) + "\n")
        if frames > 1:
            (TARGET / (name + ".png.mcmeta")).write_text(json.dumps({"animation": {"frametime": 2}}) + "\n")
    manifest = {"formatVersion": 1, "officialPackSha256": PACK_HASH, "originalAssets": records,
                "derivedConnectedTextures": derived,
                "sourceReferences": [{"source": "https://github.com/" + repo, "revision": rev,
                                      "sourcePath": path, "referencePath": original.relative_to(REFERENCE).as_posix(),
                                      "sha256": digest(original.read_bytes())}
                                     for (repo, rev, path), original in sorted(originals.items())],
                "reusedAssets": {"C": "FieldExtruderCasing.HyperMechanical", "F": "DimensionalFocusCasing.FusionGlass",
                                 "S": "DimensionalFocusCasing.DimensionInjection"}}
    data = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    (REFERENCE / "STRUCTURE_ASSET_MANIFEST.json").write_text(data, encoding="utf-8")
    (NOTICES / "STRUCTURE_ASSET_MANIFEST.json").write_text(data, encoding="utf-8")
    print(f"Imported {len(records)} original assets, {len(derived)} CTM sprites.")


if __name__ == "__main__":
    main()
