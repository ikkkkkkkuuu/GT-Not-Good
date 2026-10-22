import hashlib
import json
import re
import time
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
REFERENCES = Path(__file__).resolve().parent / "render-upstream"
NOTICES = ROOT / "src/main/resources/META-INF/dimensional-focus-port"
GTO_REVISION = "3121e23241e7f6ccd2e6a8dbd5655962de63ed9a"
GTM_REVISION = "30cfd9fb3de3faee38cab6b36c5ba171ed82ba48"
GTM_CORROBORATION_REVISION = "d228b2929332f4a52313cfb798e4780c7c1b3013"
LDLIB_REVISION = "5d68947b2fa1db7fdaadf36922973adbd1a2a183"


def main():
    records = []
    specifications = (
        ("GregTech-Odyssey/GTOCore", GTO_REVISION, "LGPL-3.0", "gto", (
            "src/main/java/com/gtocore/client/renderer/machine/DimensionalFocusEngravingArrayRenderer.java",
            "src/main/java/com/gtocore/client/renderer/RenderHelper.java", "gradle.properties", "dependencies.gradle", "LICENSE")),
        ("GregTech-Odyssey/GregTech-Modern", GTM_REVISION, "LGPL-3.0", "gtceu-current", (
            "src/main/java/com/gregtechceu/gtceu/client/renderer/GTRenderTypes.java", "LICENSE")),
        ("GregTech-Odyssey/GregTech-Modern", GTM_CORROBORATION_REVISION, "LGPL-3.0", "gtceu", (
            "src/main/java/com/gregtechceu/gtceu/client/renderer/GTRenderTypes.java", "LICENSE")),
        ("Low-Drag-MC/LDLib-MultiLoader", LDLIB_REVISION, "GPL-3.0", "ldlib", (
            "common/src/main/java/com/lowdragmc/lowdraglib/utils/ColorUtils.java",
            "common/src/main/java/com/lowdragmc/lowdraglib/utils/interpolate/Eases.java",
            "common/src/main/java/com/lowdragmc/lowdraglib/utils/interpolate/IEase.java", "gradle.properties", "LICENSE")),
    )
    for repository, revision, license_name, group, files in specifications:
        for source_path in files:
            url = f"https://raw.githubusercontent.com/{repository}/{revision}/{source_path}"
            target = REFERENCES / group / source_path
            if target.exists():
                content = target.read_bytes()
            else:
                for attempt in range(3):
                    try:
                        with urllib.request.urlopen(url, timeout=20) as response:
                            content = response.read()
                        break
                    except OSError:
                        if attempt == 2:
                            raise
                        time.sleep(1)
            if not target.exists():
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(content)
            records.append({"source": f"https://github.com/{repository}", "revision": revision,
                            "sourcePath": source_path, "rawUrl": url, "license": license_name,
                            "referenceDestination": target.relative_to(ROOT).as_posix(),
                            "modifications": "None; raw reference copied byte-for-byte and excluded from compilation and resource packaging.",
                            "sha256": hashlib.sha256(content).hexdigest()})
            if source_path == "LICENSE":
                name = {"gto": "LICENSE-GTOCore-LGPL-3.0.txt", "gtceu-current": "LICENSE-GTO-GTCEu-LGPL-3.0.txt",
                        "gtceu": "LICENSE-GTO-GTCEu-LGPL-3.0.txt",
                        "ldlib": "LICENSE-LDLib-render-GPL-3.0.txt"}[group]
                (NOTICES / name).write_bytes(content)
                records[-1]["packagedLicenseDestination"] = (NOTICES / name).relative_to(ROOT / "src/main/resources").as_posix()
    manifest = {"formatVersion": 1, "component": "Dimensional Focus Engraving Array dynamic core renderer",
                "rawSources": records,
                "dependencyEvidence": {
                    "gtoCoreRevision": GTO_REVISION,
                    "declaredGtceuDependency": "com.gregtechceu.gtceu:gtceu-1.20.1-forge-1.20.1:26.7.3",
                    "gtceuReferenceRevision": GTM_REVISION,
                    "gtceuReferenceNote": "Pinned owning-fork source used by the local renderer. The LIGHT_RING/getLightRing state excerpt is identical to the 2026-07-03 d228b292 snapshot. This is a source-method reference; no claim of byte identity with the unavailable Maven 26.7.3 source JAR is made.",
                    "gtceuCorroborationRevision": GTM_CORROBORATION_REVISION,
                    "actualLdlibDependency": "curse.maven:ldlib-626676:7809449",
                    "actualLdlibVersion": "1.0.50",
                    "ldlibVersionEvidence": "https://www.curseforge.com/minecraft/mc-mods/ldlib/files/7809449",
                    "unusedLdlibProperty": "gradle.properties declares ldlib_version=1.0.48, but dependencies.gradle uses file 7809449 instead."},
                "artworkRequired": False,
                "renderState": {"format": "POSITION_COLOR", "primitive": "TRIANGLE_STRIP", "culling": False,
                                "shader": "Minecraft built-in POSITION_COLOR_SHADER", "transparency": "TRANSLUCENT_TRANSPARENCY",
                                "texture": None, "customShader": None},
                "geometry": {"cylinders": 6, "radius": 0.5, "height": 8.0, "sides": 10,
                             "controllerRelativeCenter": "(0.5, -17.5, 0.5) plus 22 blocks inward along front-facing axis",
                             "pulse": "0.1 + (abs((abs(offsetTimer % 100) + partialTicks) - 50) / 50)^2; blend coil ARGB toward white with upper-clamped integer channels."},
                "adaptations": []}
    excerpts = []
    for group in ("gtceu-current", "gtceu"):
        text = (REFERENCES / group / "src/main/java/com/gregtechceu/gtceu/client/renderer/GTRenderTypes.java").read_text(encoding="utf-8")
        state = re.search(r"private static final RenderType LIGHT_RING\s*=.*?\.createCompositeState\(false\)\);", text, re.S).group()
        getter = re.search(r"public static RenderType getLightRing\(\)\s*\{.*?\}", text, re.S).group()
        excerpts.append("".join((state + getter).split()))
    assert excerpts[0] == excerpts[1]
    manifest["renderState"]["normalizedStateExcerptSha256"] = hashlib.sha256(excerpts[0].encode()).hexdigest()
    manifest["renderState"]["depthTest"] = "LEQUAL (default composite state)"
    manifest["renderState"]["writeMask"] = "COLOR_DEPTH_WRITE (default composite state)"
    for filename, changes in (
        ("DimensionalFocusCoreGeometry.java", "Preserve cylinder radius/height/sides, all six cumulative rotations, the 360-vertex TRIANGLE_STRIP stream, quadratic pulse and LDLib ARGB rounding/clamping; replace PoseStack/JOML with local arithmetic and Minecraft 1.7.10 MathHelper."),
        ("DimensionalFocusCoreRenderer.java", "Replace modern WorkableCasingMachineRenderer/PoseStack/MultiBufferSource with a client-side Forge 1.7.10 world render hook, native OpenGL triangle strip rendering and local controller state. Use installed GTNHLib for the optional native bloom path; preserve the upstream untextured fallback.")):
        destination = ROOT / "src/main/java/com/xyp/gtnotgood/client/render" / filename
        if destination.exists():
            manifest["adaptations"].append({"destination": destination.relative_to(ROOT).as_posix(),
                "license": "GPL-3.0-only", "modifications": changes,
                "destinationSha256": hashlib.sha256(destination.read_bytes()).hexdigest(),
                "sources": [record["rawUrl"] for record in records if record["sourcePath"].endswith(
                    ("DimensionalFocusEngravingArrayRenderer.java", "RenderHelper.java", "GTRenderTypes.java", "ColorUtils.java", "Eases.java"))]})
    path = NOTICES / "RENDER_MANIFEST.json"
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Recorded {len(records)} pinned raw source/license files.")


if __name__ == "__main__":
    main()
