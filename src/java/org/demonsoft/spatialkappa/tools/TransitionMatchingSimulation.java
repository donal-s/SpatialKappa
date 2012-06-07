package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.ComplexMapping;
import org.demonsoft.spatialkappa.model.ComplexMatcher;
import org.demonsoft.spatialkappa.model.ComplexStore;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedComplex;
import org.demonsoft.spatialkappa.model.LocatedComplexMap;
import org.demonsoft.spatialkappa.model.LocatedTransform;
import org.demonsoft.spatialkappa.model.LocatedTransition;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;
import org.demonsoft.spatialkappa.model.Perturbation;
import org.demonsoft.spatialkappa.model.SimulationState;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.Transition;
import org.demonsoft.spatialkappa.model.Transport;
import org.demonsoft.spatialkappa.model.Utils;
import org.demonsoft.spatialkappa.model.Variable;
import org.demonsoft.spatialkappa.model.Variable.Type;
import org.demonsoft.spatialkappa.model.VariableExpression;


public class TransitionMatchingSimulation implements Simulation, SimulationState {

    private final ComplexMatcher matcher = new ComplexMatcher();

    private final ComplexStore transitionComponentActivity = new ComplexStore();
    private final LocatedComplexMap<List<LocatedComplex>> complexComponentMap = new LocatedComplexMap<List<LocatedComplex>>();
    private final LocatedComplexMap<List<LocatedTransition>> complexTransitionMap = new LocatedComplexMap<List<LocatedTransition>>();
    private final Map<Location, List<LocatedTransition>> emptySubstrateTransitionMap = new HashMap<Location, List<LocatedTransition>>();
    private final LocatedComplexMap<List<ComplexMapping>> componentComplexMappingMap = new LocatedComplexMap<List<ComplexMapping>>();
    private final Map<Location, List<Complex>> locationComplexMap = new HashMap<Location, List<Complex>>();

    private static final Map<String, Integer> NO_VARIABLES = new HashMap<String, Integer>();

    private List<LocatedTransition> finiteRateTransitions = new ArrayList<LocatedTransition>();
    private List<LocatedTransition> infiniteRateTransitions = new ArrayList<LocatedTransition>();
    private final List<Perturbation> perturbations = new ArrayList<Perturbation>();

    private final Map<LocatedTransition, Float> finiteRateTransitionActivityMap = new HashMap<LocatedTransition, Float>();
    private final Map<LocatedTransition, Boolean> infiniteRateTransitionActivityMap = new HashMap<LocatedTransition, Boolean>();
    private final Map<Variable, List<ObservableMapValue>> observableComplexMap = new HashMap<Variable, List<ObservableMapValue>>();
    final Map<Variable, Integer> transitionsFiredMap = new HashMap<Variable, Integer>();
    private final ComplexStore complexStore = new ComplexStore();

    private boolean stop = false;
    private boolean noTransitionsPossible = false;
    private float time = 0;
    private long startTime;
    private int eventCount = 0;

    private final IKappaModel kappaModel;
    private final List<ObservationListener> observationListeners = new ArrayList<ObservationListener>();

    public TransitionMatchingSimulation() {
        this(new KappaModel());
    }

    public TransitionMatchingSimulation(IKappaModel kappaModel) {
        this.kappaModel = kappaModel;
        
        for (Map.Entry<LocatedComplex, Integer> entry : kappaModel.getFixedLocatedInitialValuesMap().entrySet()) {
            complexStore.increaseComplexQuantity(entry.getKey().complex, entry.getKey().location, entry.getValue());
        }

        for (Variable variable : kappaModel.getVariables().values()) {
            if (variable.type == Type.KAPPA_EXPRESSION) {
                observableComplexMap.put(variable, new ArrayList<ObservableMapValue>());
            }
        }

        for (LocatedTransition transition : kappaModel.getFixedLocatedTransitions()) {
            if (transition.transition.getRate().isInfinite(kappaModel.getVariables())) {
                infiniteRateTransitions.add(transition);
            }
            else {
                finiteRateTransitions.add(transition);
            }
        }
        
        perturbations.addAll(kappaModel.getPerturbations());

        updateTransitionsFiredMap();
        initialiseActivityMaps();
    }


    


