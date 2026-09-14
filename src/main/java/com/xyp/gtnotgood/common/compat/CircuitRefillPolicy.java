package com.xyp.gtnotgood.common.compat;

/** Determines when another complete AE batch can share the machine's existing input buffer. */
public final class CircuitRefillPolicy {

    private CircuitRefillPolicy() {}

    /**
     * An idle empty machine may switch circuits. Otherwise the circuit and buffered recipe must stay unchanged.
     * A running machine also needs a known successful delivery of this recipe; after a chunk reload, an unknown
     * running batch must finish first. Idle buffered recipes can be recovered directly from their full input
     * quantities.
     *
     * @param running          whether an operation is still in progress
     * @param buffered         whether any consumed item or fluid remains in the input buffer
     * @param sameCircuit      whether the virtual circuit already matches the incoming recipe, including no circuit
     * @param sameDelivery     whether the last successfully delivered recipe is the incoming recipe
     * @param compatibleBuffer whether the buffer is empty or contains whole batches of the incoming recipe
     * @return whether recipe isolation permits proceeding to the capacity and sided insertion checks
     */
    public static boolean canAccept(boolean running, boolean buffered, boolean sameCircuit, boolean sameDelivery,
        boolean compatibleBuffer) {
        if (!running && !buffered) return true;
        return sameCircuit && compatibleBuffer && (!running || sameDelivery);
    }
}
