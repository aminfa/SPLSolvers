package de.upb.spl.guo11;

import com.google.gson.Gson;
import de.upb.spl.*;
import de.upb.spl.benchmarks.*;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.benchmarks.env.VideoEncoderEnv;
import de.upb.spl.presentation.ParetoPresentation;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.XMLFeatureModel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class GuoTest {

	static BenchmarkEnvironment video_encoding_env;
	static BenchmarkAgent agent;

	final static Gson gson = new Gson().newBuilder().setPrettyPrinting().create();

	@BeforeClass
	public static void setEnvironment() throws FeatureModelException, IOException {
		final Random random = new Random();;
		final String featureModelFile = FesTransformTest.class.getClassLoader().getResource(
				"radom_1000.xml").getPath();
		final FeatureModel video_encoder = new XMLFeatureModel(featureModelFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
		agent = new BenchmarkAgent(10000);
//		VideoEncoderExecutor executor1 = new VideoEncoderExecutor(agent, "/Users/aminfaez/Documents/BA/x264_1");
		VideoEncoderExecutor.randomExecutor(agent);
		// Load the XML file and creates the listFeatures model
		video_encoder.loadModel();
		video_encoding_env = new VideoEncoderEnv(agent);
	}

	@Test
	public void testRun() throws InvocationTargetException, InterruptedException, ExecutionException {
		Guo11 guo11 = new Guo11();
		Population population = guo11.runAndGetPopulation(video_encoding_env);
		dumpPopulation(population);
		JFrame frame = ParetoPresentation.showGUI(video_encoding_env, guo11, population);
		EventQueue.invokeAndWait(()->frame.setVisible(true));

	}
	private void dumpPopulation(Population population) throws ExecutionException,
			InterruptedException {
		for(Solution solution : population) {
			BinaryVariable variable = (BinaryVariable) solution.getVariable(0);
			FeatureSelection selection = FMUtil.selectFromPredicate(FMUtil.featureStream(video_encoding_env.model()).collect(Collectors.toList()),
					variable::get);
			System.out.println("Configuration: "      + selection);
			System.out.println("File size: "          + solution.getObjective(0));
			System.out.println("Subjective quality: " + solution.getObjective(1));
//			System.out.println("Runtime: "            + solution.getObjective(2));
		}
	}
}
