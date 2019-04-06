package de.upb.spl.reasoner;

import de.upb.spl.FeatureSelection;
import de.upb.spl.FeatureSet;
import de.upb.spl.ailibsintegration.SPLReasonerAlgorithm;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.BenchmarkHelper;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.util.FileUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Replays the recording done by RecordReplay
 */
public class ReasonerReplayer implements SPLReasoner {
    private final static Logger logger = LoggerFactory.getLogger(ReasonerReplayer.class);

    private String reasonerName;

    private final List<JSONObject> recordings;

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

    public void setName(String name) {
        this.reasonerName = name;
    }

    public Iterator<JobReport> jobReportIterator() {
        return new Iterator<JobReport>() {
            int replayIndex = 0;
            @Override
            public boolean hasNext() {
                return replayIndex < recordings.size();
            }

            @Override
            public JobReport next() {
                JSONObject record = recordings.get(replayIndex);
                Map rawJsonReport = (JSONObject) record.get("report");
                JobReport report = new JobReport(rawJsonReport);
                return report;
            }
        };
    }

    private class ReplayAlgorithm extends SPLReasonerAlgorithm {
        int replayIndex;

        public ReplayAlgorithm(BenchmarkEnvironment env) {
            super(
                    (env).getDecoration(BookkeeperEnv.class),
                    reasonerName);
        }

        @Override
        public BookkeeperEnv getInput() {
            return (BookkeeperEnv) super.getInput();
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
                        getInput().logEvaluation(selection, report);
            }
            replayIndex++;
        }

    }

    public static List<ReasonerReplayer> loadReplays(String replayHome, boolean nameToFilename) {
        if(!replayHome.endsWith(File.separator)) {
            replayHome = replayHome.concat(File.separator);
        }
        List<ReasonerReplayer> replayers = new ArrayList<>();
        List<String> replayFiles = FileUtil.listAllFilesInDir(replayHome, ".*json$", true);
        for(String replayFile : replayFiles) {
            String replayPath = replayHome + replayFile;
            ReasonerReplayer replayer = new ReasonerReplayer(replayPath);
            if(nameToFilename)
                replayer.setName(replayFile.substring(0, replayFile.length()-".json".length()));
            logger.info("Replayer loaded from file: {}", replayPath);
            replayers.add(replayer);
        }
        return replayers;
    }

    public static void retainGroup(List<ReasonerReplayer> replayers, String groupName) {
        Iterator<ReasonerReplayer> replayerIterator = replayers.iterator();
        while(replayerIterator.hasNext()) {
            ReasonerReplayer replayer = replayerIterator.next();
            Iterator<JobReport> iterator = replayer.jobReportIterator();
            if(iterator.hasNext()) {
                JobReport report = iterator.next();
                if (!groupName.equals(report.getGroup())) {
                    logger.warn("Removing recording with group {}.", report.getGroup());
                    replayerIterator.remove();
                }
            }
        }
    }

}
