package com.xyp.gtnotgood.utils.enums;

/**
 * Allocates stable GregTech meta-tile entity IDs for GT Not Good machines.
 */
public enum GTNGMachineID {

    /**
     * Base ID for GT Not Good meta-tile entities.
     * <p>
     * The nearby {@code 28001-28055} range is occupied by CropsNH in the GTNH environment, so this mod starts at
     * {@code 28100} to keep its private machine IDs away from that shipped addon range.
     */
    Machine(28500),
    BasicMachine(28600),
    /** LV through MAX: seven laser inputs and one dynamo slot per tier, 28700-28811. */
    WirelessLaser(28700),

    LargeOreProcessor(Machine, 0),
    MaxCapacityMEOutputBus(Machine, 1),
    MaxCapacityMEOutputHatch(Machine, 2),
    SuperCraftingInputBusME(Machine, 3),
    SuperCraftingInputME(Machine, 4),
    SuperCraftingInputSlave(Machine, 5),
    LargeVoidMiner(Machine, 6),
    LargeBeeBreeder(Machine, 7),
    LargeCropBreeder(Machine, 8),
    SingularityDataHub(Machine, 11),
    VaultPortHatch(Machine, 12),
    IntegratedProductionFactory(Machine, 13),
    QuantumComputer(Machine, 14),
    AssemblerMatrix(Machine, 15),
    LargeTransmutationMachine(Machine, 16),
    LargeCombProcessor(Machine, 17),
    MEDataAccessHatch(Machine, 18),
    CompactSuperCraftingInputME(Machine, 19),
    CrossRecipeWirelessEnergyHatch(Machine, 20),
    SuperAdvancedMEInputHatch(Machine, 21),
    SuperAdvancedMEInputBus(Machine, 22),
    CircuitMEPatternBuffer(Machine, 23),

    DieselGeneratorLV(BasicMachine, 0),
    DieselGeneratorMV(BasicMachine, 1),
    DieselGeneratorHV(BasicMachine, 2),
    DieselGeneratorEV(BasicMachine, 3),
    SteamTurbineLV(BasicMachine, 4),
    SteamTurbineMV(BasicMachine, 5),
    SteamTurbineHV(BasicMachine, 6),
    SteamTurbineEV(BasicMachine, 7),
    SteamTurbineIV(BasicMachine, 8),
    SteamTurbineLuV(BasicMachine, 9),
    EssentiaDisassembler(BasicMachine, 10),
    UniversalFluidPump(BasicMachine, 11);

    public final int id;
    private static final int META_INCREMENT = 1;

    GTNGMachineID(int id) {
        this.id = id;
    }

    GTNGMachineID(GTNGMachineID base, int offset) {
        this.id = base.id + (offset * META_INCREMENT);
    }
}
