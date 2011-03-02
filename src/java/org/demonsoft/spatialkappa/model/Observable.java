package org.demonsoft.spatialkappa.model;

import java.io.Serializable;


public class Observable implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public final Complex complex;
    public final String label;
    public final boolean inObservations;
    private final boolean generatedLabel;
    
    public Observable(Complex complex, String label, boolean inObservations) {
        if (complex == null) {
            throw new NullPointerException();
        }
        this.complex = complex;
        this.inObservations = inObservations;
        if (label != null) {
            this.label = label;
            generatedLabel = false;
        }
        else {
            this.label = complex.toString();
            generatedLabel = true;
        }
    }
    
    public Observable(String label) {
        if (label == null) {
            throw new NullPointerException();
        }
        this.complex = null;
        this.label = label;
        this.inObservations = true;
        generatedLabel = false;
    }

    @Override
    public String toString() {
        if (complex == null) {
            return "'" + label + "'";
        }
        return "'" + label + "' (" + complex + ")";
    }

    public boolean isInObservations() {
        return inObservations;
    }
    
    public boolean isGeneratedLabel() {
        return generatedLabel;
    }

//    public String toKappaString() {
//        if (complex == null) {
//            return "%obs: '" + label + "'";
//        }
//        if (generatedLabel) {
//            return "%obs: " + complex.toKappaString();
//        }
//        return "%obs: '" + label + "' " + complex.toKappaString();
//    }
}
