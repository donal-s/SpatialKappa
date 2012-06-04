package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.commons.io.FileUtils;
import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentLink;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.AggregateAgent;
import org.demonsoft.spatialkappa.model.AggregateSite;
import org.demonsoft.spatialkappa.model.Channel;
import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.InitialValue;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedTransform;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.Transport;
import org.demonsoft.spatialkappa.model.Utils;
import org.demonsoft.spatialkappa.model.Variable;
import org.demonsoft.spatialkappa.model.Variable.Type;
import org.demonsoft.spatialkappa.parser.SpatialKappaLexer;
import org.demonsoft.spatialkappa.parser.SpatialKappaParser;
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker;

public class SpatialTranslator {

    private static final Map<String, Integer> NO_VARIABLES = new HashMap<String, Integer>();

    private final IKappaModel kappaModel;

    public SpatialTranslator(File inputFile) throws Exception {
        if (inputFile == null) {
            throw new NullPointerException();
        }
        kappaModel = getKappaModel(new FileInputStream(inputFile));
    }

    public SpatialTranslator(String input) throws Exception {
        if (input == null) {
            throw new NullPointerException();
        }
        kappaModel = getKappaModel(new ByteArrayInputStream(input.getBytes()));
    }

    public SpatialTranslator(IKappaModel kappaModel) {
        if (kappaModel == null) {
            throw new NullPointerException();
        }
        this.kappaModel = kappaModel;
    }

    static IKappaModel getKappaModel(InputStream inputStream) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(inputStream);
        CommonTokenStream tokens = new CommonTokenStream(new SpatialKappaLexer(input));
        SpatialKappaParser.prog_return r = new SpatialKappaParser(tokens).prog();

        CommonTree t = (CommonTree) r.getTree();

