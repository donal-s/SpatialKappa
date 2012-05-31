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
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.LocatedComplex;
import org.demonsoft.spatialkappa.model.LocatedTransition;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.TransformCache;
import org.demonsoft.spatialkappa.model.Transition;
import org.demonsoft.spatialkappa.model.Transport;
import org.demonsoft.spatialkappa.model.Utils;
import org.demonsoft.spatialkappa.model.Variable;


public class ComplexMatchingSimulation extends AbstractSimulation {

    private static final int UNUSED_LIMIT = 100;
    protected static final boolean USE_CACHE = true;

    
    protected final Map<Complex, List<Transition>> complexTransitionMap = new HashMap<Complex, List<Transition>>();
    protected final Map<Complex, List<ComplexMapping>> transitionComplexMap = new HashMap<Complex, List<ComplexMapping>>();
    private List<Complex> unusedComplexes = new ArrayList<Complex>();
    private int unusedQueueLength = UNUSED_LIMIT;
    private boolean transformCacheUsed = USE_CACHE;
    private TransformCache transformCache = new TransformCache();


    public ComplexMatchingSimulation() {
        super();
        initialise();
    }

    public ComplexMatchingSimulation(IKappaModel kappaModel) {
        super(kappaModel);
        initialise();
    }

    private void initialise() {
        updateObservableComplexMap();
        updateTransitionComplexMap();
        updateComplexTransitionMap();
        initialiseActivityMaps();
    }
    
