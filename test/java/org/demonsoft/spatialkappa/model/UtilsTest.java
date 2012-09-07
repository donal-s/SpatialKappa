package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class UtilsTest {

    @Test
    public void testGetFlatString() {
        Object[] input = { "test", 5, null, new Agent("test2") };
        
        assertEquals("test,5,null,test2()", Utils.getFlatString(Arrays.asList(input)));
        
        assertEquals("test,5,null,test2()", Utils.getFlatString(",", false, input));
        assertEquals("test,5,test2()", Utils.getFlatString(",", true, input));
        assertEquals("test 5 null test2()", Utils.getFlatString(" ", false, input));
    }

    @Test
    public void testGetComplexes() {
        List<Agent> agents = getList(new Agent("agent1"));

        try {
            Utils.getComplexes(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        List<Complex> complexes = Utils.getComplexes(new ArrayList<Agent>());
        assertEquals(0, complexes.size());

        complexes = Utils.getComplexes(agents);

        List<Complex> expected = getList(new Complex(new Agent("agent1")));

        checkComplexes(expected, complexes);

        Agent agent1 = new Agent("agent1");
        Agent agent2 = new Agent("agent2", new AgentSite("link1", null, "?"), new AgentSite("link2", null, "1"));
        Agent agent3 = new Agent("agent3", new AgentSite("link1", null, "?"), new AgentSite("link2", null, "2"));
        Agent agent4 = new Agent("agent4", new AgentSite("link1", null, "_"), new AgentSite("link2", null, "1"));
        Agent agent5 = new Agent("agent5", new AgentSite("link1", null, "_"), new AgentSite("link2", null, "2"), new AgentSite("link3", null, "3"));
        Agent agent6 = new Agent("agent6", new AgentSite("link1", null, "3"), new AgentSite("link2", (String) null, null));
        Agent agent7 = new Agent("agent7", new AgentSite("link1", (String) null, null));

        agents = getList(agent1, agent2, agent3, agent4, agent5, agent6, agent7);
        complexes = Utils.getComplexes(agents);

        expected = getList(new Complex(agent1), new Complex(agent2, agent4), 
                new Complex(agent3, agent5, agent6), new Complex(agent7));

        checkComplexes(expected, complexes);
    }

    private void checkComplexes(List<Complex> expected, List<Complex> actual) {
        Set<String> expectedSet = getComplexStrings(expected);
        Set<String> actualSet = getComplexStrings(actual);
        assertEquals(expectedSet, actualSet);
    }
    
    private Set<String> getComplexStrings(List<Complex> complexes) {
        Set<String> result = new HashSet<String>();
        for (Complex complex : complexes) {
            result.add(complex.toString());
        }
        return result;
    }
    

    @Test
    public void testGetLinkedAgents() {

        try {
            Utils.getLinkedAgents(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Agent agent1 = new Agent("agent1");
        Agent agent2 = new Agent("agent2", new AgentSite("link1", null, "?"), new AgentSite("link2", null, "1"));
        Agent agent3 = new Agent("agent3", new AgentSite("link1", null, "?"), new AgentSite("link2", null, "2"));
        Agent agent4 = new Agent("agent4", new AgentSite("link1", null, "_"), new AgentSite("link2", null, "1"));
        Agent agent5 = new Agent("agent5", new AgentSite("link1", null, "_"), new AgentSite("link2", null, "2"), new AgentSite("link3", null, "3"));
        Agent agent6 = new Agent("agent6", new AgentSite("link1", null, "3"), new AgentSite("link2", (String) null, null));
        Agent agent7 = new Agent("agent7", new AgentSite("link1", (String) null, null));
        Utils.getComplexes(getList(agent1, agent2, agent3, agent4, agent5, agent6, agent7));
        
        List<Agent> result = Utils.getLinkedAgents(agent1);
        List<Agent> expected = getList(agent1);
        assertEquals(expected, result);
        
        result = Utils.getLinkedAgents(agent2);
        expected = getList(agent2, agent4);
        assertEquals(expected, result);
        
        result = Utils.getLinkedAgents(agent5);
        expected = getList(agent5, agent3, agent6);
        assertEquals(expected, result);
        
        result = Utils.getLinkedAgents(agent7);
        expected = getList(agent7);
        assertEquals(expected, result);
    }

    @Test
    public void testGetLinkedColocatedAgents() {

        try {
            Utils.getLinkedColocatedAgents(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Agent agent1 = new Agent("agent1", new Location("A"));
        Agent agent2 = new Agent("agent2", new Location("A"), new AgentSite("link1", null, "?"), new AgentSite("link2", null, "1"));
        Agent agent3 = new Agent("agent3", new Location("A"), new AgentSite("link1", null, "?"), new AgentSite("link2", null, "2"));
        Agent agent4 = new Agent("agent4", new Location("A"), new AgentSite("link1", null, "_"), new AgentSite("link2", null, "1"));
        Agent agent5 = new Agent("agent5", new Location("B"), new AgentSite("link1", null, "_"), new AgentSite("link2", null, "2", "ch"), new AgentSite("link3", null, "3", "ch"));
        Agent agent6 = new Agent("agent6", new Location("A"), new AgentSite("link1", null, "3"), new AgentSite("link2", (String) null, null));
        Agent agent7 = new Agent("agent7", new Location("A"), new AgentSite("link1", (String) null, null));
        Utils.getComplexes(getList(agent1, agent2, agent3, agent4, agent5, agent6, agent7));
        
        List<Agent> result = Utils.getLinkedColocatedAgents(agent1);
        List<Agent> expected = getList(agent1);
        assertEquals(expected, result);
        
        result = Utils.getLinkedColocatedAgents(agent2);
        expected = getList(agent2, agent4);
        assertEquals(expected, result);
        
        result = Utils.getLinkedColocatedAgents(agent5);
        expected = getList(agent5);
        assertEquals(expected, result);
        
        result = Utils.getLinkedColocatedAgents(agent3);
        expected = getList(agent3);
        assertEquals(expected, result);
        
        result = Utils.getLinkedColocatedAgents(agent6);
        expected = getList(agent6);
        assertEquals(expected, result);
        
        result = Utils.getLinkedColocatedAgents(agent7);
        expected = getList(agent7);
        assertEquals(expected, result);
    }


    @SuppressWarnings("unused")
    @Test
    public void testCreateCloneAgentMap() {
        List<Agent> leftAgents = new ArrayList<Agent>();
        List<Agent> rightAgents = new ArrayList<Agent>();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "t", null)));

        // Test empty case
        Map<Agent, Agent> originalMap = new HashMap<Agent, Agent>();
        Map<Agent, Agent> resultMap = Utils.createCloneAgentMap(originalMap);
        assertEquals(0, resultMap.size());

        // Test different template complexes, original complexes
        Agent templateSourceAgent = new Agent("source");
        Agent templateTargetAgent = new Agent("target");
        new Complex(templateSourceAgent);
        new Complex(templateTargetAgent);

        Agent realSourceAgent = new Agent("source");
        Agent realTargetAgent = new Agent("target");
        new Complex(realSourceAgent);
        new Complex(realTargetAgent);

        originalMap.clear();
        originalMap.put(templateSourceAgent, realSourceAgent);
        originalMap.put(templateTargetAgent, realTargetAgent);

        resultMap = Utils.createCloneAgentMap(originalMap);
        checkCloneMap(originalMap, resultMap);

        // Test same template complexes, original complexes
        new Complex(templateSourceAgent, templateTargetAgent);
        realSourceAgent = new Agent("source");
        realTargetAgent = new Agent("target");
        new Complex(realSourceAgent, realTargetAgent);

        originalMap.clear();
        originalMap.put(templateSourceAgent, realSourceAgent);
        originalMap.put(templateTargetAgent, realTargetAgent);

        resultMap = Utils.createCloneAgentMap(originalMap);
        checkCloneMap(originalMap, resultMap);

        // Test same template complexes, original complexes, linked agents
        realSourceAgent = new Agent("source", new AgentSite("x", null, "1"));
        realTargetAgent = new Agent("target", new AgentSite("y", null, "1"));
        new Complex(realSourceAgent, realTargetAgent);

        originalMap.clear();
        originalMap.put(templateSourceAgent, realSourceAgent);
        originalMap.put(templateTargetAgent, realTargetAgent);

        resultMap = Utils.createCloneAgentMap(originalMap);
        checkCloneMap(originalMap, resultMap);
        
        // Test problem case - multiple domains
        templateSourceAgent = new Agent("source", new Location("cytosol"), new AgentSite("x", null, "1", "domainLink"));
        templateTargetAgent = new Agent("target", new Location("membrane"), new AgentSite("y", null, "1"));
        new Complex(templateSourceAgent, templateTargetAgent);
        
        realSourceAgent = new Agent("source", new Location("cytosol", INDEX_1), new AgentSite("x", null, "1", "domainLink"));
        realTargetAgent = new Agent("target", new Location("membrane", INDEX_0), new AgentSite("y", null, "1"));
        new Complex(realSourceAgent, realTargetAgent);

        originalMap.clear();
        originalMap.put(templateTargetAgent, realTargetAgent);
        originalMap.put(templateSourceAgent, realSourceAgent);
        
        resultMap = Utils.createCloneAgentMap(originalMap);
        checkCloneMap(originalMap, resultMap);
    }
    

    private void checkCloneMap(Map<Agent, Agent> originalMap, Map<Agent, Agent> resultMap) {
        assertEquals(originalMap.size(), resultMap.size());

        for (Map.Entry<Agent, Agent> originalEntry : originalMap.entrySet()) {
            Agent templateAgent = originalEntry.getKey();
            Agent originalAgent = originalEntry.getValue();

            assertTrue(resultMap.containsKey(templateAgent));
            Agent cloneAgent = resultMap.get(templateAgent);
            assertEquals(originalAgent.getComplex().toString(), cloneAgent.getComplex().toString());
            assertNotSame(cloneAgent, originalAgent);
            assertNotSame(cloneAgent.getComplex(), originalAgent.getComplex());
        }

        // Unlinked template agents should each have unique cloned complex
        for (Map.Entry<Agent, Agent> originalEntry : originalMap.entrySet()) {
            Agent templateAgent = originalEntry.getKey();
            for (Map.Entry<Agent, Agent> originalOtherEntry : originalMap.entrySet()) {
                Agent templateOtherAgent = originalOtherEntry.getKey();
                if (templateAgent != templateOtherAgent) {
                    if (templateAgent.getComplex() == templateOtherAgent.getComplex()) {
                        assertSame(resultMap.get(templateAgent).getComplex(), resultMap.get(templateOtherAgent).getComplex());
                    }
                    else {
                        assertNotSame(resultMap.get(templateAgent).getComplex(), resultMap.get(templateOtherAgent).getComplex());
                    }
                }
            }
        }
    }

    @Test
    public void testIsValidComplexes_basic() {
        List<Compartment> compartments = getList(new Compartment("A", 2, 3), new Compartment("B", 2, 3));
        Channel channel = new Channel("dl", new Location("A", INDEX_X, INDEX_Y), new Location("B", INDEX_X, INDEX_Y));
        List<Channel> channels = getList(channel);
        
        List<Agent> agents = getList(new Agent("a"));
        Utils.getComplexes(agents);
        assertTrue(Utils.isValidComplexes(agents, channels, compartments));
        
        // Unlinked
        
        agents = getList(new Agent("a"), new Agent("b"));
        Utils.getComplexes(agents);
        assertTrue(Utils.isValidComplexes(agents, channels, compartments));
        
        agents = getList(new Agent("a", new Location("A", INDEX_1, INDEX_1)), new Agent("b", new Location("A", INDEX_1, INDEX_1)));
        Utils.getComplexes(agents);
        assertTrue(Utils.isValidComplexes(agents, channels, compartments));
        
        agents = getList(new Agent("a", new Location("A", INDEX_1, INDEX_1)), new Agent("b", new Location("B", INDEX_0, INDEX_1)));
        Utils.getComplexes(agents);
        assertTrue(Utils.isValidComplexes(agents, channels, compartments));
        
        // Co located
        
        agents = getList(new Agent("a", new Location("A", INDEX_1, INDEX_1), new AgentSite("s", null, "1")), 
                new Agent("b", new Location("A", INDEX_1, INDEX_1), new AgentSite("s", null, "1")));
        Utils.getComplexes(agents);
        assertTrue(Utils.isValidComplexes(agents, channels, compartments));
        
        agents = getList(new Agent("a", new Location("A", INDEX_1, INDEX_1), new AgentSite("s", null, "1")), 
                new Agent("b", new Location("A", INDEX_0, INDEX_1), new AgentSite("s", null, "1")));
        Utils.getComplexes(agents);
        assertFalse(Utils.isValidComplexes(agents, channels, compartments));
        
        agents = getList(new Agent("a", new Location("A", INDEX_1, INDEX_1), new AgentSite("s", null, "1")), 
                new Agent("b", new Location("B", INDEX_1, INDEX_1), new AgentSite("s", null, "1")));
        Utils.getComplexes(agents);
        assertFalse(Utils.isValidComplexes(agents, channels, compartments));
        
        // Channel linked
        
        agents = getList(new Agent("a", new Location("A", INDEX_1, INDEX_1), new AgentSite("s", null, "1", "dl")), 
                new Agent("b", new Location("A", INDEX_1, INDEX_1), new AgentSite("s", null, "1")));
        Utils.getComplexes(agents);
        assertFalse(Utils.isValidComplexes(agents, channels, compartments));
        
        agents = getList(new Agent("a", new Location("A", INDEX_1, INDEX_1), new AgentSite("s", null, "1", "dl")), 
                new Agent("b", new Location("A", INDEX_0, INDEX_1), new AgentSite("s", null, "1")));
        Utils.getComplexes(agents);
        assertFalse(Utils.isValidComplexes(agents, channels, compartments));
        
        agents = getList(new Agent("a", new Location("A", INDEX_1, INDEX_1), new AgentSite("s", null, "1", "dl")), 
                new Agent("b", new Location("B", INDEX_1, INDEX_1), new AgentSite("s", null, "1")));
        Utils.getComplexes(agents);
        assertTrue(Utils.isValidComplexes(agents, channels, compartments));
        
        agents = getList(new Agent("a", new Location("A", INDEX_1, INDEX_1), new AgentSite("s", null, "1", "dl")), 
                new Agent("b", new Location("B", INDEX_0, INDEX_1), new AgentSite("s", null, "1")));
        Utils.getComplexes(agents);
        assertFalse(Utils.isValidComplexes(agents, channels, compartments));
    }

}
