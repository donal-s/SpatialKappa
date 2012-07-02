package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.TransformPrimitive.Type;


public class Transition {

    private static final int AGENT_PENALTY = 100;
    private static final int STATE_CHANGE_PENALTY = 10;
    private static final int LINK_PENALTY = 1;
    
    static final int DELETED = -1;
    static final int UNMAPPED = -2;
    
    protected VariableExpression rate;
    public final String label;
    public final List<Complex> sourceComplexes = new ArrayList<Complex>();
    public final List<Complex> targetComplexes = new ArrayList<Complex>();
    public final String channelName;
    
    List<TransformPrimitive> bestPrimitives;
    int bestPrimitivesCost = Integer.MAX_VALUE;
    int[] bestIndexMapLeftRight;
    public final List<Agent> leftAgents = new ArrayList<Agent>();
    public final List<Agent> rightAgents = new ArrayList<Agent>();
    public final Location leftLocation;
    public final Location rightLocation;


    public Transition(String label, List<Agent> leftAgents, String channelName, List<Agent> rightAgents, float rate) {
        this(label, leftAgents, channelName, rightAgents, new VariableExpression(rate));
    }

    public Transition(String label, List<Agent> leftAgents, String channelName, List<Agent> rightAgents, VariableExpression rate) {
        if (rate == null) {
            throw new NullPointerException();
        }
        this.rate = rate;
        this.label = label;
        this.channelName = channelName;
        this.leftLocation = null;
        this.rightLocation = null;
        
        if ((leftAgents == null || leftAgents.size() == 0) && (rightAgents == null || rightAgents.size() == 0) && channelName == null) {
            throw new IllegalArgumentException("Both left and right agent lists may not be empty with empty channel");
        }

        if (leftAgents != null) {
            sourceComplexes.addAll(Utils.getComplexes(leftAgents));
        }
        if (rightAgents != null) {
            targetComplexes.addAll(Utils.getComplexes(rightAgents));
        }

        if (leftAgents != null) {
            this.leftAgents.addAll(leftAgents);
        }
        if (rightAgents != null) {
            this.rightAgents.addAll(rightAgents);
        }

        createTransitionMap(this.leftAgents, this.rightAgents);
    }

    // For unit tests
    public Transition(String label, Location leftLocation, String channelName, Location rightLocation, float rate) {
        this(label, leftLocation, channelName, rightLocation, new VariableExpression(rate));
    }

    public Transition(String label, Location leftLocation, String channelName, Location rightLocation, VariableExpression rate) {
        if (rate == null || channelName == null) {
            throw new NullPointerException();
        }
        this.rate = rate;
        this.label = label;
        this.channelName = channelName;
        this.leftLocation = leftLocation;
        this.rightLocation = rightLocation;

        createTransitionMap(this.leftLocation, this.rightLocation, this.channelName);
    }

    public final VariableExpression getRate() {
        return rate;
    }

    public final void setRate(VariableExpression rate) {
        this.rate = rate;
    }

    public final boolean isInfiniteRate(Map<String, Variable> variables) {
        return rate.isInfinite(variables);
    }


    void createTransitionMap(List<Agent> leftSideAgents, List<Agent> rightSideAgents) {
        if (leftSideAgents == null || rightSideAgents == null) {
            throw new NullPointerException();
        }
        Map<String, Integer> deletes = new HashMap<String, Integer>();
        for (Agent agent : leftSideAgents) {
            if (deletes.containsKey(agent.name)) {
                deletes.put(agent.name, deletes.get(agent.name) + 1);
            }
            else {
                deletes.put(agent.name, 1);
            }
        }
        for (Agent agent : rightSideAgents) {
            if (deletes.containsKey(agent.name) && deletes.get(agent.name) > 0) {
                deletes.put(agent.name, deletes.get(agent.name) - 1);
            }
            else {
                deletes.put(agent.name, 0);
            }
        }

        int[] indexMapLeftRight = new int[leftSideAgents.size()];
        int[] indexMapRightLeft = new int[rightSideAgents.size()];
        Arrays.fill(indexMapLeftRight, UNMAPPED);
        Arrays.fill(indexMapRightLeft, UNMAPPED);
        createTransitionMap(leftSideAgents, rightSideAgents, indexMapLeftRight, indexMapRightLeft, deletes);
    }

