package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.Map;

public class Observation implements Serializable {

    private static final long serialVersionUID = 1L;

    public final float time;
    public final Map<LocatedObservable, ObservationElement> observables;
    public final boolean finalObservation;
    public final long elapsedTime;
    public final long estimatedRemainingTime;
    
    public Observation(float time, Map<LocatedObservable, ObservationElement> observables, boolean finalObservation, long elapsedTime, long estimatedRemainingTime) {
        this.time = time;
        this.observables = observables;
        this.finalObservation = finalObservation;
        this.elapsedTime = elapsedTime;
        this.estimatedRemainingTime = estimatedRemainingTime;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder().append("Time ").append(time).append("\n");
        for (Map.Entry<LocatedObservable, ObservationElement> entry : observables.entrySet()) {
            result.append(entry.getValue() + "\t" + entry.getKey() + "\n");
        }
        return result.toString();
    }
}
