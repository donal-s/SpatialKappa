package org.demonsoft.spatialkappa.model;

public class LocatedComplex {

    public final Complex complex;
    public final Location location;
    private int hashCode;

    public LocatedComplex(Complex complex, Location location) {
        this.complex = complex;
        this.location = location;
        calculateHashCode();
    }

    @Override
    public String toString() {
        return location + "\t" + complex;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private void calculateHashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((complex == null) ? 0 : complex.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        hashCode =  result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LocatedComplex other = (LocatedComplex) obj;
        if (complex == null) {
            if (other.complex != null)
                return false;
        }
        else if (!complex.equals(other.complex))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        }
        else if (!location.equals(other.location))
            return false;
        return true;
    }

    public boolean isExactMatch(LocatedComplex other) {
        return Utils.equal(location, other.location) && complex.isExactMatch(other.complex);
    }
}