package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.VariableExpression.Type;

public class Channel {

    private final String name;
    private final List<Location[]> locations = new ArrayList<Location[]>();

    public Channel(String name, Location sourceReference, Location targetReference) {
        if (name == null || sourceReference == null || targetReference == null) {
            throw new NullPointerException();
        }
        this.name = name;
        locations.add(new Location[] {sourceReference, targetReference});
    }

    public Channel(String name, List<Location[]> locations) {
        if (name == null || locations == null) {
            throw new NullPointerException();
        }
        if (locations.size() == 0) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.locations.addAll(locations);
    }

    public String getName() {
        return name;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(": ");
        if (locations.size() == 1) {
            builder.append(locations.get(0)[0]).append(" -> ").append(locations.get(0)[1]);
        }
        else {
            boolean first = true;
            for (Location[] locationPair : locations) {
                if (!first) {
                    builder.append(" + ");
                }
                builder.append("(").append(locationPair[0]).append(" -> ").append(locationPair[1]).append(")");
                first = false;
            }
        }
        
        return builder.toString();
    }

    public Location[][] getCellReferencePairs(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        List<Location[]> result = new ArrayList<Location[]>();
        for (Location[] locationPair : locations) {
            Location source = locationPair[0];
            Location target = locationPair[1];

            Compartment sourceCompartment = source.getReferencedCompartment(compartments);
            Compartment targetCompartment = target.getReferencedCompartment(compartments);
            if (sourceCompartment == null || targetCompartment == null) {
                throw new IllegalArgumentException();
            }
    
            if (sourceCompartment.getDimensions().length != source.getIndices().length
                    || targetCompartment.getDimensions().length != target.getIndices().length) {
                throw new IllegalArgumentException();
            }
            if (isConcreteLink(source, target)) {
                result.add(new Location[] { source, target });
            }
            else {
                Object[][] variableRanges = getVariableRanges(source, sourceCompartment);
                Map<String, Integer> variables = new HashMap<String, Integer>();
                for (int index = 0; index < variableRanges.length; index++) {
                    variables.put((String) variableRanges[index][0], 0);
                }
                getCellReferencePairs(result, variableRanges, source, target, sourceCompartment, targetCompartment, variables, 0);
            }
        }
        return result.toArray(new Location[result.size()][]);
    }

    private Object[][] getVariableRanges(Location reference, Compartment compartment) {
        List<Object[]> result = new ArrayList<Object[]>();
        for (int index = 0; index < reference.getIndices().length; index++) {
            CellIndexExpression expr = reference.getIndices()[index];
            if (!expr.isFixed()) {
                result.add(new Object[] { expr.reference.variableName, compartment.getDimensions()[index] });
            }
        }
        return result.toArray(new Object[result.size()][]);
    }

    private void getCellReferencePairs(List<Location[]> result, Object[][] variableRanges, Location source, Location target, 
            Compartment sourceCompartment, Compartment targetCompartment, Map<String, Integer> variables, int variableIndex) {
        if (variableIndex >= variableRanges.length) {
            for (int index = 0; index < targetCompartment.getDimensions().length; index++) {
                int value = target.getIndices()[index].evaluateIndex(variables);
                if (value < 0 || value >= targetCompartment.getDimensions()[index]) {
                    return;
                }
            }
            result.add(new Location[] { source.getConcreteLocation(variables), target.getConcreteLocation(variables) });
            return;
        }

        String currentSourceVariable = (String) variableRanges[variableIndex][0];
        int currentSourceMax = (Integer) variableRanges[variableIndex][1];

        for (int index = 0; index < currentSourceMax; index++) {
            variables.put(currentSourceVariable, index);
            getCellReferencePairs(result, variableRanges, source, target, sourceCompartment, targetCompartment, variables, variableIndex + 1);
        }
    }

    private boolean isConcreteLink(Location source, Location target) {
        return source.isConcreteLocation() && target.isConcreteLocation();
    }

