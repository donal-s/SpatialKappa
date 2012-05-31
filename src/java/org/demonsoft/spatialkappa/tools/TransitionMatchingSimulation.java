package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.ComplexMapping;
import org.demonsoft.spatialkappa.model.ComplexMatcher;
import org.demonsoft.spatialkappa.model.ComplexStore;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.LocatedComplex;
import org.demonsoft.spatialkappa.model.LocatedComplexMap;
import org.demonsoft.spatialkappa.model.LocatedTransition;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.Transport;
import org.demonsoft.spatialkappa.model.Utils;
import org.demonsoft.spatialkappa.model.Variable;


public class TransitionMatchingSimulation extends AbstractSimulation {

    final ComplexMatcher matcher = new ComplexMatcher();

    final ComplexStore transitionComponentActivity = new ComplexStore();
    final LocatedComplexMap<List<LocatedComplex>> complexComponentMap = new LocatedComplexMap<List<LocatedComplex>>();
    final LocatedComplexMap<List<LocatedTransition>> complexTransitionMap = new LocatedComplexMap<List<LocatedTransition>>();
    final Map<Location, List<LocatedTransition>> emptySubstrateTransitionMap = new HashMap<Location, List<LocatedTransition>>();
    final LocatedComplexMap<List<ComplexMapping>> componentComplexMappingMap = new LocatedComplexMap<List<ComplexMapping>>();
    final Map<Location, List<Complex>> locationComplexMap = new HashMap<Location, List<Complex>>();

    public TransitionMatchingSimulation() {
        super();
        initialiseActivityMaps();
    }

    public TransitionMatchingSimulation(IKappaModel kappaModel) {
        super(kappaModel);
        initialiseActivityMaps();
    }

    private void initialiseActivityMaps() {
        for (LocatedTransition transform : getAllLocatedTransitions()) {
            if (transform.transition.sourceComplexes.size() > 0) {
                for (Complex component : transform.transition.sourceComplexes) {
                    componentComplexMappingMap.put(component, transform.sourceLocation, new ArrayList<ComplexMapping>());
                }
            }
            else { // transition without source complexes
                List<LocatedTransition> transitionMap = emptySubstrateTransitionMap.get(transform.sourceLocation);
                if (transitionMap == null) {
                    transitionMap = new ArrayList<LocatedTransition>();
                    emptySubstrateTransitionMap.put(transform.sourceLocation, transitionMap);
                }
                transitionMap.add(transform);
                updateTransitionActivity(transform, false);
            }
        }

        for (LocatedComplex complex : complexStore.getLocatedComplexes()) {
            int quantity = complexStore.getComplexQuantity(complex.complex, complex.location);
            for (int index = 0; index < quantity; index++) {
                increaseTransitionActivities(complex.complex.clone(), complex.location);
            }
        }
    }


    @Override
    public String getDebugOutput() {
        StringBuilder builder = new StringBuilder();
        builder.append("Runtime (s): " + (time / 1000) + "\n");
        builder.append("Distinct complexes: " + complexStore.getComplexes().size() + "\n");
        builder.append("Final counts: " + getCurrentObservation(true, 1f) + "\n");

        builder.append("Final all counts:" + "\n");
        builder.append(getCanonicalStore().getDebugOutput());
        
        builder.append("\nFinal count per agent:" + "\n");
        for (Map.Entry<String, Integer> entry : getCountsPerAgent().entrySet()) {
            builder.append(entry.getValue() + "\t" + entry.getKey() + "\n");
        }
        return builder.toString();
    }


    private  ComplexStore getCanonicalStore() {
        ComplexStore result = new ComplexStore();
        
        for (Map.Entry<Location, List<Complex>> entry : locationComplexMap.entrySet()) {
            Location location = entry.getKey();
            for (Complex complex : entry.getValue()) {
                Complex storedComplex = result.getExistingComplex(complex);
    
                if (storedComplex != null) {
                    complex = storedComplex;
                }
                result.increaseComplexQuantity(complex, location, 1);
            }
        }
        return result;
    }

