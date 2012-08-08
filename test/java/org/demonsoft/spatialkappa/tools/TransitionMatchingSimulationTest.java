package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.AggregateAgent;
import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.demonsoft.spatialkappa.model.Channel;
import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.ComplexMapping;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.Transition;
import org.demonsoft.spatialkappa.model.TransitionInstance;
import org.demonsoft.spatialkappa.model.Variable;
import org.demonsoft.spatialkappa.model.VariableExpression;
import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.junit.Before;
import org.junit.Test;

public class TransitionMatchingSimulationTest {

    private KappaModel kappaModel = new KappaModel();
    private TransitionMatchingSimulation simulation;

    @Before
    public void setUp() {
        simulation = new TransitionMatchingSimulation(kappaModel);
    }
    
    @Test
    public void testGetCurrentObservation_noCompartments() {
        
        // Empty observation
        Observation observation = simulation.getCurrentObservation();
        assertNotNull(observation);
        assertEquals(0, observation.observables.size());
        
        // Add observables and variables
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent2"));
        kappaModel.addTransition("label", NOT_LOCATED, agents, null, NOT_LOCATED, new ArrayList<Agent>(), new VariableExpression(0.1f));
        kappaModel.addPlot("label");
        
        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addVariable(agents, "label2", NOT_LOCATED);
        kappaModel.addPlot("label2");
        
        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent2"));
        kappaModel.addVariable(agents, "label3", NOT_LOCATED);
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        observation = simulation.getCurrentObservation();
        checkObservation(observation, "label", "label2");
    }

    @Test
    public void testGetCurrentObservation_singleCellCompartment() {
        kappaModel.addAgentDeclaration(new AggregateAgent("agent1"));
        kappaModel.addAgentDeclaration(new AggregateAgent("agent2"));

        kappaModel.addCompartment("cytosol", null, new ArrayList<Integer>());
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(1);
        kappaModel.addCompartment("nucleus", null, dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addVariable(agents, "observable1", new Location("cytosol"));
        kappaModel.addPlot("observable1");
        kappaModel.addInitialValue(agents, "5", new Location("cytosol"));
        
        agents.clear();
        agents.add(new Agent("agent2"));
        kappaModel.addVariable(agents, "observable2", new Location("nucleus"));
        kappaModel.addPlot("observable2");
        kappaModel.addInitialValue(agents, "7", new Location("nucleus"));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkObservation("observable1", new ObservationElement(5));
        // TODO - grid observations - currently disabled
//        checkObservation("observable2", new ObservationElement(7, new int[] {1}, "cytosol", new float[] {7}));
        checkObservation("observable2", new ObservationElement(7));
    }

    @Test
    public void testGetCurrentObservation_1DCompartment() {
        kappaModel.addAgentDeclaration(new AggregateAgent("agent1"));
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(4);
        kappaModel.addCompartment("cytosol", null, dimensions);
        
        kappaModel.addVariable(getList(new Agent("agent1")), "observable1", new Location("cytosol"));
        kappaModel.addPlot("observable1");
        kappaModel.addVariable(getList(new Agent("agent1")), "observable2", new Location("cytosol", new CellIndexExpression("0")));
        kappaModel.addPlot("observable2");
        kappaModel.addInitialValue(getList(new Agent("agent1")), "5", new Location("cytosol", new CellIndexExpression("0")));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "7", new Location("cytosol", new CellIndexExpression("3")));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkObservation("observable2", new ObservationElement(5));
        // TODO - grid observations - currently disabled
//        checkObservation("observable1", new ObservationElement(12, new int[] {4}, "cytosol", new float[] {5, 0, 0, 7}));
        checkObservation("observable1", new ObservationElement(12));
    }

