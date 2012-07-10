package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class UtilsTest {

    @Test
    public void testGetFlatString() {
        Object[] input = { "test", 5, null, new Agent("test2") };
        
        assertEquals("test,5,null,test2", Utils.getFlatString(Arrays.asList(input)));
        
        assertEquals("test,5,null,test2", Utils.getFlatString(",", false, input));
        assertEquals("test,5,test2", Utils.getFlatString(",", true, input));
        assertEquals("test 5 null test2", Utils.getFlatString(" ", false, input));
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


}
