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
import org.demonsoft.spatialkappa.model.Direction;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedObservable;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.MathExpression;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
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
        kappaModel.addTransform(Direction.FORWARD, "label", agents, new ArrayList<Agent>(), "0.1", null, null);
        kappaModel.addObservable("label");
        
        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addObservable(agents, "label2", null, true);
        simulation.initialise();
        
        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent2"));
        kappaModel.addObservable(agents, "label3", null, false);
        
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
        kappaModel.addObservable(agents, "observable1", new Location("cytosol"), true);
        kappaModel.addInitialValue(agents, "5", new Location("cytosol"));
        
        agents.clear();
        agents.add(new Agent("agent2"));
        kappaModel.addObservable(agents, "observable2", new Location("nucleus"), true);
        kappaModel.addInitialValue(agents, "7", new Location("nucleus"));
        
        simulation.initialise();
        
        checkObservation(getLocatedObservable("observable1 cytosol"), new ObservationElement(5));
        checkObservation(getLocatedObservable("observable2 nucleus"), new ObservationElement(7, new int[] {1}, new int[] {7}));
    }

    @Test
    public void testGetCurrentObservation_1DCompartment() {
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(4);
        kappaModel.addCompartment("cytosol", dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addObservable(agents, "observable1", new Location("cytosol"), true);
        kappaModel.addObservable(agents, "observable2", new Location("cytosol", new MathExpression("0")), true);
        kappaModel.addInitialValue(agents, "5", new Location("cytosol", new MathExpression("0")));
        kappaModel.addInitialValue(agents, "7", new Location("cytosol", new MathExpression("3")));
        
        simulation.initialise();
        
        checkObservation(getLocatedObservable("observable2 cytosol[0]"), new ObservationElement(5));
        checkObservation(getLocatedObservable("observable1 cytosol"), new ObservationElement(12, new int[] {4}, new int[] {5, 0, 0, 7}));
    }

    @Test
    public void testGetCurrentObservation_2DCompartment() {
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(3);
        dimensions.add(2);
        kappaModel.addCompartment("cytosol", dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addObservable(agents, "observable1", new Location("cytosol"), true);
        kappaModel.addObservable(agents, "observable2", new Location("cytosol", new MathExpression("0"), new MathExpression("0")), true);
        kappaModel.addInitialValue(agents, "5", new Location("cytosol", new MathExpression("0"), new MathExpression("0")));
        kappaModel.addInitialValue(agents, "7", new Location("cytosol", new MathExpression("2"), new MathExpression("1")));
        
        simulation.initialise();
        
        checkObservation(getLocatedObservable("observable1 cytosol"), new ObservationElement(12, new int[] {3, 2}, new int[][] {{5, 0}, {0, 0}, {0, 7}}));
        checkObservation(getLocatedObservable("observable2 cytosol[0][0]"), new ObservationElement(5));
    }

//    @Test
//    public void testCanonicalComplexes() {
//        kappaModel.addCompartment(new Compartment("cytosol"));
//        kappaModel.addCompartment(new Compartment("nucleus"));
//        kappaModel.addInitialValue(getList(new Agent("mRNA", new AgentSite("enc", "CRY1", null))), "29", new Location("nucleus"));
//        kappaModel.addInitialValue(getList(new Agent("mRNA", new AgentSite("enc", "CRY1", null))), "4706", new Location("cytosol"));
//
//        simulation.initialise();
//        
//        Map<String, Complex> complexNameMap = new HashMap<String, Complex>();
//        for (LocatedComplex current : simulation.complexStore.locatedComplexQuantities.keySet()) {
//            String label = current.complex.toString();
//            if (complexNameMap.containsKey(label)) {
//                assertSame(complexNameMap.get(label), current.complex);
//            }
//            else {
//                complexNameMap.put(label, current.complex);
//            }
//        }
//    }

    private void checkObservation(LocatedObservable locatedObservable, ObservationElement element) {
        Observation observation = simulation.getCurrentObservation();
        ObservationElement actual = observation.observables.get(locatedObservable);
        assertEquals(element.toString(), actual.toString());
    }
    
    private void checkGetQuantity(LocatedObservable locatedObservable, ObservationElement element) {
        ObservationElement actual = simulation.getComplexQuantity(locatedObservable);
        assertEquals(element.toString(), actual.toString());
    }
    
    private LocatedObservable getLocatedObservable(String label) {
        for (LocatedObservable observable : simulation.getLocatedObservables()) {
            if (label.equals(observable.label)) {
                return observable;
            }
        }
        return null;
    }
    
    private void checkObservation(Observation observation, String... expectedObservableNames) {
        assertNotNull(observation);
        assertEquals(expectedObservableNames.length, observation.observables.size());
        
        Set<String> actualNames = new HashSet<String>();
        for (LocatedObservable observable : observation.observables.keySet()) {
            actualNames.add(observable.observable.label);
        }
        
        for (String current : expectedObservableNames) {
            assertTrue("Missing observable: " + current, actualNames.contains(current));
        }
    }
    
    private void checkQuantity(String label, int expected, Agent...agents) {
        kappaModel.addObservable(Arrays.asList(agents), label, null, true);
        simulation.initialise();
        LocatedObservable observable = getSimulationObservable(label);
        assertEquals(new ObservationElement(expected), simulation.getComplexQuantity(observable));
    }

    private LocatedObservable getSimulationObservable(String label) {
        for (LocatedObservable observable : simulation.getLocatedObservables()) {
            if (label.equals(observable.label)) {
                return observable;
            }
        }
        return null;
    }

    @Test
    public void testGetNextEndTime() {
        assertEquals(2.0, simulation.getNextEndTime(1.01f, 1f), 0.05f);
        assertEquals(10.0, simulation.getNextEndTime(7.51f, 2.5f), 0.05f);
    }


    @Test
    public void testGetQuantity_noCompartments() {
        try {
            new ComplexMatchingSimulation().getComplexQuantity(null);
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
        simulation.initialise();
        
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
        kappaModel.addObservable(agents, "observable1", new Location("cytosol"), true);
        kappaModel.addInitialValue(agents, "5", new Location("cytosol"));
        
        agents.clear();
        agents.add(new Agent("agent2"));
        kappaModel.addObservable(agents, "observable2", new Location("nucleus"), true);
        kappaModel.addInitialValue(agents, "7", new Location("nucleus"));
        
        simulation.initialise();
        
        checkGetQuantity(getLocatedObservable("observable1 cytosol"), new ObservationElement(5));
        checkGetQuantity(getLocatedObservable("observable2 nucleus"), new ObservationElement(7, new int[] {1}, new int[] {7}));
    }

    @Test
    public void testGetQuantity_1DCompartment() {
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(4);
        kappaModel.addCompartment("cytosol", dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addObservable(agents, "observable1", new Location("cytosol"), true);
        kappaModel.addObservable(agents, "observable2", new Location("cytosol", new MathExpression("0")), true);
        kappaModel.addInitialValue(agents, "5", new Location("cytosol", new MathExpression("0")));
        kappaModel.addInitialValue(agents, "7", new Location("cytosol", new MathExpression("3")));
        
        simulation.initialise();
        
        checkGetQuantity(getLocatedObservable("observable1 cytosol"), new ObservationElement(12, new int[] {4}, new int[] {5, 0, 0, 7}));
        checkGetQuantity(getLocatedObservable("observable2 cytosol[0]"), new ObservationElement(5));
    }

    @Test
    public void testGetQuantity_2DCompartment() {
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(3);
        dimensions.add(2);
        kappaModel.addCompartment("cytosol", dimensions);
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        kappaModel.addObservable(agents, "observable1", new Location("cytosol"), true);
        kappaModel.addObservable(agents, "observable2", new Location("cytosol", new MathExpression("0"), new MathExpression("0")), true);
        kappaModel.addInitialValue(agents, "5", new Location("cytosol", new MathExpression("0"), new MathExpression("0")));
        kappaModel.addInitialValue(agents, "7", new Location("cytosol", new MathExpression("2"), new MathExpression("1")));
        
        simulation.initialise();
        
        checkGetQuantity(getLocatedObservable("observable1 cytosol"), new ObservationElement(12, new int[] {3, 2}, new int[][] {{5, 0}, {0, 0}, {0, 7}}));
        checkGetQuantity(getLocatedObservable("observable2 cytosol[0][0]"), new ObservationElement(5));
    }

    
}
