package org.demonsoft.spatialkappa.model;

import java.io.Serializable;

public class AgentSite implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public final Agent agent;
    public final String name;
    private String state;
    private String linkName;
	private String channel;
    
    public AgentSite(String name, String state, String linkName) {
        this((Agent) null, name, state, linkName, null);
    }
    
    public AgentSite(Agent agent, AgentSite placeholder) {
        this(agent, placeholder.name, placeholder.state, placeholder.linkName, placeholder.channel);
    }
    
    public AgentSite(Agent agent, String name, String state, String linkName) {
        this(agent, name, state, linkName, null);
    }
    
    public AgentSite(Agent agent, String name, String state, String linkName, String channel) {
        if (name == null) {
            throw new NullPointerException();
        }
        if (channel != null && (linkName == null || "?".equals(linkName))) {
            throw new IllegalArgumentException("Channel unexpected: " + channel);
        }
        this.agent = agent;
        this.name = name;
        this.state = state;
        this.linkName = linkName;
        this.channel = channel;
    }
    
    public AgentSite(String name, String state, String linkName, String channel) {
    	this((Agent) null, name, state, linkName, channel);
	}

	public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
        agent.updateStateHash();
    }
    
    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }
    
    public boolean isNamedLink() {
        return linkName != null && !"?".equals(linkName) && !"_".equals(linkName);
    }
    
    public boolean isAbstract() {
        return "?".equals(linkName) || "_".equals(linkName);
    }
    
    @Override
    public String toString() {
        return toString(false);
    }
    
    @Override
    protected AgentSite clone() {
        return new AgentSite(name, state, linkName);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agent == null) ? 0 : agent.hashCode());
        result = prime * result + ((linkName == null) ? 0 : linkName.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
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
        AgentSite other = (AgentSite) obj;
        if (agent == null) {
            if (other.agent != null)
                return false;
        }
        else if (!agent.equals(other.agent))
            return false;
        if (linkName == null) {
            if (other.linkName != null)
                return false;
        }
        else if (!linkName.equals(other.linkName))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        }
        else if (!state.equals(other.state))
            return false;
        return true;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String toString(boolean basicKappaOnly) {
        StringBuilder output = new StringBuilder(name);
        if (state != null) {
            output.append("~").append(state);
        }
        if (linkName != null) {
            if ("?".equals(linkName)) {
                output.append("?");
            }
            else {
                output.append("!").append(linkName);
            }
        }
        if (channel != null && !basicKappaOnly) {
            output.append(":").append(channel);
        }
        return output.toString();
    }

}
