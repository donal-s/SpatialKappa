package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_Y;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.KappaModel.MappingInstance;
import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.demonsoft.spatialkappa.model.VariableExpression.SimulationToken;
import org.junit.Test;

public class KappaModelTest {

	// TODO test name conflicts

	// TODO test channel/location conflicts
	
    private KappaModel model = new KappaModel();



    @Test
    public void testAddInitialValue_concreteValue() {
        List<Agent> agents = Utils.getList(new Agent("agent1"));
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

        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addAgentDeclaration(new AggregateAgent("agent2"));
        model.addAgentDeclaration(new AggregateAgent("agent3"));
        model.addAgentDeclaration(new AggregateAgent("agent4"));
        model.addInitialValue(agents, "3", NOT_LOCATED);

        agents = Utils.getList(new Agent("agent1", new AgentSite("x", null, "1")), 
                new Agent("agent2", new AgentSite("x", null, "1")), 
                new Agent("agent3", new AgentSite("x", null, "2")), 
                new Agent("agent4", new AgentSite("x", null, "2")));
        model.addInitialValue(agents, "5", location);
        
        // Check canonicalisation of initial value complexes
        agents = Utils.getList(new Agent("agent3", new AgentSite("x", null, "7")), 
                new Agent("agent4", new AgentSite("x", null, "7")));
        model.addInitialValue(agents, "7", otherLocation);

        Complex expectedComplex1 = new Complex(new Agent("agent1"));
        Complex expectedComplex2 = new Complex(new Agent("agent1", location, new AgentSite("x", null, "1")), new Agent("agent2", location, new AgentSite("x", null, "1")));
        Complex expectedComplex3 = new Complex(new Agent("agent3", location, new AgentSite("x", null, "2")), new Agent("agent4", location, new AgentSite("x", null, "2")));
        Complex expectedComplex4 = new Complex(new Agent("agent3", otherLocation, new AgentSite("x", null, "7")), new Agent("agent4", otherLocation, new AgentSite("x", null, "7")));

        checkFixedLocatedInitialValues(new Object[][] { 
                { expectedComplex1, 3 }, 
                { expectedComplex2, 5 }, 
                { expectedComplex3, 5 }, 
                { expectedComplex4, 7 } });
        checkAggregateAgents(new AggregateAgent("agent1", new AggregateSite("x", null, "1")), 
                new AggregateAgent("agent2", new AggregateSite("x", null, "1")),
                new AggregateAgent("agent3", new AggregateSite("x", null, new String[] {"2", "7"})), 
                new AggregateAgent("agent4", new AggregateSite("x", null, new String[] {"2", "7"})));
    }
    
    @Test
    public void testAddInitialValue_propogateLocationsToAgents() {
        List<Agent> agents = Utils.getList(new Agent("agent1"));
        Location location = new Location("cytosol", new CellIndexExpression("2"));
        Location otherLocation = new Location("nucleus", new CellIndexExpression("2"));

        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addAgentDeclaration(new AggregateAgent("agent2"));
        model.addAgentDeclaration(new AggregateAgent("agent3"));
        model.addAgentDeclaration(new AggregateAgent("agent4"));
        model.addInitialValue(agents, "3", location);

        agents = Utils.getList(new Agent("agent1", new AgentSite("x", null, "1")), 
                new Agent("agent2", otherLocation, new AgentSite("x", null, "1")), 
                new Agent("agent3", new AgentSite("x", null, "2")), 
                new Agent("agent4", location, new AgentSite("x", null, "2")));
        model.addInitialValue(agents, "5", location);
        
        Complex expectedComplex1 = new Complex(new Agent("agent1", location));
        Complex expectedComplex2 = new Complex(new Agent("agent1", location, new AgentSite("x", null, "1")), new Agent("agent2", otherLocation, new AgentSite("x", null, "1")));
        Complex expectedComplex3 = new Complex(new Agent("agent3", location, new AgentSite("x", null, "2")), new Agent("agent4", location, new AgentSite("x", null, "2")));

        checkFixedLocatedInitialValues(new Object[][] { 
                { expectedComplex1, 3 }, 
                { expectedComplex2, 5 }, 
                { expectedComplex3, 5 } });
        checkAggregateAgents(new AggregateAgent("agent1", new AggregateSite("x", null, "1")), 
                new AggregateAgent("agent2", new AggregateSite("x", null, "1")),
                new AggregateAgent("agent3", new AggregateSite("x", null, new String[] {"2"})), 
                new AggregateAgent("agent4", new AggregateSite("x", null, new String[] {"2"})));
    }
    
    @Test
    public void testAddInitialValue_reference() {
        List<Agent> agents = Utils.getList(new Agent("agent1"));
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

        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addAgentDeclaration(new AggregateAgent("agent2"));
        model.addAgentDeclaration(new AggregateAgent("agent3"));
        model.addAgentDeclaration(new AggregateAgent("agent4"));
        model.addInitialValue(agents, new VariableReference("variable 3"), location);

        agents = Utils.getList(new Agent("agent1", new AgentSite("x", null, "1")), 
                new Agent("agent2", new AgentSite("x", null, "1")), 
                new Agent("agent3", new AgentSite("x", null, "2")), 
                new Agent("agent4", new AgentSite("x", null, "2")));
        model.addInitialValue(agents, new VariableReference("variable 5"), location);
        
        // Check canonicalisation of initial value complexes
        agents = Utils.getList(new Agent("agent3", new AgentSite("x", null, "7")), 
                new Agent("agent4", new AgentSite("x", null, "7")));
        model.addInitialValue(agents, "7", otherLocation);

        Complex expectedComplex1 = new Complex(new Agent("agent1", location));
        Complex expectedComplex2 = new Complex(new Agent("agent1", location, new AgentSite("x", null, "1")), new Agent("agent2", location, new AgentSite("x", null, "1")));
        Complex expectedComplex3 = new Complex(new Agent("agent3", location, new AgentSite("x", null, "2")), new Agent("agent4", location, new AgentSite("x", null, "2")));
        Complex expectedComplex4 = new Complex(new Agent("agent3", otherLocation, new AgentSite("x", null, "7")), new Agent("agent4", otherLocation, new AgentSite("x", null, "7")));

        checkFixedLocatedInitialValues(new Object[][] { 
                { expectedComplex1, 3 }, 
                { expectedComplex2, 5 }, 
                { expectedComplex3, 5 }, 
                { expectedComplex4, 7 } });
        checkAggregateAgents(new AggregateAgent("agent1", new AggregateSite("x", null, "1")), 
                new AggregateAgent("agent2", new AggregateSite("x", null, "1")),
                new AggregateAgent("agent3", new AggregateSite("x", null, new String[] {"2", "7"})), 
                new AggregateAgent("agent4", new AggregateSite("x", null, new String[] {"2", "7"})));
    }
    
    @Test
    public void testGetFixedLocatedInitialValuesMap() {
        Map<Complex, Integer> actual = model.getFixedLocatedInitialValuesMap();
        assertEquals(0, actual.size());
        
        // No compartments
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addAgentDeclaration(new AggregateAgent("agent2"));
        model.addInitialValue(Utils.getList(new Agent("agent1")), "3", NOT_LOCATED);

        checkFixedLocatedInitialValues(new Object[][] { { new Complex(new Agent("agent1")), 3 } });
        
        // add compartment
        model.addCompartment(new Compartment("cytosol", 3));
        

        model.addInitialValue(Utils.getList(new Agent("agent2")), "5", new Location("cytosol"));
        model.addInitialValue(Utils.getList(new Agent("agent2")), "10", new Location("cytosol", INDEX_1));

        checkFixedLocatedInitialValues(new Object[][] { 
                { new Complex(new Agent("agent1", new Location("cytosol", INDEX_0))), 1 }, 
                { new Complex(new Agent("agent1", new Location("cytosol", INDEX_1))), 1 }, 
                { new Complex(new Agent("agent1", new Location("cytosol", INDEX_2))), 1 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_0))), 2 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_1))), 12 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_2))), 1 }, 
                });
    }
    
    @Test
    public void testGetFixedLocatedInitialValuesMap_additionOfSiteDefaults() {
        model.addAgentDeclaration(new AggregateAgent("agent1", 
                new AggregateSite("s", (String) null, null), 
                new AggregateSite("t", "x", null),
                new AggregateSite("u", getList("y", "z"), null)));
        model.addAgentDeclaration(new AggregateAgent("agent2", 
                new AggregateSite("v", getList("a", "b"), null)));
        
        // No compartments
        model.addInitialValue(Utils.getList(new Agent("agent1")), "3", NOT_LOCATED);

        checkFixedLocatedInitialValues(new Object[][] { { new Complex(new Agent("agent1", 
                new AgentSite("s", null, null), new AgentSite("t", "x", null), new AgentSite("u", "y", null))), 3 } });
        
        // add compartment
        model.addCompartment(new Compartment("cytosol", 3));
        

        model.addInitialValue(Utils.getList(new Agent("agent2")), "5", new Location("cytosol"));
        model.addInitialValue(Utils.getList(new Agent("agent2", new AgentSite("v", "b", null))), "10", new Location("cytosol", INDEX_1));

        checkFixedLocatedInitialValues(new Object[][] { 
                { new Complex(new Agent("agent1", new Location("cytosol", INDEX_0), 
                        new AgentSite("s", null, null), new AgentSite("t", "x", null), new AgentSite("u", "y", null))), 1 }, 
                { new Complex(new Agent("agent1", new Location("cytosol", INDEX_1),
                        new AgentSite("s", null, null), new AgentSite("t", "x", null), new AgentSite("u", "y", null))), 1 }, 
                { new Complex(new Agent("agent1", new Location("cytosol", INDEX_2),
                        new AgentSite("s", null, null), new AgentSite("t", "x", null), new AgentSite("u", "y", null))), 1 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_0), new AgentSite("v", "a", null))), 2 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_1), new AgentSite("v", "b", null))), 10 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_1), new AgentSite("v", "a", null))), 2 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_2), new AgentSite("v", "a", null))), 1 }, 
                });
    }

