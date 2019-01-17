package de.upb.spl;

import fm.FeatureModel;
import fm.FeatureTreeNode;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

public interface FeatureSelection extends Predicate<FeatureTreeNode>, Iterable<FeatureTreeNode>, Collection<FeatureTreeNode> {
	boolean isSelected(FeatureTreeNode feature);

	@Override
	default boolean test(FeatureTreeNode featureTreeNode) {
		return isSelected(featureTreeNode);
	}

	boolean isSelectionEqual(Iterator<FeatureTreeNode> nodes, FeatureSelection featureSelection);

	boolean isSelected(String featureId);

}
