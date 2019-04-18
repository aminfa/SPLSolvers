package de.upb.spl.finish;

import de.upb.spl.ailibsintegration.ParetoDominanceOrdering;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.jumpstarter.Finish;
import de.upb.spl.util.DefaultMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class NBestSolutions extends Finisher {

    private final static Logger logger = LoggerFactory.getLogger(NBestSolutions.class);

    private List<BenchmarkEntry> solutions;

    private String solutionPerformanceJSON;

    public NBestSolutions(BenchmarkEnvironment env) {
        super(env);
    }

    public List<BenchmarkEntry> getSolutions() {
        if(solutions == null) {
            logger.error("Solutions were accessed before the finisher has been executed.");
            run();
        }
        return solutions;
    }

    @Override
    public void run() {
        if(solutions != null) {
            logger.warn("Solutions were already calculated");
            return;
        }
        solutions = new ArrayList<>();
        BenchmarkBill allSolutions = env().currentTab();
        int solutionCount = env().configuration().getEvalSolutionCount();
        int candidateCount = allSolutions.getEvaluationsCount();
        logger.info("Select {} best solutions from {} many candidates.", solutionCount, candidateCount);
        if(solutionCount >= candidateCount) {
            for(BenchmarkEntry entry : allSolutions){
                solutions.add(entry);
            }
            return;
        }

        DefaultMap<BenchmarkEntry, ParetoDominanceOrdering> performanceCache = Finisher.performanceCache(env());
        while(solutions.size() < solutionCount) {
            for(Set<BenchmarkEntry> paretoLayer : paretoLayers(env(), performanceCache, allSolutions)) {
                solutions.addAll(paretoLayer);
            }
        }
        logger.info("Selected a solution list with size: {}.", solutions.size());

        solutionPerformanceJSON = new StringBuilder()
                .append("{\n\t")
                .append(
                        solutions.stream()
                        .map(performanceCache::get)
                        .map(Object::toString)
                        .collect(Collectors.joining(",\n\t")))
                .append("\n}").toString();


        logger.debug("Solutions for {}: {}",  env().objectives(), getSolutionPerformanceJSON());
    }

    public String getSolutionPerformanceJSON() {
        return solutionPerformanceJSON;
    }

}
