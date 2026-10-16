"""Derive native-pixel CTM frames without stretching imported feature artwork."""

import hashlib
import json
from pathlib import Path
import shutil

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "src/main/resources/assets/gtnotgood/textures/blocks/stellar"
REFERENCE = ROOT / "build/stellar-ctm-reference"
LEFT, RIGHT, DOWN, UP = 1, 2, 4, 8
DOWN_LEFT, DOWN_RIGHT, UP_LEFT, UP_RIGHT = 16, 32, 64, 128
CORNERS = ((LEFT, DOWN, DOWN_LEFT, 0, 1), (RIGHT, DOWN, DOWN_RIGHT, 1, 1),
           (LEFT, UP, UP_LEFT, 0, 0), (RIGHT, UP, UP_RIGHT, 1, 0))
BANDS = {
    "sl_bastion", "sl_casimir", "sl_coil", "sl_deckband", "sl_disccoil_t1", "sl_disccoil_t2",
    "sl_disccoil_t3", "sl_disccoil_t4", "sl_dmcoil", "sl_helix", "sl_mirrorback", "sl_vessel",
}
RAILS = {
    "reactorFuelColumnSide", "reactorFuelColumnSideFueled", "rodLoadingPortSide",
    "turbineHousingCW", "turbineHousingCCW",
}
GLASS = {"sl_glass_darkmatter", "sl_glass_magnetar"}
ANCHORS = {
    "sl_dmframe": (3, 2), "sl_injector_mk1": (2, 3), "sl_injector_mk2": (2, 3),
    "sl_injector_mk3": (2, 3), "sl_mirror": (7, 7), "sl_module_extractor": (3, 2),
    "sl_region": (3, 2),
    "sl_hull": (3, 3), "sl_latframe": (3, 5), "sl_lining": (3, 3),
    "sl_module_compressor": (3, 5), "sl_phase": (3, 3), "sl_stir": (3, 3),
}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def normalize_mask(mask):
    result = mask & 15
    for horizontal, vertical, diagonal, _, _ in CORNERS:
        if mask & horizontal and mask & vertical and mask & diagonal:
            result |= diagonal
    return result


def profile(name):
    if name in GLASS:
        return {"mode": "glass", "frameWidth": 1, "backgroundSample": [2, 2]}
    if name in RAILS:
        return {"mode": "rail", "frameWidth": 2, "sampleInset": 3}
    if name == "sl_petal":
        return {"mode": "fins", "frameWidth": 2}
    if name in BANDS:
        return {"mode": "band", "frameWidth": 3 if name == "sl_mirrorback" else 2}
    if name in ("reactorFuelColumnTop", "rodLoadingPortTop"):
        return {"mode": "core", "frameWidth": 1, "backgroundSample": [1, 1]}
    return {"mode": "core", "frameWidth": 1 if name == "sl_region" else 2,
            "backgroundSample": list(ANCHORS.get(name, (2, 2)))}


def derive_tile(source, mask, style):
    tile = source.copy()
    width = style["frameWidth"]
    high = 15 - width
    inset = style.get("sampleInset", width)
    background = source.getpixel(tuple(style.get("backgroundSample", (2, 2))))
    for y in range(16):
        for x in range(16):
            connected_x = (x < width and mask & LEFT) or (x > high and mask & RIGHT)
            connected_y = (y < width and mask & UP) or (y > high and mask & DOWN)
            if connected_x or connected_y:
                if style["mode"] in ("rail", "fins"):
                    value = source.getpixel((max(inset, min(15 - inset, x)) if connected_x else x,
                                             max(inset, min(15 - inset, y)) if connected_y else y))
                elif style["mode"] == "band":
                    value = source.getpixel((max(width, min(high, x)), max(width, min(high, y))))
                else:
                    value = background
                tile.putpixel((x, y), value)

    for horizontal, vertical, diagonal, right, bottom in CORNERS:
        cx, cy = (16 - width if right else 0), (16 - width if bottom else 0)
        horizontal_connected = bool(mask & horizontal)
        vertical_connected = bool(mask & vertical)
        for y in range(cy, cy + width):
            for x in range(cx, cx + width):
                if style["mode"] in ("rail", "fins"):
                    if horizontal_connected and vertical_connected and not mask & diagonal:
                        tile.putpixel((x, y), source.getpixel((x, y)))
                elif horizontal_connected and not vertical_connected:
                    tile.putpixel((x, y), source.getpixel((7, y)))
                elif vertical_connected and not horizontal_connected:
                    tile.putpixel((x, y), source.getpixel((x, 8)))
                elif horizontal_connected and vertical_connected and not mask & diagonal:
                    tile.putpixel((x, y), source.getpixel((x, y)))
    return tile


def neighbors_mask(cells, x, y):
    offsets = ((-1, 0, LEFT), (1, 0, RIGHT), (0, 1, DOWN), (0, -1, UP),
               (-1, 1, DOWN_LEFT), (1, 1, DOWN_RIGHT), (-1, -1, UP_LEFT), (1, -1, UP_RIGHT))
    return normalize_mask(sum(bit for dx, dy, bit in offsets if (x + dx, y + dy) in cells))


def wall(source, tiles, connected, cells):
    result = Image.new("RGBA", (max(x for x, _ in cells) * 16 + 16, max(y for _, y in cells) * 16 + 16))
    for x, y in sorted(cells):
        tile = tiles[neighbors_mask(cells, x, y)] if connected else source
        result.paste(tile, (16 * x, 16 * y))
    return result


