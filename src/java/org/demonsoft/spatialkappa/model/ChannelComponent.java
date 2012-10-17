package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getCompartment;
import static org.demonsoft.spatialkappa.model.Utils.getList;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.demonsoft.spatialkappa.model.VariableExpression.Type;

public class ChannelComponent {
    
    private static final List<List<ChannelConstraint>> NO_CONSTRAINTS = new ArrayList<List<ChannelConstraint>>();
    
    public final List<ChannelConstraint> templateConstraints = new ArrayList<ChannelConstraint>();
    
    // Constructor for unit tests
    public ChannelComponent(Location sourceLocation, Location targetLocation) {
        this(getList(sourceLocation), getList(targetLocation));
    }
    
    public ChannelComponent(List<Location> sourceLocations, List<Location> targetLocations) {
        if (sourceLocations == null || targetLocations == null) {
            throw new NullPointerException();
        }
        if (sourceLocations.size() == 0 || sourceLocations.size() != targetLocations.size()) {
            throw new IllegalArgumentException();
        }
        for (int index=0; index < sourceLocations.size(); index++) {
            templateConstraints.add(new ChannelConstraint(sourceLocations.get(index), targetLocations.get(index)));
        }
    }
    
    ChannelComponent(List<ChannelConstraint> constraints) {
        if (constraints == null) {
            throw new NullPointerException();
        }
        if (constraints.size() == 0) {
            throw new IllegalArgumentException();
        }
        templateConstraints.addAll(constraints);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(templateConstraints);
        return builder.toString();
    }

    
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
            
                        int[] targetIndices;
                        if (templateTargetLocation.isConcreteLocation()) {
                            targetIndices = Arrays.copyOf(templateTargetLocation.getFixedIndices(), 
                                    templateTargetLocation.getFixedIndices().length);
                        }
                        else {
                            targetIndices = new int[templateTargetLocation.getIndices().length];
                            for (int index = 0; index < templateTargetLocation.getIndices().length; index++) {
                                CellIndexExpression targetIndex = templateTargetLocation.getIndices()[index];
                                targetIndices[index] = targetIndex.evaluateIndex(variables);
                            }
                        }
                        
