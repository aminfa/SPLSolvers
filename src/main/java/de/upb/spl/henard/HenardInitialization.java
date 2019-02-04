package de.upb.spl.henard;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkEnvironment;
import de.upb.spl.benchmarks.BenchmarkReport;
import de.upb.spl.guo11.FesTransform;
import fm.FeatureTreeNode;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HenardInitialization implements Initialization {

	private final static Logger logger = LoggerFactory.getLogger("Henard");

	final private BenchmarkEnvironment env;
	final private Integer	 p,  // population size
					     n ; // number of features

	HenardInitialization(BenchmarkEnvironment environment) {
		this.env = environment;
		this.n = FMUtil.countVariantFeatures(env.model());
		this.p = environment.readParameter(Henard.INIT_POPULATION_SIZE);
	}


	@Override
	public Solution[] initialize() {
		Random generator = env.generator();
		List<FeatureTreeNode> featureOrder = FMUtil.optFeatureStream(env.model()).collect(Collectors.toList());
		List<Solution> generatedSolutions = new ArrayList<>();
		List<Future<BenchmarkReport>> reports=  new ArrayList<>();
		for (int i = 0; i < p; i++) {
			Solution solution = new Solution(1, env.objectives().size());
			generatedSolutions.add(solution);
			BinaryVariable binaryString =  new BinaryVariable(FMUtil.countFeatures(env.model()));
			solution.setVariable(0, binaryString);
			for (int j = 0; j < n; j++) {
				double rand = generator.nextDouble();
				boolean gene = false;
				binaryString.set(j, gene);
			}
			FeatureSelection selection = FMUtil.selectFromPredicate(featureOrder, binaryString::get);
			FeatureSelection transformedSelection = new FesTransform(env.model(), selection).getValidSelection();
			Future<BenchmarkReport> report = env.run(transformedSelection);
			reports.add(report);
			Predicate<Integer> finalSelection = FMUtil.predicateFromSelection(featureOrder, transformedSelection);
			for (int j = 0; j < n; j++) {
				binaryString.set(j, finalSelection.test(j));
			}
		}
		for (int i = 0; i < p; i++) {
			Solution solution = generatedSolutions.get(i);
			BenchmarkReport report;
			try {
				report = reports.get(i).get();
			} catch (InterruptedException | ExecutionException | NullPointerException e) {
				double[] failedExecutionResult = new double[env.objectives().size()];
				for (int j = 0; j < failedExecutionResult.length; j++) {
					failedExecutionResult[j] = Double.MAX_VALUE;
				}
				/*
				 * Set to highest possible value to penalize selections with failed benchmark results
				 */
				solution.setObjectives(failedExecutionResult);
				logger.info("Skipped evaluation of a member of the initial population.");
				continue;
			}
			double[] executionResult = new double[env.objectives().size()];
			for (int j = 0; j < executionResult.length; j++) {
				String objectiveName = env.objectives().get(j);
				executionResult[j] = report.readResult(objectiveName).orElse(Double.MIN_VALUE);
				executionResult[j] = executionResult[j] / report.resourceSum();
			}
			logger.info("Successfully Evaluated a member of the initial population.");
			// negate objectives since the result is meant to be maximized but moea always minimizes objectives.
			solution.setObjectives(Vector.negate(executionResult));
		}
		logger.info("Initialization is finished! Population created.");
		return generatedSolutions.toArray(new Solution[0]);
	}
}
