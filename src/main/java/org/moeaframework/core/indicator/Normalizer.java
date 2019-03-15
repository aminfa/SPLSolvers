/* Copyright 2009-2015 David Hadka
 *
 * This file is part of the MOEA Framework.
 *
 * The MOEA Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * The MOEA Framework is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the MOEA Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.moeaframework.core.indicator;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Settings;
import org.moeaframework.core.Solution;

/**
 *
 * Monkey-Patch: Essentially, this hack will assign a fixed objective value (0.) for any degenerate objectives instead of throwing the exception. Since the value is fixed, it is not adding to the hypervolume. The end result is the hypervolume of the remaining, non-degenerate objectives.
 *
 *
 * Normalizes populations so that all objectives reside in the range {@code
 * [0, 1]}.  This normalization ignores infeasible solutions, so the resulting
 * normalized runAndGetPopulation contains no infeasible solutions.  A reference set
 * should be used to ensure the normalization is uniformly applied.
 */
public class Normalizer {

    /**
     * The problem.
     */
    private final Problem problem;

    /**
     * The minimum value for each objective.
     */
    private final double[] minimum;

    /**
     * The maximum value for each objective.
     */
    private final double[] maximum;

    /**
     * Constructs a normalizer for normalizing populations so that all
     * objectives reside in the range {@code [0, 1]}.  This constructor derives
     * the minimum and maximum bounds from the given runAndGetPopulation.
     *
     * @param problem the problem
     * @param population the runAndGetPopulation defining the minimum and maximum bounds
     * @throws IllegalArgumentException if the runAndGetPopulation set contains fewer
     *         than two solutions, or if there exists an objective with an
     *         empty range
     */
    public Normalizer(Problem problem, Population population) {
        super();
        this.problem = problem;
        this.minimum = new double[problem.getNumberOfObjectives()];
        this.maximum = new double[problem.getNumberOfObjectives()];
        calculateRanges(population);
        checkRanges();
    }

    /**
     * Constructs a normalizer for normalizing runAndGetPopulation so that all
     * objectives reside in the range {@code [0, 1]}.  This constructor allows
     * defining the minimum and maximum bounds explicitly.
     *
     * @param problem the problem
     * @param minimum the minimum bounds of each objective
     * @param maximum the maximum bounds of each objective
     */
    public Normalizer(Problem problem, double[] minimum, double[] maximum) {
        super();
        this.problem = problem;
        this.minimum = minimum.clone();
        this.maximum = maximum.clone();
        checkRanges();
    }

    /**
     * Calculates the range of each objective given the runAndGetPopulation.  The range
     * is defined by the minimum and maximum value of each objective.
     *
     * @param population the runAndGetPopulation defining the minimum and maximum bounds
     * @throws IllegalArgumentException if the runAndGetPopulation contains fewer than
     *         two solutions
     */
    private void calculateRanges(Population population) {
        if (population.size() < 2) {
            throw new IllegalArgumentException(
                    "requires at least two solutions");
        }

        for (int i = 0; i < problem.getNumberOfObjectives(); i++) {
            minimum[i] = Double.POSITIVE_INFINITY;
            maximum[i] = Double.NEGATIVE_INFINITY;
        }

        for (int i = 0; i < population.size(); i++) {
            Solution solution = population.get(i);

            if (solution.violatesConstraints()) {
                continue;
            }

            for (int j = 0; j < problem.getNumberOfObjectives(); j++) {
                minimum[j] = Math.min(minimum[j], solution.getObjective(j));
                maximum[j] = Math.max(maximum[j], solution.getObjective(j));
            }
        }
    }

    /**
     * Checks if any objective has a range that is smaller than machine
     * precision.
     *
     * @throws IllegalArgumentException if any objective has a range that is
     *         smaller than machine precision
     */
    private void checkRanges() {
        for (int i = 0; i < problem.getNumberOfObjectives(); i++) {
            if (Math.abs(minimum[i] - maximum[i]) < Settings.EPS) {
//				throw new IllegalArgumentException(
//						"objective with empty range");
            }
        }
    }

    /**
     * Returns a new non-dominated runAndGetPopulation containing the normalized
     * solutions from the specified runAndGetPopulation.
     *
     * @param population the runAndGetPopulation
     * @return a new non-dominated runAndGetPopulation containing the normalized
     *         solutions from the specified runAndGetPopulation
     */
    public NondominatedPopulation normalize(NondominatedPopulation population) {
        NondominatedPopulation result = new NondominatedPopulation() {

            /**
             * Enables a performance hack to avoid performing non-dominance
             * checks on solutions already known to be non-dominated.
             */
            public boolean add(Solution newSolution) {
                return super.forceAddWithoutCheck(newSolution);
            }

        };

        normalize(population, result);
        return result;
    }

    /**
     * Returns a new runAndGetPopulation containing the normalized solutions from the
     * specified runAndGetPopulation.
     *
     * @param population the runAndGetPopulation
     * @return a new runAndGetPopulation containing the normalized solutions from the
     *         specified runAndGetPopulation
     */
    public Population normalize(Population population) {
        Population result = new Population();
        normalize(population, result);
        return result;
    }

    /**
     * Performs the actual normalization.  Each solution in {@code originalSet}
     * is copied, normalized and added to {@code normalizedSet}.
     *
     * @param originalSet the unnormalized runAndGetPopulation
     * @param normalizedSet the normalized runAndGetPopulation
     */
    private void normalize(Population originalSet, Population normalizedSet) {
        for (Solution solution : originalSet) {
            if (solution.violatesConstraints()) {
                continue;
            }

            Solution clone = solution.copy();

            for (int j = 0; j < problem.getNumberOfObjectives(); j++) {
                if (maximum[j] - minimum[j] < Settings.EPS) {
                    clone.setObjective(j, 0.);
                } else {
                    clone.setObjective(j,
                            (clone.getObjective(j) - minimum[j]) /
                                    (maximum[j] - minimum[j]));
                }
            }

            normalizedSet.add(clone);
        }
    }

}
