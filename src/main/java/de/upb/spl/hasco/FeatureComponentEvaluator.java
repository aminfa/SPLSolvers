package de.upb.spl.hasco;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.ibea.BasicIbea;
import hasco.model.ComponentInstance;
import jaicore.basic.IObjectEvaluator;
import jaicore.basic.algorithm.exceptions.ObjectEvaluationFailedException;

import java.util.concurrent.TimeoutException;

import static de.upb.spl.hasco.HascoSPLReasoner.NAME;

public class FeatureComponentEvaluator implements IObjectEvaluator<ComponentInstance, FeatureSelectionPerformance> {

    private final FM2CM fm2CM;
    private final BenchmarkEnvironment env;

    public FeatureComponentEvaluator(BenchmarkEnvironment env, FM2CM fm2CM) {
        this.fm2CM = fm2CM;
        this.env = env;
    }

    @Override
    public FeatureSelectionPerformance evaluate(ComponentInstance object) throws TimeoutException, InterruptedException, ObjectEvaluationFailedException {
        try {
            FeatureSelection selection = fm2CM.transform(object);
            return new FeatureSelectionPerformance(BasicIbea.evaluateAndCountViolatedConstraints(env, selection, NAME));
        } catch (Exception ex) {
            throw new ObjectEvaluationFailedException(ex, "Couldn't evaluate ComponentInstance");
        }
    }

}
