package de.upb.spl;

import fm.FeatureGroup;
import fm.FeatureModel;
import fm.FeatureModelException;
import fm.FeatureTreeNode;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.*;
import org.sat4j.reader.EfficientScanner;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IteratorInt;
import util.Iterators;


import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class FMSAT {
	private final Object2IntRBTreeMap<String> indexer;
	private final List<String> featureNames;
	private final List<VecInt> clauses;

	private static final int[] rootClaus = {1}; // root is always true
	private static final VecInt rootClausVector = new VecInt(rootClaus);

	private FMSAT(Object2IntRBTreeMap<String> indexer, List<String> featureNames, List<VecInt> clauses) {
		this.indexer = indexer;
		this.featureNames = featureNames;
		this.clauses = clauses;
	}

	public static FMSAT transform(FeatureModel model) {
		List<String> featureNames = new ArrayList<>();
		Object2IntRBTreeMap<String> indexer = new Object2IntRBTreeMap<String>();
		int literalIndex = 1; // root has index 1
		for(FeatureTreeNode feature : FMUtil.listFeatures(model)){
			String featureName = FMUtil.id(feature);
			featureNames.add(featureName);
			indexer.put(featureName, literalIndex);
			literalIndex ++;
		}
		featureNames = Collections.unmodifiableList(featureNames);
		FeatureLiteralUtil util = new FeatureLiteralUtil(indexer);
		/*
		 * Iterate over all features and add clauses for each:
		 */
		List<VecInt> clauses = new ArrayList<>(featureNames.size() * 10);
		clauses.add(rootClausVector);
		for(FeatureTreeNode parent : FMUtil.listFeatures(model)){
			for(FeatureTreeNode child : FMUtil.children(parent)) {
				// add child -> parent constraint:
				clauses.add(new VecInt(util.imply(child, parent)));
				if(FMUtil.isMandatoryFeature(child)) {
					clauses.add(new VecInt(util.imply(parent, child)));
				}
			}
            if(parent instanceof FeatureGroup) {
                // GROUP:
                clauses.add(util.group(parent, FMUtil.children(parent).iterator()));
                if(FMUtil.isAlternativeGroup(parent)) {
                    // XOR clause:
                    clauses.addAll(util.exclude(FMUtil.children(parent)));
                }
            }
		}
		for(FMUtil.CrossTreeConstraint ctc : FMUtil.crossTreeConstraints(model)) {
			if(ctc.implication) {
				clauses.add(new VecInt(util.imply(ctc.feature1, ctc.feature2)));
			} else {
				clauses.add(new VecInt(util.exclude(ctc.feature1, ctc.feature2)));
			}
		}
		return new FMSAT(indexer, featureNames, clauses);
	}


	public static FMSAT readDIMACS(InputStream inputStream) throws IOException, ParseFormatException {
		EfficientScanner scanner = new EfficientScanner(inputStream);

		/*
		 * TODO extract listFeatures names
		 * Read comments:
		 */
		scanner.skipComments();

		/*
		 * Read problem line:
		 */
		String line = scanner.nextLine().trim();
		if (line == null) {
			throw new IllegalArgumentException(
					"premature end of file: <p cnf ...> expected");
		}
		String[] tokens = line.split("\\s+");
		if (tokens.length < 4 || !"p".equals(tokens[0])
				|| !"cnf".equals(tokens[1])) {
			throw new IllegalArgumentException("problem line expected (p cnf ...)");
		}
		int vars;
		// reads the max var id
		vars = Integer.parseInt(tokens[2]);
		assert vars > 0;
		// reads the number of clauses
		int expectedNbOfConstr = Integer.parseInt(tokens[3]);
		assert expectedNbOfConstr > 0;

		/*
		 * Read clauses:
		 */
		List<VecInt> clauses = new ArrayList<>(expectedNbOfConstr);
		List<String> featureNames = new ArrayList<>();
		Object2IntRBTreeMap<String> indexer = new Object2IntRBTreeMap<>();
		for (int i = 0; i < vars; i++) {
			String featureName = "Feature_" + i;
			featureNames.add(featureName);
			indexer.put(featureName, i);
		}

		VecInt literals = new VecInt();
		while (!scanner.eof()) {
			boolean added = false;
			if (scanner.currentChar() == 'c') {
				// ignore comment line
				scanner.skipRestOfLine();
				continue;
			}
			int lit;
			while (!scanner.eof()) {
				lit = scanner.nextInt();
				if (lit == 0) {
					if (literals.size() > 0) {
						clauses.add(literals);
						literals = new VecInt();
					}
					break;
				}
				literals.push(lit);
			}
		}
		return new FMSAT(indexer, featureNames, clauses);
	}

	public void shuffleClausesInPlace(Random random) {
		Collections.shuffle(clauses, random);
	}

	public void shuffleLiteralsInPlace(Random random) {
		for(VecInt clause : clauses) {
			int literals = clause.size();
			int[] internalArr = clause.toArray();
			for (int i = 0; i < literals; i++) {
				// Get a random index of the array past the current index.
				// ... The argument is an exclusive bound.
				//     It will not go past the array's end.
				int randomValue = i + random.nextInt(literals - i);
				// Swap the random element with the present element.
				int randomElement = internalArr[randomValue];
				internalArr[randomValue] = internalArr[i];
				internalArr[i] = randomElement;
			}
		}
	}


	public VecInt violatingFeatures(FeatureSelection selection) {
		VecInt vialotedFeatures = new VecInt((selection.size()/2));
		Set<Integer> blacklistedFeatures = new HashSet<>();
		for (VecInt clause : clauses) {
			if (satisfiesClause(clause, selection)) {
				continue;
			}
			IteratorInt it = clause.iterator();
			while(it.hasNext()) {
				blacklistedFeatures.add(LiteralUtil.global.positiv(it.next()));
			}
		}
		blacklistedFeatures.forEach(vialotedFeatures::push);
		return vialotedFeatures;
	}


	public VecInt unsetFeatures(VecInt selection, VecInt unset) {
		VecInt partial = new VecInt(selection.size() - unset.size());
		IteratorInt it = selection.iterator();
		while(it.hasNext()) {
			int literal = it.next();
			int literalNeg = literal * -1;
			if(unset.contains(literal)|| unset.contains(literalNeg)) {
			} else {
				partial.push(literal);
			}
		}
		return partial;
	}

    public int violatedConstraints(int[] model) {
        int s = 0;
        for (VecInt clause : clauses) {
            if (!satisfiesClause(clause, model)) {
                s++;
            }

        }
        return s;
    }

    public int violatedConstraints(FeatureSelection selection) {
        int s = 0;
        for (VecInt clause : clauses) {
            if (!satisfiesClause(clause, selection)) {
                s++;
            }

        }
        return s;
    }

    public boolean satisfiesClause(VecInt clause, FeatureSelection selection) {
        IteratorInt it = clause.iterator();
        while(it.hasNext()) {
            int i = it.next();
            int abs = (i < 0) ? -i : i;
            boolean sign = i > 0;
            String featureName = featureNames.get(abs-1);
            if (selection.isSelected(featureName) == sign) {
                return true;
            }
        }
        return false;
    }

	public static boolean satisfiesClause(VecInt clause, int[] model) {
		boolean sat = false;
		IteratorInt it = clause.iterator();
		while(it.hasNext()) {
			int i = it.next();
			int abs = (i < 0) ? -i : i;
			boolean sign = i > 0;
			if((model[(abs - 1)] > 0) == sign){
				sat = true;
				break;
			}
		}
		return sat;
	}

	public ISolver getSolver() {
		ISolver solver = SolverFactory.newDefault();
		insertCNF(solver);
		return solver;
	}

	public ISolver getDiverseSolver(Random random) {
		int rand = random.nextInt(3);
		IOrder order;
		if (rand == 0) {
			order = new RandomWalkDecorator(new VarOrderHeap(new NegativeLiteralSelectionStrategy()), 1);
		} else if (rand == 1) {
			order = new RandomWalkDecorator(new VarOrderHeap(new PositiveLiteralSelectionStrategy()), 1);
		} else {
			order = new RandomWalkDecorator(new VarOrderHeap(new RandomLiteralSelectionStrategy()), 1);
		}

		ISolver solver = SolverFactory.newDefault();
		insertCNF(solver);
		((Solver) solver).setOrder(order);
		return solver;
	}



	public void insertCNF(ISolver solver) {
		solver.newVar(featureNames.size());
		solver.setExpectedNumberOfClauses(clauses.size());
		for (VecInt clause : clauses) {
			try {
				solver.addClause(clause);
			} catch (ContradictionException e) {
				throw new IllegalStateException("Contradiction adding clause: " + toString(clause), e);
			}
		}
	}

	public FeatureSelection toSelection(FeatureModel fm, int[] model) {
	    return new LiteralSelection(fm, model);
//		FeatureSet set = new FeatureSet();
//		for(int literal : model) {
//			if(LiteralUtil.global.isPositive(literal)){
//				String name = getLiteralFeatureName(literal);
//				FeatureTreeNode feature = FMUtil.find(fm, name);
//				set.add(feature);
//			}
//		}
//		return set;
	}

    public int[] toModel(FeatureModel fm, FeatureSelection selection) {
        int[] model = new int[featureNames.size()];
        int index = 0;
        for(String featureName : featureNames) {
            index++;
            FeatureTreeNode feature = FMUtil.find(fm, featureName);
            model[index-1] = (selection.isSelected(feature) ? index : -index);
        }
        return model;
    }

    @Deprecated
	public FeatureModel toModel() {

		FeatureModel model = new FMCNF(this);
		try {
			model.loadModel();
		} catch (FeatureModelException e) {
			throw new RuntimeException("Cannot convert to FeatureModel", e);
		}
		model.countNodes();
		return model;
	}

    public int[] toModel(int[] unorderedPartialModel, boolean positiveMissingLiterals) {
        if(isModel(unorderedPartialModel)) {
            return unorderedPartialModel;
        }
        int featureCount = featureNames.size();
        int[] model = new int[featureCount];
        if(positiveMissingLiterals) {
            for (int i = 1; i <= featureCount; i++) {
                model[i-1] = i;
            }
        } else {
            for (int i = 1; i <= featureCount; i++) {
                model[i-1] = -i;
            }
        }
        for (int literal : unorderedPartialModel) {
            if(literal == 0) {
                continue;
            }
            int literalIndex = LiteralUtil.global.positiv(literal) - 1;
            if (model[literalIndex] != literal) {
                model[literalIndex] = literal;
            }
        }
        return model;
    }

    public boolean isModel(int[] model) {
        int featureCount = featureNames.size();
        if(model.length != featureCount) {
            return false;
        }
        for (int i = 0; i < featureCount; i++) {
            int literal = i + 1;
            int negativeLiteral = - literal;
            if (model[i] != literal && model[i] != negativeLiteral) {
                return false;
            }
        }
        return true;
    }

	public String toString(VecInt clause) {
		StringBuilder clauseText = new StringBuilder("{ ");
		IteratorInt iterator =  clause.iterator();
		while(iterator.hasNext()) {
			int literal = iterator.next();
			if(LiteralUtil.global.isNegative(literal)) {
				clauseText.append("¬");
			}
			clauseText.append(getLiteralFeatureName(literal));
			if(iterator.hasNext()) {
				clauseText.append(", ");
			}
		}
		return  clauseText.toString();
	}

	public String getLiteralFeatureName(int literal) {
		return featureNames.get(LiteralUtil.global.positiv(literal)-1);
	}

	public int toLiteral(FeatureTreeNode feature) {
	    return indexer.getInt(FMUtil.id(feature));
    }

	public List<VecInt> getClauses() {
		return  clauses;
	}

	public List<String> getFeatureNames() {
		return featureNames;
	}

	public static double dissimilarity(int[] c1, int[] c2) {
		int disjunctionSize = 0, conjunctionSize = 0;
		for (int i = 0; i < c1.length; i++) {
			int c1i = c1[i], c2i = c2[i];
			if(LiteralUtil.global.isPositive(c1i) || LiteralUtil.global.isPositive(c2i)) {
				disjunctionSize++;
				if(LiteralUtil.global.isPositive(c1i) && LiteralUtil.global.isPositive(c2i)) {
					conjunctionSize ++;
				}
			}
		}
		return ((double)disjunctionSize / (double) conjunctionSize) / (double) (disjunctionSize);
	}

    public int highestLiteral() {
	    return featureNames.size() + 1;
    }

    public class LiteralSelection implements FeatureSelection {
	    private final FeatureModel fm;
	    private int[] model;

        private LiteralSelection(FeatureModel fm, int[] m) {
            this.fm = fm;
            this.model = toModel(m, false);
        }

        public void setModel(int[] newModel) {
            this.model = toModel(newModel, false);
        }

        public int[] getModel() {
            return model;
        }

        @Override
        public boolean isSelected(FeatureTreeNode feature) {
            int index = indexer.getInt(FMUtil.id(feature));
            return model[index-1] > 0;
        }

        @Override
        public boolean isSelected(String featureId) {
            int index = indexer.getInt(featureId);
            return model[index-1] > 0;
        }

        @Override
        public int size() {
            int size = 0;
            for (int i = 0; i < model.length; i++) {
                if(model[i] > 0) {
                    size ++;
                }
            }
            return size;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < model.length; i++) {
                if(model[i] > 0) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean contains(Object o) {
            if(o == null || !(o instanceof FeatureTreeNode)) {
                return false;
            }
            else {
                return isSelected((FeatureTreeNode) o);
            }
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(FeatureTreeNode featureTreeNode) {
            int index = indexer.getInt(FMUtil.id(featureTreeNode));
            if(model[index-1] < 0) {
                model[index-1] = index;
                return true;
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if(o == null || !(o instanceof FeatureTreeNode)) {
                return false;
            }
            FeatureTreeNode feature = (FeatureTreeNode) o;
            int index = indexer.getInt(FMUtil.id(feature));
            if(model[index-1] > 0) {
                model[index-1] = -index;
                return true;
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends FeatureTreeNode> c) {
            boolean added = false;
            for(FeatureTreeNode feature : c) {
                added = add(feature) || added;
            }
            return added;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            for (int i = 0; i < model.length; i++) {
                int selection = model[i];
                if(selection > 0) {
                    model[i] = -selection;
                }
            }
        }

        @Override
        public Iterator<FeatureTreeNode> iterator() {

            return new Iterator<FeatureTreeNode>() {
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < model.length;
                }

                @Override
                public FeatureTreeNode next() {
                    int literal = model[index];
                    index ++;
                    if(literal < 0) {
                        literal = -literal;
                    }
                    FeatureTreeNode feature = FMUtil.find(fm, featureNames.get(literal-1));
                    return feature;
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (!(o instanceof FeatureSelection)) return false;

            FeatureSelection otherSelection = (FeatureSelection) o;

            if(otherSelection.size() != this.size())
                return false;

            if(otherSelection instanceof  LiteralSelection) {
                int[] lhs = model,
                        rhs = ((LiteralSelection) otherSelection).model;
                if (lhs == rhs) {
                    return true;
                }
                if (lhs == null || rhs == null) {
                    return false;
                }
                if (lhs.length != rhs.length) {
                    return false;
                }
                for (int i = 0; i < lhs.length; ++i) {
                    if(lhs[i] != rhs[i]) {
                        return false;
                    }
                }
                return true;
            } else {
                for(FeatureTreeNode feature : otherSelection) {
                    if(!isSelected(feature)) {
                        return false;
                    }
                }
                return true;
            }
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(model)
                    .toHashCode();
        }
    }

	public static double dissimilarity(List<int[]> models) {
		double dissimilarity = 0;
		for (int i = 0; i < models.size(); i++) {
			for (int j = i + 1; j < models.size(); j++) {
				int[] model1 = models.get(i);
				int[] model2 = models.get(j);
				dissimilarity += dissimilarity(model1, model2);
			}
		}
		return dissimilarity / CombinatoricsUtils.binomialCoefficientDouble(models.size(), 2);
	}

	public static class LiteralUtil {
		public final static LiteralUtil global = new LiteralUtil();
		int negative(int literal) {
			return literal < 0 ? literal : literal * -1;
		}

		int positiv(int literal) {
			return literal > 0 ? literal : literal * -1;
		}

		int negate(int literal) {
			return literal * -1;
		}

		int[] imply(int literal1, int literal2) {
			int[] implication = {negate(literal1), literal2};
			return implication;
		}

		int[][] equivalence(int literal1, int literal2) {
			int[] equivalence1 = {negative(literal1), literal2};
			int[] equivalence2 = {negative(literal2), literal1};

			int[][] equivalence = {equivalence1, equivalence2};
			return equivalence;
		}

		boolean isNegative(int literal) {
			return literal < 0;
		}

		boolean isPositive(int literal) {
			return literal > 0;
		}


		int[] exclude(int literal1, int literal2) {
			int[] exclusion = {negative(literal1), negative(literal2)};
			return exclusion;
		}

		VecInt orGroup(int literalParent, Iterator<Integer> literals) {
			VecInt group = new VecInt();
			group.push(negate(literalParent));
			while(literals.hasNext()) {
				group.push(literals.next());
			}
			return group;
		}

		List<VecInt> exclude(Iterable<Integer> literals) {
		    List<VecInt> exclusions = new ArrayList<>();
		    Iterator<Integer> literalsIt1 = literals.iterator();
		    int index = 0;
		    while(literalsIt1.hasNext()) {
		        int literal1 = literalsIt1.next();
                Iterator<Integer> literalsIt2 = literals.iterator();
                int innerIndex = 0;
                while(literalsIt2.hasNext()) {
                    int literal2 = literalsIt2.next();
                    if(innerIndex <= index) {
                        innerIndex ++;
                        continue;
                    }
                    innerIndex ++;
                    exclusions.add(new VecInt(exclude(literal1, literal2)));
                }
                index++;
            }
			return exclusions;
		}
	}

	public static class FeatureLiteralUtil {
		private final LiteralUtil util;
		private final Object2IntRBTreeMap<String> indexer;

		FeatureLiteralUtil(Object2IntRBTreeMap<String> indexer) {
			this.indexer = indexer;
			util = new LiteralUtil();
		}

		int negative(FeatureTreeNode node) {
			return indexer.getInt(FMUtil.id(node)) * -1;
		}


		int positiv(FeatureTreeNode node) {
			return indexer.getInt(FMUtil.id(node));
		}

		int[] imply(FeatureTreeNode node1, FeatureTreeNode node2) {
			return util.imply(positiv(node1), positiv(node2));
		}

		int[][] equivalence(FeatureTreeNode node1, FeatureTreeNode node2) {
			return util.equivalence(positiv(node1), positiv(node2));
		}

		int[] exclude(FeatureTreeNode node1, FeatureTreeNode node2) {
			return util.exclude(positiv(node1), positiv(node2));
		}

		VecInt group(FeatureTreeNode parent, Iterator<FeatureTreeNode> group) {
			return util.orGroup(indexer.getInt(FMUtil.id(parent)), Iterators.map(group, feature -> indexer.getInt(FMUtil.id(feature))));
		}

		List<VecInt> exclude(Iterable<FeatureTreeNode> group) {
			return util.exclude(() -> Iterators.map(group.iterator(), feature -> indexer.getInt(FMUtil.id(feature))));
		}
	}
}
