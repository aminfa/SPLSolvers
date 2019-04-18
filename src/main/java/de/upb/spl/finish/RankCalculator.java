package de.upb.spl.finish;

import de.upb.spl.ailibsintegration.ParetoDominanceOrdering;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.util.DefaultMap;

import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.StreamSupport;

public class RankCalculator extends Finisher {

    double averageRank = -1;
    int start, limit;
    List<Set<BenchmarkEntry>> paretoLayers;
    public RankCalculator(BenchmarkEnvironment env, List<Set<BenchmarkEntry>> paretoLayers, int start, int end) {
        super(env);
        this.start = start;
        this.limit = end - start;
        this.paretoLayers = paretoLayers;
    }

    public double getAverageRank() {
        return averageRank;
    }

    public void setAverageRank(double averageRank) {
        this.averageRank = averageRank;
    }

    @Override
    public void run() {
        OptionalDouble average = StreamSupport.stream(env().currentTab().spliterator(), false)
                .skip(start)
                .limit(limit)
                .mapToDouble(entry -> {
                    double rank = 0;
                    for (Set<BenchmarkEntry> paretoFront : paretoLayers) {
                        if (paretoFront.contains(entry)) {
                            break;
                        } else {
                            rank += paretoFront.size();
                        }
                    }
                    return rank / (double) env().currentTab().getEvaluationsCount();
                }).average();
        averageRank = 1. - average.orElse(2.);
    }
}
