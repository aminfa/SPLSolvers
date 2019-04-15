package de.upb.spl.jumpstarter;

import de.upb.spl.benchmarks.BenchmarkReplay;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.benchmarks.x264.VideoEncoderBaseInterpreter;
import de.upb.spl.finish.NBestSolutions;
import de.upb.spl.finish.ReasonerSolutionContribution;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.sayyad.Sayyad;
import de.upb.spl.util.Iterators;
import jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import jaicore.basic.algorithm.exceptions.AlgorithmException;

import java.util.*;
import java.util.stream.Collectors;

import static de.upb.spl.benchmarks.x264.VideoEncoderPreferences.*;

public class EvaluateInformedX264 {

    protected BenchmarkEnvironment base;

    protected BookkeeperEnv books;

    protected BenchmarkEnvironment interpreters;

    protected List<BenchmarkReplay> replayers;

    List<NBestSolutions> solutions = new ArrayList<>();

    protected Map<Integer, Benchmark> benchmarks = new HashMap<>();

    protected  List<String> reasoners = Arrays.asList(
            HASCOSPLReasoner.NAME
            , Guo11.NAME
            , Hierons.NAME
            , Sayyad.NAME
            , Henard.NAME
            , "random-sat"
    );


    public void start() {
        base = new VideoEncoderBaseInterpreter();
        base.configuration().setProperty("de.upb.spl.SPLReasoner.evaluations", "60");
        base.configuration().setProperty("de.upb.spl.benchmark.videoEncoding.RAWSourceFile", "touchdown_pass");
        base.configuration().setProperty("de.upb.spl.eval.solutionCount", "50");

        interpreters = new SizeThreshold(
                new RuntimeThreshold(base, 0.8),
                0.0025
        );

        books = new BookkeeperEnv(base);

        List<ReasonerReplayer> benchmarkReplays =
                ReasonerReplayer.loadReplays("replays/x264/informed/size-0.01_runtime-1.5", true);

        replayers = benchmarkReplays.stream()
                .map(BenchmarkReplay::new)
                .filter(replayer -> reasoners.contains(replayer.getReasonerName()))
                .collect(Collectors.toList());

        if(replayers.size()==0) {
            System.err.println("No replayers..");
            return;
        }

        replayers.forEach(replayer -> {
            Benchmark benchmark = benchmarks.computeIfAbsent(replayer.getBenchmarkIndex(), index ->
                    new Benchmark(
                            books.billedEnvironment(books, "" + index),
                            index));
            benchmark.replays.add(replayer);
        });


        /*
         * rerun all benchmarks
         */
        benchmarks.values().parallelStream().forEach(benchmark -> {
            replayers.parallelStream().forEach(replayer -> {
                try {
                    replayer.getReplayer()
                            .algorithm(benchmark.bookkeeper.billedEnvironment(benchmark.bookkeeper, replayer.getReasonerName()))
                            .call();
                } catch (InterruptedException | AlgorithmExecutionCanceledException | AlgorithmException e) {
                    e.printStackTrace();
                }
            });

        });
        benchmarks.values().forEach(benchmark -> {
            System.out.println("Starting interpreter: " + interpreters.toString() + " for benchmark " + benchmark.index);
            BookkeeperEnv books = new BookkeeperEnv(interpreters, benchmark.bookkeeper.currentTab());
            NBestSolutions bestSolutions = new NBestSolutions(books);
            bestSolutions.run();
            while (solutions.size() <= benchmark.index) {
                solutions.add(null);
            }
            solutions.set(benchmark.index, bestSolutions);
        });
        Map<String, List<Double>> reasonerContributions = new HashMap<>();
        for (int i = 0; i < solutions.size(); i++) {
            NBestSolutions nBestSolutions = solutions.get(i);
            Benchmark benchmark = benchmarks.get(i);
            if(benchmark == null) {
                System.err.println("The " + Iterators.ordinal(i) + " benchmark doesn't exist.");
                return;
            }
            for(BenchmarkReplay replay : replayers) {
                ReasonerSolutionContribution contributions = new ReasonerSolutionContribution(benchmark.bookkeeper, nBestSolutions);
                contributions.run();
                for(String reasoner : reasoners) {
                    Double reasonerContrib = contributions.getContributions().get(reasoner);
                    if(reasonerContrib == null) {
                        System.err.println("No Contribution for reasoner " + reasoner + " was calculated.");
                    }
                    reasonerContributions
                            .computeIfAbsent(reasoner, r -> new ArrayList<>())
                            .add(reasonerContrib);
                }
            }
        }
        reasonerContributions.entrySet().stream().forEach(
                (entry) -> {
                    OptionalDouble average = entry.getValue()
                            .stream()
                            .mapToDouble(contrib -> contrib)
                            .average();
                    System.out.println(entry.getKey() + ": " + average.orElse(0));
                }
        );
    }

    public static void main(String... args ){
        new EvaluateInformedX264().start();;
    }

    static class Benchmark {
        int index;
        final BookkeeperEnv bookkeeper;
        List<BenchmarkReplay> replays = new ArrayList<>();
        Benchmark(BenchmarkEnvironment env, int index) {
            bookkeeper = new BookkeeperEnv(env);
            this.index = index;
        }
    }
}
