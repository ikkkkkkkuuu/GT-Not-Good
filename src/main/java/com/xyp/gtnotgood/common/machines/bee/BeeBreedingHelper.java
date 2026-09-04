package com.xyp.gtnotgood.common.machines.bee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import net.minecraft.item.ItemStack;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBee;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeMutation;
import forestry.api.apiculture.IBeeRoot;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleFloat;
import forestry.api.genetics.IAlleleInteger;
import forestry.api.genetics.IChromosome;
import forestry.apiculture.genetics.alleles.AlleleEffect;
import forestry.core.genetics.alleles.AlleleHelper;
import forestry.core.genetics.alleles.EnumAllele;

/**
 * 蜜蜂繁育工具类
 * <p>
 * 提供杂交路径计算、蜜蜂物品判断等功能
 */
public class BeeBreedingHelper {

    private static final Random RANDOM = new Random();

    // ==================== 缓存 ====================
    // 蜂种模板与突变表在模组加载时注册后即固定，运行期不变，可一次性建索引避免每次全表扫描。

    /** UID → 物种 缓存（O(1) 替代 getSpeciesByUID 的全表遍历） */
    private static Map<String, IAlleleBeeSpecies> uidSpeciesCache = null;

    /** 结果UID → 该品种的所有突变配方 索引（O(1) 替代 getMutationsForUID 的全表遍历） */
    private static Map<String, List<MutationData>> mutationsByUidCache = null;

    /** 显示名缓存（纯函数结果，按输入 key 缓存） */
    private static final Map<String, String> displayNameCache = new HashMap<>();

    /**
     * 懒加载构建 UID→物种 与 结果UID→突变 两张索引。
     * 遍历一次 genome templates + mutations，之后所有查询走 map。
     */
    private static void ensureCaches() {
        if (uidSpeciesCache != null && mutationsByUidCache != null) return;
        IBeeRoot root = getBeeRoot();
        if (root == null) return;

        Map<String, IAlleleBeeSpecies> speciesMap = new HashMap<>();
        Map<String, List<MutationData>> mutationsMap = new HashMap<>();

        Map<String, IAllele[]> templates = root.getGenomeTemplates();
        if (templates != null) {
            for (IAllele[] template : templates.values()) {
                if (template != null && template.length > 0 && template[0] instanceof IAlleleBeeSpecies) {
                    IAlleleBeeSpecies s = (IAlleleBeeSpecies) template[0];
                    speciesMap.putIfAbsent(s.getUID(), s);
                }
            }
        }

        for (IBeeMutation mutation : root.getMutations(false)) {
            IAllele[] resultTemplate = mutation.getTemplate();
            if (resultTemplate.length == 0 || !(resultTemplate[0] instanceof IAlleleBeeSpecies)) continue;
            IAlleleBeeSpecies resultSpecies = (IAlleleBeeSpecies) resultTemplate[0];
            speciesMap.putIfAbsent(resultSpecies.getUID(), resultSpecies);

            IAllele parent1Allele = mutation.getAllele0();
            IAllele parent2Allele = mutation.getAllele1();
            String p1 = parent1Allele != null ? parent1Allele.getUID() : "";
            String p2 = parent2Allele != null ? parent2Allele.getUID() : "";
            mutationsMap.computeIfAbsent(resultSpecies.getUID(), k -> new ArrayList<>())
                .add(new MutationData(p1, p2, resultSpecies.getUID(), mutation.getBaseChance()));
        }

        uidSpeciesCache = speciesMap;
        mutationsByUidCache = mutationsMap;
    }

    /**
     * 获取 Forestry 蜜蜂根接口
     */
    public static IBeeRoot getBeeRoot() {
        return BeeManager.beeRoot;
    }

    /**
     * 判断物品是否为蜜蜂
     */
    public static boolean isBee(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        IBeeRoot root = getBeeRoot();
        return root != null && root.isMember(stack);
    }

    /**
     * 判断物品是否为公主蜂
     */
    public static boolean isPrincess(ItemStack stack) {
        if (!isBee(stack)) return false;
        return getBeeRoot().getType(stack) == EnumBeeType.PRINCESS;
    }

    /**
     * 判断物品是否为雄蜂
     */
    public static boolean isDrone(ItemStack stack) {
        if (!isBee(stack)) return false;
        return getBeeRoot().getType(stack) == EnumBeeType.DRONE;
    }

    /**
     * 判断物品是否为蜂后
     */
    public static boolean isQueen(ItemStack stack) {
        if (!isBee(stack)) return false;
        return getBeeRoot().getType(stack) == EnumBeeType.QUEEN;
    }

    /**
     * 获取蜜蜂的品种标识名（使用未本地化的内部名称，保证客户端/服务端一致）
     */
    public static String getBeeSpecies(ItemStack stack) {
        if (!isBee(stack)) return null;
        IBee bee = getBeeRoot().getMember(stack);
        if (bee == null || bee.getGenome() == null) return null;
        IAlleleBeeSpecies species = bee.getGenome()
            .getPrimary();
        return species != null ? species.getUnlocalizedName() : null;
    }

    /**
     * 获取蜜蜂的品种 UID（Forestry 内部唯一标识符，同一 unlocalizedName 可能有多个不同 UID 的品种）
     */
    public static String getBeeUID(ItemStack stack) {
        if (!isBee(stack)) return null;
        IBee bee = getBeeRoot().getMember(stack);
        if (bee == null || bee.getGenome() == null) return null;
        IAlleleBeeSpecies species = bee.getGenome()
            .getPrimary();
        return species != null ? species.getUID() : null;
    }

