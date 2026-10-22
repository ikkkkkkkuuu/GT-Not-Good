# SPDX-License-Identifier: GPL-3.0-only
"""Import pinned original art; bake LDLib CTM quarters for Minecraft 1.7.10."""
import concurrent.futures
import hashlib
import importlib.util
import json
import shutil
import time
import urllib.request
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
REFERENCE = Path(__file__).resolve().parent
CACHE = Path("C:/Users/Administrator/AppData/Local/Temp/gtng-gto-render-audit-3121e232")
GTO = "3121e23241e7f6ccd2e6a8dbd5655962de63ed9a"
GTM = "30cfd9fb3de3faee38cab6b36c5ba171ed82ba48"
TARGET = ROOT / "src/main/resources/assets/gtnotgood/textures/blocks/kerrnewman"
PACK = ROOT / "src/main/resources/META-INF/kerr-newman-port"
VARIANTS = [
    ("EnhancedHyperCasing", "enhanced_hyper_casing", "casings/enhance_hyper_mechanical_casing"),
    ("ZirconiaCeramicCasing", "zirconia_ceramic_casing", "zirconia_ceramic_high_strength_bending_resistance_mechanical_block"),
    ("PressureContainmentCasing", "pressure_containment_casing", "casings/pressure_containment_casing"),
    ("AmprosiumGearbox", "amprosium_gearbox", "neutronium_gearbox"),
    ("AmprosiumPipeCasing", "amprosium_pipe_casing", "neutronium_pipe_casing"),
    ("OilGasPipeCasing", "oil_gas_pipe_casing", "casings/oil_gas_transportation_pipe_casing"),
    ("HighPressurePipeCasing", "high_pressure_pipe_casing", "casings/high_pressure_pipe_casing"),
    ("GravityStabilizationCasing", "gravity_stabilization_casing", "gravity_stabilization_casing"),
    ("IndustrialFramelessGlass", "industrial_frameless_glass", "industrial_frameless_glass"),
    ("ShieldedAcceleratorCasing", "shielded_accelerator_casing", "shielded_accelerator"),
    ("BioactiveCasing", "bioactive_casing", "casings/bioactive_mechanical_casing"),
    ("AmprosiumActiveCasing", "amprosium_active_casing", "neutronium_active_casing"),
    ("HighStrengthSupportSpindle", "high_strength_support_spindle", "casings/high_strength_support_spindle"),
    ("AcceleratorCoilUhv", "accelerator_coil_uhv", "casings/accelerator/accelerator_electromagnetic_coil_constraint_casing_uhv"),
    ("ExtremeDensityCasing", "extreme_density_casing", "extreme_density_casing"),
    ("DimensionalBridgeCasing", "dimensional_bridge_casing", "dimensional_bridge_casing"),
    ("PpsCorrosionResistantCasing", "pps_corrosion_resistant_casing", "pps_corrosion_resistant_mechanical_housing"),
    ("QuantumGlass", "quantum_glass", "casings/quantum_glass"),
    ("PlasmaHeaterCasing", "plasma_heater_casing", "plasma_heater_casing"),
    ("AtomicCasing", "atomic_casing", "casings/gcym/atomic_casing"),
    ("RadiationAbsorbentCasing", "radiation_absorbent_casing", "radiation_absorbent_casing"),
    ("FusionCasingMkIV", "fusion_casing_mk4", "casings/fusion/fusion_casing_mk4"),
    ("AdvancedFusionCoil", "advanced_fusion_coil", "advanced_compressed_fusion_coil"),
]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def fetch(repo, pin, source):
    target = REFERENCE / "upstream" / ("gto" if pin == GTO else "gtm") / source
    target.parent.mkdir(parents=True, exist_ok=True)
    if not target.exists():
        cached = CACHE / source
        if pin == GTO and cached.exists():
            shutil.copyfile(cached, target)
        else:
            url = f"https://raw.githubusercontent.com/{repo}/{pin}/{source}"
            for attempt in range(5):
                try:
                    target.write_bytes(urllib.request.urlopen(url, timeout=45).read())
                    break
                except Exception:
                    if attempt == 4:
                        raise
                    time.sleep(1 + attempt)
    return target


def animation(metadata):
    return json.loads(metadata.read_text(encoding="utf8")).get("animation") if metadata.exists() else None


def frames(path, tile_size):
    image = Image.open(path).convert("RGBA")
    if image.width != tile_size or image.height % tile_size:
        raise ValueError(f"Unexpected source dimensions {path}: {image.size}")
    return [image.crop((0, y, tile_size, y + tile_size)) for y in range(0, image.height, tile_size)]


