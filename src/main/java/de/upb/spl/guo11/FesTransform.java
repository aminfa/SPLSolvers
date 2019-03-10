package de.upb.spl.guo11;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.FeatureSet;
import fm.FeatureModel;
import fm.FeatureTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Fe ature s election Transform
 */
public class FesTransform {

	private final static Logger logger = LoggerFactory.getLogger(FesTransform.class);

	private final FeatureModel fm;
	private final Set<FeatureTreeNode>
			Sv,	// output, a valid listFeatures combination
			Se, // contains all the features that should be excluded
			Sg; //

	private final FeatureSet validFeatureSelection;
	private final Random random;

	public FesTransform(FeatureModel fm, FeatureSelection featureSelection, Random random) {
		this.fm = fm;
		this.random = random;
		Sv = new HashSet<>();
		Se = new HashSet<>();
		Sg = new HashSet<>();

		for(FeatureTreeNode feature : featureSelection) {
			if(!Sv.contains(feature) && !Se.contains(feature)){
				includeFeature(feature);
			}
		}
		/*
		 * Collect features whose children are not contained in Sv:
		 */
		for(FeatureTreeNode feature : Sv) {
			boolean childIncluded = true;
			for(int i = 0; i < feature.getChildCount(); i++) {
				FeatureTreeNode child = (FeatureTreeNode) feature.getChildAt(i);
				if(!Sv.contains(child)) {
					childIncluded = false;
					break;
				}
			}
			if(!childIncluded) {
				Sg.add(feature);
			}
		}
		/*
		 *
		 */
		for(FeatureTreeNode feature : Sg) {
			int childCount = feature.getChildCount();
			if(feature.getChildCount() > 0) {
				// include a random child
				int randomChildIndex = random.nextInt(childCount);
				FeatureTreeNode child = (FeatureTreeNode) feature.getChildAt(randomChildIndex);
				includeFeature(child);
			}
		}

		validFeatureSelection = new FeatureSet(Sv);
		FMUtil.addImpliedFeatures(fm, validFeatureSelection);
	}

	private void includeFeature(FeatureTreeNode feature) {
		if(!Sv.contains(feature) && !Se.contains(feature)){
			Sv.add(feature);
		} else {
			logger.trace("includeFeature:: Feature " + feature.toString() + " can't be added to Sv as "
					+ (Sv.contains(feature)?" it is already included." : " it is already excluded."));
			return;
		}
		if(!FMUtil.isRoot(feature)) {
			includeFeature((FeatureTreeNode) feature.getParent());
		}
		if(FMUtil.isInAlternativeGroup(feature)) {
			FeatureTreeNode parent = (FeatureTreeNode) feature.getParent();
			for(FeatureTreeNode sibling : FMUtil.children(parent)) {
				if(sibling != feature) {
					excludeFeature(sibling);
				}
			}
		}

		if(! FMUtil.isAlternativeGroup(feature)){
			for(FeatureTreeNode child : FMUtil.children(feature)) {
				if(!FMUtil.isOptionalFeature(child)) {
					includeFeature(child);
				}
			}
		}

		for(FeatureTreeNode excludedFeature : FMUtil.crosstreeExclusion(fm, feature)) {
			excludeFeature(excludedFeature);
		}

		for(FeatureTreeNode impliedFeature : FMUtil.crosstreeConclusions(fm, feature)) {
			includeFeature(impliedFeature);
		}

	}

	private void excludeFeature(FeatureTreeNode feature) {
		if(FMUtil.isRoot(feature)) {
			throw new IllegalArgumentException("Cannot exclude root!");
		}
		if(!Sv.contains(feature) && !Se.contains(feature)){
			Se.add(feature);
		} else {
			if(logger.isTraceEnabled()) logger.trace("excludeFeature:: Feature " + feature.toString() + " can't be added to Se as "
					+ (Sv.contains(feature) ? " it is already included." : " it is already excluded."));
//			return;
		}
		for(FeatureTreeNode childFeature : FMUtil.children(feature)) {
			if(Se.contains(childFeature)) {
				continue;
			}
			excludeFeature(childFeature);
		}
		if(!FMUtil.isOptionalFeature(feature)) {
			excludeFeature((FeatureTreeNode) feature.getParent());
		}
		for(FeatureTreeNode impliedBy : FMUtil.crosstreePremises(fm, feature)) {
			excludeFeature(impliedBy);
		}
	}

	public String toString() {
		return "FesTransform \n\tSv:{ " + Sv.toString() + " }\n\tSe:{ " + Se.toString() + " }\n\tOut:{ " + validFeatureSelection + " }\n";
	}

	public FeatureSelection getValidSelection() {
		return validFeatureSelection;
	}
}
