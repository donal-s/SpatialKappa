package org.demonsoft.spatialkappa.model;


class LocatedTransport extends LocatedTransition {

    public LocatedTransport(Transport transport, Location sourceLocation, Location targetLocation) {
        transition = transport;
        this.sourceLocation = sourceLocation;
        this.targetLocation = targetLocation;
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
        LocatedTransport other = (LocatedTransport) obj;
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
    public LocatedTransport clone() {
        return new LocatedTransport(((Transport) transition).clone(), sourceLocation, targetLocation);
    }

}