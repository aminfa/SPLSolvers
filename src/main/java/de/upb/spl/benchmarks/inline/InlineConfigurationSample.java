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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class InlineConfigurationSample {

    private final static Logger logger = LoggerFactory.getLogger(InlineConfigurationSample.class);

    private final static Cache<Options> baseOpts = new Cache<>(
            () ->
            new OptionsBuilder()
                .warmupTime(TimeValue.seconds(1))
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(3)
                .timeUnit(TimeUnit.MICROSECONDS)
                .mode(Mode.AverageTime)
                .forks(1)
                .verbosity(logger.isDebugEnabled() ? VerboseMode.NORMAL : VerboseMode.SILENT)
                .build());

    // Current score is not yet computed.
    private Optional<Double> score = null;

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

    int warmups = 4;

    String target = "jmh.Factorial";

    public InlineConfigurationSample() {
    }

    public Optional<Double> score() {
        if (score != null) {
            // Already got the score, shortcutting
            return score;
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
                    .warmupIterations(getWarmups())
                    .include(getTarget())
                    .build();

            // Run through JMH and get the result back.
            RunResult runResults = new Runner(theseOpts).runSingle();
            score = Optional.of(runResults.getPrimaryResult().getScore());
        } catch (RunnerException e) {
            // Something went wrong, the solution is defective
            logger.error("Error while benchmarking {}: ",  getTarget(), e);
            score = Optional.empty();
        }

        return score;
    }

    @Override
    public String toString() {
        return "-XX:FreqInlineSize=" + freqInlineSize +
                " -XX:InlineSmallCode=" + inlineSmallCode +
                " -XX:MaxInlineLevel=" + maxInlineLevel +
                " -XX:MaxInlineSize=" + maxInlineSize +
                " -XX:MaxRecursiveInlineLevel=" + maxRecursiveInlineLevel +
                " -XX:MinInliningThreshold=" + minInliningThreshold
                + (logger.isDebugEnabled() ? " -XX:+PrintCodeCache" : "")
//              + " -XX:+UseCodeCacheFlushing"
//                + " -XX:ReservedCodeCacheSize=350000k"
//              + " -XX:CodeCacheMinimumFreeSpace=100k"
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

    public int getWarmups() {
        return warmups;
    }

    public void setWarmups(int warmups) {
        this.warmups = warmups;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Map<String, Object> dumpMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("FreqInlineSize", freqInlineSize );
        map.put("InlineSmallCode", inlineSmallCode );
        map.put("MaxInlineLevel", maxInlineLevel );
        map.put("MaxInlineSize", maxInlineSize );
        map.put("MaxRecursiveInlineLevel", maxRecursiveInlineLevel );
        map.put("MinInliningThreshold", minInliningThreshold);
        map.put("warmups", getWarmups());
        map.put("target", getTarget());
        return map;
    }

    public void loadMap(Map map) {
        Map<String, Number> intMap = map;
        freqInlineSize = intMap.get("FreqInlineSize").intValue();
        inlineSmallCode = intMap.get("InlineSmallCode").intValue();
        maxInlineLevel = intMap.get("MaxInlineLevel").intValue();
        maxInlineSize = intMap.get("Ma‰xInlineSize").intValue();
        maxRecursiveInlineLevel = intMap.get("MaxRecursiveInlineLevel").intValue();
        minInliningThreshold = intMap.get("MinInliningThreshold").intValue();
        setWarmups(intMap.get("warumups").intValue());
        Map<String, String> stringMap = map;
        target = stringMap.get("target");
    }
}
