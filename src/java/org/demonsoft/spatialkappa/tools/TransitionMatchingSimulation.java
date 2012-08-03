package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.Utils.getChannel;
import static org.demonsoft.spatialkappa.model.Utils.getComplexes;
import static org.demonsoft.spatialkappa.model.Utils.getList;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentLink;
import org.demonsoft.spatialkappa.model.Channel;
import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.ComplexMapping;
import org.demonsoft.spatialkappa.model.ComplexMatcher;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;
import org.demonsoft.spatialkappa.model.Perturbation;
import org.demonsoft.spatialkappa.model.SimulationState;
import org.demonsoft.spatialkappa.model.Transition;
import org.demonsoft.spatialkappa.model.TransitionInstance;
import org.demonsoft.spatialkappa.model.TransitionPrimitive;
import org.demonsoft.spatialkappa.model.Variable;
import org.demonsoft.spatialkappa.model.Variable.Type;
import org.demonsoft.spatialkappa.model.VariableExpression;


public class TransitionMatchingSimulation implements Simulation, SimulationState {

    // TODO potential bug - does rerunning simulation also reset perturbations ?
    
//    private static final Map<String, Integer> NO_VARIABLES = new HashMap<String, Integer>();

    private static final List<ComplexMapping> NO_COMPLEX_MAPPINGS = new ArrayList<ComplexMapping>();
    private static final List<TransitionInstance> NO_TRANSITION_INSTANCES = new ArrayList<TransitionInstance>();
    private static final TransitionInstance EMPTY_TRANSITION_INSTANCE = new TransitionInstance(NO_COMPLEX_MAPPINGS, 1);
    
    private List<Transition> finiteRateTransitions = new ArrayList<Transition>();
    private List<Transition> infiniteRateTransitions = new ArrayList<Transition>();
    private final List<Perturbation> perturbations = new ArrayList<Perturbation>();
    final Map<Transition, Boolean> infiniteRateTransitionActivityMap = new HashMap<Transition, Boolean>();
    final Map<Transition, Float> finiteRateTransitionActivityMap = new HashMap<Transition, Float>();
    final Map<Variable, Integer> transitionsFiredMap = new HashMap<Variable, Integer>();
    private final Map<Complex, List<Complex>> complexComponentMap = new HashMap<Complex, List<Complex>>();
    private final Map<Complex, List<Transition>> complexTransitionMap = new HashMap<Complex, List<Transition>>();
    private final Map<Complex, List<ComplexMapping>> componentComplexMappingMap = new HashMap<Complex, List<ComplexMapping>>();
    final Map<Transition, List<TransitionInstance>> transitionInstanceMap = new HashMap<Transition, List<TransitionInstance>>();
    final Map<Complex, Integer> complexStore = new HashMap<Complex, Integer>();
    private final Map<Variable, List<ObservableMapValue>> observableComplexMap = new HashMap<Variable, List<ObservableMapValue>>();
    
    private boolean stop = false;
    private boolean noTransitionsPossible = false;
    private float time = 0;
    private long startTime;
    private int eventCount = 0;
    
    private final IKappaModel kappaModel;
    private final List<ObservationListener> observationListeners = new ArrayList<ObservationListener>();
    private final ComplexMatcher matcher = new ComplexMatcher();

    
//    private final Map<Location, List<Transition>> emptySubstrateTransitionMap = new HashMap<Location, List<Transition>>();


