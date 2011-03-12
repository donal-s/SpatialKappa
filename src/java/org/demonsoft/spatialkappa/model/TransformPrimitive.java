package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getFlatString;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class TransformPrimitive {
    enum Type {
        DELETE_LINK, DELETE_AGENT, CREATE_COMPLEX, MERGE_COMPLEXES, CREATE_AGENT, CREATE_LINK, CHANGE_STATE
    }

    public final TransformPrimitive.Type type;
    public final Agent sourceAgent;
    public final Agent targetAgent;
    public final AgentLink agentLink;
    public final Complex complex;
    public final AgentSite sourceSite;
    public final AgentSite targetSite;
    public final String state;

    TransformPrimitive(TransformPrimitive.Type type, AgentLink agentLink, Agent sourceAgent, Agent targetAgent, Complex complex, AgentSite sourceSite,
            AgentSite targetSite, String state) {
        this.type = type;
        this.sourceAgent = sourceAgent;
        this.targetAgent = targetAgent;
        this.agentLink = agentLink;
        this.sourceSite = sourceSite;
        this.targetSite = targetSite;
        this.complex = complex;
        this.state = state;
    }

    @Override
    public String toString() {
        return type + "(" + getFlatString(", ", true, sourceAgent, targetAgent, agentLink, complex, sourceSite, targetSite, state) + ")";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agentLink == null) ? 0 : agentLink.hashCode());
        result = prime * result + ((complex == null) ? 0 : complex.hashCode());
        result = prime * result + ((sourceAgent == null) ? 0 : sourceAgent.hashCode());
        result = prime * result + ((sourceSite == null) ? 0 : sourceSite.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + ((targetAgent == null) ? 0 : targetAgent.hashCode());
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
        TransformPrimitive other = (TransformPrimitive) obj;
        if (agentLink == null) {
            if (other.agentLink != null)
                return false;
        }
        else if (!agentLink.equals(other.agentLink))
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
        if (targetSite == null) {
            if (other.targetSite != null)
                return false;
        }
        else if (!targetSite.equals(other.targetSite))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        }
        else if (!type.equals(other.type))
            return false;
        return true;
    }

    public abstract void apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes);

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

    public static TransformPrimitive getDeleteLink(AgentLink agentLink) {
        return new TransformPrimitive(Type.DELETE_LINK, agentLink, null, null, null, null, null, null) {
            @Override
            public void apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes) {
                Agent mappedSourceAgent = transformMap.get(agentLink.sourceSite.agent);
                mappedSourceAgent.getComplex().deleteLink(mappedSourceAgent, agentLink.sourceSite.name);
            }
        };
    }

    public static TransformPrimitive getCreateLink(AgentSite sourceSite, AgentSite targetSite) {
        if (sourceSite == null || targetSite == null) {
            throw new NullPointerException();
        }
        return new TransformPrimitive(Type.CREATE_LINK, null, null, null, null, sourceSite, targetSite, null) {
            @Override
            public void apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes) {

                Agent mappedSourceAgent = transformMap.get(sourceSite.agent);
                AgentSite mappedSourceSite = mappedSourceAgent.getSite(sourceSite.name);

                if (targetSite == AgentLink.ANY || targetSite == AgentLink.NONE || targetSite == AgentLink.OCCUPIED) {
                    mappedSourceAgent.getComplex().createAgentLink(mappedSourceSite, targetSite, null);
                }
                else {
                    Agent mappedTargetAgent = transformMap.get(targetSite.agent);
                    AgentSite mappedTargetSite = mappedTargetAgent.getSite(targetSite.name);
                    String linkID = getNewLinkId(targetComplexes);
                    mappedSourceAgent.getComplex().createAgentLink(mappedSourceSite, mappedTargetSite, linkID);

                    if (mappedTargetAgent.getComplex() != mappedSourceAgent.getComplex()) {
                        throw new IllegalArgumentException("Link sites not in same complex");
                    }
                }
            }
            
            @Override
            public String toString() {
                return type + "(" + sourceSite.agent + " [" + sourceSite + "] <-> " + targetSite.agent + " [" + targetSite + "])";
            }
        };
    }

    public static TransformPrimitive getDeleteAgent(Agent agent) {
        return new TransformPrimitive(Type.DELETE_AGENT, null, agent, null, null, null, null, null) {
            @Override
            public void apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes) {
                Agent mappedSourceAgent = transformMap.get(sourceAgent);
                @SuppressWarnings("hiding")
                Complex complex = mappedSourceAgent.getComplex();
                complex.deleteAgent(mappedSourceAgent);
                if (complex.agents.size() == 0) {
                    targetComplexes.remove(complex);
                }
            }
        };
    }

    public static TransformPrimitive getCreateComplex(Complex complex) {
        return new TransformPrimitive(Type.CREATE_COMPLEX, null, null, null, complex, null, null, null) {
            @Override
            public void apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes) {
                Complex cloneComplex = complex.clone();
                targetComplexes.add(cloneComplex);
            }
        };
    }

    public static TransformPrimitive getCreateAgent(Agent sourceAgent, Agent targetAgent) {
        return new TransformPrimitive(Type.CREATE_AGENT, null, sourceAgent, targetAgent, null, null, null, null) {
            @Override
            public void apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes) {
                Agent cloneAgent = sourceAgent.clone();
                for (AgentSite site1 : cloneAgent.getSites()) {
                    site1.setLinkName(null);
                }
                Agent mappedTargetAgent = transformMap.get(targetAgent);
                mappedTargetAgent.getComplex().addAgent(cloneAgent);
                transformMap.put(sourceAgent, cloneAgent);
            }
        };
    }

    public static TransformPrimitive getChangeState(Agent agent, AgentSite agentSite, String state) {
        return new TransformPrimitive(Type.CHANGE_STATE, null, agent, null, null, agentSite, null, state) {
            @Override
            public void apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes) {
                Agent target = transformMap.get(sourceAgent);
                AgentSite site = target.getSite(sourceSite.name);
                site.setState(state);
            }
        };
    }

    public static TransformPrimitive getMergeComplexes(Agent sourceAgent, Agent targetAgent) {
        return new TransformPrimitive(Type.MERGE_COMPLEXES, null, sourceAgent, targetAgent, null, null, null, null) {
            @Override
            public void apply(Map<Agent, Agent> transformMap, List<Complex> targetComplexes) {
                Agent mappedSourceAgent = transformMap.get(sourceAgent);
                Agent mappedTargetAgent = transformMap.get(targetAgent);

                if (mappedSourceAgent.getComplex() == mappedTargetAgent.getComplex()) {
                    return;
                }

                Complex sourceComplex = mappedSourceAgent.getComplex();
                targetComplexes.remove(sourceComplex);
                int maxLinkIdNumber = mappedTargetAgent.getComplex().renumberLinkNames(1);
                sourceComplex.renumberLinkNames(maxLinkIdNumber + 1);
                mappedTargetAgent.getComplex().mergeComplex(sourceComplex);
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

}