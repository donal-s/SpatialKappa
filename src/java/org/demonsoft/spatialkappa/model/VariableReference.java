package org.demonsoft.spatialkappa.model;

import java.io.Serializable;

public class VariableReference implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String variableName;
    
    public VariableReference(String variableName) {
        if (variableName == null) {
            throw new NullPointerException();
        }
        if (variableName.length() == 0) {
            throw new IllegalArgumentException(variableName);
        }
        this.variableName = variableName;
    }


    @Override
    public String toString() {
        return "'" + variableName + "'";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
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
        VariableReference other = (VariableReference) obj;
        if (variableName == null) {
            if (other.variableName != null)
                return false;
        }
        else if (!variableName.equals(other.variableName))
            return false;
        return true;
    }

    
}