def checkerboard(width, height):
    canvas = Image.new("RGB", (width, height), (58, 60, 64))
    draw = ImageDraw.Draw(canvas)
    for y in range(0, height, 12):
        for x in range(0, width, 12):
            if ((x + y) // 12) % 2:
                draw.rectangle((x, y, x + 11, y + 11), fill=(83, 85, 89))
    return canvas


def preview_page(entries, page):
    canvas = Image.new("RGB", (1096, 56 + len(entries) * 210), (22, 25, 30))
    draw = ImageDraw.Draw(canvas)
    draw.text((12, 10), "Native 16 px CTM | original / joined wall / concave opening | nearest-neighbor x3", fill="white")
    rectangle = {(x, y) for y in range(3) for x in range(5)}
    opening = rectangle - {(2, 1)}
    for i, (name, source, tiles, style) in enumerate(entries):
        top = 50 + i * 210
        draw.text((12, top), name + " | " + style["mode"] + " | frame " + str(style["frameWidth"]), fill=(240, 189, 117))
        for j, (connected, cells) in enumerate(((False, rectangle), (True, rectangle), (True, opening))):
            texture = wall(source, tiles, connected, cells).resize((240, 144), Image.Resampling.NEAREST)
            background = checkerboard(240, 144)
            background.paste(texture, (0, 0), texture)
            canvas.paste(background, (12 + 364 * j, top + 24))
    canvas.save(REFERENCE / ("preview-" + str(page) + ".png"))


def main():
    REFERENCE.mkdir(parents=True, exist_ok=True)
    masks = sorted({normalize_mask(mask) for mask in range(256)})
    assert len(masks) == 47
    sources = sorted(path for path in SOURCES.glob("*.png") if path.stem != "sl_eddington")
    assert len(sources) == 55
    records, styles, previews = [], {}, []
    source_hashes = {path: digest(path) for path in sources}
    for path in sources:
        name = path.stem
        source = Image.open(path).convert("RGBA")
        assert source.size == (16, 16)
        style = profile(name)
        width = style["frameWidth"]
        destination = SOURCES / "ctm" / name
        destination.mkdir(parents=True, exist_ok=True)
        tiles = {}
        source_colors = set(source.get_flattened_data())
        for mask in masks:
            tile = derive_tile(source, mask, style)
            output = destination / (str(mask) + ".png")
            if mask == 0:
                shutil.copyfile(path, output)
            else:
                tile.save(output, optimize=False)
            assert all(tile.getpixel((x, y)) == source.getpixel((x, y))
                       for y in range(width, 16 - width) for x in range(width, 16 - width))
            assert set(tile.get_flattened_data()) <= source_colors
            if name in GLASS:
                assert all(tile.getpixel((x, y))[3] == source.getpixel((x, y))[3]
                           for y in range(1, 15) for x in range(1, 15))
                assert tile.getpixel((0, 7))[3] == (110 if mask & LEFT else 255)
                assert tile.getpixel((15, 7))[3] == (110 if mask & RIGHT else 255)
                assert tile.getpixel((7, 0))[3] == (110 if mask & UP else 255)
                assert tile.getpixel((7, 15))[3] == (110 if mask & DOWN else 255)
            else:
                assert tile.getextrema()[3] == (255, 255)
            records.append({"source": path.relative_to(ROOT).as_posix(), "sourceSha256": source_hashes[path],
                            "destination": output.relative_to(ROOT).as_posix(), "sha256": digest(output),
                            "mask": mask, "modified": mask != 0, "license": "MIT"})
            tiles[mask] = tile
        assert (destination / "0.png").read_bytes() == path.read_bytes()
        assert digest(path) == source_hashes[path]
        styles[name] = {**style, "uniqueTiles": len({image.tobytes() for image in tiles.values()})}
        previews.append((name, source, tiles, style))
    for index in range(0, len(previews), 8):
        preview_page(previews[index:index + 8], index // 8 + 1)
    selected = {"sl_node_l1", "sl_coil", "sl_glass_darkmatter", "sl_glass_magnetar", "sl_mirror",
                "sl_petal", "reactorFuelColumnSide", "sl_galactic"}
    preview_page([entry for entry in previews if entry[0] in selected], "representative")
    report = {
        "generator": Path(__file__).relative_to(ROOT).as_posix(), "generatorSha256": digest(Path(__file__)),
        "sourceTextureCount": len(sources), "tileCount": len(records), "dimensions": [16, 16],
        "license": "MIT; derived from the existing Beyond the Stars block imports",
        "algorithm": "Preserve the central artwork pixel-for-pixel and only replace each connected outer frame. "
                     "Core textures use a safe background pixel, band and rail textures extend corresponding source "
                     "rows/columns, and 1 px glass frames use the existing alpha-110 glass color. Rail samples "
                     "skip edge rivets; unframed rails and directional fins preserve their original exposed ends. "
                     "Other exposed frames continue through perpendicular joins, and missing diagonals restore "
                     "native concave corner caps. "
                     "Mask 0 is a byte-identical original. No rotation, interpolation, rescaling or new colors.",
        "maskBits": {"left": LEFT, "right": RIGHT, "down": DOWN, "up": UP, "downLeft": DOWN_LEFT,
                     "downRight": DOWN_RIGHT, "upLeft": UP_LEFT, "upRight": UP_RIGHT},
        "styles": styles, "tiles": records,
    }
    (REFERENCE / "generation.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: report[key] for key in ("sourceTextureCount", "tileCount", "generatorSha256")}, indent=2))


if __name__ == "__main__":
    main()
