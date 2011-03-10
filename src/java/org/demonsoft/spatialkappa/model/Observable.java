package org.demonsoft.spatialkappa.model;

import java.io.Serializable;


public class Observable implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public final Complex complex;
    public final String label;
    
    public Observable(Complex complex, String label) {
        if (complex == null || label == null) {
            throw new NullPointerException();
        }
        this.complex = complex;
        this.label = label;
    }
    
    public Observable(String label) {
        if (label == null) {
            throw new NullPointerException();
        }
        this.complex = null;
        this.label = label;
    }

    @Override
    public String toString() {
        if (complex == null) {
            return "'" + label + "'";
        }
        return "'" + label + "' (" + complex + ")";
    }

//    public String toKappaString() {
//        if (complex == null) {
//            return "%obs: '" + label + "'";
//        }
//        return "%obs: '" + label + "' " + complex.toKappaString();
//    }
}
