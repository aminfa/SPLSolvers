package de.upb.spl.finish;

import de.upb.spl.ailibsintegration.ParetoPerformance;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.BenchmarkHelper;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.util.DefaultMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;
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

        DefaultMap<BenchmarkEntry, ParetoPerformance> performanceCache = new DefaultMap<BenchmarkEntry, ParetoPerformance>(
                entry  -> {
                    double[] objectiveValues = BenchmarkHelper.extractEvaluation(
                            env(),
                            entry.report());
                    return new ParetoPerformance(
                            0, // solutions dont have violated constraints.
                            objectiveValues);
                }
        );

        Comparator<BenchmarkEntry> entryComparator = Comparator.comparing(performanceCache::get);
        PriorityQueue<BenchmarkEntry> heap = new PriorityQueue<>(entryComparator);
        StreamSupport
                .stream(allSolutions.spliterator(), false)
                .forEach(heap::add);

        BenchmarkEntry lastSolution = heap.poll();
        if(lastSolution == null) {
            return;
        }
        solutions.add(lastSolution);
        while(true) {
            BenchmarkEntry currentSolution = heap.poll();
            if(currentSolution == null) {
                break;
            }
            if(solutions.size() >= solutionCount && entryComparator.compare(lastSolution, currentSolution) != 0) {
                break;
            }
            solutions.add(currentSolution);
            lastSolution = currentSolution;
        }
        logger.info("Select a solution list with size: {}.", solutions.size());

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
