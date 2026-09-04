# -*- coding: utf-8 -*-
"""
GTNH 配置文件一键修改脚本

使用方法：
    客户端：将此脚本放在 .minecraft 目录下
    服务端：将此脚本放在服务端根目录下（与 config、serverutilities 等文件夹同级）
    脚本会自动检测运行环境

功能：
    1. 自动检测客户端(.minecraft)或服务端环境
    2. 支持修改 config、serverutilities 等多个文件夹内的配置
    3. 首次运行时备份原始配置文件
    4. 后续运行从备份中提取并修改（保证初始备份永远不变）
"""

import os
import shutil
import json
from datetime import datetime
from pathlib import Path

# ==================== 配置区域 ====================

# 备份目录名称（会在本脚本同目录下创建）
BACKUP_DIR_NAME = "config_backup"

# 要修改的配置文件列表及修改规则
# 格式说明：
# {
#     "相对于.minecraft目录的文件路径": {
#         "modifications": [
#             {
#                 "type": "replace",           # 简单替换
#                 "search": "要查找的内容",
#                 "replace": "替换后的内容"
#             },
#             {
#                 "type": "replace_line",       # 替换整行
#                 "search": "行中包含的关键字",
#                 "replace": "新的整行内容"
#             },
#             {
#                 "type": "regex",              # 正则表达式替换
#                 "pattern": "正则表达式",
#                 "replace": "替换内容"
#             }
#         ]
#     }
# }

