package de.upb.spl.ibea;

import org.moeaframework.core.FrameworkException;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CompoundVariation implements Variation {
    private final List<Variation> operators;
    private String name;

    public CompoundVariation() {
        this.operators = new ArrayList();
    }

    public CompoundVariation(Variation... operators) {
        this();
        Variation[] arr$ = operators;
        int len$ = operators.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Variation operator = arr$[i$];
            this.appendOperator(operator);
        }

    }

    public String getName() {
        if (this.name == null) {
            StringBuilder sb = new StringBuilder();

            Variation operator;
            for(Iterator i$ = this.operators.iterator(); i$.hasNext(); sb.append(operator.getClass().getSimpleName())) {
                operator = (Variation)i$.next();
                if (sb.length() > 0) {
                    sb.append('+');
                }
            }

            return sb.toString();
        } else {
            return this.name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void appendOperator(Variation variation) {
        this.operators.add(variation);
    }

    public Solution[] evolve(Solution[] parents) {
        Solution[] result = (Solution[]) Arrays.copyOf(parents, parents.length);
        Iterator i$ = this.operators.iterator();

        while(true) {
            while(i$.hasNext()) {
                Variation operator = (Variation)i$.next();
                if (result.length == operator.getArity()) {
                    result = operator.evolve(result);
                } else {
                    if (operator.getArity() != 1) {
                        throw new FrameworkException("invalid number of parents");
                    }

                    for(int j = 0; j < result.length; ++j) {
                        result[j] = operator.evolve(new Solution[]{result[j]})[0];
                    }
                }
            }

            return result;
        }
    }

    public int getArity() {
        return (this.operators.get(0)).getArity();
    }
}

