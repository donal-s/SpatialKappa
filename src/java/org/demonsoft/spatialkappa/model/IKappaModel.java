package org.demonsoft.spatialkappa.model;

import java.util.List;
import java.util.Map;




public interface IKappaModel {

    public void addTransform(Direction direction, String label, List<Agent> leftSideAgents, List<Agent> rightSideAgents, VariableExpression forwardRate, VariableExpression backwardRate,
            Location location);

    public void addTransport(String label, String compartmentLinkName, List<Agent> agents, VariableExpression rate);

    public void addInitialValue(List<Agent> agents, String valueText, Location compartment);
    public void addInitialValue(List<Agent> agents, VariableReference reference, Location compartment);

    public void addVariable(List<Agent> agents, String label, Location location);
    public void addVariable(VariableExpression expression, String label);

    public void addPlot(String label);

    public void addPerturbation(Perturbation perturbation);

    public void addCompartment(String name, List<Integer> dimensions);
    public void addCompartment(Compartment compartment);

    public void addCompartmentLink(CompartmentLink link);



    public void validate();
    
    public Map<String, Variable> getVariables();

    public List<InitialValue> getInitialValues();

    public Map<LocatedComplex, Integer> getFixedLocatedInitialValuesMap();

    public List<String> getPlottedVariables();

    public List<LocatedTransition> getFixedLocatedTransitions();

    public List<LocatedTransform> getLocatedTransforms();

    public List<Compartment> getCompartments();
    
    public List<CompartmentLink> getCompartmentLinks();
    
    public List<Transport> getTransports();
    
    public Map<String, AggregateAgent> getAggregateAgentMap();
    
    public List<Perturbation> getPerturbations();

}
