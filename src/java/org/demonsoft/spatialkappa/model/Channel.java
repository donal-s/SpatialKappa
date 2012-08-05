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
import java.util.Map;

import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableExpression.Type;

public class Channel {

    private final String name;
    private final List<ChannelComponent> channelComponents = new ArrayList<ChannelComponent>();

    // Constructor for unit tests
    public Channel(String name, Location sourceReference, Location targetReference) {
        this(name);
        if (sourceReference == null || targetReference == null) {
            throw new NullPointerException();
        }
        addChannelComponent(null, getList(sourceReference), getList(targetReference));
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

    public void addChannelComponent(String channelType, List<Location> sourceLocations, List<Location> targetLocations) {
        channelComponents.add(createChannelComponent(channelType, sourceLocations, targetLocations));
    }

    ChannelComponent createChannelComponent(String channelType, List<Location> sourceLocations,
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(": ");
        if (channelComponents.size() == 1) {
            ChannelComponent component = channelComponents.get(0);
            builder.append(component);
        }
        else {
            boolean first = true;
            for (ChannelComponent component : channelComponents) {
                if (!first) {
                    builder.append(" + ");
                }
                builder.append("(").append(component).append(")");
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
            for (ChannelComponent component : channelComponents) {
                result.addAll(component.getCellReferencePairs(compartments));
            }
        }
        return result.toArray(new Location[result.size()][]);
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
        for (ChannelComponent component : channelComponents) {
            result.addAll(component.applyChannel(sourceLocations, targetConstraints, compartments));
        }
        return result;
    }

    public void validate(List<Compartment> compartments) {
        if (compartments == null) {
            throw new NullPointerException();
        }
        if (channelComponents.size() == 0) {
            throw new IllegalStateException("No location pairs defined");
        }
        for (ChannelComponent component : channelComponents) {
            for (Location location : component.templateSourceLocations) {
                getCompartment(compartments, location.getName());
            }            
            for (Location location : component.templateTargetLocations) {
                getCompartment(compartments, location.getName());
            }
        }
    }

    public boolean isValidLinkChannel() {
        for (ChannelComponent component : channelComponents) {
            if (component.templateSourceLocations.size() > 1) {
                return false;
            }
        }
        return true;
    }
    
    public static class ChannelComponent {
        public final String channelType;
        public final List<Location> templateSourceLocations;
        public final List<Location> templateTargetLocations;
        
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
            this.templateSourceLocations = sourceLocations;
            this.templateTargetLocations = targetLocations;
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (channelType != null) {
                builder.append("(").append(channelType).append(") ");
            }
            builder.append(templateSourceLocations).append(" -> ");
            builder.append(templateTargetLocations);
            return builder.toString();
        }

        
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((channelType == null) ? 0 : channelType.hashCode());
            result = prime * result + ((templateSourceLocations == null) ? 0 : templateSourceLocations.hashCode());
            result = prime * result + ((templateTargetLocations == null) ? 0 : templateTargetLocations.hashCode());
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
            if (templateSourceLocations == null) {
                if (other.templateSourceLocations != null)
                    return false;
            }
            else if (!templateSourceLocations.equals(other.templateSourceLocations))
                return false;
            if (templateTargetLocations == null) {
                if (other.templateTargetLocations != null)
                    return false;
            }
            else if (!templateTargetLocations.equals(other.templateTargetLocations))
                return false;
            return true;
        }

        public List<List<Location>> applyChannel(List<Location> sourceLocations, List<Location> targetConstraints, List<Compartment> compartments) {
            List<List<Location>> templateSourcePermutations = new ArrayList<List<Location>>();
            List<List<Location>> templateTargetPermutations = new ArrayList<List<Location>>();
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            permuteLocations(templateSourceLocations, templateTargetLocations, templateSourcePermutations, templateTargetPermutations);
            
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
                        
                        Compartment targetCompartment = getCompartment(compartments, templateTargetLocation.getName());
            
                        if (sourceLocation.getIndices().length != templateSourceLocation.getIndices().length 
                                || !sourceLocation.getName().equals(templateSourceLocation.getName())) {
                            continue;
                        }
            
                        Map<String, Integer> variables = getVariables(templateSourceLocation, sourceLocation);
            
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
                            if (targetCompartment.isValidVoxel(targetLocation)) {
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
            return result;
        }
        
        public List<Location[]> getCellReferencePairs(List<Compartment> compartments) {
            List<Location[]> result = new ArrayList<Location[]>();
            Location source = templateSourceLocations.get(0);
            Location target = templateTargetLocations.get(0);

            Compartment sourceCompartment = getCompartment(compartments, source.getName());
            Compartment targetCompartment = getCompartment(compartments, target.getName());
    
            if (!source.isVoxel(sourceCompartment) || !target.isVoxel(targetCompartment)) {
                throw new IllegalArgumentException();
            }
            if (isConcreteLink(source, target)) {
                if (sourceCompartment.isValidVoxel(source) && targetCompartment.isValidVoxel(target)) {
                    result.add(new Location[] { source, target });
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

        private void getCellReferencePairs(List<Location[]> result, Object[][] variableRanges, Location source, Location target, 
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
                    result.add(new Location[] { concreteSource, concreteTarget });
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

        private List<List<Location>> permuteLocations(List<Location> locations) {
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
    }
    
    private static abstract class PredefinedChannelComponent extends ChannelComponent {

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
        public List<List<Location>> applyChannel(List<Location> sourceLocations, List<Location> targetConstraints, List<Compartment> compartments) {
            List<ChannelComponent> channelSubcomponents = getChannelSubcomponents(compartments);
            List<List<Location>> result = new ArrayList<List<Location>>();
            for (ChannelComponent component : channelSubcomponents) {
                result.addAll(component.applyChannel(sourceLocations, targetConstraints, compartments));
            }
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
            
            List<List<CellIndexExpression>> targetIndices = new ArrayList<List<CellIndexExpression>>();
            targetIndices.add(getList(INDEX_X_MINUS_1, INDEX_Y));
            targetIndices.add(getList(INDEX_X_PLUS_1, INDEX_Y));
            targetIndices.add(getList(INDEX_X, INDEX_Y_MINUS_1));
            targetIndices.add(getList(INDEX_X, INDEX_Y_PLUS_1));
            
            List<ChannelComponent> result = new ArrayList<ChannelComponent>();
            
            for (int index = 0; index < sourceIndices.size(); index++) {
                List<CellIndexExpression> currentSourceIndices = sourceIndices.get(index);
                List<CellIndexExpression> currentTargetIndices = targetIndices.get(index);
                
                List<Location> currentSourceLocations = new ArrayList<Location>();
                for (Location location : templateSourceLocations) {
                    currentSourceLocations.add(new Location(location.getName(), currentSourceIndices));
                }
                List<Location> currentTargetLocations = new ArrayList<Location>();
                for (Location location : templateTargetLocations) {
                    currentTargetLocations.add(new Location(location.getName(), currentTargetIndices));
                }
                result.add(new ChannelComponent(currentSourceLocations, currentTargetLocations));
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
            Compartment compartment = getCompartment(compartments, templateSourceLocations.get(0).getName());
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
                for (int i=0; i<26; i++) {
                    sourceIndices.add(getList(INDEX_X, INDEX_Y, INDEX_Z));
                }
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
            
            List<ChannelComponent> result = new ArrayList<ChannelComponent>();
            
            for (int index = 0; index < sourceIndices.size(); index++) {
                List<CellIndexExpression> currentSourceIndices = sourceIndices.get(index);
                List<CellIndexExpression> currentTargetIndices = targetIndices.get(index);
                
                List<Location> currentSourceLocations = new ArrayList<Location>();
                for (Location location : templateSourceLocations) {
                    currentSourceLocations.add(new Location(location.getName(), currentSourceIndices));
                }
                List<Location> currentTargetLocations = new ArrayList<Location>();
                for (Location location : templateTargetLocations) {
                    currentTargetLocations.add(new Location(location.getName(), currentTargetIndices));
                }
                result.add(new ChannelComponent(currentSourceLocations, currentTargetLocations));
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
            
            List<List<CellIndexExpression>> targetIndices = new ArrayList<List<CellIndexExpression>>();
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
                for (Location location : templateSourceLocations) {
                    currentSourceLocations.add(new Location(location.getName(), currentSourceIndices));
                }
                List<Location> currentTargetLocations = new ArrayList<Location>();
                for (Location location : templateTargetLocations) {
                    currentTargetLocations.add(new Location(location.getName(), currentTargetIndices));
                }
                result.add(new ChannelComponent(currentSourceLocations, currentTargetLocations));
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
            
            List<List<CellIndexExpression>> targetIndices = new ArrayList<List<CellIndexExpression>>();
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
                for (Location location : templateSourceLocations) {
                    currentSourceLocations.add(new Location(location.getName(), currentSourceIndices));
                }
                List<Location> currentTargetLocations = new ArrayList<Location>();
                for (Location location : templateTargetLocations) {
                    currentTargetLocations.add(new Location(location.getName(), currentTargetIndices));
                }
                result.add(new ChannelComponent(currentSourceLocations, currentTargetLocations));
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
        public List<List<Location>> applyChannel(List<Location> sourceLocations, List<Location> targetConstraints,
                List<Compartment> compartments) {
            
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            if (isValidSourceLocations(templateSourceLocations, sourceLocations, compartments)) {

                // TODO constrain to single location at a time
                for (int templateIndex=0; templateIndex<templateSourceLocations.size(); templateIndex++) {
                    Location templateSourceLocation = templateSourceLocations.get(templateIndex);
                    Location templateTargetLocation = templateTargetLocations.get(templateIndex);
                    Location sourceLocation = sourceLocations.get(templateIndex);
                    Location targetConstraint = targetConstraints.get(templateIndex);
                    
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
                    
                    for (Location location : newLocations) {
                        if (targetCompartment.isValidVoxel(location)) {
                            if (targetConstraint == null || targetConstraint.equals(location) || targetConstraint.isRefinement(location)) {
                                result.add(getList(location));
                            }
                        }
                    }
                }
            }
            return result;
        }
        
        private List<Location> getRadialLocations2D(Location location, Compartment compartment) {
            int doubleDeltaY = ((int) location.getIndices()[0].value) * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = ((int) location.getIndices()[1].value) * 2 - compartment.dimensions[1] + 1;
            
            CellIndexExpression newIndexX = null;
            CellIndexExpression newIndexY = null;
            
            List<Location> result = new ArrayList<Location>();
            
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
        public List<List<Location>> applyChannel(List<Location> sourceLocations, List<Location> targetConstraints,
                List<Compartment> compartments) {
            
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            if (isValidSourceLocations(templateSourceLocations, sourceLocations, compartments)) {

                // TODO constrain to single location at a time
                for (int templateIndex=0; templateIndex<templateSourceLocations.size(); templateIndex++) {
                    Location templateSourceLocation = templateSourceLocations.get(templateIndex);
                    Location templateTargetLocation = templateTargetLocations.get(templateIndex);
                    Location sourceLocation = sourceLocations.get(templateIndex);
                    Location targetConstraint = targetConstraints.get(templateIndex);
                    
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
                    
                    for (Location location : newLocations) {
                        if (targetCompartment.isValidVoxel(location)) {
                            if (targetConstraint == null || targetConstraint.equals(location) || targetConstraint.isRefinement(location)) {
                                result.add(getList(location));
                            }
                        }
                    }
                }
            }
            return result;
        }
        
        private List<Location> getRadialLocations2D(Location location, Compartment compartment) {
            int doubleDeltaY = ((int) location.getIndices()[0].value) * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = ((int) location.getIndices()[1].value) * 2 - compartment.dimensions[1] + 1;
            
            CellIndexExpression newIndexX = null;
            CellIndexExpression newIndexY = null;
            
            List<Location> result = new ArrayList<Location>();
            
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
        public List<List<Location>> applyChannel(List<Location> sourceLocations, List<Location> targetConstraints,
                List<Compartment> compartments) {
            
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            if (isValidSourceLocations(templateSourceLocations, sourceLocations, compartments)) {

                // TODO constrain to single location at a time
                for (int templateIndex=0; templateIndex<templateSourceLocations.size(); templateIndex++) {
                    Location templateSourceLocation = templateSourceLocations.get(templateIndex);
                    Location templateTargetLocation = templateTargetLocations.get(templateIndex);
                    Location sourceLocation = sourceLocations.get(templateIndex);
                    Location targetConstraint = targetConstraints.get(templateIndex);
                    
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
                    
                    for (Location location : newLocations) {
                        if (targetCompartment.isValidVoxel(location)) {
                            if (targetConstraint == null || targetConstraint.equals(location) || targetConstraint.isRefinement(location)) {
                                result.add(getList(location));
                            }
                        }
                    }
                }
            }
            return result;
        }
        
        private List<Location> getRadialLocations2D(Location location, Compartment compartment) {
            int doubleDeltaY = ((int) location.getIndices()[0].value) * 2 - compartment.dimensions[0] + 1;
            int doubleDeltaX = ((int) location.getIndices()[1].value) * 2 - compartment.dimensions[1] + 1;
            
            CellIndexExpression newIndexX = null;
            CellIndexExpression newIndexY = null;
            
            List<Location> result = new ArrayList<Location>();
            
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
        public List<List<Location>> applyChannel(List<Location> sourceLocations, List<Location> targetConstraints,
                List<Compartment> compartments) {
            
            List<List<Location>> result = new ArrayList<List<Location>>();
            
            if (isValidSourceLocations(templateSourceLocations, sourceLocations, compartments)) {

                // TODO constrain to single location at a time
                for (int templateIndex=0; templateIndex<templateSourceLocations.size(); templateIndex++) {
                    Location templateSourceLocation = templateSourceLocations.get(templateIndex);
                    Location templateTargetLocation = templateTargetLocations.get(templateIndex);
                    Location sourceLocation = sourceLocations.get(templateIndex);
                    Location targetConstraint = targetConstraints.get(templateIndex);
                    
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
                    
                    for (Location location : newLocations) {
                        if (targetCompartment.isValidVoxel(location)) {
                            if (targetConstraint == null || targetConstraint.equals(location) || targetConstraint.isRefinement(location)) {
                                result.add(getList(location));
                            }
                        }
                    }
                }
            }
            return result;
        }
        
        private List<Location> getLateralLocations2D(Location location, Compartment compartment) {
            float distanceToCentre = getDistanceToCentre(location, compartment);
            
            CellIndexExpression newIndexX = location.getIndices()[1].getDeltaIndex(-1);
            CellIndexExpression newIndexY = location.getIndices()[0].getDeltaIndex(-1);
            CellIndexExpression newIndexX2 = location.getIndices()[1].getDeltaIndex(1);
            CellIndexExpression newIndexY2 = location.getIndices()[0].getDeltaIndex(1);

            List<Location> result = new ArrayList<Location>();
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

}
