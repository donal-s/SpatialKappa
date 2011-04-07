package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Observation implements Serializable {

    private static final long serialVersionUID = 1L;

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
