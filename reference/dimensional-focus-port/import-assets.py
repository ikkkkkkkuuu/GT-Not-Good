# SPDX-License-Identifier: GPL-3.0-only
# CTM composition adapts reference/field-extruder-port/generate-connected-textures.py.

import hashlib
import importlib.util
import json
import sys
import urllib.request
import zipfile
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
RESOURCES = ROOT / "src/main/resources"
TEXTURES = RESOURCES / "assets/gtnotgood/textures/blocks/dimensionalfocus"
NOTICES = RESOURCES / "META-INF/dimensional-focus-port"
GTL_REV = "6443c848264fb15c90cf50e031f146522616a8b9"
GTM_REV = "e3844ba943e2388c0856617ae4f8b473a0ae1b64"
PACK_HASH = "1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b"


def main():
    archive = Path(sys.argv[2])
    assert hashlib.sha256(archive.read_bytes()).hexdigest() == PACK_HASH
    records = []
    TEXTURES.mkdir(parents=True, exist_ok=True)
    NOTICES.mkdir(parents=True, exist_ok=True)

    def record(data, source_url, revision, path, destination, license_name, modifications="None; copied byte-for-byte."):
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        records.append({"source": source_url, "revision": revision, "sourcePath": path,
                        "destination": destination.relative_to(RESOURCES).as_posix(), "license": license_name,
                        "modifications": modifications, "sourceSha256": hashlib.sha256(data).hexdigest(),
                        "destinationSha256": hashlib.sha256(destination.read_bytes()).hexdigest()})

    def gtl(path, destination):
        url = f"https://raw.githubusercontent.com/GNSW/GTLCore/{GTL_REV}/{path}"
        with urllib.request.urlopen(url, timeout=40) as response:
            data = response.read()
        record(data, "https://github.com/GNSW/GTLCore", GTL_REV, path, destination, "LGPL-3.0")

    def gtm(path, destination):
        url = f"https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/{GTM_REV}/{path}"
        with urllib.request.urlopen(url, timeout=40) as response:
            data = response.read()
        record(data, "https://github.com/GregTechCEu/GregTech-Modern", GTM_REV, path, destination, "LGPL-3.0")

    for name, directory in (("dimensionally_transcendent_casing", "casings/"),
                            ("dimension_injection_casing", "casings/"),
                            ("improved_superconductor_coil", ""),
                            ("improved_superconductor_coil_active", "")):
        prefix = "src/main/resources/assets/gtlcore/textures/block/" + directory + name
        gtl(prefix + ".png", TEXTURES / (name + ".png"))
        gtl(prefix + ".png.mcmeta", NOTICES / "source-metadata" / (name + ".png.mcmeta"))
        gtl(prefix + "_ctm.png", TEXTURES / "source_ctm" / (name + "_ctm.png"))
        if name.endswith("_active"):
            gtl(prefix + "_ctm.png.mcmeta", NOTICES / "source-metadata" / (name + "_ctm.png.mcmeta"))
    for name, directory in (("fusion_glass", "transparent/"), ("laser_safe_engraving_casing", "gcym/")):
        prefix = "src/main/resources/assets/gtceu/textures/block/casings/" + directory + name
        gtm(prefix + ".png", TEXTURES / (name + ".png"))
        gtm(prefix + ".png.mcmeta", NOTICES / "source-metadata" / (name + ".png.mcmeta"))
        gtm(prefix + "_ctm.png", TEXTURES / "source_ctm" / (name + "_ctm.png"))
    gtl("LICENSE", NOTICES / "LICENSE-GTLCore-LGPL-3.0.txt")
    gtm("LICENSE", NOTICES / "LICENSE-GTCEu-LGPL-3.0.txt")
    with zipfile.ZipFile(archive) as pack:
        for texture in ("molecular_coil", "molecular_coil_top"):
            path = ".minecraft/kubejs/assets/kubejs/textures/block/" + texture + ".png"
            matches = [name for name in pack.namelist() if name.endswith(path)]
            assert len(matches) == 1, matches
            record(pack.read(matches[0]), "https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465",
                   "1.4.5.3", matches[0], TEXTURES / (texture + ".png"), "CC-BY-NC-SA-4.0")
            for name in pack.namelist():
                if name.endswith(path + ".mcmeta"):
                    record(pack.read(name), "https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465",
                           "1.4.5.3", name, TEXTURES / (texture + ".png.mcmeta"), "CC-BY-NC-SA-4.0")
        license_path = next(name for name in pack.namelist() if name.rstrip("/").endswith("/LICENSE") or name == "LICENSE")
        record(pack.read(license_path), "https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465",
               "1.4.5.3", license_path, NOTICES / "LICENSE-GTL-pack-CC-BY-NC-SA-4.0.txt", "CC-BY-NC-SA-4.0")

    spec = importlib.util.spec_from_file_location("ctm", ROOT / "reference/field-extruder-port/generate-connected-textures.py")
    ctm = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(ctm)
    derived = []
    for name in ("dimensionally_transcendent_casing", "dimension_injection_casing", "improved_superconductor_coil",
                 "improved_superconductor_coil_active", "fusion_glass", "laser_safe_engraving_casing"):
        base = Image.open(TEXTURES / (name + ".png")).convert("RGBA")
        atlas = Image.open(TEXTURES / "source_ctm" / (name + "_ctm.png")).convert("RGBA")
        assert base.width == 16 and atlas.width == 32
        frames = max(base.height // 16, atlas.height // 32)
        for mask in range(256):
            if ctm.normalize(mask) != mask:
                continue
            result = Image.new("RGBA", (16, 16 * frames))
            for frame in range(frames):
                base_y = frame % (base.height // 16) * 16
                atlas_y = frame % (atlas.height // 32) * 32
                result.paste(ctm.compose(base.crop((0, base_y, 16, base_y + 16)),
                                        atlas.crop((0, atlas_y, 32, atlas_y + 32)), mask), (0, frame * 16))
            if mask == 0:
                assert result.crop((0, 0, 16, 16)).tobytes() == base.crop((0, 0, 16, 16)).tobytes()
            destination = TEXTURES / "ctm" / name / (str(mask) + ".png")
            destination.parent.mkdir(parents=True, exist_ok=True)
            result.save(destination)
            derived.append({"destination": destination.relative_to(RESOURCES).as_posix(), "mask": mask,
                            "sha256": hashlib.sha256(destination.read_bytes()).hexdigest()})
            if frames > 1:
                metadata = {"animation": {"frametime": 2}}
                (destination.parent / (destination.name + ".mcmeta")).write_text(json.dumps(metadata) + "\n")
        if frames > 1:
            (TEXTURES / (name + ".png.mcmeta")).write_text(json.dumps({"animation": {"frametime": 2}}) + "\n")
    (NOTICES / "ASSET_MANIFEST.json").write_text(json.dumps({"formatVersion": 1, "originalAssets": records,
        "officialPackSha256": PACK_HASH}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (NOTICES / "CTM_MANIFEST.json").write_text(json.dumps(derived, indent=2) + "\n", encoding="utf-8")
    print(f"Imported {len(records)} original assets; generated {len(derived)} CTM sprites.")


if __name__ == "__main__":
    main()
