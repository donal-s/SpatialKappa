package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentDeclaration;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.AggregateSite;
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
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableReference;
import org.junit.Before;
import org.junit.Test;

public class TransitionMatchingSimulationTest {

    private static final Map<String, Variable> NO_VARIABLES = new HashMap<String, Variable>();
    
    public static CellIndexExpression INDEX_10 = new CellIndexExpression("10");
    public static CellIndexExpression INDEX_20 = new CellIndexExpression("20");

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
        
        List<Agent> agents = getList(new Agent("agent2"));
        kappaModel.addTransition("label", NOT_LOCATED, agents, null, NOT_LOCATED, new ArrayList<Agent>(), new VariableExpression(0.1f));
        kappaModel.addPlot("label");
        
        agents = getList(new Agent("agent1"));
        kappaModel.addVariable(agents, "label2", NOT_LOCATED, false);
        kappaModel.addPlot("label2");
        
        agents = getList(new Agent("agent2"));
        kappaModel.addVariable(agents, "label3", NOT_LOCATED, false);
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        observation = simulation.getCurrentObservation();
        checkObservation(observation, "label", "label2");
    }

    @Test
    public void testGetCurrentObservation_singleCellCompartment() {
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent1"));
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent2"));

        kappaModel.addCompartment("cytosol", null, new ArrayList<Integer>());
        kappaModel.addCompartment("nucleus", null, getList(1));
        
        List<Agent> agents = getList(new Agent("agent1"));
        kappaModel.addVariable(agents, "observable1", new Location("cytosol"), false);
        kappaModel.addPlot("observable1");
        kappaModel.addInitialValue(agents, "5", new Location("cytosol"));
        
        agents.clear();
        agents.add(new Agent("agent2"));
        kappaModel.addVariable(agents, "observable2", new Location("nucleus"), true);
        kappaModel.addPlot("observable2");
        kappaModel.addInitialValue(agents, "7", new Location("nucleus"));
        
        kappaModel.addVariable(agents, "observable3", new Location("nucleus"), false);
        kappaModel.addPlot("observable3");
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkObservation("observable1", new ObservationElement(5));
        checkObservation("observable2", new ObservationElement(7, new int[] {1}, "cytosol", new Serializable[] {7}));
        checkObservation("observable3", new ObservationElement(7));
    }
    
    @Test
    public void testGetCurrentObservation_1DCompartment() {
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent1"));
        kappaModel.addCompartment("cytosol", null, getList(4));
        
        kappaModel.addVariable(getList(new Agent("agent1")), "observable1", new Location("cytosol"), true);
        kappaModel.addPlot("observable1");
        kappaModel.addVariable(getList(new Agent("agent1")), "observable3", new Location("cytosol"), false);
        kappaModel.addPlot("observable3");
        kappaModel.addVariable(getList(new Agent("agent1")), "observable2", new Location("cytosol", 0), false);
        kappaModel.addPlot("observable2");
        kappaModel.addInitialValue(getList(new Agent("agent1")), "5", new Location("cytosol", 0));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "7", new Location("cytosol", 3));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkObservation("observable2", new ObservationElement(5));
        checkObservation("observable1", new ObservationElement(12, new int[] {4}, "cytosol", new Serializable[] {5, 0, 0, 7}));
        checkObservation("observable3", new ObservationElement(12));
    }

    @Test
    public void testGetCurrentObservation_voxelObservationsSplitAcrossVoxels() {
        kappaModel.addAgentDeclaration(new AgentDeclaration("A", 
                new AggregateSite("x", (String) null, null), new AggregateSite("y", (String) null, null)));
        kappaModel.addAgentDeclaration(new AgentDeclaration("B", 
                new AggregateSite("x", (String) null, null), new AggregateSite("y", (String) null, null)));
        kappaModel.addCompartment("loc1", null, getList(4));
        kappaModel.addCompartment(new Compartment("loc2"));

        Channel intra = new Channel("intra", new Location("loc1", INDEX_X), new Location("loc1", INDEX_X_PLUS_1));
        kappaModel.addChannel(intra);
        
        Channel inter = new Channel("inter", new Location("loc1", INDEX_X), new Location("loc2"));
        kappaModel.addChannel(inter);
        
        kappaModel.addInitialValue(getList(
                new Agent("A", new Location("loc1", 0))),
                "5", NOT_LOCATED);
        kappaModel.addInitialValue(getList(
                new Agent("A", new Location("loc1", 1), new AgentSite("x", null, "1")),
                new Agent("A", new Location("loc1", 1), new AgentSite("x", null, "1"))), 
                "3", NOT_LOCATED);
        kappaModel.addInitialValue(getList(
                new Agent("A", new Location("loc1", 1), new AgentSite("x", null, "1")),
                new Agent("B", new Location("loc1", 1), new AgentSite("x", null, "1"))), 
                "7", NOT_LOCATED);
        kappaModel.addInitialValue(getList(
                new Agent("A", new Location("loc1", 2), new AgentSite("x", null, "1", "intra")),
                new Agent("A", new Location("loc1", 3), new AgentSite("x", null, "1"))), 
                "11", NOT_LOCATED);
        kappaModel.addInitialValue(getList(
                new Agent("A", new Location("loc1", 2), new AgentSite("x", null, "1", "intra")),
                new Agent("B", new Location("loc1", 3), new AgentSite("x", null, "1"))), 
                "13", NOT_LOCATED);
        kappaModel.addInitialValue(getList(
                new Agent("A", new Location("loc1", 2), new AgentSite("x", null, "1", "inter")),
                new Agent("A", new Location("loc2"), new AgentSite("x", null, "1"))), 
                "19", NOT_LOCATED);
//        kappaModel.addInitialValue(getList(
//                new Agent("B", new Location("loc1", INDEX_3), new AgentSite("x", null, "1", "inter")),
//                new Agent("A", new Location("loc2"), new AgentSite("x", null, "1"))), 
//                "23", NOT_LOCATED);
        // TODO make link channels bidirectional
        
        
        kappaModel.addVariable(getList(new Agent("A")), "A", new Location("loc1"), true);
        kappaModel.addVariable(getList(new Agent("B")), "B", new Location("loc1"), true);
        kappaModel.addVariable(getList(
                new Agent("A", new AgentSite("x", null, "1")),
                new Agent("A", new AgentSite("x", null, "1"))), 
                "AA", new Location("loc1"), true);
        kappaModel.addVariable(getList(
                new Agent("A", new AgentSite("x", null, "1")),
                new Agent("B", new AgentSite("x", null, "1"))), 
                "AB", new Location("loc1"), true);
        
        kappaModel.addPlot("A");
        kappaModel.addPlot("B");
        kappaModel.addPlot("AA");
        kappaModel.addPlot("AB");

        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkObservation("A",  new ObservationElement(5 + 3*2 + 7 + 11 * 2 + 13 + 19, new int[] {4}, "loc1", 
                new Serializable[] {5, 3*2 + 7, 11 + 13 + 19, 11}));
        // TODO make link channels bidirectional
//        checkObservation("B",  new ObservationElement(7 + 13 + 23, new int[] {4}, "loc1", 
//                new Serializable[] {0, 7, 0, 13 + 23}));
        checkObservation("B",  new ObservationElement(7 + 13, new int[] {4}, "loc1", 
                new Serializable[] {0, 7, 0, 13}));
        checkObservation("AA", new ObservationElement(3 * 2 + 11 * 2, new int[] {4}, "loc1", 
                new Serializable[] {0, 3 * 2, 0, 0}));
        checkObservation("AB", new ObservationElement(7 + 13, new int[] {4}, "loc1", 
                new Serializable[] {0, 7, 0, 0}));
    }

    @Test
    public void testSetTransitionRateOrVariable() {
        kappaModel.addAgentDeclaration(new AgentDeclaration("A"));
        kappaModel.addAgentDeclaration(new AgentDeclaration("B"));
        kappaModel.addTransition("rule1", null, getList(new Agent("A")), null, null, getList(new Agent("B")), 
                new VariableExpression(1));
        
        // To test dependent rate updates
        kappaModel.addTransition("rule2", null, getList(new Agent("A")), null, null, getList(new Agent("B")), 
                new VariableExpression(new VariableExpression(2), Operator.MULTIPLY, 
                        new VariableExpression(new VariableReference("variable1"))));
        kappaModel.addVariable(new VariableExpression(2), "variable1");

        Transition rule1 = kappaModel.getTransitions().get(0);
        Transition rule2 = kappaModel.getTransitions().get(1);
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        assertEquals(1f, rule1.getRate().evaluate(NO_VARIABLES), 0.01f);
        assertTrue(rule2.hasSimpleRate);
        assertEquals(4f, rule2.simpleRate, 0.01f);
        assertEquals(4f, rule2.getRate().evaluate(kappaModel.getVariables()), 0.01f);
        assertEquals(2f, kappaModel.getVariables().get("variable1").evaluate(NO_VARIABLES), 0.01f);
        
        try {
            simulation.setTransitionRateOrVariable(null, new VariableExpression(3));
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            simulation.setTransitionRateOrVariable("variable1", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            simulation.setTransitionRateOrVariable("unknown", new VariableExpression(3));
            fail("unknown label should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        simulation.setTransitionRateOrVariable("variable1", new VariableExpression(3));
        
        assertEquals(1f, rule1.getRate().evaluate(NO_VARIABLES), 0.01f);
        assertTrue(rule2.hasSimpleRate);
        assertEquals(6f, rule2.simpleRate, 0.01f);
        assertEquals(6f, rule2.getRate().evaluate(kappaModel.getVariables()), 0.01f);
        assertEquals(3f, kappaModel.getVariables().get("variable1").evaluate(NO_VARIABLES), 0.01f);
        
        simulation.setTransitionRateOrVariable("rule1", new VariableExpression(5));
        
        assertEquals(5f, rule1.getRate().evaluate(NO_VARIABLES), 0.01f);
        assertTrue(rule2.hasSimpleRate);
        assertEquals(6f, rule2.simpleRate, 0.01f);
        assertEquals(6f, rule2.getRate().evaluate(kappaModel.getVariables()), 0.01f);
        assertEquals(3f, kappaModel.getVariables().get("variable1").evaluate(NO_VARIABLES), 0.01f);
    }

    @Test
    public void testGetCurrentObservation_2DCompartment() {
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent1"));
        List<Integer> dimensions = getList(3, 2);
        kappaModel.addCompartment("cytosol", null, dimensions);
        
        kappaModel.addVariable(getList(new Agent("agent1")), "observable1", new Location("cytosol"), true);
        kappaModel.addPlot("observable1");
        kappaModel.addVariable(getList(new Agent("agent1")), "observable3", new Location("cytosol"), false);
        kappaModel.addPlot("observable3");
        kappaModel.addVariable(getList(new Agent("agent1")), "observable2", new Location("cytosol", 0, 0), false);
        kappaModel.addPlot("observable2");
        kappaModel.addInitialValue(getList(new Agent("agent1")), "5", new Location("cytosol", 0, 0));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "7", new Location("cytosol", 2, 1));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkObservation("observable1", new ObservationElement(12, new int[] {3, 2}, "cytosol", new Serializable[][] {{5, 0}, {0, 0}, {0, 7}}));
        checkObservation("observable3", new ObservationElement(12));
        checkObservation("observable2", new ObservationElement(5));
    }

    // TODO add grid observations for shapes
    
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
        kappaModel.addVariable(Arrays.asList(agents), label, NOT_LOCATED, false);
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
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent1"));
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent2"));
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent3"));
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent4"));
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent5"));
        
        try {
            simulation.getComplexQuantity(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Agent> agents = getList(
                new Agent("agent1", new AgentSite("x", null, null)), 
                new Agent("agent4", new AgentSite("y", "s", null)));
        kappaModel.addInitialValue(agents, "3", NOT_LOCATED);

        agents = getList(
                new Agent("agent1", new AgentSite("x", null, "1")),
                new Agent("agent2", new AgentSite("x", null, "1")),
                new Agent("agent3", new AgentSite("x", null, "2")),
                new Agent("agent4", new AgentSite("x", null, "2"), new AgentSite("y", null, null)));
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
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent1"));
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent2"));
        kappaModel.addCompartment("cytosol", null, new ArrayList<Integer>());
        List<Integer> dimensions = getList(1);
        kappaModel.addCompartment("nucleus", null, dimensions);
        
        List<Agent> agents = getList(new Agent("agent1"));
        kappaModel.addVariable(agents, "observable1", new Location("cytosol"), false);
        kappaModel.addInitialValue(agents, "5", new Location("cytosol"));
        
        agents.clear();
        agents.add(new Agent("agent2"));
        kappaModel.addVariable(agents, "observable2", new Location("nucleus"), true);
        kappaModel.addVariable(agents, "observable3", new Location("nucleus"), false);
        kappaModel.addInitialValue(agents, "7", new Location("nucleus"));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(5));
        checkGetQuantity(simulation.getVariable("observable2"), new ObservationElement(7, new int[] {1}, "cytosol", new Serializable[] {7}));
        checkGetQuantity(simulation.getVariable("observable3"), new ObservationElement(7));
    }

    @Test
    public void testGetQuantity_1DCompartment() {
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent1"));
        List<Integer> dimensions = getList(4);
        kappaModel.addCompartment("cytosol", null, dimensions);
        
        kappaModel.addVariable(getList(new Agent("agent1")), "observable1", new Location("cytosol"), true);
        kappaModel.addVariable(getList(new Agent("agent1")), "observable3", new Location("cytosol"), false);
        kappaModel.addVariable(getList(new Agent("agent1")), "observable2", new Location("cytosol", 0), false);
        kappaModel.addInitialValue(getList(new Agent("agent1")), "5", new Location("cytosol", 0));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "7", new Location("cytosol", 3));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(12, new int[] {4}, "cytosol", new Serializable[] {5, 0, 0, 7}));
        checkGetQuantity(simulation.getVariable("observable3"), new ObservationElement(12));
        checkGetQuantity(simulation.getVariable("observable2"), new ObservationElement(5));
    }

    @Test
    public void testGetQuantity_2DCompartment() {
        kappaModel.addAgentDeclaration(new AgentDeclaration("agent1"));
        List<Integer> dimensions = getList(3, 2);
        kappaModel.addCompartment("cytosol", null, dimensions);
        
        kappaModel.addVariable(getList(new Agent("agent1")), "observable1", new Location("cytosol"), true);
        kappaModel.addVariable(getList(new Agent("agent1")), "observable3", new Location("cytosol"), false);
        kappaModel.addVariable(getList(new Agent("agent1")), "observable2", new Location("cytosol", 0, 0), false);
        kappaModel.addInitialValue(getList(new Agent("agent1")), "5", new Location("cytosol", 0, 0));
        kappaModel.addInitialValue(getList(new Agent("agent1")), "7", new Location("cytosol", 2, 1));
        
        simulation = new TransitionMatchingSimulation(kappaModel);
        
        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(12, new int[] {3, 2}, "cytosol", new Serializable[][] {{5, 0}, {0, 0}, {0, 7}}));
        checkGetQuantity(simulation.getVariable("observable3"), new ObservationElement(12));
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
        Complex component = getComplexByString("[agent()]", transition.sourceComplexes);
        
        newComponentComplexMappings = getList(new ComplexMapping(component, complex, agentMap));
        result = simulation.getNewTransitionInstances(transition, newComponentComplexMappings, allComponentComplexMappings, 
                complexCounts, channels, compartments);
        expected = getList(new TransitionInstance(newComponentComplexMappings, 1));
        assertEquals(expected, result);
        
        // Multi component match - no spatial constraints
        sourceAgents = getList(new Agent("agent"), new Agent("agent2"));
        targetAgents = getList(new Agent("agent"), new Agent("agent2"));
        transition = new Transition(null, sourceAgents, null, targetAgents, 1f);
        
        component = getComplexByString("[agent()]", transition.sourceComplexes);
        newComponentComplexMappings = getList(new ComplexMapping(component, complex, agentMap));
        result = simulation.getNewTransitionInstances(transition, newComponentComplexMappings, allComponentComplexMappings, 
                complexCounts, channels, compartments);
        expected = new ArrayList<TransitionInstance>();
        assertEquals(expected, result);
        
        allComponentComplexMappings.put(component, newComponentComplexMappings);
        Complex complex2 = new Complex(new Agent("agent2"));
        complexCounts.put(complex2, 1);
        Complex component2 = getComplexByString("[agent2()]", transition.sourceComplexes);
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
        
        kappaModel.addVariable(getList(new Agent("A")), "A", NOT_LOCATED, false);
        kappaModel.addVariable(getList(new Agent("B")), "B", NOT_LOCATED, false);
        kappaModel.addVariable(getList(new Agent("C")), "C", NOT_LOCATED, false);
        
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
    public void testIsTransitionActive() {
        try {
            simulation.isTransitionActive(null);
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
        
        List<TransitionInstance> instances = getList(new TransitionInstance(getList(new ComplexMapping(complex1)), 1));
        checkIsTransitionActive(transition, instances, true);
        
        // Single list of multiple mappings
        checkIsTransitionActive(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1), new ComplexMapping(complex2)), 1)), true);
        
        // Multiple lists of single mappings
        checkIsTransitionActive(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1)), 1),
                        new TransitionInstance(getList(new ComplexMapping(complex2)), 1)), true);
        
        // Multiple lists of multiple mappings
        checkIsTransitionActive(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1), new ComplexMapping(complex2)), 1),
                        new TransitionInstance(getList(new ComplexMapping(complex3), new ComplexMapping(complex4)), 1)), true);
        
        // Multiple target locations per mapping
        checkIsTransitionActive(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1)), 2)), true);
        
        transition = new Transition("createComplex", null, null, getList(new Agent("newAgent")), 1f);
        assertTrue(simulation.isTransitionActive(transition));
        
        transition = new Transition("moveAnything", (Location) null, "channel", null, 1f);
        simulation.complexStore.put(complex1, 0);
        
        // Single list of single mapping
        checkIsTransitionActive(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1)), 1)), false);
        
        // Single list of multiple mappings
        checkIsTransitionActive(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1), new ComplexMapping(complex2)), 1)), false);
        
        // Multiple lists of single mappings
        checkIsTransitionActive(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1)), 1),
                        new TransitionInstance(getList(new ComplexMapping(complex2)), 1)), true);
        
        // Multiple lists of multiple mappings
        checkIsTransitionActive(transition, 
                getList(new TransitionInstance(getList(new ComplexMapping(complex1), new ComplexMapping(complex2)), 1),
                        new TransitionInstance(getList(new ComplexMapping(complex3), new ComplexMapping(complex4)), 1)), true);
        
        // Multiple target locations per mapping
        checkIsTransitionActive(transition, getList(new TransitionInstance(getList(new ComplexMapping(complex1)), 2)), false);
        
        transition = new Transition("createComplex", null, null, getList(new Agent("newAgent")), 1f);
        assertTrue(simulation.isTransitionActive(transition));
    }
    
    
    
    private void checkIsTransitionActive(Transition transition, List<TransitionInstance> transitionInstances, boolean expectedActive) {
        updateTransitionInstanceActivities(transitionInstances);
        simulation.transitionInstanceMap.put(transition, transitionInstances);
        assertEquals(expectedActive, simulation.isTransitionActive(transition));
    }

    private void updateTransitionInstanceActivities(List<TransitionInstance> instances) {
        for (TransitionInstance instance : instances) {
            simulation.updateTransitionInstanceActivity(instance);
        }
    }

    @Test
    public void testGetTransitionInstanceActivity() {
        try {
            simulation.getTransitionInstanceActivity(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Complex complex1 = new Complex(new Agent("agent1"));
        Complex complex2 = new Complex(new Agent("agent2"));
        Complex complex3 = new Complex(new Agent("agent3"));
        Complex complex4 = new Complex(new Agent("agent4"));
        simulation.complexStore.put(complex1, 3);
        simulation.complexStore.put(complex2, 7);
        simulation.complexStore.put(complex3, 11);
        simulation.complexStore.put(complex4, 2);
        
        
        // Check if updateTransitionInstanceActivity not called
        TransitionInstance instance = new TransitionInstance(getList(new ComplexMapping(complex1)), 1);
        try {
            simulation.getTransitionInstanceActivity(instance);
            fail("uninitialised should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        // Single list of single mapping
        simulation.updateTransitionInstanceActivity(instance);
        assertEquals(3, simulation.getTransitionInstanceActivity(instance));
        
        // Single list of multiple mappings
        instance = new TransitionInstance(getList(new ComplexMapping(complex1), new ComplexMapping(complex2)), 1);
        simulation.updateTransitionInstanceActivity(instance);
        assertEquals(21, simulation.getTransitionInstanceActivity(instance));
        
        // Multiple target locations per mapping
        instance = new TransitionInstance(getList(new ComplexMapping(complex1)), 2);
        simulation.updateTransitionInstanceActivity(instance);
        assertEquals(6, simulation.getTransitionInstanceActivity(instance));
    }
    
    @Test
    public void testGetTransitionInstanceRate() {
        Transition transition = new Transition("moveAnything", (Location) null, "channel", null, 1f);
        Complex complex1 = new Complex(new Agent("agent1"));
        TransitionInstance instance = new TransitionInstance(getList(new ComplexMapping(complex1)), 1);
        
        try {
            simulation.getTransitionInstanceRate(instance, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            simulation.getTransitionInstanceRate(null, transition);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        transition = new Transition("moveAnything", (Location) null, "channel", null, 1f);
        instance = new TransitionInstance(getList(new ComplexMapping(complex1)), 1);
        assertEquals(1, simulation.getTransitionInstanceRate(instance, transition), 0.1);

        // Not dependent on instance agents
        VariableExpression rateExpression = new VariableExpression(4);
        List<Complex> instanceComplexes = new ArrayList<Complex>();
        checkGetTransitionInstanceRate(rateExpression, instanceComplexes, 4);
        
        // Single agent type 
        rateExpression = new VariableExpression(new VariableExpression(4), Operator.DIVIDE, 
                new VariableExpression(getList(new Agent("A")), NOT_LOCATED));
        instanceComplexes.clear();
        checkGetTransitionInstanceRate(rateExpression, instanceComplexes, Float.POSITIVE_INFINITY);
        
        rateExpression = new VariableExpression(new VariableExpression(4), Operator.DIVIDE, 
                new VariableExpression(getList(new Agent("A")), NOT_LOCATED));
        instanceComplexes.add(new Complex(new Agent("A")));
        checkGetTransitionInstanceRate(rateExpression, instanceComplexes, 4);
        
        rateExpression = new VariableExpression(new VariableExpression(4), Operator.DIVIDE, 
                new VariableExpression(getList(new Agent("A")), NOT_LOCATED));
        instanceComplexes = getList(new Complex(new Agent("A")), new Complex(new Agent("A")));
        checkGetTransitionInstanceRate(rateExpression, instanceComplexes, 2);
        
        rateExpression = new VariableExpression(new VariableExpression(4), Operator.DIVIDE, 
                new VariableExpression(getList(new Agent("A")), NOT_LOCATED));
        instanceComplexes = getList(new Complex(
                new Agent("A", new AgentSite("x", null, "1"), new AgentSite("y", null, "2")), 
                new Agent("A", new AgentSite("x", null, "1")), 
                new Agent("A", new AgentSite("x", null, "2"))));
        checkGetTransitionInstanceRate(rateExpression, instanceComplexes, 1.33f);
        
        // TODO multiple agent types and agent state
    }

    private void checkGetTransitionInstanceRate(VariableExpression rateExpression, List<Complex> instanceComplexes, float expectedRate) {
        Transition transition = new Transition("moveAnything", (Location) null, "channel", null, rateExpression);
        List<ComplexMapping> complexMappings = new ArrayList<ComplexMapping>();
        for (Complex complex : instanceComplexes) {
            complexMappings.add(new ComplexMapping(complex));
        }
        TransitionInstance instance = new TransitionInstance(complexMappings, 1);
        assertEquals(expectedRate, simulation.getTransitionInstanceRate(instance, transition), 0.1);
    }
    
    @Test
    public void testPickTransitionInstance() {
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
        checkPickTransitionInstance(transition, 12000, 200, transitionInstances, new int[] {2000, 4000, 4000, 2000});
    }
    
    @Test
    public void testPickTransitionInstance_infiniteRates() {
        Transition transition = new Transition("moveAnything", (Location) null, "channel", null, 
                new VariableExpression(VariableExpression.Constant.INFINITY));
        Complex complex1 = new Complex(new Agent("agent1"));
        Complex complex2 = new Complex(new Agent("agent2"));
        Complex complex3 = new Complex(new Agent("agent3"));
        Complex complex4 = new Complex(new Agent("agent4"));
        
        // Check even random distribution - with multiple locations per transition
        simulation.complexStore.put(complex1, 5);
        simulation.complexStore.put(complex2, 5);
        simulation.complexStore.put(complex3, 5);
        simulation.complexStore.put(complex4, 5);
        
        List<TransitionInstance> transitionInstances = getList(
                new TransitionInstance(getList(new ComplexMapping(complex1)), 1),
                new TransitionInstance(getList(new ComplexMapping(complex2)), 2),
                new TransitionInstance(getList(new ComplexMapping(complex3)), 2),
                new TransitionInstance(getList(new ComplexMapping(complex4)), 1));
        
        simulation.transitionInstanceMap.put(transition, transitionInstances);
        checkPickTransitionInstance(transition, 12000, 200, transitionInstances, new int[] {2000, 4000, 4000, 2000});
    }

    @Test
    public void testPickTransitionInstance_agentBasedRate() {

        Complex complex1 = new Complex(new Agent("agent1"));
        Complex complex2 = new Complex(new Agent("agent1", new AgentSite("x", null, "1")), new Agent("agent1", new AgentSite("x", null, "1")));
        simulation.complexStore.put(complex1, 6);
        simulation.complexStore.put(complex2, 3);
        
        Transition transition = new Transition("moveAnything", (Location) null, "channel", null, 
                new VariableExpression(getList(new Agent("agent1")), NOT_LOCATED));
        List<TransitionInstance> transitionInstances = getList(
                new TransitionInstance(getList(new ComplexMapping(complex1)), 1));
        simulation.transitionInstanceMap.put(transition, transitionInstances);
        checkPickTransitionInstance(transition, 100, 0, transitionInstances, new int[] {100});
        
        transition = new Transition("moveAnything", (Location) null, "channel", null, new VariableExpression(1f));
        transitionInstances = getList(
                new TransitionInstance(getList(new ComplexMapping(complex1)), 1),
                new TransitionInstance(getList(new ComplexMapping(complex2)), 1));
        simulation.transitionInstanceMap.put(transition, transitionInstances);
        checkPickTransitionInstance(transition, 10000, 150, transitionInstances, new int[] {6670, 3330});
        
        transition = new Transition("moveAnything", (Location) null, "channel", null, 
                new VariableExpression(getList(new Agent("agent1")), NOT_LOCATED));
        simulation.transitionInstanceMap.put(transition, transitionInstances);
        checkPickTransitionInstance(transition, 10000, 150, transitionInstances, new int[] {5000, 5000});
    }

    private void checkPickTransitionInstance(Transition transition, int runCount, int error,
            List<TransitionInstance> transitionInstances, int[] expectedDistribution) {
        
        updateTransitionInstanceActivities(transitionInstances);
        simulation.updateTransitionActivity(transition, false);
        
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
        targetAgentA = new Agent("A", new Location("cytosol", 1), new AgentSite("s", null, null));
        targetComplexA = new Complex(targetAgentA);
        targetAgentB = new Agent("B", new Location("cytosol", 1), new AgentSite("s", null, null));
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
        targetAgentA = new Agent("A", new Location("cytosol", 1), new AgentSite("s", null, null));
        targetComplexA = new Complex(targetAgentA);
        targetAgentB = new Agent("B", new Location("cytosol", 0), new AgentSite("s", null, null));
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
    
    @Test
    public void testSnapshot() throws IOException {
        File expectedFile = new File("snap_444.ka");
        assertFalse(expectedFile.exists());
        
        kappaModel.addCompartment(new Compartment("location", 5, 4));
        kappaModel.addAgentDeclaration(new AgentDeclaration("A"));
        kappaModel.addInitialValue(getList(new Agent("A")), "7", new Location("location", 0, 1));
        simulation = new TransitionMatchingSimulation(kappaModel);
        simulation.eventCount = 444;
        
        simulation.snapshot();
        assertTrue("Snapshot not created", expectedFile.exists());
        try {
            String output = FileUtils.readFileToString(expectedFile);
            assertEquals("%init: 7 A:location[0][1]()\n", output);

            kappaModel.addAgentDeclaration(new AgentDeclaration("B"));
            kappaModel.addInitialValue(getList(new Agent("B")), "9", new Location("location", 1, 0));
            simulation = new TransitionMatchingSimulation(kappaModel);
            simulation.eventCount = 444;
            
            simulation.snapshot();
            File expectedFile2 = new File("snap_444_2.ka");
            assertTrue("Snapshot not created", expectedFile2.exists());
            try {
                output = FileUtils.readFileToString(expectedFile2);
                assertEquals("%init: 7 A:location[0][1]()\n%init: 9 B:location[1][0]()\n", output);
            }
            finally {
                expectedFile2.delete();
            }
        }
        finally {
            expectedFile.delete();
        }
    }
    
    @Test
    public void testRaceConditionFailure() {
        
        // This model setup caused an intermittent NullPointerException
        // Maintained here as a regression test
        
        for (int index=0; index < 10; index++) {
            kappaModel = new KappaModel();
            kappaModel.addCompartment(new Compartment.SolidSphere("cytosol", 2));
            kappaModel.addCompartment(new Compartment.OpenSphere("membrane", 4, 1));
            kappaModel.addAgentDeclaration(new AgentDeclaration("A1", new AggregateSite("d", (String) null, null)));
            kappaModel.addInitialValue(getList(
                    new Agent("A1", new Location("membrane", 0, 1, 1), new AgentSite("d", null, "1")),
                    new Agent("A1", new Location("cytosol"), new AgentSite("d", null, "1", "domainLink"))), 
                    "1", NOT_LOCATED);
            
            Channel channel = new Channel("diffusion");
            channel.addChannelComponent("Neighbour", getList(new Location("membrane")),
                    getList(new Location("membrane")));
            kappaModel.addChannel(channel);
            
            channel = new Channel("domainLink");
            channel.addChannelComponent("Neighbour", getList(new Location("cytosol")), getList(new Location("membrane")));
            channel.addChannelComponent("Neighbour", getList(new Location("membrane")), getList(new Location("cytosol")));
            kappaModel.addChannel(channel);
            
            kappaModel.addTransition(null, NOT_LOCATED, getList(
                    new Agent("A1", new Location("membrane"), new AgentSite("d", null, "1")),
                    new Agent("A1", new AgentSite("d", null, "1", "domainLink"))),
                    "diffusion", NOT_LOCATED, getList(
                            new Agent("A1", new Location("membrane"), new AgentSite("d", null, "1")),
                            new Agent("A1", new AgentSite("d", null, "1", "domainLink"))),
                            new VariableExpression(1f));
            
            simulation = new TransitionMatchingSimulation(kappaModel);
        }
    }
    
    
    // TODO locations in transition
}
