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

    public List<Transform> getValidLocatedTransforms(Transform templateTransform, List<Compartment> compartments, List<Channel> channels) {
        if (templateTransform == null || compartments == null || channels == null) {
            throw new NullPointerException();
        }
        List<Transform> result = new ArrayList<Transform>();
        
        List<Agent> leftAgents = new ArrayList<Agent>(templateTransform.leftAgents);
        List<Agent> rightAgents = new ArrayList<Agent>(templateTransform.rightAgents);
        Map<Agent,Agent> leftRightTemplateMap = templateTransform.getLeftRightAgentMap();
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
        List<MappingInstance> mergedMappings = initMappingStructure(mergedComplex, compartments, channels);
        if (mergedMappings.size() == 1) {
            MappingInstance mapping = mergedMappings.get(0);
            result.add(new Transform(templateTransform.label, 
                    getUnmergedAgents(templateTransform.leftAgents, mapping.mapping, templateMergedMap), 
                    getUnmergedAgents(templateTransform.rightAgents, mapping.mapping, templateMergedMap), 
                    templateTransform.getRate(), false));

        }
        else if (mergedMappings.size() > 1) {
            int labelSuffix = 1;
            for (MappingInstance mapping : mergedMappings) {
                String label = (templateTransform.label == null) ? 
                        null : templateTransform.label + "-" + (labelSuffix++);
                result.add(new Transform(label, 
                        getUnmergedAgents(templateTransform.leftAgents, mapping.mapping, templateMergedMap), 
                        getUnmergedAgents(templateTransform.rightAgents, mapping.mapping, templateMergedMap), 
                        templateTransform.getRate(), false));
            }
        }

        return result;
    }

    public List<MappingInstance> initMappingStructure(Complex complex, List<Compartment> compartments, List<Channel> channels) {
        if (complex == null || compartments == null || channels == null) {
            throw new NullPointerException();
        }
        
        List<Agent> remainingTemplateAgents = new ArrayList<Agent>(complex.agents);
        List<Agent> fixedTemplateAgents = getFixedAgents(complex, compartments);
        remainingTemplateAgents.removeAll(fixedTemplateAgents);
        List<AgentLink> remainingTemplateLinks = complex.agentLinks;
        List<MappingInstance> mappings = new ArrayList<MappingInstance>();
        
        if (fixedTemplateAgents.size() > 0) {
            List<AgentLink> processedTemplateLinks = getInternalLinks(remainingTemplateLinks, fixedTemplateAgents);
            remainingTemplateLinks.removeAll(processedTemplateLinks);
            
            MappingInstance mappingInstance = new MappingInstance();
            for (Agent agent : fixedTemplateAgents) {
                mappingInstance.mapping.put(agent, agent);
            }
            mappings.add(mappingInstance);
        }
        
        if (fixedTemplateAgents.size() == 0) {
            Agent templateTargetAgent = remainingTemplateAgents.get(0);
            fixedTemplateAgents.add(templateTargetAgent);
            remainingTemplateAgents.remove(templateTargetAgent);
            List<AgentLink> processedTemplateLinks = getInternalLinks(remainingTemplateLinks, fixedTemplateAgents);
            remainingTemplateLinks.removeAll(processedTemplateLinks);
            
            List<Agent> locatedTargetAgents = templateTargetAgent.getLocatedAgents(compartments);
            for (Agent locatedTargetAgent : locatedTargetAgents) {
                MappingInstance mapping = new MappingInstance();
                mapping.mapping.put(templateTargetAgent, locatedTargetAgent);
                mappings.add(mapping);
            }
        }
        
        while (remainingTemplateAgents.size() > 0) {
            Agent templateTargetAgent = chooseNextAgent(fixedTemplateAgents, remainingTemplateAgents, remainingTemplateLinks);
            fixedTemplateAgents.add(templateTargetAgent);
            remainingTemplateAgents.remove(templateTargetAgent);
            List<AgentLink> addedTemplateLinks = getInternalLinks(remainingTemplateLinks, fixedTemplateAgents);
            remainingTemplateLinks.removeAll(addedTemplateLinks);
            
            List<MappingInstance> newMappings = new ArrayList<MappingInstance>();
            
            for (MappingInstance oldMapping : mappings) {
            
                List<Location> targetLocations = null;
                
                for (AgentLink link : addedTemplateLinks) {
                    Agent templateSourceAgent = link.getLinkedAgent(templateTargetAgent);
                    
                    if (templateSourceAgent != null) {
                        Agent locatedSourceAgent = oldMapping.mapping.get(templateSourceAgent);
                        
                        String channelName = (link.sourceSite.getChannel() != null) ? link.sourceSite.getChannel() : link.targetSite.getChannel();
                        Channel channel = null;
                        if (channelName != null) {
                            channel = getChannel(channels, channelName);
                        }
                        List<Location> currentTargetLocations = getPossibleLocations(locatedSourceAgent.location, templateTargetAgent.location, channel, compartments);
                        
                        if (targetLocations == null) {
                            targetLocations = currentTargetLocations;
                        }
                        else {
                            targetLocations.retainAll(currentTargetLocations);
                        }
                        // TODO handling !_ links
                    }
                }
                
                if (targetLocations == null || targetLocations.size() == 0) {
                    continue;
                }
                
                for (Location targetLocation : targetLocations) {
                    MappingInstance newMapping = new MappingInstance();
                    newMapping.mapping.putAll(oldMapping.mapping);
                    Agent locatedTargetAgent = new Agent(templateTargetAgent.name, targetLocation, templateTargetAgent.getSites());
                    newMapping.mapping.put(templateTargetAgent, locatedTargetAgent);
                    
                    newMappings.add(newMapping);
                }
            }
            mappings = newMappings;
        }
        
        reorderLocatedMappings(mappings, complex.agents);
        
        return mappings;
    }
    
    private void reorderLocatedMappings(List<MappingInstance> mappings, List<Agent> templateAgents) {
        for (MappingInstance mapping : mappings) {
            mapping.locatedAgents.clear();
            for (Agent templateAgent : templateAgents) {
                mapping.locatedAgents.add(mapping.mapping.get(templateAgent));
            }
        }
    }

    public static class MappingInstance {
        public final Map<Agent, Agent> mapping = new HashMap<Agent, Agent>();
        public final List<Agent> locatedAgents = new ArrayList<Agent>();
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
            if (location1.getIndices().length == 0) {
                return location2;
            }
            if (location2.getIndices().length == 0) {
                return location1;
            }
            if (location1.equals(location2)) {
                return location1;
            }
        }
        throw new IllegalArgumentException("Locations are incompatible: " + location1 + "; " + location2);
    }

    List<AgentLink> getInternalLinks(List<AgentLink> links, List<Agent> agents) {
        if (links == null || agents == null) {
            throw new NullPointerException();
        }
        List<AgentLink> result = new ArrayList<AgentLink>();
        for (AgentLink link : links) {
            if (link.sourceSite.agent != null && !agents.contains(link.sourceSite.agent)) {
                continue;
            }
            if (link.targetSite.agent != null && !agents.contains(link.targetSite.agent)) {
                continue;
            }
            result.add(link);
        }
        return result;
    }
    
    List<Location> getPossibleLocations(Location sourceLocation, Location locationConstraint, Channel channel, List<Compartment> compartments) {
        if (sourceLocation == null || compartments == null) {
            throw new NullPointerException();
        }
        List<Location> result = new ArrayList<Location>();
        
        if (channel == null) {
            if (locationConstraint == NOT_LOCATED) {
                result.add(sourceLocation);
            }
            else {
                boolean matchNameOnly = locationConstraint.getIndices().length == 0;
                if (sourceLocation.matches(locationConstraint, matchNameOnly)) {
                    result.add(sourceLocation);
                }
            }
        }
        else { // channel != null
            List<Location> targetLocations = sourceLocation.getLinkedLocations(compartments, channel);
            if (locationConstraint == NOT_LOCATED) {
                result.addAll(targetLocations);
            }
            else {
                boolean matchNameOnly = locationConstraint.getIndices().length == 0;
                for (Location targetLocation : targetLocations) {
                    if (targetLocation.matches(locationConstraint, matchNameOnly)) {
                        result.add(targetLocation);
                    }
                }
            }
        }
        return result;
    }

    Agent chooseNextAgent(List<Agent> fixedAgents, List<Agent> remainingAgents, List<AgentLink> remainingLinks) {
        if (fixedAgents == null || remainingAgents == null || remainingLinks == null) {
            throw new NullPointerException();
        }
        if (fixedAgents.size() == 0 || remainingAgents.size() == 0 || remainingLinks.size() == 0) {
            throw new IllegalArgumentException("No next agent available");
        }
        for (AgentLink link : remainingLinks) {
            if (fixedAgents.contains(link.sourceSite.agent)) {
                return link.targetSite.agent;
            }
            if (fixedAgents.contains(link.targetSite.agent)) {
                return link.sourceSite.agent;
            }
        }
        throw new IllegalArgumentException("No next agent available");
    }

    List<Agent> getFixedAgents(Complex complex, List<Compartment> compartments) {
        if (complex == null || compartments == null) {
            throw new NullPointerException();
        }
        List<Agent> result = new ArrayList<Agent>();
        for (Agent agent : complex.agents) {
            Location location = agent.location;
            if (location != null) {
                Compartment compartment = location.getReferencedCompartment(compartments);
                if (compartment != null) {
                    if (location.getIndices().length == compartment.getDimensions().length 
                            && location.isConcreteLocation()) {
                        result.add(agent);
                    }
                }
            }
        }
        return result;
    }
    
    public Channel getChannel(List<Channel> channels, String channelName) {
        for (Channel channel : channels) {
            if (channelName.equals(channel.getName())) {
                return channel;
            }
        }
        return null;
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
        
        Set<String> channelNames = new HashSet<String>();
        for (Channel channel : channels) {
            if (channelNames.contains(channel.getName())) {
                throw new IllegalStateException("Duplicate channel '" + channel.getName() + "'");
            }
            channelNames.add(channel.getName());
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