    @Override
    protected boolean applyTransform(Transform transform, Location location, boolean incrementTime) {

        List<ComplexMapping> concreteSourceComplexMappings = new ArrayList<ComplexMapping>();
        for (Complex leftComplex : transform.sourceComplexes) {
            ComplexMapping sourceComplexMapping = pickComplexMapping(leftComplex, location);
            if (sourceComplexMapping == null) {
                return false;
            }
            concreteSourceComplexMappings.add(sourceComplexMapping);
        }

        Set<Complex> chosenComplexes = new HashSet<Complex>();
        for (ComplexMapping complexMapping : concreteSourceComplexMappings) {
            Complex complex = complexMapping.target;
            if (!chosenComplexes.add(complex)) {
                return false;
            }
        }

        if (incrementTime) {
            time += getTimeDelta();
        }

        for (Complex complex : chosenComplexes) {
            reduceTransitionActivities(complex, location);
        }

        incrementTransitionsFired(transform);

        List<Complex> resultComplexes = transform.apply(concreteSourceComplexMappings);

        for (Complex complex : resultComplexes) {
            increaseTransitionActivities(complex, location);
        }

        return true;
    }

    @Override
    protected boolean applyTransport(Transport transport, Location sourceLocation, Location targetLocation, boolean incrementTime) {

        Complex sourceComplex;
        if (transport.sourceComplexes.size() > 0) {
            sourceComplex = pickComplex(transport.sourceComplexes, sourceLocation);
        }
        else { // Match all at location
            sourceComplex = pickComplex(sourceLocation);
        }
        if (sourceComplex == null) {
            return false;
        }

        if (incrementTime) {
            time += getTimeDelta();
        }

        reduceTransitionActivities(sourceComplex, sourceLocation);
        increaseTransitionActivities(sourceComplex, targetLocation);

        incrementTransitionsFired(transport);

        return true;
    }

    private void increaseTransitionActivities(Complex complex, Location location) {
        List<Complex> locationComplexes = locationComplexMap.get(location);
        if (locationComplexes == null) {
            locationComplexes = new ArrayList<Complex>();
            locationComplexMap.put(location, locationComplexes);
        }
        locationComplexes.add(complex);

        List<LocatedComplex> affectedTransitionComponents = new ArrayList<LocatedComplex>();
        List<LocatedTransition> affectedTransitions = new ArrayList<LocatedTransition>();
        complexComponentMap.put(complex, location, affectedTransitionComponents);
        complexTransitionMap.put(complex, location, affectedTransitions);

        for (LocatedTransition transition : getAllLocatedTransitions()) {
            if (Utils.equal(location, transition.sourceLocation)) {
                boolean found = false;
                for (Complex component : transition.transition.sourceComplexes) {
                    List<ComplexMapping> mappings = matcher.getPartialMatches(component, complex);
                    if (mappings.size() > 0) {
                        affectedTransitionComponents.add(new LocatedComplex(component, location));
                        found = true;
                        componentComplexMappingMap.get(component, location).addAll(mappings);
                    }
                    transitionComponentActivity.increaseComplexQuantity(component, location, mappings.size());
                }
                if (found) {
                    affectedTransitions.add(transition);
                    updateTransitionActivity(transition, false);
                }
            }
        }

        addComplexToObservables(complex, location);

        List<LocatedTransition> transitionMap = emptySubstrateTransitionMap.get(location);
        if (transitionMap != null) {
            for (LocatedTransition transition : transitionMap) {
                updateTransitionActivity(transition, false);
            }
        }

    }

