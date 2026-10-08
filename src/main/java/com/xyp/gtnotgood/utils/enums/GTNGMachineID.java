package com.xyp.gtnotgood.utils.enums;

public enum GTNGMachineID {

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
