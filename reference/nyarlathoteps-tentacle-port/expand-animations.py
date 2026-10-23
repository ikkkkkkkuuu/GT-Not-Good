# SPDX-License-Identifier: GPL-3.0-only
"""Bake modern per-tick RGB interpolation for the 1.7.10 animation player."""
import hashlib
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
TEXTURES = RES / "assets/gtnotgood/textures/blocks"
MANIFEST = RES / "META-INF/nyarlathoteps-tentacle-port/ASSET_MANIFEST.json"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def expand(source, target):
    metadata_path = source.with_suffix(".png.mcmeta")
    animation = json.loads(metadata_path.read_text())["animation"]
    image = Image.open(source).convert("RGBA")
    size = image.width
    frames = [image.crop((0, y, size, y + size)) for y in range(0, image.height, size)]
    timeline = animation.get("frames", list(range(len(frames))))
    sequence = [(f["index"], f.get("time", animation.get("frametime", 1))) if isinstance(f, dict)
                else (f, animation.get("frametime", 1)) for f in timeline]
    unique, indices, pixels = {}, [], []
    for i, (index, duration) in enumerate(sequence):
        current = list(frames[index].getdata())
        following = list(frames[sequence[(i + 1) % len(sequence)][0]].getdata())
        for tick in range(duration):
            # Vanilla interpolates RGB and keeps the current source frame's alpha.
            weight = 1.0 - tick / duration
            data = bytes(channel for a, b in zip(current, following)
                         for channel in (*[int(weight * a[c] + (1.0 - weight) * b[c]) for c in range(3)], a[3]))
            if data not in unique:
                unique[data] = len(pixels)
                pixels.append(data)
            indices.append(unique[data])
    result = Image.new("RGBA", (size, size * len(pixels)))
    for index, data in enumerate(pixels):
        result.paste(Image.frombytes("RGBA", (size, size), data), (0, size * index))
    target.parent.mkdir(parents=True, exist_ok=True)
    result.save(target)
    target_meta = target.with_suffix(".png.mcmeta")
    target_meta.write_text(json.dumps({"animation": {"frametime": 1, "frames": indices}}, separators=(",", ":")) + "\n")
    return {"source": source.relative_to(RES).as_posix(), "sourceSha256": sha(source),
            "sourceMetadataSha256": sha(metadata_path), "destination": target.relative_to(RES).as_posix(),
            "sha256": sha(target), "metadataSha256": sha(target_meta), "cycleTicks": len(indices),
            "storedFrames": len(pixels), "license": "LGPL-3.0-only",
            "modification": "Per-tick RGB interpolation; source alpha, frame order and duration retained; duplicate frames shared."}


def main():
    records = []
    for group in ("tentacle", "kerrnewman"):
        folder = TEXTURES / group
        for source in sorted(folder.rglob("*.png")):
            relative = source.relative_to(folder)
            if "animated" in relative.parts or "source_ctm" in relative.parts:
                continue
            if group == "kerrnewman" and "dimensional_bridge_casing" not in relative.as_posix():
                continue
            metadata = source.with_suffix(".png.mcmeta")
            if not metadata.exists() or not json.loads(metadata.read_text()).get("animation", {}).get("interpolate"):
                continue
            records.append(expand(source, folder / "animated" / relative))
    manifest = json.loads(MANIFEST.read_text())
    manifest["interpolatedAnimations"] = records
    manifest["animationGenerator"] = "reference/nyarlathoteps-tentacle-port/expand-animations.py"
    MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Expanded {len(records)} animations; original assets unchanged.")


if __name__ == "__main__":
    main()
