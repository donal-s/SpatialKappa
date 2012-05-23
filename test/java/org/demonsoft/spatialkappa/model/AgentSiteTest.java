package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.junit.Test;

public class AgentSiteTest {

    @SuppressWarnings("unused")
    @Test
    public void testAgentInterface_singleState() {
        try {
            new AgentSite(null, "state", "link");
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        AgentSite site = new AgentSite("name", "state", "link");
        assertEquals("name", site.name);
        assertEquals("state", site.getState());
        assertEquals("link", site.getLinkName());
        assertEquals("name~state!link", site.toString());
        
        site = new AgentSite("name", "state", "_");
        assertEquals("name", site.name);
        assertEquals("state", site.getState());
        assertEquals("_", site.getLinkName());
        assertEquals("name~state!_", site.toString());

        site = new AgentSite("name", "state", "?");
        assertEquals("name", site.name);
        assertEquals("state", site.getState());
        assertEquals("?", site.getLinkName());
        assertEquals("name~state?", site.toString());

        site = new AgentSite("name", (String) null, null);
        assertEquals("name", site.name);
        assertEquals(null, site.getState());
        assertEquals(null, site.getLinkName());
        assertEquals("name", site.toString());
    }

    @Test
    public void testIsNamedLink() {
        assertTrue(new AgentSite("name", "state", "2").isNamedLink());
        assertFalse(new AgentSite("name", "state", null).isNamedLink());
        assertFalse(new AgentSite("name", "state", "_").isNamedLink());
        assertFalse(new AgentSite("name", "state", "?").isNamedLink());
    }

    @Test
    public void testIsAbstract() {
        assertFalse(new AgentSite("x", null, null).isAbstract());
        assertFalse(new AgentSite("x", "s", null).isAbstract());
        assertFalse(new AgentSite("x", null, "1").isAbstract());
        assertTrue(new AgentSite("x", null, "?").isAbstract());
        assertTrue(new AgentSite("x", null, "_").isAbstract());
    }

    @Test
    public void testEqualsHashCode() {
        Agent agent1 = new Agent("agent1");
        Agent agent2 = new Agent("agent2");
        AgentSite site = new AgentSite(agent1, "name", "state", "link");

        checkEqualsHashCode(site, new AgentSite(agent1, "name", "state", "link"), true);
        checkEqualsHashCode(site, new AgentSite(agent2, "name", "state", "link"), false);
        checkEqualsHashCode(site, new AgentSite(agent1, "name2", "state", "link"), false);
        checkEqualsHashCode(site, new AgentSite(agent1, "name", "state2", "link"), false);
        checkEqualsHashCode(site, new AgentSite(agent1, "name", "state", "link2"), false);
        checkEqualsHashCode(site, new AgentSite(null, "name", "state", "link"), false);
        checkEqualsHashCode(site, new AgentSite(agent1, "name2", null, "link"), false);
        checkEqualsHashCode(site, new AgentSite(agent1, "name", "state2", null), false);
        
        site = new AgentSite(null, "name", null, null);

        checkEqualsHashCode(site, new AgentSite(null, "name", null, null), true);
        checkEqualsHashCode(site, new AgentSite(agent1, "name", null, null), false);
        checkEqualsHashCode(site, new AgentSite(null, "name", "state", null), false);
        checkEqualsHashCode(site, new AgentSite(null, "name", null, "link"), false);
    }

    private void checkEqualsHashCode(AgentSite site1, AgentSite site2, boolean equal) {
        assertEquals(equal, site1.equals(site2));
        assertEquals(equal, site1.hashCode() == site2.hashCode());
    }
}
