package org.demonsoft.spatialkappa.model;

import java.util.List;

public class PerturbationEffect {

    public static enum Type {ADD, REMOVE, SET, FIXED}

    public static final PerturbationEffect SNAPSHOT = new PerturbationEffect("$SNAPSHOT");
    public static final PerturbationEffect STOP = new PerturbationEffect("$STOP");

    public final String event;
    public final Type type;
    public final VariableExpression expression;
    public final String transitionName;
    public final List<Agent> agents;
    
    public PerturbationEffect(Type type, VariableExpression quantityExpression, List<Agent> agents) {
        if (type == null || quantityExpression == null || agents == null) {
            throw new NullPointerException();
        }
        if (type != Type.ADD && type != Type.REMOVE) {
            throw new IllegalArgumentException();
        }
        this.event = null;
        this.type = type;
        this.expression = quantityExpression;
        this.transitionName = null;
        this.agents = agents;
    }
    
    public PerturbationEffect(String transitionName, VariableExpression rateExpression) {
        if (transitionName == null || rateExpression == null) {
            throw new NullPointerException();
        }
        this.event = null;
        this.type = Type.SET;
        this.expression = rateExpression;
        this.transitionName = transitionName;
        this.agents = null;
    }
    
    private PerturbationEffect(String event) {
        this.event = event;
        this.type = Type.FIXED;
        this.expression = null;
        this.transitionName = null;
        this.agents = null;
    }

    public void apply(SimulationState state) {
        if (state == null) {
            throw new NullPointerException();
        }
        switch (type) {
        case ADD:
            // TODO - upgrade to allow adding of compartment deltas (eg different values per cell)
            state.addComplexInstances(agents, (int) expression.evaluate(state).value);
            break;
        case REMOVE:
            state.addComplexInstances(agents, - (int) expression.evaluate(state).value);
            break;
        case SET:
            state.setTransitionRate(transitionName, expression);
            break;
        case FIXED:
            if ("$STOP".equals(event)) {
                state.stop();
            }
            else if ("$SNAPSHOT".equals(event)) {
                state.snapshot();
            }
            break;

        default:
            throw new IllegalStateException();
        }
    }
    
    @Override
    public String toString() {
        switch (type) {
        case ADD:
            return "$ADD " + expression + " " + agents;
        case REMOVE:
            return "$DEL " + expression + " " + agents;
        case SET:
            return "'" + transitionName + "' := " + expression;
        case FIXED:
            return event;

        default:
            throw new IllegalStateException();
        }
    }
}
