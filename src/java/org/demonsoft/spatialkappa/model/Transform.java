package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.TransformPrimitive.Type;


public class Transform extends Transition {

    private static final int AGENT_PENALTY = 100;
    private static final int STATE_CHANGE_PENALTY = 10;
    private static final int LINK_PENALTY = 1;
    
    private static final int DELETED = -1;
    private static final int UNMAPPED = -2;

    List<TransformPrimitive> bestPrimitives;
    int bestPrimitivesCost = Integer.MAX_VALUE;
    public final List<Agent> leftAgents;
    public final List<Agent> rightAgents;


    public Transform(String label, List<Complex> leftSideComplexes, List<Complex> rightSideComplexes, VariableExpression rate) {
        super(label, rate);
        if ((leftSideComplexes == null || leftSideComplexes.size() == 0) && (rightSideComplexes == null || rightSideComplexes.size() == 0)) {
            throw new IllegalArgumentException("Both left and right complex lists may not be empty");
        }

        if (leftSideComplexes != null) {
            sourceComplexes.addAll(leftSideComplexes);
        }
        if (rightSideComplexes != null) {
            targetComplexes.addAll(rightSideComplexes);
        }

        leftAgents = (leftSideComplexes != null) ? getAgents(leftSideComplexes) : new ArrayList<Agent>();
        rightAgents = (rightSideComplexes != null) ? getAgents(rightSideComplexes) : new ArrayList<Agent>();
        createTransformMap(leftAgents, rightAgents);
    }

    public Transform(String label, List<Agent> leftAgents, List<Agent> rightAgents, VariableExpression rate, @SuppressWarnings("unused") boolean dummy) {
        super(label, rate);
        if ((leftAgents == null || leftAgents.size() == 0) && (rightAgents == null || rightAgents.size() == 0)) {
            throw new IllegalArgumentException("Both left and right agent lists may not be empty");
        }

        if (leftAgents != null) {
            sourceComplexes.addAll(Utils.getComplexes(leftAgents));
        }
        if (rightAgents != null) {
            targetComplexes.addAll(Utils.getComplexes(rightAgents));
        }

        this.leftAgents = (leftAgents != null) ? leftAgents : new ArrayList<Agent>();
        this.rightAgents = (rightAgents != null) ? rightAgents : new ArrayList<Agent>();
        createTransformMap(this.leftAgents, this.rightAgents);
    }

    // For unit tests
    public Transform(String label, List<Complex> leftSideComplexes, List<Complex> rightSideComplexes, float rate) {
        this(label, leftSideComplexes, rightSideComplexes, new VariableExpression(rate));
    }

    // For unit tests
    public Transform(String label, List<Agent> leftAgents, List<Agent> rightAgents, float rate, boolean dummy) {
        this(label, leftAgents, rightAgents, new VariableExpression(rate), dummy);
    }

    public static List<Agent> getAgents(List<Complex> complexes) {
        List<Agent> result = new ArrayList<Agent>();
        for (Complex complex : complexes) {
            result.addAll(complex.agents);
        }
        return result;
    }

    void createTransformMap(List<Agent> leftSideAgents, List<Agent> rightSideAgents) {
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
        createTransformMap(leftSideAgents, rightSideAgents, indexMapLeftRight, indexMapRightLeft, deletes);
    }

    private void chooseLowerCostPrimitives(List<TransformPrimitive> currentPrimitives) {
        int currentScore = getPrimitivesCost(currentPrimitives);
        if (currentScore < bestPrimitivesCost) {
            bestPrimitives = currentPrimitives;
            bestPrimitivesCost = currentScore;
        }
    }

    int getPrimitivesCost(List<TransformPrimitive> primitives) {
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
                key = primitive.agentLink.targetSite.agent.name + "@" + primitive.agentLink.targetSite.name;
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

    private void createTransformMap(List<Agent> leftSideAgents, List<Agent> rightSideAgents,
            int[] indexMapLeftRight, int[] indexMapRightLeft, Map<String, Integer> deletes) {

        if (countUnmappedNodes(indexMapLeftRight) == 0) {
            chooseLowerCostPrimitives(createPrimitives(leftSideAgents, rightSideAgents, indexMapLeftRight, indexMapRightLeft));
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
            createTransformMap(leftSideAgents, rightSideAgents, indexMapLeftRight, indexMapRightLeft, deletes);
            indexMapLeftRight[candidatePair[0]] = UNMAPPED;
            if (candidatePair[1] != DELETED) {
                indexMapRightLeft[candidatePair[1]] = UNMAPPED;
            }
            else { // DELETED
                deletes.put(deletedAgentName, deletes.get(deletedAgentName) + 1);
            }
        }
    }

    private List<TransformPrimitive> createPrimitives(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] indexMapLeftRight, int[] indexMapRightLeft) {
        List<TransformPrimitive> primitives = new ArrayList<TransformPrimitive>();
        Set<AgentLink> deletedLinks = new HashSet<AgentLink>();
        Set<Agent> deletedAgents = new HashSet<Agent>();
        Set<Complex> createdComplexes = new HashSet<Complex>();

        createPrimitivesDeleteAgentsAndLinks(leftSideAgents, rightSideAgents, indexMapLeftRight, primitives, deletedLinks, deletedAgents);

        createPrimitivesCreateComplexes(rightSideAgents, indexMapRightLeft, primitives, createdComplexes);

        createPrimitivesCreateAgentsAndMergeComplexes(leftSideAgents, rightSideAgents, indexMapRightLeft, primitives, createdComplexes);

        createPrimitivesChangeSiteStates(leftSideAgents, rightSideAgents, indexMapLeftRight, primitives);

        createPrimitivesAddLinks(leftSideAgents, rightSideAgents, indexMapRightLeft, primitives);

        return primitives;
    }

