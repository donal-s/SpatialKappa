package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.junit.Test;

public class AgentTest {

    @Test
    public void testAgent_nameOnly() {
        try {
            new Agent(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Agent agent = new Agent("name");
        assertEquals("name", agent.name);
        assertNull(agent.location);
        assertEquals("name", agent.toString());
        assertEquals("", agent.getStateHash());
        assertTrue(agent.getSites().isEmpty());
        
        agent = new Agent("name", (Location) null);
        assertEquals("name", agent.name);
        assertNull(agent.location);
        assertEquals("name", agent.toString());
        
        Location location = new Location("location");
        agent = new Agent("name", location);
        assertEquals("name", agent.name);
        assertSame(location, agent.location);
        assertEquals("name:location", agent.toString());
    }

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
        
        Agent agent = new Agent("name", sites);
        assertEquals("name", agent.name);
        assertNull(agent.location);
        assertEquals("name(site1~state1!link1,site2~state2!link2)", agent.toString());
        
        Set<AgentSite> expectedSites = new HashSet<AgentSite>();
        expectedSites.add(new AgentSite(agent, "site1", "state1", "link1"));
        expectedSites.add(new AgentSite(agent, "site2", "state2", "link2"));
        
        assertEquals(expectedSites, new HashSet<AgentSite>(agent.getSites()));
        
        for (AgentSite site : agent.getSites()) {
            assertSame(agent, site.agent);
        }
        
        agent = new Agent("name", null, sites);
        assertEquals("name", agent.name);
        assertNull(agent.location);
        assertEquals("name(site1~state1!link1,site2~state2!link2)", agent.toString());
        
        Location location = new Location("location");
        agent = new Agent("name", location, sites);
        assertEquals("name", agent.name);
        assertSame(location, agent.location);
        assertEquals("name:location(site1~state1!link1,site2~state2!link2)", agent.toString());
    }

    @Test
    public void testAgent_nameAndInterfaces_varArgs() {
        AgentSite site1 = new AgentSite("site1", "state1", "link1");
        AgentSite site2 = new AgentSite("site2", "state2", "link2");
        
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
        
        Agent agent = new Agent("name", site1, site2);
        assertEquals("name(site1~state1!link1,site2~state2!link2)", agent.toString());
        assertEquals("name", agent.name);
        assertNull(agent.location);
        
        Set<AgentSite> expectedSites = new HashSet<AgentSite>();
        expectedSites.add(new AgentSite(agent, site1));
        expectedSites.add(new AgentSite(agent, site2));
        
        assertEquals(expectedSites, new HashSet<AgentSite>(agent.getSites()));
        
        for (AgentSite site : agent.getSites()) {
            assertSame(agent, site.agent);
        }
        
        agent = new Agent("name", (Location) null, site1, site2);
        assertEquals("name(site1~state1!link1,site2~state2!link2)", agent.toString());
        assertEquals("name", agent.name);
        assertNull(agent.location);
        
        Location location = new Location("location");
        agent = new Agent("name", location, site1, site2);
        assertEquals("name", agent.name);
        assertSame(location, agent.location);
        assertEquals("name:location(site1~state1!link1,site2~state2!link2)", agent.toString());
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
        	new Agent("name").toString(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Agent agent = new Agent("name");
        assertEquals("name", agent.toString());
        assertEquals("name(suffix)", agent.toString("suffix"));

        Set<AgentSite> sites = new HashSet<AgentSite>();
        sites.add(new AgentSite("site1", "state1", "link1"));
        sites.add(new AgentSite("site2", "state2", "link2"));
        
        agent = new Agent("name", sites);
        assertEquals("name(site1~state1!link1,site2~state2!link2)", agent.toString());
        assertEquals("name(site1~state1!link1,site2~state2!link2,suffix)", agent.toString("suffix"));
    }


}