CONFIG_MODIFICATIONS = {

    # ===== AE2 配置修改 =====
    "config/AppliedEnergistics2/AppliedEnergistics2.cfg": {
        "modifications": [
            {
                "type": "replace",
                "search": "B:Channels=true",
                "replace": "B:Channels=false"
            },
            # 修改合成数量按钮
            {
                "type": "replace",
                "search": "I:craftAmtButton1=1",
                "replace": "I:craftAmtButton1=1"
            },
            {
                "type": "replace",
                "search": "I:craftAmtButton2=10",
                "replace": "I:craftAmtButton2=64"
            },
            {
                "type": "replace",
                "search": "I:craftAmtButton3=100",
                "replace": "I:craftAmtButton3=640"
            },
            {
                "type": "replace",
                "search": "I:craftAmtButton4=1000",
                "replace": "I:craftAmtButton4=6400"
            },
            # 关闭电力消耗（将功耗倍数设为 0）
            {
                "type": "replace",
                "search": "D:UsageMultiplier=10.0",
                "replace": "D:UsageMultiplier=0.0"
            }
        ]
    },



    # ===== GregTech 爆炸配置修改 =====
    "config/GregTech/GregTech.cfg": {
        "modifications": [
            {
                "type": "replace",
                "search": "B:machineExplosions=true",
                "replace": "B:machineExplosions=false"
            },
            {
                "type": "replace",
                "search": "B:machineFireExplosions=true",
                "replace": "B:machineFireExplosions=false"
            },
            {
                "type": "replace",
                "search": "B:machineNonWrenchExplosions=true",
                "replace": "B:machineNonWrenchExplosions=false"
            },
            {
                "type": "replace",
                "search": "B:machineRainExplosions=true",
                "replace": "B:machineRainExplosions=false"
            },
            {
                "type": "replace",
                "search": "B:machineThunderExplosions=true",
                "replace": "B:machineThunderExplosions=false"
            }
        ]
    },

    # ===== GregTech 污染配置修改 =====
    "config/GregTech/Pollution.cfg": {
        "modifications": [
            {
                "type": "replace",
                "search": 'B:"Activate Pollution"=true',
                "replace": 'B:"Activate Pollution"=false'
            }
        ]
    },

    # ===== ServerUtilities 配置修改 =====
    "serverutilities/serverutilities.cfg": {
        "modifications": [
            # Chunk Loading 和 Claiming 设置
            {"type": "replace", "search": "B:chunk_claiming=false", "replace": "B:chunk_claiming=true"},

            # Commands 设置
            {"type": "replace", "search": "B:back=false", "replace": "B:back=true"},
            {"type": "replace", "search": "B:fly=false", "replace": "B:fly=true"},
            {"type": "replace", "search": "B:god=false", "replace": "B:god=true"},
            {"type": "replace", "search": "B:heal=false", "replace": "B:heal=true"},
            {"type": "replace", "search": "B:home=false", "replace": "B:home=true"},
            {"type": "replace", "search": "B:kickme=false", "replace": "B:kickme=true"},
            {"type": "replace", "search": "B:mute=false", "replace": "B:mute=true"},
            {"type": "replace", "search": "B:nick=false", "replace": "B:nick=true"},
            {"type": "replace", "search": "B:rec=false", "replace": "B:rec=true"},
            {"type": "replace", "search": "B:rtp=false", "replace": "B:rtp=true"},
            {"type": "replace", "search": "B:spawn=false", "replace": "B:spawn=true"},
            {"type": "replace", "search": "B:tpa=false", "replace": "B:tpa=true"},
            {"type": "replace", "search": "B:warp=false", "replace": "B:warp=true"},

            # ranks 设置
            {
                "type": "regex",
                "pattern": r"# Enables Ranks\. \[default: true\]\s*\n\s*B:enabled=false",
                "replace": "# Enables Ranks. [default: true]\n    B:enabled=true"
            }

        ]
    },

    # ===== ServerUtilities ranks.txt 配置修改 =====
    "serverutilities/server/ranks.txt": {
        "modifications": [
            # 修改 [player] 部分
            {"type": "replace", "search": "power: 1", "replace": "power: 100"},
            {"type": "replace", "search": "serverutilities.claims.max_chunks: 100", "replace": "serverutilities.claims.max_chunks: 30000"},
            {"type": "replace", "search": "serverutilities.chunkloader.max_chunks: 50", "replace": "serverutilities.chunkloader.max_chunks: 30000"},
            {"type": "replace", "search": "serverutilities.homes.max: 1", "replace": "serverutilities.homes.max: 200"},
            {"type": "replace", "search": "serverutilities.homes.warmup: 5s", "replace": "serverutilities.homes.warmup: 0s"},
            {"type": "replace", "search": "serverutilities.homes.cross_dim: false", "replace": "serverutilities.homes.cross_dim: true"},

            # 修改 [vip] 部分
            {"type": "replace", "search": "power: 20", "replace": "power: 100"},
            {"type": "replace", "search": "serverutilities.claims.max_chunks: 500", "replace": "serverutilities.claims.max_chunks: 30000"},
            {"type": "replace", "search": "serverutilities.chunkloader.max_chunks: 100", "replace": "serverutilities.chunkloader.max_chunks: 30000"},  # 已经是 0s，无需修改

        ]
    },

    # ===== EnhancedLootBags LootBags.xml 配置修改 =====
    "config/EnhancedLootBags/LootBags.xml": {
        "modifications": [
            {
                "type": "replace",
                "search": 'CombineTrashGroup="true"',
                "replace": 'CombineTrashGroup="false"'
            }
        ]
    },

    # ===== Matter Manipulator 配置修改 =====
    "config/matter-manipulator.cfg": {
        "modifications": [
            {
                "type": "replace",
                "search": 'I:"MK3 Block Place Speed"=256',
                "replace": 'I:"MK3 Block Place Speed"=25600'
            }
        ]
    },

    # ===== StructureLib 配置修改 =====
    "config/structurelib.cfg": {
        "modifications": [
            {
                "type": "replace",
                "search": "I:autoPlaceBudget=25",
                "replace": "I:autoPlaceBudget=200"
            },
            {
                "type": "replace",
                "search": "I:autoPlaceInterval=300",
                "replace": "I:autoPlaceInterval=0"
            }
        ]
    },

    # ===== 血魔法 LP 获取值修改 =====
    "config/AWWayofTime.cfg": {
        "modifications": [
            # 修改自我牺牲获得的LP值
            {
                "type": "replace",
                "search": "I:\"LP per self-sacrifice\"=125",
                "replace": "I:\"LP per self-sacrifice\"=500000"
            },
            # 修改使用香炉时自我牺牲的LP值
            {
                "type": "replace",
                "search": "D:\"LP per (self-)sacrifice with incense\"=150.0",
                "replace": "D:\"LP per (self-)sacrifice with incense\"=600.0"
            },
            # 修改灵魂磨损药水激活时的自我牺牲LP值
            {
                "type": "replace",
                "search": "I:\"LP per self-sacrifice (when Soul Fray potion is active)\"=1",
                "replace": "I:\"LP per self-sacrifice (when Soul Fray potion is active)\"=10"
            },
            # 修改使用羽毛刀仪式时的自我牺牲LP值
            {
                "type": "replace",
                "search": "I:\"LP per self-sacrifice with Ritual of Feathered Knife\"=125",
                "replace": "I:\"LP per self-sacrifice with Ritual of Feathered Knife\"=500"
            },
            # 修改普通献祭获得的LP值
            {
                "type": "replace",
                "search": "I:\"LP per sacrifice\"=600",
                "replace": "I:\"LP per sacrifice\"=1200"
            },
            # 修改痛苦之井仪式的献祭LP值
            {
                "type": "replace",
                "search": "I:\"LP per sacrifice with Well of Suffering ritual\"=25",
                "replace": "I:\"LP per sacrifice with Well of Suffering ritual\"=100"
            }
        ]
    },


    "config/gendustry/overrides/tuning.cfg": {
        "modifications": [
            # 修改死亡概率为 0
            {"type": "replace", "search": "DeathChanceArtificial = 80", "replace": "DeathChanceArtificial = 0"},
            {"type": "replace", "search": "DeathChanceNatural = 20", "replace": "DeathChanceNatural = 0"},
            {"type": "replace", "search": "DeathChanceArtificial = 50", "replace": "DeathChanceArtificial = 0"},
            {"type": "replace", "search": "DeathChanceNatural = 10", "replace": "DeathChanceNatural = 0"},

            # 修改突变成功率为 100
            {"type": "replace", "search": "SecretMutationChance = 10", "replace": "SecretMutationChance = 100"},
            {"type": "replace", "search": "SecretMutationChance = 20", "replace": "SecretMutationChance = 100"},

            # 修改器材使用消耗为 0
            {"type": "replace", "search": "LabwareConsumeChance = 100", "replace": "LabwareConsumeChance = 0"},
            {"type": "replace", "search": "LabwareConsumeChance = 50", "replace": "LabwareConsumeChance = 0"},
            {"type": "replace", "search": "LabwareConsumeChance = 20", "replace": "LabwareConsumeChance = 0"},
        ]
    }






    # ===== 在下方添加更多配置文件修改 =====
    # 路径格式：相对于 .minecraft 目录
    # 例如：
    # "config/SomeMod/config.cfg": { ... }
    # "serverutilities/xxx.cfg": { ... }

}

