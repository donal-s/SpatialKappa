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

    public int[] getDistributedCellCounts(int quantity) {
        return getDistributedCellCounts(quantity, this);
    }

    private int getCellCount() {
        int result = 1;
        for (int dimension : dimensions) {
            result *= dimension;
        }
        return result;
    }

    public String[] getCellStateSuffixes() {
        String[] suffixes = new String[] {"loc~" + name };
        for (int dimensionIndex = 0; dimensionIndex < dimensions.length; dimensionIndex++) {
            int currentDimension = dimensions[dimensionIndex];
            
            String currentName = (dimensions.length == 1) ? ",loc_index~" : ",loc_index_" + (dimensionIndex + 1) + "~";
            String[] newSuffixes = new String[suffixes.length * currentDimension];
            
            
            for (int index = 0; index < currentDimension; index++) {
                String currentSuffix = currentName + index;
                for (int previousIndex = 0; previousIndex < suffixes.length; previousIndex++) {
                    newSuffixes[index * suffixes.length + previousIndex] = suffixes[previousIndex] + currentSuffix;
                }
            }
            
            suffixes = newSuffixes;
        }
        return suffixes;
    }

    public Location[] getDistributedCellReferences() {
        if (dimensions.length == 0) {
            return new Location[] { new Location(name) };
        }
        
        MathExpression[][] indices = new MathExpression[getCellCount()][dimensions.length];
        int cellRunLength = 1;
        int cycleLength = 1;
        for (int dimensionIndex = 0; dimensionIndex < dimensions.length; dimensionIndex++) {
            cycleLength *= dimensions[dimensionIndex];
            for (int cellIndex = 0; cellIndex < indices.length; cellIndex++) {
                indices[cellIndex][dimensionIndex] = new MathExpression("" + ((cellIndex / cellRunLength) % cycleLength));
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
        Serializable result = (Serializable) Array.newInstance(int.class, dimensions);
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

    public static int[] getDistributedCellCounts(int quantity, List<Compartment> compartments) {
        return getDistributedCellCounts(quantity, compartments.toArray(new Compartment[0]));
    }

    private static int[] getDistributedCellCounts(int quantity, Compartment... compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        if (compartments.length == 0) {
            throw new IllegalArgumentException();
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("" + quantity);
        }
        int totalCellCount = 0;
        for (Compartment compartment : compartments) {
            totalCellCount += compartment.getCellCount();
        }
        
        int[] result = new int[totalCellCount];
        int baseValue = quantity / totalCellCount;
        int remainder = quantity % totalCellCount;
        for (int index = 0; index < totalCellCount; index++) {
            result[index] = baseValue + (index < remainder ? 1 : 0);
        }
        return result;
    }
    
}