    @Override
    public String toString() {
        return kappaModel.toString();
    }

    public void stop() {
        stop = true;
    }

    public void runByEvent(int steps, int eventsPerStep) {
        startTime = Calendar.getInstance().getTimeInMillis();
        stop = false;

        for (int stepCount = 0; stepCount < steps && !noTransitionsPossible && !stop; stepCount++) {
            resetTransformsFiredCount();
            for (int count = 0; count < eventsPerStep && !noTransitionsPossible && !stop; count++) {
                int clashes = 0;
                while (!runSingleEvent() && clashes < 1000 && !noTransitionsPossible && !stop) {
                    // repeat
                    clashes++;
                    Thread.yield();
                }
                if (clashes >= 1000) {
                    System.out.println("Aborted timepoint");
                }
            }
            notifyObservationListeners(false, (float) (stepCount + 1) / (float) steps);
        }
        notifyObservationListeners(true, 1);
    }

    public void runByTime(float totalTime, float timePerStep) {
        startTime = Calendar.getInstance().getTimeInMillis();
        stop = false;

        do {
            resetTransformsFiredCount();
            float stepEndTime = getNextEndTime(time, timePerStep);
            while (time < stepEndTime && !noTransitionsPossible && !stop) {
                int clashes = 0;
                while (!runSingleEvent() && clashes < 1000 && !noTransitionsPossible && !stop) {
                    // repeat
                    clashes++;
                    Thread.yield();
                }
                if (clashes >= 1000) {
                    System.out.println("Aborted timepoint");
                }
            }
            notifyObservationListeners(false, time / totalTime);
        }
        while (!noTransitionsPossible && !stop && time < totalTime);
        notifyObservationListeners(true, 1);
    }

    float getNextEndTime(float currentTime, float timePerStep) {
        int eventsSoFar = Math.round(currentTime / timePerStep);
        return timePerStep * (eventsSoFar + 1);
    }

    public void addObservationListener(ObservationListener listener) {
        observationListeners.add(listener);
    }

    public void removeObservationListener(ObservationListener listener) {
        observationListeners.remove(listener);
    }


    private void notifyObservationListeners(boolean finalEvent, float progress) {
        Observation observation = getCurrentObservation(finalEvent, progress);
        for (ObservationListener listener : observationListeners) {
            listener.observation(observation);
        }
    }

    public Observation getCurrentObservation() {
        return getCurrentObservation(false, 1);
    }

    public int getEventCount() {
        return eventCount;
    }
    
    public float getTime() {
        return time;
    }

    private void addCellValue(Object cellValues, float quantity, CellIndexExpression[] indices) {
        Object slice = cellValues;
        for (int index = 0; index < indices.length - 1; index++) {
            slice = ((Object[]) slice)[indices[index].evaluateIndex(NO_VARIABLES)];
        }
        int index = indices[indices.length - 1].evaluateIndex(NO_VARIABLES);
        ((float[]) slice)[index] = ((float[]) slice)[index] + quantity;
    }

    private void resetTransformsFiredCount() {
        for (Map.Entry<Variable, Integer> entry : transitionsFiredMap.entrySet()) {
            entry.setValue(0);
        }
    }

    private Observation getCurrentObservation(boolean finalEvent, float progress) {
        Map<String, ObservationElement> result = new HashMap<String, ObservationElement>();
        for (String variableName : kappaModel.getPlottedVariables()) {
            Variable variable = kappaModel.getVariables().get(variableName);
            result.put(variableName, variable.evaluate(this));
        }
        long elapsedTime = Calendar.getInstance().getTimeInMillis() - startTime;
        long estimatedRemainingTime = ((long) (elapsedTime / progress)) - elapsedTime;
        return new Observation(time, eventCount, kappaModel.getPlottedVariables(), result, finalEvent, elapsedTime, estimatedRemainingTime);
    }