    @Test
    public void testGetCurrentObservation_2DCompartment() {
        kappaModel.addAgentDeclaration(new AggregateAgent("agent1"));
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(3);
        dimensions.add(2);
        kappaModel.addCompartment("cytosol", null, dimensions);
        
        kappaModel.addVariable(getList(new Agent("agent1")), "observable1", new Location("cytosol"));
        kappaModel.addPlot("observable1");
        kappaModel.addVariable(getList(new Agent("agent1")), "observable2", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("0")));
        kappaModel.addPlot("observable2");
        kappaModel.addInitialValue(getList(new Agent("agent1")), "5", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("0")));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "7", new Location("cytosol", new CellIndexExpression("2"), new CellIndexExpression("1")));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        // TODO - grid observations - currently disabled
//        checkObservation("observable1", new ObservationElement(12, new int[] {3, 2}, "cytosol", new float[][] {{5, 0}, {0, 0}, {0, 7}}));
        checkObservation("observable1", new ObservationElement(12));
        checkObservation("observable2", new ObservationElement(5));
    }

    private void checkObservation(String observableName, ObservationElement element) {
        Observation observation = simulation.getCurrentObservation();
        ObservationElement actual = observation.observables.get(observableName);
        assertEquals(element.toString(), actual.toString());
    }
    
    private void checkGetQuantity(Variable variable, ObservationElement element) {
        ObservationElement actual = simulation.getComplexQuantity(variable);
        assertEquals(element.toString(), actual.toString());
    }
    
    private void checkObservation(Observation observation, String... expectedObservableNames) {
        assertNotNull(observation);
        assertEquals(expectedObservableNames.length, observation.observables.size());
        
        Set<String> actualNames = new HashSet<String>(observation.observables.keySet());
        
        for (String current : expectedObservableNames) {
            assertTrue("Missing observable: " + current, actualNames.contains(current));
        }
    }
    
    private void checkQuantity(String label, int expected, Agent...agents) {
        kappaModel.addVariable(Arrays.asList(agents), label, NOT_LOCATED);
        simulation = new TransitionMatchingSimulation(kappaModel);
        Variable variable = simulation.getVariable(label);
        assertEquals(new ObservationElement(expected), simulation.getComplexQuantity(variable));
    }

    @Test
    public void testGetNextEndTime() {
        assertEquals(2.0, simulation.getNextEndTime(1.01f, 1f), 0.05f);
        assertEquals(10.0, simulation.getNextEndTime(7.51f, 2.5f), 0.05f);
    }


    @Test
    public void testGetQuantity_noCompartments() {
        kappaModel.addAgentDeclaration(new AggregateAgent("agent1"));
        kappaModel.addAgentDeclaration(new AggregateAgent("agent2"));
        kappaModel.addAgentDeclaration(new AggregateAgent("agent3"));
        kappaModel.addAgentDeclaration(new AggregateAgent("agent4"));
        kappaModel.addAgentDeclaration(new AggregateAgent("agent5"));
        
        try {
            simulation.getComplexQuantity(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1", new AgentSite("x", null, null)));
        agents.add(new Agent("agent4", new AgentSite("y", "s", null)));
        kappaModel.addInitialValue(agents, "3", NOT_LOCATED);

        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1", new AgentSite("x", null, "1")));
        agents.add(new Agent("agent2", new AgentSite("x", null, "1")));
        agents.add(new Agent("agent3", new AgentSite("x", null, "2")));
        agents.add(new Agent("agent4", new AgentSite("x", null, "2"), new AgentSite("y", null, null)));
        kappaModel.addInitialValue(agents, "5", NOT_LOCATED);
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkQuantity("agent1()", 8, new Agent("agent1"));
        checkQuantity("agent4()", 8, new Agent("agent4"));
        checkQuantity("agent2()", 5, new Agent("agent2"));
        checkQuantity("agent5()", 0, new Agent("agent5"));
        checkQuantity("agent1(x)", 3, new Agent("agent1", new AgentSite("x", null, null)));
        checkQuantity("agent1(x!_)", 5, new Agent("agent1", new AgentSite("x", null, "_")));
        checkQuantity("agent1(x?)", 8, new Agent("agent1", new AgentSite("x", null, "?")));
        checkQuantity("agent4(y~s)", 3, new Agent("agent4", new AgentSite("y", "s", null)));
        checkQuantity("agent4(y)", 8, new Agent("agent4", new AgentSite("y", null, null)));
        checkQuantity("agent1(x!1),agent2(x!1)", 5, new Agent("agent1", new AgentSite("x", null, "1")),
                new Agent("agent2", new AgentSite("x", null, "1")));
        checkQuantity("agent1(x!2),agent2(x!2)", 5, new Agent("agent1", new AgentSite("x", null, "2")),
                new Agent("agent2", new AgentSite("x", null, "2")));
        checkQuantity("agent1(x!1),agent3(x!1)", 0, new Agent("agent1", new AgentSite("x", null, "1")),
                new Agent("agent3", new AgentSite("x", null, "1")));
    }
    
    @Test
    public void testGetQuantity_singleCellCompartment() {
        kappaModel.addAgentDeclaration(new AggregateAgent("agent1"));
        kappaModel.addAgentDeclaration(new AggregateAgent("agent2"));
        kappaModel.addCompartment("cytosol", null, new ArrayList<Integer>());
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(1);
        kappaModel.addCompartment("nucleus", null, dimensions);
        
        List<Agent> agents = getList(new Agent("agent1"));
        kappaModel.addVariable(agents, "observable1", new Location("cytosol"));
        kappaModel.addInitialValue(agents, "5", new Location("cytosol"));
        
        agents.clear();
        agents.add(new Agent("agent2"));
        kappaModel.addVariable(agents, "observable2", new Location("nucleus"));
        kappaModel.addInitialValue(agents, "7", new Location("nucleus"));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(5));
        // TODO - grid observations - currently disabled
//        checkGetQuantity(simulation.getVariable("observable2"), new ObservationElement(7, new int[] {1}, "cytosol", new float[] {7}));
        checkGetQuantity(simulation.getVariable("observable2"), new ObservationElement(7));
    }

    @Test
    public void testGetQuantity_1DCompartment() {
        kappaModel.addAgentDeclaration(new AggregateAgent("agent1"));
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(4);
        kappaModel.addCompartment("cytosol", null, dimensions);
        
        kappaModel.addVariable(getList(new Agent("agent1")), "observable1", new Location("cytosol"));
        kappaModel.addVariable(getList(new Agent("agent1")), "observable2", new Location("cytosol", new CellIndexExpression("0")));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "5", new Location("cytosol", new CellIndexExpression("0")));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "7", new Location("cytosol", new CellIndexExpression("3")));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        // TODO - grid observations - currently disabled
