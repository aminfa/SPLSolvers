package de.upb.spl.henard;

import de.upb.spl.FMSAT;
import de.upb.spl.FeatureSelection;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.Variation;
import org.moeaframework.core.variable.BinaryVariable;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.function.Function;

public class SmartMutation implements Variation {

    private final VecInt literalsOrder;
    private final FMSAT fmsat;
    private final Function<BinaryVariable, FeatureSelection> assembler;
    private final double probability;

    public SmartMutation(VecInt literalsOrder, FMSAT fmsat, Function<BinaryVariable, FeatureSelection> assembler, double probability) {
        this.literalsOrder = literalsOrder;
        this.fmsat = fmsat;
        this.assembler = assembler;
        this.probability = probability;
    }

    public double getProbability() {
        return this.probability;
    }

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
        VecInt model = new VecInt();
        for (int i = 0; i < variable.getNumberOfBits(); i++) {
            int literal = literalsOrder.get(i);
            if(variable.get(i)) {
                model.push(literal);
            }
        }
        VecInt blacklistedFeatures = fmsat.violatingFeatures(assembler.apply(variable));
        for (int i = 0; i < blacklistedFeatures.size(); i++) {
            int index = model.indexOf(blacklistedFeatures.get(i));
            if(index != -1) {
                model.delete(index);
            }
        }
        ISolver solver = fmsat.getDiverseSolver(PRNG.getRandom());
        solver.setTimeoutMs(10000);
        try {
            if (solver.isSatisfiable(model)) {
                int[] corrected = solver.findModel(model);
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