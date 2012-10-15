package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Z;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Z_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Z_PLUS_1;
import static org.demonsoft.spatialkappa.model.Utils.getCompartment;
import static org.demonsoft.spatialkappa.model.Utils.getList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.demonsoft.spatialkappa.model.Compartment.OpenCircle;
import org.demonsoft.spatialkappa.model.Compartment.OpenCuboid;
import org.demonsoft.spatialkappa.model.Compartment.OpenCylinder;
import org.demonsoft.spatialkappa.model.Compartment.OpenRectangle;
import org.demonsoft.spatialkappa.model.Compartment.OpenSphere;
import org.demonsoft.spatialkappa.model.Compartment.OpenSpine;
import org.demonsoft.spatialkappa.model.Compartment.SolidCircle;
import org.demonsoft.spatialkappa.model.Compartment.SolidCylinder;
import org.demonsoft.spatialkappa.model.Compartment.SolidSphere;
import org.demonsoft.spatialkappa.model.Compartment.SolidSpine;
import org.demonsoft.spatialkappa.model.Compartment.Spine;
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableExpression.Type;

// TODO add target voxel calculation for nested compartments

public class ChannelComponent {
    
    protected static final String SUBCOMPONENT = "subcomponent";
    
    public final String channelType;
    public final List<ChannelConstraint> templateConstraints = new ArrayList<ChannelConstraint>();
    
    // Constructor for unit tests
    public ChannelComponent(Location sourceLocation, Location targetLocation) {
        this(null, getList(sourceLocation), getList(targetLocation));
    }
    
    public ChannelComponent(List<Location> sourceLocations, List<Location> targetLocations) {
        this(null, sourceLocations, targetLocations);
    }
    
    ChannelComponent(String channelType, List<Location> sourceLocations, List<Location> targetLocations) {
        if (sourceLocations == null || targetLocations == null) {
            throw new NullPointerException();
        }
        if (sourceLocations.size() == 0 || sourceLocations.size() != targetLocations.size()) {
            throw new IllegalArgumentException();
        }
        this.channelType = channelType;
        for (int index=0; index < sourceLocations.size(); index++) {
            templateConstraints.add(new ChannelConstraint(sourceLocations.get(index), targetLocations.get(index)));
        }
    }
    
