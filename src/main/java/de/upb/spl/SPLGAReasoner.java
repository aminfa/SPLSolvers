package de.upb.spl;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.problem.AbstractProblem;

import java.util.Objects;

public abstract class SPLGAReasoner extends AbstractProblem {

	public SPLGAReasoner(int numberOfVariables, int numberOfObjectives) {
		super(numberOfVariables, numberOfObjectives);
	}
}