//    @Test
//    public void testGetFixedLocatedTransitions_transforms() {
//        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
//        assertEquals(0, actual.size());
//        
//        // No compartments
//        List<Agent> leftAgents = getList(new Agent("agent1"));
//        List<Agent> rightAgents = getList(new Agent("agent2"));
//        model.addTransform("label", leftAgents, rightAgents, new VariableExpression(0.1f), NOT_LOCATED);
//        Transform transform1 = new Transform("label", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.1f);
//
//        checkLocatedTransitions(new LocatedTransform[] { 
//                new LocatedTransform(transform1, NOT_LOCATED)
//                });
//        
//        // add compartment
//        model = new KappaModel();
//        model.addCompartment(new Compartment("cytosol", 3));
//        
//        leftAgents = getList(new Agent("agent3"));
//        rightAgents = getList(new Agent("agent4"));
//        model.addTransform("label2", leftAgents, rightAgents, new VariableExpression(0.2f), new Location("cytosol"));
//        Transform transform2 = new Transform("label2", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.2f);
//
//        leftAgents = getList(new Agent("agent5"));
//        rightAgents = getList(new Agent("agent6"));
//        model.addTransform("label3", leftAgents, rightAgents, new VariableExpression(0.3f), new Location("cytosol", new CellIndexExpression("1")));
//        Transform transform3 = new Transform("label3", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.3f);
//
//        checkLocatedTransitions(new LocatedTransform[] { 
//                new LocatedTransform(transform2, new Location("cytosol", new CellIndexExpression("0"))),
//                new LocatedTransform(transform2, new Location("cytosol", new CellIndexExpression("1"))),
//                new LocatedTransform(transform2, new Location("cytosol", new CellIndexExpression("2"))),
//                new LocatedTransform(transform3, new Location("cytosol", new CellIndexExpression("1"))),
//        });
//    }
//
//    @Test
//    public void testGetFixedLocatedTransitions_transforms_noCompartmentSpecified() {
//        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
//        assertEquals(0, actual.size());
//        
//        // No compartments
//        List<Agent> leftAgents = getList(new Agent("agent1"));
//        List<Agent> rightAgents = getList(new Agent("agent2"));
//        model.addTransform("label", leftAgents, rightAgents, new VariableExpression(0.1f), NOT_LOCATED);
//        Transform transform = new Transform("label", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.1f);
//
//        checkLocatedTransitions(new LocatedTransform[] { 
//                new LocatedTransform(transform, NOT_LOCATED)
//                });
//        
//        // add compartment
//        model.addCompartment(new Compartment("cytosol", 3));
//        model.addCompartment(new Compartment("membrane", 2));
//        
//        checkLocatedTransitions(new LocatedTransform[] { 
//                new LocatedTransform(transform, new Location("cytosol", new CellIndexExpression("0"))),
//                new LocatedTransform(transform, new Location("cytosol", new CellIndexExpression("1"))),
//                new LocatedTransform(transform, new Location("cytosol", new CellIndexExpression("2"))),
//                new LocatedTransform(transform, new Location("membrane", new CellIndexExpression("0"))),
//                new LocatedTransform(transform, new Location("membrane", new CellIndexExpression("1"))),
//        });
//    }

//    @Test
//    public void testGetConcreteLocatedTransitions_transports() {
//        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
//        assertEquals(0, actual.size());
//        
//        model.addCompartment(new Compartment("cytosol", 3));
//        model.addChannel(new Channel("intra", getList(
//                new Location[] {
//                        new Location("cytosol", INDEX_X),
//                        new Location("cytosol", INDEX_X_PLUS_1),
//                }, 
//                new Location[] {
//                        new Location("cytosol", INDEX_X),
//                        new Location("cytosol", INDEX_X_MINUS_1),
//                }, 
//                new Location[] {
//                        new Location("cytosol", INDEX_2), 
//                        new Location("cytosol", INDEX_0),
//                },
//                new Location[] {
//                        new Location("cytosol", INDEX_0), 
//                        new Location("cytosol", INDEX_2),
//                }
//                )));
//        
//        List<Agent> agents = getList(new Agent("agent1"));
//        model.addTransport("label", "intra", agents, new VariableExpression(0.2f));
//        Transport transport = new Transport("label",  "intra", agents, 0.2f);
//
//        Location cell0 = new Location("cytosol", INDEX_0);
//        Location cell1 = new Location("cytosol", INDEX_1);
//        Location cell2 = new Location("cytosol", INDEX_2);
//        
//        checkLocatedTransitions(new LocatedTransport[] { 
//                new LocatedTransport(transport, cell0, cell1),
//                new LocatedTransport(transport, cell1, cell0),
//                new LocatedTransport(transport, cell1, cell2),
//                new LocatedTransport(transport, cell2, cell1),
//                new LocatedTransport(transport, cell0, cell2),
//                new LocatedTransport(transport, cell2, cell0),
//        });
//    }

//    @Test
//    public void testGetConcreteLocatedTransitions_transports_multipleLinksSameLabel() {
//        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
//        assertEquals(0, actual.size());
//        
//        model.addCompartment(new Compartment("cytosol", 3));
//        model.addChannel(new Channel("intra", getList(
//                new Location[] {
//                        new Location("cytosol", INDEX_0), 
//                        new Location("cytosol", INDEX_1)
//                },
//                new Location[] {
//                        new Location("cytosol", INDEX_1), 
//                        new Location("cytosol", INDEX_2),
//                },
//                new Location[] {
//                        new Location("cytosol", INDEX_1), 
//                        new Location("cytosol", INDEX_0)
//                },
//                new Location[] {
//                        new Location("cytosol", INDEX_2), 
//                        new Location("cytosol", INDEX_1),
//                }
//                )));
//        
//        List<Agent> agents = getList(new Agent("agent1"));
//        model.addTransport("label", "intra", agents, new VariableExpression(0.2f));
//        Transport transport = new Transport("label",  "intra", agents, 0.2f);
//
//        Location cell0 = new Location("cytosol", INDEX_0);
//        Location cell1 = new Location("cytosol", INDEX_1);
//        Location cell2 = new Location("cytosol", INDEX_2);
//        
//        checkLocatedTransitions(new LocatedTransport[] { 
//                new LocatedTransport(transport, cell0, cell1),
//                new LocatedTransport(transport, cell1, cell0),
//                new LocatedTransport(transport, cell1, cell2),
//                new LocatedTransport(transport, cell2, cell1),
//        });
//    }

