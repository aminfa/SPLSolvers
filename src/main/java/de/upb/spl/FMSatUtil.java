package de.upb.spl;


import fm.FeatureModel;
import fm.FeatureTreeNode;
import org.sat4j.core.VecInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.DefaultMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FMSatUtil {

    private final static Logger logger = LoggerFactory.getLogger(FMSatUtil.class);

    private final static DefaultMap<FMSAT, SATCache> cache = new DefaultMap<>(SATCache::new);

    private static class SATCache {

        private VecInt allLiteralOrder;
        private VecInt sayyadFixedLiterals;
        private VecInt sayyadLiteralOrder;
        private VecInt unitLiterals;
        private VecInt nonUnitLiteralOrder;

        SATCache(FMSAT fmsat) {
            allLiteralOrder = new VecInt(fmsat.highestLiteral());
            IntStream.rangeClosed(1, fmsat.highestLiteral()).forEachOrdered(allLiteralOrder::push);

            unitPropagation(fmsat);
            calculateSayyadFixedLiterals(fmsat);
            sayyadLiteralOrder = new VecInt();
            nonUnitLiteralOrder = new VecInt();
            int maxLiteral = fmsat.highestLiteral();
            for (int i = 1; i < maxLiteral; i++) {
                int literal = i;
                if(!sayyadFixedLiterals.contains(literal) && !sayyadFixedLiterals.contains(-literal)) {
                    sayyadLiteralOrder.push(literal);
                }
                if(!unitLiterals.contains(literal) && !unitLiterals.contains(-literal)) {
                    nonUnitLiteralOrder.push(literal);
                }
            }
        }

        void unitPropagation(FMSAT fmsat) {
            List<VecInt> clauses = new ArrayList<>();
            fmsat.getClauses().forEach(clause ->  {
                VecInt copiedClause = new VecInt();
                clause.copyTo(copiedClause);
                clauses.add(copiedClause);
            });
            VecInt fixedLiterals = new VecInt();
            unitPropagation(clauses, fixedLiterals);
            unitLiterals = fixedLiterals;
        }

        void unitPropagation(List<VecInt> clauses, VecInt fixedLiterals) {
            /*
             * See: https://en.wikipedia.org/wiki/Unit_propagation
             */
            int initialClausesSize = clauses.size();
            int initialFixedLiteralsSize = fixedLiterals.size();

            /*
             * Find all unit clauses
             */
            for(VecInt clause : clauses) {
                if(clause.size() == 1) {
                    int literal = clause.get(0);
                    if(fixedLiterals.contains(FMSAT.LiteralUtil.global.negate(literal))) {
                        throw new IllegalArgumentException("Fixed literal " + literal + " appears positive and negative in uni clauses.");
                    } else if(!fixedLiterals.contains(literal)){
                        fixedLiterals.push(literal);
                    } else {
                        logger.info("Literal {} appears in more than one uni clause.", literal);
                    }
                }
            }

            /*
             * Remove all clauses that contain the unit
             */
            Iterator<VecInt> iterator = clauses.iterator();
            while(iterator.hasNext()) {
                VecInt clause = iterator.next();
                int size = fixedLiterals.size();
                for (int i = size - 1; i >= 0; i--) {
                    if(clause.contains(fixedLiterals.get(i))) {
                        iterator.remove();
                        break;
                    }
                }
            }

            /*
             * Remove all literals from clauses that are negative values of the fixed literals
             */
            iterator = clauses.iterator();
            while(iterator.hasNext()) {
                VecInt clause = iterator.next();
                int size = fixedLiterals.size();
                for (int i = size - 1; i >= 0; i--) {
                    int negativeLiteral = FMSAT.LiteralUtil.global.negate(fixedLiterals.get(i));
                    int indexOf = clause.indexOf(negativeLiteral);
                    if(indexOf != -1) {
                        clause.delete(indexOf);
                    }
                }
            }

            /*
             * Continue until input doesn't change.
             */
            if(initialClausesSize != clauses.size() || initialFixedLiteralsSize != fixedLiterals.size()) {
                unitPropagation(clauses, fixedLiterals);
            }
        }

        void calculateSayyadFixedLiterals(FMSAT sat) {
            sayyadFixedLiterals = new VecInt();
            for(VecInt clauses : sat.getClauses()) {
                if(clauses.size() == 1) {
                    sayyadFixedLiterals.push(clauses.get(0));
                }
            }
            for(VecInt clauses : sat.getClauses()) {
                if(clauses.size() == 2) {
                    int literal1 = clauses.get(0);
                    int literal1Neg = FMSAT.LiteralUtil.global.negate(literal1);
                    int literal2 = clauses.get(1);
                    int literal2Neg = FMSAT.LiteralUtil.global.negate(literal2);

                    boolean literal1FixedPos = sayyadFixedLiterals.contains(literal1);
                    boolean literal1FixedNeg = sayyadFixedLiterals.contains(literal1Neg);

                    boolean literal2FixedPos = sayyadFixedLiterals.contains(literal2);
                    boolean literal2FixedNeg = sayyadFixedLiterals.contains(literal2Neg);

                    boolean literal1Fixed = literal1FixedPos || literal1FixedNeg;
                    boolean literal2Fixed = literal2FixedPos || literal2FixedNeg;

                    if(literal1Fixed && literal2Fixed) {
                        continue;
                    } else if(literal1Fixed) {
                        if(literal1FixedNeg) {
                            sayyadFixedLiterals.push(literal2);
                        }
                    } else if(literal2Fixed) {
                        if(literal2FixedNeg) {
                            sayyadFixedLiterals.push(literal1);
                        }
                    } else {
                        // unchange
                    }
                }
            }
        }



    }
    public static class ModelSpliterator implements Spliterator.OfInt {


        private final VecInt literals;

        private int origin; // current index, advanced on split or traversal

        private final int fence; // one past the greatest index

        public ModelSpliterator(VecInt literals, int origin, int fence) {
            this.literals = literals;
            this.origin = origin;
            this.fence = fence;
        }

        public ModelSpliterator(VecInt literals) {
            this(literals, 0, literals.size());
        }

        @Override
        public OfInt trySplit() {
            int lo = origin; // divide range in half
            int mid = ((lo + fence) >>> 1) & ~1; // force midpoint to be even
            if (lo < mid) { // split out left half
                origin = mid; // reset this Spliterator's origin
                return new ModelSpliterator(literals, lo, mid);
            }
            else       // too small to split
                return null;
        }

        @Override
        public long estimateSize() {
            return (fence - origin);
        }

        @Override
        public long getExactSizeIfKnown() {
            return (fence - origin);
        }

        @Override
        public int characteristics() {
            return SIZED | IMMUTABLE | SUBSIZED;
        }
        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (origin < fence) {
                action.accept(literals.get(origin));
                origin += 1;
                return true;
            }
            else // cannot advance
                return false;
        }

    }

    public static IntStream streamLiterals(VecInt literals) {
        return StreamSupport.intStream(new ModelSpliterator(literals), false);
    }

    public static VecInt unitLiterals(FMSAT fmsat) {
        return cache.get(fmsat).unitLiterals;
    }

    public static VecInt nonUnitLiteralOrder(FMSAT fmsat) {
        return cache.get(fmsat).nonUnitLiteralOrder;
    }

    public static VecInt literals(FMSAT fmsat) {
        return cache.get(fmsat).allLiteralOrder;
    }

    public static Stream<String> toName(FMSAT fmsat, VecInt literals) {
        return streamLiterals(literals).mapToObj(fmsat::getLiteralFeatureName);
    }

    public static Stream<FeatureTreeNode> toFeature(FeatureModel fm, FMSAT fmsat, VecInt literals) {
        return toName(fmsat, literals).map(name -> FMUtil.find(fm, name));
    }


}
