from __future__ import annotations

import struct
from pathlib import Path


MAGIC = b"MBS1"
PYTHON_DIR = Path(__file__).resolve().parent
RESOURCE_MULTIBLOCK_ROOT = PYTHON_DIR.parent


def read_mbs(path: Path) -> list[list[str]]:
    """读取 .mbs 文件并还原为二维结构数组。"""
    with path.open("rb") as handle:
        if handle.read(4) != MAGIC:
            raise ValueError(f"{path} 不是有效的 .mbs 文件")
        string_count = struct.unpack(">i", handle.read(4))[0]
        string_table: list[str] = []
        for _ in range(string_count):
            byte_length = struct.unpack(">i", handle.read(4))[0]
            string_table.append(handle.read(byte_length).decode("utf-8"))
        row_count = struct.unpack(">i", handle.read(4))[0]
        structure: list[list[str]] = []
        for _ in range(row_count):
            column_count = struct.unpack(">i", handle.read(4))[0]
            row: list[str] = []
            for _ in range(column_count):
                string_index = struct.unpack(">i", handle.read(4))[0]
                row.append(string_table[string_index])
            structure.append(row)
        return structure


def decompile_file(path: Path) -> Path:
    structure = read_mbs(path)
    output_path = path.with_suffix(".txt")
    output_path.write_text("\n".join(",".join(row) for row in structure) + "\n", encoding="utf-8")
    return output_path


def main() -> None:
    print("该工具会把 .mbs 二进制文件反编译为当前 .mb 样式的文本。")
    target = input("请输入相对 multiblock 资源目录的 .mbs 路径，或输入 * 处理全部：").strip()
    if not target:
        raise ValueError("必须输入要处理的目标路径")

    if target == "*":
        for mbs_file in sorted(RESOURCE_MULTIBLOCK_ROOT.rglob("*.mbs")):
            output = decompile_file(mbs_file)
            print(f"已反编译：{mbs_file.relative_to(RESOURCE_MULTIBLOCK_ROOT)} -> {output.relative_to(RESOURCE_MULTIBLOCK_ROOT)}")
        return

    source = (RESOURCE_MULTIBLOCK_ROOT / target).resolve()
    output = decompile_file(source)
    print(f"已反编译：{source.name} -> {output.name}")


if __name__ == "__main__":
    main()