    public List<Location> applyChannel(Location location, List<Compartment> compartments) {
        if (location == null || compartments == null) {
            throw new NullPointerException();
        }
        List<Location> result = new ArrayList<Location>();
        
        boolean valid = false;
        for (Location[] locationPair : locations) {
            Location source = locationPair[0];
            Compartment compartment = source.getReferencedCompartment(compartments);
            if (compartment == null) {
                throw new IllegalArgumentException("Unknown compartment: " + source.getName());
            }
            if (location.getIndices().length != source.getIndices().length 
                    || !location.getName().equals(source.getName())) {
                continue;
            }
            boolean validIndices = true;
            for (int index = 0; index < source.getIndices().length; index++) {
                CellIndexExpression sourceIndex = source.getIndices()[index];
                CellIndexExpression inputIndex = location.getIndices()[index];
                if (!inputIndex.isFixed()) {
                    validIndices = false;
                    break;
                }
                if (sourceIndex.isFixed()) {
                    if (!sourceIndex.equals(inputIndex)) {
                        validIndices = false;
                        break;
                    }
                }
            }
            valid = validIndices;
            if (valid) {
                break;
            }
        }

        for (Location[] locationPair : locations) {
            Location source = locationPair[0];
            Location target = locationPair[1];
        
            Compartment sourceCompartment = source.getReferencedCompartment(compartments);
            if (sourceCompartment == null) {
                throw new IllegalArgumentException("Unknown compartment: " + source.getName());
            }
            Compartment targetCompartment = target.getReferencedCompartment(compartments);
            if (targetCompartment == null) {
                throw new IllegalArgumentException("Unknown compartment: " + target.getName());
            }

            if (location.getIndices().length != source.getIndices().length 
                    || !location.getName().equals(source.getName())) {
                continue;
            }
            boolean validIndices = true;
            for (int index = 0; index < source.getIndices().length; index++) {
                CellIndexExpression sourceIndex = source.getIndices()[index];
                CellIndexExpression inputIndex = location.getIndices()[index];
                if (!inputIndex.isFixed()) {
                    validIndices = false;
                    break;
                }
                if (sourceIndex.isFixed()) {
                    if (!sourceIndex.equals(inputIndex)) {
                        validIndices = false;
                        break;
                    }
                }
            }
            if (!validIndices) {
                continue;
            }

            Map<String, Integer> variables = new HashMap<String, Integer>();
            for (int index = 0; index < source.getIndices().length; index++) {
                CellIndexExpression sourceIndex = source.getIndices()[index];
                if (sourceIndex.type == Type.VARIABLE_REFERENCE) {
                    CellIndexExpression inputIndex = location.getIndices()[index];
                    variables.put(sourceIndex.reference.variableName, (int) inputIndex.value);
                }
            }

            valid = true;
            
            List<CellIndexExpression> targetIndices = new ArrayList<CellIndexExpression>();
            for (int index = 0; index < target.getIndices().length; index++) {
                CellIndexExpression targetIndex = target.getIndices()[index];
                int targetIndexValue = targetIndex.evaluateIndex(variables);
                if (targetIndexValue < 0 || targetIndexValue >= targetCompartment.getDimensions()[index]) {
                    valid = false;
                    break;
                }
                targetIndices.add(new CellIndexExpression("" + targetIndexValue));
            }
            
            if (valid) {
                Location targetLocation = new Location(target.getName(), targetIndices);
                result.add(targetLocation);
            }
        }
        return result;
    }

    public void validate(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        for (Location[] locationPair : locations) {
            boolean found = false;
            
            for (Compartment compartment : compartments) {
                if (locationPair[0].getName().equals(compartment.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException("Compartment '" + locationPair[0].getName() + "' not found");
            }
            
            found = false;
            for (Compartment compartment : compartments) {
                if (locationPair[1].getName().equals(compartment.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException("Compartment '" + locationPair[1].getName() + "' not found");
            }
        }
    }
}
