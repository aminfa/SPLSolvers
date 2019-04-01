package de.upb.spl.finish;

import de.upb.spl.ailibsintegration.ParetoDominanceOrdering;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.BenchmarkHelper;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
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

        Set<BenchmarkEntry> solutionSet = new HashSet<>();
        DefaultMap<BenchmarkEntry, ParetoDominanceOrdering> performanceCache = new DefaultMap<BenchmarkEntry, ParetoDominanceOrdering>(
                entry  -> {
                    double[] objectiveValues = BenchmarkHelper.extractEvaluation(
                            env(),
                            entry.report());
                    return new ParetoDominanceOrdering(
                            0, // solutions dont have violated constraints.
                            objectiveValues);
                }
        );

        Comparator<BenchmarkEntry> entryComparator = Comparator.comparing(performanceCache::get);
        while(solutionSet.size() < solutionCount) {
            List<BenchmarkEntry> paretoSet = new ArrayList<>();
            StreamSupport
                    .stream(allSolutions.spliterator(), false)
                    .filter(((Predicate<BenchmarkEntry>) solutionSet::contains).negate())
                    .filter(entry -> !performanceCache.get(entry).hasEmptyResults())
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
            if(paretoSet.isEmpty()) {
                break;
            }
            solutionSet.addAll(paretoSet);
            solutions.addAll(paretoSet);
        }
        logger.info("Selected a solution list with size: {}.", solutionSet.size());

        solutionPerformanceJSON = new StringBuilder()
                .append("{\n\t")
                .append(
                        solutions.stream()
                        .map(performanceCache::get)
                        .map(Object::toString)
                        .collect(Collectors.joining(",\n\t")))
                .append("\n}").toString();


        logger.debug("Solutions: {}",  getSolutionPerformanceJSON());
    }

    public String getSolutionPerformanceJSON() {
        return solutionPerformanceJSON;
    }


}
