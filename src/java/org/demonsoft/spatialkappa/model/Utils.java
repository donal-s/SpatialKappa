package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
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

    public static List<Agent> getAgents(List<Complex> complexes) {
        List<Agent> result = new ArrayList<Agent>();
        for (Complex complex : complexes) {
            result.addAll(complex.agents);
        }
        return result;
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

}
