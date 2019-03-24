package de.upb.spl.benchmarks.inline;

import de.upb.spl.util.Cache;
import jmh.Hanoi;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InlineConfigurationSample {

    private final static Cache<Options> baseOpts = new Cache<>(
            () ->
            new OptionsBuilder()
                .include(Hanoi.class.getName())
                .warmupTime(TimeValue.seconds(1))
                .measurementTime(TimeValue.seconds(10))
                .warmupIterations(0)
                .measurementIterations(5)
                .mode(Mode.SingleShotTime)
                .forks(1)
                .verbosity(VerboseMode.SILENT)
                .build());

    // Current score is not yet computed.
    private Optional<Double> score = Optional.empty();

    // These are current HotSpot defaults.
    int freqInlineSize = 325;
    int inlineSmallCode = 1000;

    // Maximum number of nested calls that are inlined
    int maxInlineLevel = 9;

    // Maximum bytecode size of a method to be inlined
    int maxInlineSize = 35;


    int maxRecursiveInlineLevel = 1;

    // Minimum invocation count a method needs to have to be inlined
    int minInliningThreshold = 250;

    public InlineConfigurationSample() {
    }

    public double score() {
        if (score.isPresent()) {
            // Already got the score, shortcutting
            return score.get();
        }

        try {
            // Add the options encoded by this solution:
            //  a) Mix in base options.
            //  b) Add JVM arguments: we opt to parse the
            //     stringly representation to make the example
            //     shorter. There are, of course, cleaner ways
            //     to do this.
            Options theseOpts = new OptionsBuilder()
                    .parent(baseOpts.get())
                    .jvmArgs(toString().split("[ ]"))
                    .build();

            // Run through JMH and get the result back.
            RunResult runResults = new Runner(theseOpts).runSingle();
            runResults.getSecondaryResults();
            score = Optional.of(runResults.getPrimaryResult().getScore());
        } catch (RunnerException e) {
            // Something went wrong, the solution is defective
            score = Optional.of(1000.);
        }

        return score.get();
    }

    @Override
    public String toString() {
        return "-XX:FreqInlineSize=" + freqInlineSize +
                " -XX:InlineSmallCode=" + inlineSmallCode +
                " -XX:MaxInlineLevel=" + maxInlineLevel +
                " -XX:MaxInlineSize=" + maxInlineSize +
                " -XX:MaxRecursiveInlineLevel=" + maxRecursiveInlineLevel +
                " -XX:MinInliningThreshold=" + minInliningThreshold
                +
//                " -XX:+PrintCodeCache" +
                " -XX:+UseCodeCacheFlushing" +
                " -XX:ReservedCodeCacheSize=2500k" +
                " -XX:CodeCacheMinimumFreeSpace=100k"
                ;
    }

    public int getFreqInlineSize() {
        return freqInlineSize;
    }

    public void setFreqInlineSize(int freqInlineSize) {
        this.freqInlineSize = freqInlineSize;
    }

    public int getInlineSmallCode() {
        return inlineSmallCode;
    }

    public void setInlineSmallCode(int inlineSmallCode) {
        this.inlineSmallCode = inlineSmallCode;
    }

    public int getMaxInlineLevel() {
        return maxInlineLevel;
    }

    public void setMaxInlineLevel(int maxInlineLevel) {
        this.maxInlineLevel = maxInlineLevel;
    }

    public int getMaxInlineSize() {
        return maxInlineSize;
    }

    public void setMaxInlineSize(int maxInlineSize) {
        this.maxInlineSize = maxInlineSize;
    }

    public int getMaxRecursiveInlineLevel() {
        return maxRecursiveInlineLevel;
    }

    public void setMaxRecursiveInlineLevel(int maxRecursiveInlineLevel) {
        this.maxRecursiveInlineLevel = maxRecursiveInlineLevel;
    }

    public int getMinInliningThreshold() {
        return minInliningThreshold;
    }

    public void setMinInliningThreshold(int minInliningThreshold) {
        this.minInliningThreshold = minInliningThreshold;
    }

    public Map<String, Integer> dumpMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("FreqInlineSize", freqInlineSize );
        map.put("InlineSmallCode", inlineSmallCode );
        map.put("MaxInlineLevel", maxInlineLevel );
        map.put("MaxInlineSize", maxInlineSize );
        map.put("MaxRecursiveInlineLevel", maxRecursiveInlineLevel );
        map.put("MinInliningThreshold", minInliningThreshold);
        return map;
    }

    public void loadMap(Map<String, Integer> map) {
        freqInlineSize = map.get("FreqInlineSize");
        inlineSmallCode = map.get("InlineSmallCode");
        maxInlineLevel = map.get("MaxInlineLevel");
        maxInlineSize = map.get("MaxInlineSize");
        maxRecursiveInlineLevel = map.get("MaxRecursiveInlineLevel");
        minInliningThreshold = map.get("MinInliningThreshold");
    }
}
