package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class Transition {

    public static final float ZERO_RATE = 0f;

    protected VariableExpression rate;
    public final String label;
    public final List<Complex> sourceComplexes = new ArrayList<Complex>();
    public final List<Complex> targetComplexes = new ArrayList<Complex>();
    
    public Transition(String label, VariableExpression rate) {
        if (rate == null) {
            throw new NullPointerException();
        }
        this.rate = rate;
        this.label = label;
    }

    public final VariableExpression getRate() {
        return rate;
    }

    public final void setRate(VariableExpression rate) {
        this.rate = rate;
    }

    public final boolean isInfiniteRate(Map<String, Variable> variables) {
        return rate.isInfinite(variables);
    }

}
