package org.demonsoft.spatialkappa.model;

import java.util.Map;


/**
 * A more restrictive form of VariableExpression for definining cell links.
 * Uses integer only operations.
 */
public class CellIndexExpression extends VariableExpression {

    private static final long serialVersionUID = 1L;

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
            return reference.toString();
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
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
