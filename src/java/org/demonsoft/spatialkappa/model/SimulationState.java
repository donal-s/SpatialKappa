package org.demonsoft.spatialkappa.model;

import java.util.List;
import java.util.Map;

public interface SimulationState {

    float getTime();
    int getEventCount();

    Map<String, Variable> getVariables();
    Variable getVariable(String label);

    ObservationElement getComplexQuantity(Variable variable);
    ObservationElement getTransitionFiredCount(Variable variable);

    void addComplexInstances(List<Agent> agents, int amount);
    void setTransitionRate(String transitionName, VariableExpression rateExpression);
    void stop();
    void snapshot();
}