    @SuppressWarnings("hiding")
    private void createTransitionMap(Location leftLocation, Location rightLocation, String channelName) {
        List<TransformPrimitive> primitives = getList(TransformPrimitive.getMoveComplex(leftLocation, rightLocation, channelName));
        chooseLowerCostPrimitives(primitives, new int[0]);
    }


    private void chooseLowerCostPrimitives(List<TransformPrimitive> currentPrimitives, int[] indexMapLeftRight) {
        int currentScore = getPrimitivesCost(currentPrimitives);
        if (currentScore < bestPrimitivesCost) {
            bestPrimitives = currentPrimitives;
            bestPrimitivesCost = currentScore;
            bestIndexMapLeftRight = Arrays.copyOf(indexMapLeftRight, indexMapLeftRight.length);
        }
    }

    private int getPrimitivesCost(List<TransformPrimitive> primitives) {
        Map<String, Integer> deleteAgents = new HashMap<String, Integer>();
        Map<String, Integer> createAgents = new HashMap<String, Integer>();
        Map<String, Integer> deleteLinks = new HashMap<String, Integer>();
        Map<String, Integer> createLinks = new HashMap<String, Integer>();
        
        Map<String, Object[]> stateChange = new HashMap<String, Object[]>();
        
        int cost = 0;
        
        
        for (TransformPrimitive primitive : primitives) {
            if (primitive.type == Type.DELETE_AGENT) {
                if (deleteAgents.containsKey(primitive.sourceAgent.name)) {
                    deleteAgents.put(primitive.sourceAgent.name, deleteAgents.get(primitive.sourceAgent.name) + 1);
                }
                else {
                    deleteAgents.put(primitive.sourceAgent.name, 1);
                }
            }
            if (primitive.type == Type.CREATE_AGENT) {
                if (createAgents.containsKey(primitive.sourceAgent.name)) {
                    createAgents.put(primitive.sourceAgent.name, createAgents.get(primitive.sourceAgent.name) + 1);
                }
                else {
                    createAgents.put(primitive.sourceAgent.name, 1);
                }
            }
            if (primitive.type == Type.CHANGE_STATE) {
                String key = primitive.sourceAgent.name + "@" + primitive.sourceSite.name + "@" + primitive.sourceSite.getState() + "@" + primitive.state;
                String invertedKey = primitive.sourceAgent.name + "@" + primitive.sourceSite.name + "@" + primitive.state + "@" + primitive.sourceSite.getState();
                if (stateChange.containsKey(key)) {
                    Object[] previousValue = stateChange.get(key);
                    stateChange.put(key, new Object[] {invertedKey, (Integer) previousValue[1] + 1});
                }
                else {
                    stateChange.put(key, new Object[] {invertedKey, 1});
                }
            }
            if (primitive.type == Type.DELETE_LINK) {
                String key = primitive.agentLink.sourceSite.agent.name + "@" + primitive.agentLink.sourceSite.name;
                if (deleteLinks.containsKey(key)) {
                    deleteLinks.put(key, deleteLinks.get(key) + 1);
                }
                else {
                    deleteLinks.put(key, 1);
                }
                if (primitive.agentLink.targetSite.agent != null) {
                    key = primitive.agentLink.targetSite.agent.name + "@" + primitive.agentLink.targetSite.name;
                }
                else {
                    key = "[none]@" + primitive.agentLink.targetSite.name;
                }
                if (deleteLinks.containsKey(key)) {
                    deleteLinks.put(key, deleteLinks.get(key) + 1);
                }
                else {
                    deleteLinks.put(key, 1);
                }
            }
            if (primitive.type == Type.CREATE_LINK) {
                String key = primitive.sourceSite.agent.name + "@" + primitive.sourceSite.name;
                if (createLinks.containsKey(key)) {
                    createLinks.put(key, createLinks.get(key) + 1);
                }
                else {
                    deleteLinks.put(key, 1);
                }
                key = primitive.targetSite.agent.name + "@" + primitive.targetSite.name;
                if (createLinks.containsKey(key)) {
                    createLinks.put(key, createLinks.get(key) + 1);
                }
                else {
                    createLinks.put(key, 1);
                }
            }
        }
        
        for (Map.Entry<String, Integer> deleteEntry : deleteAgents.entrySet()) {
            String agentName = deleteEntry.getKey();
            if (createAgents.containsKey(agentName)) {
                cost += AGENT_PENALTY * (Math.min(deleteEntry.getValue(), createAgents.get(agentName)));
            }
        }
        
        // doubled
        for (Map.Entry<String, Object[]> stateChangeEntry : stateChange.entrySet()) {
            String invertedKey = (String) stateChangeEntry.getValue()[0];
            int count = (Integer) stateChangeEntry.getValue()[1];
            if (stateChange.containsKey(invertedKey)) {
                cost += STATE_CHANGE_PENALTY * (Math.min(count, (Integer) stateChange.get(invertedKey)[1]));
            }
        }
        
        // doubled
        for (Map.Entry<String, Integer> deleteLinksEntry : deleteLinks.entrySet()) {
            String key = deleteLinksEntry.getKey();
            if (createLinks.containsKey(key)) {
                int deleteCount = deleteLinksEntry.getValue();
                int createCount = createLinks.get(key);
                cost += LINK_PENALTY * (Math.min(deleteCount, createCount));
            }
        }
        
        

        return cost;
    }

