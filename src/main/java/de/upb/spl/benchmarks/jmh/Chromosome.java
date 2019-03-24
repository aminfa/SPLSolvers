package de.upb.spl.benchmarks.jmh;

import de.upb.spl.util.Cache;
import jmh.Hanoi;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import jmh.Recursion;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class Chromosome  {

    private final static Cache<Options> baseOpts = new Cache<>(
            () ->
            new OptionsBuilder()
                .include(Hanoi.class.getName())
                .warmupTime(TimeValue.milliseconds(1))
                .measurementTime(TimeValue.seconds(2))
                .warmupIterations(6)
                .measurementIterations(3)
                .mode(Mode.Throughput)
                .forks(1)
                .verbosity(VerboseMode.NORMAL)
                .build());

    // Current score is not yet computed.
    private Optional<Double> score = Optional.empty();

    // These are current HotSpot defaults.
    int freqInlineSize = 325;
    int inlineSmallCode = 1000;
    int maxInlineLevel = 9;
    int maxInlineSize = 35;
    int maxRecursiveInlineLevel = 1;
    int minInliningThreshold = 250;

    public Chromosome() {
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
            score = Optional.of(0.);
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
                " -XX:MinInliningThreshold=" + minInliningThreshold;
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
}
