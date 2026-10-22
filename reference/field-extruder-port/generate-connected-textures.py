# SPDX-License-Identifier: GPL-3.0-only
# LDLib-MultiLoader 1d4e4390ae58d16038845c651e122185c29f334b: Connections.fillSubmaps / Submap.uvs.

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image


def normalize(mask):
    result = mask & 15
    for sides, diagonal in ((5, 16), (6, 32), (9, 64), (10, 128)):
        if result & sides == sides:
            result |= mask & diagonal
    return result


def compose(base, connected, mask):
    output = Image.new("RGBA", base.size)
    quarters = ((4, 1, 16, 4, 18, 0, 8), (4, 2, 32, 5, 19, 8, 8),
                (8, 2, 128, 1, 17, 8, 0), (8, 1, 64, 0, 16, 0, 0))
    for vertical, horizontal, diagonal, offset, fallback, x, y in quarters:
        tile = fallback
        if mask & (vertical | horizontal):
            tile = offset if mask & (vertical | horizontal | diagonal) == vertical | horizontal | diagonal else (
                offset + (2 if mask & vertical else 0) + (8 if mask & horizontal else 0))
        if tile > 15:
            bx, by = (tile - 16) % 2 * 8, (tile - 16) // 2 * 8
            part = base.crop((bx, by, bx + 8, by + 8))
        else:
            cx, cy = tile % 4 * 8, tile // 4 * 8
            part = connected.crop((cx, cy, cx + 8, cy + 8))
        output.paste(part, (x, y))
    return output


def generate(texture_root):
    records = []
    masks = [mask for mask in range(256) if normalize(mask) == mask]
    assert len(masks) == 47
    for name in ("iridium_casing", "hyper_mechanical_casing", "cleanroom_glass"):
        base = Image.open(texture_root / (name + ".png")).convert("RGBA")
        connected = Image.open(texture_root / "source_ctm" / (name + "_ctm.png")).convert("RGBA")
        assert base.size == (16, 16) and connected.size == (32, 32)
        target = texture_root / "ctm" / name
        target.mkdir(parents=True, exist_ok=True)
        for mask in masks:
            image = compose(base, connected, mask)
            if mask == 0:
                assert image.tobytes() == base.tobytes()
            path = target / (str(mask) + ".png")
            image.save(path, optimize=False)
            records.append({"destination": path.relative_to(texture_root).as_posix(), "mask": mask,
                            "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
    assert len(records) == 141
    return records


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("texture_root", type=Path)
    parser.add_argument("--manifest", type=Path)
    args = parser.parse_args()
    records = generate(args.texture_root)
    if args.manifest:
        args.manifest.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Generated and verified 141 connected textures from the original LDLib quarter-tile layouts.")
