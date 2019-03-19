package de.upb.spl.eval;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.FMUtil;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.util.FileUtil;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReasonerRecorder extends Evaluator {

    private final static Logger logger = LoggerFactory.getLogger(ReasonerRecorder.class);
    private final String reasonerName, recordFile;

    public ReasonerRecorder(BenchmarkEnvironment env, String reasonerName, String recordFile) {
        super(env);
        this.recordFile = recordFile;
        this.reasonerName = reasonerName;
    }

    @Override
    public void run() {
        BenchmarkBill bill = env().bill(reasonerName);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JSONObject record = new JSONObject();
        record.put("name", reasonerName);
        List<JSONObject> logs = new ArrayList<>();
        record.put("recordings", logs);
        for(BenchmarkEntry log : bill) {
            JSONObject logObj = new JSONObject();
            logObj.put("selection", log.selection().stream()
                    .map(FMUtil::id)
                    .collect(Collectors.toList()));
            logObj.put("report", log.report().getJsonObj());
            logs.add(logObj);
        }
        FileUtil.writeStringToFile(recordFile, gson.toJson(record));
        logger.info("Recorded replay for {} in {}.", reasonerName, recordFile);
    }
}
