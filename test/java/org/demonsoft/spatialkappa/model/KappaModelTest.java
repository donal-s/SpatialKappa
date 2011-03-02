package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.TestUtils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.AggregateAgent;
import org.demonsoft.spatialkappa.model.AggregateSite;
import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.CompartmentLink;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.ComplexMatcher;
import org.demonsoft.spatialkappa.model.Direction;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedComplex;
import org.demonsoft.spatialkappa.model.LocatedObservable;
import org.demonsoft.spatialkappa.model.LocatedTransform;
import org.demonsoft.spatialkappa.model.LocatedTransition;
import org.demonsoft.spatialkappa.model.LocatedTransport;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.MathExpression;
import org.demonsoft.spatialkappa.model.Observable;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.Transport;
import org.demonsoft.spatialkappa.model.Utils;
import org.demonsoft.spatialkappa.model.KappaModel.InitialValue;
import org.demonsoft.spatialkappa.model.MathExpression.Operator;
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
            model.addTransform(null, "label", leftAgents, rightAgents, "0.1", null, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.FORWARD, "label", leftAgents, rightAgents, null, null, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.FORWARD, "label", null, null, "0.1", null, null);
            fail("lhs and rhs null should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.FORWARD, "label", new ArrayList<Agent>(), new ArrayList<Agent>(), "0.1", null, null);
            fail("lhs and rhs empty should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.BACKWARD, "label", leftAgents, rightAgents, "0.1", null, null);
            fail("unknown transition should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.FORWARD, "label", leftAgents, rightAgents, "", null, null);
            fail("unknown rate should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.FORWARD, "label", leftAgents, rightAgents, "a", null, null);
            fail("unknown rate should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.BIDIRECTIONAL, "label", leftAgents, rightAgents, "0.1", "", null);
            fail("unknown rate should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.BIDIRECTIONAL, "label", leftAgents, rightAgents, "0.1", "a", null);
            fail("unknown rate should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.FORWARD, "label", leftAgents, rightAgents, "0.1", "0.2", null);
            fail("backward rate present should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransform(Direction.BIDIRECTIONAL, "label", leftAgents, rightAgents, "0.1", null, null);
            fail("backward rate missing should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

    }

    @Test
    public void testAddTransform_withCompartment() {
        Collection<AgentSite> interfaces = new ArrayList<AgentSite>();
        interfaces.add(new AgentSite("interface1", "state1", "link1"));
        interfaces.add(new AgentSite("interface2", "state2", "link1"));
        List<Complex> leftComplexes = TransformTest.getComplexes(new Agent("agent1", interfaces));
        List<Complex> rightComplexes = TransformTest.getComplexes(new Agent("agent2"));
        List<Agent> leftAgents = Transform.getAgents(leftComplexes);
        List<Agent> rightAgents = Transform.getAgents(rightComplexes);
        Location location = new Location("cytosol", new MathExpression("2"));

        model.addTransform(Direction.FORWARD, "label", leftAgents, rightAgents, "0.1", null, location);

        Transform transform1 = new Transform("label", leftComplexes, rightComplexes, "0.1");
        checkTransforms(location, new Transform[] { transform1 });

        AggregateAgent expectedAgent = new AggregateAgent("agent1", new AggregateSite("interface1", "state1", "link1"), new AggregateSite("interface2",
                "state2", "link1"));
        checkAggregateAgents(expectedAgent, new AggregateAgent("agent2"));

        interfaces = new ArrayList<AgentSite>();
        interfaces.add(new AgentSite("interface1", "state4", "link4"));
        interfaces.add(new AgentSite("interface3", "state3", "link4"));
        leftComplexes = TransformTest.getComplexes(new Agent("agent1", interfaces));
        rightComplexes = TransformTest.getComplexes(new Agent("agent3"), new Agent("agent4"));
        leftAgents = Transform.getAgents(leftComplexes);
        rightAgents = Transform.getAgents(rightComplexes);

        model.addTransform(Direction.BIDIRECTIONAL, "label", leftAgents, rightAgents, "0.1", "0.3", location);

        checkTransforms(location, new Transform[] { transform1, new Transform("label", leftComplexes, rightComplexes, "0.1"),
                new Transform("label", rightComplexes, leftComplexes, "0.3") });

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
            model.addTransport("label", null, agents, "0.1");
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

        try {
            model.addTransport("label", "linkName", agents, "");
            fail("unknown rate should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            model.addTransport("label", "linkName", agents, "a");
            fail("unknown rate should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

    }

    @Test
    public void testAddTransport() {
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));

        model.addTransport("label", "linkName", agents, "0.1");
        checkTransports(model.getTransports(), new String[] {"label: linkName agent1(),agent2() @ 0.1"});
        
        model.getTransports().clear();
        model.addTransport(null, "linkName", null, "0.1");
        checkTransports(model.getTransports(), new String[] {"linkName @ 0.1"});
    }

    @Test
    public void testAddInitialValue() {
        List<Agent> agents = getList(new Agent("agent1"));
        Location location = new Location("cytosol", new MathExpression("2"));
        Location otherLocation = new Location("nucleus", new MathExpression("2"));

        try {
            model.addInitialValue(null, "0.1", location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addInitialValue(agents, null, location);
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
    public void testGetConcreteLocatedInitialValuesMap() {
        Map<LocatedComplex, Integer> actual = model.getConcreteLocatedInitialValuesMap();
        assertEquals(0, actual.size());
        
        // No compartments
        Complex complex1 = TransformTest.getComplexes(new Agent("agent1")).get(0);
        model.addInitialValue(complex1.agents, "3", null);

        checkConcreteLocatedInitialValues(new Object[][] { { complex1, 3, null } });
        
        // add compartment
        model.addCompartment(new Compartment("cytosol", 3));
        
        Complex complex2 = TransformTest.getComplexes(new Agent("agent2")).get(0);

        model.addInitialValue(complex2.agents, "5", new Location("cytosol"));
        model.addInitialValue(complex2.agents, "10", new Location("cytosol", new MathExpression("1")));

        checkConcreteLocatedInitialValues(new Object[][] { 
                { complex1, 1, new Location("cytosol", new MathExpression("0")) }, 
                { complex1, 1, new Location("cytosol", new MathExpression("1")) }, 
                { complex1, 1, new Location("cytosol", new MathExpression("2")) }, 
                { complex2, 2, new Location("cytosol", new MathExpression("0")) }, 
                { complex2, 12, new Location("cytosol", new MathExpression("1")) }, 
                { complex2, 1, new Location("cytosol", new MathExpression("2")) }, 
                });
    }

    @Test
    public void testGetConcreteLocatedTransitions_transforms() {
        List<LocatedTransition> actual = model.getConcreteLocatedTransitions();
        assertEquals(0, actual.size());
        
        // No compartments
        List<Agent> leftAgents = getAgents(new Agent("agent1"));
        List<Agent> rightAgents = getAgents(new Agent("agent2"));
        model.addTransform(Direction.FORWARD, "label", leftAgents, rightAgents, "0.1", null, null);
        Transform transform1 = new Transform("label", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), "0.1");

        checkLocatedTransitions(new LocatedTransform[] { 
                new LocatedTransform(transform1, null)
                });
        
        // add compartment
        model = new KappaModel();
        model.addCompartment(new Compartment("cytosol", 3));
        
        leftAgents = getAgents(new Agent("agent3"));
        rightAgents = getAgents(new Agent("agent4"));
        model.addTransform(Direction.FORWARD, "label2", leftAgents, rightAgents, "0.2", null, new Location("cytosol"));
        Transform transform2 = new Transform("label2", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), "0.2");

        leftAgents = getAgents(new Agent("agent5"));
        rightAgents = getAgents(new Agent("agent6"));
        model.addTransform(Direction.FORWARD, "label3", leftAgents, rightAgents, "0.3", null, new Location("cytosol", new MathExpression("1")));
        Transform transform3 = new Transform("label3", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), "0.3");

        checkLocatedTransitions(new LocatedTransform[] { 
                new LocatedTransform(transform2, new Location("cytosol", new MathExpression("0"))),
                new LocatedTransform(transform2, new Location("cytosol", new MathExpression("1"))),
                new LocatedTransform(transform2, new Location("cytosol", new MathExpression("2"))),
                new LocatedTransform(transform3, new Location("cytosol", new MathExpression("1"))),
        });
    }

    @Test
    public void testGetConcreteLocatedTransitions_transforms_noCompartmentSpecified() {
        List<LocatedTransition> actual = model.getConcreteLocatedTransitions();
        assertEquals(0, actual.size());
        
        // No compartments
        List<Agent> leftAgents = getAgents(new Agent("agent1"));
        List<Agent> rightAgents = getAgents(new Agent("agent2"));
        model.addTransform(Direction.FORWARD, "label", leftAgents, rightAgents, "0.1", null, null);
        Transform transform = new Transform("label", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), "0.1");

        checkLocatedTransitions(new LocatedTransform[] { 
                new LocatedTransform(transform, null)
                });
        
        // add compartment
        model.addCompartment(new Compartment("cytosol", 3));
        model.addCompartment(new Compartment("membrane", 2));
        
        checkLocatedTransitions(new LocatedTransform[] { 
                new LocatedTransform(transform, new Location("cytosol", new MathExpression("0"))),
                new LocatedTransform(transform, new Location("cytosol", new MathExpression("1"))),
                new LocatedTransform(transform, new Location("cytosol", new MathExpression("2"))),
                new LocatedTransform(transform, new Location("membrane", new MathExpression("0"))),
                new LocatedTransform(transform, new Location("membrane", new MathExpression("1"))),
        });
    }

    @Test
    public void testGetConcreteLocatedTransitions_transports() {
        List<LocatedTransition> actual = model.getConcreteLocatedTransitions();
        assertEquals(0, actual.size());
        
        model.addCompartment(new Compartment("cytosol", 3));
        model.addCompartmentLink(new CompartmentLink("intra", new Location("cytosol", new MathExpression("x")), 
                new Location("cytosol", new MathExpression(new MathExpression("x"), Operator.PLUS, new MathExpression("1"))),
                Direction.BIDIRECTIONAL));
        
        List<Agent> agents = getAgents(new Agent("agent1"));
        model.addTransport("label", "intra", agents, "0.2");
        Transport transport = new Transport("label",  "intra", agents, "0.2");

        Location cell0 = new Location("cytosol", new MathExpression("0"));
        Location cell1 = new Location("cytosol", new MathExpression("1"));
        Location cell2 = new Location("cytosol", new MathExpression("2"));
        
        checkLocatedTransitions(new LocatedTransport[] { 
                new LocatedTransport(transport, cell0, cell1),
                new LocatedTransport(transport, cell1, cell0),
                new LocatedTransport(transport, cell1, cell2),
                new LocatedTransport(transport, cell2, cell1),
        });
    }

    @Test
    public void testGetConcreteLocatedTransitions_transports_multipleLinksSameLabel() {
        List<LocatedTransition> actual = model.getConcreteLocatedTransitions();
        assertEquals(0, actual.size());
        
        model.addCompartment(new Compartment("cytosol", 3));
        model.addCompartmentLink(new CompartmentLink("intra", 
                new Location("cytosol", new MathExpression("0")), new Location("cytosol", new MathExpression("1")),
                Direction.BIDIRECTIONAL));
        model.addCompartmentLink(new CompartmentLink("intra", 
                new Location("cytosol", new MathExpression("1")), new Location("cytosol", new MathExpression("2")),
                Direction.BIDIRECTIONAL));
        
        List<Agent> agents = getAgents(new Agent("agent1"));
        model.addTransport("label", "intra", agents, "0.2");
        Transport transport = new Transport("label",  "intra", agents, "0.2");

        Location cell0 = new Location("cytosol", new MathExpression("0"));
        Location cell1 = new Location("cytosol", new MathExpression("1"));
        Location cell2 = new Location("cytosol", new MathExpression("2"));
        
        checkLocatedTransitions(new LocatedTransport[] { 
                new LocatedTransport(transport, cell0, cell1),
                new LocatedTransport(transport, cell1, cell0),
                new LocatedTransport(transport, cell1, cell2),
                new LocatedTransport(transport, cell2, cell1),
        });
    }

    private void checkLocatedTransitions(LocatedTransition[] expecteds) {
        List<String> actuals = new ArrayList<String>();
        for (LocatedTransition transition : model.getConcreteLocatedTransitions()) {
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
    public void testAddObservable_withAgents() {
        List<Agent> agents1 = new ArrayList<Agent>();
        agents1.add(new Agent("agent1"));

        try {
            model.addObservable(null, "label", null, true);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        model.addObservable(agents1, "label", null, true);

        checkObservables(null, new Observable[] { new Observable(new Complex(agents1), "label", true) });
        checkAggregateAgents(new AggregateAgent("agent1"));

        List<Agent> agents2 = new ArrayList<Agent>();
        agents2.add(new Agent("agent1"));
        agents2.add(new Agent("agent2"));
        model.addObservable(agents2, null, null, true);

        checkObservables(null, new Observable[] { new Observable(new Complex(agents1), "label", true), new Observable(new Complex(agents2), null, true) });
        checkAggregateAgents(new AggregateAgent("agent1"), new AggregateAgent("agent2"));
    }

    @Test
    public void testAddObservable_withAgentsAndCompartments() {
        List<Agent> agents1 = new ArrayList<Agent>();
        agents1.add(new Agent("agent1"));
        Location location = new Location("cytosol", new MathExpression("2"));

        try {
            model.addObservable(null, "label", location, true);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        model.addObservable(agents1, "label", location, true);

        checkObservables(location, new Observable[] { new Observable(new Complex(agents1), "label", true) });
        checkAggregateAgents(new AggregateAgent("agent1"));

        List<Agent> agents2 = new ArrayList<Agent>();
        agents2.add(new Agent("agent1"));
        agents2.add(new Agent("agent2"));
        model.addObservable(agents2, null, location, true);

        checkObservables(location, new Observable[] { new Observable(new Complex(agents1), "label", true),
                new Observable(new Complex(agents2), null, true) });
        checkAggregateAgents(new AggregateAgent("agent1"), new AggregateAgent("agent2"));
    }

    @Test
    public void testAddObservable_labelOnly() {
        try {
            model.addObservable(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        model.addObservable("label");
        model.addObservable("label2");

        checkObservables(null, new Observable[] { new Observable("label"), new Observable("label2") });
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

    @Test
    public void testInitialValue() {
        List<Complex> complexes = Utils.getComplexes(getList(new Agent("A"), 
                new Agent("B", new AgentSite("x", null, "1")), 
                new Agent("C", new AgentSite("y", null, "1"))));
        Location location = new Location("cytosol", new MathExpression("2"));

        try {
            new InitialValue(null, 5, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new InitialValue(new ArrayList<Complex>(), 5, location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        try {
            new InitialValue(complexes, 0, location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        InitialValue initialValue = new InitialValue(complexes, 5, location);
        assertEquals("[[A()], [B(x!1), C(y!1)]]", initialValue.complexes.toString());
        assertEquals(5, initialValue.quantity);
        assertEquals(location, initialValue.location);

        initialValue = new InitialValue(complexes, 5, null);
        assertEquals("[[A()], [B(x!1), C(y!1)]]", initialValue.complexes.toString());
        assertEquals(5, initialValue.quantity);
        assertEquals(null, initialValue.location);
    }

    @Test
    public void testGetInitialValues() {
        Map<Complex, Integer> initialValues = model.getInitialValuesMap();
        assertNotNull(initialValues);
        assertEquals(0, initialValues.size());

        List<Agent> agents1 = new ArrayList<Agent>();
        agents1.add(new Agent("agent1"));
        model.addInitialValue(agents1, "3", null);

        List<Agent> agents2 = new ArrayList<Agent>();
        agents2.add(new Agent("agent1", new AgentSite("x", null, "1")));
        agents2.add(new Agent("agent2", new AgentSite("x", null, "1")));
        agents2.add(new Agent("agent3", new AgentSite("x", null, "2")));
        agents2.add(new Agent("agent4", new AgentSite("x", null, "2")));
        model.addInitialValue(agents2, "5", null);

        Complex expectedComplex1 = new Complex(new Agent("agent1"));
        Complex expectedComplex2 = new Complex(new Agent("agent1", new AgentSite("x", null, "1")), new Agent("agent2", new AgentSite("x", null, "1")));
        Complex expectedComplex3 = new Complex(new Agent("agent3", new AgentSite("x", null, "2")), new Agent("agent4", new AgentSite("x", null, "2")));

        Object[][] expected = new Object[][] { { expectedComplex1, 3, null }, { expectedComplex2, 5, null }, { expectedComplex3, 5, null } };
        checkInitialValues(model.getInitialValuesMap(), expected);
    }

    @Test
    public void testGetVisibleObservables() {
        assertEquals(0, model.getVisibleObservables().size());
        
        List<Agent> agents1 = new ArrayList<Agent>();
        agents1.add(new Agent("A"));
        model.addObservable(agents1, "label", null, true);
        model.addObservable(agents1, "label2", null, false);

        List<Agent> agents2 = new ArrayList<Agent>();
        agents2.add(new Agent("B"));
        agents2.add(new Agent("C"));
        model.addObservable(agents2, null, null, true);

        checkListByString(model.getVisibleObservables(), new String[] { "'label' ([A()])", "'[B(), C()]' ([B(), C()])" });

    }

    private void checkTransforms(Location location, Transform[] expectedTransforms) {
        checkTransforms(model, location, expectedTransforms);
    }

    public static void checkTransforms(KappaModel kappaModel, Transform[] expectedTransforms) {
        checkTransforms(kappaModel, null, expectedTransforms);
    }

    public static void checkTransforms(KappaModel kappaModel, Location location, Transform[] expectedTransforms) {
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
        checkInitialLocatedValues(model.getConcreteLocatedInitialValuesMap(), expectedQuantities);
    }

    public static void checkInitialValues(KappaModel kappaModel, Object[][] expectedQuantities) {
        checkLocatedInitialValues(kappaModel.getConcreteLocatedInitialValuesMap(), expectedQuantities);
    }

    private static void checkLocatedInitialValues(Map<LocatedComplex, Integer> quantityMap, Object[][] expectedQuantities) {
        assertEquals(expectedQuantities.length, quantityMap.size());

        for (Object[] expected : expectedQuantities) {
            String complexString = expected[0].toString();
            boolean found = false;
            for (Map.Entry<LocatedComplex, Integer> entry : quantityMap.entrySet()) {
                if (complexString.equals(entry.getKey().complex.toString())) {
                    assertEquals(expected[1], entry.getValue());
                    assertEquals(expected[2], entry.getKey().location);
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    private static void checkInitialValues(Map<Complex, Integer> quantityMap, Object[][] expectedQuantities) {
        assertEquals(expectedQuantities.length, quantityMap.size());

        for (Object[] expected : expectedQuantities) {
            String complexString = expected[0].toString();
            boolean found = false;
            for (Map.Entry<Complex, Integer> entry : quantityMap.entrySet()) {
                if (complexString.equals(entry.getKey().toString())) {
                    assertEquals(expected[1], entry.getValue());
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    private static void checkInitialLocatedValues(Map<LocatedComplex, Integer> quantityMap, Object[][] expectedQuantities) {
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

    private void checkObservables(Location location, Observable[] expectedObservables) {
        String[] expectedStrings = new String[expectedObservables.length];
        for (int index = 0; index < expectedObservables.length; index++) {
            expectedStrings[index] = new LocatedObservable(expectedObservables[index], location).toString();
        }
        checkObservables(model, expectedStrings);
    }

    public static void checkObservables(KappaModel kappaModel, String[] expectedObservables) {
        checkListByString(kappaModel.getLocatedObservables(), expectedObservables);
    }

    private static void checkListByString(List<? extends Object> objects, String[] expectedObservables) {
        assertEquals(expectedObservables.length, objects.size());

        Set<String> actual = new HashSet<String>();
        for (Object object : objects) {
            actual.add(object.toString());
        }

        for (int index = 0; index < expectedObservables.length; index++) {
            assertTrue(actual.contains(expectedObservables[index]));
            actual.remove(expectedObservables[index]);
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
    
    private static List<Agent> getAgents(Agent... agents) {
        return Arrays.asList(agents);
    }

}
