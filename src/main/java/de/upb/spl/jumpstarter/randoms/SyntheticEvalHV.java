package de.upb.spl.jumpstarter.randoms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.benchmarks.BenchmarkReplay;
import de.upb.spl.benchmarks.env.*;
import de.upb.spl.finish.HypervolumeCalculator;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.jumpstarter.panels.DeviationPlotter;
import de.upb.spl.reasoner.ReasonerReplayer;
import de.upb.spl.sayyad.Sayyad;
import de.upb.spl.util.FileUtil;
import jaicore.basic.algorithm.AlgorithmExecutionCanceledException;
import jaicore.basic.algorithm.exceptions.AlgorithmException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SyntheticEvalHV {


    final static double[] MIN_REFERENCE = {-1, -1}, MAX_REFERENCE = {1.5, 1};

    protected BenchmarkEnvironment base;

    protected BookkeeperEnv books;

    protected List<BenchmarkReplay> replayers;

    protected Map<Integer, Benchmark> benchmarks = new HashMap<>();

    protected  List<String> reasoners = Arrays.asList(
            HASCOSPLReasoner.NAME
            , Guo11.NAME
            , Hierons.NAME
            , Sayyad.NAME
            , Henard.NAME
            , "random-sat"
    );


    public void eval() {
        base = new SyntheticEnv().createEnv();
        books = new BookkeeperEnv(base);

        List<ReasonerReplayer> benchmarkReplays =
                ReasonerReplayer.loadReplays("replays/synthetic/hv-liftgraph", true);

        replayers = benchmarkReplays.stream()
                .map(BenchmarkReplay::new)
                .filter(replayer -> reasoners.contains(replayer.getReasonerName()))
                .collect(Collectors.toList());

        if (replayers.size() == 0) {
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
            benchmark.replays.parallelStream().forEach(replayer -> {
                try {
                    replayer.getReplayer()
                            .algorithm(benchmark.bookkeeper.billedEnvironment(benchmark.bookkeeper, replayer.getReasonerName()))
                            .call();
                } catch (InterruptedException | AlgorithmExecutionCanceledException | AlgorithmException e) {
                    e.printStackTrace();
                }
            });
        });
        /*
         * Calculate hyper volume for each 10 steps:
         */
        final Map<String, List<Map<Integer, Double>>> hyperVolumeData = new HashMap<>();
        benchmarks.values().forEach(benchmark -> benchmark.replays.forEach(replayer -> {
            BookkeeperEnv measurements = new BookkeeperEnv(base,
                    benchmark.bookkeeper.bill(replayer.getReasonerName()));
            int hvSamples = base.configuration().getHVTimelinesamples();
            int evaluations = base.configuration().getEvaluationPermits();
            double powerStep = (Math.log(evaluations) - Math.log(1.))/ ((double)hvSamples);
            Map<Integer, Double> hvData =
                    Stream.concat(
                        IntStream.range(0, hvSamples)
                            .mapToDouble(i -> Math.pow(Math.E, (i * powerStep)))
                            .filter(Double::isFinite)
                            .mapToInt(i -> (int) Math.floor(i)).boxed(),
                        IntStream.of(evaluations).boxed())
                    .distinct()
                    .collect(Collectors.toMap(
                            i -> i,
                            i -> {
                            HypervolumeCalculator calculator = new HypervolumeCalculator(
                                measurements, i, MIN_REFERENCE, MAX_REFERENCE);
                        calculator.run();
                        return calculator.getResult();}));
            hyperVolumeData.computeIfAbsent(replayer.getReasonerName(), name -> new ArrayList<>())
                    .add(hvData);
        }));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileUtil.writeStringToFile("synthetic-hv-steps.json", gson.toJson(hyperVolumeData));
    }

    public static void main(String... args ){
        new SyntheticEvalHV().eval();
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
