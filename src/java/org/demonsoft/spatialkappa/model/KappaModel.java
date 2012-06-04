package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.demonsoft.spatialkappa.model.Variable.Type;
import org.demonsoft.spatialkappa.parser.SpatialKappaLexer;
import org.demonsoft.spatialkappa.parser.SpatialKappaParser;
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker;


public class KappaModel implements IKappaModel {

    private static final ComplexMatcher matcher = new ComplexMatcher();
    
    private final List<LocatedTransform> locatedTransforms = new ArrayList<LocatedTransform>();
    private final Map<String, AggregateAgent> agentDeclarationMap = new HashMap<String, AggregateAgent>();
    private final Map<String, AggregateAgent> aggregateAgentMap = new HashMap<String, AggregateAgent>();
    private final List<InitialValue> initialValues = new ArrayList<InitialValue>();
    private final List<Perturbation> perturbations = new ArrayList<Perturbation>();
    private final List<Compartment> compartments = new ArrayList<Compartment>();
    private final List<Channel> channels = new ArrayList<Channel>();
    private final List<Transport> transports = new ArrayList<Transport>();
    private final Set<Complex> canonicalComplexes = new HashSet<Complex>();
    private final List<String> plottedVariables = new ArrayList<String>();
    private final Map<String, Variable> variables = new HashMap<String, Variable>();
	private final List<String> orderedVariableNames = new ArrayList<String>();


    public void addTransform(String label, List<Agent> leftSideAgents, List<Agent> rightSideAgents, VariableExpression rate, Location location) {
        addTransform(new LocatedTransform(new Transform(label, leftSideAgents, rightSideAgents, rate, false), location));
        if (label != null) {
            variables.put(label, new Variable(label));
        }
    }


    private void addTransform(LocatedTransform transform) {
        if (transform == null) {
            throw new NullPointerException();
        }
        locatedTransforms.add(transform);
        for (Complex complex : transform.transition.sourceComplexes) {
        	propogateLocation(complex.agents, transform.sourceLocation);
            for (Agent agent : complex.agents) {
                aggregateAgent(agent);
            }
        }
        for (Complex complex : transform.transition.targetComplexes) {
        	propogateLocation(complex.agents, transform.sourceLocation);
            for (Agent agent : complex.agents) {
                aggregateAgent(agent);
            }
        }
    }

    private void aggregateAgent(Agent agent) {
        if (aggregateAgentMap.get(agent.name) == null) {
            aggregateAgentMap.put(agent.name, new AggregateAgent(agent.name));
        }
        aggregateAgentMap.get(agent.name).addSites(agent.getSites());
    }

    public void addInitialValue(List<Agent> agents, String valueText, Location location) {
        if (agents == null || valueText == null) {
            throw new NullPointerException();
        }
        if (agents.size() == 0) {
            throw new IllegalArgumentException("Empty complex");
        }
        int quantity = Integer.parseInt(valueText);
        propogateLocation(agents, location);
        for (Agent agent : agents) {
            aggregateAgent(agent);
        }
        List<Complex> complexes = getCanonicalComplexes(Utils.getComplexes(agents));
        
        initialValues.add(new InitialValue(complexes, quantity, location));
    }

    private void propogateLocation(List<Agent> agents, Location location) {
		for (Agent agent : agents) {
			if (agent.location == NOT_LOCATED) {
				agent.setLocation(location);
			}
		}
	}


	public void addInitialValue(List<Agent> agents, VariableReference reference, Location location) {
        if (agents == null || reference == null) {
            throw new NullPointerException();
        }
        if (agents.size() == 0) {
            throw new IllegalArgumentException("Empty complex");
        }
        propogateLocation(agents, location);
        for (Agent agent : agents) {
            aggregateAgent(agent);
        }
        List<Complex> complexes = getCanonicalComplexes(Utils.getComplexes(agents));
        
        initialValues.add(new InitialValue(complexes, reference, location));
    }

    private List<Complex> getCanonicalComplexes(List<Complex> complexes) {
        for (int index = 0; index < complexes.size(); index++) {
            boolean found = false;
            for (Complex current : canonicalComplexes) {
                if (matcher.isExactMatch(current, complexes.get(index), true)) {
                    complexes.set(index, current);
                    break;
                }
            }
            if (!found) {
                canonicalComplexes.add(complexes.get(index));
            }
        }
        return complexes;
    }

