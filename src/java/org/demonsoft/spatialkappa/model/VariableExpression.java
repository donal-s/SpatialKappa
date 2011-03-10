package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.Map;

import org.demonsoft.spatialkappa.model.Variable.Type;

public class VariableExpression implements Serializable {

    private static final long serialVersionUID = 1L;

    public static enum Operator {
        PLUS {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.plus(y);
            }

            @Override
            public String toString() {
                return "+";
            }
        },
        MINUS {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.minus(y);
            }

            @Override
            public String toString() {
                return "-";
            }
        },
        MULTIPLY {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.times(y);
            }

            @Override
            public String toString() {
                return "*";
            }
        },
        DIVIDE {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.dividedBy(y);
            }

            @Override
            public String toString() {
                return "/";
            }
        },
        MODULUS {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.modulus(y);
            }

            @Override
            public String toString() {
                return "%";
            }
        };

        abstract ObservationElement eval(ObservationElement x, ObservationElement y);
        
        public static Operator getOperator(String input) {
            if ("+".equals(input)) {
                return PLUS;
            }
            if ("-".equals(input)) {
                return MINUS;
            }
            if ("*".equals(input)) {
                return MULTIPLY;
            }
            if ("/".equals(input)) {
                return DIVIDE;
            }
            if ("%".equals(input)) {
                return MODULUS;
            }
            throw new IllegalArgumentException(input);
        }
    }

    public static enum Constant {
        INFINITY {
            @Override
            public String toString() {
                return "[inf]";
            }
        },
    }

    public static enum SimulationToken {
        EVENTS {
            @Override
            public String toString() {
                return "[E]";
            }
        },
        TIME {
            @Override
            public String toString() {
                return "[T]";
            }
        },
    }

    private final VariableReference reference;
    private final float value;
    private final Operator operator;
    private final VariableExpression lhsExpression;
    private final VariableExpression rhsExpression;
    private final SimulationToken simulationToken;
    private final boolean infinite;
    
    public VariableExpression(String input) {
        if (input == null) {
            throw new NullPointerException();
        }
        if (input.length() == 0) {
            throw new IllegalArgumentException(input);
        }
        reference = null;
        value = Float.parseFloat(input);
        operator = null;
        lhsExpression = null;
        rhsExpression = null;
        simulationToken = null;
        infinite = false;
    }

    public VariableExpression(float input) {
        reference = null;
        value = input;
        operator = null;
        lhsExpression = null;
        rhsExpression = null;
        simulationToken = null;
        infinite = false;
    }

    public VariableExpression(VariableExpression expr1, Operator operator, VariableExpression expr2) {
        if (expr1 == null || operator == null || expr2 == null) {
            throw new NullPointerException();
        }
        reference = null;
        value = 0;
        this.operator = operator;
        lhsExpression = expr1;
        rhsExpression = expr2;
        simulationToken = null;
        infinite = false;
    }

    public VariableExpression(VariableReference reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        this.reference = reference;
        value = 0;
        operator = null;
        lhsExpression = null;
        rhsExpression = null;
        simulationToken = null;
        infinite = false;
    }

    public VariableExpression(Constant constant) {
        if (constant == null) {
            throw new NullPointerException();
        }
        reference = null;
        value = 0;
        operator = null;
        lhsExpression = null;
        rhsExpression = null;
        simulationToken = null;
        infinite = Constant.INFINITY == constant;
    }

    public VariableExpression(SimulationToken token) {
        if (token == null) {
            throw new NullPointerException();
        }
        reference = null;
        value = 0;
        operator = null;
        lhsExpression = null;
        rhsExpression = null;
        simulationToken = token;
        infinite = false;
    }

    public Operator getOperator() {
        return operator;
    }

    public VariableReference getReference() {
        return reference;
    }

    public float getValue() {
        return value;
    }

    public VariableExpression getLhsExpression() {
        return lhsExpression;
    }

    public VariableExpression getRhsExpression() {
        return rhsExpression;
    }

    @Override
    public String toString() {
        if (infinite) {
            return Constant.INFINITY.toString();
        }
        if (simulationToken != null) {
            return simulationToken.toString();
        }
        if (reference != null) {
            return reference.toString();
        }
        if (operator != null) {
            return "(" + lhsExpression + " " + operator + " " + rhsExpression + ")";
        }
        return "" + value;
    }


    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (infinite ? 1231 : 1237);
        result = prime * result + ((lhsExpression == null) ? 0 : lhsExpression.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        result = prime * result + ((reference == null) ? 0 : reference.hashCode());
        result = prime * result + ((rhsExpression == null) ? 0 : rhsExpression.hashCode());
        result = prime * result + ((simulationToken == null) ? 0 : simulationToken.hashCode());
        result = prime * result + Float.floatToIntBits(value);
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
        VariableExpression other = (VariableExpression) obj;
        if (infinite != other.infinite)
            return false;
        if (lhsExpression == null) {
            if (other.lhsExpression != null)
                return false;
        }
        else if (!lhsExpression.equals(other.lhsExpression))
            return false;
        if (operator != other.operator)
            return false;
        if (reference == null) {
            if (other.reference != null)
                return false;
        }
        else if (!reference.equals(other.reference))
            return false;
        if (rhsExpression == null) {
            if (other.rhsExpression != null)
                return false;
        }
        else if (!rhsExpression.equals(other.rhsExpression))
            return false;
        if (simulationToken != other.simulationToken)
            return false;
        if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
            return false;
        return true;
    }

    public ObservationElement evaluate(SimulationState state) {
        if (state == null) {
            throw new NullPointerException();
        }
        if (infinite) {
            throw new IllegalStateException();
        }
        if (reference != null) {
            Variable target = state.getVariable(reference.variableName);
            if (target == null) {
                throw new IllegalArgumentException("Missing value: " + reference);
            }
            return target.evaluate(state);
        }
        if (operator != null) {
            return operator.eval(lhsExpression.evaluate(state), rhsExpression.evaluate(state));
        }
        return new ObservationElement(value);
    }

    public boolean isInfinite(Map<String, Variable> variables) {
        if (variables == null) {
            throw new NullPointerException();
        }
        if (reference != null) {
            if (!variables.containsKey(reference.variableName)) {
                throw new IllegalArgumentException("Missing value: " + reference);
            }
            Variable target = variables.get(reference.variableName);
            return target.type == Type.VARIABLE_EXPRESSION && target.expression.isInfinite(variables);
        }
        
        return infinite || 
                (operator != null && (lhsExpression.isInfinite(variables) || rhsExpression.isInfinite(variables)));
    }


    public boolean isFixed(Map<String, Variable> variables) {
        if (variables == null) {
            throw new NullPointerException();
        }
        if (reference != null) {
            if (!variables.containsKey(reference.variableName)) {
                throw new IllegalArgumentException("Missing value: " + reference);
            }
            return variables.get(reference.variableName).expression.isFixed(variables);
        }
        
        return infinite || (simulationToken == null && 
                (operator == null || (lhsExpression.isFixed(variables) && rhsExpression.isFixed(variables))));
    }
    
}
