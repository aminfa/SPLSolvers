package de.upb.spl.henard;

import de.upb.spl.FMSAT;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.Variation;
import org.moeaframework.core.variable.BinaryVariable;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.Random;

public class SmartReplacement implements Variation {

    private final VecInt literalsOrder;

    private final FMSAT fmsat;
    private final double probability;

    public SmartReplacement(VecInt literalsOrder, FMSAT fmsat, double probability) {
        this.literalsOrder = literalsOrder;
        this.fmsat = fmsat;
        this.probability = probability;
    }

    @Override
    public Solution[] evolve(Solution[] parents) {
        Solution result = parents[0];

        for(int i = 0; i < result.getNumberOfVariables(); ++i) {
            Variable variable = result.getVariable(i);
            if (variable instanceof BinaryVariable) {
                evolve((BinaryVariable)variable, this.probability);
            }
        }

        return new Solution[]{result};
    }

    public void evolve(BinaryVariable variable, double probability) {
        if (PRNG.nextDouble() > probability) {
            return;
        }
        ISolver solver = fmsat.getDiverseSolver(PRNG.getRandom());
        solver.setTimeoutMs(10000);
        try {
            if (solver.isSatisfiable()) {
                int[] corrected = solver.findModel();
                for (int j = 0; j < literalsOrder.size(); j++) {
                    int literal = literalsOrder.get(j);
                    variable.set(j, corrected[literal-1] > 0);
                }
            }
        } catch (TimeoutException e) {
            return;
        }
    }

    @Override
    public int getArity() {
        return 1;
    }
}
