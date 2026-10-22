# SPDX-License-Identifier: GPL-3.0-only
# Reuses the recorded LDLib quarter-tile layout adapter without changing the original assets.

import argparse
import hashlib
import json
import runpy
from pathlib import Path

from PIL import Image


def generate(texture_root):
    layout = runpy.run_path(str(Path(__file__).resolve().parents[1] / "field-extruder-port"
                               / "generate-connected-textures.py"))
    normalize, compose = layout["normalize"], layout["compose"]
    masks = [mask for mask in range(256) if normalize(mask) == mask]
    assert len(masks) == 47
    records = []
    for name in ("filter_casing", "laminated_glass", "tempered_glass", "molybdenum_disilicide_coil",
                 "molybdenum_disilicide_coil_bloom"):
        base = Image.open(texture_root / (name + ".png")).convert("RGBA")
        atlas_name = name + "_ctm"
        atlas = Image.open(texture_root / "source_ctm" / (atlas_name + ".png")).convert("RGBA")
        assert base.size == (16, 16) and atlas.size == (32, 32)
        target = texture_root / "ctm" / name
        target.mkdir(parents=True, exist_ok=True)
        for mask in masks:
            image = compose(base, atlas, mask)
            if mask == 0:
                assert image.tobytes() == base.tobytes()
            path = target / (str(mask) + ".png")
            image.save(path, optimize=False)
            records.append({"destination": path.relative_to(texture_root).as_posix(), "mask": mask,
                            "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
    assert len(records) == 235
    return records


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("texture_root", type=Path)
    parser.add_argument("--manifest", type=Path)
    args = parser.parse_args()
    records = generate(args.texture_root)
    if args.manifest:
        args.manifest.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Generated and verified 235 assembly plant connected textures from the original quarter tiles.")
