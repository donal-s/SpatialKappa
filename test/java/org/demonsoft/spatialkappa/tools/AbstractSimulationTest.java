package org.demonsoft.spatialkappa.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.Variable;
import org.demonsoft.spatialkappa.model.VariableExpression;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractSimulationTest {

    private KappaModel kappaModel = new KappaModel();
    private AbstractSimulation simulation;

    @Before
    public void setUp() {
        simulation = createSimulation(kappaModel);
    }
    
    protected abstract AbstractSimulation createSimulation(KappaModel model);
    
    @Test
    public void testGetCurrentObservation_noCompartments() {
        
        // Empty observation
        Observation observation = simulation.getCurrentObservation();
        assertNotNull(observation);
        assertEquals(0, observation.observables.size());
        
        // Add observables and variables
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent2"));
        kappaModel.addTransform("label", agents, new ArrayList<Agent>(), new VariableExpression(0.1f), null);
        kappaModel.addPlot("label");
        
        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addVariable(agents, "label2", null);
        kappaModel.addPlot("label2");
        
        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent2"));
        kappaModel.addVariable(agents, "label3", null);
        
        simulation = createSimulation(kappaModel);
        observation = simulation.getCurrentObservation();
        checkObservation(observation, "label", "label2");
    }

    @Test
    public void testGetCurrentObservation_singleCellCompartment() {
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
        
        simulation = createSimulation(kappaModel);
        
        checkObservation("observable1", new ObservationElement(5));
        checkObservation("observable2", new ObservationElement(7, new int[] {1}, "cytosol", new float[] {7}));
    }

    @Test
    public void testGetCurrentObservation_1DCompartment() {
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(4);
        kappaModel.addCompartment("cytosol", dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addVariable(agents, "observable1", new Location("cytosol"));
        kappaModel.addPlot("observable1");
        kappaModel.addVariable(agents, "observable2", new Location("cytosol", new CellIndexExpression("0")));
        kappaModel.addPlot("observable2");
        kappaModel.addInitialValue(agents, "5", new Location("cytosol", new CellIndexExpression("0")));
        kappaModel.addInitialValue(agents, "7", new Location("cytosol", new CellIndexExpression("3")));
        
        simulation = createSimulation(kappaModel);
        
        checkObservation("observable2", new ObservationElement(5));
        checkObservation("observable1", new ObservationElement(12, new int[] {4}, "cytosol", new float[] {5, 0, 0, 7}));
    }

    @Test
    public void testGetCurrentObservation_2DCompartment() {
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(3);
        dimensions.add(2);
        kappaModel.addCompartment("cytosol", dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addVariable(agents, "observable1", new Location("cytosol"));
        kappaModel.addPlot("observable1");
        kappaModel.addVariable(agents, "observable2", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("0")));
        kappaModel.addPlot("observable2");
        kappaModel.addInitialValue(agents, "5", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("0")));
        kappaModel.addInitialValue(agents, "7", new Location("cytosol", new CellIndexExpression("2"), new CellIndexExpression("1")));
        
        simulation = createSimulation(kappaModel);
        
        checkObservation("observable1", new ObservationElement(12, new int[] {3, 2}, "cytosol", new float[][] {{5, 0}, {0, 0}, {0, 7}}));
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
        kappaModel.addVariable(Arrays.asList(agents), label, null);
        simulation = createSimulation(kappaModel);
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
        kappaModel.addInitialValue(agents, "3", null);

        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1", new AgentSite("x", null, "1")));
        agents.add(new Agent("agent2", new AgentSite("x", null, "1")));
        agents.add(new Agent("agent3", new AgentSite("x", null, "2")));
        agents.add(new Agent("agent4", new AgentSite("x", null, "2"), new AgentSite("y", null, null)));
        kappaModel.addInitialValue(agents, "5", null);
        simulation = createSimulation(kappaModel);
        
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
        kappaModel.addCompartment("cytosol", new ArrayList<Integer>());
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(1);
        kappaModel.addCompartment("nucleus", dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addVariable(agents, "observable1", new Location("cytosol"));
        kappaModel.addInitialValue(agents, "5", new Location("cytosol"));
        
        agents.clear();
        agents.add(new Agent("agent2"));
        kappaModel.addVariable(agents, "observable2", new Location("nucleus"));
        kappaModel.addInitialValue(agents, "7", new Location("nucleus"));
        
        simulation = createSimulation(kappaModel);
        
        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(5));
        checkGetQuantity(simulation.getVariable("observable2"), new ObservationElement(7, new int[] {1}, "cytosol", new float[] {7}));
    }

    @Test
    public void testGetQuantity_1DCompartment() {
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(4);
        kappaModel.addCompartment("cytosol", dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addVariable(agents, "observable1", new Location("cytosol"));
        kappaModel.addVariable(agents, "observable2", new Location("cytosol", new CellIndexExpression("0")));
        kappaModel.addInitialValue(agents, "5", new Location("cytosol", new CellIndexExpression("0")));
        kappaModel.addInitialValue(agents, "7", new Location("cytosol", new CellIndexExpression("3")));
        
        simulation = createSimulation(kappaModel);
        
        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(12, new int[] {4}, "cytosol", new float[] {5, 0, 0, 7}));
        checkGetQuantity(simulation.getVariable("observable2"), new ObservationElement(5));
    }

    @Test
    public void testGetQuantity_2DCompartment() {
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(3);
        dimensions.add(2);
        kappaModel.addCompartment("cytosol", dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addVariable(agents, "observable1", new Location("cytosol"));
        kappaModel.addVariable(agents, "observable2", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("0")));
        kappaModel.addInitialValue(agents, "5", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("0")));
        kappaModel.addInitialValue(agents, "7", new Location("cytosol", new CellIndexExpression("2"), new CellIndexExpression("1")));
        
        simulation = createSimulation(kappaModel);
        
        checkGetQuantity(simulation.getVariable("observable1"), new ObservationElement(12, new int[] {3, 2}, "cytosol", new float[][] {{5, 0}, {0, 0}, {0, 7}}));
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
    
    
}
