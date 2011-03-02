package org.demonsoft.spatialkappa.model;

import java.io.Serializable;

public class LocatedObservable implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public final Observable observable;
    public final Location location;
    public final String label;

    public LocatedObservable(Observable observable, Location location) {
        if (observable == null) {
            throw new NullPointerException();
        }
        this.observable = observable;
        this.location = location;
        
        if (location == null) {
            this.label = observable.label;
        }
        else {
            this.label = observable.label + " " + location;
        }
    }

    @Override
    public String toString() {
        if (location == null) {
            return observable.toString();
        }
        return "'" + observable.label + "' " + location + " (" + observable.complex + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((observable == null) ? 0 : observable.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LocatedObservable other = (LocatedObservable) obj;
        if (location == null) {
            if (other.location != null)
                return false;
        }
        else if (!location.equals(other.location))
            return false;
        if (observable == null) {
            if (other.observable != null)
                return false;
        }
        else if (!observable.equals(other.observable))
            return false;
        return true;
    }

}