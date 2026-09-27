// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good machines.
package com.xyp.gtnotgood.utils.crafting;

import java.util.Deque;
import java.util.List;

import appeng.api.storage.data.IAEStack;
import appeng.crafting.MECraftingInventory;
import lombok.Getter;

/**
 * Plans and commits one exact crafting dispatch batch.
 * <p>
 * The planner exists because AE must use one quantity for energy checks, material extraction, medium submission,
 * diagnostics, and expected outputs. Its API keeps arithmetic and transaction rules independent from mixin injection
 * details so they can be verified directly.
 */
public interface CraftingBatchPlanner {

    /**
     * Describes whether every medium registered for a pattern supports the same batching contract.
     */
    enum MediumStrategy {

        /** All registered media are compatible with exact long-sized batching. */
        BATCH,

        /** At least one registered medium is absent or incompatible, so AE must use its native single-craft path. */
        NATIVE
    }

    /**
     * Identifies the constraint that selected the final craft count, primarily for diagnostics and tests.
     */
    enum LimitingFactor {

        /** The complete medium set does not support batching. */
        MEDIUM,

        /** The task itself has only one craft left. */
        TASK,

        /** An input or output does not satisfy the item-stack batching contract. */
        STACK_CONTRACT,

        /** A checked long multiplication limits the batch. */
        LONG_ARITHMETIC,

        /** The precise, aggregated material inventory limits the batch. */
        MATERIAL,

        /** Available AE energy limits the batch. */
        ENERGY,

        /** No constraint below the requested task count was encountered. */
        REQUESTED
    }

    /**
     * Simulates AE power extraction for a proposed dispatch without changing the energy grid.
     */
    @FunctionalInterface
    interface EnergySimulation {

        /**
         * Returns the power AE would provide for {@code requestedPower} in SIMULATE mode.
         *
         * @param requestedPower exact power required by the proposed expanded input list
         * @return simulated extractable power
         */
        double extract(double requestedPower);
    }

    /**
     * Mutable view of one contiguous diagnostics-session count stored by AE's task progress.
     *
     * @param <S> diagnostics session identifier type
     */
    interface SessionSegment<S> {

        /**
         * Returns the session that owns this contiguous craft-count segment.
         *
         * @return owning diagnostics session
         */
        S getSessionId();

        /**
         * Returns how many crafts remain in this segment.
         *
         * @return positive remaining craft count
         */
        long getRemaining();

        /**
         * Updates the segment after a bulk prefix has been consumed.
         *
         * @param remaining positive unconsumed craft count
         */
        void setRemaining(long remaining);
    }

    /**
     * Immutable result of planning one dispatch.
     */
    final class BatchPlan {

        /**
         * Returns the exact multiplier shared by all dispatch stages.
         *
         * <p>
         * Value: positive craft count
         */
        @Getter
        private final long crafts;

        /**
         * Returns the constraint that selected {@link #getCrafts()}.
         *
         * <p>
         * Value: limiting constraint
         */
        @Getter
        private final LimitingFactor limitingFactor;

        public BatchPlan(long crafts, LimitingFactor limitingFactor) {
            if (crafts < 1) throw new IllegalArgumentException("A crafting batch must contain at least one craft");
            if (limitingFactor == null) throw new IllegalArgumentException("A crafting batch needs a limiting factor");
            this.crafts = crafts;
            this.limitingFactor = limitingFactor;
        }

        /**
         * Reports whether the dispatch must use the batching path.
         *
         * @return {@code true} only for two or more crafts
         */
        public boolean isBatched() {
            return crafts >= 2;
        }
    }

    /**
     * Describes one expanded input slot for the core quantity planner.
     */
    final class BatchRequirement {

        /**
         * Returns the identity used to combine duplicate expanded slots.
         *
         * <p>
         * Value: stable material key
         */
        @Getter
        private final Object materialKey;

        /**
         * Returns this slot's one-craft quantity.
         *
         * <p>
         * Value: positive required amount
         */
        @Getter
        private final long amountPerCraft;

