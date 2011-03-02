package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompartmentLink {

    private final String name;
    private final Location sourceReference;
    private final Location targetReference;
    private final Direction direction;

    public CompartmentLink(String name, Location sourceReference, Location targetReference, Direction direction) {
        if (name == null || sourceReference == null || targetReference == null || direction == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.sourceReference = sourceReference;
        this.targetReference = targetReference;
        this.direction = direction;
    }

    public String getName() {
        return name;
    }

    public Location getSourceReference() {
        return sourceReference;
    }

    public Location getTargetReference() {
        return targetReference;
    }

    @Override
    public String toString() {
        return name + ": " + sourceReference + " " + direction + " " + targetReference;
    }

    public Location[][] getCellReferencePairs(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        Compartment sourceCompartment = sourceReference.getReferencedCompartment(compartments);
        Compartment targetCompartment = targetReference.getReferencedCompartment(compartments);
        if (sourceCompartment == null || targetCompartment == null) {
            throw new IllegalArgumentException();
        }

        if (sourceCompartment.getDimensions().length != sourceReference.getIndices().length
                || targetCompartment.getDimensions().length != targetReference.getIndices().length) {
            throw new IllegalArgumentException();
        }
        List<Location[]> result = new ArrayList<Location[]>();
        if (isConcreteLink()) {
            result.add(new Location[] { sourceReference, targetReference });
        }
        else {
            Object[][] variableRanges = getVariableRanges(sourceReference, sourceCompartment);
            Map<String, Integer> variables = new HashMap<String, Integer>();
            for (int index = 0; index < variableRanges.length; index++) {
                variables.put((String) variableRanges[index][0], 0);
            }
            getCellReferencePairs(result, variableRanges, sourceCompartment, targetCompartment, variables, 0);
        }
        return result.toArray(new Location[result.size()][]);
    }

    private Object[][] getVariableRanges(Location reference, Compartment compartment) {
        List<Object[]> result = new ArrayList<Object[]>();
        for (int index = 0; index < reference.getIndices().length; index++) {
            MathExpression expr = reference.getIndices()[index];
            if (!expr.isConcrete()) {
                result.add(new Object[] { expr.getVariable(), compartment.getDimensions()[index] });
            }
        }
        return result.toArray(new Object[result.size()][]);
    }

    private void getCellReferencePairs(List<Location[]> result, Object[][] variableRanges, Compartment sourceCompartment,
            Compartment targetCompartment, Map<String, Integer> variables, int variableIndex) {
        if (variableIndex >= variableRanges.length) {
            for (int index = 0; index < targetCompartment.getDimensions().length; index++) {
                int value = targetReference.getIndices()[index].evaluate(variables);
                if (value < 0 || value >= targetCompartment.getDimensions()[index]) {
                    return;
                }
            }
            result.add(new Location[] { sourceReference.getConcreteLocation(variables), targetReference.getConcreteLocation(variables) });
            return;
        }

        String currentSourceVariable = (String) variableRanges[variableIndex][0];
        int currentSourceMax = (Integer) variableRanges[variableIndex][1];

        for (int index = 0; index < currentSourceMax; index++) {
            variables.put(currentSourceVariable, index);
            getCellReferencePairs(result, variableRanges, sourceCompartment, targetCompartment, variables, variableIndex + 1);
        }
    }

    private boolean isConcreteLink() {
        return sourceReference.isConcreteLocation() && targetReference.isConcreteLocation();
    }

    public Direction getDirection() {
        return direction;
    }
}
