package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getFlatString;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AggregateAgent {
    
    private final Map<String, AggregateSite> sites = new HashMap<String, AggregateSite>();
    private final String name;
    
    public AggregateAgent(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
    }
    
    public AggregateAgent(String name, AggregateSite... sites) {
        this(name);
        for (AggregateSite site : sites) {
            this.sites.put(site.getName(), site);
        }
    }
    
    public void addSites(Collection<AgentSite> newSites) {
        for (AgentSite current : newSites) {
            if (sites.get(current.name) == null) {
                sites.put(current.name, new AggregateSite(current));
            }
            else {
                sites.get(current.name).merge(current);
            }
        }
    }
    
    public String getName() {
        return name;
    }
    
    public Set<AggregateSite> getSites() {
        return new HashSet<AggregateSite>(sites.values());
    }
    
    @Override
    public String toString() {
        return name + "(" + getFlatString(sites.values()) + ")";
    }

}
