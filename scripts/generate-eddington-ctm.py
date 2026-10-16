"""Derive native 16 px blob-CTM tiles from the imported MIT Eddington casing."""

import hashlib
import json
from pathlib import Path
import shutil

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/resources/assets/gtnotgood/textures/blocks/stellar/sl_eddington.png"
DESTINATION = SOURCE.parent / "ctm"
REFERENCE = ROOT / "build/eddington-ctm-reference"
LEFT, RIGHT, DOWN, UP = 1, 2, 4, 8
DOWN_LEFT, DOWN_RIGHT, UP_LEFT, UP_RIGHT = 16, 32, 64, 128
CORNERS = (
    (LEFT, DOWN, DOWN_LEFT, 0, 14),
    (RIGHT, DOWN, DOWN_RIGHT, 14, 14),
    (LEFT, UP, UP_LEFT, 0, 0),
    (RIGHT, UP, UP_RIGHT, 14, 0),
)


def normalize_mask(mask):
    result = mask & 15
    for horizontal, vertical, diagonal, _, _ in CORNERS:
        if mask & horizontal and mask & vertical and mask & diagonal:
            result |= diagonal
    return result


def derive_tile(source, mask):
    tile = source.copy()
    for y in range(16):
        for x in range(16):
            connected_x = (x < 2 and mask & LEFT) or (x > 13 and mask & RIGHT)
            connected_y = (y < 2 and mask & UP) or (y > 13 and mask & DOWN)
            if connected_x or connected_y:
                tile.putpixel((x, y), source.getpixel((max(2, min(13, x)), max(2, min(13, y)))))

    # The source grille begins three pixels in; extend it through the complete joined band.
    for y in (4, 7, 10):
        if mask & LEFT:
            for x in range(3):
                tile.putpixel((x, y), source.getpixel((3, y)))
        if mask & RIGHT:
            for x in range(13, 16):
                tile.putpixel((x, y), source.getpixel((12, y)))

    # An exposed frame continues across the perpendicular join rather than being clipped by it.
    for horizontal, vertical, diagonal, corner_x, corner_y in CORNERS:
        horizontal_connected = bool(mask & horizontal)
        vertical_connected = bool(mask & vertical)
        for y in range(corner_y, corner_y + 2):
            for x in range(corner_x, corner_x + 2):
                if horizontal_connected and not vertical_connected:
                    tile.putpixel((x, y), source.getpixel((7, y)))
                elif vertical_connected and not horizontal_connected:
                    tile.putpixel((x, y), source.getpixel((x, 8)))
                elif horizontal_connected and vertical_connected and not mask & diagonal:
                    tile.putpixel((x, y), source.getpixel((x, y)))
    return tile


def neighbors_mask(cells, x, y):
    offsets = (
        (-1, 0, LEFT), (1, 0, RIGHT), (0, 1, DOWN), (0, -1, UP),
        (-1, 1, DOWN_LEFT), (1, 1, DOWN_RIGHT), (-1, -1, UP_LEFT), (1, -1, UP_RIGHT),
    )
    return normalize_mask(sum(bit for dx, dy, bit in offsets if (x + dx, y + dy) in cells))


def draw_wall(cells, source, tiles, connected):
    width, height = max(x for x, _ in cells) + 1, max(y for _, y in cells) + 1
    canvas = Image.new("RGBA", (width * 16, height * 16), (44, 51, 59, 255))
    for x, y in sorted(cells):
        tile = tiles[neighbors_mask(cells, x, y)] if connected else source
        canvas.paste(tile, (x * 16, y * 16))
    return canvas


