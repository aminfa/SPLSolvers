package de.upb.spl;

import fm.FeatureGroup;
import fm.FeatureModel;
import fm.FeatureTreeNode;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.*;
import org.sat4j.reader.EfficientScanner;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IteratorInt;
import util.Iterators;


import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class FMCNF {
	private final Object2IntRBTreeMap<String> indexer;
	private final List<String> featureNames;
	private final List<VecInt> clauses;

	private static final int[] rootClaus = {1}; // root is always true
	private static final VecInt rootClausVector = new VecInt(rootClaus);

	private FMCNF(Object2IntRBTreeMap<String> indexer, List<String> featureNames, List<VecInt> clauses) {
		this.indexer = indexer;
		this.featureNames = featureNames;
		this.clauses = clauses;
	}

	public static FMCNF transform(FeatureModel model) {
		List<String> featureNames = new ArrayList<>();
		Object2IntRBTreeMap<String> indexer = new Object2IntRBTreeMap<String>();
		Integer literalIndex = 1; // root has index 1
		for(FeatureTreeNode feature : FMUtil.featureIterable(model)){
			String featureName = feature.getName();
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
		for(FeatureTreeNode parent : FMUtil.featureIterable(model)){
			for(FeatureTreeNode child : FMUtil.children(parent)) {
				// add child -> parent constraint:
				clauses.add(new VecInt(util.imply(child, parent)));
				if(!FMUtil.isOptionalFeature(child)) {
					clauses.add(new VecInt(util.imply(parent, child)));
				}
				if(parent instanceof FeatureGroup) {
					// GROUP:
					clauses.add(util.group(parent, FMUtil.children(parent).iterator()));
					if(FMUtil.isAlternativeGroup(parent)) {
						// XOR clause:
						clauses.add(util.exclude(FMUtil.children(parent).iterator()));
					}
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
		return new FMCNF(indexer, featureNames, clauses);
	}


	public static FMCNF readDIMACS(InputStream inputStream) throws IOException, ParseFormatException {
		EfficientScanner scanner = new EfficientScanner(inputStream);

		/*
		 * TODO extract feature names
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
		return new FMCNF(indexer, featureNames, clauses);
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


	public VecInt violatedFeatures(VecInt selection) {
		VecInt vialotedFeatures = new VecInt((selection.size()/2));
		Set<Integer> blacklistedFeatures = new HashSet<>();
		for (VecInt clause : clauses) {
			if (satisfies(clause, selection)) {
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

	public int violatedConstraints(VecInt selection) {
		int s = 0;
		for (VecInt clause : clauses) {
			if (!satisfies(clause, selection)) {
				s++;
			}

		}
		return s;
	}

	public static boolean satisfies(VecInt clause, VecInt selection) {
		boolean sat = false;
		IteratorInt it = clause.iterator();
		while(it.hasNext()) {
			int i = it.next();
			int abs = (i < 0) ? -i : i;
			boolean sign = i > 0;
			if ( (selection.get(abs - 1) > 0) == sign) {
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

		ISolver solver = SolverFactory.newMiniSATHeap();
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

	private static class LiteralUtil {
		private final static LiteralUtil global = new LiteralUtil();
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
			int[] equivalence1 = {negate(literal1), literal2};
			int[] equivalence2 = {negate(literal2), literal1};

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
			int[] exclusion = {negate(literal1), negate(literal2)};
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

		VecInt exclude(Iterator<Integer> literals) {
			VecInt exlusion = new VecInt();
			while(literals.hasNext()) {
				exlusion.push(negate(literals.next()));
			}
			return exlusion;
		}
	}

	private static class FeatureLiteralUtil {
		private final LiteralUtil util;
		private final Object2IntRBTreeMap<String> indexer;

		FeatureLiteralUtil(Object2IntRBTreeMap<String> indexer) {
			this.indexer = indexer;
			util = new LiteralUtil();
		}

		int negative(FeatureTreeNode node) {
			return indexer.getInt(node.getName()) * -1;
		}


		int positiv(FeatureTreeNode node) {
			return indexer.getInt(node.getName());
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
			return util.orGroup(indexer.getInt(parent.getName()), Iterators.map(group, feature -> indexer.getInt(feature.getName())));
		}

		VecInt exclude(Iterator<FeatureTreeNode> group) {
			return util.exclude(Iterators.map(group, feature -> indexer.getInt(feature.getName())));
		}
	}
}