        if (t == null) {
            return new KappaModel();
        }
        CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
        nodes.setTokenStream(tokens);
        SpatialKappaWalker walker = new SpatialKappaWalker(nodes);
        return walker.prog();
    }

    public String translateToKappa() {
        StringBuilder builder = new StringBuilder();

        String aggregateLocationState = getAgentDeclLocationState(kappaModel.getCompartments());

        List<String> agentNames = new ArrayList<String>(kappaModel.getAgentDeclarationMap().keySet());
        Collections.sort(agentNames);

        for (String agentName : agentNames) {
            builder.append(getKappaString(kappaModel.getAgentDeclarationMap().get(agentName), aggregateLocationState));
        }
        builder.append("\n");

        // TODO - allow multiple diffusions same name
        // TODO - restrict diffusion agents to unlinked complexes
        for (Transport transport : kappaModel.getTransports()) {
            builder.append(getKappaString(transport));
        }
        builder.append("\n");

        for (LocatedTransform transform : kappaModel.getLocatedTransforms()) {
            builder.append(getKappaString(transform));
        }
        builder.append("\n");

        for (InitialValue initialValue : kappaModel.getInitialValues()) {
            builder.append(getKappaString(initialValue));
        }
        builder.append("\n");
        List<String> variableNames = new ArrayList<String>(kappaModel.getOrderedVariableNames());

        for (String variableName : variableNames) {
            Variable variable = kappaModel.getVariables().get(variableName);
            if (variable.type != Type.TRANSITION_LABEL) {
                builder.append(getKappaString(variable));
            }
        }

        for (String plotName : kappaModel.getPlottedVariables()) {
            builder.append("%plot: '").append(plotName).append("'\n");
        }
        builder.append("\n");
        return builder.toString();
    }

    private String getAgentDeclLocationState(List<Compartment> compartments) {
        if (compartments == null || compartments.size() == 0) {
            return "";
        }

        int maxDimensions = 0;
        List<String> compartmentNames = new ArrayList<String>();
        for (Compartment compartment : compartments) {
            compartmentNames.add(compartment.getName());
            if (compartment.getDimensions().length > maxDimensions) {
                maxDimensions = compartment.getDimensions().length;
            }
        }

        int[] maxDimensionSizes = new int[maxDimensions];
        for (Compartment compartment : compartments) {
            for (int index = 0; index < compartment.getDimensions().length; index++) {
                if (compartment.getDimensions()[index] > maxDimensionSizes[index]) {
                    maxDimensionSizes[index] = compartment.getDimensions()[index];
                }
            }
        }

        StringBuilder result = new StringBuilder();

        result.append("loc");
        Collections.sort(compartmentNames);
        for (String compartmentName : compartmentNames) {
            result.append("~").append(compartmentName);
        }

        for (int index = 0; index < maxDimensions; index++) {
            result.append(",loc_index_").append(index + 1);
            for (int stateIndex = 0; stateIndex < maxDimensionSizes[index]; stateIndex++) {
                result.append("~").append(stateIndex);
            }
        }

        return result.toString();
    }

    private Object getKappaString(AggregateAgent agent, String agentDeclLocationState) {
        StringBuilder builder = new StringBuilder();
        builder.append("%agent: ").append(agent.getName()).append("(");

        boolean firstElement = true;
        for (AggregateSite site : agent.getSites()) {
            if (!firstElement) {
                builder.append(",");
            }
            else {
                firstElement = false;
            }
            builder.append(site.getName());
            for (String state : site.getStates()) {
                builder.append("~").append(state);
            }
        }
        if (agent.getSites().size() > 0 && agentDeclLocationState.length() > 0) {
            builder.append(",");
        }
        builder.append(agentDeclLocationState).append(")\n");
        return builder.toString();
    }

    private List<Agent> getAggregateAgents(Map<String, AggregateAgent> aggregateAgentMap) {
        List<Agent> result = new ArrayList<Agent>();
        for (String agentName : aggregateAgentMap.keySet()) {
            result.add(new Agent(agentName));
        }
        Collections.sort(result, new Comparator<Agent>() {
            public int compare(Agent o1, Agent o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        return result;
    }

    private List<Channel> getChannels(List<Channel> channels, String channelName) {
        List<Channel> result = new ArrayList<Channel>();
        for (Channel link : channels) {
            if (channelName.equals(link.getName())) {
                result.add(link);
            }
        }
        return result;
    }

    String getAgentKappaString(List<Agent> agents, Location location) {
        return getAgentKappaString(agents, getKappaString(location));
    }

    String getAgentKappaString(List<Agent> agents, String stateSuffix) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Agent agent : agents) {
            if (!first) {
                builder.append(",");
            }
            if (agent.location != NOT_LOCATED) {
                builder.append(agent.toString(getKappaString(agent.location), true));
            }
            else {
                builder.append(agent.toString(stateSuffix, true));
            }
            first = false;
        }
        return builder.toString();
    }

    String getKappaString(Complex complex, String agentStateSuffix) {
        return getAgentKappaString(complex.agents, agentStateSuffix);
    }

    String getKappaString(Location location) {
        int dimensionCount = 0;
        if (location != null && location != NOT_LOCATED && location.getIndices() != null && location.getIndices().length > 0) {
            dimensionCount = location.getIndices().length;
        }
        return getKappaString(location, NO_VARIABLES, dimensionCount);
    }

    String getKappaString(Location location, int dimensionCount) {
        return getKappaString(location, NO_VARIABLES, dimensionCount);
    }

    String getKappaString(Location location, Map<String, Integer> variables, int dimensionCount) {
        if (variables == null) {
            throw new NullPointerException();
        }
        if (location == NOT_LOCATED) {
            return "";
        }
        int usedDimensions = 0;
        if (location.getIndices() != null && location.getIndices().length > 0) {
            usedDimensions = location.getIndices().length;
        }
        if (usedDimensions > dimensionCount) {
            throw new IllegalArgumentException();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("loc~").append(location.getName());
        for (int index = 0; index < usedDimensions; index++) {
            builder.append(",loc_index_").append(index + 1).append("~")
                    .append(location.getIndices()[index].evaluateIndex(variables));
        }
        for (int index = usedDimensions; index < dimensionCount; index++) {
            builder.append(",loc_index_").append(index + 1).append("~0");
        }

        return builder.toString();
    }

    String getKappaString(Variable variable) {
        StringBuilder builder = new StringBuilder();
        switch (variable.type) {
        case KAPPA_EXPRESSION:
            if (!hasLinksWithChannels(variable.complex)) {
                builder.append("%var: '").append(variable.label).append("'");
                builder.append(" ").append(getKappaString(variable.complex, getKappaString(variable.location)));
                builder.append("\n");
            }
            else {
                List<MappingInstance> mappings = initMappingStructure(variable.complex, 
                        kappaModel.getCompartments(), kappaModel.getChannels());
                if (mappings.size() == 1) {
                    builder.append("%var: '").append(variable.label).append("'");
                    builder.append(" ").append(getAgentKappaString(mappings.get(0).locatedAgents, ""));
                    builder.append("\n");
                }
                else if (mappings.size() > 1) {
                    int index = 0;
                    StringBuilder totalVariableExpression = new StringBuilder();
                    for (MappingInstance mapping : mappings) {
                        String mappingVariableName = variable.label + "-" + (++index);
                        builder.append("%var: '").append(mappingVariableName).append("'");
                        builder.append(" ").append(getAgentKappaString(mapping.locatedAgents, ""));
                        builder.append("\n");
                        
                        if (index > 1) {
                            totalVariableExpression.append(" + ");
                        }
                        totalVariableExpression.append("'").append(mappingVariableName).append("'");
                    }
                    
                    builder.append("%var: '").append(variable.label).append("'");
                    builder.append(" ").append(totalVariableExpression.toString());
                    builder.append("\n");
                }
            }
            break;
        case VARIABLE_EXPRESSION:
            builder.append("%var: '").append(variable.label).append("'");
            builder.append(" ").append(variable.expression.toString());
            builder.append("\n");
            break;
        case TRANSITION_LABEL:
            // Do nothing
            break;
        }
        return builder.toString();
    }

    boolean hasLinksWithChannels(Complex complex) {
        if (complex == null) {
            throw new NullPointerException();
        }
        return hasLinksWithChannels(complex.agents);
    }

    boolean hasLinksWithChannels(List<Agent> agents) {
        if (agents == null) {
            throw new NullPointerException();
        }
        for (Agent agent : agents) {
            for (AgentSite site : agent.getSites()) {
                if (site != null && site.getChannel() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    String getKappaString(Transport transport) {
        StringBuilder builder = new StringBuilder();
        List<Channel> channels = getChannels(kappaModel.getChannels(), transport.getCompartmentLinkName());
        int labelSuffix = 1;
        boolean forceSuffix = channels.size() > 0;
        for (Channel channel : channels) {
            String[][] stateSuffixPairs = getLinkStateSuffixPairs(channel, kappaModel.getCompartments());
            List<Agent> agents = transport.getAgents();
            if (agents != null) {
                labelSuffix = writeAgents(builder, stateSuffixPairs, transport, channel, agents, labelSuffix,
                        kappaModel.getAgentDeclarationMap(), forceSuffix);
            }
            else {
                agents = getAggregateAgents(kappaModel.getAgentDeclarationMap());
                List<Agent> currentAgents = new ArrayList<Agent>();
                for (Agent agent : agents) {
                    currentAgents.clear();
                    currentAgents.add(agent);
                    labelSuffix = writeAgents(builder, stateSuffixPairs, transport, channel, currentAgents,
                            labelSuffix, kappaModel.getAgentDeclarationMap(), forceSuffix);
                }
            }
        }
        return builder.toString();
    }

    private int writeAgents(StringBuilder builder, String[][] stateSuffixPairs, Transport transport, Channel channel,
            List<Agent> agents, int startLabelSuffix, Map<String, AggregateAgent> aggregateAgentMap, boolean forceSuffix) {

        int labelSuffix = startLabelSuffix;
        List<Agent> isolatedAgents = getIsolatedAgents(agents, aggregateAgentMap);

        for (int index = 0; index < stateSuffixPairs.length; index++) {
            if (transport.label != null) {
                builder.append("'").append(transport.label);
                if (stateSuffixPairs.length > 1 || forceSuffix) {
                    builder.append("-").append(labelSuffix++);
                }
                builder.append("' ");
            }
            String leftSuffix = stateSuffixPairs[index][0];
            String rightSuffix = stateSuffixPairs[index][1];
            builder.append(getAgentKappaString(isolatedAgents, leftSuffix));
            builder.append(" -> ");
            builder.append(getAgentKappaString(isolatedAgents, rightSuffix)).append(" @ ");
            builder.append(transport.getRate().toString());
            builder.append("\n");
        }
        return labelSuffix;
    }

    private List<Agent> getIsolatedAgents(List<Agent> agents, Map<String, AggregateAgent> aggregateAgentMap) {
        List<Agent> result = new ArrayList<Agent>();
        for (Agent agent : agents) {
            List<AgentSite> isolatedSites = new ArrayList<AgentSite>();
            isolatedSites.addAll(agent.getSites()); // Sites are cloned - no
                                                    // problem here

            AggregateAgent aggregateAgent = aggregateAgentMap.get(agent.name);
            for (AggregateSite site : aggregateAgent.getSites()) {
                if (site.getLinks().size() > 0 && agent.getSite(site.getName()) == null) {
                    isolatedSites.add(new AgentSite(site.getName(), null, null));
                }
            }
            result.add(new Agent(agent.name, isolatedSites));
        }
        return result;
    }

    // Transforms
    String getKappaString(LocatedTransform transition) {
        StringBuilder builder = new StringBuilder();
        Transform templateTransform = (Transform) transition.transition;
        
        
        List<Transform> locatedTransforms = 
                getValidLocatedTransforms(templateTransform, kappaModel.getCompartments(), kappaModel.getChannels());

        for (Transform transform : locatedTransforms) {
            if (transform.label != null) {
                builder.append("'").append(transform.label).append("' ");
            }
            if (transform.leftAgents.size() > 0) {
                builder.append(getAgentKappaString(transform.leftAgents, "")).append(" ");
            }
            builder.append("-> ");
            if (transform.rightAgents.size() > 0) {
                builder.append(getAgentKappaString(transform.rightAgents, "")).append(" ");
            }
            builder.append("@ ").append(transform.getRate().toString()).append("\n");
        }
        
        return builder.toString();
    }

    List<Transform> getValidLocatedTransforms(Transform templateTransform, List<Compartment> compartments, List<Channel> channels) {
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

    List<Map<Location, String>> createPartitionMaps(List<Location> partitionLocations) {
        List<Map<Location, String>> result = new ArrayList<Map<Location, String>>();
        if (partitionLocations.size() == 1) {
            Location location = partitionLocations.get(0);
            Compartment compartment = location.getReferencedCompartment(kappaModel.getCompartments());

            String[] suffixes = compartment.getCellStateSuffixes();
            for (String suffix : suffixes) {
                Map<Location, String> map = new HashMap<Location, String>();
                result.add(map);
                map.put(location, suffix);
            }
        }
        else if (partitionLocations.size() > 1) {
            Location location = partitionLocations.get(0);
            Compartment compartment = location.getReferencedCompartment(kappaModel.getCompartments());

            List<Location> remainingLocations = new ArrayList<Location>(partitionLocations);
            remainingLocations.remove(location);
            List<Map<Location, String>> otherLocations = createPartitionMaps(remainingLocations);

            String[] suffixes = compartment.getCellStateSuffixes();
            for (String suffix : suffixes) {
                for (Map<Location, String> otherPartition : otherLocations) {
                    Map<Location, String> map = new HashMap<Location, String>(otherPartition);
                    result.add(map);
                    map.put(location, suffix);
                }
            }
        }
        return result;
    }

    Set<Location> getPartitionedLocations(List<Agent> agents, List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        
        Set<Location> result = new HashSet<Location>();

        if (agents != null) {
            for (Agent agent : agents) {
                Location location = agent.location;
                if (location != NOT_LOCATED && isPartitionable(location, compartments)) {
                    result.add(location);
                }
            }
        }
        return result;
    }

    boolean isPartitionable(Location location, List<Compartment> compartments) {
        Compartment compartment = location.getReferencedCompartment(compartments);
        return compartment.getDimensions().length != location.getIndices().length;
    }

    String getKappaString(InitialValue initialValue) {
        StringBuilder builder = new StringBuilder();
        int quantity = initialValue.quantity;
        
        if (initialValue.reference != null) {
            Variable target = kappaModel.getVariables().get(initialValue.reference.variableName);
            quantity = target.evaluate(kappaModel);
        }
        
        for (Complex complex : initialValue.complexes) {
            List<MappingInstance> mappings = initMappingStructure(complex, kappaModel.getCompartments(), kappaModel.getChannels());
            int totalCellCount = mappings.size();
            if (totalCellCount > 0) {
                int baseValue = quantity / totalCellCount;
                int remainder = quantity % totalCellCount;
    
                int index = 0;
    
                for (MappingInstance mapping : mappings) {
                    builder.append("%init: ").append(baseValue + (index++ < remainder ? 1 : 0)).append(" ");
                    builder.append(getAgentKappaString(mapping.locatedAgents, ""));
                    builder.append("\n");
                }
            }
        }
        
        return builder.toString();
    }

    String[][] getLinkStateSuffixPairs(Channel link, List<Compartment> compartments) {
        Location[][] references = link.getCellReferencePairs(compartments);
        String[][] result = new String[references.length][2];
        int maxDimensions = 0;
        for (Compartment compartment : compartments) {
            if (compartment.getDimensions() != null && compartment.getDimensions().length > maxDimensions) {
                maxDimensions = compartment.getDimensions().length;
            }
        }
        for (int index = 0; index < references.length; index++) {
            result[index][0] = getKappaString(references[index][0], maxDimensions);
            result[index][1] = getKappaString(references[index][1], maxDimensions);
        }
        return result;
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
                            List<Channel> linkChannels = getChannels(channels, channelName);
                            if (linkChannels.size() > 0) {
                                channel = linkChannels.get(0); // TODO just the first for now
                            }
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

    List<Location> getPossibleLocations(Location sourceLocation, Location locationConstraint, Channel channel, List<Compartment> compartments) {
        if (sourceLocation == null || compartments == null) {
            throw new NullPointerException();
        }
        List<Location> result = new ArrayList<Location>();
        
        if (channel == null) {
            if (locationConstraint == null) {
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
            if (locationConstraint == null) {
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

    static class MappingInstance {
        public final Map<Agent, Agent> mapping = new HashMap<Agent, Agent>();
        public final List<Agent> locatedAgents = new ArrayList<Agent>();
    }
    
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("SpatialTranslator version " + Version.VERSION);
            System.err.println("Usage: SpatialTranslator <input file path> [<output file path>]");
            return;
        }

        String result = new SpatialTranslator(new File(args[0])).translateToKappa();
        if (args.length == 1) {
            System.out.println(result);
        }
        else {
            FileUtils.writeStringToFile(new File(args[1]), result);
            System.out.println("Result written to " + args[1]);
        }
    }

}
