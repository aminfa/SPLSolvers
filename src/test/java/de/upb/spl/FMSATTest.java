package de.upb.spl;

import de.upb.spl.benchmarks.BenchmarkReport;
import de.upb.spl.benchmarks.env.AbstractBenchmarkEnv;
import de.upb.spl.benchmarks.env.AttributedFeatureModelEnv;
import de.upb.spl.hierons.NovelRepresentation;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.XMLFeatureModel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FMSATTest {
	static FeatureModel featureModel;

	@BeforeClass
	public static void load() throws FeatureModelException {
		String featureModelFile = FMTest.class.getClassLoader().getResource("video_encoder_simple.xml").getPath();
		featureModel = new XMLFeatureModel(featureModelFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
		// Load the XML file and creates the listFeatures model
		featureModel.loadModel();
	}

	@Test
	public void cnfDissimilarityTest() throws TimeoutException, ContradictionException {
		FMSAT cnf = FMSAT.transform(featureModel);
//		Assert.assertEquals(23, cnf.getClauses().size());
		Random random = new Random();
		ISolver problem = cnf.getSolver();
		IProblem it = new ModelIterator(problem);
		Assert.assertTrue(problem.isSatisfiable());
		int count = 0;
		List<int[]>  models = new ArrayList<>();
		while(it.isSatisfiable() && count < 10000) {
			count ++;
			int [] model = it.model();
			models.add(model);
//			System.out.println("Selection " + count + ": " +
//					//cnf.toString(new VecInt(model)));
//					Arrays.toString(model));
		}
		System.out.println("Dissimilarity without shuffle: " + FMSAT.dissimilarity(models));

		count = 0;
		models.clear();
		while(count < 10000) {
			count++;
			cnf.shuffleClausesInPlace(random);
			cnf.shuffleLiteralsInPlace(random);
			IProblem problem1 = cnf.getSolver();
			ISolver solver = ((ISolver) problem1);
			for (int[] ints : models) {
				VecInt vecInt = new VecInt(ints);
				solver.addBlockingClause(vecInt);
			}
			if(problem1.isSatisfiable()) {
				int[] model = problem1.model();
				models.add(model);
			} else {
				break;
			}
		}
		System.out.println("Dissimilarity with shuffle: " + FMSAT.dissimilarity(models));



		count = 0;
		models.clear();
		ISolver solver = new ModelIterator(cnf.getDiverseSolver(random));
		while(count < 10000) {
			count++;
			if(solver.isSatisfiable()) {
				int[] model = solver.model();
				models.add(model);
			} else {
				break;
			}
		}
		System.out.println("Dissimilarity with order: " + FMSAT.dissimilarity(models));

	}





}
