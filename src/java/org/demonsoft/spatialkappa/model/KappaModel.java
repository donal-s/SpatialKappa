package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getFlatString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class KappaModel {

    private static final ComplexMatcher matcher = new ComplexMatcher();
    
    private final List<LocatedTransform> locatedTransforms = new ArrayList<LocatedTransform>();
    private final Map<String, AggregateAgent> aggregateAgentMap = new HashMap<String, AggregateAgent>();
    private final List<InitialValue> initialValues = new ArrayList<InitialValue>();
    private final List<LocatedObservable> locatedObservables = new ArrayList<LocatedObservable>();
    private final List<Perturbation> perturbations = new ArrayList<Perturbation>();
    private final List<Compartment> compartments = new ArrayList<Compartment>();
    private final List<CompartmentLink> compartmentLinks = new ArrayList<CompartmentLink>();
    private final List<Transport> transports = new ArrayList<Transport>();
    private final Set<Complex> canonicalComplexes = new HashSet<Complex>();

    public void addTransform(Direction direction, String label, List<Agent> leftSideAgents, List<Agent> rightSideAgents, String forwardRate, String backwardRate,
            Location location) {
        if (direction == null) {
            throw new NullPointerException();
        }

        if (Direction.FORWARD == direction) {
            if (backwardRate != null) {
                throw new IllegalArgumentException("transform should not have a second rate");
            }
        }
        else if (Direction.BIDIRECTIONAL == direction) {
            if (backwardRate == null) {
                throw new IllegalArgumentException("equilibrium should have a second rate");
            }
        }
        else {
            throw new IllegalArgumentException("Invalid transform type: " + direction);
        }

//        List<Complex> leftComplexes = (leftSideAgents != null) ? Utils.getComplexes(leftSideAgents) : null;
//        List<Complex> rightComplexes = (rightSideAgents != null) ? Utils.getComplexes(rightSideAgents) : null;

//        addTransform(new LocatedTransform(new Transform(label, leftComplexes, rightComplexes, forwardRate), location));
//        if (Direction.BIDIRECTIONAL == direction) {
//            addTransform(new LocatedTransform(new Transform(label, rightComplexes, leftComplexes, backwardRate), location));
//        }
        addTransform(new LocatedTransform(new Transform(label, leftSideAgents, rightSideAgents, forwardRate, false), location));
        if (Direction.BIDIRECTIONAL == direction) {
            addTransform(new LocatedTransform(new Transform(label, rightSideAgents, leftSideAgents, backwardRate, false), location));
        }
    }

    public void addTransform(LocatedTransform transform) {
        if (transform == null) {
            throw new NullPointerException();
        }
        locatedTransforms.add(transform);
        for (Complex complex : transform.transition.sourceComplexes) {
            for (Agent agent : complex.agents) {
                aggregateAgent(agent);
            }
        }
        for (Complex complex : transform.transition.targetComplexes) {
            for (Agent agent : complex.agents) {
                aggregateAgent(agent);
            }
        }
    }

    private void aggregateAgent(Agent agent) {
        if (aggregateAgentMap.get(agent.name) == null) {
            aggregateAgentMap.put(agent.name, new AggregateAgent(agent.name));
        }
        aggregateAgentMap.get(agent.name).addSites(agent.getSites());
    }

    public void addInitialValue(List<Agent> agents, String valueText, Location compartment) {
        if (agents == null || valueText == null) {
            throw new NullPointerException();
        }
        if (agents.size() == 0) {
            throw new IllegalArgumentException("Empty complex");
        }
        int quantity = Integer.parseInt(valueText);
        for (Agent agent : agents) {
            aggregateAgent(agent);
        }
        List<Complex> complexes = getCanonicalComplexes(Utils.getComplexes(agents));
        
        initialValues.add(new InitialValue(complexes, quantity, compartment));
    }

    private List<Complex> getCanonicalComplexes(List<Complex> complexes) {
        for (int index = 0; index < complexes.size(); index++) {
            boolean found = false;
            for (Complex current : canonicalComplexes) {
                if (matcher.isExactMatch(current, complexes.get(index))) {
                    complexes.set(index, current);
                    break;
                }
            }
            if (!found) {
                canonicalComplexes.add(complexes.get(index));
            }
        }
        return complexes;
    }

    public void addObservable(List<Agent> agents, String label, Location location, boolean inObservations) {
        locatedObservables.add(new LocatedObservable(new Observable(new Complex(agents), label, inObservations), location));

        for (Agent agent : agents) {
            aggregateAgent(agent);
        }
    }

    public void addObservable(String label) {
        locatedObservables.add(new LocatedObservable(new Observable(label), null));
    }

    public void addPerturbation(Perturbation perturbation) {
        perturbations.add(perturbation);
    }

    public void addCompartment(String name, List<Integer> dimensions) {
        if (name == null || dimensions == null) {
            throw new NullPointerException();
        }
        int[] dimArray = new int[dimensions.size()];
        for (int index = 0; index < dimensions.size(); index++) {
            dimArray[index] = dimensions.get(index);
        }
        addCompartment(new Compartment(name, dimArray));
    }

    public void addCompartment(Compartment compartment) {
        if (compartment == null) {
            throw new NullPointerException();
        }
        compartments.add(compartment);
    }

    public void addCompartmentLink(CompartmentLink link) {
        if (link == null) {
            throw new NullPointerException();
        }
        compartmentLinks.add(link);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("COMPARTMENTS\n");
        for (Compartment compartment : compartments) {
            result.append(compartment).append("\n");
        }
        result.append("\nCOMPARTMENT LINKS\n");
        for (CompartmentLink link : compartmentLinks) {
            result.append(link).append("\n");
        }
        result.append("\nTRANSPORT RULES\n");
        for (Transport transport : transports) {
            result.append(transport).append("\n");
        }
        result.append("\nAGENTS\n");
        for (AggregateAgent agent : aggregateAgentMap.values()) {
            result.append(agent).append("\n");
        }
        result.append("\nTRANSFORM RULES\n");
        for (LocatedTransition transform : locatedTransforms) {
            result.append(transform).append("\n");
        }
        result.append("\nINITIAL VALUES\n");
        for (InitialValue initialValue : initialValues) {
            result.append(initialValue).append("\n");
        }
        result.append("\nOBSERVABLES\n");
        for (LocatedObservable observable : locatedObservables) {
            result.append(observable).append("\n");
        }
        result.append("\nPERTURBATIONS\n");
        for (Perturbation perturbation : perturbations) {
            result.append(perturbation).append("\n");
        }
        return result.toString();
    }

    public List<InitialValue> getInitialValues() {
        return initialValues;
    }

    public Map<Complex, Integer> getInitialValuesMap() {
        Map<Complex, Integer> result = new HashMap<Complex, Integer>();
        for (InitialValue value : initialValues) {
            for (Complex complex : value.complexes) {
                result.put(complex, value.quantity);
            }
        }
        return result;
    }

    public Map<LocatedComplex, Integer> getConcreteLocatedInitialValuesMap() {
        Map<LocatedComplex, Integer> result = new HashMap<LocatedComplex, Integer>();

        for (InitialValue initialValue : initialValues) {
            boolean partition = false;
            Compartment compartment = null;
            Location location = initialValue.location;
            if (location != null) {
                compartment = location.getReferencedCompartment(compartments);
                if (compartment != null && compartment.getDimensions().length != location.getIndices().length) {
                    partition = true;
                }

                if (partition && compartment != null) {
                    int[] cellCounts = compartment.getDistributedCellCounts(initialValue.quantity);
                    Location[] cellLocations = compartment.getDistributedCellReferences();
                    
                    for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                        Location cellLocation = cellLocations[cellIndex];
                        for (Complex complex : initialValue.complexes) {
                            addInitialLocatedValue(result, complex, cellLocation, cellCounts[cellIndex]);
                        }
                    }
                }
                else {
                    for (Complex complex : initialValue.complexes) {
                        addInitialLocatedValue(result, complex, location, initialValue.quantity);
                    }
                }
            }
            else { // location == null
                if (compartments.size() > 0) {
                    int[] cellCounts = Compartment.getDistributedCellCounts(initialValue.quantity, compartments);
                    Location[] cellLocations = Compartment.getDistributedCellReferences(compartments);
                    
                    for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                        Location cellLocation = cellLocations[cellIndex];
                        for (Complex complex : initialValue.complexes) {
                            addInitialLocatedValue(result, complex, cellLocation, cellCounts[cellIndex]);
                        }
                    }
            }
                else { // no compartments
                    for (Complex complex : initialValue.complexes) {
                        addInitialLocatedValue(result, complex, location, initialValue.quantity);
                    }
                }

            }
        }

        return result;
    }

    private void addInitialLocatedValue(Map<LocatedComplex, Integer> result, Complex complex, Location location, int quantity) {
        LocatedComplex locatedComplex = new LocatedComplex(complex, location);
        for (Map.Entry<LocatedComplex, Integer> entry : result.entrySet()) {
            if (locatedComplex.isExactMatch(entry.getKey())) {
                entry.setValue(entry.getValue() + quantity);
                return;
            }
        }
        result.put(locatedComplex, quantity);
    }

    public List<Observable> getVisibleObservables() {
        List<Observable> result = new ArrayList<Observable>();
        for (LocatedObservable observable : locatedObservables) {
            if (observable.observable.inObservations) {
                result.add(observable.observable);
            }
        }
        return result;
    }

    public List<LocatedObservable> getVisibleLocatedObservables() {
        List<LocatedObservable> result = new ArrayList<LocatedObservable>();
        for (LocatedObservable observable : locatedObservables) {
            if (observable.observable.inObservations) {
                result.add(observable);
            }
        }
        return result;
    }

    public void addTransport(String label, String compartmentLinkName, List<Agent> agents, String rate) {
        addTransport(new Transport(label, compartmentLinkName, agents, rate));
    }

    void addTransport(Transport transport) {
        transports.add(transport);
    }

    public List<LocatedTransition> getConcreteLocatedTransitions() {
        List<LocatedTransition> result = new ArrayList<LocatedTransition>();
        for (LocatedTransform transition : locatedTransforms) {
            Location location = transition.sourceLocation;
            if (location != null) {
                Compartment compartment = location.getReferencedCompartment(compartments);
                if (compartment.getDimensions().length != location.getIndices().length) {
                    Location[] cellLocations = compartment.getDistributedCellReferences();
                    Transform cloneTransform = ((Transform) transition.transition).clone();
                    for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                        result.add(new LocatedTransform(cloneTransform, cellLocations[cellIndex]));
                    }
                }
                else {
                    result.add(transition.clone());
                }
            }
            else { // location == null
                if (compartments.size() > 0) {
                    Transform cloneTransform = ((Transform) transition.transition).clone();
                    for (Compartment compartment : compartments) {
                        Location[] cellLocations = compartment.getDistributedCellReferences();
                        for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                            result.add(new LocatedTransform(cloneTransform, cellLocations[cellIndex]));
                        }
                    }
                }
                else { // No compartments
                    result.add(transition.clone());
                }
            }
        }

        for (Transport transport : transports) {
            List<CompartmentLink> links = getCompartmentLinks(transport.getCompartmentLinkName());
            if (links.size() > 0) {
                Transport cloneTransport = transport.clone();
                for (CompartmentLink link : links) {
                    Location[][] cellLocations = link.getCellReferencePairs(compartments);
                    for (int cellIndex = 0; cellIndex < cellLocations.length; cellIndex++) {
                        Location sourceReference = (link.getDirection() != Direction.BACKWARD) ? cellLocations[cellIndex][0] : cellLocations[cellIndex][1];
                        Location targetReference = (link.getDirection() != Direction.BACKWARD) ? cellLocations[cellIndex][1] : cellLocations[cellIndex][0];
    
                        result.add(new LocatedTransport(cloneTransport, sourceReference, targetReference));
                        if (link.getDirection() == Direction.BIDIRECTIONAL) {
                            result.add(new LocatedTransport(cloneTransport, targetReference, sourceReference));
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<CompartmentLink> getCompartmentLinks(String compartmentLinkName) {
        List<CompartmentLink> result = new ArrayList<CompartmentLink>();
        for (CompartmentLink current : compartmentLinks) {
            if (current.getName().equals(compartmentLinkName)) {
                result.add(current);
            }
        }
        return result;
    }

    public List<LocatedTransform> getLocatedTransforms() {
        return locatedTransforms;
    }

    public List<LocatedObservable> getLocatedObservables() {
        return locatedObservables;
    }

    public List<Observable> getObservables() {
        List<Observable> result = new ArrayList<Observable>();
        for (LocatedObservable observable : locatedObservables) {
            result.add(observable.observable);
        }
        return result;
    }

    public List<Transform> getTransforms() {
        List<Transform> result = new ArrayList<Transform>();
        for (LocatedTransform transform : locatedTransforms) {
            result.add((Transform) transform.transition);
        }
        return result;
    }

    public List<Compartment> getCompartments() {
        return compartments;
    }
    
    public List<CompartmentLink> getCompartmentLinks() {
        return compartmentLinks;
    }
    
    public List<Transport> getTransports() {
        return transports;
    }
    
    public Map<String, AggregateAgent> getAggregateAgentMap() {
        return aggregateAgentMap;
    }
    
    public List<Perturbation> getPerturbations() {
        return perturbations;
    }
    
    
    public static class InitialValue {

        public final List<Complex> complexes = new ArrayList<Complex>();
        public final int quantity;
        public final Location location;

        public InitialValue(List<Complex> complexes, int quantity, Location location) {
            if (complexes == null) {
                throw new NullPointerException();
            }
            if (complexes.size() == 0 || quantity <= 0) {
                throw new IllegalArgumentException();
            }

            this.quantity = quantity;
            this.complexes.addAll(complexes);
            this.location = location;
        }

//        @Deprecated
//        public InitialValue(List<Agent> agents, int quantity, Location location) {
//            this(Utils.getComplexes(agents), quantity, location);
//        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(quantity);
            if (location != null) {
                result.append(" ").append(location);
            }

            result.append(" * (").append(getFlatString(complexes)).append(")");
            return result.toString();
        }

    }






}
