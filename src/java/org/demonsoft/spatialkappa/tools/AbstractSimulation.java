package org.demonsoft.spatialkappa.tools;

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
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.ComplexMatcher;
import org.demonsoft.spatialkappa.model.ComplexStore;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedComplex;
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

public abstract class AbstractSimulation implements Simulation, SimulationState {

    protected static final Map<String, Integer> NO_VARIABLES = new HashMap<String, Integer>();

    protected List<LocatedTransition> finiteRateTransitions = new ArrayList<LocatedTransition>();
    protected List<LocatedTransition> infiniteRateTransitions = new ArrayList<LocatedTransition>();
    private final List<Perturbation> perturbations = new ArrayList<Perturbation>();

    final Map<LocatedTransition, Float> finiteRateTransitionActivityMap = new HashMap<LocatedTransition, Float>();
    final Map<LocatedTransition, Boolean> infiniteRateTransitionActivityMap = new HashMap<LocatedTransition, Boolean>();
    protected final Map<Variable, List<ObservableMapValue>> observableComplexMap = new HashMap<Variable, List<ObservableMapValue>>();
    protected final Map<Variable, Integer> transitionsFiredMap = new HashMap<Variable, Integer>();
    final ComplexStore complexStore = new ComplexStore();

    protected boolean stop = false;
    protected boolean noTransitionsPossible = false;
    protected float time = 0;
    protected long startTime;
    private int eventCount = 0;

    protected final IKappaModel kappaModel;
    private final List<ObservationListener> observationListeners = new ArrayList<ObservationListener>();

    public AbstractSimulation() {
        this(new KappaModel());
    }

    public AbstractSimulation(IKappaModel kappaModel) {
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
    }

    @Override
    public String toString() {
        return kappaModel.toString();
    }

    public final void stop() {
        stop = true;
    }

    public final void runByEvent(int steps, int eventsPerStep) {
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

    public final void runByTime(float totalTime, float timePerStep) {
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

    final float getNextEndTime(float currentTime, float timePerStep) {
        int eventsSoFar = Math.round(currentTime / timePerStep);
        return timePerStep * (eventsSoFar + 1);
    }

    public final void addObservationListener(ObservationListener listener) {
        observationListeners.add(listener);
    }

    public final void removeObservationListener(ObservationListener listener) {
        observationListeners.remove(listener);
    }


    private final void notifyObservationListeners(boolean finalEvent, float progress) {
        Observation observation = getCurrentObservation(finalEvent, progress);
        for (ObservationListener listener : observationListeners) {
            listener.observation(observation);
        }
    }

    public final Observation getCurrentObservation() {
        return getCurrentObservation(false, 1);
    }

    public final int getEventCount() {
        return eventCount;
    }
    
    public final float getTime() {
        return time;
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

    public abstract ObservationElement getComplexQuantity(Variable observable);

    protected final void addCellValue(Object cellValues, float quantity, CellIndexExpression[] indices) {
        Object slice = cellValues;
        for (int index = 0; index < indices.length - 1; index++) {
            slice = ((Object[]) slice)[indices[index].evaluateIndex(NO_VARIABLES)];
        }
        int index = indices[indices.length - 1].evaluateIndex(NO_VARIABLES);
        ((float[]) slice)[index] = ((float[]) slice)[index] + quantity;
    }

    private final void resetTransformsFiredCount() {
        for (Map.Entry<Variable, Integer> entry : transitionsFiredMap.entrySet()) {
            entry.setValue(0);
        }
    }

    protected final Observation getCurrentObservation(boolean finalEvent, float progress) {
        Map<String, ObservationElement> result = new HashMap<String, ObservationElement>();
        for (String variableName : kappaModel.getPlottedVariables()) {
            Variable variable = kappaModel.getVariables().get(variableName);
            result.put(variableName, variable.evaluate(this));
        }
        long elapsedTime = Calendar.getInstance().getTimeInMillis() - startTime;
        long estimatedRemainingTime = ((long) (elapsedTime / progress)) - elapsedTime;
        return new Observation(time, eventCount, kappaModel.getPlottedVariables(), result, finalEvent, elapsedTime, estimatedRemainingTime);
    }

    private final boolean runSingleEvent() {
        updateActivityMaps();

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

    protected abstract boolean applyTransform(Transform transform, Location location, boolean incrementTime);

    protected abstract boolean applyTransport(Transport transport, Location sourceLocation, Location targetLocation, boolean incrementTime);

    private void applyInfiniteRateTransitions() {
        int clashes = 0;
        while (clashes < 1000 && !stop) {
            LocatedTransition transform = pickInfiniteRateTransform();
            if (transform == null) {
                return;
            }
            if (applyTransition(transform, false)) {
                clashes = 0;
                updateActivityMaps();
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

    protected final float getTimeDelta() {
        float totalQuantity = 0;
        for (Float current : finiteRateTransitionActivityMap.values()) {
            totalQuantity += current;
        }
        return (float) -Math.log(Math.random()) / totalQuantity;
    }

    private final LocatedTransition pickFiniteRateTransition() {
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

    private final LocatedTransition pickInfiniteRateTransform() {
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

    protected abstract void updateActivityMaps();

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

    protected final void updateTransitionActivity(LocatedTransition transition, boolean rateChanged) {
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

    protected int getComponentFreeTransitionActivity(LocatedTransition transition) {
        return complexStore.getComplexCount(transition.sourceLocation);
    }

    protected abstract int getTransitionComponentActivity(Complex transitionComplex, Location location);


    protected final Set<Transition> getAllTransitions() {
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

    protected final void addComplexToObservables(Complex complex, Location location) {
        ComplexMatcher matcher = new ComplexMatcher();
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

    protected final void removeComplexFromObservables(Complex complex, Location location) {
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
    
    public final Variable getVariable(String name) {
        return kappaModel.getVariables().get(name);
    }

    protected final Map<String, Integer> getCountsPerAgent() {
        Map<String, Integer> result = new HashMap<String, Integer>();

        for (String agentName : kappaModel.getAggregateAgentMap().keySet()) {
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

    private final Transition getTransition(String label) {
        for (Transition transition : getAllTransitions()) {
            if (label.equals(transition.label)) {
                return transition;
            }
        }
        return null;
    }
    
    protected final void incrementTransitionsFired(Transition transition) {
        if (transition.label != null) {
            Variable variable = getVariable(transition.label);
            if (variable != null && Variable.Type.TRANSITION_LABEL == variable.type) {
                transitionsFiredMap.put(variable, transitionsFiredMap.get(variable) + 1);
            }
        }
        eventCount++;
    }

    public final Map<String, Variable> getVariables() {
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

}
