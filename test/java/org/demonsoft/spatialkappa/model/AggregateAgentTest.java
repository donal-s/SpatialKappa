package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class AggregateAgentTest {

    @SuppressWarnings("unused")
    @Test
    public void testAggregateAgent() {
        try {
            new AggregateAgent(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        AggregateAgent agent = new AggregateAgent("name");
        assertEquals("name", agent.getName());
        assertTrue(agent.getSites().isEmpty());
    }

    @SuppressWarnings("unused")
    @Test
    public void testAggregateAgent_nameAndInterfaces_varArgs() {
        Collection<AggregateSite> interfaces = new ArrayList<AggregateSite>();
        interfaces.add(new AggregateSite("interface1", "state1", "link1"));
        interfaces.add(new AggregateSite("interface2", "state2", "link2"));
        
        try {
            new AggregateAgent(null, new AggregateSite("interface1", "state1", "link1"), new AggregateSite("interface2", "state2", "link2"));
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new AggregateAgent("name", (AggregateSite) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        AggregateAgent agent = new AggregateAgent("name", new AggregateSite("interface1", "state1", "link1"), new AggregateSite("interface2", "state2", "link2"));
        assertEquals("name", agent.getName());
        assertEquals(2, agent.getSites().size());
        assertTrue(agent.getSites().containsAll(interfaces));
    }

    @Test
    public void testAddInterfaces() {
        try {
            new AggregateAgent("name").addSites(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        AggregateAgent agent = new AggregateAgent("name", new AggregateSite("interface1", "state1", "link1"), new AggregateSite("interface2", "state2", "link2"));

        List<AggregateSite> expected = new ArrayList<AggregateSite>();
        expected.add(new AggregateSite("interface1", "state1", "link1"));
        expected.add(new AggregateSite("interface2", "state2", "link2"));

        assertEquals(expected, agent.getSites());
        
        // Add some interfaces
        
        ArrayList<AgentSite> interfaces = new ArrayList<AgentSite>();
        interfaces.add(new AgentSite("interface3", "state3", "link3"));
        interfaces.add(new AgentSite("interface1", "state4", "link4"));
        interfaces.add(new AgentSite("interface1", "state1", "link1"));
        interfaces.add(new AgentSite("interface1", null, "link5"));
        interfaces.add(new AgentSite("interface2", "state6", null));
        agent.addSites(interfaces);

        expected.clear();
        expected.add(new AggregateSite("interface1", new String[] {"state1", "state4"}, new String[] {"link1", "link4", "link5"}));
        expected.add(new AggregateSite("interface2", new String[] {"state2", "state6"}, new String[] {"link2"}));
        expected.add(new AggregateSite("interface3", "state3", "link3"));
        
        assertEquals(expected, agent.getSites());
    }

}
