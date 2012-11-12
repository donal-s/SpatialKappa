package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.WILDCARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ComplexMatcherTest {

    private ComplexMatcher matcher = new ComplexMatcher();

    @Test
    public void testGetExactMatches() {
        Complex complex = new Complex(new Agent("agent"));

        try {
            matcher.isExactMatch(null, complex);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            matcher.isExactMatch(complex, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Agent agent1 = new Agent("agent");
        complex = new Complex(agent1);

        checkExactMatch(complex, complex, true);

        checkExactMatch(complex, new Complex(new Agent("agent2")), false);
        checkExactMatch(complex, new Complex(new Agent("agent"), new Agent("agent2")), false);
        checkExactMatch(complex, new Complex(new Agent("agent"), new Agent("agent")), false);

        agent1 = new Agent("agent1");
        Agent agent2 = new Agent("agent2");
        complex = new Complex(agent1, agent2);
        checkExactMatch(complex, complex, true);

        Agent agent3 = new Agent("agent2");
        Agent agent4 = new Agent("agent1");
        checkExactMatch(complex, new Complex(agent3, agent4), true);

        checkExactMatch(complex, new Complex(new Agent("agent1")), false);
        checkExactMatch(complex, new Complex(new Agent("agent1"), new Agent("agent1")), false);

        agent1 = new Agent("agent1");
        agent2 = new Agent("agent2");
        agent3 = new Agent("agent1");
        complex = new Complex(agent1, agent2, agent3);
        checkExactMatch(complex, complex, true);

        checkExactMatch(complex, new Complex(new Agent("agent1"), new Agent("agent2")), false);
        checkExactMatch(complex, new Complex(new Agent("agent2"), new Agent("agent2")), false);

        agent1 = new Agent("agent", new AgentSite("x", "s", null));
        complex = new Complex(agent1);
        checkExactMatch(complex, complex, true);

        agent2 = new Agent("agent", new AgentSite("x", "s", null));
        checkExactMatch(complex, new Complex(agent2), true);

        checkExactMatch(complex, new Complex(new Agent("agent")), false);

        checkExactMatch(complex, new Complex(new Agent("agent", new AgentSite("y", "s", null))), false);
        // If state interface is named, should be an exact match
        checkExactMatch(complex, new Complex(new Agent("agent", new AgentSite("x", "t", null))), false);
        checkExactMatch(complex, new Complex(new Agent("agent", new AgentSite("x", null, null))), false);

        agent1 = new Agent("agent1", new AgentSite("x", null, "1"));
        agent2 = new Agent("agent2", new AgentSite("y", null, "1"));
        complex = new Complex(agent1, agent2);
        checkExactMatch(complex, complex, true);

        agent3 = new Agent("agent1", new AgentSite("x", null, "2"));
        agent4 = new Agent("agent2", new AgentSite("y", null, "2"));
        checkExactMatch(complex, new Complex(agent3, agent4), true);

        checkExactMatch(complex, new Complex(new Agent("agent1", new AgentSite("x", null, "?"))), false);
        checkExactMatch(complex, new Complex(new Agent("agent2", new AgentSite("y", null, "_"))), false);

        agent1 = new Agent("agent1", new AgentSite("x", null, "1"), new AgentSite("y", null, "2"));
        agent2 = new Agent("agent2", new AgentSite("y", null, "1"), new AgentSite("z", null, "2"));
        complex = new Complex(agent1, agent2);
        checkExactMatch(complex, complex, true);

        agent3 = new Agent("agent1", new AgentSite("x", null, "3"), new AgentSite("y", null, "4"));
        agent4 = new Agent("agent2", new AgentSite("y", null, "3"), new AgentSite("z", null, "4"));
        checkExactMatch(complex, new Complex(agent3, agent4), true);

        agent3 = new Agent("agent1", new AgentSite("y", null, "4"), new AgentSite("x", null, "3"));
        agent4 = new Agent("agent2", new AgentSite("y", null, "3"), new AgentSite("z", null, "4"));
        checkExactMatch(complex, new Complex(agent3, agent4), true);

        checkExactMatch(complex, new Complex(new Agent("agent1", new AgentSite("x", null, "3"), new AgentSite("y", null, null)), new Agent("agent2",
                new AgentSite("y", null, "3"), new AgentSite("z", null, null))), false);
        checkExactMatch(complex, new Complex(new Agent("agent1", new AgentSite("x", null, "2")), new Agent("agent2", new AgentSite("y", null, "2"))), false);
        
//      [ERK(Y187~p,T185~u!2), MEK(S218~p,S222~p,s!2)]
        Complex template = new Complex(
                new Agent("ERK", new AgentSite("Y187", "p", null), new AgentSite("T185", "u", "1")),
                new Agent("MEK", new AgentSite("S218", "p", null), new AgentSite("S222", "p", null), new AgentSite("s", null, "1")));
        
        Complex target = new Complex(
            new Agent("ERK", new AgentSite("Y187", "p", null), new AgentSite("T185", "u", "2")),
            new Agent("MEK", new AgentSite("S218", "p", null), new AgentSite("S222", "p", null), new AgentSite("s", null, "2")));
        checkExactMatch(template, target, true);

    }

    @Test
    public void testGetExactMatches_located() {
        Complex complex = new Complex(new Agent("agent", new Location("comp1")));

        checkExactMatch(complex, complex, true);
        checkExactMatch(complex, new Complex(new Agent("agent2", new Location("comp1"))), false);
        checkExactMatch(complex, new Complex(new Agent("agent")), false);
        checkExactMatch(complex, new Complex(new Agent("agent", new Location("comp2"))), false);
        checkExactMatch(complex, new Complex(new Agent("agent", new Location("comp1", INDEX_0))), false);
        checkExactMatch(complex, new Complex(new Agent("agent", new Location("comp1"))), true);
        
        complex = new Complex(new Agent("agent", new Location("comp1", INDEX_0)));

        checkExactMatch(complex, complex, true);
        checkExactMatch(complex, new Complex(new Agent("agent2", new Location("comp1", INDEX_0))), false);
        checkExactMatch(complex, new Complex(new Agent("agent")), false);
        checkExactMatch(complex, new Complex(new Agent("agent", new Location("comp2"))), false);
        checkExactMatch(complex, new Complex(new Agent("agent", new Location("comp1"))), false);
        checkExactMatch(complex, new Complex(new Agent("agent", new Location("comp1", INDEX_1))), false);
        checkExactMatch(complex, new Complex(new Agent("agent", new Location("comp1", INDEX_0))), true);
    }

    @Test
    public void testIsStatesMatch() {
        Agent agent1 = new Agent("agent");
        Agent agent2 = new Agent("agent2");
        assertTrue(matcher.isStatesMatch(agent1, agent2, true));
        assertTrue(matcher.isStatesMatch(agent1, agent2, false));

        agent1 = new Agent("agent", new AgentSite("x", "s", null));
        agent2 = new Agent("agent2", new AgentSite("x", "s", null));
        assertTrue(matcher.isStatesMatch(agent1, agent2, true));
        assertTrue(matcher.isStatesMatch(agent1, agent2, false));

        agent1 = new Agent("agent", new AgentSite("x", "t", null));
        agent2 = new Agent("agent2", new AgentSite("x", "s", null));
        assertFalse(matcher.isStatesMatch(agent1, agent2, true));
        assertFalse(matcher.isStatesMatch(agent1, agent2, false));

        agent1 = new Agent("agent", new AgentSite("x", "s", null));
        agent2 = new Agent("agent2", new AgentSite("y", "s", null));
        assertFalse(matcher.isStatesMatch(agent1, agent2, true));
        assertFalse(matcher.isStatesMatch(agent1, agent2, false));

        agent1 = new Agent("agent");
        agent2 = new Agent("agent2", new AgentSite("y", "s", null));
        assertFalse(matcher.isStatesMatch(agent1, agent2, true));
        assertTrue(matcher.isStatesMatch(agent1, agent2, false));

        agent1 = new Agent("agent", new AgentSite("x", "s", "1"));
        agent2 = new Agent("agent2", new AgentSite("x", "s", "2"));
        assertTrue(matcher.isStatesMatch(agent1, agent2, true));
        assertTrue(matcher.isStatesMatch(agent1, agent2, false));
    }

    private void checkExactMatch(Complex template, Complex target, boolean expectedMatch) {
        assertEquals(expectedMatch, matcher.isExactMatch(template, target));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetPartialMatches() {
        Complex complex = new Complex(new Agent("agent"));

        try {
            matcher.getPartialMatches(null, complex);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            matcher.getPartialMatches(complex, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Agent agent1 = new Agent("agent");
        complex = new Complex(agent1);

        Map<Agent, Agent> expected = new HashMap<Agent, Agent>();
        expected.put(agent1, agent1);
        checkPartialMatches(complex, complex, expected);

        checkPartialMatches(new Complex(new Agent("agent2")), complex);
        checkPartialMatches(new Complex(new Agent("agent"), new Agent("agent2")), complex);
        checkPartialMatches(new Complex(new Agent("agent"), new Agent("agent")), complex);

        agent1 = new Agent("agent1");
        Agent agent2 = new Agent("agent2");
        complex = new Complex(agent1, agent2);
        expected.clear();
        expected.put(agent1, agent1);
        expected.put(agent2, agent2);
        checkPartialMatches(complex, complex, expected);

        Agent agent3 = new Agent("agent2");
        Agent agent4 = new Agent("agent1");
        expected.clear();
        expected.put(agent1, agent4);
        expected.put(agent2, agent3);
        checkPartialMatches(complex, new Complex(agent3, agent4), expected);

        expected.clear();
        expected.put(agent4, agent1);
        checkPartialMatches(new Complex(agent4), complex, expected);

        checkPartialMatches(new Complex(new Agent("agent1"), new Agent("agent1")), complex);

        agent1 = new Agent("agent1");
        agent2 = new Agent("agent2");
        agent3 = new Agent("agent1");
        complex = new Complex(agent1, agent2, agent3);
        expected.clear();
        expected.put(agent1, agent1);
        expected.put(agent2, agent2);
        expected.put(agent3, agent3);
        Map<Agent, Agent> expected2 = new HashMap<Agent, Agent>();
        expected2.put(agent1, agent3);
        expected2.put(agent2, agent2);
        expected2.put(agent3, agent1);
        checkPartialMatches(complex, complex, expected, expected2);

        agent4 = new Agent("agent1");
        expected.clear();
        expected.put(agent4, agent1);
        expected2.clear();
        expected2.put(agent4, agent3);
        checkPartialMatches(new Complex(agent4), complex, expected, expected2);

        Agent agent5 = new Agent("agent2");
        expected.clear();
        expected.put(agent5, agent2);
        checkPartialMatches(new Complex(agent5), complex, expected);

        agent4 = new Agent("agent1");
        agent5 = new Agent("agent1");
        expected.clear();
        expected.put(agent4, agent1);
        expected.put(agent5, agent3);
        expected2.clear();
        expected2.put(agent4, agent3);
        expected2.put(agent5, agent1);
        checkPartialMatches(new Complex(agent4, agent5), complex, expected, expected2);

        agent4 = new Agent("agent1");
        agent5 = new Agent("agent2");
        expected.clear();
        expected.put(agent4, agent1);
        expected.put(agent5, agent2);
        expected2.clear();
        expected2.put(agent4, agent3);
        expected2.put(agent5, agent2);
        checkPartialMatches(new Complex(agent4, agent5), complex, expected, expected2);

        checkPartialMatches(new Complex(new Agent("agent2"), new Agent("agent2")), complex);

        agent1 = new Agent("agent", new AgentSite("x", "s", null));
        complex = new Complex(agent1);
        expected.clear();
        expected.put(agent1, agent1);
        checkPartialMatches(complex, complex, expected);

        agent2 = new Agent("agent", new AgentSite("x", "s", null));
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(new Complex(agent2), complex, expected);

        agent2 = new Agent("agent");
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(new Complex(agent2), complex, expected);

        checkPartialMatches(new Complex(new Agent("agent", new AgentSite("y", "s", null))), complex);
        // If state interface is named, should be an exact match
        checkPartialMatches(new Complex(new Agent("agent", new AgentSite("x", "t", null))), complex);

        agent2 = new Agent("agent", new AgentSite("x", null, null));
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(new Complex(agent2), complex, expected);

        agent1 = new Agent("agent1", new AgentSite("x", null, "1"));
        agent2 = new Agent("agent2", new AgentSite("y", null, "1"));
        complex = new Complex(agent1, agent2);
        expected.clear();
        expected.put(agent1, agent1);
        expected.put(agent2, agent2);
        checkPartialMatches(complex, complex, expected);

        agent3 = new Agent("agent1", new AgentSite("x", null, "?"));
        expected.clear();
        expected.put(agent3, agent1);
        checkPartialMatches(new Complex(agent3), complex, expected);

        agent3 = new Agent("agent2", new AgentSite("y", null, "_"));
        expected.clear();
        expected.put(agent3, agent2);
        checkPartialMatches(new Complex(agent3), complex, expected);

        agent3 = new Agent("agent1", new AgentSite("x", null, "2"));
        agent4 = new Agent("agent2", new AgentSite("y", null, "2"));
        expected.clear();
        expected.put(agent3, agent1);
        expected.put(agent4, agent2);
        checkPartialMatches(new Complex(agent3, agent4), complex, expected);

        agent1 = new Agent("agent1", new AgentSite("x", null, "1"), new AgentSite("y", null, "2"));
        agent2 = new Agent("agent2", new AgentSite("y", null, "1"), new AgentSite("z", null, "2"));
        complex = new Complex(agent1, agent2);
        expected.clear();
        expected.put(agent1, agent1);
        expected.put(agent2, agent2);
        checkPartialMatches(complex, complex, expected);

        agent3 = new Agent("agent1", new AgentSite("x", null, "2"));
        agent4 = new Agent("agent2", new AgentSite("y", null, "2"));
        expected.clear();
        expected.put(agent3, agent1);
        expected.put(agent4, agent2);
        checkPartialMatches(new Complex(agent3, agent4), complex, expected);

        agent3 = new Agent("agent1", new AgentSite("y", null, "2"));
        agent4 = new Agent("agent2", new AgentSite("z", null, "2"));
        expected.clear();
        expected.put(agent3, agent1);
        expected.put(agent4, agent2);
        checkPartialMatches(new Complex(agent3, agent4), complex, expected);

        checkPartialMatches(new Complex(new Agent("agent1", new AgentSite("x", null, "2")), new Agent("agent2", new AgentSite("z", null, "2"))), complex);

        
//        MEK(S218~p!?,S222~p!?)
        Complex template = new Complex(new Agent("MEK", new AgentSite("S218", "p", "?"), new AgentSite("S222", "p", "?")));
        
//        [MEK(S218~p,S222~p,s)]
        Complex target = new Complex(
                new Agent("MEK", new AgentSite("S218", "p", null), new AgentSite("S222", "p", "?"), new AgentSite("s", null, null)));
        expected.clear();
        expected.put(template.agents.get(0), target.agents.get(0));
        checkPartialMatches(template, target, expected);

        //      [ERK(Y187~p,T185~u!1), MEK(S218~p,S222~p,s!1)]
        target = new Complex(
              new Agent("ERK", new AgentSite("Y187", "p", null), new AgentSite("T185", "u", "1")),
              new Agent("MEK", new AgentSite("S218", "p", null), new AgentSite("S222", "p", null), new AgentSite("s", null, "1")));
        expected.clear();
        expected.put(template.agents.get(0), target.agents.get(1));
        checkPartialMatches(template, target, expected);

//    [ERK(Y187~p,T185~u!2), MEK(S218~p,S222~p,s!2)]
        target = new Complex(
            new Agent("ERK", new AgentSite("Y187", "p", null), new AgentSite("T185", "u", "2")),
            new Agent("MEK", new AgentSite("S218", "p", null), new AgentSite("S222", "p", null), new AgentSite("s", null, "2")));
        expected.clear();
        expected.put(template.agents.get(0), target.agents.get(1));
        checkPartialMatches(template, target, expected);

//        [ERK(Y187~u!1,T185~p), MEK(S218~p,S222~p,s!1)]
        target = new Complex(
                new Agent("ERK", new AgentSite("Y187", "u", "1"), new AgentSite("T185", "p", null)),
                new Agent("MEK", new AgentSite("S218", "p", null), new AgentSite("S222", "p", null), new AgentSite("s", null, "1")));
        expected.clear();
        expected.put(template.agents.get(0), target.agents.get(1));
        checkPartialMatches(template, target, expected);

        
//        [ERK(Y187~u!1,T185~u!2), MEK(S218~p,S222~p,s!1), MEK(S218~p,S222~p,s!2)]
        target = new Complex(
                new Agent("ERK", new AgentSite("Y187", "u", "1"), new AgentSite("T185", "u", "2")),
                new Agent("MEK", new AgentSite("S218", "p", null), new AgentSite("S222", "p", null), new AgentSite("s", null, "1")),
                new Agent("MEK", new AgentSite("S218", "p", null), new AgentSite("S222", "p", null), new AgentSite("s", null, "2")));
        expected.clear();
        expected.put(template.agents.get(0), target.agents.get(1));
        expected2.clear();
        expected2.put(template.agents.get(0), target.agents.get(2));
        checkPartialMatches(template, target, expected, expected2);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetPartialMatches_located() {
        Agent agent1 = new Agent("agent", new Location("comp1"));
        Complex target = new Complex(agent1);

        Map<Agent, Agent> expected = new HashMap<Agent, Agent>();
        expected.put(agent1, agent1);
        checkPartialMatches(target, target, expected);

        checkPartialMatches(new Complex(new Agent("agent2")), target);
        checkPartialMatches(new Complex(new Agent("agent", new Location("comp2"))), target);
        checkPartialMatches(new Complex(new Agent("agent", new Location("comp1", INDEX_0))), target);

        Agent agent2 = new Agent("agent");
        Complex template = new Complex(agent2);
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(template, target, expected);

        agent2 = new Agent("agent", new Location("comp1"));
        template = new Complex(agent2);
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(template, target, expected);

        
        agent1 = new Agent("agent", new Location("comp1", INDEX_0));
        target = new Complex(agent1);

        expected.clear();
        expected.put(agent1, agent1);
        checkPartialMatches(target, target, expected);

        checkPartialMatches(new Complex(new Agent("agent2")), target);
        checkPartialMatches(new Complex(new Agent("agent", new Location("comp2"))), target);
        checkPartialMatches(new Complex(new Agent("agent", new Location("comp1", INDEX_1))), target);

        agent2 = new Agent("agent");
        template = new Complex(agent2);
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(template, target, expected);

        agent2 = new Agent("agent", new Location("comp1"));
        template = new Complex(agent2);
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(template, target, expected);

        agent2 = new Agent("agent", new Location("comp1", INDEX_0));
        template = new Complex(agent2);
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(template, target, expected);
        
        // Check with sites
        agent2 = new Agent("A", new Location("cytosol", INDEX_0), new AgentSite("s", null, null));
        template = new Complex(agent2);
        agent1 = new Agent("A", new Location("cytosol", INDEX_0), new AgentSite("s", null, null));
        target = new Complex(agent1);
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(template, target, expected);
        
        agent1 = new Agent("A", new Location("cytosol", INDEX_0), new AgentSite("t", null, null));
        target = new Complex(agent1);
        expected.clear();
        checkPartialMatches(template, target);
        
        // Check with location wildcards
        agent2 = new Agent("A", new Location("cytosol", INDEX_0, WILDCARD, WILDCARD));
        template = new Complex(agent2);
        
        agent1 = new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_1));
        target = new Complex(agent1);
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(template, target, expected);
        
        checkPartialMatches(template, new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_Y))));
        checkPartialMatches(template, new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, WILDCARD))));
        checkPartialMatches(template, new Complex(new Agent("A", new Location("cytosol", INDEX_1, INDEX_1, INDEX_Y))));
        checkPartialMatches(template, new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1))));
        checkPartialMatches(template, new Complex(new Agent("A", new Location("nucleus", INDEX_0, INDEX_1, INDEX_Y))));
        
        
        agent2 = new Agent("A", new Location("cytosol", WILDCARD, INDEX_1, INDEX_2));
        template = new Complex(agent2);
        
        agent1 = new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, INDEX_2));
        target = new Complex(agent1);
        expected.clear();
        expected.put(agent2, agent1);
        checkPartialMatches(template, target, expected);
        
        checkPartialMatches(template, new Complex(new Agent("A", new Location("cytosol", INDEX_0, INDEX_1, WILDCARD))));
        checkPartialMatches(template, new Complex(new Agent("A", new Location("cytosol", INDEX_1, INDEX_1, INDEX_1))));
    }

    private void checkPartialMatches(Complex template, Complex target, Map<Agent, Agent>... expectedMaps) {
        checkMatches(matcher.getPartialMatches(template, target), template, target, expectedMaps);
    }

    private void checkMatches(List<ComplexMapping> actualMaps, Complex template, Complex target, Map<Agent, Agent>... expectedMaps) {
        assertNotNull(actualMaps);
        assertEquals(expectedMaps.length, actualMaps.size());
        for (int index = 0; index < expectedMaps.length; index++) {
            Map<Agent, Agent> match = null;
            for (ComplexMapping current : actualMaps) {
                if (current.mapping.equals(expectedMaps[index])) {
                    match = current.mapping;
                    assertSame(template, current.template);
                    assertSame(target, current.target);
                    break;
                }
            }
            if (match == null) {
                fail("no match found");
            }
            actualMaps.remove(match);
        }
    }

}
