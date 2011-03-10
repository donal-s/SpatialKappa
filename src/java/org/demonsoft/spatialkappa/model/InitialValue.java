package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getFlatString;

import java.util.ArrayList;
import java.util.List;

public class InitialValue {

    public final List<Complex> complexes = new ArrayList<Complex>();
    public final int quantity;
    public final Location location;
    public final VariableReference reference;

    public InitialValue(List<Complex> complexes, int quantity, Location location) {
        if (complexes == null) {
            throw new NullPointerException();
        }
        if (complexes.size() == 0 || quantity <= 0) {
            throw new IllegalArgumentException();
        }

        this.quantity = quantity;
        this.complexes.addAll(complexes);
        this.location = location;
        this.reference = null;
    }

    public InitialValue(List<Complex> complexes, VariableReference reference, Location location) {
        if (complexes == null || reference == null) {
            throw new NullPointerException();
        }
        if (complexes.size() == 0) {
            throw new IllegalArgumentException();
        }
        this.quantity = 0;
        this.complexes.addAll(complexes);
        this.location = location;
        this.reference = reference;

    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(quantity);
        if (location != null) {
            result.append(" ").append(location);
        }

        result.append(" * (").append(getFlatString(complexes)).append(")");
        return result.toString();
    }

}