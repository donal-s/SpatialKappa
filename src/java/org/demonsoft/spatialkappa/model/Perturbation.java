package org.demonsoft.spatialkappa.model;

import org.demonsoft.spatialkappa.model.MathExpression.Operator;

public class Perturbation {

    public static enum Inequality {
        LESS_THAN {
            @Override
            boolean eval(float x, float y) {
                return x < y;
            }

            @Override
            public String toString() {
                return "<";
            }
        },
        GREATER_THAN {
            @Override
            boolean eval(float x, float y) {
                return x > y;
            }

            @Override
            public String toString() {
                return ">";
            }
        };

        abstract boolean eval(float x, float y);
    }
    
    private final Condition condition;
    private final float time;
    private final Assignment assignment;
    
    public Perturbation(String timeString, Assignment assignment) {
        if (timeString == null || assignment == null) {
            throw new NullPointerException();
        }
        this.time = Float.parseFloat(timeString);
        if (this.time < 0) {
            throw new IllegalArgumentException(timeString);
        }
        this.assignment = assignment;
        this.condition = null;
    }
    
    public Perturbation(Condition condition, Assignment assignment) {
        if (condition == null || assignment == null) {
            throw new NullPointerException();
        }
        this.assignment = assignment;
        this.condition = condition;
        this.time = 0f;
    }
    
    public float getTime() {
        return time;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public Condition getCondition() {
        return condition;
    }


    public boolean isConditionMet(SimulationState simulationState) {
        if (simulationState == null) {
            throw new NullPointerException();
        }
        if (condition != null) {
            return condition.isConditionMet(simulationState);
        }
        return simulationState.getTime() > time;
    }
    
    public void apply(SimulationState simulationState) {
        if (simulationState == null) {
            throw new NullPointerException();
        }
        assignment.apply(simulationState);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (condition == null) {
            builder.append("$T > ").append(time);
        }
        else {
            builder.append(condition);
        }
        builder.append(" do ").append(assignment);
        return builder.toString();
    }
    
    
    public static class Assignment {
        
        private String targetTransform;
        private ConcentrationExpression expression;

        public Assignment(String targetTransform, ConcentrationExpression expression) {
            if (targetTransform == null || expression == null) {
                throw new NullPointerException();
            }
            this.targetTransform = targetTransform;
            this.expression = expression;
        }
        
        public void apply(SimulationState simulationState) {
            if (simulationState == null) {
                throw new NullPointerException();
            }
            Transition transition = simulationState.getTransition(targetTransform);
            if (transition != null) {
                transition.setRate(new VariableExpression(expression.evaluate(simulationState)));
                simulationState.updateTransitionActivity(transition, true);
            }
        }
        
        @Override
        public String toString() {
            return "'" + targetTransform + "' := " + expression;
        }

        public String getTargetTransform() {
            return targetTransform;
        }

        public ConcentrationExpression getExpression() {
            return expression;
        }
    }
    
    
    public static class Condition {
        
        private final ConcentrationExpression lhs;
        private final ConcentrationExpression rhs;
        private final Inequality inequality;

        public Condition(ConcentrationExpression lhs, Inequality inequality, ConcentrationExpression rhs) {
            if (lhs == null || inequality == null || rhs == null) {
                throw new NullPointerException();
            }
            this.lhs = lhs;
            this.rhs = rhs;
            this.inequality = inequality;
        }
        
        public boolean isConditionMet(SimulationState simulationState) {
            if (simulationState == null) {
                throw new NullPointerException();
            }
            return inequality.eval(lhs.evaluate(simulationState), rhs.evaluate(simulationState));
        }
        
        @Override
        public String toString() {
            return lhs + " " + inequality + " " + rhs;
        }

    }
    
    public static class ConcentrationExpression {
        
        public static final ConcentrationExpression INFINITE_RATE = new ConcentrationExpression(Float.MAX_VALUE);
        
        private float value;
        private String label;
        private Operator operator;
        private ConcentrationExpression expression;

        public ConcentrationExpression(float value) {
            this.value = value;
        }
        
        public ConcentrationExpression(String label) {
            if (label == null) {
                throw new NullPointerException();
            }
            this.label = label;
        }
        
        public ConcentrationExpression(float value, String operatorString, ConcentrationExpression expression) {
            if (operatorString == null || expression == null) {
                throw new NullPointerException();
            }
            this.value = value;
            operator = Operator.getOperator(operatorString);
            this.expression = expression;
        }
        
        public ConcentrationExpression(String label, String operatorString, ConcentrationExpression expression) {
            if (label == null || operatorString == null || expression == null) {
                throw new NullPointerException();
            }
            this.label = label;
            operator = Operator.getOperator(operatorString);
            this.expression = expression;
        }
        
        public float evaluate(SimulationState simulationState) {
            if (simulationState == null) {
                throw new NullPointerException();
            }
            float result = 0;
            if (label == null) {
                result = value;
            }
            else {
                Transition transition = simulationState.getTransition(label);
                if (transition != null) {
                    result = transition.getRate().evaluate(simulationState).value; //TODO test infinite rate
                }
                else {
                    Variable variable = simulationState.getVariable(label);
                    if (variable != null) {
                        // TODO cache this              
                        ObservationElement element = simulationState.getComplexQuantity(variable);
                        if (element != null) {
                            result = element.value;
                        }
                    }
                }
            }
            if (operator != null) {
                result = operator.eval(result, expression.evaluate(simulationState));
            }
            return result;
        }

        public float getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public Operator getOperator() {
            return operator;
        }

        public ConcentrationExpression getExpression() {
            return expression;
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append((label != null) ? label : value);
            if (operator != null) {
                builder.append(" ").append(operator).append(" ").append(expression);
            }
            return builder.toString();
        }
    }

}