    private void createTransitionMap(List<Agent> leftSideAgents, List<Agent> rightSideAgents,
            int[] indexMapLeftRight, int[] indexMapRightLeft, Map<String, Integer> deletes) {

        if (countUnmappedNodes(indexMapLeftRight) == 0) {
            chooseLowerCostPrimitives(
                    createPrimitives(leftSideAgents, rightSideAgents, indexMapLeftRight, indexMapRightLeft), 
                    indexMapLeftRight);
            return;
        }

        List<int[]> candidatePairs = getCandidatePairs(leftSideAgents, rightSideAgents, indexMapLeftRight, indexMapRightLeft, deletes);
        for (int[] candidatePair : candidatePairs) {
            indexMapLeftRight[candidatePair[0]] = candidatePair[1];
            String deletedAgentName = null;
            if (candidatePair[1] != DELETED) {
                indexMapRightLeft[candidatePair[1]] = candidatePair[0];
            }
            else { // DELETED
                deletedAgentName = leftSideAgents.get(candidatePair[0]).name;
                deletes.put(deletedAgentName, deletes.get(deletedAgentName) - 1);
            }
            createTransitionMap(leftSideAgents, rightSideAgents, indexMapLeftRight, indexMapRightLeft, deletes);
            indexMapLeftRight[candidatePair[0]] = UNMAPPED;
            if (candidatePair[1] != DELETED) {
                indexMapRightLeft[candidatePair[1]] = UNMAPPED;
            }
            else { // DELETED
                deletes.put(deletedAgentName, deletes.get(deletedAgentName) + 1);
            }
        }
    }

