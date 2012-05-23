package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getFlatString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO rename to AgentDeclaration
public class AggregateAgent {
    
    private final Map<String, AggregateSite> sites = new HashMap<String, AggregateSite>();
    private final List<String> siteNames = new ArrayList<String>();
    private final String name;
    
    public AggregateAgent(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
    }
    
    public AggregateAgent(String name, AggregateSite... sites) {
        this(name, Arrays.asList(sites));
    }
    
    public AggregateAgent(String name, List<AggregateSite> sites) {
        this(name);
        for (AggregateSite site : sites) {
            this.sites.put(site.getName(), site);
            siteNames.add(site.getName());
        }
    }
    
    public void addSites(Collection<AgentSite> newSites) {
        for (AgentSite current : newSites) {
            if (sites.get(current.name) == null) {
                sites.put(current.name, new AggregateSite(current));
                siteNames.add(current.name);
            }
            else {
                sites.get(current.name).merge(current);
            }
        }
    }
    
    public String getName() {
        return name;
    }
    
    public List<AggregateSite> getSites() {
    	List<AggregateSite> orderedSites = new ArrayList<AggregateSite>();
    	for (String siteName : siteNames) {
    		orderedSites.add(sites.get(siteName));
    	}
        return orderedSites;
    }
    
    @Override
    public String toString() {
    	if (sites.size() == 0) {
    		return name;
    	}
    	List<AggregateSite> orderedSites = new ArrayList<AggregateSite>();
    	for (String siteName : siteNames) {
    		orderedSites.add(sites.get(siteName));
    	}
        return name + "(" + getFlatString(orderedSites) + ")";
    }

}
