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
    def __init__(self):
        self.script_dir = Path(__file__).parent.resolve()
        self.backup_dir = self.script_dir / BACKUP_DIR_NAME
        self.minecraft_dir = None  # .minecraft 目录
        self.backup_info_file = self.backup_dir / "_backup_info.json"

    def log(self, message, level="INFO"):
        """打印日志"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(f"[{timestamp}] [{level}] {message}")

    def find_game_dir(self):
        """
        自动检测游戏目录位置（客户端或服务端）

        客户端检测逻辑：
        - 查找包含 config 文件夹的 .minecraft 目录

        服务端检测逻辑：
        - 查找包含 config 文件夹的根目录
        """
        search_paths = []

        # 向上查找最多6层
        current = self.script_dir
        for _ in range(7):
            # 检查当前目录是否包含 config 文件夹
            if (current / "config").exists() and (current / "config").is_dir():
                # 检查是否是客户端（有 .minecraft 父目录）
                is_client = (current.name == ".minecraft")
                env_type = "客户端" if is_client else "服务端"
                search_paths.append((current, env_type))

            # 检查 .minecraft 子目录
            if (current / ".minecraft" / "config").exists():
                search_paths.append((current / ".minecraft", "客户端"))

            current = current.parent

        for path, env_type in search_paths:
            if path.exists() and path.is_dir():
                self.log(f"找到 {env_type} 配置目录: {path}")
                return path, env_type

        return None, None

    def check_game_dir(self):
        """检查游戏目录是否存在"""
        self.game_dir, self.env_type = self.find_game_dir()

        if self.game_dir is None:
            self.log("无法自动检测到游戏配置目录！", "ERROR")
            self.log("客户端：请将此脚本放在 .minecraft 目录下", "ERROR")
            self.log("服务端：请将此脚本放在服务端根目录下（与 config 文件夹同级）", "ERROR")
            return False

        self.log(f"运行环境: {self.env_type}")
        self.log(f"配置目录: {self.game_dir}")
        return True

    def check_files_exist(self):
        """检查要修改的文件是否都存在"""
        missing_files = []
        found_files = []

        for relative_path in CONFIG_MODIFICATIONS.keys():
            full_path = self.game_dir / relative_path
            if full_path.exists():
                found_files.append(relative_path)
                self.log(f"找到配置文件: {relative_path}")
            else:
                missing_files.append(relative_path)
                self.log(f"配置文件不存在: {relative_path}", "WARNING")

        if missing_files:
            self.log(f"有 {len(missing_files)} 个配置文件未找到", "WARNING")

        return found_files, missing_files

    def is_first_run(self):
        """判断是否是首次运行"""
        return not self.backup_info_file.exists()

    def create_backup(self, files_to_backup):
        """创建初始备份"""
        self.log("=" * 50)
        self.log("首次运行，创建初始备份...")

        # 创建备份目录
        self.backup_dir.mkdir(parents=True, exist_ok=True)

        backup_info = {
            "created_time": datetime.now().isoformat(),
            "game_dir": str(self.game_dir),
            "environment": self.env_type,
            "backed_up_files": []
        }

        for relative_path in files_to_backup:
            source = self.game_dir / relative_path
            dest = self.backup_dir / relative_path

            # 创建子目录
            dest.parent.mkdir(parents=True, exist_ok=True)

            # 复制文件
            shutil.copy2(source, dest)
            backup_info["backed_up_files"].append(relative_path)
            self.log(f"已备份: {relative_path}")

        # 保存备份信息
        with open(self.backup_info_file, 'w', encoding='utf-8') as f:
            json.dump(backup_info, f, ensure_ascii=False, indent=2)

        self.log(f"初始备份完成，共备份 {len(files_to_backup)} 个文件")
        self.log(f"备份位置: {self.backup_dir}")
        self.log("=" * 50)

    def backup_new_files(self, found_files):
        """为新增的配置文件创建单独备份"""
        self.log("检测新增配置文件...")

        # 读取现有备份信息
        existing_backups = set()
        if self.backup_info_file.exists():
            with open(self.backup_info_file, 'r', encoding='utf-8') as f:
                backup_info = json.load(f)
                existing_backups = set(backup_info.get("backed_up_files", []))

        # 找出新增的文件
        new_files = []
        for relative_path in found_files:
            if relative_path not in existing_backups:
                new_files.append(relative_path)

        if not new_files:
            self.log("没有发现新增配置文件")
            return

        self.log(f"发现 {len(new_files)} 个新增配置文件，正在创建备份...")

        # 为新增文件创建备份
        for relative_path in new_files:
            source = self.game_dir / relative_path
            dest = self.backup_dir / relative_path

            # 创建目录结构
            dest.parent.mkdir(parents=True, exist_ok=True)

            # 复制文件
            shutil.copy2(source, dest)
            self.log(f"已备份新增文件: {relative_path}")

        # 更新备份信息
        if self.backup_info_file.exists():
            with open(self.backup_info_file, 'r', encoding='utf-8') as f:
                backup_info = json.load(f)
        else:
            backup_info = {
                "created_time": datetime.now().isoformat(),
                "game_dir": str(self.game_dir),
                "environment": self.env_type,
                "backed_up_files": []
            }

        # 添加新增文件到备份列表
        backup_info["backed_up_files"].extend(new_files)

        # 保存更新后的备份信息
        with open(self.backup_info_file, 'w', encoding='utf-8') as f:
            json.dump(backup_info, f, ensure_ascii=False, indent=2)

        self.log(f"新增文件备份完成，共备份 {len(new_files)} 个文件")

    def restore_from_backup(self, files_to_restore):
        """从备份恢复文件（用于再次修改前）"""
        self.log("从备份恢复原始文件...")

        restored = []
        for relative_path in files_to_restore:
            backup_file = self.backup_dir / relative_path
            if backup_file.exists():
                dest = self.game_dir / relative_path
                shutil.copy2(backup_file, dest)
                restored.append(relative_path)
                self.log(f"已恢复: {relative_path}")
            else:
                self.log(f"备份文件不存在，跳过恢复: {relative_path}", "WARNING")

        return restored

    def apply_modifications(self, relative_path, modifications):
        """应用修改规则"""
        import re

        file_path = self.game_dir / relative_path

        # 读取文件内容
        with open(file_path, 'r', encoding='utf-8', errors='replace') as f:
            content = f.read()

        original_content = content
        changes_made = 0

        for mod in modifications:
            mod_type = mod.get("type", "replace")

            if mod_type == "replace":
                # 简单字符串替换
                search = mod.get("search", "")
                replace = mod.get("replace", "")
                if search in content:
                    content = content.replace(search, replace)
                    changes_made += 1
                    self.log(f"  替换: '{search}' -> '{replace}'")
                else:
                    self.log(f"  未找到: '{search}'", "WARNING")

            elif mod_type == "replace_line":
                # 替换包含特定内容的整行
                search = mod.get("search", "")
                replace = mod.get("replace", "")
                lines = content.split('\n')
                new_lines = []
                found = False
                for line in lines:
                    if search in line:
                        new_lines.append(replace)
                        found = True
                        changes_made += 1
                    else:
                        new_lines.append(line)
                if found:
                    content = '\n'.join(new_lines)
                    self.log(f"  替换行: 包含'{search}'的行 -> '{replace}'")
                else:
                    self.log(f"  未找到包含'{search}'的行", "WARNING")

            elif mod_type == "regex":
                # 正则表达式替换
                pattern = mod.get("pattern", "")
                replace = mod.get("replace", "")
                new_content, count = re.subn(pattern, replace, content)
                if count > 0:
                    content = new_content
                    changes_made += count
                    self.log(f"  正则替换: 模式'{pattern}' 替换了 {count} 处")
                else:
                    self.log(f"  正则未匹配: '{pattern}'", "WARNING")

        # 写入修改后的内容
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return changes_made

        return 0

    def run(self):
        """运行主流程"""
        self.log("=" * 60)
        self.log("GTNH 配置文件修改脚本启动")
        self.log("=" * 60)

        # 检查是否有配置要修改
        if not CONFIG_MODIFICATIONS:
            self.log("没有配置任何要修改的文件！", "ERROR")
            self.log("请编辑脚本中的 CONFIG_MODIFICATIONS 变量添加修改规则", "ERROR")
            return False

        # 检查游戏目录
        if not self.check_game_dir():
            return False

        # 检查文件存在性
        found_files, missing_files = self.check_files_exist()

        if not found_files:
            self.log("没有找到任何要修改的配置文件！", "ERROR")
            return False

        # 判断是否首次运行
        if self.is_first_run():
            # 首次运行：创建备份
            self.create_backup(found_files)
        else:
            # 非首次运行：从备份恢复
            self.log("检测到已有备份，从备份恢复原始文件...")
            self.restore_from_backup(found_files)

        # 应用修改
        self.log("=" * 50)
        self.log("开始应用配置修改...")

        total_changes = 0
        for relative_path in found_files:
            modifications = CONFIG_MODIFICATIONS[relative_path].get("modifications", [])
            if modifications:
                self.log(f"\n处理文件: {relative_path}")
                changes = self.apply_modifications(relative_path, modifications)
                total_changes += changes

        # 完成
        self.log("=" * 50)
        self.log(f"配置修改完成！共进行了 {total_changes} 处修改")
        self.log("=" * 60)

        return True


def main():
    """主函数"""
    modifier = ConfigModifier()

    try:
        success = modifier.run()
        if success:
            print("\n✓ 配置修改成功！")
        else:
            print("\n✗ 配置修改失败，请查看上方日志")
    except Exception as e:
        print(f"\n✗ 发生错误: {e}")
        import traceback
        traceback.print_exc()

    # 暂停等待用户查看结果
    input("\n按回车键退出...")


if __name__ == "__main__":
    main()
