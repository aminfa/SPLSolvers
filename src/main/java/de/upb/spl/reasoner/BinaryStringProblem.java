package de.upb.spl.reasoner;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import fm.FeatureTreeNode;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.problem.AbstractProblem;

import java.util.List;
import java.util.function.Predicate;

public abstract class BinaryStringProblem extends AbstractProblem {

    private final int binaryStringLength;
    private final List<FeatureTreeNode> features;

    public BinaryStringProblem(int numberOfObjectives, List<FeatureTreeNode> features) {
        super(1, numberOfObjectives);
        this.features = features;
        this.binaryStringLength = features.size();
    }

    public List<FeatureTreeNode> getFeatures() {
        return features;
    }

    public int getBinaryStringLength() {
        return binaryStringLength;
    }

    @Override
    public Solution newSolution() {
        Solution solution = new Solution(1, this.getNumberOfObjectives());
        solution.setVariable(0, new BinaryVariable(features.size()));
        return solution;
    }

    public FeatureSelection assemble(BinaryVariable variable) {
        FeatureSelection selection = FMUtil.selectFromPredicate(getFeatures(), variable::get);
        return selection;
    }

    public BinaryVariable binarize(FeatureSelection selection) {
        int binaryLength = getBinaryStringLength();
        BinaryVariable var = new BinaryVariable(binaryLength);
        Predicate<Integer> predicate = FMUtil.predicateFromSelection(getFeatures(), selection);
        for (int i = 0; i < binaryLength; i++) {
            var.set(i, predicate.test(i));
        }
        return var;
    }

}
