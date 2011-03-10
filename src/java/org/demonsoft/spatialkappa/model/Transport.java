package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getFlatString;

import java.util.List;

public class Transport extends Transition {

    private final String compartmentLinkName;
    private final List<Agent> agents;
    
    public Transport(String label, String compartmentLinkName, List<Agent> agents, VariableExpression rate) {
        super(label, rate);
        if (compartmentLinkName == null || rate == null) {
            throw new NullPointerException();
        }
        this.compartmentLinkName = compartmentLinkName;
        this.agents = agents;
        if (agents != null) {
            sourceComplexes.addAll(Utils.getComplexes(agents));
        }
    }

    public Transport(String label, String compartmentLinkName, List<Agent> agents, float rate) {
        this(label, compartmentLinkName, agents, new VariableExpression(rate));
    }

    public String getCompartmentLinkName() {
        return compartmentLinkName;
    }

    public List<Agent> getAgents() {
        return agents;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (label != null) {
            builder.append(label).append(": ");
        }
        builder.append(compartmentLinkName).append(" ");
        if (agents != null && agents.size() > 0) {
            builder.append(getFlatString(agents)).append(" ");
        }
        builder.append("@ ").append(rate);
        return builder.toString();
    }

    @Override
    protected Transport clone() {
        return new Transport(label, compartmentLinkName, agents, rate);
    }
}