    private boolean runSingleEvent() {
        applyPerturbations();

        applyInfiniteRateTransitions();

        return applyFiniteRateTransition();
    }

    private boolean applyFiniteRateTransition() {
        LocatedTransition transition = pickFiniteRateTransition();
        if (transition == null) {
            noTransitionsPossible = true;
            return false;
        }

        return applyTransition(transition, true);
    }

    private boolean applyTransition(LocatedTransition transition, boolean incrementTime) {
        if (transition instanceof LocatedTransform) {
            return applyTransform((Transform) transition.transition, transition.sourceLocation, incrementTime);
        }
        return applyTransport((Transport) transition.transition, transition.sourceLocation, transition.targetLocation, incrementTime);
    }

    private void applyInfiniteRateTransitions() {
        int clashes = 0;
        while (clashes < 1000 && !stop) {
            LocatedTransition transform = pickInfiniteRateTransform();
            if (transform == null) {
                return;
            }
            if (applyTransition(transform, false)) {
                clashes = 0;
            }
            else {
                clashes++;
            }
        }
    }

    private void applyPerturbations() {
        ListIterator<Perturbation> iter = perturbations.listIterator();
        while (iter.hasNext()) {
            Perturbation perturbation = iter.next();
            if (perturbation.isConditionMet(this)) {
                perturbation.apply(this);
                iter.remove();
            }
        }
    }

    private float getTimeDelta() {
        float totalQuantity = 0;
        for (Float current : finiteRateTransitionActivityMap.values()) {
            totalQuantity += current;
        }
        return (float) -Math.log(Math.random()) / totalQuantity;
    }

    private LocatedTransition pickFiniteRateTransition() {
        float totalQuantity = 0;
        if (finiteRateTransitionActivityMap.size() == 0) {
            return null;
        }
        for (Map.Entry<LocatedTransition, Float> entry : finiteRateTransitionActivityMap.entrySet()) {
            totalQuantity += entry.getValue();
        }
        LocatedTransition lastTransition = null;
        float item = (float) (totalQuantity * Math.random());
        for (Map.Entry<LocatedTransition, Float> entry : finiteRateTransitionActivityMap.entrySet()) {
            if (entry.getValue() > 0) {
                lastTransition = entry.getKey();
                if (item <= entry.getValue()) {
                    return entry.getKey();
                }
                item -= entry.getValue();
            }
        }
        
        // To handle rounding errors, the last non-zero rate transition, if any, is returned by default
        return lastTransition;
    }

    private LocatedTransition pickInfiniteRateTransform() {
        float totalCount = 0;
        if (infiniteRateTransitionActivityMap.size() == 0) {
            return null;
        }
        for (Map.Entry<LocatedTransition, Boolean> entry : infiniteRateTransitionActivityMap.entrySet()) {
            if (entry.getValue()) {
                totalCount++;
            }
        }
        float item = (float) (totalCount * Math.random());
        for (Map.Entry<LocatedTransition, Boolean> entry : infiniteRateTransitionActivityMap.entrySet()) {
            if (entry.getValue() && item <= 1) {
                return entry.getKey();
            }
            if (entry.getValue()) {
                item--;
            }
        }
        return null;
    }

    private Set<LocatedTransition> getLocatedTransforms(Transform transform) {
        Set<LocatedTransition> result = new HashSet<LocatedTransition>();
        for (LocatedTransition current : infiniteRateTransitions) {
            if (current.transition.equals(transform)) {
                result.add(current);
            }
        }
        for (LocatedTransition current : finiteRateTransitions) {
            if (current.transition.equals(transform)) {
                result.add(current);
            }
        }
        return result;
    }

