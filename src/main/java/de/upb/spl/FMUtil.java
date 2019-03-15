package de.upb.spl;

import constraints.BooleanVariable;
import constraints.PropositionalFormula;
import fm.*;
import de.upb.spl.util.Cache;
import de.upb.spl.util.DefaultMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FMUtil {

	private final static DefaultMap<FeatureModel, FeatureModelCache> cache = new DefaultMap<FeatureModel, FeatureModelCache>(FeatureModelCache::new);

	private static class FeatureModelCache {

		final Cache<List<FeatureTreeNode>> features;
		final Cache<List<FeatureTreeNode>> optionalNodes;
		final Cache<List<CrossTreeConstraint>> ctcs;
		final DefaultMap<FeatureTreeNode, List<FeatureTreeNode>> premisses;
		final DefaultMap<FeatureTreeNode, List<FeatureTreeNode>> exclusions;
		final DefaultMap<FeatureTreeNode, List<FeatureTreeNode>> conclusions;

		FeatureModelCache(FeatureModel fm) {
		    {
				ctcs = new Cache<>( () -> {
                    List<CrossTreeConstraint> list = new ArrayList<>();
                    for (PropositionalFormula formula : fm.getConstraints()) {
                        CrossTreeConstraint interpretation = ctc(fm, formula);
                        list.add(interpretation);
                    }
                    return list;
                });

				/*
				 * CROSS TREE CONSTRAINTS
				 */

				premisses = new DefaultMap<FeatureTreeNode, List<FeatureTreeNode>>(feature-> {
					List<FeatureTreeNode> impliedByFeatures = new ArrayList<>();

					for(CrossTreeConstraint interpretation : ctcs.get()){
						if(interpretation.isConclusion(feature)) {
							// implication:
							impliedByFeatures.add(interpretation.feature1);
						}
					}
					return impliedByFeatures;
				});

				exclusions = new DefaultMap<FeatureTreeNode, List<FeatureTreeNode>>(feature-> {
					List<FeatureTreeNode> excludedFeatures = new ArrayList<>();
					for(CrossTreeConstraint interpretation : ctcs.get()){
						if(interpretation.isExclusion(feature)) {
							// exclusion:
							excludedFeatures.add(interpretation.other(feature));
						}
					}
					return excludedFeatures;
				});
				conclusions = new DefaultMap<FeatureTreeNode, List<FeatureTreeNode>>(feature-> {
					List<FeatureTreeNode> impliedFeatures = new ArrayList<>();
					for (CrossTreeConstraint interpretation : ctcs.get()) {
						if (interpretation.isPremiss(feature)) {
							// implication:
							impliedFeatures.add(interpretation.feature2);
						}
					}
					return impliedFeatures;
				});
			}

			features = new Cache<>( () -> {
			    List<FeatureTreeNode> features = new ArrayList<>();
                new FeatureModelIterable(fm).forEachRemaining(features::add);
                return features;
            });

			optionalNodes = new Cache<>( () ->
                    features.get().stream().filter(this::isImpliedFeature).collect(Collectors.toList()));
		}




//
//		boolean isSayyadUnfixed(FeatureTreeNode feature) {
//		    if(feature.isRoot()) {
//		        return false;
//            }
//		    if(!FMUtil.isNonMandatoryFeature(feature) && !isSayyadUnfixed((FeatureTreeNode) feature.getParent())) {
//		        return false;
//            }
//            List<FeatureTreeNode> impliedFeatures = conclusions.get(feature);
//            List<FeatureTreeNode> impliedByFeatures = premisses.get(feature);
//		    for(FeatureTreeNode premiss : impliedByFeatures) {
//                if(!impliedFeatures.contains(premiss) && !isSayyadUnfixed(premiss)) {
//                    return false;
//                }
//            }
//		    return true;
//        }
		boolean isImpliedFeature(FeatureTreeNode feature) {
			/*
			 * If there is a cross tree constraint `a->b` with b==listFeatures and a is NOT optional then b is also not optional:
			 * (if there is another contraint `b->a`, the recursion will loop endlessly. To shield against that it is checked if 'b->a' exists.)
			 *
			 */
			List<FeatureTreeNode> impliedFeatures = conclusions.get(feature);
			List<FeatureTreeNode> impliedByFeatures = premisses.get(feature);
			for(FeatureTreeNode premiss : impliedByFeatures) {
				if(!impliedFeatures.contains(premiss) && !isImpliedFeature(premiss)) {
					return true;
				}
			}
			/*
			 * If listFeatures is optional or in a alternative/or-group it is not implied.
			 */
			return isNonMandatoryFeature(feature);
		}


    }
    private static class FeatureModelIterable implements Iterator<FeatureTreeNode> {
		FeatureTreeNode current;

		FeatureModelIterable(FeatureModel model) {
			this.current = model.getRoot();
		}

		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public FeatureTreeNode next() {
			FeatureTreeNode toBeReturned = this.current;

			FeatureTreeNode priorNode = this.current;
			if(priorNode.getChildCount() > 0) {
				this.current = (FeatureTreeNode) priorNode.getChildAt(0);
			} else {
				// recurse:
				FeatureTreeNode parent = (FeatureTreeNode) priorNode.getParent();
				while(parent != null) {
					FeatureTreeNode nextChild = (FeatureTreeNode) parent.getChildAfter(priorNode);
					if(nextChild != null) {
						this.current = nextChild;
						break;
					} else {
						// currently at a last child. go one step up:
						priorNode = parent;
						parent = (FeatureTreeNode) priorNode.getParent();
					}
				}
				if(parent == null) {
					// iteration is over:
					this.current = null;
				}
			}
			return toBeReturned;
		}


    }
    /**
	 * if implication is true:
	 *  	feature1 implies feature2
	 * else:
	 * 		feature1 and feature2 exclude each other
	 *
	 *
	 */
	public static class CrossTreeConstraint {
        public final FeatureTreeNode feature1, feature2;
        public final boolean implication;
        CrossTreeConstraint(FeatureTreeNode feature1, FeatureTreeNode feature2, boolean implication) {
			this.feature1 = feature1;
			this.feature2 = feature2;
			this.implication = implication;
		}
		FeatureTreeNode other(FeatureTreeNode feature) {
			if(feature1 == feature) {
				return feature2;
			} else if(feature2 == feature) {
				return feature1;
			} else {
				throw new IllegalArgumentException("The supplied listFeatures isn't contained in this constraint.");
			}
		}

		boolean isPremiss(FeatureTreeNode feature) {
			return implication && feature == feature1;
		}

		boolean isConclusion(FeatureTreeNode feature) {
			return implication && feature == feature2;
		}

        boolean isExclusion(FeatureTreeNode feature) {
			return !implication && (feature == feature1 || feature == feature2);
		}
    }
    public static FeatureSelection selectFromPredicate(List<FeatureTreeNode> features, Predicate<Integer> selection) {
		FeatureSet set = new FeatureSet();
		for (int i = 0, size = features.size(); i < size; i++) {
			if(selection.test(i)) {
				FeatureTreeNode feature = features.get(i);
				set.add(feature);
			}
		}
		return set;
	}
	public static Predicate<Integer> predicateFromSelection(List<FeatureTreeNode> features, FeatureSelection selection) {
		return i -> selection.isSelected(features.get(i));
	}


	public static List<FeatureTreeNode> listFeatures(FeatureModel model){
		return cache.get(model).features.get();
	}



//	public static List<FeatureTreeNode> listUnfixedFeatures(FeatureModel model) {
//        return cache.get(model).sayyadUnfixedFeatures;
//    }
	public static Stream<FeatureTreeNode> featureStream(FeatureModel model){
		return cache.get(model).features.get().stream();
	}


//	public static Stream<FeatureTreeNode> unfixedFeatureStream(FeatureModel model){
//		return cache.get(model).sayyadUnfixedFeatures.stream();
//	}
	public static List<CrossTreeConstraint> crossTreeConstraints(FeatureModel model) {
		return  cache.get(model).ctcs.get();
	}

	public static int countFeatures(FeatureModel model){
		return cache.get(model).features.get().size();
	}

	public static int countVariantFeatures(FeatureModel model){
		return cache.get(model).optionalNodes.get().size();
	}

	public static boolean isRoot(FeatureTreeNode feature) {
		return feature.getParent() == null;
	}

	public static boolean isAlternativeGroup(FeatureTreeNode feature) {
		if(feature instanceof FeatureGroup) {
			return ((FeatureGroup) feature).getMin() == 1 && ((FeatureGroup)feature).getMax() == 1;
		} else {
			return false;
		}
	}

	public static boolean isInAlternativeGroup(FeatureTreeNode feature) {
		if(isRoot(feature)) {
			return false;
		}
		return isAlternativeGroup((FeatureTreeNode) feature.getParent());
	}

	public static boolean isOrGroup(FeatureTreeNode feature) {
		if(feature instanceof FeatureGroup) {
			return ((FeatureGroup) feature).getMin() == 1 && ((FeatureGroup)feature).getMax() == -1;
		} else {
			return false;
		}
	}

	public static boolean isInOrGroup(FeatureTreeNode feature) {
		if(isRoot(feature)) {
			return false;
		}
		FeatureTreeNode parent = (FeatureTreeNode) feature.getParent();
		return isOrGroup(parent);
	}

	public static boolean isImpliedFeature(FeatureModel fm, FeatureTreeNode feature){
		return !cache.get(fm).optionalNodes.get().contains(feature);
	}

    public static boolean isHierarchicalFeature(FeatureTreeNode feature) {
        for(FeatureTreeNode child : children(feature)) {
            if(!isOptionalFeature(child)) {
                return true;
            }
        }
        return false;
    }


	public static boolean isNonMandatoryFeature(FeatureTreeNode feature) {
		/*
		 * If listFeatures is optional or in a alternative/or-group it is optional.
		 */
		return !isMandatoryFeature(feature);
	}


    public static boolean isOptionalFeature(FeatureTreeNode feature) {
        return feature instanceof SolitaireFeature && ((SolitaireFeature)feature).isOptional();
    }


    public static boolean isMandatoryFeature(FeatureTreeNode feature) {
        return
                (feature instanceof SolitaireFeature && !((SolitaireFeature)feature).isOptional()) || feature instanceof FeatureGroup;
    }

	public static CrossTreeConstraint ctc(FeatureModel fm, PropositionalFormula formula) {
		Iterator<BooleanVariable> it = formula.getVariables().iterator();
		BooleanVariable var1 = it.next();
		BooleanVariable var2 = it.next();
		boolean implication = var1.isPositive() || var2.isPositive();
		FeatureTreeNode feature1 = fm.getNodeByID(var1.getID());
		FeatureTreeNode feature2 = fm.getNodeByID(var2.getID());
		if(var1.isPositive()) {
			FeatureTreeNode tmp = feature1;
			feature1 = feature2;
			feature2 = tmp;
		}
		return new CrossTreeConstraint(feature1, feature2, implication);
	}


	public static void checkModel(FeatureModel model) {
		for(PropositionalFormula formula : model.getConstraints()) {
			if(formula.getVariables().size() != 2) {
				throw new IllegalArgumentException("ILLEGAL CONSTRAINT, " +
						"only constraints with 2 variables are allowed: " + formula.toString());
			}
			boolean negativ = false;
			for(BooleanVariable var : formula.getVariables()) {
				if(!var.isPositive()) {
					negativ = true;
				}
			}
			if(! negativ) {
				throw new IllegalArgumentException("ILLEGAL CONSTRAINT, " +
						"at least one variable needs to be negative in each constraint: " + formula.toString());
			}
		}
		for(FeatureTreeNode feature : listFeatures(model)) {
			if(feature instanceof FeatureGroup) {
				RuntimeException illegalGroupCardinality = new IllegalArgumentException("ILLEGAL GROUP CARDINALITY, " +
						"only alternative and or groups are allowed: " + feature.toString());
				FeatureGroup group = (FeatureGroup) feature;
				if(group.getMin() != 1) {
					throw illegalGroupCardinality;
				} else if(group.getMax() != -1 && group.getMax() == 1) {
					throw illegalGroupCardinality;
				}
			}
		}
	}


	public static List<FeatureTreeNode> crosstreeConclusions(FeatureModel model, FeatureTreeNode feature) {
		return cache.get(model).conclusions.get(feature);
	}

	public static List<FeatureTreeNode> crosstreePremises(FeatureModel model, FeatureTreeNode feature) {
		return cache.get(model).premisses.get(feature);
	}

	public static List<FeatureTreeNode> crosstreeExclusion(FeatureModel model, FeatureTreeNode feature) {
		return cache.get(model).exclusions.get(feature);
	}



	public static Iterable<FeatureTreeNode> children(FeatureTreeNode feature) {
		final int childCount = feature.getChildCount();
		return () -> new Iterator<FeatureTreeNode>() {
			int index = 0;
			@Override
			public boolean hasNext() {
				return index < childCount;
			}

			@Override
			public FeatureTreeNode next() {
				index++;
				return (FeatureTreeNode) feature.getChildAt(index - 1);
			}
		};
	}

	public static void addImpliedFeatures(FeatureModel fm, FeatureSelection selection) {
		for(CrossTreeConstraint interpretation : crossTreeConstraints(fm)){
			/*
			 * if implication is true:
			 *  	feature1 implies feature2
			 * else:
			 * 		feature1 and feature2 exclude each other
			 *
			 * we are only interested in the implication:
			 */
			if(interpretation.implication && selection.isSelected(interpretation.feature1)) {
				selection.add(interpretation.feature2);
			}
		}
		selection.add(fm.getRoot()); // root is always implied
		boolean newFeatureSelected = true;
		while(newFeatureSelected) {
			newFeatureSelected = false;
			for(FeatureTreeNode feature : listFeatures(fm)) { // the iterator guarantees that parents are visited before their children
				if(selection.isSelected(feature)) {
					{
						/*
						 * Add parent:
						 */
						FeatureTreeNode parent = (FeatureTreeNode) feature.getParent();
						if(parent != null) {
							newFeatureSelected = selection.add(parent) || newFeatureSelected;
						}
					}
					{
						/*
						 * Add non optional children:
						 */
						for(FeatureTreeNode child : FMUtil.children(feature)) {
							if(isMandatoryFeature(child)) {
								newFeatureSelected = selection.add(child) || newFeatureSelected;
							}
						}
					}
				}
			}
		}
	}

	public static boolean isValidSelection(FeatureModel fm, FeatureSelection selection) {

		for(CrossTreeConstraint interpretation : crossTreeConstraints(fm)){

			if(interpretation.implication && selection.isSelected(interpretation.feature1) && !selection.isSelected(interpretation.feature2)) {
				return false; // implication violated
			} else if(!interpretation.implication && (selection.isSelected(interpretation.feature1) && selection.isSelected(interpretation.feature2))) {
				return false; // exclusion violated
			}
		}
		if(!selection.isSelected(fm.getRoot())) {
			return false;
		}
		for(FeatureTreeNode feature : listFeatures(fm)) { // the iterator guarantees that parents are visited before their children
			if(selection.isSelected(feature)) {
				if(!isRoot(feature) && !selection.isSelected((FeatureTreeNode) feature.getParent())) {
					return false;
				}
				if(isOrGroup(feature)) {
                    boolean childSelected = false;
					for(FeatureTreeNode child : children(feature)) {
						if(selection.isSelected(child)) {
							childSelected = true;
							break;
						}
					}
					if(!childSelected) {
						return false;
					}
				} else if(isAlternativeGroup(feature)) {
                    boolean childSelected = false;
                    for(FeatureTreeNode child : children(feature)) {
                        if(selection.isSelected(child)) {
                            if(childSelected) {
                                return false;
                            }
                            childSelected = true;
                        }
                    }
                    if(!childSelected) {
                        return false;
                    }
                } else {
                    for(FeatureTreeNode child : children(feature)) {
                        if(!isOptionalFeature(child) && !selection.isSelected(child)) {
                            return false;
                        }
                    }
                }
			}
		}
		return true;
	}

	/*
	 * CNF methods:
	 */

	public static String id(FeatureTreeNode feature) {
		return feature.getID();
	}

	public static FeatureTreeNode find(FeatureModel fm, String name) {
		return fm.getNodeByID(name);
	}


}
