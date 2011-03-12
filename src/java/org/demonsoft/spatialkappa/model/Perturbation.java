package org.demonsoft.spatialkappa.model;


public class Perturbation {

    public final BooleanExpression condition;
    public final PerturbationEffect effect;
    public final BooleanExpression untilCondition;
    
    public Perturbation(BooleanExpression condition, PerturbationEffect effect, BooleanExpression untilCondition) {
        if (condition == null || effect == null) {
            throw new NullPointerException();
        }
        this.condition = condition;
        this.effect = effect;
        this.untilCondition = untilCondition;
    }

    public boolean isConditionMet(SimulationState simulationState) {
        if (simulationState == null) {
            throw new NullPointerException();
        }
        if (condition.evaluate(simulationState)) {
            return untilCondition == null || !untilCondition.evaluate(simulationState);
        }
        return false;
    }
    
    public void apply(SimulationState simulationState) {
        if (simulationState == null) {
            throw new NullPointerException();
        }
        effect.apply(simulationState);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(condition).append(" do ").append(effect);
        if (untilCondition != null) {
            builder.append(" until ").append(untilCondition);
        }
        return builder.toString();
    }

}
