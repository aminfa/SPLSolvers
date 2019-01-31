package de.upb.spl.guo11;

import com.google.gson.Gson;
import de.upb.spl.*;
import de.upb.spl.benchmarks.*;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.XMLFeatureModel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class GuoTest {

	static BenchmarkEnvironment video_encoding_env;
	static BenchmarkAgent agent;

	final static Gson gson = new Gson().newBuilder().setPrettyPrinting().create();

	@BeforeClass
	public static void setEnvironment() throws FeatureModelException, IOException {
		final Random random = new Random(2);;
		final String featureModelFile = FesTransformTest.class.getClassLoader().getResource("video_encoder.xml").getPath();
		final FeatureModel video_encoder = new XMLFeatureModel(featureModelFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
		agent = new BenchmarkAgent(10000);
		VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent);

		// Load the XML file and creates the feature model
		video_encoder.loadModel();
		video_encoding_env = new VideoEncoderEnv(agent);
	}

	@Test
	public void testRun() throws InvocationTargetException, InterruptedException, ExecutionException {
		Guo11 guo11 = new Guo11(video_encoding_env);
		NondominatedPopulation population = guo11.run();
		for(Solution solution : population) {
			BinaryVariable variable = (BinaryVariable) solution.getVariable(0);
			FeatureSelection selection = FMUtil.selectFromPredicate(FMUtil.featureStream(video_encoding_env.model()).collect(Collectors.toList()),
					variable::get);
			VideoEncoderEnv.VideoEncoderReport report = (VideoEncoderEnv.VideoEncoderReport) video_encoding_env.run(selection).get();
			System.out.println("Configuration: " + gson.toJson(report.getFinalReport().getConfiguration()));
			System.out.println("File size: " + report.readResult(VideoEncoderEnv.objectives.get(0)));
			System.out.println("Subjective quality: " + report.readResult(VideoEncoderEnv.objectives.get(1)));
		}
		JFrame frame = new Plot().add("GUO", population).show();
		EventQueue.invokeAndWait(()->frame.setVisible(true));
		while(frame.isValid() || frame.isActive()) {
			Thread.sleep(100);
		}
	}

}
