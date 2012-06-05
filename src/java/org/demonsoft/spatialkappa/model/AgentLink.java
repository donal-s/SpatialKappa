package org.demonsoft.spatialkappa.model;

import java.io.Serializable;

public class AgentLink implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final AgentSite OCCUPIED = new AgentSite("occupied", null, null);
    public static final AgentSite ANY = new AgentSite("any", null, null);
    public static final AgentSite NONE = new AgentSite("none", null, null);

    public final AgentSite sourceSite;
    public final AgentSite targetSite;

    public AgentLink(AgentSite sourceSite, AgentSite targetSite) {
        if (sourceSite == null || targetSite == null) {
            throw new NullPointerException();
        }
        if (sourceSite == OCCUPIED || sourceSite == ANY) {
            throw new IllegalArgumentException();
        }
        this.sourceSite = sourceSite;
        this.targetSite = targetSite;
    }

    public Agent getLinkedAgent(Agent sourceAgent) {
        if (sourceAgent == null) {
            throw new NullPointerException();
        }
        if (sourceAgent == sourceSite.agent) {
            return targetSite.agent;
        }
        if (sourceAgent == targetSite.agent) {
            return sourceSite.agent;
        }
        throw new IllegalArgumentException();
    }
    
    public AgentSite getSite(Agent agent) {
        if (agent == null) {
            throw new NullPointerException();
        }
        if (agent == sourceSite.agent) {
            return sourceSite;
        }
        if (agent == targetSite.agent) {
            return targetSite;
        }
        throw new IllegalArgumentException();
    }
    
    @Override
    public String toString() {
        return sourceSite + "->" + targetSite;
    }

    public static AgentLink getAnyLink(AgentSite site1) {
        return new AgentLink(site1, ANY);
    }

    public static AgentLink getNoneLink(AgentSite site1) {
        return new AgentLink(site1, NONE);
    }

    public static AgentLink getOccupiedLink(AgentSite site1) {
        return new AgentLink(site1, OCCUPIED);
    }
}
