package de.upb.spl.benchmarks;

import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BookkeeperEnv;
import de.upb.spl.reasoner.ReasonerReplayer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BenchmarkReplay {

    private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH.mm");

    private final ReasonerReplayer replayer;
    private int benchmarkIndex;
    private Date benchmarkDate;
    private String reasonerName;

    public BenchmarkReplay(ReasonerReplayer replayer) {
        this.replayer = replayer;
        String benchmarkTag = replayer.name();
        String[] spliteroo = benchmarkTag.split("--");
        try {
            benchmarkIndex = Integer.parseInt(spliteroo[0]);
            String date = (spliteroo[1]);
            String dayTime = (spliteroo[2]);
            benchmarkDate = formatter.parse(date + "-" + dayTime);
            reasonerName = (spliteroo[3]);
        } catch (RuntimeException | ParseException ex) {
            throw new IllegalArgumentException(
                    " Given benchmark tag cannot be parsed: " + benchmarkTag + "\n" +
                    "Benchmark tags must have the following format: " +
                    "<benchmark index>--yyyy-MM-dd-HH.mm--<reasoner name>", ex);
        }
    }



    public int getBenchmarkIndex() {
        return benchmarkIndex;
    }

    public Date getBenchmarkDate() {
        return benchmarkDate;
    }

    public String getReasonerName() {
        return reasonerName;
    }

    public ReasonerReplayer getReplayer() {
        return replayer;
    }
}
