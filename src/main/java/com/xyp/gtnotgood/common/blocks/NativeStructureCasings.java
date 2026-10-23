package com.xyp.gtnotgood.common.blocks;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.fieldextruder.FieldExtruderCasing;
import com.xyp.gtnotgood.common.blocks.heart.HeartCasing;
import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;
import com.xyp.gtnotgood.common.blocks.tentacle.TentacleCasing;

import bartworks.common.loaders.ItemRegistry;
import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.TextureFactory;

public final class NativeStructureCasings {

    private static final Map<StructureCasing, Supplier<ItemStack>> REPLACEMENTS = replacements();

    private static final Map<StructureCasing, ITexture> TEXTURES = new LinkedHashMap<>();

    private NativeStructureCasings() {}

    private static Map<StructureCasing, Supplier<ItemStack>> replacements() {
        Map<StructureCasing, Supplier<ItemStack>> replacements = new LinkedHashMap<>();
        replacements.put(AssemblyPlantCasing.LaminatedGlass, () -> Casings.ChemicalGradeGlass.toStack(1));
        replacements.put(AssemblyPlantCasing.FilterCasing, () -> Casings.FilterMachineCasing.toStack(1));
        replacements.put(AssemblyPlantCasing.PtfePipeCasing, () -> Casings.PTFEPipeCasing.toStack(1));
        replacements.put(AssemblyPlantCasing.TemperedGlass, () -> new ItemStack(ItemRegistry.bw_realglas, 1, 0));
        replacements.put(DimensionalFocusCasing.DimensionallyTranscendent,
            () -> Casings.DimensionallyTranscendentCasing.toStack(1));
        replacements.put(DimensionalFocusCasing.DimensionInjection,
            () -> Casings.DimensionalInjectionCasing.toStack(1));
        replacements.put(DimensionalFocusCasing.MolecularCoil, () -> Casings.MolecularCoil.toStack(1));
        replacements.put(FieldExtruderCasing.Iridium, () -> Casings.AdvancedIridiumPlatedMachineCasing.toStack(1));
        replacements.put(FieldExtruderCasing.Molecular, () -> Casings.MolecularCasing.toStack(1));
        replacements.put(FieldExtruderCasing.Hollow, () -> Casings.HollowCasing.toStack(1));
        replacements.put(FieldExtruderCasing.ContainmentFieldGenerator,
            () -> Casings.ContainmentFieldGenerator.toStack(1));
        replacements.put(HeartCasing.DysonDeploymentMagnet, () -> ItemList.DysonSwarmDeploymentUnitMagnet.get(1));
        replacements.put(KerrNewmanCasing.PressureContainmentCasing,
            () -> Casings.PressureContainmentCasing.toStack(1));
        replacements.put(KerrNewmanCasing.ShieldedAcceleratorCasing,
            () -> Casings.ShieldedAcceleratorCasing.toStack(1));
        replacements.put(KerrNewmanCasing.DimensionalBridgeCasing, () -> Casings.DimensionalBridge.toStack(1));
        replacements.put(KerrNewmanCasing.QuantumGlass, () -> Casings.QuantumGlass.toStack(1));
        replacements.put(KerrNewmanCasing.FusionCasingMkIV, () -> Casings.FusionMachineCasingMKIV.toStack(1));
        replacements.put(KerrNewmanCasing.AdvancedFusionCoil, () -> Casings.AdvancedFusionCoil.toStack(1));
        replacements.put(LightHunterCasing.UhvMachineCasing, () -> Casings.UHVMachineCasing.toStack(1));
        replacements.put(LightHunterCasing.UivMachineCasing, () -> Casings.UIVMachineCasing.toStack(1));
        replacements.put(LightHunterCasing.UxvMachineCasing, () -> Casings.UXVMachineCasing.toStack(1));
        replacements.put(LightHunterCasing.ComputerHeatVent, () -> Casings.ComputerHeatVent.toStack(1));
        replacements.put(LightHunterCasing.HighPowerCasing, () -> Casings.HighPowerCasing.toStack(1));
        replacements.put(LightHunterCasing.AdvancedComputerCasing, () -> Casings.AdvancedComputerCasing.toStack(1));
        replacements.put(LightHunterCasing.UxvHermeticCasing, () -> Casings.HermeticCasing12.toStack(1));
        replacements.put(LightHunterCasing.ForceFieldGlass, () -> Casings.ForceFieldGlass.toStack(1));
        replacements.put(LightHunterCasing.DimensionalBridgeCasing, () -> Casings.DimensionalBridge.toStack(1));
        replacements.put(LightHunterCasing.FusionCasingMkIV, () -> Casings.FusionMachineCasingMKIV.toStack(1));
        replacements.put(ParticleColliderCasing.SuperconductingCoil, () -> Casings.SuperconductingCoilBlock.toStack(1));
        replacements.put(TentacleCasing.MolecularCasing, () -> Casings.MolecularCasing.toStack(1));
        replacements.put(TentacleCasing.ContainmentFieldGenerator, () -> Casings.ContainmentFieldGenerator.toStack(1));
        replacements.put(TentacleCasing.HollowCasing, () -> Casings.HollowCasing.toStack(1));
        replacements.put(HeartCasing.GravitonFieldConstraintCasing,
            () -> Casings.UltimateContainmentFieldGenerator.toStack(1));
        replacements.put(HeartCasing.DimensionalStabilityCasing, () -> Casings.CosmicFabricShieldingCore.toStack(1));
        replacements.put(HeartCasing.TemporalAnchorFieldCasing, () -> Casings.SpacetimeAlteringCasing.toStack(1));
        replacements.put(HeartCasing.DimensionConnectionCasing, () -> Casings.DimensionalBridgeGenerator.toStack(1));
        replacements.put(HeartCasing.CreateCasing, () -> Casings.InfiniteSpacetimeEnergyBoundaryCasing.toStack(1));
        replacements.put(LightHunterCasing.UltimateStellarContainmentCasing,
            () -> Casings.SingularityReinforcedStellarShieldingCasing.toStack(1));
        return Collections.unmodifiableMap(replacements);
    }