        /**
         * Returns the precise CPU inventory amount for this material.
         *
         * <p>
         * Value: non-negative available amount
         */
        @Getter
        private final long availableAmount;

        /**
         * Returns AE's amount represented by one unit of crafting energy.
         *
         * <p>
         * Value: finite positive conversion value
         */
        @Getter
        private final double amountPerEnergyUnit;

        public BatchRequirement(Object materialKey, long amountPerCraft, long availableAmount,
            double amountPerEnergyUnit) {
            if (materialKey == null) throw new IllegalArgumentException("A batch requirement needs a material key");
            if (amountPerCraft <= 0) throw new IllegalArgumentException("Required material amount must be positive");
            if (availableAmount < 0) throw new IllegalArgumentException("Available material amount cannot be negative");
            if (!Double.isFinite(amountPerEnergyUnit) || amountPerEnergyUnit <= 0) {
                throw new IllegalArgumentException("Amount per energy unit must be finite and positive");
            }
            this.materialKey = materialKey;
            this.amountPerCraft = amountPerCraft;
            this.availableAmount = availableAmount;
            this.amountPerEnergyUnit = amountPerEnergyUnit;
        }

    }

    /**
     * Immutable state to apply immediately after a medium accepts a pattern and before AE performs native decrements.
     */
    final class CommitResult {

        /**
         * Reports whether state changes must be applied.
         *
         * <p>
         * Value: {@code true} only after an accepted batched push
         */
        @Getter
        private final boolean committed;

        /**
         * Returns the task value that leaves one decrement to AE.
         *
         * <p>
         * Value: task value before AE's native decrement
         */
        @Getter
        private final long taskValueBeforeNativeDecrement;

        /**
         * Returns the operation budget that leaves one decrement to AE without narrowing a long multiplier to int.
         *
         * <p>
         * Value: operation budget before AE's native decrement
         */
        @Getter
        private final int remainingOperationsBeforeNativeDecrement;

        public CommitResult(boolean committed, long taskValueBeforeNativeDecrement,
            int remainingOperationsBeforeNativeDecrement) {
            this.committed = committed;
            this.taskValueBeforeNativeDecrement = taskValueBeforeNativeDecrement;
            this.remainingOperationsBeforeNativeDecrement = remainingOperationsBeforeNativeDecrement;
        }

    }

    /**
     * Immutable diagnostics allocation for one session represented in a committed batch.
     *
     * @param <S> diagnostics session identifier type
     */
    final class SessionAllocation<S> {

        /**
         * Returns the diagnostics session receiving this allocation.
         *
         * <p>
         * Value: diagnostics session identifier
         */
        @Getter
        private final S sessionId;

        /**
         * Returns the number of crafts represented for this session.
         *
         * <p>
         * Value: positive craft count
         */
        @Getter
        private final long crafts;

        public SessionAllocation(S sessionId, long crafts) {
            if (sessionId == null) throw new IllegalArgumentException("A session allocation needs a session id");
            if (crafts < 1) throw new IllegalArgumentException("A session allocation must contain crafts");
            this.sessionId = sessionId;
            this.crafts = crafts;
        }

    }

    /**
     * Result of consuming a batch-sized prefix from AE diagnostics session segments.
     *
     * @param <S> diagnostics session identifier type
     */
    final class SessionConsumption<S> {

        /**
         * Returns ordered per-session craft allocations.
         *
         * <p>
         * Value: immutable allocation list
         */
        @Getter
        private final List<SessionAllocation<S>> allocations;

        /**
         * Returns how many crafts had diagnostics session metadata.
         *
         * <p>
         * Value: consumed session-tagged craft count
         */
        @Getter
        private final long consumedCrafts;

        public SessionConsumption(List<SessionAllocation<S>> allocations, long consumedCrafts) {
            this.allocations = allocations;
            this.consumedCrafts = consumedCrafts;
        }