    private void reduceTransitionActivities(Complex complex, Location location) {
        locationComplexMap.get(location).remove(complex);

        List<LocatedComplex> affectedTransitionComponents = complexComponentMap.get(complex, location);
        List<LocatedTransition> affectedTransitions = complexTransitionMap.get(complex, location);
        complexComponentMap.remove(complex, location);
        complexTransitionMap.remove(complex, location);

        for (LocatedComplex transitionComponent : affectedTransitionComponents) {
            int activity = 0;
            ListIterator<ComplexMapping> iter = componentComplexMappingMap.get(transitionComponent.complex, location).listIterator();
            while (iter.hasNext()) {
                ComplexMapping complexMapping = iter.next();
                if (complexMapping.target == complex) {
                    activity++;
                    iter.remove();
                }
            }
            transitionComponentActivity.decreaseComplexQuantity(transitionComponent.complex, transitionComponent.location, activity);
        }
        for (LocatedTransition transition : affectedTransitions) {
            updateTransitionActivity(transition, false);
        }

        removeComplexFromObservables(complex, location);

        List<LocatedTransition> transitionMap = emptySubstrateTransitionMap.get(location);
        if (transitionMap != null) {
            for (LocatedTransition transition : transitionMap) {
                updateTransitionActivity(transition, false);
            }
        }
    }

    private List<LocatedTransition> getAllLocatedTransitions() {
        List<LocatedTransition> result = new ArrayList<LocatedTransition>(finiteRateTransitions);
        result.addAll(infiniteRateTransitions);
        return result;
    }

    @Override
    protected void updateActivityMaps() {
        // Do nothing
    }

    @Override
    protected int getTransitionComponentActivity(Complex transitionComplex, Location location) {
        return transitionComponentActivity.getComplexQuantity(transitionComplex, location);
    }

    @Override
    protected int getComponentFreeTransitionActivity(LocatedTransition transition) {
        List<Complex> complexes = locationComplexMap.get(transition.sourceLocation);
        return complexes == null ? 0 : complexes.size();
    }

    private ComplexMapping pickComplexMapping(Complex component, Location location) {
        List<ComplexMapping> sourceComplexMappings = componentComplexMappingMap.get(component, location);
        return pickComplexMapping(sourceComplexMappings);
    }

    private ComplexMapping pickComplexMapping(List<ComplexMapping> complexMappings) {
        int totalQuantity = complexMappings.size();
        if (totalQuantity == 0) {
            return null;
        }
        int item = (int) (totalQuantity * Math.random());
        return complexMappings.get(item);
    }

    private Complex pickComplex(List<Complex> components, Location location) {
        List<ComplexMapping> complexMappings = new ArrayList<ComplexMapping>();
        for (Complex templateComplex : components) {
            complexMappings.addAll(componentComplexMappingMap.get(templateComplex, location));
        }
        ComplexMapping complexMapping = pickComplexMapping(complexMappings);
        return complexMapping == null ? null : complexMapping.target;
    }

    private Complex pickComplex(Location location) {
        List<Complex> complexes = locationComplexMap.get(location);
        if (complexes == null) {
            return null;
        }
        int item = (int) (complexes.size() * Math.random());
        return complexes.get(item);
    }

    @Override
    public ObservationElement getComplexQuantity(Variable variable) {
        if (variable == null) {
            throw new NullPointerException();
        }
        int value = 0;
        int[] dimensions = null;
        Serializable cellValues = null;
        boolean partition = false;
        List<ObservableMapValue> complexes = observableComplexMap.get(variable);
        if (complexes != null) {
            if (variable.location != NOT_LOCATED) {
                Compartment compartment = variable.location.getReferencedCompartment(kappaModel.getCompartments());
                if (compartment.getDimensions().length != variable.location.getIndices().length) {
                    partition = true;
                    dimensions = compartment.getDimensions();
                    cellValues = compartment.createValueArray();
                }

                for (ObservableMapValue current : complexes) {
                    int quantity = current.count;
                    value += quantity;
                    if (partition) {
                        addCellValue(cellValues, quantity, current.location.getIndices());
                    }
                }
                if (partition) {
                    return new ObservationElement(value, dimensions, compartment.getName(), cellValues);
                }
            }
            else { // No compartment
                for (ObservableMapValue current : complexes) {
                    value += current.count;
                }
            }
        }
        return new ObservationElement(value);
    }

}
