package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.VariableExpression.Type;

public class Channel {

    private final String name;
    private final List<List<Location>[]> templateLocationPairs = new ArrayList<List<Location>[]>();

    // Constructor for unit tests
    public Channel(String name, Location sourceReference, Location targetReference) {
        this(name);
        if (sourceReference == null || targetReference == null) {
            throw new NullPointerException();
        }
        addLocationPair(getList(sourceReference), getList(targetReference));
    }

    public Channel(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public void addLocationPair(List<Location> sourceLocations, List<Location> targetLocations) {
        if (sourceLocations == null || targetLocations == null) {
            throw new NullPointerException();
        }
        if (sourceLocations.size() == 0 || sourceLocations.size() != targetLocations.size()) {
            throw new IllegalArgumentException();
        }
        templateLocationPairs.add(new List[] {sourceLocations, targetLocations});
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(": ");
        if (templateLocationPairs.size() == 1) {
            builder.append(templateLocationPairs.get(0)[0]).append(" -> ").append(templateLocationPairs.get(0)[1]);
        }
        else {
            boolean first = true;
            for (List<Location>[] locationPair : templateLocationPairs) {
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
        if (isValidLinkChannel()) {
            for (List<Location>[] locationPair : templateLocationPairs) {
                Location source = locationPair[0].get(0);
                Location target = locationPair[1].get(0);
    
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

    public List<Location> applyChannel(Location sourceLocation, Location targetConstraint, List<Compartment> compartments) {
        List<List<Location>> multiResult = applyChannel(getList(sourceLocation), getList(targetConstraint), compartments);
        List<Location> result = new ArrayList<Location>();
        for (List<Location> current : multiResult) {
            result.add(current.get(0));
        }
        return result;
    }
    
    public List<List<Location>> applyChannel(List<Location> sourceLocations, List<Location> targetConstraints, List<Compartment> compartments) {
        if (sourceLocations == null || compartments == null) {
            throw new NullPointerException();
        }
        if (sourceLocations.size() != targetConstraints.size()) {
            throw new IllegalArgumentException("Source locations and target constraints should have same size");
        }
        List<List<Location>> result = new ArrayList<List<Location>>();
        
            
        for (List<Location>[] locationPair : templateLocationPairs) {
            List<List<Location>> templateSourcePermutations = new ArrayList<List<Location>>();
            List<List<Location>> templateTargetPermutations = new ArrayList<List<Location>>();
            
            permuteLocations(locationPair[0], locationPair[1], templateSourcePermutations, templateTargetPermutations);
            
            for (int permIndex=0; permIndex<templateSourcePermutations.size(); permIndex++) {
                List<Location> templateSourcePermutation = templateSourcePermutations.get(permIndex);
                List<Location> templateTargetPermutation = templateTargetPermutations.get(permIndex);
                    

                if (isValidSourceLocations(templateSourcePermutation, sourceLocations, compartments)) {

                    List<Location> targetLocations = new ArrayList<Location>();
                    boolean valid = true;
                    
                    for (int templateIndex=0; templateIndex<templateSourcePermutation.size() && valid; templateIndex++) {
                        Location templateSourceLocation = templateSourcePermutation.get(templateIndex);
                        Location templateTargetLocation = templateTargetPermutation.get(templateIndex);
                        Location sourceLocation = sourceLocations.get(templateIndex);
                        Location targetConstraint = targetConstraints.get(templateIndex);
                        
                        Compartment sourceCompartment = templateSourceLocation.getReferencedCompartment(compartments);
                        if (sourceCompartment == null) {
                            throw new IllegalArgumentException("Unknown compartment: " + templateSourceLocation.getName());
                        }
                        Compartment targetCompartment = templateTargetLocation.getReferencedCompartment(compartments);
                        if (targetCompartment == null) {
                            throw new IllegalArgumentException("Unknown compartment: " + templateTargetLocation.getName());
                        }
            
                        if (sourceLocation.getIndices().length != templateSourceLocation.getIndices().length 
                                || !sourceLocation.getName().equals(templateSourceLocation.getName())) {
                            continue;
                        }
            
                        Map<String, Integer> variables = new HashMap<String, Integer>();
                        for (int index = 0; index < templateSourceLocation.getIndices().length; index++) {
                            CellIndexExpression sourceIndex = templateSourceLocation.getIndices()[index];
                            if (sourceIndex.type == Type.VARIABLE_REFERENCE) {
                                CellIndexExpression inputIndex = sourceLocation.getIndices()[index];
                                variables.put(sourceIndex.reference.variableName, (int) inputIndex.value);
                            }
                        }
            
                        
                        List<CellIndexExpression> targetIndices = new ArrayList<CellIndexExpression>();
                        for (int index = 0; index < templateTargetLocation.getIndices().length; index++) {
                            CellIndexExpression targetIndex = templateTargetLocation.getIndices()[index];
                            int targetIndexValue = targetIndex.evaluateIndex(variables);
                            if (targetIndexValue < 0 || targetIndexValue >= targetCompartment.getDimensions()[index]) {
                                valid = false;
                                break;
                            }
                            targetIndices.add(new CellIndexExpression("" + targetIndexValue));
                        }
                        
                        if (valid) {
                            Location targetLocation = new Location(templateTargetLocation.getName(), targetIndices);
                            if (targetConstraint == null || targetConstraint.equals(targetLocation) || targetConstraint.isRefinement(targetLocation)) {
                                targetLocations.add(targetLocation);
                            }
                            else {
                                valid = false;
                            }
                        }
                    }
                    
                    if (valid) {
                        result.add(targetLocations);
                    }
                }
            }
        }
        return result;
    }

    void permuteLocations(List<Location> locations, List<Location> targetConstraints,
            List<List<Location>> locationPermutations, List<List<Location>> targetConstraintPermutations) {
        
        locationPermutations.addAll(permuteLocations(locations));
        for (List<Location> locationPermutation : locationPermutations) {
            List<Location> constraintPermutation = new ArrayList<Location>();
            for (Location location : locationPermutation) {
                constraintPermutation.add(targetConstraints.get(locations.indexOf(location)));
            }
            targetConstraintPermutations.add(constraintPermutation);
        }
    }

    List<List<Location>> permuteLocations(List<Location> locations) {
        List<List<Location>> result = new ArrayList<List<Location>>();
        if (locations.size() == 1) {
            result.add(getList(locations.get(0)));
            return result;
        }
        for (Location location : locations) {
            List<Location> workingLocations = new ArrayList<Location>(locations);
            workingLocations.remove(location);
            List<List<Location>> suffixLists = permuteLocations(workingLocations);
            for (List<Location> suffixList : suffixLists) {
                suffixList.add(0, location);
            }
            result.addAll(suffixLists);
        }
        
        return result;
    }

    boolean isValidSourceLocations(List<Location> templateLocations, List<Location> locationPermutation, List<Compartment> compartments) {
        if (locationPermutation.size() != templateLocations.size()) {
            return false;
        }
        for (int index = 0; index < templateLocations.size(); index++) {
            if (!isValidSourceLocation(templateLocations.get(index), locationPermutation.get(index), compartments)) {
                return false;
            }
        }
        return true;
    }

    boolean isValidSourceLocation(Location template, Location source, List<Compartment> compartments) {
        Compartment compartment = template.getReferencedCompartment(compartments);
        if (compartment == null) {
            throw new IllegalArgumentException("Unknown compartment: " + template.getName());
        }
        if (template.getIndices().length != source.getIndices().length 
                || !template.getName().equals(source.getName())) {
            return false;
        }
        for (int index = 0; index < source.getIndices().length; index++) {
            CellIndexExpression sourceIndex = source.getIndices()[index];
            CellIndexExpression templateIndex = template.getIndices()[index];
            if (!sourceIndex.isFixed()) {
                return false;
            }
            if (templateIndex.isFixed()) {
                if (!templateIndex.equals(sourceIndex)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void validate(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        if (templateLocationPairs.size() == 0) {
            throw new IllegalStateException("No location pairs defined");
        }
        for (List<Location>[] locationPair : templateLocationPairs) {
            boolean found = false;
            
            for (Location location : locationPair[0]) {
                found = false;
                for (Compartment compartment : compartments) {
                    if (location.getName().equals(compartment.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalStateException("Compartment '" + location.getName() + "' not found");
                }
            }            
            for (Location location : locationPair[1]) {
                found = false;
                for (Compartment compartment : compartments) {
                    if (location.getName().equals(compartment.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalStateException("Compartment '" + location.getName() + "' not found");
                }
            }
        }
    }

    public boolean isValidLinkChannel() {
        for (List<Location>[] locationPair : templateLocationPairs) {
            if (locationPair[0].size() > 1) {
                return false;
            }
        }
        return true;
    }
}