    List<TransformPrimitive> createPrimitives(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] indexMapLeftRight, int[] indexMapRightLeft) {
        List<TransformPrimitive> primitives = new ArrayList<TransformPrimitive>();
        Set<AgentLink> deletedLinks = new HashSet<AgentLink>();
        Set<Agent> deletedAgents = new HashSet<Agent>();
        Set<Complex> createdComplexes = new HashSet<Complex>();

        createPrimitivesDeleteAgents(leftSideAgents, indexMapLeftRight, primitives, deletedLinks, deletedAgents);

        createPrimitivesDeleteLinks(leftSideAgents, rightSideAgents, indexMapLeftRight, primitives, deletedLinks);

        createPrimitivesCreateComplexes(rightSideAgents, indexMapRightLeft, primitives, createdComplexes);

        createPrimitivesCreateAgentsAndMergeComplexes(leftSideAgents, rightSideAgents, indexMapRightLeft, primitives, createdComplexes);

        createPrimitivesChangeSiteStates(leftSideAgents, rightSideAgents, indexMapLeftRight, primitives);

        createPrimitivesMoveComplexes(primitives);
        
        createPrimitivesMoveAgents(leftSideAgents, rightSideAgents, indexMapLeftRight, primitives);
        
        createPrimitivesAddLinks(leftSideAgents, rightSideAgents, indexMapRightLeft, primitives);

        return primitives;
    }

    void createPrimitivesMoveAgents(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] indexMapLeftRight,
            List<TransformPrimitive> primitives) {
        if (channelName == null) {
            return;
        }
        List<Agent> movedAgents = new ArrayList<Agent>();
        List<Location> targetLocations = new ArrayList<Location>();
        
        for (int index = 0; index < indexMapLeftRight.length; index++) {
            if (indexMapLeftRight[index] != DELETED) {
                Agent leftAgent = leftSideAgents.get(index);
                
                Agent rightAgent = rightSideAgents.get(indexMapLeftRight[index]);
                Location sourceLocation = leftAgent.location;
                Location targetLocation = rightAgent.location;
                
                if (NOT_LOCATED == sourceLocation || NOT_LOCATED == targetLocation || !targetLocation.equals(sourceLocation)) {
                    movedAgents.add(leftAgent);
                    targetLocations.add(targetLocation);
                }
            }
        }
        primitives.add(TransformPrimitive.getMoveAgents(movedAgents, targetLocations, channelName));
    }

    void createPrimitivesMoveComplexes(List<TransformPrimitive> primitives) {
        if (leftAgents.size() == 0 && rightAgents.size() == 0) {
            primitives.add(TransformPrimitive.getMoveComplex(leftLocation, rightLocation, channelName));
        }
    }

    void createPrimitivesDeleteAgents(List<Agent> leftSideAgents, int[] indexMapLeftRight,
            List<TransformPrimitive> primitives, Set<AgentLink> deletedLinks, Set<Agent> deletedAgents) {
        for (int index = 0; index < indexMapLeftRight.length; index++) {
            if (indexMapLeftRight[index] == DELETED) {
                Agent leftAgent = leftSideAgents.get(index);
                deletedLinks.addAll(leftAgent.getLinks());
                if (!deletedAgents.contains(leftAgent)) {
                    primitives.add(TransformPrimitive.getDeleteAgent(leftAgent));
                    deletedAgents.add(leftAgent);
                }
            }
        }
    }

    void createPrimitivesDeleteLinks(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] indexMapLeftRight,
            List<TransformPrimitive> primitives, Set<AgentLink> deletedLinks) {
        for (int index = 0; index < indexMapLeftRight.length; index++) {
            if (indexMapLeftRight[index] == DELETED) {
                continue;
            }
            Agent leftAgent = leftSideAgents.get(index);
            Agent rightAgent = rightSideAgents.get(indexMapLeftRight[index]);
            for (AgentLink link : leftAgent.getLinks()) {
                if (deletedLinks.contains(link)) {
                    continue;
                }
                
                AgentSite leftSite = link.getSite(leftAgent);
                boolean delete = false;
                
                AgentLink rightLink = rightAgent.getLink(leftSite.name);
                if (rightLink == null) {
                    delete = true;
                }
                else {
                    AgentSite rightSite = rightLink.getSite(rightAgent);
                    if (!equal(leftSite.getChannel(), rightSite.getChannel())) {
                        delete = true;
                    }
                    else if (link.isAnyLink() && !rightLink.isAnyLink() || 
                            link.isNoneLink() && !rightLink.isNoneLink() || 
                            link.isOccupiedLink() && !rightLink.isOccupiedLink()) {
                        delete = true;
                    }
                    else {
                        Agent leftTargetAgent = link.getLinkedAgent(leftAgent);
                        Agent rightTargetAgent = rightLink.getLinkedAgent(rightAgent);
                        if (leftTargetAgent == null && rightTargetAgent != null ||
                                leftTargetAgent != null && (rightTargetAgent == null || 
                                indexMapLeftRight[leftSideAgents.indexOf(leftTargetAgent)] != 
                                rightSideAgents.indexOf(rightTargetAgent))) {
                            delete = true;
                        }
                        
                    }
                }
                if (delete) {
                    if (!deletedLinks.contains(link)) {
                        primitives.add(TransformPrimitive.getDeleteLink(link));
                        deletedLinks.add(link);
                    }
                }
            }
        }
    }

    private void createPrimitivesCreateComplexes(List<Agent> rightSideAgents, int[] indexMapRightLeft, List<TransformPrimitive> primitives,
            Set<Complex> createdComplexes) {
        Set<Agent> copyRightAgents = new HashSet<Agent>(rightSideAgents);
        while (!copyRightAgents.isEmpty()) {
            Agent rightAgent = copyRightAgents.iterator().next();
            Complex rightComplex = rightAgent.getComplex();
            copyRightAgents.removeAll(rightComplex.agents);
            boolean existingComplex = false;
            for (Agent agent : rightComplex.agents) {
                int leftIndex = indexMapRightLeft[rightSideAgents.indexOf(agent)];
                if (leftIndex != UNMAPPED) {
                    existingComplex = true;
                    break;
                }
            }
            if (!existingComplex) {
                primitives.add(TransformPrimitive.getCreateComplex(rightComplex));
                createdComplexes.add(rightComplex);
            }
        }
    }

    private void createPrimitivesCreateAgentsAndMergeComplexes(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] indexMapRightLeft,
            List<TransformPrimitive> primitives, Set<Complex> createdComplexes) {
        Set<Agent> copyRightAgents;
        copyRightAgents = new HashSet<Agent>(rightSideAgents);
        while (!copyRightAgents.isEmpty()) {
            Agent rightAgent = copyRightAgents.iterator().next();
            Agent leftTargetAgent = null;
            Complex rightComplex = rightAgent.getComplex();
            copyRightAgents.removeAll(rightComplex.agents);
            Map<Complex, Agent> mergableComplexAgents = new HashMap<Complex, Agent>();
            for (Agent agent : rightComplex.agents) {
                int leftIndex = indexMapRightLeft[rightSideAgents.indexOf(agent)];
                if (leftIndex != UNMAPPED) {
                    Agent leftAgent = leftSideAgents.get(leftIndex);
                    if (leftTargetAgent == null) {
                        leftTargetAgent = leftAgent;
                    }
                    else if (leftAgent.getComplex() != leftTargetAgent.getComplex()) {
                        mergableComplexAgents.put(leftAgent.getComplex(), leftAgent);
                    }
                }
                else if (!createdComplexes.contains(agent.getComplex())) {
                    primitives.add(TransformPrimitive.getCreateAgent(agent, leftTargetAgent));
                }
            }
            for (Agent agent : mergableComplexAgents.values()) {
                primitives.add(TransformPrimitive.getMergeComplexes(agent, leftTargetAgent));
            }
        }
    }

    private void createPrimitivesChangeSiteStates(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] indexMapLeftRight,
            List<TransformPrimitive> primitives) {
        for (int index = 0; index < indexMapLeftRight.length; index++) {
            if (indexMapLeftRight[index] != DELETED) {
                Agent leftAgent = leftSideAgents.get(index);
                Agent rightAgent = rightSideAgents.get(indexMapLeftRight[index]);
                for (AgentSite leftSite : leftAgent.getSites()) {
                    AgentSite rightSite = rightAgent.getSite(leftSite.name);
                    if (leftSite.getState() == null) {
                        if (rightSite != null && rightSite.getState() != null) {
                            primitives.add(TransformPrimitive.getChangeState(leftAgent, leftSite, rightSite.getState()));
                        }
                    }
                    else {
                        if (rightSite != null && !leftSite.getState().equals(rightSite.getState())) {
                            primitives.add(TransformPrimitive.getChangeState(leftAgent, leftSite, rightSite.getState()));
                        }
                        else if (rightSite == null) {
                            primitives.add(TransformPrimitive.getChangeState(leftAgent, leftSite, null));
                        }
                    }
                }
            }
        }
    }

    void createPrimitivesAddLinks(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] indexMapRightLeft, List<TransformPrimitive> primitives) {
        Set<AgentLink> addedLinks = new HashSet<AgentLink>();
        for (int index = 0; index < indexMapRightLeft.length; index++) {
            Agent rightAgent = rightSideAgents.get(index);
            if (indexMapRightLeft[index] == UNMAPPED) {
                for (AgentLink link : rightAgent.getLinks()) {
                    Agent rightTargetAgent = link.getLinkedAgent(rightAgent);
                    int rightTargetIndex = rightSideAgents.indexOf(rightTargetAgent);
                    if (rightTargetAgent != null && indexMapRightLeft[rightTargetIndex] == UNMAPPED) {
                        System.out.println("should add link for new agent pair");
                    }
                }
            }
            else {
                Agent leftAgent = leftSideAgents.get(indexMapRightLeft[index]);
                for (AgentLink link : rightAgent.getLinks()) {
                    AgentSite rightSite = link.getSite(rightAgent);
                    Agent rightTargetAgent = link.getLinkedAgent(rightAgent);
                    if (rightTargetAgent != null) {
                        AgentSite rightTargetSite = link.getSite(rightTargetAgent);
                        int rightTargetIndex = rightSideAgents.indexOf(rightTargetAgent);

                        AgentLink leftLink = leftAgent.getLink(rightSite.name);
                        AgentSite leftSite = leftAgent.getSite(rightSite.name);

                        Agent leftTargetAgent;
                        if (indexMapRightLeft[rightTargetIndex] == UNMAPPED) {
                            // Target agent doesn't exist yet - use right target
                            // agent as placeholder
                            leftTargetAgent = rightTargetAgent;
                        }
                        else {
                            leftTargetAgent = leftSideAgents.get(indexMapRightLeft[rightTargetIndex]);
                        }

                        AgentSite leftTargetSite = leftTargetAgent.getSite(rightTargetSite.name);
                        if (leftLink == null) {
                            if (!addedLinks.contains(link)) {
                                primitives.add(TransformPrimitive.getCreateLink(leftSite, leftTargetSite, channelName));
                                addedLinks.add(link);
                            }
                            continue;
                        }

                        Agent leftCurrentTargetAgent = leftLink.getLinkedAgent(leftAgent);
                        if ((leftCurrentTargetAgent == null || 
                                indexMapRightLeft[rightTargetIndex] != leftSideAgents.indexOf(leftCurrentTargetAgent) ||
                                !equal(link.getChannel(), leftLink.getChannel()))
                                && !addedLinks.contains(link)) {
                            primitives.add(TransformPrimitive.getCreateLink(leftSite, leftTargetSite, link.getChannel()));
                            addedLinks.add(link);
                        }
                    }
                }
            }
        }
    }

    private List<int[]> getCandidatePairs(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] core1, int[] core2, Map<String, Integer> deletes) {
        List<int[]> result = new ArrayList<int[]>();
        for (int currentTemplate = 0; currentTemplate < core1.length; currentTemplate++) {
            if (core1[currentTemplate] == UNMAPPED) {
                Agent templateAgent = leftSideAgents.get(currentTemplate);
                for (int currentTarget = 0; currentTarget < core2.length; currentTarget++) {
                    if (core2[currentTarget] == UNMAPPED) {
                        Agent targetAgent = rightSideAgents.get(currentTarget);
                        if (isCandidatePair(templateAgent, targetAgent)) {
                            result.add(new int[] { currentTemplate, currentTarget });
                        }
                    }
                }
                if (deletes.get(templateAgent.name) > 0) {
                    result.add(new int[] { currentTemplate, DELETED });
                }
                break;
            }
        }
        return result;
    }

    private boolean isCandidatePair(Agent templateAgent, Agent targetAgent) {
        if (!templateAgent.name.equals(targetAgent.name)) {
            return false;
        }
        Collection<AgentSite> templateSites = templateAgent.getSites();
        Collection<AgentSite> targetSites = targetAgent.getSites();
        if (templateSites.size() != targetSites.size()) {
            return false;
        }
        for (AgentSite site : templateSites) {
            if (targetAgent.getSite(site.name) == null) {
                return false;
            }
        }
        
        return true;
    }

    private int countUnmappedNodes(int[] nodeMappings) {
        int result = 0;
        for (int current : nodeMappings) {
            if (current == UNMAPPED) {
                result++;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean showPrimitives) {
        String leftText;
        String rightText;
        if (leftLocation != null || rightLocation != null) {
            leftText = (leftLocation != null) ? leftLocation.toString() : "";
            rightText = (rightLocation != null) ? rightLocation.toString() : "";
        }
        else {
            leftText = sourceComplexes.toString();
            rightText = targetComplexes.toString();
        }

        StringBuilder builder = new StringBuilder();
        if (label != null) {
            builder.append("'").append(label).append("' : ");
        }
        builder.append(leftText).append(" ->");
        if (channelName != null) {
            builder.append(":").append(channelName);
        }
        builder.append(" ");
        builder.append(rightText).append(" @ ").append(rate);

        if (showPrimitives && bestPrimitives != null) {
            builder.append(":");
            for (TransformPrimitive primitive : bestPrimitives) {
                builder.append("\t").append(primitive).append("\n");
            }
        }

        return builder.toString();
    }



    Map<Agent, Agent> createCloneMap(Map<Agent, Agent> originalMap) {
        Map<Agent, Agent> result = new HashMap<Agent, Agent>();
        List<Agent> templateAgents = new ArrayList<Agent>(originalMap.keySet());
        while (!templateAgents.isEmpty()) {
            Agent agent = templateAgents.get(0);
            Map<Agent, Agent> linkedMapEntries = getLinkedMapEntries(originalMap, agent);

            Complex complex = originalMap.get(agent).getComplex();
            Complex cloneComplex = complex.clone();
            for (Map.Entry<Agent, Agent> entry : linkedMapEntries.entrySet()) {
                int agentIndex = complex.agents.indexOf(entry.getValue());
                if (agentIndex >= 0) {
                    result.put(entry.getKey(), cloneComplex.agents.get(agentIndex));
                }
            }

            templateAgents.removeAll(linkedMapEntries.keySet());
        }

        return result;
    }

    private Map<Agent, Agent> getLinkedMapEntries(Map<Agent, Agent> originalMap, Agent agent) {
        Map<Agent, Agent> result = new HashMap<Agent, Agent>();
        for (Map.Entry<Agent, Agent> entry : originalMap.entrySet()) {
            if (entry.getKey() == agent || entry.getKey().getComplex() == agent.getComplex()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public List<Complex> apply(List<ComplexMapping> sourceComplexMappings, List<Channel> channels, List<Compartment> compartments) {
        boolean transportComplexesOnly = false;
        List<Complex> complexes = new ArrayList<Complex>();
        Map<Agent, Agent> transformMap = new HashMap<Agent, Agent>();
        for (ComplexMapping complexMapping : sourceComplexMappings) {
            if (complexMapping.template != ComplexMapping.UNSPECIFIED_COMPLEX) {
                transformMap.putAll(complexMapping.mapping);
            }
            else {
                transportComplexesOnly = true;
                complexes.add(complexMapping.target.clone());
            }
        }

        if (!transportComplexesOnly) {
            transformMap = createCloneMap(transformMap);
    
            for (Agent agent : transformMap.values()) {
                if (!complexes.contains(agent.getComplex())) {
                    complexes.add(agent.getComplex());
                }
            }
        }
        
        for (TransformPrimitive primitive : bestPrimitives) {
            primitive.apply(transformMap, complexes, channels, compartments);
        }

        populateEmptyLinks(complexes);
        splitAllComplexes(complexes);
        return complexes;
    }

    private void splitAllComplexes(List<Complex> complexes) {
        int complexCount = complexes.size();
        for (int index = 0; index < complexCount; index++) {
            Complex complex = complexes.get(index);
            Complex splitComplex = complex.splitComplex();
            while (splitComplex != null) {
                complexes.add(splitComplex);
                splitComplex = splitComplex.splitComplex();
            }
        }
    }

    private void populateEmptyLinks(List<Complex> complexes) {
        for (Complex complex : complexes) {
            for (Agent agent : complex.agents) {
                for (AgentSite site : agent.getSites()) {
                    if (agent.getLink(site.name) == null) {
                        complex.agentLinks.add(AgentLink.getNoneLink(site));
                    }
                }
            }
        }
    }
    
    @Override
    protected Transition clone() {
        // TODO Check if copies are needed
        return new Transition(label, leftAgents, channelName, rightAgents, rate);
    }

    public Map<Agent, Agent> getLeftRightAgentMap() {
        Map<Agent, Agent> result = new HashMap<Agent, Agent>();
        for (int index = 0; index < bestIndexMapLeftRight.length; index++) {
            if (bestIndexMapLeftRight[index] != UNMAPPED && bestIndexMapLeftRight[index] != DELETED) {
                result.put(leftAgents.get(index), rightAgents.get(bestIndexMapLeftRight[index]));
            }
        }
        return result;
    }

    public boolean canApply(List<ComplexMapping> sourceComplexMappings, List<Channel> channels, List<Compartment> compartments) {
        boolean transportComplexesOnly = false;
        List<Complex> complexes = new ArrayList<Complex>();
        Map<Agent, Agent> transformMap = new HashMap<Agent, Agent>();
        for (ComplexMapping complexMapping : sourceComplexMappings) {
            if (complexMapping.template != ComplexMapping.UNSPECIFIED_COMPLEX) {
                transformMap.putAll(complexMapping.mapping);
            }
            else {
                transportComplexesOnly = true;
                complexes.add(complexMapping.target.clone());
            }
        }

        if (!transportComplexesOnly) {
            transformMap = createCloneMap(transformMap);
    
            for (Agent agent : transformMap.values()) {
                if (!complexes.contains(agent.getComplex())) {
                    complexes.add(agent.getComplex());
                }
            }
        }
        
        for (TransformPrimitive primitive : bestPrimitives) {
            if (!primitive.apply(transformMap, complexes, channels, compartments)) {
                return false;
            }
        }

        return true;
    }

}
