package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.*;
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
        Location location = new Location("cytosol", INDEX_2);
        Location otherLocation = new Location("nucleus", INDEX_2);
        model.addCompartment(new Compartment("cytosol", 3));
        model.addCompartment(new Compartment("nucleus", 3));
        
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

        checkFixedLocatedInitialValues(new Object[][] { 
                { "[agent1:cytosol[0]()]", 1 }, 
                { "[agent1:cytosol[1]()]", 1 }, 
                { "[agent1:cytosol[2]()]", 1 }, 
                { "[agent1:nucleus[0]()]", 0 }, 
                { "[agent1:nucleus[1]()]", 0 }, 
                { "[agent1:nucleus[2]()]", 0 }, 
                { "[agent1:cytosol[2](x!1), agent2:cytosol[2](x!1)]", 5 }, 
                { "[agent3:cytosol[2](x!2), agent4:cytosol[2](x!2)]", 5 }, 
                { "[agent3:nucleus[2](x!7), agent4:nucleus[2](x!7)]", 7 } });
        checkAggregateAgents(new AggregateAgent("agent1", new AggregateSite("x", null, "1")), 
                new AggregateAgent("agent2", new AggregateSite("x", null, "1")),
                new AggregateAgent("agent3", new AggregateSite("x", null, new String[] {"2", "7"})), 
                new AggregateAgent("agent4", new AggregateSite("x", null, new String[] {"2", "7"})));
    }
    
    @Test
    public void testAddInitialValue_propogateLocationsToAgents() {
        List<Agent> agents = Utils.getList(new Agent("agent1"));
        Location location = new Location("cytosol", INDEX_2);
        Location otherLocation = new Location("nucleus", INDEX_2);
        model.addCompartment(new Compartment("cytosol", 3));
        model.addCompartment(new Compartment("nucleus", 3));
        
        Channel channel = new Channel("dl", new Location("cytosol", INDEX_X), new Location("nucleus", INDEX_X));
        model.addChannel(channel);
        
        model.addAgentDeclaration(new AggregateAgent("agent1"));
        model.addAgentDeclaration(new AggregateAgent("agent2"));
        model.addAgentDeclaration(new AggregateAgent("agent3"));
        model.addAgentDeclaration(new AggregateAgent("agent4"));
        model.addInitialValue(agents, "3", location);

        agents = Utils.getList(new Agent("agent1", new AgentSite("x", null, "1", "dl")), 
                new Agent("agent2", otherLocation, new AgentSite("x", null, "1")), 
                new Agent("agent3", new AgentSite("x", null, "2")), 
                new Agent("agent4", location, new AgentSite("x", null, "2")));
        model.addInitialValue(agents, "5", location);
        
        checkFixedLocatedInitialValues(new Object[][] { 
                { "[agent1:cytosol[2]()]", 3 }, 
                { "[agent1:cytosol[2](x!1:dl), agent2:nucleus[2](x!1)]", 5 }, 
                { "[agent3:cytosol[2](x!2), agent4:cytosol[2](x!2)]", 5 } });
        checkAggregateAgents(new AggregateAgent("agent1", new AggregateSite("x", null, "1")), 
                new AggregateAgent("agent2", new AggregateSite("x", null, "1")),
                new AggregateAgent("agent3", new AggregateSite("x", null, new String[] {"2"})), 
                new AggregateAgent("agent4", new AggregateSite("x", null, new String[] {"2"})));
    }
    
    @Test
    public void testAddInitialValue_reference() {
        List<Agent> agents = Utils.getList(new Agent("agent1"));
        Location location = new Location("cytosol", INDEX_2);
        Location otherLocation = new Location("nucleus", INDEX_2);
        model.addVariable(new VariableExpression(3), "variable 3");
        model.addVariable(new VariableExpression(5), "variable 5");
        VariableReference reference = new VariableReference("variable 3");
        model.addCompartment(new Compartment("cytosol", 3));
        model.addCompartment(new Compartment("nucleus", 3));

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

        checkFixedLocatedInitialValues(new Object[][] { 
                { "[agent1:cytosol[2]()]", 3 }, 
                { "[agent1:cytosol[2](x!1), agent2:cytosol[2](x!1)]", 5 }, 
                { "[agent3:cytosol[2](x!2), agent4:cytosol[2](x!2)]", 5 }, 
                { "[agent3:nucleus[2](x!7), agent4:nucleus[2](x!7)]", 7 } });
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

        checkFixedLocatedInitialValues(new Object[][] { { "[agent1()]", 3 } });
        
        // add compartment
        model.addCompartment(new Compartment("cytosol", 3));
        

        model.addInitialValue(Utils.getList(new Agent("agent2")), "5", new Location("cytosol"));
        model.addInitialValue(Utils.getList(new Agent("agent2")), "10", new Location("cytosol", INDEX_1));

        checkFixedLocatedInitialValues(new Object[][] { 
                { "[agent1:cytosol[0]()]", 1 }, 
                { "[agent1:cytosol[1]()]", 1 }, 
                { "[agent1:cytosol[2]()]", 1 }, 
                { "[agent2:cytosol[0]()]", 2 }, 
                { "[agent2:cytosol[1]()]", 12 }, 
                { "[agent2:cytosol[2]()]", 1 }, 
                });
    }
    
    @Test
    public void testGetFixedLocatedInitialValuesMap_multiCompartmentAgents() {
        Map<Complex, Integer> actual = model.getFixedLocatedInitialValuesMap();
        assertEquals(0, actual.size());
        
        model.addCompartment(new Compartment("cytosol", 3, 3, 3));
        model.addCompartment(new Compartment("membrane", 3, 3));

        model.addChannel(new Channel("domainLink", 
                new Location("cytosol", INDEX_X, INDEX_Y, INDEX_0), new Location("membrane", INDEX_X, INDEX_Y)));
        
        model.addAgentDeclaration(new AggregateAgent("A", new AggregateSite("d", (String) null, null)));
        model.addAgentDeclaration(new AggregateAgent("B", new AggregateSite("d", (String) null, null)));
        
        model.addInitialValue(Utils.getList(new Agent("A", new Location("cytosol"), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new AgentSite("d", null, "1"))), "900", NOT_LOCATED);

        checkFixedLocatedInitialValues(new Object[][] { 
                { "[A:cytosol[0][0][0](d!1:domainLink), B:membrane[0][0](d!1)]", 100 }, 
                { "[A:cytosol[0][1][0](d!1:domainLink), B:membrane[0][1](d!1)]", 100 }, 
                { "[A:cytosol[0][2][0](d!1:domainLink), B:membrane[0][2](d!1)]", 100 }, 
                { "[A:cytosol[1][0][0](d!1:domainLink), B:membrane[1][0](d!1)]", 100 }, 
                { "[A:cytosol[1][1][0](d!1:domainLink), B:membrane[1][1](d!1)]", 100 }, 
                { "[A:cytosol[1][2][0](d!1:domainLink), B:membrane[1][2](d!1)]", 100 }, 
                { "[A:cytosol[2][0][0](d!1:domainLink), B:membrane[2][0](d!1)]", 100 }, 
                { "[A:cytosol[2][1][0](d!1:domainLink), B:membrane[2][1](d!1)]", 100 }, 
                { "[A:cytosol[2][2][0](d!1:domainLink), B:membrane[2][2](d!1)]", 100 }, 
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

        checkFixedLocatedInitialValues(new Object[][] { { "[agent1(s,t~x,u~y)]", 3 } });
        
        // add compartment
        model.addCompartment(new Compartment("cytosol", 3));
        

        model.addInitialValue(Utils.getList(new Agent("agent2")), "5", new Location("cytosol"));
        model.addInitialValue(Utils.getList(new Agent("agent2", new AgentSite("v", "b", null))), "10", new Location("cytosol", INDEX_1));

        checkFixedLocatedInitialValues(new Object[][] { 
                { "[agent1:cytosol[0](s,t~x,u~y)]", 1 }, 
                { "[agent1:cytosol[1](s,t~x,u~y)]", 1 }, 
                { "[agent1:cytosol[2](s,t~x,u~y)]", 1 }, 
                { "[agent2:cytosol[0](v~a)]", 2 }, 
                { "[agent2:cytosol[1](v~b)]", 10 }, 
                { "[agent2:cytosol[1](v~a)]", 2 }, 
                { "[agent2:cytosol[2](v~a)]", 1 }, 
                });
    }

    @Test
    public void testGetValidLocatedTransitions_invalid() {
        Transition templateTransition = new Transition("label", new Location("leftLocation"), 
                "channel", new Location("rightLocation"), 2f);
        List<Compartment> compartments = getList(new Compartment("leftLocation"), new Compartment("rightLocation"));
        List<Channel> channels = getList(new Channel("channel", new Location("leftLocation"), new Location("rightLocation")));
        
        try {
            model.getValidLocatedTransitions(null, compartments, channels);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            model.getValidLocatedTransitions(templateTransition, null, channels);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            model.getValidLocatedTransitions(templateTransition, compartments, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.getValidLocatedTransitions(templateTransition, new ArrayList<Compartment>(), channels);
            fail("unknown compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        try {
            model.getValidLocatedTransitions(templateTransition, compartments, new ArrayList<Channel>());
            fail("unknown channel should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
    }

    @Test
    public void testGetValidLocatedTransitions_noDiffusion() {
        List<Compartment> compartments = new ArrayList<Compartment>();
        List<Channel> channels = new ArrayList<Channel>();

        // No compartments
        Transition templateTransition = new Transition("label", 
                getList(new Agent("agent1")), null, 
                getList(new Agent("agent1")), 0.1f);

        checkLocatedTransitions(templateTransition, new Transition[] { 
                templateTransition
                }, compartments, channels);
        
        // add compartment
        compartments.add(new Compartment("cytosol", 3));
        checkLocatedTransitions(templateTransition, new Transition[] { 
                new Transition("label-1", 
                        getList(new Agent("agent1", new Location("cytosol", INDEX_0))), null, 
                        getList(new Agent("agent1", new Location("cytosol", INDEX_0))), 0.1f),
                new Transition("label-2", 
                        getList(new Agent("agent1", new Location("cytosol", INDEX_1))), null, 
                        getList(new Agent("agent1", new Location("cytosol", INDEX_1))), 0.1f),
                new Transition("label-3", 
                        getList(new Agent("agent1", new Location("cytosol", INDEX_2))), null, 
                        getList(new Agent("agent1", new Location("cytosol", INDEX_2))), 0.1f),
                }, compartments, channels);
    }

    private void checkLocatedTransitions(Transition template, Transition[] expecteds, 
            List<Compartment> compartments, List<Channel> channels) {
        
        List<String> actuals = new ArrayList<String>();
        for (Transition transition : model.getValidLocatedTransitions(template, compartments, channels)) {
            actuals.add(transition.toString());
        }

        assertEquals(expecteds.length, actuals.size());
        
        for (Transition expected : expecteds) {
            String expectedString = expected.toString();
            if (!actuals.contains(expectedString)) {
                fail("Not found: " + expected + " in " + actuals);
            }
            actuals.remove(expectedString);
        }
    }

    @Test
    public void testAddVariable_withAgents() {
        List<Agent> agents1 = getList(new Agent("agent1"));

        try {
            model.addVariable(null, "label", null, false);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            model.addVariable(agents1, null, null, false);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        model.addVariable(agents1, "label", NOT_LOCATED, false);

        checkVariables(model, "'label' ([agent1()])");
        checkOrderedVariableNames(model, "label");
        checkAggregateAgents(new AggregateAgent("agent1"));

        model.addVariable(getList(new Agent("agent1"), new Agent("agent2")), "label2", NOT_LOCATED, false);

        checkVariables(model, "'label' ([agent1()])", "'label2' ([agent1(), agent2()])");
        checkOrderedVariableNames(model, "label", "label2");
        checkAggregateAgents(new AggregateAgent("agent1"), new AggregateAgent("agent2"));
    }


    @Test
    public void testAddVariable_orderingOfNames() {
        model.addVariable(getList(new Agent("agent1")), "C", NOT_LOCATED, false);

        model.addVariable(getList(new Agent("agent1"), new Agent("agent2")), "A", NOT_LOCATED, false);

        model.addVariable(new VariableExpression(100f), "B");

        checkVariables(model, "'A' ([agent1(), agent2()])", "'B' (100.0)", "'C' ([agent1()])");
        checkOrderedVariableNames(model, "C", "A", "B");
    }

    @Test
    public void testAddVariable_withAgentsAndCompartments() {
        List<Agent> agents1 = getList(new Agent("agent1"));
        Location location = new Location("cytosol", INDEX_2);

        try {
            model.addVariable(null, "label", location, false);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            model.addVariable(agents1, null, location, false);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        model.addVariable(agents1, "label", location, false);

        checkVariables(model, "'label' cytosol[2] ([agent1:cytosol[2]()])");
        checkOrderedVariableNames(model, "label");
        checkAggregateAgents(new AggregateAgent("agent1"));

        model.addVariable(getList(new Agent("agent1", new Location("cytosol", INDEX_0)), 
                new Agent("agent2")), "label2", location, false);

        checkVariables(model, "'label' cytosol[2] ([agent1:cytosol[2]()])", 
                "'label2' cytosol[2] ([agent1:cytosol[0](), agent2:cytosol[2]()])");
        checkOrderedVariableNames(model, "label", "label2");
        checkAggregateAgents(new AggregateAgent("agent1"), new AggregateAgent("agent2"));

        model.addVariable(getList(new Agent("agent3")), "label3", new Location("cytosol"), true);

        checkVariables(model, "'label' cytosol[2] ([agent1:cytosol[2]()])", 
                "'label2' cytosol[2] ([agent1:cytosol[0](), agent2:cytosol[2]()])",
                "'label3' voxel cytosol ([agent3:cytosol()])");
        checkOrderedVariableNames(model, "label", "label2", "label3");
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
            model.addCompartment(null, "type", new ArrayList<Integer>());
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            model.addCompartment("label", "type", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            model.addCompartment("label", "unknownShape", new ArrayList<Integer>());
            fail("unknown shape should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        assertNotNull(model.getCompartments());
        assertEquals(0, model.getCompartments().size());

        model.addCompartment("compartment1", null, new ArrayList<Integer>());
        model.addCompartment("compartment2", null, getList(2, 3));
        model.addCompartment("compartment3", "OpenRectangle", getList(10, 8, 3));
        assertEquals(3, model.getCompartments().size());
        assertEquals("compartment1", model.getCompartments().get(0).toString());
        assertEquals("compartment2[2][3]", model.getCompartments().get(1).toString());
        assertEquals(Compartment.OpenRectangle.class, model.getCompartments().get(2).getClass());
        assertEquals("compartment3 (OpenRectangle) [10][8] [3]", model.getCompartments().get(2).toString());

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
            String complexString = (String) expected[0];
            boolean found = false;
            for (Map.Entry<Complex, Integer> entry : quantityMap.entrySet()) {
                if (complexString.equals(entry.getKey().toString())) {
                    assertEquals(expected[1], entry.getValue());
                    found = true;
                    break;
                }
            }
            assertTrue("Complex not found: " + complexString + " in " + quantityMap, found);
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
        model.addVariable(Utils.getList(new Agent("agent1")), "kappaRef", NOT_LOCATED, false);
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
        model.addVariable(Utils.getList(new Agent("agent1")), "kappaRef", NOT_LOCATED, false);
        model.addVariable(new VariableExpression(new VariableReference("kappaRef")), "label");
        model.validate();
    }
    
    @Test
    public void testValidate_variable_recordVoxels() {
        Compartment compartment = new Compartment("loc1", 4);
        AggregateAgent agentDeclaration = new AggregateAgent("agent1", new AggregateSite("x", null, "1"));
        
        // Test variable.validate() is called
        model = new KappaModel();
        model.addAgentDeclaration(agentDeclaration);
        model.addCompartment(compartment);
        model.addVariable(Utils.getList(new Agent("agent1")), "label", new Location("loc1", INDEX_0), true);
        checkValidate_failure("Voxel based observation must refer to a voxel based compartment: label");
    }
    
    @Test
    public void testValidate_compartment() {
        // Test compartment.validate() is called
        model = new KappaModel();
        model.addCompartment(new Compartment("fixed"));
        checkValidate_failure("Compartment 'fixed' uses a reserved compartment name");
        
        // Test one one compartment per name is used
        model = new KappaModel();
        model.addCompartment(new Compartment("name"));
        model.addCompartment(new Compartment("name"));
        checkValidate_failure("Duplicate compartment 'name'");
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
    public void testValidate_agentDeclaration() {
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("A"));
        model.addAgentDeclaration(new AggregateAgent("B", new AggregateSite("site1", "x", null)));
        model.validate();
    }
    
    @Test
    public void testValidate_agentDeclarationInvalid() {
        model = new KappaModel();
        model.addVariable(Utils.getList(new Agent("A")), "test", NOT_LOCATED, false);
        checkValidate_failure("Agent 'A' not declared");

        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("A", new AggregateSite("site2", (String) null, null)));
        model.addVariable(Utils.getList(new Agent("A", new AgentSite("site1", null, null))), "test", NOT_LOCATED, false);
        checkValidate_failure("Agent site A(site1) not declared");
        
        model = new KappaModel();
        model.addAgentDeclaration(new AggregateAgent("A", new AggregateSite("site1", "x", null)));
        model.addVariable(Utils.getList(new Agent("A", new AgentSite("site1", "y", null))), "test", NOT_LOCATED, false);
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
        Location leftLocation = new Location("left", INDEX_2);
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
        Location leftLocation = new Location("left", INDEX_2);
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

}
