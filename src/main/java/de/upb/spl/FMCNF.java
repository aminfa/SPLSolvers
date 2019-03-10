package de.upb.spl;

import fm.FeatureModel;
import fm.FeatureModelException;
import fm.FeatureTreeNode;
import fm.RootNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.sat4j.core.LiteralsUtils;
import org.sat4j.core.VecInt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class FMCNF extends FeatureModel {
	private FMSAT.LiteralUtil util = FMSAT.LiteralUtil.global;
	private FMSAT fmsat;
	private List<VecInt> clauses;
	private List<IntSet> literalClones = new ArrayList<>();
	private IntSet abstractLiterals = new IntOpenHashSet();
	private IntSet  constants = new IntOpenHashSet();
	private final AtomicInteger literalCount = new AtomicInteger(0);

	public FMCNF(FMSAT fmsat) {
		this.fmsat = fmsat;
		this.clauses = new ArrayList<>(fmsat.getClauses());
	}

	@Override
	protected FeatureTreeNode createNodes() throws FeatureModelException {
		if(fmsat == null) {
			return getRoot();
		}
		FeatureTreeNode root = null;
		countInitialLiterals();
		to3SAT();

		fmsat = null;
		return root;
	}

	private void countInitialLiterals() {
		int maxLiteral = 0;
		for(VecInt clause : clauses) {
			for (int i = 0, size = clause.size(); i < size; i++) {
				int literal = clause.get(i);
				if(literal > maxLiteral || literal < (- maxLiteral)) {
					maxLiteral = util.positiv(literal);
				}
			}
		}
		literalCount.set(maxLiteral);
	}

	private void readConstants() {
		int lastClauseCount = 0;
		while(lastClauseCount != clauses.size()) {
			lastClauseCount = clauses.size();
			Iterator<VecInt> iterator = clauses.iterator();
			while(iterator.hasNext()) {
				VecInt clause = iterator.next();
				if(clause.size() == 0) {
					int constant = clause.get(0);
					constants.add(constant);
					iterator.remove();
				} else {

				}

			}
		}
	}

	private void to3SAT() {
		/*
		 * Transform all clauses with more than
		 */

	}

	private int newLiteral() {
		int newLiteral = literalCount.incrementAndGet();
		abstractLiterals.add(newLiteral);
		return newLiteral;
	}

	private int cloneLiteral(int literal) {
		int clone = newLiteral();
		Optional<IntSet> clones = literalClones.parallelStream().filter(set -> set.contains(literal)).findAny();
		if(!clones.isPresent()) {
			clones = Optional.of(new IntOpenHashSet());
			clones.get().add(literal);
			literalClones.add(clones.get());
		}
		clones.get().add(clone);
		return clone;
 	}

	@Override
	protected void saveNodes() {

	}
}
