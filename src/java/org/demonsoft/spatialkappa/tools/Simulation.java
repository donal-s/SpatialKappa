package org.demonsoft.spatialkappa.tools;

import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationListener;

public interface Simulation {

    void reset();

    Observation getCurrentObservation();

    void removeObservationListener(ObservationListener listener);

    void addObservationListener(ObservationListener listener);

    void runByEvent(int steps, int stepSize);

    void runByTime(float steps, float stepSize);

    void stop();

    String getDebugOutput();
}