                        if (targetCompartment == null || targetCompartment.isValidVoxel(targetIndices)) {
                            Location targetLocation = new Location(templateTargetLocation.getName(), targetIndices);
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
            if (sourceCompartment.isValidVoxel(source.getFixedIndices()) && 
                    targetCompartment.isValidVoxel(target.getFixedIndices())) {
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
            if (sourceCompartment.isValidVoxel(concreteSource.getFixedIndices()) && 
                    targetCompartment.isValidVoxel(concreteTarget.getFixedIndices())) {
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
            return NO_CONSTRAINTS;
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
        if (!source.isConcreteLocation()) {
            return false;
        }
        
        Compartment compartment = getCompartment(compartments, template.getName());
        
        
        if (compartment.getDimensions().length != source.getDimensionCount() 
                || !template.getName().equals(source.getName())) {
            return false;
        }
        
        if (template.getDimensionCount() == compartment.getDimensions().length) {

            if (template.isConcreteLocation()) {
                for (int index = 0; index < source.getFixedIndices().length; index++) {
                    int sourceIndex = source.getFixedIndices()[index];
                    int templateIndex = template.getFixedIndices()[index];
                    if (templateIndex != sourceIndex) {
                        return false;
                    }
                }
            }
            else {
                for (int index = 0; index < source.getFixedIndices().length; index++) {
                    int sourceIndex = source.getFixedIndices()[index];
                    CellIndexExpression templateIndex = template.getIndices()[index];
                    if (templateIndex.isFixed()) {
                        if (templateIndex.evaluateIndex() != sourceIndex) {
                            return false;
                        }
                    }
                }
            }
        }
        return compartment.isValidVoxel(source.getFixedIndices());
    }
    
    private Map<String, Integer> getVariables(Location templateSourceLocation, Location sourceLocation) {
        Map<String, Integer> variables = new HashMap<String, Integer>();
        if (!templateSourceLocation.isConcreteLocation()) {
            for (int index = 0; index < templateSourceLocation.getIndices().length; index++) {
                CellIndexExpression sourceIndex = templateSourceLocation.getIndices()[index];
                if (sourceIndex.type == Type.VARIABLE_REFERENCE) {
                    int inputIndex = sourceLocation.getFixedIndices()[index];
                    variables.put(sourceIndex.reference.variableName, inputIndex);
                }
            }
        }
        return variables;
    }
    
    
    static abstract class PredefinedChannelComponent extends ChannelComponent {

        private final String channelType;

        public PredefinedChannelComponent(String channelType, List<Location> sourceLocations, List<Location> targetLocations) {
            super(sourceLocations, targetLocations);
            if (channelType == null) {
                throw new NullPointerException();
            }
            this.channelType = channelType;
            for (Location location : sourceLocations) {
                if (!location.isCompartment()) {
                    throw new IllegalArgumentException("Channel location must be compartment only: " + location);
                }
            }
            for (Location location : targetLocations) {
                if (!location.isCompartment()) {
                    throw new IllegalArgumentException("Channel location must be compartment only: " + location);
                }
            }
        }
        
        @Override
        public List<List<Location>> applyChannel(List<ChannelConstraint> constraints, List<Compartment> compartments) {
            
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            List<List<ChannelConstraint>> templateConstraintPermutations = permuteChannelConstraints(templateConstraints, constraints.size());
            for (List<ChannelConstraint> templateConstraintPermutation : templateConstraintPermutations) {
    
                if (isValidSourceLocations(templateConstraintPermutation, constraints, compartments)) {
                    
                    List<List<Location>> targetLocations = new ArrayList<List<Location>>();
                    boolean valid = true;
                    

                    for (int templateIndex=0; templateIndex<templateConstraintPermutation.size() && valid; templateIndex++) {
                        List<Location> currentTemplateTargetLocations = new ArrayList<Location>();
                        
                        ChannelConstraint constraint = constraints.get(templateIndex);
                        ChannelConstraint templateConstraint = templateConstraintPermutation.get(templateIndex);
                        Location templateTargetLocation = templateConstraint.targetConstraint;
                        Location targetConstraint = constraint.targetConstraint;
                        Location sourceLocation = constraint.sourceLocation;
    
                        if (templateTargetLocation == Location.FIXED_LOCATION) {
                            if (targetConstraint == null || targetConstraint.equals(sourceLocation) || targetConstraint.isRefinement(sourceLocation)) {
                                currentTemplateTargetLocations.add(sourceLocation);
                            }
                        }
                        else {
                            Location templateSourceLocation = templateConstraint.sourceLocation;
                            
                            Compartment sourceCompartment = getCompartment(compartments, templateSourceLocation.getName());
                            Compartment targetCompartment = getCompartment(compartments, templateTargetLocation.getName());

                            List<int[]> newLocations = null;
                            if (sourceCompartment.getDimensions().length == 2) {
                                newLocations = getNewLocations2D(sourceLocation.getFixedIndices(), sourceCompartment);
                            }
                            else {
                                newLocations = getNewLocations3D(sourceLocation.getFixedIndices(), sourceCompartment);
                            }
                            
                            if (isNesting(sourceCompartment, targetCompartment)) {
                                Compartment.translate(sourceCompartment, targetCompartment, newLocations);
                            }
                            
                            for (int[] voxel : newLocations) {
                                if (targetCompartment.isValidVoxel(voxel)) {
                                    Location location = new Location(targetCompartment.name, voxel);
                                    if (targetConstraint == null || targetConstraint.equals(location) || targetConstraint.isRefinement(location)) {
                                        currentTemplateTargetLocations.add(location);
                                    }
                                }
                            }
                        }
                        
                        if (currentTemplateTargetLocations.size() == 0) {
                            valid = false;
                            break;
                        }
                        
                        List<List<Location>> oldTargetLocations = targetLocations;
                        targetLocations = new ArrayList<List<Location>>();
                        if (oldTargetLocations.size() == 0) {
                            for (Location currentTemplateTargetLocation : currentTemplateTargetLocations) {
                                targetLocations.add(getList(currentTemplateTargetLocation));
                            }
                        }
                        else {
                            for (List<Location> previousTemplatesTargetLocations : oldTargetLocations) {
                                for (Location currentTemplateTargetLocation : currentTemplateTargetLocations) {
                                    List<Location> newTargetLocations = new ArrayList<Location>(previousTemplatesTargetLocations);
                                    newTargetLocations.add(currentTemplateTargetLocation);
                                    targetLocations.add(newTargetLocations);
                                }
                            }
                        }
                    }
                    if (valid) {
                        result.addAll(targetLocations);
                    }

                }
            }
            removeMotionlessResults(constraints, result);
            return result;
        }
        

        protected abstract List<int[]> getNewLocations2D(int[] inputIndices, Compartment compartment);
        
        protected abstract List<int[]> getNewLocations3D(int[] inputIndices, Compartment compartment);

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("(").append(channelType).append(") ").append(templateConstraints);
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
            PredefinedChannelComponent other = (PredefinedChannelComponent) obj;
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
        
        @Override
        public void validate(List<Compartment> compartments) {
            if (compartments == null) {
                throw new NullPointerException();
            }
            for (ChannelConstraint constraint : templateConstraints) {
                Compartment sourceCompartment = getCompartment(compartments, constraint.sourceLocation.getName());
                Compartment targetCompartment = getCompartment(compartments, constraint.targetConstraint.getName());
                
                if (!isValidCompartmentDimension(sourceCompartment.dimensions.length) ||
                        !isValidCompartmentDimension(targetCompartment.dimensions.length) ||
                        sourceCompartment.dimensions.length != targetCompartment.dimensions.length) {
                    throw new IllegalStateException("Component has wrong number of dimensions for channel");
                }
                
                if (isNesting(sourceCompartment, targetCompartment)) {
                    if (!isValidNesting(sourceCompartment, targetCompartment)) {
                        throw new IllegalStateException("Compartments not compatible for nesting: '" + 
                                sourceCompartment + "', '" + targetCompartment + "'");
                    }
                }
            }
        }

        protected abstract boolean isValidCompartmentDimension(int dimensions);

        private boolean isNesting(Compartment sourceCompartment, Compartment targetCompartment) {
            return sourceCompartment != targetCompartment;
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

    }
    
    public static class EdgeNeighbourComponent extends PredefinedChannelComponent {

        public static final String NAME = "EdgeNeighbour";
        
        public EdgeNeighbourComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }
        
        @Override
        protected List<int[]> getNewLocations2D(int[] location, Compartment compartment) {
            List<int[]> result = new ArrayList<int[]>();
            
            result.add(new int[] {location[0] - 1, location[1]});
            result.add(new int[] {location[0], location[1] - 1});
            result.add(new int[] {location[0], location[1]});
            result.add(new int[] {location[0], location[1] + 1});
            result.add(new int[] {location[0] + 1, location[1]});

            return result;
        }

        @Override
        protected List<int[]> getNewLocations3D(int[] location, Compartment compartment) {
            throw new IllegalStateException();
        }

        @Override
        protected boolean isValidCompartmentDimension(int dimensions) {
            return dimensions == 2;
        }

    }
    
    public static class NeighbourComponent extends PredefinedChannelComponent {

        public static final String NAME = "Neighbour";

        public NeighbourComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }

        @Override
        protected List<int[]> getNewLocations2D(int[] location, Compartment compartment) {
            List<int[]> result = new ArrayList<int[]>();
            
            result.add(new int[] {location[0] - 1, location[1] - 1});
            result.add(new int[] {location[0] - 1, location[1]});
            result.add(new int[] {location[0] - 1, location[1] + 1});
            result.add(new int[] {location[0], location[1] - 1});
            result.add(new int[] {location[0], location[1]});
            result.add(new int[] {location[0], location[1] + 1});
            result.add(new int[] {location[0] + 1, location[1] - 1});
            result.add(new int[] {location[0] + 1, location[1]});
            result.add(new int[] {location[0] + 1, location[1] + 1});

            return result;
        }

        @Override
        protected List<int[]> getNewLocations3D(int[] location, Compartment compartment) {
            List<int[]> result = new ArrayList<int[]>();

            result.add(new int[] {location[0] - 1, location[1] - 1, location[2] - 1});
            result.add(new int[] {location[0] - 1, location[1] - 1, location[2]});
            result.add(new int[] {location[0] - 1, location[1] - 1, location[2] + 1});
            result.add(new int[] {location[0] - 1, location[1], location[2] - 1});
            result.add(new int[] {location[0] - 1, location[1], location[2]});
            result.add(new int[] {location[0] - 1, location[1], location[2] + 1});
            result.add(new int[] {location[0] - 1, location[1] + 1, location[2] - 1});
            result.add(new int[] {location[0] - 1, location[1] + 1, location[2]});
            result.add(new int[] {location[0] - 1, location[1] + 1, location[2] + 1});

            result.add(new int[] {location[0], location[1] - 1, location[2] - 1});
            result.add(new int[] {location[0], location[1] - 1, location[2]});
            result.add(new int[] {location[0], location[1] - 1, location[2] + 1});
            result.add(new int[] {location[0], location[1], location[2] - 1});
            result.add(new int[] {location[0], location[1], location[2]});
            result.add(new int[] {location[0], location[1], location[2] + 1});
            result.add(new int[] {location[0], location[1] + 1, location[2] - 1});
            result.add(new int[] {location[0], location[1] + 1, location[2]});
            result.add(new int[] {location[0], location[1] + 1, location[2] + 1});

            result.add(new int[] {location[0] + 1, location[1] - 1, location[2] - 1});
            result.add(new int[] {location[0] + 1, location[1] - 1, location[2]});
            result.add(new int[] {location[0] + 1, location[1] - 1, location[2] + 1});
            result.add(new int[] {location[0] + 1, location[1], location[2] - 1});
            result.add(new int[] {location[0] + 1, location[1], location[2]});
            result.add(new int[] {location[0] + 1, location[1], location[2] + 1});
            result.add(new int[] {location[0] + 1, location[1] + 1, location[2] - 1});
            result.add(new int[] {location[0] + 1, location[1] + 1, location[2]});
            result.add(new int[] {location[0] + 1, location[1] + 1, location[2] + 1});

            return result;
        }
        
        @Override
        protected boolean isValidCompartmentDimension(int dimensions) {
            return dimensions == 2 || dimensions == 3;
        }
    }
    
    public static class HexagonalComponent extends PredefinedChannelComponent {

        public static final String NAME = "Hexagonal";

        public HexagonalComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }
        @Override
        protected List<int[]> getNewLocations2D(int[] location, Compartment compartment) {
            List<int[]> result = new ArrayList<int[]>();
            
            int x = location[0];
            int y = location[1];
            
            result.add(new int[] {x-1, y-1 + ((x-1)%2)*2});
            result.add(new int[] {x-1, y});
            result.add(new int[] {x, y-1});
            result.add(new int[] {x, y});
            result.add(new int[] {x, y+1});
            result.add(new int[] {x+1, y});
            result.add(new int[] {x+1, y+1 - (x%2)*2});


            return result;
        }

        @Override
        protected List<int[]> getNewLocations3D(int[] location, Compartment compartment) {
            throw new IllegalStateException();
        }

        @Override
        protected boolean isValidCompartmentDimension(int dimensions) {
            return dimensions == 2;
        }

    }
    
