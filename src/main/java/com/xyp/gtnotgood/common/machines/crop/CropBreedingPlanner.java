package com.xyp.gtnotgood.common.machines.crop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.ICropMutation;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.gtnewhorizon.cropsnh.farming.registries.MutationRegistry;

/**
 * Plans a deterministic CropsNH mutation path from archived crops to a target crop.
 */
public final class CropBreedingPlanner {

    private CropBreedingPlanner() {}

    public static Plan plan(String targetCropId, Set<String> archivedCrops) {
        if (targetCropId == null || targetCropId.isEmpty()) return Plan.empty("");

        Set<String> archived = archivedCrops == null ? Collections.emptySet() : new HashSet<>(archivedCrops);
        Candidate candidate = resolve(targetCropId, archived, new HashSet<>(), new HashMap<>());
        if (candidate == null) {
            return new Plan(targetCropId, Collections.emptyList(), Collections.singleton(targetCropId));
        }
        return new Plan(targetCropId, new ArrayList<>(candidate.stepsByResult.values()), candidate.missingCrops);
    }

    @Nullable
    private static Candidate resolve(String cropId, Set<String> archived, Set<String> visiting,
        Map<String, Candidate> memo) {
        if (archived.contains(cropId)) return Candidate.empty();

        Candidate cached = memo.get(cropId);
        if (cached != null) return cached;
        if (!visiting.add(cropId)) return null;

        ICropCard crop = CropRegistry.instance.get(cropId);
        Collection<ICropMutation> mutations = crop == null ? null
            : MutationRegistry.instance.getDeterministicMutationsForCrop(crop);
        Candidate best = null;
        if (mutations != null) {
            List<ICropMutation> sorted = new ArrayList<>(mutations);
            sorted.sort(
                java.util.Comparator.comparingInt(ICropMutation::getParentCount)
                    .thenComparing(CropBreedingPlanner::mutationSignature));
            for (ICropMutation mutation : sorted) {
                Candidate candidate = resolveMutation(cropId, mutation, archived, visiting, memo);
                if (candidate != null && (best == null || compare(candidate, best) < 0)) {
                    best = candidate;
                }
            }
        }

        visiting.remove(cropId);
        if (best == null) best = Candidate.missing(cropId);
        memo.put(cropId, best);
        return best;
    }

    @Nullable
    private static Candidate resolveMutation(String resultId, ICropMutation mutation, Set<String> archived,
        Set<String> visiting, Map<String, Candidate> memo) {
        Candidate combined = Candidate.empty();
        for (ICropCard parent : mutation.getParents()) {
            if (parent == null || parent.getId() == null
                || parent.getId()
                    .isEmpty()
                || parent.getId()
                    .equals(resultId)) {
                return null;
            }
            Candidate parentCandidate = resolve(parent.getId(), archived, visiting, memo);
            if (parentCandidate == null) return null;
            combined = Candidate.combine(combined, parentCandidate);
        }
        return Candidate.withStep(combined, new Step(mutation));
    }

    private static int compare(Candidate left, Candidate right) {
        int missing = Integer.compare(left.missingCrops.size(), right.missingCrops.size());
        if (missing != 0) return missing;

        int steps = Integer.compare(left.stepsByResult.size(), right.stepsByResult.size());
        if (steps != 0) return steps;

        int cost = Long.compare(left.recipeCost(), right.recipeCost());
        if (cost != 0) return cost;

        return left.signature()
            .compareTo(right.signature());
    }

    private static String mutationSignature(ICropMutation mutation) {
        return mutation.getParents()
            .stream()
            .map(parent -> parent == null ? "" : parent.getId())
            .sorted()
            .collect(Collectors.joining("+")) + ">"
            + mutation.getOutput()
                .getId();
    }

    private static final class Candidate {

        private final LinkedHashMap<String, Step> stepsByResult;
        private final LinkedHashSet<String> missingCrops;

