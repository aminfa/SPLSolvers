package de.upb.spl.jumpstarter.randoms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.BenchmarkReplay;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.benchmarks.env.ConfiguredEnv;
import de.upb.spl.benchmarks.x264.VideoEncoderBaseInterpreter;
import de.upb.spl.benchmarks.x264.VideoEncoderBlackBox;
import de.upb.spl.benchmarks.x264.VideoEncoderPreferences;
import de.upb.spl.finish.Finisher;
import de.upb.spl.finish.HypervolumeCalculator;
import de.upb.spl.finish.MinMaxValues;
import de.upb.spl.finish.RankCalculator;
import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.jumpstarter.EvaluationAnalysis;
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

public class VEEval {

    double[] min = {0,0}, max = {0,0};

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
        base = new VideoEncoderBaseInterpreter();

        base = new ConfiguredEnv(base);
        base.configuration().setProperty("de.upb.spl.SPLReasoner.evaluations", "60");
        base.configuration().setProperty("de.upb.spl.eval.hvTimeline.samples", "60");
        base.configuration().setProperty("de.upb.spl.eval.hvTimeline.logarithmic", "false");
        books = new BookkeeperEnv(base);

        List<ReasonerReplayer> benchmarkReplays =
                ReasonerReplayer.loadReplays(".", true);

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

        System.out.println("Calculating min max");


        MinMaxValues minMaxValues = new MinMaxValues(books);
        minMaxValues.run();
        min = minMaxValues.getMin();
        max = minMaxValues.getMax();

        System.out.println("Min is " + Arrays.toString(min));
        System.out.println("Max is " + Arrays.toString(max));

        final Map<String, List<Map<Integer, EvaluationAnalysis>>> hyperVolumeData = new HashMap<>();
        benchmarks.values().forEach(benchmark -> benchmark.replays.forEach(replayer -> {
            BookkeeperEnv measurements = new BookkeeperEnv(base,
                    benchmark.bookkeeper.bill(replayer.getReasonerName()));
            int hvSamples = base.configuration().getTimelinesamples();
            boolean logarithmicEval = base.configuration().getLogarithmicEval();
            int evaluations = base.configuration().getEvaluationPermits();
            final double powerStep;
            if(!logarithmicEval) {
                powerStep = ((double)evaluations /  (double)hvSamples);
            } else {
                powerStep = (Math.log(evaluations) - Math.log(1.))/ ((double)hvSamples);
            }
            long startTime = measurements.currentTab().checkLog(0).report().getTimestamp().orElse(0L);

            List<Set<BenchmarkEntry>> paretoFrontLayers = StreamSupport
                    .stream(Finisher.paretoLayers(
                            measurements,
                            Finisher.performanceCache(measurements),
                            measurements.currentTab()).spliterator(), false)
                    .collect(Collectors.toList());
            List<Integer> evaluationSamples;
            if(logarithmicEval)
                evaluationSamples = Stream.concat(
                    IntStream.range(0, hvSamples)
                            .mapToDouble(i -> Math.pow(Math.E, (i * powerStep)))
                            .filter(Double::isFinite)
                            .mapToInt(i -> (int) Math.floor(i)).boxed(),
                    IntStream.of(evaluations).boxed())
                    .sorted()
                    .distinct()
                    .collect(Collectors.toList());
            else
                evaluationSamples = Stream.concat(
                        IntStream.range(0, hvSamples)
                                .mapToDouble(i -> i * powerStep)
                                .filter(i-> i < evaluations)
                                .mapToInt(i -> (int) Math.floor(i)).boxed(),
                        IntStream.of(evaluations).boxed())
                        .sorted()
                        .distinct()
                        .collect(Collectors.toList());

            Map<Integer, EvaluationAnalysis> hvData = new LinkedHashMap<>();
            int lastSample = -1;
            for (int i = 0; i < evaluationSamples.size(); i++) {
                int sampleIndex = evaluationSamples.get(i);
                HypervolumeCalculator calculator = new HypervolumeCalculator(measurements, sampleIndex, min, max);
                calculator.run();
                double hv = calculator.getResult();
                RankCalculator rankCalculator = new RankCalculator(measurements, paretoFrontLayers, lastSample + 1, sampleIndex + 1);
                rankCalculator.run();
                double rank = rankCalculator.getAverageRank();
                JobReport report = measurements.currentTab().checkLog(sampleIndex).report();
                int memory = report.getMemory().orElse(-1);
                long timestamp = report.getTimestamp().orElse(-1L);
                if(timestamp != -1L) {
                    timestamp = timestamp - startTime;
                }
                EvaluationAnalysis analysis = new EvaluationAnalysis(memory, timestamp, hv, rank);
                hvData.put(sampleIndex, analysis);
                lastSample = sampleIndex;
            }
            hyperVolumeData
                    .computeIfAbsent(
                            replayer.getReasonerName(),
                            name -> new ArrayList<>())
                    .add(hvData);
        }));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileUtil.writeStringToFile("timeline/data.json", gson.toJson(hyperVolumeData));
    }

    public static void main(String... args ){
        new VEEval().eval();
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
