package org.demonsoft.spatialkappa.model;


public class LocatedTransform extends LocatedTransition {

    public LocatedTransform(Transform transform, Location location) {
        transition = transform;
        this.sourceLocation = location;
        this.targetLocation = null;
    }

    @Override
    public String toString() {
        return sourceLocation + (targetLocation == null ? "" : " ->" + targetLocation) + "\t" + transition;
    }

    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceLocation == null) ? 0 : sourceLocation.hashCode());
        result = prime * result + ((targetLocation == null) ? 0 : targetLocation.hashCode());
        result = prime * result + ((transition == null) ? 0 : transition.hashCode());
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
        LocatedTransform other = (LocatedTransform) obj;
        if (sourceLocation == null) {
            if (other.sourceLocation != null)
                return false;
        }
        else if (!sourceLocation.equals(other.sourceLocation))
            return false;
        if (targetLocation == null) {
            if (other.targetLocation != null)
                return false;
        }
        else if (!targetLocation.equals(other.targetLocation))
            return false;
        if (transition == null) {
            if (other.transition != null)
                return false;
        }
        else if (!transition.equals(other.transition))
            return false;
        return true;
    }

    @Override
    public LocatedTransform clone() {
        return new LocatedTransform(((Transform) transition).clone(), sourceLocation);
    }
}