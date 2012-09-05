package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getList;

import java.util.ArrayList;
import java.util.List;

public class Channel {

    private final String name;
    private final List<ChannelComponent> channelComponents = new ArrayList<ChannelComponent>();

    // Constructor for unit tests
    public Channel(String name, Location sourceReference, Location targetReference) {
        this(name);
        if (sourceReference == null || targetReference == null) {
            throw new NullPointerException();
        }
        addChannelComponent(null, getList(sourceReference), getList(targetReference));
    }

    public Channel(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public void addChannelComponent(String channelType, List<Location> sourceLocations, List<Location> targetLocations) {
        channelComponents.add(ChannelComponent.createChannelComponent(channelType, sourceLocations, targetLocations));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(": ");
        if (channelComponents.size() == 1) {
            ChannelComponent component = channelComponents.get(0);
            builder.append(component);
        }
        else {
            boolean first = true;
            for (ChannelComponent component : channelComponents) {
                if (!first) {
                    builder.append(" + ");
                }
                builder.append("(").append(component).append(")");
                first = false;
            }
        }
        
        return builder.toString();
    }

    public List<ChannelConstraint> getCellReferencePairs(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        List<ChannelConstraint> result = new ArrayList<ChannelConstraint>();
        if (isValidLinkChannel()) {
            for (ChannelComponent component : channelComponents) {
                result.addAll(component.getCellReferencePairs(compartments));
            }
        }
        return result;
    }

    public final List<Location> applyChannel(Location sourceLocation, Location targetConstraint, List<Compartment> compartments) {
        List<List<Location>> multiResult = applyChannel(getList(new ChannelConstraint(sourceLocation, targetConstraint)), compartments);
        List<Location> result = new ArrayList<Location>();
        for (List<Location> current : multiResult) {
            result.add(current.get(0));
        }
        return result;
    }
    
    public List<List<Location>> applyChannel(List<ChannelConstraint> constraints, List<Compartment> compartments) {
        if (constraints == null || compartments == null) {
            throw new NullPointerException();
        }
        List<List<Location>> result = new ArrayList<List<Location>>();
        for (ChannelComponent component : channelComponents) {
            result.addAll(component.applyChannel(constraints, compartments));
        }
        return result;
    }

    public void validate(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        if (channelComponents.size() == 0) {
            throw new IllegalStateException("No location pairs defined");
        }
        for (ChannelComponent component : channelComponents) {
            component.validate(compartments);
        }
    }

    public boolean isValidLinkChannel() {
        for (ChannelComponent component : channelComponents) {
            if (component.templateConstraints.size() > 1) {
                return false;
            }
        }
        return true;
    }
 

}
