package org.demonsoft.spatialkappa.model;

import java.util.Map;

public interface SimulationState {

    float getTime();

    Transition getTransition(String label);

//    LocatedObservable getLocatedObservable(String label);

    Variable getVariable(String label);

    ObservationElement getComplexQuantity(Variable variable);

    void updateTransitionActivity(Transition transition, boolean b);

    Map<String, Variable> getVariables();

}
