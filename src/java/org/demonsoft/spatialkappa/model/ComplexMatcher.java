package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexMatcher {
    public boolean isExactMatch(Complex template, Complex target) {
        return getMatches(template, target, true).size() > 0;
    }

    public boolean isPartialMatch(Complex template, Complex target) {
        return getMatches(template, target, false).size() > 0;
    }


    public List<ComplexMapping> getPartialMatches(Complex template, Complex target) {
        return getMatches(template, target, false);
    }

    private List<ComplexMapping> getMatches(Complex template, Complex target, boolean exactMatch) {
        if (template == null || target == null) {
            throw new NullPointerException();
        }
        List<ComplexMapping> result = new ArrayList<ComplexMapping>();
        if (isFastFail(template, target, exactMatch)) {
            return result;
        }
        int[] core1 = new int[template.agents.size()];
        int[] core2 = new int[target.agents.size()];
        Arrays.fill(core1, -1);
        Arrays.fill(core2, -1);
        getMatches(result, template, target, core1, core2, 0, exactMatch);
        return result;
    }

    private boolean isFastFail(Complex template, Complex target, boolean exactMatch) {
        if (exactMatch && (!template.getMatchHash().equals(target.getMatchHash()) || template.agentLinks.size() != target.agentLinks.size())) {
            return true;
        }
        List<String> agentNames = new ArrayList<String>();
        for (Agent agent : target.agents) {
            agentNames.add(agent.name);
        }
        for (Agent agent : template.agents) {
            if (!agentNames.remove(agent.name)) {
                return true;
            }
        }
        return false;
    }
    
    private void getMatches(List<ComplexMapping> result, Complex template, Complex target, int[] core1, int[] core2, int currentTemplateAgent, boolean exactMatch) {
        if (countUnmappedNodes(core1) == 0) {
            ComplexMapping newMap = createMapping(template, target, core1);
            if (!result.contains(newMap)) {
                result.add(newMap);
            }
            return;
        }

        List<Integer> candidateAgents = getCandidateAgents(template, target, core2, currentTemplateAgent, exactMatch);
        for (int candidateTargetAgent : candidateAgents) {
            if (isLinksMatch(template, target, core1, core2, currentTemplateAgent, candidateTargetAgent, exactMatch)) {
                core1[currentTemplateAgent] = candidateTargetAgent;
                core2[candidateTargetAgent] = currentTemplateAgent;
                getMatches(result, template, target, core1, core2, currentTemplateAgent + 1, exactMatch);
                if (exactMatch && result.size() > 0) {
                    break;
                }
            }
            core1[currentTemplateAgent] = -1;
            core2[candidateTargetAgent] = -1;
        }
    }

    private ComplexMapping createMapping(Complex template, Complex target, int[] core1) {
        Map<Agent, Agent> mapping = new HashMap<Agent, Agent>();
        for (int index = 0; index < core1.length; index++) {
            mapping.put(template.agents.get(index), target.agents.get(core1[index]));
        }
        return new ComplexMapping(template, target, mapping);
    }

    private boolean isLinksMatch(Complex template, Complex target, int[] core1, int[] core2, int templateCandidate, int targetCandidate, boolean exactMatch) {
        Agent templateCandidateAgent = template.agents.get(templateCandidate);
        Agent targetCandidateAgent = target.agents.get(targetCandidate);

        Map<String, AgentLink> templateLinks = getCandidateLinks(template, core1, templateCandidateAgent, exactMatch);
        Map<String, AgentLink> targetLinks = getCandidateLinks(target, core2, targetCandidateAgent, exactMatch);

        if (exactMatch && templateLinks.size() != targetLinks.size()) {
            return false;
        }

        for (String templateLinkName : templateLinks.keySet()) {
            AgentLink templateLink = templateLinks.get(templateLinkName);
            AgentLink targetLink = targetLinks.get(templateLinkName);

            if (targetLink == null) {
                return false;
            }

            if (exactMatch) {
                if (!isLinkExactMatch(template, target, core1, templateLink.targetSite, targetLink.targetSite)) {
                    return false;
                }
            }
            else {
                if (!isLinkPartialMatch(template, target, core1, templateLink.targetSite, targetLink.targetSite)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isLinkPartialMatch(Complex template, Complex target, int[] core1, AgentSite templateDest, AgentSite targetDest) {
        if (templateDest == AgentLink.ANY) {
            // continue
        }
        else if (templateDest == AgentLink.NONE) {
            if (targetDest != AgentLink.NONE) {
                return false;
            }
        }
        else if (templateDest == AgentLink.OCCUPIED) {
            if (targetDest == AgentLink.ANY || targetDest == AgentLink.NONE) {
                return false;
            }
        }
        else if (targetDest == AgentLink.OCCUPIED || targetDest == AgentLink.ANY || targetDest == AgentLink.NONE) {
            return false;
        }
        else {
            if (!targetDest.name.equals(templateDest.name)) {
                return false;
            }
            if (core1[template.agents.indexOf(templateDest.agent)] != target.agents.indexOf(targetDest.agent)) {
                return false;
            }
        }
        return true;
    }

    private boolean isLinkExactMatch(Complex template, Complex target, int[] core1, AgentSite templateDest, AgentSite targetDest) {
        if (templateDest == AgentLink.ANY || templateDest == AgentLink.NONE || templateDest == AgentLink.OCCUPIED) {
            return (targetDest == templateDest);
        }
        else if (targetDest == AgentLink.OCCUPIED || targetDest == AgentLink.ANY || targetDest == AgentLink.NONE) {
            return false;
        }
        else {
            if (!targetDest.name.equals(templateDest.name)) {
                return false;
            }
            if (core1[template.agents.indexOf(templateDest.agent)] != target.agents.indexOf(targetDest.agent)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, AgentLink> getCandidateLinks(Complex complex, int[] complexSubmap, Agent candidate, boolean exactMatch) {
        Map<String, AgentLink> result = new HashMap<String, AgentLink>();
        for (AgentLink link : complex.getAgentLinks(candidate)) {
            if (link.targetSite == AgentLink.ANY || link.targetSite == AgentLink.OCCUPIED || link.targetSite == AgentLink.NONE) {
                result.put(link.sourceSite.name, link);
            }
            else {
                Agent targetAgent;
                if (link.sourceSite.agent == candidate) {
                    targetAgent = link.targetSite.agent;
                    if (isAgentMapped(complex, complexSubmap, targetAgent)) {
                        result.put(link.sourceSite.name, link);
                    }
                    else if (!exactMatch) {
                        result.put(link.sourceSite.name, AgentLink.getOccupiedLink(link.sourceSite));
                    }
                }
                if (link.targetSite.agent == candidate) {
                    targetAgent = link.sourceSite.agent;
                    if (isAgentMapped(complex, complexSubmap, targetAgent)) {
                        result.put(link.targetSite.name, new AgentLink(link.targetSite, link.sourceSite));
                    }
                    else if (!exactMatch) {
                        result.put(link.targetSite.name, AgentLink.getOccupiedLink(link.targetSite));
                    }
                }
            }
        }

        return result;
    }

    private boolean isAgentMapped(Complex complex, int[] complexSubmap, Agent agent) {
        return complexSubmap[complex.agents.indexOf(agent)] > -1;
    }

    private List<Integer> getCandidateAgents(Complex template, Complex target, int[] core2, int currentTemplateAgent, boolean exactMatch) {
        List<Integer> result = new ArrayList<Integer>();
        Agent templateAgent = template.agents.get(currentTemplateAgent);
        for (int currentTarget = 0; currentTarget < core2.length; currentTarget++) {
            if (core2[currentTarget] == -1) {
                Agent targetAgent = target.agents.get(currentTarget);
                if (templateAgent.name.equals(targetAgent.name) && isStatesMatch(templateAgent, targetAgent, exactMatch)) {
                    result.add(currentTarget);
                }
            }
        }
        return result;
    }

    boolean isStatesMatch(Agent templateAgent, Agent targetAgent, boolean exactMatch) {

        if (exactMatch) {
            return templateAgent.getStateHash().equals(targetAgent.getStateHash());
        }

        for (AgentSite templateState : templateAgent.getSites()) {
            AgentSite targetState = targetAgent.getSite(templateState.name);
            if (templateState.getState() != null && targetState == null) { // TODO case when template state is null ?
                return false;
            }
            if (templateState.getState() != null && !templateState.getState().equals(targetState.getState())) {
                return false;
            }
        }
        return true;
    }

    private int countUnmappedNodes(int[] nodeMappings) {
        int result = 0;
        for (int current : nodeMappings) {
            if (current == -1) {
                result++;
            }
        }
        return result;
    }

}
