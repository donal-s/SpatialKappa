package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
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
import org.demonsoft.spatialkappa.model.Utils;
import org.demonsoft.spatialkappa.model.Variable;
import org.demonsoft.spatialkappa.model.VariableExpression;
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

        kappaModel.addCompartment("cytosol", new ArrayList<Integer>());
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(1);
        kappaModel.addCompartment("nucleus", dimensions);
        
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
        kappaModel.addCompartment("cytosol", dimensions);
        
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
        kappaModel.addCompartment("cytosol", dimensions);
        
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
        kappaModel.addCompartment("cytosol", new ArrayList<Integer>());
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(1);
        kappaModel.addCompartment("nucleus", dimensions);
        
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
        kappaModel.addCompartment("cytosol", dimensions);
        
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
        kappaModel.addCompartment("cytosol", dimensions);
        
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

//    @Test
//    public void testGetTransitionFiredCount() {
//        try {
//            simulation.getTransitionFiredCount(null);
//            fail("null should have failed");
//        }
//        catch (NullPointerException ex) {
//            // Expected exception
//        }
//        
//        try {
//            simulation.getTransitionFiredCount(new Variable(new VariableExpression(2), "not a transition"));
//            fail("invalid type should have failed");
//        }
//        catch (IllegalArgumentException ex) {
//            // Expected exception
//        }
//        
//        simulation.transitionsFiredMap.put(new Variable("label"), 55);
//        
//        assertEquals(new ObservationElement(55), simulation.getTransitionFiredCount(new Variable("label")));
//        assertEquals(new ObservationElement(0), simulation.getTransitionFiredCount(new Variable("other")));
//    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetNewTransitionMappings() {
        List<ComplexMapping> newComponentComplexMappings = new ArrayList<ComplexMapping>();
        Map<Complex, List<ComplexMapping>> allComponentComplexMappings = new HashMap<Complex, List<ComplexMapping>>();
        Complex complex = new Complex(new Agent("agent"));
        List<Agent> sourceAgents = Utils.getList(new Agent("agent"));
        List<Agent> targetAgents = Utils.getList(new Agent("agent"));
        Transition transition = new Transition(null, sourceAgents, null, targetAgents, 1f);
        Map<Agent, Agent> agentMap = new HashMap<Agent, Agent>();
        List<Compartment> compartments = new ArrayList<Compartment>();
        List<Channel> channels = new ArrayList<Channel>();
        
        try {
            simulation.getNewTransitionMappings(null, newComponentComplexMappings, allComponentComplexMappings, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            simulation.getNewTransitionMappings(transition, null, allComponentComplexMappings, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            simulation.getNewTransitionMappings(transition, newComponentComplexMappings, null, channels, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.getNewTransitionMappings(transition, newComponentComplexMappings, allComponentComplexMappings, null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            simulation.getNewTransitionMappings(transition, newComponentComplexMappings, allComponentComplexMappings, channels, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // New mappings empty
        
        List<List<ComplexMapping>> result = simulation.getNewTransitionMappings(
                transition, newComponentComplexMappings, allComponentComplexMappings, channels, compartments);
        List<List<ComplexMapping>> expected = new ArrayList<List<ComplexMapping>>();
        assertEquals(expected, result);
        
        // Single component match
        
        transition = new Transition(null, sourceAgents, null, targetAgents, 1f);
        Complex component = getComplexByString("[agent]", transition.sourceComplexes);
        
        newComponentComplexMappings = Utils.getList(new ComplexMapping(component, complex, agentMap));
        result = simulation.getNewTransitionMappings(transition, newComponentComplexMappings, allComponentComplexMappings, channels, compartments);
        expected = Utils.getList(newComponentComplexMappings);
        assertEquals(expected, result);
        
        // Multi component match - no spatial constraints
        sourceAgents = Utils.getList(new Agent("agent"), new Agent("agent2"));
        targetAgents = Utils.getList(new Agent("agent"), new Agent("agent2"));
        transition = new Transition(null, sourceAgents, null, targetAgents, 1f);
        
        component = getComplexByString("[agent]", transition.sourceComplexes);
        newComponentComplexMappings = Utils.getList(new ComplexMapping(component, complex, agentMap));
        result = simulation.getNewTransitionMappings(transition, newComponentComplexMappings, allComponentComplexMappings, channels, compartments);
        expected = new ArrayList<List<ComplexMapping>>();
        assertEquals(expected, result);
        
        allComponentComplexMappings.put(component, newComponentComplexMappings);
        Complex complex2 = new Complex(new Agent("agent2"));
        Complex component2 = getComplexByString("[agent2]", transition.sourceComplexes);
        newComponentComplexMappings = Utils.getList(new ComplexMapping(component2, complex2, agentMap));
        result = simulation.getNewTransitionMappings(transition, newComponentComplexMappings, allComponentComplexMappings, channels, compartments);
        expected = Utils.getList(Utils.getList(new ComplexMapping(component2, complex2, agentMap), new ComplexMapping(component, complex, agentMap)));
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

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveTransitionMappings() {
        List<List<ComplexMapping>> transitionMappings = new ArrayList<List<ComplexMapping>>();
        Complex complex = new Complex(new Agent("agent"));
        
        try {
            simulation.removeTransitionMappings(null, complex);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            simulation.removeTransitionMappings(transitionMappings, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Complex component = new Complex(new Agent("agent"));
        Map<Agent, Agent> agentMap = new HashMap<Agent, Agent>();
        List<ComplexMapping> notTargetMapping1 = Utils.getList(
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap));
        List<ComplexMapping> notTargetMapping2 = Utils.getList(
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap),
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap));
        List<ComplexMapping> targetMapping1 = Utils.getList(
                new ComplexMapping(component, complex, agentMap));
        List<ComplexMapping> targetMapping2 = Utils.getList(
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap),
                new ComplexMapping(component, complex, agentMap),
                new ComplexMapping(component, new Complex(new Agent("other")), agentMap));
        
        transitionMappings = Utils.getList(notTargetMapping1, targetMapping1, notTargetMapping2, targetMapping2);
        List<List<ComplexMapping>> expectedMappings = Utils.getList(notTargetMapping1, notTargetMapping2);
        
        simulation.removeTransitionMappings(transitionMappings, complex);
        assertEquals(expectedMappings, transitionMappings);
        
        transitionMappings.clear();
        expectedMappings.clear();
        
        simulation.removeTransitionMappings(transitionMappings, complex);
        assertEquals(expectedMappings, transitionMappings);
    }
    
    @SuppressWarnings("unchecked")
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
        simulation.transitionComplexMappingMap.put(transition, 
                getList(getList(new ComplexMapping(complex1))));
        assertEquals(3, simulation.getTotalTransitionActivity(transition));
        
        // Single list of multiple mappings
        simulation.transitionComplexMappingMap.put(transition, 
                getList(getList(new ComplexMapping(complex1),new ComplexMapping(complex2))));
        assertEquals(21, simulation.getTotalTransitionActivity(transition));
        
        // Multiple lists of single mappings
        simulation.transitionComplexMappingMap.put(transition, 
                getList(getList(new ComplexMapping(complex1)), 
                        getList(new ComplexMapping(complex2))));
        assertEquals(10, simulation.getTotalTransitionActivity(transition));
        
        // Multiple lists of multiple mappings
        simulation.transitionComplexMappingMap.put(transition, 
                getList(getList(new ComplexMapping(complex1), new ComplexMapping(complex2)),
                        getList(new ComplexMapping(complex3), new ComplexMapping(complex4))));
        assertEquals(43, simulation.getTotalTransitionActivity(transition));
        
        
        transition = new Transition("createComplex", null, null, getList(new Agent("newAgent")), 1f);
        assertEquals(1, simulation.getTotalTransitionActivity(transition));
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testPickComplexMappings() {
        try {
            simulation.pickComplexMappings(null);
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
        
        List<List<ComplexMapping>> mappingsList = getList(getList(new ComplexMapping(complex1)));
        simulation.transitionComplexMappingMap.put(transition, mappingsList);
        Map<List<ComplexMapping>, Integer> expectedDistribution = new HashMap<List<ComplexMapping>, Integer>();
        checkPickComplexMappings(transition, 100, 0, mappingsList, new int[] {100});
        
        mappingsList = getList(
                getList(new ComplexMapping(complex3)),
                getList(new ComplexMapping(complex1), new ComplexMapping(complex4)));
        simulation.transitionComplexMappingMap.put(transition, mappingsList);
        checkPickComplexMappings(transition, 10000, 150, mappingsList, new int[] {6670, 3330});
        
        // Check even random distribution
        simulation.complexStore.put(complex1, 5);
        simulation.complexStore.put(complex2, 5);
        simulation.complexStore.put(complex3, 5);
        simulation.complexStore.put(complex4, 5);
        
        mappingsList = getList(getList(new ComplexMapping(complex1)),
                getList(new ComplexMapping(complex2)),
                getList(new ComplexMapping(complex3)),
                getList(new ComplexMapping(complex4)));
        
        simulation.transitionComplexMappingMap.put(transition, mappingsList);
        expectedDistribution.clear();
        checkPickComplexMappings(transition, 10000, 150, mappingsList, new int[] {2500, 2500, 2500, 2500});
    }

    private void checkPickComplexMappings(Transition transition, int runCount, int error,
            List<List<ComplexMapping>> mappingsList, int[] expectedDistribution) {
        int[] actualDistribution = new int[expectedDistribution.length];
        
        for (int run=0; run < runCount; run++) {
            List<ComplexMapping> result = simulation.pickComplexMappings(transition);
            int index = mappingsList.indexOf(result);
            assertTrue(index >= 0);
            actualDistribution[index] = actualDistribution[index] + 1;
        }
        
        for (int index=0; index < expectedDistribution.length; index++) {
            assertTrue("Expected " + Arrays.toString(expectedDistribution) + " but was " + Arrays.toString(actualDistribution),
                    Math.abs(expectedDistribution[index] - actualDistribution[index]) <= error);
        }
    }
}