        private Candidate(LinkedHashMap<String, Step> stepsByResult, LinkedHashSet<String> missingCrops) {
            this.stepsByResult = stepsByResult;
            this.missingCrops = missingCrops;
        }

        private static Candidate empty() {
            return new Candidate(new LinkedHashMap<>(), new LinkedHashSet<>());
        }

        private static Candidate missing(String cropId) {
            LinkedHashSet<String> missing = new LinkedHashSet<>();
            missing.add(cropId);
            return new Candidate(new LinkedHashMap<>(), missing);
        }

        private static Candidate combine(Candidate left, Candidate right) {
            LinkedHashMap<String, Step> steps = new LinkedHashMap<>(left.stepsByResult);
            steps.putAll(right.stepsByResult);
            LinkedHashSet<String> missing = new LinkedHashSet<>(left.missingCrops);
            missing.addAll(right.missingCrops);
            return new Candidate(steps, missing);
        }

        private static Candidate withStep(Candidate base, Step step) {
            LinkedHashMap<String, Step> steps = new LinkedHashMap<>(base.stepsByResult);
            steps.put(step.result, step);
            return new Candidate(steps, new LinkedHashSet<>(base.missingCrops));
        }

        private long recipeCost() {
            long cost = 0;
            for (Step step : stepsByResult.values()) {
                cost += Math.max(1, step.eut) * (long) Math.max(1, step.duration);
            }
            return cost;
        }

        private String signature() {
            StringBuilder signature = new StringBuilder();
            for (Step step : stepsByResult.values()) {
                signature.append(step.signature())
                    .append(';');
            }
            return signature.toString();
        }
    }

    /**
     * One deterministic CropsNH mutation that the machine can perform after all parent crop ids are archived.
     */
    public static final class Step {

        private final ICropMutation mutation;
        public final List<String> parents;
        public final String result;
        public final int duration;
        public final int eut;

        private Step(ICropMutation mutation) {
            this.mutation = mutation;
            this.parents = mutation.getParents()
                .stream()
                .map(ICropCard::getId)
                .collect(Collectors.toList());
            this.result = mutation.getOutput()
                .getId();
            this.duration = Math.max(1, mutation.getBreedingMachineRecipeDuration());
            this.eut = Math.max(1, mutation.getBreedingMachineRecipeEUt());
        }

        public ICropMutation getMutation() {
            return mutation;
        }

        public String signature() {
            return parents.stream()
                .sorted()
                .collect(Collectors.joining("+")) + ">"
                + result;
        }
    }

    /**
     * Immutable deterministic mutation plan from the current crop archive to a requested target crop id.
     */
    public static final class Plan {

        private final String targetCropId;
        private final List<Step> steps;
        private final Set<String> missingCrops;

        private Plan(String targetCropId, List<Step> steps, Set<String> missingCrops) {
            this.targetCropId = targetCropId;
            this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
            this.missingCrops = Collections.unmodifiableSet(new LinkedHashSet<>(missingCrops));
        }

        public static Plan empty(String targetCropId) {
            return new Plan(targetCropId, Collections.emptyList(), Collections.emptySet());
        }

        public String getTargetCropId() {
            return targetCropId;
        }

        public List<Step> getSteps() {
            return steps;
        }

        public String getFirstMissingCrop() {
            return missingCrops.isEmpty() ? ""
                : missingCrops.iterator()
                    .next();
        }

        public List<Step> getReadySteps(Set<String> archivedCrops, int limit) {
            if (limit <= 0) return Collections.emptyList();
            Set<String> archived = archivedCrops == null ? Collections.emptySet() : archivedCrops;
            List<Step> ready = new ArrayList<>();
            for (Step step : steps) {
                if (archived.contains(step.result)) continue;
                if (archived.containsAll(step.parents)) {
                    ready.add(step);
                    if (ready.size() >= limit) break;
                }
            }
            return ready;
        }
    }
}
