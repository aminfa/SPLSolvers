package de.upb.spl.ailibsintegration;

import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.hasco.FM2CM;
import de.upb.spl.ibea.BasicIbea;
import hasco.model.ComponentInstance;
import jaicore.basic.IObjectEvaluator;
import jaicore.basic.algorithm.exceptions.ObjectEvaluationFailedException;

import static de.upb.spl.hasco.HASCOSPLReasoner.NAME;

public class FeatureComponentEvaluator implements IObjectEvaluator<ComponentInstance, FeatureSelectionOrdering> {

    private final FM2CM fm2CM;
    private final BenchmarkEnvironment env;

    public FeatureComponentEvaluator(BenchmarkEnvironment env) {
        this.fm2CM = env.componentModel();
        this.env = env;
    }

    @Override
    public FeatureSelectionOrdering evaluate(ComponentInstance object) throws InterruptedException, ObjectEvaluationFailedException {
        try {
            FeatureSelection selection = fm2CM.transform(object);
            return new ParetoDominanceOrdering(BasicIbea.evaluateAndCountViolatedConstraints(env, selection, NAME));
        } catch (Exception ex) {
            throw new ObjectEvaluationFailedException(ex, "Couldn't evaluate ComponentInstance");
        }
    }

}
