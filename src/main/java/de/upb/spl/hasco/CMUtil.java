package de.upb.spl.hasco;

import de.upb.spl.ailibsintegration.FeatureSelectionPerformance;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.util.DefaultMap;

public class CMUtil {

    private final static DefaultMap<FM2CM, ComponentModelCache> cache = new DefaultMap<FM2CM, ComponentModelCache>(ComponentModelCache::new);

    @Deprecated
    public static FeatureSelectionPerformance getBestPerformance(BenchmarkEnvironment env) {
        return cache.get(env.componentModel()).bestPerformance(env.objectives().size());
    }

    private static class ComponentModelCache {
        FeatureSelectionPerformance bestPerformance;
        ComponentModelCache(final FM2CM cm){

        }

        public FeatureSelectionPerformance bestPerformance(int objectiveCount) {
            if(bestPerformance == null)
                bestPerformance = new FeatureSelectionPerformance(new double[objectiveCount + 1]);
            return bestPerformance;
        }
    }

}
