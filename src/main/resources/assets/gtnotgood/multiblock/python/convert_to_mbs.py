from __future__ import annotations

import struct
from pathlib import Path


MAGIC = b"MBS1"
PYTHON_DIR = Path(__file__).resolve().parent
RESOURCE_MULTIBLOCK_ROOT = PYTHON_DIR.parent
SOURCE_MULTIBLOCK_ROOT = PYTHON_DIR.parent  # .mb .mbs 同目录
INPUT_FILE = PYTHON_DIR / "input.txt"


def parse_mb_text(text: str) -> list[list[str]]:
    """将现有 .mb 文本解析为二维结构数组。"""
    rows = []
    for line in text.splitlines():
        if line:
            rows.append(line.split(","))
    return rows


def write_mbs(path: Path, structure: list[list[str]]) -> None:
    """将结构写入 .mbs 二进制文件，使用字符串表减少重复内容。"""
    string_table: list[str] = []
    string_indexes: dict[str, int] = {}
    for row in structure:
        for cell in row:
            if cell not in string_indexes:
                string_indexes[cell] = len(string_table)
                string_table.append(cell)

    with path.open("wb") as handle:
        handle.write(MAGIC)
        handle.write(struct.pack(">i", len(string_table)))
        for cell in string_table:
            encoded = cell.encode("utf-8")
            handle.write(struct.pack(">i", len(encoded)))
            handle.write(encoded)
        handle.write(struct.pack(">i", len(structure)))
        for row in structure:
            handle.write(struct.pack(">i", len(row)))
            for cell in row:
                handle.write(struct.pack(">i", string_indexes[cell]))


def convert_existing_mb_file(path: Path) -> Path:
    structure = parse_mb_text(path.read_text(encoding="utf-8"))
    relative_path = path.relative_to(SOURCE_MULTIBLOCK_ROOT)
    output_path = RESOURCE_MULTIBLOCK_ROOT / relative_path.with_suffix(".mbs")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    write_mbs(output_path, structure)
    return output_path


def batch_convert_existing_mb_files() -> int:
    if not SOURCE_MULTIBLOCK_ROOT.is_dir():
        raise FileNotFoundError(f"未找到源文件目录：{SOURCE_MULTIBLOCK_ROOT}")
    converted = 0
    for mb_file in sorted(SOURCE_MULTIBLOCK_ROOT.rglob("*.mb")):
        convert_existing_mb_file(mb_file)
        converted += 1
    return converted


def find_special_letter(input_text: str) -> str | None:
    in_special_tiles = False
    for raw_line in input_text.splitlines():
        stripped = raw_line.strip()
        if stripped.startswith("Special Tiles:"):
            in_special_tiles = True
            continue
        if in_special_tiles and stripped.startswith("Offsets:"):
            break
        if in_special_tiles and "ofSpecialTileAdder" in stripped and "->" in stripped:
            return stripped.split("->", 1)[0].strip()[:1]
    return None


def replace_special_marker(row: str, special_letter: str | None) -> str:
    if special_letter is None:
        return row
    return "".join("~" if char in (special_letter, special_letter.lower()) else char for char in row)


def parse_input_structure(input_text: str) -> list[list[str]]:
    lines = input_text.splitlines()
    start_index = None
    end_index = None
    for index, line in enumerate(lines):
        if "new String[][]{{" in line:
            start_index = index + 1
            continue
        if start_index is not None and line.strip() == "}}":
            end_index = index
            break

    if start_index is None or end_index is None or end_index <= start_index:
        raise ValueError("input.txt does not contain a valid structure scan block")

    special_letter = find_special_letter(input_text)
    blocks: list[list[str]] = []
    current_block: list[str] = []
    for raw_line in lines[start_index:end_index]:
        stripped = raw_line.strip()
        if not stripped:
            continue
        if stripped == "},{":
            if current_block:
                blocks.append(current_block)
                current_block = []
            continue
        cleaned = stripped.rstrip(",")
        if cleaned.startswith('"') and cleaned.endswith('"'):
            current_block.append(replace_special_marker(cleaned[1:-1], special_letter))
    if current_block:
        blocks.append(current_block)

    if not blocks:
        raise ValueError("No structure rows were parsed from input.txt")

    row_count = len(blocks[0])
    if any(len(block) != row_count for block in blocks):
        raise ValueError("Inconsistent layer heights found in input.txt")

    structure: list[list[str]] = []
    for row_index in range(row_count):
        structure.append([block[row_index] for block in blocks])
    return structure


def convert_input_file(output_name: str) -> tuple[Path, Path]:
    if not output_name:
        raise ValueError("输出文件名不能为空")
    structure = parse_input_structure(INPUT_FILE.read_text(encoding="utf-8"))
    mb_path = SOURCE_MULTIBLOCK_ROOT / f"{output_name}.mb"
    mbs_path = RESOURCE_MULTIBLOCK_ROOT / f"{output_name}.mbs"
    mb_path.parent.mkdir(parents=True, exist_ok=True)
    mbs_path.parent.mkdir(parents=True, exist_ok=True)
    mb_path.write_text("\n".join(",".join(row) for row in structure) + "\n", encoding="utf-8")
    write_mbs(mbs_path, structure)
    return mb_path, mbs_path


def main() -> None:
    print("1. 将 input.txt 转成新的 .mb 源文件和 .mbs 二进制文件")
    print("2. 仅批量刷新已有 .mb 源文件对应的 .mbs 文件")
    choice = input("请选择模式 [1/2，默认 2]：").strip() or "2"

    if choice == "1":
        output_name = input("请输入输出文件名（不带扩展名）：").strip()
        mb_path, mbs_path = convert_input_file(output_name)
        print(f"已生成源文件：{mb_path}")
        print(f"已生成二进制文件：{mbs_path}")

    converted = batch_convert_existing_mb_files()
    print(f"已刷新 {converted} 个 .mbs 文件")


if __name__ == "__main__":
    main()
