package de.upb.spl.finish;

import de.upb.spl.ailibsintegration.ParetoDominanceOrdering;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.BenchmarkHelper;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.util.DefaultMap;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public abstract class Finisher implements Runnable {

    private BenchmarkEnvironment env;

    public Finisher(BenchmarkEnvironment env) {
        this.env = env;
    }

    public BenchmarkEnvironment env() {
        return env;
    }

    public void setEnv(BenchmarkEnvironment env) {
        this.env = env;
    }


    public static DefaultMap<BenchmarkEntry, ParetoDominanceOrdering> performanceCache(BenchmarkEnvironment env) {
        return new DefaultMap<BenchmarkEntry, ParetoDominanceOrdering>(
                entry  -> {
                    double[] objectiveValues = BenchmarkHelper.extractEvaluation(
                            env,
                            entry.report());
                    return new ParetoDominanceOrdering(
                            0, // solutions dont have violated constraints.
                            objectiveValues);
                }
        );
    }

    public static Iterable<Set<BenchmarkEntry>> paretoLayers(BenchmarkEnvironment env, DefaultMap<BenchmarkEntry, ParetoDominanceOrdering> cache,
                                                       Iterable<BenchmarkEntry> population) {
        final Comparator<BenchmarkEntry> entryComparator = Comparator.comparing(cache::get);
        return () -> new Iterator<Set<BenchmarkEntry>>() {

            Set<BenchmarkEntry> solutionSet = new HashSet<>();

            Set<BenchmarkEntry> nextFront = stripNextFront();

            Set<BenchmarkEntry> stripNextFront() {
                Set<BenchmarkEntry> paretoSet = new HashSet<>();
                StreamSupport
                        .stream(population.spliterator(), false)
                        .filter(((Predicate<BenchmarkEntry>) solutionSet::contains).negate())
                        .filter(entry -> !cache.get(entry).hasEmptyResults())
                        .filter(entry -> !env.interpreter(entry.report()).violatedConstraints())
                        .forEach((newCandidate) -> {
                            Iterator<BenchmarkEntry> it = paretoSet.iterator();
                            boolean superiorFound = false;
                            while (it.hasNext()) {
                                BenchmarkEntry candidate = it.next();
                                int comparison = entryComparator.compare(newCandidate, candidate);
                                if (comparison > 0) {
                                    superiorFound = true;
                                    break;
                                } else if (comparison < 0) {
                                    it.remove();
                                }
                            }
                            if (!superiorFound) {
                                paretoSet.add(newCandidate);
                            }
                        });
                solutionSet.addAll(paretoSet);
                return paretoSet;
            }

            @Override
            public boolean hasNext() {
                return nextFront != null && !nextFront.isEmpty();
            }

            @Override
            public Set<BenchmarkEntry> next() {
                Set<BenchmarkEntry> lastFront = nextFront;
                nextFront = stripNextFront();
                return lastFront;
            }
        };
    }
}
