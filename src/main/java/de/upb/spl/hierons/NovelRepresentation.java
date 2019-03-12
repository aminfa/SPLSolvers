package de.upb.spl.hierons;

import de.upb.spl.FMSAT;
import de.upb.spl.FMSatUtil;
import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.benchmarks.env.BenchmarkEnvironment;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.reasoner.BinaryStringProblem;
import fm.FeatureModel;
import fm.FeatureTreeNode;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.sat4j.core.VecInt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NovelRepresentation {

    private final List<FeatureTreeNode> coreFeatures;

    private final List<FeatureTreeNode> featureOrder;

    private final List<FeatureTreeNode> hierarchicalFeatures;

    private final VecInt literalOrder;

    private final BenchmarkEnvironment env;

    private final String name;

    private final Problem problem;

    public NovelRepresentation(BenchmarkEnvironment env, String name) {
        this.env = env;
        this.name = name;
        FeatureModel fm = env.model();
        FMSAT fmsat = env.sat();
        // all features
        featureOrder = new ArrayList<>(FMUtil.listFeatures(fm));
        // core features
        coreFeatures = new ArrayList<>();
        // remove core features
        VecInt unitLiterals = FMSatUtil.unitLiterals(fmsat);
        FMSatUtil.toFeature(fm, fmsat, unitLiterals).forEach(feature-> {
            coreFeatures.add(feature);
            featureOrder.remove(feature);
        });
        // remove hierarchical features
        hierarchicalFeatures = new ArrayList<>();
        Iterator<FeatureTreeNode> it = featureOrder.iterator();
        while(it.hasNext()) {
            FeatureTreeNode feature = it.next();
            if(FMUtil.isHierarchicalFeature(feature)){
                hierarchicalFeatures.add(feature);
                it.remove();
            }
        }

        literalOrder = new VecInt(featureOrder.size());
        for(FeatureTreeNode feature : featureOrder) {
            int literal = fmsat.toLiteral(feature);
            literalOrder.push(literal);
        }

        this.problem = new Problem(1 + env.objectives().size());
    }

    public Problem getProblem() {
        return problem;
    }

    public List<FeatureTreeNode> featureOrder() {
        return featureOrder;
    }

    public VecInt literalOrder() {
        return literalOrder;
    }

    public class Problem extends BinaryStringProblem {

        public Problem(int numberOfObjectives) {
            super(numberOfObjectives, featureOrder);
        }

        @Override
        public void evaluate(Solution solution) {
            FeatureSelection selection =  assemble((BinaryVariable) solution.getVariable(0));
            solution.setObjectives(BasicIbea.evaluateAndCountViolatedConstraints(env,
                    selection,
                    name));
        }

        public FeatureSelection assemble(BinaryVariable binaryVariable) {
            FeatureSelection selection =  super.assemble(binaryVariable);
            augment(selection);
            return selection;
        }

    }

    public void augment(FeatureSelection selection) {
        selection.addAll(coreFeatures);
        boolean featureAdded = true;
        List<FeatureTreeNode> hierarchicalFeatures = new ArrayList<>(this.hierarchicalFeatures);
        while (featureAdded) {
            featureAdded = false;
            Iterator<FeatureTreeNode> it = hierarchicalFeatures.iterator();
            while (it.hasNext()) {
                FeatureTreeNode feature = it.next();
                for (FeatureTreeNode child : FMUtil.children(feature)) {
                    if (selection.isSelected(child)) {
                        featureAdded = selection.add(feature) || featureAdded;
                        it.remove();
                        break;
                    }
                }
            }
        }
    }
}
