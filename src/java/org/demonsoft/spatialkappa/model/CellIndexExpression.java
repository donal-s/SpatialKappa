package org.demonsoft.spatialkappa.model;

import java.util.HashMap;
import java.util.Map;


/**
 * A more restrictive form of VariableExpression for definining cell links.
 * Uses integer only operations.
 */
public class CellIndexExpression extends VariableExpression {

    private static final long serialVersionUID = 1L;

    private static final Map<String, Integer> NO_VARIABLES = new HashMap<String, Integer>();

    
    // Commonly used indices
    public static final CellIndexExpression WILDCARD = new CellIndexExpression("-1") {
        private static final long serialVersionUID = 1L;

        @Override
        public String toString() {
            return "?";
        }
    };
    
    public static CellIndexExpression INDEX_0 = new CellIndexExpression("0");
    public static CellIndexExpression INDEX_1 = new CellIndexExpression("1");
    public static CellIndexExpression INDEX_2 = new CellIndexExpression("2");
    public static CellIndexExpression INDEX_3 = new CellIndexExpression("3");
    public static CellIndexExpression INDEX_X = new CellIndexExpression(new VariableReference("x"));
    public static CellIndexExpression INDEX_X_PLUS_1 = 
            new CellIndexExpression(INDEX_X, Operator.PLUS, INDEX_1);
    public static CellIndexExpression INDEX_X_MINUS_1 = 
            new CellIndexExpression(INDEX_X, Operator.MINUS, INDEX_1);
    public static CellIndexExpression INDEX_Y = new CellIndexExpression(new VariableReference("y"));
    public static CellIndexExpression INDEX_Y_PLUS_1 = 
            new CellIndexExpression(INDEX_Y, Operator.PLUS, INDEX_1);
    public static CellIndexExpression INDEX_Y_MINUS_1 = 
            new CellIndexExpression(INDEX_Y, Operator.MINUS, INDEX_1);
    public static CellIndexExpression INDEX_Z = new CellIndexExpression(new VariableReference("z"));
    public static CellIndexExpression INDEX_Z_PLUS_1 = 
            new CellIndexExpression(INDEX_Z, Operator.PLUS, INDEX_1);
    public static CellIndexExpression INDEX_Z_MINUS_1 = 
            new CellIndexExpression(INDEX_Z, Operator.MINUS, INDEX_1);

    public CellIndexExpression(String input) {
        super(input);
    }

    public CellIndexExpression(VariableReference input) {
        super(input);
    }

    public CellIndexExpression(CellIndexExpression expr1, Operator operator, CellIndexExpression expr2) {
        super(expr1, operator, expr2);
    }

    @Override
    public String toString() {
        switch (type) {
        case BINARY_EXPRESSION:
            return "(" + lhsExpression + " " + operator + " " + rhsExpression + ")";
            
        case NUMBER:
            return "" + ((int) value);
            
        case VARIABLE_REFERENCE:
            return reference.variableName;
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }
    
    public CellIndexExpression getDeltaIndex(int delta) {
        if (delta == 0) {
            return this;
        }
        switch (type) {
        case BINARY_EXPRESSION:
        case VARIABLE_REFERENCE:
            Operator oper = (delta < 0) ? Operator.MINUS : Operator.PLUS;
            return new CellIndexExpression(this, oper, new CellIndexExpression("" + Math.abs(delta)));
            
        case NUMBER:
            return new CellIndexExpression("" + (((int) value) + delta));
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }

    // TODO - fixed values should be preevaluated on construction
    
    public int evaluateIndex() {
        return evaluateIndex(NO_VARIABLES);
    }

    public int evaluateIndex(Map<String, Integer> variables) {
        if (variables == null) {
            throw new NullPointerException();
        }
        
        switch (type) {
        case BINARY_EXPRESSION:
            return operator.eval(((CellIndexExpression) lhsExpression).evaluateIndex(variables), 
                    ((CellIndexExpression) rhsExpression).evaluateIndex(variables));
            
        case NUMBER:
            return (int) value;
            
        case VARIABLE_REFERENCE:
            if (!variables.containsKey(reference.variableName)) {
                throw new IllegalArgumentException("Missing value: " + reference.variableName);
            }
            return variables.get(reference.variableName);
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }

    public boolean isFixed() {
        switch (type) {
        case BINARY_EXPRESSION:
            return ((CellIndexExpression) lhsExpression).isFixed() && ((CellIndexExpression) rhsExpression).isFixed();
            
        case NUMBER:
            return true;
            
        case VARIABLE_REFERENCE:
            return false;
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }
    
}
