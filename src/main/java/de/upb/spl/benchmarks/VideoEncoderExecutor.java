package de.upb.spl.benchmarks;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.upb.spl.util.FileUtil;
import de.upb.spl.util.Streams;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class VideoEncoderExecutor implements Runnable{

	public static final String GROUP = "x264";
	private final static Logger logger = LoggerFactory.getLogger(VideoEncoderExecutor.class);

	private final static AtomicLong GLOBAL_ID_COUNT = new AtomicLong(0);

	private final static String BENCHMARK_DIR;

	private final static String PYTHON_BIN;

	private final String scriptsHome;

	static {
		String benchmarkHome = System.getenv("X264_BENCH_HOME");
		if(benchmarkHome == null) {
			BENCHMARK_DIR = System.getProperty("user.home") + "/Documents/BA/VideoBench";
		} else {
			BENCHMARK_DIR = Paths.get(benchmarkHome).toString();
		}
        String pythonBin = System.getenv("PYTHON_BIN");
        if(pythonBin == null) {
            PYTHON_BIN = System.getProperty("user.home") + "/anaconda3/envs/x264/bin/python";
        } else {
            PYTHON_BIN = Paths.get(benchmarkHome).toString();
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
				logger.error("`{}` unexpected error while executing job:\n ", getExecutorId(),  ex);
			}
		}
	}

	public static List<String> unpackInto(Map<String, Object> settings, List<String> features) {
		for(String feature : settings.keySet()) {
			Object val = settings.get(feature);
			if(val == null) {
				continue;
			}
			if(val instanceof Boolean && ((Boolean)val)) {
				features.add(feature);
			} else {
				features.add(feature+val.toString());
			}
		}
		return features;
	}

    public static RandomAttributesExecutor randomExecutor(BenchmarkAgent agent) {
        RandomAttributesExecutor executor1 = new RandomAttributesExecutor(agent, VideoEncoderExecutor.GROUP,
                config -> {
                    List<String> features = new ArrayList<>();
                    Map<String, Object> compileConfig = (Map<String, Object>) config.get("compile");
                    unpackInto(compileConfig, features);
                    Map<String, Object> runtimeConfig = (Map<String, Object>) config.get("runtime");
                    unpackInto(runtimeConfig, features);
                    return features;
                });
        return executor1;
    }

    public static StoredAttributesExecutor fixedAttributesExecutor(BenchmarkAgent agent) {
	    JSONParser parser = new JSONParser();
        JSONObject attributeValues;
	    try {
            attributeValues = (JSONObject) parser.parse(
                    FileUtil.readResourceAsString("attributes/video_encoder.attributes.json"));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        StoredAttributesExecutor executor1 = new StoredAttributesExecutor(agent, VideoEncoderExecutor.GROUP,
                config -> {
                    List<String> features = new ArrayList<>();
                    Map<String, Object> compileConfig = (Map<String, Object>) config.get("compile");
                    unpackInto(compileConfig, features);
                    Map<String, Object> runtimeConfig = (Map<String, Object>) config.get("runtime");
                    unpackInto(runtimeConfig, features);
                    return features;
                }, attributeValues);
        return executor1;
    }


	private JobApplication getJobApplication () {
		List<String> binCaches = FileUtil.listAllFilesInDir(Paths.get(BENCHMARK_DIR, "bin_cache").toString(), "^[A-Fa-f0-9]+$", false);
		List<String> outCaches = FileUtil.listAllFilesInDir(Paths.get(BENCHMARK_DIR, "out_cache").toString(), "^[A-Fa-f0-9]+$", false);
		JobApplication application = new JobApplication(id, GROUP, Arrays.asList(binCaches, outCaches));
		return application;
	}

	private Map getJobResult(String hash) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(FileUtil.readFileAsString(Paths.get(BENCHMARK_DIR, "out_cache", hash, "report.json").toString()));
		if(obj.containsKey("vmaf")) {
			Map vmaf = (Map) obj.get("vmaf");
			if (vmaf.containsKey("VMAF_score")) {
				double quality = ((Number) vmaf.get("VMAF_score")).doubleValue();
				obj.put("subjective_quality", quality);
			}
		}
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
        String[] shell_cmd = {PYTHON_BIN, benchmark_script_path, jobId, base64Config};
        logger.info("Executing job {}.", report.getJobId());
//		String shell_command = String.join(" ", shell_cmd);
        Process child = Runtime.getRuntime().exec(shell_cmd);
        boolean exited = child.waitFor(3, TimeUnit.MINUTES);
        if(!exited) {
            child.destroyForcibly();
            throw new RuntimeException("Execution timeout for job: " + report.getJobId() +
                    " Command:\n " + Arrays.asList(shell_cmd).stream().collect(Collectors.joining(" ")));
        }
        String shellOutput = Streams.InReadString(child.getInputStream());
        String shellError  = Streams.InReadString(child.getErrorStream());
        if(child.exitValue()!=0) {
            throw new RuntimeException("Couldn't execute video encoding pipeline: " + shellOutput + "\nError: "+ shellError);
        }
		if(logger.isTraceEnabled()) {
			FileUtil.writeStringToFile(String.format("x264-logs/%s-shellout-%s.log", getExecutorId(), report.getJobId()), shellOutput);
		}
		String lastHash = report.getConfigHashes().get(report.getConfigHashes().size()-1);
		report.setResults(getJobResult(lastHash));
	}

}
