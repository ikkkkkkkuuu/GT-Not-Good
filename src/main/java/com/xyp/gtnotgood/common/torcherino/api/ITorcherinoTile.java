package com.xyp.gtnotgood.common.torcherino.api;

/**
 * Shared state contract for every GT Not Good Torcherino tile entity.
 */
public interface ITorcherinoTile {

    /** @return whether redstone currently allows this torch to run */
    boolean getActive();

    /**
     * Updates the redstone-controlled active state.
     *
     * @param active {@code true} when the torch may accelerate targets
     */
    void setActive(boolean active);

    /** @return whether the player-paused flag is set */
    boolean isStopped();

    /** @return effective extra ticks applied per world tick */
    int getEffectiveSpeed();

    /** @return torch X coordinate */
    int getTorchX();

    /** @return torch Y coordinate */
    int getTorchY();

    /** @return torch Z coordinate */
    int getTorchZ();

    /** @return configured X radius */
    int getXRadius();

    /** @return configured Y radius */
    int getYRadius();

    /** @return configured Z radius */
    int getZRadius();
}
