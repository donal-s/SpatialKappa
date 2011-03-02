package org.demonsoft.spatialkappa.model;

public interface SimulationState {

    float getTime();

    Transition getTransition(String label);

    LocatedObservable getLocatedObservable(String label);

    ObservationElement getComplexQuantity(LocatedObservable observable);

    void updateTransitionActivity(Transition transition, boolean b);

}
