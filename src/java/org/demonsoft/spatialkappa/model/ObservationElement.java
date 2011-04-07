package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.Arrays;

public class ObservationElement implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final ObservationElement ZERO = new ObservationElement(0);
    
    public final float value;
    public final boolean isCompartment;
    public final int[] dimensions;
    // int array of unknown dimension
    public final Serializable cellValues;
    public final String compartmentName;
    
    public ObservationElement(float value) {
        this.value = value;
        this.isCompartment = false;
        this.dimensions = null;
        this.cellValues = null;
        this.compartmentName = null;
    }
    
    public ObservationElement(int value, int[] dimensions, String compartmentName, Serializable cellValues) {
        if (dimensions == null || compartmentName == null || cellValues == null) {
            throw new NullPointerException();
        }
        
        Object slice = cellValues;
        for (int index = 0; index < dimensions.length - 1; index++) {
            if (((Object[]) slice).length != dimensions[index]) {
                throw new IllegalArgumentException();
            }
            slice = ((Object[]) slice)[0];
        }
        if (((float[]) slice).length != dimensions[dimensions.length - 1]) {
            throw new IllegalArgumentException();
        }

        this.value = value;
        this.isCompartment = true;
        this.dimensions = dimensions;
        this.cellValues = cellValues;
        this.compartmentName = compartmentName;
    }

    public boolean matchesDimensions(ObservationElement other) {
        return isCompartment == other.isCompartment && Arrays.equals(dimensions, other.dimensions);
    }
    
    @Override
    public String toString() {
        if (dimensions == null) {
            return "" + value;
        }
        return value + " " + Arrays.toString(dimensions) + " " + toString(cellValues);
    }
    
    private String toString(Object cells) {
        if (cells instanceof float[]) {
            return Arrays.toString((float[]) cells);
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
        result = prime * result + ((compartmentName == null) ? 0 : compartmentName.hashCode());
        result = prime * result + Arrays.hashCode(dimensions);
        result = prime * result + (isCompartment ? 1231 : 1237);
        result = prime * result + Float.floatToIntBits(value);
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
        if (compartmentName == null) {
            if (other.compartmentName != null)
                return false;
        }
        else if (!compartmentName.equals(other.compartmentName))
            return false;
        if (!Arrays.equals(dimensions, other.dimensions))
            return false;
        if (isCompartment != other.isCompartment)
            return false;
        if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
            return false;
        return true;
    }

    public float getCellValue(int columnIndex, int rowIndex) {
        if (dimensions.length == 1) {
            return ((float[]) cellValues)[columnIndex];
        }
        if (dimensions.length == 2) {
            return ((float[]) ((Object[]) cellValues)[columnIndex])[rowIndex];
        }
        Object slice = ((Object[][]) cellValues)[columnIndex][rowIndex];
        for (int index = 0; index < dimensions.length - 3; index++) {
            slice = ((Object[]) slice)[0];
        }
        return ((float[]) slice)[0];
    }

    public ObservationElement plus(ObservationElement y) {
        if (isCompartment) {
            if (y.isCompartment) {
                throw new RuntimeException("not implemented");
            }
            throw new RuntimeException("not implemented");
        }
        if (y.isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement(value + y.value);
    }

    public ObservationElement minus(ObservationElement y) {
        if (isCompartment) {
            if (y.isCompartment) {
                throw new RuntimeException("not implemented");
            }
            throw new RuntimeException("not implemented");
        }
        if (y.isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement(value - y.value);
    }

    public ObservationElement times(ObservationElement y) {
        if (isCompartment) {
            if (y.isCompartment) {
                throw new RuntimeException("not implemented");
            }
            throw new RuntimeException("not implemented");
        }
        if (y.isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement(value * y.value);
    }

    public ObservationElement dividedBy(ObservationElement y) {
        if (isCompartment) {
            if (y.isCompartment) {
                throw new RuntimeException("not implemented");
            }
            throw new RuntimeException("not implemented");
        }
        if (y.isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement(value / y.value);
    }

    public ObservationElement modulus(ObservationElement y) {
        if (isCompartment) {
            if (y.isCompartment) {
                throw new RuntimeException("not implemented");
            }
            throw new RuntimeException("not implemented");
        }
        if (y.isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement(value % y.value);
    }

    public ObservationElement log() {
        if (isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement((float) Math.log(value));
    }

    public ObservationElement exponent(ObservationElement y) {
        if (isCompartment) {
            if (y.isCompartment) {
                throw new RuntimeException("not implemented");
            }
            throw new RuntimeException("not implemented");
        }
        if (y.isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement((float) Math.pow(value, y.value));
    }

    public ObservationElement sin() {
        if (isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement((float) Math.sin(value));
    }

    public ObservationElement cos() {
        if (isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement((float) Math.cos(value));
    }

    public ObservationElement tan() {
        if (isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement((float) Math.tan(value));
    }

    public ObservationElement sqrt() {
        if (isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement((float) Math.sqrt(value));
    }

    public ObservationElement exp() {
        if (isCompartment) {
            throw new RuntimeException("not implemented");
        }
        
        return new ObservationElement((float) Math.exp(value));
    }

    public float getMean() {
        if (!isCompartment) {
            return value;
        }
        return getSum(cellValues) / getCellCount();
    }

    private int getCellCount() {
        int cellCount = 1;
        for (int index = 0; index < dimensions.length; index++) {
            cellCount = cellCount * dimensions[index];
        }
        return cellCount;
    }

    private float getSum(Object cells) {
        float result = 0;
        if (cells instanceof float[]) {
            for (int index = 0; index < ((float[]) cells).length; index++) {
                float cell = ((float[]) cells)[index];
                if (!Float.isInfinite(cell) && !Float.isNaN(cell)) {
                    result += cell;
                }
            }
        }
        else {
            Object[] values = (Object[]) cells;
            for (int index = 0; index < values.length; index++) {
                result += getSum(values[index]);
            }
        }
        return result;
    }

    public float getStandardDeviation() {
        if (!isCompartment) {
            return value;
        }
        float mean = getMean();
        return (float) Math.sqrt(getVarianceSum(cellValues, mean) / getCellCount());
    }

    private float getVarianceSum(Object cells, float mean) {
        float result = 0;
        if (cells instanceof float[]) {
            for (int index = 0; index < ((float[]) cells).length; index++) {
                float cell = ((float[]) cells)[index];
                if (!Float.isInfinite(cell) && !Float.isNaN(cell)) {
                    result += Math.pow(cell - mean, 2);
                }
            }
        }
        else {
            Object[] values = (Object[]) cells;
            for (int index = 0; index < values.length; index++) {
                result += getVarianceSum(values[index], mean);
            }
        }
        return result;
    }
    
}