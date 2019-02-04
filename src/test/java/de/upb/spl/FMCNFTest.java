package de.upb.spl;

import fm.FeatureModel;
import fm.FeatureModelException;
import fm.XMLFeatureModel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.*;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FMCNFTest {
	static FeatureModel featureModel;

	@BeforeClass
	public static void load() throws FeatureModelException {
		String featureModelFile = FMTest.class.getClassLoader().getResource("video_encoder_x264.xml").getPath();
		featureModel = new XMLFeatureModel(featureModelFile, XMLFeatureModel.USE_VARIABLE_NAME_AS_ID);
		// Load the XML file and creates the feature model
		featureModel.loadModel();
	}

	@Test
	public void transformToCNF() throws TimeoutException, ContradictionException {
		FMCNF cnf = FMCNF.transform(featureModel);
//		Assert.assertEquals(23, cnf.getClauses().size());
		Random random = new Random();
		ISolver problem = cnf.getSolver();
		IProblem it = new ModelIterator(problem);
		Assert.assertTrue(problem.isSatisfiable());
		int count = 0;
		List<int[]>  models = new ArrayList<>();
		while(it.isSatisfiable() && count < 1000) {
			count ++;
			int [] model = it.model();
			models.add(model);
//			System.out.println("Selection " + count + ": " +
//					//cnf.toString(new VecInt(model)));
//					Arrays.toString(model));
		}
		System.out.println("Dissimilarity without shuffle: " + FMCNF.dissimilarity(models));

		count = 0;
		models.clear();
		while(count < 1000) {
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
		System.out.println("Dissimilarity with shuffle: " + FMCNF.dissimilarity(models));



		count = 0;
		models.clear();
		ISolver solver = new ModelIterator(cnf.getDiverseSolver(random));
		while(count < 1000) {
			count++;
			if(solver.isSatisfiable()) {
				int[] model = solver.model();
				models.add(model);
			} else {
				break;
			}
		}
		System.out.println("Dissimilarity with order: " + FMCNF.dissimilarity(models));

	}

}
