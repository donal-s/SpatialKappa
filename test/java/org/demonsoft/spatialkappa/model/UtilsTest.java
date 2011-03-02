package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.Utils;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void testGetFlatString() {
        Object[] input = { "test", 5, null, new Agent("test2") };
        
        assertEquals("test,5,null,test2()", Utils.getFlatString(Arrays.asList(input)));
        
        assertEquals("test,5,null,test2()", Utils.getFlatString(",", false, Arrays.asList(input)));
        assertEquals("test,5,test2()", Utils.getFlatString(",", true, Arrays.asList(input)));
        assertEquals("test 5 null test2()", Utils.getFlatString(" ", false, Arrays.asList(input)));
        
        assertEquals("test,5,null,test2()", Utils.getFlatString(",", false, input));
        assertEquals("test,5,test2()", Utils.getFlatString(",", true, input));
        assertEquals("test 5 null test2()", Utils.getFlatString(" ", false, input));
    }

    @Test
    public void testGetComplexes() {
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(new Agent("agent1"));

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

        List<Complex> expected = new ArrayList<Complex>();
        expected.add(new Complex(new Agent("agent1")));

        checkComplexes(expected, complexes);

        Agent agent1 = new Agent("agent1");
        Agent agent2 = new Agent("agent2", new AgentSite("link1", null, "?"), new AgentSite("link2", null, "1"));
        Agent agent3 = new Agent("agent3", new AgentSite("link1", null, "?"), new AgentSite("link2", null, "2"));
        Agent agent4 = new Agent("agent4", new AgentSite("link1", null, "_"), new AgentSite("link2", null, "1"));
        Agent agent5 = new Agent("agent5", new AgentSite("link1", null, "_"), new AgentSite("link2", null, "2"), new AgentSite("link3", null, "3"));
        Agent agent6 = new Agent("agent6", new AgentSite("link1", null, "3"), new AgentSite("link2", (String) null, null));
        Agent agent7 = new Agent("agent7", new AgentSite("link1", (String) null, null));

        agents = new ArrayList<Agent>();
        agents.add(agent1);
        agents.add(agent2);
        agents.add(agent3);
        agents.add(agent4);
        agents.add(agent5);
        agents.add(agent6);
        agents.add(agent7);
        complexes = Utils.getComplexes(agents);

        expected = new ArrayList<Complex>();
        expected.add(new Complex(agent1));
        expected.add(new Complex(agent2, agent4));
        expected.add(new Complex(agent3, agent5, agent6));
        expected.add(new Complex(agent7));

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
    

}