    public void addVariable(List<Agent> agents, String label, Location location) {
        variables.put(label, new Variable(new Complex(agents), location, label));
        orderedVariableNames.add(label);
        propogateLocation(agents, location);

        for (Agent agent : agents) {
            aggregateAgent(agent);
        }
    }
    
    public void addVariable(VariableExpression expression, String label) {
        variables.put(label, new Variable(expression, label));
        orderedVariableNames.add(label);
    }


    public void addPlot(String label) {
        if (label == null) {
            throw new NullPointerException();
        }
        plottedVariables.add(label);
    }

    public void addPerturbation(Perturbation perturbation) {
        perturbations.add(perturbation);
    }

    public void addCompartment(String name, List<Integer> dimensions) {
        if (name == null || dimensions == null) {
            throw new NullPointerException();
        }
        int[] dimArray = new int[dimensions.size()];
        for (int index = 0; index < dimensions.size(); index++) {
            dimArray[index] = dimensions.get(index);
        }
        addCompartment(new Compartment(name, dimArray));
    }

    public void addCompartment(Compartment compartment) {
        if (compartment == null) {
            throw new NullPointerException();
        }
        compartments.add(compartment);
    }

    public void addChannel(Channel channel) {
        if (channel == null) {
            throw new NullPointerException();
        }
        channels.add(channel);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("COMPARTMENTS\n");
        for (Compartment compartment : compartments) {
            result.append(compartment).append("\n");
        }
        result.append("\nCOMPARTMENT LINKS\n");
        for (Channel link : channels) {
            result.append(link).append("\n");
        }
        result.append("\nTRANSPORT RULES\n");
        for (Transport transport : transports) {
            result.append(transport).append("\n");
        }
        result.append("\nAGENTS\n");
        for (AggregateAgent agent : aggregateAgentMap.values()) {
            result.append(agent).append("\n");
        }
        result.append("\nTRANSFORM RULES\n");
        for (LocatedTransition transform : locatedTransforms) {
            result.append(transform).append("\n");
        }
        result.append("\nINITIAL VALUES\n");
        for (InitialValue initialValue : initialValues) {
            result.append(initialValue).append("\n");
        }
        result.append("\nVARIABLES\n");
        for (Variable variable : variables.values()) {
            result.append(variable).append("\n");
        }
        result.append("\nPERTURBATIONS\n");
        for (Perturbation perturbation : perturbations) {
            result.append(perturbation).append("\n");
        }
        return result.toString();
    }

