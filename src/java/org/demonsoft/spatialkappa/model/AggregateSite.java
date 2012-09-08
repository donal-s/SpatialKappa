package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getFlatString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AggregateSite {
    
    private final String name;
    final List<String> states = new ArrayList<String>();
    final List<String> links = new ArrayList<String>();
    
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
    	this(name, (states == null) ? null : Arrays.asList(states), 
    			(links == null) ? null :Arrays.asList(links));
    }

    public AggregateSite(String name, List<String> states, List<String> links) {
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
    
    public  List<String> getLinks() {
        return links;
    }

    public  List<String> getStates() {
        return states;
    }

    public void merge(AgentSite other) {
        if (other.getState() != null) {
    		if (!states.contains(other.getState())) {
    			states.add(other.getState());
    		}
        }
        if (other.getLinkName() != null) {
    		if (!links.contains(other.getLinkName())) {
    			links.add(other.getLinkName());
    		}
        }
    }
    
    @Override
    public String toString() {
        return name + (states.size() > 0 ? "~" +  getFlatString("~", false, states.toArray()) : "") + (links.size() > 0 ? "!" + links.toString() : "");
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