    private void createPrimitivesDeleteAgentsAndLinks(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] indexMapLeftRight,
            List<TransformPrimitive> primitives, Set<AgentLink> deletedLinks, Set<Agent> deletedAgents) {
        for (int index = 0; index < indexMapLeftRight.length; index++) {
            Agent leftAgent = leftSideAgents.get(index);
            if (indexMapLeftRight[index] == DELETED) {
                for (AgentLink link : leftAgent.getLinks()) {
                    if (!deletedLinks.contains(link)) {
                        deletedLinks.add(link);
                    }
                }
                if (!deletedAgents.contains(leftAgent)) {
                    primitives.add(TransformPrimitive.getDeleteAgent(leftAgent));
                    deletedAgents.add(leftAgent);
                }
            }
            else {
                Agent rightAgent = rightSideAgents.get(indexMapLeftRight[index]);
                for (AgentLink link : leftAgent.getLinks()) {
                    Agent leftTargetAgent = link.getLinkedAgent(leftAgent);
                    AgentSite leftSite = link.getSite(leftAgent);
                    if (leftTargetAgent != null) {
                        AgentLink rightLink = rightAgent.getLink(leftSite.name);
                        if (rightLink == null) {
                            if (!deletedLinks.contains(link)) {
                                primitives.add(TransformPrimitive.getDeleteLink(link));
                                deletedLinks.add(link);
                            }
                            continue;
                        }
                        Agent rightTargetAgent = rightLink.getLinkedAgent(rightAgent);
                        if ((rightTargetAgent == null || indexMapLeftRight[leftSideAgents.indexOf(leftTargetAgent)] != rightSideAgents
                                .indexOf(rightTargetAgent))
                                && !deletedLinks.contains(link)) {
                            primitives.add(TransformPrimitive.getDeleteLink(link));
                            deletedLinks.add(link);
                        }
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

    private void createPrimitivesAddLinks(List<Agent> leftSideAgents, List<Agent> rightSideAgents, int[] indexMapRightLeft, List<TransformPrimitive> primitives) {
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
                                primitives.add(TransformPrimitive.getCreateLink(leftSite, leftTargetSite));
                                addedLinks.add(link);
                            }
                            continue;
                        }

                        Agent leftCurrentTargetAgent = leftLink.getLinkedAgent(leftAgent);
                        if ((leftCurrentTargetAgent == null || indexMapRightLeft[rightTargetIndex] != leftSideAgents.indexOf(leftCurrentTargetAgent))
                                && !addedLinks.contains(link)) {
                            primitives.add(TransformPrimitive.getCreateLink(leftSite, leftTargetSite));
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
        String leftText = sourceComplexes.toString();
        String rightText = targetComplexes.toString();

        String result = "'" + label + "' " + ": " + leftText.substring(1, leftText.length() - 1) + " -> " + rightText.substring(1, rightText.length() - 1)
                + " @ " + rate + "\n";
        if (bestPrimitives != null) {
            for (TransformPrimitive primitive : bestPrimitives) {
                result += "\t" + primitive + "\n";
            }
        }
        return result;
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

    public List<Complex> apply(List<ComplexMapping> sourceComplexMappings) {
        Map<Agent, Agent> transformMap = new HashMap<Agent, Agent>();
        for (ComplexMapping complexMapping : sourceComplexMappings) {
            transformMap.putAll(complexMapping.mapping);
        }

        transformMap = createCloneMap(transformMap);

        List<Complex> complexes = new ArrayList<Complex>();
        for (Agent agent : transformMap.values()) {
            if (!complexes.contains(agent.getComplex())) {
                complexes.add(agent.getComplex());
            }
        }

        for (TransformPrimitive primitive : bestPrimitives) {
            primitive.apply(transformMap, complexes);
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

    public int getCountForAgent(String agentName, List<Complex> complexes) {
        int result = 0;

        for (Complex complex : complexes) {
            for (Agent currentAgent : complex.agents) {
                if (agentName.equals(currentAgent.name)) {
                    result++;
                }
            }
        }

        return result;
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
    protected Transform clone() {
        return new Transform(label, sourceComplexes, targetComplexes, rate);
    }
}
