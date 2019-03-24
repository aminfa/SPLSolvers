package de.upb.spl.benchmarks.x264;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkAgent;
import de.upb.spl.benchmarks.BenchmarkBill;
import de.upb.spl.benchmarks.ReportInterpreter;
import de.upb.spl.benchmarks.JobReport;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.BenchmarkEnvironmentDecoration;
import de.upb.spl.benchmarks.env.FMAttributes;
import de.upb.spl.benchmarks.env.FMXML;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.upb.spl.util.FileUtil;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public class VideoEncoderBlackBox extends BenchmarkEnvironmentDecoration {

	private final static Logger logger = LoggerFactory.getLogger(VideoEncoderBlackBox.class);

	final String testVideo;
	final static String SPL_NAME = "video_encoder";

	public enum Objectives {
        subjective_quality, run_time, file_size;
    }

	ExecutorService executorService = Executors.newCachedThreadPool();

	final BenchmarkAgent agent;

	public VideoEncoderBlackBox(BenchmarkAgent agent) {
        this(
                new FMAttributes(
                        new FMXML(FileUtil.getPathOfResource(SPL_NAME + ".xml")),
                        new File(FileUtil.getPathOfResource(SPL_NAME + ".xml")).getParent(),
                        SPL_NAME),
                agent
        );
	}


    public VideoEncoderBlackBox(BenchmarkEnvironment env, BenchmarkAgent agent) {
	    super(env);
        this.testVideo = this.configuration().getVideoSourceFile();
        this.agent = Objects.requireNonNull(agent);
    }


	@Override
	public Future<JobReport> run(FeatureSelection selection, BenchmarkBill bill) {
        try {
            JobReport report = toReport(selection, bill.getClientName());
            return executorService.submit(new SubmitVideoEncoding(report));
        } catch(Exception ex) {
            logger.warn("Couldnt runAndGetPopulation benchmark for assemble {}. Exception message: {}", selection, ex.getMessage());
            logger.trace("Exception: ", ex);
            return null;
        }
	}

    @Override
    public ReportInterpreter interpreter(JobReport jobReport) {
        return new VideoEncoderReportInterpreter(jobReport);
    }

	private class SubmitVideoEncoding implements Callable<JobReport> {

		final JobReport report;

		SubmitVideoEncoding(JobReport report) {
			this.report = report;
		}

		@Override
		public JobReport call() throws Exception {
			agent.jobs().offerJob(report);
			agent.jobs().waitForResults(report);
            return report;
		}
	}

	private boolean checkResults(ReportInterpreter report) {
	    for(String objective : objectives()) {
	        if(!report.readResult(objective).isPresent()) {
	            return false;
            }
        }
	    return true;
    }

	public JobReport toReport(FeatureSelection selection, String clientName) {
		JobReport report = new JobReport();
		report.setGroup("x264");
		report.setClient(clientName);
		report.setConfiguration(toConfiguration(selection),"compile_hash", "runtime_hash");
		report.setObjectives(objectives());
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
		String compileHash = DigestUtils.sha1Hex(config.toJSONString());
		rootConfiguration.put("compile_hash", compileHash);

		config = new JSONObject();
//		String preset = "medium";
//		if(assemble.isSelected("preset_veryfast"))
//			preset = "veryfast";
//		if(assemble.isSelected("preset_faster"))
//			preset = "faster";
//		if(assemble.isSelected("preset_fast"))
//			preset = "fast";
//		if(assemble.isSelected("preset_slow"))
//			preset = "slow";
//		if(assemble.isSelected("preset_slower"))
//			preset = "slower";
//		config.put("preset", preset);
//
//		String tune = "film";
//		if(assemble.isSelected("tune_film"))
//			tune = "film";
//		if(assemble.isSelected("tune_animation"))
//			tune = "animation";
//		if(assemble.isSelected("tune_grain"))
//			tune = "grain";
//		if(assemble.isSelected("tune_stillimage"))
//			tune = "stillimage";
//		if(assemble.isSelected("tune_psnr"))
//			tune = "psnr";
//		if(assemble.isSelected("tune_ssim"))
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
		    if(selection.isSelected("two_pass")) {
		        throw new IllegalArgumentException("CRF and Two pass exclude one another.");
            }
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
		else if(selection.isSelected("aq_mode_2"))
			config.put("aq_mode", 2);
		else if(selection.isSelected("aq_mode_3"))
			config.put("aq_mode", 3);
        else {
            throw new RuntimeException("Selection isnt valid.");
        }

		if(selection.isSelected("aq_strength_1"))
			config.put("aq_strength", 1);
		else if(selection.isSelected("aq_strength_2"))
			config.put("aq_strength", 2);
		else if(selection.isSelected("aq_strength_3"))
			config.put("aq_strength", 3);
        else {
            throw new RuntimeException("Selection isnt valid.");
        }

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
        String runtimeHash = DigestUtils.sha1Hex(compileHash + config.toJSONString());
		rootConfiguration.put("runtime_hash", runtimeHash);

		return rootConfiguration;
	}


    public static class VideoEncoderReportInterpreter implements ReportInterpreter {
		private final JobReport report;

		VideoEncoderReportInterpreter(JobReport report) {
			this.report = report;
		}

		@Override
		public Optional<Double> readResult(String objective) {
            Optional<Double> raw = rawResult(objective);
            if(!raw.isPresent()) {
                return raw;
            }
            if(objective.equals("subjective_quality")) {
                return Optional.of(-1 * raw.get());
            }
            return raw;
		}

		@Override
        public Optional<Double> rawResult(String objective) {
            if(report.getResults().isPresent()) {
                Map results = report.getResults().get();
                if(results.containsKey(objective)){
                    return Optional.of(((Number) results.get(objective) ).doubleValue());
                }
            }
		    return Optional.empty();
        }

	}
}