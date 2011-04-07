package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Map;

public class Observation implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("0.000000E00");
    static {
        DecimalFormatSymbols symbols = VALUE_FORMAT.getDecimalFormatSymbols();
        symbols.setInfinity("Infinity");
        symbols.setNaN("NaN");
        VALUE_FORMAT.setDecimalFormatSymbols(symbols);
    }

    public final int event;
    public final float time;
    public final List<String> orderedObservables;
    public final Map<String, ObservationElement> observables;
    public final boolean finalObservation;
    public final long elapsedTime;
    public final long estimatedRemainingTime;


    
    public Observation(float time, int event, List<String> orderedObservables, Map<String, ObservationElement> observables, boolean finalObservation, long elapsedTime, long estimatedRemainingTime) {
        this.time = time;
        this.event = event;
        this.observables = observables;
        this.orderedObservables = orderedObservables;
        this.finalObservation = finalObservation;
        this.elapsedTime = elapsedTime;
        this.estimatedRemainingTime = estimatedRemainingTime;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder().append("Time ").append(time).append(", Event ").append(event).append("\n");
        for (Map.Entry<String, ObservationElement> entry : observables.entrySet()) {
            result.append(entry.getValue() + "\t" + entry.getKey() + "\n");
        }
        return result.toString();
    }
    
    public String toKaSimString() {
        StringBuilder result = new StringBuilder();
        result.append(" ").append(VALUE_FORMAT.format(time));
        result.append(" ").append(event);
        
        for (String observableName : orderedObservables) {
            ObservationElement element = observables.get(observableName);
            
            if (element.isCompartment) {
                result.append(toKaSimString(element.cellValues));
            }
            result.append(" ").append(VALUE_FORMAT.format(element.value));
        }
        
        result.append("\n");
        return result.toString();
    }

    
    private String toKaSimString(Object cells) {
        StringBuilder result = new StringBuilder();
        if (cells instanceof float[]) {
            for (float cell : (float[]) cells) {
                result.append(" ").append(VALUE_FORMAT.format(cell));
            }
        }
        else {
            for (Object slice : (Object[]) cells) {
                result.append(toKaSimString(slice));
            }
        }
        return result.toString();
    }
    

    
    public String toKaSimHeaderString() {
        StringBuilder result = new StringBuilder();
        result.append("# time E");
        
        for (String observableName : orderedObservables) {
            ObservationElement element = observables.get(observableName);
            
            if (element.isCompartment) {
                String[] cellIndices = getCellIndexStrings(element.dimensions, 0);
                for (String cellIndex : cellIndices) {
                    String name = observableName + "_:loc~" + element.compartmentName + cellIndex;
                    result.append(" '").append(name.replace(' ', '_')).append("'");
                }
            }
            result.append(" '").append(observableName.replace(' ', '_')).append("'");
            
        }
        
        result.append("\n");
        return result.toString();
    }

    
    String[] getCellIndexStrings(int[] dimensions, int index) {
        String prefix = ",loc_index_" + (index + 1) + "~";
        String[] suffixes = new String[] {""};
        
        if (index + 1 < dimensions.length) {
            suffixes = getCellIndexStrings(dimensions, index + 1);
        }
        
        String[] result = new String[dimensions[index] * suffixes.length];
        int resultIndex = 0;
        for (int outer = 0; outer < dimensions[index]; outer++) {
            for (int inner = 0; inner < suffixes.length; inner++) {
                result[resultIndex++] = prefix + outer + suffixes[inner];
            }
        }
        return result;
    }



    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (elapsedTime ^ (elapsedTime >>> 32));
        result = prime * result + (int) (estimatedRemainingTime ^ (estimatedRemainingTime >>> 32));
        result = prime * result + event;
        result = prime * result + (finalObservation ? 1231 : 1237);
        result = prime * result + ((observables == null) ? 0 : observables.hashCode());
        result = prime * result + ((orderedObservables == null) ? 0 : orderedObservables.hashCode());
        result = prime * result + Float.floatToIntBits(time);
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
        Observation other = (Observation) obj;
        if (elapsedTime != other.elapsedTime)
            return false;
        if (estimatedRemainingTime != other.estimatedRemainingTime)
            return false;
        if (event != other.event)
            return false;
        if (finalObservation != other.finalObservation)
            return false;
        if (observables == null) {
            if (other.observables != null)
                return false;
        }
        else if (!observables.equals(other.observables))
            return false;
        if (orderedObservables == null) {
            if (other.orderedObservables != null)
                return false;
        }
        else if (!orderedObservables.equals(other.orderedObservables))
            return false;
        if (Float.floatToIntBits(time) != Float.floatToIntBits(other.time))
            return false;
        return true;
    }

    
    
}