//        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(12, new int[] {4}, "cytosol", new float[] {5, 0, 0, 7}));
        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(12));
        checkGetQuantity(simulation.getVariable("observable2"), new ObservationElement(5));
    }

    @Test
    public void testGetQuantity_2DCompartment() {
        kappaModel.addAgentDeclaration(new AggregateAgent("agent1"));
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(3);
        dimensions.add(2);
        kappaModel.addCompartment("cytosol", null, dimensions);
        
        kappaModel.addVariable(getList(new Agent("agent1")), "observable1", new Location("cytosol"));
        kappaModel.addVariable(getList(new Agent("agent1")), "observable2", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("0")));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "5", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("0")));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "7", new Location("cytosol", new CellIndexExpression("2"), new CellIndexExpression("1")));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        // TODO - grid observations - currently disabled
//        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(12, new int[] {3, 2}, "cytosol", new float[][] {{5, 0}, {0, 0}, {0, 7}}));
        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(12));
        checkGetQuantity(simulation.getVariable("observable2"), new ObservationElement(5));
    }

    @Test
    public void testGetTransitionFiredCount() {
        try {
            simulation.getTransitionFiredCount(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.getTransitionFiredCount(new Variable(new VariableExpression(2), "not a transition"));
            fail("invalid type should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        simulation.transitionsFiredMap.put(new Variable("label"), 55);
        
        assertEquals(new ObservationElement(55), simulation.getTransitionFiredCount(new Variable("label")));
        assertEquals(new ObservationElement(0), simulation.getTransitionFiredCount(new Variable("other")));
    }

    @Test
    public void testGetNewTransitionInstances() {
        List<ComplexMapping> newComponentComplexMappings = new ArrayList<ComplexMapping>();
        Map<Complex, List<ComplexMapping>> allComponentComplexMappings = new HashMap<Complex, List<ComplexMapping>>();
        Complex complex = new Complex(new Agent("agent"));
        List<Agent> sourceAgents = getList(new Agent("agent"));
        List<Agent> targetAgents = getList(new Agent("agent"));
        Transition transition = new Transition(null, sourceAgents, null, targetAgents, 1f);
        Map<Agent, Agent> agentMap = new HashMap<Agent, Agent>();
        List<Compartment> compartments = new ArrayList<Compartment>();
        List<Channel> channels = new ArrayList<Channel>();
        Map<Complex, Integer> complexCounts = new HashMap<Complex, Integer>();
        complexCounts.put(complex, 1);
        
        try {
            simulation.getNewTransitionInstances(null, newComponentComplexMappings, allComponentComplexMappings, complexCounts, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            simulation.getNewTransitionInstances(transition, null, allComponentComplexMappings, complexCounts, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            simulation.getNewTransitionInstances(transition, newComponentComplexMappings, null, complexCounts, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.getNewTransitionInstances(transition, newComponentComplexMappings, allComponentComplexMappings, null, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.getNewTransitionInstances(transition, newComponentComplexMappings, allComponentComplexMappings, complexCounts, null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.getNewTransitionInstances(transition, newComponentComplexMappings, allComponentComplexMappings, complexCounts, channels, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // New mappings empty
        
        List<TransitionInstance> result = simulation.getNewTransitionInstances(
                transition, newComponentComplexMappings, allComponentComplexMappings, complexCounts, channels, compartments);
        List<TransitionInstance> expected = new ArrayList<TransitionInstance>();
        assertEquals(expected, result);
        
        // Single component match
        
        transition = new Transition(null, sourceAgents, null, targetAgents, 1f);
        Complex component = getComplexByString("[agent]", transition.sourceComplexes);
        
        newComponentComplexMappings = getList(new ComplexMapping(component, complex, agentMap));
        result = simulation.getNewTransitionInstances(transition, newComponentComplexMappings, allComponentComplexMappings, 
                complexCounts, channels, compartments);
        expected = getList(new TransitionInstance(newComponentComplexMappings, 1));
        assertEquals(expected, result);
        
        // Multi component match - no spatial constraints
        sourceAgents = getList(new Agent("agent"), new Agent("agent2"));
        targetAgents = getList(new Agent("agent"), new Agent("agent2"));
        transition = new Transition(null, sourceAgents, null, targetAgents, 1f);
        
        component = getComplexByString("[agent]", transition.sourceComplexes);
        newComponentComplexMappings = getList(new ComplexMapping(component, complex, agentMap));
        result = simulation.getNewTransitionInstances(transition, newComponentComplexMappings, allComponentComplexMappings, 
                complexCounts, channels, compartments);
        expected = new ArrayList<TransitionInstance>();
        assertEquals(expected, result);
        
        allComponentComplexMappings.put(component, newComponentComplexMappings);
        Complex complex2 = new Complex(new Agent("agent2"));
        complexCounts.put(complex2, 1);
        Complex component2 = getComplexByString("[agent2]", transition.sourceComplexes);
        newComponentComplexMappings = getList(new ComplexMapping(component2, complex2, agentMap));
        result = simulation.getNewTransitionInstances(transition, newComponentComplexMappings, allComponentComplexMappings, 
                complexCounts, channels, compartments);
        expected = getList(new TransitionInstance(getList(new ComplexMapping(component2, complex2, agentMap), 
                new ComplexMapping(component, complex, agentMap)), 1));
        assertEquals(expected, result);
        
        //TODO add spatial constraint tests
    }

    private Complex getComplexByString(String signature, List<Complex> complexes) {
        for (Complex complex : complexes) {
            if (complex.toString().equals(signature)) {
                return complex;
            }
        }
        fail("Complex " + signature + " not found in " + complexes);
        return null;
    }

    @Test
    public void testRemoveTransitionInstances() {
        List<TransitionInstance> transitionInstances = new ArrayList<TransitionInstance>();
        Complex complex = new Complex(new Agent("agent"));
        
        try {
            simulation.removeTransitionInstances(null, complex);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            simulation.removeTransitionInstances(transitionInstances, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Complex component = new Complex(new Agent("agent"));
        Map<Agent, Agent> agentMap = new HashMap<Agent, Agent>();
        List<ComplexMapping> notTargetMapping1 = getList(
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap));
        List<ComplexMapping> notTargetMapping2 = getList(
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap),
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap));
        List<ComplexMapping> targetMapping1 = getList(
                new ComplexMapping(component, complex, agentMap));
        List<ComplexMapping> targetMapping2 = getList(
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap),
                new ComplexMapping(component, complex, agentMap),
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap));
        
        transitionInstances = getList(
                new TransitionInstance(notTargetMapping1, 1), 
                new TransitionInstance(targetMapping1, 1), 
                new TransitionInstance(notTargetMapping2, 1), 
                new TransitionInstance(targetMapping2, 1));
        List<TransitionInstance> expectedInstances = getList(
                new TransitionInstance(notTargetMapping1, 1), 
                new TransitionInstance(notTargetMapping2, 1));
        
        simulation.removeTransitionInstances(transitionInstances, complex);
        assertEquals(expectedInstances, transitionInstances);
        
        transitionInstances.clear();
        expectedInstances.clear();
        
        simulation.removeTransitionInstances(transitionInstances, complex);
        assertEquals(expectedInstances, transitionInstances);
    }
    
    
    @Test
    public void testAddComplexInstances() {
        try {
            simulation.addComplexInstances(null, 5);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        kappaModel = new KappaModel();
        kappaModel.addTransition("A", NOT_LOCATED, getList(new Agent("A")), null, null, null, new VariableExpression(1f));
        kappaModel.addTransition("B", NOT_LOCATED, getList(new Agent("B")), null, null, null, new VariableExpression(1f));
        kappaModel.addTransition("C", NOT_LOCATED, getList(new Agent("C")), null, null, null, new VariableExpression(Constant.INFINITY));
        
        kappaModel.addVariable(getList(new Agent("A")), "A", NOT_LOCATED);
        kappaModel.addVariable(getList(new Agent("B")), "B", NOT_LOCATED);
        kappaModel.addVariable(getList(new Agent("C")), "C", NOT_LOCATED);
        
        Transition transitionA = kappaModel.getTransitions().get(0);
        Transition transitionB = kappaModel.getTransitions().get(1);
        Transition transitionC = kappaModel.getTransitions().get(2);
        
        Variable variableA = kappaModel.getVariables().get("A");
        Variable variableB = kappaModel.getVariables().get("B");
        Variable variableC = kappaModel.getVariables().get("C");
        
        simulation = new TransitionMatchingSimulation(kappaModel);

        simulation.addComplexInstances(getList(new Agent("A"), new Agent("B"), new Agent("C")), 10);
        
        assertEquals((Float) 10f, simulation.finiteRateTransitionActivityMap.get(transitionA));
        assertEquals((Float) 10f, simulation.finiteRateTransitionActivityMap.get(transitionB));
        assertEquals(true, simulation.infiniteRateTransitionActivityMap.get(transitionC));
        assertEquals(10f, simulation.getComplexQuantity(variableA).value, 0.01f);
        assertEquals(10f, simulation.getComplexQuantity(variableB).value, 0.01f);
        assertEquals(10f, simulation.getComplexQuantity(variableC).value, 0.01f);
        
        simulation.addComplexInstances(getList(new Agent("B")), -8);
        
        assertEquals((Float) 10f, simulation.finiteRateTransitionActivityMap.get(transitionA));
        assertEquals((Float) 2f, simulation.finiteRateTransitionActivityMap.get(transitionB));
        assertEquals(true, simulation.infiniteRateTransitionActivityMap.get(transitionC));
        assertEquals(10f, simulation.getComplexQuantity(variableA).value, 0.01f);
        assertEquals(2f, simulation.getComplexQuantity(variableB).value, 0.01f);
        assertEquals(10f, simulation.getComplexQuantity(variableC).value, 0.01f);
        
        simulation.addComplexInstances(getList(new Agent("A"), new Agent("B"), new Agent("C")), -10);
        
        assertEquals((Float) 0f, simulation.finiteRateTransitionActivityMap.get(transitionA));
        assertEquals((Float) 0f, simulation.finiteRateTransitionActivityMap.get(transitionB));
        assertEquals(false, simulation.infiniteRateTransitionActivityMap.get(transitionC));
        assertEquals(0f, simulation.getComplexQuantity(variableA).value, 0.01f);
        assertEquals(0f, simulation.getComplexQuantity(variableB).value, 0.01f);
        assertEquals(0f, simulation.getComplexQuantity(variableC).value, 0.01f);
    }
    
    
    @Test
    public void testGetTotalTransitionActivity() {
        try {
            simulation.getTotalTransitionActivity(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Transition transition = new Transition("moveAnything", (Location) null, "channel", null, 1f);
        Complex complex1 = new Complex(new Agent("agent1"));
        Complex complex2 = new Complex(new Agent("agent2"));
        Complex complex3 = new Complex(new Agent("agent3"));
        Complex complex4 = new Complex(new Agent("agent4"));
        simulation.complexStore.put(complex1, 3);
        simulation.complexStore.put(complex2, 7);
        simulation.complexStore.put(complex3, 11);
        simulation.complexStore.put(complex4, 2);
        
        
        // Single list of single mapping
        simulation.transitionInstanceMap.put(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1)), 1)));
        assertEquals(3, simulation.getTotalTransitionActivity(transition));
        
        // Single list of multiple mappings
        simulation.transitionInstanceMap.put(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1), new ComplexMapping(complex2)), 1)));
        assertEquals(21, simulation.getTotalTransitionActivity(transition));
        
        // Multiple lists of single mappings
        simulation.transitionInstanceMap.put(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1)), 1),
                        new TransitionInstance(getList(new ComplexMapping(complex2)), 1)));
        assertEquals(10, simulation.getTotalTransitionActivity(transition));
        
        // Multiple lists of multiple mappings
        simulation.transitionInstanceMap.put(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1), new ComplexMapping(complex2)), 1),
                        new TransitionInstance(getList(new ComplexMapping(complex3), new ComplexMapping(complex4)), 1)));
        assertEquals(43, simulation.getTotalTransitionActivity(transition));
        
        // Multiple target locations per mapping
        simulation.transitionInstanceMap.put(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1)), 2)));
        assertEquals(6, simulation.getTotalTransitionActivity(transition));
        
        transition = new Transition("createComplex", null, null, getList(new Agent("newAgent")), 1f);
        assertEquals(1, simulation.getTotalTransitionActivity(transition));
        
    }
    
    @Test
    public void testPickComplexMappings() {
        try {
            simulation.pickTransitionInstance(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Transition transition = new Transition("moveAnything", (Location) null, "channel", null, 1f);
        Complex complex1 = new Complex(new Agent("agent1"));
        Complex complex2 = new Complex(new Agent("agent2"));
        Complex complex3 = new Complex(new Agent("agent3"));
        Complex complex4 = new Complex(new Agent("agent4"));
        simulation.complexStore.put(complex1, 3);
        simulation.complexStore.put(complex2, 7);
        simulation.complexStore.put(complex3, 12);
        simulation.complexStore.put(complex4, 2);
        
        List<TransitionInstance> transitionInstances = getList(
                new TransitionInstance(getList(new ComplexMapping(complex1)), 1));
        simulation.transitionInstanceMap.put(transition, transitionInstances);
        Map<List<ComplexMapping>, Integer> expectedDistribution = new HashMap<List<ComplexMapping>, Integer>();
        checkPickTransitionInstance(transition, 100, 0, transitionInstances, new int[] {100});
        
        transitionInstances = getList(
                new TransitionInstance(getList(new ComplexMapping(complex3)), 1),
                new TransitionInstance(getList(new ComplexMapping(complex1), new ComplexMapping(complex4)), 1));
        simulation.transitionInstanceMap.put(transition, transitionInstances);
        checkPickTransitionInstance(transition, 10000, 150, transitionInstances, new int[] {6670, 3330});
        
        // Check even random distribution - with multiple locations per transition
        simulation.complexStore.put(complex1, 5);
        simulation.complexStore.put(complex2, 5);
        simulation.complexStore.put(complex3, 5);
        simulation.complexStore.put(complex4, 5);
        
        transitionInstances = getList(
                new TransitionInstance(getList(new ComplexMapping(complex1)), 1),
                new TransitionInstance(getList(new ComplexMapping(complex2)), 2),
                new TransitionInstance(getList(new ComplexMapping(complex3)), 2),
                new TransitionInstance(getList(new ComplexMapping(complex4)), 1));
        
        simulation.transitionInstanceMap.put(transition, transitionInstances);
        expectedDistribution.clear();
        checkPickTransitionInstance(transition, 12000, 200, transitionInstances, new int[] {2000, 4000, 4000, 2000});
    }

    private void checkPickTransitionInstance(Transition transition, int runCount, int error,
            List<TransitionInstance> transitionInstances, int[] expectedDistribution) {
        int[] actualDistribution = new int[expectedDistribution.length];
        
        for (int run=0; run < runCount; run++) {
            TransitionInstance result = simulation.pickTransitionInstance(transition);
            int index = transitionInstances.indexOf(result);
            assertTrue(index >= 0);
            actualDistribution[index] = actualDistribution[index] + 1;
        }
        
        for (int index=0; index < expectedDistribution.length; index++) {
            assertTrue("Expected " + Arrays.toString(expectedDistribution) + " but was " + Arrays.toString(actualDistribution),
                    Math.abs(expectedDistribution[index] - actualDistribution[index]) <= error);
        }
    }
    
    @Test
    public void testIsTransitionMappingComponentCompatible_invalidParameters() {
        Agent templateAgent = new Agent("A");
        Agent targetAgent = new Agent("A");
        Complex targetComplex = new Complex(targetAgent);
        Transition transition = new Transition(null, getList(templateAgent), null, getList(new Agent("A")), 1f);
        List<ComplexMapping> complexMapping = new ArrayList<ComplexMapping>();
        Map<Agent, Agent> mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgent, targetAgent);
        ComplexMapping componentComplexMapping = new ComplexMapping(templateAgent.getComplex(), targetComplex, mapping);
        List<Channel> channels = new ArrayList<Channel>();
        List<Compartment> compartments = new ArrayList<Compartment>();
        
        try {
            simulation.isTransitionMappingComponentCompatible(null, complexMapping, componentComplexMapping, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.isTransitionMappingComponentCompatible(transition, null, componentComplexMapping, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.isTransitionMappingComponentCompatible(transition, complexMapping, null, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.isTransitionMappingComponentCompatible(transition, complexMapping, componentComplexMapping, null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.isTransitionMappingComponentCompatible(transition, complexMapping, componentComplexMapping, channels, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
    }
    
    @Test
    public void testIsTransitionMappingComponentCompatible_nonLocatedTransition() {
        Agent templateAgentA = new Agent("A", new AgentSite("s", null, null));
        Agent templateAgentB = new Agent("B", new AgentSite("s", null, null));
        Transition transition = new Transition(null, getList(templateAgentA, templateAgentB), null, 
                getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1"))), 1f);

        Agent targetAgentA = new Agent("A", new AgentSite("s", null, null));
        Complex targetComplexA = new Complex(targetAgentA);
        Agent targetAgentB = new Agent("B", new AgentSite("s", null, null));
        Complex targetComplexB = new Complex(targetAgentB);

        Map<Agent, Agent> mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgentA, targetAgentA);
        ComplexMapping complexMappingA = new ComplexMapping(templateAgentA.getComplex(), targetComplexA, mapping);
        mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgentB, targetAgentB);
        ComplexMapping complexMappingB = new ComplexMapping(templateAgentB.getComplex(), targetComplexB, mapping);

        List<ComplexMapping> complexMappings = new ArrayList<ComplexMapping>();
        List<Channel> channels = new ArrayList<Channel>();
        List<Compartment> compartments = new ArrayList<Compartment>();
        
        // First component mapping
        assertTrue(simulation.isTransitionMappingComponentCompatible(transition, complexMappings, complexMappingA, channels, compartments));
        assertTrue(simulation.isTransitionMappingComponentCompatible(transition, complexMappings, complexMappingB, channels, compartments));
        
        // Non located targets should be compatible
        complexMappings.add(complexMappingA);
        assertTrue(simulation.isTransitionMappingComponentCompatible(transition, complexMappings, complexMappingB, channels, compartments));
        
        // Co located targets should be compatible
        complexMappings.clear();
        targetAgentA = new Agent("A", new Location("cytosol", INDEX_1), new AgentSite("s", null, null));
        targetComplexA = new Complex(targetAgentA);
        targetAgentB = new Agent("B", new Location("cytosol", INDEX_1), new AgentSite("s", null, null));
        targetComplexB = new Complex(targetAgentB);

        mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgentA, targetAgentA);
        complexMappingA = new ComplexMapping(templateAgentA.getComplex(), targetComplexA, mapping);
        mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgentB, targetAgentB);
        complexMappingB = new ComplexMapping(templateAgentB.getComplex(), targetComplexB, mapping);
        
        complexMappings.add(complexMappingA);
        assertTrue(simulation.isTransitionMappingComponentCompatible(transition, complexMappings, complexMappingB, channels, compartments));
        
        // Differently located targets should not be compatible
        complexMappings.clear();
        targetAgentA = new Agent("A", new Location("cytosol", INDEX_1), new AgentSite("s", null, null));
        targetComplexA = new Complex(targetAgentA);
        targetAgentB = new Agent("B", new Location("cytosol", INDEX_0), new AgentSite("s", null, null));
        targetComplexB = new Complex(targetAgentB);

        mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgentA, targetAgentA);
        complexMappingA = new ComplexMapping(templateAgentA.getComplex(), targetComplexA, mapping);
        mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgentB, targetAgentB);
        complexMappingB = new ComplexMapping(templateAgentB.getComplex(), targetComplexB, mapping);
        
        complexMappings.add(complexMappingA);
        assertFalse(simulation.isTransitionMappingComponentCompatible(transition, complexMappings, complexMappingB, channels, compartments));
    }
    
    @Test
    public void testIsTransitionMappingComponentCompatible_sameTargetComplex() {
        Agent templateAgentA = new Agent("A", new AgentSite("s", null, null));
        Agent templateAgentB = new Agent("A", new AgentSite("s", null, null));
        Transition transition = new Transition(null, getList(templateAgentA, templateAgentB), null, 
                getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("A", new AgentSite("s", null, "1"))), 1f);

        Agent targetAgentA = new Agent("A", new AgentSite("s", null, null));
        Complex targetComplexA = new Complex(targetAgentA);

        Map<Agent, Agent> mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgentA, targetAgentA);
        ComplexMapping complexMappingA = new ComplexMapping(templateAgentA.getComplex(), targetComplexA, mapping);
        mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgentB, targetAgentA);
        ComplexMapping complexMappingB = new ComplexMapping(templateAgentB.getComplex(), targetComplexA, mapping);

        List<ComplexMapping> complexMappings = new ArrayList<ComplexMapping>();
        List<Channel> channels = new ArrayList<Channel>();
        List<Compartment> compartments = new ArrayList<Compartment>();
        
        // First component mapping
        assertTrue(simulation.isTransitionMappingComponentCompatible(transition, complexMappings, complexMappingA, channels, compartments));
        assertTrue(simulation.isTransitionMappingComponentCompatible(transition, complexMappings, complexMappingB, channels, compartments));
        
        // Same targets should be compatible - leave it to transition activity calculation to find conflicts
        complexMappings.add(complexMappingA);
        assertTrue(simulation.isTransitionMappingComponentCompatible(transition, complexMappings, complexMappingA, channels, compartments));
    }
    
    // TODO locations in transition
}
