"""Derive panel-only CTM borders from the pinned LGPL GregTech casing textures."""

import hashlib
import json
from pathlib import Path
import shutil

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
VERSION = "5.09.54.183"
UPSTREAM = "https://github.com/GTNewHorizons/GT5-Unofficial"
SOURCE_ROOT = ROOT / "build/gtnh-tooltip-reference" / VERSION
ASSET_ROOT = ROOT / "src/main/resources/assets/gtnotgood/textures/blocks/stellar/ctm"
REFERENCE = ROOT / "build/stellar-panel-ctm-reference"
LEFT, RIGHT, DOWN, UP = 1, 2, 4, 8
DOWN_LEFT, DOWN_RIGHT, UP_LEFT, UP_RIGHT = 16, 32, 64, 128
CORNERS = (
    (LEFT, DOWN, DOWN_LEFT),
    (RIGHT, DOWN, DOWN_RIGHT),
    (LEFT, UP, UP_LEFT),
    (RIGHT, UP, UP_RIGHT),
)
PROFILES = (
    ("space_bending", "EXTREME_DENSITY_CASING", 1, "circuit", "Extend the adjacent circuit pixels through the 1 px outer bevel; keep every interior circuit line."),
    ("radiation_absorbent", "RADIATION_ABSORBENT_CASING", 1, "silver", "Replace the joined 1 px outer bevel with the existing flat silver background; keep the complete circular shield and inset rivets."),
    ("spatial", "EM_OUTER_SPACETIME_REINFORCED_EOH_CASING", 1, "blue_plate", "Extend the central blue plate through the joined 1 px outer rim; preserve the inner plate and all inset corner bolts."),
    ("temporal", "EM_INNER_SPACETIME_REINFORCED_EOH_CASING", 1, "blue_field", "Extend the existing blue field through the joined 1 px outer rim; preserve the full inner radial pattern."),
    ("electromagnetic", "BEC_CASING", 1, "grid", "Extend the central silver cell through the joined 1 px perimeter, preserving intersecting interior lattice lines; keep the interior 3 by 3 mirrors unchanged."),
)


def normalize_mask(mask):
    result = mask & 15
    for horizontal, vertical, diagonal in CORNERS:
        if mask & horizontal and mask & vertical and mask & diagonal:
            result |= diagonal
    return result


def joined_pixel(source, x, y, horizontal, vertical, profile):
    if profile == "silver":
        return source.getpixel((7 + (x & 1), 7 + (y & 1)))
    if profile == "blue_plate":
        return source.getpixel((max(5, min(10, x)), max(5, min(10, y))))
    if profile == "blue_field":
        return source.getpixel((7 if horizontal else x, 7 if vertical else y))
    if profile == "grid":
        return source.getpixel((8 if horizontal else x, 8 if vertical else y))
    return source.getpixel((max(2, min(13, x)) if horizontal else x,
                            max(2, min(13, y)) if vertical else y))


def derive_tile(source, mask, border, profile):
    tile = source.copy()
    for y in range(16):
        for x in range(16):
            horizontal = LEFT if x < border else RIGHT if x >= 16 - border else 0
            vertical = UP if y < border else DOWN if y >= 16 - border else 0
            joined_horizontal = bool(horizontal and mask & horizontal)
            joined_vertical = bool(vertical and mask & vertical)
            if not joined_horizontal and not joined_vertical:
                continue
            if horizontal and vertical:
                if joined_horizontal and not joined_vertical:
                    color = source.getpixel((8, y))
                elif joined_vertical and not joined_horizontal:
                    color = source.getpixel((x, 8))
                else:
                    diagonal = next(c[2] for c in CORNERS if c[0] == horizontal and c[1] == vertical)
                    if not mask & diagonal:
                        continue
                    color = joined_pixel(source, x, y, True, True, profile)
            else:
                color = joined_pixel(source, x, y, joined_horizontal, joined_vertical, profile)
            tile.putpixel((x, y), color)
    return tile


def neighbors_mask(cells, x, y):
    offsets = ((-1, 0, LEFT), (1, 0, RIGHT), (0, 1, DOWN), (0, -1, UP),
               (-1, 1, DOWN_LEFT), (1, 1, DOWN_RIGHT), (-1, -1, UP_LEFT), (1, -1, UP_RIGHT))
    return normalize_mask(sum(bit for dx, dy, bit in offsets if (x + dx, y + dy) in cells))


def draw_wall(source, tiles, cells, connected):
    width, height = max(x for x, _ in cells) + 1, max(y for _, y in cells) + 1
    canvas = Image.new("RGBA", (width * 16, height * 16), (31, 35, 41, 255))
    for x, y in sorted(cells):
        tile = tiles[neighbors_mask(cells, x, y)] if connected else source
        canvas.paste(tile, (x * 16, y * 16))
    return canvas


