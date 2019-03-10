package de.upb.spl.guo11;

import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.FeatureSelection;
import de.upb.spl.reasoner.BinaryStringProblem;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.core.*;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.variable.BinaryVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Random;

public class GuoAlgorithm extends AbstractEvolutionaryAlgorithm {
	private final static Logger logger = LoggerFactory.getLogger("GUO_11");

	private final BenchmarkEnvironment env;
	private final ParetoDominanceComparator paretoDominanceComparator = new ParetoDominanceComparator();
	/**
	 * Constructs GuoAlgorithm.
	 *
	 * @param problem        the problem being solved
	 * @param initialization the initialization operator
	 */
	public GuoAlgorithm(BenchmarkEnvironment env, Problem problem, Initialization initialization) {
		super(problem, new Population(), new NondominatedPopulation(), initialization);
		this.env = env;
	}

	@Override
	protected void iterate() {
		/*
		 * For each generation, two parent chromosomes are selected randomly in the population.
		 */
		int populationsize = population.size();
		BinaryStringProblem problem = (BinaryStringProblem) getProblem();
		if(populationsize < 2) {
			throw new RuntimeException("Population is too small. Size was: "  + populationsize);
		}
		Random generator =  env.generator();
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

		FeatureSelection newSelection = problem.assemble(newChr);
		FesTransform transform = new FesTransform(env.model(), newSelection, env.generator());
		newChr = problem.binarize(transform.getValidSelection());
        for(Solution members : getPopulation()) {
            if(members.getVariable(0).toString().equals(newChr.toString())) {
                logger.info("Offspring's chromosome `{}` already exists. Skipping evaluation and adding it to the population.", newChr.toString());
                return;
            }
        }
		/*
		 * If the generated offspring is superior to both parents, it replaces the similar parent;
		 * if it is in between the two parents, it replaces the inferior parent;
		 * otherwise, the most inferior chromosome in the population is replaced.
		 */
		Solution offspring = new Solution(1, parent1.getNumberOfObjectives());
		offspring.setVariable(0, newChr);
		evaluate(offspring);
		logger.info("Parent1 {} + Parent2 {} = Offspring {}.", parent1.getObjective(0), parent2.getObjective(0), offspring.getObjective(0));

		boolean parent1Inferior = paretoDominanceComparator.compare(offspring, parent1) < 0;
		boolean parent2Inferior = paretoDominanceComparator.compare(offspring, parent2) < 0;
		if(parent1Inferior && parent2Inferior) {
		    logger.info("Both parents were inferior");
			int difference1 = newChr.hammingDistance(chr1);
			int difference2 = newChr.hammingDistance(chr2);
			if(difference1 > difference2) {
			    logger.info("Removing parent 2.");
				getPopulation().remove(parent2);
                getPopulation().add(offspring);
			} else {
				getPopulation().remove(parent1);
                getPopulation().add(offspring);
                logger.info("Removing parent 1.");
			}
		} else if(parent1Inferior) {
            logger.info("Parent 1 was inferior: {}", parent1.getObjective(0));
			getPopulation().remove(parent1);
            getPopulation().add(offspring);
		} else if(parent2Inferior) {
            logger.info("Parent 2 was inferior: {}", parent2.getObjective(0));
			getPopulation().remove(parent2);
            getPopulation().add(offspring);
		} else {
            logger.info("No parent was inferior.");
			Iterator<Solution> populationIterator = getPopulation().iterator();
			Solution mostInferior = offspring;
			while(populationIterator.hasNext()) {
				Solution otherSolution = populationIterator.next();
				double otherPerformance = otherSolution.getObjective(0);
				double currentlyWorstPerformance = mostInferior.getObjective(0);
				if(currentlyWorstPerformance < otherPerformance) {
					mostInferior = otherSolution;
				}
			}
			if(offspring != mostInferior) {
                logger.info("Offspring is replacing a random member with performance: {}", mostInferior.getObjective(0));
                getPopulation().remove(mostInferior);
                getPopulation().add(offspring);
            } else {
			    logger.info("New offspring is not added to the population.");
            }
		}
		logger.info("Population size is: " + getPopulation().size());
	}
}
