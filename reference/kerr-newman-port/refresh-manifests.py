"""Refresh destination hashes after formatting; never modifies runtime sources or original references."""
import gzip
import hashlib
import json
import struct
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REFERENCE = Path(__file__).resolve().parent
PACK = ROOT / "src/main/resources/META-INF/kerr-newman-port"
GTO = "3121e23241e7f6ccd2e6a8dbd5655962de63ed9a"
GTM = "30cfd9fb3de3faee38cab6b36c5ba171ed82ba48"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write(name, value):
    data = json.dumps(value, ensure_ascii=False, indent=2) + "\n"
    for parent in (REFERENCE, PACK):
        parent.mkdir(parents=True, exist_ok=True)
        (parent / name).write_text(data, encoding="utf8")


def destination(path, modifications, license_name="LGPL-3.0-only", sources=None):
    file = ROOT / path
    return {"destination": path, "sha256": sha(file), "license": license_name,
            "modification": modifications, "source_paths": sources or []}


def main():
    assets = json.loads((PACK / "ASSET_MANIFEST.json").read_text(encoding="utf8"))["assets"]
    for record in assets:
        assert sha(ROOT / record["destination"]) == record["sha256"], record["destination"]
    assert (REFERENCE / "RECIPE_ADAPTATION.md").read_bytes() == (PACK / "RECIPE_ADAPTATION.md").read_bytes()
    for group, name in (("gto", "GTOCore"), ("gtm", "GregTech-Modern")):
        assert (REFERENCE / "upstream" / group / "LICENSE").read_bytes() == (PACK / ("LICENSE-" + name + "-LGPL-3.0.txt")).read_bytes()
    raw = []
    for group, repo, pin in (("gto", "GregTech-Odyssey/GTOCore", GTO), ("gtm", "GregTech-Odyssey/GregTech-Modern", GTM)):
        directory = REFERENCE / "upstream" / group
        for path in sorted(directory.rglob("*")):
            if not path.is_file():
                continue
            source = path.relative_to(directory).as_posix()
            raw.append({"repository": "https://github.com/" + repo, "commit": pin, "source_path": source,
                        "url": f"https://github.com/{repo}/blob/{pin}/{source}", "license": "LGPL-3.0-only",
                        "reference_destination": path.relative_to(ROOT).as_posix(), "sha256": sha(path),
                        "modification": "None; byte-identical raw reference, excluded from compilation and resources."})
    pattern_path = "src/main/resources/pattern/kerr_newman_homogenizer.mbs"
    original = REFERENCE / "upstream/pattern/kerr_newman_homogenizer.mbs"
    raw.append({"repository": "https://github.com/GregTech-Odyssey/GTOCore", "commit": GTO,
                "source_path": pattern_path,
                "url": f"https://github.com/GregTech-Odyssey/GTOCore/blob/{GTO}/{pattern_path}",
                "license": "LGPL-3.0-only", "reference_destination": original.relative_to(ROOT).as_posix(),
                "sha256": sha(original), "modification": "None; exact original compressed structure."})
    write("SOURCE_MANIFEST.json", {"format": 1, "raw_sources": raw, "reference_excluded_from_build": True,
          "packaged_licenses": [destination("src/main/resources/META-INF/kerr-newman-port/" + name,
                                            "Byte-identical upstream license; GNU GPL text also supplies the terms incorporated by LGPLv3.", license_name)
                                for name, license_name in (("LICENSE-GTOCore-LGPL-3.0.txt", "LGPL-3.0-only"),
                                                           ("LICENSE-GregTech-Modern-LGPL-3.0.txt", "LGPL-3.0-only"),
                                                           ("LICENSE-LDLib-GPL-3.0.txt", "GPL-3.0-only"))],
          "rebuild_scripts": [destination("reference/kerr-newman-port/import-assets.py",
                                          "Pinned asset importer and animation-aware wrapper around the existing GPL CTM layout generator.", "GPL-3.0-only"),
                              destination("reference/kerr-newman-port/refresh-manifests.py",
                                          "Independent source/asset/structure verification and provenance manifest writer.", "MIT")],
          "gtm_reference_note": "GTO declares GTCEu 26.7.3. The Atomic Casing artwork is pinned to its owning fork's 30cfd9fb source snapshot; no claim of byte identity with the unavailable 26.7.3 source artifact is made.",
          "ctm_generator": {"destination": "reference/field-extruder-port/generate-connected-textures.py",
                            "sha256": sha(ROOT / "reference/field-extruder-port/generate-connected-textures.py"),
                            "license": "GPL-3.0-only", "upstream": "https://github.com/Low-Drag-MC/LDLib-MultiLoader/tree/1d4e4390ae58d16038845c651e122185c29f334b",
                            "methods": "Connections.fillSubmaps / Submap.uvs", "modification": "Existing offline quarter-tile layout implementation reused; not runtime LDLib code."}})

    binary = gzip.decompress(original.read_bytes())
    cursor = 0

    def varint():
        nonlocal cursor
        value = 0
        for shift in range(0, 35, 7):
            byte = binary[cursor]
            cursor += 1
            value |= (byte & 127) << shift
            if not byte & 128:
                return value - (1 << 32) if value >= 1 << 31 else value
        raise ValueError("invalid varint")

    header = [varint() for _ in range(6)]
    assert header == [-1, 1, 2, 0, 4, 83]
    aisles = []
    for aisle in range(83):
        assert varint() == 36
        rows = []
        for row in range(36):
            length, = struct.unpack_from(">H", binary, cursor)
            cursor += 2
            text = binary[cursor:cursor + length].decode("ascii")
            cursor += length
            assert len(text) == 83
            rows.append(text)
        aisles.append(rows)
    assert cursor == len(binary)
    canonical = "\n".join(row for aisle in aisles for row in aisle).encode("ascii")
    assert hashlib.sha256(canonical).hexdigest() == "74f42886b775152571cd79824b5144e7907b2fc3a9aa69680f20f7ef0d86eec3"
    target_path = "src/main/resources/assets/gtnotgood/structures/kerr_newman_homogenizer.mbs"
    assert (ROOT / target_path).read_bytes() == original.read_bytes()
    write("STRUCTURE_MANIFEST.json", {"format": 1, "repository": "https://github.com/GregTech-Odyssey/GTOCore",
          "commit": GTO, "license": "LGPL-3.0-only", "source_path": pattern_path, "source_sha256": sha(original),
          "compressed_bytes": len(original.read_bytes()), "uncompressed_bytes": len(binary), "native_dimensions": [83, 36, 83],
          "native_axes": ["LEFT", "UP", "FRONT"], "native_controller_aisle_row_column": [75, 23, 41],
          "structurelib_controller_offset_xyz": [41, 12, 7], "source_canonical_rows_sha256": hashlib.sha256(canonical).hexdigest(),
          "source_character_counts": dict(sorted(Counter(canonical.decode().replace("\n", "")).items())),
          "character_mapping": "See README.md; exact MultiBlockH.java KERR_NEWMAN_HOMOGENIZER registration.",
          "adaptations": [destination(target_path, "None; byte-identical native MBS resource.", sources=[pattern_path]),
              destination("src/main/java/com/xyp/gtnotgood/common/machines/multiblock/structure/KerrNewmanHomogenizerStructure.java",
                          "Read original gzip/varint/modified-UTF MBS. result[35-row][82-aisle] preserves LEFT/UP/FRONT orientation; only controller b becomes StructureLib ~. All spaces remain any predicates.", sources=[pattern_path])],
          "tests": "KerrNewmanHomogenizerStructureTest verifies every inverse coordinate using the canonical SHA256, all counts, binary identity, offset and defensive copies."})

    render_sources = [entry for entry in raw if ("/client/renderer/" in entry["source_path"] or "/shaders/core/black_hole_event_horizon." in entry["source_path"] or entry["source_path"].endswith("/electric/KerrNewmanHomogenizer.java"))]
    render_changes = {
        "src/main/java/com/xyp/gtnotgood/client/render/KerrNewmanBlackHoleMath.java": "Preserve source radii, growth/shrink timing, screen projection, sphere positions and detail thresholds. Swap the last two vertices of each source triangle for outward CCW winding: the modern helper's inward winding otherwise conflicts with gl_FrontFacing under the 1.7.10 state.",
        "src/main/java/com/xyp/gtnotgood/client/render/KerrNewmanBlackHoleRenderer.java": "Replace modern FXManager/PoseStack/JOML/MultiBufferSource with Forge 1.7.10 world rendering and native OpenGL. Capture the actual world target with installed GTNHLib CustomFramebuffer; use framebuffer blit when supported, copyTex fallback otherwise. Restore GL/FBO state and resources on reload. Preserve a world-fixed ending animation after the controller stops. Optional ring code disabled upstream remains disabled.",
        "src/main/resources/assets/gtnotgood/shaders/kerrnewman/black_hole_event_horizon.vsh": "Adapt GLSL 150 Position/Color vertex inputs to GLSL 120 gl_Vertex/gl_Color built-ins and compatibility-profile matrices. Preserve the upstream screen-space effect inputs.",
        "src/main/resources/assets/gtnotgood/shaders/kerrnewman/black_hole_event_horizon.fsh": "Adapt GLSL 150 texture/in/out syntax to GLSL 120 texture2D/varying/gl_FragColor. Preserve refraction/discard/color formulas; add ScreenOrigin for nonzero native viewport origins.",
    }
    write("RENDER_MANIFEST.json", {"format": 1, "component": "Kerr-Newman Homogenizer black-hole scene refraction",
          "raw_sources": render_sources, "source_event": "KerrNewmanHomogenizer.tickClient -> KerrNewmanFX.addFX -> FXManager.addFX(new BlackHole(center,2.5,4.5))",
          "geometry": {"center": "controller block center plus 34 blocks opposite its front face", "core_radius": 2.5, "event_horizon_radius": 4.5, "growth_ticks": 60, "distortion_strength": 0.12, "extra_rings": False},
          "texture_inputs": "Runtime scene color captured from the active world framebuffer; no external texture artwork is required.",
          "historical_credit": "Upstream KerrNewmanHomogenizerRenderer credits a prior GTNL effect under LGPLv3; that file's optional rainbow rings are disabled and are not activated by this port.",
          "adaptations": [destination(path, changes, sources=[entry["source_path"] for entry in render_sources]) for path, changes in render_changes.items() if (ROOT / path).exists()]})

    recipe_sources = ["src/main/java/com/gtocore/data/recipe/classified/" + name + ".java" for name in
                      ("Assembler", "AssemblerA", "AssemblyLine", "Vanilla", "ArcFurnace", "PrecisionAssembler")]
    recipe_sources.append("src/main/java/com/gtocore/data/recipe/Temporary.java")
    recipe_changes = "Adapt pinned recipe costs to native GTNH recipe maps, native available materials and stack limits; see packaged NOTICE.md and RECIPE_ADAPTATION.md for all substitutions and explicitly omitted unsupported room gates. No modern machine APIs are compiled."
    recipe_paths = ["src/main/java/com/xyp/gtnotgood/common/recipe/machine/KerrNewmanCasingRecipes.java", "src/main/java/com/xyp/gtnotgood/common/recipe/machine/KerrNewmanHomogenizerRecipes.java"]
    production = sorted((ROOT / "src/main/java/com/xyp/gtnotgood/common/blocks/kerrnewman").glob("*.java"))
    production.extend(sorted((ROOT / "src/main/java/com/xyp/gtnotgood/client/render").glob("KerrNewman*Texture.java")))
    production.extend([ROOT / "src/main/java/com/xyp/gtnotgood/client/render/KerrNewmanConnectedIcons.java", ROOT / "src/main/java/com/xyp/gtnotgood/client/render/KerrNewmanConnections.java"])
    write("PORT_MANIFEST.json", {"format": 1,
          "block_registration": {"method": "KerrNewmanBlocks.register", "texture_page": 122, "registry_ids": ["kerr_newman_casing", "kerr_newman_casing_2", "kerr_newman_glass"], "variants": 23, "active_bit": 8, "active_metadata": {"AcceleratorCoilUhv": 11, "AdvancedFusionCoil": 12}},
          "native_machine": destination("src/main/java/com/xyp/gtnotgood/common/machines/multiblock/modularmachine/KerrNewmanHomogenizer.java",
              "Native GTNH mixer ProcessingLogic and StructureLib integration around the separately licensed original MBS geometry. User-selected balance: fixed 6000% speed (duration divided by 60) and perfect overclocking (four times EU/t and duration divided by four per overclock). Preserve native energy/voltage protection and power-panel parallel control. These rates are local balance choices, not reconstructed closed GTOLib internals. Replace modern hatch predicates/client state with native equivalents; preserve source block mapping and black-hole location. Reuse existing Field Extruder fusion-reactor controller overlays with their existing attribution.", "LGPL-3.0-only",
              ["src/main/java/com/gtocore/common/data/machines/MultiBlockH.java", "src/main/java/com/gtocore/common/machine/multiblock/electric/KerrNewmanHomogenizer.java"]),
          "native_adapters": [destination(path.relative_to(ROOT).as_posix(), "Independent GTNH 1.7.10 block/item/StructureLib/CTM adapter. Original upstream art remains LGPL; native renderer supplies separately lit emissive overlays for the two active coils.", "MIT") for path in production if path.exists()],
          "recipe_adaptations": [destination(path, recipe_changes, "LGPL-3.0-only", recipe_sources) for path in recipe_paths if (ROOT / path).exists()],
          "recipe_audit": destination("src/main/resources/META-INF/kerr-newman-port/RECIPE_ADAPTATION.md",
                                      "Preserve cumulative controller audit and closed-GTOLib boundary; document all native casing material/form/recipe-facility substitutions.", "MIT", recipe_sources)})
    print(f"Verified {len(assets)} packaged texture/metadata hashes and exact original structure; refreshed {len(raw)} raw source/license records and final destination hashes.")


if __name__ == "__main__":
    main()
