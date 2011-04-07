package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.TestUtils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.demonsoft.spatialkappa.model.VariableExpression.SimulationToken;
import org.junit.Test;

public class KappaModelTest {

    private KappaModel model = new KappaModel();

    @Test
    public void testAddTransform_invalid() {
        List<Agent> leftAgents = new ArrayList<Agent>();
        List<Agent> rightAgents = new ArrayList<Agent>();
        leftAgents.add(new Agent("agent1"));
        rightAgents.add(new Agent("agent2"));

        try {
            model.addTransform("label", leftAgents, rightAgents, null, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addTransform("label", null, null, new VariableExpression(0.1f), null);
            fail("lhs and rhs null should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransform("label", new ArrayList<Agent>(), new ArrayList<Agent>(), new VariableExpression(0.1f), null);
            fail("lhs and rhs empty should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
    }

    @Test
    public void testAddTransform_withCompartment() {
        List<Complex> leftComplexes = TransformTest.getComplexes(new Agent("agent1", new AgentSite("interface1", "state1", "link1"), new AgentSite("interface2", "state2", "link1")));
        List<Complex> rightComplexes = TransformTest.getComplexes(new Agent("agent2"));
        List<Agent> leftAgents = Transform.getAgents(leftComplexes);
        List<Agent> rightAgents = Transform.getAgents(rightComplexes);
        Location location = new Location("cytosol", new CellIndexExpression("2"));

        model.addTransform("label", leftAgents, rightAgents, new VariableExpression(0.1f), location);

        Transform transform1 = new Transform("label", leftComplexes, rightComplexes, 0.1f);
        checkTransforms(model, location, new Transform[] { transform1 });
        assertEquals(new Variable("label"), model.getVariables().get("label"));

        AggregateAgent expectedAgent = new AggregateAgent("agent1", new AggregateSite("interface1", "state1", "link1"), new AggregateSite("interface2",
                "state2", "link1"));
        checkAggregateAgents(expectedAgent, new AggregateAgent("agent2"));

        leftComplexes = TransformTest.getComplexes(new Agent("agent1", new AgentSite("interface1", "state4", "link4"), new AgentSite("interface3", "state3", "link4")));
        rightComplexes = TransformTest.getComplexes(new Agent("agent3"), new Agent("agent4"));
        leftAgents = Transform.getAgents(leftComplexes);
        rightAgents = Transform.getAgents(rightComplexes);

        model.addTransform("label2", leftAgents, rightAgents, new VariableExpression(0.1f), location);

        checkTransforms(model, location, new Transform[] { transform1, new Transform("label2", leftComplexes, rightComplexes, 0.1f) });
        assertEquals(new Variable("label2"), model.getVariables().get("label2"));

        expectedAgent = new AggregateAgent("agent1", new AggregateSite("interface1", new String[] { "state1", "state4" }, new String[] { "link1", "link4" }),
                new AggregateSite("interface2", "state2", "link1"), new AggregateSite("interface3", "state3", "link4"));
        checkAggregateAgents(expectedAgent, new AggregateAgent("agent2"), new AggregateAgent("agent3"), new AggregateAgent("agent4"));
    }

    @Test
    public void testAddTransport_invalid() {
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));

        try {
            model.addTransport("label", null, agents, new VariableExpression(0.1f));
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            model.addTransport("label", "linkName", agents, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
    }

    @Test
    public void testAddTransport() {
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));

        model.addTransport("label", "linkName", agents, new VariableExpression(0.1f));
        checkTransports(model.getTransports(), new String[] {"label: linkName agent1(),agent2() @ 0.1"});
        assertEquals(new Variable("label"), model.getVariables().get("label"));
        
        model = new KappaModel();
        model.addTransport(null, "linkName", null, new VariableExpression(0.1f));
        checkTransports(model.getTransports(), new String[] {"linkName @ 0.1"});
        assertEquals(0, model.getVariables().size());
    }

    @Test
    public void testAddInitialValue_concreteValue() {
        List<Agent> agents = getList(new Agent("agent1"));
        Location location = new Location("cytosol", new CellIndexExpression("2"));
        Location otherLocation = new Location("nucleus", new CellIndexExpression("2"));

        try {
            model.addInitialValue(null, "0.1", location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addInitialValue(agents, (String) null, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addInitialValue(new ArrayList<Agent>(), "2", location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addInitialValue(agents, "", location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addInitialValue(agents, "a", location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addInitialValue(agents, "0.1", location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        model.addInitialValue(agents, "3", location);

        agents = getList(new Agent("agent1", new AgentSite("x", null, "1")), 
                new Agent("agent2", new AgentSite("x", null, "1")), 
                new Agent("agent3", new AgentSite("x", null, "2")), 
                new Agent("agent4", new AgentSite("x", null, "2")));
        model.addInitialValue(agents, "5", location);
        
        // Check canonicalisation of initial value complexes
        agents = getList(new Agent("agent3", new AgentSite("x", null, "7")), 
                new Agent("agent4", new AgentSite("x", null, "7")));
        model.addInitialValue(agents, "7", otherLocation);

        Complex expectedComplex1 = new Complex(new Agent("agent1"));
        Complex expectedComplex2 = new Complex(new Agent("agent1", new AgentSite("x", null, "1")), new Agent("agent2", new AgentSite("x", null, "1")));
        Complex expectedComplex3 = new Complex(new Agent("agent3", new AgentSite("x", null, "2")), new Agent("agent4", new AgentSite("x", null, "2")));

        checkConcreteLocatedInitialValues(new Object[][] { 
                { expectedComplex1, 3, location }, 
                { expectedComplex2, 5, location }, 
                { expectedComplex3, 5, location }, 
                { expectedComplex3, 7, otherLocation } });
        checkAggregateAgents(new AggregateAgent("agent1", new AggregateSite("x", null, "1")), 
                new AggregateAgent("agent2", new AggregateSite("x", null, "1")),
                new AggregateAgent("agent3", new AggregateSite("x", null, new String[] {"2", "7"})), 
                new AggregateAgent("agent4", new AggregateSite("x", null, new String[] {"2", "7"})));
    }
    
    @Test
    public void testAddInitialValue_reference() {
        List<Agent> agents = getList(new Agent("agent1"));
        Location location = new Location("cytosol", new CellIndexExpression("2"));
        Location otherLocation = new Location("nucleus", new CellIndexExpression("2"));
        model.addVariable(new VariableExpression(3), "variable 3");
        model.addVariable(new VariableExpression(5), "variable 5");
        VariableReference reference = new VariableReference("variable 3");

        try {
            model.addInitialValue(null, reference, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addInitialValue(agents, (VariableReference) null, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addInitialValue(new ArrayList<Agent>(), reference, location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        model.addInitialValue(agents, new VariableReference("variable 3"), location);

        agents = getList(new Agent("agent1", new AgentSite("x", null, "1")), 
                new Agent("agent2", new AgentSite("x", null, "1")), 
                new Agent("agent3", new AgentSite("x", null, "2")), 
                new Agent("agent4", new AgentSite("x", null, "2")));
        model.addInitialValue(agents, new VariableReference("variable 5"), location);
        
        // Check canonicalisation of initial value complexes
        agents = getList(new Agent("agent3", new AgentSite("x", null, "7")), 
                new Agent("agent4", new AgentSite("x", null, "7")));
        model.addInitialValue(agents, "7", otherLocation);

        Complex expectedComplex1 = new Complex(new Agent("agent1"));
        Complex expectedComplex2 = new Complex(new Agent("agent1", new AgentSite("x", null, "1")), new Agent("agent2", new AgentSite("x", null, "1")));
        Complex expectedComplex3 = new Complex(new Agent("agent3", new AgentSite("x", null, "2")), new Agent("agent4", new AgentSite("x", null, "2")));

        checkConcreteLocatedInitialValues(new Object[][] { 
                { expectedComplex1, 3, location }, 
                { expectedComplex2, 5, location }, 
                { expectedComplex3, 5, location }, 
                { expectedComplex3, 7, otherLocation } });
        checkAggregateAgents(new AggregateAgent("agent1", new AggregateSite("x", null, "1")), 
                new AggregateAgent("agent2", new AggregateSite("x", null, "1")),
                new AggregateAgent("agent3", new AggregateSite("x", null, new String[] {"2", "7"})), 
                new AggregateAgent("agent4", new AggregateSite("x", null, new String[] {"2", "7"})));
    }
    
    @Test
    public void testGetFixedLocatedInitialValuesMap() {
        Map<LocatedComplex, Integer> actual = model.getFixedLocatedInitialValuesMap();
        assertEquals(0, actual.size());
        
        // No compartments
        Complex complex1 = TransformTest.getComplexes(new Agent("agent1")).get(0);
        model.addInitialValue(complex1.agents, "3", null);

        checkConcreteLocatedInitialValues(new Object[][] { { complex1, 3, null } });
        
        // add compartment
        model.addCompartment(new Compartment("cytosol", 3));
        
        Complex complex2 = TransformTest.getComplexes(new Agent("agent2")).get(0);

        model.addInitialValue(complex2.agents, "5", new Location("cytosol"));
        model.addInitialValue(complex2.agents, "10", new Location("cytosol", new CellIndexExpression("1")));

        checkConcreteLocatedInitialValues(new Object[][] { 
                { complex1, 1, new Location("cytosol", new CellIndexExpression("0")) }, 
                { complex1, 1, new Location("cytosol", new CellIndexExpression("1")) }, 
                { complex1, 1, new Location("cytosol", new CellIndexExpression("2")) }, 
                { complex2, 2, new Location("cytosol", new CellIndexExpression("0")) }, 
                { complex2, 12, new Location("cytosol", new CellIndexExpression("1")) }, 
                { complex2, 1, new Location("cytosol", new CellIndexExpression("2")) }, 
                });
    }

    @Test
    public void testGetFixedLocatedTransitions_transforms() {
        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
        assertEquals(0, actual.size());
        
        // No compartments
        List<Agent> leftAgents = getList(new Agent("agent1"));
        List<Agent> rightAgents = getList(new Agent("agent2"));
        model.addTransform("label", leftAgents, rightAgents, new VariableExpression(0.1f), null);
        Transform transform1 = new Transform("label", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.1f);

        checkLocatedTransitions(new LocatedTransform[] { 
                new LocatedTransform(transform1, null)
                });
        
        // add compartment
        model = new KappaModel();
        model.addCompartment(new Compartment("cytosol", 3));
        
        leftAgents = getList(new Agent("agent3"));
        rightAgents = getList(new Agent("agent4"));
        model.addTransform("label2", leftAgents, rightAgents, new VariableExpression(0.2f), new Location("cytosol"));
        Transform transform2 = new Transform("label2", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.2f);

        leftAgents = getList(new Agent("agent5"));
        rightAgents = getList(new Agent("agent6"));
        model.addTransform("label3", leftAgents, rightAgents, new VariableExpression(0.3f), new Location("cytosol", new CellIndexExpression("1")));
        Transform transform3 = new Transform("label3", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.3f);

        checkLocatedTransitions(new LocatedTransform[] { 
                new LocatedTransform(transform2, new Location("cytosol", new CellIndexExpression("0"))),
                new LocatedTransform(transform2, new Location("cytosol", new CellIndexExpression("1"))),
                new LocatedTransform(transform2, new Location("cytosol", new CellIndexExpression("2"))),
                new LocatedTransform(transform3, new Location("cytosol", new CellIndexExpression("1"))),
        });
    }

    @Test
    public void testGetFixedLocatedTransitions_transforms_noCompartmentSpecified() {
        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
        assertEquals(0, actual.size());
        
        // No compartments
        List<Agent> leftAgents = getList(new Agent("agent1"));
        List<Agent> rightAgents = getList(new Agent("agent2"));
        model.addTransform("label", leftAgents, rightAgents, new VariableExpression(0.1f), null);
        Transform transform = new Transform("label", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.1f);

        checkLocatedTransitions(new LocatedTransform[] { 
                new LocatedTransform(transform, null)
                });
        
        // add compartment
        model.addCompartment(new Compartment("cytosol", 3));
        model.addCompartment(new Compartment("membrane", 2));
        
        checkLocatedTransitions(new LocatedTransform[] { 
                new LocatedTransform(transform, new Location("cytosol", new CellIndexExpression("0"))),
                new LocatedTransform(transform, new Location("cytosol", new CellIndexExpression("1"))),
                new LocatedTransform(transform, new Location("cytosol", new CellIndexExpression("2"))),
                new LocatedTransform(transform, new Location("membrane", new CellIndexExpression("0"))),
                new LocatedTransform(transform, new Location("membrane", new CellIndexExpression("1"))),
        });
    }

    @Test
    public void testGetConcreteLocatedTransitions_transports() {
        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
        assertEquals(0, actual.size());
        
        model.addCompartment(new Compartment("cytosol", 3));
        model.addCompartmentLink(new CompartmentLink("intra", 
                new Location("cytosol", new CellIndexExpression(new VariableReference("x"))), 
                new Location("cytosol", new CellIndexExpression(new CellIndexExpression(new VariableReference("x")), Operator.PLUS, new CellIndexExpression("1"))),
                Direction.BIDIRECTIONAL));
        model.addCompartmentLink(new CompartmentLink("intra", 
                new Location("cytosol", new CellIndexExpression("2")), 
                new Location("cytosol", new CellIndexExpression("0")),
                Direction.BIDIRECTIONAL));
        
        List<Agent> agents = getList(new Agent("agent1"));
        model.addTransport("label", "intra", agents, new VariableExpression(0.2f));
        Transport transport = new Transport("label",  "intra", agents, 0.2f);

        Location cell0 = new Location("cytosol", new CellIndexExpression("0"));
        Location cell1 = new Location("cytosol", new CellIndexExpression("1"));
        Location cell2 = new Location("cytosol", new CellIndexExpression("2"));
        
        checkLocatedTransitions(new LocatedTransport[] { 
                new LocatedTransport(transport, cell0, cell1),
                new LocatedTransport(transport, cell1, cell0),
                new LocatedTransport(transport, cell1, cell2),
                new LocatedTransport(transport, cell2, cell1),
                new LocatedTransport(transport, cell0, cell2),
                new LocatedTransport(transport, cell2, cell0),
        });
    }

    @Test
    public void testGetConcreteLocatedTransitions_transports_multipleLinksSameLabel() {
        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
        assertEquals(0, actual.size());
        
        model.addCompartment(new Compartment("cytosol", 3));
        model.addCompartmentLink(new CompartmentLink("intra", 
                new Location("cytosol", new CellIndexExpression("0")), new Location("cytosol", new CellIndexExpression("1")),
                Direction.BIDIRECTIONAL));
        model.addCompartmentLink(new CompartmentLink("intra", 
                new Location("cytosol", new CellIndexExpression("1")), new Location("cytosol", new CellIndexExpression("2")),
                Direction.BIDIRECTIONAL));
        
        List<Agent> agents = getList(new Agent("agent1"));
        model.addTransport("label", "intra", agents, new VariableExpression(0.2f));
        Transport transport = new Transport("label",  "intra", agents, 0.2f);

        Location cell0 = new Location("cytosol", new CellIndexExpression("0"));
        Location cell1 = new Location("cytosol", new CellIndexExpression("1"));
        Location cell2 = new Location("cytosol", new CellIndexExpression("2"));
        
        checkLocatedTransitions(new LocatedTransport[] { 
                new LocatedTransport(transport, cell0, cell1),
                new LocatedTransport(transport, cell1, cell0),
                new LocatedTransport(transport, cell1, cell2),
                new LocatedTransport(transport, cell2, cell1),
        });
    }

    private void checkLocatedTransitions(LocatedTransition[] expecteds) {
        List<String> actuals = new ArrayList<String>();
        for (LocatedTransition transition : model.getFixedLocatedTransitions()) {
            actuals.add(transition.toString());
        }
        assertEquals(expecteds.length, actuals.size());
        
        for (LocatedTransition expected : expecteds) {
            String expectedString = expected.toString();
            if (!actuals.contains(expectedString)) {
                fail("Not found: " + expected);
            }
            actuals.remove(expectedString);
        }
    }

    @Test
    public void testAddVariable_withAgents() {
        List<Agent> agents1 = new ArrayList<Agent>();
        agents1.add(new Agent("agent1"));

        try {
            model.addVariable(null, "label", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            model.addVariable(agents1, null, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        model.addVariable(agents1, "label", null);

        checkVariables(model, "'label' ([agent1()])");
        checkAggregateAgents(new AggregateAgent("agent1"));

        List<Agent> agents2 = new ArrayList<Agent>();
        agents2.add(new Agent("agent1"));
        agents2.add(new Agent("agent2"));
        model.addVariable(agents2, "label2", null);

        checkVariables(model, "'label' ([agent1()])", "'label2' ([agent1(), agent2()])");
        checkAggregateAgents(new AggregateAgent("agent1"), new AggregateAgent("agent2"));
    }

    @Test
    public void testAddVariable_withAgentsAndCompartments() {
        List<Agent> agents1 = new ArrayList<Agent>();
        agents1.add(new Agent("agent1"));
        Location location = new Location("cytosol", new CellIndexExpression("2"));

        try {
            model.addVariable(null, "label", location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            model.addVariable(agents1, null, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        model.addVariable(agents1, "label", location);

        checkVariables(model, "'label' cytosol[2] ([agent1()])");
        checkAggregateAgents(new AggregateAgent("agent1"));

        List<Agent> agents2 = new ArrayList<Agent>();
        agents2.add(new Agent("agent1"));
        agents2.add(new Agent("agent2"));
        model.addVariable(agents2, "label2", location);

        checkVariables(model, "'label' cytosol[2] ([agent1()])", "'label2' cytosol[2] ([agent1(), agent2()])");
        checkAggregateAgents(new AggregateAgent("agent1"), new AggregateAgent("agent2"));
    }

    @Test
    public void testAddPlot() {
        try {
            model.addPlot(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        model.addPlot("label");
        model.addPlot("label2");

        assertEquals(2, model.getPlottedVariables().size());
        assertTrue(model.getPlottedVariables().containsAll(getList("label", "label2")));
    }

    @Test
    public void testAddCompartment() {
        try {
            model.addCompartment((Compartment) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        assertNotNull(model.getCompartments());
        assertEquals(0, model.getCompartments().size());

        Compartment compartment1 = new Compartment("name1");
        Compartment compartment2 = new Compartment("name2");
        model.addCompartment(compartment1);
        model.addCompartment(compartment2);
        assertEquals(2, model.getCompartments().size());
        assertEquals(compartment1, model.getCompartments().get(0));
        assertEquals(compartment2, model.getCompartments().get(1));
    }

    @Test
    public void testAddCompartment_notObject() {
        try {
            model.addCompartment(null, new ArrayList<Integer>());
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addCompartment("label", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        assertNotNull(model.getCompartments());
        assertEquals(0, model.getCompartments().size());

        model.addCompartment("compartment1", new ArrayList<Integer>());
        List<Integer> dimensions = new ArrayList<Integer>();
        dimensions.add(2);
        dimensions.add(3);
        model.addCompartment("compartment2", dimensions);
        assertEquals(2, model.getCompartments().size());
        assertEquals("compartment1", model.getCompartments().get(0).toString());
        assertEquals("compartment2[2][3]", model.getCompartments().get(1).toString());
    }

    @Test
    public void testAddCompartmentLink() {
        try {
            model.addCompartmentLink(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        assertNotNull(model.getCompartmentLinks());
        assertEquals(0, model.getCompartmentLinks().size());

        CompartmentLink link1 = new CompartmentLink("name1", new Location("1"), new Location("2"), Direction.BACKWARD);
        CompartmentLink link2 = new CompartmentLink("name2", new Location("1"), new Location("3"), Direction.BIDIRECTIONAL);
        model.addCompartmentLink(link1);
        model.addCompartmentLink(link2);
        assertEquals(2, model.getCompartmentLinks().size());
        assertEquals(link1, model.getCompartmentLinks().get(0));
        assertEquals(link2, model.getCompartmentLinks().get(1));
    }

    private void checkTransforms(KappaModel kappaModel, Location location, Transform[] expectedTransforms) {
        assertEquals(expectedTransforms.length, kappaModel.getLocatedTransforms().size());

        Set<String> actual = new HashSet<String>();
        for (LocatedTransition transform : kappaModel.getLocatedTransforms()) {
            actual.add(transform.toString());
        }
        for (Transform expectedTransform : expectedTransforms) {
            assertTrue(actual.contains(new LocatedTransform(expectedTransform, location).toString()));
        }
    }

    private void checkAggregateAgents(AggregateAgent... expectedAgents) {
        assertEquals(expectedAgents.length, model.getAggregateAgentMap().size());

        for (AggregateAgent expectedAgent : expectedAgents) {
            AggregateAgent agent = model.getAggregateAgentMap().get(expectedAgent.getName());
            assertNotNull(agent);
            assertEquals(expectedAgent.getName(), agent.getName());
            assertEquals(expectedAgent.getSites().size(), agent.getSites().size());
            assertTrue(agent.getSites().containsAll(expectedAgent.getSites()));
        }
    }

    private void checkConcreteLocatedInitialValues(Object[][] expectedQuantities) {
        Map<LocatedComplex, Integer> quantityMap = model.getFixedLocatedInitialValuesMap();
        assertEquals(expectedQuantities.length, quantityMap.size());
        
        for (Object[] expected : expectedQuantities) {
            String complexString = expected[0].toString();
            String locationString = expected[2] == null ? "" : expected[2].toString();
            boolean found = false;
            for (Map.Entry<LocatedComplex, Integer> entry : quantityMap.entrySet()) {
                if (complexString.equals(entry.getKey().complex.toString()) && 
                        locationString.equals(entry.getKey().location == null ? "" : entry.getKey().location.toString())) {
                    assertEquals(expected[1], entry.getValue());
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
        
        // Check canonicalisation
        ComplexMatcher matcher = new ComplexMatcher();
        Set<Complex> complexes = new HashSet<Complex>();
        for (LocatedComplex current : quantityMap.keySet()) {
            boolean found = false;
            for (Complex complex : complexes) {
                if (matcher.isExactMatch(complex, current.complex)) {
                    assertSame(complex, current.complex);
                    found = true;
                    break;
                }
            }
            if (!found) {
                complexes.add(current.complex);
            }
        }
    }

    private void checkVariables(KappaModel kappaModel, String... expectedVariables) {
        checkListByString(kappaModel.getVariables().values(), expectedVariables);
    }

    private void checkListByString(Collection<? extends Object> objects, String[] expected) {
        assertEquals(expected.length, objects.size());

        Set<String> actual = new HashSet<String>();
        for (Object object : objects) {
            actual.add(object.toString());
        }

        for (int index = 0; index < expected.length; index++) {
            assertTrue(actual.contains(expected[index]));
            actual.remove(expected[index]);
        }
        assertTrue(actual.isEmpty());
    }

    private void checkTransports(List<Transport> transports, String[] expected) {
        assertEquals(expected.length, transports.size());

        Set<String> actual = new HashSet<String>();
        for (Transport transport : transports) {
            actual.add(transport.toString());
        }

        for (int index = 0; index < expected.length; index++) {
            assertTrue(actual.contains(expected[index]));
            actual.remove(expected[index]);
        }
        assertTrue(actual.isEmpty());
    }
    
    @Test
    public void testValidate() {
        // Trivial case
        model.validate();
        
        // TODO deep referencing
        
        // TODO cyclic variables

    }
    
    @Test
    public void testValidate_plot() {
        // Missing plot reference
        model.addPlot("unknown");
        checkValidate_failure("Reference 'unknown' not found");
        
        // Plot reference to infinite variable
        model = new KappaModel();
        model.addVariable(new VariableExpression(Constant.INFINITY), "variableRef");
        model.addPlot("variableRef");
        checkValidate_failure("Reference 'variableRef' evaluates to infinity - cannot plot");
        
        // Plot reference to variable
        model = new KappaModel();
        model.addVariable(new VariableExpression(2f), "variableRef");
        model.addPlot("variableRef");
        model.validate();
        
        // Plot reference to transport
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addCompartmentLink(new CompartmentLink("compartmentLink", new Location("known"), new Location("known"), Direction.BIDIRECTIONAL));
        model.addTransport("transportRef", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(2f));
        model.addPlot("transportRef");
        model.validate();
        
        // Plot reference to transform
        model = new KappaModel();
        model.addTransform("transformRef", getList(new Agent("agent1")), null, new VariableExpression(2f), null);
        model.addPlot("transformRef");
        model.validate();
        
        // Plot reference to kappa expression
        model = new KappaModel();
        model.addVariable(getList(new Agent("agent1")), "kappaRef", null);
        model.addPlot("kappaRef");
        model.validate();
    }
    
    @Test
    public void testValidate_initialValue() {
        // Missing initial value reference
        model = new KappaModel();
        model.addInitialValue(getList(new Agent("agent1")), new VariableReference("unknown"), null);
        checkValidate_failure("Reference 'unknown' not found");
        
        // Non fixed initial value reference
        model = new KappaModel();
        model.addVariable(new VariableExpression(SimulationToken.EVENTS), "variableRef");
        model.addInitialValue(getList(new Agent("agent1")), new VariableReference("variableRef"), null);
        checkValidate_failure("Reference 'variableRef' not fixed - cannot be initial value");
        
        // Infinite initial value reference
        model = new KappaModel();
        model.addVariable(new VariableExpression(Constant.INFINITY), "variableRef");
        model.addInitialValue(getList(new Agent("agent1")), new VariableReference("variableRef"), null);
        checkValidate_failure("Reference 'variableRef' evaluates to infinity - cannot be initial value");
        
        // Initial value reference to variable
        model = new KappaModel();
        model.addVariable(new VariableExpression(2f), "variableRef");
        model.addInitialValue(getList(new Agent("agent1")), new VariableReference("variableRef"), null);
        model.validate();
        
        model = new KappaModel();
        model.addVariable(new VariableExpression(2f), "variableRef");
        model.addVariable(new VariableExpression(new VariableReference("variableRef")), "other");
        model.addInitialValue(getList(new Agent("agent1")), new VariableReference("other"), null);
        model.validate();
        
        // Initial value concrete
        model = new KappaModel();
        model.addInitialValue(getList(new Agent("agent1")), "1000", null);
        model.validate();
    }
    
    @Test
    public void testValidate_transport() {
        // Missing transport reference
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addCompartmentLink(new CompartmentLink("compartmentLink", new Location("known"), new Location("known"), Direction.BIDIRECTIONAL));
        model.addTransport("transportRef", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(new VariableReference("unknown")));
        checkValidate_failure("Reference 'unknown' not found");
        
        model = new KappaModel();
        model.addVariable(new VariableExpression(2), "variableRef");
        model.addTransport("transportRef", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(new VariableReference("variableRef")));
        checkValidate_failure("Compartment link 'compartmentLink' not found");
        
        // Non fixed transport reference
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addCompartmentLink(new CompartmentLink("compartmentLink", new Location("known"), new Location("known"), Direction.BIDIRECTIONAL));
        model.addVariable(new VariableExpression(SimulationToken.EVENTS), "variableRef");
        model.addTransport("other", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(new VariableReference("variableRef")));
        checkValidate_failure("Reference 'variableRef' not fixed");
        
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addCompartmentLink(new CompartmentLink("compartmentLink", new Location("known"), new Location("known"), Direction.BIDIRECTIONAL));
        model.addTransport("transportRef", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(2));
        model.validate();

        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addCompartmentLink(new CompartmentLink("compartmentLink", new Location("known"), new Location("known"), Direction.BIDIRECTIONAL));
        model.addTransport("transportRef", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(Constant.INFINITY));
        model.validate();
        
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addCompartmentLink(new CompartmentLink("compartmentLink", new Location("known"), new Location("known"), Direction.BIDIRECTIONAL));
        model.addVariable(new VariableExpression(2), "variableRef");
        model.addTransport("other", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(new VariableReference("variableRef")));
        model.validate();
        
    }
    
    @Test
    public void testValidate_transform() {
        // Missing transform reference
        model = new KappaModel();
        model.addTransform(null, getList(new Agent("agent1")), null, 
                new VariableExpression(new VariableReference("unknown")), 
                null);
        checkValidate_failure("Reference 'unknown' not found");
        
        // Non fixed transform reference
        model = new KappaModel();
        model.addVariable(new VariableExpression(SimulationToken.EVENTS), "variableRef");
        model.addTransform(null, getList(new Agent("agent1")), null, 
                new VariableExpression(new VariableReference("variableRef")), 
                null);
        checkValidate_failure("Reference 'variableRef' not fixed");
        
        model = new KappaModel();
        model.addTransform(null, getList(new Agent("agent1")), null, 
                new VariableExpression(Constant.INFINITY), 
                null);
        model.validate();
    }
    
    @Test
    public void testValidate_variable() {
        // Missing variable reference
        model = new KappaModel();
        model.addVariable(new VariableExpression(new VariableReference("unknown")), "label");
        checkValidate_failure("Reference 'unknown' not found");

        // Variable reference to infinite variable
        model = new KappaModel();
        model.addVariable(new VariableExpression(Constant.INFINITY), "variableRef");
        model.validate();
        
        // Variable reference to variable
        model = new KappaModel();
        model.addVariable(new VariableExpression(2f), "variableRef");
        model.addVariable(new VariableExpression(new VariableReference("variableRef")), "label");
        model.validate();
        
        // Variable reference to non fixed variable
        model = new KappaModel();
        model.addVariable(new VariableExpression(SimulationToken.EVENTS), "other");
        model.addVariable(new VariableExpression(new VariableReference("other")), "label");
        model.validate();
        
        // Variable reference to kappa expression
        model = new KappaModel();
        model.addVariable(getList(new Agent("agent1")), "kappaRef", null);
        model.addVariable(new VariableExpression(new VariableReference("kappaRef")), "label");
        model.validate();

    }
    
    @Test
    public void testValidate_compartmentLink() {
        // Missing compartment link reference
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addCompartmentLink(new CompartmentLink("name", new Location("known"), new Location("unknown"), Direction.BIDIRECTIONAL));
        checkValidate_failure("Compartment 'unknown' not found");
        
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addCompartmentLink(new CompartmentLink("name", new Location("known"), new Location("unknown"), Direction.BIDIRECTIONAL));
        checkValidate_failure("Compartment 'unknown' not found");
        
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addCompartmentLink(new CompartmentLink("name", new Location("known"), new Location("known"), Direction.BIDIRECTIONAL));
        model.validate();

        // TODO compartment link range out of bounds ?
        
    }
    
    @Test
    public void testValidate_perturbation() {
        
        /*
         *     
    public void addPerturbation(Perturbation perturbation); // TODO

         * 
         */
    }
    
    private void checkValidate_failure(String expectedMessage) {
        try {
            model.validate();
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals(expectedMessage, ex.getMessage());
        }

    }

}