    public static boolean contains(StructureCasing casing) {
        return REPLACEMENTS.containsKey(casing);
    }

    public static Set<StructureCasing> variants() {
        return REPLACEMENTS.keySet();
    }

    public static ItemStack stack(StructureCasing casing, int amount) {
        ItemStack stack = Objects.requireNonNull(REPLACEMENTS.get(casing).get(), "Missing native casing: " + casing)
            .copy();
        if (Block.getBlockFromItem(stack.getItem()) == Blocks.air)
            throw new IllegalStateException("Native casing is not a block: " + casing);
        stack.stackSize = amount;
        return stack;
    }

    public static ITexture texture(StructureCasing casing) {
        return TEXTURES.computeIfAbsent(casing, DeferredTexture::new);
    }

    /** Texture registration runs before all native blocks exist. Resolve only when actually rendering. */
    private static final class DeferredTexture implements ITexture {

        private final StructureCasing casing;
        private ITexture resolved;

        private DeferredTexture(StructureCasing casing) {
            this.casing = casing;
        }

        private ITexture resolve() {
            if (resolved == null)
                resolved = TextureFactory.of(casing.getStructureBlock(), casing.getStructureMetadata());
            return resolved;
        }

        @Override
        public void renderXPos(ISBRContext context) {
            resolve().renderXPos(context);
        }

        @Override
        public void renderXNeg(ISBRContext context) {
            resolve().renderXNeg(context);
        }

        @Override
        public void renderYPos(ISBRContext context) {
            resolve().renderYPos(context);
        }

        @Override
        public void renderYNeg(ISBRContext context) {
            resolve().renderYNeg(context);
        }

        @Override
        public void renderZPos(ISBRContext context) {
            resolve().renderZPos(context);
        }

        @Override
        public void renderZNeg(ISBRContext context) {
            resolve().renderZNeg(context);
        }

        @Override
        public boolean isValidTexture() {
            return true;
        }
    }

    /** External block registrations must be complete before resolving item aliases and textures. */
    public static void bind() {
        for (StructureCasing casing : variants()) {
            casing.getItemContainer().set(casing.getStack(1));
            Textures.BlockIcons.setCasingTextureForId(casing.getTextureId(), texture(casing));
        }
    }
}
