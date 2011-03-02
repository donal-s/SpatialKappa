package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.List;


public abstract class Transition {

    protected static final String INFINITE_RATE_TEXT = "$INF";
    
    public static final float INFINITE_RATE = Float.MAX_VALUE;
    public static final float ZERO_RATE = 0f;

    protected float rate;
    public final String label;
    public final List<Complex> sourceComplexes = new ArrayList<Complex>();
    public final List<Complex> targetComplexes = new ArrayList<Complex>();
    
    public Transition(String label, String rate) {
        if (rate == null) {
            throw new NullPointerException();
        }
        if (INFINITE_RATE_TEXT.equals(rate)) {
            this.rate = INFINITE_RATE;
        }
        else {
            this.rate = Float.parseFloat(rate);
        }
        this.label = label;
    }

    public final float getRate() {
        return rate;
    }

    public final void setRate(float rate) {
        this.rate = rate;
    }

    public final boolean isInfiniteRate() {
        return rate == INFINITE_RATE;
    }

    public final String getRateText() {
        return isInfiniteRate() ? INFINITE_RATE_TEXT : Float.toString(rate);
    }

}
