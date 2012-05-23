package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.Transport;
import org.demonsoft.spatialkappa.model.KappaModel.ModelOnlySimulationState;
import org.junit.Test;

public class TransportTest extends TransitionTest {

    @SuppressWarnings("unused")
    @Test
    public void testTransport_invalid() {
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));

        try {
            new Transport("label", null, agents, new VariableExpression("0.1"));
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
    }

    @Test
    public void testTransport() {
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));
        agents.add(new Agent("agent2"));
        Map<String, Variable> variables = new HashMap<String, Variable>();
        SimulationState state = new ModelOnlySimulationState(variables);

        Transport transport = new Transport("label", "linkName", agents, new VariableExpression(0.1f));
        assertEquals("label", transport.label);
        assertEquals("linkName", transport.getCompartmentLinkName());
        assertEquals(agents, transport.getAgents());
        assertEquals(0.1f, transport.getRate().evaluate(state).value, 0.01f);
        assertEquals("label: linkName agent1,agent2 @ 0.1", transport.toString());
        
        transport = new Transport(null, "linkName", null, new VariableExpression(0.1f));
        assertEquals(null, transport.label);
        assertEquals("linkName", transport.getCompartmentLinkName());
        assertEquals(null, transport.getAgents());
        assertEquals(0.1f, transport.getRate().evaluate(state).value, 0.01f);
        assertEquals("linkName @ 0.1", transport.toString());
    }
}
