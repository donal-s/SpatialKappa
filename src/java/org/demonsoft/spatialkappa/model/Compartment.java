package org.demonsoft.spatialkappa.model;


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

    // TODO - remember - only limits when talking about spheres/cylinders/etc
    @Deprecated
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

    // TODO shape handling here
    private int getCellCount() {
        int result = 1;
        for (int dimension : dimensions) {
            result *= dimension;
        }
        return result;
    }

    // TODO target for subclasses
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

}