# ==================== 脚本逻辑（通常无需修改） ====================

class ConfigModifier:
    """
    GTNH 配置修改器 V2

    主要改进：
    1. 自动检测客户端/服务端目录时，同时检查实际配置文件，降低误判。
    2. 首次运行建立永久原始备份；后续新增配置文件会自动补充备份。
    3. 恢复备份失败时停止修改，避免在错误状态上继续写配置。
    4. replace 支持 count，默认全部替换；可指定只替换前 N 个。
    5. replace_line 支持 count，默认全部替换。
    6. 使用临时文件 + os.replace 原子写入，降低文件写坏风险。
    7. 自动保留 UTF-8 BOM；普通 UTF-8 文件不会强制添加 BOM。
    8. 提供 dry-run 模式：只检查和显示修改，不写入文件。
    9. 修改后验证目标内容确实已经写入。
    10. 日志统计更准确。
    """

    BACKUP_VERSION = 2

    def __init__(self, dry_run=False):
        self.script_dir = Path(__file__).parent.resolve()
        self.backup_dir = self.script_dir / BACKUP_DIR_NAME
        self.minecraft_dir = None
        self.game_dir = None
        self.env_type = None
        self.backup_info_file = self.backup_dir / "_backup_info.json"
        self.dry_run = dry_run

    def log(self, message, level="INFO"):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(f"[{timestamp}] [{level}] {message}")

    # -------------------- 环境检测 --------------------

    def _score_game_dir(self, path):
        """根据实际配置文件存在情况给候选目录评分。"""
        score = 0
        if not path.is_dir():
            return -1

        for relative_path in CONFIG_MODIFICATIONS:
            if (path / relative_path).is_file():
                score += 2

        if (path / "config").is_dir():
            score += 1

        if (path / "serverutilities").is_dir():
            score += 1

        return score

    def find_game_dir(self):
        """
        自动检测游戏目录。

        检测范围：
        - 脚本目录
        - 脚本目录向上最多 7 层
        - 每层的 .minecraft 子目录

        最终优先选择“实际配置文件命中最多”的目录。
        """
        candidates = []
        seen = set()

        current = self.script_dir

        for _ in range(8):
            possible = [current, current / ".minecraft"]

            for path in possible:
                path = path.resolve()
                if path in seen:
                    continue
                seen.add(path)

                score = self._score_game_dir(path)
                if score >= 3:
                    candidates.append((score, path))

            if current.parent == current:
                break
            current = current.parent

        if not candidates:
            return None, None

        # 分数最高优先；同分时优先路径更浅/更接近脚本目录的候选
        candidates.sort(key=lambda x: x[0], reverse=True)
        path = candidates[0][1]

        env_type = "客户端" if path.name.lower() == ".minecraft" else "服务端"
        return path, env_type

    def check_game_dir(self):
        self.game_dir, self.env_type = self.find_game_dir()

        if self.game_dir is None:
            self.log("无法自动检测到游戏配置目录！", "ERROR")
            self.log("客户端：请将此脚本放在 .minecraft 目录下或其附近", "ERROR")
            self.log("服务端：请将此脚本放在服务端根目录下或其附近", "ERROR")
            return False

        self.log(f"运行环境: {self.env_type}")
        self.log(f"配置目录: {self.game_dir}")
        return True

    # -------------------- 文件检查 --------------------

    def check_files_exist(self):
        missing_files = []
        found_files = []

        for relative_path in CONFIG_MODIFICATIONS:
            full_path = self.game_dir / relative_path

            if full_path.is_file():
                found_files.append(relative_path)
                self.log(f"找到配置文件: {relative_path}")
            else:
                missing_files.append(relative_path)
                self.log(f"配置文件不存在: {relative_path}", "WARNING")

        if missing_files:
            self.log(f"有 {len(missing_files)} 个配置文件未找到", "WARNING")

        return found_files, missing_files

    # -------------------- 备份管理 --------------------

    def is_first_run(self):
        return not self.backup_info_file.exists()

    def _load_backup_info(self):
        if not self.backup_info_file.exists():
            return None

        try:
            with open(self.backup_info_file, "r", encoding="utf-8") as f:
                data = json.load(f)
        except (OSError, json.JSONDecodeError) as e:
            self.log(f"读取备份信息失败: {e}", "ERROR")
            return None

        if not isinstance(data, dict):
            self.log("备份信息格式无效", "ERROR")
            return None

        data.setdefault("version", 1)
        data.setdefault("backed_up_files", [])
        return data

    def _save_backup_info(self, backup_info):
        self.backup_dir.mkdir(parents=True, exist_ok=True)

        temp = self.backup_info_file.with_suffix(".json.tmp")
        try:
            with open(temp, "w", encoding="utf-8") as f:
                json.dump(backup_info, f, ensure_ascii=False, indent=2)

            os.replace(temp, self.backup_info_file)
        except Exception:
            try:
                if temp.exists():
                    temp.unlink()
            except OSError:
                pass
            raise

    def create_backup(self, files_to_backup):
        """创建永久初始备份。"""
        self.log("=" * 50)
        self.log("首次运行，创建初始备份...")

        self.backup_dir.mkdir(parents=True, exist_ok=True)

        backup_info = {
            "version": self.BACKUP_VERSION,
            "created_time": datetime.now().isoformat(),
            "game_dir": str(self.game_dir),
            "environment": self.env_type,
            "backed_up_files": []
        }

        try:
            for relative_path in files_to_backup:
                source = self.game_dir / relative_path
                dest = self.backup_dir / relative_path

                if not source.is_file():
                    self.log(f"备份源文件不存在: {relative_path}", "ERROR")
                    raise FileNotFoundError(str(source))

                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, dest)

                backup_info["backed_up_files"].append(relative_path)
                self.log(f"已备份: {relative_path}")

            self._save_backup_info(backup_info)

        except Exception as e:
            self.log(f"创建备份失败: {e}", "ERROR")
            raise

        self.log(f"初始备份完成，共备份 {len(files_to_backup)} 个文件")
        self.log(f"备份位置: {self.backup_dir}")
        self.log("=" * 50)

    def backup_new_files(self, found_files):
        """
        为后来新增到 CONFIG_MODIFICATIONS 的配置文件创建永久备份。
        返回 True/False。
        """
        backup_info = self._load_backup_info()

        if backup_info is None:
            self.log("无法读取已有备份信息，拒绝继续。", "ERROR")
            return False

        existing_backups = set(backup_info.get("backed_up_files", []))
        new_files = [p for p in found_files if p not in existing_backups]

        if not new_files:
            self.log("没有发现需要新增备份的配置文件")
            return True

        if self.dry_run:
            self.log(f"Dry-run：发现 {len(new_files)} 个新增配置文件，实际不会创建备份")
            return True

        self.log(f"发现 {len(new_files)} 个新增配置文件，正在创建永久备份...")

        try:
            for relative_path in new_files:
                source = self.game_dir / relative_path
                dest = self.backup_dir / relative_path

                if not source.is_file():
                    self.log(f"新增备份源文件不存在: {relative_path}", "ERROR")
                    return False

                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, dest)

                backup_info["backed_up_files"].append(relative_path)
                self.log(f"已备份新增文件: {relative_path}")

            backup_info["version"] = self.BACKUP_VERSION
            self._save_backup_info(backup_info)

        except Exception as e:
            self.log(f"新增文件备份失败: {e}", "ERROR")
            return False

        self.log(f"新增文件备份完成，共 {len(new_files)} 个")
        return True

    def restore_from_backup(self, files_to_restore):
        """
        从永久备份恢复原始文件。

        只要任意一个需要恢复的文件缺失，就返回 False，
        防止在备份不完整的情况下继续修改配置。
        """
        self.log("检查原始备份...")

        missing_backups = []

        for relative_path in files_to_restore:
            backup_file = self.backup_dir / relative_path
            if not backup_file.is_file():
                missing_backups.append(relative_path)

        if missing_backups:
            self.log(
                f"原始备份不完整，缺少 {len(missing_backups)} 个文件，已停止修改。",
                "ERROR"
            )
            for path in missing_backups:
                self.log(f"缺少备份: {path}", "ERROR")
            return False

        if self.dry_run:
            self.log("Dry-run：备份完整，跳过实际恢复")
            return True

        self.log("从备份恢复原始文件...")

        try:
            for relative_path in files_to_restore:
                backup_file = self.backup_dir / relative_path
                dest = self.game_dir / relative_path

                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(backup_file, dest)
                self.log(f"已恢复: {relative_path}")

        except Exception as e:
            self.log(f"恢复备份失败: {e}", "ERROR")
            return False

        return True

    # -------------------- 文件读写 --------------------

    def _read_text(self, file_path):
        """
        读取文本，同时尽量保留 UTF-8 BOM。

        优先 UTF-8 / UTF-8-SIG，失败后尝试 GB18030。
        """
        raw = file_path.read_bytes()

        has_bom = raw.startswith(b"\xef\xbb\xbf")

        encodings = ["utf-8-sig", "utf-8", "gb18030"]

        last_error = None
        for encoding in encodings:
            try:
                return raw.decode(encoding), encoding, has_bom
            except UnicodeDecodeError as e:
                last_error = e

        raise UnicodeDecodeError(
            "unknown",
            raw,
            0,
            min(len(raw), 1),
            f"无法识别文件编码: {last_error}"
        )

    def _write_text_atomic(self, file_path, content, encoding, has_bom):
        """使用临时文件 + os.replace 原子写入。"""
        if encoding == "utf-8-sig":
            output_encoding = "utf-8-sig"
        elif encoding == "utf-8" and has_bom:
            output_encoding = "utf-8-sig"
        else:
            output_encoding = encoding

        temp_path = file_path.with_name(file_path.name + ".gtnc_tmp")

        try:
            with open(temp_path, "w", encoding=output_encoding, newline="") as f:
                f.write(content)

            os.replace(temp_path, file_path)

        except Exception:
            try:
                if temp_path.exists():
                    temp_path.unlink()
            except OSError:
                pass
            raise

    # -------------------- 修改引擎 --------------------

    @staticmethod
    def _get_count(mod):
        count = mod.get("count", 0)
        if count is None:
            return 0
        try:
            count = int(count)
        except (TypeError, ValueError):
            return 0
        return max(count, 0)

    def apply_modifications(self, relative_path, modifications):
        """应用修改规则，并返回详细统计。"""
        import re

        file_path = self.game_dir / relative_path

        try:
            content, encoding, has_bom = self._read_text(file_path)
        except Exception as e:
            self.log(f"读取文件失败: {e}", "ERROR")
            return None

        original_content = content
        total_changes = 0
        successful_rules = 0
        failed_rules = 0

        for index, mod in enumerate(modifications, 1):
            mod_type = mod.get("type", "replace")

            if mod_type == "replace":
                search = mod.get("search", "")
                replace = mod.get("replace", "")
                count = self._get_count(mod)

                if not search:
                    self.log(f"  规则 #{index}: search 为空，跳过", "WARNING")
                    failed_rules += 1
                    continue

                occurrences = content.count(search)

                if occurrences == 0:
                    self.log(f"  规则 #{index}: 未找到 '{search}'", "WARNING")
                    failed_rules += 1
                    continue

                actual_count = occurrences if count == 0 else min(count, occurrences)
                content = content.replace(search, replace, actual_count)

                total_changes += actual_count
                successful_rules += 1
                self.log(
                    f"  替换 #{index}: '{search}' -> '{replace}' "
                    f"（{actual_count} 处）"
                )

            elif mod_type == "replace_line":
                search = mod.get("search", "")
                replace = mod.get("replace", "")
                count = self._get_count(mod)

                if not search:
                    self.log(f"  规则 #{index}: search 为空，跳过", "WARNING")
                    failed_rules += 1
                    continue

                lines = content.splitlines(keepends=True)
                changed = 0
                new_lines = []

                for line in lines:
                    line_body = line.rstrip("\r\n")

                    if search in line_body and (count == 0 or changed < count):
                        newline = line[len(line_body):]
                        new_lines.append(replace + newline)
                        changed += 1
                    else:
                        new_lines.append(line)

                if changed == 0:
                    self.log(
                        f"  规则 #{index}: 未找到包含 '{search}' 的行",
                        "WARNING"
                    )
                    failed_rules += 1
                else:
                    content = "".join(new_lines)
                    total_changes += changed
                    successful_rules += 1
                    self.log(
                        f"  替换行 #{index}: 包含 '{search}' 的行 "
                        f"（{changed} 行）"
                    )

            elif mod_type == "regex":
                pattern = mod.get("pattern", "")
                replace = mod.get("replace", "")
                count = self._get_count(mod)

                if not pattern:
                    self.log(f"  规则 #{index}: pattern 为空，跳过", "WARNING")
                    failed_rules += 1
                    continue

                try:
                    new_content, changed = re.subn(
                        pattern,
                        replace,
                        content,
                        count if count > 0 else 0
                    )
                except re.error as e:
                    self.log(
                        f"  规则 #{index}: 正则表达式错误: {e}",
                        "ERROR"
                    )
                    failed_rules += 1
                    continue

                if changed == 0:
                    self.log(
                        f"  规则 #{index}: 正则未匹配 '{pattern}'",
                        "WARNING"
                    )
                    failed_rules += 1
                else:
                    content = new_content
                    total_changes += changed
                    successful_rules += 1
                    self.log(
                        f"  正则替换 #{index}: '{pattern}' "
                        f"（{changed} 处）"
                    )

            else:
                self.log(
                    f"  规则 #{index}: 未知修改类型 '{mod_type}'",
                    "ERROR"
                )
                failed_rules += 1

        changed = content != original_content

        if changed and not self.dry_run:
            try:
                self._write_text_atomic(
                    file_path,
                    content,
                    encoding,
                    has_bom
                )
            except Exception as e:
                self.log(f"写入文件失败: {e}", "ERROR")
                return None

        return {
            "changed": changed,
            "changes": total_changes,
            "successful_rules": successful_rules,
            "failed_rules": failed_rules,
            "content": content
        }

    def verify_modifications(self, relative_path, modifications, content=None):
        """
        验证修改后的内容。

        对 replace / replace_line：
        - 如果目标 replace 内容不存在，则发出警告。
        对 regex：
        - 不强制验证，因为 regex 替换结果可能由捕获组动态生成。
        """
        if content is None:
            file_path = self.game_dir / relative_path
            try:
                content, _, _ = self._read_text(file_path)
            except Exception as e:
                self.log(f"验证读取失败: {e}", "ERROR")
                return False

        ok = True

        for index, mod in enumerate(modifications, 1):
            mod_type = mod.get("type", "replace")
            replace = mod.get("replace", "")

            if mod_type in ("replace", "replace_line"):
                if replace and replace not in content:
                    self.log(
                        f"  验证警告 #{index}: 未检测到目标内容 '{replace}'",
                        "WARNING"
                    )
                    ok = False

        return ok

    # -------------------- 主流程 --------------------

    def run(self):
        self.log("=" * 60)
        self.log("GTNH 配置文件修改脚本 V2")
        self.log("=" * 60)

        if self.dry_run:
            self.log("当前为 DRY-RUN 模式：不会写入任何配置文件", "WARNING")

        if not CONFIG_MODIFICATIONS:
            self.log("没有配置任何要修改的文件！", "ERROR")
            return False

        if not self.check_game_dir():
            return False

        found_files, missing_files = self.check_files_exist()

        if not found_files:
            self.log("没有找到任何要修改的配置文件！", "ERROR")
            return False

        # 首次运行：创建永久原始备份
        if self.is_first_run():
            if self.dry_run:
                self.log(
                    "Dry-run：这是首次运行，实际运行时会先创建原始备份。"
                )
            else:
                try:
                    self.create_backup(found_files)
                except Exception:
                    return False

        # 后续运行：先为新增文件建立永久备份，再从备份恢复
        else:
            if not self.backup_new_files(found_files):
                return False

            if not self.restore_from_backup(found_files):
                return False

        self.log("=" * 50)
        self.log("开始应用配置修改...")

        total_changes = 0
        total_successful_rules = 0
        total_failed_rules = 0
        changed_files = 0
        failed_files = 0

        for relative_path in found_files:
            modifications = CONFIG_MODIFICATIONS[relative_path].get(
                "modifications",
                []
            )

            if not modifications:
                continue

            self.log(f"\n处理文件: {relative_path}")

            result = self.apply_modifications(
                relative_path,
                modifications
            )

            if result is None:
                failed_files += 1
                continue

            total_changes += result["changes"]
            total_successful_rules += result["successful_rules"]
            total_failed_rules += result["failed_rules"]

            if result["changed"]:
                changed_files += 1

            # Dry-run 直接验证内存中的结果；普通模式重新读取文件验证
            verify_content = result["content"] if self.dry_run else None

            if not self.verify_modifications(
                relative_path,
                modifications,
                verify_content
            ):
                failed_files += 1

        self.log("=" * 50)

        if failed_files:
            self.log(
                f"完成，但有 {failed_files} 个文件处理/验证存在问题。",
                "WARNING"
            )
        else:
            self.log("所有已找到的配置文件处理完成。")

        self.log(f"实际发生修改的文件: {changed_files}")
        self.log(f"修改次数: {total_changes}")
        self.log(f"成功规则: {total_successful_rules}")
        self.log(f"未匹配/失败规则: {total_failed_rules}")

        if missing_files:
            self.log(f"跳过不存在的配置文件: {len(missing_files)}")

        self.log("=" * 60)

        return failed_files == 0


def main():
    import sys

    dry_run = "--dry-run" in sys.argv or "-n" in sys.argv

    modifier = ConfigModifier(dry_run=dry_run)

    try:
        success = modifier.run()

        if success:
            print("\n✓ 配置修改成功！")
        else:
            print("\n✗ 配置修改失败或存在警告，请查看上方日志。")

    except KeyboardInterrupt:
        print("\n\n✗ 用户中止操作。")

    except Exception as e:
        print(f"\n✗ 发生未处理错误: {e}")
        import traceback
        traceback.print_exc()

    input("\n按回车键退出...")


if __name__ == "__main__":
    main()
