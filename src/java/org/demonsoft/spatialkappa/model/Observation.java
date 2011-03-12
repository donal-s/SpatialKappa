package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Observation implements Serializable {

    private static final long serialVersionUID = 1L;

    public final float time;
    public final List<String> orderedObservables;
    public final Map<String, ObservationElement> observables;
    public final boolean finalObservation;
    public final long elapsedTime;
    public final long estimatedRemainingTime;
    
    public Observation(float time, List<String> orderedObservables, Map<String, ObservationElement> observables, boolean finalObservation, long elapsedTime, long estimatedRemainingTime) {
        this.time = time;
        this.observables = observables;
        this.orderedObservables = orderedObservables;
        this.finalObservation = finalObservation;
        this.elapsedTime = elapsedTime;
        this.estimatedRemainingTime = estimatedRemainingTime;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder().append("Time ").append(time).append("\n");
        for (Map.Entry<String, ObservationElement> entry : observables.entrySet()) {
            result.append(entry.getValue() + "\t" + entry.getKey() + "\n");
        }
        return result.toString();
    }
}
