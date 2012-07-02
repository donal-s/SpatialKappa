package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.List;

public class Compartment {

    private final String name;
    private final int[] dimensions;
    
    public Compartment(String name, int... dimensions) {
        if (name == null) {
            throw new NullPointerException();
        }
        for (int dimension : dimensions) {
            if (dimension <= 0) {
                throw new IllegalArgumentException("" + dimension);
            }
        }
        this.name = name;
        this.dimensions = dimensions;
    }

    public String getName() {
        return name;
    }

    public int[] getDimensions() {
        return dimensions;
    }
    
    @Override
    public String toString() {
        if (dimensions == null || dimensions.length == 0) {
            return name;
        }
        StringBuilder builder = new StringBuilder(name);
        for (int dimension : dimensions) {
            builder.append("[").append(dimension).append("]");
        }
        return builder.toString();
    }

    private int getCellCount() {
        int result = 1;
        for (int dimension : dimensions) {
            result *= dimension;
        }
        return result;
    }

    public Location[] getDistributedCellReferences() {
        if (dimensions.length == 0) {
            return new Location[] { new Location(name) };
        }
        
        CellIndexExpression[][] indices = new CellIndexExpression[getCellCount()][dimensions.length];
        int cellRunLength = 1;
        int cycleLength = 1;
        for (int dimensionIndex = 0; dimensionIndex < dimensions.length; dimensionIndex++) {
            cycleLength *= dimensions[dimensionIndex];
            for (int cellIndex = 0; cellIndex < indices.length; cellIndex++) {
                indices[cellIndex][dimensionIndex] = new CellIndexExpression("" + ((cellIndex / cellRunLength) % cycleLength));
            }
            cellRunLength = cycleLength;
        }

        Location[] result = new Location[indices.length];
        for (int index = 0; index < indices.length; index++) {
            result[index] = new Location(name, indices[index]);
        }
        return result;
    }

    public Serializable createValueArray() {
        Serializable result = (Serializable) Array.newInstance(float.class, dimensions);
        return result;
    }

    public static Location[] getDistributedCellReferences(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        if (compartments.size() == 0) {
            throw new IllegalArgumentException();
        }
        
        int totalCellCount = 0;
        for (Compartment compartment : compartments) {
            totalCellCount += compartment.getCellCount();
        }

        Location[] result = new Location[totalCellCount];
        int resultIndex = 0;
        
        for (Compartment compartment : compartments) {
            Location[] compartmentResult = compartment.getDistributedCellReferences();
            System.arraycopy(compartmentResult, 0, result, resultIndex, compartmentResult.length);
            resultIndex += compartmentResult.length;
        }
        return result;
    }
    
}