    /**
     * 剥离 Forestry/GTNH 模组蜜蜂 unlocalizedName 中的常见前缀。
     * 大小写不敏感，处理 "SpeciesTE"（如 "SpeciesTENickel"→"Nickel"）
     * 和 "species"（如 "speciesCertus"→"Certus"）前缀。
     */
    private static String stripBeePrefix(String name) {
        if (name == null || name.isEmpty()) return name;
        // "SpeciesTE" 前缀（Forestry 原生蜜蜂，如 "SpeciesTENickel"）
        if (name.length() > 9 && name.regionMatches(true, 0, "SpeciesTE", 0, 9)) {
            return name.substring(9);
        }
        // "species" 前缀（ExtraBees/MagicBees 模组蜜蜂，如 "speciesCertus"）
        if (name.length() > 7 && name.regionMatches(true, 0, "species", 0, 7)) {
            return name.substring(7);
        }
        return name;
    }

    /**
     * 从未本地化名称或 UID 中提取可读的品种名。
     * 优先取点分隔最后一段并剥离常见前缀；其次剥离无点格式的前缀；
     * 最后尝试通过物种注册表获取英文名（仅 ASCII）。
     * 如果传入的是 UID，会先通过 UID 查物种再提取显示名。
     * 例如 "magicbees.speciesCertus" → "Certus"、"SpeciesTENickel" → "Nickel"
     */
    public static String getSpeciesDisplayName(String input) {
        if (input == null || input.isEmpty()) return "";
        String cached = displayNameCache.get(input);
        if (cached != null) return cached;
        String result = computeSpeciesDisplayName(input);
        displayNameCache.put(input, result);
        return result;
    }

