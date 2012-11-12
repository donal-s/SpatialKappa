package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.WILDCARD;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.demonsoft.spatialkappa.model.Complex.MappingInstance;
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
        assertEquals("agent##NOT LOCATED##1", complex.getMatchHash());

        agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));
        complex = new Complex(agents);
        assertEquals(2, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertSame(complex, complex.agents.get(1).getComplex());
        assertEquals("agent1##NOT LOCATED##1agent2##NOT LOCATED##1", complex.getMatchHash());

        agents = new ArrayList<Agent>();
        agents.add(new Agent("dimerAgent"));
        agents.add(new Agent("dimerAgent"));
        complex = new Complex(agents);
        assertEquals(2, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertSame(complex, complex.agents.get(1).getComplex());
        assertEquals("dimerAgent##NOT LOCATED##2", complex.getMatchHash());

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
        assertEquals("agent1##NOT LOCATED##1", complex.getMatchHash());

        agents = new ArrayList<Agent>();
        agents.add(agent1);
        agents.add(agent2);
        complex = new Complex(agent1, agent2);
        assertEquals(2, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertSame(complex, complex.agents.get(1).getComplex());
        assertEquals("agent1##NOT LOCATED##1agent2##NOT LOCATED##1", complex.getMatchHash());

        agents = new ArrayList<Agent>();
        agents.add(agent1);
        agents.add(agent1);
        complex = new Complex(agent1, agent1);
        assertEquals(2, complex.agents.size());
        assertEquals(agents, complex.agents);
        assertSame(complex, complex.agents.get(0).getComplex());
        assertEquals("agent1##NOT LOCATED##2", complex.getMatchHash());

        // Test canonical ordering
        assertEquals(new Complex(agent1, agent2).toString(), new Complex(agent2, agent1).toString());
    }

    @SuppressWarnings("unused")
    @Test
    public void testAgentLinks() {
        Complex complex = new Complex(new Agent("agent1"), new Agent("agent2"), new Agent("agent3"));
        assertEquals(0, complex.agentLinks.size());
        assertEquals("agent1##NOT LOCATED##1agent2##NOT LOCATED##1agent3##NOT LOCATED##1", complex.getMatchHash());

        complex = new Complex(new Agent("agent1", new AgentSite("x", null, "1")), new Agent("agent2", new AgentSite("y", null, "1")));

        List<String[]> expected = new ArrayList<String[]>();
        expected.add(new String[] { "agent1", "x", "agent2", "y" });
        checkAgentLinks(complex, expected);
        assertEquals("agent1##NOT LOCATED##1agent2##NOT LOCATED##1agent1xagent2y1", complex.getMatchHash());

        complex = new Complex(new Agent("agent1", new AgentSite("x", null, "1"), new AgentSite("y", null, "2")), new Agent("agent2",
                new AgentSite("z", null, "2"), new AgentSite("y", null, "1")));

        expected = new ArrayList<String[]>();
        expected.add(new String[] { "agent1", "y", "agent2", "z" });
        expected.add(new String[] { "agent1", "x", "agent2", "y" });
        checkAgentLinks(complex, expected);
        assertEquals("agent1##NOT LOCATED##1agent2##NOT LOCATED##1agent1xagent2y1agent1yagent2z1", complex.getMatchHash());

        // Self binding
        complex = new Complex(new Agent("agent1", new AgentSite("x", null, "1"), new AgentSite("y", null, "1")));

        expected = new ArrayList<String[]>();
        expected.add(new String[] { "agent1", "x", "agent1", "y" });
        checkAgentLinks(complex, expected);
        assertEquals("agent1##NOT LOCATED##1agent1xagent1y1", complex.getMatchHash());

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
    
    @Test
    public void testGetMappingInstances() throws Exception {
        
        List<Agent> agents = Utils.getList(new Agent("A", new Location("loc1")));
        Complex complex = new Complex(agents);
        
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(new Channel("intra", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1)));
        channels.add(new Channel("inter", new Location("loc1"), new Location("loc2", INDEX_2)));
        
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("loc1"));
        compartments.add(new Compartment("loc2", 5));
        
        try {
            complex.getMappingInstances(null, channels);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            complex.getMappingInstances(compartments, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // Simple agent - fixed location
        agents = getList(new Agent("A", new Location("loc1")));
        complex = new Complex(agents);
        
        List<List<Agent>> expected = new ArrayList<List<Agent>>();
        expected.add(getList(new Agent("A", new Location("loc1"))));
        
        checkMappingInstances(complex, compartments, channels, expected);

        agents = getList(new Agent("A", new Location("loc2", INDEX_1)));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1))));
        
        checkMappingInstances(complex, compartments, channels, expected);
        

        // Simple agent - no location
        agents = getList(new Agent("A"));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_2))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("4")))));
        
        checkMappingInstances(complex, compartments, channels, expected);
        
        
        // Simple agent - partial location

        agents = getList(new Agent("A", new Location("loc2")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_2))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("4")))));
               
        checkMappingInstances(complex, compartments, channels, expected);

        
        // Dimer - one agent fixed, other same compartment
        agents = getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc1"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc1"), new AgentSite("s", null, "1"))));
        
        checkMappingInstances(complex, compartments, channels, expected);

        agents = getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))));
        
        checkMappingInstances(complex, compartments, channels, expected);
        

        // Dimer - one agent no location, other same compartment
        agents = getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc1"), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1"))));
        
        checkMappingInstances(complex, compartments, channels, expected);
        
        
        // Dimer - one agent partial location, other same compartment

        agents = getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1"))));
               
        checkMappingInstances(complex, compartments, channels, expected);

        // Dimer - one agent fixed, other using channel
        agents = getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1", "inter")),
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1", "inter")),
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))));
        
        checkMappingInstances(complex, compartments, channels, expected);

        agents = getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1", "intra")),
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1", "intra")),
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))));
        
        checkMappingInstances(complex, compartments, channels, expected);
        

        // Dimer - one agent partial location, other using channel

        agents = getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1"))));
               
        checkMappingInstances(complex, compartments, channels, expected);

        // Dimer - one agent partial location, other using channel, 2 dimensions

        channels.clear();
        // Ensure more than the first pair get processed
        Channel channel = new Channel("horiz");
        channel.addChannelComponent(null, getList(new Location("loc2", new CellIndexExpression("100"), INDEX_0)), 
                getList(new Location("loc2", new CellIndexExpression("101"), INDEX_0)));
        channel.addChannelComponent(null, getList(new Location("loc2", INDEX_X, INDEX_Y)), 
                getList(new Location("loc2", INDEX_X_MINUS_1, INDEX_Y)));
        channel.addChannelComponent(null, getList(new Location("loc2", INDEX_X, INDEX_Y)), 
                getList(new Location("loc2", INDEX_X_PLUS_1, INDEX_Y)));
        channels.add(channel);
        
        compartments.clear();
        compartments.add(new Compartment("loc2", 2, 2));

        agents = getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1", "horiz")), 
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0, INDEX_0), new AgentSite("s", null, "1", "horiz")), 
                new Agent("B", new Location("loc2", INDEX_1, INDEX_0), new AgentSite("s", null, "1"))));
        expected.add(getList(
                new Agent("A", new Location("loc2", INDEX_1, INDEX_0), new AgentSite("s", null, "1", "horiz")),
                new Agent("B", new Location("loc2", INDEX_0, INDEX_0), new AgentSite("s", null, "1"))
                ));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0, INDEX_1), new AgentSite("s", null, "1", "horiz")), 
                new Agent("B", new Location("loc2", INDEX_1, INDEX_1), new AgentSite("s", null, "1"))));
        expected.add(getList(
                new Agent("A", new Location("loc2", INDEX_1, INDEX_1), new AgentSite("s", null, "1", "horiz")),
                new Agent("B", new Location("loc2", INDEX_0, INDEX_1), new AgentSite("s", null, "1"))
                ));
               
        checkMappingInstances(complex, compartments, channels, expected);
        
        // TODO test predefined channel types

    }
    
    @Test
    public void testGetMappingInstances_nestedCompartmentComplexes() throws Exception {
        
        Channel channel = new Channel("domainLink");
        channel.addChannelComponent("EdgeNeighbour", getList(new Location("inner")), getList(new Location("outer")));
        channel.addChannelComponent("EdgeNeighbour", getList(new Location("outer")), getList(new Location("inner")));
        List<Channel> channels = getList(channel);
        
        List<Compartment> compartments = getList(new Compartment.SolidCircle("inner", 3), 
                new Compartment.OpenCircle("outer", 7, 2));

        List<Agent> agents = getList(new Agent("A", new Location("inner"), new AgentSite("s", null, "1", "domainLink")), 
                new Agent("B", new Location("outer"), new AgentSite("s", null, "1")));
        Complex complex = new Complex(agents);
        
        List<String> expected = getList(
                "[A:inner[0][0](s!1:domainLink), B:outer[1][2](s!1)]",
                "[A:inner[0][0](s!1:domainLink), B:outer[2][1](s!1)]",
                "[A:inner[1][0](s!1:domainLink), B:outer[3][1](s!1)]",
                "[A:inner[2][0](s!1:domainLink), B:outer[4][1](s!1)]",
                "[A:inner[2][0](s!1:domainLink), B:outer[5][2](s!1)]",
                "[A:inner[0][1](s!1:domainLink), B:outer[1][3](s!1)]",
                "[A:inner[2][1](s!1:domainLink), B:outer[5][3](s!1)]",
                "[A:inner[0][2](s!1:domainLink), B:outer[1][4](s!1)]",
                "[A:inner[0][2](s!1:domainLink), B:outer[2][5](s!1)]",
                "[A:inner[1][2](s!1:domainLink), B:outer[3][5](s!1)]",
                "[A:inner[2][2](s!1:domainLink), B:outer[4][5](s!1)]",
                "[A:inner[2][2](s!1:domainLink), B:outer[5][4](s!1)]"
                );
               
        checkMappingInstancesByString(complex, compartments, channels, expected);
        
        // Corner
        
        agents = getList(new Agent("A", new Location("inner", INDEX_0, INDEX_0), new AgentSite("s", null, "1", "domainLink")), 
                new Agent("B", new Location("outer"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected = getList(
                "[A:inner[0][0](s!1:domainLink), B:outer[1][2](s!1)]",
                "[A:inner[0][0](s!1:domainLink), B:outer[2][1](s!1)]");
               
        checkMappingInstancesByString(complex, compartments, channels, expected);
        
        // Edge
        
        agents = getList(new Agent("A", new Location("inner", INDEX_0, INDEX_1), new AgentSite("s", null, "1", "domainLink")), 
                new Agent("B", new Location("outer"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected = getList("[A:inner[0][1](s!1:domainLink), B:outer[1][3](s!1)]");
               
        checkMappingInstancesByString(complex, compartments, channels, expected);
        
        // Centre
        
        agents = getList(new Agent("A", new Location("inner", INDEX_1, INDEX_1), new AgentSite("s", null, "1", "domainLink")), 
                new Agent("B", new Location("outer"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        checkMappingInstancesByString(complex, compartments, channels, new ArrayList<String>());
        
        // Outer compartment located
        
        agents = getList(new Agent("A", new Location("inner"), new AgentSite("s", null, "1", "domainLink")), 
                new Agent("B", new Location("outer", INDEX_1, INDEX_2), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected = getList("[A:inner[0][0](s!1:domainLink), B:outer[1][2](s!1)]");
               
        checkMappingInstancesByString(complex, compartments, channels, expected);
        
        // Invalid location
        
        agents = getList(new Agent("A", new Location("inner", INDEX_0, INDEX_0), new AgentSite("s", null, "1", "domainLink")), 
                new Agent("B", new Location("outer", INDEX_0, INDEX_2), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        checkMappingInstancesByString(complex, compartments, channels, new ArrayList<String>());
    }
    
    private void checkMappingInstances(Complex complex, List<Compartment> compartments, List<Channel> channels,
            List<List<Agent>> expected) {
        List<MappingInstance> result = complex.getMappingInstances(compartments, channels);
        assertEquals(expected.size(), result.size());
        
        for (int index=0; index < expected.size(); index++) {
            List<Agent> expectedAgents = expected.get(index);
            List<Agent> actualAgents = result.get(index).locatedAgents;
            
            assertEquals(expectedAgents.toString(), actualAgents.toString());
        }
    }

    private void checkMappingInstancesByString(Complex complex, List<Compartment> compartments, List<Channel> channels,
            List<String> expected) {
        List<MappingInstance> result = complex.getMappingInstances(compartments, channels);
        assertEquals(expected.size(), result.size());
        
        for (int index=0; index < expected.size(); index++) {
            List<Agent> actualAgents = result.get(index).locatedAgents;
            
            assertEquals(expected.get(index), actualAgents.toString());
        }
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
            complex.getFixedAgents(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Agent> expected = new ArrayList<Agent>();
        expected.add(agent2);
        expected.add(agent4);
        assertEquals(expected, complex.getFixedAgents(compartments));
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
        remainingLinks.removeAll(Complex.getInternalLinks(remainingLinks, fixedAgents));
        
        try {
            Complex.chooseNextAgent(null, remainingAgents, remainingLinks);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            Complex.chooseNextAgent(fixedAgents, null, remainingLinks);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            Complex.chooseNextAgent(fixedAgents, remainingAgents, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            Complex.chooseNextAgent(new ArrayList<Agent>(), remainingAgents, remainingLinks);
            fail("no linked agent to choose should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            Complex.chooseNextAgent(fixedAgents, new ArrayList<Agent>(), remainingLinks);
            fail("no linked agent to choose should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            Complex.chooseNextAgent(fixedAgents, remainingAgents, new ArrayList<AgentLink>());
            fail("no linked agent to choose should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        assertEquals(agent2, Complex.chooseNextAgent(fixedAgents, remainingAgents, remainingLinks));

        fixedAgents.add(agent2);
        remainingAgents.remove(agent2);
        remainingLinks.removeAll(Complex.getInternalLinks(remainingLinks, fixedAgents));
        
        assertEquals(agent3, Complex.chooseNextAgent(fixedAgents, remainingAgents, remainingLinks));
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
            Complex.getInternalLinks(null, agents);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            Complex.getInternalLinks(complex.agentLinks, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Agent> input = new ArrayList<Agent>();
        
        List<AgentLink> expected = new ArrayList<AgentLink>();
        assertEquals(expected, Complex.getInternalLinks(complex.agentLinks, input));
        
        input.add(agent1);
        expected.add(complex.getAgentLink(agent1, "s2"));
        assertEquals(expected, Complex.getInternalLinks(complex.agentLinks, input));
        
        input.add(agent2);
        expected.add(complex.getAgentLink(agent1, "s1"));
        assertEquals(expected, Complex.getInternalLinks(complex.agentLinks, input));
    }
    
    
    @Test
    public void testGetPossibleLocations() throws Exception {
        
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("loc1"));
        compartments.add(new Compartment("loc2", 5));
        
        try {
            Complex.getPossibleLocations(null, NOT_LOCATED, null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            Complex.getPossibleLocations(NOT_LOCATED, null, null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            Complex.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, null, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // No location or channel - stay in same location
        List<Location> expected = new ArrayList<Location>();
        expected.add(NOT_LOCATED);
        assertEquals(expected, Complex.getPossibleLocations(NOT_LOCATED, NOT_LOCATED, null, compartments));
        
        // No channel - stay in same location
        expected = new ArrayList<Location>();
        expected.add(new Location("loc1"));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc1"), NOT_LOCATED, null, compartments));

        expected.clear();
        expected.add(new Location("loc2", INDEX_1));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, null, compartments));
        
        // No channel, but now constraints
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_1), null, compartments));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2"), null, compartments));
        
        expected.clear();
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_2), null, compartments));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc1"), null, compartments));

        // No channel, but wildcard constraints
        expected.add(new Location("loc2", INDEX_1));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", WILDCARD), null, compartments));

        // Matching channel - no constraints
        Channel channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc2", INDEX_2));
        expected.clear();
        expected.add(new Location("loc2", INDEX_2));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, channel, compartments));
        
        channel = new Channel("label", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, channel, compartments));
        
        channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc1"));
        expected.clear();
        expected.add(new Location("loc1"));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), NOT_LOCATED, channel, compartments));
        
        // Matching channel - constraints
        channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc2", INDEX_2));
        expected.clear();
        expected.add(new Location("loc2", INDEX_2));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_2), channel, compartments));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2"), channel, compartments));
        
        expected.clear();
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_1), channel, compartments));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc1"), channel, compartments));
        
        // Matching channel - wildcard constraints
        channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc2", INDEX_2));
        expected.clear();
        expected.add(new Location("loc2", INDEX_2));
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", WILDCARD), channel, compartments));
        
        // Non matching channel
        channel = new Channel("label", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1));
        expected.clear();
        assertEquals(expected, Complex.getPossibleLocations(new Location("loc2", new CellIndexExpression("4")), NOT_LOCATED, channel, compartments));

        assertEquals(expected, Complex.getPossibleLocations(new Location("loc1"), NOT_LOCATED, channel, compartments));
    
    }
    


}
