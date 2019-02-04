package de.upb.spl.benchmarks;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FileUtil;
import util.ShellUtil;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class VideoEncoderExecutor implements Runnable{

	private final static Logger logger = LoggerFactory.getLogger(VideoEncoderExecutor.class);

	private final static AtomicLong GLOBAL_ID_COUNT = new AtomicLong(0);

	private final static String BENCHMARK_DIR;

	private final String scriptsHome;

	static {
		String benchmarkHome = System.getenv("X264_BENCH_HOME");
		if(benchmarkHome == null) {
			BENCHMARK_DIR = System.getProperty("user.home") + "/Documents/BA/VideoBench";
		} else {
			BENCHMARK_DIR = Paths.get(benchmarkHome).toString();
		}
	}

	private final Thread executor;
	private final String id;
	private final BenchmarkAgent agent;

	public VideoEncoderExecutor(BenchmarkAgent agent, String scriptsHome) {
		this.scriptsHome = scriptsHome;
		id = String.format("video-encoder-executor-%02d", GLOBAL_ID_COUNT.getAndIncrement());
		this.agent = agent;
		executor = new Thread(this);
		executor.setDaemon(true);
		executor.setName(id);
		executor.setPriority(Thread.MIN_PRIORITY);
		executor.start();
	}

	public String getExecutorId(){
		return id;
	}

	public void run() {
		logger.info("`{}` started.", getExecutorId());
		while(true) {
			try{
				executeJob();
			}catch(Exception ex) {
				logger.error("`{}` unexpected error while executing job:\n ", ex);
			}
		}
	}


	private JobApplication getJobApplication () {
		List<String> binCaches = FileUtil.listAllFilesInDir(Paths.get(BENCHMARK_DIR, "bin_cache").toString(), "^[A-Fa-f0-9]+$", false);
		List<String> outCaches = FileUtil.listAllFilesInDir(Paths.get(BENCHMARK_DIR, "out_cache").toString(), "^[A-Fa-f0-9]+$", false);
		JobApplication application = new JobApplication(id, "x264", Arrays.asList(binCaches, outCaches));
		return application;
	}

	private Map getJobResult(String jobId) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(FileUtil.readFileAsString(Paths.get(BENCHMARK_DIR, "reports", jobId, "result.json").toString()));
		return obj;
	}

	private void executeJob() throws InterruptedException {
		logger.info("Asking agent for job");
		JobReport report = agent.jobs().waitForJob(getJobApplication());
		try {
			executeJob(report);
		} catch (Exception ex) {
			logger.error("{} error while executing job with id {}: ", id, report.getJobId(), ex);
			report.setResultsIfNull();
		} finally {
			agent.jobs().update(report);
		}
	}

	private void executeJob(JobReport report) throws Exception {
		Map configuration = report.getConfiguration();
		byte[] configurationBytes = JSONObject.toJSONString(configuration).getBytes();
		String base64Config = Base64.getEncoder().encodeToString(configurationBytes);
		String benchmark_script_path = Paths.get(scriptsHome, "benchmark.py").toAbsolutePath().toString();
		String jobId = report.getJobId();
//		String shell_command = "python %s %s %s";
//		shell_command = String.format(shell_command, BENCHMARK_DIR, benchmark_script_path, jobId, base64Config);
		String[] shell_cmd = {"python", benchmark_script_path, jobId, base64Config};
		String shell_command = String.join(" ", shell_cmd);
		String shellOutput = ShellUtil.sh(shell_cmd);
		if(logger.isDebugEnabled()) {
			FileUtil.writeStringToFile(String.format("x264-logs/%s-shellout-%s.log", getExecutorId(), report.getJobId()), shellOutput);
		}
		report.setResults(getJobResult(report.getJobId()));
	}

}
