package de.upb.spl.guo11;

import de.upb.spl.FMUtil;
import de.upb.spl.FeatureSelection;
import de.upb.spl.FeatureSet;
import fm.FeatureModel;
import fm.FeatureTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Fe ature s election Transform
 */
public class FesTransform {

	private final static Logger logger = LoggerFactory.getLogger("GUO_11");

	private final FeatureModel fm;
	private final Set<FeatureTreeNode>
			Sv,	// output, a valid feature combination
			Se, // contains all the features that should be excluded
			Sg; //

	private final FeatureSet validFeatureSelection;

	public FesTransform(FeatureModel fm, FeatureSelection featureSelection) {
		this.fm = fm;
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
			boolean childIncluded = false;
			for(int i = 0; i < feature.getChildCount(); i++) {
				FeatureTreeNode child = (FeatureTreeNode) feature.getChildAt(i);
				if(Sv.contains(child)) {
					childIncluded = true;
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
				int randomChildIndex = (int) (Math.random() * childCount);
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
		}
		if(!FMUtil.isRoot(feature)) {
			includeFeature((FeatureTreeNode) feature.getParent());
		}
		if(FMUtil.isInAlternativeGroup(feature)) {
			FeatureTreeNode parent = (FeatureTreeNode) feature.getParent();
			for(int i = 0, siblingcount = parent.getChildCount(); i < siblingcount; i++) {
				FeatureTreeNode sibling = (FeatureTreeNode) parent.getChildAt(i);
				if(sibling != feature) {
					excludeFeature(sibling);
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
			logger.trace("excludeFeature:: Feature " + feature.toString() + " can't be added to Se as "
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
