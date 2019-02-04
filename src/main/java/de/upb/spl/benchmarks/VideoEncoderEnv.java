package de.upb.spl.benchmarks;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.guo11.Guo11;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.XMLFeatureModel;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class VideoEncoderEnv implements  BenchmarkEnvironment {
	private final static Logger logger = LoggerFactory.getLogger(BenchmarkEnvironment.class);

	final static FeatureModel video_encoder_fm;
	public final static List<String> objectives = Arrays.asList("file_size", "subjective_quality", "run_time");
	final static String testVideo = "flower_garden";


	final Random generator = new Random();

	ExecutorService executorService = Executors.newFixedThreadPool(4);


	static {
		final String featureModelFile = VideoEncoderEnv.class.getClassLoader().getResource("video_encoder_x264.xml").getPath();
		video_encoder_fm = new XMLFeatureModel(featureModelFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
		// Load the XML file and creates the feature model
		try {
			video_encoder_fm.loadModel();
		} catch (FeatureModelException e) {
			throw new RuntimeException("Cannot load x264 feature model.", e);
		}
	}

	final BenchmarkAgent agent;

	public VideoEncoderEnv(BenchmarkAgent agent) {
		this.agent = Objects.requireNonNull(agent);
	}

	@Override
	public FeatureModel model() {
		return video_encoder_fm;
	}

	@Override
	public List<String> objectives() {
		return objectives;
	}

	@Override
	public Future<BenchmarkReport> run(FeatureSelection selection) {
		try {
			JobReport report = toReport(selection);
			return executorService.submit(new SubmitVideoEncoding(agent, report));
		} catch(Exception ex) {
			logger.warn("Couldnt run benchmark for selection {}. Exception message: {}", selection, ex.getMessage());
			logger.trace("Exception: ", ex);
			return null;
		}
	}

	private static class SubmitVideoEncoding implements Callable<BenchmarkReport> {

		final JobReport report;
		final BenchmarkAgent agent;
		SubmitVideoEncoding(BenchmarkAgent agent, JobReport report) {
			this.report = report;
			this.agent = agent;
		}

		@Override
		public BenchmarkReport call() throws Exception {
			agent.jobs().offerJob(report);
			agent.jobs().waitForResults(report);
			return new VideoEncoderReport(report);
		}
	}

	public JobReport toReport(FeatureSelection selection) {
		JobReport report = new JobReport();
		report.setGroup("x264");
		report.setConfiguration(toConfiguration(selection),"compile_hash", "runtime_hash");
		return report;
	}

	public JSONObject toConfiguration(FeatureSelection selection) {
		if(! FMUtil.isValidSelection(model(), selection)) {
			throw new IllegalArgumentException("Selection is not valid: " + selection);
		}
		JSONObject rootConfiguration = new JSONObject();
		JSONObject config = new JSONObject();
		config.put("interlaced", selection.isSelected("interlaced"));
		config.put("asm", selection.isSelected("asm"));
		config.put("strip", selection.isSelected("strip"));
		config.put("lto", selection.isSelected("lto"));
		config.put("pic", selection.isSelected("pic"));

		String bitDepth = "all";
		if(selection.isSelected("bit_depth_8"))
			bitDepth = "8";
		if(selection.isSelected("bit_depth_10"))
			bitDepth = "10";
		config.put("BIT_DEPTH", bitDepth);

		String format = "all";
		if(selection.isSelected("format_420"))
			format = "420";
		if(selection.isSelected("format_422"))
			format = "422";
		if(selection.isSelected("format_444"))
			format = "444";
		config.put("FORMAT", format);
		rootConfiguration.put("compile", config);
		rootConfiguration.put("compile_hash", DigestUtils.sha1Hex(config.toJSONString()));

		config = new JSONObject();
//		String preset = "medium";
//		if(selection.isSelected("preset_veryfast"))
//			preset = "veryfast";
//		if(selection.isSelected("preset_faster"))
//			preset = "faster";
//		if(selection.isSelected("preset_fast"))
//			preset = "fast";
//		if(selection.isSelected("preset_slow"))
//			preset = "slow";
//		if(selection.isSelected("preset_slower"))
//			preset = "slower";
//		config.put("preset", preset);
//
//		String tune = "film";
//		if(selection.isSelected("tune_film"))
//			tune = "film";
//		if(selection.isSelected("tune_animation"))
//			tune = "animation";
//		if(selection.isSelected("tune_grain"))
//			tune = "grain";
//		if(selection.isSelected("tune_stillimage"))
//			tune = "stillimage";
//		if(selection.isSelected("tune_psnr"))
//			tune = "psnr";
//		if(selection.isSelected("tune_ssim"))
//			tune = "ssim";
//		config.put("tune", tune);

		config.put("video", testVideo);

		if(selection.isSelected("qp")) {
			int q = 24;
			if(selection.isSelected("q_26"))
				q = 26;
			if(selection.isSelected("q_32"))
				q = 32;
			if(selection.isSelected("q_38"))
				q =	38;

			int qpstep = 4;
			if(selection.isSelected("qpstep_4"))
				qpstep = 4;
			if(selection.isSelected("qpstep_8"))
				qpstep = 8;
			if(selection.isSelected("qpstep_12"))
				qpstep = 12;

			config.put("rate_ctrl", "qp");
			config.put("q", q);
			config.put("qpstep", qpstep);
		}
		if(selection.isSelected("crf")) {
			int crf = 23;
			if(selection.isSelected("crf_20"))
				crf = 20;
			if(selection.isSelected("crf_23"))
				crf = 23;
			if(selection.isSelected("crf_26"))
				crf = 26;
			if(selection.isSelected("crf_29"))
				crf = 29;
			if(selection.isSelected("crf_32"))
				crf = 32;
			if(selection.isSelected("crf_35"))
				crf = 35;
			config.put("rate_ctrl", "crf");
			config.put("crf", crf);
		}
		if(selection.isSelected("abr")) {
			int bitrate = 600;
			if(selection.isSelected("bitrate_800"))
				bitrate = 800;
			if(selection.isSelected("bitrate_1200"))
				bitrate = 1200;
			if(selection.isSelected("bitrate_1600"))
				bitrate = 1600;
			if(selection.isSelected("bitrate_2000"))
				bitrate = 2000;
			config.put("bitrate", bitrate);
			config.put("rate_ctrl", "abr");
		}
		int lookahead = 20;
		if(selection.isSelected("lookahead_50"))
			lookahead = 50;
		if(selection.isSelected("lookahead_80"))
			lookahead = 80;
		config.put("lookahead", lookahead);

		if(selection.isSelected("two_pass"))
			config.put("pass", "2");
		else
			config.put("pass", "1");

		if(selection.isSelected("aq_mode_1"))
			config.put("aq_mode", 1);
		if(selection.isSelected("aq_mode_2"))
			config.put("aq_mode", 2);
		if(selection.isSelected("aq_mode_3"))
			config.put("aq_mode", 3);

		if(selection.isSelected("aq_strength_1"))
			config.put("aq_strength", 1);
		if(selection.isSelected("aq_strength_2"))
			config.put("aq_strength", 2);
		if(selection.isSelected("aq_strength_3"))
			config.put("aq_strength", 3);

		if(selection.isSelected("ipratio_1_4"))
			config.put("ipratio", "1.4");
		if(selection.isSelected("ipratio_1_8"))
			config.put("ipratio", "1.8");
		if(selection.isSelected("ipratio_2_2"))
			config.put("ipratio", "2.2");

		if(selection.isSelected("pbratio_1_3"))
			config.put("pbratio", "1.3");
		if(selection.isSelected("pbratio_1_8"))
			config.put("pbratio", "1.8");
		if(selection.isSelected("pbratio_2_3"))
			config.put("pbratio", "2.3");


		rootConfiguration.put("runtime", config);
		rootConfiguration.put("runtime_hash", DigestUtils.sha1Hex(config.toJSONString()));

		return rootConfiguration;
	}

	@Override
	public Random generator() {
		return generator;
	}

	@Override
	public <T> T readParameter(String parameterName) {
		Objects.requireNonNull(parameterName);
		switch (parameterName) {
			case Guo11.INIT_D:
				return (T) Double.valueOf(0.5);
			case Guo11.INIT_POPULATION_SIZE:
				return (T) Integer.valueOf(30);
			case Guo11.GA_GENERATIONS:
				return (T) Integer.valueOf(1000);
			default:
				throw new IllegalArgumentException("Parameter " + parameterName + " is not defined.");
		}
	}
	public static class VideoEncoderReport implements BenchmarkReport {

		private final JobReport report;

		VideoEncoderReport(JobReport report) {
			this.report = report;
		}

		@Override
		public Optional<Double> readResult(String objective) {
			if(report.getResults().isPresent()) {
				Map results = report.getResults().get();
				if(objective.equals("subjective_quality") && results.containsKey("vmaf")) {
					Map vmaf = (Map) results.get("vmaf");
					if(vmaf.containsKey("VMAF_score"))
						return Optional.of(((Number) vmaf.get("VMAF_score") ).doubleValue());
				}
				if(objective.equals("run_time") && results.containsKey("run_time")) {
					return Optional.of(-1 * ((Number) results.get("run_time") ).doubleValue());
				}
				if(objective.equals("file_size") && results.containsKey("file_size")) {
					return Optional.of(-1 * ((Number) results.get("file_size") ).doubleValue());
				}
			}
			return Optional.empty();
		}

		@Override
		public boolean constraintsViolated() {
			return false;
		}

		@Override
		public double resourceSum() {
			return 1;
		}

		public JobReport getFinalReport() {
			return report;
		}
	}
}