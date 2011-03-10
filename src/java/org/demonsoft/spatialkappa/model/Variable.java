package org.demonsoft.spatialkappa.model;

public class Variable {

    public enum Type { VARIABLE_EXPRESSION, KAPPA_EXPRESSION, TRANSITION_LABEL }
    
    public final String label;
    public final VariableExpression expression;
    public final Location location;
    public final Complex complex;
    public final Type type;

    public Variable(VariableExpression expression, String label) {
        if (expression == null || label == null) {
            throw new NullPointerException();
        }
        this.expression = expression;
        this.label = label;
        this.location = null;
        this.complex = null;
        this.type = Type.VARIABLE_EXPRESSION;
    }
    
    public Variable(Complex complex, Location location, String label) {
        if (complex == null || label == null) {
            throw new NullPointerException();
        }
        this.expression = null;
        this.label = label;
        this.location = location;
        this.complex = complex;
        this.type = Type.KAPPA_EXPRESSION;
    }
    
    public Variable(String label) {
        if (label == null) {
            throw new NullPointerException();
        }
        this.expression = null;
        this.label = label;
        this.location = null;
        this.complex = null;
        this.type = Type.TRANSITION_LABEL;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((complex == null) ? 0 : complex.hashCode());
        result = prime * result + ((expression == null) ? 0 : expression.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        Variable other = (Variable) obj;
        if (complex == null) {
            if (other.complex != null)
                return false;
        }
        else if (!complex.equals(other.complex))
            return false;
        if (expression == null) {
            if (other.expression != null)
                return false;
        }
        else if (!expression.equals(other.expression))
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        }
        else if (!label.equals(other.label))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        }
        else if (!location.equals(other.location))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        switch (type) {
        case VARIABLE_EXPRESSION:
            return "'" + label + "' (" + expression + ")";
            
        case KAPPA_EXPRESSION:
            if (location == null) {
                return "'" + label + "' (" + complex + ")";
            }
            return "'" + label + "' " + location + " (" + complex + ")";
            
        default:
            return "'" + label + "'";
        }
    }

    public ObservationElement evaluate(SimulationState state) {
        switch (type) {
        case VARIABLE_EXPRESSION:
            return expression.evaluate(state);
            
        case KAPPA_EXPRESSION:
            return state.getComplexQuantity(this);
            
        default:
            return null;
        }
    }

}
