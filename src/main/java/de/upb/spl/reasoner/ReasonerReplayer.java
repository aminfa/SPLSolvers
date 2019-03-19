package de.upb.spl.reasoner;

import de.upb.spl.FeatureSelection;
import de.upb.spl.FeatureSet;
import de.upb.spl.ailibsintegration.SPLReasonerAlgorithm;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkHelper;
import de.upb.spl.util.FileUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Replays the recording done by RecordReplay
 */
public class ReasonerReplayer implements SPLReasoner {
    private final static Logger logger = LoggerFactory.getLogger(ReasonerReplayer.class);

    private final String reasonerName;

    private final List<JSONObject> recordings;

    private final List<BenchmarkEntry> entryList = new ArrayList<>();

    public ReasonerReplayer(String recordFilePath) {
        JSONParser parser = new JSONParser();
        JSONObject recording;
        try {
            recording = (JSONObject) parser.parse(FileUtil.readFileAsString(recordFilePath));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        reasonerName = (String) recording.get("name");
        recordings = new ArrayList<>((List<JSONObject>) recording.get("recordings"));
    }

    @Override
    public SPLReasonerAlgorithm algorithm(BenchmarkEnvironment env) {
        return new ReplayAlgorithm(env);
    }


    @Override
    public String name() {
        return reasonerName;
    }

    private class ReplayAlgorithm extends SPLReasonerAlgorithm {
        int replayIndex;

        public ReplayAlgorithm(BenchmarkEnvironment env) {
            super(env, reasonerName);
        }

        @Override
        protected void init() throws Exception {
            replayIndex = 0;
            this.activate();
        }

        @Override
        protected void proceed() throws Exception {
            if(replayIndex >= recordings.size()) {
                terminate();
                return;
            }
            JSONObject record = recordings.get(replayIndex);
            List<String> selectionList = (List<String>) record.get("selection");
            FeatureSelection selection = new FeatureSet(getInput().model(),selectionList);
            if(getInput().configuration().getReplayRerunSelection()) {
                logger.debug("Replay {} is reevaluating feature selection.", name());
                BenchmarkHelper.evaluateFeatureSelection(getInput(), selection);
            } else {
                logger.debug("Replay {} is logging feature selection and report.", name());
                Map rawJsonReport = (JSONObject) record.get("report");
                JobReport report = new JobReport(rawJsonReport);
                getInput().currentTab().logEvaluation(selection, report);
            }
            replayIndex++;
        }

        @Override
        protected FeatureSelection best() {
//            throw new UnsupportedOperationException("Replay doesn't know which selection is the best.");
            logger.warn("Replay doesn't know which selection is the best. Returning last record.");
            return new FeatureSet(getInput().model(), ((List<String>) recordings.get(recordings.size() - 1).get("selection")));
        }
    }

}