def main():
    tree_file = CACHE / "tree.json"
    if tree_file.exists():
        tree = {entry["path"] for entry in json.loads(tree_file.read_text(encoding="utf8"))["tree"]}
    else:
        originals = REFERENCE / "upstream/gto"
        tree = {path.relative_to(originals).as_posix() for path in originals.rglob("*") if path.is_file()}
    gto_repo = "GregTech-Odyssey/GTOCore"
    gtm_repo = "GregTech-Odyssey/GregTech-Modern"
    requests = set()
    layers = {}
    for enum, target, source in VARIANTS:
        repo, pin = (gtm_repo, GTM) if enum == "AtomicCasing" else (gto_repo, GTO)
        prefix = "src/main/resources/assets/" + ("gtceu" if pin == GTM else "gtocore") + "/textures/block/"
        source_layers = [(target, prefix + source)]
        if enum == "AdvancedFusionCoil":
            source_layers.append((target + "_bloom", prefix + "fusion_coil_bloom"))
        if enum == "AcceleratorCoilUhv":
            source_layers.append((target + "_bloom", prefix + source + "_bloom"))
        for name, stem in source_layers:
            paths = []
            for suffix in (".png", ".png.mcmeta", "_ctm.png", "_ctm.png.mcmeta"):
                path = stem + suffix
                if pin == GTM or path in tree:
                    # Atomic has no animation metadata on its CTM image.
                    if pin == GTM and suffix == "_ctm.png.mcmeta":
                        continue
                    requests.add((repo, pin, path))
                    paths.append((suffix, path))
            layers[name] = (repo, pin, paths)
    for path in (
        "LICENSE",
        "src/main/java/com/gtocore/common/data/GTOBlocks.java",
        "src/main/java/com/gtocore/common/data/machines/MultiBlockH.java",
        "src/main/java/com/gtocore/common/machine/multiblock/electric/KerrNewmanHomogenizer.java",
        "src/main/java/com/gtocore/data/recipe/classified/Assembler.java",
        "src/main/java/com/gtocore/data/recipe/classified/AssemblerA.java",
        "src/main/java/com/gtocore/data/recipe/classified/AssemblyLine.java",
        "src/main/java/com/gtocore/data/recipe/classified/Vanilla.java",
        "src/main/java/com/gtocore/data/recipe/classified/ArcFurnace.java",
        "src/main/java/com/gtocore/data/recipe/Temporary.java",
        "src/main/java/com/gtocore/data/recipe/classified/PrecisionAssembler.java",
        "src/main/java/com/gtocore/client/renderer/GTORenderTypes.java",
        "src/main/java/com/gtocore/client/renderer/fx/BlackHole.java",
        "src/main/java/com/gtocore/client/renderer/fx/AbstractFX.java",
        "src/main/java/com/gtocore/client/renderer/fx/FXManager.java",
        "src/main/java/com/gtocore/client/renderer/fx/ScreenSpaceMeshRenderer.java",
        "src/main/java/com/gtocore/client/renderer/fx/ScreenSpaceProjection.java",
        "src/main/java/com/gtocore/client/renderer/fx/ScreenSpaceSceneCapture.java",
        "src/main/java/com/gtocore/client/renderer/RenderHelper.java",
        "src/main/java/com/gtocore/client/renderer/machine/KerrNewmanHomogenizerRenderer.java",
        "src/main/resources/assets/gtocore/shaders/core/black_hole_event_horizon.fsh",
        "src/main/resources/assets/gtocore/shaders/core/black_hole_event_horizon.vsh",
        "src/main/resources/assets/gtocore/shaders/core/black_hole_event_horizon.json",
    ):
        requests.add((gto_repo, GTO, path))
    requests.add((gtm_repo, GTM, "LICENSE"))
    # Keep generated model declarations as evidence for layer order and active-state choice.
    names = [source.rsplit("/", 1)[-1] for _, _, source in VARIANTS]
    names.extend(("advanced_fusion_coil", "dimensional_bridge_casing", "amprosium_pipe_casing", "amprosium_gearbox"))
    for path in tree:
        if "/models/block/" in path and path.endswith(".json") and any(path.rsplit("/", 1)[-1] in (name + ".json", name + "_active.json") for name in names):
            requests.add((gto_repo, GTO, path))
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
        list(executor.map(lambda args: fetch(*args), sorted(requests)))
    PACK.mkdir(parents=True, exist_ok=True)
    TARGET.mkdir(parents=True, exist_ok=True)
    records = []
    layer_data = {}
    for name, (repo, pin, paths) in layers.items():
        for suffix, source in paths:
            original = fetch(repo, pin, source)
            target = TARGET / (("source_ctm/" + name + suffix) if suffix.startswith("_ctm") else name + suffix)
            target.parent.mkdir(parents=True, exist_ok=True)
            if suffix.endswith("mcmeta"):
                metadata_copy = PACK / "source-metadata" / (name + suffix)
                metadata_copy.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(original, metadata_copy)
                records.append({"source": source, "repository": repo, "commit": pin, "license": "LGPL-3.0-only", "source_sha256": sha(original), "destination": metadata_copy.relative_to(ROOT).as_posix(), "sha256": sha(metadata_copy), "modification": "Byte-identical original modern-loader metadata, retained as attribution/reference rather than loaded by 1.7.10."})
                metadata = json.loads(original.read_text(encoding="utf8"))
                if "animation" in metadata:
                    target.write_text(json.dumps({"animation": metadata["animation"]}, separators=(",", ":")) + "\n", encoding="utf8")
                else:
                    continue
                modification = "Retain original animation; remove unsupported LDLib connection directive (baked below)."
            else:
                shutil.copyfile(original, target)
                modification = "Byte-identical original PNG, resource namespace/path changed only."
            records.append({"source": source, "repository": repo, "commit": pin, "license": "LGPL-3.0-only", "source_sha256": sha(original), "destination": target.relative_to(ROOT).as_posix(), "sha256": sha(target), "modification": modification})
        layer_data[name] = {"repo": repo, "pin": pin, "paths": paths}
    spec = importlib.util.spec_from_file_location("ldlib_ctm", ROOT / "reference/field-extruder-port/generate-connected-textures.py")
    ctm = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(ctm)
    masks = [mask for mask in range(256) if ctm.normalize(mask) == mask]
    for name, data in layer_data.items():
        base_path, atlas_path = TARGET / (name + ".png"), TARGET / "source_ctm" / (name + "_ctm.png")
        if not atlas_path.exists():
            continue
        base, atlas = frames(base_path, 16), frames(atlas_path, 32)
        count = max(len(base), len(atlas))
        if count % len(base) or count % len(atlas):
            raise ValueError(f"Incompatible frame lengths: {name}")
        animated = animation(atlas_path.with_suffix(".png.mcmeta")) or animation(base_path.with_suffix(".png.mcmeta"))
        for mask in masks:
            target = TARGET / "ctm" / name / (str(mask) + ".png")
            target.parent.mkdir(parents=True, exist_ok=True)
            output = Image.new("RGBA", (16, 16 * count))
            for index in range(count):
                output.paste(ctm.compose(base[index % len(base)], atlas[index % len(atlas)], mask), (0, index * 16))
            output.save(target)
            if animated:
                target_metadata = target.with_suffix(".png.mcmeta")
                target_metadata.write_text(json.dumps({"animation": animated}, separators=(",", ":")) + "\n", encoding="utf8")
                records.append({"repository": data["repo"], "commit": data["pin"], "license": "LGPL-3.0-only", "destination": target_metadata.relative_to(ROOT).as_posix(), "sha256": sha(target_metadata), "source_metadata": [path for suffix, path in data["paths"] if suffix.endswith("mcmeta")], "mask": mask, "modification": "Original animation order, per-frame timing and interpolation request retained for the composed sprite."})
            records.append({"repository": data["repo"], "commit": data["pin"], "license": "LGPL-3.0-only", "destination": target.relative_to(ROOT).as_posix(), "sha256": sha(target), "source_layers": [path for suffix, path in data["paths"] if suffix.endswith("png")], "mask": mask, "modification": "Original LDLib quarter-tile selection, 47 normalized masks; vertical animation frames retained. No repainting."})
    for name, repo, pin in (("GTOCore", gto_repo, GTO), ("GregTech-Modern", gtm_repo, GTM)):
        shutil.copyfile(fetch(repo, pin, "LICENSE"), PACK / ("LICENSE-" + name + "-LGPL-3.0.txt"))
    shutil.copyfile(ROOT / "src/main/resources/META-INF/dimensional-focus-port/LICENSE-LDLib-GPL-3.0.txt", PACK / "LICENSE-LDLib-GPL-3.0.txt")
    for destination in (PACK / "ASSET_MANIFEST.json", REFERENCE / "ASSET_MANIFEST.json"):
        destination.write_text(json.dumps({"format": 1, "generator": "reference/kerr-newman-port/import-assets.py", "assets": records}, ensure_ascii=False, indent=2) + "\n", encoding="utf8")
    print(f"Imported {len(requests)} pinned originals; wrote {len(records)} texture records.")
    print("Connected variants: " + ", ".join(name for name in layer_data if (TARGET / "source_ctm" / (name + "_ctm.png")).exists()))
    for name in layer_data:
        image = Image.open(TARGET / (name + ".png"))
        print(name, image.size, animation(TARGET / (name + ".png.mcmeta")))


if __name__ == "__main__":
    main()
