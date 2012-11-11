package org.demonsoft.spatialkappa.model;


public class BooleanExpression {

    public static enum Operator {
        AND("&&") {
            @Override
            boolean eval(boolean x, boolean y) {
                return x && y;
            }
        },
        OR("||") {
            @Override
            boolean eval(boolean x, boolean y) {
                return x || y;
            }
        },
        NOT("[not]") {
            @Override
            boolean eval(boolean x, boolean y) {
                return !x;
            }
        };

        private final String text;
        
        Operator(String text) {
            this.text = text;
        }
        
        @Override
        public String toString() {
            return text;
        }

        abstract boolean eval(boolean x, boolean y);
    }

    public static enum RelationalOperator {
        GREATER(">") {
            @Override
            boolean eval(ObservationElement x, ObservationElement y) {
                return x.value > y.value;
            }
        },
        EQUAL("=") {
            @Override
            boolean eval(ObservationElement x, ObservationElement y) {
                return x.value == y.value;
            }
        },
        NOT_EQUAL("<>") {
            @Override
            boolean eval(ObservationElement x, ObservationElement y) {
                return x.value != y.value;
            }
        },
        LESS("<") {
            @Override
            boolean eval(ObservationElement x, ObservationElement y) {
                return x.value < y.value;
            }
        };

        private final String text;
        
        RelationalOperator(String text) {
            this.text = text;
        }
        
        @Override
        public String toString() {
            return text;
        }
        
        abstract boolean eval(ObservationElement x, ObservationElement y);

        public static RelationalOperator getOperator(String input) {
            for (RelationalOperator current : RelationalOperator.values()) {
                if (current.text.equals(input)) {
                    return current;
                }
            }
            throw new IllegalArgumentException(input);
        }
    }
    
    public static enum Type {
        VALUE, VARIABLE_RELATION, BOOLEAN_RELATION, NEGATION
    }

    final Type type;
    private boolean value;
    private Operator booleanOperator;
    private RelationalOperator relationalOperator;
    private VariableExpression lhsVariableExpression;
    private VariableExpression rhsVariableExpression;
    private BooleanExpression lhsBooleanExpression;
    private BooleanExpression rhsBooleanExpression;

    public BooleanExpression(boolean value) {
        type = Type.VALUE;
        this.value = value;
    }

    public BooleanExpression(RelationalOperator operator, VariableExpression expr1, VariableExpression expr2) {
        if (operator == null || expr1 == null || expr2 == null) {
            throw new NullPointerException();
        }
        type = Type.VARIABLE_RELATION;
        relationalOperator = operator;
        lhsVariableExpression = expr1;
        rhsVariableExpression = expr2;
    }

    public BooleanExpression(Operator not, BooleanExpression expr) {
        if (not == null || expr == null) {
            throw new NullPointerException();
        }
        if (not != Operator.NOT) {
            throw new IllegalArgumentException();
        }
        type = Type.NEGATION;
        booleanOperator = Operator.NOT;
        lhsBooleanExpression = expr;
    }

    public BooleanExpression(Operator operator, BooleanExpression expr1, BooleanExpression expr2) {
        if (operator == null || expr1 == null || expr2 == null) {
            throw new NullPointerException();
        }
        if (operator == Operator.NOT) {
            throw new IllegalArgumentException();
        }
        type = Type.BOOLEAN_RELATION;
        booleanOperator = operator;
        lhsBooleanExpression = expr1;
        rhsBooleanExpression = expr2;
    }

    public boolean evaluate(SimulationState state) {
        if (state == null) {
            throw new NullPointerException();
        }
        switch (type) {
        case VALUE:
            return value;
            
        case NEGATION:
            return !lhsBooleanExpression.evaluate(state);
            
        case BOOLEAN_RELATION:
            return booleanOperator.eval(lhsBooleanExpression.evaluate(state), rhsBooleanExpression.evaluate(state));
            
        case VARIABLE_RELATION:
            return relationalOperator.eval(lhsVariableExpression.evaluate(state), rhsVariableExpression.evaluate(state));
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }
    
    @Override
    public String toString() {
        switch (type) {
        case VALUE:
            return value ? "[true]" : "[false]";
        case NEGATION:
            return booleanOperator + " " + lhsBooleanExpression;
        case BOOLEAN_RELATION:
            return "(" + lhsBooleanExpression + " " + booleanOperator + " " + rhsBooleanExpression + ")";
        case VARIABLE_RELATION:
            return "(" + lhsVariableExpression + " " + relationalOperator + " " + rhsVariableExpression + ")";

        default:
            throw new IllegalStateException();
        }
    }
}