    @Override
    public String getDebugOutput() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.getDebugOutput());
        builder.append("Distinct concrete transforms: " + transformCache.size() + "\n");
        return builder.toString();
    }

    private void increaseTransitionActivities(Complex complex, Location location) {
        invalidateTransitionActivities(complex, location);
    }

    private void reduceTransitionActivities(Complex complex, Location location) {
        invalidateTransitionActivities(complex, location);
        if (complexStore.getComplexQuantity(complex, location) == 0) {
            removeComplexFromObservables(complex, location);
        }
    }

    private void invalidateTransitionActivities(Complex complex, Location location) {
        Set<Transition> affectedTransitions = new HashSet<Transition>();
        if (complexTransitionMap.containsKey(complex)) {
            affectedTransitions.addAll(complexTransitionMap.get(complex));
        }
        for (LocatedTransition current : finiteRateTransitions) {
            Transition transition = current.transition;
            if (transition.sourceComplexes.size() == 0 || affectedTransitions.contains(transition)) {
                if (Utils.equal(current.sourceLocation, location)) {
                    finiteRateTransitionActivityMap.remove(current);
                }
            }
        }
        for (LocatedTransition current : infiniteRateTransitions) {
            Transition transition = current.transition;
            if (transition.sourceComplexes.size() == 0 || affectedTransitions.contains(transition)) {
                if (Utils.equal(current.sourceLocation, location)) {
                    infiniteRateTransitionActivityMap.remove(current);
                }
            }
        }

    }

    private void initialiseActivityMaps() {
        finiteRateTransitionActivityMap.clear();
        infiniteRateTransitionActivityMap.clear();

        for (LocatedTransition transform : finiteRateTransitions) {
            if (!finiteRateTransitionActivityMap.containsKey(transform)) {
                updateTransitionActivity(transform, false);
            }
        }
        for (LocatedTransition transform : infiniteRateTransitions) {
            if (!infiniteRateTransitionActivityMap.containsKey(transform)) {
                updateTransitionActivity(transform, false);
            }
        }
    }

    @Override
    protected void updateActivityMaps() {
        initialiseActivityMaps();
    }

    @Override
    public int getTransitionComponentActivity(Complex transitionComplex, Location location) {
        List<ComplexMapping> complexMappings = transitionComplexMap.get(transitionComplex);
        int result = 0;
        for (ComplexMapping complexMapping : complexMappings) {
            result += complexStore.getComplexQuantity(complexMapping.target, location);
        }
        return result;
    }

    private void addNewComplexToState(Complex complex, Location location) {
        updateTransitionComplexMap(complex);
        updateComplexTransitionMap(complex);
        addComplexToObservables(complex, location);
    }

    private void updateComplexTransitionMap() {
        for (Complex complex : complexStore.getComplexes()) {
            updateComplexTransitionMap(complex);
        }
    }

    private void updateComplexTransitionMap(Complex complex) {
        if (!complexTransitionMap.containsKey(complex)) {
            List<Transition> currentTransitions = new ArrayList<Transition>();
            for (LocatedTransition transition : finiteRateTransitions) {
                List<Complex> sourceComplexes = transition.transition.sourceComplexes;
                for (Complex leftComplex : sourceComplexes) {
                    for (ComplexMapping complexMapping : transitionComplexMap.get(leftComplex)) {
                        if (complexMapping.target == complex) {
                            currentTransitions.add(transition.transition);
                            finiteRateTransitionActivityMap.remove(transition);
                        }
                    }
                }
            }
            for (LocatedTransition transition : infiniteRateTransitions) {
                List<Complex> sourceComplexes = transition.transition.sourceComplexes;
                for (Complex leftComplex : sourceComplexes) {
                    for (ComplexMapping complexMapping : transitionComplexMap.get(leftComplex)) {
                        if (complexMapping.target == complex) {
                            currentTransitions.add(transition.transition);
                            infiniteRateTransitionActivityMap.remove(transition);
                        }
                    }
                }
            }
            complexTransitionMap.put(complex, currentTransitions);
        }
    }

    private void markComplexUsed(Complex complex) {
        if (!transformCacheUsed) {
            return;
        }

        unusedComplexes.remove(complex);
    }

    private void markComplexUnused(Complex complex) {
        if (!transformCacheUsed) {
            return;
        }

        unusedComplexes.add(complex);
        if (unusedComplexes.size() > unusedQueueLength) {
            Complex removedComplex = unusedComplexes.remove(0);
            complexStore.removeComplex(removedComplex);

            List<Transition> transitions = complexTransitionMap.get(removedComplex);
            if (transitions != null) {
                complexTransitionMap.remove(removedComplex);
                for (Transition transition : transitions) {
                    for (Complex transformComplex : transition.sourceComplexes) {
                        List<ComplexMapping> complexMappings = transitionComplexMap.get(transformComplex);
                        if (complexMappings != null) {
                            ListIterator<ComplexMapping> iter = complexMappings.listIterator();
                            while (iter.hasNext()) {
                                ComplexMapping mapping = iter.next();
                                if (mapping.target == removedComplex) {
                                    iter.remove();
                                }
                            }
                        }
                    }

                }

            }
            for (Map.Entry<Variable, List<ObservableMapValue>> entry : observableComplexMap.entrySet()) {
                ListIterator<ObservableMapValue> iter = entry.getValue().listIterator();
                while (iter.hasNext()) {
                    ObservableMapValue current = iter.next();
                    if (current.complex == removedComplex) {
                        iter.remove();
                    }
                }
            }

            transformCache.removeAllWith(removedComplex);
        }
    }

    private void updateTransitionComplexMap(Complex concreteComplex) {
        ComplexMatcher matcher = new ComplexMatcher();
        for (LocatedTransition transition : finiteRateTransitions) {
            List<Complex> sourceComplexes = transition.transition.sourceComplexes;
            for (Complex transitionComplex : sourceComplexes) {
                if (transitionComplexMap.containsKey(transitionComplex)) {
                    List<ComplexMapping> complexMappings = matcher.getPartialMatches(transitionComplex, concreteComplex);
                    if (!complexMappings.isEmpty()) {
                        transitionComplexMap.get(transitionComplex).addAll(complexMappings);
                    }
                }
            }
        }
        for (LocatedTransition transition : infiniteRateTransitions) {
            List<Complex> sourceComplexes = transition.transition.sourceComplexes;
            for (Complex transitionComplex : sourceComplexes) {
                if (transitionComplexMap.containsKey(transitionComplex)) {
                    List<ComplexMapping> complexMappings = matcher.getPartialMatches(transitionComplex, concreteComplex);
                    if (!complexMappings.isEmpty()) {
                        transitionComplexMap.get(transitionComplex).addAll(complexMappings);
                    }
                }
            }
        }
    }

    private void updateTransitionComplexMap() {
        ComplexMatcher matcher = new ComplexMatcher();
        for (Transition transition : getAllTransitions()) {
            for (Complex transitionComplex : transition.sourceComplexes) {
                if (!transitionComplexMap.containsKey(transitionComplex)) {
                    List<ComplexMapping> concreteComplexMappings = new ArrayList<ComplexMapping>();
                    transitionComplexMap.put(transitionComplex, concreteComplexMappings);
                    for (Complex concreteComplex : complexStore.getComplexes()) {
                        List<ComplexMapping> complexMappings = matcher.getPartialMatches(transitionComplex, concreteComplex);
                        if (!complexMappings.isEmpty()) {
                            concreteComplexMappings.addAll(complexMappings);
                        }
                    }
                }
            }
        }
    }

    private ComplexMapping pickComplexMapping(Complex component, Location location) {
        List<ComplexMapping> sourceComplexMappings = transitionComplexMap.get(component);
        return pickComplexMapping(sourceComplexMappings, location);
    }

    private ComplexMapping pickComplexMapping(List<ComplexMapping> complexMappings, Location location) {
        int totalQuantity = 0;
        for (ComplexMapping complexMapping : complexMappings) {
            totalQuantity += complexStore.getComplexQuantity(complexMapping.target, location);
        }
        if (totalQuantity == 0) {
            return null;
        }
        int item = (int) (totalQuantity * Math.random());
        for (ComplexMapping complexMapping : complexMappings) {
            int currentQuantity = complexStore.getComplexQuantity(complexMapping.target, location);
            if (currentQuantity > 0) {
                if (item <= currentQuantity) {
                    return complexMapping;
                }
                item -= currentQuantity;
            }
        }
        return null;
    }

    private Complex pickComplex(List<Complex> components, Location location) {
        List<ComplexMapping> complexMappings = new ArrayList<ComplexMapping>();
        for (Complex templateComplex : components) {
            complexMappings.addAll(transitionComplexMap.get(templateComplex));
        }
        ComplexMapping complexMapping = pickComplexMapping(complexMappings, location);
        return complexMapping == null ? null : complexMapping.target;
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

        Map<Complex, Integer> deltaComplexQuantities = new HashMap<Complex, Integer>();
        for (ComplexMapping complexMapping : concreteSourceComplexMappings) {
            Complex complex = complexMapping.target;
            if (deltaComplexQuantities.containsKey(complex)) {
                deltaComplexQuantities.put(complex, deltaComplexQuantities.get(complex) + 1);
            }
            else {
                deltaComplexQuantities.put(complex, 1);
            }
        }

        for (Map.Entry<Complex, Integer> entry : deltaComplexQuantities.entrySet()) {
            if (entry.getValue() > complexStore.getComplexQuantity(entry.getKey(), location)) {
                return false;
            }
        }

        if (incrementTime) {
            time += getTimeDelta();
        }

        for (Map.Entry<Complex, Integer> entry : deltaComplexQuantities.entrySet()) {
            Complex complex = entry.getKey();
            if (complexStore.decreaseComplexQuantity(complex, location, entry.getValue())) {
                markComplexUnused(complex);
            }
            if (complexStore.getComplexQuantity(complex, location) == 0) {
                removeComplexFromObservables(complex, location);
            }
            reduceTransitionActivities(complex, location);
        }

        incrementTransitionsFired(transform);

        List<Complex> cachedTargetComplexes = transformCacheUsed ? transformCache.get(transform, concreteSourceComplexMappings) : null;
        if (cachedTargetComplexes == null) {
            cachedTargetComplexes = new ArrayList<Complex>();
            List<Complex> concreteTargetComplexes = transform.apply(concreteSourceComplexMappings);

            for (Complex complex : concreteTargetComplexes) {
                Complex storedComplex = complexStore.getExistingComplex(complex);

                if (storedComplex != null) {
                    complex = storedComplex;
                    markComplexUsed(complex);
                    if (complexStore.isExistingLocatedComplex(complex, location)) {
                        complexStore.increaseComplexQuantity(complex, location, 1);
                        increaseTransitionActivities(complex, location);
                        cachedTargetComplexes.add(complex);
                        continue;
                    }
                }
                else {
                    addNewComplexToState(complex, location);
                }

                if (!complexStore.isExistingLocatedComplex(complex, location)) {
                    addComplexToObservables(complex, location);
                }
                increaseTransitionActivities(complex, location);
                complexStore.increaseComplexQuantity(complex, location, 1);
                cachedTargetComplexes.add(complex);
            }

            if (transformCacheUsed) {
                transformCache.put(transform, concreteSourceComplexMappings, cachedTargetComplexes);
            }
        }
        else {
            for (Complex complex : cachedTargetComplexes) {
                if (!complexStore.isExistingLocatedComplex(complex, location)) {
                    addComplexToObservables(complex, location);
                }
                complexStore.increaseComplexQuantity(complex, location, 1);
                markComplexUsed(complex);
                increaseTransitionActivities(complex, location);
            }
        }

        return true;
    }

    @Override
    protected boolean applyTransport(Transport transport, Location sourceLocation, Location targetLocation, boolean incrementTime) {

        Complex sourceComplex;
        if (transport.sourceComplexes.size() > 0) {
            sourceComplex = pickComplex(transport.sourceComplexes, sourceLocation);
        }
        else { // Match all
            sourceComplex = complexStore.pickComplex(sourceLocation);
        }
        if (sourceComplex == null) {
            return false;
        }

        if (incrementTime) {
            time += getTimeDelta();
        }

        complexStore.decreaseComplexQuantity(sourceComplex, sourceLocation, 1);
        
        if (!complexStore.isExistingLocatedComplex(sourceComplex, targetLocation)) {
            addComplexToObservables(sourceComplex, targetLocation);
        }
        complexStore.increaseComplexQuantity(sourceComplex, targetLocation, 1);

        reduceTransitionActivities(sourceComplex, sourceLocation);
        increaseTransitionActivities(sourceComplex, targetLocation);
        
        incrementTransitionsFired(transport);

        return true;
    }


    private void updateObservableComplexMap() {
        ComplexMatcher matcher = new ComplexMatcher();
        Set<LocatedComplex> complexes = complexStore.getLocatedComplexes();
        for (Map.Entry<Variable, List<ObservableMapValue>> entry : observableComplexMap.entrySet()) {
            List<ObservableMapValue> matches = entry.getValue();
            Variable variable = entry.getKey();
            matches.clear();
            for (LocatedComplex complex : complexes) {
                boolean matchNameOnly = variable.location != null && variable.location.getIndices().length == 0;
                if (Location.doLocationsMatch(variable.location, complex.location, matchNameOnly)) {
                    int matchCount = matcher.getPartialMatches(variable.complex, complex.complex).size();
                    if (matchCount > 0) {
                        matches.add(new ObservableMapValue(complex.complex, complex.location, matchCount));
                    }
                }
            }
        }
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
                    int quantity = complexStore.getComplexQuantity(current.complex, current.location) * current.count;
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
                    value += complexStore.getComplexQuantity(current.complex, current.location) * current.count;
                }
            }
        }
        return new ObservationElement(value);
    }

}