def save_previews(source, tiles):
    examples = (
        ("Single / horizontal / vertical", {(0, 0), (2, 0), (3, 0), (5, 0), (5, 1)}),
        ("Solid wall", {(x, y) for x in range(6) for y in range(4)}),
        ("Inner corners / opening", {(x, y) for x in range(6) for y in range(4)} - {(2, 1), (3, 1), (2, 2), (3, 2)}),
        ("Stairs / concave corners", {(x, y) for x in range(6) for y in range(4) if x <= y + 1}),
    )
    scale = 5
    preview = Image.new("RGB", (1024, 1440), (20, 24, 29))
    draw = ImageDraw.Draw(preview)
    draw.text((16, 12), "Eddington native blob-CTM | original left, connected right | nearest-neighbor x5", fill="white")
    top = 40
    for name, cells in examples:
        draw.text((16, top), name, fill=(236, 172, 93))
        top += 20
        for connected in (False, True):
            wall = draw_wall(cells, source, tiles, connected)
            native_name = name.lower().replace(" / ", "-").replace(" ", "-")
            if connected:
                wall.save(REFERENCE / (native_name + "-native.png"))
            enlarged = wall.resize((wall.width * scale, wall.height * scale), Image.Resampling.NEAREST)
            preview.paste(enlarged, (16 + int(connected) * 504, top))
        top += max(y for _, y in cells) * 16 * scale + 16 * scale + 26
    preview.crop((0, 0, preview.width, top)).save(REFERENCE / "preview.png")

    atlas = Image.new("RGB", (8 * 96, 6 * 112), (20, 24, 29))
    draw = ImageDraw.Draw(atlas)
    for index, (mask, tile) in enumerate(sorted(tiles.items())):
        x, y = index % 8 * 96, index // 8 * 112
        atlas.paste(tile.resize((80, 80), Image.Resampling.NEAREST), (x + 8, y + 6))
        draw.text((x + 8, y + 90), str(mask), fill="white")
    atlas.save(REFERENCE / "tiles.png")


def main():
    DESTINATION.mkdir(parents=True, exist_ok=True)
    REFERENCE.mkdir(parents=True, exist_ok=True)
    source = Image.open(SOURCE).convert("RGBA")
    assert source.size == (16, 16)
    masks = sorted({normalize_mask(mask) for mask in range(256)})
    assert len(masks) == 47
    tiles = {}
    records = []
    for mask in masks:
        path = DESTINATION / f"eddington_{mask}.png"
        tile = derive_tile(source, mask)
        if mask == 0:
            shutil.copyfile(SOURCE, path)
        else:
            tile.save(path, optimize=False)
        tiles[mask] = tile
        records.append({"mask": mask, "path": str(path.relative_to(ROOT)).replace("\\", "/"),
                        "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
    assert (DESTINATION / "eddington_0.png").read_bytes() == SOURCE.read_bytes()
    assert len({tile.tobytes() for tile in tiles.values()}) == 47
    assert all(tile.getpixel((7, 7)) == source.getpixel((7, 7)) for tile in tiles.values())
    assert all(tile.getpixel((x, y))[3] == 255 for tile in tiles.values() for y in range(16) for x in range(16))
    report = {
        "source": str(SOURCE.relative_to(ROOT)).replace("\\", "/"),
        "sourceSha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        "license": "MIT; derived from the existing Beyond the Stars Eddington casing import",
        "dimensions": [16, 16],
        "generator": "scripts/generate-eddington-ctm.py",
        "algorithm": "Keep the original metal interior and exposed 2 px frame; replace joined frame bands with nearest interior metal pixels; extend the orange grille across horizontal joins; continue exposed frames across perpendicular joins; restore native 2x2 corner patches for absent diagonals. Mask 0 is a byte-identical source copy. No interpolation, rescaling or new colors.",
        "maskBits": {"left": LEFT, "right": RIGHT, "down": DOWN, "up": UP, "downLeft": DOWN_LEFT,
                     "downRight": DOWN_RIGHT, "upLeft": UP_LEFT, "upRight": UP_RIGHT},
        "tiles": records,
    }
    (REFERENCE / "generation.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    save_previews(source, tiles)
    print(json.dumps({"tiles": len(tiles), "sourceSha256": report["sourceSha256"],
                      "preview": str(REFERENCE / "preview.png")}, indent=2))


if __name__ == "__main__":
    main()
