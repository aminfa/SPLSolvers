package de.upb.spl;

import fm.FeatureModel;
import fm.FeatureTreeNode;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;

public class FeatureSet extends LinkedHashSet<FeatureTreeNode> implements FeatureSelection {

	public FeatureSet(FeatureTreeNode... features) {
		this.addAll(Arrays.asList(features));
	}

	public FeatureSet(FeatureModel fm, String... featuresIds) {
	    this(fm, Arrays.stream(featuresIds).iterator());
	}


    public FeatureSet(FeatureModel fm, Collection<String> featuresIds) {
        this(fm, featuresIds.iterator());
    }

    public FeatureSet(FeatureModel fm, Iterator<String> iterator) {
        while(iterator.hasNext())
        {
            String featureId = iterator.next();
            FeatureTreeNode feature = FMUtil.find(fm, featureId);
            if(feature == null) {
                throw new IllegalArgumentException("Cannot find listFeatures with id " + featureId);
            }
            this.add(feature);
        }
    }





    /**
	 * Copy constructor
	 * @param fm
	 */
	public FeatureSet(FeatureModel fm) {
		for(FeatureTreeNode featureTreeNode : FMUtil.listFeatures(fm)) {
			this.add(featureTreeNode);
		}
	}

	/**
	 * Copy constructor
	 */
	public FeatureSet(Set<FeatureTreeNode> featureSet) {
		super(featureSet);
	}

	@Override
	public boolean isSelected(FeatureTreeNode feature) {
		return contains(feature);
	}

	@Override
	public boolean isSelectionEqual(Iterator<FeatureTreeNode> features, FeatureSelection otherSelection) {
		while(features.hasNext()) {
			FeatureTreeNode feature = features.next();
			if(this.isSelected(feature) != otherSelection.isSelected(feature)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSelected(String featureId) {
		for(FeatureTreeNode selectedFeature : this) {
			if(selectedFeature.getID().equals(featureId)) {
				return true;
			}
		}
		return false;
	}

	public boolean add(FeatureTreeNode newFeauter) {
		return super.add(Objects.requireNonNull(newFeauter, "Feature set doens't permit null features!"));
	}


}