    public Map<LocatedComplex, Integer> getFixedLocatedInitialValuesMap() {
        Map<LocatedComplex, Integer> result = new HashMap<LocatedComplex, Integer>();
        
        for (InitialValue initialValue : initialValues) {
            boolean partition = false;
            Compartment compartment = null;
            
            int quantity = initialValue.quantity;
            if (initialValue.reference != null) {
                quantity = variables.get(initialValue.reference.variableName).expression.evaluate(this);
            }
            
            Location location = initialValue.location;
            if (location != NOT_LOCATED) {
                compartment = location.getReferencedCompartment(compartments);
                if (compartment != null && compartment.getDimensions().length != location.getIndices().length) {
                    partition = true;
                }

                if (partition && compartment != null) {
                    int[] cellCounts = compartment.getDistributedCellCounts(quantity);
                    Location[] cellLocations = compartment.getDistributedCellReferences();
                    
                    for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                        Location cellLocation = cellLocations[cellIndex];
                        for (Complex complex : initialValue.complexes) {
                            addInitialLocatedValue(result, complex, cellLocation, cellCounts[cellIndex]);
                        }
                    }
                }
                else {
                    for (Complex complex : initialValue.complexes) {
                        addInitialLocatedValue(result, complex, location, quantity);
                    }
                }
            }
            else { // location == null
                if (compartments.size() > 0) {
                    int[] cellCounts = Compartment.getDistributedCellCounts(initialValue.quantity, compartments);
                    Location[] cellLocations = Compartment.getDistributedCellReferences(compartments);
                    
                    for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                        Location cellLocation = cellLocations[cellIndex];
                        for (Complex complex : initialValue.complexes) {
                            addInitialLocatedValue(result, complex, cellLocation, cellCounts[cellIndex]);
                        }
                    }
            }
                else { // no compartments
                    for (Complex complex : initialValue.complexes) {
                        addInitialLocatedValue(result, complex, location, quantity);
                    }
                }

            }
        }

        return result;
    }

    private void addInitialLocatedValue(Map<LocatedComplex, Integer> result, Complex complex, Location location, int quantity) {
        LocatedComplex locatedComplex = new LocatedComplex(complex, location);
        for (Map.Entry<LocatedComplex, Integer> entry : result.entrySet()) {
            if (locatedComplex.isExactMatch(entry.getKey())) {
                entry.setValue(entry.getValue() + quantity);
                return;
            }
        }
        result.put(locatedComplex, quantity);
    }

    public void addTransport(String label, String compartmentLinkName, List<Agent> agents, VariableExpression rate) {
        addTransport(new Transport(label, compartmentLinkName, agents, rate));
        if (label != null) {
            variables.put(label, new Variable(label));
        }
    }

    private void addTransport(Transport transport) {
        transports.add(transport);
    }

    public List<LocatedTransition> getFixedLocatedTransitions() {
        List<LocatedTransition> result = new ArrayList<LocatedTransition>();
        for (LocatedTransform transition : locatedTransforms) {
            Location location = transition.sourceLocation;
            if (location != NOT_LOCATED) {
                Compartment compartment = location.getReferencedCompartment(compartments);
                if (compartment.getDimensions().length != location.getIndices().length) {
                    Location[] cellLocations = compartment.getDistributedCellReferences();
                    Transform cloneTransform = ((Transform) transition.transition).clone();
                    for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                        result.add(new LocatedTransform(cloneTransform, cellLocations[cellIndex]));
                    }
                }
                else {
                    result.add(transition.clone());
                }
            }
            else { // location == NOT_LOCATED
                if (compartments.size() > 0) {
                    Transform cloneTransform = ((Transform) transition.transition).clone();
                    for (Compartment compartment : compartments) {
                        Location[] cellLocations = compartment.getDistributedCellReferences();
                        for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                            result.add(new LocatedTransform(cloneTransform, cellLocations[cellIndex]));
                        }
                    }
                }
                else { // No compartments
                    result.add(transition.clone());
                }
            }
        }

        for (Transport transport : transports) {
            List<Channel> links = getChannels(transport.getCompartmentLinkName());
            if (links.size() > 0) {
                Transport cloneTransport = transport.clone();
                for (Channel link : links) {
                    Location[][] cellLocations = link.getCellReferencePairs(compartments);
                    for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                        Location sourceReference = cellLocations[cellIndex][0];
                        Location targetReference = cellLocations[cellIndex][1];
    
                        result.add(new LocatedTransport(cloneTransport, sourceReference, targetReference));
                    }
                }
            }
        }
        return result;
    }

    private List<Channel> getChannels(String channelName) {
        List<Channel> result = new ArrayList<Channel>();
        for (Channel current : channels) {
            if (current.getName().equals(channelName)) {
                result.add(current);
            }
        }
        return result;
    }

    public List<LocatedTransform> getLocatedTransforms() {
        return locatedTransforms;
    }

    public List<Compartment> getCompartments() {
        return compartments;
    }
    
    public List<Channel> getChannels() {
        return channels;
    }
    
    public List<Transport> getTransports() {
        return transports;
    }
    
    Map<String, AggregateAgent> getAggregateAgentMap() {
        return aggregateAgentMap;
    }
    
    public List<Perturbation> getPerturbations() {
        return perturbations;
    }
    
    public List<InitialValue> getInitialValues() {
        return initialValues;
    }

    public List<String> getPlottedVariables() {
        return plottedVariables;
    }
    
    public Map<String, Variable> getVariables() {
        return variables;
    }

	public List<String> getOrderedVariableNames() {
		return orderedVariableNames;
	}
    



    public void validate() {
        
        for (Variable variable : variables.values()) {
            if (Variable.Type.VARIABLE_EXPRESSION == variable.type && VariableExpression.Type.VARIABLE_REFERENCE == variable.expression.type) {
                VariableReference reference = variable.expression.reference;
                Variable other = variables.get(reference.variableName);
                if (other == null) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not found");
                }
            }
        }
        
        for (Channel channel : channels) {
            channel.validate(compartments);
        }
       
        for (InitialValue initialValue : initialValues) {
            if (initialValue.reference != null) {
                VariableReference reference = initialValue.reference;
                Variable variable = variables.get(reference.variableName);
                if (variable == null) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not found");
                }
                if (!variable.expression.isFixed(variables)) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not fixed - cannot be initial value");
                }
                if (variable.expression.isInfinite(variables)) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' evaluates to infinity - cannot be initial value");
                }
                
                
            }
        }
        
        for (LocatedTransform transform : locatedTransforms) {
            VariableReference reference = transform.transition.rate.reference;
            if (reference != null) {
                Variable variable = variables.get(reference.variableName);
                if (variable == null) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not found");
                }
                if (!variable.expression.isFixed(variables)) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not fixed");
                }
            }
        }
        
        for (Transport transport : transports) {
            VariableReference reference = transport.rate.reference;
            if (reference != null) {
                Variable variable = variables.get(reference.variableName);
                if (variable == null) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not found");
                }
                if (!variable.expression.isFixed(variables)) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not fixed");
                }
            }
            
            boolean found = false;
            for (Channel link : channels) {
                if (transport.getCompartmentLinkName().equals(link.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException("Compartment link '" + transport.getCompartmentLinkName() + "' not found");
            }
        }
        
        for (String reference : plottedVariables) {
            boolean found = false;
            for (Transport transport : transports) {
                if (reference.equals(transport.label)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                continue;
            }
            for (LocatedTransform transform : locatedTransforms) {
                if (reference.equals(transform.transition.label)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                continue;
            }
            
            Variable variable = variables.get(reference);
            if (variable == null) {
                throw new IllegalStateException("Reference '" + reference + "' not found");
            }
            if (variable.type == Type.VARIABLE_EXPRESSION && variable.expression.isInfinite(variables)) {
                throw new IllegalStateException("Reference '" + reference + "' evaluates to infinity - cannot plot");
            }
        }
        
    	for (AggregateAgent agent : aggregateAgentMap.values()) {
    		if (!agentDeclarationMap.containsKey(agent.getName())) {
                throw new IllegalStateException("Agent '" + agent.getName() + "' not declared");
    		}
    		AggregateAgent declaredAgent = agentDeclarationMap.get(agent.getName());
    		Map<String, AggregateSite> declaredSites = new HashMap<String, AggregateSite>();
    		for (AggregateSite site : declaredAgent.getSites()) {
    			declaredSites.put(site.getName(), site);
    		}
    		for (AggregateSite site : agent.getSites()) {
    			if (!declaredSites.containsKey(site.getName())) {
    				throw new IllegalStateException("Agent site " + agent.getName() + "(" + site.getName() + ") not declared");
    			}
    			AggregateSite declaredSite = declaredSites.get(site.getName());
    			for (String state : site.getStates()) {
    				if (!declaredSite.getStates().contains(state)) {
    					throw new IllegalStateException("Agent state " + agent.getName() + "(" + site.getName() + "~" + state + ") not declared");
    				}
    			}
    			declaredSite.getLinks().addAll(site.getLinks());
    		}
    	}
    	
    }
    
    public static IKappaModel createModel(File inputFile) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(inputFile));
        CommonTokenStream tokens = new CommonTokenStream(new SpatialKappaLexer(input));
        SpatialKappaParser.prog_return r = new SpatialKappaParser(tokens).prog();
        CommonTree t = (CommonTree) r.getTree();

        CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
        nodes.setTokenStream(tokens);
        SpatialKappaWalker walker = new SpatialKappaWalker(nodes);
        return walker.prog();
    }



    public static class ModelOnlySimulationState implements SimulationState {

        private final Map<String, Variable> variables;
        
        public ModelOnlySimulationState(Map<String, Variable> variables) {
            this.variables = variables;
        }
        
        public float getTime() {
            throw new IllegalStateException("Should not be called");
        }

        public Variable getVariable(String label) {
            return variables.get(label);
        }

        public ObservationElement getComplexQuantity(Variable variable) {
            throw new IllegalStateException("Should not be called");
        }

        public Map<String, Variable> getVariables() {
            return variables;
        }

        public int getEventCount() {
            throw new IllegalStateException("Should not be called");
        }

        public ObservationElement getTransitionFiredCount(Variable variable) {
            throw new IllegalStateException("Should not be called");
        }

        public void addComplexInstances(List<Agent> agents, int amount) {
            throw new IllegalStateException("Should not be called");
        }

        public void setTransitionRate(String transitionName, VariableExpression expression) {
            throw new IllegalStateException("Should not be called");
        }

        public void stop() {
            throw new IllegalStateException("Should not be called");
        }
        
    }

	public void addAgentDeclaration(AggregateAgent agent) {
        agentDeclarationMap.put(agent.getName(), agent);
	}

	public Map<String, AggregateAgent> getAgentDeclarationMap() {
		return agentDeclarationMap;
	}
    
}
