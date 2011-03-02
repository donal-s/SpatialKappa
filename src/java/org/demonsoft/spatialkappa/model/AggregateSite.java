package org.demonsoft.spatialkappa.model;

import java.util.HashSet;
import java.util.Set;

public class AggregateSite {
    
    private final String name;
    final Set<String> states = new HashSet<String>();
    final Set<String> links = new HashSet<String>();
    
    public AggregateSite(String name, String state, String link) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        if (state != null) {
            states.add(state);
        }
        if (link != null) {
            links.add(link);
        }
    }
    
    public AggregateSite(String name, String[] states, String[] links) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        if (states != null) {
            for (String current : states) {
                this.states.add(current);
            }
        }
        if (links != null) {
            for (String current : links) {
                this.links.add(current);
            }
        }
    }

    public AggregateSite(AgentSite site) {
        name = site.name;
        if (site.getState() != null) {
            states.add(site.getState());
        }
        if (site.getLinkName() != null) {
            links.add(site.getLinkName());
        }
    }

    public String getName() {
        return name;
    }
    
    public  Set<String> getLinks() {
        return links;
    }

    public void merge(AgentSite other) {
        if (other.getState() != null) {
            states.add(other.getState());
        }
        if (other.getLinkName() != null) {
            links.add(other.getLinkName());
        }
    }
    
    @Override
    public String toString() {
        return name + (states.size() > 0 ? "~" + states.toString() : "") + (links.size() > 0 ? "!" + links.toString() : "");
        
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((links == null) ? 0 : links.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((states == null) ? 0 : states.hashCode());
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
        AggregateSite other = (AggregateSite) obj;
        if (links == null) {
            if (other.links != null)
                return false;
        }
        else if (!links.equals(other.links))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        if (states == null) {
            if (other.states != null)
                return false;
        }
        else if (!states.equals(other.states))
            return false;
        return true;
    }
}