    public TransitionMatchingSimulation(IKappaModel kappaModel) {
        this.kappaModel = kappaModel;
        
        for (Map.Entry<Complex, Integer> entry : kappaModel.getFixedLocatedInitialValuesMap().entrySet()) {
            complexStore.put(entry.getKey(), entry.getValue());
        }

        for (Variable variable : kappaModel.getVariables().values()) {
            if (variable.type == Type.KAPPA_EXPRESSION) {
                observableComplexMap.put(variable, new ArrayList<ObservableMapValue>());
            }
        }

        for (Transition transition : kappaModel.getTransitions()) {
            if (transition.getRate().isInfinite(kappaModel.getVariables())) {
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
            resetTransitionsFiredCount();
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
            resetTransitionsFiredCount();
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

//    private void addCellValue(Object cellValues, float quantity, CellIndexExpression[] indices) {
//        Object slice = cellValues;
//        for (int index = 0; index < indices.length - 1; index++) {
//            slice = ((Object[]) slice)[indices[index].evaluateIndex(NO_VARIABLES)];
//        }
//        int index = indices[indices.length - 1].evaluateIndex(NO_VARIABLES);
//        ((float[]) slice)[index] = ((float[]) slice)[index] + quantity;
//    }

    private void resetTransitionsFiredCount() {
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
        Transition transition = pickFiniteRateTransition();
        if (transition == null) {
            noTransitionsPossible = true;
            return false;
        }

        return applyTransition(transition, true);
    }

    private void applyInfiniteRateTransitions() {
        int clashes = 0;
        while (clashes < 1000 && !stop) {
            Transition transition = pickInfiniteRateTransition();
            if (transition == null) {
                return;
            }
            if (applyTransition(transition, false)) {
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
                if (perturbation.isUntilConditionMet(this)) {
                    iter.remove();
                }   
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

    private Transition pickFiniteRateTransition() {
        float totalQuantity = 0;
        if (finiteRateTransitionActivityMap.size() == 0) {
            return null;
        }
        for (Map.Entry<Transition, Float> entry : finiteRateTransitionActivityMap.entrySet()) {
            totalQuantity += entry.getValue();
        }
        Transition lastTransition = null;
        float item = (float) (totalQuantity * Math.random());
        for (Map.Entry<Transition, Float> entry : finiteRateTransitionActivityMap.entrySet()) {
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

    private Transition pickInfiniteRateTransition() {
        float totalCount = 0;
        if (infiniteRateTransitionActivityMap.size() == 0) {
            return null;
        }
        for (Map.Entry<Transition, Boolean> entry : infiniteRateTransitionActivityMap.entrySet()) {
            if (entry.getValue()) {
                totalCount++;
            }
        }
        float item = (float) (totalCount * Math.random());
        for (Map.Entry<Transition, Boolean> entry : infiniteRateTransitionActivityMap.entrySet()) {
            if (entry.getValue() && item <= 1) {
                return entry.getKey();
            }
            if (entry.getValue()) {
                item--;
            }
        }
        return null;
    }


    private void updateTransitionActivity(Transition transition, boolean rateChanged) {
        if (rateChanged) {
            if (transition.isInfiniteRate(kappaModel.getVariables())) {
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

        int totalTransitionActivity = getTotalTransitionActivity(transition);
        if (transition.isInfiniteRate(kappaModel.getVariables())) {
            infiniteRateTransitionActivityMap.put(transition, totalTransitionActivity > 0);
        }
        else {
            float totalTransitionRate = transition.getRate().evaluate(this).value;
            totalTransitionRate *= totalTransitionActivity;
            finiteRateTransitionActivityMap.put(transition, totalTransitionRate);
        }
    }

    int getTotalTransitionActivity(Transition transition) {
        int totalComponentActivity = 0;
        if (transition.sourceComplexes.size() >  0 || transition.channelName != null) {
            List<TransitionInstance> transitionInstances = transitionInstanceMap.get(transition);
            for (TransitionInstance transitionInstance : transitionInstances) {
                int componentActivity = 1;
                for (Map.Entry<Complex, Integer> countEntry : transitionInstance.requiredComplexCounts.entrySet()) {
                    int availableCount = complexStore.get(countEntry.getKey());
                    if (countEntry.getValue() > availableCount) {
                        componentActivity = 0;
                        break;
                    }
                    for (int index=0; index < countEntry.getValue(); index++) {
                        componentActivity *= (availableCount--);
                    }
                }
                
                componentActivity *= transitionInstance.targetLocationCount;
                totalComponentActivity += componentActivity;
            }
        }
        else {
            totalComponentActivity = 1;
        }
        return totalComponentActivity;
    }

    private Set<Transition> getAllTransitions() {
        Set<Transition> result = new HashSet<Transition>(finiteRateTransitions);
        result.addAll(infiniteRateTransitions);
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

    private void addComplexToObservables(Complex complex) {
        for (Map.Entry<Variable, List<ObservableMapValue>> entry : observableComplexMap.entrySet()) {
            int matchCount = matcher.getPartialMatches(entry.getKey().complex, complex).size();
            if (matchCount > 0) {
                entry.getValue().add(new ObservableMapValue(complex, matchCount));
            }
        }
    }

    private void removeComplexFromObservables(Complex complex) {
        for (Map.Entry<Variable, List<ObservableMapValue>> entry : observableComplexMap.entrySet()) {
            ObservableMapValue found = null;
            for (ObservableMapValue current : entry.getValue()) {
                if (complex == current.complex) {
                    found = current;
                    break;
                }
            }
            if (found != null) {
                entry.getValue().remove(found);
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
            for (Complex complex : complexStore.keySet()) {
                int instanceCount = 0;
                for (Agent currentAgent : complex.agents) {
                    if (agentName.equals(currentAgent.name)) {
                        instanceCount++;
                    }
                }
                count += complexStore.get(complex) * instanceCount;
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
        if (agents == null) {
            throw new NullPointerException();
        }
        
        if (amount == 0) {
            return;
        }
        
        List<Complex> complexes = getComplexes(agents);
        for (Complex complex : complexes) {
            Complex canonicalComplex = getCanonicalComplex(complex);
            if (canonicalComplex == null) {
                if (amount > 0) {
                    complexStore.put(complex, amount);
                    increaseTransitionActivities(complex, true);
                }
            }
            else {
                int quantity = Math.max(0, complexStore.get(canonicalComplex) + amount);
                complexStore.put(canonicalComplex, quantity);
                if (amount > 0) {
                    increaseTransitionActivities(canonicalComplex, false);
                }
                else {
                    reduceTransitionActivities(canonicalComplex);
                }
            }
        }
    }

    public void setTransitionRate(String transitionName, VariableExpression rateExpression) {
        Transition transition = getTransition(transitionName);
        if (transition != null) {
            transition.setRate(rateExpression);
            updateTransitionActivity(transition, true);
        }
    }

    public static class ObservableMapValue {

        public final Complex complex;
        public final int count;

        public ObservableMapValue(Complex complex, int count) {
            this.complex = complex;
            this.count = count;
        }

        @Override
        public String toString() {
            return complex.toString();
        }

    }

    private void initialiseActivityMaps() {
        for (Transition transition : getAllTransitions()) {
            transitionInstanceMap.put(transition, new ArrayList<TransitionInstance>());
            if (transition.sourceComplexes.size() > 0) {
                for (Complex component : transition.sourceComplexes) {
                    componentComplexMappingMap.put(component, new ArrayList<ComplexMapping>());
                }
            }
            else { // transition without source complexes
//                List<Transition> transitionMap = emptySubstrateTransitionMap.get(transition.sourceLocation);
//                if (transitionMap == null) {
//                    transitionMap = new ArrayList<Transition>();
//                    emptySubstrateTransitionMap.put(transition.sourceLocation, transitionMap);
//                }
//                transitionMap.add(transition);
                updateTransitionActivity(transition, false);
            }
        }

        for (Complex complex : complexStore.keySet()) {
            increaseTransitionActivities(complex, true);
        }
    }


    public String getDebugOutput() {
        StringBuilder builder = new StringBuilder();
        builder.append("Runtime (s): " + (time / 1000) + "\n");
        builder.append("Distinct complexes: " + complexStore.size() + "\n");
        builder.append("Final counts: " + getCurrentObservation(true, 1f) + "\n");

        builder.append("Final all counts:" + "\n");
        
        List<Complex> complexes = new ArrayList<Complex>(complexStore.keySet());
        Collections.sort(complexes, new Comparator<Complex>() {
            public int compare(Complex o1, Complex o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
    
        for (Complex complex : complexes) {
            builder.append(complexStore.get(complex) + "\t" + complex + "\n");
        }
        
        builder.append("\nKappa Init Section Format:\n");
        
        for (Complex complex : complexes) {
            builder.append("%init:");
            builder.append(" " + complexStore.get(complex) + " ");
            builder.append(complex.agents.get(0).toString());
            for (int index = 1; index < complex.agents.size(); index++) {
                builder.append(",").append(complex.agents.get(index).toString());
            }

            builder.append("\n");
        }
            
        
        builder.append("\nFinal count per agent:" + "\n");
        for (Map.Entry<String, Integer> entry : getCountsPerAgent().entrySet()) {
            builder.append(entry.getValue() + "\t" + entry.getKey() + "\n");
        }
        return builder.toString();
    }


    private boolean applyTransition(Transition transition, boolean incrementTime) {

        TransitionInstance concreteInstance = EMPTY_TRANSITION_INSTANCE;
        
        if (transition.sourceComplexes.size() > 0 || transition.channelName != null) {
            concreteInstance = pickTransitionInstance(transition);
            if (concreteInstance == null) {
                return false;
            }
        }
        
        if (incrementTime) {
            time += getTimeDelta();
        }

        if (transition.sourceComplexes.size() > 0 || transition.channelName != null) {
            for (ComplexMapping complexMapping : concreteInstance.sourceMapping) {
                int quantity = complexStore.get(complexMapping.target) - 1;
                complexStore.put(complexMapping.target, quantity);
                reduceTransitionActivities(complexMapping.target);
            }
        }
        
        incrementTransitionsFired(transition);

        List<Complex> resultComplexes = transition.apply(concreteInstance, kappaModel.getChannels(), kappaModel.getCompartments());
        
        for (Complex complex : resultComplexes) {
            Complex canonicalComplex = getCanonicalComplex(complex);
            if (canonicalComplex == null) {
                complexStore.put(complex, 1);
                increaseTransitionActivities(complex, true);
            }
            else {
                complexStore.put(canonicalComplex, complexStore.get(canonicalComplex) + 1);
                increaseTransitionActivities(canonicalComplex, false);
            }
        }

        return true;
    }

    private Complex getCanonicalComplex(Complex complex) {
        for (Complex current : complexStore.keySet()) {
            if (matcher.isExactMatch(complex, current)) {
                return current;
            }
        }
        return null;
    }

    TransitionInstance pickTransitionInstance(Transition transition) {
        List<TransitionInstance> transitionInstances = transitionInstanceMap.get(transition);
        if (transitionInstances.size() == 0) {
            return null;
        }

        int[] allTransitionInstanceActivities = new int[transitionInstances.size()];
        int totalTransitionInstanceActivity = 0;
        for (int index = 0; index < allTransitionInstanceActivities.length; index++) {
            TransitionInstance transitionInstance = transitionInstances.get(index);
            int activity = 1;
            for (Map.Entry<Complex, Integer> countEntry : transitionInstance.requiredComplexCounts.entrySet()) {
                int availableCount = complexStore.get(countEntry.getKey());
                if (countEntry.getValue() > availableCount) {
                    activity = 0;
                    break;
                }
                for (int index2=0; index2 < countEntry.getValue(); index2++) {
                    activity *= (availableCount--);
                }
            }

            activity *= transitionInstance.targetLocationCount;
            allTransitionInstanceActivities[index] = activity;
            totalTransitionInstanceActivity += activity;
        }
        
        
        TransitionInstance lastInstance = null;
        int randomValue = (int) (totalTransitionInstanceActivity * Math.random());
        for (int index = 0; index < allTransitionInstanceActivities.length; index++) {
            if (allTransitionInstanceActivities[index] > 0) {
                lastInstance = transitionInstances.get(index);
                if (randomValue < allTransitionInstanceActivities[index]) {
                    break;
                }
                randomValue -= allTransitionInstanceActivities[index];
            }
        }
        return lastInstance;
    }

    private void increaseTransitionActivities(Complex complex, boolean isNewComplex) {
        if (isNewComplex) {
            List<Complex> affectedTransitionComponents = new ArrayList<Complex>();
            List<Transition> affectedTransitions = new ArrayList<Transition>();
            complexComponentMap.put(complex, affectedTransitionComponents);
            complexTransitionMap.put(complex, affectedTransitions);
    
            for (Transition transition : getAllTransitions()) {
                boolean found = false;
                List<TransitionInstance> newTransitionInstances = new ArrayList<TransitionInstance>();
                for (Complex component : transition.sourceComplexes) {
                    List<ComplexMapping> mappings = matcher.getPartialMatches(component, complex);
                    if (mappings.size() > 0) {
                        affectedTransitionComponents.add(component);
                        found = true;
                        newTransitionInstances.addAll(getNewTransitionInstances(transition, mappings, componentComplexMappingMap,
                                complexStore, kappaModel.getChannels(), kappaModel.getCompartments()));
                        componentComplexMappingMap.get(component).addAll(mappings);
                    }
                }
                if (transition.sourceComplexes.size() == 0 && transition.channelName != null) {
                    // Unspecified source complex
                    Location complexLocation = complex.getSingleLocation();
                    if (complexLocation != null && transition.leftLocation.isRefinement(complexLocation)) {
                        Channel channel = kappaModel.getChannel(transition.channelName);
                        List<Location> targetLocations = channel.applyChannel(complexLocation, transition.rightLocation, kappaModel.getCompartments());
                        if (targetLocations.size() > 0) {
                            // TODO - check move can actually be applied
                            newTransitionInstances.add(new TransitionInstance(
                                    getList(new ComplexMapping(complex)), targetLocations.size()));
                            found = true;
                        }
                    }
                }
                if (found) {
                    affectedTransitions.add(transition);
                    transitionInstanceMap.get(transition).addAll(newTransitionInstances);
                    updateTransitionActivity(transition, false);
                }
            }
    
            addComplexToObservables(complex);
    
    //        List<Transition> transitionMap = emptySubstrateTransitionMap.get(location);
    //        if (transitionMap != null) {
    //            for (Transition transition : transitionMap) {
    //                updateTransitionActivity(transition, false);
    //            }
    //        }
        }
        else {
            List<Transition> affectedTransitions = complexTransitionMap.get(complex);
            for (Transition transition : affectedTransitions) {
                updateTransitionActivity(transition, false);
            }
        }
    }

    List<TransitionInstance> getNewTransitionInstances(Transition transition, 
            List<ComplexMapping> newComponentComplexMappings, 
            Map<Complex, List<ComplexMapping>> allComponentComplexMappings,
            Map<Complex, Integer> complexCounts, List<Channel> channels, List<Compartment> compartments) {
        
        if (transition == null || newComponentComplexMappings == null || allComponentComplexMappings == null
                || complexCounts == null || channels == null || compartments == null) {
            throw new NullPointerException();
        }
        if (newComponentComplexMappings.size() == 0) {
            return NO_TRANSITION_INSTANCES;
        }
        
        Complex newComponent = newComponentComplexMappings.get(0).template;
        List<Complex> remainingComponents = new ArrayList<Complex>(transition.sourceComplexes);
        remainingComponents.remove(newComponent);
        
        List<List<ComplexMapping>> newMappings = new ArrayList<List<ComplexMapping>>();
        for (ComplexMapping mapping : newComponentComplexMappings) {
            newMappings.add(getList(mapping));
        }
        
        while (remainingComponents.size() > 0) {
            List<List<ComplexMapping>> mappings = newMappings;
            newMappings = new ArrayList<List<ComplexMapping>>();
            Complex currentComponent = remainingComponents.get(0);
            remainingComponents.remove(currentComponent);
            
            List<ComplexMapping> currentComponentComplexMappings = allComponentComplexMappings.get(currentComponent);
            if (currentComponentComplexMappings == null || currentComponentComplexMappings.size() == 0) {
                return NO_TRANSITION_INSTANCES;
            }
            for (ComplexMapping currentComponentComplexMapping : currentComponentComplexMappings) {
                for (List<ComplexMapping> currentComplexMapping : mappings) {
                    
                    if (isTransitionMappingComponentCompatible(transition, currentComplexMapping, 
                            currentComponentComplexMapping, channels, compartments)) {
                        List<ComplexMapping> extendedComplexMapping = new ArrayList<ComplexMapping>(currentComplexMapping);
                        extendedComplexMapping.add(currentComponentComplexMapping);
                        newMappings.add(extendedComplexMapping);
                    }
                }
            }
            if (newMappings.size() == 0) {
                return NO_TRANSITION_INSTANCES;
            }
        }
        
        List<TransitionInstance> result = new ArrayList<TransitionInstance>();
        // Check compatibility of complex locations
        for (List<ComplexMapping> currentMappings : newMappings) {
            int locationCount = transition.getApplicationCount(currentMappings, channels, compartments);
            if (locationCount > 0) {
                result.add(new TransitionInstance(currentMappings, locationCount));
            }
        }
        
        return result;
    }

    boolean isTransitionMappingComponentCompatible(Transition transition,
            List<ComplexMapping> complexMappings, ComplexMapping componentComplexMapping,
            List<Channel> channels, List<Compartment> compartments) {
        
        if (transition == null || complexMappings == null || componentComplexMapping == null || channels == null || 
                compartments == null) {
            throw new NullPointerException();
        }
        for (TransitionPrimitive primitive : transition.bestPrimitives) {
            if (primitive.type == TransitionPrimitive.Type.CREATE_LINK) {
                if (primitive.targetSite == AgentLink.ANY || primitive.targetSite == AgentLink.NONE || primitive.targetSite == AgentLink.OCCUPIED) {
                    continue;
                }
                Agent templateSourceAgent = primitive.sourceSite.agent;
                Agent templateTargetAgent = primitive.targetSite.agent;
                Complex templateSourceComplex = templateSourceAgent.getComplex();
                Complex templateTargetComplex = templateTargetAgent.getComplex();

                if (templateSourceComplex != componentComplexMapping.template && templateTargetComplex != componentComplexMapping.template) {
                    continue;
                }
                if (templateTargetComplex == componentComplexMapping.template) {
                    templateSourceAgent = primitive.targetSite.agent;
                    templateTargetAgent = primitive.sourceSite.agent;
                    templateSourceComplex = templateSourceAgent.getComplex();
                    templateTargetComplex = templateTargetAgent.getComplex();
                }
                
                ComplexMapping targetMapping = null;
                for (ComplexMapping mapping : complexMappings) {
                    if (templateTargetComplex == mapping.template) {
                        targetMapping = mapping;
                        break;
                    }
                }
                if (targetMapping == null) {
                    continue;
                }
                
                Agent realSourceAgent = componentComplexMapping.mapping.get(templateSourceAgent);
                Agent realTargetAgent = targetMapping.mapping.get(templateTargetAgent);
                Location realSourceLocation = realSourceAgent.location;
                Location realTargetLocation = realTargetAgent.location;
                
                if (primitive.channelName == null) {
                    // Must be co located
                    if (!realSourceLocation.equals(realTargetLocation)) {
                        return false;
                    }
                }
                else {
                    Channel channel = getChannel(channels, primitive.channelName);
                    if (channel.applyChannel(realSourceLocation, realTargetLocation, compartments).size() == 0) {
                        return false;
                    }
                }
            }
        }
        
        // TODO handle unlinked complex groups
        return true;
    }

    void removeTransitionInstances(List<TransitionInstance> transitionInstances, Complex complex) {
        if (transitionInstances == null || complex == null) {
            throw new NullPointerException();
        }
        ListIterator<TransitionInstance> iter = transitionInstances.listIterator();
        while (iter.hasNext()) {
            TransitionInstance transitionInstance = iter.next();
            if (transitionInstance.requiredComplexCounts.containsKey(complex)) {
                iter.remove();
            }
        }
    }

    private void reduceTransitionActivities(Complex complex) {
        List<Transition> affectedTransitions = complexTransitionMap.get(complex);
        int quantity = complexStore.get(complex);

        if (quantity == 0) {
            List<Complex> affectedTransitionComponents = complexComponentMap.get(complex);
            complexComponentMap.remove(complex);
            complexTransitionMap.remove(complex);
    
            for (Complex transitionComponent : affectedTransitionComponents) {
                ListIterator<ComplexMapping> iter = componentComplexMappingMap.get(transitionComponent).listIterator();
                while (iter.hasNext()) {
                    ComplexMapping complexMapping = iter.next();
                    if (complexMapping.target == complex) {
                        iter.remove();
                    }
                }
            }
            
            for (Transition transition : affectedTransitions) {
                removeTransitionInstances(transitionInstanceMap.get(transition), complex);
            }
            
            removeComplexFromObservables(complex);
            
            complexStore.remove(complex);
        }
    
        for (Transition transition : affectedTransitions) {
            updateTransitionActivity(transition, false);
        }
//        List<Transition> transitionMap = emptySubstrateTransitionMap.get(location);
//        if (transitionMap != null) {
//            for (Transition transition : transitionMap) {
//                updateTransitionActivity(transition, false);
//            }
//        }
    }


    public ObservationElement getComplexQuantity(Variable variable) {
        if (variable == null) {
            throw new NullPointerException();
        }
        int value = 0;
//        int[] dimensions = null;
//        Serializable cellValues = null;
//        boolean partition = false;
        List<ObservableMapValue> complexes = observableComplexMap.get(variable);
        if (complexes != null) {

            for (ObservableMapValue current : complexes) {
                value += current.count * complexStore.get(current.complex);
            }
            
            // TODO - grid observations - currently disabled
//            if (variable.location != NOT_LOCATED) {
//                Compartment compartment = variable.location.getReferencedCompartment(kappaModel.getCompartments());
//                if (compartment.getDimensions().length != variable.location.getIndices().length) {
//                    partition = true;
//                    dimensions = compartment.getDimensions();
//                    cellValues = compartment.createValueArray();
//                }
//
//                for (ObservableMapValue current : complexes) {
//                    int quantity = current.count;
//                    value += quantity;
//                    if (partition) {
//                        addCellValue(cellValues, quantity, current.location.getIndices());
//                    }
//                }
//                if (partition) {
//                    return new ObservationElement(value, dimensions, compartment.getName(), cellValues);
//                }
//            }
//            else { // No compartment
//                for (ObservableMapValue current : complexes) {
//                    value += current.count;
//                }
//            }
        }
        return new ObservationElement(value);
    }

    
}
