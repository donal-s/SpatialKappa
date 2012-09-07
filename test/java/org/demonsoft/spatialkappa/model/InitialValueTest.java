package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getComplexes;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;


public class InitialValueTest {

    public static CellIndexExpression INDEX_10 = new CellIndexExpression("10");
    public static CellIndexExpression INDEX_13 = new CellIndexExpression("13");
    public static CellIndexExpression INDEX_20 = new CellIndexExpression("20");
    public static CellIndexExpression INDEX_23 = new CellIndexExpression("23");

    
    @SuppressWarnings("unused")
    @Test
    public void testInitialValue_value() {
        List<Complex> complexes = Utils.getComplexes(Utils.getList(new Agent("A"), 
                new Agent("B", new Location("cytosol"), new AgentSite("x", null, "1")), 
                new Agent("C", new Location("nucleus"), new AgentSite("y", null, "1"))));
        Location location = new Location("cytosol", INDEX_2);

        try {
            new InitialValue(null, 5, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new InitialValue(complexes, 5, null);
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
        assertEquals("[[A:cytosol[2]()], [B:cytosol(x!1), C:nucleus(y!1)]]", initialValue.complexes.toString());
        assertEquals(5, initialValue.quantity);
        assertNull(initialValue.reference);

        complexes = Utils.getComplexes(Utils.getList(new Agent("A"), 
                new Agent("B", new Location("cytosol"), new AgentSite("x", null, "1")), 
                new Agent("C", new Location("nucleus"), new AgentSite("y", null, "1"))));
        initialValue = new InitialValue(complexes, 5, NOT_LOCATED);
        assertEquals("[[A()], [B:cytosol(x!1), C:nucleus(y!1)]]", initialValue.complexes.toString());
        assertEquals(5, initialValue.quantity);
        assertNull(initialValue.reference);
    }

    @SuppressWarnings("unused")
    @Test
    public void testInitialValue_reference() {
        List<Complex> complexes = Utils.getComplexes(Utils.getList(new Agent("A"), 
                new Agent("B", new Location("cytosol"), new AgentSite("x", null, "1")), 
                new Agent("C", new Location("nucleus"), new AgentSite("y", null, "1"))));
        Location location = new Location("cytosol", INDEX_2);
        VariableReference reference = new VariableReference("variable");

        try {
            new InitialValue(null, reference, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new InitialValue(complexes, null, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new InitialValue(complexes, reference, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new InitialValue(new ArrayList<Complex>(), reference, location);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        InitialValue initialValue = new InitialValue(complexes, reference, location);
        assertEquals("[[A:cytosol[2]()], [B:cytosol(x!1), C:nucleus(y!1)]]", initialValue.complexes.toString());
        assertEquals(0, initialValue.quantity);
        assertSame(reference, initialValue.reference);

        complexes = Utils.getComplexes(Utils.getList(new Agent("A"), 
                new Agent("B", new Location("cytosol"), new AgentSite("x", null, "1")), 
                new Agent("C", new Location("nucleus"), new AgentSite("y", null, "1"))));
        initialValue = new InitialValue(complexes, reference, NOT_LOCATED);
        assertEquals("[[A()], [B:cytosol(x!1), C:nucleus(y!1)]]", initialValue.complexes.toString());
        assertEquals(0, initialValue.quantity);
        assertSame(reference, initialValue.reference);
    }

    
    @Test
    public void testGetFixedLocatedComplexMap_overallLocation() {
        // TODO check missing quantity reference
        List<Compartment> compartments = new ArrayList<Compartment>();
        List<Channel> channels = new ArrayList<Channel>();
        
        // No compartments
        InitialValue initialValue = new InitialValue(getComplexes(getList(new Agent("agent"))), 12, NOT_LOCATED);
        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] {
                { new Complex(new Agent("agent")), 12 } 
                });
        
        // add compartment
        compartments.add(new Compartment("cytosol", 3));
        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_0))), 4 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_1))), 4 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_2))), 4 }, 
                });
        
        compartments.add(new Compartment("nucleus"));
        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("agent", new Location("nucleus"))), 3 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_0))), 3 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_1))), 3 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_2))), 3 }, 
                });
        

        initialValue = new InitialValue(getComplexes(getList(new Agent("agent"))), 5, new Location("cytosol"));
        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_0))), 2 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_1))), 2 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_2))), 1 }, 
                });

        initialValue = new InitialValue(getComplexes(getList(new Agent("agent"))), 10, new Location("cytosol", INDEX_1));
        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_1))), 10 }, 
                });
        
        // Multiple distinct complexes
        initialValue = new InitialValue(getComplexes(getList(new Agent("agent"), new Agent("agent2"))), 12, new Location("cytosol"));
        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_0))), 4 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_1))), 4 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_2))), 4 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_0))), 4 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_1))), 4 }, 
                { new Complex(new Agent("agent2", new Location("cytosol", INDEX_2))), 4 }, 
                });
    }
    
    @Test
    public void testGetFixedLocatedComplexMap_agentLocation() {
        // TODO check missing quantity reference
        List<Compartment> compartments = getList(new Compartment("cytosol", 3), new Compartment("nucleus"));
        List<Channel> channels = new ArrayList<Channel>();
        
        InitialValue initialValue = new InitialValue(getComplexes(getList(new Agent("agent", new Location("cytosol")))), 5, NOT_LOCATED);
        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_0))), 2 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_1))), 2 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_2))), 1 }, 
                });

        initialValue = new InitialValue(getComplexes(getList(new Agent("agent", new Location("cytosol", INDEX_1)))), 10, NOT_LOCATED);
        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_1))), 10 }, 
                });
        
        // Multiple distinct complexes
        initialValue = new InitialValue(getComplexes(getList(new Agent("agent"), 
                new Agent("agent2", new Location("nucleus")))), 12, new Location("cytosol"));
        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_0))), 4 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_1))), 4 }, 
                { new Complex(new Agent("agent", new Location("cytosol", INDEX_2))), 4 }, 
                { new Complex(new Agent("agent2", new Location("nucleus"))), 12 }, 
                });
    }
    
    @Test
    public void testGetFixedLocatedComplexMap_multiCompartmentAgents() {
        List<Compartment> compartments = getList(new Compartment("cytosol", 3, 3, 3), new Compartment("membrane", 3, 3));
        List<Channel> channels = getList(new Channel("domainLink", 
                new Location("cytosol", INDEX_X, INDEX_Y, INDEX_0), new Location("membrane", INDEX_X, INDEX_Y)));
        
        
        InitialValue initialValue = new InitialValue(getComplexes(getList(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_0), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new AgentSite("d", null, "1")))), 100, NOT_LOCATED);

        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_0, INDEX_1), new AgentSite("d", null, "1"))), 100 }, 
                });
        
        initialValue = new InitialValue(getComplexes(getList(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_0), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new Location("membrane"), new AgentSite("d", null, "1")))), 100, NOT_LOCATED);

        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_0, INDEX_1), new AgentSite("d", null, "1"))), 100 }, 
                });
        
        initialValue = new InitialValue(getComplexes(getList(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_0), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new Location("membrane", INDEX_0, INDEX_1), new AgentSite("d", null, "1")))), 100, NOT_LOCATED);

        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_0, INDEX_1), new AgentSite("d", null, "1"))), 100 }, 
                });
        
        initialValue = new InitialValue(getComplexes(getList(new Agent("A", new Location("cytosol"), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new AgentSite("d", null, "1")))), 900, NOT_LOCATED);

        checkFixedLocatedComplexMap(initialValue, compartments, channels, new Object[][] { 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_0, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_0, INDEX_0), new AgentSite("d", null, "1"))), 100 }, 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_0, INDEX_1), new AgentSite("d", null, "1"))), 100 }, 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_2, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_0, INDEX_2), new AgentSite("d", null, "1"))), 100 }, 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_1, INDEX_0, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_1, INDEX_0), new AgentSite("d", null, "1"))), 100 }, 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_1, INDEX_1, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_1, INDEX_1), new AgentSite("d", null, "1"))), 100 }, 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_1, INDEX_2, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_1, INDEX_2), new AgentSite("d", null, "1"))), 100 }, 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_2, INDEX_0, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_2, INDEX_0), new AgentSite("d", null, "1"))), 100 }, 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_2, INDEX_1, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_2, INDEX_1), new AgentSite("d", null, "1"))), 100 }, 
                { new Complex(new Agent("A", new Location("cytosol", INDEX_2, INDEX_2, INDEX_0), 
                        new AgentSite("d", null, "1", "domainLink")), 
                        new Agent("B", new Location("membrane", INDEX_2, INDEX_2), new AgentSite("d", null, "1"))), 100 }, 
                });
        
        initialValue = new InitialValue(getComplexes(getList(new Agent("A", new Location("cytosol", INDEX_1, INDEX_2, INDEX_0), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new Location("membrane", INDEX_2, INDEX_2), new AgentSite("d", null, "1")))), 900, NOT_LOCATED);
        try {
            initialValue.getFixedLocatedComplexMap(compartments, channels);
            fail("Invalid locations should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        initialValue = new InitialValue(getComplexes(getList(new Agent("A", new Location("cytosol"), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new Location("cytosol"), new AgentSite("d", null, "1")))), 900, NOT_LOCATED);
        try {
            initialValue.getFixedLocatedComplexMap(compartments, channels);
            fail("Invalid locations should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        initialValue = new InitialValue(getComplexes(getList(new Agent("A", new Location("cytosol", INDEX_1, INDEX_2, INDEX_0), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new Location("cytosol"), new AgentSite("d", null, "1")))), 900, NOT_LOCATED);
        try {
            initialValue.getFixedLocatedComplexMap(compartments, channels);
            fail("Invalid locations should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
    }

    @Test
    public void testGetFixedLocatedComplexMap_multiCompartmentAgents_nestedCircles() {
        List<Compartment> compartments = getList(new Compartment.SolidCircle("cytosol", 21), new Compartment.OpenCircle("membrane", 25, 2));
        Channel channel = new Channel("domainLink");
        channel.addChannelComponent("Neighbour", getList(new Location("cytosol")), getList(new Location("membrane")));
        channel.addChannelComponent("Neighbour", getList(new Location("membrane")), getList(new Location("cytosol")));
        List<Channel> channels = getList(channel);
        
        InitialValue initialValue = new InitialValue(getComplexes(getList(new Agent("A", new Location("cytosol", INDEX_10, INDEX_20), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new AgentSite("d", null, "1")))), 100, NOT_LOCATED);

        checkFixedLocatedComplexMapByString(initialValue, compartments, channels, new Object[][] { 
                { "[A:cytosol[10][20](d!1:domainLink), B:membrane[13][23](d!1)]", 33 }, 
                { "[A:cytosol[10][20](d!1:domainLink), B:membrane[12][23](d!1)]", 34 }, 
                { "[A:cytosol[10][20](d!1:domainLink), B:membrane[11][23](d!1)]", 33 }, 
                });
        
    }

    @Test
    public void testGetFixedLocatedComplexMap_multiCompartmentAgents_nestedSpheres() {
        List<Compartment> compartments = getList(new Compartment.SolidSphere("cytosol", 21), new Compartment.OpenSphere("membrane", 25, 2));
        Channel channel = new Channel("domainLink");
        channel.addChannelComponent("Neighbour", getList(new Location("cytosol")), getList(new Location("membrane")));
        channel.addChannelComponent("Neighbour", getList(new Location("membrane")), getList(new Location("cytosol")));
        List<Channel> channels = getList(channel);
        
        InitialValue initialValue = new InitialValue(getComplexes(getList(new Agent("A", new Location("cytosol", INDEX_10, INDEX_10, INDEX_20), new AgentSite("d", null, "1", "domainLink")), 
                new Agent("B", new AgentSite("d", null, "1")))), 90, NOT_LOCATED);

        checkFixedLocatedComplexMapByString(initialValue, compartments, channels, new Object[][] { 
                { "[A:cytosol[10][10][20](d!1:domainLink), B:membrane[13][13][23](d!1)]", 10 }, 
                { "[A:cytosol[10][10][20](d!1:domainLink), B:membrane[12][13][23](d!1)]", 10 }, 
                { "[A:cytosol[10][10][20](d!1:domainLink), B:membrane[11][13][23](d!1)]", 10 }, 
                { "[A:cytosol[10][10][20](d!1:domainLink), B:membrane[13][12][23](d!1)]", 10 }, 
                { "[A:cytosol[10][10][20](d!1:domainLink), B:membrane[12][12][23](d!1)]", 10 }, 
                { "[A:cytosol[10][10][20](d!1:domainLink), B:membrane[11][12][23](d!1)]", 10 }, 
                { "[A:cytosol[10][10][20](d!1:domainLink), B:membrane[13][11][23](d!1)]", 10 }, 
                { "[A:cytosol[10][10][20](d!1:domainLink), B:membrane[12][11][23](d!1)]", 10 }, 
                { "[A:cytosol[10][10][20](d!1:domainLink), B:membrane[11][11][23](d!1)]", 10 }, 
                });
    }

    @Test
    public void testGetFixedLocatedComplexMap_originalAgentLinksUndamaged() {
        List<Compartment> compartments = getList(new Compartment("cytosol", 2));
        List<Channel> channels = new ArrayList<Channel>();

        List<Agent> agents = getList(
                new Agent("agent1", new AgentSite("x", null, "1")),
                new Agent("agent2", new AgentSite("x", null, "1")),
                new Agent("agent3", new AgentSite("x", null, "2")),
                new Agent("agent4", new AgentSite("x", null, "2")));
        InitialValue initialValue = new InitialValue(getComplexes(agents), 900, NOT_LOCATED);

        assertEquals(2, initialValue.complexes.size());
        for (Complex complex : initialValue.complexes) {
            assertEquals("Complex: " + complex, 1, complex.agentLinks.size());
        }

        initialValue.getFixedLocatedComplexMap(compartments, channels);
        
        assertEquals(2, initialValue.complexes.size());
        for (Complex complex : initialValue.complexes) {
            assertEquals("Complex: " + complex, 1, complex.agentLinks.size());
        }

        agents = getList(new Agent("agent1", new AgentSite("s", null, null)));
        initialValue = new InitialValue(getComplexes(agents), 900, new Location("cytosol", INDEX_0));

        assertEquals(1, initialValue.complexes.size());
        for (Complex complex : initialValue.complexes) {
            assertEquals("Complex: " + complex, 1, complex.agentLinks.size());
        }

        Map<Complex, Integer> result = initialValue.getFixedLocatedComplexMap(compartments, channels);
        
        assertEquals(1, initialValue.complexes.size());
        for (Complex complex : initialValue.complexes) {
            assertEquals("Complex: " + complex, 1, complex.agentLinks.size());
        }
        
        assertEquals(1, result.size());
        for (Complex complex : result.keySet()) {
            assertEquals("Complex: " + complex, 1, complex.agentLinks.size());
        }

    }
    

    
    private void checkFixedLocatedComplexMap(InitialValue initialValue, List<Compartment> compartments, 
            List<Channel> channels, Object[][] expectedQuantities) {
        
        Object[][] expectedStringQuantities = new Object[expectedQuantities.length][2];
        for (int index=0; index<expectedQuantities.length; index++) {
            expectedStringQuantities[index][0] = expectedQuantities[index][0].toString();
            expectedStringQuantities[index][1] = expectedQuantities[index][1];
        }
        checkFixedLocatedComplexMapByString(initialValue, compartments, channels, expectedStringQuantities);
    }
    
    private void checkFixedLocatedComplexMapByString(InitialValue initialValue, List<Compartment> compartments, 
            List<Channel> channels, Object[][] expectedQuantities) {
        Map<Complex, Integer> quantityMap = initialValue.getFixedLocatedComplexMap(compartments, channels);
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
    }

}
