package de.upb.spl;

import fm.FeatureModel;
import fm.FeatureTreeNode;

import java.util.*;
import java.util.function.Predicate;

public interface FeatureSelection extends Predicate<FeatureTreeNode>, Iterable<FeatureTreeNode>, Collection<FeatureTreeNode> {
	boolean isSelected(FeatureTreeNode feature);

	@Override
	default boolean test(FeatureTreeNode featureTreeNode) {
		return isSelected(featureTreeNode);
	}

	default boolean isSelectionEqual(Iterator<FeatureTreeNode> nodes, FeatureSelection featureSelection){
	    throw new UnsupportedOperationException();
    }

	boolean isSelected(String featureId);

	boolean equals(Object o);

	int hashCode();

}