    ChannelComponent(String channelType, List<ChannelConstraint> constraints) {
        if (constraints == null) {
            throw new NullPointerException();
        }
        if (constraints.size() == 0) {
            throw new IllegalArgumentException();
        }
        this.channelType = channelType;
        templateConstraints.addAll(constraints);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (channelType != null && channelType != SUBCOMPONENT) {
            builder.append("(").append(channelType).append(") ");
        }
        builder.append(templateConstraints);
        return builder.toString();
    }

    
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channelType == null) ? 0 : channelType.hashCode());
        result = prime * result + ((templateConstraints == null) ? 0 : templateConstraints.hashCode());
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
        ChannelComponent other = (ChannelComponent) obj;
        if (channelType == null) {
            if (other.channelType != null)
                return false;
        }
        else if (!channelType.equals(other.channelType))
            return false;
        if (templateConstraints == null) {
            if (other.templateConstraints != null)
                return false;
        }
        else if (!templateConstraints.equals(other.templateConstraints))
            return false;
        return true;
    }

    // For unit tests
    final List<Location> applyChannel(Location sourceLocation, Location targetConstraint, List<Compartment> compartments) {
        List<List<Location>> multiResult = applyChannel(getList(new ChannelConstraint(sourceLocation, targetConstraint)), compartments);
        List<Location> result = new ArrayList<Location>();
        for (List<Location> current : multiResult) {
            result.add(current.get(0));
        }
        return result;
    }

    
    public List<List<Location>> applyChannel(List<ChannelConstraint> constraints, List<Compartment> compartments) {
        List<List<Location>> result = new ArrayList<List<Location>>();
        
        List<List<ChannelConstraint>> templateConstraintPermutations = permuteChannelConstraints(templateConstraints, constraints.size());
        
        for (List<ChannelConstraint> templateConstraintPermutation : templateConstraintPermutations) {

            if (isValidSourceLocations(templateConstraintPermutation, constraints, compartments)) {

                List<Location> targetLocations = new ArrayList<Location>();
                boolean valid = true;
                
                for (int templateIndex=0; templateIndex<templateConstraintPermutation.size() && valid; templateIndex++) {
                    
                    ChannelConstraint templateConstraint = templateConstraintPermutation.get(templateIndex);
                    ChannelConstraint constraint = constraints.get(templateIndex);
                    Location templateTargetLocation = templateConstraint.targetConstraint;
                    
                    if (templateTargetLocation == Location.FIXED_LOCATION) {
                        targetLocations.add(constraint.sourceLocation);
                    }
                    else {
                        Location templateSourceLocation = templateConstraint.sourceLocation;
                        Location sourceLocation = constraint.sourceLocation;
                        
                        Compartment targetCompartment = getCompartment(compartments, templateTargetLocation.getName());

                        Map<String, Integer> variables = getVariables(templateSourceLocation, sourceLocation);
            
                        List<CellIndexExpression> targetIndices = new ArrayList<CellIndexExpression>();
                        for (int index = 0; index < templateTargetLocation.getIndices().length; index++) {
                            CellIndexExpression targetIndex = templateTargetLocation.getIndices()[index];
                            int targetIndexValue = targetIndex.evaluateIndex(variables);
                            targetIndices.add(new CellIndexExpression("" + targetIndexValue));
                        }
                        
                        if (valid) {
                            Location targetLocation = new Location(templateTargetLocation.getName(), targetIndices);

                            Compartment sourceCompartment = getCompartment(compartments, templateSourceLocation.getName());

                            if (isNesting(sourceCompartment, targetCompartment)) {
                                targetLocation = applyNestingOffsets(sourceCompartment, targetCompartment, targetLocation);
                            }

                            if (targetCompartment == null || targetCompartment.isValidVoxel(targetLocation)) {
                                Location targetConstraint = constraint.targetConstraint;
                                if (targetConstraint == null || targetConstraint.equals(targetLocation) || targetConstraint.isRefinement(targetLocation)) {
                                    targetLocations.add(targetLocation);
                                }
                                else {
                                    valid = false;
                                }
                            }
                            else {
                                valid = false;
                            }
                        }
                    }
                }
                
                if (valid) {
                    result.add(targetLocations);
                }
            }
        }
        
        removeMotionlessResults(constraints, result);
        return result;
    }
    
    static void removeMotionlessResults(List<ChannelConstraint> originalConstraints, List<List<Location>> newLocations) {
        ListIterator<List<Location>> iter = newLocations.listIterator();
        while (iter.hasNext()) {
            List<Location> currentLocations = iter.next();
            boolean different = false;
            for (int index=0; index < originalConstraints.size(); index++) {
                ChannelConstraint originalConstraint = originalConstraints.get(index);
                Location currentLocation = currentLocations.get(index);
                
                if (!originalConstraint.sourceLocation.equals(currentLocation)) {
                    different = true;
                    break;
                }
            }
            
            if (!different) {
                iter.remove();
            }
        }
    }

    
    public List<ChannelConstraint> getCellReferencePairs(List<Compartment> compartments) {
        List<ChannelConstraint> result = new ArrayList<ChannelConstraint>();
        Location source = templateConstraints.get(0).sourceLocation;
        Location target = templateConstraints.get(0).targetConstraint;

        Compartment sourceCompartment = getCompartment(compartments, source.getName());
        Compartment targetCompartment = getCompartment(compartments, target.getName());

        if (!source.isVoxel(sourceCompartment) || !target.isVoxel(targetCompartment)) {
            throw new IllegalArgumentException();
        }
        if (isConcreteLink(source, target)) {
            if (sourceCompartment.isValidVoxel(source) && targetCompartment.isValidVoxel(target)) {
                result.add(new ChannelConstraint(source, target));
            }
        }
        else {
            Object[][] variableRanges = getVariableRanges(source, sourceCompartment);
            Map<String, Integer> variables = new HashMap<String, Integer>();
            for (int index = 0; index < variableRanges.length; index++) {
                variables.put((String) variableRanges[index][0], 0);
            }
            getCellReferencePairs(result, variableRanges, source, target, sourceCompartment, targetCompartment, variables, 0);
        }
        return result;
    }

    private void getCellReferencePairs(List<ChannelConstraint> result, Object[][] variableRanges, Location source, Location target, 
            Compartment sourceCompartment, Compartment targetCompartment, Map<String, Integer> variables, int variableIndex) {
        if (variableIndex >= variableRanges.length) {
            for (int index = 0; index < targetCompartment.getDimensions().length; index++) {
                int value = target.getIndices()[index].evaluateIndex(variables);
                if (value < 0 || value >= targetCompartment.getDimensions()[index]) {
                    return;
                }
            }
            Location concreteSource = source.getConcreteLocation(variables);
            Location concreteTarget = target.getConcreteLocation(variables);
            if (sourceCompartment.isValidVoxel(concreteSource) && targetCompartment.isValidVoxel(concreteTarget)) {
                result.add(new ChannelConstraint(concreteSource, concreteTarget));
            }
            return;
        }

        String currentSourceVariable = (String) variableRanges[variableIndex][0];
        int currentSourceMax = (Integer) variableRanges[variableIndex][1];

        for (int index = 0; index < currentSourceMax; index++) {
            variables.put(currentSourceVariable, index);
            getCellReferencePairs(result, variableRanges, source, target, sourceCompartment, targetCompartment, variables, variableIndex + 1);
        }
    }

    List<List<ChannelConstraint>> permuteChannelConstraints(List<ChannelConstraint> constraints, int totalConstraints) {
        if (totalConstraints < constraints.size()) {
            return new ArrayList<List<ChannelConstraint>>(); // TODO make constant
        }
        
        List<ChannelConstraint> workingConstraints = new ArrayList<ChannelConstraint>(constraints);
        for (int index=constraints.size(); index<totalConstraints; index++) {
            workingConstraints.add(ChannelConstraint.FIXED_CONSTRAINT);
        }
        return permuteChannelConstraints(workingConstraints);
    }

    private List<List<ChannelConstraint>> permuteChannelConstraints(List<ChannelConstraint> constraints) {
        List<List<ChannelConstraint>> result = new ArrayList<List<ChannelConstraint>>();
        if (constraints.size() == 1) {
            result.add(getList(constraints.get(0)));
            return result;
        }
        for (ChannelConstraint constraint : constraints) {
            List<ChannelConstraint> workingConstraints = new ArrayList<ChannelConstraint>(constraints);
            workingConstraints.remove(constraint);
            List<List<ChannelConstraint>> suffixLists = permuteChannelConstraints(workingConstraints);
            for (List<ChannelConstraint> suffixList : suffixLists) {
                suffixList.add(0, constraint);
                if (!result.contains(suffixList)) {
                    result.add(suffixList);
                }
            }
        }
        
        return result;
    }

    private boolean isConcreteLink(Location source, Location target) {
        return source.isConcreteLocation() && target.isConcreteLocation();
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

    @SuppressWarnings("hiding")
    boolean isValidSourceLocations(List<ChannelConstraint> templateConstraints, List<ChannelConstraint> constraints, List<Compartment> compartments) {
        if (constraints.size() != templateConstraints.size()) {
            return false;
        }
        for (int index = 0; index < templateConstraints.size(); index++) {
            ChannelConstraint templateConstraint = templateConstraints.get(index);
            ChannelConstraint constraint = constraints.get(index);
            if (Location.FIXED_LOCATION == templateConstraint.targetConstraint) {
                if (!constraint.sourceLocation.equals(constraint.targetConstraint) &&
                        Location.FIXED_LOCATION != constraint.targetConstraint) {
                    return false;
                }
            }
            else if (!isValidSourceLocation(templateConstraint.sourceLocation, constraint.sourceLocation, compartments)) {
                return false;
            }
        }
        return true;
    }

    protected boolean isValidSourceLocation(Location template, Location source, List<Compartment> compartments) {
        Compartment compartment = getCompartment(compartments, template.getName());
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
        return compartment.isValidVoxel(source);
    }

    private Map<String, Integer> getVariables(Location templateSourceLocation, Location sourceLocation) {
        Map<String, Integer> variables = new HashMap<String, Integer>();
        for (int index = 0; index < templateSourceLocation.getIndices().length; index++) {
            CellIndexExpression sourceIndex = templateSourceLocation.getIndices()[index];
            if (sourceIndex.type == Type.VARIABLE_REFERENCE) {
                CellIndexExpression inputIndex = sourceLocation.getIndices()[index];
                variables.put(sourceIndex.reference.variableName, (int) inputIndex.value);
            }
        }
        return variables;
    }
    
    
    static abstract class PredefinedChannelComponent extends ChannelComponent {

        public PredefinedChannelComponent(String channelType, List<Location> sourceLocations, List<Location> targetLocations) {
            super(channelType, sourceLocations, targetLocations);
            for (Location location : sourceLocations) {
                if (location.getIndices().length > 0) {
                    throw new IllegalArgumentException("Channel location must be compartment only: " + location);
                }
            }
            for (Location location : targetLocations) {
                if (location.getIndices().length > 0) {
                    throw new IllegalArgumentException("Channel location must be compartment only: " + location);
                }
            }
        }
        
        @Override
        public List<List<Location>> applyChannel(List<ChannelConstraint> constraints, List<Compartment> compartments) {
            List<ChannelComponent> channelSubcomponents = getChannelSubcomponents(compartments);
            List<List<Location>> result = new ArrayList<List<Location>>();
            for (ChannelComponent component : channelSubcomponents) {
                result.addAll(component.applyChannel(constraints, compartments));
            }
            removeMotionlessResults(constraints, result);
            return result;
        }

        protected abstract List<ChannelComponent> getChannelSubcomponents(List<Compartment> compartments);

    }
    
    public static class EdgeNeighbourComponent extends PredefinedChannelComponent {

        public static final String NAME = "EdgeNeighbour";
        
        public EdgeNeighbourComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
            // TODO test dimensions
        }
        
        @Override
        protected List<ChannelComponent> getChannelSubcomponents(List<Compartment> compartments) {
            List<List<CellIndexExpression>> sourceIndices = new ArrayList<List<CellIndexExpression>>();
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            
            List<List<CellIndexExpression>> targetIndices = new ArrayList<List<CellIndexExpression>>();
            targetIndices.add(getList(INDEX_X, INDEX_Y));
            targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y));
            targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y));
            targetIndices.add(getList(INDEX_X, INDEX_Y_MINUS_1));
            targetIndices.add(getList(INDEX_X, INDEX_Y_PLUS_1));
            
            List<ChannelComponent> result = new ArrayList<ChannelComponent>();
            
            for (int index = 0; index < sourceIndices.size(); index++) {
                List<CellIndexExpression> currentSourceIndices = sourceIndices.get(index);
                List<CellIndexExpression> currentTargetIndices = targetIndices.get(index);
                
                List<Location> currentSourceLocations = new ArrayList<Location>();
                List<Location> currentTargetLocations = new ArrayList<Location>();
                for (ChannelConstraint constraint : templateConstraints) {
                    currentSourceLocations.add(new Location(constraint.sourceLocation.getName(), currentSourceIndices));
                    currentTargetLocations.add(new Location(constraint.targetConstraint.getName(), currentTargetIndices));
                }
                result.add(new ChannelComponent(SUBCOMPONENT, currentSourceLocations, currentTargetLocations));
            }
            return result;
        }
    }
    
    public static class NeighbourComponent extends PredefinedChannelComponent {

        public static final String NAME = "Neighbour";

        public NeighbourComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }

        @Override
        protected List<ChannelComponent> getChannelSubcomponents(List<Compartment> compartments) {
            Compartment compartment = getCompartment(compartments, templateConstraints.get(0).sourceLocation.getName());
            int dimensionCount = compartment.dimensions.length;
            
            
            List<List<CellIndexExpression>> sourceIndices = new ArrayList<List<CellIndexExpression>>();
            List<List<CellIndexExpression>> targetIndices = new ArrayList<List<CellIndexExpression>>();

            if (dimensionCount == 2) {
                sourceIndices.add(getList(INDEX_X, INDEX_Y));
                sourceIndices.add(getList(INDEX_X, INDEX_Y));
                sourceIndices.add(getList(INDEX_X, INDEX_Y));
                sourceIndices.add(getList(INDEX_X, INDEX_Y));
                sourceIndices.add(getList(INDEX_X, INDEX_Y));
                sourceIndices.add(getList(INDEX_X, INDEX_Y));
                sourceIndices.add(getList(INDEX_X, INDEX_Y));
                sourceIndices.add(getList(INDEX_X, INDEX_Y));
                sourceIndices.add(getList(INDEX_X, INDEX_Y));
                
                targetIndices.add(getList(INDEX_X, INDEX_Y));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y));
                targetIndices.add(getList(INDEX_X, INDEX_Y_MINUS_1));
                targetIndices.add(getList(INDEX_X, INDEX_Y_PLUS_1));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y_MINUS_1));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y_PLUS_1));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y_PLUS_1));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y_MINUS_1));
            }
            else if (dimensionCount == 3) {
                for (int i=0; i<27; i++) {
                    sourceIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
                }
                targetIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y_MINUS_1, INDEX_Z_MINUS_1));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y_MINUS_1, INDEX_Z));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y_MINUS_1, INDEX_Z_PLUS_1));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y, INDEX_Z_MINUS_1));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y, INDEX_Z));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y, INDEX_Z_PLUS_1));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y_PLUS_1, INDEX_Z_MINUS_1));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y_PLUS_1, INDEX_Z));
                targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y_PLUS_1, INDEX_Z_PLUS_1));
                targetIndices.add(getList(INDEX_X, INDEX_Y_MINUS_1, INDEX_Z_MINUS_1));
                targetIndices.add(getList(INDEX_X, INDEX_Y_MINUS_1, INDEX_Z));
                targetIndices.add(getList(INDEX_X, INDEX_Y_MINUS_1, INDEX_Z_PLUS_1));
                targetIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z_MINUS_1));
                targetIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z_PLUS_1));
                targetIndices.add(getList(INDEX_X, INDEX_Y_PLUS_1, INDEX_Z_MINUS_1));
                targetIndices.add(getList(INDEX_X, INDEX_Y_PLUS_1, INDEX_Z));
                targetIndices.add(getList(INDEX_X, INDEX_Y_PLUS_1, INDEX_Z_PLUS_1));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y_MINUS_1, INDEX_Z_MINUS_1));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y_MINUS_1, INDEX_Z));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y_MINUS_1, INDEX_Z_PLUS_1));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y, INDEX_Z_MINUS_1));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y, INDEX_Z));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y, INDEX_Z_PLUS_1));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y_PLUS_1, INDEX_Z_MINUS_1));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y_PLUS_1, INDEX_Z));
                targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y_PLUS_1, INDEX_Z_PLUS_1));
            }
            
            // TODO add unmoving
            
            List<List<ChannelConstraint>> allConstraints = new ArrayList<List<ChannelConstraint>>();
            for (ChannelConstraint templateConstraint : templateConstraints) {
                List<List<ChannelConstraint>> newConstraints = new ArrayList<List<ChannelConstraint>>();
                
                for (int index = 0; index < sourceIndices.size(); index++) {
                    List<CellIndexExpression> currentSourceIndices = sourceIndices.get(index);
                    List<CellIndexExpression> currentTargetIndices = targetIndices.get(index);
                    ChannelConstraint currentConstraint = new ChannelConstraint(
                            new Location(templateConstraint.sourceLocation.getName(), currentSourceIndices),
                            new Location(templateConstraint.targetConstraint.getName(), currentTargetIndices));
                    
                    if (allConstraints.size() == 0) {
                        newConstraints.add(getList(currentConstraint));
                    }
                    else {
                        for (List<ChannelConstraint> previousAllConstraints : allConstraints) {
                            List<ChannelConstraint> currentAllConstraints = new ArrayList<ChannelConstraint>(previousAllConstraints);
                            currentAllConstraints.add(currentConstraint);
                            newConstraints.add(currentAllConstraints);
                        }
                    }
                }
                allConstraints = newConstraints;
            }

            List<ChannelComponent> result = new ArrayList<ChannelComponent>();
            for (List<ChannelConstraint> constraints : allConstraints) {
                result.add(new ChannelComponent(SUBCOMPONENT, constraints));
            }
            
            return result;
        }
    }
    
    public static class HexagonalComponent extends PredefinedChannelComponent {

        public static final String NAME = "Hexagonal";

        public HexagonalComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }
        
        @Override
        protected List<ChannelComponent> getChannelSubcomponents(List<Compartment> compartments) {
            List<List<CellIndexExpression>> sourceIndices = new ArrayList<List<CellIndexExpression>>();
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            sourceIndices.add(getList(INDEX_X, INDEX_Y));
            
            List<List<CellIndexExpression>> targetIndices = new ArrayList<List<CellIndexExpression>>();
            targetIndices.add(getList(INDEX_X, INDEX_Y));
            targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y));
            targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y));
            targetIndices.add(getList(INDEX_X, INDEX_Y_MINUS_1));
            targetIndices.add(getList(INDEX_X, INDEX_Y_PLUS_1));
            targetIndices.add(getList(INDEX_X_MINUS_1, 
                    new CellIndexExpression(INDEX_Y_MINUS_1, Operator.PLUS, 
                            new CellIndexExpression(INDEX_2, Operator.MULTIPLY, 
                                    new CellIndexExpression(INDEX_X_MINUS_1, Operator.MODULUS, INDEX_2)))));
            targetIndices.add(getList(INDEX_X_PLUS_1, 
                    new CellIndexExpression(INDEX_Y_PLUS_1, Operator.MINUS, 
                            new CellIndexExpression(INDEX_2, Operator.MULTIPLY, 
                                    new CellIndexExpression(INDEX_X, Operator.MODULUS, INDEX_2)))));

            
            List<ChannelComponent> result = new ArrayList<ChannelComponent>();
            
            for (int index = 0; index < sourceIndices.size(); index++) {
                List<CellIndexExpression> currentSourceIndices = sourceIndices.get(index);
                List<CellIndexExpression> currentTargetIndices = targetIndices.get(index);
                
                List<Location> currentSourceLocations = new ArrayList<Location>();
                List<Location> currentTargetLocations = new ArrayList<Location>();
                for (ChannelConstraint constraint : templateConstraints) {
                    currentSourceLocations.add(new Location(constraint.sourceLocation.getName(), currentSourceIndices));
                    currentTargetLocations.add(new Location(constraint.targetConstraint.getName(), currentTargetIndices));
                }
                result.add(new ChannelComponent(SUBCOMPONENT, currentSourceLocations, currentTargetLocations));
            }
            return result;
        }
    }
    
    public static class FaceNeighbourComponent extends PredefinedChannelComponent {

        public static final String NAME = "FaceNeighbour";
        
        public FaceNeighbourComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
            // TODO test dimensions
        }
        
        @Override
        protected List<ChannelComponent> getChannelSubcomponents(List<Compartment> compartments) {
            List<List<CellIndexExpression>> sourceIndices = new ArrayList<List<CellIndexExpression>>();
            sourceIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
            sourceIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
            sourceIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
            sourceIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
            sourceIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
            sourceIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
            sourceIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
            
            List<List<CellIndexExpression>> targetIndices = new ArrayList<List<CellIndexExpression>>();
            targetIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
            targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y, INDEX_Z));
            targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y, INDEX_Z));
            targetIndices.add(getList(INDEX_X, INDEX_Y_MINUS_1, INDEX_Z));
            targetIndices.add(getList(INDEX_X, INDEX_Y_PLUS_1, INDEX_Z));
            targetIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z_MINUS_1));
            targetIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z_PLUS_1));
            
            List<ChannelComponent> result = new ArrayList<ChannelComponent>();
            
            for (int index = 0; index < sourceIndices.size(); index++) {
                List<CellIndexExpression> currentSourceIndices = sourceIndices.get(index);
                List<CellIndexExpression> currentTargetIndices = targetIndices.get(index);
                
                List<Location> currentSourceLocations = new ArrayList<Location>();
                List<Location> currentTargetLocations = new ArrayList<Location>();
                for (ChannelConstraint constraint : templateConstraints) {
                    currentSourceLocations.add(new Location(constraint.sourceLocation.getName(), currentSourceIndices));
                    currentTargetLocations.add(new Location(constraint.targetConstraint.getName(), currentTargetIndices));
                }
                result.add(new ChannelComponent(SUBCOMPONENT, currentSourceLocations, currentTargetLocations));
            }
            return result;
        }
    }

    public static class RadialComponent extends PredefinedChannelComponent {

        public static final String NAME = "Radial";
        
        public RadialComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
            // TODO test dimensions
            // TODO constrain to single location at a time
        }
        
        @Override
        public List<List<Location>> applyChannel(List<ChannelConstraint> constraints, List<Compartment> compartments) {
            
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            if (isValidSourceLocations(templateConstraints, constraints, compartments)) {

                // TODO constrain to single location at a time
                for (int templateIndex=0; templateIndex<templateConstraints.size(); templateIndex++) {
                    Location templateSourceLocation = templateConstraints.get(templateIndex).sourceLocation;
                    Location templateTargetLocation = templateConstraints.get(templateIndex).targetConstraint;
                    ChannelConstraint constraint = constraints.get(templateIndex);
                    Location sourceLocation = constraint.sourceLocation;
                    Location targetConstraint = constraint.targetConstraint;
                    
                    Compartment sourceCompartment = getCompartment(compartments, templateSourceLocation.getName());
                    Compartment targetCompartment = getCompartment(compartments, templateTargetLocation.getName());
        
                    if (sourceLocation.getIndices().length != sourceCompartment.getDimensions().length 
                            || !sourceLocation.getName().equals(templateSourceLocation.getName())) {
                        continue;
                    }
        
                    List<Location> newLocations = null;
                    if (sourceCompartment.getDimensions().length == 2) {
                        newLocations = getRadialLocations2D(sourceLocation, sourceCompartment);
                    }
                    else {
                        newLocations = getRadialLocations3D(sourceLocation, sourceCompartment);
                    }
                    
                    if (isNesting(sourceCompartment, targetCompartment)) {
                        newLocations = applyNestingOffsets(sourceCompartment, targetCompartment, newLocations);
                    }

                    for (Location location : newLocations) {
                        if (targetCompartment.isValidVoxel(location)) {
                            if (targetConstraint == null || targetConstraint.equals(location) || targetConstraint.isRefinement(location)) {
                                result.add(getList(location));
                            }
                        }
                    }
                }
            }
            removeMotionlessResults(constraints, result);
            return result;
        }
        
        private List<Location> getRadialLocations2D(Location location, Compartment compartment) {
            int doubleDeltaY = ((int) location.getIndices()[0].value) * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = ((int) location.getIndices()[1].value) * 2 - compartment.dimensions[1] + 1;
            
            CellIndexExpression newIndexX = null;
            CellIndexExpression newIndexY = null;
            
            List<Location> result = new ArrayList<Location>();
            result.add(location);
            
            // Direction out 
            
            if (Math.abs(doubleDeltaX) > Math.abs(doubleDeltaY)) {
                newIndexY = location.getIndices()[0];
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? 1 : -1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else if (Math.abs(doubleDeltaX) < Math.abs(doubleDeltaY)) {
                newIndexX = location.getIndices()[1];
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? 1 : -1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else if (doubleDeltaX != 0) {
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? 1 : -1);
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? 1 : -1);
                result.add(new Location(location.getName(), newIndexY, location.getIndices()[1]));
                result.add(new Location(location.getName(), location.getIndices()[0], newIndexX));
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else {
                newIndexX = location.getIndices()[1].getDeltaIndex(-1);
                newIndexY = location.getIndices()[0].getDeltaIndex(-1);
                CellIndexExpression newIndexX2 = location.getIndices()[1].getDeltaIndex(1);
                CellIndexExpression newIndexY2 = location.getIndices()[0].getDeltaIndex(1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
                result.add(new Location(location.getName(), newIndexY, location.getIndices()[1]));
                result.add(new Location(location.getName(), newIndexY, newIndexX2));
                result.add(new Location(location.getName(), location.getIndices()[0], newIndexX));
                result.add(new Location(location.getName(), location.getIndices()[0], newIndexX2));
                result.add(new Location(location.getName(), newIndexY2, newIndexX));
                result.add(new Location(location.getName(), newIndexY2, location.getIndices()[1]));
                result.add(new Location(location.getName(), newIndexY2, newIndexX2));
            }

            // Direction in 
            
            if (Math.abs(doubleDeltaX) > Math.abs(doubleDeltaY)) {
                newIndexY = location.getIndices()[0];
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? -1 : 1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else if (Math.abs(doubleDeltaX) < Math.abs(doubleDeltaY)) {
                newIndexX = location.getIndices()[1];
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? -1 : 1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else if (doubleDeltaX != 0) {
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? -1 : 1);
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? -1 : 1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            return result;
        }

        private List<Location> getRadialLocations3D(Location location, Compartment compartment) {
            int doubleDeltaY = ((int) location.getIndices()[0].value) * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = ((int) location.getIndices()[1].value) * 2 - compartment.dimensions[1] + 1;
            int doubleDeltaZ = ((int) location.getIndices()[2].value) * 2 - compartment.dimensions[2] + 1;
            
            // Direction out 

            List<Location> result = new ArrayList<Location>();
            result.add(location);

            int doubleDeltaMax = Math.max(Math.abs(doubleDeltaX), Math.max(Math.abs(doubleDeltaY), Math.abs(doubleDeltaZ)));
            if (doubleDeltaMax == 0) {
                CellIndexExpression[] xIndices = new CellIndexExpression[] {
                        location.getIndices()[1].getDeltaIndex(-1),
                        location.getIndices()[1],
                        location.getIndices()[1].getDeltaIndex(1),
                };
                CellIndexExpression[] yIndices = new CellIndexExpression[] {
                        location.getIndices()[0].getDeltaIndex(-1),
                        location.getIndices()[0],
                        location.getIndices()[0].getDeltaIndex(1),
                };
                CellIndexExpression[] zIndices = new CellIndexExpression[] {
                        location.getIndices()[2].getDeltaIndex(-1),
                        location.getIndices()[2],
                        location.getIndices()[2].getDeltaIndex(1),
                };
                
                for (CellIndexExpression yIndex : yIndices) {
                    for (CellIndexExpression xIndex : xIndices) {
                        for (CellIndexExpression zIndex : zIndices) {
                            if (xIndex != location.getIndices()[1] || yIndex != location.getIndices()[0] || zIndex != location.getIndices()[2]) {
                                result.add(new Location(location.getName(), yIndex, xIndex, zIndex));
                            }
                        }
                    }
                }
                
                return result;
            }

            CellIndexExpression newIndexX = null;
            CellIndexExpression newIndexY = null;
            CellIndexExpression newIndexZ = null;
            
            if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? 1 : -1);
            }
            if (Math.abs(doubleDeltaY) == doubleDeltaMax) {
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? 1 : -1);
            }
            if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                newIndexZ = location.getIndices()[2].getDeltaIndex((doubleDeltaY > 0) ? 1 : -1);
            }
            
            if (newIndexY != null) {
                if (newIndexX != null) {
                    if (newIndexZ != null) {
                        result.add(new Location(location.getName(), newIndexY, newIndexX, newIndexZ));
                    }
                    result.add(new Location(location.getName(), newIndexY, newIndexX, location.getIndices()[2]));
                }
                if (newIndexZ != null) {
                    result.add(new Location(location.getName(), newIndexY, location.getIndices()[1], newIndexZ));
                }
                result.add(new Location(location.getName(), newIndexY, location.getIndices()[1], location.getIndices()[2]));
            }
            if (newIndexX != null) {
                if (newIndexZ != null) {
                    result.add(new Location(location.getName(), location.getIndices()[0], newIndexX, newIndexZ));
                }
                result.add(new Location(location.getName(), location.getIndices()[0], newIndexX, location.getIndices()[2]));
            }
            if (newIndexZ != null) {
                result.add(new Location(location.getName(), location.getIndices()[0], location.getIndices()[1], newIndexZ));
            }
            
            // Direction in 
            
            newIndexX = location.getIndices()[1];
            newIndexY = location.getIndices()[0];
            newIndexZ = location.getIndices()[2];
            
            if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? -1 : 1);
            }
            if (Math.abs(doubleDeltaY) == doubleDeltaMax) {
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? -1 : 1);
            }
            if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                newIndexZ = location.getIndices()[2].getDeltaIndex((doubleDeltaY > 0) ? -1 : 1);
            }
            result.add(new Location(location.getName(), newIndexY, newIndexX, newIndexZ));

            
            return result;
        }

        @Override
        protected boolean isValidSourceLocation(Location template, Location source, List<Compartment> compartments) {
            Compartment compartment = getCompartment(compartments, template.getName());
            if (compartment.getDimensions().length != source.getIndices().length 
                    || !template.getName().equals(source.getName())) {
                return false;
            }
            for (int index = 0; index < source.getIndices().length; index++) {
                CellIndexExpression sourceIndex = source.getIndices()[index];
                if (!sourceIndex.isFixed()) {
                    return false;
                }
            }
            return compartment.isValidVoxel(source);
        }

        @Override
        protected List<ChannelComponent> getChannelSubcomponents(List<Compartment> compartments) {
            return null;
        }
        
    }

    public static class RadialOutComponent extends PredefinedChannelComponent {

        public static final String NAME = "RadialOut";
        
        public RadialOutComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
            // TODO test dimensions
        }
        
        @Override
        public List<List<Location>> applyChannel(List<ChannelConstraint> constraints, List<Compartment> compartments) {
            
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            if (isValidSourceLocations(templateConstraints, constraints, compartments)) {

                // TODO constrain to single location at a time
                for (int templateIndex=0; templateIndex<templateConstraints.size(); templateIndex++) {
                    Location templateSourceLocation = templateConstraints.get(templateIndex).sourceLocation;
                    Location templateTargetLocation = templateConstraints.get(templateIndex).targetConstraint;
                    ChannelConstraint constraint = constraints.get(templateIndex);
                    Location sourceLocation = constraint.sourceLocation;
                    Location targetConstraint = constraint.targetConstraint;
                    
                    Compartment sourceCompartment = getCompartment(compartments, templateSourceLocation.getName());
                    Compartment targetCompartment = getCompartment(compartments, templateTargetLocation.getName());
        
                    if (sourceLocation.getIndices().length != sourceCompartment.getDimensions().length 
                            || !sourceLocation.getName().equals(templateSourceLocation.getName())) {
                        continue;
                    }
        
                    List<Location> newLocations = null;
                    if (sourceCompartment.getDimensions().length == 2) {
                        newLocations = getRadialLocations2D(sourceLocation, sourceCompartment);
                    }
                    else {
                        newLocations = getRadialLocations3D(sourceLocation, sourceCompartment);
                    }
                    
                    if (isNesting(sourceCompartment, targetCompartment)) {
                        newLocations = applyNestingOffsets(sourceCompartment, targetCompartment, newLocations);
                    }
                    
                    for (Location location : newLocations) {
                        if (targetCompartment.isValidVoxel(location)) {
                            if (targetConstraint == null || targetConstraint.equals(location) || targetConstraint.isRefinement(location)) {
                                result.add(getList(location));
                            }
                        }
                    }
                }
            }
            removeMotionlessResults(constraints, result);
            return result;
        }
        
        private List<Location> getRadialLocations2D(Location location, Compartment compartment) {
            int doubleDeltaY = ((int) location.getIndices()[0].value) * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = ((int) location.getIndices()[1].value) * 2 - compartment.dimensions[1] + 1;
            
            CellIndexExpression newIndexX = null;
            CellIndexExpression newIndexY = null;
            
            List<Location> result = new ArrayList<Location>();
            result.add(location);
            
            // Direction out 
            
            if (Math.abs(doubleDeltaX) > Math.abs(doubleDeltaY)) {
                newIndexY = location.getIndices()[0];
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? 1 : -1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else if (Math.abs(doubleDeltaX) < Math.abs(doubleDeltaY)) {
                newIndexX = location.getIndices()[1];
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? 1 : -1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else if (doubleDeltaX != 0) {
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? 1 : -1);
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? 1 : -1);
                result.add(new Location(location.getName(), newIndexY, location.getIndices()[1]));
                result.add(new Location(location.getName(), location.getIndices()[0], newIndexX));
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else {
                newIndexX = location.getIndices()[1].getDeltaIndex(-1);
                newIndexY = location.getIndices()[0].getDeltaIndex(-1);
                CellIndexExpression newIndexX2 = location.getIndices()[1].getDeltaIndex(1);
                CellIndexExpression newIndexY2 = location.getIndices()[0].getDeltaIndex(1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
                result.add(new Location(location.getName(), newIndexY, location.getIndices()[1]));
                result.add(new Location(location.getName(), newIndexY, newIndexX2));
                result.add(new Location(location.getName(), location.getIndices()[0], newIndexX));
                result.add(new Location(location.getName(), location.getIndices()[0], newIndexX2));
                result.add(new Location(location.getName(), newIndexY2, newIndexX));
                result.add(new Location(location.getName(), newIndexY2, location.getIndices()[1]));
                result.add(new Location(location.getName(), newIndexY2, newIndexX2));
            }

            return result;
        }

        private List<Location> getRadialLocations3D(Location location, Compartment compartment) {
            int doubleDeltaY = ((int) location.getIndices()[0].value) * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = ((int) location.getIndices()[1].value) * 2 - compartment.dimensions[1] + 1;
            int doubleDeltaZ = ((int) location.getIndices()[2].value) * 2 - compartment.dimensions[2] + 1;
            
            List<Location> result = new ArrayList<Location>();
            result.add(location);

            int doubleDeltaMax = Math.max(Math.abs(doubleDeltaX), Math.max(Math.abs(doubleDeltaY), Math.abs(doubleDeltaZ)));
            if (doubleDeltaMax == 0) {
                CellIndexExpression[] xIndices = new CellIndexExpression[] {
                        location.getIndices()[1].getDeltaIndex(-1),
                        location.getIndices()[1],
                        location.getIndices()[1].getDeltaIndex(1),
                };
                CellIndexExpression[] yIndices = new CellIndexExpression[] {
                        location.getIndices()[0].getDeltaIndex(-1),
                        location.getIndices()[0],
                        location.getIndices()[0].getDeltaIndex(1),
                };
                CellIndexExpression[] zIndices = new CellIndexExpression[] {
                        location.getIndices()[2].getDeltaIndex(-1),
                        location.getIndices()[2],
                        location.getIndices()[2].getDeltaIndex(1),
                };
                
                for (CellIndexExpression yIndex : yIndices) {
                    for (CellIndexExpression xIndex : xIndices) {
                        for (CellIndexExpression zIndex : zIndices) {
                            if (xIndex != location.getIndices()[1] || yIndex != location.getIndices()[0] || zIndex != location.getIndices()[2]) {
                                result.add(new Location(location.getName(), yIndex, xIndex, zIndex));
                            }
                        }
                    }
                }
                
                return result;
            }

            CellIndexExpression newIndexX = null;
            CellIndexExpression newIndexY = null;
            CellIndexExpression newIndexZ = null;
            
            if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? 1 : -1);
            }
            if (Math.abs(doubleDeltaY) == doubleDeltaMax) {
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? 1 : -1);
            }
            if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                newIndexZ = location.getIndices()[2].getDeltaIndex((doubleDeltaY > 0) ? 1 : -1);
            }
            
            if (newIndexY != null) {
                if (newIndexX != null) {
                    if (newIndexZ != null) {
                        result.add(new Location(location.getName(), newIndexY, newIndexX, newIndexZ));
                    }
                    result.add(new Location(location.getName(), newIndexY, newIndexX, location.getIndices()[2]));
                }
                if (newIndexZ != null) {
                    result.add(new Location(location.getName(), newIndexY, location.getIndices()[1], newIndexZ));
                }
                result.add(new Location(location.getName(), newIndexY, location.getIndices()[1], location.getIndices()[2]));
            }
            if (newIndexX != null) {
                if (newIndexZ != null) {
                    result.add(new Location(location.getName(), location.getIndices()[0], newIndexX, newIndexZ));
                }
                result.add(new Location(location.getName(), location.getIndices()[0], newIndexX, location.getIndices()[2]));
            }
            if (newIndexZ != null) {
                result.add(new Location(location.getName(), location.getIndices()[0], location.getIndices()[1], newIndexZ));
            }
            return result;
        }

        @Override
        protected boolean isValidSourceLocation(Location template, Location source, List<Compartment> compartments) {
            Compartment compartment = getCompartment(compartments, template.getName());
            if (compartment.getDimensions().length != source.getIndices().length 
                    || !template.getName().equals(source.getName())) {
                return false;
            }
            for (int index = 0; index < source.getIndices().length; index++) {
                CellIndexExpression sourceIndex = source.getIndices()[index];
                if (!sourceIndex.isFixed()) {
                    return false;
                }
            }
            return compartment.isValidVoxel(source);
        }

        @Override
        protected List<ChannelComponent> getChannelSubcomponents(List<Compartment> compartments) {
            return null;
        }
    }

    public static class RadialInComponent extends PredefinedChannelComponent {

        public static final String NAME = "RadialIn";
        
        public RadialInComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
            // TODO test dimensions
        }
        
        @Override
        public List<List<Location>> applyChannel(List<ChannelConstraint> constraints, List<Compartment> compartments) {
            
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            if (isValidSourceLocations(templateConstraints, constraints, compartments)) {

                // TODO constrain to single location at a time
                for (int templateIndex=0; templateIndex<templateConstraints.size(); templateIndex++) {
                    Location templateSourceLocation = templateConstraints.get(templateIndex).sourceLocation;
                    Location templateTargetLocation = templateConstraints.get(templateIndex).targetConstraint;
                    ChannelConstraint constraint = constraints.get(templateIndex);
                    Location sourceLocation = constraint.sourceLocation;
                    Location targetConstraint = constraint.targetConstraint;
                    
                    Compartment sourceCompartment = getCompartment(compartments, templateSourceLocation.getName());
                    Compartment targetCompartment = getCompartment(compartments, templateTargetLocation.getName());
        
                    if (sourceLocation.getIndices().length != sourceCompartment.getDimensions().length 
                            || !sourceLocation.getName().equals(templateSourceLocation.getName())) {
                        continue;
                    }
        
                    List<Location> newLocations = null;
                    if (sourceCompartment.getDimensions().length == 2) {
                        newLocations = getRadialLocations2D(sourceLocation, sourceCompartment);
                    }
                    else {
                        newLocations = getRadialLocations3D(sourceLocation, sourceCompartment);
                    }
                    
                    if (isNesting(sourceCompartment, targetCompartment)) {
                        newLocations = applyNestingOffsets(sourceCompartment, targetCompartment, newLocations);
                    }

                    for (Location location : newLocations) {
                        if (targetCompartment.isValidVoxel(location)) {
                            if (targetConstraint == null || targetConstraint.equals(location) || targetConstraint.isRefinement(location)) {
                                result.add(getList(location));
                            }
                        }
                    }
                }
            }
            removeMotionlessResults(constraints, result);
            return result;
        }
        
        private List<Location> getRadialLocations2D(Location location, Compartment compartment) {
            int doubleDeltaY = ((int) location.getIndices()[0].value) * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = ((int) location.getIndices()[1].value) * 2 - compartment.dimensions[1] + 1;
            
            CellIndexExpression newIndexX = null;
            CellIndexExpression newIndexY = null;
            
            List<Location> result = new ArrayList<Location>();
            result.add(location);
            
            if (Math.abs(doubleDeltaX) > Math.abs(doubleDeltaY)) {
                newIndexY = location.getIndices()[0];
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? -1 : 1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else if (Math.abs(doubleDeltaX) < Math.abs(doubleDeltaY)) {
                newIndexX = location.getIndices()[1];
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? -1 : 1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            else if (doubleDeltaX != 0) {
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? -1 : 1);
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? -1 : 1);
                result.add(new Location(location.getName(), newIndexY, newIndexX));
            }
            return result;
        }

        private List<Location> getRadialLocations3D(Location location, Compartment compartment) {
            int doubleDeltaY = ((int) location.getIndices()[0].value) * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = ((int) location.getIndices()[1].value) * 2 - compartment.dimensions[1] + 1;
            int doubleDeltaZ = ((int) location.getIndices()[2].value) * 2 - compartment.dimensions[2] + 1;
            
            List<Location> result = new ArrayList<Location>();
            result.add(location);

            int doubleDeltaMax = Math.max(Math.abs(doubleDeltaX), Math.max(Math.abs(doubleDeltaY), Math.abs(doubleDeltaZ)));
            if (doubleDeltaMax == 0) {
                return result;
            }

            CellIndexExpression newIndexX = location.getIndices()[1];
            CellIndexExpression newIndexY = location.getIndices()[0];
            CellIndexExpression newIndexZ = location.getIndices()[2];
            
            if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                newIndexX = location.getIndices()[1].getDeltaIndex((doubleDeltaX > 0) ? -1 : 1);
            }
            if (Math.abs(doubleDeltaY) == doubleDeltaMax) {
                newIndexY = location.getIndices()[0].getDeltaIndex((doubleDeltaY > 0) ? -1 : 1);
            }
            if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                newIndexZ = location.getIndices()[2].getDeltaIndex((doubleDeltaY > 0) ? -1 : 1);
            }
            result.add(new Location(location.getName(), newIndexY, newIndexX, newIndexZ));
            return result;
        }

        @Override
        protected boolean isValidSourceLocation(Location template, Location source, List<Compartment> compartments) {
            Compartment compartment = getCompartment(compartments, template.getName());
            if (compartment.getDimensions().length != source.getIndices().length 
                    || !template.getName().equals(source.getName())) {
                return false;
            }
            for (int index = 0; index < source.getIndices().length; index++) {
                CellIndexExpression sourceIndex = source.getIndices()[index];
                if (!sourceIndex.isFixed()) {
                    return false;
                }
            }
            return compartment.isValidVoxel(source);
        }

        @Override
        protected List<ChannelComponent> getChannelSubcomponents(List<Compartment> compartments) {
            return null;
        }
    }

    public static class LateralComponent extends PredefinedChannelComponent {

        public static final String NAME = "Lateral";
        
        public LateralComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
            // TODO test dimensions
        }
        
        @Override
        public List<List<Location>> applyChannel(List<ChannelConstraint> constraints, List<Compartment> compartments) {
            
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            if (isValidSourceLocations(templateConstraints, constraints, compartments)) {

                // TODO constrain to single location at a time
                for (int templateIndex=0; templateIndex<templateConstraints.size(); templateIndex++) {
                    Location templateSourceLocation = templateConstraints.get(templateIndex).sourceLocation;
                    Location templateTargetLocation = templateConstraints.get(templateIndex).targetConstraint;
                    ChannelConstraint constraint = constraints.get(templateIndex);
                    Location sourceLocation = constraint.sourceLocation;
                    Location targetConstraint = constraint.targetConstraint;
                    
                    Compartment sourceCompartment = getCompartment(compartments, templateSourceLocation.getName());
                    Compartment targetCompartment = getCompartment(compartments, templateTargetLocation.getName());
        
                    if (sourceLocation.getIndices().length != sourceCompartment.getDimensions().length 
                            || !sourceLocation.getName().equals(templateSourceLocation.getName())) {
                        continue;
                    }
        
                    List<Location> newLocations = null;
                    if (sourceCompartment.getDimensions().length == 2) {
                        newLocations = getLateralLocations2D(sourceLocation, sourceCompartment);
                    }
                    else {
                        newLocations = getLateralLocations3D(sourceLocation, sourceCompartment);
                    }
                    
                    if (isNesting(sourceCompartment, targetCompartment)) {
                        newLocations = applyNestingOffsets(sourceCompartment, targetCompartment, newLocations);
                    }

                    for (Location location : newLocations) {
                        if (targetCompartment.isValidVoxel(location)) {
                            if (targetConstraint == null || targetConstraint.equals(location) || targetConstraint.isRefinement(location)) {
                                result.add(getList(location));
                            }
                        }
                    }
                }
            }
            removeMotionlessResults(constraints, result);
            return result;
        }
        
        private List<Location> getLateralLocations2D(Location location, Compartment compartment) {
            float distanceToCentre = getDistanceToCentre(location, compartment);
            
            CellIndexExpression newIndexX = location.getIndices()[1].getDeltaIndex(-1);
            CellIndexExpression newIndexY = location.getIndices()[0].getDeltaIndex(-1);
            CellIndexExpression newIndexX2 = location.getIndices()[1].getDeltaIndex(1);
            CellIndexExpression newIndexY2 = location.getIndices()[0].getDeltaIndex(1);

            List<Location> result = new ArrayList<Location>();
            result.add(location);
            
            Location current = new Location(location.getName(), newIndexY, newIndexX);
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new Location(location.getName(), newIndexY, location.getIndices()[1]);
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new Location(location.getName(), newIndexY, newIndexX2);
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new Location(location.getName(), location.getIndices()[0], newIndexX);
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new Location(location.getName(), location.getIndices()[0], newIndexX2);
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new Location(location.getName(), newIndexY2, newIndexX);
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new Location(location.getName(), newIndexY2, location.getIndices()[1]);
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new Location(location.getName(), newIndexY2, newIndexX2);
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            return result;
        }

        private List<Location> getLateralLocations3D(Location location, Compartment compartment) {
            float distanceToCentre = getDistanceToCentre(location, compartment);
            
            CellIndexExpression[] xIndices = new CellIndexExpression[] {
                    location.getIndices()[1].getDeltaIndex(-1),
                    location.getIndices()[1],
                    location.getIndices()[1].getDeltaIndex(1),
            };
            CellIndexExpression[] yIndices = new CellIndexExpression[] {
                    location.getIndices()[0].getDeltaIndex(-1),
                    location.getIndices()[0],
                    location.getIndices()[0].getDeltaIndex(1),
            };
            CellIndexExpression[] zIndices = new CellIndexExpression[] {
                    location.getIndices()[2].getDeltaIndex(-1),
                    location.getIndices()[2],
                    location.getIndices()[2].getDeltaIndex(1),
            };
            
            List<Location> result = new ArrayList<Location>();
            result.add(location);

            for (CellIndexExpression yIndex : yIndices) {
                for (CellIndexExpression xIndex : xIndices) {
                    for (CellIndexExpression zIndex : zIndices) {
                        if (xIndex != location.getIndices()[1] || yIndex != location.getIndices()[0] || zIndex != location.getIndices()[2]) {
                            Location current = new Location(location.getName(), yIndex, xIndex, zIndex);
                            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                                result.add(current);
                            }
                        }
                    }
                }
            }
            return result;
        }

        private float getDistanceToCentre(Location location, Compartment compartment) {
            if (compartment.getDimensions().length == 2) {
                float doubleDeltaY = location.getIndices()[0].value - compartment.dimensions[0]/2f + 0.5f;
                float doubleDeltaX = location.getIndices()[1].value - compartment.dimensions[1]/2f + 0.5f;
                return (float) Math.sqrt(doubleDeltaX*doubleDeltaX + doubleDeltaY*doubleDeltaY);
            }
            float doubleDeltaY = location.getIndices()[0].value - compartment.dimensions[0]/2f + 0.5f;
            float doubleDeltaX = location.getIndices()[1].value - compartment.dimensions[1]/2f + 0.5f;
            float doubleDeltaZ = location.getIndices()[2].value - compartment.dimensions[2]/2f + 0.5f;
            return (float) Math.sqrt(doubleDeltaX*doubleDeltaX + doubleDeltaY*doubleDeltaY + doubleDeltaZ*doubleDeltaZ);
        }
        
        @Override
        protected boolean isValidSourceLocation(Location template, Location source, List<Compartment> compartments) {
            Compartment compartment = getCompartment(compartments, template.getName());
            if (compartment.getDimensions().length != source.getIndices().length 
                    || !template.getName().equals(source.getName())) {
                return false;
            }
            for (int index = 0; index < source.getIndices().length; index++) {
                CellIndexExpression sourceIndex = source.getIndices()[index];
                if (!sourceIndex.isFixed()) {
                    return false;
                }
            }
            return compartment.isValidVoxel(source);
        }

        @Override
        protected List<ChannelComponent> getChannelSubcomponents(List<Compartment> compartments) {
            return null;
        }
    }
    
    
    public static ChannelComponent createChannelComponent(String channelType, List<Location> sourceLocations,
            List<Location> targetLocations) {
        if (channelType == null) {
            return new ChannelComponent(sourceLocations, targetLocations);
        }
        if (EdgeNeighbourComponent.NAME.equals(channelType)) {
            return new EdgeNeighbourComponent(sourceLocations, targetLocations);
        }
        if (HexagonalComponent.NAME.equals(channelType)) {
            return new HexagonalComponent(sourceLocations, targetLocations);
        }
        if (NeighbourComponent.NAME.equals(channelType)) {
            return new NeighbourComponent(sourceLocations, targetLocations);
        }
        if (FaceNeighbourComponent.NAME.equals(channelType)) {
            return new FaceNeighbourComponent(sourceLocations, targetLocations);
        }
        if (RadialComponent.NAME.equals(channelType)) {
            return new RadialComponent(sourceLocations, targetLocations);
        }
        if (RadialOutComponent.NAME.equals(channelType)) {
            return new RadialOutComponent(sourceLocations, targetLocations);
        }
        if (RadialInComponent.NAME.equals(channelType)) {
            return new RadialInComponent(sourceLocations, targetLocations);
        }
        if (LateralComponent.NAME.equals(channelType)) {
            return new LateralComponent(sourceLocations, targetLocations);
        }
        throw new IllegalArgumentException("Unknown channel type: " + channelType);
    }

    public void validate(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        for (ChannelConstraint constraint : templateConstraints) {
            Compartment sourceCompartment = getCompartment(compartments, constraint.sourceLocation.getName());
            int dimensions = constraint.sourceLocation.getIndices().length;
            if (dimensions != 0 && dimensions != sourceCompartment.dimensions.length) {
                throw new IllegalStateException("Not a valid voxel for compartment '" + sourceCompartment + "'");
            }
            Compartment targetCompartment = getCompartment(compartments, constraint.targetConstraint.getName());
            dimensions = constraint.targetConstraint.getIndices().length;
            if (dimensions != 0 && dimensions != targetCompartment.dimensions.length) {
                throw new IllegalStateException("Not a valid voxel for compartment '" + targetCompartment + "'");
            }
            
            if (isNesting(sourceCompartment, targetCompartment)) {
                if (!isValidNesting(sourceCompartment, targetCompartment)) {
                    throw new IllegalStateException("Compartments not compatible for nesting: '" + sourceCompartment + "', '" + targetCompartment + "'");
                }
            }
            
        }
    }

    protected boolean isNesting(Compartment sourceCompartment, Compartment targetCompartment) {
        return channelType != null && sourceCompartment != targetCompartment;
    }

    private boolean isValidNesting(Compartment sourceCompartment, Compartment targetCompartment) {
        Compartment inner;
        Compartment outer;
        
        if (sourceCompartment.getDimensions()[0] < targetCompartment.getDimensions()[0]) {
            inner = sourceCompartment;
            outer = targetCompartment;
        }
        else {
            outer = sourceCompartment;
            inner = targetCompartment;
        }
        
        if (OpenRectangle.class == outer.getClass()) {
            if (Compartment.class == inner.getClass() && inner.dimensions.length == 2 || 
                    OpenRectangle.class == inner.getClass()) {
                return isCorrectNestedDimensions(inner, outer, 2);
            }
        }
        else if (OpenCuboid.class == outer.getClass()) {
            if (Compartment.class == inner.getClass() && inner.dimensions.length == 3 || 
                    OpenCuboid.class == inner.getClass()) {
                return isCorrectNestedDimensions(inner, outer, 3);
            }
        }
        else if (OpenCircle.class == outer.getClass()) {
            if (OpenCircle.class == inner.getClass() || SolidCircle.class == inner.getClass()) {
                return isCorrectNestedDimensions(inner, outer, 1);
            }
        }
        else if (OpenSphere.class == outer.getClass()) {
            if (OpenSphere.class == inner.getClass() || SolidSphere.class == inner.getClass()) {
                return isCorrectNestedDimensions(inner, outer, 1);
            }
        }
        else if (OpenCylinder.class == outer.getClass()) {
            if (OpenCylinder.class == inner.getClass() || SolidCylinder.class == inner.getClass()) {
                return isCorrectNestedDimensions(inner, outer, 3);
            }
        }
        else if (OpenSpine.class == outer.getClass()) {
            if (OpenSpine.class == inner.getClass() || SolidSpine.class == inner.getClass()) {
                return isCorrectNestedDimensions(inner, outer, 3) && 
                        ((Spine) inner).getCylinderDiameter() + 2*outer.getThickness() == ((Spine) outer).getCylinderDiameter();
            }
        }
        
        return false;
    }

    private boolean isCorrectNestedDimensions(Compartment inner, Compartment outer, int dimensionCount) {
        int thickness = outer.getThickness();
        
        for (int index=0; index < dimensionCount; index++) {
            if (inner.getDimensions()[index] + 2*thickness != outer.getDimensions()[index]) {
                return false;
            }
        }
        return true;
    }

    protected final List<Location> applyNestingOffsets(Compartment sourceCompartment, Compartment targetCompartment,
            List<Location> locations) {
        List<Location> result = new ArrayList<Location>();
        for (Location location : locations) {
            result.add(targetCompartment.getOriginLocation(sourceCompartment.getCentreLocation(location)));
        }
        return result;
    }

    protected final Location applyNestingOffsets(Compartment sourceCompartment, Compartment targetCompartment,
            Location location) {
        return targetCompartment.getOriginLocation(sourceCompartment.getCentreLocation(location));
    }


}