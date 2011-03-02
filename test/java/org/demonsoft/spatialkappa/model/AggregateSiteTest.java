package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.AggregateSite;
import org.junit.Test;

public class AggregateSiteTest {

    @Test
    public void testAggregateInterface_singleState() {
        try {
            new AggregateSite(null, "state", "link");
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        AggregateSite interface1 = new AggregateSite("name", "state", "link");
        assertEquals("name", interface1.getName());
        assertEquals(1, interface1.states.size());
        assertEquals(1, interface1.links.size());
        assertTrue(interface1.states.contains("state"));
        assertTrue(interface1.links.contains("link"));

        interface1 = new AggregateSite("name", (String) null, null);
        assertEquals("name", interface1.getName());
        assertEquals(0, interface1.states.size());
        assertEquals(0, interface1.links.size());
    }

    @Test
    public void testAggregateInterface_fromAgentInterface() {
        AgentSite agentInterface = new AgentSite("name", "state", "link");

        try {
            new AggregateSite(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        AggregateSite interface1 = new AggregateSite(agentInterface);
        assertEquals("name", interface1.getName());
        assertEquals(1, interface1.states.size());
        assertEquals(1, interface1.links.size());
        assertTrue(interface1.states.contains("state"));
        assertTrue(interface1.links.contains("link"));

        interface1 = new AggregateSite(new AgentSite("name", (String) null, null));
        assertEquals("name", interface1.getName());
        assertEquals(0, interface1.states.size());
        assertEquals(0, interface1.links.size());
    }

    @Test
    public void testAggregateInterface_multipleStates() {
        String[] states = {"state1", "state2"};
        String[] links = {"link1", "link2"};

        try {
            new AggregateSite(null, states, links);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        AggregateSite interface1 = new AggregateSite("name", states, links);
        assertEquals("name", interface1.getName());
        assertEquals(2, interface1.states.size());
        assertEquals(2, interface1.links.size());
        assertTrue(interface1.states.contains("state1"));
        assertTrue(interface1.states.contains("state2"));
        assertTrue(interface1.links.contains("link1"));
        assertTrue(interface1.links.contains("link2"));

        interface1 = new AggregateSite("name", (String[]) null, null);
        assertEquals("name", interface1.getName());
        assertEquals(0, interface1.states.size());
        assertEquals(0, interface1.links.size());
    }

    @Test
    public void testMerge() {
        try {
            new AggregateSite("name", "state", null).merge(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        AggregateSite interface1 = new AggregateSite("name", "state1", "link1");
        interface1.merge(new AgentSite("", "state1", "link1"));
        interface1.merge(new AgentSite("", "state2", "link2"));
        interface1.merge(new AgentSite("", null, "link5"));
        interface1.merge(new AgentSite("", "state6", null));
        interface1.merge(new AgentSite("", (String) null, null));
        
        Set<String> expectedStates = new HashSet<String>(Arrays.asList(new String[] {"state1", "state2", "state6"}));
        Set<String> expectedLinks = new HashSet<String>(Arrays.asList(new String[] {"link1", "link2", "link5"}));

        assertEquals(expectedStates, interface1.states);
        assertEquals(expectedLinks, interface1.links);
    }

}
