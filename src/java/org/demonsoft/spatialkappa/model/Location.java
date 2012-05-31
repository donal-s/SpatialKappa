package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Location implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Location NOT_LOCATED = new Location("##NOT LOCATED##");
    
    private final String name;
    private final CellIndexExpression[] indices;
    private int hashCode;

    public Location(String name, List<CellIndexExpression> indices) {
        this(name, indices.toArray(new CellIndexExpression[indices.size()]));
    }

    public Location(String name, CellIndexExpression... indices) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.indices = indices;
        calculateHashCode();
    }

    public String getName() {
        return name;
    }

    public CellIndexExpression[] getIndices() {
        return indices;
    }

    public Compartment getReferencedCompartment(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        for (Compartment compartment : compartments) {
            if (name.equals(compartment.getName())) {
                return compartment;
            }
        }
        return null;
    }

    public boolean isConcreteLocation() {
        for (CellIndexExpression index : indices) {
            if (!index.isFixed()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        if (indices == null || indices.length == 0) {
            return name;
        }
        StringBuilder builder = new StringBuilder(name);
        for (CellIndexExpression expression : indices) {
            builder.append("[").append(expression).append("]");
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private void calculateHashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(indices);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        hashCode = result;
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

    public Location getConcreteLocation(Map<String, Integer> variables) {
        if (variables == null) {
            throw new NullPointerException();
        }
        CellIndexExpression[] newIndices = new CellIndexExpression[this.indices.length];
        for (int index = 0; index < indices.length; index++) {
            newIndices[index] = new CellIndexExpression("" + indices[index].evaluateIndex(variables));
        }
        return new Location(name, newIndices);
    }
    
    public boolean matches(Location location, boolean matchNameOnly) {
        if (matchNameOnly) {
            return location != null && name.equals(location.name);
        }
        return equals(location);
    }
    
    public static boolean doLocationsMatch(Location location1, Location location2, boolean matchNameOnly) {
        return location1 == null || location1.matches(location2, matchNameOnly);
    }

    public List<Location> getLinkedLocations(List<Compartment> compartments, Channel... channels) {
        Compartment compartment = getReferencedCompartment(compartments);
        if (compartment == null) {
            throw new IllegalArgumentException("Unknown compartment: " + name);
        }
        List<Location> result = new ArrayList<Location>();
        for (Channel channel : channels) {
            if (channel.canUseChannel(this, compartments)) {
                result.addAll(channel.applyChannel(this, compartments));
            }
        }
        return result;
    }
    

}
