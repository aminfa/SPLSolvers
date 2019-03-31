package de.upb.spl.hasco;

import de.upb.spl.ailibsintegration.FeatureSelectionOrdering;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.util.DefaultMap;

public class CMUtil {

    private final static DefaultMap<FM2CM, ComponentModelCache> cache = new DefaultMap<FM2CM, ComponentModelCache>(ComponentModelCache::new);

    @Deprecated
    public static FeatureSelectionOrdering getBestPerformance(BenchmarkEnvironment env) {
        return cache.get(env.componentModel()).bestPerformance(env.objectives().size());
    }

    private static class ComponentModelCache {
        FeatureSelectionOrdering bestPerformance;
        ComponentModelCache(final FM2CM cm){

        }

        public FeatureSelectionOrdering bestPerformance(int objectiveCount) {
            if(bestPerformance == null)
                bestPerformance = new FeatureSelectionOrdering(new double[objectiveCount + 1]);
            return bestPerformance;
        }
    }

}
