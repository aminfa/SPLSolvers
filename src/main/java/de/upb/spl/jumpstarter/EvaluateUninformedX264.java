package de.upb.spl.jumpstarter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
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
import de.upb.spl.benchmarks.BenchmarkReplay;
import de.upb.spl.sayyad.Sayyad;
import de.upb.spl.util.Iterators;
import jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import jaicore.basic.algorithm.exceptions.AlgorithmException;

import java.util.*;
import java.util.stream.Collectors;
import static de.upb.spl.benchmarks.x264.VideoEncoderPreferences.*;

public class EvaluateUninformedX264 {

    protected BenchmarkEnvironment base;

    protected BookkeeperEnv books;

    protected List<BenchmarkEnvironment> interpreters;

    protected List<BenchmarkReplay> replayers;

    Map<BenchmarkEnvironment, List<NBestSolutions>> solutions = new HashMap<>();

    protected Map<Integer, Benchmark> benchmarks = new HashMap<>();

    protected  List<String> reasoners = Arrays.asList(
            HASCOSPLReasoner.NAME
            , Guo11.NAME ,
            Hierons.NAME
            , Sayyad.NAME
            , Henard.NAME);

    private final double[] qualityThresholdDeltas = {0., -15., -30., -45., -60};

    private final double[] sizeThresholds = {0.05, 0.04, 0.02, 0.015, 0.0075};

    private final double[] runtimeThresholds = {1., 2., 3., 6., 8.};


    private void createInterpreters() {
        interpreters = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                QualityThreshold quality = new QualityThreshold(base, qualityThresholdDeltas[i]);
                SizeThreshold size = new SizeThreshold(base, sizeThresholds[i]);

                interpreters.add(new SizeThreshold(quality, sizeThresholds[j]));
                interpreters.add(new RuntimeThreshold(quality, runtimeThresholds[j]));

                interpreters.add(new RuntimeThreshold(size, runtimeThresholds[j]));
            }
        }
    }

    public void start() {

        base = new VideoEncoderBaseInterpreter();
        base.configuration().setProperty("de.upb.spl.SPLReasoner.evaluations", "20");
        base.configuration().setProperty("de.upb.spl.benchmark.videoEncoding.RAWSourceFile", "ducks_take_off");
        base.configuration().setProperty("de.upb.spl.eval.solutionCount", "10");

        createInterpreters();

        books = new BookkeeperEnv(base);

        List<ReasonerReplayer> benchmarkReplays =
                ReasonerReplayer.loadReplays("replays/x264/uninformed/ducks_take_off", true);

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
            interpreters.forEach(interpreter -> {
                System.out.println("Starting interpreter: " + interpreter.toString());
                BookkeeperEnv books = new BookkeeperEnv(interpreter, benchmark.bookkeeper.currentTab());
                NBestSolutions bestSolutions = new NBestSolutions(books);
                bestSolutions.run();
                List<NBestSolutions> bestSolutionsList = solutions
                        .computeIfAbsent(interpreter, (i) -> new ArrayList<>());
                while (bestSolutionsList.size() <= benchmark.index) {
                    bestSolutionsList.add(null);
                }
                bestSolutionsList.set(benchmark.index, bestSolutions);
            });
        });
        Map<String, List<Double>> reasonerContributions = new HashMap<>();
        solutions.keySet().forEach(interpreter -> {
            List<NBestSolutions> solutionsList = solutions.get(interpreter);
            for (int i = 0; i < solutionsList.size(); i++) {
                NBestSolutions nBestSolutions = solutionsList.get(i);
                Benchmark benchmark = benchmarks.get(i);
                if(benchmark == null) {
                    System.err.println("The " + Iterators.ordinal(i) + " benchmark doesn't exist.");
                    continue;
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
        });
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
        new EvaluateUninformedX264().start();;
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
