package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

public class Utils {

    public static String getFlatString(Collection<? extends Object> elements) {
        return getFlatString(",", false, elements.toArray());
    }
    
    public static String getFlatString(String seperator, boolean skipNulls, Object... elements) {
        StringBuilder builder = new StringBuilder();
        boolean firstWritten = false;
        for (int index = 0; index < elements.length; index++) {
            if (!skipNulls || elements[index] != null) {
                if (firstWritten) {
                    builder.append(seperator);
                }
                else {
                    firstWritten = true;
                }
                builder.append(elements[index]);
            }
        }
        return builder.toString();
    }

    public static List<Complex> getComplexes(List<Agent> agents) {
        if (agents == null) {
            throw new NullPointerException();
        }
        List<Complex> result = new ArrayList<Complex>();
        List<Agent> remainingAgents = new ArrayList<Agent>(agents);
        while (!remainingAgents.isEmpty()) {
            List<Agent> linkedAgents = new ArrayList<Agent>();
            Stack<String> links = new Stack<String>();

            Agent current = remainingAgents.get(0);
            remainingAgents.remove(current);
            linkedAgents.add(current);

            addLinksToStack(links, current);

            while (links.size() > 0) {
                String currentLink = links.pop();

                ListIterator<Agent> iter = remainingAgents.listIterator();
                while (iter.hasNext()) {
                    current = iter.next();
                    if (current.hasLink(currentLink)) {
                        linkedAgents.add(current);
                        iter.remove();
                        addLinksToStack(links, current);
                    }
                }
            }

            result.add(new Complex(linkedAgents));
        }
        return result;
    }

    private static void addLinksToStack(Stack<String> links, Agent agent) {
        for (AgentSite site : agent.getSites()) {
            if (site.getLinkName() != null) {
                String link = site.getLinkName();
                if (!"_".equals(link) && !"?".equals(link)) {
                    links.push(link);
                }
            }
        }
    }

    public static boolean equal(Object o1, Object o2) {
        return (o1 == null && o2 == null) || (o1 != null && o1.equals(o2));
    }

    public static <T> List<T> getList(T... elements) {
        List<T> result = new ArrayList<T>();
        result.addAll(Arrays.asList(elements));
        return result;
    }

    public static List<Agent> getLinkedAgents(Agent agent) {
        if (agent == null) {
            throw new NullPointerException();
        }
        List<Agent> result = new ArrayList<Agent>();
        result.add(agent);
        
        Stack<AgentLink> links = new Stack<AgentLink>();
        links.addAll(agent.getLinks());
        
        while (!links.isEmpty()) {
            AgentLink link = links.pop();
            Agent sourceAgent = link.sourceSite.agent;
            if (sourceAgent != null && !result.contains(sourceAgent)) {
                result.add(sourceAgent);
                links.addAll(sourceAgent.getLinks());
            }
            Agent targetAgent = link.targetSite.agent;
            if (targetAgent != null && !result.contains(targetAgent)) {
                result.add(targetAgent);
                links.addAll(targetAgent.getLinks());
            }
        }
        return result;
    }
    
    public static void propogateLocation(List<Agent> agents, Location location) {
        for (Agent agent : agents) {
            if (agent.location == NOT_LOCATED) {
                agent.setLocation(location);
            }
        }
    }

    public static void refineLocation(List<Agent> agents, Location location) {
        for (Agent agent : agents) {
            if (agent.location.isRefinement(location)) {
                agent.setLocation(location);
            }
        }
    }

    public static List<Agent> getLinkedColocatedAgents(Agent agent) {
        if (agent == null) {
            throw new NullPointerException();
        }
        List<Agent> result = new ArrayList<Agent>();
        result.add(agent);
        
        Stack<AgentLink> links = new Stack<AgentLink>();
        for (AgentLink link : agent.getLinks()) {
            if (link.getChannel() == null) {
                links.add(link);
            }
        }
        
        while (!links.isEmpty()) {
            AgentLink link = links.pop();
            Agent sourceAgent = link.sourceSite.agent;
            if (sourceAgent != null && !result.contains(sourceAgent)) {
                result.add(sourceAgent);
                for (AgentLink newlink : sourceAgent.getLinks()) {
                    if (newlink.getChannel() == null) {
                        links.add(newlink);
                    }
                }
            }
            Agent targetAgent = link.targetSite.agent;
            if (targetAgent != null && !result.contains(targetAgent)) {
                result.add(targetAgent);
                for (AgentLink newlink : targetAgent.getLinks()) {
                    if (newlink.getChannel() == null) {
                        links.add(newlink);
                    }
                }
            }
        }
        return result;
    }

    public static Channel getChannel(List<Channel> channels, String channelName) {
        for (Channel channel : channels) {
            if (channelName.equals(channel.getName())) {
                return channel;
            }
        }
        throw new IllegalStateException("Channel '" + channelName + "' not found");
    }

    public static Compartment getCompartment(List<Compartment> compartments, String compartmentName) {
        for (Compartment compartment : compartments) {
            if (compartmentName.equals(compartment.getName())) {
                return compartment;
            }
        }
        throw new IllegalStateException("Compartment '" + compartmentName + "' not found");
    }

    public static Map<Agent, Agent> createCloneAgentMap(Map<Agent, Agent> originalMap) {
        // TODO temporary check
        for (Map.Entry<Agent, Agent> entry : originalMap.entrySet()) {
            Complex expected = entry.getValue().getComplex();
            for (Agent current : expected.agents) {
                if (expected != current.getComplex()) {
                    throw new IllegalStateException("Mismatched agents: " + originalMap);
                }
            }
        }
        

        
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

    private static Map<Agent, Agent> getLinkedMapEntries(Map<Agent, Agent> originalMap, Agent agent) {
        Map<Agent, Agent> result = new HashMap<Agent, Agent>();
        for (Map.Entry<Agent, Agent> entry : originalMap.entrySet()) {
            if (entry.getKey() == agent || entry.getKey().getComplex() == agent.getComplex()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static boolean isValidComplexes(Collection<Agent> agents, List<Channel> channels,
            List<Compartment> compartments) {
        
        List<Agent> remainingAgents = new ArrayList<Agent>(agents);
        
        while (remainingAgents.size() > 0) {
            Agent currentAgent = remainingAgents.get(0);
            remainingAgents.remove(currentAgent);
            
            for (AgentLink currentLink : currentAgent.getLinks()) {
                Agent otherAgent = currentLink.getLinkedAgent(currentAgent);
                if (currentLink.getChannel() == null) {
                    if (otherAgent != null && !equal(currentAgent.location, otherAgent.location)) {
                        return false;
                    }
                }
                else {
                    Channel channel = getChannel(channels, currentLink.getChannel());
                    List<Location> possibleLocations = channel.applyChannel(currentAgent.location, NOT_LOCATED, compartments);
                    if (!possibleLocations.contains(otherAgent.location)) {
                        possibleLocations = channel.applyChannel(otherAgent.location, NOT_LOCATED, compartments);
                        if (!possibleLocations.contains(currentAgent.location)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

}