        /**
         * Returns the first valid session, matching AE's single-craft method contract.
         *
         * @return first session or {@code null} when no session segment exists
         */
        public S getFirstSessionId() {
            return allocations.isEmpty() ? null
                : allocations.get(0)
                    .getSessionId();
        }
    }

    /**
     * Resolves the all-or-nothing medium policy for one pattern.
     *
     * @param compatibleMedia one compatibility result for every medium registered for the pattern
     * @return batching only when the list is non-empty and every entry is compatible
     */
    MediumStrategy resolveMediumStrategy(List<Boolean> compatibleMedia);

    /**
     * Computes the exact dispatch multiplier from task, arithmetic, material, and energy constraints.
     *
     * @param requestedCrafts  remaining task count
     * @param mediumStrategy   all-medium compatibility decision
     * @param expandedInputs   AE's unmodified expanded slot input list for one craft
     * @param condensedOutputs AE's unmodified condensed output array for one craft
     * @param inventory        actual CPU crafting inventory used by the subsequent extraction
     * @param energySimulation SIMULATE-only energy query for proposed expanded quantities
     * @return exact plan; values below two select the fully native path
     */
    BatchPlan plan(long requestedCrafts, MediumStrategy mediumStrategy, List<IAEStack<?>> expandedInputs,
        IAEStack<?>[] condensedOutputs, MECraftingInventory inventory, EnergySimulation energySimulation);

    /**
     * Runs the core batch calculation from explicit quantities. The AE-facing overload converts real stacks and its
     * real CPU inventory to this model, while logic tests can exercise the same algorithm without starting FML.
     *
     * @param requestedCrafts  remaining task count
     * @param mediumStrategy   all-medium compatibility decision
     * @param requirements     expanded input slots, including duplicates
     * @param outputAmounts    positive one-craft quantities for every condensed output
     * @param energySimulation SIMULATE-only energy query
     * @return exact plan; values below two select the native path
     */
    BatchPlan planRequirements(long requestedCrafts, MediumStrategy mediumStrategy, List<BatchRequirement> requirements,
        long[] outputAmounts, EnergySimulation energySimulation);

    /**
     * Returns a slot-preserving, checked-multiply copy of an expanded input list.
     *
     * @param inputs original AE expanded input list
     * @param crafts exact planned multiplier
     * @return independent list whose non-null stacks are independent copies
     * @throws ArithmeticException if the supplied multiplier violates the prior plan
     */
    List<IAEStack<?>> scaleInputs(List<IAEStack<?>> inputs, long crafts);

    /**
     * Returns a checked-multiply copy of every condensed output.
     *
     * @param outputs original AE condensed output array
     * @param crafts  exact committed multiplier
     * @return independent output array and stack copies
     * @throws ArithmeticException if the supplied multiplier violates the prior plan
     */
    IAEStack<?>[] scaleOutputs(IAEStack<?>[] outputs, long crafts);

    /**
     * Performs exact positive long multiplication shared by stack and diagnostics quantities.
     *
     * @param amount one-craft amount
     * @param crafts craft multiplier
     * @return exact product
     * @throws ArithmeticException when the product cannot be represented as a positive long
     */
    long checkedMultiply(long amount, long crafts);

    /**
     * Calculates state to apply at the pushPattern commit boundary while leaving AE's native decrement intact.
     *
     * @param plan                dispatch plan
     * @param pushAccepted        whether the medium accepted the pattern
     * @param taskValue           current task progress value
     * @param remainingOperations current int operation budget
     * @return unchanged state for rejected/native pushes, or pre-decrement state for an accepted batch
     */
    CommitResult commit(BatchPlan plan, boolean pushAccepted, long taskValue, int remainingOperations);

    /**
     * Consumes a batch-sized prefix from diagnostics session segments in O(number of crossed segments).
     *
     * @param segments mutable ordered AE session-count segments
     * @param crafts   committed batch size
     * @param <S>      diagnostics session identifier type
     * @return ordered allocations and the total number of session-tagged crafts consumed
     */
    <S> SessionConsumption<S> consumeSessions(Deque<SessionSegment<S>> segments, long crafts);
}
