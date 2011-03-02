package org.demonsoft.spatialkappa.model;

import java.util.Map;

public class ComplexMapping {

    public final Complex template;
    public final Complex target;
    public final Map<Agent, Agent> mapping;

    public ComplexMapping(Complex template, Complex target, Map<Agent, Agent> mapping) {
        this.template = template;
        this.target = target;
        this.mapping = mapping;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mapping == null) ? 0 : mapping.hashCode());
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        result = prime * result + ((template == null) ? 0 : template.hashCode());
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
        ComplexMapping other = (ComplexMapping) obj;
        if (mapping == null) {
            if (other.mapping != null)
                return false;
        }
        else if (!mapping.equals(other.mapping))
            return false;
        if (target == null) {
            if (other.target != null)
                return false;
        }
        else if (!target.equals(other.target))
            return false;
        if (template == null) {
            if (other.template != null)
                return false;
        }
        else if (!template.equals(other.template))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ComplexMapping:\n\tTemplate: ").append(template).append("\n\tTarget: ").append(target).append("\n\tMapping: ").append(mapping);
        return builder.toString();
    }
}
