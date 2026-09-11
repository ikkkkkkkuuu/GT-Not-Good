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
    MACHINE(28500),
    BASIC_MACHINE(28600),

    LARGE_ORE_PROCESSOR(MACHINE, 0),
    MAX_CAPACITY_ME_OUTPUT_BUS(MACHINE, 1),
    MAX_CAPACITY_ME_OUTPUT_HATCH(MACHINE, 2),
    SUPER_CRAFTING_INPUT_BUS_ME(MACHINE, 3),
    SUPER_CRAFTING_INPUT_ME(MACHINE, 4),
    SUPER_CRAFTING_INPUT_SLAVE(MACHINE, 5),
    LARGE_VOID_MINER(MACHINE, 6),
    LARGE_BEE_BREEDER(MACHINE, 7),
    LARGE_CROP_BREEDER(MACHINE, 8),
    DIMENSIONALLY_TRANSCENDENT_PLASMA_FUSION_COMPUTER(MACHINE, 9),
    ASSEMBLY_FACTORY(MACHINE, 10),
    SINGULARITY_DATA_HUB(MACHINE, 11),
    VAULT_PORT_HATCH(MACHINE, 12),
    INTEGRATED_PRODUCTION_FACTORY(MACHINE, 13),

    Diesel_Generator_LV(BASIC_MACHINE, 0),
    Diesel_Generator_MV(BASIC_MACHINE, 1),
    Diesel_Generator_HV(BASIC_MACHINE, 2),
    Diesel_Generator_EV(BASIC_MACHINE, 3),
    STEAM_TURBINE_LV(BASIC_MACHINE, 4),
    STEAM_TURBINE_MV(BASIC_MACHINE, 5),
    STEAM_TURBINE_HV(BASIC_MACHINE, 6),
    STEAM_TURBINE_EV(BASIC_MACHINE, 7),
    STEAM_TURBINE_IV(BASIC_MACHINE, 8),
    STEAM_TURBINE_LUV(BASIC_MACHINE, 9);

    public final int ID;
    private static final int META_INCREMENT = 1;

    GTNGMachineID(int ID) {
        this.ID = ID;
    }

    GTNGMachineID(GTNGMachineID base, int offset) {
        this.ID = base.ID + (offset * META_INCREMENT);
    }
}
