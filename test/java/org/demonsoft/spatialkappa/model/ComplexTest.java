package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_1;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class ComplexTest {

    @SuppressWarnings("unused")
    @Test
    public void testComplex_collection() {
        try {
            new Complex((Collection<Agent>) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            new Complex(new ArrayList<Agent>());
            fail("empty collection should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent"));
        Complex complex = new Complex(agents);
        assertEquals(1, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertEquals("agent1", complex.getMatchHash());

        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));
        complex = new Complex(agents);
        assertEquals(2, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertSame(complex, complex.agents.get(1).getComplex());
        assertEquals("agent11agent21", complex.getMatchHash());

        agents = new ArrayList<Agent>();
        agents.add(new Agent("dimerAgent"));
        agents.add(new Agent("dimerAgent"));
        complex = new Complex(agents);
        assertEquals(2, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertSame(complex, complex.agents.get(1).getComplex());
        assertEquals("dimerAgent2", complex.getMatchHash());

        // Test canonical ordering
        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));
        complex = new Complex(agents);
        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent2"));
        agents.add(new Agent("agent1"));
        assertEquals(complex.toString(), new Complex(agents).toString());
        assertEquals(complex.getMatchHash(), new Complex(agents).getMatchHash());

    }

    @SuppressWarnings("unused")
    @Test
    public void testComplex_varargs() {
        try {
            new Complex((Agent) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            new Complex();
            fail("empty should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Agent agent1 = new Agent("agent1");
        Agent agent2 = new Agent("agent2");
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(agent1);
        Complex complex = new Complex(agent1);
        assertEquals(1, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertEquals("agent11", complex.getMatchHash());

        agents = new ArrayList<Agent>();
        agents.add(agent1);
        agents.add(agent2);
        complex = new Complex(agent1, agent2);
        assertEquals(2, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertSame(complex, complex.agents.get(1).getComplex());
        assertEquals("agent11agent21", complex.getMatchHash());

        agents = new ArrayList<Agent>();
        agents.add(agent1);
        agents.add(agent1);
        complex = new Complex(agent1, agent1);
        assertEquals(2, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertEquals("agent12", complex.getMatchHash());

        // Test canonical ordering
        assertEquals(new Complex(agent1, agent2).toString(), new Complex(agent2, agent1).toString());
    }

    @SuppressWarnings("unused")
    @Test
    public void testAgentLinks() {
        Complex complex = new Complex(new Agent("agent1"), new Agent("agent2"), new Agent("agent3"));
        assertEquals(0, complex.agentLinks.size());
        assertEquals("agent11agent21agent31", complex.getMatchHash());

        complex = new Complex(new Agent("agent1", new AgentSite("x", null, "1")), new Agent("agent2", new AgentSite("y", null, "1")));

        List<String[]> expected = new ArrayList<String[]>();
        expected.add(new String[] { "agent1", "x", "agent2", "y" });
        checkAgentLinks(complex, expected);
        assertEquals("agent11agent21agent1xagent2y1", complex.getMatchHash());

        complex = new Complex(new Agent("agent1", new AgentSite("x", null, "1"), new AgentSite("y", null, "2")), new Agent("agent2",
                new AgentSite("z", null, "2"), new AgentSite("y", null, "1")));

        expected = new ArrayList<String[]>();
        expected.add(new String[] { "agent1", "y", "agent2", "z" });
        expected.add(new String[] { "agent1", "x", "agent2", "y" });
        checkAgentLinks(complex, expected);
        assertEquals("agent11agent21agent1xagent2y1agent1yagent2z1", complex.getMatchHash());

        // Self binding
        complex = new Complex(new Agent("agent1", new AgentSite("x", null, "1"), new AgentSite("y", null, "1")));

        expected = new ArrayList<String[]>();
        expected.add(new String[] { "agent1", "x", "agent1", "y" });
        checkAgentLinks(complex, expected);
        assertEquals("agent11agent1xagent1y1", complex.getMatchHash());

        try {
            new Complex(new Agent("agent1", new AgentSite("x", null, "1"), new AgentSite("y", null, "3")), new Agent("agent2", new AgentSite(
                    "z", null, "2"), new AgentSite("y", null, "1")));
            fail("mismatched links should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            new Complex(new Agent("agent1", new AgentSite("x", null, "1")), new Agent("agent2", new AgentSite("z", null, "1"), new AgentSite(
                    "y", null, "1")));
            fail("mismatched links should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            new Complex(new Agent("agent1", new AgentSite("x", null, "1")), new Agent("agent1", new AgentSite("y", null, "1")), new Agent("agent2",
                    new AgentSite("z", null, "1")));
            fail("mismatched links should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

    }
    
    private void checkAgentLinks(Complex complex, List<String[]> expected) {
        List<AgentLink> actual = complex.agentLinks;
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            if (expected.get(index)[1].equals(actual.get(index).sourceSite.name)) {
                assertEquals(expected.get(index)[0], actual.get(index).sourceSite.agent.name);
                assertEquals(expected.get(index)[1], actual.get(index).sourceSite.name);
                assertEquals(expected.get(index)[2], actual.get(index).targetSite.agent.name);
                assertEquals(expected.get(index)[3], actual.get(index).targetSite.name);
            }
            else {
                assertEquals(expected.get(index)[2], actual.get(index).sourceSite.agent.name);
                assertEquals(expected.get(index)[3], actual.get(index).sourceSite.name);
                assertEquals(expected.get(index)[0], actual.get(index).targetSite.agent.name);
                assertEquals(expected.get(index)[1], actual.get(index).targetSite.name);
            }
        }
    }
    
    
    @Test
    public void testGetSingleLocation() {
        Complex complex = new Complex(
                new Agent("agent1", new AgentSite("x", null, "1")), 
                new Agent("agent2", new AgentSite("z", null, "1")));
        assertEquals(NOT_LOCATED, complex.getSingleLocation());
        
        complex = new Complex(
                new Agent("agent1", new Location("A"), new AgentSite("x", null, "1")), 
                new Agent("agent2", new Location("A"), new AgentSite("z", null, "1")));
        assertEquals(new Location("A"), complex.getSingleLocation());
        
        complex = new Complex(
                new Agent("agent1", new Location("A", INDEX_0), new AgentSite("x", null, "1")), 
                new Agent("agent2", new Location("A", INDEX_0), new AgentSite("z", null, "1")));
        assertEquals(new Location("A", INDEX_0), complex.getSingleLocation());
        
        complex = new Complex(
                new Agent("agent1", new Location("A"), new AgentSite("x", null, "1")), 
                new Agent("agent2", new Location("B"), new AgentSite("z", null, "1")));
        assertNull(complex.getSingleLocation());
        
        complex = new Complex(
                new Agent("agent1", new Location("A"), new AgentSite("x", null, "1")), 
                new Agent("agent2", new AgentSite("z", null, "1")));
        assertNull(complex.getSingleLocation());
        
        complex = new Complex(
                new Agent("agent1", new Location("A", INDEX_0), new AgentSite("x", null, "1")), 
                new Agent("agent2", new Location("A", INDEX_1), new AgentSite("z", null, "1")));
        assertNull(complex.getSingleLocation());
        
        complex = new Complex(
                new Agent("agent1", new Location("A", INDEX_0), new AgentSite("x", null, "1")), 
                new Agent("agent2", new Location("A"), new AgentSite("z", null, "1")));
        assertNull(complex.getSingleLocation());
        
        complex = new Complex(
                new Agent("agent1", new Location("A", INDEX_0), new AgentSite("x", null, "1")), 
                new Agent("agent2", new AgentSite("z", null, "1")));
        assertNull(complex.getSingleLocation());
    }
    
}
