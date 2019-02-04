package de.upb.spl.henard;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.BenchmarkEnvironment;
import de.upb.spl.guo11.FesTransform;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.core.*;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.variable.BinaryVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Random;

public class HenardAlgorithm extends AbstractEvolutionaryAlgorithm {
	private final static Logger logger = LoggerFactory.getLogger("Henard");

	private final ParetoDominanceComparator paretoDominanceComparator = new ParetoDominanceComparator();
	/**
	 * Constructs GuoAlgorithm.
	 *
	 * @param problem        the problem being solved
	 * @param initialization the initialization operator
	 */
	public HenardAlgorithm(Problem problem, Initialization initialization) {
		super(problem, new Population(), new NondominatedPopulation(), initialization);
	}

	@Override
	protected void iterate() {
		/*
		 * For each generation, two parent chromosomes are selected randomly in the population.
		 */
		int populationsize = population.size();
		HenardProblemDefinition problem = (HenardProblemDefinition) getProblem();
		if(populationsize < 2) {
			throw new RuntimeException("Population is too small. Size was: "  + populationsize);
		}
		Random generator =  env().generator();
		int parentIndex1, parentIndex2;
		parentIndex1 = generator.nextInt(populationsize);
		/*
		 * dont choose the same parent for parent2:
		 */
		parentIndex2 = generator.nextInt(populationsize -1);
		if(parentIndex1 == parentIndex2) {
			parentIndex2++;
		}
		Solution parent1 = population.get(parentIndex1);
		Solution parent2 = population.get(parentIndex2);
		/*
		 *  uniform crossover operation is used to combine bits sampled uniformly from the two parents:
		 */
		BinaryVariable chr1 = (BinaryVariable) parent1.getVariable(0);
		BinaryVariable chr2 = (BinaryVariable) parent2.getVariable(0);
		int geneCount = chr1.getNumberOfBits();
		/*
		 * the crossover mask is generated as a random bit string with each bit chosen at random and independent of the others.
		 */
		BinaryVariable newChr = new BinaryVariable(geneCount);
		for (int i = 0; i < geneCount; i++) {
			int parentGene = generator.nextInt(2) + 1;
			newChr.set(i, parentGene == 1 ? chr1.get(i) : chr2.get(i));
		}
		/*
		 * The point mutation operation produces small random changes to the bit string by choosing a single bit at random, then changing its value.
		 */
		int flipGeneIndex = generator.nextInt(geneCount);
		newChr.set(flipGeneIndex, ! newChr.get(flipGeneIndex));

		FeatureSelection newSelection = problem.selection(newChr);
		FesTransform transform = new FesTransform(env().model(), newSelection);
		newChr = problem.binarize(transform.getValidSelection());
		/*
		 * If the generated offspring is superior to both parents, it replaces the similar parent;
		 * if it is in between the two parents, it replaces the inferior parent;
		 * otherwise, the most inferior chromosome in the population is replaced.
		 */
		Solution offspring = new Solution(1, parent1.getNumberOfObjectives());
		offspring.setVariable(0, newChr);
		evaluate(offspring);
		boolean parent1Inferior = paretoDominanceComparator.compare(offspring, parent1) < 0;
		boolean parent2Inferior = paretoDominanceComparator.compare(offspring, parent2) < 0;
		if(parent1Inferior && parent2Inferior) {
			int difference1 = newChr.hammingDistance(chr1);
			int difference2 = newChr.hammingDistance(chr2);
			if(difference1 > difference2) {
				getPopulation().remove(parent2);
			} else {
				getPopulation().remove(parent1);
			}
		} else if(parent1Inferior) {
			getPopulation().remove(parent1);
		} else if(parent2Inferior) {
			getPopulation().remove(parent2);
		} else {
			Iterator<Solution> populationIterator = getPopulation().iterator();
			Solution mostInferior = populationIterator.next();
			while(populationIterator.hasNext()) {
				Solution otherSolution = populationIterator.next();
				if(paretoDominanceComparator.compare(mostInferior, otherSolution) > 0) {
					mostInferior = otherSolution;
				}
			}
			getPopulation().remove(mostInferior);
		}
		getPopulation().add(offspring);
	}

	private BenchmarkEnvironment env() {
		return ((HenardProblemDefinition) getProblem()).env();
	}
}