    private void updateTransitionActivity(LocatedTransition transition, boolean rateChanged) {
        if (rateChanged) {
            if (transition.transition.isInfiniteRate(kappaModel.getVariables())) {
                finiteRateTransitionActivityMap.remove(transition);
                if (!infiniteRateTransitions.contains(transition)) {
                    infiniteRateTransitions.add(transition);
                    finiteRateTransitions.remove(transition);
                }
            }
            else {
                infiniteRateTransitionActivityMap.remove(transition);
                if (!finiteRateTransitions.contains(transition)) {
                    finiteRateTransitions.add(transition);
                    infiniteRateTransitions.remove(transition);
                }
            }
        }

        int totalComponentActivity = (transition instanceof LocatedTransform) ? 1 : 0;
        List<Complex> sourceComplexes = transition.transition.sourceComplexes;
        if (sourceComplexes.size() > 0 || transition instanceof LocatedTransform) {
            for (Complex transitionComplex : sourceComplexes) {
                int componentActivity = getTransitionComponentActivity(transitionComplex, transition.sourceLocation);
                if (transition instanceof LocatedTransform) {
                    totalComponentActivity *= componentActivity;
                    if (totalComponentActivity == 0) {
                        break;
                    }
                }
                else { // Transport
                    totalComponentActivity += componentActivity;
                }
            }
        }
        else { // Match all
            totalComponentActivity = getComponentFreeTransitionActivity(transition);
        }
        if (transition.transition.isInfiniteRate(kappaModel.getVariables())) {
            infiniteRateTransitionActivityMap.put(transition, totalComponentActivity > 0);
        }
        else {
            float transformActivity = transition.transition.getRate().evaluate(this).value;
            transformActivity *= totalComponentActivity;
            finiteRateTransitionActivityMap.put(transition, transformActivity);
        }
    }

    private Set<Transition> getAllTransitions() {
        Set<Transition> result = new HashSet<Transition>();
        for (LocatedTransition transition : finiteRateTransitions) {
            result.add(transition.transition);
        }
        for (LocatedTransition transition : infiniteRateTransitions) {
            result.add(transition.transition);
        }
        return result;
    }

    private void updateTransitionsFiredMap() {
        for (Transition transition : getAllTransitions()) {
            if (transition.label != null) {
                Variable variable = getVariable(transition.label);
                if (variable != null && Variable.Type.TRANSITION_LABEL == variable.type) {
                    transitionsFiredMap.put(variable, 0);
                }
            }
        }
    }

