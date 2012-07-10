package org.demonsoft.spatialkappa.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransitionInstance {

    public final List<ComplexMapping> sourceMapping;
    public final int targetLocationCount;
    public final Map<Complex, Integer> requiredComplexCounts = new HashMap<Complex, Integer>();
    
    public TransitionInstance(List<ComplexMapping> sourceMapping, int targetLocationCount) {
        this.sourceMapping = sourceMapping;
        this.targetLocationCount = targetLocationCount;
        
        for (ComplexMapping mapping : sourceMapping) {
            if (requiredComplexCounts.containsKey(mapping.target)) {
                requiredComplexCounts.put(mapping.target, requiredComplexCounts.get(mapping.target) + 1);
            }
            else {
                requiredComplexCounts.put(mapping.target, 1);
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceMapping == null) ? 0 : sourceMapping.hashCode());
        result = prime * result + targetLocationCount;
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
        TransitionInstance other = (TransitionInstance) obj;
        if (sourceMapping == null) {
            if (other.sourceMapping != null)
                return false;
        }
        else if (!sourceMapping.equals(other.sourceMapping))
            return false;
        if (targetLocationCount != other.targetLocationCount)
            return false;
        return true;
    }
    
}