    private static String computeSpeciesDisplayName(String input) {
        // 优先按 UID 查找物种，使用 Forestry 本地化名称
        IAlleleBeeSpecies uidSpecies = getSpeciesByUID(input);
        if (uidSpecies != null) {
            String name = uidSpecies.getName();
            if (name != null && !name.isEmpty() && !"name".equalsIgnoreCase(name)) {
                return name;
            }
            // 回退到 unlocalizedName 提取
            String uln = uidSpecies.getUnlocalizedName();
            String display = extractDisplayFromUnlocalizedName(uln);
            if (display != null) return display;
        }
        // 按普通名称处理
        String display = extractDisplayFromUnlocalizedName(input);
        if (display != null) return display;
        // 无法剥离前缀：尝试从物种注册表获取本地化名
        IAlleleBeeSpecies species = findSpeciesByUnlocalizedName(input);
        if (species != null) {
            String name = species.getName();
            if (name != null && !name.isEmpty() && !"name".equalsIgnoreCase(name)) {
                return name;
            }
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    /**
     * 从 unlocalizedName 格式的字符串提取显示名（带前缀剥离和首字母大写）
     */
    private static String extractDisplayFromUnlocalizedName(String uln) {
        int lastDot = uln.lastIndexOf('.');
        if (lastDot >= 0) {
            String name = uln.substring(lastDot + 1);
            name = stripBeePrefix(name);
            if (!name.isEmpty()) {
                return Character.toUpperCase(name.charAt(0)) + name.substring(1);
            }
        }
        // 无点分隔：尝试剥离前缀
        String stripped = stripBeePrefix(uln);
        if (!stripped.equals(uln) && !stripped.isEmpty()) {
            return Character.toUpperCase(stripped.charAt(0)) + stripped.substring(1);
        }
        return null;
    }

    /**
     * 仅通过 unlocalizedName（大小写不敏感）查找物种，不触发 matchSpeciesName 避免循环。
     */
    private static IAlleleBeeSpecies findSpeciesByUnlocalizedName(String uln) {
        IBeeRoot root = getBeeRoot();
        if (root == null) return null;
        Map<String, IAllele[]> templates = root.getGenomeTemplates();
        if (templates != null) {
            for (IAllele[] template : templates.values()) {
                if (template != null && template.length > 0 && template[0] instanceof IAlleleBeeSpecies) {
                    IAlleleBeeSpecies s = (IAlleleBeeSpecies) template[0];
                    if (uln.equalsIgnoreCase(s.getUnlocalizedName())) return s;
                }
            }
        }
        for (IBeeMutation mutation : root.getMutations(false)) {
            IAllele[] template = mutation.getTemplate();
            if (template.length > 0 && template[0] instanceof IAlleleBeeSpecies) {
                IAlleleBeeSpecies s = (IAlleleBeeSpecies) template[0];
                if (uln.equalsIgnoreCase(s.getUnlocalizedName())) return s;
            }
        }
        return null;
    }

    /**
     * 将任意形式的品种名规范化为 UID（唯一标识符）。
     * <p>
     * 先尝试 UID 精确查找，再尝试名称模糊匹配。
     * 例如 "Nickel"、"SpeciesTENickel"、"extrabees.species.nickel" 都会规范化为其 UID。
     */
    public static String getCanonicalUID(String rawName) {
        if (rawName == null || rawName.isEmpty()) return rawName;
        // 先尝试 UID 精确查找
        IAlleleBeeSpecies species = getSpeciesByUID(rawName);
        if (species != null) return species.getUID();
        // 再尝试名称查找
        species = getSpeciesByName(rawName);
        if (species != null) return species.getUID();
        // 通过显示名桥接
        String display = getSpeciesDisplayName(rawName);
        if (!display.isEmpty() && !display.equals(rawName)) {
            species = getSpeciesByName(display);
            if (species != null) return species.getUID();
        }
        return rawName;
    }

    /**
     * 将任意形式的品种名规范化为物种注册表中的统一标识名（unlocalizedName）。
     * <p>
     * 例如 "Nickel"、"SpeciesTENickel" 都会规范化为 "extrabees.species.nickel"。
     * ⚠️ 注意：同一 unlocalizedName 可能对应多个不同 UID 的品种，推荐使用 getCanonicalUID()。
     */
    public static String getCanonicalSpeciesName(String rawName) {
        if (rawName == null || rawName.isEmpty()) return rawName;
        IAlleleBeeSpecies species = getSpeciesByName(rawName);
        if (species != null) return species.getUnlocalizedName();
        String display = getSpeciesDisplayName(rawName);
        if (!display.isEmpty() && !display.equals(rawName)) {
            species = getSpeciesByName(display);
            if (species != null) return species.getUnlocalizedName();
        }
        return rawName;
    }

    /**
     * 从 unlocalizedName 中提取可读部分（不去首字母大写）。
     * 用于 matchSpeciesName 快速匹配，统一走 stripBeePrefix。
     */
    private static String extractReadableName(String uln) {
        int lastDot = uln.lastIndexOf('.');
        String name = lastDot >= 0 ? uln.substring(lastDot + 1) : uln;
        return stripBeePrefix(name);
    }

    /**
     * 通过 UID 精确查找物种（UID 在 Forestry 中保证唯一）
     */
    public static IAlleleBeeSpecies getSpeciesByUID(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        ensureCaches();
        return uidSpeciesCache != null ? uidSpeciesCache.get(uid) : null;
    }

    private static boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) return false;
        }
        return true;
    }

    /**
     * 直接从 IAlleleBeeSpecies 获取显示名（无额外查找），供 matchSpeciesName 使用。
     */
    private static String getDisplayNameForSpecies(IAlleleBeeSpecies species) {
        // 优先使用 Forestry 本地化名称
        String name = species.getName();
        if (name != null && !name.isEmpty() && !"name".equalsIgnoreCase(name)) {
            return name;
        }
        // 回退到 unlocalizedName 提取
        String uln = species.getUnlocalizedName();
        if (uln == null || uln.isEmpty()) return "";
        int lastDot = uln.lastIndexOf('.');
        if (lastDot >= 0) {
            String extracted = uln.substring(lastDot + 1);
            extracted = stripBeePrefix(extracted);
            if (!extracted.isEmpty()) {
                return Character.toUpperCase(extracted.charAt(0)) + extracted.substring(1);
            }
        }
        // 无点分隔：尝试剥离前缀
        String stripped = stripBeePrefix(uln);
        if (!stripped.equals(uln) && !stripped.isEmpty()) {
            return Character.toUpperCase(stripped.charAt(0)) + stripped.substring(1);
        }
        return Character.toUpperCase(uln.charAt(0)) + uln.substring(1);
    }

    /**
     * 模拟杂交尝试（使用实际杂交概率）
     *
     * @param chance 杂交成功概率（0-100，来自 Forestry 的 getBaseChance）
     * @return true 如果杂交成功
     */
    public static boolean tryMutation(double chance) {
        return RANDOM.nextDouble() * 100 < chance;
    }

    /**
     * 根据品种名称获取品种等位基因（大小写不敏感，同时匹配 getName() 和 getUnlocalizedName()）
     */
    public static IAlleleBeeSpecies getSpeciesByName(String speciesName) {
        IBeeRoot root = getBeeRoot();
        if (root == null) return null;

        Map<String, IAllele[]> templates = root.getGenomeTemplates();
        if (templates != null) {
            for (IAllele[] template : templates.values()) {
                if (template != null && template.length > 0 && template[0] instanceof IAlleleBeeSpecies) {
                    IAlleleBeeSpecies species = (IAlleleBeeSpecies) template[0];
                    if (matchSpeciesName(species, speciesName)) {
                        return species;
                    }
                }
            }
        }

        for (IAllele allele : root.getDefaultTemplate()) {
            if (allele instanceof IAlleleBeeSpecies) {
                IAlleleBeeSpecies species = (IAlleleBeeSpecies) allele;
                if (matchSpeciesName(species, speciesName)) {
                    return species;
                }
            }
        }

        for (IBeeMutation mutation : root.getMutations(false)) {
            IAllele[] template = mutation.getTemplate();
            if (template.length > 0 && template[0] instanceof IAlleleBeeSpecies) {
                IAlleleBeeSpecies species = (IAlleleBeeSpecies) template[0];
                if (matchSpeciesName(species, speciesName)) {
                    return species;
                }
            }
        }
        return null;
    }

    /**
     * 匹配品种名称（大小写不敏感，同时匹配本地化名、未本地化名、提取的可读名、以及最终显示名）
     */
    private static boolean matchSpeciesName(IAlleleBeeSpecies species, String speciesName) {
        if (species == null || speciesName == null) return false;
        // 匹配本地化名（用户手动输入的英文名）
        if (species.getName() != null && species.getName()
            .equalsIgnoreCase(speciesName)) {
            return true;
        }
        // 匹配未本地化的内部名（NEI拖放获取的标识名）
        if (species.getUnlocalizedName() != null) {
            if (species.getUnlocalizedName()
                .equalsIgnoreCase(speciesName)) {
                return true;
            }
            // 匹配点分隔后的可读名（如 "for.bees.species.Nickel" → "Nickel"）
            String readable = extractReadableName(species.getUnlocalizedName());
            if (readable.equalsIgnoreCase(speciesName)) {
                return true;
            }
            // 匹配最终显示名（无点分隔时 getSpeciesDisplayName 可能通过 getName() 解析出英文名）
            String display = getDisplayNameForSpecies(species);
            if (display.equalsIgnoreCase(speciesName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有已注册的蜜蜂品种名称
     */
    public static List<String> getAllBeeSpeciesNames() {
        List<String> names = new ArrayList<>();
        IBeeRoot root = getBeeRoot();
        if (root == null) return names;

        Set<String> addedNames = new HashSet<>();
        for (IBeeMutation mutation : root.getMutations(false)) {
            IAllele[] template = mutation.getTemplate();
            if (template.length > 0 && template[0] instanceof IAlleleBeeSpecies) {
                String name = ((IAlleleBeeSpecies) template[0]).getName();
                if (!addedNames.contains(name)) {
                    names.add(name);
                    addedNames.add(name);
                }
            }
        }
        return names;
    }

    /**
     * 杂交配方数据
     */
    public static class MutationData {

        public final String parent1;
        public final String parent2;
        public final String result;
        public final double chance;

        public MutationData(String parent1, String parent2, String result, double chance) {
            this.parent1 = parent1;
            this.parent2 = parent2;
            this.result = result;
            this.chance = chance;
        }
    }

    /**
     * 获取指定品种的所有杂交配方（作为结果）（大小写不敏感匹配）
     */
    public static List<MutationData> getMutationsForSpecies(String speciesName) {
        List<MutationData> mutations = new ArrayList<>();
        IBeeRoot root = getBeeRoot();
        if (root == null) return mutations;

        for (IBeeMutation mutation : root.getMutations(false)) {
            IAllele[] resultTemplate = mutation.getTemplate();
            if (resultTemplate.length > 0 && resultTemplate[0] instanceof IAlleleBeeSpecies) {
                IAlleleBeeSpecies resultSpecies = (IAlleleBeeSpecies) resultTemplate[0];
                if (matchSpeciesName(resultSpecies, speciesName)) {
                    String parent1 = mutation.getAllele0()
                        .getUnlocalizedName();
                    String parent2 = mutation.getAllele1()
                        .getUnlocalizedName();
                    mutations.add(new MutationData(parent1, parent2, speciesName, mutation.getBaseChance()));
                }
            }
        }
        return mutations;
    }

    /**
     * 通过 UID 获取指定品种的所有杂交配方（作为结果）。
     * 返回的 MutationData 中 parent1/parent2/result 均为 UID 字符串。
     */
    public static List<MutationData> getMutationsForUID(String uid) {
        ensureCaches();
        if (mutationsByUidCache == null) return new ArrayList<>();
        List<MutationData> cached = mutationsByUidCache.get(uid);
        // 返回副本：调用方（createBreedingChainForUID）会对结果做 removeIf，不能污染缓存
        return cached != null ? new ArrayList<>(cached) : new ArrayList<>();
    }

    /**
     * 杂交路径中的一步
     */
    public static class BreedingStep {

        public final String parent1;
        public final String parent2;
        public final String result;
        public final double chance;

        public BreedingStep(String parent1, String parent2, String result, double chance) {
            this.parent1 = parent1;
            this.parent2 = parent2;
            this.result = result;
            this.chance = chance;
        }

        @Override
        public String toString() {
            return parent1 + " + " + parent2 + " -> " + result;
        }
    }

    /**
     * 蜜蜂杂交路径优先配置（当有多条路径时优先选择）
     * 键为 target 品种的内部标识名（unlocalizedName），值为优先选择的亲本对
     */
    private static final Map<String, String[]> BREEDING_PREFERENCES = new HashMap<>();

    private static void putPref(String target, String parent1, String parent2) {
        BREEDING_PREFERENCES.put(target, new String[] { parent1, parent2 });
    }

    static {
        // 初始化时用 English 名填入，运行时由 resolvePreferences() 转为 unlocalizedName
        putPref("Common", "Forest", "Meadows");
        putPref("Cultivated", "Common", "Forest");
        putPref("Sinister", "Cultivated", "Modest");
        putPref("Fiendish", "Sinister", "Cultivated");
        putPref("Frugal", "Modest", "Sinister");
        putPref("Arid", "Meadows", "Frugal");
        putPref("Diamond", "Certus", "Coal");
        putPref("Ruby", "Redstone", "Diamond");
        putPref("Sapphire", "Certus", "Lapis");
        putPref("Emerald", "Olivine", "Diamond");
        putPref("Rusty", "Meadows", "Resilient");
        putPref("Corroded", "Wintry", "Resilient");
        putPref("Leaden", "Meadows", "Resilient");
        putPref("Tarnished", "Marshy", "Resilient");
        putPref("Lustered", "Forest", "Resilient");
        putPref("Galvanized", "Wintry", "Resilient");
        putPref("Shining", "Majestic", "Galvanized");
        putPref("Glittering", "Majestic", "Rusty");
        putPref("Oily", "Ocean", "Primeval");
        putPref("Fossilised", "Primeval", "Growing");
        putPref("Fungal", "Boggy", "Miry");
        putPref("Scummy", "Agrarian", "Exotic");
        putPref("Spirit", "Ethereal", "Aware");
        putPref("Nuclear", "Unstable", "Rusty");
        putPref("Vengeful", "Demonic", "Vindictive");
        putPref("Eldritch", "Mystical", "Cultivated");
        putPref("Indium", "Lead", "Osmium");
    }

    /**
     * 将 BREEDING_PREFERENCES 中的 English 名称转为 unlocalizedName（在所有蜂种注册后调用）
     */
    private static boolean preferencesResolved = false;

    /** UID 版本的偏好映射（与 BREEDING_PREFERENCES 对应，但键值为 UID） */
    private static final Map<String, String[]> BREEDING_PREFERENCES_UID = new HashMap<>();

    private static void resolvePreferences() {
        if (preferencesResolved) return;
        preferencesResolved = true;

        Map<String, String[]> resolved = new HashMap<>();
        Map<String, String[]> resolvedUID = new HashMap<>();
        for (Map.Entry<String, String[]> entry : BREEDING_PREFERENCES.entrySet()) {
            String targetEnglish = entry.getKey();
            String[] parentsEnglish = entry.getValue();

            // 解析 target 品种名
            IAlleleBeeSpecies targetSpecies = findSpeciesByEnglishName(targetEnglish);
            String targetKey = targetSpecies != null ? targetSpecies.getUnlocalizedName() : targetEnglish;

            // 解析 parent 品种名
            IAlleleBeeSpecies p1Species = findSpeciesByEnglishName(parentsEnglish[0]);
            IAlleleBeeSpecies p2Species = findSpeciesByEnglishName(parentsEnglish[1]);
            String p1Key = p1Species != null ? p1Species.getUnlocalizedName() : parentsEnglish[0];
            String p2Key = p2Species != null ? p2Species.getUnlocalizedName() : parentsEnglish[1];

            resolved.put(targetKey, new String[] { p1Key, p2Key });

            // UID 版本
            if (targetSpecies != null && p1Species != null && p2Species != null) {
                resolvedUID.put(targetSpecies.getUID(), new String[] { p1Species.getUID(), p2Species.getUID() });
            }
        }

        BREEDING_PREFERENCES.clear();
        BREEDING_PREFERENCES.putAll(resolved);
        BREEDING_PREFERENCES_UID.putAll(resolvedUID);
    }

    /**
     * 通过 English 名（getName() 返回值）查找 IAlleleBeeSpecies
     */
    private static IAlleleBeeSpecies findSpeciesByEnglishName(String englishName) {
        IBeeRoot root = getBeeRoot();
        if (root == null) return null;

        // 遍历 genome templates
        Map<String, IAllele[]> templates = root.getGenomeTemplates();
        if (templates != null) {
            for (IAllele[] template : templates.values()) {
                if (template != null && template.length > 0 && template[0] instanceof IAlleleBeeSpecies) {
                    IAlleleBeeSpecies species = (IAlleleBeeSpecies) template[0];
                    if (englishName.equalsIgnoreCase(species.getName())) {
                        return species;
                    }
                }
            }
        }

        // 遍历 mutations
        for (IBeeMutation mutation : root.getMutations(false)) {
            IAllele[] template = mutation.getTemplate();
            if (template.length > 0 && template[0] instanceof IAlleleBeeSpecies) {
                IAlleleBeeSpecies species = (IAlleleBeeSpecies) template[0];
                if (englishName.equalsIgnoreCase(species.getName())) {
                    return species;
                }
            }
        }
        return null;
    }

    /**
     * 选择最佳杂交配方
     */
    public static MutationData selectBestMutation(String species, List<MutationData> mutations) {
        return selectBestMutationForUID(species, mutations);
    }

    /**
     * 选择最佳杂交配方（UID 版本）
     */
    public static MutationData selectBestMutationForUID(String uid, List<MutationData> mutations) {
        if (mutations.isEmpty()) return null;
        if (mutations.size() == 1) return mutations.get(0);

        resolvePreferences();
        String[] preference = BREEDING_PREFERENCES_UID.get(uid);
        if (preference != null) {
            for (MutationData mutation : mutations) {
                if ((mutation.parent1.equals(preference[0]) && mutation.parent2.equals(preference[1]))
                    || (mutation.parent1.equals(preference[1]) && mutation.parent2.equals(preference[0]))) {
                    return mutation;
                }
            }
        }
        return mutations.get(0);
    }

    /**
     * 创建从现有蜜蜂到目标蜜蜂的杂交路径（UID 版本 BFS）。
     * 所有字符串参数均为 UID。
     */
    public static List<BreedingStep> createBreedingChainForUID(String targetUID, Set<String> existingUIDs) {
        if (existingUIDs.contains(targetUID)) {
            return new ArrayList<>();
        }

        Map<String, BreedingStep> breedingChain = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>(existingUIDs);
        visited.add(targetUID);

        List<MutationData> targetMutations = getMutationsForUID(targetUID);
        targetMutations.removeIf(m -> m.parent1.equals(targetUID) || m.parent2.equals(targetUID));
        MutationData targetMutation = selectBestMutationForUID(targetUID, targetMutations);
        if (targetMutation == null) {
            return new ArrayList<>();
        }

        breedingChain.put(
            targetUID,
            new BreedingStep(targetMutation.parent1, targetMutation.parent2, targetUID, targetMutation.chance));
        queue.add(targetUID);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            BreedingStep step = breedingChain.get(current);
            if (step == null) continue;

            for (String parent : new String[] { step.parent1, step.parent2 }) {
                if (visited.contains(parent)) continue;
                visited.add(parent);

                List<MutationData> parentMutations = getMutationsForUID(parent);
                parentMutations.removeIf(m -> m.parent1.equals(targetUID) || m.parent2.equals(targetUID));
                MutationData parentMutation = selectBestMutationForUID(parent, parentMutations);
                if (parentMutation != null) {
                    breedingChain.put(
                        parent,
                        new BreedingStep(
                            parentMutation.parent1,
                            parentMutation.parent2,
                            parent,
                            parentMutation.chance));
                    queue.add(parent);
                }
            }
        }

        List<BreedingStep> orderedSteps = new ArrayList<>();
        Set<String> canBreed = new HashSet<>(existingUIDs);

        boolean changed = true;
        while (changed && !breedingChain.isEmpty()) {
            changed = false;
            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, BreedingStep> entry : breedingChain.entrySet()) {
                BreedingStep step = entry.getValue();
                if (canBreed.contains(step.parent1) && canBreed.contains(step.parent2)) {
                    orderedSteps.add(step);
                    canBreed.add(step.result);
                    toRemove.add(entry.getKey());
                    changed = true;
                }
            }

            for (String key : toRemove) {
                breedingChain.remove(key);
            }
        }

        return orderedSteps;
    }

    /**
     * 创建从现有蜜蜂到目标蜜蜂的杂交路径（BFS）
     *
     * @param targetBee    目标蜜蜂品种名
     * @param existingBees 现有蜜蜂品种名集合
     * @return 杂交步骤列表（从基础蜜蜂到目标蜜蜂的顺序），每步包含实际杂交概率
     */
    public static List<BreedingStep> createBreedingChain(String targetBee, Set<String> existingBees) {
        if (existingBees.contains(targetBee)) {
            return new ArrayList<>();
        }

        Map<String, BreedingStep> breedingChain = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>(existingBees);
        visited.add(targetBee);

        List<MutationData> targetMutations = getMutationsForSpecies(targetBee);
        targetMutations.removeIf(m -> m.parent1.equals(targetBee) || m.parent2.equals(targetBee));
        MutationData targetMutation = selectBestMutation(targetBee, targetMutations);
        if (targetMutation == null) {
            return new ArrayList<>();
        }

        breedingChain.put(
            targetBee,
            new BreedingStep(targetMutation.parent1, targetMutation.parent2, targetBee, targetMutation.chance));
        queue.add(targetBee);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            BreedingStep step = breedingChain.get(current);
            if (step == null) continue;

            for (String parent : new String[] { step.parent1, step.parent2 }) {
                if (visited.contains(parent)) continue;
                visited.add(parent);

                List<MutationData> parentMutations = getMutationsForSpecies(parent);
                parentMutations.removeIf(m -> m.parent1.equals(targetBee) || m.parent2.equals(targetBee));
                MutationData parentMutation = selectBestMutation(parent, parentMutations);
                if (parentMutation != null) {
                    breedingChain.put(
                        parent,
                        new BreedingStep(
                            parentMutation.parent1,
                            parentMutation.parent2,
                            parent,
                            parentMutation.chance));
                    queue.add(parent);
                }
            }
        }

        List<BreedingStep> orderedSteps = new ArrayList<>();
        Set<String> canBreed = new HashSet<>(existingBees);

        boolean changed = true;
        while (changed && !breedingChain.isEmpty()) {
            changed = false;
            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, BreedingStep> entry : breedingChain.entrySet()) {
                BreedingStep step = entry.getValue();
                if (canBreed.contains(step.parent1) && canBreed.contains(step.parent2)) {
                    orderedSteps.add(step);
                    canBreed.add(step.result);
                    toRemove.add(entry.getKey());
                    changed = true;
                }
            }

            for (String key : toRemove) {
                breedingChain.remove(key);
            }
        }

        return orderedSteps;
    }

    /**
     * 将模板中除物种(SPECIES)外的所有染色体覆写为“满分”等位基因。
     * <p>
     * <ul>
     * <li>工作(生产)速度最高 (Speed.FASTEST)</li>
     * <li>授粉速度最快 (Flowering.MAXIMUM)</li>
     * <li>温度/湿度适应度拉满 (Tolerance.BOTH_5，上下各 ±5)</li>
     * <li>昼夜性：夜行/全天可工作 (NOCTURNAL = true)</li>
     * <li>耐雨 (TOLERANT_FLYER = true)</li>
     * <li>穴居 (CAVE_DWELLING = true)</li>
     * <li>寿命最长 (Lifespan.LONGEST)</li>
     * <li>生育能力固定为 1 (Fertility.LOW)</li>
     * <li>采蜜对象：鲜花 (Flowers.VANILLA)</li>
     * <li>活动范围：平均 (Territory.AVERAGE)</li>
     * <li>特殊效果：无 (effectNone)</li>
     * </ul>
     * 模板由 {@code getTemplate()} 返回的独立副本（Arrays.copyOf），可安全就地修改。
     */
    private static void applyMaxGenome(IAllele[] template) {
        applyMaxGenome(template, false);
    }

    /**
     * @param mortal true = 凡蜂模式（寿命夭折 + 生育保持原本）：世界蜂巢掉落 / 普通蜂箱杂交走此路径；
     *               false = 满基因模式（寿命不死 + 生育固定 LOW）：本 mod 杂交机 createDrone/createPrincess 走此路径。
     *               其余染色体（速度/授粉/温湿度/夜行/耐雨/穴居/采蜜/领地/效果）两种模式一致，全部拉满。
     */
    private static void applyMaxGenome(IAllele[] template, boolean mortal) {
        if (template == null || AlleleHelper.instance == null) return;
        AlleleHelper helper = AlleleHelper.instance;
        // 速度：优先取运行时注册的最高档；没有时回退到林业原版 FASTEST。
        IAllele maxSpeed = findHighestValueAllele(EnumBeeChromosome.SPEED);
        if (maxSpeed != null) {
            helper.set(template, EnumBeeChromosome.SPEED, maxSpeed);
        } else {
            helper.set(template, EnumBeeChromosome.SPEED, EnumAllele.Speed.FASTEST);
        }
        helper.set(template, EnumBeeChromosome.FLOWERING, EnumAllele.Flowering.MAXIMUM);
        helper.set(template, EnumBeeChromosome.TEMPERATURE_TOLERANCE, EnumAllele.Tolerance.BOTH_5);
        helper.set(template, EnumBeeChromosome.HUMIDITY_TOLERANCE, EnumAllele.Tolerance.BOTH_5);
        helper.set(template, EnumBeeChromosome.NOCTURNAL, true);
        helper.set(template, EnumBeeChromosome.TOLERANT_FLYER, true);
        helper.set(template, EnumBeeChromosome.CAVE_DWELLING, true);
        if (mortal) {
            // 凡蜂：寿命夭折(最短)、生育保持原本(不覆写)。世界蜂巢掉落与普通蜂箱杂交走此。
            helper.set(template, EnumBeeChromosome.LIFESPAN, EnumAllele.Lifespan.SHORTEST);
        } else {
            // 满基因：寿命优先取运行时注册的最高档；没有时回退原版 LONGEST。
            IAllele maxLifespan = findHighestValueAllele(EnumBeeChromosome.LIFESPAN);
            if (maxLifespan != null) {
                helper.set(template, EnumBeeChromosome.LIFESPAN, maxLifespan);
            } else {
                helper.set(template, EnumBeeChromosome.LIFESPAN, EnumAllele.Lifespan.LONGEST);
            }
            // 生育：固定 LOW(1 只)。
            helper.set(template, EnumBeeChromosome.FERTILITY, EnumAllele.Fertility.LOW);
        }
        helper.set(template, EnumBeeChromosome.FLOWER_PROVIDER, EnumAllele.Flowers.VANILLA);
        helper.set(template, EnumBeeChromosome.TERRITORY, EnumAllele.Territory.AVERAGE);
        if (AlleleEffect.effectNone != null) {
            helper.set(template, EnumBeeChromosome.EFFECT, AlleleEffect.effectNone);
        }
    }

    /**
     * 在指定染色体类型上，遍历所有「运行时注册」的等位基因，返回数值最大的那个。
     * <p>
     * 速度(SPEED)是 {@link IAlleleFloat}、寿命(LIFESPAN)是 {@link IAlleleInteger}，
     * 都实现了 getValue()。除林业原版 FASTEST(1.7)/LONGEST(70) 外，其它蜂类附属
     * 模组可能注册更高档(如极速/永生)，这里按实际数值挑最高，从而始终选到全局最优。
     *
     * @return 数值最高的等位基因；若该染色体无任何带数值的等位基因则返回 null（调用方回退到原版枚举）。
     */
    private static IAllele findHighestValueAllele(EnumBeeChromosome chromosome) {
        if (AlleleManager.alleleRegistry == null) return null;
        Collection<IAllele> alleles = AlleleManager.alleleRegistry.getRegisteredAlleles(chromosome);
        if (alleles == null || alleles.isEmpty()) return null;

        IAllele best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (IAllele allele : alleles) {
            double value;
            if (allele instanceof IAlleleFloat) {
                value = ((IAlleleFloat) allele).getValue();
            } else if (allele instanceof IAlleleInteger) {
                value = ((IAlleleInteger) allele).getValue();
            } else {
                continue;
            }
            if (value > bestValue) {
                bestValue = value;
                best = allele;
            }
        }
        return best;
    }

    /**
     * 把一只蜜蜂 ItemStack 就地重建为「满基因」——物种(SPECIES)保持原样，其余染色体全部拉满
     * （速度/授粉/温湿度耐性/夜行/耐雨/穴居/寿命/生育/采蜜对象/领地/效果），并写成纯合(可稳定遗传)。
     * <p>
     * 与 {@code createDrone}/{@code createPrincess} 同一套逻辑（{@code applyMaxGenome} + {@code templateAsGenome}），
     * 区别是模板取自传入蜂的 <b>当前激活等位基因</b>（保留物种），而非固定物种模板。
     * 这是<b>真正改写基因组 NBT</b>：分析仪能读到满值、后代能 breed true。
     * <p>
     * 供蜂巢掉落 mixin（{@code MixinBlockBeehives}）调用。杂交机不经此路径，天然隔离。
     *
     * @return 满基因蜂 ItemStack（保留原蜂型 公主/雄蜂/蜂后）；无法处理时返回原 stack 不变。
     */
    public static ItemStack maximizeBeeStack(ItemStack stack) {
        if (!isBee(stack)) return stack;
        IBeeRoot root = getBeeRoot();
        if (root == null) return stack;

        IBee bee = root.getMember(stack);
        IBee maxBee = maximizeBee(bee);
        if (maxBee == null || maxBee == bee) return stack;

        // 保留原蜂型（公主 / 雄蜂 / 蜂后）。
        EnumBeeType type = root.getType(stack);
        int typeOrdinal = type != null ? type.ordinal() : EnumBeeType.DRONE.ordinal();
        ItemStack result = root.getMemberStack(maxBee, typeOrdinal);
        return result != null ? result : stack;
    }

    /**
     * 把一只 {@link IBee} 重建为「满基因」——物种(SPECIES)保持原样，其余染色体全部拉满并写成纯合(可稳定遗传)。
     * <p>
     * 是 {@link #maximizeBeeStack} 与蜂箱杂交注入（{@code MixinBeeHomozygous} 在 {@code Bee.createOffspring}
     * 的 RETURN 处）共用的核心：前者作用于世界蜂巢掉落，后者作用于普通蜂箱杂交后代。均<b>真正改写基因组 NBT</b>，
     * 因此分析仪能读到满值、后代能 breed true。
     * <p>
     * <b>杂交机隔离</b>：杂交机走 {@code createDrone/createPrincess → templateAsGenome} 直接生成，既不经蜂巢掉落也不经
     * {@code createOffspring}，故不受这两处影响。
     *
     * @return 满基因蜂（保留物种，isNatural 沿用原蜂）；无法处理时返回传入的原蜂不变。
     */
    public static IBee maximizeBee(IBee bee) {
        if (bee == null || bee.getGenome() == null) return bee;
        IBeeRoot root = getBeeRoot();
        if (root == null || AlleleHelper.instance == null) return bee;
        IBeeGenome genome = bee.getGenome();

        // 用当前激活等位基因建模板（保留物种），再把非物种染色体拉满。
        // ⚠️ Forestry 把 EnumBeeChromosome.HUMIDITY 标为「未使用」染色体（见 core.genetics.Genome
        // 的 unusedChromsomes），合法蜂的 chromosomes[HUMIDITY.ordinal()] 恒为 null。若对它调
        // genome.getActiveAllele(HUMIDITY)（内部 chromosomes[ordinal].getActiveAllele()）会直接 NPE，
        // 破坏世界蜂巢时在封包线程崩服（"Failed to handle packet" + 玩家掉线回主界面）。
        // 因此直接读 genome.getChromosomes() 逐槽取激活等位基因，跳过 null 染色体（HUMIDITY 位置留 null，
        // SpeciesRoot.templateAsChromosomes 对 template 里的 null 项本就跳过，不影响构造）。
        EnumBeeChromosome[] types = EnumBeeChromosome.values();
        IChromosome[] chromosomes = genome.getChromosomes();
        IAllele[] template = new IAllele[types.length];
        for (EnumBeeChromosome type : types) {
            int ordinal = type.ordinal();
            if (ordinal >= chromosomes.length) continue;
            IChromosome chromosome = chromosomes[ordinal]; // 未使用染色体（如 HUMIDITY）为 null
            if (chromosome == null) continue;
            template[ordinal] = chromosome.getActiveAllele();
        }
        if (template[EnumBeeChromosome.SPECIES.ordinal()] == null) return bee; // 物种缺失，放弃处理

        applyMaxGenome(template, true); // 凡蜂模式：寿命夭折+生育原值。世界蜂巢掉落/普通蜂箱杂交走此，物种原样保留

        IBeeGenome maxGenome = root.templateAsGenome(template); // 纯合基因组
        IBee maxBee = root.getBee(null, maxGenome);
        if (maxBee == null) return bee;
        maxBee.setIsNatural(bee.isNatural());
        return maxBee;
    }

    /**
     * 创建指定品种的雄蜂 ItemStack。
     * 优先按 UID 查找，其次按名称查找。
     */
    public static ItemStack createDrone(String speciesName) {
        IBeeRoot root = getBeeRoot();
        if (root == null) return null;

        IAlleleBeeSpecies species = getSpeciesByUID(speciesName);
        if (species == null) species = getSpeciesByName(speciesName);
        if (species == null) return null;

        IAllele[] template = root.getTemplate(species.getUID());
        if (template == null) return null;

        applyMaxGenome(template);
        IBeeGenome genome = root.templateAsGenome(template);
        IBee bee = root.getBee(null, genome);
        if (bee == null) return null;
        return root.getMemberStack(bee, EnumBeeType.DRONE.ordinal());
    }

    /**
     * 创建指定品种的公主蜂 ItemStack。
     * 优先按 UID 查找，其次按名称查找。
     */
    public static ItemStack createPrincess(String speciesName) {
        IBeeRoot root = getBeeRoot();
        if (root == null) return null;

        IAlleleBeeSpecies species = getSpeciesByUID(speciesName);
        if (species == null) species = getSpeciesByName(speciesName);
        if (species == null) return null;

        IAllele[] template = root.getTemplate(species.getUID());
        if (template == null) return null;

        applyMaxGenome(template);
        IBeeGenome genome = root.templateAsGenome(template);
        IBee bee = root.getBee(null, genome);
        if (bee == null) return null;
        return root.getMemberStack(bee, EnumBeeType.PRINCESS.ordinal());
    }
}
