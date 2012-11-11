package org.demonsoft.spatialkappa.model;

import java.util.List;


public class Perturbation {

    public final BooleanExpression condition;
    public final List<PerturbationEffect> effects;
    public final BooleanExpression untilCondition;
    
    public Perturbation(BooleanExpression condition, List<PerturbationEffect> effects, BooleanExpression untilCondition) {
        if (condition == null || effects == null) {
            throw new NullPointerException();
        }
        if (effects.size() == 0) {
            throw new IllegalArgumentException("No effects supplied");
        }
        this.condition = condition;
        this.effects = effects;
        this.untilCondition = untilCondition;
    }

    public boolean isConditionMet(SimulationState simulationState) {
        if (simulationState == null) {
            throw new NullPointerException();
        }
        return condition.evaluate(simulationState);
    }
    
    public boolean isUntilConditionMet(SimulationState simulationState) {
        if (simulationState == null) {
            throw new NullPointerException();
        }
        return untilCondition == null || untilCondition.evaluate(simulationState);
    }
    
    public void apply(SimulationState simulationState) {
        if (simulationState == null) {
            throw new NullPointerException();
        }
        for (PerturbationEffect effect : effects) {
            effect.apply(simulationState);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(condition).append(" do ").append(effects);
        if (untilCondition != null) {
            builder.append(" until ").append(untilCondition);
        }
        return builder.toString();
    }

}
