package de.upb.spl.finish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.util.Cache;
import jaicore.basic.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ReasonerSolutionContribution extends Finisher {

    private final static Logger logger = LoggerFactory.getLogger(ReasonerSolutionContribution.class);

    private Cache<List<BenchmarkEntry>> solutions;

    private Map<String, Double> percentageContribution;

    public ReasonerSolutionContribution(BenchmarkEnvironment env,
                                        List<BenchmarkEntry> solutionList) {
        super(env);
        solutions = Cache.of(solutionList);
    }

    public ReasonerSolutionContribution(BenchmarkEnvironment env,
                                        final NBestSolutions nBestSolutions) {
        super(env);
        this.solutions = new Cache<>(
                () -> Objects.requireNonNull(nBestSolutions.getSolutions()));
    }

    public BookkeeperEnv env() {
        return super.env().getDecoration(BookkeeperEnv.class);
    }

    public Map<String, Double> getContributions() {
        return percentageContribution;
    }

    public String dumpContributions() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(getContributions());
    }

    @Override
    public void run() {
        if(percentageContribution != null) {
            logger.warn("Already calculated solution contribution.");
            return;
        }
        Map<String, Double> percentageContribution = new HashMap<>();
        if(solutions.get().size() == 0) {
            logger.warn("Solution set it empty. cannot compute contributions.");
        }
        for(BenchmarkBill bill : env().bills()) {
            String reasoner = bill.getReasonerName();
            if(solutions.get().size() == 0) {
                percentageContribution.put(reasoner, 0.);
                continue;
            }
            double members = 0.;
            for(BenchmarkEntry entry : bill) {
                if(solutions.get().contains(entry)) {
                    members++;
                }
            }
            percentageContribution.put(reasoner, members / solutions.get().size());
        }
        this.percentageContribution = new LinkedHashMap<>();
        percentageContribution.entrySet().stream()
                .sorted(
                        Comparator.comparingDouble(entry -> - entry.getValue()))
                .forEach(entry -> this.percentageContribution.put(entry.getKey(), entry.getValue()));

        System.out.println("Contributions:\n" + dumpContributions());
    }
}
