package de.upb.spl.henard;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import fm.FeatureTreeNode;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.problem.AbstractProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HenardProblemDefinition extends AbstractProblem {
	private final static Logger logger = LoggerFactory.getLogger("Henard");
	final private BenchmarkEnvironment env;
	public static BenchmarkEnvironment global_env;
	private final List<FeatureTreeNode> featureOrder;

	HenardProblemDefinition(BenchmarkEnvironment env) {
		super(0, env.objectives().size());
		this.env = env;
		featureOrder = FMUtil.featureStream(env.model()).collect(Collectors.toList());

		PRNG.setRandom(env().generator());
	}

	public HenardProblemDefinition() {
		this(global_env);
	}

	@Override
	public void evaluate(Solution solution) {
//		BinaryVariable featureSelectionBinaryString = (BinaryVariable) solution.getVariable(0);
//		FeatureSelection assemble = FMUtil.selectFromPredicate(featureOrder, featureSelectionBinaryString::get);
//		if(! FMUtil.isValidSelection(env.model(), assemble)) {
//			logger.info("Evaluation unsuccessful. Selection is not valid");
//			double[] invalidSelection = new double[this.getNumberOfObjectives()];
//			for (int i = 0; i < invalidSelection.length; i++) {
//				invalidSelection[i] = Double.MAX_VALUE;
//			}
//			solution.setObjectives(invalidSelection);
//			return;
//		}
//		BenchmarkReport report;
//		try {
//			report = env.run(assemble).get();
//		} catch (InterruptedException | ExecutionException e) {
//			logger.warn("Couldn't run benchmark for " + assemble + ".", e);
//			report = null;
//		}
//		if(report == null || report.constraintsViolated()) {
//			double[] failedExecutionResult = new double[this.getNumberOfObjectives()];
//			for (int i = 0; i < failedExecutionResult.length; i++) {
//				failedExecutionResult[i] = Double.MAX_VALUE;
//			}
//			/*
//			 * Set to highest possible value to penalize selections with failed benchmark results
//			 */
//			solution.setObjectives(failedExecutionResult);
//		} else {
//			logger.info("Selection successfully evaluated.");
//			double[] executionResult = new double[this.numberOfObjectives];
//			for (int i = 0; i < executionResult.length; i++) {
//				String objectiveName = env.objectives().get(i);
//				executionResult[i] = report.readResult(objectiveName).orElse(Double.MIN_VALUE);
//				logger.info("Selection objective value {} = {}.", objectiveName, executionResult[i]);
//				executionResult[i] = executionResult[i] / report.resourceSum();
//			}
//			// negate objectives since the result is meant to be maximized but moea always minimizes objectives.
//			solution.setObjectives(Vector.negate(executionResult));
//		}
	}

	@Override
	public Solution newSolution() {
		Solution solution = new Solution(1, this.getNumberOfObjectives());
		solution.setVariable(0, new BinaryVariable(FMUtil.countFeatures(env.model())));
		return solution;
	}

	public BenchmarkEnvironment env() {
		return env;
	}

	public List<FeatureTreeNode> order() {
		return featureOrder;
	}


	public FeatureSelection selection(BinaryVariable variable) {
		FeatureSelection selection = FMUtil.selectFromPredicate(order(), variable::get);
		return selection;
	}

	public BinaryVariable binarize(FeatureSelection selection) {
		int bitsize = order().size();
		BinaryVariable var = new BinaryVariable(bitsize);
		Predicate<Integer> predicate = FMUtil.predicateFromSelection(order(), selection);
		for (int i = 0; i < bitsize; i++) {
			var.set(i, predicate.test(i));
		}
		return var;
	}
}