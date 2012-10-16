package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Location.FIXED_LOCATION;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getCompartment;
import static org.demonsoft.spatialkappa.model.Utils.propogateLocation;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.demonsoft.spatialkappa.model.Complex.MappingInstance;
import org.demonsoft.spatialkappa.model.Variable.Type;
import org.demonsoft.spatialkappa.parser.SpatialKappaLexer;
import org.demonsoft.spatialkappa.parser.SpatialKappaParser;
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker;


public class KappaModel implements IKappaModel {

    private static final ComplexMatcher matcher = new ComplexMatcher();
    
    private final Map<String, AggregateAgent> agentDeclarationMap = new HashMap<String, AggregateAgent>();
    private final Map<String, AggregateAgent> aggregateAgentMap = new HashMap<String, AggregateAgent>();
    private final List<InitialValue> initialValues = new ArrayList<InitialValue>();
    private final List<Perturbation> perturbations = new ArrayList<Perturbation>();
    private final List<Compartment> compartments = new ArrayList<Compartment>();
    private final List<Channel> channels = new ArrayList<Channel>();
    private final List<Transition> transitions = new ArrayList<Transition>();
    private final Set<Complex> canonicalComplexes = new HashSet<Complex>();
    private final List<String> plottedVariables = new ArrayList<String>();
    private final Map<String, Variable> variables = new HashMap<String, Variable>();
	private final List<String> orderedVariableNames = new ArrayList<String>();

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
                if (matcher.isExactMatch(current, complexes.get(index))) {
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

    public void addVariable(List<Agent> agents, String label, Location location, boolean recordVoxels) {
        variables.put(label, new Variable(new Complex(agents), location, label, recordVoxels));
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

    public void addCompartment(String name, String type, List<Integer> dimensions) {
        addCompartment(Compartment.createCompartment(name, type, dimensions));
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
        result.append("\nTRANSITION RULES\n");
        for (Transition transition : transitions) {
            result.append(transition).append("\n");
        }
        result.append("\nAGENTS\n");
        for (AggregateAgent agent : aggregateAgentMap.values()) {
            result.append(agent).append("\n");
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

    public Map<Complex, Integer> getFixedLocatedInitialValuesMap() {
        Map<Complex, Integer> result = new HashMap<Complex, Integer>();
        
        for (InitialValue initialValue : initialValues) {
            if (initialValue.reference != null) {
                initialValue.quantity = variables.get(initialValue.reference.variableName).expression.evaluate(this);
            }

            Map<Complex, Integer> currentResult = initialValue.getFixedLocatedComplexMap(compartments, channels);
            
            for (Map.Entry<Complex, Integer> current : currentResult.entrySet()) {
                addInitialLocatedValue(result, current.getKey(), current.getValue());
            }
        }
        return result;
    }

    private void addInitialLocatedValue(Map<Complex, Integer> result, Complex complex, int quantity) {
        for (Agent agent : complex.agents) {
            addDefaultAgentSites(agent);
        }
        complex.update();
        
        for (Map.Entry<Complex, Integer> entry : result.entrySet()) {
            if (complex.isExactMatch(entry.getKey())) {
                entry.setValue(entry.getValue() + quantity);
                return;
            }
        }
        result.put(complex, quantity);
    }

    private void addDefaultAgentSites(Agent agent) {
        AggregateAgent aggregateAgent = agentDeclarationMap.get(agent.name);

        if (aggregateAgent == null) {
            throw new IllegalStateException("Agent '" + agent.name + "' not found");
        }
        
        for (AggregateSite aggregateSite : aggregateAgent.getSites()) {
            String siteName = aggregateSite.getName();
            if (agent.getSite(siteName) == null) {
                String state = null;
                if (aggregateSite.states.size() > 0) {
                    state = aggregateSite.states.get(0);
                }
                agent.addSite(new AgentSite(siteName, state, null));
            }
        }
    }

    @SuppressWarnings("hiding")
    public List<Transition> getValidLocatedTransitions(Transition templateTransition, List<Compartment> compartments, List<Channel> channels) {
        if (templateTransition == null || compartments == null || channels == null) {
            throw new NullPointerException();
        }
        List<Transition> result = new ArrayList<Transition>();
        
        List<Agent> leftAgents = new ArrayList<Agent>(templateTransition.leftAgents);
        List<Agent> rightAgents = new ArrayList<Agent>(templateTransition.rightAgents);
        Map<Agent,Agent> leftRightTemplateMap = templateTransition.getLeftRightAgentMap();
        Map<Agent,Agent> templateMergedMap = new HashMap<Agent, Agent>();
        List<Agent> mergedAgents = new ArrayList<Agent>();
        
        for (Agent leftTemplateAgent : leftAgents) {
            Agent rightTemplateAgent = leftRightTemplateMap.get(leftTemplateAgent);
            rightAgents.remove(rightTemplateAgent);
            
            Agent mergedAgent = leftTemplateAgent.clone();
            mergedAgents.add(mergedAgent);
            templateMergedMap.put(leftTemplateAgent, mergedAgent);
            
            if (rightTemplateAgent != null) {
                mergedAgent.location = getMergedLocation(mergedAgent.location, rightTemplateAgent.location);
                
                for (AgentSite agentSite : rightTemplateAgent.getSites()) {
                    String linkName = agentSite.isNamedLink() ? "rhs-" + agentSite.getLinkName() : agentSite.getLinkName();
                    
                    AgentSite mergedSite = new AgentSite("rhs-" + agentSite.name, null, linkName, agentSite.getChannel());
                    mergedAgent.addSite(mergedSite);
                }
                templateMergedMap.put(rightTemplateAgent, mergedAgent);
            }
        }
        
        for (Agent rightTemplateAgent : rightAgents) {
            Agent mergedAgent = rightTemplateAgent.clone();
            mergedAgents.add(mergedAgent);
            templateMergedMap.put(rightTemplateAgent, mergedAgent);
            
            for (AgentSite agentSite : mergedAgent.getSites()) {
                if (agentSite.isNamedLink()) {
                    agentSite.setLinkName("rhs-" + agentSite.getLinkName());
                }
            }
        }
        
        List<Complex> mergedComplexes = Utils.getComplexes(mergedAgents);
        if (mergedComplexes.size() != 1) {
            throw new IllegalArgumentException("Currently only connected transforms are supported");
        }
        
        Complex mergedComplex = mergedComplexes.get(0);
        List<MappingInstance> mergedMappings = mergedComplex.getMappingInstances(compartments, channels);
        if (mergedMappings.size() == 1) {
            MappingInstance mapping = mergedMappings.get(0);
            result.add(new Transition(templateTransition.label, 
                    getUnmergedAgents(templateTransition.leftAgents, mapping.mapping, templateMergedMap), 
                    null,
                    getUnmergedAgents(templateTransition.rightAgents, mapping.mapping, templateMergedMap), 
                    templateTransition.getRate()));

        }
        else if (mergedMappings.size() > 1) {
            int labelSuffix = 1;
            for (MappingInstance mapping : mergedMappings) {
                String label = (templateTransition.label == null) ? 
                        null : templateTransition.label + "-" + (labelSuffix++);
                result.add(new Transition(label, 
                        getUnmergedAgents(templateTransition.leftAgents, mapping.mapping, templateMergedMap), 
                        null,
                        getUnmergedAgents(templateTransition.rightAgents, mapping.mapping, templateMergedMap), 
                        templateTransition.getRate()));
            }
        }

        return result;
    }

    
    List<Agent> getUnmergedAgents(List<Agent> templateAgents, Map<Agent, Agent> mergedLocatedMap,
            Map<Agent, Agent> templateMergedMap) {
        if (templateAgents == null || mergedLocatedMap == null || templateMergedMap == null) {
            throw new NullPointerException();
        }
        List<Agent> result = new ArrayList<Agent>();
        for (Agent templateAgent : templateAgents) {
            if (!templateMergedMap.containsKey(templateAgent)) {
                throw new IllegalArgumentException("Agent not found: " + templateAgent);
            }
            Agent mergedAgent = templateMergedMap.get(templateAgent);
            
            if (!mergedLocatedMap.containsKey(mergedAgent)) {
                throw new IllegalArgumentException("Agent not found: " + mergedAgent);
            }
            Agent locatedAgent = mergedLocatedMap.get(mergedAgent);
            
            Agent resultAgent = templateAgent.clone();
            resultAgent.setLocation(locatedAgent.location);
            result.add(resultAgent);
        }
        return result;
    }

    Location getMergedLocation(Location location1, Location location2) {
        if (location1 == null || location2 == null) {
            throw new NullPointerException();
        }
        if (location1 == NOT_LOCATED) {
            return location2;
        }
        if (location2 == NOT_LOCATED) {
            return location1;
        }
        if (location1.getName().equals(location2.getName())) {
            if (location1.getDimensionCount() == 0) {
                return location2;
            }
            if (location2.getDimensionCount() == 0) {
                return location1;
            }
            if (location1.equals(location2)) {
                return location1;
            }
        }
        throw new IllegalArgumentException("Locations are incompatible: " + location1 + "; " + location2);
    }

    public Channel getChannel(String channelName) {
        return Utils.getChannel(channels, channelName);
    }


    public List<Compartment> getCompartments() {
        return compartments;
    }
    
    public List<Channel> getChannels() {
        return channels;
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
        
        Set<String> compartmentNames = new HashSet<String>();
        for (Compartment compartment : compartments) {
            if (compartmentNames.contains(compartment.getName())) {
                throw new IllegalStateException("Duplicate compartment '" + compartment.getName() + "'");
            }
            compartment.validate();
            compartmentNames.add(compartment.getName());
        }
       
        Set<String> channelNames = new HashSet<String>();
        for (Channel channel : channels) {
            if (channelNames.contains(channel.getName())) {
                throw new IllegalStateException("Duplicate channel '" + channel.getName() + "'");
            }
            channel.validate(compartments);
            channelNames.add(channel.getName());
        }
        
        for (Variable variable : variables.values()) {
            if (Variable.Type.VARIABLE_EXPRESSION == variable.type && VariableExpression.Type.VARIABLE_REFERENCE == variable.expression.type) {
                VariableReference reference = variable.expression.reference;
                Variable other = variables.get(reference.variableName);
                if (other == null) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not found");
                }
            }
            variable.validate(compartments);
        }
        // TODO check for circular dependencies
        

       
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
        
        for (Transition transition : transitions) {
            VariableReference reference = transition.rate.reference;
            if (reference != null) {
                Variable variable = variables.get(reference.variableName);
                if (variable == null) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not found");
                }
                if (!variable.expression.isFixed(variables)) {
                    throw new IllegalStateException("Reference '" + reference.variableName + "' not fixed");
                }
            }
            
            if (transition.channelName != null) {
                getChannel(transition.channelName);
            }
        }
        
        for (String reference : plottedVariables) {
            boolean found = false;
            for (Transition transition : transitions) {
                if (reference.equals(transition.label)) {
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
    	
    	for (InitialValue initialValue : initialValues) {
    	    checkLocations(initialValue.complexes);
    	}
        for (Perturbation perturbation : perturbations) {
            checkAgentLocations(perturbation.effect.agents, false);
        }
        checkLocations(canonicalComplexes);
        for (Variable variable : variables.values()) {
            checkLocations(variable.complex);
            checkLocations(variable.location, false);
        }
        for (Transition transition : transitions) {
            checkAgentLocations(transition.leftAgents, false);
            checkAgentLocations(transition.rightAgents, true);
            checkLocations(transition.leftLocation, false);
            checkLocations(transition.rightLocation, false);
        }
        
        // TODO add validation of when its possible to use variable expression agent groups
    }
    
    private void checkAgentLocations(Collection<Agent> agents, boolean allowFixed) {
        if (agents == null) {
            return;
        }
        for (Agent agent : agents) {
            checkLocations(agent, allowFixed);
        }
    }

    private void checkLocations(Collection<Complex> complexes) {
        if (complexes == null) {
            return;
        }
        for (Complex complex : complexes) {
            checkLocations(complex);
        }
    }

    private void checkLocations(Complex complex) {
        if (complex == null) {
            return;
        }
        for (Agent agent : complex.agents) {
            checkLocations(agent, false);
        }
    }

    private void checkLocations(Agent agent, boolean allowFixed) {
        if (agent == null) {
            return;
        }
        checkLocations(agent.location, allowFixed);
    }

    private void checkLocations(Location location, boolean allowFixed) {
        if (location == null || location == NOT_LOCATED || (allowFixed && location == FIXED_LOCATION)) {
            return;
        }
        Compartment compartment = getCompartment(compartments, location.getName());
        if (location.getDimensionCount() > 0 && !compartment.isValidVoxel(location)) {
            throw new IllegalStateException("Location " + location + " not valid");
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

        public void snapshot() {
            throw new IllegalStateException("Should not be called");
        }
        
    }

	public void addAgentDeclaration(AggregateAgent agent) {
        agentDeclarationMap.put(agent.getName(), agent);
	}

	public Map<String, AggregateAgent> getAgentDeclarationMap() {
		return agentDeclarationMap;
	}


    public void addTransition(String label, Location leftLocation, List<Agent> leftSideAgents, String channelName,
            Location rightLocation, List<Agent> rightSideAgents, VariableExpression rate) {
        if (leftSideAgents != null && leftLocation != null) {
            propogateLocation(leftSideAgents, leftLocation);
        }
        if (rightSideAgents != null && rightLocation != null) {
            propogateLocation(rightSideAgents, rightLocation);
        }
        if (leftSideAgents == null && rightSideAgents == null) {
            transitions.add(new Transition(label, leftLocation, channelName, rightLocation, rate));
        }
        else {
            transitions.add(new Transition(label, leftSideAgents, channelName, rightSideAgents, rate));
        }
        if (label != null) {
            variables.put(label, new Variable(label));
        }
        if (leftSideAgents != null) {
            for (Agent agent : leftSideAgents) {
                aggregateAgent(agent);
            }
        }
        if (rightSideAgents != null) {
            for (Agent agent : rightSideAgents) {
                aggregateAgent(agent);
            }
        }

    }


    public List<Transition> getTransitions() {
        return transitions;
    }
    
}