def save_preview(profiles):
    cells = {(x, y) for x in range(6) for y in range(4)} - {(2, 1), (3, 1), (2, 2), (3, 2)}
    canvas = Image.new("RGB", (1024, 5 * 374 + 40), (20, 24, 29))
    draw = ImageDraw.Draw(canvas)
    draw.text((16, 12), "Panel casing CTM | original left, connected right | original 16 px pixels x5", fill="white")
    for index, (name, source, tiles) in enumerate(profiles):
        top = 42 + index * 374
        draw.text((16, top), name, fill=(236, 172, 93))
        for connected in (False, True):
            wall = draw_wall(source, tiles, cells, connected)
            enlarged = wall.resize((wall.width * 5, wall.height * 5), Image.Resampling.NEAREST)
            canvas.paste(enlarged, (16 + int(connected) * 504, top + 22))
        wall.save(REFERENCE / f"base_{name}-opening-native.png")
    canvas.save(REFERENCE / "preview.png")


def main():
    REFERENCE.mkdir(parents=True, exist_ok=True)
    masks = sorted({normalize_mask(mask) for mask in range(256)})
    assert len(masks) == 47
    records, profiles = [], []
    for name, filename, border, profile, algorithm in PROFILES:
        relative_source = f"assets/gregtech/textures/blocks/iconsets/{filename}.png"
        source_path = SOURCE_ROOT / relative_source
        source = Image.open(source_path).convert("RGBA")
        assert source.size == (16, 16)
        assert not Path(str(source_path) + ".mcmeta").exists()
        source_hash = hashlib.sha256(source_path.read_bytes()).hexdigest()
        destination = ASSET_ROOT / ("base_" + name)
        destination.mkdir(parents=True, exist_ok=True)
        source_colors = {source.getpixel((x, y)) for y in range(16) for x in range(16)}
        tiles = {}
        for mask in masks:
            tile = derive_tile(source, mask, border, profile)
            path = destination / f"{mask}.png"
            if mask == 0:
                shutil.copyfile(source_path, path)
            else:
                tile.save(path, optimize=False)
            assert {tile.getpixel((x, y)) for y in range(16) for x in range(16)} <= source_colors
            assert all(tile.getpixel((x, y)) == source.getpixel((x, y))
                       for y in range(border, 16 - border) for x in range(border, 16 - border))
            tiles[mask] = tile
            records.append({
                "profile": name, "mask": mask, "source": relative_source,
                "sourceVersion": VERSION, "sourceSha256": source_hash,
                "sourceUrl": f"{UPSTREAM}/blob/{VERSION}/src/main/resources/{relative_source}",
                "destination": str(path.relative_to(ROOT / "src/main/resources")).replace("\\", "/"),
                "destinationSha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                "modified": mask != 0, "license": "LGPL-3.0-or-later", "algorithm": algorithm,
            })
        assert (destination / "0.png").read_bytes() == source_path.read_bytes()
        profiles.append((name, source, tiles))
    generator = Path(__file__)
    report = {
        "upstream": UPSTREAM, "sourceVersion": VERSION, "license": "LGPL-3.0-or-later",
        "licenseUrl": f"{UPSTREAM}/blob/{VERSION}/LICENSE.txt",
        "creditsUrl": f"{UPSTREAM}/blob/{VERSION}/README.md#attribution",
        "sourceRoot": str(SOURCE_ROOT.relative_to(ROOT)).replace("\\", "/"),
        "dimensions": [16, 16], "assetCount": len(records),
        "generator": str(generator.relative_to(ROOT)).replace("\\", "/"),
        "generatorSha256": hashlib.sha256(generator.read_bytes()).hexdigest(),
        "algorithm": "Edit only each profile's 1 px perimeter. Extend exposed frames across perpendicular joins; keep original corner patches where both cardinal neighbours exist but the diagonal is absent. All centre pixels and internal patterns remain byte-identical. Mask 0 preserves source bytes. No interpolation, rescaling, animation or new colours.",
        "profiles": [{"name": name, "border": border, "profile": profile, "algorithm": algorithm}
                     for name, _, border, profile, algorithm in PROFILES],
        "maskBits": {"left": LEFT, "right": RIGHT, "down": DOWN, "up": UP,
                     "downLeft": DOWN_LEFT, "downRight": DOWN_RIGHT,
                     "upLeft": UP_LEFT, "upRight": UP_RIGHT},
        "assets": records,
    }
    (REFERENCE / "generation.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    save_preview(profiles)
    print(json.dumps({"tiles": len(records), "preview": str(REFERENCE / "preview.png"),
                      "report": str(REFERENCE / "generation.json")}, indent=2))


if __name__ == "__main__":
    main()
