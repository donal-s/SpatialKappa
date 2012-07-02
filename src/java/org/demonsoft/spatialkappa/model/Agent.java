package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Agent implements Serializable {

    private static final long serialVersionUID = 1L;

    final Map<String, AgentSite> sites = new HashMap<String, AgentSite>();
    final List<String> orderedSiteNames = new ArrayList<String>();
    public final String name;
    public Location location;
    private Complex complex;
    private String stateHash;

    public Agent(String name) {
        this(name, NOT_LOCATED);
    }

    public Agent(String name, Location location) {
        if (name == null || location == null) {
            throw new NullPointerException();
        }
        this.name = name;
        updateStateHash();
        this.location = location;
    }

    public Agent(String name, Collection<AgentSite> sites) {
        this(name, NOT_LOCATED, sites);
    }

    public Agent(String name, Location location, Collection<AgentSite> sites) {
        this(name, location);
        for (AgentSite site : sites) {
            this.sites.put(site.name, new AgentSite(this, site));
        }
        canonicalSortSites();
        updateStateHash();
    }

    public Agent(String name, AgentSite... sites) {
        this(name, NOT_LOCATED, sites);
    }

    public Agent(String name, Location location, AgentSite... sites) {
        this(name, location);
        if (sites == null) {
            throw new NullPointerException();
        }
        for (AgentSite site : sites) {
            this.sites.put(site.name, new AgentSite(this, site));
        }
        canonicalSortSites();
        updateStateHash();
    }

    void updateStateHash() {
        StringBuilder builder = new StringBuilder();
        for (String current : orderedSiteNames) {
            AgentSite site = sites.get(current);
            builder.append(current);
            if (site.getState() != null) {
                builder.append("~").append(site.getState());
            }
            builder.append(",");
        }
        stateHash = builder.toString();
    }

    private void canonicalSortSites() {
        orderedSiteNames.clear();
        orderedSiteNames.addAll(sites.keySet());
        Collections.sort(orderedSiteNames);
    }

    public void addSite(AgentSite site) {
        this.sites.put(site.name, new AgentSite(this, site));
        canonicalSortSites();
        updateStateHash();
    }
    
    public Collection<AgentSite> getSites() {
        return sites.values();
    }

    @Override
    public String toString() {
        return toString("", false);
    }

    public boolean hasLink(String link) {
        if (link == null) {
            throw new NullPointerException();
        }
        for (AgentSite site : sites.values()) {
            if (link.equals(site.getLinkName())) {
                return true;
            }
        }
        return false;
    }

    public AgentSite getSite(String siteName) {
        if (siteName == null) {
            throw new NullPointerException();
        }
        return sites.get(siteName);
    }

    public List<AgentLink> getLinks() {
        List<AgentLink> result = new ArrayList<AgentLink>();
        for (AgentLink link : getComplex().agentLinks) {
            if (this == link.sourceSite.agent || this == link.targetSite.agent) {
                result.add(link);
            }
        }
        return result;
    }

    public Complex getComplex() {
        return complex;
    }
    
    public void setComplex(Complex complex) {
        this.complex = complex;
    }

    public AgentLink getLink(String linkName) {
        for (AgentLink link : getLinks()) {
            if (linkName.equals(link.getSite(this).name)) {
                return link;
            }
        }
        return null;
    }
    
    @Override
    public Agent clone() {
        // AgentSites are cloned in agent constructor
        return new Agent(name, location, sites.values());
    }

    public String getStateHash() {
        return stateHash;
    }

    public String toString(String siteSuffix, boolean basicKappaOnly) {
        if (siteSuffix == null) {
            throw new NullPointerException();
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        
        if (location != NOT_LOCATED && siteSuffix.length() == 0) {
        	builder.append(':').append(location.toString());
        }
        
        if (orderedSiteNames.size() > 0 || siteSuffix.length() > 0) {
	        builder.append("(");
	
	        if (orderedSiteNames.size() > 0) {
	            builder.append(sites.get(orderedSiteNames.get(0)).toString(basicKappaOnly));
	            for (int index = 1; index < orderedSiteNames.size(); index++) {
	                builder.append(",").append(sites.get(orderedSiteNames.get(index)).toString(basicKappaOnly));
	            }
	        }
	        
	        if (siteSuffix.length() > 0) {
	            if (orderedSiteNames.size() > 0) {
	                builder.append(",");
	            }
	            builder.append(siteSuffix);
	        }
	        
	        builder.append(")");
        }
        return builder.toString();
    }

	public void setLocation(Location location) {
        if (location == null) {
            throw new NullPointerException();
        }
		this.location = location;
	}

    public List<Agent> getLocatedAgents(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        List<Agent> result = new ArrayList<Agent>();
        
        if (location == NOT_LOCATED) {
            if (compartments.size() == 0) {
                result.add(this.clone());
            }
            else {
                for (Compartment compartment : compartments) {
                    Location[] locations = compartment.getDistributedCellReferences();
                    for (Location currentLocation : locations) {
                        Agent locatedAgent = clone();
                        locatedAgent.location = currentLocation;
                        result.add(locatedAgent);
                    }
                }
            }
        }
        else {
            Compartment compartment = location.getReferencedCompartment(compartments);
            if (compartment == null) {
                throw new IllegalArgumentException("Unknown location: " + location);
            }
            if (location.getIndices().length == compartment.getDimensions().length) {
                result.add(this.clone());
            }
            else {
                Location[] locations = compartment.getDistributedCellReferences();
                for (Location currentLocation : locations) {
                    Agent locatedAgent = clone();
                    locatedAgent.location = currentLocation;
                    result.add(locatedAgent);
                }
            }
        }
        return result;
    }

}
