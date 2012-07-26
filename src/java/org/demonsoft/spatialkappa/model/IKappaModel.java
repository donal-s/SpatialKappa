package org.demonsoft.spatialkappa.model;

import java.util.List;
import java.util.Map;

public interface IKappaModel {

    public void addTransition(String label, Location leftLocation, List<Agent> leftSideAgents, 
            String channelName, Location rightLocation, List<Agent> rightSideAgents, VariableExpression rate);

    public void addInitialValue(List<Agent> agents, String valueText, Location compartment);
    public void addInitialValue(List<Agent> agents, VariableReference reference, Location compartment);

    public void addVariable(List<Agent> agents, String label, Location location);
    public void addVariable(VariableExpression expression, String label);

    public void addPlot(String label);

    public void addPerturbation(Perturbation perturbation);

    public void addCompartment(String name, String type, List<Integer> dimensions);
    public void addCompartment(Compartment compartment);

    public void addChannel(Channel link);

    public void addAgentDeclaration(AggregateAgent agent);

    public void validate();
    
    public Map<String, Variable> getVariables();

    public List<InitialValue> getInitialValues();

    public Map<Complex, Integer> getFixedLocatedInitialValuesMap();

    public List<String> getPlottedVariables();



    public List<Compartment> getCompartments();
    
    public List<Channel> getChannels();
    public Channel getChannel(String channelName);

    
    public Map<String, AggregateAgent> getAgentDeclarationMap();
    
    public List<Perturbation> getPerturbations();
    
    public List<String> getOrderedVariableNames();

    
    public List<Transition> getTransitions();

//    public List<LocatedTransition> getFixedLocatedTransitions();
    
}
