package de.upb.spl.guo11;

import de.upb.spl.BenchmarkEnvironment;
import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import fm.FeatureTreeNode;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GuoInitialization implements Initialization {

	final private BenchmarkEnvironment env;
	final private Double d;  // [0, 1] parameter d determines expected number of selected features in every generated chromosome.
	final private Integer	 p,  // population size
					     n ; // number of features

	GuoInitialization(BenchmarkEnvironment environment) {
		this.env = environment;
		this.n = FMUtil.countVariantFeatures(env.model());
		this.d = environment.readParameter(Guo11.INIT_D);
		this.p = environment.readParameter(Guo11.INIT_POPULATION_SIZE);
	}


	@Override
	public Solution[] initialize() {
		Random generator = env.generator();
		List<FeatureTreeNode> featureOrder = FMUtil.optFeatureStream(env.model()).collect(Collectors.toList());
		List<Solution> generatedSolutions = new ArrayList<>();
		for (int i = 0; i < p; i++) {
			Solution solution = new Solution(1, env.objectives().size());
			generatedSolutions.add(solution);
			BinaryVariable binaryString =  new BinaryVariable(FMUtil.countFeatures(env.model()));
			solution.setVariable(0, binaryString);
			for (int j = 0; j < n; j++) {
				double rand = generator.nextDouble();
				boolean gene = false;
				if(rand < d) {
					gene = true;
				}
				binaryString.set(j, gene);
			}
			FeatureSelection selection = FMUtil.selectFromPredicate(featureOrder, binaryString::get);
			FeatureSelection transformedSelection = new FesTransform(env.model(), selection).getValidSelection();
			Predicate<Integer> finalSelection = FMUtil.predicateFromSelection(featureOrder, transformedSelection);
			for (int j = 0; j < n; j++) {
				binaryString.set(j, finalSelection.test(j));
			}
		}
		return generatedSolutions.toArray(new Solution[0]);
	}
}
