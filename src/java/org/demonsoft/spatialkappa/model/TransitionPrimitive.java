package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getChannel;
import static org.demonsoft.spatialkappa.model.Utils.getFlatString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class TransitionPrimitive {
    public enum Type {
        DELETE_LINK, DELETE_AGENT, CREATE_COMPLEX, MERGE_COMPLEXES, CREATE_AGENT, CREATE_LINK, CHANGE_STATE, MOVE_COMPLEX, MOVE_AGENTS
    }

    public final TransitionPrimitive.Type type;
    public final Agent sourceAgent;
    public final Agent targetAgent;
    public final AgentLink agentLink;
    public final Complex complex;
    public final AgentSite sourceSite;
    public final AgentSite targetSite;
    public final String state;
    public final Location sourceLocation;
    public final Location targetLocation;
    public final String channelName;
    public final List<Agent> sourceAgents;
    public final List<Location> targetLocations;

    TransitionPrimitive(Type type, AgentLink agentLink, Agent sourceAgent, Agent targetAgent, Complex complex, AgentSite sourceSite,
            AgentSite targetSite, String state, Location sourceLocation, Location targetLocation, String channelName) {
        this.type = type;
        this.sourceAgent = sourceAgent;
        this.targetAgent = targetAgent;
        this.agentLink = agentLink;
        this.sourceSite = sourceSite;
        this.targetSite = targetSite;
        this.complex = complex;
        this.state = state;
        this.sourceLocation = sourceLocation;
        this.targetLocation = targetLocation;
        this.channelName = channelName;
        this.sourceAgents = null;
        this.targetLocations = null;
    }

    public TransitionPrimitive(Type type, List<Agent> sourceAgents, List<Location> targetLocations,
            String channelName) {
        this.type = type;
        this.sourceAgent = null;
        this.targetAgent = null;
        this.agentLink = null;
        this.sourceSite = null;
        this.targetSite = null;
        this.complex = null;
        this.state = null;
        this.sourceLocation = null;
        this.targetLocation = null;
        this.channelName = channelName;
        this.sourceAgents = sourceAgents;
        this.targetLocations = targetLocations;
    }

    @Override
    public String toString() {
        return type + "(" + getFlatString(", ", true, sourceAgent, sourceAgents, targetAgent, agentLink, complex, sourceSite, targetSite, state, sourceLocation, targetLocation, targetLocations, channelName) + ")";
    }
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agentLink == null) ? 0 : agentLink.hashCode());
        result = prime * result + ((channelName == null) ? 0 : channelName.hashCode());
        result = prime * result + ((complex == null) ? 0 : complex.hashCode());
        result = prime * result + ((sourceAgent == null) ? 0 : sourceAgent.hashCode());
        result = prime * result + ((sourceLocation == null) ? 0 : sourceLocation.hashCode());
        result = prime * result + ((sourceSite == null) ? 0 : sourceSite.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + ((targetAgent == null) ? 0 : targetAgent.hashCode());
        result = prime * result + ((targetLocation == null) ? 0 : targetLocation.hashCode());
        result = prime * result + ((targetSite == null) ? 0 : targetSite.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TransitionPrimitive other = (TransitionPrimitive) obj;
        if (agentLink == null) {
            if (other.agentLink != null)
                return false;
        }
        else if (!agentLink.equals(other.agentLink))
            return false;
        if (channelName == null) {
            if (other.channelName != null)
                return false;
        }
        else if (!channelName.equals(other.channelName))
            return false;
        if (complex == null) {
            if (other.complex != null)
                return false;
        }
        else if (!complex.equals(other.complex))
            return false;
        if (sourceAgent == null) {
            if (other.sourceAgent != null)
                return false;
        }
        else if (!sourceAgent.equals(other.sourceAgent))
            return false;
        if (sourceLocation == null) {
            if (other.sourceLocation != null)
                return false;
        }
        else if (!sourceLocation.equals(other.sourceLocation))
            return false;
        if (sourceSite == null) {
            if (other.sourceSite != null)
                return false;
        }
        else if (!sourceSite.equals(other.sourceSite))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        }
        else if (!state.equals(other.state))
            return false;
        if (targetAgent == null) {
            if (other.targetAgent != null)
                return false;
        }
        else if (!targetAgent.equals(other.targetAgent))
            return false;
        if (targetLocation == null) {
            if (other.targetLocation != null)
                return false;
        }
        else if (!targetLocation.equals(other.targetLocation))
            return false;
        if (targetSite == null) {
            if (other.targetSite != null)
                return false;
        }
        else if (!targetSite.equals(other.targetSite))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    public abstract boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels, List<Compartment> compartments);

    protected String getNewLinkId(List<Complex> targetComplexes) {
        Set<Integer> foundLinks = new HashSet<Integer>();
        for (@SuppressWarnings("hiding")
        Complex complex : targetComplexes) {
            for (Agent agent : complex.agents) {
                for (AgentSite site1 : agent.getSites()) {
                    String linkname = site1.getLinkName();
                    if (linkname != null) {
                        try {
                            foundLinks.add(Integer.parseInt(linkname));
                        }
                        catch (NumberFormatException ex) {
                            // ignore
                        }
                    }
                }
            }
        }       
        int number = 1;
        while (true) {
            if (!foundLinks.contains(number)) {
                return "" + number;
            }
            number++;
        }
    }

    public static TransitionPrimitive getDeleteLink(AgentLink agentLink) {
        return new TransitionPrimitive(Type.DELETE_LINK, agentLink, null, null, null, null, null, null, null, null, null) {
            @Override
            public boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels, List<Compartment> compartments) {
                Agent mappedSourceAgent = transformMap.get(agentLink.sourceSite.agent);
                mappedSourceAgent.getComplex().deleteLink(mappedSourceAgent, agentLink.sourceSite.name);
                return true;
            }
        };
    }

    public static TransitionPrimitive getCreateLink(AgentSite sourceSite, AgentSite targetSite, String channelName) {
        if (sourceSite == null || targetSite == null) {
            throw new NullPointerException();
        }
        return new TransitionPrimitive(Type.CREATE_LINK, null, null, null, null, sourceSite, targetSite, null, null, null, channelName) {
            @Override
            public boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels, List<Compartment> compartments) {

                Agent mappedSourceAgent = transformMap.get(sourceSite.agent);
                AgentSite mappedSourceSite = mappedSourceAgent.getSite(sourceSite.name);

                if (targetSite == AgentLink.ANY || targetSite == AgentLink.NONE || targetSite == AgentLink.OCCUPIED) {
                    mappedSourceAgent.getComplex().createAgentLink(mappedSourceSite, targetSite, null, channelName);
                }
                else {
                    Agent mappedTargetAgent = transformMap.get(targetSite.agent);
                    AgentSite mappedTargetSite = mappedTargetAgent.getSite(targetSite.name);
                    String linkID = getNewLinkId(targetComplexes);
                    mappedSourceAgent.getComplex().createAgentLink(mappedSourceSite, mappedTargetSite, linkID, channelName);

                    if (mappedTargetAgent.getComplex() != mappedSourceAgent.getComplex()) {
                        throw new IllegalArgumentException("Link sites not in same complex");
                    }
                }
                return true;
            }
            
            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                builder.append(type).append("(").append(sourceSite.agent).append(" [").append(sourceSite).append("] -> ");
                builder.append(targetSite.agent).append(" [").append(targetSite).append("]");
                if (channelName != null) {
                    builder.append(" ").append(channelName);
                }
                builder.append(")");
                return builder.toString();
            }
        };
    }

    public static TransitionPrimitive getDeleteAgent(Agent agent) {
        return new TransitionPrimitive(Type.DELETE_AGENT, null, agent, null, null, null, null, null, null, null, null) {
            @Override
            public boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels, List<Compartment> compartments) {
                Agent mappedSourceAgent = transformMap.get(sourceAgent);
                @SuppressWarnings("hiding")
                Complex complex = mappedSourceAgent.getComplex();
                complex.deleteAgent(mappedSourceAgent);
                if (complex.agents.size() == 0) {
                    targetComplexes.remove(complex);
                }
                return true;
            }
        };
    }

    public static TransitionPrimitive getCreateComplex(Complex complex) {
        return new TransitionPrimitive(Type.CREATE_COMPLEX, null, null, null, complex, null, null, null, null, null, null) {
            @Override
            public boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels, List<Compartment> compartments) {
                Complex cloneComplex = complex.clone();
                targetComplexes.add(cloneComplex);
                return true;
            }
        };
    }

    public static TransitionPrimitive getCreateAgent(Agent sourceAgent, Agent targetAgent) {
        return new TransitionPrimitive(Type.CREATE_AGENT, null, sourceAgent, targetAgent, null, null, null, null, null, null, null) {
            @Override
            public boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels, List<Compartment> compartments) {
                Agent cloneAgent = sourceAgent.clone();
                for (AgentSite site1 : cloneAgent.getSites()) {
                    site1.setLinkName(null);
                }
                Agent mappedTargetAgent = transformMap.get(targetAgent);
                mappedTargetAgent.getComplex().addAgent(cloneAgent);
                transformMap.put(sourceAgent, cloneAgent);
                return true;
            }
        };
    }

    public static TransitionPrimitive getChangeState(Agent agent, AgentSite agentSite, String state) {
        return new TransitionPrimitive(Type.CHANGE_STATE, null, agent, null, null, agentSite, null, state, null, null, null) {
            @Override
            public boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels, List<Compartment> compartments) {
                Agent target = transformMap.get(sourceAgent);
                AgentSite site = target.getSite(sourceSite.name);
                site.setState(state);
                return true;
            }
        };
    }

    public static TransitionPrimitive getMergeComplexes(Agent sourceAgent, Agent targetAgent) {
        return new TransitionPrimitive(Type.MERGE_COMPLEXES, null, sourceAgent, targetAgent, null, null, null, null, null, null, null) {
            @Override
            public boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels, List<Compartment> compartments) {
                Agent mappedSourceAgent = transformMap.get(sourceAgent);
                Agent mappedTargetAgent = transformMap.get(targetAgent);

                if (mappedSourceAgent.getComplex() == mappedTargetAgent.getComplex()) {
                    return true;
                }

                Complex sourceComplex = mappedSourceAgent.getComplex();
                targetComplexes.remove(sourceComplex);
                int maxLinkIdNumber = mappedTargetAgent.getComplex().renumberLinkNames(1);
                sourceComplex.renumberLinkNames(maxLinkIdNumber + 1);
                mappedTargetAgent.getComplex().mergeComplex(sourceComplex);
                return true;
            }
        };
    }

    protected int getMaxLinkIdNumber(List<Complex> targetComplexes) {
        int maxFound = 0;
        for (@SuppressWarnings("hiding")
        Complex complex : targetComplexes) {
            for (Agent agent : complex.agents) {
                for (AgentSite site1 : agent.getSites()) {
                    String linkname = site1.getLinkName();
                    if (linkname != null) {
                        try {
                            int value = Integer.parseInt(linkname);
                            if (value > maxFound) {
                                maxFound = value;
                            }
                        }
                        catch (NumberFormatException ex) {
                            // ignore
                        }
                    }
                }
            }
        }

        return maxFound;
    }

    public static TransitionPrimitive getMoveComplex(Location sourceLocation, Location targetLocation, String channelName) {
        return new TransitionPrimitive(Type.MOVE_COMPLEX, null, null, null, null, null, null, null, sourceLocation, targetLocation, channelName) {

            @Override
            public int getApplicationCount(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels,
                    List<Compartment> compartments) {
                Complex targetComplex = targetComplexes.get(0);
                Location oldLocation = targetComplex.agents.get(0).location;
                
                Channel channel = getChannel(channels, channelName);
                
                List<Location> newLocations = channel.applyChannel(oldLocation, targetLocation, compartments);
                return newLocations.size();
            }
            
            @Override
            public boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels, List<Compartment> compartments) {
                Complex targetComplex = targetComplexes.get(0);
                Location oldLocation = targetComplex.agents.get(0).location;
                
                Channel channel = getChannel(channels, channelName);
                
                List<Location> newLocations = channel.applyChannel(oldLocation, targetLocation, compartments);
                Location newLocation;
                if (newLocations.size() == 1) {
                    newLocation = newLocations.get(0);
                }
                else {
                    int item = (int) (newLocations.size() * Math.random());
                    newLocation =  newLocations.get(item);
                }
                
                for (Agent agent : targetComplex.agents) {
                    agent.setLocation(newLocation);
                }
                return true;
            }
        };
    }

    public static TransitionPrimitive getMoveAgents(List<Agent> leftAgents, List<Location> targetLocations, String channelName) {
        return new TransitionPrimitive(Type.MOVE_AGENTS, leftAgents, targetLocations, channelName) {
            @Override
            public int getApplicationCount(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels,
                    List<Compartment> compartments) {

                List<ChannelConstraint> channelConstraints = getChannelConstraints(transformMap);
                List<List<Location>> newLocationLists = getPossibleChannelApplications(channelConstraints, channels, compartments);
                return newLocationLists.size();
            }
            
            @Override
            public boolean apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels,
                    List<Compartment> compartments) {

                List<ChannelConstraint> channelConstraints = getChannelConstraints(transformMap);
                List<List<Location>> newLocationLists = getPossibleChannelApplications(channelConstraints, channels, compartments);
                
                while (true) {
                    List<Location> newLocations;
                    if (newLocationLists.size() == 0) {
                        return false;
                    }
                    else if (newLocationLists.size() == 1) {
                        newLocations = newLocationLists.get(0);
                    }
                    else {
                        int item = (int) (newLocationLists.size() * Math.random());
                        newLocations =  newLocationLists.get(item);
                    }
    
                    Map<Agent, Agent> cloneTransformMap = Utils.createCloneAgentMap(transformMap);
                    Set<Agent> movedAgents = new HashSet<Agent>();
                    for (int index=0; index<sourceAgents.size(); index++) {
                        @SuppressWarnings("hiding")
                        Agent sourceAgent = sourceAgents.get(index);
                        Location targetConstraint = targetLocations.get(index);
                        Agent realAgent = cloneTransformMap.get(sourceAgent);
                        if (!movedAgents.contains(realAgent)) {
                            Location oldLocation = realAgent.location;
                            ChannelConstraint channelConstraint = new ChannelConstraint(oldLocation, targetConstraint);
                            
                            Location newLocation = newLocations.get(channelConstraints.indexOf(channelConstraint));
                            for (Agent agent : Utils.getLinkedColocatedAgents(realAgent)) {
                                if (!movedAgents.contains(agent)) {
                                    agent.setLocation(newLocation);
                                    movedAgents.add(agent);
                                }
                            }
                        }
                    }
                    if (Utils.isValidComplexes(cloneTransformMap.values(), channels, compartments)) {
                        for (Agent key : transformMap.keySet()) {
                            Complex cloneComplex = cloneTransformMap.get(key).getComplex();
                            Complex originalComplex = transformMap.get(key).getComplex();
                            for (int index=0; index < cloneComplex.agents.size(); index++) {
                                Agent cloneAgent = cloneComplex.agents.get(index);
                                Agent originalAgent = originalComplex.agents.get(index);
                                
                                originalAgent.setLocation(cloneAgent.location);
                                // TODO performance improvement - hit each complex only once
                            }
                        }
                        return true;
                    }
                    newLocationLists.remove(newLocations);
                }
            }

            private List<List<Location>> getPossibleChannelApplications(List<ChannelConstraint> channelConstraints, List<Channel> channels,
                    List<Compartment> compartments) {
                
                Channel channel = getChannel(channels, channelName);
                if (channelConstraints.size() == 0) {
                    return new ArrayList<List<Location>>(); // TODO constant
                }
                return channel.applyChannel(channelConstraints, compartments);
            }
            
            private List<ChannelConstraint> getChannelConstraints(Map<Agent, Agent> transformMap) {
                List<ChannelConstraint> result = new ArrayList<ChannelConstraint>();
                
                for (int index=0; index<sourceAgents.size(); index++) {
                    @SuppressWarnings("hiding")
                    Agent sourceAgent = sourceAgents.get(index);
                    Location targetConstraint = targetLocations.get(index);
                    Agent realAgent = transformMap.get(sourceAgent);
                    Location oldLocation = realAgent.location;
                    
                    if (!oldLocation.equals(sourceAgent.location) && !sourceAgent.location.isRefinement(oldLocation)) {
                        return new ArrayList<ChannelConstraint>(); // TODO constant
                    }
                    ChannelConstraint channelConstraint = new ChannelConstraint(oldLocation, targetConstraint);
                    if (!result.contains(channelConstraint)) {
                        result.add(channelConstraint);
                    }
                }
                
                return result;
            }
        };
        
        
        // TODO additional constraints for linked agents
    }

    @SuppressWarnings("unused")
    public int getApplicationCount(Map<Agent, Agent> transformMap, List<Complex> targetComplexes, List<Channel> channels,
            List<Compartment> compartments) {
        return 1;
    }

}