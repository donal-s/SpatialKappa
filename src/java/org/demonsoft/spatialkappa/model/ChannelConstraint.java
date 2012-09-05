package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Location.*;

public class ChannelConstraint {
    
    public static final ChannelConstraint FIXED_CONSTRAINT = new ChannelConstraint(FIXED_LOCATION, FIXED_LOCATION);
    
    public final Location sourceLocation;
    public final Location targetConstraint;
    
    public ChannelConstraint(Location sourceLocation, Location targetConstraint) {
        this.sourceLocation = sourceLocation;
        this.targetConstraint = targetConstraint;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceLocation == null) ? 0 : sourceLocation.hashCode());
        result = prime * result + ((targetConstraint == null) ? 0 : targetConstraint.hashCode());
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
        ChannelConstraint other = (ChannelConstraint) obj;
        if (sourceLocation == null) {
            if (other.sourceLocation != null)
                return false;
        }
        else if (!sourceLocation.equals(other.sourceLocation))
            return false;
        if (targetConstraint == null) {
            if (other.targetConstraint != null)
                return false;
        }
        else if (!targetConstraint.equals(other.targetConstraint))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "[" + sourceLocation + " -> " + targetConstraint + "]";
    }
    
}