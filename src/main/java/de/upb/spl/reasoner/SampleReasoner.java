package de.upb.spl.reasoner;

import de.upb.spl.FeatureSelection;
import de.upb.spl.FeatureSet;
import de.upb.spl.ailibsintegration.FeatureSelectionOrdering;
import de.upb.spl.ailibsintegration.SPLReasonerAlgorithm;
import de.upb.spl.benchmarks.BenchmarkHelper;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.util.FileUtil;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SampleReasoner implements SPLReasoner {

    private final static Logger logger = LoggerFactory.getLogger(SampleReasoner.class);

    private static final AtomicInteger sampleReasonerId = new AtomicInteger(0);
    private String samplesName;
    private  List<List<String>> featureSelectionSamples;

    public SampleReasoner(List<List<String>> featureSelectionSamples) {
        samplesName = SampleReasoner.class.getSimpleName() + "-" + sampleReasonerId.getAndIncrement();
        this.featureSelectionSamples = featureSelectionSamples;
        logger.info("Sample reasoner `{}` with {} many samples created.", name(), featureSelectionSamples.size());
    }

    public SampleReasoner(String sampleFilePath) {
        this.samplesName = new File(sampleFilePath).getName();
        if(samplesName.endsWith(".json")) {
            samplesName = samplesName.substring(0, samplesName.length()-5);
        }
        String jsonString = FileUtil.readFileAsString(sampleFilePath);
        JSONParser parser = new JSONParser();
        try {
            this.featureSelectionSamples = (List<List<String>>) parser.parse(jsonString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        logger.info("Sample reasoner `{}` with {} many samples created.", name(), featureSelectionSamples.size());
    }



    @Override
    public SPLReasonerAlgorithm algorithm(BenchmarkEnvironment env) {
        return new Algorithm(env);
    }

    @Override
    public String name() {
        return samplesName;
    }

    private class Algorithm extends  SPLReasonerAlgorithm {

        AtomicInteger sampleIndex = new AtomicInteger(0);
        public Algorithm(BenchmarkEnvironment env) {
            super(env, name());
        }

        @Override
        protected void init() throws Exception {
            this.activate();
        }

        @Override
        protected void proceed() throws Exception {
            int index = sampleIndex.getAndIncrement();
            if(index >= featureSelectionSamples.size()) {
                this.terminate();
                return;
            }
            List<String> featureIdList = featureSelectionSamples.get(index);
            FeatureSelection selection = new FeatureSet(getInput().model(), featureIdList);
            double[] performance = BasicIbea.evaluateAndCountViolatedConstraints(getInput(), selection);
            FeatureSelectionOrdering ordering = new FeatureSelectionOrdering(performance);
            logger.info("Sample reasoner `{}` evaluated sample index {}: {}", name(), index, ordering.toString());
        }

    }
}
