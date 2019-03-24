package de.upb.spl.benchmarks.inline;

import org.junit.Test;

public class InlineConfigurationSampleTest {

    @Test
    public void score() {
        InlineConfigurationSample sample = new InlineConfigurationSample();
        System.out.println(sample.toString() + "\n" + sample.score());

        sample = new InlineConfigurationSample();
        sample.setFreqInlineSize(700);
        sample.setInlineSmallCode(2000);
        sample.setMaxInlineLevel(36);
        sample.setMaxInlineSize(70);
        sample.setMaxRecursiveInlineLevel(100);
        sample.setMinInliningThreshold(50);
        System.out.println(sample.toString() + "\n" + sample.score());

        sample = new InlineConfigurationSample();
        sample.setFreqInlineSize(2800);
        sample.setInlineSmallCode(8000);
        sample.setMaxInlineLevel(144);
        sample.setMaxInlineSize(2800);
        sample.setMaxRecursiveInlineLevel(1000);
        sample.setMinInliningThreshold(2500);
        System.out.println(sample.toString() + "\n" + sample.score());

    }
}