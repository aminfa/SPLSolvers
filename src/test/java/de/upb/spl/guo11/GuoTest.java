package de.upb.spl.guo11;

import de.upb.spl.*;
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
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class GuoTest {

	static BenchmarkEnvironment video_encoding_env;


	@BeforeClass
	public static void setEnvironment() throws FeatureModelException {
		final Random random = new Random(2);;
		final String featureModelFile = FesTransformTest.class.getClassLoader().getResource("video_encoder.xml").getPath();
		final FeatureModel video_encoder = new XMLFeatureModel(featureModelFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
		// Load the XML file and creates the feature model
		video_encoder.loadModel();

		video_encoding_env = new BenchmarkSimulation(
				(selection, objective)-> {
					if(!FMUtil.isValidSelection(video_encoder, selection)) {
						return Double.MIN_VALUE;
					}
					double value = 0;
					if("Filesize".equals(objective)) {
						if(selection.isSelected("op1")) {
							value += 1;
						}
						if(selection.isSelected("op2")) {
							value += 5;
						}
						if(selection.isSelected("op3")) {
							value -= 10;
						}
						if(selection.isSelected("small")) {
							value += 20;
						}
						if(selection.isSelected("fast")) {
							value -= 10;
						}
						if(selection.isSelected("compress")) {
							value += 20;
						}
						if(selection.isSelected("a1")) {
							value -= 5;
						}
						if(selection.isSelected("a2")) {
							value -= 10;
						}
						if(selection.isSelected("a3")) {
							value -= 15;
						}

					} else if("Runtime".equals(objective)) {
						if(selection.isSelected("op1")) {
							value += 1;
						}
						if(selection.isSelected("op2")) {
							value -= 5;
						}
						if(selection.isSelected("op3")) {
							value += 5;
						}
						if(selection.isSelected("small")) {
							value -= 10;
						}
						if(selection.isSelected("fast")) {
							value += 25;
						}
						if(selection.isSelected("compress")) {
							value -= 15;
						}
						if(selection.isSelected("encrypt")) {
							value -= 5;
						}
						if(selection.isSelected("a1")) {
							value += 5;
						}
						if(selection.isSelected("a2")) {
							value += 10;
						}
						if(selection.isSelected("a3")) {
							value += 15;
						}
					} else {
						throw new IllegalArgumentException("Objective not recognized: " + objective.toString());
					}
					return value;
				}) {
			@Override
			public FeatureModel model() {
				return video_encoder;
			}

			@Override
			public List<String> objectives() {
				return Arrays.asList("Filesize", "Runtime");
			}

			@Override
			public Random generator() {
				return random;
			}

			@Override
			public <T> T readParameter(String parameterName) {
				Objects.requireNonNull(parameterName);
				switch (parameterName) {
					case Guo11.INIT_D:
						return (T) Double.valueOf(0.8);
					case Guo11.INIT_POPULATION_SIZE:
						return (T) Integer.valueOf(30);
					case Guo11.GA_GENERATIONS:
						return (T) Integer.valueOf(100);
					default:
						throw new IllegalArgumentException("Parameter " + parameterName + " is not defined.");
				}
			}
		};
	}

	@Test
	public void testRun() throws InvocationTargetException, InterruptedException {
		Guo11 guo11 = new Guo11(video_encoding_env);
		NondominatedPopulation population = guo11.run();
		for(Solution solution : population) {
			BinaryVariable variable = (BinaryVariable) solution.getVariable(0);
			FeatureSelection selection = FMUtil.selectFromPredicate(FMUtil.featureStream(video_encoding_env.model()).collect(Collectors.toList()),
					variable::get);
			System.out.println(selection);

		}
		JFrame frame = new Plot().add("GUO", population).show();
		EventQueue.invokeAndWait(()->frame.setVisible(true));
		while(frame.isValid() || frame.isActive()) {
			Thread.sleep(100);
		}
	}

}
