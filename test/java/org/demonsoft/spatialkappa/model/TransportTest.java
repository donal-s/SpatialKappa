package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.Transport;
import org.junit.Test;

public class TransportTest extends TransitionTest {

    @Test
    public void testTransport_invalid() {
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));

        try {
            new Transport("label", null, agents, "0.1");
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Transport("label", "linkName", agents, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            new Transport("label", "linkName", agents, "");
            fail("unknown rate should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            new Transport("label", "linkName", agents, "a");
            fail("unknown rate should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
    }

    @Test
    public void testTransport() {
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));

        Transport transport = new Transport("label", "linkName", agents, "0.1");
        assertEquals("label", transport.label);
        assertEquals("linkName", transport.getCompartmentLinkName());
        assertEquals(agents, transport.getAgents());
        assertEquals(0.1f, transport.getRate(), 0.01f);
        assertEquals("label: linkName agent1(),agent2() @ 0.1", transport.toString());
        
        transport = new Transport(null, "linkName", null, "0.1");
        assertEquals(null, transport.label);
        assertEquals("linkName", transport.getCompartmentLinkName());
        assertEquals(null, transport.getAgents());
        assertEquals(0.1f, transport.getRate(), 0.01f);
        assertEquals("linkName @ 0.1", transport.toString());
    }
}
