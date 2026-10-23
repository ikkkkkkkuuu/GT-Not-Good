# SPDX-License-Identifier: GPL-3.0-only
import argparse
import hashlib
import json
import math
import runpy
import shutil
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
TARGET = RES / "assets/gtnotgood/textures/blocks/tentacle"
NOTICE = RES / "META-INF/nyarlathoteps-tentacle-port"
REVISION = "156835a0e66a90cbc4e60c3e95a36c35212b97d6"
JAR_HASH = "041e3ce8c13c04168f01ea3a9dc3078acfe7533aa8e46348cdc10b1dd3c115b6"
TEXTURES = {
    "molecular_casing": "casings/molecular_casing",
    "molecular_casing_bloom": "casings/molecular_casing_bloom",
    "uruium_coil_block": "coil/uruium_coil_block",
    "uruium_coil_block_bloom": "coil/uruium_coil_block_bloom",
    "laser_casing": "laser_casing",
    "spacetime_assembly_line_casing": "spacetime_assembly_line_casing",
    "laser_cooling_casing": "laser_cooling_casing",
    "naquadah_alloy_casing": "casings/naquadah_alloy_casing",
    "electron_permeable_glass": "casings/electron_permeable_neutronium_coated_glass",
    "quark_pipe": "quark_pipe",
    "spacetime_assembly_line_unit": "spacetime_assembly_line_unit",
    "containment_field_generator": "containment_field_generator",
    "hollow_casing": "hollow_casing",
    "restraint_device": "restraint_device",
    "degenerate_rhenium_constrained_casing": "casings/degenerate_rhenium_constrained_casing",
}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    parser.add_argument("reference", type=Path)
    args = parser.parse_args()
    assert sha(args.jar.read_bytes()) == JAR_HASH
    TARGET.mkdir(parents=True, exist_ok=True)
    NOTICE.mkdir(parents=True, exist_ok=True)
    originals, derived, animations, connected = [], [], [], []

    def copy(archive, source, destination, repository, revision):
        data = archive.read(source)
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        originals.append({"repository": repository, "revision": revision, "sourcePath": source,
                          "destination": destination.relative_to(RES).as_posix(),
                          "license": "LGPL-3.0-only", "sourceSha256": sha(data),
                          "destinationSha256": sha(data), "modification": "Copied byte-for-byte."})

    with zipfile.ZipFile(args.jar) as archive:
        for name, source in TEXTURES.items():
            for suffix in (".png", ".png.mcmeta", "_ctm.png", "_ctm.png.mcmeta"):
                path = "assets/gtocore/textures/block/" + source + suffix
                if path not in archive.namelist():
                    continue
                destination = TARGET / ("source_ctm" if "_ctm" in suffix else "") / (name + suffix)
                copy(archive, path, destination, "GregTech-Odyssey/GTOCore-Main", REVISION)
            if (TARGET / "source_ctm" / (name + "_ctm.png")).exists():
                connected.append(name)
        copy(archive, "pattern/nyarlathoteps_tentacle.mbs",
             RES / "assets/gtnotgood/structures/nyarlathoteps_tentacle.mbs",
             "GregTech-Odyssey/GTOCore-Main", REVISION)
        for name in ("molecular_casing", "uruium_coil_block", "laser_casing",
                     "spacetime_assembly_line_casing", "electron_permeable_amprosium_coated_glass"):
            for path in archive.namelist():
                if path.startswith("assets/gtocore/models/block/") and name in path:
                    destination = Path(__file__).parent / "models" / path
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    destination.write_bytes(archive.read(path))
        nested = args.reference / "gtceu-1.20.1-forge-1.20.1-26.10.53.jar"
        with zipfile.ZipFile(nested) as gtm:
            for name in gtm.namelist():
                if name.startswith("assets/gtceu/textures/block/casings/cleanroom/") and "sterilizing_filter_casing" in name:
                    copy(gtm, name, TARGET / ("source_ctm" if "_ctm" in name else "") / Path(name).name,
                         "GregTech-Odyssey/GregTech-Modern", "26.10.53")

    layout = runpy.run_path(str(ROOT / "reference/field-extruder-port/generate-connected-textures.py"))
    for path in (TARGET / "source_ctm").glob("*_ctm.png"):
        name = path.name.removesuffix("_ctm.png")
        if name not in connected:
            connected.append(name)
        base = Image.open(TARGET / (name + ".png")).convert("RGBA")
        atlas = Image.open(path).convert("RGBA")
        assert base.width == 16 and atlas.width == 32
        frames = math.lcm(base.height // 16, atlas.height // 32)
        for mask in range(256):
            if layout["normalize"](mask) != mask:
                continue
            result = Image.new("RGBA", (16, frames * 16))
            for frame in range(frames):
                by = frame % (base.height // 16) * 16
                ay = frame % (atlas.height // 32) * 32
                tile = layout["compose"](base.crop((0, by, 16, by + 16)),
                                         atlas.crop((0, ay, 32, ay + 32)), mask)
                if mask == 0:
                    assert tile.tobytes() == base.crop((0, by, 16, by + 16)).tobytes()
                result.paste(tile, (0, 16 * frame))
            destination = TARGET / "ctm" / name / (str(mask) + ".png")
            destination.parent.mkdir(parents=True, exist_ok=True)
            result.save(destination)
            derived.append({"destination": destination.relative_to(RES).as_posix(), "mask": mask,
                            "sha256": sha(destination.read_bytes()), "license": "LGPL-3.0-only"})
            if frames > 1:
                metadata = TARGET / (name + ".png.mcmeta")
                assert metadata.exists(), name
                animation = destination.with_suffix(".png.mcmeta")
                animation.write_bytes(metadata.read_bytes())
                animations.append({"destination": animation.relative_to(RES).as_posix(),
                                   "source": metadata.relative_to(RES).as_posix(),
                                   "sha256": sha(animation.read_bytes()), "license": "LGPL-3.0-only"})

    for source, target in [(args.reference / "upstream/LICENSE", "LICENSE-GTOCore-LGPL-3.0.txt"),
                           (args.reference / "gtm/LICENSE", "LICENSE-GTM-LGPL-3.0.txt"),
                           (RES / "META-INF/tst-modular-port/LICENSE-TST-GPL-3.0.txt", "LICENSE-GPL-3.0.txt")]:
        if source.exists():
            shutil.copyfile(source, NOTICE / target)
    trees = {key: json.loads((args.reference / name).read_text(encoding="utf-8"))
             for key, name in (("gto", "GTOCore-Main-tree.json"), ("gtm", "GTM-tree.json"))}
    for record in originals:
        tree = trees["gto" if "GTOCore" in record["repository"] else "gtm"]
        blobs = {entry["path"]: entry["sha"] for entry in tree["tree"]}
        data = (RES / record["destination"]).read_bytes()
        blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        for prefix in ("src/main/resources/", "src/generated/resources/"):
            path = prefix + record["sourcePath"]
            if blobs.get(path) == blob:
                record.update(sourceGitPath=path, sourceGitBlob=blob, sourceCommit=tree["sha"])
                break
        else:
            record["sourceOrigin"] = "Bundled runtime artifact; no byte-identical blob at the inspected source commit."
    manifest = {"repository": "https://github.com/GregTech-Odyssey/GTOCore-Main", "revision": REVISION,
                "instanceJarSha256": JAR_HASH, "width": 165, "height": 48, "length": 49,
                "bundledGtmJarSha256": sha((args.reference / "gtceu-1.20.1-forge-1.20.1-26.10.53.jar").read_bytes()),
                "controllerSource": [82, 24, 48], "structureLibOffset": [82, 23, 0],
                "connectedTextures": sorted(connected), "originalAssets": originals, "derivedAssets": derived,
                "derivedAnimationMetadata": animations,
                "generatorSource": "reference/field-extruder-port/generate-connected-textures.py",
                "generatorLicense": "GPL-3.0-only",
                "generatorRevision": "LDLib-MultiLoader 1d4e4390ae58d16038845c651e122185c29f334b"}
    (NOTICE / "ASSET_MANIFEST.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print("Imported", len(originals), "original assets and", len(derived), "CTM sprites.")
    runpy.run_path(str(Path(__file__).with_name("expand-animations.py")))["main"]()


if __name__ == "__main__":
    main()