    public static class FaceNeighbourComponent extends PredefinedChannelComponent {

        public static final String NAME = "FaceNeighbour";
        
        public FaceNeighbourComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }
        
        
        @Override
        protected List<int[]> getNewLocations2D(int[] location, Compartment compartment) {
            throw new IllegalStateException();
        }

        @Override
        protected List<int[]> getNewLocations3D(int[] location, Compartment compartment) {
            List<int[]> result = new ArrayList<int[]>();

            result.add(new int[] {location[0] - 1, location[1], location[2]});

            result.add(new int[] {location[0], location[1] - 1, location[2]});
            result.add(new int[] {location[0], location[1], location[2] - 1});
            result.add(new int[] {location[0], location[1], location[2]});
            result.add(new int[] {location[0], location[1], location[2] + 1});
            result.add(new int[] {location[0], location[1] + 1, location[2]});

            result.add(new int[] {location[0] + 1, location[1], location[2]});

            return result;
        }

        @Override
        protected boolean isValidCompartmentDimension(int dimensions) {
            return dimensions == 3;
        }
    }

    public static class RadialComponent extends PredefinedChannelComponent {

        public static final String NAME = "Radial";
        
        public RadialComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }
        
        @Override
        protected List<int[]> getNewLocations2D(int[] location, Compartment compartment) {
            int doubleDeltaY = location[0] * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = location[1] * 2 - compartment.dimensions[1] + 1;
            
            int newIndexX;
            int newIndexY;
            
            List<int[]> result = new ArrayList<int[]>();
            result.add(new int[] {location[0], location[1]});
            
            // Direction out 
            
            if (Math.abs(doubleDeltaX) > Math.abs(doubleDeltaY)) {
                newIndexX = location[1] + ((doubleDeltaX > 0) ? 1 : -1);
                result.add(new int[] {location[0], newIndexX});
            }
            else if (Math.abs(doubleDeltaX) < Math.abs(doubleDeltaY)) {
                newIndexY = location[0] + ((doubleDeltaY > 0) ? 1 : -1);
                result.add(new int[] {newIndexY, location[1]});
            }
            else if (doubleDeltaX != 0) {
                newIndexX = location[1] + ((doubleDeltaX > 0) ? 1 : -1);
                newIndexY = location[0] + ((doubleDeltaY > 0) ? 1 : -1);
                result.add(new int[] {newIndexY, location[1]});
                result.add(new int[] {location[0], newIndexX});
                result.add(new int[] {newIndexY, newIndexX});
            }
            else {
                result.add(new int[] {location[0] - 1, location[1] - 1});
                result.add(new int[] {location[0] - 1, location[1]});
                result.add(new int[] {location[0] - 1, location[1] + 1});
                result.add(new int[] {location[0], location[1] - 1});
                result.add(new int[] {location[0], location[1] + 1});
                result.add(new int[] {location[0] + 1, location[1] - 1});
                result.add(new int[] {location[0] + 1, location[1]});
                result.add(new int[] {location[0] + 1, location[1] + 1});
            }
            
            // Direction in 
            
            if (Math.abs(doubleDeltaX) > Math.abs(doubleDeltaY)) {
                result.add(new int[] {location[0], location[1] + ((doubleDeltaX > 0) ? -1 : 1)});
            }
            else if (Math.abs(doubleDeltaX) < Math.abs(doubleDeltaY)) {
                result.add(new int[] {location[0] + ((doubleDeltaY > 0) ? -1 : 1), location[1]});
            }
            else if (doubleDeltaX != 0) {
                result.add(new int[] {location[0] + ((doubleDeltaY > 0) ? -1 : 1), location[1] + ((doubleDeltaX > 0) ? -1 : 1)});
            }
            return result;
        }

        @Override
        protected List<int[]> getNewLocations3D(int[] location, Compartment compartment) {
            int doubleDeltaY = location[0] * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = location[1] * 2 - compartment.dimensions[1] + 1;
            int doubleDeltaZ = location[2] * 2 - compartment.dimensions[2] + 1;
            
            List<int[]> result = new ArrayList<int[]>();
            result.add(new int[] {location[0], location[1], location[2]});

            // Direction out 

            int doubleDeltaMax = Math.max(Math.abs(doubleDeltaX), Math.max(Math.abs(doubleDeltaY), Math.abs(doubleDeltaZ)));
            if (doubleDeltaMax == 0) {
                int[] xIndices = new int[] {
                        location[1] - 1,
                        location[1],
                        location[1] + 1,
                };
                int[] yIndices = new int[] {
                        location[0] - 1,
                        location[0],
                        location[0] + 1,
                };
                int[] zIndices = new int[] {
                        location[2] - 1,
                        location[2],
                        location[2] + 1,
                };
                
                for (int yIndex : yIndices) {
                    for (int xIndex : xIndices) {
                        for (int zIndex : zIndices) {
                            if (xIndex != location[1] || yIndex != location[0] || zIndex != location[2]) {
                                result.add(new int[] {yIndex, xIndex, zIndex});
                            }
                        }
                    }
                }
                
                return result;
            }

            int newIndexX = location[1] + ((doubleDeltaX > 0) ? 1 : -1);
            int newIndexY = location[0] + ((doubleDeltaY > 0) ? 1 : -1);
            int newIndexZ = location[2] + ((doubleDeltaZ > 0) ? 1 : -1);
            
            if (Math.abs(doubleDeltaY) == doubleDeltaMax) {
                if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                    if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                        result.add(new int[] {newIndexY, newIndexX, newIndexZ});
                    }
                    result.add(new int[] {newIndexY, newIndexX, location[2]});
                }
                if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                    result.add(new int[] {newIndexY, location[1], newIndexZ});
                }
                result.add(new int[] {newIndexY, location[1], location[2]});
            }
            if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                    result.add(new int[] {location[0], newIndexX, newIndexZ});
                }
                result.add(new int[] {location[0], newIndexX, location[2]});
            }
            if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                result.add(new int[] {location[0], location[1], newIndexZ});
            }
            
            // Direction in 
            
            newIndexX = location[1];
            newIndexY = location[0];
            newIndexZ = location[2];
            
            if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                newIndexX = location[1] + ((doubleDeltaX > 0) ? -1 : 1);
            }
            if (Math.abs(doubleDeltaY) == doubleDeltaMax) {
                newIndexY = location[0] + ((doubleDeltaY > 0) ? -1 : 1);
            }
            if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                newIndexZ = location[2] + ((doubleDeltaY > 0) ? -1 : 1);
            }
            result.add(new int[] {newIndexY, newIndexX, newIndexZ});
            
            return result;
        }
        
        @Override
        protected boolean isValidCompartmentDimension(int dimensions) {
            return dimensions == 2 || dimensions == 3;
        }
    }

    public static class RadialOutComponent extends PredefinedChannelComponent {

        public static final String NAME = "RadialOut";
        
        public RadialOutComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }
        
        @Override
        protected List<int[]> getNewLocations2D(int[] location, Compartment compartment) {
            int doubleDeltaY = location[0] * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = location[1] * 2 - compartment.dimensions[1] + 1;
            
            int newIndexX;
            int newIndexY;
            
            List<int[]> result = new ArrayList<int[]>();
            result.add(new int[] {location[0], location[1]});
            
            // Direction out 
            
            if (Math.abs(doubleDeltaX) > Math.abs(doubleDeltaY)) {
                newIndexX = location[1] + ((doubleDeltaX > 0) ? 1 : -1);
                result.add(new int[] {location[0], newIndexX});
            }
            else if (Math.abs(doubleDeltaX) < Math.abs(doubleDeltaY)) {
                newIndexY = location[0] + ((doubleDeltaY > 0) ? 1 : -1);
                result.add(new int[] {newIndexY, location[1]});
            }
            else if (doubleDeltaX != 0) {
                newIndexX = location[1] + ((doubleDeltaX > 0) ? 1 : -1);
                newIndexY = location[0] + ((doubleDeltaY > 0) ? 1 : -1);
                result.add(new int[] {newIndexY, location[1]});
                result.add(new int[] {location[0], newIndexX});
                result.add(new int[] {newIndexY, newIndexX});
            }
            else {
                result.add(new int[] {location[0] - 1, location[1] - 1});
                result.add(new int[] {location[0] - 1, location[1]});
                result.add(new int[] {location[0] - 1, location[1] + 1});
                result.add(new int[] {location[0], location[1] - 1});
                result.add(new int[] {location[0], location[1] + 1});
                result.add(new int[] {location[0] + 1, location[1] - 1});
                result.add(new int[] {location[0] + 1, location[1]});
                result.add(new int[] {location[0] + 1, location[1] + 1});
            }

            return result;
        }

        @Override
        protected List<int[]> getNewLocations3D(int[] location, Compartment compartment) {
            int doubleDeltaY = location[0] * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = location[1] * 2 - compartment.dimensions[1] + 1;
            int doubleDeltaZ = location[2] * 2 - compartment.dimensions[2] + 1;
            
            List<int[]> result = new ArrayList<int[]>();
            result.add(new int[] {location[0], location[1], location[2]});

            int doubleDeltaMax = Math.max(Math.abs(doubleDeltaX), Math.max(Math.abs(doubleDeltaY), Math.abs(doubleDeltaZ)));
            if (doubleDeltaMax == 0) {
                int[] xIndices = new int[] {
                        location[1] - 1,
                        location[1],
                        location[1] + 1,
                };
                int[] yIndices = new int[] {
                        location[0] - 1,
                        location[0],
                        location[0] + 1,
                };
                int[] zIndices = new int[] {
                        location[2] - 1,
                        location[2],
                        location[2] + 1,
                };
                
                for (int yIndex : yIndices) {
                    for (int xIndex : xIndices) {
                        for (int zIndex : zIndices) {
                            if (xIndex != location[1] || yIndex != location[0] || zIndex != location[2]) {
                                result.add(new int[] {yIndex, xIndex, zIndex});
                            }
                        }
                    }
                }
                
                return result;
            }

            int newIndexX = location[1] + ((doubleDeltaX > 0) ? 1 : -1);
            int newIndexY = location[0] + ((doubleDeltaY > 0) ? 1 : -1);
            int newIndexZ = location[2] + ((doubleDeltaZ > 0) ? 1 : -1);
            
            if (Math.abs(doubleDeltaY) == doubleDeltaMax) {
                if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                    if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                        result.add(new int[] {newIndexY, newIndexX, newIndexZ});
                    }
                    result.add(new int[] {newIndexY, newIndexX, location[2]});
                }
                if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                    result.add(new int[] {newIndexY, location[1], newIndexZ});
                }
                result.add(new int[] {newIndexY, location[1], location[2]});
            }
            if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                    result.add(new int[] {location[0], newIndexX, newIndexZ});
                }
                result.add(new int[] {location[0], newIndexX, location[2]});
            }
            if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                result.add(new int[] {location[0], location[1], newIndexZ});
            }
            return result;
        }
        
        @Override
        protected boolean isValidCompartmentDimension(int dimensions) {
            return dimensions == 2 || dimensions == 3;
        }
    }

    public static class RadialInComponent extends PredefinedChannelComponent {

        public static final String NAME = "RadialIn";
        
        public RadialInComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }
        
        @Override
        protected List<int[]> getNewLocations2D(int[] location, Compartment compartment) {
            int doubleDeltaY = location[0] * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = location[1] * 2 - compartment.dimensions[1] + 1;
            
            List<int[]> result = new ArrayList<int[]>();
            result.add(new int[] {location[0], location[1]});
            
            if (Math.abs(doubleDeltaX) > Math.abs(doubleDeltaY)) {
                result.add(new int[] {location[0], location[1] + ((doubleDeltaX > 0) ? -1 : 1)});
            }
            else if (Math.abs(doubleDeltaX) < Math.abs(doubleDeltaY)) {
                result.add(new int[] {location[0] + ((doubleDeltaY > 0) ? -1 : 1), location[1]});
            }
            else if (doubleDeltaX != 0) {
                result.add(new int[] {location[0] + ((doubleDeltaY > 0) ? -1 : 1), location[1] + ((doubleDeltaX > 0) ? -1 : 1)});
            }
            return result;
        }

        @Override
        protected List<int[]> getNewLocations3D(int[] location, Compartment compartment) {
            int doubleDeltaY = location[0] * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = location[1] * 2 - compartment.dimensions[1] + 1;
            int doubleDeltaZ = location[2] * 2 - compartment.dimensions[2] + 1;
            
            List<int[]> result = new ArrayList<int[]>();
            result.add(new int[] {location[0], location[1], location[2]});

            int doubleDeltaMax = Math.max(Math.abs(doubleDeltaX), Math.max(Math.abs(doubleDeltaY), Math.abs(doubleDeltaZ)));
            if (doubleDeltaMax == 0) {
                return result;
            }

            int newIndexX = location[1];
            int newIndexY = location[0];
            int newIndexZ = location[2];
            
            if (Math.abs(doubleDeltaX) == doubleDeltaMax) {
                newIndexX = location[1] + ((doubleDeltaX > 0) ? -1 : 1);
            }
            if (Math.abs(doubleDeltaY) == doubleDeltaMax) {
                newIndexY = location[0] + ((doubleDeltaY > 0) ? -1 : 1);
            }
            if (Math.abs(doubleDeltaZ) == doubleDeltaMax) {
                newIndexZ = location[2] + ((doubleDeltaY > 0) ? -1 : 1);
            }
            result.add(new int[] {newIndexY, newIndexX, newIndexZ});
            return result;
        }
        
        @Override
        protected boolean isValidCompartmentDimension(int dimensions) {
            return dimensions == 2 || dimensions == 3;
        }
    }

    public static class LateralComponent extends PredefinedChannelComponent {

        public static final String NAME = "Lateral";
        
        public LateralComponent(List<Location> sourceLocations, List<Location> targetLocations) {
            super(NAME, sourceLocations, targetLocations);
        }
        
        @Override
        protected List<int[]> getNewLocations2D(int[] location, Compartment compartment) {
            float distanceToCentre = getDistanceToCentre(location, compartment);
            
            List<int[]> result = new ArrayList<int[]>();
            result.add(new int[] {location[0], location[1]});
            
            int[] current = new int[] {location[0] - 1, location[1] - 1};
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new int[] {location[0] - 1, location[1]};
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new int[] {location[0] - 1, location[1] + 1};
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new int[] {location[0], location[1] - 1};
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new int[] {location[0], location[1] + 1};
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new int[] {location[0] + 1, location[1] - 1};
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new int[] {location[0] + 1, location[1]};
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            current = new int[] {location[0] + 1, location[1] + 1};
            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                result.add(current);
            }
            return result;
        }

        @Override
        protected List<int[]> getNewLocations3D(int[] location, Compartment compartment) {
            float distanceToCentre = getDistanceToCentre(location, compartment);
            
            int[] xIndices = new int[] {
                    location[1] - 1,
                    location[1],
                    location[1] + 1,
            };
            int[] yIndices = new int[] {
                    location[0] - 1,
                    location[0],
                    location[0] + 1,
            };
            int[] zIndices = new int[] {
                    location[2] - 1,
                    location[2],
                    location[2] + 1,
            };
            
            List<int[]> result = new ArrayList<int[]>();
            result.add(new int[] {location[0], location[1], location[2]});

            for (int yIndex : yIndices) {
                for (int xIndex : xIndices) {
                    for (int zIndex : zIndices) {
                        if (xIndex != location[1] || yIndex != location[0] || zIndex != location[2]) {
                            int[] current = new int[] {yIndex, xIndex, zIndex};
                            if (Math.abs(distanceToCentre - getDistanceToCentre(current, compartment)) < 0.5f) {
                                result.add(current);
                            }
                        }
                    }
                }
            }
            return result;
        }

        private float getDistanceToCentre(int[] location, Compartment compartment) {
            float doubleDeltaY = location[0] - compartment.dimensions[0]/2f + 0.5f;
            float doubleDeltaX = location[1] - compartment.dimensions[1]/2f + 0.5f;
            if (compartment.getDimensions().length == 2) {
                return (float) Math.sqrt(doubleDeltaX*doubleDeltaX + doubleDeltaY*doubleDeltaY);
            }
            float doubleDeltaZ = location[2] - compartment.dimensions[2]/2f + 0.5f;
            return (float) Math.sqrt(doubleDeltaX*doubleDeltaX + doubleDeltaY*doubleDeltaY + doubleDeltaZ*doubleDeltaZ);
        }
        
        @Override
        protected boolean isValidCompartmentDimension(int dimensions) {
            return dimensions == 2 || dimensions == 3;
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
            Location location = constraint.sourceLocation;
            int dimensions = location.getDimensionCount();
            if (dimensions != 0 && dimensions != sourceCompartment.dimensions.length) {
                throw new IllegalStateException("Not a valid voxel for compartment '" + sourceCompartment + "'");
            }
            Compartment targetCompartment = getCompartment(compartments, constraint.targetConstraint.getName());
            location = constraint.targetConstraint;
            dimensions = location.getDimensionCount();
            if (dimensions != 0 && dimensions != targetCompartment.dimensions.length) {
                throw new IllegalStateException("Not a valid voxel for compartment '" + targetCompartment + "'");
            }
        }
    }

}