package de.upb.spl;


import org.sat4j.core.VecInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.DefaultMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FMSatUtil {

    private final static Logger logger = LoggerFactory.getLogger(FMSatUtil.class);

    private final static DefaultMap<FMSAT, SATCache> cache = new DefaultMap<>(SATCache::new);

    private static class SATCache {

        private VecInt sayyadFixedLiterals;
        private VecInt sayyadLiteralOrder;
        private VecInt unitLiterals;
        private VecInt nonUnitLiteralOrder;

        SATCache(FMSAT fmsat) {
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

    public static VecInt unitLiterals(FMSAT fmsat) {
        return cache.get(fmsat).unitLiterals;
    }

    public static VecInt nonUnitLiteralOrder(FMSAT fmsat) {
        return cache.get(fmsat).nonUnitLiteralOrder;
    }

}
