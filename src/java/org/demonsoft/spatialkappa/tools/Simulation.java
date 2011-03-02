package org.demonsoft.spatialkappa.tools;

import java.util.List;

import org.demonsoft.spatialkappa.model.LocatedObservable;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationListener;

public interface Simulation {

    void reset();

    Observation getCurrentObservation();

    void removeObservationListener(ObservationListener listener);

    List<LocatedObservable> getLocatedObservables();

    void addObservationListener(ObservationListener listener);

    void runByEvent(int steps, int stepSize);

    void runByTime(float steps, float stepSize);

    void stop();

    String getDebugOutput();
}