//    private void checkLocatedTransitions(Transition[] expecteds) {
//        List<String> actuals = new ArrayList<String>();
//        for (Transition transition : model.getFixedLocatedTransitions()) {
//            actuals.add(transition.toString());
//        }
//        assertEquals(expecteds.length, actuals.size());
//        
//        for (Transition expected : expecteds) {
//            String expectedString = expected.toString();
//            if (!actuals.contains(expectedString)) {
//                fail("Not found: " + expected);
//            }
//            actuals.remove(expectedString);
//        }
//    }

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

        model.addVariable(agents1, "label", NOT_LOCATED);

        checkVariables(model, "'label' ([agent1])");
        checkOrderedVariableNames(model, "label");
        checkAggregateAgents(new AggregateAgent("agent1"));

        List<Agent> agents2 = new ArrayList<Agent>();
        agents2.add(new Agent("agent1"));
        agents2.add(new Agent("agent2"));
        model.addVariable(agents2, "label2", NOT_LOCATED);

        checkVariables(model, "'label' ([agent1])", "'label2' ([agent1, agent2])");
        checkOrderedVariableNames(model, "label", "label2");
        checkAggregateAgents(new AggregateAgent("agent1"), new AggregateAgent("agent2"));
    }


    @Test
    public void testAddVariable_orderingOfNames() {
        List<Agent> agents1 = new ArrayList<Agent>();
        agents1.add(new Agent("agent1"));

        model.addVariable(agents1, "C", NOT_LOCATED);

        List<Agent> agents2 = new ArrayList<Agent>();
        agents2.add(new Agent("agent1"));
        agents2.add(new Agent("agent2"));
        model.addVariable(agents2, "A", NOT_LOCATED);

        model.addVariable(new VariableExpression(100f), "B");

        checkVariables(model, "'A' ([agent1, agent2])", "'B' (100.0)", "'C' ([agent1])");
        checkOrderedVariableNames(model, "C", "A", "B");
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

        checkVariables(model, "'label' cytosol[2] ([agent1:cytosol[2]])");
        checkOrderedVariableNames(model, "label");
        checkAggregateAgents(new AggregateAgent("agent1"));

        List<Agent> agents2 = new ArrayList<Agent>();
        agents2.add(new Agent("agent1", new Location("cytosol", INDEX_0)));
        agents2.add(new Agent("agent2"));
        model.addVariable(agents2, "label2", location);

        checkVariables(model, "'label' cytosol[2] ([agent1:cytosol[2]])", 
                "'label2' cytosol[2] ([agent1:cytosol[0], agent2:cytosol[2]])");
        checkOrderedVariableNames(model, "label", "label2");
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
        assertTrue(model.getPlottedVariables().containsAll(Utils.getList("label", "label2")));
    }

    @Test
    public void testAddAgentDeclaration() {
        try {
            model.addAgentDeclaration(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        assertNotNull(model.getAgentDeclarationMap());
        assertEquals(0, model.getAgentDeclarationMap().size());

        AggregateAgent agent1 = new AggregateAgent("name1");
        AggregateAgent agent2 = new AggregateAgent("name2");
        model.addAgentDeclaration(agent1);
        model.addAgentDeclaration(agent2);
        assertEquals(2, model.getAgentDeclarationMap().size());
        assertEquals(agent1, model.getAgentDeclarationMap().get("name1"));
        assertEquals(agent2, model.getAgentDeclarationMap().get("name2"));
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
    public void testAddChannel() {
        try {
            model.addChannel(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        assertNotNull(model.getChannels());
        assertEquals(0, model.getChannels().size());

        Channel link1 = new Channel("name1", new Location("1"), new Location("2"));
        Channel link2 = new Channel("name2", new Location("1"), new Location("3"));
        model.addChannel(link1);
        model.addChannel(link2);
        assertEquals(2, model.getChannels().size());
        assertEquals(link1, model.getChannels().get(0));
        assertEquals(link2, model.getChannels().get(1));
    }


    private void checkAggregateAgents(AggregateAgent... expectedAgents) {
        assertEquals(expectedAgents.length, model.getAggregateAgentMap().size());

        for (AggregateAgent expectedAgent : expectedAgents) {
            AggregateAgent agent = model.getAggregateAgentMap().get(expectedAgent.getName());
            assertNotNull(agent);
            assertEquals(expectedAgent.getName(), agent.getName());
            assertEquals(expectedAgent.getSites().size(), agent.getSites().size());
            assertTrue("Sites missing from: " + expectedAgent.getSites(), agent.getSites().containsAll(expectedAgent.getSites()));
        }
    }

    private void checkFixedLocatedInitialValues(Object[][] expectedQuantities) {
        Map<Complex, Integer> quantityMap = model.getFixedLocatedInitialValuesMap();
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
            assertTrue("Complex not found: " + complexString, found);
        }
        
        // Check canonicalisation
        ComplexMatcher matcher = new ComplexMatcher();
        Set<Complex> complexes = new HashSet<Complex>();
        for (Complex current : quantityMap.keySet()) {
            boolean found = false;
            for (Complex complex : complexes) {
                if (matcher.isExactMatch(complex, current)) {
                    assertSame(complex, current);
                    found = true;
                    break;
                }
            }
            if (!found) {
                complexes.add(current);
            }
        }
    }

    private void checkVariables(KappaModel kappaModel, String... expectedVariables) {
        checkListByString(kappaModel.getVariables().values(), expectedVariables);
    }

    private void checkOrderedVariableNames(KappaModel kappaModel, String... expectedVariableNames) {
        checkListByString(kappaModel.getOrderedVariableNames(), expectedVariableNames);
    }

    private void checkListByString(Collection<? extends Object> objects, String[] expected) {
        assertEquals(expected.length, objects.size());

        Set<String> actual = new HashSet<String>();
        for (Object object : objects) {
            actual.add(object.toString());
        }

        for (int index = 0; index < expected.length; index++) {
            if (!actual.contains(expected[index])) {
                fail("Item not found: " + expected[index] + "\nin:" + actual);
            }
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
        
        // Plot reference to transition
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addCompartment(new Compartment("known"));
        model.addChannel(new Channel("channel", new Location("known"), new Location("known")));
        model.addTransition("transitionRef", null, null, "channel", null, Utils.getList(new Agent("agent1")), new VariableExpression(2f));
        model.addPlot("transitionRef");
        model.validate();
        
        // Plot reference to kappa expression
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addVariable(Utils.getList(new Agent("agent1")), "kappaRef", NOT_LOCATED);
        model.addPlot("kappaRef");
        model.validate();
    }
    
    @Test
    public void testValidate_initialValue() {
        // Missing initial value reference
        model = new KappaModel();
        model.addInitialValue(Utils.getList(new Agent("agent1")), new VariableReference("unknown"), NOT_LOCATED);
        checkValidate_failure("Reference 'unknown' not found");
        
        // Non fixed initial value reference
        model = new KappaModel();
        model.addVariable(new VariableExpression(SimulationToken.EVENTS), "variableRef");
        model.addInitialValue(Utils.getList(new Agent("agent1")), new VariableReference("variableRef"), NOT_LOCATED);
        checkValidate_failure("Reference 'variableRef' not fixed - cannot be initial value");
        
        // Infinite initial value reference
        model = new KappaModel();
        model.addVariable(new VariableExpression(Constant.INFINITY), "variableRef");
        model.addInitialValue(Utils.getList(new Agent("agent1")), new VariableReference("variableRef"), NOT_LOCATED);
        checkValidate_failure("Reference 'variableRef' evaluates to infinity - cannot be initial value");
        
        // Initial value reference to variable
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addVariable(new VariableExpression(2f), "variableRef");
        model.addInitialValue(Utils.getList(new Agent("agent1")), new VariableReference("variableRef"), NOT_LOCATED);
        model.validate();
        
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addVariable(new VariableExpression(2f), "variableRef");
        model.addVariable(new VariableExpression(new VariableReference("variableRef")), "other");
        model.addInitialValue(Utils.getList(new Agent("agent1")), new VariableReference("other"), NOT_LOCATED);
        model.validate();
        
        // Initial value concrete
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addInitialValue(Utils.getList(new Agent("agent1")), "1000", NOT_LOCATED);
        model.validate();
    }
    
    @Test
    public void testValidate_transport() {
        // Missing transport reference
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addChannel(new Channel("channel", new Location("known"), new Location("known")));
        model.addTransition("transportRef", null, Utils.getList(new Agent("agent1")), "channel", null, 
                Utils.getList(new Agent("agent1")), new VariableExpression(new VariableReference("unknown")));
        checkValidate_failure("Reference 'unknown' not found");
        
        model = new KappaModel();
        model.addVariable(new VariableExpression(2), "variableRef");
        model.addTransition("transportRef", null, Utils.getList(new Agent("agent1")), "channel", null, 
                Utils.getList(new Agent("agent1")), new VariableExpression(new VariableReference("variableRef")));
        checkValidate_failure("Channel 'channel' not found");
        
        // Non fixed transport reference
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addChannel(new Channel("channel", new Location("known"), new Location("known")));
        model.addVariable(new VariableExpression(SimulationToken.EVENTS), "variableRef");
        model.addTransition("other", null, Utils.getList(new Agent("agent1")), "channel", null, 
                Utils.getList(new Agent("agent1")), new VariableExpression(new VariableReference("variableRef")));
        checkValidate_failure("Reference 'variableRef' not fixed");
        
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addCompartment(new Compartment("known"));
        model.addChannel(new Channel("channel", new Location("known"), new Location("known")));
        model.addTransition("transportRef", null, Utils.getList(new Agent("agent1")), "channel", null, 
                Utils.getList(new Agent("agent1")), new VariableExpression(2));
        model.validate();

        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addCompartment(new Compartment("known"));
        model.addChannel(new Channel("channel", new Location("known"), new Location("known")));
        model.addTransition("transportRef", null, Utils.getList(new Agent("agent1")), "channel", null, 
                Utils.getList(new Agent("agent1")), new VariableExpression(Constant.INFINITY));
        model.validate();
        
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addCompartment(new Compartment("known"));
        model.addChannel(new Channel("channel", new Location("known"), new Location("known")));
        model.addVariable(new VariableExpression(2), "variableRef");
        model.addTransition("other", null, Utils.getList(new Agent("agent1")), "channel", null, 
                Utils.getList(new Agent("agent1")), new VariableExpression(new VariableReference("variableRef")));
        model.validate();
        
    }
    
    @Test
    public void testValidate_transform() {
        // Missing transform reference
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addTransition(null, NOT_LOCATED, Utils.getList(new Agent("agent1")), null, null, null,
                new VariableExpression(new VariableReference("unknown")));
        checkValidate_failure("Reference 'unknown' not found");
        
        // Non fixed transform reference
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addVariable(new VariableExpression(SimulationToken.EVENTS), "variableRef");
        model.addTransition(null, NOT_LOCATED, Utils.getList(new Agent("agent1")), null, null, null, 
                new VariableExpression(new VariableReference("variableRef")));
        checkValidate_failure("Reference 'variableRef' not fixed");
        
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addTransition(null, NOT_LOCATED, Utils.getList(new Agent("agent1")), null, null, null, 
                new VariableExpression(Constant.INFINITY));
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
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addVariable(Utils.getList(new Agent("agent1")), "kappaRef", NOT_LOCATED);
        model.addVariable(new VariableExpression(new VariableReference("kappaRef")), "label");
        model.validate();

    }
    
    @Test
    public void testValidate_channel() {
        // Test channel.validate() is called
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addChannel(new Channel("name", new Location("known"), new Location("unknown")));
        checkValidate_failure("Compartment 'unknown' not found");
        
        // Test one one channel per name is used
        model = new KappaModel();
        model.addCompartment(new Compartment("known"));
        model.addChannel(new Channel("name", new Location("known"), new Location("known")));
        model.addChannel(new Channel("name", new Location("known"), new Location("known")));
        checkValidate_failure("Duplicate channel 'name'");
    }
    
    @Test
    public void testValidate_perturbation() {
        
        /*
         *     
    public void addPerturbation(Perturbation perturbation); // TODO

         * 
         */
    }
    
    @Test
    public void testValidate_agentDeclaration() {
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("A"));
        model.addAgentDeclaration(new AggregateAgent("B", new AggregateSite("site1", "x", null)));
        model.validate();
    }
    
    @Test
    public void testValidate_agentDeclarationInvalid() {
        model = new KappaModel();
        model.addVariable(Utils.getList(new Agent("A")), "test", NOT_LOCATED);
        checkValidate_failure("Agent 'A' not declared");

        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("A", new AggregateSite("site2", (String) null, null)));
        model.addVariable(Utils.getList(new Agent("A", new AgentSite("site1", null, null))), "test", NOT_LOCATED);
        checkValidate_failure("Agent site A(site1) not declared");
        
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("A", new AggregateSite("site1", "x", null)));
        model.addVariable(Utils.getList(new Agent("A", new AgentSite("site1", "y", null))), "test", NOT_LOCATED);
        checkValidate_failure("Agent state A(site1~y) not declared");
        
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

    
    @Test
    public void testChooseNextAgent() throws Exception {
        
        Agent agent1 = new Agent("agent1", new AgentSite("s1", null, "1"), new AgentSite("s2", null, "_"));
        Agent agent2 = new Agent("agent2", new AgentSite("s3", null, "1"), new AgentSite("s4", null, "2"));
        Agent agent3 = new Agent("agent3", new AgentSite("s5", null, "2"), new AgentSite("s6", null, "?"));
        Agent agent4 = new Agent("agent4");
        
        List<Agent> remainingAgents = new ArrayList<Agent>();
        remainingAgents.add(agent1);
        remainingAgents.add(agent2);
        remainingAgents.add(agent3);
        remainingAgents.add(agent4);
        
        Complex complex = new Complex(remainingAgents);
        
        List<Agent> fixedAgents = new ArrayList<Agent>();
        fixedAgents.add(agent1);
        remainingAgents.remove(agent1);
        
        List<AgentLink> remainingLinks = new ArrayList<AgentLink>(complex.agentLinks);
        remainingLinks.removeAll(model.getInternalLinks(remainingLinks, fixedAgents));
        
        try {
            model.chooseNextAgent(null, remainingAgents, remainingLinks);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.chooseNextAgent(fixedAgents, null, remainingLinks);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.chooseNextAgent(fixedAgents, remainingAgents, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.chooseNextAgent(new ArrayList<Agent>(), remainingAgents, remainingLinks);
            fail("no linked agent to choose should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            model.chooseNextAgent(fixedAgents, new ArrayList<Agent>(), remainingLinks);
            fail("no linked agent to choose should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            model.chooseNextAgent(fixedAgents, remainingAgents, new ArrayList<AgentLink>());
            fail("no linked agent to choose should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        assertEquals(agent2, model.chooseNextAgent(fixedAgents, remainingAgents, remainingLinks));

        fixedAgents.add(agent2);
        remainingAgents.remove(agent2);
        remainingLinks.removeAll(model.getInternalLinks(remainingLinks, fixedAgents));
        
        assertEquals(agent3, model.chooseNextAgent(fixedAgents, remainingAgents, remainingLinks));
    }
    
    
    @Test
    public void testGetFixedAgents() throws Exception {
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("loc1"));
        compartments.add(new Compartment("loc2", 5));
        
        Agent agent1 = new Agent("agent1");
        Agent agent2 = new Agent("agent2", new Location("loc1"));
        Agent agent3 = new Agent("agent3", new Location("loc2"));
        Agent agent4 = new Agent("agent4", new Location("loc2", INDEX_0));
        Agent agent5 = new Agent("agent5", new Location("loc2", INDEX_X));
        Agent agent6 = new Agent("agent6", new Location("loc2", INDEX_X_PLUS_1));
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(agent1);
        agents.add(agent2);
        agents.add(agent3);
        agents.add(agent4);
        agents.add(agent5);
        agents.add(agent6);
        Complex complex = new Complex(agents);
        
        try {
            model.getFixedAgents(null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.getFixedAgents(complex, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Agent> expected = new ArrayList<Agent>();
        expected.add(agent2);
        expected.add(agent4);
        assertEquals(expected, model.getFixedAgents(complex, compartments));
    }
    
    @Test
    public void testGetInternalLinks() throws Exception {
        
        Agent agent1 = new Agent("agent1", new AgentSite("s1", null, "1"), new AgentSite("s2", null, "_"));
        Agent agent2 = new Agent("agent2", new AgentSite("s3", null, "1"), new AgentSite("s4", null, "2"));
        Agent agent3 = new Agent("agent3", new AgentSite("s5", null, "2"), new AgentSite("s6", null, "?"));
        Agent agent4 = new Agent("agent4");
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(agent1);
        agents.add(agent2);
        agents.add(agent3);
        agents.add(agent4);
        Complex complex = new Complex(agents);
        
        try {
            model.getInternalLinks(null, agents);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.getInternalLinks(complex.agentLinks, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Agent> input = new ArrayList<Agent>();
        
        List<AgentLink> expected = new ArrayList<AgentLink>();
        assertEquals(expected, model.getInternalLinks(complex.agentLinks, input));
        
        input.add(agent1);
        expected.add(complex.getAgentLink(agent1, "s2"));
        assertEquals(expected, model.getInternalLinks(complex.agentLinks, input));
        
        input.add(agent2);
        expected.add(complex.getAgentLink(agent1, "s1"));
        assertEquals(expected, model.getInternalLinks(complex.agentLinks, input));
    }
    
    
    @Test
    public void testGetPossibleLocations() throws Exception {
        
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("loc1"));
        compartments.add(new Compartment("loc2", 5));
        
        try {
            model.getPossibleLocations(null, NOT_LOCATED, null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.getPossibleLocations(NOT_LOCATED, null, null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, null, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // No location or channel - stay in same location
        List<Location> expected = new ArrayList<Location>();
        expected.add(NOT_LOCATED);
        assertEquals(expected, model.getPossibleLocations(NOT_LOCATED, NOT_LOCATED, null, compartments));
        
        // No channel - stay in same location
        expected = new ArrayList<Location>();
        expected.add(new Location("loc1"));
        assertEquals(expected, model.getPossibleLocations(new Location("loc1"), NOT_LOCATED, null, compartments));

        expected.clear();
        expected.add(new Location("loc2", INDEX_1));
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, null, compartments));
        
        // No channel, but now constraints
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_1), null, compartments));
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2"), null, compartments));
        
        expected.clear();
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_2), null, compartments));
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc1"), null, compartments));

        // Matching channel - no constraints
        Channel channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc2", INDEX_2));
        expected.clear();
        expected.add(new Location("loc2", INDEX_2));
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, channel, compartments));
        
        channel = new Channel("label", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1));
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, channel, compartments));
        
        channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc1"));
        expected.clear();
        expected.add(new Location("loc1"));
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, channel, compartments));
        
        // Matching channel - constraints
        channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc2", INDEX_2));
        expected.clear();
        expected.add(new Location("loc2", INDEX_2));
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_2), channel, compartments));
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2"), channel, compartments));
        
        expected.clear();
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_1), channel, compartments));
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc1"), channel, compartments));
        
        // Non matching channel
        channel = new Channel("label", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1));
        expected.clear();
        assertEquals(expected, model.getPossibleLocations(new Location("loc2", new CellIndexExpression("4")), NOT_LOCATED, channel, compartments));

        assertEquals(expected, model.getPossibleLocations(new Location("loc1"), NOT_LOCATED, channel, compartments));
    }
    
    @Test
    public void testGetMergedLocation() {
        
        try {
            model.getMergedLocation(null, new Location("A"));
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.getMergedLocation(new Location("A"), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.getMergedLocation(new Location("A"), new Location("B"));
            fail("mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            model.getMergedLocation(new Location("A", INDEX_1), new Location("A", INDEX_2));
            fail("mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        assertEquals(NOT_LOCATED, 
                model.getMergedLocation(NOT_LOCATED, NOT_LOCATED));
        assertEquals(new Location("A", INDEX_1), 
                model.getMergedLocation(NOT_LOCATED, new Location("A", INDEX_1)));
        assertEquals(new Location("A", INDEX_1), 
                model.getMergedLocation(new Location("A", INDEX_1), NOT_LOCATED));
        assertEquals(new Location("A", INDEX_1), 
                model.getMergedLocation(new Location("A"), new Location("A", INDEX_1)));
        assertEquals(new Location("A", INDEX_1), 
                model.getMergedLocation(new Location("A"), new Location("A", INDEX_1)));
        assertEquals(new Location("A"), 
                model.getMergedLocation(new Location("A"), new Location("A")));
    }

    
    @Test
    public void testGetUnmergedAgents() {
        
        Agent templateAgent = new Agent("template", new Location("T"), new AgentSite("name", "state", "3", "channel"));
        Agent mergedAgent = new Agent("merged", new Location("M"));
        Agent locatedAgent = new Agent("located", new Location("L"));
        
        List<Agent> templateAgents = Utils.getList(templateAgent);
        Map<Agent, Agent> mergedLocatedMap = new HashMap<Agent, Agent>();
        Map<Agent, Agent> templateMergedMap = new HashMap<Agent, Agent>();
        
        mergedLocatedMap.put(mergedAgent, locatedAgent);
        templateMergedMap.put(templateAgent, mergedAgent);
        
        try {
            model.getUnmergedAgents(null, mergedLocatedMap, templateMergedMap);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.getUnmergedAgents(templateAgents, null, templateMergedMap);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.getUnmergedAgents(templateAgents, mergedLocatedMap, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        mergedLocatedMap.clear();
        
        try {
            model.getUnmergedAgents(templateAgents, mergedLocatedMap, templateMergedMap);
            fail("missing map entry should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        mergedLocatedMap.put(mergedAgent, locatedAgent);
        templateMergedMap.clear();
        
        try {
            model.getUnmergedAgents(templateAgents, mergedLocatedMap, templateMergedMap);
            fail("missing map entry should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        mergedLocatedMap.put(mergedAgent, locatedAgent);
        templateMergedMap.put(templateAgent, mergedAgent);
        
        List<Agent> expected = Utils.getList(new Agent("template", new Location("L"), new AgentSite("name", "state", "3", "channel")));
        assertEquals(expected.toString(), 
                model.getUnmergedAgents(templateAgents, mergedLocatedMap, templateMergedMap).toString());
    }
    
//    @Test
//    public void testGetValidLocatedTransforms() throws Exception {
//        
//        Transform transform = new Transform("label", 
//                getList(new Agent("A", new AgentSite("s", null, null))), 
//                getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1"))), 10f, false);
//        
//        List<Channel> channels = new ArrayList<Channel>();
//        channels.add(new Channel("intra", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1)));
//        channels.add(new Channel("inter", new Location("loc1"), new Location("loc2", INDEX_2)));
//        
//        List<Compartment> compartments = new ArrayList<Compartment>();
//        compartments.add(new Compartment("loc1"));
//        compartments.add(new Compartment("loc2", 3));
//        compartments.add(new Compartment("loc3", 3));
//        
//
//        try {
//            model.getValidLocatedTransforms(null, compartments, channels);
//            fail("null should have failed");
//        }
//        catch (NullPointerException ex) {
//            // Expected exception
//        }
//        
//        try {
//            model.getValidLocatedTransforms(transform, null, channels);
//            fail("null should have failed");
//        }
//        catch (NullPointerException ex) {
//            // Expected exception
//        }
//        
//        try {
//            model.getValidLocatedTransforms(transform, compartments, null);
//            fail("null should have failed");
//        }
//        catch (NullPointerException ex) {
//            // Expected exception
//        }
//        
//        // No location
//        
//        List<Transform> expected = new ArrayList<Transform>();
//        expected.add(new Transform("label", getList(new Agent("A", new AgentSite("s", null, null))), 
//                getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1"))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // Full location
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc1"))), 
//                getList(new Agent("A", new Location("loc1"))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label", 
//                getList(new Agent("A", new Location("loc1"))), 
//                getList(new Agent("A", new Location("loc1"))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // Single unlinked partial location
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2"))), 
//                getList(new Agent("A", new Location("loc2"))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label-1", 
//                getList(new Agent("A", new Location("loc2", INDEX_0))), 
//                getList(new Agent("A", new Location("loc2", INDEX_0))), 10f, false));
//        expected.add(new Transform("label-2", 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 10f, false));
//        expected.add(new Transform("label-3", 
//                getList(new Agent("A", new Location("loc2", INDEX_2))), 
//                getList(new Agent("A", new Location("loc2", INDEX_2))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // Multiple unlinked partial locations should fail
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2"))), 
//                getList(new Agent("B", new Location("loc3"))), 10f, false);
//
//        try {
//            model.getValidLocatedTransforms(transform, compartments, channels);
//            fail("invalid transform should have failed");
//        }
//        catch (IllegalArgumentException ex) {
//            // Expected exception
//        }
//        
//        // Dimer unlinking same full location, no channel
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // Dimer linked, same partial location, no channel
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, null))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label-1", 
//                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, null))), 10f, false));
//        expected.add(new Transform("label-2", 
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false));
//        expected.add(new Transform("label-3", 
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, null))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // No label
//        transform = new Transform(null, 
//                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, null))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform(null, 
//                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, null))), 10f, false));
//        expected.add(new Transform(null, 
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false));
//        expected.add(new Transform(null, 
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, null))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, null))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, null))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//
//        
//    }
    

    // TODO remember to rename
    @Test
    public void testInitMappingStructure() throws Exception {
        
        List<Agent> agents = Utils.getList(new Agent("A", new Location("loc1")));
        Complex complex = new Complex(agents);
        
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(new Channel("intra", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1)));
        channels.add(new Channel("inter", new Location("loc1"), new Location("loc2", INDEX_2)));
        
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("loc1"));
        compartments.add(new Compartment("loc2", 5));
        
        try {
            model.initMappingStructure(null, compartments, channels);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.initMappingStructure(complex, null, channels);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.initMappingStructure(complex, compartments, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // Simple agent - fixed location
        agents = Utils.getList(new Agent("A", new Location("loc1")));
        complex = new Complex(agents);
        
        List<List<Agent>> expected = new ArrayList<List<Agent>>();
        List<Agent> instanceAgents = Utils.getList(new Agent("A", new Location("loc1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);

        agents = Utils.getList(new Agent("A", new Location("loc2", INDEX_1)));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = Utils.getList(new Agent("A", new Location("loc2", INDEX_1)));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        

        // Simple agent - no location
        agents = Utils.getList(new Agent("A"));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = Utils.getList(new Agent("A"));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        
        
        // Simple agent - partial location

        agents = Utils.getList(new Agent("A", new Location("loc2")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_0))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_1))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_2))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", new CellIndexExpression("4")))));
               
        checkMappingStructure(complex, compartments, channels, expected);

        
        // Dimer - one agent fixed, other same compartment
        agents = Utils.getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc1"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = Utils.getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc1"), new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);

        agents = Utils.getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = Utils.getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        

        // Dimer - one agent no location, other same compartment
        agents = Utils.getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = Utils.getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        
        
        // Dimer - one agent partial location, other same compartment

        agents = Utils.getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1"))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1"))));
               
        checkMappingStructure(complex, compartments, channels, expected);

        // Dimer - one agent fixed, other using channel
        agents = Utils.getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1", "inter")),
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = Utils.getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1", "inter")),
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);

        agents = Utils.getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1", "intra")),
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = Utils.getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1", "intra")),
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        

        // Dimer - one agent partial location, other using channel

        agents = Utils.getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1"))));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1"))));
               
        checkMappingStructure(complex, compartments, channels, expected);

        // Dimer - one agent partial location, other using channel, 2 dimensions

        channels.clear();
        List<Location[]> locations = new ArrayList<Location[]>();
        // Ensure more than the first pair get processed
        locations.add(new Location[] {new Location("loc2", new CellIndexExpression("100"), INDEX_0), new Location("loc2", new CellIndexExpression("101"), INDEX_0)});
        locations.add(new Location[] {new Location("loc2", INDEX_X, INDEX_Y), new Location("loc2", INDEX_X_MINUS_1, INDEX_Y)});
        locations.add(new Location[] {new Location("loc2", INDEX_X, INDEX_Y), new Location("loc2", INDEX_X_PLUS_1, INDEX_Y)});
        channels.add(new Channel("horiz", locations));
        
        compartments.clear();
        compartments.add(new Compartment("loc2", 2, 2));

        agents = Utils.getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1", "horiz")), 
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_0, INDEX_0), new AgentSite("s", null, "1", "horiz")), 
                new Agent("B", new Location("loc2", INDEX_1, INDEX_0), new AgentSite("s", null, "1"))));
        expected.add(Utils.getList(
                new Agent("A", new Location("loc2", INDEX_1, INDEX_0), new AgentSite("s", null, "1", "horiz")),
                new Agent("B", new Location("loc2", INDEX_0, INDEX_0), new AgentSite("s", null, "1"))
                ));
        expected.add(Utils.getList(new Agent("A", new Location("loc2", INDEX_0, INDEX_1), new AgentSite("s", null, "1", "horiz")), 
                new Agent("B", new Location("loc2", INDEX_1, INDEX_1), new AgentSite("s", null, "1"))));
        expected.add(Utils.getList(
                new Agent("A", new Location("loc2", INDEX_1, INDEX_1), new AgentSite("s", null, "1", "horiz")),
                new Agent("B", new Location("loc2", INDEX_0, INDEX_1), new AgentSite("s", null, "1"))
                ));
               
        checkMappingStructure(complex, compartments, channels, expected);

    }
    
    private void checkMappingStructure(Complex complex, List<Compartment> compartments, List<Channel> channels,
            List<List<Agent>> expected) {
        List<MappingInstance> result = model.initMappingStructure(complex, compartments, channels);
        assertEquals(expected.size(), result.size());
        
        for (int index=0; index < expected.size(); index++) {
            List<Agent> expectedAgents = expected.get(index);
            List<Agent> actualAgents = result.get(index).locatedAgents;
            
            assertEquals(expectedAgents.toString(), actualAgents.toString());
        }
    }
    

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    // TODO ADDITIONS START HERE
    
    
    
    
    
    
    
    
    
    
    @Test
    public void testAddTransition_invalid() {
        List<Agent> leftAgents = new ArrayList<Agent>();
        List<Agent> rightAgents = new ArrayList<Agent>();
        leftAgents.add(new Agent("agent1"));
        rightAgents.add(new Agent("agent2"));

        try {
            model.addTransition("label", new Location("left"), leftAgents, 
                    "channelName", new Location("right"), rightAgents, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addTransition("label", new Location("left"), null, 
                    null, new Location("right"), null, new VariableExpression(0.1f));
            fail("null agents and channel should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addTransition("label", new Location("left"), new ArrayList<Agent>(), 
                    null, new Location("right"), new ArrayList<Agent>(), new VariableExpression(0.1f));
            fail("lhs and rhs empty and null channel should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
    }


    @Test
    public void testAddTransition_noChannel() {
        Location leftLocation = new Location("left", new CellIndexExpression("2"));
        Location rightLocation = new Location("right");
        List<Agent> leftAgents = Utils.getList(
                new Agent("agent1", new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        List<Agent> rightAgents = Utils.getList(
                new Agent("agent2"), new Agent("agent4", new Location("otherRight")));
        VariableExpression rate = new VariableExpression(0.1f);
        
        // All other parameters
        
        model.addTransition("label", leftLocation, leftAgents, null, rightLocation, rightAgents, rate);

        List<Agent> expectedLeftAgents = Utils.getList(
                new Agent("agent1", leftLocation, new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        List<Agent> expectedRightAgents = Utils.getList(
                new Agent("agent2", rightLocation), 
                new Agent("agent4", new Location("otherRight")));
        Transition expected = new Transition("label", expectedLeftAgents, null, expectedRightAgents, 0.1f);
        checkTransitions(model, new Transition[] { expected });
        assertEquals(new Variable("label"), model.getVariables().get("label"));

        checkAggregateAgents(
                new AggregateAgent("agent1", new AggregateSite("interface1", "state1", "link1"), 
                        new AggregateSite("interface2", "state2", "link1")), 
                new AggregateAgent("agent2"),
                new AggregateAgent("agent3"),
                new AggregateAgent("agent4"));

        // No locations
        
        leftAgents = Utils.getList(
                new Agent("agent1", new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        rightAgents = Utils.getList(
                new Agent("agent2"), new Agent("agent4", new Location("otherRight")));
        model = new KappaModel();
        model.addTransition("label", null, leftAgents, null, null, rightAgents, rate);

        expectedLeftAgents = Utils.getList(
                new Agent("agent1", NOT_LOCATED, new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        expectedRightAgents = Utils.getList(
                new Agent("agent2", NOT_LOCATED), 
                new Agent("agent4", new Location("otherRight")));
        expected = new Transition("label", expectedLeftAgents, null, expectedRightAgents, 0.1f);
        checkTransitions(model, new Transition[] { expected });


        // No left agents
        
        rightAgents = Utils.getList(
                new Agent("agent2"), new Agent("agent4", new Location("otherRight")));
        model = new KappaModel();
        model.addTransition("label", null, null, null, null, rightAgents, rate);

        expectedLeftAgents.clear();
        expectedRightAgents = Utils.getList(
                new Agent("agent2", NOT_LOCATED), 
                new Agent("agent4", new Location("otherRight")));
        expected = new Transition("label", expectedLeftAgents, null, expectedRightAgents, 0.1f);
        checkTransitions(model, new Transition[] { expected });

        // No right agents
        
        leftAgents = Utils.getList(
                new Agent("agent1", new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        model = new KappaModel();
        model.addTransition("label", null, leftAgents, null, null, null, rate);

        expectedLeftAgents = Utils.getList(
                new Agent("agent1", NOT_LOCATED, new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        expectedRightAgents.clear();
        expected = new Transition("label", expectedLeftAgents, null, expectedRightAgents, 0.1f);
        checkTransitions(model, new Transition[] { expected });
    }

    private void checkTransitions(KappaModel kappaModel, Transition[] expectedTransitions) {
        assertEquals(expectedTransitions.length, kappaModel.getTransitions().size());

        Set<String> actual = new HashSet<String>();
        for (Transition transition : kappaModel.getTransitions()) {
            actual.add(transition.toString());
        }
        for (Transition expectedTransition : expectedTransitions) {
            String transitionString = expectedTransition.toString();
            if (!actual.contains(transitionString)) {
                fail("Transition not found: " + transitionString + "\nin:" + actual);
            }
        }
    }

    @Test
    public void testAddTransition_withChannel() {
        Location leftLocation = new Location("left", new CellIndexExpression("2"));
        Location rightLocation = new Location("right");
        List<Agent> leftAgents = Utils.getList(
                new Agent("agent1", new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        List<Agent> rightAgents = Utils.getList(
                new Agent("agent2"), new Agent("agent4", new Location("otherRight")));
        VariableExpression rate = new VariableExpression(0.1f);
        
        // All parameters
        
        model.addTransition("label", leftLocation, leftAgents, "channel", rightLocation, rightAgents, rate);

        List<Agent> expectedLeftAgents = Utils.getList(
                new Agent("agent1", leftLocation, new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        List<Agent> expectedRightAgents = Utils.getList(
                new Agent("agent2", rightLocation), 
                new Agent("agent4", new Location("otherRight")));
        Transition expected = new Transition("label", expectedLeftAgents, "channel", expectedRightAgents, 0.1f);
        checkTransitions(model, new Transition[] { expected });
        assertEquals(new Variable("label"), model.getVariables().get("label"));

        checkAggregateAgents(
                new AggregateAgent("agent1", new AggregateSite("interface1", "state1", "link1"), 
                        new AggregateSite("interface2", "state2", "link1")), 
                new AggregateAgent("agent2"),
                new AggregateAgent("agent3"),
                new AggregateAgent("agent4"));

        // No locations
        
        leftAgents = Utils.getList(
                new Agent("agent1", new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        rightAgents = Utils.getList(
                new Agent("agent2"), new Agent("agent4", new Location("otherRight")));
        model = new KappaModel();
        model.addTransition("label", null, leftAgents, "channel", null, rightAgents, rate);

        expectedLeftAgents = Utils.getList(
                new Agent("agent1", NOT_LOCATED, new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        expectedRightAgents = Utils.getList(
                new Agent("agent2", NOT_LOCATED), 
                new Agent("agent4", new Location("otherRight")));
        expected = new Transition("label", expectedLeftAgents, "channel", expectedRightAgents, 0.1f);
        checkTransitions(model, new Transition[] { expected });


        // No left agents
        
        rightAgents = Utils.getList(
                new Agent("agent2"), new Agent("agent4", new Location("otherRight")));
        model = new KappaModel();
        model.addTransition("label", null, null, "channel", null, rightAgents, rate);

        expectedLeftAgents.clear();
        expectedRightAgents = Utils.getList(
                new Agent("agent2", NOT_LOCATED), 
                new Agent("agent4", new Location("otherRight")));
        expected = new Transition("label", expectedLeftAgents, "channel", expectedRightAgents, 0.1f);
        checkTransitions(model, new Transition[] { expected });

        // No right agents
        
        leftAgents = Utils.getList(
                new Agent("agent1", new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        model = new KappaModel();
        model.addTransition("label", null, leftAgents, "channel", null, null, rate);

        expectedLeftAgents = Utils.getList(
                new Agent("agent1", NOT_LOCATED, new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("otherLeft")));
        expectedRightAgents.clear();
        expected = new Transition("label", expectedLeftAgents, "channel", expectedRightAgents, 0.1f);
        checkTransitions(model, new Transition[] { expected });

        // No agents
        
        model = new KappaModel();
        model.addTransition("label", null, null, "channel", null, null, rate);

        expectedLeftAgents.clear();
        expectedRightAgents.clear();
        expected = new Transition("label", expectedLeftAgents, "channel", expectedRightAgents, 0.1f);
        checkTransitions(model, new Transition[] { expected });

        // Locations only
        
        model = new KappaModel();
        model.addTransition("label", leftLocation, null, "channel", rightLocation, null, rate);

        expectedLeftAgents.clear();
        expectedRightAgents.clear();
        expected = new Transition("label", leftLocation, "channel", rightLocation, 0.1f);
        checkTransitions(model, new Transition[] { expected });

        // Left location only
        
        model = new KappaModel();
        model.addTransition("label", leftLocation, null, "channel", null, null, rate);

        expectedLeftAgents.clear();
        expectedRightAgents.clear();
        expected = new Transition("label", leftLocation, "channel", null, 0.1f);
        checkTransitions(model, new Transition[] { expected });

        // Right location only
        
        model = new KappaModel();
        model.addTransition("label", null, null, "channel", rightLocation, null, rate);

        expectedLeftAgents.clear();
        expectedRightAgents.clear();
        expected = new Transition("label", null, "channel", rightLocation, 0.1f);
        checkTransitions(model, new Transition[] { expected });
    }

//    @Test
//    public void testGetFixedLocatedTransitions_transforms() {
//        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
//        assertEquals(0, actual.size());
//        
//        // No compartments
//        List<Agent> leftAgents = getList(new Agent("agent1"));
//        List<Agent> rightAgents = getList(new Agent("agent2"));
//        model.addTransform("label", leftAgents, rightAgents, new VariableExpression(0.1f), NOT_LOCATED);
//        Transform transform1 = new Transform("label", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.1f);
//
//        checkLocatedTransitions(new LocatedTransform[] { 
//                new LocatedTransform(transform1, NOT_LOCATED)
//                });
//        
//        // add compartment
//        model = new KappaModel();
//        model.addCompartment(new Compartment("cytosol", 3));
//        
//        leftAgents = getList(new Agent("agent3"));
//        rightAgents = getList(new Agent("agent4"));
//        model.addTransform("label2", leftAgents, rightAgents, new VariableExpression(0.2f), new Location("cytosol"));
//        Transform transform2 = new Transform("label2", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.2f);
//
//        leftAgents = getList(new Agent("agent5"));
//        rightAgents = getList(new Agent("agent6"));
//        model.addTransform("label3", leftAgents, rightAgents, new VariableExpression(0.3f), new Location("cytosol", new CellIndexExpression("1")));
//        Transform transform3 = new Transform("label3", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.3f);
//
//        checkLocatedTransitions(new LocatedTransform[] { 
//                new LocatedTransform(transform2, new Location("cytosol", new CellIndexExpression("0"))),
//                new LocatedTransform(transform2, new Location("cytosol", new CellIndexExpression("1"))),
//                new LocatedTransform(transform2, new Location("cytosol", new CellIndexExpression("2"))),
//                new LocatedTransform(transform3, new Location("cytosol", new CellIndexExpression("1"))),
//        });
//    }
//
//    @Test
//    public void testGetFixedLocatedTransitions_transforms_noCompartmentSpecified() {
//        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
//        assertEquals(0, actual.size());
//        
//        // No compartments
//        List<Agent> leftAgents = getList(new Agent("agent1"));
//        List<Agent> rightAgents = getList(new Agent("agent2"));
//        model.addTransform("label", leftAgents, rightAgents, new VariableExpression(0.1f), NOT_LOCATED);
//        Transform transform = new Transform("label", Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.1f);
//
//        checkLocatedTransitions(new LocatedTransform[] { 
//                new LocatedTransform(transform, NOT_LOCATED)
//                });
//        
//        // add compartment
//        model.addCompartment(new Compartment("cytosol", 3));
//        model.addCompartment(new Compartment("membrane", 2));
//        
//        checkLocatedTransitions(new LocatedTransform[] { 
//                new LocatedTransform(transform, new Location("cytosol", new CellIndexExpression("0"))),
//                new LocatedTransform(transform, new Location("cytosol", new CellIndexExpression("1"))),
//                new LocatedTransform(transform, new Location("cytosol", new CellIndexExpression("2"))),
//                new LocatedTransform(transform, new Location("membrane", new CellIndexExpression("0"))),
//                new LocatedTransform(transform, new Location("membrane", new CellIndexExpression("1"))),
//        });
//    }
//
//    @Test
//    public void testGetConcreteLocatedTransitions_transports() {
//        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
//        assertEquals(0, actual.size());
//        
//        model.addCompartment(new Compartment("cytosol", 3));
//        model.addChannel(new Channel("intra", getList(
//                new Location[] {
//                        new Location("cytosol", INDEX_X),
//                        new Location("cytosol", INDEX_X_PLUS_1),
//                }, 
//                new Location[] {
//                        new Location("cytosol", INDEX_X),
//                        new Location("cytosol", INDEX_X_MINUS_1),
//                }, 
//                new Location[] {
//                        new Location("cytosol", INDEX_2), 
//                        new Location("cytosol", INDEX_0),
//                },
//                new Location[] {
//                        new Location("cytosol", INDEX_0), 
//                        new Location("cytosol", INDEX_2),
//                }
//                )));
//        
//        List<Agent> agents = getList(new Agent("agent1"));
//        model.addTransport("label", "intra", agents, new VariableExpression(0.2f));
//        Transport transport = new Transport("label",  "intra", agents, 0.2f);
//
//        Location cell0 = new Location("cytosol", INDEX_0);
//        Location cell1 = new Location("cytosol", INDEX_1);
//        Location cell2 = new Location("cytosol", INDEX_2);
//        
//        checkLocatedTransitions(new LocatedTransport[] { 
//                new LocatedTransport(transport, cell0, cell1),
//                new LocatedTransport(transport, cell1, cell0),
//                new LocatedTransport(transport, cell1, cell2),
//                new LocatedTransport(transport, cell2, cell1),
//                new LocatedTransport(transport, cell0, cell2),
//                new LocatedTransport(transport, cell2, cell0),
//        });
//    }
//
//    @Test
//    public void testGetConcreteLocatedTransitions_transports_multipleLinksSameLabel() {
//        List<LocatedTransition> actual = model.getFixedLocatedTransitions();
//        assertEquals(0, actual.size());
//        
//        model.addCompartment(new Compartment("cytosol", 3));
//        model.addChannel(new Channel("intra", getList(
//                new Location[] {
//                        new Location("cytosol", INDEX_0), 
//                        new Location("cytosol", INDEX_1)
//                },
//                new Location[] {
//                        new Location("cytosol", INDEX_1), 
//                        new Location("cytosol", INDEX_2),
//                },
//                new Location[] {
//                        new Location("cytosol", INDEX_1), 
//                        new Location("cytosol", INDEX_0)
//                },
//                new Location[] {
//                        new Location("cytosol", INDEX_2), 
//                        new Location("cytosol", INDEX_1),
//                }
//                )));
//        
//        List<Agent> agents = getList(new Agent("agent1"));
//        model.addTransport("label", "intra", agents, new VariableExpression(0.2f));
//        Transport transport = new Transport("label",  "intra", agents, 0.2f);
//
//        Location cell0 = new Location("cytosol", INDEX_0);
//        Location cell1 = new Location("cytosol", INDEX_1);
//        Location cell2 = new Location("cytosol", INDEX_2);
//        
//        checkLocatedTransitions(new LocatedTransport[] { 
//                new LocatedTransport(transport, cell0, cell1),
//                new LocatedTransport(transport, cell1, cell0),
//                new LocatedTransport(transport, cell1, cell2),
//                new LocatedTransport(transport, cell2, cell1),
//        });
//    }
//
//    private void checkLocatedTransitions(LocatedTransition[] expecteds) {
//        List<String> actuals = new ArrayList<String>();
//        for (LocatedTransition transition : model.getFixedLocatedTransitions()) {
//            actuals.add(transition.toString());
//        }
//        assertEquals(expecteds.length, actuals.size());
//        
//        for (LocatedTransition expected : expecteds) {
//            String expectedString = expected.toString();
//            if (!actuals.contains(expectedString)) {
//                fail("Not found: " + expected);
//            }
//            actuals.remove(expectedString);
//        }
//    }
//
//
//
//    
//    @Test
//    public void testValidate_transport() {
//        // Missing transport reference
//        model = new KappaModel();
//        model.addCompartment(new Compartment("known"));
//        model.addChannel(new Channel("compartmentLink", new Location("known"), new Location("known")));
//        model.addTransport("transportRef", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(new VariableReference("unknown")));
//        checkValidate_failure("Reference 'unknown' not found");
//        
//        model = new KappaModel();
//        model.addVariable(new VariableExpression(2), "variableRef");
//        model.addTransport("transportRef", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(new VariableReference("variableRef")));
//        checkValidate_failure("Compartment link 'compartmentLink' not found");
//        
//        // Non fixed transport reference
//        model = new KappaModel();
//        model.addCompartment(new Compartment("known"));
//        model.addChannel(new Channel("compartmentLink", new Location("known"), new Location("known")));
//        model.addVariable(new VariableExpression(SimulationToken.EVENTS), "variableRef");
//        model.addTransport("other", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(new VariableReference("variableRef")));
//        checkValidate_failure("Reference 'variableRef' not fixed");
//        
//        model = new KappaModel();
//        model.addCompartment(new Compartment("known"));
//        model.addChannel(new Channel("compartmentLink", new Location("known"), new Location("known")));
//        model.addTransport("transportRef", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(2));
//        model.validate();
//
//        model = new KappaModel();
//        model.addCompartment(new Compartment("known"));
//        model.addChannel(new Channel("compartmentLink", new Location("known"), new Location("known")));
//        model.addTransport("transportRef", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(Constant.INFINITY));
//        model.validate();
//        
//        model = new KappaModel();
//        model.addCompartment(new Compartment("known"));
//        model.addChannel(new Channel("compartmentLink", new Location("known"), new Location("known")));
//        model.addVariable(new VariableExpression(2), "variableRef");
//        model.addTransport("other", "compartmentLink", getList(new Agent("agent1")), new VariableExpression(new VariableReference("variableRef")));
//        model.validate();
//        
//    }
//    
//    @Test
//    public void testValidate_transform() {
//        // Missing transform reference
//        model = new KappaModel();
//        model.addAgentDeclaration(new AggregateAgent("agent1"));
//        model.addTransform(null, getList(new Agent("agent1")), null, 
//                new VariableExpression(new VariableReference("unknown")), 
//                NOT_LOCATED);
//        checkValidate_failure("Reference 'unknown' not found");
//        
//        // Non fixed transform reference
//        model = new KappaModel();
//        model.addAgentDeclaration(new AggregateAgent("agent1"));
//        model.addVariable(new VariableExpression(SimulationToken.EVENTS), "variableRef");
//        model.addTransform(null, getList(new Agent("agent1")), null, 
//                new VariableExpression(new VariableReference("variableRef")), 
//                NOT_LOCATED);
//        checkValidate_failure("Reference 'variableRef' not fixed");
//        
//        model = new KappaModel();
//        model.addAgentDeclaration(new AggregateAgent("agent1"));
//        model.addTransform(null, getList(new Agent("agent1")), null, 
//                new VariableExpression(Constant.INFINITY), 
//                NOT_LOCATED);
//        model.validate();
//    }
//    
//    
//     
//    @Test
//    public void testGetValidLocatedTransforms() throws Exception {
//        
//        Transform transform = new Transform("label", 
//                getList(new Agent("A", new AgentSite("s", null, null))), 
//                getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1"))), 10f, false);
//        
//        List<Channel> channels = new ArrayList<Channel>();
//        channels.add(new Channel("intra", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1)));
//        channels.add(new Channel("inter", new Location("loc1"), new Location("loc2", INDEX_2)));
//        
//        List<Compartment> compartments = new ArrayList<Compartment>();
//        compartments.add(new Compartment("loc1"));
//        compartments.add(new Compartment("loc2", 3));
//        compartments.add(new Compartment("loc3", 3));
//        
//
//        try {
//            model.getValidLocatedTransforms(null, compartments, channels);
//            fail("null should have failed");
//        }
//        catch (NullPointerException ex) {
//            // Expected exception
//        }
//        
//        try {
//            model.getValidLocatedTransforms(transform, null, channels);
//            fail("null should have failed");
//        }
//        catch (NullPointerException ex) {
//            // Expected exception
//        }
//        
//        try {
//            model.getValidLocatedTransforms(transform, compartments, null);
//            fail("null should have failed");
//        }
//        catch (NullPointerException ex) {
//            // Expected exception
//        }
//        
//        // No location
//        
//        List<Transform> expected = new ArrayList<Transform>();
//        expected.add(new Transform("label", getList(new Agent("A", new AgentSite("s", null, null))), 
//                getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1"))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // Full location
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc1"))), 
//                getList(new Agent("A", new Location("loc1"))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label", 
//                getList(new Agent("A", new Location("loc1"))), 
//                getList(new Agent("A", new Location("loc1"))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // Single unlinked partial location
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2"))), 
//                getList(new Agent("A", new Location("loc2"))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label-1", 
//                getList(new Agent("A", new Location("loc2", INDEX_0))), 
//                getList(new Agent("A", new Location("loc2", INDEX_0))), 10f, false));
//        expected.add(new Transform("label-2", 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 
//                getList(new Agent("A", new Location("loc2", INDEX_1))), 10f, false));
//        expected.add(new Transform("label-3", 
//                getList(new Agent("A", new Location("loc2", INDEX_2))), 
//                getList(new Agent("A", new Location("loc2", INDEX_2))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // Multiple unlinked partial locations should fail
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2"))), 
//                getList(new Agent("B", new Location("loc3"))), 10f, false);
//
//        try {
//            model.getValidLocatedTransforms(transform, compartments, channels);
//            fail("invalid transform should have failed");
//        }
//        catch (IllegalArgumentException ex) {
//            // Expected exception
//        }
//        
//        // Dimer unlinking same full location, no channel
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // Dimer linked, same partial location, no channel
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, null))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label-1", 
//                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, null))), 10f, false));
//        expected.add(new Transform("label-2", 
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false));
//        expected.add(new Transform("label-3", 
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, null))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        // No label
//        transform = new Transform(null, 
//                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, null))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform(null, 
//                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, null))), 10f, false));
//        expected.add(new Transform(null, 
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false));
//        expected.add(new Transform(null, 
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, null))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//        transform = new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2"), new AgentSite("s", null, null))), 10f, false);
//
//        expected.clear();
//        expected.add(new Transform("label", 
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))),
//                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
//                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, null))), 10f, false));
//        
//        checkTransforms(expected, model.getValidLocatedTransforms(transform, compartments, channels));
//        
//
//        
//    }
//    
    


}
