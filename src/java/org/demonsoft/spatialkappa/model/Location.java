package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Location implements Serializable {

    private static final Map<String, Integer> NO_VARIABLES = new HashMap<String, Integer>();
    
    private static final long serialVersionUID = 1L;

    public static final Location NOT_LOCATED = new Location("##NOT LOCATED##");
    public static final Location FIXED_LOCATION = new Location("fixed");
    
    private final String name;
    private final CellIndexExpression[] indices;
    private final int[] fixedIndices;
    private int hashCode;
    private final boolean wildcard;

    public Location(String name, List<CellIndexExpression> indices) {
        this(name, indices.toArray(new CellIndexExpression[indices.size()]));
    }

    public Location(String name) {
        this(name, new CellIndexExpression[0]);
    }

    public Location(String name, CellIndexExpression... indices) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;

        boolean fixed = true;
        boolean wildcardIndexFound = false;
        for (CellIndexExpression index : indices) {
            if (!index.isFixed()) {
                fixed = false;
            }
            if (CellIndexExpression.WILDCARD == index) {
                wildcardIndexFound = true;
            }
        }
        if (!fixed && wildcardIndexFound) {
            throw new IllegalArgumentException("Mixed variable and wildcard indices not allowed: " + name);
        }
        if (fixed && !wildcardIndexFound) {
            fixedIndices = new int[indices.length];
            for (int i=0; i < indices.length; i++) {
                fixedIndices[i] = indices[i].evaluateIndex(NO_VARIABLES);
            }
            this.indices = null;
        }
        else {
            this.fixedIndices = null;
            this.indices = indices;
        }
        this.wildcard = wildcardIndexFound;
        calculateHashCode();
    }
    
    public Location(String name, int... indices) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.indices = null;
        this.fixedIndices = indices;
        this.wildcard = false;
        calculateHashCode();
    }
    
    

    public String getName() {
        return name;
    }

    public CellIndexExpression[] getIndices() {
        return indices;
    }

    public boolean isConcreteLocation() {
        return fixedIndices != null;
    }

    public boolean isVoxel(Compartment compartment) {
        if (this == NOT_LOCATED) {
            throw new IllegalStateException();
        }
        if (!compartment.getName().equals(name)) {
            throw new IllegalArgumentException("Compartment mismatch: " + compartment.getName());
        }
        return compartment.getDimensions().length == (isConcreteLocation() ? fixedIndices.length : indices.length);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(name);
        if (isConcreteLocation()) {
            if (fixedIndices.length == 0) {
                return name;
            }
            for (int index : fixedIndices) {
                builder.append("[").append(index).append("]");
            }
        }
        else {
            if (indices == null || indices.length == 0) {
                return name;
            }
            for (CellIndexExpression expression : indices) {
                builder.append("[").append(expression).append("]");
            }
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public Location getConcreteLocation(Map<String, Integer> variables) {
        if (variables == null) {
            throw new NullPointerException();
        }
        if (isConcreteLocation()) {
            return this;
        }
        int[] newIndices = new int[this.indices.length];
        for (int index = 0; index < indices.length; index++) {
            newIndices[index] = indices[index].evaluateIndex(variables);
        }
        return new Location(name, newIndices);
    }
    
    private void calculateHashCode() {
        final int prime = 31;
        hashCode = 1;
        hashCode = prime * hashCode + Arrays.hashCode(fixedIndices);
        hashCode = prime * hashCode + hashCode;
        hashCode = prime * hashCode + Arrays.hashCode(indices);
        hashCode = prime * hashCode + ((name == null) ? 0 : name.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Location other = (Location) obj;
        if (!Arrays.equals(fixedIndices, other.fixedIndices))
            return false;
        if (hashCode != other.hashCode)
            return false;
        if (!Arrays.equals(indices, other.indices))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        return true;
    }

    public boolean matches(Location location, boolean matchNameOnly) {
        if (matchNameOnly) {
            return location != null && name.equals(location.name);
        }
        return equals(location);
    }
    
    public List<Location> getLinkedLocations(List<Compartment> compartments, Channel... channels) {
        List<Location> result = new ArrayList<Location>();
        for (Channel channel : channels) {
            result.addAll(channel.applyChannel(this, NOT_LOCATED, compartments));
        }
        return result;
    }

    public boolean isRefinement(Location location) {
        if (location == null) {
            throw new NullPointerException();
        }
        if (this == NOT_LOCATED && location != NOT_LOCATED) {
            return true;
        }

        if (wildcard) {
            return isWildcardRefinement(location);
        }
        
        if (this.name.equals(location.name)) {
            int indexSize = isConcreteLocation() ? fixedIndices.length : indices.length;
            int otherIndexSize = location.isConcreteLocation() ? location.fixedIndices.length : location.indices.length;
            return (indexSize < otherIndexSize);
        }
        return false;
        

    }

    public int[] getFixedIndices() {
        return fixedIndices;
    }

    public boolean isCompartment() {
        return (fixedIndices != null) ? fixedIndices.length == 0 : indices.length == 0;
    }

    public int getDimensionCount() {
        return (fixedIndices != null) ? fixedIndices.length : indices.length;
    }

    private boolean isWildcardRefinement(Location location) {
        if (!location.isConcreteLocation() || location.getDimensionCount() != getDimensionCount()) {
            return false;
        }
        if (!this.name.equals(location.name)) {
            return false;
        }
        for (int index = 0; index < getDimensionCount(); index++) {
            
            if (CellIndexExpression.WILDCARD != indices[index] && indices[index].evaluateIndex() != location.fixedIndices[index]) {
                return false;
            }
        }
        return true;
    }

    public boolean isWildcard() {
        return wildcard;
    }

}
