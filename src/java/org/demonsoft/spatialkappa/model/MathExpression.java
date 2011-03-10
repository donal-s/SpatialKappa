package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.Map;

public class MathExpression implements Serializable {

    private static final long serialVersionUID = 1L;

    public static enum Operator {
        PLUS {
            @Override
            float eval(float x, float y) {
                return x + y;
            }

            @Override
            int eval(int x, int y) {
                return x + y;
            }

            @Override
            public String toString() {
                return "+";
            }
        },
        MINUS {
            @Override
            float eval(float x, float y) {
                return x - y;
            }

            @Override
            int eval(int x, int y) {
                return x - y;
            }

            @Override
            public String toString() {
                return "-";
            }
        },
        MULTIPLY {
            @Override
            float eval(float x, float y) {
                return x * y;
            }

            @Override
            int eval(int x, int y) {
                return x * y;
            }

            @Override
            public String toString() {
                return "*";
            }
        },
        DIVIDE {
            @Override
            float eval(float x, float y) {
                return x / y;
            }

            @Override
            int eval(int x, int y) {
                return x / y;
            }

            @Override
            public String toString() {
                return "/";
            }
        },
        MODULUS {
            @Override
            float eval(float x, float y) {
                return x % y;
            }

            @Override
            int eval(int x, int y) {
                return x % y;
            }

            @Override
            public String toString() {
                return "%";
            }
        },
        EXPONENT {
            @Override
            float eval(float x, float y) {
                return (float) Math.pow(x, y);
            }

            @Override
            int eval(int x, int y) {
                return (int) Math.pow(x, y);
            }

            @Override
            public String toString() {
                return "^";
            }
        };

        abstract float eval(float x, float y);
        
        abstract int eval(int x, int y);
        
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
            if ("^".equals(input)) {
                return EXPONENT;
            }
            throw new IllegalArgumentException(input);
        }
    }

    private final String variable;
    private final int value;
    private final Operator operator;
    private final MathExpression lhsExpression;
    private final MathExpression rhsExpression;
    
    public MathExpression(String input) {
        if (input == null) {
            throw new NullPointerException();
        }
        if (input.length() == 0) {
            throw new IllegalArgumentException(input);
        }
        if (Character.isDigit(input.charAt(0))) {
            variable = null;
            value = Integer.parseInt(input);
        }
        else {
            variable = input;
            value = 0;
        }
        operator = null;
        lhsExpression = null;
        rhsExpression = null;
    }

    public MathExpression(MathExpression expr1, Operator operator, MathExpression expr2) {
        if (expr1 == null || operator == null || expr2 == null) {
            throw new NullPointerException();
        }
        variable = null;
        value = 0;
        this.operator = operator;
        lhsExpression = expr1;
        rhsExpression = expr2;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getVariable() {
        return variable;
    }

    public int getValue() {
        return value;
    }

    public MathExpression getLhsExpression() {
        return lhsExpression;
    }

    public MathExpression getRhsExpression() {
        return rhsExpression;
    }

    @Override
    public String toString() {
        if (variable != null) {
            return "'" + variable + "'";
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
        result = prime * result + ((lhsExpression == null) ? 0 : lhsExpression.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        result = prime * result + ((rhsExpression == null) ? 0 : rhsExpression.hashCode());
        result = prime * result + value;
        result = prime * result + ((variable == null) ? 0 : variable.hashCode());
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
        MathExpression other = (MathExpression) obj;
        if (lhsExpression == null) {
            if (other.lhsExpression != null)
                return false;
        }
        else if (!lhsExpression.equals(other.lhsExpression))
            return false;
        if (operator == null) {
            if (other.operator != null)
                return false;
        }
        else if (!operator.equals(other.operator))
            return false;
        if (rhsExpression == null) {
            if (other.rhsExpression != null)
                return false;
        }
        else if (!rhsExpression.equals(other.rhsExpression))
            return false;
        if (value != other.value)
            return false;
        if (variable == null) {
            if (other.variable != null)
                return false;
        }
        else if (!variable.equals(other.variable))
            return false;
        return true;
    }
    
    public int evaluate(Map<String, Integer> variables) {
        if (variables == null) {
            throw new NullPointerException();
        }
        if (variable != null) {
            if (!variables.containsKey(variable)) {
                throw new IllegalArgumentException("Missing value: " + variable);
            }
            return variables.get(variable);
        }
        if (operator != null) {
            return operator.eval(lhsExpression.evaluate(variables), rhsExpression.evaluate(variables));
        }
        return value;
    }

    public boolean isConcrete() {
        return (variable == null && operator == null) || (operator != null && lhsExpression.isConcrete() && rhsExpression.isConcrete());
    }
    
}
