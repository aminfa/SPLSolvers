package de.upb.spl.finish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.upb.spl.FMUtil;
import de.upb.spl.benchmarks.BenchmarkEntry;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.util.FileUtil;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ReasonerRecorder extends Finisher {

    private final static Logger logger = LoggerFactory.getLogger(ReasonerRecorder.class);
    private final String recordHome;
    private final Date date = new Date();

    public ReasonerRecorder(BenchmarkEnvironmentDecoration env) {
        super(env);
        this.recordHome = env.configuration().getRecordHome();
    }

    public BookkeeperEnv env() {
        return ((BenchmarkEnvironmentDecoration) super.env()).getDecoration(BookkeeperEnv.class);
    }

    @Override
    public void run() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for(BenchmarkBill bill : env().bills()) {
            JSONObject record = new JSONObject();
            record.put("name", bill.getReasonerName());
            List<JSONObject> logs = new ArrayList<>();
            record.put("recordings", logs);
            for (BenchmarkEntry log : bill) {
                JSONObject logObj = new JSONObject();
                logObj.put("selection", log.selection().stream()
                        .map(FMUtil::id)
                        .collect(Collectors.toList()));
                logObj.put("report", log.report().getJsonObj());
                logs.add(logObj);
            }
            String recordFile = String.format("%1$s/%2$tY-%2$tm-%2$td--%2$tH.%2$tM--%3$s.json", recordHome, date, bill.getReasonerName());
            FileUtil.writeStringToFile(recordFile, gson.toJson(record));
            logger.info("Recorded replay for {} in {}.", bill.getReasonerName(), recordFile);
        }
    }

    @Override
    public String toString() {
        return "Record";
    }

    public static void main(String[] args) {
        System.out.println(String.format("%1$s/%2$tY-%2$tm-%2$td--%2$tH.%2$tM--%3$s.json", "a", new Date(), "b"));
    }
}
