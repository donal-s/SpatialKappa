package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.*;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class AgentTest {

    @SuppressWarnings("unused")
    @Test
    public void testAgent_nameOnly() {
        try {
            new Agent(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Agent(null, (Location) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Agent agent = new Agent("name");
        assertEquals("name", agent.name);
        assertSame(NOT_LOCATED, agent.location);
        assertEquals("name()", agent.toString());
        assertEquals("", agent.getStateHash());
        assertTrue(agent.getSites().isEmpty());
        
        Location location = new Location("location");
        agent = new Agent("name", location);
        assertEquals("name", agent.name);
        assertSame(location, agent.location);
        assertEquals("name:location()", agent.toString());
    }

    @SuppressWarnings("unused")
    @Test
    public void testAgent_nameAndInterfaces() {
        Set<AgentSite> sites = new HashSet<AgentSite>();
        sites.add(new AgentSite("site1", "state1", "link1"));
        sites.add(new AgentSite("site2", "state2", "link2"));
        
        try {
            new Agent(null, sites);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Agent("name", (Collection<AgentSite>) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Agent("name", null, sites);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Agent agent = new Agent("name", sites);
        assertEquals("name", agent.name);
        assertSame(NOT_LOCATED, agent.location);
        assertEquals("name(site1~state1!link1,site2~state2!link2)", agent.toString());
        
        Set<AgentSite> expectedSites = new HashSet<AgentSite>();
        expectedSites.add(new AgentSite(agent, "site1", "state1", "link1"));
        expectedSites.add(new AgentSite(agent, "site2", "state2", "link2"));
        
        assertEquals(expectedSites, new HashSet<AgentSite>(agent.getSites()));
        
        for (AgentSite site : agent.getSites()) {
            assertSame(agent, site.agent);
        }
        
        Location location = new Location("location");
        agent = new Agent("name", location, sites);
        assertEquals("name", agent.name);
        assertSame(location, agent.location);
        assertEquals("name:location(site1~state1!link1,site2~state2!link2)", agent.toString());
    }

    @Test
    public void testClone() {
        Set<AgentSite> sites = new HashSet<AgentSite>();
        sites.add(new AgentSite("site1", "state1", "link1"));
        sites.add(new AgentSite("site2", "state2", "link2"));
       
        Location location = new Location("location");
        Agent agent = new Agent("name", location, sites);
        
        Agent clone = agent.clone();
        assertEquals(agent.name, clone.name);
        assertSame(agent.location, clone.location);
        assertEquals(agent.toString(), clone.toString());
    }
    
    
    @SuppressWarnings("unused")
    @Test
    public void testAgent_nameAndInterfaces_varArgs() {
        AgentSite site1 = new AgentSite("site1", "state1", "link1");
        AgentSite site2 = new AgentSite("site2", "state2", "?");
        
        try {
            new Agent(null, site1, site2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Agent("name", (AgentSite[]) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Agent("name", (Location) null, site1, site2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Agent agent = new Agent("name", site1, site2);
        assertEquals("name(site1~state1!link1,site2~state2?)", agent.toString());
        assertEquals("name", agent.name);
        assertSame(NOT_LOCATED, agent.location);
        
        Set<AgentSite> expectedSites = new HashSet<AgentSite>();
        expectedSites.add(new AgentSite(agent, site1));
        expectedSites.add(new AgentSite(agent, site2));
        
        assertEquals(expectedSites, new HashSet<AgentSite>(agent.getSites()));
        
        for (AgentSite site : agent.getSites()) {
            assertSame(agent, site.agent);
        }
        
        Location location = new Location("location");
        agent = new Agent("name", location, site1, site2);
        assertEquals("name", agent.name);
        assertSame(location, agent.location);
        assertEquals("name:location(site1~state1!link1,site2~state2?)", agent.toString());
    }

    @Test
    public void testSetLocation() {
        Agent agent = new Agent("name");
        assertSame(NOT_LOCATED, agent.location);

        try {
            agent.setLocation(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Location location = new Location("location");
        agent.setLocation(location);
        assertSame(location, agent.location);
        
        agent.setLocation(NOT_LOCATED);
        assertSame(NOT_LOCATED, agent.location);
    }

    @Test
    public void testGetLocatedAgents() {
        AgentSite site1 = new AgentSite("site1", "state1", "link1");
        AgentSite site2 = new AgentSite("site2", "state2", "?");
        
        Location location = new Location("unknown");
        Agent agent = new Agent("name", location, site1, site2);
        List<Compartment> compartments = new ArrayList<Compartment>();
        
        try {
            agent.getLocatedAgents(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            agent.getLocatedAgents(compartments);
            fail("unknown location should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        // No compartments
        agent.location = NOT_LOCATED;
        checkGetLocatedAgents(agent, compartments, agent);
        
        // With compartments
        compartments.add(new Compartment("nucleus"));
        compartments.add(new Compartment("cytosol", 2, 2));

        checkGetLocatedAgents(agent, compartments, 
                new Agent("name", new Location("nucleus"), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_0, INDEX_0), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_1, INDEX_0), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_0, INDEX_1), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_1, INDEX_1), site1, site2)
                );

        agent.location = new Location("nucleus");
        checkGetLocatedAgents(agent, compartments, agent);

        agent.location = new Location("cytosol", INDEX_1, INDEX_1);
        checkGetLocatedAgents(agent, compartments, agent);

        agent.location = new Location("cytosol");
        checkGetLocatedAgents(agent, compartments, 
                new Agent("name", new Location("cytosol", INDEX_0, INDEX_0), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_1, INDEX_0), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_0, INDEX_1), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_1, INDEX_1), site1, site2)
                );

        // Using wildcards
        agent.location = new Location("cytosol", WILDCARD, INDEX_1);
        checkGetLocatedAgents(agent, compartments, 
                new Agent("name", new Location("cytosol", INDEX_0, INDEX_1), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_1, INDEX_1), site1, site2)
                );

        agent.location = new Location("cytosol", INDEX_1, WILDCARD);
        checkGetLocatedAgents(agent, compartments, 
                new Agent("name", new Location("cytosol", INDEX_1, INDEX_0), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_1, INDEX_1), site1, site2)
                );

        agent.location = new Location("cytosol", WILDCARD, WILDCARD);
        checkGetLocatedAgents(agent, compartments, 
                new Agent("name", new Location("cytosol", INDEX_0, INDEX_0), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_1, INDEX_0), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_0, INDEX_1), site1, site2),
                new Agent("name", new Location("cytosol", INDEX_1, INDEX_1), site1, site2)
                );
    }

    private void checkGetLocatedAgents(Agent inputAgent, List<Compartment> compartments, Agent... expectedAgents) {
        List<Agent> result = inputAgent.getLocatedAgents(compartments);
        assertEquals(expectedAgents.length, result.size());
        for (int i = 0; i < expectedAgents.length; i++) {
            Agent expectedAgent = expectedAgents[i];
            Agent actualAgent = result.get(i);
            assertEquals(expectedAgent.toString(), actualAgent.toString());
        }
    }
    
    @Test
    public void testGetStateHash() {
        AgentSite site1 = new AgentSite("site1", "state1", "link1");
        AgentSite site2 = new AgentSite("site2", "state2", "link2");
        
        Agent agent = new Agent("name", site1, site2);
        assertEquals("site1~state1,site2~state2,", agent.getStateHash());

        agent.getSite("site1").setState(null);
        agent.getSite("site2").setState("new");
        assertEquals("site1,site2~new,", agent.getStateHash());
    }
    
    @Test
    public void testHasLink() {
        try {
            new Agent("name").hasLink(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Agent agent = new Agent("name", new AgentSite("site1", "state1", "link1"), new AgentSite("site2", "state2", "link2"));
        
        assertTrue(agent.hasLink("link1"));
        assertTrue(agent.hasLink("link2"));
        assertFalse(agent.hasLink("unknown"));
        assertFalse(agent.hasLink("state1"));
    }

    
    @Test
    public void testGetSite() {
        AgentSite site1 = new AgentSite("site1", "state1", "link1");
        AgentSite site2 = new AgentSite("site2", "state2", "link2");
        Agent agent = new Agent("name", site1, site2);
        
        try {
            agent.getSite(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        checkSitesEquivalent(site1, agent, agent.getSite("site1"));
        checkSitesEquivalent(site2, agent, agent.getSite("site2"));
        assertNull(agent.getSite("unknown"));
        assertNull(agent.getSite(""));
    }

    private void checkSitesEquivalent(AgentSite expected, Agent expectedAgent, AgentSite actual) {
        assertSame(expectedAgent, actual.agent);
        assertSame(expected.name, actual.name);
        assertSame(expected.getLinkName(), actual.getLinkName());
        assertSame(expected.getState(), actual.getState());
    }
    
    @Test
    public void testToString() {
        try {
        	new Agent("name").toString(null, true);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Agent agent = new Agent("name");
        assertEquals("name()", agent.toString());
        assertEquals("name(suffix)", agent.toString("suffix", true));

        Set<AgentSite> sites = new HashSet<AgentSite>();
        sites.add(new AgentSite("site1", "state1", "link1"));
        sites.add(new AgentSite("site2", "state2", "?"));
        
        agent = new Agent("name", sites);
        assertEquals("name(site1~state1!link1,site2~state2?)", agent.toString());
        assertEquals("name(site1~state1!link1,site2~state2?,suffix)", agent.toString("suffix", true));
    }


}
