package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.Arrays;

public class ObservationElement implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public final int value;
    public final boolean isCompartment;
    public final int[] dimensions;
    // int array of unknown dimension
    public final Serializable cellValues;
    
    public ObservationElement(int value) {
        this.value = value;
        this.isCompartment = false;
        this.dimensions = null;
        this.cellValues = null;
    }
    
    public ObservationElement(int value, int[] dimensions, Serializable cellValues) {
        if (dimensions == null || cellValues == null) {
            throw new NullPointerException();
        }
        
        Object slice = cellValues;
        for (int index = 0; index < dimensions.length - 1; index++) {
            if (((Object[]) slice).length != dimensions[index]) {
                throw new IllegalArgumentException();
            }
            slice = ((Object[]) slice)[0];
        }
        if (((int[]) slice).length != dimensions[dimensions.length - 1]) {
            throw new IllegalArgumentException();
        }

        this.value = value;
        this.isCompartment = true;
        this.dimensions = dimensions;
        this.cellValues = cellValues;
    }

    @Override
    public String toString() {
        if (dimensions == null) {
            return "" + value;
        }
        return value + " " + Arrays.toString(dimensions) + " " + toString(cellValues);
    }
    
    private String toString(Object cells) {
        if (cells instanceof int[]) {
            return Arrays.toString((int[]) cells);
        }
        Object[] values = (Object[]) cells;
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(toString(values[index]));
        }
        builder.append("]");
        return builder.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cellValues == null) ? 0 : cellValues.hashCode());
        result = prime * result + Arrays.hashCode(dimensions);
        result = prime * result + (isCompartment ? 1231 : 1237);
        result = prime * result + value;
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
        ObservationElement other = (ObservationElement) obj;
        if (cellValues == null) {
            if (other.cellValues != null)
                return false;
        }
        else if (!cellValues.equals(other.cellValues))
            return false;
        if (!Arrays.equals(dimensions, other.dimensions))
            return false;
        if (isCompartment != other.isCompartment)
            return false;
        if (value != other.value)
            return false;
        return true;
    }

    public int getCellValue(int columnIndex, int rowIndex) {
        if (dimensions.length == 1) {
            return ((int[]) cellValues)[columnIndex];
        }
        if (dimensions.length == 2) {
            return ((int[][]) cellValues)[columnIndex][rowIndex];
        }
        Object slice = ((Object[][]) cellValues)[columnIndex][rowIndex];
        for (int index = 0; index < dimensions.length - 3; index++) {
            slice = ((Object[]) slice)[0];
        }
        return ((int[]) slice)[0];
    }
    
    
}