    private void addComplexToObservables(Complex complex, Location location) {
        for (Map.Entry<Variable, List<ObservableMapValue>> entry : observableComplexMap.entrySet()) {
            Variable variable = entry.getKey();
            boolean matchNameOnly = variable.location != null && variable.location.getIndices().length == 0;
            if (Location.doLocationsMatch(variable.location, location, matchNameOnly)) {
                boolean exists = false;
                for (ObservableMapValue current : entry.getValue()) {
                    if (complex == current.complex && Utils.equal(location, current.location)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    int matchCount = matcher.getPartialMatches(entry.getKey().complex, complex).size();
                    if (matchCount > 0) {
                        entry.getValue().add(new ObservableMapValue(complex, location, matchCount));
                    }
                }
            }
        }
    }

    private void removeComplexFromObservables(Complex complex, Location location) {
        for (Map.Entry<Variable, List<ObservableMapValue>> entry : observableComplexMap.entrySet()) {
            Variable variable = entry.getKey();
            boolean matchNameOnly = variable.location != null && variable.location.getIndices().length == 0;
            if (Location.doLocationsMatch(variable.location, location, matchNameOnly)) {
                ObservableMapValue found = null;
                for (ObservableMapValue current : entry.getValue()) {
                    if (complex == current.complex && Utils.equal(location, current.location)) {
                        found = current;
                        break;
                    }
                }
                if (found != null) {
                    entry.getValue().remove(found);
                }
            }
        }
    }
    
    public Variable getVariable(String name) {
        return kappaModel.getVariables().get(name);
    }

    private Map<String, Integer> getCountsPerAgent() {
        Map<String, Integer> result = new HashMap<String, Integer>();

        for (String agentName : kappaModel.getAgentDeclarationMap().keySet()) {
            int count = 0;
            for (Complex complex : complexStore.getComplexes()) {
                int instanceCount = 0;
                for (Agent currentAgent : complex.agents) {
                    if (agentName.equals(currentAgent.name)) {
                        instanceCount++;
                    }
                }
                count += complexStore.getComplexQuantity(complex) * instanceCount;
            }

            result.put(agentName, count);
        }

        return result;
    }

    private Transition getTransition(String label) {
        for (Transition transition : getAllTransitions()) {
            if (label.equals(transition.label)) {
                return transition;
            }
        }
        return null;
    }
    
    private void incrementTransitionsFired(Transition transition) {
        if (transition.label != null) {
            Variable variable = getVariable(transition.label);
            if (variable != null && Variable.Type.TRANSITION_LABEL == variable.type) {
                transitionsFiredMap.put(variable, transitionsFiredMap.get(variable) + 1);
            }
        }
        eventCount++;
    }

    public Map<String, Variable> getVariables() {
        return kappaModel.getVariables();
    }

    public ObservationElement getTransitionFiredCount(Variable variable) {
        if (variable == null) {
            throw new NullPointerException();
        }
        if (variable.type != Variable.Type.TRANSITION_LABEL) {
            throw new IllegalArgumentException();
        }
        if (transitionsFiredMap.containsKey(variable)) {
            return new ObservationElement(transitionsFiredMap.get(variable));
        }
        return ObservationElement.ZERO;
    }

    public void addComplexInstances(List<Agent> agents, int amount) {
        throw new IllegalStateException("Not implemented yet");
    }

    public void setTransitionRate(String transitionName, VariableExpression rateExpression) {
        Transition transition = getTransition(transitionName);
        if (transition != null) {
            transition.setRate(rateExpression);
            for (LocatedTransition locatedTransform : getLocatedTransforms((Transform) transition)) {
                updateTransitionActivity(locatedTransform, true);
            }
        }
    }

    public static class ObservableMapValue {

        public final Complex complex;
        public final Location location;
        public final int count;

        public ObservableMapValue(Complex complex, Location location, int count) {
            this.complex = complex;
            this.location = location;
            this.count = count;
        }

        @Override
        public String toString() {
            return location.toString() + "\t" + complex.toString();
        }

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


    public String getDebugOutput() {
        StringBuilder builder = new StringBuilder();
        builder.append("Runtime (s): " + (time / 1000) + "\n");
        builder.append("Distinct complexes: " + complexStore.getComplexes().size() + "\n");
        builder.append("Final counts: " + getCurrentObservation(true, 1f) + "\n");

        builder.append("Final all counts:" + "\n");
        builder.append(complexStore.getDebugOutput());

        builder.append("\nFinal count per agent:" + "\n");
        for (Map.Entry<String, Integer> entry : getCountsPerAgent().entrySet()) {
            builder.append(entry.getValue() + "\t" + entry.getKey() + "\n");
        }
        return builder.toString();
    }


    private boolean applyTransform(Transform transform, Location location, boolean incrementTime) {

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

    private boolean applyTransport(Transport transport, Location sourceLocation, Location targetLocation, boolean incrementTime) {

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

    private int getTransitionComponentActivity(Complex transitionComplex, Location location) {
        return transitionComponentActivity.getComplexQuantity(transitionComplex, location);
    }

    private int getComponentFreeTransitionActivity(LocatedTransition transition) {
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
